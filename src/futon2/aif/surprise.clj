(ns futon2.aif.surprise
  "Record-only mismatches to selection-frozen positive-support expectations.
   Incidents without this expectation/comparison carrier are not surprises."
  (:require [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.time Instant]))

(load-identity/register! *ns* *file*)

(defn- instant [x]
  (try (when (string? x) (Instant/parse x)) (catch Exception _ nil)))

(defn records
  "Historical observations can lack an exact timestamp; retain that absence.
   The caller supplies the durable selection time and post-build observation.
   A known reversed clock, missing declaration time or missing occurrence cannot
   establish D1. This records no revision, parameter update or causal proof."
  [{:keys [comparison occurrence declared-at observed-at]}]
  (let [p (:prediction comparison)
        declared (instant declared-at)
        observed (instant observed-at)]
    (if-not (and (= :wm/token-outcome-comparison-v1 (:schema comparison))
                 (= :compared (:status comparison)) (= :frozen (:status p))
                 (= :positive-marginal-support (:prediction-rule p))
                 (:action/id occurrence) declared
                 (or (nil? observed-at) (and observed (.isBefore declared observed))))
      []
      (vec
       (for [{:keys [token predicted observed verdict measurement]} (:tokens comparison)
             :when (and (number? predicted) (boolean? observed)
                        (or (and (= :predicted-not-observed verdict) (pos? predicted) (false? observed))
                            (and (= :not-predicted-observed verdict) (zero? predicted) (true? observed))))
             :let [expectation-digest (identity/digest {:prediction p :token token})
                   produced? (contains? (:intended-outputs p) token)
                   model-part (cond (= :not-predicted-observed verdict) :D-or-external
                                    produced? :B-effect :else :D-prediction)]]
         {:schema :wm/surprise-v1
          :surprise/id (str "surprise-" (identity/digest
                                       {:occurrence occurrence :token token
                                        :expectation-digest expectation-digest}))
          :occurrence occurrence :token token :verdict verdict
          :model-part model-part
          :model-part-reason (case model-part
                               :B-effect :declared-produced-token-not-observed
                               :D-or-external :observed-outside-predicted-support-cause-unattributed
                               :D-prediction :predicted-state-token-not-a-declared-effect)
          :expectation {:id (str "expectation-" expectation-digest)
                        :digest expectation-digest :declared-at declared-at
                        :scope {:target (:target p) :token token :horizon (:horizon p)}
                        :rule :positive-marginal-support :predicted predicted
                        :tolerance {:expected-observed (pos? predicted)}}
          :observation {:value observed :evidence-digest (identity/digest measurement)
                        :artifact-sha (:artifact-sha comparison)
                        :observed-at (or observed-at {:status :not-recorded})
                        :placement :post-build-artifact-observation}
          :causal-attribution :not-established
          :revision {:status :none-yet}})))))
