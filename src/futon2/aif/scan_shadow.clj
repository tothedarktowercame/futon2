(ns futon2.aif.scan-shadow
  "Pure shadow update for PROOF-2a decision 6B-8.

   Adoption is read only from the predecessor's recorded scan BMR. Learned
   channels contribute the learner's integrated likelihood once; tied channels
   contribute no likelihood and are only removed from the legacy R3 driver.
   This namespace performs no judge wiring and mutates no learner state."
  (:require [futon2.aif.scan-learn :as scan-learn]))

(def channel-map
  {:support :support-coverage
   :attack :attack-coverage
   :active-repos :active-repo-ratio
   :coupling :coupling-density
   :ticks :ticks-firing-ratio
   :sorrys :sorry-count-norm
   :mission-health :mission-health
   :annotation :annotation-health})

(defn adoption
  "Classify eligible predecessor BMR choices for the shadow.

   Eligible :learned keys are adopted and excluded from the legacy driver;
   eligible :tied keys are excluded only. Other choices and ineligible rows do
   neither. Learner-only adopted keys have no channel to exclude."
  [bmr]
  (cond
    (nil? bmr) {:status :absent :reason :no-predecessor-bmr}
    (:status bmr) {:status :absent :reason :predecessor-bmr-refused}
    :else
    (let [channels (:channels bmr)
          adopted (->> channels
                       (keep (fn [[key row]]
                               (when (and (:eligible row)
                                          (= :learned (:chosen-model row))) key)))
                       (sort-by str) vec)
          tied (->> channels
                    (keep (fn [[key row]]
                            (when (and (:eligible row)
                                       (= :tied (:chosen-model row))) key)))
                    (sort-by str) vec)]
      {:adopted adopted
       :tied tied
       :exclude-channels (into #{} (keep channel-map) (concat adopted tied))})))

(defn- finite? [x]
  (and (number? x) (Double/isFinite (double x))))

(defn- log-sum-exp [xs]
  (let [finite-xs (filter finite? xs)]
    (when (seq finite-xs)
      (let [m (apply max finite-xs)]
        (+ m (Math/log (reduce + (map #(Math/exp (- % m)) finite-xs))))))))

(defn shadow-row
  "Apply adopted learned likelihoods once to the proxy's driver-excluded row.

   Returns MU-EXCL identically when ADOPTED is empty. Otherwise computes in
   log space and returns a normalised status→double map. Zero prior mass stays
   zero. A non-finite or empty normaliser is a typed refusal."
  [{:keys [mu-excl learner-state exposures adopted]}]
  (if (empty? adopted)
    mu-excl
    (let [{:keys [log-likelihoods]}
          (scan-learn/status-log-likelihoods learner-state exposures adopted)
          log-weights (into {}
                            (for [[status mass] mu-excl]
                              [status (if (pos? mass)
                                        (+ (Math/log (double mass))
                                           (get log-likelihoods status 0.0))
                                        Double/NEGATIVE_INFINITY)]))
          log-z (log-sum-exp (vals log-weights))]
      (if-not (finite? log-z)
        {:status :refused :reason :shadow-normaliser-not-finite}
        (into {}
              (for [[status log-weight] log-weights]
                [status (if (finite? log-weight)
                          (Math/exp (- log-weight log-z))
                          0.0)]))))))
