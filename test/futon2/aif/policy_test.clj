(ns futon2.aif.policy-test
  "Tests for R6: action selection policy (softmax + abstain)."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.policy :as policy]))

;; ---------------------------------------------------------------------------
;; softmax-weights / selection-scores (the surviving general seam;
;; the flat selectors were removed 2026-09-17, H6b)
;; ---------------------------------------------------------------------------

(deftest softmax-weights-sum-to-one-test
  (testing "softmax-weights sum to 1.0"
    (let [w (policy/softmax-weights [0.0 0.5 1.0] 0.2)]
      (is (< (Math/abs (- 1.0 (reduce + w))) 1e-9)))))

(deftest softmax-weights-monotone-test
  (testing "lower G → higher weight (under uniform τ)"
    (let [w (policy/softmax-weights [0.0 0.5 1.0] 0.2)]
      (is (apply > w)))))

(deftest softmax-weights-empty-test
  (testing "empty input returns nil"
    (is (nil? (policy/softmax-weights [] 0.1)))))

(defn- temperature-refusal [tau]
  (try
    (policy/softmax-weights [0.0 1.0] tau)
    nil
    (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest softmax-weights-refuses-invalid-temperature-test
  (testing "zero and negative temperatures refuse before division"
    (is (= {:kind :nonpositive-temperature :temperature 0.0}
           (temperature-refusal 0.0)))
    (is (= {:kind :nonpositive-temperature :temperature -1.0}
           (temperature-refusal -1.0))))
  (testing "NaN is a typed nonfinite refusal"
    (let [r (temperature-refusal ##NaN)]
      (is (= :nonfinite-temperature (:kind r)))
      (is (Double/isNaN (:temperature r))))))

(deftest softmax-weights-production-tau-baseline-unchanged-test
  (let [baseline (edn/read-string
                  (slurp "holes/labs/wm-contract/runs/row-16-softmax-temperature-2026-09-12/positive-baseline.edn"))]
    (is (= (:weights baseline)
           (policy/softmax-weights (:g-totals baseline) (:temperature baseline))))))

(deftest softmax-weights-f-pi-flag-off-ignores-options-test
  (let [g [0.2 0.4 0.8]
        tau 0.3
        ln-e [-0.1 -0.2 -0.3]
        historical (policy/softmax-weights g tau ln-e)]
    (is (= historical
           (policy/softmax-weights
            g tau ln-e
            {:f-pi-policy-posterior? false
             :f-pi-values [:deliberately :invalid :and :misaligned]
             :f-pi-scaling :not-a-scaling})))))

(deftest softmax-weights-f-pi-enters-at-the-posterior-seam-test
  (let [g [0.0 0.0]
        f-pi [0.0 1.0]
        unscaled (policy/softmax-weights
                  g 0.5 nil
                  {:f-pi-policy-posterior? true
                   :f-pi-values f-pi
                   :f-pi-scaling :unscaled})
        by-tau (policy/softmax-weights
                g 0.5 nil
                {:f-pi-policy-posterior? true
                 :f-pi-values f-pi
                 :f-pi-scaling :by-tau})]
    (is (> (first unscaled) (second unscaled)))
    (is (> (first by-tau) (second by-tau)))
    (is (> (first by-tau) (first unscaled))
        "dividing F_pi by tau=0.5 gives it twice the posterior leverage")))

(deftest softmax-weights-f-pi-requires-typed-alignment-test
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"align"
       (policy/softmax-weights
        [0.0 1.0] 1.0 nil
        {:f-pi-policy-posterior? true :f-pi-values [0.0]})))
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"numeric"
       (policy/softmax-weights
        [0.0] 1.0 nil
        {:f-pi-policy-posterior? true :f-pi-values [:unknown]})))
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"scaling"
       (policy/softmax-weights
        [0.0] 1.0 nil
        {:f-pi-policy-posterior? true
         :f-pi-values [0.0]
         :f-pi-scaling :other}))))

;; ---------------------------------------------------------------------------
;; select-action — chosen branch
;; ---------------------------------------------------------------------------

