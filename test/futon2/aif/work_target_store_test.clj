(ns futon2.aif.work-target-store-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.work-target-store :as store])
  (:import (java.nio.file Files Path StandardCopyOption AtomicMoveNotSupportedException)
           (java.io IOException)
           (java.util UUID)))

(def declaration-path
  "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/declarations/wm-work-target-interpretation-v1.edn")
(defn genesis []
  {:schema :wm/work-target-store-genesis-v1 :store/id (UUID/randomUUID)
   :storage-protocol/revision "v1"
   :declaration {:path declaration-path
                 :sha256 "055d579d4bec9ef52c6a3e2949b730d413624ddbc6b60cc00931810a77675e5e"
                 :interpretation-revision "v1"}
   :decision-refs ["invoke-1789502747631-21229-70a1d4a7"]
   :created-at "2026-09-15T18:00:00Z"
   :authorized-by {:actor "test-only" :commission "isolated-fixture"}
   :statement "rollout genesis, not historical initialization"})
(defn operation [id]
  {:id id :kind :carry-and-introduce :caller-identity-type :test
   :information-cutoff "2026-09-15T18:00:00Z"})
(defn p ^Path [s n] (.resolve ^Path (:path s) ^String n))
(defn get-edn [s n] (edn/read-string (slurp (.toFile (p s n)))))
(defn put-edn [s n v] (spit (.toFile (p s n)) (pr-str v)))
(defn cleanup [^Path dir]
  (doseq [f (reverse (file-seq (.toFile dir)))] (io/delete-file f)))
(defn fixture [f]
  ;; Real filesystem, temporary directories only. Authority declaration is read-only.
  (let [dir (Files/createTempDirectory "work-target-store-test-" (make-array java.nio.file.attribute.FileAttribute 0))
        s (store/open-store (str (.resolve dir "store")) {:payload-validator (constantly :ok)})]
    (try (f s) (finally (cleanup dir)))))
(defn establish [s] (store/initialize! s (genesis)))
(defn append! [s id payload]
  (store/commit! s (:head (store/read-store s)) (operation id) payload))
(defn damage [s reason]
  (let [result (store/read-store s)]
    (is (= :damaged (:status result)) (pr-str result))
    (is (= reason (:reason result)) (pr-str result))))

(deftest lifecycle-and-create-once
  (fixture
   (fn [s]
     (is (= :model-not-established (:status (store/read-store s))))
     (is (= :model-not-established (:status (store/commit! s nil (operation "a") {}))))
     (is (not (.exists (.toFile (:path s)))))
     (let [g (genesis) init (store/initialize! s g)]
       (is (= :established-no-snapshots (:status init)) (pr-str init))
       (is (= :already-established (:status (store/initialize! s g))))
       (is (= :genesis-conflict (:status (store/initialize! s (assoc g :created-at "different")))))
       (let [r (append! s "empty" {}) read (store/read-store s)]
         (is (= :committed (:status r)))
         (is (= :committed (:status read)))
         (is (= {} (:payload (:snapshot read))))
         (is (= (:ref r) (:ref read))))))))

(deftest incomplete-initialization-and-missing-head
  (fixture
   (fn [s]
     ;; Simulated interruption after durable genesis, before publishing HEAD.
     (let [g (genesis)
           result (binding [store/*failpoint* #(when (= :genesis.edn-after-directory-force %)
                                               (throw (IOException. "simulated crash")))]
                    (store/initialize! s g))]
       (is (= :persistence-failed (:status result)))
       (is (= :initialization-incomplete (:status (store/read-store s))))
       (is (= :initialization-incomplete (:status (store/initialize! s g)))))))
  (fixture
   (fn [s]
     (establish s)
     (Files/delete (p s "HEAD.edn"))
     (damage s :missing-head)
     (is (= :damaged (:status (store/initialize! s (genesis)))))))
  (fixture
   (fn [s]
     (establish s)
     ;; No initialization evidence: cannot distinguish data loss from setup.
     (doseq [n ["HEAD.edn" "INIT.edn" "INITIALIZED.edn"]] (Files/delete (p s n)))
     (is (= :pending-recovery (:status (store/read-store s)))))))

(deftest chain-damage-controls
  ;; Real filesystem mutations simulate corruption/deletion; no power-loss claim.
  (doseq [[label mutation reason]
          [[:duplicate #(Files/copy (p % "snapshots/1.edn") (p % "snapshots/01.edn")
                                    (make-array java.nio.file.CopyOption 0)) :duplicate-seq]
           [:altered #(spit (.toFile (p % "snapshots/1.edn")) " \n" :append true) :hash-mismatch]
           [:middle #(Files/delete (p % "snapshots/2.edn")) :middle-gap]
           [:tail #(Files/delete (p % "snapshots/3.edn")) :missing-head-snapshot]
           [:head #(Files/delete (p % "HEAD.edn")) :missing-head]
           [:genesis #(put-edn % "genesis.edn" (assoc (get-edn % "genesis.edn") :created-at "altered")) :wrong-genesis]
           [:store-id #(put-edn % "HEAD.edn" (assoc (get-edn % "HEAD.edn") :store/id (UUID/randomUUID))) :wrong-store-id]
           [:declaration #(spit (.toFile (p % "declaration.edn")) " \n" :append true) :declaration-hash-mismatch]
           [:multiple-forms #(spit (.toFile (p % "snapshots/2.edn")) "{}" :append true) :parse-failure]
           [:malformed #(spit (.toFile (p % "HEAD.edn")) "{") :parse-failure]
           [:non-contiguous #(Files/move (p % "snapshots/2.edn") (p % "snapshots/9.edn")
                                        (into-array java.nio.file.CopyOption [StandardCopyOption/ATOMIC_MOVE])) :non-contiguous-seq]]]
    (testing (name label)
      (fixture (fn [s]
                 (establish s)
                 (doseq [i (range 3)] (is (= :committed (:status (append! s (str i) {:i i})))))
                 (mutation s)
                 (damage s reason)
                 (is (= :damaged (:status (append! s "refuses" {})))))))))

(deftest threads-share-one-writer-lock
  ;; Actual threads and FileLock, synchronized to propose from the same head.
  (fixture
   (fn [s]
     (let [h (:head (establish s)) start (promise)
           alias (store/open-store (str (:path s) "/../store") {:payload-validator (constantly :ok)})
           a (future @start (store/commit! s h (operation "a") {}))
           b (future @start (store/commit! alias h (operation "b") {}))]
       (deliver start true)
       (let [results [(deref a 5000 :timeout) (deref b 5000 :timeout)]]
         (is (= #{:committed :stale-predecessor} (set (map :status results))) (pr-str results))
         (is (= 1 (:seq (store/read-store s)))))))))

(deftest lost-response-and-operation-intent
  (fixture
   (fn [s]
     (let [h (:head (establish s)) op (operation "lost")
           ;; Simulates successful durable commit followed by loss of its response.
           failure (binding [store/*failpoint* #(when (= :response %) (throw (IOException. "response lost")))]
                     (store/commit! s h op {:a 1 :b 2}))
           original (:ref (store/read-store s))]
       (is (= :persistence-failed (:status failure)))
       (is (:commit-point-reached? failure))
       (is (= :committed (:status (append! s "later" {}))))
       (let [retry (store/commit! s h op (array-map :b 2 :a 1))]
         (is (= :committed (:status retry)))
         (is (:idempotent? retry))
         (is (= original (:ref retry))))
       (is (= :operation-id-reused (:status (store/commit! s h op {:a 3}))))
       (is (= :operation-id-reused (:status (store/commit! s h (assoc op :information-cutoff "new") {:a 1 :b 2}))))
       (is (= 2 (:seq (store/read-store s))))))))

(deftest interrupted-commit-boundaries
  (doseq [boundary [:snapshot-before-write :snapshot-after-write :snapshot-after-force
                    :snapshot-after-rename :snapshot-after-directory-force
                    :head-before-write :head-after-write :head-after-force :head-after-rename
                    :head-after-directory-force :pending-after-delete :response]]
    (testing (name boundary)
      (fixture
       (fn [s]
         (establish s)
         (append! s "acknowledged" {:keep "these exact bytes"})
         (let [before (slurp (.toFile (p s "snapshots/1.edn")))
               h (:head (store/read-store s))
               result (binding [store/*failpoint* #(when (= boundary %) (throw (IOException. "simulated interruption")))]
                        (store/commit! s h (operation "interrupted") {}))
               read (store/read-store s)
               completed? (#{:pending-after-delete :response} boundary)]
           (is (= :persistence-failed (:status result)))
           (is (= boundary (:stage result)))
           (is (= (if completed? :committed :pending-recovery) (:status read)) (pr-str read))
           (is (= (if (#{:head-after-rename :head-after-directory-force :pending-after-delete :response} boundary) 2 1)
                  (get-in read [:head :seq])))
           (is (= before (slurp (.toFile (p s "snapshots/1.edn")))))
           (when-not completed?
             (is (= :pending-recovery (:status (store/commit! s h (operation "interrupted") {})))))))))))

(deftest reference-ancestry
  (fixture
   (fn [s]
     (establish s)
     (let [first-ref (:ref (append! s "first" {}))]
       (append! s "second" {})
       (is (= :resolved (:status (store/resolve-reference s first-ref))))
       (doseq [[ref reason] [[(assoc first-ref :sha256 (apply str (repeat 64 "0"))) :off-chain]
                            [(assoc first-ref :seq 3) :ahead-of-head]
                            [(dissoc first-ref :seq) :missing-reference]
                            [(assoc first-ref :store/id (UUID/randomUUID)) :wrong-store-id]]]
         (is (= {:status :reference-refused :reason reason} (store/resolve-reference s ref))))
       (Files/delete (p s "snapshots/1.edn"))
       (is (= :damaged (:status (store/resolve-reference s first-ref))))))))

(deftest validators-and-declaration-admission
  (fixture
   (fn [s]
     (let [rejecting (assoc s :payload-validator (constantly {:status :lineage-refused}))]
       (is (= :model-not-established (:status (store/commit! rejecting nil (operation "a") {}))))
       (is (not (.exists (.toFile (:path s)))))
       (establish s)
       (is (= :payload-validation-failed (:reason (append! rejecting "a" {}))))
       (is (= :established-no-snapshots (:status (store/read-store s))))
       (append! s "accepted" {})
       (damage rejecting :payload-validation-failed))))
  (fixture
   (fn [s]
     (is (= :declaration-hash-mismatch
            (:reason (store/initialize! s (assoc-in (genesis) [:declaration :sha256] (apply str (repeat 64 "0")))))))
     (is (not (.exists (.toFile (:path s))))))))

(deftest orphan-artifacts-never-reset
  (fixture
   (fn [s]
     (establish s)
     (append! s "a" {})
     (Files/delete (p s "genesis.edn"))
     (is (= :pending-recovery (:status (store/read-store s))))
     (is (= :pending-recovery (:status (store/initialize! s (genesis)))))
     (is (.exists (.toFile (p s "snapshots/1.edn")))))))

(deftest real-force-order-and-simulated-io-failures
  (fixture
   (fn [s]
     (establish s)
     (let [events (atom []) result (binding [store/*event* #(swap! events conj %)] (append! s "io" {}))
           order (mapv (fn [{:keys [io path]}] [io (last (str/split path #"/"))]) @events)]
       (is (= :committed (:status result)))
       ;; Events are emitted only AFTER successful real JDK calls, no redefs/mocks.
       (is (= [[:write "PENDING.edn.tmp"] [:file-force "PENDING.edn.tmp"]
               [:atomic-move "PENDING.edn"] [:directory-force "store"]
               [:write "snapshot.tmp"] [:file-force "snapshot.tmp"]
               [:atomic-move "1.edn"] [:directory-force "snapshots"]
               [:write "HEAD.edn.tmp"] [:file-force "HEAD.edn.tmp"]
               [:atomic-move "HEAD.edn"] [:directory-force "store"] [:directory-force "store"]]
              order))
       (println "REAL-FILESYSTEM force/rename receipt:" (pr-str @events)))))
  (doseq [[label boundary exception] [[:enospc :snapshot-after-write (IOException. "No space left on device (simulated ENOSPC)")]
                                     [:atomic-unsupported :snapshot-after-force (AtomicMoveNotSupportedException. "temp" "snapshot" "simulated")]
                                     [:directory-force :snapshot-after-rename (IOException. "simulated directory fsync failure")]]]
    (testing (name label)
      (fixture
       (fn [s]
         (establish s)
         (let [h (:head (store/read-store s))
               result (binding [store/*failpoint* #(when (= boundary %) (throw exception))]
                        (store/commit! s h (operation "fails") {}))]
           (is (= :persistence-failed (:status result)))
           (is (false? (:commit-point-reached? result)))
           (is (= :pending-recovery (:status (store/read-store s))))
           (is (= h (:head (store/read-store s))))))))))

(deftest production-shaped-opaque-payload
  ;; Shape follows the commissioned P1b packet/declaration, not a production claim.
  (fixture
   (fn [s]
     (establish s)
     (let [payload {:model-context {:declaration-sha256 "055d579d4bec9ef52c6a3e2949b730d413624ddbc6b60cc00931810a77675e5e"}
                    :state {:belief {[:mission "M-test"] {:spawned 1/7 :refined 1/7 :strengthened 1/7
                                                         :addressed 1/7 :falsified 1/7 :foreclosed 1/7 :reopened 1/7}}}
                    :lineage {[:mission "M-test"] {:introduced-by "fixture-admission"}}
                    :information-cutoff "2026-09-15T18:00:00Z"
                    :admissions [{:target [:mission "M-test"] :authority :test-only}]
                    :not-admitted [{:target [:ticket "T-test"] :reason :unresolved-full-policy-coverage}]
                    :registry-context {:path "fixture-only" :sha256 "fixture-pin"}}]
       (is (= :committed (:status (append! s "shaped" payload))))
       (is (= payload (:payload (:snapshot (store/read-store s)))))))))

(deftest initialization-interruption-controls
  ;; Simulated interruption at initialization publication boundaries on real FS.
  (doseq [[boundary expected]
          [[:INIT.edn-after-write :pending-recovery]
           [:INIT.edn-after-force :pending-recovery]
           [:INIT.edn-after-rename :pending-recovery]
           [:INIT.edn-after-directory-force :pending-recovery]
           [:declaration.edn-after-write :pending-recovery]
           [:genesis.edn-after-write :pending-recovery]
           [:genesis.edn-after-force :pending-recovery]
           [:genesis.edn-after-rename :initialization-incomplete]
           [:genesis.edn-after-directory-force :initialization-incomplete]
           [:HEAD.edn-after-write :initialization-incomplete]
           [:HEAD.edn-after-force :initialization-incomplete]
           [:HEAD.edn-after-rename :pending-recovery]
           [:HEAD.edn-after-directory-force :pending-recovery]]]
    (testing (name boundary)
      (fixture
       (fn [s]
         (let [g (genesis)
               result (binding [store/*failpoint* #(when (= boundary %) (throw (IOException. "init interrupted")))]
                        (store/initialize! s g))]
           (is (= :persistence-failed (:status result)))
           (is (= expected (:status (store/read-store s))))
           (is (= expected (:status (store/initialize! s g))))
           (is (not= :committed (:status (store/commit! s nil (operation "no-auto-init") {}))))))))))

(deftest retained-declaration-and-strict-extras
  (fixture
   (fn [s]
     (let [external (.resolve (.getParent ^Path (:path s)) "original-declaration.edn")
           _ (Files/copy (.toPath (io/file declaration-path)) external (make-array java.nio.file.CopyOption 0))
           g (assoc-in (genesis) [:declaration :path] (str external))]
       (is (= :established-no-snapshots (:status (store/initialize! s g))))
       (Files/delete external)
       (is (= :already-established (:status (store/initialize! s g))))
       (is (= :committed (:status (append! s "retained" {}))))
       ;; A temp file is a preparation, not authority: pending recovery.
       (spit (.toFile (p s "orphan.tmp")) "{} {}")
       (is (= :pending-recovery (:status (store/read-store s))))
       (Files/delete (p s "orphan.tmp"))
       ;; Any other extra record is still parsed strictly.
       (spit (.toFile (p s "orphan.edn")) "{} {}")
       (damage s :parse-failure))))
  (fixture
   (fn [s]
     (establish s)
     (Files/delete (p s "snapshots"))
     (damage s :missing-snapshot-directory))))

(deftest interrupted-temp-writes-are-pending-not-damage
  ;; Real filesystem. A crash before any bytes reach a temp file leaves it
  ;; empty; an uncommitted preparation must not read as corrupted history.
  (doseq [leftover ["snapshots/snapshot.tmp" "HEAD.edn.tmp" "PENDING.edn.tmp"]]
    (testing leftover
      (fixture
       (fn [s]
         (establish s)
         (append! s "one" {})
         (let [h (:head (store/read-store s))]
           (spit (.toFile (p s leftover)) "")
           (let [read (store/read-store s)]
             (is (= :pending-recovery (:status read)) (pr-str read))
             (is (= h (:head read))))
           (is (= :pending-recovery (:status (store/commit! s h (operation "two") {}))))
           ;; Damage to committed history still takes precedence.
           (spit (.toFile (p s "snapshots/1.edn")) " \n" :append true)
           (damage s :hash-mismatch)))))))

(deftest envelope-validation-after-consistent-rehash
  ;; Deliberately rehash the tail and HEAD to test semantic checks independently
  ;; of byte-damage detection. This is simulated tampering, not accepted recovery.
  (doseq [[change reason] [[#(assoc % :schema :wrong) :snapshot-envelope]
                          [#(assoc % :store/id (UUID/randomUUID)) :wrong-store-id]
                          [#(assoc % :genesis-sha256 (apply str (repeat 64 "0"))) :wrong-genesis]
                          [#(assoc % :previous-sha256 "wrong") :hash-mismatch]
                          [#(assoc % :expected-head {}) :expected-head-mismatch]
                          [#(dissoc % :operation) :operation-envelope]
                          [#(assoc % :information-cutoff "wrong") :information-cutoff-mismatch]
                          [#(assoc % :committed-intent-sha256 "wrong") :intent-hash-mismatch]]]
    (fixture
     (fn [s]
       (establish s)
       (append! s "first" {})
       (put-edn s "snapshots/1.edn" (change (get-edn s "snapshots/1.edn")))
       (let [bs (Files/readAllBytes (p s "snapshots/1.edn"))
             hash (apply str (map #(format "%02x" (bit-and 255 %))
                                  (.digest (java.security.MessageDigest/getInstance "SHA-256") bs)))]
         (put-edn s "HEAD.edn" (assoc (get-edn s "HEAD.edn") :snapshot-sha256 hash)))
       (damage s reason)))))

(deftest intent-is-independent-of-caller-print-settings
  (fixture
   (fn [s]
     (let [h (:head (establish s)) op (operation "print-settings")
           payload {:model/state {:model/value [1 2 3]}}
           first-result (binding [*print-length* 1 *print-level* 1 *print-namespace-maps* true]
                          (store/commit! s h op payload))
           retry (binding [*print-namespace-maps* false]
                   (store/commit! s h op payload))]
       (is (= :committed (:status first-result)))
       (is (= :committed (:status retry)))
       (is (= (:ref first-result) (:ref retry)))
       (is (:idempotent? retry))))))
