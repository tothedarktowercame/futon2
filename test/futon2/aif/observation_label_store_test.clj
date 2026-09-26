(ns futon2.aif.observation-label-store-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.observation-labels :as labels]
            [futon2.aif.observation-label-store :as store])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute PosixFilePermissions]))

(def pin "3fabf0260c056c5bd09755a288cfe179b349e83b")
(def locator {:repo "futon2" :sha pin :path "src/futon2/aif/observation_admission.clj"})
(def ^:dynamic *dir* nil)
(defn- path [] (io/file *dir* "labels.edn"))
(defn- c3 [] (checks/check-path-exists locator))
(defn- c4 [] (checks/check-decl-in-file (assoc locator :decl "(defn label-record")))
(defn- failure [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))
(defn- file-bytes [p] (vec (Files/readAllBytes (.toPath (io/file p)))))

(use-fixtures :each
  (fn [f]
    (let [dir (.toFile (Files/createTempDirectory "label-owner-" (make-array FileAttribute 0)))]
      (try (binding [*dir* dir] (f))
           (finally (doseq [file (reverse (file-seq dir))] (io/delete-file file true)))))))

(deftest initialized-store-survives-two-tick-boundaries
  (let [p (path) ids (labels/loaded-identities) checks [(c3) (c4)]]
    (store/init! p)
    (let [first-receipt (store/record! p checks ids {})
          first-snapshot (store/snapshot p)
          second-receipt (store/record! p checks ids {})
          second-snapshot (store/snapshot p)]
      (is (= 2 (:written first-receipt) (:labels-total first-receipt)))
      (is (= (:sha256 first-snapshot) (:snapshot-sha256 first-receipt)))
      (is (= 0 (:written second-receipt) (:refused second-receipt)))
      (is (= 2 (:skipped second-receipt) (:seen-total second-receipt)))
      (is (= (:sha256 second-snapshot) (:snapshot-sha256 second-receipt)))
      (is (= ids (:identities second-receipt)))
      (is (every? #(and (= :present (:admitted %)) (true? (:recorded %)))
                  (vals (get-in second-snapshot [:envelope :labels]))))
      (doseq [[k seen] (get-in second-snapshot [:envelope :seen])]
        (is (= 2 (:times seen)))
        (is (= :skipped (:last-outcome seen)))
        (is (nil? (:last-refusal seen)))
        (is (= (get-in first-snapshot [:envelope :seen k :first-seen]) (:first-seen seen)))))))

(deftest refused-subject-is-seen-but-never-labelled
  (let [p (path) ids (labels/loaded-identities) recompute labels/c4-recompute]
    (store/init! p)
    (with-redefs [labels/c4-recompute
                  (fn [s] (assoc (recompute s) :observer (:check-mechanism s)))]
      (let [r (store/record! p [(c4)] ids {})
            e (:envelope (store/snapshot p))]
        (is (= 1 (:refused r) (:seen-total r)))
        (is (= 0 (:labels-total r)))
        (is (= :self-truthed (get-in r [:refusals 0 :reason])))
        (is (= :review-not-concur (get-in r [:refusals 0 :kind])))
        (is (empty? (:labels e)))
        (is (= {:times 1 :last-outcome :refused :last-refusal :review-not-concur}
               (select-keys (first (vals (:seen e))) [:times :last-outcome :last-refusal])))))))

(deftest changed-check-identity-starts-a-second-population
  (let [p (path) ids (labels/loaded-identities) check (c3)]
    (store/init! p)
    (store/record! p [check] ids {})
    (let [r (store/record! p [check] (assoc ids :mechanism-sha (str "sha256:" (apply str (repeat 64 "0")))) {})]
      (is (= 1 (:written r)))
      (is (= 2 (:labels-total r) (:seen-total r)))
      (is (= 2 (count (get-in (store/snapshot p) [:envelope :labels])))))))

(deftest absent-and-invalid-stores-are-never-initialized-by-record
  (let [p (path) ids (labels/loaded-identities) check (c3)]
    (is (= :store-uninitialized (:kind (failure #(store/record! p [check] ids {})))))
    (is (not (.exists p)))
    (doseq [text ["{} {}" "{}" "{:schema :wm/observation-labels-v1 :labels {} :seen {}}"]]
      (spit p text)
      (let [before (file-bytes p)]
        (is (= :invalid-label-store (:kind (failure #(store/record! p [check] ids {})))))
        (is (= :invalid-label-store (:kind (failure #(store/init! p)))))
        (is (= before (file-bytes p)))))
    (is (= :path-required (:kind (failure #(store/init! nil)))))
    (is (= :path-required (:kind (failure #(store/snapshot nil)))))))

(deftest concurrent-records-preserve-the-union
  (let [ids (labels/loaded-identities) a (c3) b (c4)]
    (dotimes [i 3]
      (let [p (io/file *dir* (str "concurrent-" i ".edn")) start (promise)]
        (store/init! p)
        (let [fa (future @start (store/record! p [a] ids {}))
              fb (future @start (store/record! p [b] ids {}))]
          (deliver start true)
          (let [receipts [@fa @fb]]
            (is (= [1 2] (sort (map :labels-total receipts)))))
          (let [e (:envelope (store/snapshot p))]
            (is (= 2 (count (:labels e)) (count (:seen e)))))
          (is (= 2 (+ (:written @fa) (:written @fb)))))))))

(deftest failed-publication-preserves-previous-bytes
  ;; Actual POSIX failure: the existing data and lock files remain writable,
  ;; but creating a publication temp file in the parent is denied. No IO stub.
  (let [p (path) ids (labels/loaded-identities) check (c3)
        parent (.toPath *dir*) permissions (Files/getPosixFilePermissions parent (make-array java.nio.file.LinkOption 0))]
    (store/init! p)
    (store/record! p [check] ids {})
    (let [before (file-bytes p)]
      (try
        (Files/setPosixFilePermissions parent (PosixFilePermissions/fromString "r-x------"))
        (let [bad (failure #(store/record! p [check] ids {}))]
          (is (= :publish-failed (:kind bad)))
          (is (string? (:cause bad)))
          (is (= before (file-bytes p))))
        (finally (Files/setPosixFilePermissions parent permissions)))
      (is (= 1 (:times (first (vals (get-in (store/snapshot p) [:envelope :seen])))))))))

(deftest identities-are-the-two-captured-source-digests
  (let [ids (labels/loaded-identities)]
    (doseq [[k n] [[:mechanism-sha 'futon2.aif.observation-checks]
                   [:code-sha 'futon2.aif.observation-labels]]]
      (is (= (str "sha256:" (:sha256 (get @identity/registry n))) (get ids k)))
      (is (re-matches #"sha256:[0-9a-f]{64}" (get ids k)))
      (let [saved (get @identity/registry n)]
        (try
          (swap! identity/registry dissoc n)
          (is (= {:status :missing :kind :code-identity-unregistered :namespace n}
                 (labels/loaded-identities)))
          (swap! identity/registry assoc n (assoc saved :status :unavailable))
          (is (= :code-identity-unregistered (:kind (labels/loaded-identities))))
          (finally (swap! identity/registry assoc n saved)))))
    (println "Loaded observation identities:" (pr-str ids))))
