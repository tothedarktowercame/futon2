(ns futon2.aif.fold-test
  "The fold interface contract (E-close-the-loop exit-1)."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.fold :as fold]))

(deftest valid-fold-output
  (testing "a well-formed fold output satisfies the contract"
    (is (fold/valid-fold-output? {:wiring {:id :w} :coverage-score-delta -0.4 :policy-holes []}))
    (is (fold/valid-fold-output? {:wiring {:id :w} :coverage-score-delta nil :policy-holes [{:unfolded :p}]})
        "nil ΔG is valid — it means the gate abstains, not that the output is malformed"))
  (testing "malformed outputs are rejected"
    (is (not (fold/valid-fold-output? {:coverage-score-delta -0.4 :policy-holes []})) "missing :wiring")
    (is (not (fold/valid-fold-output? {:wiring {} :coverage-score-delta "x" :policy-holes []})) ":coverage-score-delta not a number/nil")
    (is (not (fold/valid-fold-output? {:wiring {} :coverage-score-delta -0.4 :policy-holes {}})) ":policy-holes not sequential")))

(deftest coverage-score-leg-and-closes
  (testing "coverage-score-leg extracts ΔG; nil ⇒ the gate abstains"
    (is (= -0.4 (fold/coverage-score-leg {:wiring {} :coverage-score-delta -0.4 :policy-holes []})))
    (is (nil? (fold/coverage-score-leg {:wiring {} :coverage-score-delta nil :policy-holes []}))))
  (testing "closes? iff ΔG present and descending (negative)"
    (is (true?  (fold/closes? {:wiring {} :coverage-score-delta -0.4 :policy-holes []})))
    (is (false? (fold/closes? {:wiring {} :coverage-score-delta 0.4 :policy-holes []})) "non-descending ⇒ not closing")
    (is (false? (fold/closes? {:wiring {} :coverage-score-delta nil :policy-holes []})) "nil ⇒ abstain, not closing")))
