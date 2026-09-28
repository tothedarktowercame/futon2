(ns futon2.aif.state-prediction-error
  "Equation 4.13 on finite policy-rollout marginals. Pure; records no state."
  (:require [clojure.set :as set]))

(def ^:private receipt
  {:schema :wm/state-prediction-error-v1
   :equation :state-prediction-error
   :lean "Proof2.StatePredictionErrorAtMachine.machineStatePredictionError"
   :tau 1
   :at :rollout-marginals
   :observation-grain :checked-tokens})

(defn- positive-finite? [x]
  (and (number? x) (pos? x) (Double/isFinite (double x))))

(defn- absent [reason & [details]]
  (merge receipt {:status :absent :reason reason} details))

(defn state-prediction-error
  "Compute epsilon(x) for every state in the carrier. TRANSITION-IN has
  arguments [s0 x], TRANSITION-OUT [x s1]. A single undefined logarithm
  makes the whole finite-step result absent, matching Lean's logUndefined."
  [{:keys [horizon likelihood transition-in transition-out
           s-prev s-current s-next carrier]}]
  (cond
    (nil? horizon) (absent :horizon-not-recorded)
    (or (not (pos-int? horizon)) (< horizon 2))
    (absent :out-of-horizon {:horizon horizon})
    :else
    (let [states (vec (sort-by pr-str (or (seq carrier)
                                          (set/union (set (keys s-prev))
                                                     (set (keys s-current))
                                                     (set (keys s-next))))))
          prev-support (filter (comp pos? val) s-prev)
          next-support (filter (comp pos? val) s-next)
          failure (or
                   (some (fn [x]
                           (when-not (positive-finite? (likelihood x))
                             {:term :likelihood :state x}))
                         states)
                   (some (fn [x]
                           (when-not (positive-finite? (get s-current x 0))
                             {:term :current-marginal :state x}))
                         states)
                   (some (fn [x]
                           (some (fn [[s0 _]]
                                   (when-not (positive-finite? (transition-in s0 x))
                                     {:term :incoming-transition :state x :support-state s0}))
                                 prev-support))
                         states)
                   (some (fn [x]
                           (some (fn [[s1 _]]
                                   (when-not (positive-finite? (transition-out x s1))
                                     {:term :outgoing-transition :state x :support-state s1}))
                                 next-support))
                         states))]
      (if failure
        (absent :log-undefined {:horizon horizon :where failure})
        (assoc receipt
               :status :present
               :horizon horizon
               :values
               (into {}
                     (for [x states]
                       [x (+ (Math/log (double (likelihood x)))
                             (reduce + (for [[s0 mass] prev-support]
                                         (* mass (Math/log (double (transition-in s0 x))))))
                             (reduce + (for [[s1 mass] next-support]
                                         (* mass (Math/log (double (transition-out x s1))))))
                             (- (Math/log (double (get s-current x)))))])))))))
