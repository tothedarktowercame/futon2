(load-file "test/futon2/aif/compiled_closure_classification_test.clj")
(require '[futon2.aif.compiled-closure-classification-test :as f]
 '[futon2.aif.full-loop-runner :as r] '[futon2.aif.cascade-sources :as s]
 '[futon2.aif.observation-checks :as o])
(#'f/with-fixture-repo
 (fn [fx]
  (with-redefs [o/repo-root (:root fx)
                s/load-declared (fn [& _] (#'f/fixture-sources (assoc fx :sha (:after-sha fx))))]
   (let [action (#'f/co-apply-action "T-fixture-closure" [[:p [:fixture-done]]])
         foreign (assoc-in action [:precedence :co-apply :patterns :p :produces]
                           #{["foreign-target" :fixture-done]})
         data {:artifact-binding (#'f/fresh-binding (:after-sha fx) (:before-sha fx))
               :commit (:after-sha fx)}]
    (prn {:local-product (r/classify-grounded-outcome :grounded-change action data)
          :foreign-unmeasured-product (r/classify-grounded-outcome :grounded-change foreign data)})))))
(shutdown-agents)
