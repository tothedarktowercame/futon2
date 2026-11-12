(ns futon2.aif.direct-task-outer-selection-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runtime :as runtime]))

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
