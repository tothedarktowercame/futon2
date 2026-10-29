(ns futon2.aif.outer-refusal-census-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [futon2.aif.outer-refusal-index :as index]
            [futon2.aif.reading-policy-class-history-index :as history]))

(deftest legacy-record-names-share-the-class-history-census
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "outer-census-" (make-array java.nio.file.attribute.FileAttribute 0)))
        names ["tick-run-record-probe-revert-check.edn" "tick-run-record-smoke.edn"
               "tick-run-record-2026-10-02-.edn" "tick-run-record-close-retention-success.edn"
               "tick-run-record-cohort-discharge-run.edn" "tick-run-record-2026-10-29-real.edn"]]
    (try
      (doseq [n names] (spit (io/file dir n) (pr-str {:run/id n})))
      (doseq [suffix [".edn.requirements.edn" ".learning.edn" ".scan.md"]]
        (spit (io/file dir (str "tick-run-record-2026-10-29-real" suffix)) "not a record"))
      (let [output (io/file dir "index.ednl")]
        (index/backfill! dir output)
        (let [rows (mapv edn/read-string (line-seq (io/reader output)))]
          (is (= 6 (count rows)))
          (is (= (set (map #(.getCanonicalPath %) (history/terminal-files dir)))
                 (set (map :record-path rows))))
          (is (every? #(= :indexed (:status %)) rows))))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f))))))
