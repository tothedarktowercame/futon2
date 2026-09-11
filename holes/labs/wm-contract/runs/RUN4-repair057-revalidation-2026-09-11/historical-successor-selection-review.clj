(require '[futon2.aif.full-loop-runner :as r]
         '[futon2.aif.full-loop-runner-test :as t]
         '[futon2.aif.hermetic-repair-fixture :as h])
(h/with-hermetic-stores
 (fn []
   (binding [r/*wm-status-reporting?* false]
     (let [historical (atom 0) dispatches (atom 0)
           obligation {:repair/id "repair-057" :repair/status :awaiting-validation
                       :repair/class :machine-failure
                       :repair/verification {:verification-id "verified-057"}}
           result (r/run-opportunity!
                   (merge (t/isolated-runner-opts)
                          {:repair-open-fn (constantly [obligation])
                           :historical-verification-candidate-fn
                           (fn [_] (swap! historical inc) (throw (ex-info "must not reverify" {})))
                           :roster-fn (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}})
                           :dispatch-fn (fn [& _] (swap! dispatches inc))}))]
       (assert (zero? @historical))
       (assert (zero? @dispatches))
       (assert (= :open-mission (get-in result [:checkpoints :selection :judgment :selected-action :type])))
       (prn {:historical-candidate-calls @historical :dispatches @dispatches
             :selected-action (get-in result [:checkpoints :selection :judgment :selected-action])
             :outcome (:outcome result)})))))
