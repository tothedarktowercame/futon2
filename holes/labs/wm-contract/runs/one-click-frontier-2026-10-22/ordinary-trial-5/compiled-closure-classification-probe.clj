(require '[futon2.aif.full-loop-runner :as runner]
         '[futon2.aif.meta-outcome-dataset :as dataset])
(let [action {:kind :cascade-candidate :target "M-probe"
              :precedence {:co-apply {:units [:p] :descent []
                                     :patterns {:p {:id :p :produces #{["M-probe" :wanted]}}}}}}
      actual (runner/classify-grounded-outcome :grounded-change action {})]
  (prn {:input :grounded-change :declared-products #{["M-probe" :wanted]}
        :effect-measurements [] :classified actual :outer-class (dataset/outcome-class actual)}))
(shutdown-agents)
