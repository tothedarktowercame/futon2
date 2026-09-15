(require '[clojure.edn :as edn]
         '[futon2.aif.machine-model :as m])
(when (= "--before" (first *command-line-args*))
  (load-file "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-numeric-1/baseline_row_sum.clj"))
(let [root "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-numeric-1/"
      cases (edn/read-string (slurp (str root "cases.edn")))
      before? (= "--before" (first *command-line-args*))
      keyword-fn (if before? (ns-resolve 'baseline-row-sum 'row-sum-admission) m/row-sum-admission)
      detailed (when-not before? (ns-resolve 'futon2.aif.machine-model 'numeric-row-admission))]
  (prn (mapv (fn [[name row]]
    (merge {:case name :row row :keyword (keyword-fn row)}
                (if detailed
                  {:numeric (detailed row)}
                  {:old-coerced-total
                   (if (every? #(or (integer? %) (ratio? %)) (vals row))
                     (reduce + (vals row))
                     (reduce (fn [^BigDecimal acc v] (.add acc (BigDecimal. (double v))))
                             BigDecimal/ZERO (vals row)))})))
    (sort-by key cases))))
