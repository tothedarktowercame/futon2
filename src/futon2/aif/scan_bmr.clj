(ns futon2.aif.scan-bmr
  "Pure per-channel model comparison for the learned raw-scan A.

   For each exposure key, `score` compares the status-specific Dirichlet model
   with (1) a status-tied Dirichlet using a uniform prior of total mass
   `scan-learn/kappa`, and (2), where declared, the exact hand-set point model.
   Delta-F is ln evidence(full) - ln evidence(reduced), so a reduction is
   favoured at `bmr/acceptance-threshold` or below.

   Binomial and multinomial coefficients are common to all three models for a
   channel and therefore cancel from every Delta-F; only beta-function and
   point-log-likelihood terms are retained here.  Results are recorded only.

   At or above the exposure floor, tied is chosen when favoured (and takes
   precedence if both reductions clear the threshold), otherwise hand-set is
   chosen when its finite Delta-F is favoured, otherwise learned is chosen.
   An impossible hand-set row is evidence against that row, not an adoption.
   Below the floor the same comparison is recorded but is never eligible."
  (:require [futon2.aif.belief :as belief]
            [futon2.aif.bmr :as bmr]
            [futon2.aif.scan-bins :as scan-bins]
            [futon2.aif.scan-learn :as scan-learn]))

(def schema :wm/scan-bmr-v1)
(def exposure-floor 20)

(def ^:private models (var-get #'scan-learn/models))

(defn- evidence [prior counts]
  (- (bmr/log-multivariate-beta (mapv + prior counts))
     (bmr/log-multivariate-beta prior)))

(defn- learned-counts [prior-state learned-state key status]
  (mapv - (get-in learned-state [:concentrations key status])
        (get-in prior-state [:concentrations key status])))

(defn- uniform-prior [n]
  (vec (repeat n (/ scan-learn/kappa n))))

(defn- predicted-outcome [key h]
  (case key
    :sorrys (if (== 1 h) :ge10 0)
    :mission-health (:bin (scan-bins/bin scan-bins/unit-5-v1 h))))

(defn- hand-set-model [key status]
  (let [{:keys [family alphabet hand-set]} (get models key)
        h (get-in belief/channel-emission-matrix [hand-set status])]
    (when (and hand-set (number? h) (<= 0 h 1))
      (case family
        :binomial {:family :binomial :theta h}
        :categorical
        (let [predicted (predicted-outcome key h)]
          {:family :categorical
           :probabilities (mapv #(if (= % predicted) 1.0 0.0) alphabet)})
        nil))))

(defn- categorical-point-evidence [counts probabilities]
  (reduce (fn [total [n p]]
            (cond
              (zero? n) total
              (zero? p) (reduced ::impossible)
              :else (+ total (* n (Math/log (double p))))))
          0.0 (map vector counts probabilities)))

(defn- point-evidence [counts {:keys [family theta probabilities]}]
  (case family
    :binomial
    (let [[success failure] counts]
      (cond
        (and (zero? theta) (pos? success)) ::impossible
        ;; Numeric equality is intentional: (= 1 1.0) is false in Clojure.
        (and (== 1 theta) (pos? failure)) ::impossible
        :else (+ (if (zero? success) 0.0 (* success (Math/log (double theta))))
                 (if (zero? failure) 0.0
                     (* failure (Math/log (- 1.0 (double theta))))))))
    :categorical (categorical-point-evidence counts probabilities)))

(defn- score-channel [prior-state learned-state channel-ticks key]
  (let [statuses (:statuses learned-state)
        prior-rows (get-in prior-state [:concentrations key])
        learned-rows (get-in learned-state [:concentrations key])
        n (count (val (first learned-rows)))
        counts (into {} (for [status statuses]
                          [status (learned-counts prior-state learned-state key status)]))
        learned-log (reduce + 0.0
                            (for [status statuses]
                              (evidence (get prior-rows status) (get counts status))))
        total-counts (apply mapv + (vals counts))
        tied-log (evidence (uniform-prior n) total-counts)
        tied {:log-evidence tied-log :delta-f (- learned-log tied-log)}
        hand-models (into {} (keep (fn [status]
                                     (when-let [m (hand-set-model key status)]
                                       [status m]))) statuses)
        hand-log (when (= (count statuses) (count hand-models))
                   (reduce
                    (fn [total status]
                      (let [term (point-evidence (get counts status)
                                                 (get hand-models status))]
                        (if (= ::impossible term)
                          (reduced ::impossible)
                          (+ total term))))
                    0.0 statuses))
        hand-set (cond
                   (empty? hand-models) {:status :no-hand-set-row}
                   (= ::impossible hand-log) {:impossible-under-hand-set true}
                   :else {:log-evidence hand-log
                          :delta-f (- learned-log hand-log)})
        tied-favoured? (<= (:delta-f tied) bmr/acceptance-threshold)
        hand-favoured? (and (number? (:delta-f hand-set))
                            (<= (:delta-f hand-set) bmr/acceptance-threshold))
        chosen (cond tied-favoured? :tied
                     hand-favoured? :hand-set
                     :else :learned)
        ticks (get channel-ticks key 0)]
    {:counts counts
     :n-admitted-ticks ticks
     :learned {:log-evidence learned-log}
     :tied tied
     :hand-set hand-set
     :chosen-model chosen
     :eligible (and (>= ticks exposure-floor) (some? chosen))}))

(defn score
  "Compare learned, tied, and declared hand-set models for every learned key.

   PRIOR-STATE and LEARNED-STATE are `scan-learn` states. CHANNEL-TICKS maps
   each exposure key to the number of admitted ticks carrying that channel."
  [prior-state learned-state channel-ticks]
  {:schema schema
   :threshold bmr/acceptance-threshold
   :exposure-floor exposure-floor
   :applied false
   :channels
   (into (sorted-map)
         (for [key (keys (:concentrations learned-state))]
           [key (score-channel prior-state learned-state channel-ticks key)]))})
