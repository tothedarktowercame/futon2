(ns futon2.aif.run-participants
  "Assigned run roles, observed by the runner; not evidence of agent invocation."
  (:require [clojure.string :as str]))

(def roles [:author :configured-reviewer :reviewer-of-record :repair-reviewer
            :issuing-caller])

(defn identity-value [value]
  (cond
    (nil? value) {:status :absent}
    (and (string? value) (not (str/blank? value))) {:status :present :identity value}
    :else {:status :incompatible-meaning :value value}))

(defn observe!
  "Observe resolved configuration and track the actual reviewer atom through close."
  [opts]
  (let [reviewer (atom (:reviewer opts))
        state (:participants/state opts)]
    (when state
      (reset! state {:author (identity-value (:author opts))
                     :configured-reviewer (identity-value (:reviewer opts))
                     :repair-reviewer (identity-value (:repair-reviewer opts))
                     :reviewer-of-record (identity-value @reviewer)})
      (add-watch reviewer ::reviewer
                 (fn [_ _ _ value]
                   (swap! state assoc :reviewer-of-record (identity-value value)))))
    reviewer))

(defn select-reviewer! [reviewer-of-record repair-action? reviewer repair-reviewer]
  (reset! reviewer-of-record (if repair-action? repair-reviewer reviewer)))

(defn record-value [opts]
  (let [observed (some-> (:participants/state opts) deref)]
    {:schema :wm/run-participants-v1
     :roles (assoc (merge (zipmap roles (repeat {:status :not-observed})) observed)
                   :issuing-caller (or (:issuer-provenance opts)
                                       {:status :not-observed}))}))

(defn read-role
  "Old records and missing roles are unrecorded, never inferred absent."
  [record role]
  (if-not (contains? record :participants)
    {:status :not-observed}
    (if (= :wm/run-participants-v1 (get-in record [:participants :schema]))
      (get-in record [:participants :roles role] {:status :not-observed})
      {:status :incompatible-meaning})))
