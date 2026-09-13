(ns futon2.aif.interoceptive-commitment
  "Pure R20 trip/discharge snapshot for the chartered R20 -> R14 edge.

  This namespace does not read stores and does not compose task gain.  Its
  inputs are pinned outputs shaped like the trip-report reader and
  `futon2.aif.tripwire/repair-snapshot`.  The declared v1 engineering law is
  exactly 1 when no distinct genuine trip remains open and 1/2 otherwise."
  (:require [clojure.string :as str]))

(def schema :wm/interoceptive-commitment-v1)
(def law-id :wm/interoceptive-confidence-fixed-half-v1)
(def genuine-actions #{:stop-line :park-and-summon :discharge})
(def known-actions (conj genuine-actions :record))
(def terminal-statuses #{:resolved :superseded})
(def repair-statuses #{:open :awaiting-validation :resolved :superseded})
(def allowed-status-edges
  #{[:open :awaiting-validation]
    [:awaiting-validation :resolved]
    [:open :superseded]})

(defn- refuse! [reason data]
  (throw (ex-info (str "Interoceptive commitment refused: " (name reason))
                  (assoc data :refusal reason))))

(defn- safe-id? [x]
  (and (string? x) (boolean (re-matches #"[A-Za-z0-9][A-Za-z0-9._-]{0,255}" x))))

(defn- pin? [x]
  (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x))))

(defn- authority! [kind {:keys [source-root revision read-status records
                                authority-class] :as authority}]
  (when-not (and (= :ok read-status) (string? source-root)
                 (not (str/blank? source-root)) (pin? revision)
                 (contains? #{:production :test} authority-class)
                 (if (= kind :trip) (vector? records) (map? records)))
    (refuse! (if (not= :ok read-status)
               :interoceptive/authority-unavailable
               :interoceptive/unpinned-authority)
             {:authority kind :observed authority}))
  authority)

(defn- trip-entry! [{:keys [path sha256 record] :as entry}]
  (let [id (:trip/id record) action (:trip/action record)]
    (when-not (and (map? entry) (string? path) (pin? sha256) (map? record)
                   (safe-id? id) (= 1 (:trip/schema-version record)))
      (refuse! :interoceptive/malformed-trip {:entry entry}))
    (when-not (contains? known-actions action)
      (refuse! :interoceptive/unknown-mode {:trip/id id :mode action}))
    entry))

(defn- repair-rows! [records]
  (reduce-kv
   (fn [rows path {:keys [sha256 record] :as entry}]
     (let [stage (cond
                   (and (string? path) (str/starts-with? path "findings/")) :finding
                   (and (string? path) (str/starts-with? path "implementations/")) :implementation
                   (and (string? path) (str/starts-with? path "resolutions/")) :resolution
                   :else nil)]
       (when-not (and stage (string? path) (pin? sha256) (map? record)
                    (safe-id? (:repair/id record)))
         (refuse! :interoceptive/malformed-repair-record
                  {:path path :entry entry}))
       (when-not (contains? repair-statuses (:repair/status record))
         (refuse! :interoceptive/unknown-repair-status
                  {:path path :repair/id (:repair/id record)
                   :status (:repair/status record)}))
       (when-not (case stage
                   :finding (= :open (:repair/status record))
                   :implementation (= :awaiting-validation (:repair/status record))
                   :resolution (contains? terminal-statuses (:repair/status record)))
         (refuse! :interoceptive/repair-stage-status-mismatch
                  {:path path :stage stage :repair/id (:repair/id record)
                   :status (:repair/status record)}))
       (conj rows (assoc record :authority/path path :authority/sha256 sha256))))
   [] records))

(defn- index-repairs! [rows]
  (reduce
   (fn [idx row]
     (let [id (:repair/id row) old (get idx id)]
       (when (and old (= (:repair/status old) (:repair/status row)))
         (refuse! :interoceptive/contradictory-discharge
                  {:repair/id id :statuses [(:repair/status old)
                                            (:repair/status row)]}))
       (when (and old
                  (not (contains? allowed-status-edges
                                  [(:repair/status old) (:repair/status row)])))
         (refuse! :interoceptive/contradictory-discharge
                  {:repair/id id :statuses [(:repair/status old)
                                            (:repair/status row)]}))
       (assoc idx id row)))
   {} (sort-by #(if (str/starts-with? (:authority/path %) "findings/") 0
                  (if (str/starts-with? (:authority/path %) "implementations/") 1 2)) rows)))

(defn- finding-by-trip! [rows]
  (reduce
   (fn [idx row]
     (if-let [trip-id (get-in row [:failure-data :trip/id])]
       (do
         (when-not (safe-id? trip-id)
           (refuse! :interoceptive/malformed-trip-identity {:trip/id trip-id}))
         (when (contains? idx trip-id)
           (refuse! :interoceptive/contradictory-join {:trip/id trip-id}))
         (assoc idx trip-id (:repair/id row)))
       idx))
   {} (filter #(str/starts-with? (:authority/path %) "findings/") rows)))

(defn confidence-snapshot
  "Construct a versioned confidence snapshot from pinned reader-shaped data.

  `:record` reports and every report from a `:test` authority are retained in
  `:excluded`; neither can become a genuine open trip.  Every production
  stop-line/park/discharge report must join one repair finding by the durable
  trip identity.  Resolution is read from the same repair identity."
  [{:keys [trip-authority repair-authority]}]
  (let [trip-auth (authority! :trip trip-authority)
        repair-auth (authority! :repair repair-authority)
        _ (when-not (= (:authority-class trip-auth)
                       (:authority-class repair-auth))
            (refuse! :interoceptive/authority-class-mismatch
                     {:trip-authority-class (:authority-class trip-auth)
                      :repair-authority-class (:authority-class repair-auth)}))
        trips (mapv trip-entry! (:records trip-auth))
        trip-ids (mapv #(get-in % [:record :trip/id]) trips)
        _ (when-not (= (count trip-ids) (count (distinct trip-ids)))
            (refuse! :interoceptive/duplicate-trip-identity {:trip/ids trip-ids}))
        repair-rows (repair-rows! (:records repair-auth))
        repairs (index-repairs! repair-rows)
        trip->repair (finding-by-trip! repair-rows)
        production? (= :production (:authority-class trip-auth))
        _ (when (and production?
                     (seq (remove (set trip-ids) (keys trip->repair))))
            (refuse! :interoceptive/contradictory-join
                     {:finding-trip-ids (vec (remove (set trip-ids)
                                                     (keys trip->repair)))}))
        classified
        (mapv
         (fn [{:keys [record path sha256]}]
           (let [id (:trip/id record) action (:trip/action record)]
             (cond
               (not production?) {:trip/id id :reason :test-root :path path :sha256 sha256}
               (= :record action) {:trip/id id :reason :shadow-record :path path :sha256 sha256}
               :else
               (let [repair-id (get trip->repair id)]
                 (when-not repair-id
                   (refuse! :interoceptive/missing-finding-join {:trip/id id}))
                 (let [status (:repair/status (get repairs repair-id))]
                   (when-not status
                     (refuse! :interoceptive/missing-repair-join
                              {:trip/id id :repair/id repair-id}))
                   (if (contains? terminal-statuses status)
                     {:trip/id id :repair/id repair-id :reason :discharged
                      :status status :path path :sha256 sha256}
                     {:trip/id id :repair/id repair-id :status status
                      :path path :sha256 sha256}))))))
         trips)
        open (filterv #(nil? (:reason %)) classified)
        excluded (filterv :reason classified)
        factor (if (empty? open) 1 1/2)]
    {:schema schema
     :law {:id law-id :zero-open 1 :positive-open 1/2 :floor 1/2
           :authority :declared-engineering-response}
     :open-trip-ids (mapv :trip/id open)
     :open-trips open
     :excluded excluded
     :machine-confidence factor
     :authority {:trip (select-keys trip-auth [:source-root :revision :authority-class])
                 :repair (select-keys repair-auth [:source-root :revision :authority-class])}}))
