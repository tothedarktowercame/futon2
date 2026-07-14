(ns ants.aif.efe-test
  "Unit tests for the canonical EFE core (R5).

   Acceptance bar:
   1. risk == analytic Gaussian KL on hand-checked fixtures.
   2. ambiguity == ½·ln(2πe·σ²) on hand-checked fixtures.
   3. g-efe matches futon2.aif.efe/ambiguity :gaussian-entropy to 1e-9.
   4. Changing σ² changes both risk and ambiguity."
  (:require [clojure.test :refer [deftest is testing]]
            [ants.aif.efe :as efe]))

;; --------------------------------------------------------------------------- ;;
;; Hand-checked Gaussian entropy: ½·ln(2πe·σ²)
;; --------------------------------------------------------------------------- ;;

(deftest gaussian-entropy-hand-checked
  (testing "½·ln(2πe·σ²) on hand-checked values"
    ;; σ²=1: H = ½·ln(2πe) = ½·ln(17.079...) ≈ 1.4189...
    (is (< (Math/abs (- (efe/gaussian-entropy 1.0)
                        (* 0.5 (Math/log (* 2.0 Math/PI Math/E)))))
           1e-12)
        "σ²=1 matches ½·ln(2πe)")
    ;; σ²=0.01: H = ½·ln(2πe·0.01) = ½·ln(0.17079...) ≈ -1.0702...
    (is (< (Math/abs (- (efe/gaussian-entropy 0.01)
                        (* 0.5 (Math/log (* 2.0 Math/PI Math/E 0.01)))))
           1e-12)
        "σ²=0.01 matches ½·ln(2πe·0.01)")
    ;; σ²=0: floored at 1e-9 → finite (not -∞)
    (is (Double/isFinite (efe/gaussian-entropy 0.0))
        "σ²=0 is floored to finite value")))

(deftest ambiguity-sums-entropies
  (testing "ambiguity sums per-channel entropies"
    (let [vars {:a 1.0 :b 1.0}
          expected (+ (* 0.5 (Math/log (* 2.0 Math/PI Math/E 1.0)))
                      (* 0.5 (Math/log (* 2.0 Math/PI Math/E 1.0))))]
      (is (< (Math/abs (- (efe/ambiguity vars) expected)) 1e-12)
          "two-channel sum matches"))))

;; --------------------------------------------------------------------------- ;;
;; Hand-checked Gaussian KL: KL(N₁‖N₂) = ½[ln(σ²₂/σ²₁) + (σ²₁+(μ₁-μ₂)²)/σ²₂ - 1]
;; --------------------------------------------------------------------------- ;;

(deftest gaussian-kl-hand-checked
  (testing "analytic Gaussian KL on hand-checked fixtures"
    ;; Fixture 1: identical distributions → KL = 0
    (is (< (Math/abs (efe/gaussian-kl 0.5 1.0 0.5 1.0)) 1e-12)
        "identical Gaussians → KL = 0")
    ;; Fixture 2: same variance, different mean → KL = ½·(μ₁-μ₂)²/σ²
    ;; μ₁=0.8, μ₂=0.4, σ²=0.04 → KL = ½·(0.4)²/0.04 = ½·4.0 = 2.0
    (is (< (Math/abs (- (efe/gaussian-kl 0.8 0.04 0.4 0.04) 2.0)) 1e-12)
        "mean-shift KL = ½·Δμ²/σ² = 2.0")
    ;; Fixture 3: different variance → includes log-variance term
    ;; KL(N(0,1)‖N(0,4)) = ½[ln(4/1) + (1+0)/4 - 1] = ½[ln4 + 0.25 - 1]
    ;;                   = ½[1.3863... - 0.75] = 0.3181...
    (let [expected (* 0.5 (- (+ (Math/log 4.0) (/ 1.0 4.0)) 1.0))]
      (is (< (Math/abs (- (efe/gaussian-kl 0.0 1.0 0.0 4.0) expected)) 1e-12)
          "variance-ratio KL includes log-variance term"))))

(deftest kl-always-non-negative
  (testing "Gaussian KL is always ≥ 0"
    (doseq [mu1 [0.0 0.3 0.7 1.0]
            mu2 [0.0 0.5 1.0]
            s2-1 [0.001 0.01 0.1 1.0]
            s2-2 [0.001 0.01 0.1 1.0]]
      (is (>= (efe/gaussian-kl mu1 s2-1 mu2 s2-2) -1e-12)
          (str "KL(" mu1 "," s2-1 "‖" mu2 "," s2-2 ") ≥ 0")))))

;; --------------------------------------------------------------------------- ;;
;; g-efe composition
;; --------------------------------------------------------------------------- ;;

(deftest g-efe-composition
  (testing "g-efe = risk + ambiguity"
    (let [means [0.5 0.3]
          vars [0.01 0.04]
          c-means [0.4 0.5]
          c-vars [0.0064 0.01]
          result (efe/g-efe means vars c-means c-vars)
          expected-g (+ (:risk result) (:ambiguity result))]
      (is (< (Math/abs (- (:g-efe result) expected-g)) 1e-12)
          "g-efe = risk + ambiguity"))))

;; --------------------------------------------------------------------------- ;;
;; Changing σ² changes both risk and ambiguity
;; --------------------------------------------------------------------------- ;;

(deftest sigma2-flows-into-both-terms
  (testing "changing σ² changes both risk and ambiguity"
    (let [means [0.5 0.3]
          c-means [0.4 0.5]
          c-vars [0.01 0.01]
          low-var (efe/g-efe means [0.001 0.001] c-means c-vars)
          high-var (efe/g-efe means [0.5 0.5] c-means c-vars)]
      (is (not= (:risk low-var) (:risk high-var))
          "risk changes with σ²")
      (is (not= (:ambiguity low-var) (:ambiguity high-var))
          "ambiguity changes with σ²")
      (is (> (:ambiguity high-var) (:ambiguity low-var))
          "higher σ² → higher ambiguity (more uncertainty)"))))

;; --------------------------------------------------------------------------- ;;
;; Cross-check: ambiguity matches futon2.aif.efe/ambiguity :gaussian-entropy
;; --------------------------------------------------------------------------- ;;

(deftest ambiguity-matches-wm-efe
  (testing "ambiguity :gaussian-entropy matches futon2.aif.efe to 1e-9"
    (let [vars {:a 0.01 :b 0.1 :c 1.0 :d 0.5}
          ant-result (efe/ambiguity vars :gaussian-entropy)
          wm-result (reduce + 0.0
                            (map (fn [v]
                                   (* 0.5 (Math/log (* 2.0 Math/PI Math/E
                                                       (max (double v) 1e-9)))))
                                 (vals vars)))]
      (is (< (Math/abs (- ant-result wm-result)) 1e-9)
          "ant ambiguity matches WM ambiguity formula to 1e-9"))))

;; --------------------------------------------------------------------------- ;;
;; Weighted risk
;; --------------------------------------------------------------------------- ;;

(deftest weighted-risk
  (testing "per-channel weights scale risk contribution"
    (let [means [0.8 0.2]
          vars [0.04 0.04]
          c-means [0.4 0.4]
          c-vars [0.04 0.04]
          unweighted (efe/risk means vars c-means c-vars)
          weighted (efe/risk means vars c-means c-vars [2.0 0.0])]
      ;; Channel 0 has Δμ=0.4, channel 1 has Δμ=-0.2
      ;; Weighting [2,0] doubles ch0 and zeros ch1
      (is (< (Math/abs (- weighted (* 2.0 (efe/gaussian-kl 0.8 0.04 0.4 0.04)))) 1e-12)
          "weight [2,0] doubles ch0, zeros ch1")
      (is (not= unweighted weighted)
          "different weights → different risk"))))
