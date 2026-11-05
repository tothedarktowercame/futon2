(ns futon2.aif.write-stamp-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.interoceptive-store-lock :as store-lock]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.write-stamp :as stamp])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "write-stamp-"
                                      (make-array FileAttribute 0))))

(defn- delete-tree! [dir]
  (doseq [file (reverse (file-seq dir))]
    (io/delete-file file true)))

(defn- json-lines [file]
  (with-open [reader (io/reader file)]
    (mapv #(json/parse-string % true) (line-seq reader))))

(deftest record-appends-one-json-line-per-call
  (let [root (temp-dir)]
    (try
      (let [written (io/file root "written.edn")
            request {:root root :click-id "wm-click-stamp-1" :run-id "run-1"
                     :writer "fixture/writer" :path written}]
        (spit written "value")
        (is (:stamped? (stamp/record! request)))
        (is (:stamped? (stamp/record! (assoc request :path (io/file root "second.edn")))))
        (let [rows (json-lines (io/file root "wm-click-stamp-1.jsonl"))]
          (is (= 2 (count rows)))
          (is (= ["fixture/writer" "fixture/writer"] (mapv :writer rows)))
          (is (every? #(.isAbsolute (io/file (:path %))) rows))))
      (finally (delete-tree! root)))))

(deftest record-without-click-id-writes-nothing
  (let [root (temp-dir)]
    (try
      (is (= {:stamped? false :reason :no-click-id}
             (stamp/record! {:root root :click-id " " :run-id "run-1"
                             :writer "fixture/writer" :path (io/file root "x")})))
      (is (empty? (seq (.listFiles root))))
      (finally (delete-tree! root)))))

(defn- repair-finding [root suffix]
  (let [root (str root)
        occurrence (repair/occurrence-identity
                    {:origin (str "fixture::run-" suffix)
                     :event-id (str "event-" suffix)
                     :failure-kind :fixture-failure})]
    (repair/record-system-failure!
     root
     {:attempt-id (str "attempt-" suffix)
      :repair-class :incomplete-recoverable
      :failure-stage :fixture
      :outcome :incomplete
      :failure-kind :fixture-failure
      :error "fixture failure"
      :opened-at "2026-11-05T00:00:00Z"
      :occurrence occurrence
      :observation {:observation/id (str "observation-" suffix)
                    :observed-at "2026-11-05T00:00:00Z"
                    :source "fixture"}
      :discharge-contract {:requires [] :artifact-shape :code-commit}})))

(deftest repair-publication-stamps-only-created-files-in-click-context
  (let [base (temp-dir)
        root (io/file base "wm-repair-obligations")
        foreign (io/file root "foreign-seat-output.edn")
        click-id "wm-click-repair-context"]
    (try
      (io/make-parents foreign)
      (spit foreign "{:foreign true}")
      (let [finding
            (binding [stamp/*click-context* {:click-id click-id :run-id "repair-run"}]
              (let [finding (repair-finding root "bound")]
                (repair/supersede! (str root) finding
                                   {:repair/id "successor-fixture"}
                                   :fixture-successor)
                finding))
            stamp-file (io/file base "wm-write-stamps" (str click-id ".jsonl"))
            before (json-lines stamp-file)
            files (->> (file-seq root)
                       (filter #(.isFile ^java.io.File %))
                       (map #(.getCanonicalPath ^java.io.File %))
                       set)
            stamped (set (map :path before))]
        (is (= (disj files (.getCanonicalPath foreign)) stamped))
        (is (= (count stamped) (count before)) "exactly one stamp per created file")
        (binding [stamp/*click-context* {:click-id click-id :run-id "repair-run"}]
          (repair-finding root "bound"))
        (is (= before (json-lines stamp-file)) "identical replay adds no stamp")
        (is (= (:repair/id finding)
               (:repair/id (first (repair/obligation-history root "attempt-bound"))))))
      (finally (delete-tree! base)))))

(deftest repair-publication-under-a-relocated-store-lock-stamps-only-real-files
  ;; The canonical data store takes its lock at /run/futon2/..., not beside
  ;; the store, so the lock path write-new! derives never exists there.
  (let [base (temp-dir)
        root (io/file base "wm-repair-obligations")
        elsewhere (temp-dir)
        click-id "wm-click-relocated-lock"]
    (try
      (.mkdirs root)
      (binding [store-lock/*lock-path* (str (io/file elsewhere "store.lock"))
                stamp/*click-context* {:click-id click-id :run-id "lock-run"}]
        (repair-finding root "relocated"))
      (let [rows (json-lines (io/file base "wm-write-stamps" (str click-id ".jsonl")))]
        (is (seq rows))
        (is (every? #(.isFile (io/file (:path %))) rows)
            "no stamp names a lock file that was never created"))
      (finally (delete-tree! base) (delete-tree! elsewhere)))))

(deftest repair-publication-without-conveyed-click-context-is-unstamped
  (doseq [mode [:unbound :raw-thread]]
    (let [base (temp-dir)
          root (io/file base "wm-repair-obligations")]
      (try
        (.mkdirs root)
        (if (= mode :unbound)
          (repair-finding root "unbound")
          (let [failure (atom nil)
                thread (Thread. #(try (repair-finding root "thread")
                                      (catch Throwable e (reset! failure e))))]
            (binding [stamp/*click-context* {:click-id "wm-click-not-conveyed"
                                              :run-id "thread-run"}]
              (.start thread)
              (.join thread))
            (when-let [e @failure] (throw e))))
        (is (not (.exists (io/file base "wm-write-stamps"))) (name mode))
        (finally (delete-tree! base))))))

(deftest persist-run-record-stamps-record-and-indexes
  (let [root (temp-dir)
        records (io/file root "wm-runs")]
    (try
      (let [written (runner/persist-run-record!
                     {:run-record-dir (.getPath records)
                      :click-id "wm-click-persist-1"
                      :observation-labels-path (str (io/file root "labels.edn"))
                      :scan-render-fn (fn [& _] nil)
                      :nano-time-fn (constantly 0)}
                     "run-persist-1" "2026-11-05T00:00:00Z"
                     {:outcome :incomplete
                      :wm/route [{:node :FULL_LOOP_CLOSE}]
                      :data {:failure-kind :fixture-failure
                             :failure-stage :close
                             :error "fixture"}})
            stamp-file (io/file root "wm-write-stamps/wm-click-persist-1.jsonl")
            rows (json-lines stamp-file)
            paths (set (map :path rows))]
        (testing "the terminal record and all appended indexes are stamped"
          (is (= 4 (count rows)))
          (is (contains? paths (.getCanonicalPath (io/file (:run-record written)))))
          (is (= #{"full-loop-runner/persist-run-record!"} (set (map :writer rows))))
          (is (= #{"run-persist-1"} (set (map :run-id rows))))))
      (finally (delete-tree! root)))))
