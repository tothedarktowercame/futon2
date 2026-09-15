(ns futon2.aif.machine-belief
  "Single-entity bridge from the stored categorical belief to MachineModel v1.
   This namespace reads the post-filter, post-carry belief; it never repairs,
   normalises, averages, or otherwise rewrites it."
  (:require [futon2.aif.machine-model :as machine-model]))

(def state-support
  "The declared order of MachineBeliefState.Status.all."
  [:spawned :refined :strengthened :addressed :falsified :foreclosed :reopened])

(defn- refusal [kind path]
  {:ok false :refusal {:kind kind :path path}})

(defn belief-state-distribution
  "Read one complete entity posterior as a MachineModel v1 belief input.

   model-context requires :entity/id, :model {:id :revision}, the canonical
   :state-support, :mode :single-entity, and :policy-entities. Every policy
   must address the selected entity. The returned :belief-input is passed
   unchanged to the policy-conditioned state-predictive constructor (row 9)."
  [model-context stored-belief]
  (let [entity (:entity/id model-context)
        model (:model model-context)
        support (:state-support model-context)
        mode (:mode model-context)
        policy-entities (:policy-entities model-context)
        posterior (when (map? stored-belief) (get stored-belief entity))]
    (cond
      (not= :single-entity mode)
      (refusal :joint-construction-required [:model-context :mode])

      (or (nil? entity) (and (string? entity) (empty? entity)))
      (refusal :missing-entity-context [:model-context :entity/id])

      (not (and (map? model) (some? (:id model)) (some? (:revision model))))
      (refusal :missing-model-identity [:model-context :model])

      (not= state-support support)
      (refusal :state-support-order-mismatch [:model-context :state-support])

      (not (and (vector? policy-entities) (seq policy-entities)))
      (refusal :missing-policy-entities [:model-context :policy-entities])

      (not (every? #(= entity %) policy-entities))
      (refusal :joint-construction-required [:model-context :policy-entities])

      (not (map? stored-belief))
      (refusal :missing-belief [:stored-belief])

      (nil? posterior)
      (refusal :missing-entity [:stored-belief entity])

      :else
      (let [admission (machine-model/distribution-admission posterior support)]
        (if-not (:ok admission)
          (refusal (case (get-in admission [:refusal :kind])
                     :missing-distribution :invalid-posterior
                     :distribution-support-mismatch :posterior-support-mismatch
                     :unnormalized-row :invalid-mass
                     (get-in admission [:refusal :kind]))
                   [:stored-belief entity])
          {:ok true
           :model (:model model-context)
           :context {:entity/id entity}
           :state-support state-support
           :numeric-admission admission
           :belief-input {:mode :single-entity
                          :posteriors {entity posterior}}})))))
