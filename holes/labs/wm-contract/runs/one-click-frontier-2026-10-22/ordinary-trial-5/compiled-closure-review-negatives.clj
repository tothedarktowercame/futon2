(require '[futon2.aif.full-loop-runner :as runner] '[futon2.aif.meta-outcome-dataset :as ds])
(doseq [[label precedence] [[:empty-products {:co-apply {:units [:p] :descent [] :patterns {:p {:id :p}}}}]
                            [:malformed {}]]]
 (let [v (runner/classify-grounded-outcome :grounded-change {:kind :cascade-candidate :target "M-probe" :precedence precedence} {})]
  (prn {:case label :outcome v :outer-class (ds/outcome-class v) :effect-evidence []})))
(shutdown-agents)
