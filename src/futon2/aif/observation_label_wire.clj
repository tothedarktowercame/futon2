(ns futon2.aif.observation-label-wire
  "Post-decision recording of declared-source checks. Failures are receipts,
   never a reason to refuse the decision or invent observation labels."
  (:require [futon2.aif.observation-labels :as labels]
            [futon2.aif.observation-label-store :as store]))

(defn check-results
  "Flatten the completed C3/C4 results; preserve evidence and resolved cutoffs."
  [provenance]
  (vec (for [occurrence (:occurrences provenance)
             [token result] (get-in occurrence [:observations :results])
             :when (contains? #{:C3 :C4} (:check result))]
         (assoc result :token token))))

(defn- dropped [provenance]
  (let [entries (mapcat
                 (fn [occurrence]
                   (concat
                    (for [[_ r] (get-in occurrence [:observations :results])
                          :when (not (contains? #{:C3 :C4} (:check r)))]
                      {:class (or (:check r) :unknown) :reason :unsupported-class})
                    (for [[_ r] (get-in occurrence [:observations :refused])]
                      {:class (or (:check r) (get-in r [:data :check])
                                  (get-in r [:data :class]) :unknown)
                       :reason :check-refused})))
                 (:occurrences provenance))]
    {:dropped (frequencies (map :class entries))
     :drop-reasons (frequencies (map :reason entries))}))

(defn record-declaration-reads!
  "Record the SAME declaration-read provenance written on the run record.
   A configured owner explicitly initializes an absent store on first use.
   No configured path means no IO. Identity/storage failures are typed receipts."
  [path provenance]
  (if (nil? path)
    {:status :absent :reason :no-label-store-configured}
    (let [results (check-results provenance)
          counts (merge {:occurrences (count (:occurrences provenance))
                         :results (count results)} (dropped provenance))]
      (try
        (let [identities (labels/loaded-identities)]
          (if (:status identities)
            (merge counts identities)
            (let [initialized (try (store/snapshot path) false
                                   (catch clojure.lang.ExceptionInfo e
                                     (if (= :store-uninitialized (:kind (ex-data e)))
                                       (do (store/init! path) true)
                                       (throw e))))]
              (merge counts (store/record! path results identities {})
                     {:status :recorded :initialized initialized}))))
        (catch Exception e
          (merge counts {:status :missing
                         :kind (or (:kind (ex-data e)) :label-store-write-failed)
                         :reason (ex-message e)}
                 (select-keys (ex-data e) [:cause :namespace :path])))))))
