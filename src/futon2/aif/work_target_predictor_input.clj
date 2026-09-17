(ns futon2.aif.work-target-predictor-input
  "WM-02: the selected real target's current belief row, with declared domain,
   lineage and model identity, as it must appear at the production cascade
   predictor's input.

   Pure. Takes the SAME activation and store-read envelopes the scheduled tick
   uses (work-target-tick/predecessor-from-store adapts them) plus the target
   identity and the eligible registry ids, and yields the predictor-input
   envelope: the row-7 single-entity belief input with float admission, the
   target's full lineage (D identity, admission, introduced-at,
   information-cutoff, update state), the common model context, the payload
   information cutoff, and the store-head reference the belief was carried
   from.

   Refuses typed — never a uniform prior, never an invented update, never a
   backdated D:
     :store-not-established      the work-target model is not established
     :store-not-usable           damaged/pending store, or activation mismatch
     :no-committed-belief-row    store established but no committed snapshot:
                                 the historical record has no target belief
                                 row, and a later declared D must not be
                                 backdated into it
     :cutoff-not-declared        committed payload carries no information
                                 cutoff
     plus every work-target-belief/target-belief-input refusal verbatim
     (:entity-outside-registry, :registered-not-admitted, :carry-missing,
     :entity-context-mismatch and the numeric-admission kinds)."
  (:require [futon2.aif.work-target-belief :as belief]
            [futon2.aif.work-target-tick :as tick]))

(defn predictor-input
  "Read the selected target's current belief as a cascade-predictor input.
   ACTIVATION and STORE-READ have the shapes work-target-tick/build-proposal
   takes; TARGET is the exact registry id; REGISTRY-IDS the eligible ids of
   this tick (cascade-problems/substrate-targets in production)."
  [{:keys [activation store-read registry-ids target]}]
  (let [mode (tick/predecessor-from-store activation store-read)]
    (cond
      (= :inert (:mode mode))
      {:ok false :refusal {:kind :store-not-established :reason (:reason mode)}}

      (not= :state-writing (:mode mode))
      {:ok false :refusal (merge {:kind :store-not-usable}
                                 (:failure mode))}

      (= :established-no-snapshots (get-in mode [:predecessor :status]))
      {:ok false
       :refusal {:kind :no-committed-belief-row
                 :reason :backdating-forbidden}}

      :else
      (let [state (get-in mode [:predecessor :state])
            payload (get-in store-read [:snapshot :payload])
            cutoff (:information-cutoff payload)
            head (select-keys (:head store-read)
                              [:store/id :seq :snapshot-sha256 :status])
            model-context (assoc (:model-context state)
                                 :entity/id target
                                 :mode :single-entity
                                 :policy-entities [target])
            row (belief/target-belief-input model-context state registry-ids target)]
        (cond
          (false? (:ok row)) row
          (nil? cutoff) {:ok false :refusal {:kind :cutoff-not-declared
                                             :path [:snapshot :payload
                                                    :information-cutoff]}}
          :else {:ok true
                 :target target
                 :belief-input (:belief-input row)
                 :model (:model row)
                 :state-support (:state-support row)
                 :numeric-admission (:numeric-admission row)
                 :lineage (:lineage row)
                 :model-context model-context
                 :information-cutoff cutoff
                 :store-head head})))))
