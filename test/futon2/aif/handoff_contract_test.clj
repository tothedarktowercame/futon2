(ns futon2.aif.handoff-contract-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.handoff-contract :as sut]))

(def example
  {:id :wm/example :version 1
   :response {:required [:answer] :enums {:answer [:yes :no]}}
   :validator {:id :wm/example-validator :version 1}})

(deftest one-value-renders-and-identifies-the-consumer-contract
  (let [contract (sut/contract example)
        prompt (sut/prompt-block contract)]
    (is (= sut/schema (:schema contract)))
    (is (.contains prompt ":required [:answer]"))
    (is (.contains prompt ":answer [:yes :no]"))
    (is (= :validated (:state (sut/outcome contract "request-1" :validated {}))))))

(deftest malformed-contracts-and-collapsed-states-refuse
  (is (thrown? clojure.lang.ExceptionInfo
               (sut/contract (dissoc example :validator))))
  (is (thrown? clojure.lang.ExceptionInfo
               (sut/outcome example "request-1" :done {}))))
