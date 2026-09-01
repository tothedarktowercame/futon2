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
