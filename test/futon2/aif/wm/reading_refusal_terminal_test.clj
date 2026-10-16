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
