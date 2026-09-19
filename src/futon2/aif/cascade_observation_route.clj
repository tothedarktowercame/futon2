(ns futon2.aif.cascade-observation-route
  "Replay entry point joining the production scorer and selector to an EDN
   outcome record. Pure except the selector's explicit read-only habit path.
   No actuation, implicit live-store access, calibration or production admission."
  (:require [futon2.aif.efe :as efe]
            [futon2.aif.policy :as policy]))

(defn run
  "Evaluate candidate hypotheses against a supplied observation at the declared
   occurrence/horizon, then select using F/G and retain the chosen posterior
   for the next prediction. The observation is supplied, never manufactured
   from an enacted action or the preference's evidence tokens."
  [{:keys [state candidates opts selection-opts] :as request}]
  (let [base {:schema :wm/bounded-observation-route-v1
              :scope :synthetic-bounded-replay :actuated? false
              :observation-model (:observation-model opts)
              :request request}]
    (try
      (cond
        (not (contains? opts :observation-model))
        (assoc base :status :missing :kind :explicit-observation-model-required)
        (not (and (string? (:cascade-habit-path selection-opts))
                  (seq (:cascade-habit-path selection-opts))))
        (assoc base :status :missing :kind :explicit-habit-path-required)
        :else
        (let [ranked (efe/rank-actions state candidates opts)]
          (if (map? ranked)
            (merge base (select-keys ranked [:status :kind]) {:scoring ranked})
            (let [decision (policy/select-action-cascades ranked selection-opts)
                  chosen (some #(when (= (:action decision) (:action %)) %) ranked)]
              (assoc base :status :recorded :scoring ranked :selection decision
                     :outcome {:observation (:observation opts)
                               :selected-cascade (:cascade-id chosen)
                               :posterior (:posterior chosen)}
                     :next-state {:cascade-belief (:posterior chosen)})))))
      (catch clojure.lang.ExceptionInfo e
        (assoc base :status :missing :kind :selection-refused :refusal (ex-data e))))))
