(ns wm-check-run-facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [wm-check-run-facts :as sut]))

(deftest partial-run-facts-fields-come-from-lean
  (testing "obsolete generator fields are absent when Lean removes them"
    (let [reader "structure PartialRunFacts where\n  q1 : Evidence RunFacts\n  q3 : Evidence RunFacts\n\ndef assess := true\n"]
      (is (= [:q1 :q3] (sut/partial-run-fact-fields reader)))
      (is (not-any? #{:q2 :q8 :q10}
                    (sut/partial-run-fact-fields reader))))))

(deftest unknown-lean-field-fails-before-generation
  (testing "a new Lean field without a generator clause cannot be skipped"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"no generator clause"
         (sut/lean-source {"facts" {}} "exported" [:q11])))))
