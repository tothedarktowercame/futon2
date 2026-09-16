(ns reattest
  (:require [checks.positive-proof-receipt :as receipt]
            [clojure.data :as data]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]))

(def run-dir "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-positive-binding-1")
(def target "holes/labs/wm-contract/predictive-outcome-kernel-positive-receipt.edn")
(defn record! [name value]
  (spit (str run-dir "/" name) (with-out-str (pp/pprint value))))

(let [old (edn/read-string (slurp (str run-dir "/predecessor.edn")))
      old-validation (receipt/validate old)
      refreshed (receipt/basis-record old)
      expanded (update-in refreshed [:source-basis 1 :declarations]
                          conj {:name "ProbabilityKernel"})
      successor (assoc (receipt/basis-record expanded)
                       :recorded-at (str (java.time.Instant/now)))]
  (record! "predecessor-validation.edn" old-validation)
  (assert (= [:positive-source-drift] (:failures old-validation)))
  (record! "old-recorded-derivation.edn" old)
  (record! "fresh-predecessor-derivation.edn" refreshed)
  (record! "successor.edn" successor)
  (record! "derivation-diff.edn" (data/diff old successor))
  (assert (= (:adapter old) (:adapter successor)))
  (assert (= (:dependency-closure old) (:dependency-closure successor)))
  (let [validation (receipt/validate successor)]
    (record! "successor-validation.edn" validation)
    (assert (:pass? validation)))
  ;; Isolated receipt, fixture and declaration sources; Lean imports remain
  ;; read-only probes in the declared toolchain cwd, with no build requested.
  (let [tmp (.toFile (java.nio.file.Files/createTempDirectory
                     "predictive-receipt-control-"
                     (make-array java.nio.file.attribute.FileAttribute 0)))
        root (.getAbsolutePath tmp)]
    (try
      (doseq [spec (conj (:source-basis successor) (:fixture successor))]
        (let [dest (io/file root (:repo spec) (:path spec))]
          (io/make-parents dest)
          (io/copy (io/file (receipt/source-path spec)) dest)))
      (let [copy (io/file root "receipt.edn")]
        (spit copy (pr-str successor))
        (with-redefs [receipt/repo-root root]
          (let [good (receipt/validate (edn/read-string (slurp copy)))
                mutant (assoc-in successor [:source-basis 0 :declarations 5 :sha256]
                                 (apply str (repeat 64 "0")))]
            (assert (= "predictive" (get-in mutant [:source-basis 0 :declarations 5 :name])))
            (spit copy (pr-str mutant))
            (let [bad (receipt/validate (edn/read-string (slurp copy)))]
              (record! "mutation-control.edn" {:baseline good :mutant bad :mutation mutant})
              (assert (:pass? good))
              (assert (= [:positive-source-drift] (:failures bad)))))))
      (finally
        (doseq [f (reverse (file-seq tmp))] (io/delete-file f)))))
  (io/copy (io/file (str run-dir "/successor.edn")) (io/file target))
  (println "Fresh basis-record, validation and isolated hash mutation: PASS"))
