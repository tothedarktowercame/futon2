(ns futon2.aif.interoceptive-activation-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.interoceptive-activation :as activation]
            [futon2.aif.interoceptive-store-lock :as store-lock])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute PosixFilePermission]))

(def lock-record
  {:path store-lock/default-lock-path :file-key "(dev=1,ino=2)"
   :owner "joe" :parent-owner "root" :parent-writable-by-service? false})

(def process-record
  {:process/id "jvm-1" :pid 101 :start-ticks "202"
   :exe "/usr/bin/java" :cmdline-sha256 (apply str (repeat 64 "a"))
   :coordination/status :participating
   :deployment/id "operator-deployment-1" :loaded-at-ms 900
   :loaded-source-pins activation/required-source-pins
   :writer-entrypoints (vec activation/required-writers)})

(def test-controller-authority
  (into {} (map (fn [surface]
                  [surface {:status :lease-enforced
                            :artifact/path (str "/test-controller/" (name surface))
                            :artifact/sha256 (apply str (repeat 64 "d"))}]))
        activation/required-control-surfaces))

(def valid-record
  (let [processes [process-record]
        census-edn (activation/process-census-edn processes)]
   {:schema :wm/interoceptive-writer-participation-v1
   :receipt-sha256 (apply str (repeat 64 "b"))
   :writer-entrypoints (vec activation/required-writers)
   :source-pins activation/required-source-pins
   :observed-at-ms 1000 :valid-until-ms 2000
   :host {:census-complete? true :boot-id "boot-1"
          :writer-census-sha256 activation/required-writer-census-sha256
          :process-census-edn census-edn
          :process-census-sha256 (activation/process-census-sha256 census-edn)}
   :processes processes
   :lease {:path activation/deployment-lease-path
           :protocol :host-launch-reload-lock-v1
           :generation "deployment-1" :status :enforced
           :controls (mapv (fn [[surface control]] (assoc control :surface surface))
                           test-controller-authority)}
   :lock lock-record
   :lease-lock (assoc lock-record :path activation/deployment-lease-path)}))

(defn observable [process]
  (select-keys process [:process/id :pid :start-ticks :exe :cmdline-sha256]))

(defn with-processes [record processes]
  (let [census-edn (activation/process-census-edn processes)]
    (-> record
        (assoc :processes processes)
        (assoc-in [:host :process-census-edn] census-edn)
        (assoc-in [:host :process-census-sha256]
                  (activation/process-census-sha256 census-edn)))))

(defn opts []
  {:now-ms 1500 :source-pins activation/required-source-pins :boot-id "boot-1"
   :controller-authority test-controller-authority
   :controller-probe (fn [_] (apply str (repeat 64 "d")))
   :lock-probe identity :process-probe observable})

(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest candidate-record-never-authorizes-production
  (is (= :test (:authority-class
                (activation/validate-participation
                 valid-record (assoc (opts) :authority-class :production)))))
  (with-redefs [activation/production-receipt-path "/definitely/missing/receipt.edn"]
    (is (= :interoceptive/activation-receipt-unavailable
           (refusal activation/resolve-production-participation!)))))

(deftest exact-coverage-and-freshness-refusals
  (testing "missing writer and nonparticipating process"
    (is (= :interoceptive/activation-writer-coverage
           (refusal #(activation/validate-participation
                      (update valid-record :writer-entrypoints pop) (opts)))))
    (is (= :interoceptive/activation-nonparticipating-writer
           (refusal #(activation/validate-participation
                      (with-processes valid-record
                        [(assoc process-record :coordination/status :not-loaded)])
                      (opts)))))
    (is (= :interoceptive/activation-process-source-unverified
           (refusal #(activation/validate-participation
                      (with-processes valid-record
                        [(dissoc process-record :loaded-source-pins)])
                      (opts))))))
  (testing "stale, source mismatch, process replacement and lock replacement"
    (is (= :interoceptive/activation-stale
           (refusal #(activation/validate-participation valid-record
                                                        (assoc (opts) :now-ms 3000)))))
    (is (= :interoceptive/activation-source-mismatch
           (refusal #(activation/validate-participation
                      valid-record (assoc (opts) :source-pins {})))))
    (is (= :interoceptive/activation-process-changed
           (refusal #(activation/validate-participation
                      valid-record (assoc (opts) :process-probe
                                          (fn [p] (assoc (observable p)
                                                         :start-ticks "changed")))))))
    (is (= :interoceptive/activation-lock-mismatch
           (refusal #(activation/validate-participation
                      valid-record (assoc (opts) :lock-probe
                                          (fn [lock] (assoc lock :file-key "replaced")))))))))

(deftest process-census-and-lock-contract
  (is (= :interoceptive/activation-process-census-invalid
         (refusal #(activation/validate-participation
                    (with-processes valid-record []) (opts)))))
  (is (= :interoceptive/activation-process-writer-coverage
         (refusal #(activation/validate-participation
                    (with-processes valid-record
                      [(assoc process-record :writer-entrypoints [])])
                    (opts)))))
  (is (= :interoceptive/activation-lock-mismatch
         (refusal #(activation/validate-participation
                    (assoc-in valid-record [:lock :parent-writable-by-service?] true)
                    (opts))))))

(deftest production-shaped-isolated-receipt-reader
  (let [dir (Files/createTempDirectory "activation-receipt-"
                                       (make-array FileAttribute 0))
        path (.resolve dir "participation.edn")]
    (spit (.toFile path) (str (pr-str valid-record) "\n"))
    (Files/setPosixFilePermissions
     path #{PosixFilePermission/OWNER_READ PosixFilePermission/OWNER_WRITE})
    (is (= :test (:authority-class
                  (activation/read-test-participation! (str path) (opts)))))
    (let [permissions (Files/getPosixFilePermissions path (make-array java.nio.file.LinkOption 0))]
      (Files/setPosixFilePermissions path
                                     (conj (set permissions)
                                           PosixFilePermission/GROUP_WRITE))
      (is (= :interoceptive/activation-receipt-untrusted
             (refusal #(activation/read-test-participation! (str path) (opts)))))
      (Files/setPosixFilePermissions path permissions))
    (binding [activation/*after-receipt-read-hook*
              #(spit (.toFile path) (str (pr-str valid-record) "\n "))]
      (is (= :interoceptive/activation-receipt-changed
             (refusal #(activation/read-test-participation! (str path) (opts))))))))

(deftest protected-boundary-generation-change-refuses
  (let [admission {:authority-class :production :receipt-sha256 "before"
                   :lock lock-record :lease-lock (:lease-lock valid-record)
                   :lease (:lease valid-record)}]
    (doseq [changed [(assoc admission :receipt-sha256 "after")
                     (assoc-in admission [:lease :generation] "generation-2")
                     (assoc-in admission [:lock :file-key] "replacement-inode")]]
      (with-redefs [activation/resolve-production-participation! (constantly changed)]
        (is (= :interoceptive/activation-boundary-changed
               (refusal #(activation/revalidate-production-participation! admission))))))))

(deftest lease-control-surface-gap-refuses
  (is (= :interoceptive/activation-lease-controls-unverified
         (refusal #(activation/validate-participation
                    (update-in valid-record [:lease :controls] pop) (opts)))))
  (let [authority activation/production-controller-authority
        record (assoc-in valid-record [:lease :controls]
                         (mapv (fn [[surface control]] (assoc control :surface surface))
                               authority))]
    (is (= :interoceptive/activation-controller-unavailable
           (refusal #(activation/validate-participation
                      record
                      (assoc (opts)
                             :controller-authority authority
                             :controller-probe
                             (fn [path]
                               (:artifact/sha256
                                (some (fn [[_ control]]
                                        (when (= path (:artifact/path control)) control))
                                      authority))))))))))

(deftest preprovisioned-lock-and-protected-interval-controls
  (let [dir (Files/createTempDirectory "activation-lease-"
                                       (make-array FileAttribute 0))
        missing (.resolve dir "missing.lock")
        lease (.resolve dir "deployment.lease")
        admission {:authority-class :production :receipt-sha256 "same"
                   :lock lock-record :lease-lock (assoc lock-record :path (str lease))
                   :lease {:generation "generation-1"}}]
    (is (= :interoceptive/lock-io-failure
           (refusal #(store-lock/with-existing-lock-at (str missing) identity))))
    (is (not (Files/exists missing (make-array java.nio.file.LinkOption 0))))
    (spit (.toFile lease) "")
    (with-redefs [activation/deployment-lease-path (str lease)
                  activation/resolve-production-participation! (constantly admission)]
      (is (= :interoceptive/lock-contention
             (activation/with-production-participation
              (fn [_]
                @(future
                   (refusal #(store-lock/with-existing-lock-at (str lease) identity)))))))
      (let [calls (atom 0)]
        (with-redefs [activation/resolve-production-participation!
                      (fn []
                        (if (= 1 (swap! calls inc)) admission
                            (throw (ex-info "expired"
                                            {:refusal :interoceptive/activation-stale}))))]
          (is (= :interoceptive/activation-stale
                 (refusal #(activation/with-production-participation
                            (fn [_] :capture-completed)))))))
      (is (= :interoceptive/missing-finding-join
             (refusal #(activation/with-production-participation
                        (fn [_]
                          (throw (ex-info "logical refusal"
                                          {:refusal :interoceptive/missing-finding-join})))))))
      (is (= :released-after-logical-refusal
             (store-lock/with-existing-lock-at
              (str lease) (constantly :released-after-logical-refusal)))))))
