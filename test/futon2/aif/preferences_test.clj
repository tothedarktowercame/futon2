(ns futon2.aif.preferences-test
  "Tests for the War Machine AIF preference / avoided-state / mode-prior data.

   Verifies structural invariants of the preference data — keys are valid
   channels, ranges are well-formed, mode-prior is a probability distribution.
   The numerical *values* are tuning choices (sourced from
   `war-machine-terminal-vocabulary.edn`); these tests guard against shape
   regressions, not value drift."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.observation :as obs]
            [futon2.aif.preferences :as pref]))

(defn- valid-range?
  "A [lo hi] pair where 0.0 ≤ lo ≤ hi ≤ 1.0."
  [v]
  (and (vector? v)
       (= 2 (count v))
       (every? number? v)
       (<= 0.0 (first v) (second v) 1.0)))

(deftest preferences-shape-test
  (testing "all preference keys are declared observation channels"
    (is (every? (set obs/observation-channels) (keys pref/preferences))
        "preference keys must be a subset of observation-channels"))
  (testing "all preference values are valid [lo hi] ranges in [0,1]"
    (is (every? valid-range? (vals pref/preferences))
        "preference values must be well-formed ranges")))

(deftest avoided-states-shape-test
  (testing "avoided-states channel keys (non :strategic-mode) are observation channels"
    (let [channel-keys (disj (set (keys pref/avoided-states)) :strategic-mode)]
      (is (every? (set obs/observation-channels) channel-keys)
          "avoided-state channel keys must be observation-channels")))
  (testing "avoided-states channel values are valid ranges"
    (let [channel-vals (for [[k v] pref/avoided-states
                             :when (not= k :strategic-mode)]
                         v)]
      (is (every? valid-range? channel-vals)
          "avoided range values must be well-formed")))
  (testing ":strategic-mode value is a keyword"
    (is (keyword? (:strategic-mode pref/avoided-states)))))

(deftest mode-prior-test
  (testing "mode-prior is a probability distribution"
    (is (every? number? (vals pref/mode-prior)))
    (is (every? #(<= 0.0 % 1.0) (vals pref/mode-prior)))
    (let [s (reduce + (vals pref/mode-prior))]
      (is (< (Math/abs (- 1.0 s)) 1e-6)
          (str "mode-prior probabilities should sum to 1.0; got " s)))))

(deftest pragmatic-weights-test
  (testing "pragmatic-weight keys are declared observation channels"
    (is (every? (set obs/observation-channels) (keys pref/pragmatic-weights))
        "pragmatic-weight keys must be observation-channels"))
  (testing "pragmatic-weights are non-negative"
    (is (every? #(<= 0.0 %) (vals pref/pragmatic-weights)))))

(deftest channel-health-signs-test
  (testing "v0.16: channel-health-signs covers the 4 R3a-likelihood channels"
    (is (= #{:annotation-health :sorry-count-norm :mission-health :active-repo-ratio}
           (set (keys pref/channel-health-signs)))
        "signs declared for the 4 R3a-covered channels"))
  (testing "signs are +1 or -1"
    (is (every? #{1 -1} (vals pref/channel-health-signs))))
  (testing ":sorry-count-norm is inverted (-1) because high values = unhealthy"
    (is (= -1 (get pref/channel-health-signs :sorry-count-norm))))
  (testing "other 3 channels are +1 (high = healthy)"
    (doseq [ch [:annotation-health :mission-health :active-repo-ratio]]
      (is (= +1 (get pref/channel-health-signs ch))))))
