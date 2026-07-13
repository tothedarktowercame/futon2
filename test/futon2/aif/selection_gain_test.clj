(ns futon2.aif.selection-gain-test
  "Tests for R14 precision-over-policies (γ): a bounded scalar inverse selection
   temperature learned from the realized-vs-expected PERFORMANCE of chosen
   policies (signed: beat ⇒ commit, miss ⇒ hedge)."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.policy :as policy]
            [futon2.aif.selection-gain :as gamma]))

;; ---------------------------------------------------------------------------
;; policy-performance — the signed, scale-free signal (lower G is better)
;; ---------------------------------------------------------------------------

(deftest performance-exact-match-is-zero-test
  (testing "realized == expected → 0.0 (neutral, no information about decisiveness)"
    (is (< (Math/abs (gamma/policy-performance 3.0 3.0)) 1e-9))
    (is (< (Math/abs (gamma/policy-performance -2.5 -2.5)) 1e-9))
    (testing "both ~0 → ~0 via the ε guard"
      (is (< (Math/abs (gamma/policy-performance 0.0 0.0)) 1e-6)))))

(deftest performance-beat-is-positive-test
  (testing "realized BELOW expected (lower G = better = over-delivered) → perf > 0"
    (is (pos? (gamma/policy-performance -0.5 -0.9)))   ; predicted -0.5 drop, got -0.9
    (is (pos? (gamma/policy-performance 1.0 0.1)))))

(deftest performance-miss-is-negative-test
  (testing "realized ABOVE expected (higher G = worse = underperformed) → perf < 0"
    (is (neg? (gamma/policy-performance -0.5 0.3)))    ; predicted FE drop, FE rose instead
    (is (neg? (gamma/policy-performance 0.1 1.0))))
  (testing "a total miss (predicted drop, equal-magnitude rise) → −1 (modulo the ε guard)"
    (is (< (Math/abs (- -1.0 (gamma/policy-performance -0.5 0.5))) 1e-6))))

(deftest performance-scale-free-test
  (testing "perf depends on the RATIO, not the absolute magnitude"
    (is (< (Math/abs (- (gamma/policy-performance 2.0 1.0)
                        (gamma/policy-performance 200.0 100.0)))
           1e-9))))

;; ---------------------------------------------------------------------------
;; initial state + accessor
;; ---------------------------------------------------------------------------

(deftest initial-state-is-the-prior-test
  (testing "initial γ-state is the prior: γ=1.0, empty history"
    (let [s (gamma/initial-selection-gain-state)]
      (is (= 1.0 (:selection-gain s)))
      (is (= [] (:perf-history s)))
      (is (nil? (:mean-perf s)))
      (is (= 0 (:samples s))))))

(deftest selection-gain-for-defaults-to-prior-test
  (testing "selection-gain-for returns the tracked γ, else 1.0"
    (is (= 7.0 (gamma/selection-gain-for {:selection-gain 7.0})))
    (is (= 1.0 (gamma/selection-gain-for nil)))
    (is (= 1.0 (gamma/selection-gain-for {})))))

;; ---------------------------------------------------------------------------
;; burn-in: reduction-safety
;; ---------------------------------------------------------------------------

(deftest burn-in-keeps-prior-test
  (testing "γ stays EXACTLY 1.0 until min-history samples accrue"
    (let [s0 (gamma/initial-selection-gain-state)
          ;; feed 4 maximal-beat outcomes (perf=1) with default min-history 5
          s (reduce (fn [s _] (gamma/update-selection-gain s 1.0))
                    s0 (range 4))]
      (is (= 1.0 (:selection-gain s))
          "still in burn-in (4 < 5) → prior preserved despite perfect beats")
      (is (nil? (:mean-perf s)))
      (is (= 4 (:samples s)))
      (is (= 4 (count (:perf-history s))) "history still accrues during burn-in"))))

(deftest burn-in-releases-after-min-history-test
  (testing "the 5th beat sample releases γ above the prior"
    (let [s0 (gamma/initial-selection-gain-state)
          s (reduce (fn [s _] (gamma/update-selection-gain s 1.0))
                    s0 (range 5))]
      (is (> (:selection-gain s) 1.0)
          "perf̄=1 after burn-in → γ rises toward the cap (commit harder)")
      (is (= 1.0 (:mean-perf s)))
      (is (= 5 (:samples s))))))

;; ---------------------------------------------------------------------------
;; transfer: beat → commit, miss → hedge, exact-match → neutral
;; ---------------------------------------------------------------------------

(defn- feed [perf n]
  (reduce (fn [s _] (gamma/update-selection-gain s perf))
          (gamma/initial-selection-gain-state)
          (range n)))

(deftest sustained-beats-hit-cap-test
  (testing "sustained perf=+1 (every plan over-delivered) → γ at the cap (2.0)"
    (is (= 2.0 (:selection-gain (feed 1.0 10))))))

(deftest sustained-misses-hit-floor-test
  (testing "sustained perf=−1 (every plan missed) → γ at the floor (0.5)"
    (is (= 0.5 (:selection-gain (feed -1.0 10))))))

(deftest exact-match-is-unity-test
  (testing "mean performance = 0 (realized as predicted) → γ = 1.0 exactly"
    (is (< (Math/abs (- 1.0 (:selection-gain (feed 0.0 10)))) 1e-9))))

(deftest gamma-monotone-in-performance-test
  (testing "higher realized performance → higher γ (more decisive)"
    (let [hi (:selection-gain (feed 0.8 10))
          mid (:selection-gain (feed 0.0 10))
          lo (:selection-gain (feed -0.8 10))]
      (is (> hi mid))
      (is (> mid lo)))))

;; ---------------------------------------------------------------------------
;; window bounding + observe-outcome + purity
;; ---------------------------------------------------------------------------

(deftest history-bounded-to-window-test
  (testing "history bounded to window-size; oldest drop"
    (let [s (reduce (fn [s p] (gamma/update-selection-gain s p {:window-size 5}))
                    (gamma/initial-selection-gain-state)
                    (mapv #(- (/ (double %) 15.0) 1.0) (range 30)))]
      (is (= 5 (count (:perf-history s)))))))

(deftest observe-outcome-directional-test
  (testing "observe-outcome folds the (expected, realized) pair via signed performance"
    ;; lower G is better: realized below expected = beat = γ up; above = miss = γ down
    (let [beat (reduce (fn [s _] (gamma/observe-outcome s -0.5 -1.5))
                       (gamma/initial-selection-gain-state) (range 8))
          miss (reduce (fn [s _] (gamma/observe-outcome s -0.5 0.5))
                       (gamma/initial-selection-gain-state) (range 8))
          even (reduce (fn [s _] (gamma/observe-outcome s -0.5 -0.5))
                       (gamma/initial-selection-gain-state) (range 8))]
      (is (> (gamma/selection-gain-for beat) 1.0) "realized beat expectation ⇒ commit")
      (is (< (gamma/selection-gain-for miss) 1.0) "realized missed ⇒ hedge")
      (is (< (Math/abs (- 1.0 (gamma/selection-gain-for even))) 1e-9) "realized as predicted ⇒ neutral"))))

(deftest update-is-pure-test
  (testing "same inputs → same output"
    (let [s0 (gamma/initial-selection-gain-state)
          a (gamma/update-selection-gain s0 0.3)
          b (gamma/update-selection-gain s0 0.3)]
      (is (= a b)))))

;; ---------------------------------------------------------------------------
;; fold-realized-outcome — the R16 close-the-loop seam (contract with claude-10)
;; ---------------------------------------------------------------------------

(defn- realized [expected-g realized-g tick]
  {:policy :p/x :expected-score expected-g :realized-score realized-g :tick tick})

(deftest fold-absent-outcome-holds-prior-test
  (testing "absent / nil realized-outcome ⇒ γ-state unchanged (reduction-safe)"
    (let [s0 (gamma/initial-selection-gain-state)]
      (is (= s0 (gamma/fold-realized-outcome s0 nil)))
      (is (= s0 (gamma/fold-realized-outcome s0 {}))))))

(deftest fold-malformed-outcome-holds-prior-test
  (testing "missing / non-numeric G legs ⇒ unchanged"
    (let [s0 (gamma/initial-selection-gain-state)]
      (is (= s0 (gamma/fold-realized-outcome s0 {:expected-score 1.0 :tick 3})))
      (is (= s0 (gamma/fold-realized-outcome s0 {:expected-score "x" :realized-score 1.0 :tick 3}))))))

(deftest fold-well-formed-outcome-updates-and-stamps-tick-test
  (testing "a well-formed outcome folds and records :last-outcome-tick"
    (let [s0 (gamma/initial-selection-gain-state)
          s1 (gamma/fold-realized-outcome s0 (realized -0.5 -0.9 7))]
      (is (= 7 (:last-outcome-tick s1)))
      (is (= 1 (:samples s1)) "one realized sample folded"))))

(deftest fold-dedups-on-tick-test
  (testing "the SAME outcome (same :tick) is folded at most once across ticks"
    (let [o (realized -0.5 -0.9 7)
          s0 (gamma/initial-selection-gain-state)
          s1 (gamma/fold-realized-outcome s0 o)
          ;; judge ticks again before a new enactment: same outcome seen
          s2 (gamma/fold-realized-outcome s1 o)
          s3 (gamma/fold-realized-outcome s2 o)]
      (is (= 1 (:samples s1)))
      (is (= s1 s2) "no re-fold of an already-seen tick")
      (is (= s1 s3))
      (testing "a genuinely new enactment (new :tick) DOES fold"
        (let [s4 (gamma/fold-realized-outcome s3 (realized -0.5 -0.9 8))]
          (is (= 2 (:samples s4)))
          (is (= 8 (:last-outcome-tick s4))))))))

(deftest fold-beat-vs-missed-outcomes-move-gamma-test
  (testing "after burn-in, beating expectation raises γ; missing it lowers γ"
    (let [beat (reduce (fn [s t] (gamma/fold-realized-outcome s (realized -0.5 -1.5 t)))
                       (gamma/initial-selection-gain-state) (range 1 8))
          miss (reduce (fn [s t] (gamma/fold-realized-outcome s (realized -0.5 0.5 t)))
                       (gamma/initial-selection-gain-state) (range 1 8))]
      (is (> (gamma/selection-gain-for beat) 1.0) "realized beat expectation ⇒ commit harder")
      (is (< (gamma/selection-gain-for miss) 1.0) "realized underperformed ⇒ hedge"))))

(deftest gamma-stays-within-bounds-test
  (testing "γ never leaves [floor, cap] for any perf in [−1,1]"
    (doseq [perf [-1.0 -0.6 -0.25 0.0 0.25 0.6 1.0]]
      (let [g (:selection-gain (feed perf 10))]
        (is (<= 0.5 g 2.0) (str "γ=" g " out of bounds for perf=" perf))))))

;; ---------------------------------------------------------------------------
;; LOCK-IN regression — γ self-calibration, end to end (outcome → γ → selection)
;; ---------------------------------------------------------------------------
;; This is the drift guard for R14's whole point: a run of GOOD realized outcomes
;; must SHARPEN selection (γ↑ / τ↓ / more mass on the best policy), a run of BAD
;; ones must HEDGE (γ↓ / τ↑ / flatter), and realizing exactly-as-predicted must
;; be byte-identical to the spread-only path (reduction-safe). Numbers are pinned
;; deterministically so any silent change to the signal, transfer, or wiring
;; trips this test. Outcomes are driven through the REAL R16 :realized-outcome
;; seam (fold-realized-outcome), then through the REAL selector (select-action).

(defn- fold-run
  "Fold n identical realized outcomes (distinct ticks) through the R16 seam."
  [expected realized n]
  (reduce (fn [s t] (gamma/fold-realized-outcome
                     s {:policy :pi :expected-score expected :realized-score realized :tick t}))
          (gamma/initial-selection-gain-state)
          (range 1 (inc n))))

(deftest gamma-self-calibration-regression-test
  (let [cand [{:action {:type :a} :controller-score 0.0}    ; fixed candidates ⇒ fixed
              {:action {:type :b} :controller-score 0.5}    ; G-spread, so any sharpness
              {:action {:type :c} :controller-score 1.0}]   ; change is γ alone
        ;; lower G is better: realized below expected = beat; above = miss.
        good (fold-run -0.5 -1.5 8)   ; every plan over-delivers → perf̄ = 0.5
        bad  (fold-run -0.5  0.5 8)   ; every plan misses        → perf̄ = -1.0
        even (fold-run -0.5 -0.5 8)   ; every plan as predicted  → perf̄ = 0.0
        sel  (fn [st] (policy/select-action cand {:selection-gain (gamma/selection-gain-for st)}))
        best (fn [d] (apply max (vals (:softmax-weights d))))
        dg (sel good) db (sel bad) de (sel even)]

    (testing "γ is pinned to its deterministic transfer values (catches drift)"
      (is (< (Math/abs (- (Math/sqrt 2.0) (gamma/selection-gain-for good))) 1e-6)
          "perf̄=0.5 → γ = 2^0.5 ≈ 1.4142")
      (is (< (Math/abs (- 0.5 (gamma/selection-gain-for bad))) 1e-6)  "perf̄=-1 → γ = floor 0.5")
      (is (< (Math/abs (- 1.0 (gamma/selection-gain-for even))) 1e-9) "perf̄=0 → γ = 1.0 (neutral)"))

    (testing "GOOD run sharpens selection vs the neutral baseline"
      (is (> (gamma/selection-gain-for good) 1.0))
      (is (< (:tau dg) (:tau-spread dg)) "effective τ pulled BELOW the spread-τ")
      (is (> (best dg) (best de)) "more probability mass on the best policy"))

    (testing "BAD run hedges selection vs the neutral baseline"
      (is (< (gamma/selection-gain-for bad) 1.0))
      (is (> (:tau db) (:tau-spread db)) "effective τ pushed ABOVE the spread-τ")
      (is (< (best db) (best de)) "probability mass spread flatter"))

    (testing "EXACT-as-predicted is reduction-safe (byte-identical to spread path)"
      (is (= (:tau-spread de) (:tau de)) "τ_eff == τ_spread at γ=1.0"))

    (testing "monotone decisiveness ordering: good ≻ neutral ≻ bad"
      (is (> (best dg) (best de) (best db))))))
