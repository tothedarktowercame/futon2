(ns futon2.aif.wm.reading-refusal-terminal-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-cohort :as cohort]))

(deftest selected-reading-execution-boundary-is-a-typed-refusal
  (let [refusal {:kind :reading-policy-execution-semantics-owed}
        failure (#'runner/no-entry-failure-data
                 {:decision {:status :selected}}
                 refusal)]
    (testing "a selected reading policy is not reported as no-selection"
      (is (= :abstained (:outcome failure)))
      (is (= :reading-policy-execution-semantics-owed (:failure-kind failure)))))

  ;; occ-2613ce61: an :identity-forged cascade refusal produced
  ;; {:outcome :refused ...}, which cohort/outcome-kinds rejects, so the
  ;; close checkpoint threw invalid-close-outcome and the click reported
  ;; build-failed instead of a typed abstention.
  (let [forged {:outcome :refused :kind :identity-forged}
        failure (#'runner/no-entry-failure-data
                 {:decision {:status :selected}}
                 forged)]
    (is (contains? cohort/outcome-kinds (:outcome failure))
        "the typed refusal must close on a cohort-valid outcome kind")
    (is (= :identity-forged (:failure-kind failure))
        "the refusal's typed kind must survive into the failure data")))

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
