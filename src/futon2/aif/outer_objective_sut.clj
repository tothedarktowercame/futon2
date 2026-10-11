(ns futon2.aif.outer-objective-sut
  "Controlled SUT composition for the outer-objective certificate.

  This namespace is intentionally not referenced by the live selector. Its
  no-argument/default production shape refuses before reading registries because
  no production objective registration has been authorized."
  (:require [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-live-outer-selector :as live]
            [futon2.aif.outer-objective-certificate :as objective]))

(def schema :wm/outer-objective-sut-v1)

(defn- digest [x]
  (field/sha256 (.getBytes (pr-str x) "UTF-8")))

(defn candidate->task-input
  "Retain every normalized production channel as an exact objective input.
  The source pin is evidence identity; observation and value are separately
  digested. No absent channel is invented or zero-filled."
  [registration freshness-epoch candidate]
  (let [channels
        (mapv (fn [[channel-id value]]
                {:channel/id channel-id
                 :channel/observation
                 [channel-id (digest (:observation value))]
                 :channel/evidence
                 [channel-id (get-in value [:source :path])
                  (get-in value [:source :sha256])]
                 :channel/value-digest [channel-id (digest (:value value))]
                 :channel/value (:value value)})
              (sort-by (comp str key) (:channels candidate)))
        input {:task/id (:id candidate) :task/channels channels
               :task/freshness-epoch freshness-epoch}]
    (assoc input :task/dependencies
           (objective/required-dependencies registration input))))

(defn run-controlled
  "Enumerate and normalize through production namespaces, then build a strict
  objective frontier. Registries/objective authority are injected only here.

  With no sealed registry and registration this refuses before enumeration."
  [{:keys [sealed-registry registration selector-authority snapshot
           freshness-epoch prior delta enumerate-fn]
    :or {freshness-epoch 0 prior []
         enumerate-fn live/live-registry-tasks}}]
  (if-not (and sealed-registry registration selector-authority)
    {:schema schema :status :refused
     :reason :production-outer-objective-registration-absent}
    (let [tasks (vec (enumerate-fn))
          candidates (live/task-state-candidates tasks snapshot)
          enumerated (set (map :id tasks))
          candidate-population (set (map :id candidates))
          inputs (into {} (map (fn [candidate]
                                 [(:id candidate)
                                  (candidate->task-input registration
                                                         freshness-epoch candidate)]))
                       candidates)
          delta* (or delta {:changed-observations #{}
                            :added-tasks enumerated :removed-tasks #{}})]
      (if (not= enumerated candidate-population)
        {:schema schema :status :refused
         :reason :enumeration-channel-population-mismatch
         :enumerated enumerated :normalized candidate-population}
        (let [produced (objective/produce-refresh
                        sealed-registry selector-authority registration prior
                        delta* inputs enumerated)]
          (if (not= :valid (:status produced))
            (assoc produced :schema schema :enumerated-open-tasks enumerated)
            (let [frontier {:refresh (:refresh produced)
                            :evaluations (:evaluations produced)}
                  selection (objective/select-produced-minimum frontier)]
              {:schema schema :status (:status selection)
               :enumerated-open-tasks enumerated
               :normalized-task-inputs inputs
               :frontier frontier
               :selection selection
               :selected-task (:selected selection)
               :population-correspondence
               {:enumerated enumerated
                :current (set (map #(get-in % [:entry :task/id])
                                   (:evaluations frontier)))
                :selected (when-let [selected (:selected selection)] #{selected})}})))))))
