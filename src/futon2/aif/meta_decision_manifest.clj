(ns futon2.aif.meta-decision-manifest
  "Immutable authority manifest for one production META decision.

  The manifest is not its own verifier. `verify` requires independently
  supplied task rows and snapshot bytes, checks their Git/content authorities,
  reconstructs graph-derived channels, and reruns the pure selector."
  (:require [cheshire.core :as json]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-pipeline-selector :as selector]))

(def schema :wm/meta-decision-input-manifest-v1)

(defn- git [repo & args]
  (let [{:keys [exit out err]} (apply shell/sh "git" "-C" repo args)]
    (when-not (zero? exit)
      (throw (ex-info "Git authority unavailable"
                      {:kind :decision-input-authority-unavailable
                       :repo repo :args args :stderr err})))
    (str/trim-newline out)))

(defn- git-raw [repo & args]
  (let [{:keys [exit out err]} (apply shell/sh "git" "-C" repo args)]
    (when-not (zero? exit)
      (throw (ex-info "Git authority unavailable"
                      {:kind :decision-input-authority-unavailable
                       :repo repo :args args :stderr err})))
    out))

(defn- repository-root [path]
  (loop [f (.getParentFile (java.io.File. path))]
    (when f
      (if (.exists (java.io.File. f ".git"))
        (.getCanonicalPath f)
        (recur (.getParentFile f))))))

(defn- source-authority [{:keys [path sha256] :as source}]
  (when-not (and (string? path) (string? sha256))
    (throw (ex-info "Decision input has no content authority"
                    {:kind :decision-input-authority-unavailable
                     :source source})))
  (let [bytes (slurp path)
        actual (field/sha256 (.getBytes bytes "UTF-8"))]
    (when-not (= sha256 actual)
      (throw (ex-info "Decision input source drifted before capture"
                      {:kind :decision-input-source-drift
                       :source source :actual-sha256 actual})))
    (if-let [repo (repository-root path)]
      (let [revision (git repo "rev-parse" "HEAD")
            rel (.toString (.relativize (.toPath (java.io.File. repo))
                                       (.toPath (java.io.File. path))))
            historical (git-raw repo "show" (str revision ":" rel))
            historical-sha (field/sha256 (.getBytes historical "UTF-8"))]
        (when-not (= sha256 historical-sha)
          (throw (ex-info "Decision input is not fixed by repository revision"
                          {:kind :decision-input-revision-mismatch
                           :repo repo :revision revision :path rel
                           :source-sha256 sha256
                           :revision-content-sha256 historical-sha})))
        {:kind :git :repository repo :revision revision
         :path rel :content-sha256 sha256})
      {:kind :bytes :path path :content-sha256 sha256 :content bytes})))

(defn- task-entry [task]
  (try
    {:id (:id task) :kind (:kind task)
     :automated-feasibility (:automated-feasibility task)
     :priority (:priority task)
     :source (source-authority (:source task))}
    (catch clojure.lang.ExceptionInfo e
      (throw (ex-info (ex-message e) (assoc (ex-data e) :task-id (:id task)) e)))))

(defn- snapshot-entry [snapshot]
  (let [sources [[:summary :summary-source] [:graph :graph-source]
                 [:agency :agency-source]]]
    (into {}
          (map (fn [[label source-key]]
                 (let [source (get snapshot source-key)
                       bytes (get-in snapshot [:source-bytes label])
                       actual (when (string? bytes)
                                (field/sha256 (.getBytes bytes "UTF-8")))]
                   (when-not (= (:sha256 source) actual)
                     (throw (ex-info "Snapshot source bytes unavailable or drifted"
                                     {:kind :decision-input-authority-unavailable
                                      :input label :source source :actual-sha256 actual})))
                   [label {:source source :content bytes}]))
          sources))))

(defn- decision-view [receipt]
  {:support-ids (mapv :id (:support receipt))
   :excluded-ids (mapv :id (:excluded receipt))
   :excluded-reasons (into {} (map (juxt :id :ineligible-reason)) (:excluded receipt))
   :chosen-id (get-in receipt [:chosen :id])
   :selection-status (get-in receipt [:policy :meta-selection :status])
   :selection-reason (get-in receipt [:policy :meta-selection :reason])})

(defn build [receipt {:keys [tasks snapshot candidate-inputs candidates]}]
  (try
    (when (some #(contains? (:channels %) :declared-priority-cost) candidates)
      (throw (ex-info "Declared priority lacks an independently replayable producer"
                      {:kind :decision-channel-authority-unavailable
                       :channel :declared-priority-cost})))
    {:schema schema :status :captured
     :verification-status :pending-independent-replay
     :snapshot (snapshot-entry snapshot)
     :field (mapv task-entry tasks)
     :candidate-inputs (mapv #(select-keys % [:id :kind :automated-feasibility
                                              :priority :source])
                             candidate-inputs)
     :candidates candidates
     :decision (decision-view receipt)}
    (catch clojure.lang.ExceptionInfo e
      {:schema schema :status :refused
       :reason (:kind (ex-data e)) :details (ex-data e)})))

(defn attach [receipt inputs]
  (let [manifest (build receipt inputs)]
    (if (= :captured (:status manifest))
      (assoc receipt :decision-input-manifest manifest
             :decision-input-manifest-sha256
             (field/sha256 (.getBytes (pr-str manifest) "UTF-8"))
             :decision-input-verification :pending-independent-replay)
      (-> receipt
          (assoc :decision-input-manifest manifest
                 :decision-input-verification :refused)
          (assoc-in [:policy :meta-selection]
                    {:schema selector/schema :status :refused
                     :reason (:reason manifest)
                     :details (:details manifest)})
          (assoc :chosen {:absent (:reason manifest)} :action nil)))))

(defn- authority-bytes [{:keys [kind repository revision path content
                                content-sha256]}]
  (let [bytes (case kind
                :git (git-raw repository "show" (str revision ":" path))
                :bytes content
                nil)
        actual (when (string? bytes) (field/sha256 (.getBytes bytes "UTF-8")))]
    (when-not (= content-sha256 actual)
      (throw (ex-info "Decision source authority failed"
                      {:kind :decision-input-source-drift :path path
                       :expected content-sha256 :actual actual})))
    bytes))

(defn- graph-occurrences [graph]
  (frequencies
   (keep selector/canonical-work-id
         (concat
          (mapcat (juxt :mission :target :predecessor :successor) (:lineage graph))
          (map :mission (:clusters graph))
          (mapcat (juxt :have :want) (:arrows graph))
          (map :mission (:held graph))
          (map :mission (get-in graph [:patterns :edges]))))))

(defn- rebuild-candidates [inputs snapshot]
  (let [mtimes (into {} (keep (fn [{:keys [stem mtime-ms]}]
                                (when (and (string? stem) (number? mtime-ms))
                                  [stem (double mtime-ms)])))
                     (get-in snapshot [:graph :tickets :items]))
        values (keep #(get mtimes (:id %)) inputs)
        lo (when (seq values) (apply min values))
        hi (when (seq values) (apply max values))
        occurrences (graph-occurrences (:graph snapshot))
        graph-source (:graph-source snapshot)]
    (mapv
     (fn [{:keys [id kind automated-feasibility]}]
       (let [mtime (get mtimes id)
             n (get occurrences id)
             channels (cond-> {}
                        n (assoc :pipeline-structural-centrality-cost
                                 {:value (/ 1.0 (inc (double n)))
                                  :freshness :current :source graph-source
                                  :observation
                                  {:field :pipeline-structural-occurrences
                                   :value n
                                   :cost-rule "1 / (1 + occurrence count)"}})
                        mtime (assoc :pipeline-freshness-cost
                                     {:value (if (= lo hi) 0.5
                                               (/ (- hi mtime) (- hi lo)))
                                      :freshness :current :source graph-source
                                      :observation
                                      {:field :mtime-ms
                                       :unit :unix-epoch-milliseconds
                                       :value (long mtime)}}))]
         {:id id :kind kind
          :support {:automated-feasibility
                    (if (contains? #{:supported :infeasible :unknown}
                                   automated-feasibility)
                      automated-feasibility :unknown)}
          :channels channels
          :unsupported-channels
          (cond-> []
            true (conj :declared-priority-cost)
            (not n) (conj :pipeline-structural-centrality-cost)
            (not mtime) (conj :pipeline-freshness-cost))}))
     inputs)))

(defn verify
  "Verify MANIFEST against independently supplied `:tasks` and `:snapshot`.
   Returns a typed verdict; never repairs or accepts a partial reconstruction."
  [manifest {:keys [tasks snapshot manifest-sha256]}]
  (try
    (when (and manifest-sha256
               (not= manifest-sha256
                     (field/sha256 (.getBytes (pr-str manifest) "UTF-8"))))
      (throw (ex-info "Manifest bytes differ from the retained run-record pin"
                      {:kind :manifest-content-drift})))
    (when-not (= schema (:schema manifest))
      (throw (ex-info "Manifest schema mismatch" {:kind :manifest-schema-mismatch})))
    (when-not (= :captured (:status manifest))
      (throw (ex-info "Manifest was not captured" {:kind :manifest-not-captured})))
    (let [manifest-ids (mapv :id (:field manifest))
          supplied-ids (mapv :id tasks)]
      (when-not (= manifest-ids supplied-ids)
        (throw (ex-info "Historical field identity census changed"
                        {:kind :decision-field-identity-mismatch
                         :expected manifest-ids :actual supplied-ids}))))
    (let [field-ids (mapv :id (:field manifest))
          support-ids (mapv :id (:candidate-inputs manifest))
          expected-excluded (vec (remove (set support-ids) field-ids))
          decision (:decision manifest)]
      (when-not (= support-ids (:support-ids decision))
        (throw (ex-info "Admitted identity census changed"
                        {:kind :decision-field-identity-mismatch
                         :expected support-ids :actual (:support-ids decision)})))
      (when-not (= expected-excluded (:excluded-ids decision))
        (throw (ex-info "Excluded identity census changed"
                        {:kind :decision-field-identity-mismatch
                         :expected expected-excluded
                         :actual (:excluded-ids decision)}))))
    (doseq [[record task] (map vector (:field manifest) tasks)]
      (let [bytes (authority-bytes (:source record))
            supplied-source (:source task)]
        (when-not (= (:content-sha256 (:source record)) (:sha256 supplied-source))
          (throw (ex-info "Task path/content authority substituted"
                          {:kind :decision-input-source-substitution
                           :id (:id record)})))
        (when-not (= (:content-sha256 (:source record))
                     (field/sha256 (.getBytes bytes "UTF-8")))
          (throw (ex-info "Task content drift" {:kind :decision-input-source-drift
                                                 :id (:id record)})))))
    (doseq [label [:summary :graph :agency]]
      (let [{:keys [source content]} (get-in manifest [:snapshot label])
            supplied-source (get snapshot (keyword (str (name label) "-source")))]
        (when-not (= source supplied-source)
          (throw (ex-info "Snapshot pin changed"
                          {:kind :decision-snapshot-pin-mismatch :input label})))
        (when-not (= (:sha256 source)
                     (field/sha256 (.getBytes content "UTF-8")))
          (throw (ex-info "Snapshot bytes changed"
                          {:kind :decision-snapshot-byte-drift :input label})))))
    (let [parse #(json/parse-string (get-in manifest [:snapshot % :content]) true)
          historical-graph (update (parse :graph) :section-status
                                   (fn [sections]
                                     (into {} (map (fn [[k v]]
                                                     [k (update v :status keyword)]))
                                           sections)))]
      (when-not (= (parse :summary) (:summary snapshot))
        (throw (ex-info "Summary content does not match its authority"
                        {:kind :decision-snapshot-content-mismatch :input :summary})))
      (when-not (= historical-graph (:graph snapshot))
        (throw (ex-info "Graph content does not match its authority"
                        {:kind :decision-snapshot-content-mismatch :input :graph})))
      (when-not (= (parse :agency) (:agency snapshot))
        (throw (ex-info "Ownership content does not match its authority"
                        {:kind :decision-snapshot-content-mismatch :input :agency}))))
    (let [candidate-inputs (:candidate-inputs manifest)
          rebuilt (rebuild-candidates candidate-inputs snapshot)
          _ (when-not (= rebuilt (:candidates manifest))
              (throw (ex-info "Comparison channel evidence changed"
                              {:kind :decision-channel-evidence-mismatch})))
          selected (selector/select {:snapshot snapshot :candidates rebuilt})
          expected (:decision manifest)]
      (when-not (= [(:selection-status expected) (:selection-reason expected)
                    (:chosen-id expected)]
                   [(:status selected) (:reason selected) (:selected selected)])
        (throw (ex-info "Historical chosen decision changed"
                        {:kind :decision-result-mismatch
                         :expected expected :actual selected})))
      {:schema schema :status :verified :decision expected})
    (catch Throwable t
      {:schema schema :status :refused
       :reason (or (:kind (ex-data t)) :decision-verification-error)
       :details (ex-data t)})))
