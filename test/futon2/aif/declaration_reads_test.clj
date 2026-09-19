(ns futon2.aif.declaration-reads-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-sources :as sources]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.observation-checks :as checks]))

(use-fixtures :once hermetic/with-hermetic-stores)

(def declaration {:schema :wm/cascade-source-v1 :target "target-a" :context :test
                  :beta {:value 1 :status :declared} :facts [:unlocated] :want []
                  :locators {} :patterns {} :interpretation-receipts {} :candidates []})

(defn with-dir [f]
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                     "declaration-reads" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try (f dir)
         (finally (doseq [file (reverse (file-seq dir))] (io/delete-file file true))))))

(defn record-run [read-fn]
  (with-dir
    (fn [dir]
      (with-redefs-fn
        {#'runner/ensure-dispatch-seat! (constantly nil)
         #'runner/refuse-on-runner-source-drift! (constantly {:test-only true})
         #'runner/post-wm-status! (fn [& _] nil)
         #'runner/run-opportunity-core! (fn [_] (read-fn) {:outcome :incomplete :checkpoints {}})}
        (fn []
          (edn/read-string (slurp (:run-record (runner/run-opportunity!
                                               {:run-record-dir (.getPath dir)})))))))))

(defn snapshot-sha [text]
  (evidence/sha256 (.getBytes text java.nio.charset.StandardCharsets/UTF_8)))

(deftest snapshot-is-the-parsed-bytes-even-if-file-changes-during-observation
  (with-dir
    (fn [dir]
      (let [file (io/file dir "a.edn")
            before (pr-str declaration)
            after (pr-str (assoc declaration :target "target-b"))
            observe checks/observe]
        (spit file before)
        (let [record (with-redefs [checks/observe (fn [locators]
                                                   (spit file after)
                                                   (observe locators))]
                       (record-run #(sources/load-declared (str dir))))
              row (first (get-in record [:declaration-reads :occurrences]))
              next-record (record-run #(sources/load-declared (str dir)))
              next-row (first (get-in next-record [:declaration-reads :occurrences]))
              broken (assoc row :sha256 (snapshot-sha after))]
          (is (= "target-a" (:target row)))
          (is (= (snapshot-sha before) (:sha256 row)))
          (is (= "target-b" (:target next-row)))
          (is (= (snapshot-sha after) (:sha256 next-row)))
          (is (not= (:sha256 row) (:sha256 next-row)))
          (is (not= row broken))
          (is (not= (snapshot-sha before) (:sha256 broken)))
          (is (contains? (get-in row [:observations :refused]) :unlocated))
          (is (not (contains? (get-in row [:observations :results]) :unlocated)))
          (let [bad (-> row (assoc-in [:observations :results :unlocated] false)
                        (update-in [:observations :refused] dissoc :unlocated))]
            (is (not= row bad))
            (is (not (contains? (get-in bad [:observations :refused]) :unlocated)))))))))

(deftest repeated-path-and-duplicate-target-remain-ordered
  (with-dir
    (fn [dir]
      (let [file (io/file dir "a.edn")
            first-text (pr-str declaration)
            second-text (pr-str (assoc declaration :want [:another]))]
        (spit file first-text)
        (let [record (record-run #(do (sources/load-declared (str dir))
                                     (spit file second-text)
                                     (sources/load-declared (str dir))))
              rows (get-in record [:declaration-reads :occurrences])]
          (is (= [(str file) (str file)] (mapv :path rows)))
          (is (= [(snapshot-sha first-text) (snapshot-sha second-text)] (mapv :sha256 rows)))
          (is (= 2 (count (get-in record [:declaration-reads :target-collisions "target-a"]))))
          (let [bad (assoc-in record [:declaration-reads :occurrences] [(last rows)])]
            (is (not= rows (get-in bad [:declaration-reads :occurrences])))
            (is (not= 2 (count (get-in bad [:declaration-reads :occurrences])))))
          (println "DECLARATION-READS-TEST-RECORD" (pr-str record)))
        (spit (io/file dir "b.edn") first-text)
        (let [loaded (sources/load-declared (str dir))
              rows (:read-occurrences loaded)]
          (is (= ["a.edn" "b.edn"] (mapv #(-> % :path io/file .getName) rows)))
          (is (= {"target-a" :multiple-declarations} (:target-collisions loaded)))
          (is (= 2 (count rows))))))))

(deftest empty-scan-is-distinct-from-unobserved
  (with-dir
    (fn [dir]
      (let [record (record-run #(sources/load-declared (str dir)))
            absent (:declaration-reads record)
            unobserved (:declaration-reads (record-run (constantly nil)))
            removed (dissoc record :declaration-reads)]
        (is (= :absent (:status absent)))
        (is (= :none-supplied (:reason absent)))
        (is (= [] (:occurrences absent)))
        (is (= :not-observed (:status unobserved)))
        (is (not= absent unobserved))
        (is (not= record removed))
        (is (not= absent (:declaration-reads removed)))))))
