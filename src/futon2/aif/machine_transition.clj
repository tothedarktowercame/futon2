(ns futon2.aif.machine-transition
  "Controlled B construction at the MachineModelSpec boundary.  This module
   does not install B in the live filter."
  (:require [futon2.aif.machine-model :as machine-model]))

(def schema :wm/controlled-transition-v1)

(defn- refusal [kind path]
  {:ok false :refusal {:kind kind :path path}})

(defn controlled-transition-kernel
  "Return the model's complete action-conditioned B, retaining its authority.
   `parameter-state` binds the model revision and makes the initial-tick rule
   explicit.  It never estimates rows or substitutes identity dynamics."
  [model action-vocabulary parameter-state]
  (let [admission (machine-model/validate model)
        b (:B model)]
    (cond
      (not (:ok admission)) admission
      (not= action-vocabulary (:actions model))
      (refusal :support-order-mismatch [:actions])
      (not= (select-keys parameter-state [:model/id :model/revision])
            {:model/id (get-in model [:model :id])
             :model/revision (get-in model [:model :revision])})
      (refusal :model-revision-mismatch [:parameter-state])
      (not (contains? parameter-state :previous-action))
      (refusal :previous-action-unspecified [:parameter-state :previous-action])
      (and (nil? (:previous-action parameter-state))
           (not= :use-initial-D-without-transition
                 (:initial-action-semantics parameter-state)))
      (refusal :initial-action-semantics-missing [:parameter-state])
      (and (some? (:previous-action parameter-state))
           (not ((set action-vocabulary) (:previous-action parameter-state))))
      (refusal :unsupported-action [:parameter-state :previous-action])
      (some (fn [[action-a action-b]]
              (every? #(= (get-in b [:rows [% action-a]])
                          (get-in b [:rows [% action-b]]))
                      (:state-support model)))
            (:declared-distinct-action-pairs parameter-state))
      (refusal :controlled-actions-indistinguishable
               [:parameter-state :declared-distinct-action-pairs])
      :else
      {:ok true :schema schema :model (:model model)
       :state-support (:state-support model) :action-support action-vocabulary
       :authority (:authority b) :name (:name b) :rows (:rows b)
       :previous-action (:previous-action parameter-state)
       :initial-action-semantics (:initial-action-semantics parameter-state)})))

(defn apply-belief
  "Apply one supported action row to a complete state distribution."
  [kernel belief action]
  (cond
    (not (:ok kernel)) kernel
    (not ((set (:action-support kernel)) action))
    (refusal :unsupported-action [:action])
    (not= (set (:state-support kernel)) (set (keys belief)))
    (refusal :belief-support-mismatch [:belief])
    :else
    {:ok true
     :distribution
     (into (array-map)
           (for [s' (:state-support kernel)]
             [s' (reduce + (for [[s mass] belief]
                              (* mass (get-in kernel [:rows [s action] s']))))]))}))
