(require '[clojure.edn :as edn]
         '[futon2.aif.c-fold-config :as digest]
         '[futon2.aif.interoceptive-commitment :as commitment]
         '[futon2.aif.repair-obligation :as repair]
         '[futon2.aif.tripwire :as tripwire])

(def trip-path
  "data/wm-tripwires/trips/trip-de2b087f-2ec8-4ccc-a550-e1ab1b70adbd.edn")

(let [text (slurp trip-path)
      report (edn/read-string text)
      all-repairs (tripwire/repair-snapshot)
      repair-ids (into #{}
                       (keep (fn [[_ {:keys [record]}]]
                               (when (= (:trip/id report)
                                        (get-in record [:failure-data :trip/id]))
                                 (:repair/id record))))
                       all-repairs)
      matching (into {}
                     (filter (fn [[_ {:keys [record]}]]
                               (contains? repair-ids (:repair/id record))))
                     all-repairs)
      result (commitment/confidence-snapshot
              {:trip-authority
               {:source-root tripwire/default-trip-root
                :revision (digest/sha256 text) :read-status :ok
                :authority-class :production
                :records [{:path trip-path :sha256 (digest/sha256 text)
                           :record report}]}
               :repair-authority
               {:source-root repair/default-root
                :revision (digest/sha256 (pr-str matching)) :read-status :ok
                :authority-class :production :records matching}})]
  (prn {:trip/id (:trip/id report) :trip/action (:trip/action report)
        :matching-paths (vec (sort (keys matching)))
        :result (select-keys result [:schema :open-trip-ids :excluded
                                    :machine-confidence :authority])}))
