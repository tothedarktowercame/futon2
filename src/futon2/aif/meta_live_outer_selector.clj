(ns futon2.aif.meta-live-outer-selector
  "Live adapter from the pipeline cascade to canonical outer-task selection."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-pipeline-selector :as meta]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.outer-task-selection :as outer]
            [futon2.aif.pattern-registry :as pattern-registry]))

(def policy-kind :meta-live-pipeline-task-state)

(def ^:private action-type
  {:mission :advance-mission :excursion :advance-excursion
   :ticket :advance-ticket :algorithm :run-algorithm})

(defn- pin [path body]
  {:path path :sha256 (field/sha256 (.getBytes ^String body "UTF-8"))})

(defn- fetch-json [url]
  (let [response (http/get url {:headers {"Accept" "application/json"}
                                :timeout 90000 :throw false})
        body (str (:body response))]
    (when-not (= 200 (:status response))
      (throw (ex-info "Live META source unavailable"
                      {:kind :meta-live-source-unavailable
                       :url url :status (:status response)})))
    {:source (pin url body) :value (json/parse-string body true)}))

(defn fetch-pipeline-snapshot
  "Fetch and byte-pin the current summary and graph exactly once each."
  ([] (fetch-pipeline-snapshot (pattern-registry/configured-evidence-base)))
  ([base-url]
   (let [summary-url (str base-url "/api/alpha/cascade-real")
         graph-url (str base-url "/api/alpha/cascade-real/graph")
         summary (fetch-json summary-url)
         graph (fetch-json graph-url)
         graph-value (update (:value graph) :section-status
                             (fn [sections]
                               (into {} (map (fn [[k v]]
                                               [k (update v :status keyword)]))
                                     sections)))]
     {:schema meta/snapshot-schema
      :summary-source (:source summary) :graph-source (:source graph)
      :summary (:value summary) :graph graph-value})))

(defn- normalized-priorities [tasks]
  (let [values (filter number? (map :priority tasks))
        lo (when (seq values) (double (apply min values)))
        hi (when (seq values) (double (apply max values)))]
    (into {}
          (keep (fn [{:keys [id priority source]}]
                  (when (and (number? priority)
                             (string? (:path source))
                             (re-matches #"[0-9a-f]{64}" (or (:sha256 source) "")))
                    [id {:value (if (= lo hi) 0.5
                                  (/ (- (double priority) lo) (- hi lo)))
                         :freshness :current :source source}]))
          tasks))))

(defn- normalized-pipeline-freshness [tasks snapshot]
  (let [observed (into {}
                       (keep (fn [{:keys [stem mtime-ms]}]
                               (when (and (string? stem) (number? mtime-ms))
                                 [stem (double mtime-ms)])))
                       (get-in snapshot [:graph :tickets :items]))
        values (keep #(get observed (:id %)) tasks)
        lo (when (seq values) (apply min values))
        hi (when (seq values) (apply max values))
        source (:graph-source snapshot)]
    (into {}
          (keep (fn [{:keys [id]}]
                  (when-let [mtime (get observed id)]
                    [id {:value (if (= lo hi) 0.5
                                  (/ (- hi mtime) (- hi lo)))
                         :freshness :current :source source
                         :observation {:field :mtime-ms
                                       :unit :unix-epoch-milliseconds
                                       :value (long mtime)}}])))
          tasks)))

(defn task-state-candidates
  "Project current task evidence. Missing channels remain absent, never zero."
  [tasks snapshot]
  (let [priority-by-id (normalized-priorities tasks)
        freshness-by-id (normalized-pipeline-freshness tasks snapshot)]
    (mapv (fn [{:keys [id kind automated-feasibility]}]
            {:id id :kind kind
             :support {:automated-feasibility
                       (if (contains? #{:supported :infeasible :unknown}
                                      automated-feasibility)
                         automated-feasibility :unknown)}
             :channels (cond-> {}
                         (get priority-by-id id)
                         (assoc :declared-priority-cost (get priority-by-id id))
                         (get freshness-by-id id)
                         (assoc :pipeline-freshness-cost (get freshness-by-id id)))})
          tasks)))

(defn- canonical-receipt [tasks excluded selection]
  (let [support (mapv outer/task-view tasks)
        selected-id (when (= :selected (:status selection)) (:selected selection))
        chosen (first (filter #(= selected-id (:id %)) support))]
    (cond-> {:schema outer/schema
             :policy {:kind policy-kind
                      :uses [:pipeline-cascade :pipeline-freshness
                             :declared-priority
                             :automated-feasibility]
                      :forbids [:cascade :candidates :constructed-candidates
                                :interpretations :precedence :tactical-g]
                      :meta-selection selection}
             :support support :excluded excluded
             :draw {:absent :deterministic-meta-policy}
             :chosen (or chosen {:absent (or (:reason selection)
                                             :meta-selection-refused)})}
      chosen (assoc :action {:type (get action-type (:kind chosen))
                             :target (:id chosen)}))))

(defn select-live
  "Run the live adapter. Dependencies are injectable for exact replay."
  [{:keys [tasks fetch-snapshot candidate-fn]
    :or {fetch-snapshot fetch-pipeline-snapshot
         candidate-fn task-state-candidates}}]
  (let [snapshot (fetch-snapshot)
        nodes (meta/pipeline-node-ids (:graph snapshot))
        on-map (filterv #(contains? nodes (:id %)) tasks)
        off-map (->> tasks
                     (remove #(contains? nodes (:id %)))
                     (mapv #(assoc (outer/task-view %)
                                   :eligible false
                                   :ineligible-reason :pipeline/not-on-current-map
                                   :ineligibility-evidence
                                   {:summary-source (:summary-source snapshot)
                                    :graph-source (:graph-source snapshot)})))
        selection (meta/select {:snapshot snapshot
                                :candidates (candidate-fn on-map snapshot)})]
    (canonical-receipt on-map off-map selection)))

(defn selector [{:keys [tasks]}]
  (select-live {:tasks tasks}))

(defn preview-live
  "Read the authoritative registries once and produce the same receipt used by
  the production selector. This is the read-only Arxana/API projection."
  []
  (let [missions (registry/load-missions)
        excursions (registry/load-excursions)
        tickets (registry/load-tickets)
        tasks (vec (concat
                    (map #(assoc % :kind :mission)
                         (registry/open-missions missions))
                    (map #(assoc % :kind :excursion)
                         (filter registry/live-excursion? (:excursions excursions)))
                    (map #(assoc % :kind :ticket)
                         (filter registry/live-ticket? (:tickets tickets)))))]
    (select-live {:tasks tasks})))
