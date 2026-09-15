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
    (let [initial (first (vals (:posteriors belief-input)))
          support (:state-support kernel)
          initial-admission (machine-model/distribution-admission initial support)
          step (fn [depth row admission]
                 {:depth depth :distribution row :model (:model kernel)
                  :admission (:admission admission) :numeric-admission admission})
          failure (fn [admission path]
                    ;; Preserve newly enforced support-shape failures. Existing
                    ;; row coverage/mass failures retain :invalid-mass.
                    (let [kind (get-in admission [:refusal :kind])]
                      (refusal (if (#{:missing-support :duplicate-support :unsupported-numeric-type} kind)
                                 kind :invalid-mass) path)))]
      (if (or (not (:ok initial-admission)) (not= belief/status-set (set support)))
        (failure initial-admission [:belief-input :posteriors (:entity/id policy)])
        (loop [actions (:actions policy) depth 0 current initial
               steps [(step 0 initial initial-admission)]]
          (if-let [action (first actions)]
            (let [applied (transition/apply-belief kernel current action)
                  next-row (:distribution applied)
                  admission (when (:ok applied)
                              (machine-model/distribution-admission next-row support))]
              (cond
                (not (:ok applied)) applied
                (not (:ok admission)) (failure admission [:steps (inc depth)])
                :else (recur (next actions) (inc depth) next-row
                             (conj steps (assoc (step (inc depth) next-row admission) :action action)))))
            {:ok true :schema schema :model (:model kernel)
             :policy (select-keys policy [:id :revision :entity/id :model/revision :actions])
             :state-support support
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

(defn declared-outcome-a
  "Construct the named declared state→tagged-outcome prior. This is not the
   legacy seven-event lifecycle likelihood and makes no measured claim."
  [model]
  (let [states (:state-support model)
        outcomes (get-in model [:outcome :support])]
    (cond
      (not= (:outcome model) (machine-model/outcome-authority))
      (refusal :outcome-authority-mismatch [:outcome])
      (empty? outcomes) (refusal :missing-a-support [:outcome :support])
      :else
      {:ok true :authority :declared-prior :name "wm-state-outcome-prior-v1"
       :rows (into {} (map-indexed
                       (fn [i state]
                         [state (assoc (zipmap outcomes (repeat 0))
                                       (nth outcomes (mod i (count outcomes))) 1)])
                       states))})))

(defn predictive-outcome-kernel
  "Compose row 9 terminal states with the admitted machine A over a full plan."
  [model belief-input kernel policies]
  (cond
    (= :evidence (:outcome-vertex model))
    {:ok false :refusal {:kind :evidence-vocabulary-owed
                         :path [:outcome :vertices :evidence]}}
    (not= (:model model) (:model kernel))
    (refusal :model-revision-mismatch [:model])
    (not= :declared-prior (get-in model [:A :authority]))
    (refusal :undeclared-authority [:A :authority])
    (not= (set (:state-support kernel)) (set (keys (get-in model [:A :rows]))))
    (refusal :missing-a-support [:A :rows])
    :else
    (let [predicted (predicted-state-kernel belief-input kernel policies)
          outcomes (get-in model [:outcome :support])]
      (if-not (:ok predicted)
        predicted
        (let [rows (into {}
                         (for [[id q] (:rows predicted)]
                           [id (into (array-map)
                                     (for [o outcomes]
                                       [o (reduce + (for [[s mass] q]
                                                        (* mass (get-in model [:A :rows s o]))))]))]))]
          (if (some nil? (for [s (:state-support kernel) o outcomes]
                           (get-in model [:A :rows s o])))
            (refusal :missing-a-support [:A :rows])
            {:ok true :schema :wm/predictive-outcome-kernel-v1
             :model (:model model) :authority (select-keys (:A model) [:authority :name])
             :support outcomes :policy-horizons (into {} (map (juxt :id #(count (:actions %))) policies))
             :state-predictions (:rows predicted) :rows rows
             :pins {:model (:model model) :state-support (:state-support model)
                    :outcome-support outcomes}}))))))
