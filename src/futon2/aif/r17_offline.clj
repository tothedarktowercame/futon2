(ns futon2.aif.r17-offline
  "Pure, replayable R17 structure-learning run envelopes.

   This namespace records the existing A4a/BMR concept-reduction decision. It
   performs no substrate writes and has no clock, random, HTTP, or scheduler
   dependency."
  (:require [futon2.aif.a4a :as a4a]
            [futon2.aif.bmr :as bmr]))

(def envelope-version
  "Version of the replay contract emitted by `run`."
  1)

(defn- require-parent-model!
  [parent-model]
  (when-not (and (map? parent-model) (some? (:id parent-model)))
    (throw (ex-info "R17 offline runs require a parent model with an :id"
                    {:parent-model parent-model})))
  parent-model)

(defn- parent-structure
  [{:keys [capabilities outcomes concentrations]}]
  {:concepts capabilities
   :equivalence-classes
   (into (sorted-map)
         (map (fn [capability] [capability [capability]]))
         capabilities)
   :concept-concentrations concentrations
   :outcomes outcomes})

(defn- resulting-structure
  [{:keys [concepts equivalence-classes concept-concentrations outcomes] :as reduced}]
  {:concepts concepts
   :equivalence-classes equivalence-classes
   :capability->concept (a4a/capability->concept reduced)
   :concept-concentrations concept-concentrations
   :outcomes outcomes})

(defn- proposal-receipt
  [{:keys [pair reduced-posterior delta-F accept?]}]
  {:reduction {:kind :merge-capability-concepts
               :members pair}
   :evidence {:statistic :delta-F
              :value delta-F
              :threshold bmr/acceptance-threshold
              :accept-when :less-than-or-equal}
   :decision (if accept? :accept :reject)
   :reduced-posterior reduced-posterior})

(defn- canonical-input
  [corpus-or-concentrations]
  (let [{:keys [capabilities outcomes concentrations prior]}
        (if (and (map? corpus-or-concentrations)
                 (:capabilities corpus-or-concentrations)
                 (:outcomes corpus-or-concentrations)
                 (:concentrations corpus-or-concentrations))
          corpus-or-concentrations
          (a4a/corpus->concentration corpus-or-concentrations))]
    {:capabilities capabilities
     :outcomes outcomes
     :concentrations concentrations
     :prior prior}))

(defn run
  "Run R17 offline and return a deterministic, replayable decision envelope.

   Input:
   {:run-id       stable campaign-local identifier
    :parent-model {:id stable-parent-id, ...identity metadata...}
    :corpus        A4a corpus accepted by `a4a/corpus->concentration`}

   Every pairwise proposal records its BMR delta-F and threshold. The result is
   either `:structure-reduced` or a `:principled-no-change`; both carry the
   structure a later proposal-construction tick should consume."
  [{:keys [run-id parent-model corpus]}]
  (let [parent-model (require-parent-model! parent-model)
        input (canonical-input corpus)
        reduced (a4a/reduce-concepts input)
        proposals (mapv proposal-receipt (:merge-scores reduced))
        accepted? (boolean (some #(= :accept (:decision %)) proposals))
        decision (if accepted?
                   {:outcome :structure-reduced
                    :reason :one-or-more-reductions-met-evidence-threshold}
                   {:outcome :principled-no-change
                    :reason :no-reduction-met-evidence-threshold})]
    {:r17/envelope-version envelope-version
     :r17/run-id run-id
     :r17/parent {:model parent-model
                  :structure (parent-structure input)}
     :r17/proposals proposals
     :r17/decision decision
     :r17/resulting-structure (resulting-structure reduced)
     :r17/replay {:entrypoint 'futon2.aif.r17-offline/replay
                  :input {:run-id run-id
                          :parent-model parent-model
                          :corpus input}}}))

(defn replay
  "Re-run an R17 envelope solely from its recorded replay input."
  [envelope]
  (run (get-in envelope [:r17/replay :input])))
