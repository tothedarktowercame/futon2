(ns checks.u84-trace-reason-census-test
  (:require [clojure.test :refer [deftest is]]
            [checks.u84-trace-reason-census :as census]))

(deftest persisted-location-and-record-denominator
  (let [reason {:kind :routing-rule :rule :scheduled-war-machine-tick :question "Review?"}
        hop {:node :TRACE :reason reason}]
    (is (= 0 (:reason-bearing-records (census/count-record {:trace/reason reason}))))
    (is (= 1 (:reason-bearing-records (census/count-record {:wm/route [hop hop]})))))
  (is (= 0 (:reason-bearing-records
            (census/count-record {:wm/route [{:node :TRACE :reason {:question ""}}]})))))

(deftest all-forms-and-opaque-tags
  (let [result (census/count-text "{:object #object[thing 1]}\n{:wm/route [{:node :TRACE :reason {:kind :machine-triage :rule :trace-route-reason-missing :question \"Which rule?\"}}]}")]
    (is (= 2 (:records result)))
    (is (= 1 (:reason-bearing-records result)))
    (is (= 1 (:fallback-reason-records result)))
    (is (= 0 (:producer-reason-records result))))
  (is (thrown? Exception (census/count-text "{} {")))
  (is (thrown? Exception (census/count-text "{} nil"))))
