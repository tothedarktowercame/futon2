(ns extra-cases
  (:require [clojure.edn :as edn]
            [futon2.aif.machine-model :as m]))
(load-file "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-numeric-1/baseline_row_sum.clj")
(let [f (float 0.1)
      cases [["Float(0.1), Double(1 - Float(0.1))" {:a f :b (- 1.0 (double f))}]
             ["negative unit-sum: {:a -1 :b 2}" {:a -1 :b 2}]
             ["two Long/MAX_VALUE masses" {:a Long/MAX_VALUE :b Long/MAX_VALUE}]
             ["AtomicInteger(1)" {:a (java.util.concurrent.atomic.AtomicInteger. 1)}]
             ["Double/NaN" {:a Double/NaN}]
             ["Double/POSITIVE_INFINITY" {:a Double/POSITIVE_INFINITY}]
             ["Float/NEGATIVE_INFINITY" {:a Float/NEGATIVE_INFINITY}]
             ["nil mass" {:a nil}]
             ["string mass" {:a "1"}]]]
  (prn (mapv (fn [[description row]]
    (let [old (try {:keyword ((ns-resolve 'baseline-row-sum 'row-sum-admission) row)}
                   (catch Exception e {:exception (.getName (class e))}))
          result (m/numeric-row-admission row)]
      {:case description :before old :after result
            :after-keyword (m/row-sum-admission row)
            :evidence-edn-roundtrip? (= result (edn/read-string (pr-str result)))
            :value-preservation (when (contains? result :values) (= row (:values result))) }))
    cases)))
