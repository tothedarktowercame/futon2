(ns futon2.aif.refusal-legality-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.refusal-legality :as sut]))

(deftest nonempty-field-not-exhausted
  (is (false? (sut/loop-stop-legal?
               {:kind :work-exhausted :scoredField [:candidate]
                :operatorSignal false}))))

(deftest unexcluded-not-legal
  (is (false? (sut/click-legal?
               {:kind :already-satisfied :target :t
                :satisfactionMeasured true :ambiguityCited false
                :questionInBrief false :harmRuleMatched false
                :exclusionRecorded false}))))

(deftest question-needs-brief
  (is (false? (sut/click-legal?
               {:kind :legitimate-question :target :t
                :satisfactionMeasured false :ambiguityCited true
                :questionInBrief false :harmRuleMatched false
                :exclusionRecorded true}))))

(deftest unlisted-is-debugger-error
  (is (= :debugger-error (sut/disposition {:refusal :other}))))
