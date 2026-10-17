(ns futon2.aif.wm.reading-refusal-terminal-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]))

(deftest selected-reading-execution-boundary-is-a-typed-refusal
  (let [refusal {:kind :reading-policy-execution-semantics-owed}
        failure (#'runner/no-entry-failure-data
                 {:decision {:status :selected}}
                 refusal)]
    (testing "a selected reading policy is not reported as no-selection"
      (is (= {:outcome :refused
              :failure-kind :reading-policy-execution-semantics-owed}
             failure)))))

(deftest ordinary-empty-decisions-retain-their-terminal-class
  (is (= {:outcome :abstained}
         (#'runner/no-entry-failure-data
          {:decision {:status :abstained}}
          nil)))
  (is (= {:outcome :no-selection}
         (#'runner/no-entry-failure-data
          {:decision {:status :selected}}
          nil))))

(deftest cascade-resume-errors-retain-their-typed-evidence
  (let [resume-error {:status :error
                      :kind :open-cascade-review-answer-invalid
                      :phase :cascade-execution
                      :repairable true
                      :review-answer {:status :invalid}}
        failure (#'runner/no-entry-failure-data
                 {:decision {:status :selected}}
                 resume-error)]
    (testing "a failed same-click resume is incomplete, not a policy refusal"
      (is (= :incomplete (:outcome failure)))
      (is (= :open-cascade-review-answer-invalid (:failure-kind failure)))
      (is (= :cascade-execution (:held-phase failure)))
      (is (= resume-error (:failure-detail failure))))))
