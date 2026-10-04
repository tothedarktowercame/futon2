(ns futon2.aif.meta-live-outer-selector
  "Live adapter from the pipeline cascade to canonical outer-task selection."
  (:require [babashka.http-client :as http]
            [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [cheshire.core :as json]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-pipeline-selector :as meta]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.outer-task-selection :as outer]
            [futon2.aif.pattern-registry :as pattern-registry]))

(load-identity/register! *ns* *file*)

(def policy-kind :meta-live-pipeline-task-state)

(def ^:private action-type
  {:mission :advance-mission :excursion :advance-excursion
   :ticket :advance-ticket :algorithm :run-algorithm})

(def ^:private repair-ticket-id #"^T-(repair-occ-[0-9a-f]{64})$")
(def ^:private root-task-id #"^[MET]-[^/]+$")

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

(defn- local-cascade-source
  "Call a cascade producer in the shared serving JVM and pin its canonical JSON.
  This avoids making a blocking HTTP request back into the same bounded server
  worker pool. Returns nil when the producer is not present in this process."
  [source-id producer-symbol]
  (try
    (when-let [producer (requiring-resolve producer-symbol)]
      (let [value (producer)
            body (json/generate-string value)]
        {:source (pin source-id body) :value value}))
    (catch java.io.FileNotFoundException _ nil)))

(defn fetch-pipeline-snapshot
  "Fetch and byte-pin the current summary and graph exactly once each."
  ([] (fetch-pipeline-snapshot (pattern-registry/configured-evidence-base)))
  ([base-url]
   (let [summary-url (str base-url "/api/alpha/cascade-real")
         graph-url (str base-url "/api/alpha/cascade-real/graph")
         agency-url (str base-url "/api/alpha/agents")
         ;; Futon2 and Futon3c share the canonical serving JVM. Prefer direct
         ;; producer calls there: synchronous self-HTTP can wait behind the
         ;; request currently serving META. Standalone consumers retain HTTP.
         summary (or (local-cascade-source
                      summary-url
                      'futon3c.logic.cascade-real-live/cascade-real-summary)
                     (fetch-json summary-url))
         graph (or (local-cascade-source
                    graph-url
                    'futon3c.logic.cascade-real-live/cascade-real-graph)
                   (fetch-json graph-url))
         agency (or (local-cascade-source
                     agency-url 'futon3c.agency.registry/registry-status)
                    (fetch-json agency-url))
         graph-value (update (:value graph) :section-status
                             (fn [sections]
                               (into {} (map (fn [[k v]]
                                               [k (update v :status keyword)]))
                                     sections)))]
     {:schema meta/snapshot-schema
      :summary-source (:source summary) :graph-source (:source graph)
      :agency-source (:source agency)
      :summary (:value summary) :graph graph-value :agency (:value agency)})))

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

(defn- graph-work-occurrences [graph]
  (let [raw (concat
             (mapcat (juxt :mission :target :predecessor :successor)
                     (:lineage graph))
             (map :mission (:clusters graph))
             (mapcat (juxt :have :want) (:arrows graph))
             (map :mission (:held graph))
             (map :stem (get-in graph [:tickets :items]))
             (map :mission (get-in graph [:patterns :edges])))]
    (frequencies (keep meta/canonical-work-id raw))))

(defn- normalized-pipeline-centrality [tasks snapshot]
  (let [occurrences (graph-work-occurrences (:graph snapshot))
        source (:graph-source snapshot)]
    (into {}
          (keep (fn [{:keys [id]}]
                  (when-let [n (get occurrences id)]
                    [id {:value (/ 1.0 (inc (double n)))
                         :freshness :current :source source
                         :observation {:field :pipeline-structural-occurrences
                                       :value n
                                       :cost-rule "1 / (1 + occurrence count)"}}])))
          tasks)))

(defn task-state-candidates
  "Project current task evidence. Missing channels remain absent, never zero."
  [tasks snapshot]
  (let [priority-by-id (normalized-priorities tasks)
        freshness-by-id (normalized-pipeline-freshness tasks snapshot)
        centrality-by-id (normalized-pipeline-centrality tasks snapshot)]
    (mapv (fn [{:keys [id kind automated-feasibility]}]
            (let [channels (cond-> {}
                             (get priority-by-id id)
                             (assoc :declared-priority-cost (get priority-by-id id))
                             (get centrality-by-id id)
                             (assoc :pipeline-structural-centrality-cost
                                    (get centrality-by-id id))
                             (get freshness-by-id id)
                             (assoc :pipeline-freshness-cost (get freshness-by-id id)))]
              {:id id :kind kind
             :support {:automated-feasibility
                       (if (contains? #{:supported :infeasible :unknown}
                                      automated-feasibility)
                         automated-feasibility :unknown)}
               :channels channels
               :unsupported-channels
               (cond-> []
                 (not (contains? channels :declared-priority-cost))
                 (conj :declared-priority-cost)
                 (not (contains? channels :pipeline-structural-centrality-cost))
                 (conj :pipeline-structural-centrality-cost)
                 (not (contains? channels :pipeline-freshness-cost))
                 (conj :pipeline-freshness-cost))}))
          tasks)))

(defn- exactly-one [xs]
  (when (= 1 (count xs)) (first xs)))

(defn- repair-observation
  "Read the canonical finding pinned by a repair ticket. Returns either an
  exact root observation or a typed unresolved record; neither is rankable."
  [ticket root-ids]
  (try
    (let [[_ expected-finding-id] (re-matches repair-ticket-id (:id ticket))
          ticket-text (slurp (:path ticket))
          finding-path (exactly-one
                        (map second (re-seq #"Finding:.*\(([^)]+\.edn)\)"
                                            ticket-text)))
          declared-sha (exactly-one
                        (map second (re-seq #"Finding SHA-256: `([0-9a-f]{64})`"
                                            ticket-text)))
          finding-text (when finding-path (slurp finding-path))
          actual-sha (when finding-text
                       (field/sha256 (.getBytes ^String finding-text "UTF-8")))
          finding (when (= declared-sha actual-sha) (edn/read-string finding-text))
          finding-id (:repair/id finding)
          target (:target finding)
          parent (:parent ticket)
          reason (cond
                   (not (and finding-path declared-sha)) :repair-finding/evidence-missing
                   (not= declared-sha actual-sha) :repair-finding/source-sha-mismatch
                   (not= expected-finding-id finding-id) :repair-finding/identity-mismatch
                   (not (and (string? target) (re-matches root-task-id target)))
                   :repair-finding/root-missing
                   (and parent (not= parent target)) :repair-finding/root-ambiguous
                   (not (contains? root-ids target)) :repair-finding/root-not-current)
          evidence {:ticket-id (:id ticket)
                    :ticket-source (:source ticket)
                    :finding-id finding-id
                    :finding-source {:path finding-path :sha256 actual-sha}
                    :declared-finding-sha256 declared-sha
                    :ticket-parent parent
                    :finding-target target}]
      (if reason
        {:status :unresolved :reason reason :evidence evidence}
        {:status :attached :root-id target
         :observation {:kind :repair-finding
                       :id (:id ticket)
                       :repair-class (:repair/class finding)
                       :repair-status (:repair/status finding)
                       :failure-kind (:failure-kind finding)
                       :failure-stage (:failure-stage finding)
                       :opened-at (:opened-at finding)
                       :evidence evidence}}))
    (catch Throwable t
      {:status :unresolved :reason :repair-finding/evidence-unreadable
       :evidence {:ticket-id (:id ticket) :ticket-source (:source ticket)
                  :error-class (.getName (class t))
                  :error-message (.getMessage t)}})))

(defn- attach-repair-observations [tasks]
  (let [repair? #(boolean (and (= :ticket (:kind %))
                               (re-matches repair-ticket-id (:id %))))
        ordinary (filterv (complement repair?) tasks)
        root-ids (set (map :id ordinary))
        findings (mapv #(assoc (repair-observation % root-ids) :ticket %)
                       (filter repair? tasks))
        by-root (group-by :root-id (filter #(= :attached (:status %)) findings))
        enriched (mapv (fn [task]
                         (if-let [rows (seq (get by-root (:id task)))]
                           (assoc task :repair-observations
                                  (mapv :observation rows))
                           task))
                       ordinary)
        excluded (mapv (fn [{:keys [status reason evidence ticket root-id]}]
                         (assoc (outer/task-view ticket)
                                :eligible false
                                :ineligible-reason
                                (if (= :attached status)
                                  :repair-finding/attached-to-root
                                  reason)
                                :ineligibility-evidence
                                (cond-> evidence root-id (assoc :root-id root-id))))
                       findings)]
    {:tasks enriched :excluded excluded}))

(defn- agency-target [agent]
  (some #(when (and (string? %) (re-matches root-task-id %)) %)
        [(:mission-id agent) (:excursion-id agent) (:ticket-id agent)]))

(defn- ownership-observation [task snapshot]
  (let [source (:agency-source snapshot)
        agents (get-in snapshot [:agency :agents])
        owners (when (and (map? agents) (map? source))
                 (->> agents
                      (keep (fn [[agent-id agent]]
                              (when (= (:id task) (agency-target agent))
                                {:agent-id (name agent-id)
                                 :session-id (:session-id agent)
                                 :status (:status agent)})))
                      (sort-by :agent-id) vec))]
    (cond
      (nil? owners) {:state :unknown :reason :agency-evidence-unavailable}
      (empty? owners) {:state :unowned :source source}
      (= 1 (count owners)) {:state :active :owner (first owners) :source source}
      :else {:state :ambiguous :owners owners :source source})))

(defn- git-command [dir & args]
  (let [{:keys [exit out err]} (apply shell/sh "git" "-C" dir args)]
    (if (zero? exit)
      (str/trim-newline out)
      (throw (ex-info "git provenance command failed"
                      {:dir dir :args args :exit exit :stderr err})))))

(defn- git-command-raw [dir & args]
  (let [{:keys [exit out err]} (apply shell/sh "git" "-C" dir args)]
    (if (zero? exit)
      out
      (throw (ex-info "git provenance command failed"
                      {:dir dir :args args :exit exit :stderr err})))))

(defn- trailer-values [text label]
  (mapv second (re-seq (re-pattern (str "(?m)^" label ": (.+)$")) text)))

(defn- last-touch-observation [task]
  (try
    (let [path (get-in task [:source :path])
          declared-sha (get-in task [:source :sha256])
          parent (.getParent (java.io.File. path))
          repo (git-command parent "rev-parse" "--show-toplevel")
          rel (.toString (.relativize (.toPath (java.io.File. repo))
                                     (.toPath (java.io.File. path))))
          commit (git-command repo "log" "-1" "--format=%H" "--" rel)]
      (if (str/blank? commit)
        {:state :unknown :reason :source-uncommitted :source (:source task)}
        (let [blob (git-command-raw repo "show" (str commit ":" rel))
              blob-sha (field/sha256 (.getBytes ^String blob "UTF-8"))
              current-sha (field/sha256 (.getBytes ^String (slurp path) "UTF-8"))
              commit-data (git-command
                           repo "show" "-s"
                           "--format=%H%n%(trailers:key=Agent-Id)%n%(trailers:key=Agency-Job)%n%(trailers:key=Dispatched-By)"
                           commit)
              agent-ids (trailer-values commit-data "Agent-Id")
              agency-jobs (trailer-values commit-data "Agency-Job")
              dispatched-by (trailer-values commit-data "Dispatched-By")]
          (cond
            (or (not= declared-sha blob-sha) (not= declared-sha current-sha))
            {:state :unknown :reason :source-commit-mismatch
             :source (:source task) :commit commit :commit-source-sha256 blob-sha}

            (not-every? #(= 1 (count %)) [agent-ids agency-jobs dispatched-by])
            {:state :unknown :reason :commit-trailers-missing-or-ambiguous
             :source (:source task) :commit commit
             :trailers {:agent-id agent-ids :agency-job agency-jobs
                        :dispatched-by dispatched-by}}

            :else
            (let [agent-id (first agent-ids)]
              {:state (if (str/starts-with? agent-id "wm-")
                        :war-machine-authored :agent-authored)
               :source (:source task) :commit commit
               :trailers {:agent-id agent-id
                          :agency-job (first agency-jobs)
                          :dispatched-by (first dispatched-by)}})))))
    (catch Throwable t
      {:state :unknown :reason :git-provenance-unavailable
       :source (:source task) :error-class (.getName (class t))
       :error-message (.getMessage t)})))

(defn- attach-work-attribution [tasks snapshot]
  (mapv (fn [task]
          (assoc task
                 :ownership (ownership-observation task snapshot)
                 :last-touch (last-touch-observation task)))
        tasks))

(defn- canonical-receipt [tasks excluded selection]
  (let [support (mapv outer/task-view tasks)
        selected-id (when (= :selected (:status selection)) (:selected selection))
        chosen (first (filter #(= selected-id (:id %)) support))]
    (cond-> {:schema outer/schema
             :policy {:kind policy-kind
                      :uses [:pipeline-cascade :pipeline-freshness
                             :declared-priority
                             :automated-feasibility]
                      :observes [:repair-observations :ownership :last-touch]
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
  (let [{ordinary-tasks :tasks repair-excluded :excluded}
        (attach-repair-observations tasks)
        snapshot (fetch-snapshot)
        attributed-tasks (attach-work-attribution ordinary-tasks snapshot)
        nodes (meta/pipeline-node-ids (:graph snapshot))
        actively-owned (filterv #(contains? #{:active :ambiguous}
                                             (get-in % [:ownership :state]))
                                attributed-tasks)
        selectable-tasks (filterv #(not (contains? #{:active :ambiguous}
                                                    (get-in % [:ownership :state])))
                                  attributed-tasks)
        ownership-excluded
        (mapv #(assoc (outer/task-view %)
                      :eligible false
                      :ineligible-reason
                      (if (= :active (get-in % [:ownership :state]))
                        :ownership/actively-held
                        :ownership/ambiguous-active-owner)
                      :ineligibility-evidence (:ownership %))
              actively-owned)
        on-map (filterv #(contains? nodes (:id %)) selectable-tasks)
        off-map (->> selectable-tasks
                     (remove #(contains? nodes (:id %)))
                     (mapv #(assoc (outer/task-view %)
                                   :eligible false
                                   :ineligible-reason :pipeline/not-on-current-map
                                   :ineligibility-evidence
                                   {:summary-source (:summary-source snapshot)
                                    :graph-source (:graph-source snapshot)})))
        selection (meta/select {:snapshot snapshot
                                :candidates (candidate-fn on-map snapshot)})]
    (canonical-receipt on-map
                       (into repair-excluded (concat ownership-excluded off-map))
                       selection)))

(defn selector [{:keys [tasks]}]
  (select-live {:tasks tasks}))

(defn browser-receipt
  "Return the bounded UI projection of a canonical outer-selection receipt.
  The full receipt remains the proof/audit artifact; Arxana needs identities,
  ordering, display channels and typed exclusions, not the quadratic pairwise
  comparison witnesses or repeated exclusion evidence."
  [receipt]
  {:schema (:schema receipt)
   :policy {:kind (get-in receipt [:policy :kind])
            :meta-selection
            (select-keys (get-in receipt [:policy :meta-selection])
                         [:schema :status :selected :reason
                          :epistemic-value-nats :ranking])}
   :support (mapv #(select-keys % [:id :kind :source]) (:support receipt))
   :excluded (mapv #(select-keys % [:id :kind :source :ineligible-reason])
                   (:excluded receipt))
   :chosen (:chosen receipt)
   :action (:action receipt)})

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

(defn preview-live-browser
  "Compute the canonical live selection and return its bounded UI projection."
  []
  (browser-receipt (preview-live)))
