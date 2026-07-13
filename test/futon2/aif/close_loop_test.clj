(ns futon2.aif.close-loop-test
  "The close-the-loop helper: ΔG reconciliation and resulting gate verdict
   (E-close-the-loop, pure core -- no python). Classical fold dG is off by
   default after E-live-loop-3 L4; bind the flag when testing the old fallback."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.close-loop :as cl]))

;; a cascade of real RULES patterns ⇒ the classical fold produces boxes ⇒ ΔG<0
(def ^:private foldable-shown
  ["devmap-coherence/prototype-structure-checklist"
   "math-strategy/constraint-tension-resolution"
   "math-informal/parametric-tension-dissolution"
   "devmap-coherence/next-steps-to-done"
   "devmap-coherence/prototype-alignment-role"])

(deftest step-score-delta-reconciliation
  (testing "rollout-g present ⇒ used directly (the move-set-grounded leg wins)"
    (let [ag (cl/act-gate-from-lane-entry {:mission "M-x" :cascade-score 0.3
                                           :policy-rollout-score -0.7 :shown foldable-shown})]
      (is (= -0.7 (:coverage-score-delta ag)))
      (is (= :policy-rollout (:coverage-score/source ag)))))
  (testing "rollout-g nil + a foldable cascade abstains by default after L4"
    (let [ag (cl/act-gate-from-lane-entry {:mission "M-y" :cascade-score 0.3
                                           :policy-rollout-score nil :shown foldable-shown})]
      (is (nil? (:coverage-score-delta ag)))
      (is (nil? (:coverage-score/source ag)))))
  (testing "binding the classical flag restores the pre-L4 fold fallback"
    (binding [cl/*classical-fold-score?* true]
      (let [ag (cl/act-gate-from-lane-entry {:mission "M-y" :cascade-score 0.3
                                             :policy-rollout-score nil :shown foldable-shown})]
        (is (number? (:coverage-score-delta ag)))
        (is (neg? (:coverage-score-delta ag)))
        (is (= :fold (:coverage-score/source ag))))))
  (testing "rollout-g nil + a non-foldable cascade ⇒ ΔG nil ⇒ gate abstains (honest)"
    (let [ag (cl/act-gate-from-lane-entry {:mission "M-z" :cascade-score 0.3
                                           :policy-rollout-score nil :shown ["foreign/a" "foreign/b"]})]
      (is (nil? (:coverage-score-delta ag)))
      (is (nil? (:coverage-score/source ag))))))

(deftest verdicts
  (testing "ΔF>0 and ΔG<0 passes when the pre-L4 fold fallback is explicitly bound"
    (binding [cl/*classical-fold-score?* true]
      (is (= :pass (cl/preview-verdict
                    (cl/act-gate-from-lane-entry {:mission "M-y" :cascade-score 0.3
                                                  :policy-rollout-score nil :shown foldable-shown}))))))
  (testing "ΔG nil ⇒ :abstain-missing-leg (no construction)"
    (is (= :abstain-missing-leg
           (cl/preview-verdict (cl/act-gate-from-lane-entry
                                {:mission "M-z" :cascade-score 0.3 :policy-rollout-score nil :shown []})))))
  (testing "ΔF≤0 (cascade not accepted) ⇒ :fail even with a descending ΔG"
    (is (= :fail (cl/preview-verdict {:cascade-score 0.0 :coverage-score-delta -0.5}))))
  (testing "the fold's policy-holes are carried for provenance"
    (let [ag (cl/act-gate-from-lane-entry {:mission "M-y" :cascade-score 0.3
                                           :policy-rollout-score nil
                                           :shown (conj foldable-shown "foreign/x")})]
      (is (some #(= "foreign/x" (:unfolded-pattern %)) (get-in ag [:fold :policy-holes]))))))
