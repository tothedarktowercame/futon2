(binding [*out* (java.io.StringWriter.)]
 (load-file "holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5/declared-effect-replay.clj"))
(let [counterfactual (assoc-in decision [:action :precedence]
                        (mapv #(assoc % :produces #{[target :pattern-done (:id %)]}) c2-precedence))
      pred (token-outcome/freeze-prediction counterfactual)
      cmp (token-outcome/compare-outcomes pred measurements after-sha)
      acc (swo/receipt {:selected-action {:target target :want (:want decl)
                         :observation-locators {(q :restoration-accepted) (locator-for :restoration-accepted)}}
                        :token-comparison cmp})]
 (assert (= [0] (mapv :predicted (:wanted pred))))
 (assert (= [:not-predicted-observed] (mapv :verdict (:tokens cmp))))
 (assert (= :verified (:status acc)))
 (assert (= [(q :restoration-accepted)] (get-in acc [:by-class :reached])))
 (prn {:prediction (:wanted pred) :comparison (mapv :verdict (:tokens cmp))
       :accounting (:status acc) :reached (get-in acc [:by-class :reached])
       :limit "Counterfactual pattern-done model over real pinned historical observations; demonstrates accounting semantics, not live selection"}))
(shutdown-agents)
