(ns futon2.aif.direct-task-outer-selection-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.outer-select :as outer-select]))

(deftest direct-task-pin-reaches-the-reading-family-carrier
  (let [received (atom nil)
        result
        (with-redefs-fn
          {#'runtime/run-ordinary-opportunity-with-task!
           (fn [opts]
             (reset! received opts)
             {:ran true})
           #'runtime/retain-selection!
           (fn [_ retained _]
             {:retained retained})}
          (fn []
            (#'runtime/run-ordinary-opportunity!
             {:task {:id "T-ladder-01-loop-turns-over"}})))]
    (is (:ran result))
    (is (= {:task "T-ladder-01-loop-turns-over"
            :status :not-exercised
            :reason :direct-task-entry}
           (select-keys (:outer/selection @received)
                        [:task :status :reason])))))

(deftest direct-task-pin-is-not-an-outer-enactment-pin
  (let [judgement {:decision {:selection-law {:applied :meta-algorithm-selection}
                              :action {:type :run-algorithm :target "algorithm-1"}
                              :controller-score 1}}
        direct-task {:task "T-ladder-01-loop-turns-over"
                     :status :not-exercised
                     :reason :direct-task-entry}
        entry (with-redefs [outer-select/validate-enactment-pin!
                            (fn [_]
                              (throw (ex-info "must not validate" {})))]
                (#'runner/selected-entry judgement direct-task))]
    (is (= "algorithm-1" (get-in entry [:action :target])))
    (is (not (contains? entry :outer-enactment)))
    (is (not (:outer-enactment-required? entry)))))
