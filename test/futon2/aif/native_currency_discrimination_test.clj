(ns futon2.aif.native-currency-discrimination-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.action-proposer :as proposer]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.intrinsic-values :as iv]))

(def ^:private state
  {:observation {:loop-health 0.9
                 :support-coverage 0.9
                 :attack-coverage 0.9
                 :mission-health 0.7
                 :stack-pct 0.2
                 :consulting-pct 0.25
                 :portfolio-pct 0.25
                 :mathematics-pct 0.2
                 :active-repo-ratio 0.8
                 :sorry-count-norm 0.1
                 :coupling-density 0.2
                 :ticks-firing-ratio 0.0}
   :belief (belief/initial-belief-state [:m1])})

(use-fixtures :each
  (fn [test-fn]
    (let [original (iv/current)]
      (try
        (iv/reset-to-prior!)
        (test-fn)
        (finally (reset! iv/state original))))))

(defn- gap-actions []
  (filterv #(= :learn-action-class (:type %))
           (proposer/propose proposer/bootstrap-proposer state)))

(defn- discrimination-field []
  (efe/rank-actions state (gap-actions)
                    {:predictability-control-mode :telemetry-only
                     :homeostatic-control-mode :telemetry-only}))

(deftest harvested-posteriors-discriminate-in-native-currency
  (let [artifact (edn/read-string
                  (slurp "data/capability_zones/harvest-2026-07-19-3d.edn"))]
    (iv/rehydrate! (:records artifact))
    (let [field (discrimination-field)
          g-values (mapv :G-efe field)]
      (is (>= (count field) 5))
      (is (> (count (distinct g-values)) 1)
          "harvested load and predictive variance separate the EFE core")
      (is (every? #(= :beta-predictive (:g-ambiguity-source %)) field))
      (is (every? #(contains? % :c-zone-load) field))
      (is (true? (:passes? (runner/selection-discrimination field)))))))

(deftest prior-only-posteriors-degrade-to-flat-refusal
  (let [field (discrimination-field)]
    (is (every? #(= 0.25 (:G-ambiguity %)) field))
    (is (every? #(zero? (get-in % [:c-zone-load :mass])) field))
    (is (= 1 (count (distinct (map :G-efe field)))))
    (is (false? (:passes? (runner/selection-discrimination field))))))

(deftest non-gap-actions-remain-byte-identical
  (let [action {:type :no-op}
        before (efe/compute-efe state action)]
    (iv/rehydrate! [{:class :survey
                     :as-of "2026-07-21T00:00:00Z"
                     :alpha-post 31.0 :beta-post 4.0
                     :intrinsic-value-post 0.9
                     :n-emissions-in-window 33
                     :n-followthrough-in-window 30}])
    (testing "posterior state cannot affect a non-gap score"
      (is (= before (efe/compute-efe state action))))))
