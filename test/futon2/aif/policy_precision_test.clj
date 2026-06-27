(ns futon2.aif.policy-precision-test
  "Tests for R14 precision-over-policies (γ): a bounded scalar inverse selection
   temperature learned from the realized-vs-expected outcomes of chosen policies."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.policy-precision :as gamma]))

;; ---------------------------------------------------------------------------
;; relative-error — the scale-free signal
;; ---------------------------------------------------------------------------

(deftest relative-error-perfect-test
  (testing "expected == realized → 0.0 (no surprise)"
    (is (< (gamma/relative-error 3.0 3.0) 1e-9))
    (is (< (gamma/relative-error -2.5 -2.5) 1e-9)))
  (testing "both ~0 → ~0 via the ε guard (nothing expected, nothing happened)"
    (is (< (gamma/relative-error 0.0 0.0) 1e-6))))

(deftest relative-error-opposite-signs-near-one-test
  (testing "opposite-sign equal-magnitude → ρ ≈ 1 (maximal surprise)"
    (is (> (gamma/relative-error 1.0 -1.0) 0.999))
    (is (> (gamma/relative-error -4.0 4.0) 0.999))))

(deftest relative-error-scale-free-test
  (testing "ρ depends on the RATIO, not the absolute magnitude"
    ;; same proportional gap at two magnitude regimes → same ρ
    (is (< (Math/abs (- (gamma/relative-error 1.0 2.0)
                        (gamma/relative-error 100.0 200.0)))
           1e-9))))

;; ---------------------------------------------------------------------------
;; initial state + accessor
;; ---------------------------------------------------------------------------

(deftest initial-state-is-the-prior-test
  (testing "initial γ-state is the prior: γ=1.0, empty history"
    (let [s (gamma/initial-policy-precision-state)]
      (is (= 1.0 (:policy-precision s)))
      (is (= [] (:error-history s)))
      (is (nil? (:mean-error s)))
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
          ;; feed 4 perfect outcomes (ρ=0) with default min-history 5
          s (reduce (fn [s _] (gamma/update-policy-precision s 0.0))
                    s0 (range 4))]
      (is (= 1.0 (:policy-precision s))
          "still in burn-in (4 < 5) → prior preserved despite perfect outcomes")
      (is (nil? (:mean-error s)))
      (is (= 4 (:samples s)))
      (is (= 4 (count (:error-history s))) "history still accrues during burn-in"))))

(deftest burn-in-releases-after-min-history-test
  (testing "the 5th perfect-outcome sample releases γ above the prior"
    (let [s0 (gamma/initial-policy-precision-state)
          s (reduce (fn [s _] (gamma/update-policy-precision s 0.0))
                    s0 (range 5))]
      (is (> (:policy-precision s) 1.0)
          "ρ̄=0 after burn-in → γ rises toward the cap (commit harder)")
      (is (= 0.0 (:mean-error s)))
      (is (= 5 (:samples s))))))

;; ---------------------------------------------------------------------------
;; transfer: low error → commit, high error → hedge, neutral → 1
;; ---------------------------------------------------------------------------

(defn- feed [rel-error n]
  (reduce (fn [s _] (gamma/update-policy-precision s rel-error))
          (gamma/initial-policy-precision-state)
          (range n)))

(deftest perfect-outcomes-hit-cap-test
  (testing "sustained ρ=0 → γ at the cap (2.0)"
    (is (= 2.0 (:policy-precision (feed 0.0 10))))))

(deftest total-miss-hits-floor-test
  (testing "sustained ρ=1 → γ at the floor (0.5)"
    (is (= 0.5 (:policy-precision (feed 1.0 10))))))

(deftest neutral-error-is-unity-test
  (testing "mean relative error = ρ* (0.5) → γ = 1.0 exactly"
    (is (< (Math/abs (- 1.0 (:policy-precision (feed 0.5 10)))) 1e-9))))

(deftest gamma-monotone-in-confidence-test
  (testing "lower realized error → higher γ"
    (let [low (:policy-precision (feed 0.2 10))
          mid (:policy-precision (feed 0.5 10))
          high (:policy-precision (feed 0.8 10))]
      (is (> low mid))
      (is (> mid high)))))

;; ---------------------------------------------------------------------------
;; window bounding + observe-outcome + purity
;; ---------------------------------------------------------------------------

(deftest history-bounded-to-window-test
  (testing "history bounded to window-size; oldest drop"
    (let [s (reduce (fn [s e] (gamma/update-policy-precision s e {:window-size 5}))
                    (gamma/initial-policy-precision-state)
                    (mapv #(/ (double %) 30.0) (range 30)))]
      (is (= 5 (count (:error-history s)))))))

(deftest observe-outcome-computes-relative-error-test
  (testing "observe-outcome folds the (expected, realized) pair via relative-error"
    (let [via-pair (reduce (fn [s _] (gamma/observe-outcome s 2.0 2.0)) ; ρ=0
                           (gamma/initial-policy-precision-state) (range 6))
          via-rho  (feed 0.0 6)]
      (is (= (:policy-precision via-pair) (:policy-precision via-rho))
          "expected==realized ⇒ same trajectory as feeding ρ=0"))))

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
          s1 (gamma/fold-realized-outcome s0 (realized 1.0 1.0 7))]
      (is (= 7 (:last-outcome-tick s1)))
      (is (= 1 (:samples s1)) "one realized sample folded"))))

(deftest fold-dedups-on-tick-test
  (testing "the SAME outcome (same :tick) is folded at most once across ticks"
    (let [o (realized 1.0 1.0 7)
          s0 (gamma/initial-policy-precision-state)
          s1 (gamma/fold-realized-outcome s0 o)
          ;; judge ticks again before a new enactment: same outcome seen
          s2 (gamma/fold-realized-outcome s1 o)
          s3 (gamma/fold-realized-outcome s2 o)]
      (is (= 1 (:samples s1)))
      (is (= s1 s2) "no re-fold of an already-seen tick")
      (is (= s1 s3))
      (testing "a genuinely new enactment (new :tick) DOES fold"
        (let [s4 (gamma/fold-realized-outcome s3 (realized 1.0 1.0 8))]
          (is (= 2 (:samples s4)))
          (is (= 8 (:last-outcome-tick s4))))))))

(deftest fold-perfect-vs-missed-outcomes-move-gamma-test
  (testing "after burn-in, perfect realizations raise γ; missed ones lower it"
    (let [perfect (reduce (fn [s t] (gamma/fold-realized-outcome s (realized 2.0 2.0 t)))
                          (gamma/initial-policy-precision-state) (range 1 8))
          missed (reduce (fn [s t] (gamma/fold-realized-outcome s (realized 2.0 -2.0 t)))
                         (gamma/initial-policy-precision-state) (range 1 8))]
      (is (> (gamma/gamma-for perfect) 1.0) "expected≈realized ⇒ commit harder")
      (is (< (gamma/gamma-for missed) 1.0) "expected vs realized opposite ⇒ hedge"))))

(deftest gamma-stays-within-bounds-test
  (testing "γ never leaves [floor, cap] for any ρ in [0,1]"
    (doseq [rho [0.0 0.1 0.25 0.5 0.75 0.9 1.0]]
      (let [g (:policy-precision (feed rho 10))]
        (is (<= 0.5 g 2.0) (str "γ=" g " out of bounds for ρ=" rho))))))
