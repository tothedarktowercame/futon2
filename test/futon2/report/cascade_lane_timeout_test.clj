(ns futon2.report.cascade-lane-timeout-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.report.cascade-lane :as lane]))

(deftest slow-constructor-is-a-timeout-not-a-construction-failure
  (let [process-result (#'lane/sh-timed ["sh" "-c" "sleep 1"] "." 10)
        failure
        (with-redefs-fn
          {#'lane/sh-timed (fn [& _] process-result)}
          #(try
             (lane/cascade-policy-for
              (str "timeout-control-" (java.util.UUID/randomUUID)) 20 0.15)
             nil
             (catch clojure.lang.ExceptionInfo e e)))]
    (is (= :timed-out (:status process-result)))
    (is (= 10 (:timeout-ms process-result)))
    (is (= :timed-out (:outcome (ex-data failure))))
    (is (= :construction-timeout (:failure-kind (ex-data failure))))
    (is (= :construction (:failure-stage (ex-data failure))))
    (is (= :timed-out (#'runner/outcome-from failure)))
    (is (= :construction-timeout (#'runner/failure-kind-from failure)))))
