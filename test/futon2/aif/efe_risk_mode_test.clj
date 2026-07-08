(ns futon2.aif.efe-risk-mode-test
  "D5a :risk-mode flag tests. NOTE: the default flipped to :kl (canonical) on
   2026-07-08 (Joe-directed); :risk-mode :hinge is now the escape hatch back to
   the historical behaviour."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]
            [futon2.aif.preferences :as pref]))

(def ^:private obs
  {:loop-health 0.9 :support-coverage 0.9 :attack-coverage 0.9
   :mission-health 0.7 :stack-pct 0.2 :consulting-pct 0.25
   :portfolio-pct 0.25 :mathematics-pct 0.2 :active-repo-ratio 0.7
   :sorry-count-norm 0.2 :coupling-density 0.2 :ticks-firing-ratio 0.0
   :annotation-health 0.9})

(def ^:private state
  {:observation obs :belief (belief/initial-belief-state [:m1 :m2])})

(def ^:private action {:type :address-sorry :target :m1 :intrinsic-value 0.1})

(deftest default-risk-mode-is-kl-canonical
  (testing "no opts ≡ explicit :risk-mode :kl — canonical is the default (2026-07-08)"
    (let [bare (efe/compute-efe state action)
          kl (efe/compute-efe state action {:risk-mode :kl})]
      (is (= (:G-risk bare) (:G-risk kl)))
      (is (= (:G-total bare) (:G-total kl)))
      (is (= :kl (:risk-mode bare)))))
  (testing ":risk-mode :hinge is the escape hatch back to the historical hinge"
    (is (= :hinge (:risk-mode (efe/compute-efe state action {:risk-mode :hinge}))))))

(deftest kl-mode-changes-only-risk
  (let [hinge (efe/compute-efe state action {:risk-mode :hinge})
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

(deftest kl-pragmatic-parity-preset
  ;; item 2 (E-KL-refinements): :kl-channel-weights :pragmatic-parity resolves to
  ;; pref/pragmatic-weights with MISSING channels contributing 0.0 (not 1.0).
  (let [cc-keys      (keys (pref/current-C))
        zero-base    (zipmap cc-keys (repeat 0.0))
        explicit     (merge zero-base pref/pragmatic-weights)   ; pragmatic values, 0 elsewhere
        risk         (fn [w] (:G-risk (efe/compute-efe state action
                                                       {:risk-mode :kl :kl-channel-weights w})))
        parity       (risk :pragmatic-parity)]
    (testing "parity ≡ pragmatic-weights over a 0.0 default (byte-identical G-risk)"
      (is (< (Math/abs (- (double parity) (double (risk explicit)))) 1e-9)))
    (testing "parity ≠ uniform default (the 0-default genuinely bites)"
      (is (not= parity (risk {}))))
    (testing "a channel absent from pragmatic-weights contributes 0 under parity"
      ;; :mathematics-pct ∈ current-C but ∉ pragmatic-weights ⇒ weight 0 under
      ;; parity; giving it weight 1.0 explicitly must move G-risk.
      (is (contains? (set cc-keys) :mathematics-pct))
      (is (not (contains? pref/pragmatic-weights :mathematics-pct)))
      (is (not= parity (risk (assoc explicit :mathematics-pct 1.0)))))
    (testing "explicit map form unchanged: uniform default still 1.0/channel"
      (is (Double/isFinite (double (risk {})))))))

(deftest kl-per-channel-temperature
  ;; item 3 (E-KL-refinements) plumbing: :c-temperature accepts scalar OR map.
  (let [cc   (keys (pref/current-C))
        risk (fn [ct] (:G-risk (efe/compute-efe state action
                                                {:risk-mode :kl :c-temperature ct})))]
    (testing "scalar path byte-identical (default scalar ≡ no :c-temperature opt)"
      (is (= (risk pref/default-c-temperature)
             (:G-risk (efe/compute-efe state action {:risk-mode :kl})))))
    (testing "a UNIFORM map equals the scalar it mirrors"
      (is (< (Math/abs (- (double (risk (zipmap cc (repeat 0.5)))) (double (risk 0.5)))) 1e-9)))
    (testing "missing-channel fallback = default-c-temperature ({} map ⇒ all default ⇒ scalar default)"
      (is (< (Math/abs (- (double (risk {})) (double (risk pref/default-c-temperature)))) 1e-9)))
    (testing "map form MOVES G-risk when a channel's T differs from the scalar"
      ;; raise :mission-health's T only; others fall back to the default 0.1
      (is (not= (risk 0.1) (risk {:mission-health 2.0}))))))
