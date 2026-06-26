(ns futon2.aif.fold-classical-test
  "Impl #1 (classical fold) satisfies the interface AND closes the act-gate
   (sim-only) — E-close-the-loop exit-2."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.fold :as fold]
            [futon2.aif.fold-classical :as fc]))

;; A cascade of real RULES patterns (the fold-engine's own boxed patterns) ⇒
;; folds into boxes; a foreign-only cascade ⇒ nothing folds.
(def ^:private foldable-cascade
  ["devmap-coherence/prototype-structure-checklist"   ; selector
   "math-strategy/constraint-tension-resolution"      ; match
   "math-informal/parametric-tension-dissolution"     ; fold-step
   "devmap-coherence/next-steps-to-done"              ; fixpoint
   "devmap-coherence/prototype-alignment-role"])      ; emit

(def ^:private foreign-cascade ["foreign-ns/a" "foreign-ns/b"])

;; The act-gate verdict (replicated from war_machine_pilot_backend, sim-only —
;; we do NOT touch the live pilot backend).
(defn- gate-verdict [delta-F delta-G]
  (cond (or (nil? delta-F) (nil? delta-G)) :abstain-missing-leg
        (and (pos? delta-F) (neg? delta-G)) :pass
        :else :fail))

(deftest classical-fold-satisfies-interface
  (testing "a foldable cascade → a valid fold output with the three ports"
    (let [out (fc/classical-fold foldable-cascade {})]
      (is (fold/valid-fold-output? out))
      (is (some? (:wiring out)))
      (is (seq (get-in out [:wiring :boxes])) "the construction has boxes")
      (is (number? (:delta-g out))))))

(deftest classical-fold-closes-the-gate
  (testing "a complete fold gives a descending ΔG ⇒ the gate can :pass"
    (let [out (fc/classical-fold foldable-cascade {})]
      (is (fold/closes? out) "ΔG present and negative")
      ;; with a positive ΔF (Bayesian-Occam accept), the gate flips abstain→pass
      (is (= :pass (gate-verdict 0.3 (fold/act-gate-leg out))))))
  (testing "a cascade that folds nothing → ΔG nil ⇒ the gate abstains (honest)"
    (let [out (fc/classical-fold foreign-cascade {})]
      (is (fold/valid-fold-output? out))
      (is (empty? (get-in out [:wiring :boxes])) "no fabricated construction")
      (is (nil? (:delta-g out)))
      (is (not (fold/closes? out)))
      (is (= :abstain-missing-leg (gate-verdict 0.3 (fold/act-gate-leg out)))
          "no construction ⇒ the gate still abstains (the loop only closes with a real fold)")))
  (testing "foreign patterns are surfaced as policy-holes, never dropped"
    (let [out (fc/classical-fold (into foldable-cascade foreign-cascade) {})]
      (is (= 2 (count (filter (comp #{"foreign-ns/a" "foreign-ns/b"} :unfolded-pattern)
                              (:policy-holes out))))))))
