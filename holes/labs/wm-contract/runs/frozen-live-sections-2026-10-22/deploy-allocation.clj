(do
 (alter-var-root #'futon3c.wm.ordinary-click-budget/allocated
   (fn [old] (cond (= old 148) 158 (= old 158) 158
                    :else (throw (ex-info "Allocation changed concurrently; reconcile" {:allocated old})))))
 (futon3c.wm.ordinary-click-budget/availability))
