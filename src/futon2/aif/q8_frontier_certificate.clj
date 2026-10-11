(ns futon2.aif.q8-frontier-certificate
  "Pure projection of accepted controlled-SUT evidence into Lean Q8's carrier."
  (:require [futon2.aif.outer-objective-certificate :as outer]))

(def schema :wm/q8-frontier-certificate-v1)

(defn- entry [evaluation]
  (let [e (:entry evaluation)]
    {:task (:task/id e)
     :observation (:observation/digest e)
     :evidence (:evidence/digest e)
     :ranking-law (:ranking-law e)
     :ranking-version (:ranking-version e)
     :dependencies (:dependencies e)
     :comparison-key (:comparison-key e)
     :freshness-epoch (:freshness-epoch e)}))

(defn- policy-id [target candidate]
  [target (:id candidate) (mapv :id (:precedence candidate))])

(defn- declared-id [target candidate]
  [target (:candidate-id candidate) (vec (:precedence candidate))])

(defn project
  "Derive Q8 exclusively from retained outer/frontier and cascade-stage facts."
  [{:keys [outer-objective efe-input construction scored]}]
  (let [refresh (get-in outer-objective [:frontier :refresh])
        selection (get outer-objective :selection)
        enumerated (set (:enumerated-open-tasks outer-objective))
        current (set (:current-open-tasks refresh))
        selected (:selected-task outer-objective)
        target (get-in efe-input [:problem :target])
        current-evaluations (:current refresh)
        selected-evaluation (:evaluation selection)
        available (set (map #(policy-id target %)
                            (get-in efe-input [:preference-input :candidates])))
        constructed (set (map #(declared-id target %)
                              (get-in efe-input [:problem :constructed-candidates])))
        admitted-identity (get-in construction [:composition :admitted-identity])
        admitted (set (map #(declared-id (:target admitted-identity) %)
                           (:candidates admitted-identity)))
        scored-ids (set (map #(policy-id target (:action %))
                             (get-in scored [:preference :scored])))
        posterior-map (get-in scored [:posterior :selection-law :posterior])
        posterior (set (map #(policy-id target %) (keys posterior-map)))
        errors (cond-> []
                 (not (outer/refresh-valid? refresh)) (conj :outer-refresh-invalid)
                 (not= enumerated current) (conj :enumerated-current-population-mismatch)
                 (not= enumerated (set (get-in outer-objective
                                                [:population-correspondence :current])))
                 (conj :outer-population-correspondence-mismatch)
                 (not= #{selected} (set (get-in outer-objective
                                                 [:population-correspondence :selected])))
                 (conj :selected-population-mismatch)
                 (not= selected target) (conj :selected-target-mismatch)
                 (not= selected-evaluation
                       (first (filter #(= selected (get-in % [:entry :task/id]))
                                      current-evaluations)))
                 (conj :selected-entry-not-current)
                 (not= (:comparison-key selection)
                       (get-in selected-evaluation [:entry :comparison-key]))
                 (conj :selected-comparison-key-mismatch)
                 (not (every? seq [available constructed admitted scored-ids posterior]))
                 (conj :empty-policy-population)
                 (not (apply = [available constructed admitted scored-ids posterior]))
                 (conj :target-local-policy-population-mismatch))]
    (if (seq errors)
      {:status :refused :reason :q8-frontier-certificate-invalid :errors errors}
      {:status :valid
       :certificate
       {:schema schema
        :frontier-refresh
        {:prior (mapv entry (:prior refresh))
         :current (mapv entry (:current refresh))
         :retained (mapv entry (:retained refresh))
         :invalidated (mapv entry (:invalidated refresh))
         :recomputed (mapv entry (:recomputed refresh))
         :removed (set (:removed refresh))
         :delta (:delta refresh)
         :current-open-tasks current}
        :frontier-selection
        {:selected (entry selected-evaluation)
         :current-epoch (get-in selected-evaluation [:entry :freshness-epoch])}
        :selected-cascade
        {:selected-task selected :available-admissible available
         :constructed constructed :admitted admitted
         :scored scored-ids :posterior posterior}}})))
