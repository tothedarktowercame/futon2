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

(deftest retained-scored-field-preserves-absence
  (is (nil? (sut/retained-scored-field {}))))

(deftest retained-scored-field-keeps-unrefused-candidates
  (is (= ["M-b"]
         (sut/retained-scored-field
          {:outer/selection
           {:outer-field/admitted ["E-a" "M-b"]
            :reading-reselection {:refusals [{:target "E-a"}]}}}))))

(deftest retained-scored-field-can-be-exhausted-by-in-click-refusals
  (is (= []
         (sut/retained-scored-field
          {:outer/selection
           {:outer-field/admitted ["E-a"]
            :reading-reselection {:refusals [{:target "E-a"}]}}}))))

(deftest evidence-for-close-rejects-absent-scored-field
  (let [e (sut/evidence-for-close :abstained {:failure-kind :empty-field}
                                  nil false)]
    (is (= :scored-field-carrier-absent (get-in e [:evidence :reason])))
    (is (= :debugger-error (sut/disposition e)))))

(deftest evidence-for-close-checks-retained-field
  (is (= :debugger-error
         (-> (sut/evidence-for-close :abstained {:failure-kind :empty-field}
                                     ["M-work"] false)
             sut/disposition)))
  (is (= :loop-stop
         (-> (sut/evidence-for-close :abstained {:failure-kind :empty-field}
                                     [] false)
             sut/disposition))))

(deftest evidence-for-close-requires-retained-operator-signal
  (is (= :loop-stop
         (-> (sut/evidence-for-close :cancelled
                                     {:failure-kind :debugger/aborted
                                      :debugger/restart :abort}
                                     nil true)
             sut/disposition)))
  (is (= :debugger-error
         (-> (sut/evidence-for-close :cancelled
                                     {:failure-kind :operator-cancelled}
                                     nil false)
             sut/disposition))))

(deftest work-outcome-has-no-refusal-evidence
  (is (nil? (sut/evidence-for-close :grounded-change {} nil false))))
