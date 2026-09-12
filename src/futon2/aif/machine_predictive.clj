(ns futon2.aif.machine-predictive
  "Full-plan predicted-state construction. Row-6 outcome functions intentionally
   remain absent until their separate packet."
  (:require [futon2.aif.belief :as belief]
            [futon2.aif.machine-model :as machine-model]
            [futon2.aif.machine-transition :as transition]))

(def schema :wm/predicted-state-kernel-v1)

(defn- refusal [kind path]
  {:ok false :refusal {:kind kind :path path}})

(defn- policy-refusal [belief-input kernel policy]
  (cond
    (not= :single-entity (:mode belief-input))
    (refusal :joint-construction-required [:belief-input :mode])
    (not= 1 (count (:posteriors belief-input)))
    (refusal :joint-construction-required [:belief-input :posteriors])
    (not (map? policy)) (refusal :invalid-policy [:policy])
    (not (and (some? (:id policy)) (some? (:revision policy))))
    (refusal :invalid-policy [:policy :identity])
    (not= (:entity/id policy) (first (keys (:posteriors belief-input))))
    (refusal :joint-construction-required [:policy :entity/id])
    (not= (:model/revision policy) (get-in kernel [:model :revision]))
    (refusal :model-revision-mismatch [:policy :model/revision])
    (not (vector? (:actions policy))) (refusal :invalid-policy [:policy :actions])
    (empty? (:actions policy)) (refusal :empty-policy [:policy :actions])
    (not-every? (set (:action-support kernel)) (:actions policy))
    (refusal :undeclared-action [:policy :actions])
    :else nil))

(defn predicted-state-plan
  "Evaluate one complete versioned policy, retaining depth zero and every step."
  [belief-input kernel policy]
  (if-let [r (policy-refusal belief-input kernel policy)]
    r
    (let [initial (first (vals (:posteriors belief-input)))]
      (if (or (not= belief/status-set (set (keys initial)))
              (nil? (machine-model/row-sum-admission initial)))
        (refusal :invalid-mass [:belief-input :posteriors (:entity/id policy)])
        (loop [actions (:actions policy) depth 0 current initial
               steps [{:depth 0 :distribution initial
                       :admission (machine-model/row-sum-admission initial)}]]
          (if-let [action (first actions)]
            (let [applied (transition/apply-belief kernel current action)
                  next-row (:distribution applied)
                  admission (when (:ok applied)
                              (machine-model/row-sum-admission next-row))]
              (cond
                (not (:ok applied)) applied
                (nil? admission) (refusal :invalid-mass [:steps (inc depth)])
                :else (recur (next actions) (inc depth) next-row
                             (conj steps {:depth (inc depth) :action action
                                          :distribution next-row
                                          :admission admission}))))
            {:ok true :schema schema :model (:model kernel)
             :policy (select-keys policy [:id :revision :entity/id :model/revision :actions])
             :state-support (:state-support kernel)
             :steps steps :terminal current}))))))

(defn predicted-state-kernel
  "Evaluate all supplied policies under one belief and controlled kernel."
  [belief-input kernel policies]
  (cond
    (not (:ok kernel)) kernel
    (not (and (vector? policies) (seq policies)))
    (refusal :missing-policy-support [:policies])
    :else
    (let [rows (mapv #(predicted-state-plan belief-input kernel %) policies)]
      (if-let [failed (first (remove :ok rows))]
        failed
        {:ok true :schema schema :model (:model kernel)
         :state-support (:state-support kernel)
         :policies (mapv :policy rows)
         :rows (into {} (map (juxt #(get-in % [:policy :id]) :terminal) rows))
         :trajectories (into {} (map (juxt #(get-in % [:policy :id]) :steps) rows))}))))
