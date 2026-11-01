;; claude-7 independent probe of c0f35f79d (review only). Loads the focused
;; test namespace for its fixture helpers; no serving JVM, no runner suite.
(require 'futon2.aif.compiled-closure-classification-test)
(in-ns 'futon2.aif.compiled-closure-classification-test)
(def results (atom {}))
(with-fixture-repo
  (fn [fx]
    (with-redefs [observation/repo-root (:root fx)
                  cascade-sources/load-declared
                  (fn [& _] (fixture-sources (assoc fx :sha (:after-sha fx))))]
      (let [action (co-apply-action "T-fixture-closure" [[:p [:fixture-done]]])
            data {:artifact-binding (fresh-binding (:after-sha fx) (:before-sha fx))
                  :commit (:after-sha fx)}
            with-produces (fn [ps] (assoc-in action [:precedence :co-apply :patterns :p :produces] ps))
            c (fn [a] (runner/classify-grounded-outcome :grounded-change a data))]
        (swap! results assoc
               :compiled-local-qualified (c (with-produces #{["T-fixture-closure" :fixture-done]}))
               :compiled-mixed-local-and-foreign (c (with-produces #{["T-fixture-closure" :fixture-done]
                                                                    ["foreign-target" :fixture-done]}))
               :compiled-keyword-target-qualifier (c (with-produces #{[:T-fixture-closure :fixture-done]}))
               :compiled-list-qualifier (c (with-produces #{(list "foreign-target" :fixture-done)}))
               :legacy-vector-foreign-qualified
               (c {:target "T-fixture-closure"
                   :precedence [{:id :p :produces #{["foreign-target" :fixture-done]}}]})
               :legacy-vector-local-qualified
               (c {:target "T-fixture-closure"
                   :precedence [{:id :p :produces #{["T-fixture-closure" :fixture-done]}}]}))))))
(prn @results)
(spit "/home/joe/code/futon2/holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5/closure-followup-replacement-probe.edn" (pr-str @results))
(shutdown-agents)
