(ns futon2.aif.task-selection-absence-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.cascade-decision :as decision]
            [futon2.test-support.reading-route :as route]))

(use-fixtures :each route/with-empty-learning-ledger)

(deftest no-task-does-not-select-or-enter-the-runner
  (let [calls (atom 0)
        touched (fn [& _] (swap! calls inc) {:status :ready})]
    ;; Keep the mutation check hermetic even if the no-task guard is deleted.
    (with-redefs-fn {#'runtime/production-defaults touched
                     #'runner/run-opportunity! touched
                     #'runtime/resolve-medic-authority touched
                     #'runtime/run-opportunity-with-authority! touched}
      (fn []
        (is (= {:absent :no-task-selection-policy}
               (runtime/run-opportunity! {})))
        (is (= {:absent :no-task-selection-policy}
               (runtime/run-opportunity!
                {:outer-task-selection-fn touched})))
        (is (zero? @calls))))))

(deftest removed-outer-handoff-does-not-select
  (is (= {:absent :no-task-selection-policy}
         (decision/cascade-decision
          {} {:selected-outer-cascade-policy {:status :sealed}
              :outer-policy-selection-claimed? true}))))

(deftest supplied-task-still-enters-inner-family-selection
  (let [result (route/decide (route/fixture))]
    (is (= 1 (:selector-calls result)))
    (is (= route/target (get-in result [:decision :action :target])))))
