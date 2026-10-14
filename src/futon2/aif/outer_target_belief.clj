(ns futon2.aif.outer-target-belief
  "Pure chronological Bayesian fold at the outer target-selection grain.

  The fold only updates the selected target. Unknown evidence is retained as a
  refusal event and never treated as a zero-valued observation."
  (:require [futon2.aif.meta-field-observation :as field]))

(def schema :wm/outer-target-belief-v1)
(def prior-schema :wm/outer-target-prior-v1)

(def outcome-vocabulary
  [:closure :selected-want-progress :grounded-collateral-progress
   :typed-blocker :abstention-or-failure])

(defn prior
  "Declare a symmetric provisional Dirichlet prior. The authority label makes
  clear that these concentrations are policy input, not learned evidence."
  ([concentration] (prior concentration :implementation-provisional))
  ([concentration authority]
   {:schema prior-schema
    :authority authority
    :alpha (zipmap outcome-vocabulary
                   (repeat (count outcome-vocabulary) concentration))}))

(defn- valid-prior? [p]
  (and (= prior-schema (:schema p))
       (some? (:authority p))
       (= (set outcome-vocabulary) (set (keys (:alpha p))))
       (every? #(and (number? %) (pos? %)) (vals (:alpha p)))))

(defn- observation [row]
  (let [terminal (get-in row [:terminal-outcome :class])
        effects (:selected-want-effects row)
        counts (:counts effects)
        selected-effect (+ (get counts :reached 0) (get counts :progressed 0))]
    (cond
      (= :closure terminal)
      {:status :present :class :closure
       :evidence [[:terminal-outcome]]}

      (and (= :present (:status effects)) (pos? selected-effect))
      {:status :present :class :selected-want-progress
       :evidence [[:selected-want-effects]]}

      (= :present (get-in row [:collateral-progress :status]))
      {:status :present :class :grounded-collateral-progress
       :evidence [[:collateral-progress] [:grounded-artifact]]}

      (or (= :typed-blocker terminal)
          (= :present (get-in row [:blocker :status])))
      {:status :present :class :typed-blocker
       :evidence [[:terminal-outcome] [:blocker]]}

      (= :abstention-or-failure terminal)
      {:status :present :class :abstention-or-failure
       :evidence [[:terminal-outcome]]}

      :else
      {:status :refused :reason :outcome-evidence-insufficient
       :evidence {:terminal-class terminal
                  :selected-want-effects-status (:status effects)
                  :collateral-progress-status
                  (get-in row [:collateral-progress :status])}})))

(defn- predictive [alpha]
  (let [total (reduce + (vals alpha))]
    (into (sorted-map)
          (map (fn [outcome] [outcome (/ (get alpha outcome) total)]))
          outcome-vocabulary)))

(defn build
  "Fold a produced META outcome dataset into target-qualified posteriors.
  Dataset order is authoritative chronology."
  [{:keys [dataset prior]}]
  (cond
    (not= :produced (:status dataset))
    {:schema schema :status :refused :reason :dataset-not-produced}

    (not (valid-prior? prior))
    {:schema schema :status :refused :reason :prior-invalid}

    :else
    (let [folded
          (reduce
           (fn [{:keys [targets events seen] :as state} row]
             (let [identity [(:run/id row) (:click/id row)]
                   target (get-in row [:task :id])
                   obs (observation row)]
               (cond
                 (contains? seen identity)
                 (reduced (assoc state :conflict
                                 {:reason :observation-identity-duplicated
                                  :identity identity}))

                 (= :present (:status obs))
                 (let [before (or (get-in targets [target :alpha]) (:alpha prior))
                       after (update before (:class obs) inc)
                       prior-resources (get-in targets [target :resource-use]
                                               {:wall-clock-ms 0 :tokens 0
                                                :elapsed-observations 0
                                                :token-observations 0})
                       resources
                       (cond-> prior-resources
                         (= :present (get-in row [:elapsed :status]))
                         (-> (update :wall-clock-ms + (get-in row [:elapsed :value]))
                             (update :elapsed-observations inc))
                         (= :present (get-in row [:token-use :status]))
                         (-> (update :tokens + (get-in row [:token-use :value]))
                             (update :token-observations inc)))]
                   {:targets (assoc targets target
                                    {:target target :alpha after
                                     :posterior-predictive (predictive after)
                                     :observations (inc (get-in targets
                                                                [target :observations] 0))
                                     :resource-use resources
                                     :last-observation identity})
                    :events (conj events
                                  {:identity identity :target target
                                   :observation obs :update :applied
                                   :row-evidence
                                   (select-keys row [:selected-want-effects
                                                     :grounded-artifact
                                                     :collateral-progress
                                                     :pattern-reinforcement
                                                     :elapsed :token-use :blocker])})
                    :seen (conj seen identity)})

                 :else
                 {:targets targets
                  :events (conj events
                                {:identity identity :target target
                                 :observation obs :update :not-applied
                                 :row-evidence
                                 (select-keys row [:selected-want-effects
                                                   :grounded-artifact
                                                   :collateral-progress
                                                   :pattern-reinforcement
                                                   :elapsed :token-use :blocker])})
                  :seen (conj seen identity)})))
           {:targets {} :events [] :seen #{}}
           (:rows dataset))]
      (if-let [conflict (:conflict folded)]
        {:schema schema :status :refused :reason (:reason conflict)
         :details (dissoc conflict :reason)}
        (let [body {:schema schema :status :produced
                    :dataset-source-pin (:source-pin dataset)
                    :prior (select-keys prior [:schema :authority :alpha])
                    :targets (:targets folded)
                    :events (:events folded)
                    :counts {:rows (count (:rows dataset))
                             :applied (count (filter #(= :applied (:update %))
                                                     (:events folded)))
                             :unresolved (count (filter #(= :not-applied (:update %))
                                                        (:events folded)))}}]
          (assoc body :source-pin
                 {:path "wm://outer-target-belief-v1"
                  :sha256 (field/digest body)}))))))
