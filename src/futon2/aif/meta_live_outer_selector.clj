(ns futon2.aif.meta-live-outer-selector
  "Live adapter from the pipeline cascade to canonical outer-task selection."
  (:require [babashka.http-client :as http]
            [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [cheshire.core :as json]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-decision-manifest :as decision-manifest]
            [futon2.aif.meta-pipeline-selector :as meta]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.outer-task-selection :as outer]
            [futon2.aif.outer-target-score :as outer-score]
            [futon2.aif.outer-learning-runtime :as outer-learning-runtime]
            [futon2.aif.outer-policy-evidence :as policy-evidence]
            [futon2.aif.outer-policy-evidence-provider :as evidence-provider]
            [futon2.aif.outer-policy-objective :as policy-objective]
            [futon2.aif.outer-canonical-g :as outer-canonical-g]
            [futon2.aif.outer-canonical-selection :as canonical-selection]
            [futon2.aif.assessed-policy-basis :as assessed-basis]
            [futon2.aif.focus-receipt :as focus-receipt]
            [futon2.aif.selected-outer-cascade-policy :as selected-policy]
            [futon2.aif.pattern-registry :as pattern-registry]
            [futon2.aif.q2-construction :as q2]
            [futon2.aif.q8-frontier :as q8]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.wm.family-selection :as family-selection]
            [futon2.aif.wm.cascade-decision :as cascade-decision]))

(load-identity/register! *ns* *file*)

(def policy-kind :meta-live-pipeline-task-state)

(def ^:private action-type
  {:mission :advance-mission :excursion :advance-excursion
   :ticket :advance-ticket :algorithm :run-algorithm})

(def ^:private repair-ticket-id #"^T-(repair-occ-[0-9a-f]{64})$")
(def ^:private root-task-id #"^[MET]-[^/]+$")

(defn- pin [path body]
  {:path path :sha256 (field/sha256 (.getBytes ^String body "UTF-8"))})

(defn- registry-snapshot
  "Bind the exact task population received at the selector boundary.

  Collection metadata is not a durable transport: production constructs the
  outer population through ordinary vector transformations.  Reconstruct the
  same canonical snapshot used by `live-registry-tasks` from those rows rather
  than assuming metadata survived that transport."
  [tasks]
  (or (:registry-snapshot (meta tasks))
      (let [content (decision-manifest/registry-snapshot-content tasks)]
        {:source (pin "registry://live-outer-task-field" content)
         :content content})))

(defn- fetch-json [url]
  (let [response (http/get url {:headers {"Accept" "application/json"}
                                :timeout 90000 :throw false})
        body (str (:body response))]
    (when-not (= 200 (:status response))
      (throw (ex-info "Live META source unavailable"
                      {:kind :meta-live-source-unavailable
                       :url url :status (:status response)})))
    {:source (pin url body) :bytes body :value (json/parse-string body true)}))

(defn- local-cascade-source
  "Call a cascade producer in the shared serving JVM and pin its canonical JSON.
  This avoids making a blocking HTTP request back into the same bounded server
  worker pool. Returns nil when the producer is not present in this process."
  [source-id producer-symbol]
  (try
    (when-let [producer (requiring-resolve producer-symbol)]
      (let [value (producer)
            body (json/generate-string value)]
        {:source (pin source-id body) :bytes body :value value}))
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
      :source-bytes {:summary (:bytes summary) :graph (:bytes graph)
                     :agency (:bytes agency)}
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

(defn- pin-shape?
  [x]
  (and (map? x) (string? (:path x)) (string? (:sha256 x))
       (boolean (re-matches #"[0-9a-f]{64}" (:sha256 x)))))

(defn ordinary-learning-certificate
  "Certificate for the ordinary (non-injury) cascade policy arm.

  The injury arm's `learning-certificate` attests score consumption by the
  retired target comparator; the ordinary arm consumes outer learning through
  a different, pinned chain: the outer policy evidence provider resolves the
  sealed menu, the joined policy evidence feeds the policy objective, and the
  selected outer cascade policy carrier seals the objective's choice.

  Validation is semantic, not cosmetic.  The carrier is verified with
  selected-policy/verify (seal, menu, evidence, objective recomputation and
  selected entry, on actual content); the supplied policy-evidence and
  policy-objective must BE the carrier's validated artifacts -- including
  actual content, not only copied pins -- so refused evidence, substituted
  content keeping old pins, or a changed objective receipt are all refused.
  The provider is validated with evidence-provider/verify and, when its body
  is in hand, tied to the consumed evidence through the recorded
  declaration pin.  When only the receipt-recorded provider source pin is
  available (frozen replay), the certificate says exactly that and claims
  nothing more.

  Pure: callable against a retained judgement/receipt (Trial 5,
  ordinary-trial-5 selection-stop.edn.gz).  Arguments:
  {:outer-learning :carrier :provided :provider-source-pin
   :policy-evidence :policy-objective}.  Every failure is a POSITIVE
  :refused with a typed reason -- never an abstention.  The selected policy
  is reported from the verified carrier, never altered."
  [{:keys [outer-learning carrier provided provider-source-pin
           policy-evidence policy-objective]}]
  (let [belief (:belief outer-learning)
        controller (:controller outer-learning)
        mode (:mode outer-learning)
        carrier-defect (when (map? carrier) (selected-policy/verify carrier))
        objective-receipt (when (map? carrier) (:objective-receipt carrier))
        raw-objective-receipt (when (map? objective-receipt)
                                (dissoc objective-receipt :objective-pin))
        provider-body? (map? provided)
        provider-defect (when provider-body? (evidence-provider/verify provided))
        reason (cond
                 (nil? carrier) :ordinary-selected-policy-carrier-missing
                 ;; Content binding first: the supplied artifacts must BE the
                 ;; carrier's embedded artifacts -- on actual content, not
                 ;; copied pins -- so refused evidence or a substituted
                 ;; objective is refused even before seal recomputation.
                 (not (and (map? policy-evidence) (map? (:evidence-carrier carrier))
                           (= policy-evidence (:evidence-carrier carrier))))
                 :ordinary-evidence-not-carrier-artifact
                 (not (and (map? policy-objective) (map? objective-receipt)
                           (= policy-objective raw-objective-receipt)))
                 :ordinary-objective-not-carrier-artifact
                 ;; Provider provenance is independent of the carrier seal.
                 (and (not provider-body?) (nil? provider-source-pin))
                 :ordinary-provider-absent
                 provider-defect :ordinary-provider-invalid
                 (and (not provider-body?) (not (pin-shape? provider-source-pin)))
                 :ordinary-provider-pin-invalid
                 (and provider-body?
                      (not= (get-in provided [:declaration :pin :sha256])
                            (get-in policy-evidence [:declaration-pin :sha256])))
                 :ordinary-provider-declaration-mismatch
                 ;; Total semantic verification of the sealed carrier itself.
                 carrier-defect :ordinary-carrier-invalid)]
    (cond
      (nil? outer-learning)
      {:schema :wm/outer-learning-certificate-v1
       :status :absent :reason :outer-learning-not-configured
       :re-arm-condition :supply-pinned-outer-learning-authority}

      (not (contains? #{:shadow :live} mode))
      {:schema :wm/outer-learning-certificate-v1
       :status :refused :reason :outer-learning-mode-not-admitted
       :mode mode
       :re-arm-condition (:re-arm-condition controller)}

      (not= :produced (:status belief))
      {:schema :wm/outer-learning-certificate-v1
       :status :refused :reason :outer-learning-state-unavailable
       :mode mode :belief-refusal belief
       :re-arm-condition (:re-arm-condition controller)}

      :else
      (if reason
        {:schema :wm/outer-learning-certificate-v1
         :status :refused :reason reason
         :mode mode
         :carrier-defect carrier-defect
         :provider-defect provider-defect
         :consumption-path [:outer-policy-evidence-provider :policy-evidence
                            :policy-objective :selected-outer-cascade-policy]
         :re-arm-condition (:re-arm-condition controller)}
        {:schema :wm/outer-learning-certificate-v1
         :status :ordinary-provider-consumed
         :mode mode
         :selection-basis :pinned-provider-objective-consumption
         :consumption-path [:outer-policy-evidence-provider :policy-evidence
                            :policy-objective :selected-outer-cascade-policy]
         :provider-attestation
         (if provider-body?
           {:relation :verified-declaration-pin
            :provider-source-pin (:source-pin provided)
            :declaration-pin (get-in provided [:declaration :pin])
            :evidence-declaration-pin (:declaration-pin policy-evidence)}
           {:relation :receipt-recorded-source-pin
            :provider-source-pin provider-source-pin
            :declaration-pin (:declaration-pin policy-evidence)})
         :menu-pin (:menu-pin carrier)
         :evidence-pin (:evidence-pin carrier)
         :objective-pin (:objective-pin carrier)
         :carrier-pin (:carrier-pin carrier)
         :selected-policy {:policy-id (:policy-id carrier)
                           :target-id (:target-id carrier)
                           :basis-kind (:basis-kind carrier)
                           :cascade-digest (:cascade-digest carrier)}
         :re-arm-condition (:re-arm-condition controller)}))))

(defn- learning-certificate [outer-learning scores learned-selection baseline-selection
                            injury-advisory?]
  (let [belief (:belief outer-learning)
        controller (:controller outer-learning)
        mode (:mode outer-learning)
        all-scored? (and (seq scores) (every? #(= :scored (:status %)) scores))
        selected-score (some #(when (= (:selected learned-selection) (:target %)) %)
                             scores)
        repeat-authorized?
        (or (= :present (get-in selected-score [:terms :continuation-evidence :status]))
            (= :declared (get-in selected-score [:terms :controller-exemption])))]
    (cond
      (nil? outer-learning)
      {:schema :wm/outer-learning-certificate-v1
       :status :absent :reason :outer-learning-not-configured
       :re-arm-condition :supply-pinned-outer-learning-authority}

      (not (contains? #{:shadow :live} mode))
      {:schema :wm/outer-learning-certificate-v1
       :status :refused :reason :outer-learning-mode-not-admitted
       :mode mode
       :re-arm-condition (:re-arm-condition controller)}

      (not= :produced (:status belief))
      {:schema :wm/outer-learning-certificate-v1
       :status :refused :reason :outer-learning-state-unavailable
       :mode mode :belief-refusal belief
       :re-arm-condition (:re-arm-condition controller)}

      injury-advisory?
      {:schema :wm/outer-learning-certificate-v1
       :status :advisory :mode mode
       :reason :policy-prior-conversion-unresolved
       :score-statuses (frequencies (map :status scores))
       :scores-consumed false
       :selection-basis :machine-injury-repair-cost
       :baseline-selection (:selected baseline-selection)
       :re-arm-condition (:re-arm-condition controller)}

      (or (not all-scored?) (not= :selected (:status learned-selection)))
      {:schema :wm/outer-learning-certificate-v1
       :status :refused :reason :outer-learning-shadow-not-consumed
       :mode mode :score-statuses (frequencies (map :status scores))
       :selection learned-selection
       :re-arm-condition (:re-arm-condition controller)}

      :else
      {:schema :wm/outer-learning-certificate-v1
       :status (if (= :live mode) :live-consumed :shadow-consumed) :mode mode
       :prior-revision (field/digest (:prior belief))
       :observation-source (:dataset-source-pin belief)
       :update-verdict (:counts belief)
       :posterior-revision (:source-pin belief)
       :consumption-path [:outer-task-selection :outer-learning :scores]
       :candidate-count (count scores)
       :baseline-selection (:selected baseline-selection)
       :shadow-selection (:selected learned-selection)
       :live-selection (if (= :live mode)
                         (:selected learned-selection)
                         (:selected baseline-selection))
       :changed? (not= (:selected baseline-selection) (:selected learned-selection))
       :repeat-authorized? (boolean repeat-authorized?)
       :re-arm-condition (:re-arm-condition controller)})))

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
          ;; root-missing is split (Joe, 2026-10-25): one reason used to
          ;; cover three different situations — a target that names
          ;; something that does not resolve, a typed no-choice target, and
          ;; machine-owned failures (nil or A- targets).  The same items
          ;; stay excluded; only the reason is typed now, and a root is
          ;; recovered only where retained evidence names it.
          reason (cond
                   (not (and finding-path declared-sha)) :repair-finding/evidence-missing
                   (not= declared-sha actual-sha) :repair-finding/source-sha-mismatch
                   (not= expected-finding-id finding-id) :repair-finding/identity-mismatch
                   (and (map? target) (contains? target :absent))
                   :repair-finding/no-task-selected
                   (or (nil? target)
                       (and (string? target)
                            (str/starts-with? target "A-")))
                   :repair-finding/machine-owned
                   (not (and (string? target) (re-matches root-task-id target)))
                   :repair-finding/root-unresolved
                   (and parent (not= parent target)) :repair-finding/root-ambiguous
                   (not (contains? root-ids target)) :repair-finding/root-not-current)
          evidence (cond-> {:ticket-id (:id ticket)
                            :ticket-source (:source ticket)
                            :finding-id finding-id
                            :finding-source {:path finding-path :sha256 actual-sha}
                            :declared-finding-sha256 declared-sha
                            :ticket-parent parent
                            :finding-target target}
                    (= :repair-finding/machine-owned reason)
                    (assoc :repair-class (:repair/class finding)
                           :failure-kind (:failure-kind finding)))]
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

(defn- repository-root [path]
  (loop [f (.getParentFile (java.io.File. path))]
    (when f
      (if (.exists (java.io.File. f ".git"))
        (.getCanonicalPath f)
        (recur (.getParentFile f))))))

(defn- git-blob-id [^bytes content]
  (let [digest (java.security.MessageDigest/getInstance "SHA-1")
        header (.getBytes (str "blob " (alength content) "\u0000") "UTF-8")]
    (.update digest header)
    (.update digest content)
    (format "%040x" (java.math.BigInteger. 1 (.digest digest)))))

(def ^:private git-log-format
  (str "%x1e%H%x1f%(trailers:key=Agent-Id,valueonly,separator=%x1d)"
       "%x1f%(trailers:key=Agency-Job,valueonly,separator=%x1d)"
       "%x1f%(trailers:key=Dispatched-By,valueonly,separator=%x1d)"))

(defn- split-values [s]
  (if (str/blank? s) [] (str/split s #"\u001d")))

(defn- repo-last-touch [repo rel]
  (let [out (apply git-command repo "-c" "core.quotePath=false" "log"
                   "-1" (str "--format=" git-log-format) "--raw" "--no-renames"
                   "--abbrev=40" "--" [rel])]
    (reduce
     (fn [found record]
       (let [[header body] (str/split record #"\n" 2)
             [commit agents jobs dispatchers] (str/split header #"\u001f" -1)
             provenance {:commit commit
                         :agent-ids (split-values agents)
                         :agency-jobs (split-values jobs)
                         :dispatched-by (split-values dispatchers)}]
         (reduce (fn [m line]
                   (if-let [[_ blob rel]
                            (re-matches #":[0-7]+ [0-7]+ [0-9a-f]+ ([0-9a-f]+) [A-Z]\t(.+)"
                                        line)]
                     (if (contains? m rel) m (assoc m rel (assoc provenance :blob blob)))
                     m))
                 found (str/split-lines (or body "")))))
     {} (rest (str/split out #"\u001e")))))

(defn- classify-last-touch [task touch]
  (try
    (let [path (get-in task [:source :path])
          declared-sha (get-in task [:source :sha256])
          content (java.nio.file.Files/readAllBytes (.toPath (java.io.File. path)))
          current-sha (field/sha256 content)
          current-blob (git-blob-id content)
          {:keys [commit blob agent-ids agency-jobs dispatched-by]} touch]
      (cond
        (nil? touch)
        {:state :unknown :reason :source-uncommitted :source (:source task)}

        (or (not= declared-sha current-sha) (not= blob current-blob))
        {:state :unknown :reason :source-commit-mismatch
         :source (:source task) :commit commit :commit-source-git-blob blob
         :current-source-sha256 current-sha :current-git-blob current-blob}

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
           :trailers {:agent-id agent-id :agency-job (first agency-jobs)
                      :dispatched-by (first dispatched-by)}})))
    (catch Throwable t
      {:state :unknown :reason :git-provenance-unavailable
       :source (:source task) :error-class (.getName (class t))
       :error-message (.getMessage t)})))

(defn- last-touch-observations [tasks]
  (let [located (mapv (fn [task]
                        (let [path (get-in task [:source :path])
                              repo (when (string? path) (repository-root path))]
                          {:task task :repo repo
                           :rel (when repo
                                  (.toString (.relativize
                                              (.toPath (java.io.File. repo))
                                              (.toPath (java.io.File. path)))))}))
                      tasks)
        pool (java.util.concurrent.Executors/newFixedThreadPool 8)]
    ;; One log process supplies commit, trailers and blob identity.  Run the
    ;; structurally bounded field with a fixed eight-process ceiling; latency
    ;; follows the cascade field without exhausting the serving JVM's threads.
    (try
      (let [jobs (mapv (fn [{:keys [task repo rel]}]
                         (reify java.util.concurrent.Callable
                           (call [_]
                             [(:id task)
                              (if repo
                                (try
                                  (classify-last-touch
                                   task (get (repo-last-touch repo rel) rel))
                                  (catch Throwable t
                                    {:state :unknown
                                     :reason :git-provenance-unavailable
                                     :source (:source task)
                                     :error-class (.getName (class t))
                                     :error-message (.getMessage t)}))
                                {:state :unknown
                                 :reason :git-provenance-unavailable
                                 :source (:source task)})])))
                       located)]
        (into {} (map #(.get ^java.util.concurrent.Future %)
                      (.invokeAll pool jobs))))
      (finally (.shutdown pool)))))

(defn- attach-work-attribution [tasks snapshot]
  (let [last-touches (last-touch-observations tasks)]
    (mapv (fn [task]
            (assoc task
                   :ownership (ownership-observation task snapshot)
                   :last-touch (get last-touches (:id task))))
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
                      :observes [:repair-observations :ownership :last-touch]
                      :forbids [:cascade :candidates :constructed-candidates
                                :interpretations :precedence :tactical-g]
                      :meta-selection selection}
             :support support :excluded excluded
             :draw {:absent :deterministic-meta-policy}
             :chosen (or chosen {:absent (or (:reason selection)
                                             :meta-selection-refused)})}
      chosen (assoc :action {:type (get action-type (:kind chosen))
                             :target (:id chosen)})
      (= :algorithm (:kind chosen))
      (assoc :algorithm-admission
             (select-keys (first (filter #(= selected-id (:id %)) tasks))
                          [:repairs-capability :algorithm-admission
                           :resource-admission])))))

(defn- algorithm-candidates [tasks]
  (mapv (fn [{:keys [id source repairs-capability algorithm-admission] :as task}]
          {:id id :kind :algorithm
           :support {:automated-feasibility :supported}
           :channels {:machine-injury-repair-cost
                      {:value 0.0 :freshness :current :source source}}
           :repairs-capability repairs-capability
           :algorithm-admission algorithm-admission
           :resource-admission (:resource-admission task)
           :unsupported-channels []})
        tasks))

(defn select-live
  "Run the live adapter. Dependencies are injectable for exact replay."
  [{:keys [tasks fetch-snapshot candidate-fn retain-manifest? outer-learning
           policy-field-config policy-field-fn policy-menu-fn
           policy-menu-join outer-policy-evidence-provider selection-as-of
           focus-inputs focus-context focus-classify-fn focus-classify-context]
    :or {fetch-snapshot fetch-pipeline-snapshot
         candidate-fn task-state-candidates
         policy-field-fn family-selection/full-eligible-target-field
         policy-menu-fn family-selection/sealed-pre-choice-menu}}]
  (let [algorithm-tasks (filterv #(= :algorithm (:kind %)) tasks)
        registry-tasks (filterv #(not= :algorithm (:kind %)) tasks)
        {ordinary-tasks :tasks repair-excluded :excluded}
        (attach-repair-observations registry-tasks)
        snapshot (fetch-snapshot)
        snapshot-verification (meta/validate-snapshot snapshot)
        standing (into {} (map (fn [task]
                                 [(:id task)
                                  (registry/mission-standing-observation task)]))
                       ordinary-tasks)
        standing-refused?
        (fn [task]
          (let [observation (get standing (:id task))]
            (or (= :conflict (:status observation))
                (= :mission-standing-source-mismatch (:reason observation))
                (and (= :mission (:kind task))
                     (nil? (get-in task [:source :path]))
                     (= :mission-standing-source-unavailable
                        (:reason observation))))))
        standing-refused (filterv standing-refused? ordinary-tasks)
        standing-admitted (filterv (complement standing-refused?) ordinary-tasks)
        standing-excluded
        (mapv #(assoc (outer/task-view %)
                      :eligible false
                      :ineligible-reason
                      (let [observation (get standing (:id %))]
                        (if (= :conflict (:status observation))
                          :mission-standing-conflict
                          (if (= :mission-standing-source-unavailable
                                 (:reason observation))
                            :task-document-source-unavailable
                            (:reason observation))))
                      :ineligibility-evidence (get standing (:id %)))
              standing-refused)
        membership (meta/pipeline-membership snapshot)
        nodes (:ids membership)
        ;; Git provenance is useful only for structural map members.  Resolve
        ;; membership before spawning provenance reads so the complete ticket
        ;; inventory cannot make browser latency grow with every file in it.
        raw-on-map (filterv #(contains? nodes (:id %)) standing-admitted)
        attributed-tasks (attach-work-attribution raw-on-map snapshot)
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
        on-map selectable-tasks
        membership-unknown? (not (:membership-complete? membership))
        unknown-membership (->> standing-admitted
                                (remove #(contains? nodes (:id %)))
                                (filter (constantly membership-unknown?))
                                (mapv #(assoc (outer/task-view %)
                                              :eligible false
                                              :eligibility-status :unknown
                                              :ineligible-reason
                                              :pipeline/membership-unavailable
                                              :ineligibility-evidence
                                              {:section-availability
                                               (:section-availability membership)
                                               :unavailable-structural-sections
                                               (:unavailable-structural-sections membership)
                                               :unknown-structural-sections
                                               (:unknown-structural-sections membership)})))
        off-map (->> standing-admitted
                     (remove #(contains? nodes (:id %)))
                     (remove (constantly membership-unknown?))
                     (mapv #(assoc (outer/task-view %)
                                   :eligible false
                                   :ineligible-reason :pipeline/not-on-current-map
                                   :ineligibility-evidence
                                   {:summary-source (:summary-source snapshot)
                                    :graph-source (:graph-source snapshot)})))
        injury? (boolean (seq algorithm-tasks))
        candidates (if injury? (algorithm-candidates algorithm-tasks) [])
        ;; The ordinary menu reads focus exactly once at the timestamp A1
        ;; retained.  It is deliberately not consulted by the injury arm.
        focus-inputs (when (and (not injury?) selection-as-of)
                       (or focus-inputs (focus-receipt/read-inputs)))
        predecessor-focus (focus-receipt/predecessor-focus previous-run/*predecessor*)
        focus-context (when (and (not injury?) selection-as-of)
                        (or focus-context
                            (focus-receipt/decision-focus-info focus-inputs selection-as-of
                                                              predecessor-focus)))
        focus-valid? (and selection-as-of
                          (= selection-as-of (:as-of focus-context)))
        focus-receipt (cond
                        injury? nil
                        (nil? selection-as-of)
                        {:status :refused :reason :selection-as-of-required}
                        (not focus-valid?)
                        {:status :refused :reason :focus-as-of-mismatch
                         :selection-as-of selection-as-of
                         :focus-as-of (:as-of focus-context)}
                        :else
                        {:status :present :as-of selection-as-of
                         :focus (:focus focus-context)
                         :focus-status (:status focus-context)
                         :focus-origin (:focus-origin focus-context)
                         :previous-focus (or (:previous-focus focus-context)
                                             predecessor-focus)})
        classify (or focus-classify-fn focus-receipt/classify-target)
        target-classes
        (if (= :present (:status focus-receipt))
          (into {}
                (map (fn [task]
                       (let [c (classify focus-inputs focus-context selection-as-of (:id task)
                                         (or focus-classify-context
                                             (focus-receipt/relation-context
                                              registry/default-code-root)))
                             relation (:class c)]
                         [(:id task) {:class relation
                                      :relation (:relation c)
                                      :derived-via (:derived-via c)
                                      :scorer-class (get focus-receipt/scorer-class relation)
                                      :reason (when (= :unknown relation)
                                                (or (get-in c [:relation :reason])
                                                    :classifier-reason-not-recorded))
                                      :provenance {:status :frozen-focus
                                                   :as-of selection-as-of
                                                   :relation-source (:derived-via c)
                                                   :focus-status (:status focus-context)}}]))
                     on-map))
          {})
        ;; `selector` transports optional injection keys explicitly.  Clojure's
        ;; destructuring :or does not replace a present nil, so production
        ;; requests without an injected constructor used to call nil here and
        ;; stop in the debugger before META could classify the injury.
        policy-field-fn (or policy-field-fn
                            family-selection/full-eligible-target-field)
        policy-menu-fn (or policy-menu-fn
                           family-selection/sealed-pre-choice-menu)
        no-available-targets? (and (not injury?) (empty? on-map))
        no-target-reason (if membership-unknown?
                           :pipeline-target-data-unavailable
                           :pipeline-target-population-observed-empty)
        policy-field (when (and (not injury?)
                                (= :present (:status focus-receipt))
                                (seq on-map)
                                (= :verified (:status snapshot-verification))
                                (map? policy-field-config))
                       (policy-field-fn on-map policy-field-config))
        incomplete (when policy-field
                     (filterv #(not= :computed (:status %)) (:families policy-field)))
        menu-join (assoc (or policy-menu-join
                             {:readings (fn [entry]
                                          (cascade-decision/occurrence-readings
                                           entry (:reading-pin entry)))})
                         :focus-receipt focus-receipt
                         :target-classes target-classes)
        menu-join (cond-> menu-join
                    (contains? policy-field-config :g-law)
                    (assoc :g-law (:g-law policy-field-config)))
        sealed-menu (when policy-field
                      (policy-menu-fn policy-field menu-join))
        ;; O3-1 records the canonical frontier beside the existing objective;
        ;; nothing in this packet consumes it to make a choice.
        canonical-g-frontier (when (= :sealed (:status sealed-menu))
                               (outer-canonical-g/produce sealed-menu))
        canonical-policy-selection (when canonical-g-frontier
                                     (canonical-selection/evaluate canonical-g-frontier sealed-menu))
        provided (when (= :sealed (:status sealed-menu))
                   (evidence-provider/resolve-provider
                    outer-policy-evidence-provider sealed-menu))
        policy-evidence (when (= :provided (:status provided))
                          (policy-evidence/join sealed-menu (:declaration provided)
                                                (:records provided) (:score-records provided)))
        policy-objective (when (contains? #{:computed :partial} (:status policy-evidence))
                           (policy-objective/evaluate policy-evidence sealed-menu))
        assessed-basis (when (and (= :partial (:status policy-objective))
                                  (= :selected (get-in policy-objective
                                                       [:assessed-selection :status])))
                         (assessed-basis/seal sealed-menu policy-evidence policy-objective))
        candidate-carrier
        (cond
          (= :selected (:status canonical-policy-selection))
          (selected-policy/seal-canonical sealed-menu policy-evidence policy-objective
                                          canonical-g-frontier canonical-policy-selection)

          ;; O3-2 deliberately has no objective fallback: canonical refusal is
          ;; a typed outer no-choice, while the objective remains recorded.
          (not= :selected (:status canonical-policy-selection)) nil

          (= :selected (:status policy-objective))
          (selected-policy/seal sealed-menu policy-evidence policy-objective)

          assessed-basis
          (selected-policy/seal-assessed assessed-basis))
        selected-carrier (when (= :sealed (:status candidate-carrier)) candidate-carrier)
        selected-projection (when selected-carrier
                              (selected-policy/decision selected-carrier))
        selected-action (get-in selected-projection [:decision :action])
        selected-target (:target selected-action)
        selected-task (first (filter #(= selected-target (:id %))
                                     (map outer/task-view on-map)))
        admitted-target? (contains? (set (map :id on-map)) selected-target)
        carrier-defect (when candidate-carrier
                         (or (when-not selected-carrier
                               (or (:reason candidate-carrier)
                                   :selected-policy-carrier-unsealed))
                             (selected-policy/verify selected-carrier)
                             (when-not (= :selected (:status selected-projection))
                               :selected-policy-projection-refused)
                             (when-not admitted-target?
                               :selected-policy-target-not-admitted)))
        activated-carrier (when-not carrier-defect selected-carrier)
        ordinary-result
        (when-not injury?
          (cond
            (not= :verified (:status snapshot-verification)) snapshot-verification
            no-available-targets?
            {:status :abstained :reason no-target-reason
             :section-availability (:section-availability membership)
             :unknown-targets (mapv :id unknown-membership)}
            (not (map? policy-field-config))
            {:status :refused :reason :policy-field-config-required}
            ;; Only the menu scores with the class, so focus refuses here and
            ;; not before the abstain/config stops above.
            (not= :present (:status focus-receipt)) focus-receipt
            (not= :sealed (:status sealed-menu))
            {:status :refused :reason (or (:reason sealed-menu) :sealed-policy-menu-invalid)}
            (not= :provided (:status provided)) provided
            (not (contains? #{:computed :partial} (:status policy-evidence))) policy-evidence
            carrier-defect {:status :refused :reason carrier-defect}
            (not= :selected (:status canonical-policy-selection)) canonical-policy-selection
            ;; The receipt's :meta-selection must name the canonical choice;
            ;; the objective's own winner stays under :policy-objective.
            (and activated-carrier (= :canonical-G-frontier (:basis-kind activated-carrier)))
            (assoc canonical-policy-selection :selection-basis
                   (get-in selected-projection [:decision :selection-certificate :selection-basis]))
            activated-carrier
            (assoc policy-objective :selection-basis
                   (get-in selected-projection [:decision :selection-certificate :selection-basis]))
            (not (contains? #{:selected :abstained} (:status policy-objective))) policy-objective
            (= :abstained (:status policy-objective)) policy-objective
            :else policy-objective))
        ordinary-reason (or (:reason ordinary-result)
                            (get-in ordinary-result [:assessed-selection :reason]))
        selection (cond
                    (not= :verified (:status snapshot-verification))
                    snapshot-verification
                    injury?
                    (meta/select {:snapshot snapshot :candidates candidates})
                    no-available-targets?
                    (assoc ordinary-result :schema meta/schema)
                    (not (map? policy-field-config))
                    {:schema meta/schema :status :refused
                     :reason :policy-field-config-required}
                    :else (assoc ordinary-result :schema meta/schema))
        learning-scores
        (when (and injury? (contains? #{:shadow :live} (:mode outer-learning)))
          (mapv (fn [candidate]
                  (outer-score/score
                   {:belief (:belief outer-learning)
                    :target (:id candidate)
                    :preference-costs (:preference-costs outer-learning)
                    :preference-transform (:preference-transform outer-learning)
                    :preference-authority (:preference-authority outer-learning)
                    :controller (:controller outer-learning)}))
                candidates))
        ;; The injury comparator accepts bounded current G channels.  No
        ;; authority currently maps the score's engineering terms into a
        ;; policy prior, so attaching the old clamped composite here would
        ;; relabel engineering influence as canonical G.  Keep scores in the
        ;; receipt as advisory instead of inventing that conversion.
        shadow-candidates (when learning-scores candidates)
        shadow-selection
        (when shadow-candidates
          selection)
        effective-selection
        (if (and injury? (= :live (:mode outer-learning)))
          (or shadow-selection
              {:schema meta/schema :status :refused
               :reason :outer-learning-state-unavailable})
          selection)
        injury-excluded (when (seq algorithm-tasks)
                          (mapv #(assoc (outer/task-view %)
                                        :eligible false
                                        :ineligible-reason :machine-injury-active)
                                on-map))
        all-excluded (into repair-excluded
                           (concat standing-excluded ownership-excluded off-map
                                   injury-excluded))
        receipt (canonical-receipt (into algorithm-tasks on-map)
                                   all-excluded effective-selection)
        receipt (if injury?
                  (assoc-in receipt [:policy :branch] :verified-machine-injury-algorithm)
                  (cond-> (-> receipt
                              (assoc :policy (cond-> {:kind (if activated-carrier
                                                     :cascade-live-policy-v1
                                                     :cascade-live-policy-menu-v1)
                                      :branch :ordinary-cascade-policy
                                      :status (if activated-carrier :selected
                                                (:status ordinary-result))
                                      :reason (when-not activated-carrier
                                                ordinary-reason)
                                      :meta-selection selection
                                      :focus-receipt focus-receipt
                                      :field-source-pin (:source-pin policy-field)
                                      :family-census (when policy-field
                                                       (family-selection/field-census policy-field))
                                      :frontier (when policy-field
                                                  (family-selection/measured-frontier policy-field))
                                      :pipeline-section-availability
                                      (:section-availability membership)
                                      :pipeline-membership
                                      (select-keys membership [:membership-complete?
                                                               :unavailable-structural-sections
                                                               :unknown-structural-sections])
                                      :sealed-menu
                                      (if activated-carrier
                                        {:status :carried-on-carrier
                                         :menu-pin (:menu-pin sealed-menu)
                                         :at [:selected-outer-cascade-policy
                                              :sealed-menu]}
                                        sealed-menu)
                                      :evidence-provider-source-pin (:source-pin provided)
                                      :policy-evidence policy-evidence
                                      :missing-policy-evidence (:missing policy-evidence)
                                      :policy-objective policy-objective
                                      :canonical-g-frontier canonical-g-frontier
                                      :canonical-policy-selection canonical-policy-selection
                                      :assessed-policy-basis assessed-basis
                                      :selected-outer-cascade-policy activated-carrier
                                      :missing-targets (mapv :target-id incomplete)
                                      :absent-terms (vec (distinct (concat
                                                                    (:absent-terms provided)
                                                                    (mapcat :absent-terms (:missing policy-evidence))
                                                                    (:absent-terms policy-objective)
                                                                    (mapcat #(map :kind (:failures %))
                                                                            incomplete))))
                                      :legacy-target-shadow
                                      {:status :retired :consumed false}
                                      :target-candidate-selector
                                      {:status :retired :invoked false
                                       :configured (boolean candidate-fn)}}
                                       ;; R6: the refused provider's cause
                                       ;; reaches the receipt's policy entry
                                       ;; (SeamRefusal.lean `m142_repaired_carried`).
                                       (and (not activated-carrier)
                                            (map? ordinary-result)
                                            (contains? ordinary-result :details))
                                       (assoc :details (:details ordinary-result))))
                              (assoc :policy-field policy-field)
                              (assoc :outer-policy-recomputation
                                     (when activated-carrier
                                       (:recomputation activated-carrier))))
                    activated-carrier
                    (assoc :chosen selected-task
                           :action selected-action)
                    (not activated-carrier)
                    (assoc :chosen {:absent (or (:reason ordinary-result)
                                                ordinary-reason
                                                :outer-policy-selection-refused)})
                    (not activated-carrier)
                    (dissoc :action)))
        receipt (cond-> receipt
                  (seq unknown-membership)
                  (assoc :unknown-targets unknown-membership))
        ;; Requirements Q8: the open population, its typed exclusions and
        ;; the exact candidates and pairwise result the effective selection
        ;; used, certified here where they are in hand
        live? (= :live (:mode outer-learning))
        receipt (assoc receipt :q8-frontier
                       (if-not injury?
                         (if activated-carrier
                           (condp = (:basis-kind activated-carrier)
                             :assessed-population
                             (assoc (get-in activated-carrier [:recomputation :q8])
                                    :basis :assessed-population
                                    :scope (:scope activated-carrier))
                             :canonical-G-frontier
                             (q8/canonical-outer-frontier receipt)
                             ;; unknown basis: unchanged stub
                             {:status :certified :carrier (:recomputation activated-carrier)})
                           {:status :absent :reason ordinary-reason})
                         (q8/outer-frontier
                        {:tasks tasks :excluded all-excluded
                         :candidates (if live? shadow-candidates candidates)
                         :selection effective-selection
                         :comparison (if live? :complete-union :shared-only)
                         :missing-channel-cost
                         (when live? (get-in outer-learning [:controller :missing-channel-cost]))})))
        ;; Requirements Q2: one task-level action for each candidate the
        ;; effective selection could choose, formed as the chosen :action is
        receipt (assoc receipt :q2-outer-actions
                       (if-not injury?
                         (if activated-carrier
                           (if (= :assessed-population (:basis-kind activated-carrier))
                             {:status :absent :reason :assessed-population-basis
                              :basis :assessed-population
                              :scope (:scope activated-carrier)
                              :selected-identity (get-in activated-carrier [:recomputation :q2])}
                             {:status :certified :carrier (:recomputation activated-carrier)})
                           {:status :absent :reason ordinary-reason})
                         (q2/outer-actions
                        {:candidates (if live? shadow-candidates candidates)
                         :selection effective-selection
                         :action-of (fn [c] {:type (get action-type (:kind c))
                                             :target (:id c)})})))
        certificate (if injury?
                      (learning-certificate outer-learning learning-scores
                                            shadow-selection selection injury?)
                      (ordinary-learning-certificate
                       {:outer-learning outer-learning
                        :carrier activated-carrier
                        :provided provided
                        :policy-evidence policy-evidence
                        :policy-objective policy-objective}))
        receipt (assoc receipt :outer-learning
                       (cond-> certificate
                         learning-scores (assoc :scores learning-scores
                                                :selection shadow-selection
                                                :injury-score-comparison
                                                {:status :advisory
                                                 :reason :policy-prior-conversion-unresolved
                                                 :selection-basis :machine-injury-repair-cost
                                                 :canonical-gs (mapv #(when (= :scored (:status %))
                                                                        (:g %))
                                                                     learning-scores)})))
        ;; This is the one selection time minted by generate-war-machine.
        ;; Keep it beside the outer receipt, not inside the sealed menu whose
        ;; contents were formed before the outer policy made its choice.
        receipt (cond-> receipt
                  selection-as-of (assoc :selection-as-of selection-as-of))]
    (if retain-manifest?
      (decision-manifest/attach receipt
                                {:tasks tasks :snapshot snapshot
                                 :registry-snapshot (registry-snapshot tasks)
                                 :candidate-inputs on-map
                                 :candidates candidates})
      receipt)))

(defn selector
  [{:keys [tasks meta-field-observation meta-field-source-pin
           policy-field-config policy-field-fn policy-menu-fn policy-menu-join
           outer-policy-evidence-provider selection-as-of]}]
  (if-not selection-as-of
    (canonical-receipt
     tasks [] {:schema meta/schema :status :refused
               :reason :selection-as-of-required})
    (let [verification (when meta-field-observation
                       (field/verify meta-field-observation
                                     {:expected-snapshot-pin
                                      meta-field-source-pin}))
        algorithms (if (= :verified (:status verification))
                     (filterv #(= :algorithm (:kind %))
                              (:rows meta-field-observation))
                     [])]
      (if (and meta-field-observation (not= :verified (:status verification)))
        (assoc (canonical-receipt
                tasks [] {:schema meta/schema :status :refused
                          :reason :meta-field-observation-unverified
                          :details {:verification verification}})
               :selection-as-of selection-as-of)
        (select-live {:tasks (into (vec tasks) algorithms) :retain-manifest? true
                      :selection-as-of selection-as-of
                      :policy-field-config policy-field-config
                      :policy-field-fn policy-field-fn
                      :policy-menu-fn policy-menu-fn
                      :policy-menu-join policy-menu-join
                      :outer-policy-evidence-provider outer-policy-evidence-provider
                      :outer-learning (outer-learning-runtime/load-shadow)})))))

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

(defn live-registry-tasks
  "Read the authoritative registries once into the outer-task input shape."
  []
  (let [missions (registry/load-missions)
        excursions (registry/load-excursions)
        tickets (registry/load-tickets)
        tasks (vec (concat
                    (map #(assoc % :kind :mission)
                         (registry/open-missions missions))
                    (map #(assoc % :kind :excursion)
                         (filter registry/live-excursion?
                                 (:excursions excursions)))
                    (map #(assoc % :kind :ticket)
                         (filter registry/live-ticket? (:tickets tickets)))))
        snapshot (registry-snapshot tasks)]
    (with-meta tasks
      {:registry-snapshot snapshot})))

(defn preview-live
  "Read the authoritative registries once and produce the same receipt used by
  the production selector. This is the read-only Arxana/API projection."
  ([] (preview-live nil))
  ([policy-field-config]
   (select-live {:tasks (live-registry-tasks) :retain-manifest? true
                 :policy-field-config policy-field-config
                 :outer-learning (outer-learning-runtime/load-shadow)})))

(defn preview-live-browser
  "Compute the canonical live selection and return its bounded UI projection."
  []
  (browser-receipt (preview-live)))
