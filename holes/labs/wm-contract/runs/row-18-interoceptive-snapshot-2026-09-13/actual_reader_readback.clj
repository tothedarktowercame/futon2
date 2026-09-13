(require '[clojure.edn :as edn]
         '[futon2.aif.c-fold-config :as digest]
         '[futon2.aif.interoceptive-commitment :as commitment]
         '[futon2.aif.repair-obligation :as repair]
         '[futon2.aif.tripwire :as tripwire])

(def trip-path
  "data/wm-tripwires/trips/trip-de2b087f-2ec8-4ccc-a550-e1ab1b70adbd.edn")

(defn strict-read [text]
  (with-open [reader (java.io.PushbackReader. (java.io.StringReader. text))]
    (let [value (edn/read {:eof ::empty} reader)]
      (when (or (= ::empty value) (not= ::end (edn/read {:eof ::end} reader)))
        (throw (ex-info "Trip artifact is not exactly one EDN form"
                        {:refusal :interoceptive/strict-read-failed})))
      value)))

(let [text (slurp trip-path)
      report (strict-read text)
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
      trip-sha (digest/sha256 text)
      result (commitment/confidence-snapshot
              {:trip-authority
               {:source-root tripwire/default-trip-root
                :revision trip-sha :read-status :ok
                :authority-class :production
                :records [{:path trip-path :sha256 trip-sha
                           :record report}]}
               :repair-authority
               {:source-root repair/default-root
                :revision (digest/sha256 (pr-str matching)) :read-status :ok
                :authority-class :production :records matching}})]
  (assert (= trip-sha (digest/sha256 (slurp trip-path))))
  (assert (= 3 (count matching)))
  (assert (= 1 (:machine-confidence result)))
  (prn {:trip/id (:trip/id report) :trip/action (:trip/action report)
        :matching-paths (vec (sort (keys matching)))
        :result (select-keys result [:schema :open-trip-ids :excluded
                                    :machine-confidence :authority])}))
