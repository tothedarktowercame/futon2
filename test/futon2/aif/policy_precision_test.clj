(ns futon2.aif.policy-precision-test
  "Tests for R14 precision-over-policies (γ): a bounded scalar inverse selection
   temperature learned from the realized-vs-expected PERFORMANCE of chosen
   policies (signed: beat ⇒ commit, miss ⇒ hedge)."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.policy-precision :as gamma]))

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
    (let [s (gamma/initial-policy-precision-state)]
      (is (= 1.0 (:policy-precision s)))
      (is (= [] (:perf-history s)))
      (is (nil? (:mean-perf s)))
      (is (= 0 (:samples s))))))

(deftest gamma-for-defaults-to-prior-test
  (testing "gamma-for returns the tracked γ, else 1.0"
    (is (= 7.0 (gamma/gamma-for {:policy-precision 7.0})))
    (is (= 1.0 (gamma/gamma-for nil)))
    (is (= 1.0 (gamma/gamma-for {})))))

;; ---------------------------------------------------------------------------
;; burn-in: reduction-safety
;; ---------------------------------------------------------------------------

(deftest burn-in-keeps-prior-test
  (testing "γ stays EXACTLY 1.0 until min-history samples accrue"
    (let [s0 (gamma/initial-policy-precision-state)
          ;; feed 4 maximal-beat outcomes (perf=1) with default min-history 5
          s (reduce (fn [s _] (gamma/update-policy-precision s 1.0))
                    s0 (range 4))]
      (is (= 1.0 (:policy-precision s))
          "still in burn-in (4 < 5) → prior preserved despite perfect beats")
      (is (nil? (:mean-perf s)))
      (is (= 4 (:samples s)))
      (is (= 4 (count (:perf-history s))) "history still accrues during burn-in"))))

(deftest burn-in-releases-after-min-history-test
  (testing "the 5th beat sample releases γ above the prior"
    (let [s0 (gamma/initial-policy-precision-state)
          s (reduce (fn [s _] (gamma/update-policy-precision s 1.0))
                    s0 (range 5))]
      (is (> (:policy-precision s) 1.0)
          "perf̄=1 after burn-in → γ rises toward the cap (commit harder)")
      (is (= 1.0 (:mean-perf s)))
      (is (= 5 (:samples s))))))

;; ---------------------------------------------------------------------------
;; transfer: beat → commit, miss → hedge, exact-match → neutral
;; ---------------------------------------------------------------------------

(defn- feed [perf n]
  (reduce (fn [s _] (gamma/update-policy-precision s perf))
          (gamma/initial-policy-precision-state)
          (range n)))

(deftest sustained-beats-hit-cap-test
  (testing "sustained perf=+1 (every plan over-delivered) → γ at the cap (2.0)"
    (is (= 2.0 (:policy-precision (feed 1.0 10))))))

(deftest sustained-misses-hit-floor-test
  (testing "sustained perf=−1 (every plan missed) → γ at the floor (0.5)"
    (is (= 0.5 (:policy-precision (feed -1.0 10))))))

(deftest exact-match-is-unity-test
  (testing "mean performance = 0 (realized as predicted) → γ = 1.0 exactly"
    (is (< (Math/abs (- 1.0 (:policy-precision (feed 0.0 10)))) 1e-9))))

(deftest gamma-monotone-in-performance-test
  (testing "higher realized performance → higher γ (more decisive)"
    (let [hi (:policy-precision (feed 0.8 10))
          mid (:policy-precision (feed 0.0 10))
          lo (:policy-precision (feed -0.8 10))]
      (is (> hi mid))
      (is (> mid lo)))))

;; ---------------------------------------------------------------------------
;; window bounding + observe-outcome + purity
;; ---------------------------------------------------------------------------

(deftest history-bounded-to-window-test
  (testing "history bounded to window-size; oldest drop"
    (let [s (reduce (fn [s p] (gamma/update-policy-precision s p {:window-size 5}))
                    (gamma/initial-policy-precision-state)
                    (mapv #(- (/ (double %) 15.0) 1.0) (range 30)))]
      (is (= 5 (count (:perf-history s)))))))

(deftest observe-outcome-directional-test
  (testing "observe-outcome folds the (expected, realized) pair via signed performance"
    ;; lower G is better: realized below expected = beat = γ up; above = miss = γ down
    (let [beat (reduce (fn [s _] (gamma/observe-outcome s -0.5 -1.5))
                       (gamma/initial-policy-precision-state) (range 8))
          miss (reduce (fn [s _] (gamma/observe-outcome s -0.5 0.5))
                       (gamma/initial-policy-precision-state) (range 8))
          even (reduce (fn [s _] (gamma/observe-outcome s -0.5 -0.5))
                       (gamma/initial-policy-precision-state) (range 8))]
      (is (> (gamma/gamma-for beat) 1.0) "realized beat expectation ⇒ commit")
      (is (< (gamma/gamma-for miss) 1.0) "realized missed ⇒ hedge")
      (is (< (Math/abs (- 1.0 (gamma/gamma-for even))) 1e-9) "realized as predicted ⇒ neutral"))))

(deftest update-is-pure-test
  (testing "same inputs → same output"
    (let [s0 (gamma/initial-policy-precision-state)
          a (gamma/update-policy-precision s0 0.3)
          b (gamma/update-policy-precision s0 0.3)]
      (is (= a b)))))

;; ---------------------------------------------------------------------------
;; fold-realized-outcome — the R16 close-the-loop seam (contract with claude-10)
;; ---------------------------------------------------------------------------

(defn- realized [expected-g realized-g tick]
  {:policy :p/x :expected-G expected-g :realized-G realized-g :tick tick})

(deftest fold-absent-outcome-holds-prior-test
  (testing "absent / nil realized-outcome ⇒ γ-state unchanged (reduction-safe)"
    (let [s0 (gamma/initial-policy-precision-state)]
      (is (= s0 (gamma/fold-realized-outcome s0 nil)))
      (is (= s0 (gamma/fold-realized-outcome s0 {}))))))

(deftest fold-malformed-outcome-holds-prior-test
  (testing "missing / non-numeric G legs ⇒ unchanged"
    (let [s0 (gamma/initial-policy-precision-state)]
      (is (= s0 (gamma/fold-realized-outcome s0 {:expected-G 1.0 :tick 3})))
      (is (= s0 (gamma/fold-realized-outcome s0 {:expected-G "x" :realized-G 1.0 :tick 3}))))))

(deftest fold-well-formed-outcome-updates-and-stamps-tick-test
  (testing "a well-formed outcome folds and records :last-outcome-tick"
    (let [s0 (gamma/initial-policy-precision-state)
          s1 (gamma/fold-realized-outcome s0 (realized -0.5 -0.9 7))]
      (is (= 7 (:last-outcome-tick s1)))
      (is (= 1 (:samples s1)) "one realized sample folded"))))

(deftest fold-dedups-on-tick-test
  (testing "the SAME outcome (same :tick) is folded at most once across ticks"
    (let [o (realized -0.5 -0.9 7)
          s0 (gamma/initial-policy-precision-state)
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
                       (gamma/initial-policy-precision-state) (range 1 8))
          miss (reduce (fn [s t] (gamma/fold-realized-outcome s (realized -0.5 0.5 t)))
                       (gamma/initial-policy-precision-state) (range 1 8))]
      (is (> (gamma/gamma-for beat) 1.0) "realized beat expectation ⇒ commit harder")
      (is (< (gamma/gamma-for miss) 1.0) "realized underperformed ⇒ hedge"))))

(deftest gamma-stays-within-bounds-test
  (testing "γ never leaves [floor, cap] for any perf in [−1,1]"
    (doseq [perf [-1.0 -0.6 -0.25 0.0 0.25 0.6 1.0]]
      (let [g (:policy-precision (feed perf 10))]
        (is (<= 0.5 g 2.0) (str "γ=" g " out of bounds for perf=" perf))))))
