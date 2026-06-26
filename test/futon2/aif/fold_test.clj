(ns futon2.aif.fold-test
  "The fold interface contract (E-close-the-loop exit-1)."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.fold :as fold]))

(deftest valid-fold-output
  (testing "a well-formed fold output satisfies the contract"
    (is (fold/valid-fold-output? {:wiring {:id :w} :delta-g -0.4 :policy-holes []}))
    (is (fold/valid-fold-output? {:wiring {:id :w} :delta-g nil :policy-holes [{:unfolded :p}]})
        "nil ΔG is valid — it means the gate abstains, not that the output is malformed"))
  (testing "malformed outputs are rejected"
    (is (not (fold/valid-fold-output? {:delta-g -0.4 :policy-holes []})) "missing :wiring")
    (is (not (fold/valid-fold-output? {:wiring {} :delta-g "x" :policy-holes []})) ":delta-g not a number/nil")
    (is (not (fold/valid-fold-output? {:wiring {} :delta-g -0.4 :policy-holes {}})) ":policy-holes not sequential")))

(deftest act-gate-leg-and-closes
  (testing "act-gate-leg extracts ΔG; nil ⇒ the gate abstains"
    (is (= -0.4 (fold/act-gate-leg {:wiring {} :delta-g -0.4 :policy-holes []})))
    (is (nil? (fold/act-gate-leg {:wiring {} :delta-g nil :policy-holes []}))))
  (testing "closes? iff ΔG present and descending (negative)"
    (is (true?  (fold/closes? {:wiring {} :delta-g -0.4 :policy-holes []})))
    (is (false? (fold/closes? {:wiring {} :delta-g 0.4 :policy-holes []})) "non-descending ⇒ not closing")
    (is (false? (fold/closes? {:wiring {} :delta-g nil :policy-holes []})) "nil ⇒ abstain, not closing")))
