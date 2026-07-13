(ns futon2.aif.gamma-feed-test
  "γ-feed rewire (operator-armed 2026-07-05, edge invoke-1783280248832-512-8130dc7b):
   escrow-sourced gate ΔG feeds γ's expected leg. Four claims, each tested:
   (1) source-consistency — escrow-sourced decisions feed the escrow's
       coverage-ΔG even when the classical fold computed a (distrusted) number;
   (2) the pin holds — rollout-sourced decisions still feed the classical
       coverage leg, never rollout-G;
   (3) feed-through — realized-outcome-of carries the fed value (the decision's
       :fold IS the source fold, so the (:coverage-score-delta fold) preference agrees);
   (4) sim-consistency — the first fed sample folds through the SAME
       fold-realized-outcome the 3b simulation used, with the sim's sign
       convention (under-delivery ⇒ perf<0 ⇒ γ hedges below 1 after burn-in;
       over-delivery ⇒ γ commits above 1) reproduced on live-shaped numbers."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.enact :as enact]
            [futon2.aif.fold-realized :as fr]
            [futon2.aif.selection-gain :as pp]))

;; The live 2026-07-05 shape: gate verdicts on the escrow deposit's -0.7;
;; classical still computes its distrusted -0.077 (the 9x underestimate the
;; classical-off ruling named); executor reproduces 2 boxes / 10 holes.
(def escrow-gate
  {:cascade-score 1.463 :coverage-score-delta -0.7 :coverage-score/source :fold-escrow
   :fold {:wiring {} :coverage-score-delta -0.077 :policy-holes []}
   :fold-escrow {:wiring {} :coverage-score-delta -0.7 :policy-holes []}})

(def enacted-2-of-12
  ;; coverage 2/(2+10) = 1/6 ⇒ ΔG = -1/6 — the live log's realizedG
  {:boxes (vec (repeat 2 {:id :b})) :policy-holes (vec (repeat 10 {:free "h"}))})

(deftest source-consistent-gamma-fold
  (testing "escrow-sourced ⇒ escrow fold feeds γ, classical's number ignored"
    (is (= -0.7 (:coverage-score-delta (enact/selection-gain-fold-of escrow-gate)))))
  (testing "classical-sourced ⇒ classical fold, unchanged"
    (is (= -0.077 (:coverage-score-delta (enact/selection-gain-fold-of
                             (assoc escrow-gate :coverage-score/source :fold))))))
  (testing "rollout-sourced ⇒ classical coverage leg, NEVER rollout-G (the pin)"
    (let [ag (assoc escrow-gate :coverage-score/source :policy-rollout :coverage-score-delta -3.2)]
      (is (= -0.077 (:coverage-score-delta (enact/selection-gain-fold-of ag))))))
  (testing "flag bound off ⇒ pre-rewire behaviour (classical only), revertible"
    (binding [enact/*selection-gain-escrow-feed?* false]
      (is (= -0.077 (:coverage-score-delta (enact/selection-gain-fold-of escrow-gate)))))))

(deftest fed-sample-reaches-realized-outcome
  (let [gfold (enact/selection-gain-fold-of escrow-gate)
        decision {:policy "M-bayesian-structure-learning"
                  :expected-score (:coverage-score-delta gfold)
                  :fold gfold}
        ro (fr/realized-outcome-of decision enacted-2-of-12 1783280000000)]
    (testing "expected leg = the escrow's coverage-ΔG, not classical's"
      (is (= -0.7 (:expected-score ro))))
    (testing "realized leg = executor coverage-ΔG (2 boxes / 10 holes ⇒ -1/6)"
      (is (< (Math/abs (- (:realized-score ro) (- (/ 1.0 6.0)))) 1e-9))
      (is (= 1783280000000 (:tick ro))))))

(deftest sign-convention-matches-3b-sim
  (let [expected -0.7
        realized (- (/ 1.0 6.0))
        perf (pp/policy-performance expected realized)]
    (testing "under-delivery (realized coverage above expected G) ⇒ perf < 0"
      ;; perf = (e - r)/(|e|+|r|+eps) = (-0.7 + 1/6)/(0.7 + 1/6) ≈ -0.6154
      (is (neg? perf))
      (is (< (Math/abs (- perf (/ (+ -0.7 (/ 1.0 6.0))
                                  (+ 0.7 (/ 1.0 6.0) 1.0e-9)))) 1e-12)))
    (testing "fed repeatedly through the SIM'S fold (fold-realized-outcome),
              γ hedges below 1 after burn-in — gate-3b's historical direction"
      (let [outcomes (map (fn [i] {:policy "lane" :expected-score expected
                                   :realized-score realized :tick i})
                          (range pp/default-min-history))
            final (reduce pp/fold-realized-outcome
                          (pp/initial-selection-gain-state)
                          outcomes)]
        (is (= pp/default-min-history (:samples final)))
        (is (< (:selection-gain final) 1.0))))
    (testing "over-delivery (sim's synthetic-paying shape: realized 2x expected)
              ⇒ γ commits above 1 — gate-3b's synthetic direction"
      (let [outcomes (map (fn [i] {:policy "lane" :expected-score -0.25
                                   :realized-score -0.5 :tick i})
                          (range pp/default-min-history))
            final (reduce pp/fold-realized-outcome
                          (pp/initial-selection-gain-state)
                          outcomes)]
        (is (> (:selection-gain final) 1.0))))))
