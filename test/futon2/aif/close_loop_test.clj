(ns futon2.aif.close-loop-test
  "The close-the-loop helper: ΔG reconciliation (rollout-g-for → fold fallback)
   and the resulting gate verdict (E-close-the-loop, pure core — no python)."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.close-loop :as cl]))

;; a cascade of real RULES patterns ⇒ the classical fold produces boxes ⇒ ΔG<0
(def ^:private foldable-shown
  ["devmap-coherence/prototype-structure-checklist"
   "math-strategy/constraint-tension-resolution"
   "math-informal/parametric-tension-dissolution"
   "devmap-coherence/next-steps-to-done"
   "devmap-coherence/prototype-alignment-role"])

(deftest delta-g-reconciliation
  (testing "rollout-g present ⇒ used directly (the move-set-grounded leg wins)"
    (let [ag (cl/act-gate-from-lane-entry {:mission "M-x" :F-free-energy 0.3
                                           :G-rollout -0.7 :shown foldable-shown})]
      (is (= -0.7 (:delta-G ag)))
      (is (= :rollout-g-for (:delta-G/source ag)))))
  (testing "rollout-g nil + a foldable cascade ⇒ the FOLD rescues ΔG (impl #1's reach)"
    (let [ag (cl/act-gate-from-lane-entry {:mission "M-y" :F-free-energy 0.3
                                           :G-rollout nil :shown foldable-shown})]
      (is (number? (:delta-G ag)))
      (is (neg? (:delta-G ag)))
      (is (= :fold (:delta-G/source ag)))))
  (testing "rollout-g nil + a non-foldable cascade ⇒ ΔG nil ⇒ gate abstains (honest)"
    (let [ag (cl/act-gate-from-lane-entry {:mission "M-z" :F-free-energy 0.3
                                           :G-rollout nil :shown ["foreign/a" "foreign/b"]})]
      (is (nil? (:delta-G ag)))
      (is (nil? (:delta-G/source ag))))))

(deftest verdicts
  (testing "ΔF>0 ∧ ΔG<0 ⇒ :pass (the loop closes — via fold fallback)"
    (is (= :pass (cl/preview-verdict
                  (cl/act-gate-from-lane-entry {:mission "M-y" :F-free-energy 0.3
                                                :G-rollout nil :shown foldable-shown})))))
  (testing "ΔG nil ⇒ :abstain-missing-leg (no construction)"
    (is (= :abstain-missing-leg
           (cl/preview-verdict (cl/act-gate-from-lane-entry
                                {:mission "M-z" :F-free-energy 0.3 :G-rollout nil :shown []})))))
  (testing "ΔF≤0 (cascade not accepted) ⇒ :fail even with a descending ΔG"
    (is (= :fail (cl/preview-verdict {:delta-F 0.0 :delta-G -0.5}))))
  (testing "the fold's policy-holes are carried for provenance"
    (let [ag (cl/act-gate-from-lane-entry {:mission "M-y" :F-free-energy 0.3
                                           :G-rollout nil
                                           :shown (conj foldable-shown "foreign/x")})]
      (is (some #(= "foreign/x" (:unfolded-pattern %)) (get-in ag [:fold :policy-holes]))))))
