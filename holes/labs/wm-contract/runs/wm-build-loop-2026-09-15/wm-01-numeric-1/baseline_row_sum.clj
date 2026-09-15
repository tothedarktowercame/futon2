; Exact function extracted from baseline 7ace7293b1ef934bc8fe4463ad100d96cd5f5880; tooling receipt only.
(ns baseline-row-sum)
(def float-row-tolerance 1e-12M)
(defn row-sum-admission
  "Typed numeric admission for a mass row. Exact rows (all ratios/integers)
   must sum to exactly 1 -> :exact. Rows carrying doubles are summed at
   their exact IEEE values -> :float-carried when within
   float-row-tolerance of 1. nil = inadmissible."
  [row]
  (let [vs (vals row)]
    (if (every? #(or (integer? %) (ratio? %)) vs)
      (when (== 1 (reduce + vs)) :exact)
      (let [sum (reduce (fn [^BigDecimal acc v] (.add acc (BigDecimal. (double v))))
                        BigDecimal/ZERO vs)
            gap (.abs (.subtract sum BigDecimal/ONE))]
        (when (<= (.compareTo gap ^BigDecimal float-row-tolerance) 0)
          :float-carried)))))
