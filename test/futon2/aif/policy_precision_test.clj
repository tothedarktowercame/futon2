(ns futon2.aif.policy-precision-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.policy-precision :as policy-precision]))

(deftest converges-on-real-field-shaped-input-test
  ;; Shape copied from the measured WM field: 110 aligned candidates, rather
  ;; than a two-policy toy. Values span the same order of magnitude as G-total
  ;; and the horizon-one Gaussian F_pi measurements.
  (let [g-values (mapv #(+ 0.25 (* 0.004 %)) (range 110))
        f-pi-values (mapv #(+ -18.0 (* 0.03 (mod % 17))) (range 110))
        result (policy-precision/converge-beta 1.0 g-values f-pi-values)]
    (is (:converged? result))
    (is (false? (:hit-bound? result)))
    (is (<= (Math/abs (:fixed-point-residual result)) 1.0e-9))
    (is (= 110 (count (:pi result))))
    (is (= 110 (count (:pi-0 result))))))

(deftest beta-floor-is-explicit-test
  (let [result (policy-precision/converge-beta
                0.1 [0.0 0.2] [0.0 100.0]
                {:step-size 0.25 :max-iterations 1 :beta-floor 1.0e-5})]
    (is (:beta-floor-hit? result))
    (is (pos? (:beta-posterior result)))
    (is (pos? (:gamma result)))
    (is (Double/isFinite (:gamma result)))))

(deftest iteration-bound-is-reported-test
  (let [result (policy-precision/converge-beta
                1.0 [0.0 1.0] [0.0 4.0]
                {:max-iterations 0 :tolerance 0.0})]
    (is (false? (:converged? result)))
    (is (:hit-bound? result))
    (is (= 0 (:iterations result)))))

(deftest beta-zero-is-not-defaulted-test
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"beta-prior is required"
       (apply policy-precision/converge-beta
              [nil [0.0 1.0] [0.0 0.0]])))
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"beta-prior is required"
       (policy-precision/converge-beta 0.0 [0.0 1.0] [0.0 0.0]))))

(deftest higher-gamma-sharpens-the-efe-only-policy-distribution-test
  (let [sharp (policy-precision/converge-beta 0.5 [0.0 1.0] [0.0 0.0])
        diffuse (policy-precision/converge-beta 2.0 [0.0 1.0] [0.0 0.0])]
    (testing "with equal F_pi, higher gamma puts more mass on lower G"
      (is (> (:gamma sharp) (:gamma diffuse)))
      (is (> (first (:pi sharp)) (first (:pi diffuse)))))))

;; ---------------------------------------------------------------------------
;; Owner review (claude-20, 2026-09-01). The delivered measurement reported
;; beta_0 = 2.0 as "did not converge"; it converges at 258 iterations, two past
;; the then-default bound of 256, with the same gamma to eight decimals. What
;; the field actually shows is that the iteration count grows steeply with
;; beta, and that the step size has a stability boundary that moves with beta.

(deftest bound-two-short-is-not-non-convergence-test
  (let [g-values (mapv #(+ 0.25 (* 0.004 %)) (range 110))
        f-pi-values (mapv #(+ -18.0 (* 0.03 (mod % 17))) (range 110))
        truncated (policy-precision/converge-beta
                   1.0 g-values f-pi-values {:max-iterations 3})
        full (policy-precision/converge-beta 1.0 g-values f-pi-values)]
    (testing "a bound set below the required count reports :converged? false"
      (is (false? (:converged? truncated)))
      (is (:hit-bound? truncated)))
    (testing "and the same input converges once the bound allows it"
      (is (:converged? full))
      (is (> (:iterations full) (:iterations truncated))))
    (testing "the truncated gamma is close but NOT equal — so a caller that
              ignores :converged? gets a plausible wrong number, which is why
              the flag is data rather than a log line"
      (is (not= (:gamma truncated) (:gamma full))))))

(deftest beta-ceiling-catches-upward-divergence-test
  ;; beta -> 0 gives infinite gamma and is floored. beta -> infinity gives
  ;; gamma -> 0 and a near-uniform pi, which looks like an answer. Measured on
  ;; the real-shaped field at :step-size 4.0, beta reached 2e12 and gamma
  ;; 5e-13 without any exception, because those values are finite.
  (let [g-values (mapv #(+ 0.25 (* 0.004 %)) (range 110))
        f-pi-values (mapv #(+ -18.0 (* 0.03 (mod % 17))) (range 110))
        result (policy-precision/converge-beta
                0.5 g-values f-pi-values
                {:step-size 4.0 :max-iterations 5000 :beta-ceiling 1.0e6})]
    (is (:beta-ceiling-hit? result)
        "upward divergence is recorded, not left as a plausible small gamma")
    (is (pos? (:beta-ceiling-hit-count result)))
    (is (false? (:converged? result)))
    (is (<= (:beta-posterior result) 1.0e6))))

(deftest beta-ceiling-must-exceed-the-floor-test
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"beta-ceiling"
       (policy-precision/converge-beta
        1.0 [0.0 1.0] [0.0 0.0] {:beta-floor 1.0 :beta-ceiling 0.5}))))
