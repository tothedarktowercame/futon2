(ns futon2.aif.free-energy-test
  "Tests for the War Machine AIF free-energy computation and mode inference."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.free-energy :as fe]
            [futon2.aif.observation :as obs]))

(def ^:private healthy-obs
  "Observation vector that falls within preferred ranges on weighted channels."
  {:loop-health 0.9
   :support-coverage 0.9
   :attack-coverage 0.9
   :mission-health 0.7
   :stack-pct 0.20
   :consulting-pct 0.25
   :portfolio-pct 0.25
   :mathematics-pct 0.20
   :active-repo-ratio 0.8
   :sorry-count-norm 0.1
   :coupling-density 0.2
   :ticks-firing-ratio 0.0
   :depositing-signal 0.1})

(def ^:private hermit-obs
  "Observation matching the hermit avoidance pattern."
  {:loop-health 0.5
   :support-coverage 0.5
   :attack-coverage 0.5
   :mission-health 0.5
   :stack-pct 0.85
   :consulting-pct 0.0
   :portfolio-pct 0.05
   :mathematics-pct 0.10
   :active-repo-ratio 0.5
   :sorry-count-norm 0.2
   :coupling-density 0.1
   :ticks-firing-ratio 0.0
   :depositing-signal 0.0})

(deftest compute-controller-diagnostics-shape-test
  (testing "compute-controller-diagnostics returns the documented keys"
    (let [g (fe/compute-controller-diagnostics {})]
      (is (contains? g :controller-score))
      (is (contains? g :preference-gap-score))
      (is (contains? g :coverage-uncertainty-pressure))
      (is (contains? g :per-channel))
      (is (contains? g :avoided-active))))
  (testing "controller-score respects the 0.65 / 0.35 mix"
    (let [g (fe/compute-controller-diagnostics hermit-obs)
          expected (+ (* 0.65 (:preference-gap-score g))
                      (* 0.35 (:coverage-uncertainty-pressure g)))]
      (is (< (Math/abs (- expected (:controller-score g))) 1e-9)
          "controller-score = 0.65*pragmatic + 0.35*epistemic")))
  (testing "G components are non-negative for in-distribution input"
    (let [g (fe/compute-controller-diagnostics healthy-obs)]
      (is (>= (:preference-gap-score g) 0.0))
      (is (>= (:coverage-uncertainty-pressure g) 0.0))
      (is (>= (:controller-score g) 0.0)))))

(deftest compute-controller-diagnostics-per-channel-test
  (testing "per-channel entries cover every preference key"
    (let [g (fe/compute-controller-diagnostics healthy-obs)
          ch-keys (set (keys (:per-channel g)))]
      (is (every? ch-keys
                  (->> (fe/compute-controller-diagnostics healthy-obs)
                       :per-channel keys set)))))
  (testing "in-range observations yield zero gap"
    (let [g (fe/compute-controller-diagnostics healthy-obs)
          loop-entry (get-in g [:per-channel :loop-health])]
      (is (true? (:in-range? loop-entry)))
      (is (zero? (:gap loop-entry))))))

(deftest avoided-active-test
  (testing "healthy observation triggers no avoided ranges"
    (is (empty? (:avoided-active (fe/compute-controller-diagnostics healthy-obs)))))
  (testing "hermit observation triggers stack-pct + consulting-pct avoidance"
    (let [avoided (set (:avoided-active (fe/compute-controller-diagnostics hermit-obs)))]
      (is (contains? avoided :stack-pct)
          "stack-pct 0.85 falls in avoided [0.7 1.0]")
      (is (contains? avoided :consulting-pct)
          "consulting-pct 0.0 falls in avoided [0.0 0.0]"))))

(defn- with-alive-defaults
  "Avoid the :dark branch (active < 0.2 AND loop-health < 0.3) for cases
   that want to test other mode classifications."
  [m]
  (merge {:active-repo-ratio 0.5 :loop-health 0.5} m))

(deftest infer-mode-test
  (testing "dark mode: nothing happening"
    (is (= :dark (fe/infer-mode {:active-repo-ratio 0.1 :loop-health 0.2}))))
  (testing "depositing: consulting activity"
    (is (= :depositing
           (fe/infer-mode (with-alive-defaults {:consulting-pct 0.3 :stack-pct 0.4})))))
  (testing "hermit: stack-dominated, no consulting, no depositing"
    (is (= :hermit
           (fe/infer-mode (with-alive-defaults
                            {:stack-pct 0.85 :consulting-pct 0.0 :depositing-signal 0.0})))))
  (testing "scanning: stack-dominated but daily scans active"
    (is (= :scanning
           (fe/infer-mode (with-alive-defaults
                            {:stack-pct 0.8 :consulting-pct 0.0 :depositing-signal 0.05})))))
  (testing "foraging-trapped: stack + ticks firing"
    (is (= :foraging-trapped
           (fe/infer-mode (with-alive-defaults
                            {:stack-pct 0.6 :consulting-pct 0.05 :ticks-firing-ratio 0.6
                             :depositing-signal 0.0})))))
  (testing "multiplied: catch-all when no other branch fires"
    (is (= :multiplied
           (fe/infer-mode {:stack-pct 0.2 :consulting-pct 0.2 :portfolio-pct 0.3
                           :loop-health 0.8 :active-repo-ratio 0.7
                           :ticks-firing-ratio 0.0 :depositing-signal 0.0})))))

(deftest pipeline-integration-test
  (testing "observe → compute-controller-diagnostics composes for empty scan data"
    (let [o (obs/observe {})
          g (fe/compute-controller-diagnostics o)]
      (is (map? g))
      (is (number? (:controller-score g))))))

;; ---------------------------------------------------------------------------
;; v0.10: compute-prediction-error (R3a + R3b)
;; ---------------------------------------------------------------------------

(deftest compute-prediction-error-shape-test
  (testing "compute-prediction-error returns documented fields"
    (let [e (fe/compute-prediction-error 0.5 {:mean 0.4 :variance 0.01})]
      (is (contains? e :observed))
      (is (contains? e :predicted-mean))
      (is (contains? e :predicted-variance))
      (is (contains? e :error))
      (is (contains? e :precision))
      (is (contains? e :weighted-error)))))

(deftest compute-prediction-error-positive-test
  (testing "observed > predicted → positive error and positive weighted-error"
    (let [e (fe/compute-prediction-error 0.6 {:mean 0.4 :variance 0.04})]
      (is (< (Math/abs (- 0.2 (:error e))) 1e-9))
      (is (= 25.0 (:precision e)) "precision = 1 / variance for variance > min")
      (is (< (Math/abs (- 5.0 (:weighted-error e))) 1e-9)))))

(deftest compute-prediction-error-negative-test
  (testing "observed < predicted → negative error"
    (let [e (fe/compute-prediction-error 0.3 {:mean 0.5 :variance 0.04})]
      (is (= -0.2 (:error e)))
      (is (neg? (:weighted-error e))))))

(deftest compute-prediction-error-min-variance-floor-test
  (testing "min-variance floor prevents division-by-zero when variance = 0"
    (let [e (fe/compute-prediction-error 0.5 {:mean 0.4 :variance 0.0})]
      ;; precision = 1 / 0.01 (default min-variance) = 100
      (is (= 100.0 (:precision e)))
      (is (not (Double/isInfinite (:weighted-error e)))))))

(deftest compute-prediction-error-min-variance-custom-test
  (testing "custom min-variance opt is respected"
    (let [e (fe/compute-prediction-error 0.5 {:mean 0.4 :variance 0.0}
                                          {:min-variance 0.5})]
      (is (= 2.0 (:precision e)) "precision = 1 / custom min-variance"))))

(deftest compute-prediction-error-defaults-test
  (testing "missing observed defaults to 0.0"
    (let [e (fe/compute-prediction-error nil {:mean 0.5 :variance 0.04})]
      (is (= 0.0 (:observed e))))))

(deftest compute-variational-free-energy-test
  (testing "F is half the mean precision-weighted squared prediction error"
    (is (= 2.5
           (fe/compute-variational-free-energy
            {:a {:error 1.0 :precision 2.0}
             :b {:error -2.0 :precision 2.0}}))))
  (testing "F is non-negative and zero only for zero error"
    (is (zero? (fe/compute-variational-free-energy
                {:a {:error 0.0 :precision 10.0}}))))
  (testing "missing and invalid terms fail closed"
    (is (thrown? clojure.lang.ExceptionInfo
                 (fe/compute-variational-free-energy {})))
    (is (thrown? clojure.lang.ExceptionInfo
                 (fe/compute-variational-free-energy
                  {:a {:error 1.0 :precision -1.0}})))))
