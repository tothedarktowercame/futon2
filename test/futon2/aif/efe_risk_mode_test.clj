(ns futon2.aif.efe-risk-mode-test
  "D5a :risk-mode flag tests — contract pt 3 (degrade-safe) is the load-bearing
   one: the default MUST be byte-identical to the historical behaviour."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]))

(def ^:private obs
  {:loop-health 0.9 :support-coverage 0.9 :attack-coverage 0.9
   :mission-health 0.7 :stack-pct 0.2 :consulting-pct 0.25
   :portfolio-pct 0.25 :mathematics-pct 0.2 :active-repo-ratio 0.7
   :sorry-count-norm 0.2 :coupling-density 0.2 :ticks-firing-ratio 0.0
   :annotation-health 0.9})

(def ^:private state
  {:observation obs :belief (belief/initial-belief-state [:m1 :m2])})

(def ^:private action {:type :address-sorry :target :m1 :intrinsic-value 0.1})

(deftest default-risk-mode-byte-identical
  (testing "no opts ≡ explicit :risk-mode :hinge ≡ historical output (I1)"
    (let [bare (efe/compute-efe state action)
          hinge (efe/compute-efe state action {:risk-mode :hinge})]
      (is (= (:G-risk bare) (:G-risk hinge)))
      (is (= (:G-total bare) (:G-total hinge)))
      (is (= :hinge (:risk-mode bare))))))

(deftest kl-mode-changes-only-risk
  (let [hinge (efe/compute-efe state action)
        kl (efe/compute-efe state action {:risk-mode :kl})]
    (testing "kl risk is finite and different from hinge"
      (is (Double/isFinite (:G-risk kl)))
      (is (not= (:G-risk kl) (:G-risk hinge))))
    (testing "non-risk terms untouched by the mode"
      (is (= (:G-ambiguity kl) (:G-ambiguity hinge)))
      (is (= (:G-info kl) (:G-info hinge)))
      (is (= (:G-survival kl) (:G-survival hinge)))
      (is (= (:G-structural-pressure kl) (:G-structural-pressure hinge))))
    (testing "I3 holds in kl mode too: G-core = G-risk + G-ambiguity"
      (is (= (:G-core kl) (+ (:G-risk kl) (:G-ambiguity kl)))))
    (testing "mode is self-describing in the output"
      (is (= :kl (:risk-mode kl))))))

(deftest kl-mode-channel-weights-hook
  (testing "zeroing all channel weights leaves only -intrinsic × urgency"
    (let [zeroed (efe/compute-efe state action
                                  {:risk-mode :kl
                                   :kl-channel-weights
                                   (zipmap (keys obs) (repeat 0.0))})]
      (is (< (Math/abs (- (double (:G-risk zeroed)) -0.1)) 1e-9)))))
