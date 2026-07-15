(ns futon2.aif.full-loop-runner
  "Authoritative real-actuation runner for one War Machine opportunity.

  One opportunity selects exactly one strategic action, constructs for that
  decision, dispatches an author, dispatches a distinct reviewer, verifies the
  resulting commit, records a typed implementation/discharge in Futon1b, queues
  Morning Brief QA, and closes every preregistered checkpoint. The deterministic
  fold executor is not an actuator here."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.c-vector :as cv]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.mission-registry :as missions]
            [futon2.aif.morning-brief :as brief]
            [futon2.aif.pattern-registry :as patterns]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.substrate :as substrate]
            [futon2.aif.trace :as trace]
            [futon2.report.cascade-lane :as cascade]
            [futon2.report.war-machine :as wm])
  (:import [java.security MessageDigest]
           [java.time Instant]
           [java.util UUID]))

(def default-agency-base "http://127.0.0.1:7070")
(def default-substrate-base "http://127.0.0.1:7073")
(def default-author "zai-5")
(def default-reviewer "codex-7")
(def default-phase-log "/home/joe/code/futon2/data/wm-full-loop-phases.edn.log")
(def default-inactivity-timeout-ms (* 10 60 1000))
(def semantic-epoch :full-loop-real-actuation-v5)
(def required-checkpoints [:selection :construction :dispatch :build :adjudication])

(defn config
  ([] (config {}))
  ([opts]
   (merge {:agency-base (or (System/getenv "FUTON_WM_AGENCY_BASE")
                            default-agency-base)
           :author (or (System/getenv "FUTON_WM_AUTHOR_AGENT") default-author)
           :reviewer (or (System/getenv "FUTON_WM_REVIEWER_AGENT") default-reviewer)
           :substrate-url (or (System/getenv "FUTON_SUBSTRATE_URL")
                              (System/getenv "FUTON1B_URL")
                              default-substrate-base)
           :inactivity-timeout-ms
           (or (some-> (System/getenv "FUTON_WM_AGENT_INACTIVITY_TIMEOUT_MS")
                       parse-long)
               default-inactivity-timeout-ms)
           :phase-log (or (System/getenv "FUTON_WM_PHASE_LOG") default-phase-log)
           :poll-ms 2000
           :window-days 14
           :trigger :duree-click-on-demand
           :cohort? true
           :semantic-epoch semantic-epoch}
          opts)))

(defn emit-phase!
  "Emit one line-oriented phase event to stdout and the durable operator log."
  [opts context event]
  (let [record (merge {:at (str (Instant/now))} context event)
        line (pr-str record)]
    (println "[wm-phase]" line)
    (flush)
    (if-let [log-fn (:phase-log-fn opts)]
      (log-fn record)
      (when-let [path (:phase-log opts)]
        (io/make-parents path)
        (spit path (str line "\n") :append true)))
    record))

(defn run-phase!
  "Run thunk with start/end telemetry; errors are logged and rethrown."
  [opts context phase thunk]
  (let [started (System/currentTimeMillis)]
    (emit-phase! opts context {:phase phase :transition :start})
    (try
      (let [result (thunk)]
        (emit-phase! opts context {:phase phase :transition :end :outcome :ok
                                   :duration-ms (- (System/currentTimeMillis) started)})
        result)
      (catch Throwable e
        (emit-phase! opts context {:phase phase :transition :end :outcome :error
                                   :duration-ms (- (System/currentTimeMillis) started)
                                   :error-class (.getName (class e))
                                   :error (.getMessage e)})
        (throw e)))))

(defn- sha256 [x]
  (let [bytes (.digest (MessageDigest/getInstance "SHA-256")
                       (.getBytes (pr-str x) "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff %)) bytes))))

(defn- git [repo & args]
  (apply shell/sh "git" "-C" repo args))

(defn- repo-state [repo]
  (let [sha (git repo "rev-parse" "HEAD")
        dirty (git repo "status" "--porcelain" "--untracked-files=no")]
    {:repo repo
     :git-sha (when (zero? (:exit sha)) (str/trim (:out sha)))
     :git-dirty? (if (zero? (:exit dirty))
                   (not (str/blank? (:out dirty)))
                   :unknown)}))

(defn- primary-repos []
  (->> (or (.listFiles (io/file "/home/joe/code")) [])
       (filter #(.isDirectory %))
       (filter #(re-matches #"futon[^/]*" (.getName %)))
       (filter #(or (.exists (io/file % ".git"))
                    (.isFile (io/file % ".git"))))
       (mapv #(.getAbsolutePath %))))

(defn- stack-code-state []
  (let [states (mapv repo-state (primary-repos))
        f2 (first (filter #(= "/home/joe/code/futon2" (:repo %)) states))]
    (assoc f2 :repo-heads (into {} (map (juxt :repo :git-sha) states)))))

(defn- agent-roster [agency-base]
  (let [r (http/get (str agency-base "/api/alpha/agents")
                    {:timeout 10000 :throw false})]
    (when-not (= 200 (:status r))
      (throw (ex-info "Agency roster unavailable"
                      {:outcome :agent-unavailable :status (:status r)})))
    (:agents (json/parse-string (:body r) true))))

(defn- available? [roster agent]
  (let [a (or (get roster (keyword agent)) (get roster agent))]
    (and a (true? (:invoke-ready? a)) (= "idle" (name (:status a))))))

(defn readiness
  "Read-only readiness view for the configured author/reviewer pair."
  ([] (readiness {}))
  ([raw-opts]
   (let [{:keys [agency-base author reviewer] :as opts} (config raw-opts)
         roster (agent-roster agency-base)]
     {:configuration (select-keys opts [:agency-base :substrate-url :author :reviewer
                                        :inactivity-timeout-ms
                                        :phase-log :window-days])
      :agents (into {}
                    (for [agent [author reviewer]
                          :let [record (or (get roster (keyword agent))
                                           (get roster agent))]]
                      [agent {:available? (available? roster agent)
                              :status (:status record)
                              :invoke-ready? (:invoke-ready? record)
                              :session-id (:session-id record)}]))})))

(defn substrate-preflight!
  "Prove that the configured authoritative semantic entity route is reachable.
  A 404 for a unique sentinel is success; the substrate client returns nil."
  [opts]
  (let [started (System/currentTimeMillis)
        probe-id (str "full-loop/preflight/" (UUID/randomUUID))]
    (try
      (substrate/entity-by-id probe-id
                              (assoc opts :substrate-timeout-ms
                                     (or (:substrate-preflight-timeout-ms opts) 15000)))
      {:url (substrate/configured-url opts)
       :route :entity-by-id
       :latency-ms (- (System/currentTimeMillis) started)}
      (catch Throwable e
        (throw (ex-info "Authoritative substrate preflight failed"
                        {:outcome :substrate-unavailable
                         :url (substrate/configured-url opts)
                         :cause-class (.getName (class e))}
                        e))))))

(defn- post-json! [url body]
  (let [r (http/post url {:headers {"Content-Type" "application/json"}
                          :body (json/generate-string body)
                          :timeout 30000 :throw false})
        parsed (try (json/parse-string (:body r) true)
                    (catch Throwable _ {:raw (:body r)}))]
    (if (<= 200 (:status r) 299)
      parsed
      (throw (ex-info "Agency dispatch failed"
                      {:outcome :dispatch-failed :status (:status r)
                       :response parsed})))))

(defn dispatch!
  [{:keys [agency-base]} agent caller mission prompt]
  (post-json! (str agency-base "/api/alpha/bell")
              {:agent-id agent :caller caller :mission-id (str mission)
               :type "request" :prompt prompt}))

(def terminal-states #{"done" "failed" "cancelled" "timed-out"})

(defn job-last-activity-ms
  "Latest trustworthy Agency timestamp for a job, or nil when none parses."
  [job]
  (->> (concat (keep :at (:events job))
               (keep job [:created-at :started-at]))
       (keep (fn [timestamp]
               (try
                 (.toEpochMilli (Instant/parse timestamp))
                 (catch Exception _ nil))))
       (reduce (fn [latest timestamp]
                 (if (or (nil? latest) (> timestamp latest)) timestamp latest))
               nil)))

(defn- interrupt-job!
  [{:keys [agency-base]} job]
  (let [agent-id (:agent-id job)
        job-id (:job-id job)]
    (when (and agent-id job-id)
      (let [r (http/post (str agency-base "/api/alpha/agents/" agent-id
                              "/interrupt-invoke")
                         {:headers {"Content-Type" "application/json"}
                          :body (json/generate-string {:job-id job-id})
                          :timeout 10000
                          :throw false})]
        {:status (:status r)
         :response (try
                     (json/parse-string (:body r) true)
                     (catch Throwable _ {:raw (:body r)}))}))))

(defn poll-job!
  [{:keys [agency-base inactivity-timeout-ms poll-ms] :as opts} job-id]
  (loop []
    (let [r (http/get (str agency-base "/api/alpha/invoke/jobs/" job-id)
                      {:timeout 10000 :throw false})
          body (when (= 200 (:status r)) (json/parse-string (:body r) true))
          job (:job body)]
      (cond
          (not= 200 (:status r))
          (throw (ex-info "Agency job read failed"
                          {:outcome :dispatch-failed :job-id job-id
                           :status (:status r)}))

          (contains? terminal-states (:state job)) job

        (and inactivity-timeout-ms
             (pos? inactivity-timeout-ms)
             (some-> (job-last-activity-ms job)
                     (+ inactivity-timeout-ms)
                     (<= (System/currentTimeMillis))))
        (let [last-activity-ms (job-last-activity-ms job)
              interrupt (interrupt-job! opts job)]
          (throw (ex-info "Agency job stopped reporting activity"
                          {:outcome :agent-job-stalled :job-id job-id
                           :agent-id (:agent-id job)
                           :inactivity-timeout-ms inactivity-timeout-ms
                           :last-activity (some-> last-activity-ms Instant/ofEpochMilli str)
                           :interrupt interrupt})))

        :else (do (Thread/sleep poll-ms) (recur))))))

(defn- selected-entry [judgement]
  (let [action (get-in judgement [:decision :action])]
    (when (map? action)
      (first (filter #(= action (:action %)) (:ranked-actions judgement))))))

(defn- selected-target [entry]
  (or (get-in entry [:action :target])
      (get-in entry [:action :target-class])
      (get-in entry [:action :type])))

(defn- mission-entry [target]
  (first (filter #(= (missions/mission-target-id target) (:id %))
                 (missions/open-missions))))

(defmulti construct-selected-action
  "Production constructor dispatch. Every selectable meta-action needs its
  own construction contract; it must not be made to look like an ordinary
  mission merely by renaming fields."
  (fn [entry] (get-in entry [:action :type])))

(defmethod construct-selected-action :learn-action-class
  [entry]
  (let [{:keys [target-class rationale] :as action} (:action entry)]
    (when target-class
      {:mission target-class
       :psi (or rationale (str "make action class " target-class " addressable"))
       :construction-kind :capability-gap-repair
       :selected-action action
       :capability-contract
       {:action-class target-class
        :observed-boundary rationale
        :required-components
        [:addressable-substrate-enumerator
         :action-proposer-registration
         :instance-executability-check
         :production-actuation-path]
        :acceptance
        [{:check :proposer-support
          :claim "can-propose? is true only when real addressable targets exist"}
         {:check :candidate-shape
          :claim "the proposer emits valid target-bearing action instances"}
         {:check :execution-support
          :claim "every emitted instance passes can-execute? and reaches actuation"}
         {:check :boundary-regression
          :claim "an absent substrate remains an explicit capability gap"}]}
       ;; These are construction disciplines, not similarity-search results.
       :shown ["agent/sense-deliberate-act"
               "pattern-discipline/patterns-as-categorical-objects"]
       :semilattice
       [{:from :addressable-substrate-enumerator :to :action-proposer-registration}
        {:from :action-proposer-registration :to :instance-executability-check}
        {:from :instance-executability-check :to :production-actuation-path}]
       :policy-holes []})))

(defmethod construct-selected-action :fire-pattern
  [entry]
  (patterns/actuation-construction (:action entry)))

(defmethod construct-selected-action :default
  [entry]
  (some-> (first (cascade/cascade-lane [entry] {:n 1 :budget 6}))
          (assoc :construction-kind :selected-policy
                 :selected-action (:action entry))))

(defn construct-for-decision
  "Construct only for the selected policy entry, never the pre-prior rank
  head. Dispatch by action type so meta-actions cannot masquerade as ordinary
  target-bearing mission actions."
  [entry]
  (construct-selected-action entry))

(defn- mission-for-decision [entry target]
  (let [action (:action entry)]
    (if (= :learn-action-class (:type action))
      {:id (str "capability-gap/" (name (:target-class action)))
       :type :capability-gap
       :target-class (:target-class action)
       :status :open
       :rationale (:rationale action)
       :required-transition
       {:from :not-addressable
        :to :proposable-and-executable}}
      (mission-entry target))))

(defn- job-text [job]
  ;; The prompt itself names all verdict markers, so including it makes every
  ;; review look approved. Agency persists the response prefix in
  ;; :result-summary; require the reviewer to put its verdict first.
  (str/join "\n"
            (concat (keep identity [(:result-summary job) (:terminal-message job)])
                    (keep :text (remove #(= "prompt" (:type %)) (:events job))))))

(defn- review-verdict [job]
  (let [text (job-text job)]
    (cond
      (re-find #"FULL_LOOP_REVIEW:\s*APPROVE" text) :approve
      (re-find #"FULL_LOOP_REVIEW:\s*REQUEST_CHANGES" text) :request-changes
      (re-find #"FULL_LOOP_REVIEW:\s*REJECT" text) :reject
      :else :unverifiable)))

(defn- prompt-findings [stop-lines]
  (mapv #(select-keys % [:repair/id :repair/class :attempt-id :failed-commit
                         :review-verdict :review-text :failure-stage
                         :failure-outcome :failure-error :failure-data])
        stop-lines))

(defn- author-prompt [{:keys [author reviewer batch-id]} target mission cascade-entry
                      stop-lines]
  (str author ": FULL-LOOP IMPLEMENTATION OPPORTUNITY. You are the author; "
       reviewer " is the independent reviewer.\n\n"
       "Implement one bounded, substantive advancement of the selected War Machine action. "
       "This is NOT a request for a fold-turn deposit, wiring diagram, report-only artifact, "
       "or prose claiming that work could be done. Change the actual mission/code world.\n\n"
       "SELECTED TARGET: " (pr-str target) "\n"
       "MISSION RECORD: " (pr-str mission) "\n"
       "PATTERN CASCADE: " (pr-str (select-keys cascade-entry
                                                  [:mission :psi :shown :semilattice
                                                   :cascade-score
                                                   :construction-kind
                                                   :capability-contract
                                                   :actuation-contract])) "\n"
       (when (seq stop-lines)
         (str "STOP-THE-LINE REPAIR OBLIGATIONS: "
              (pr-str (prompt-findings stop-lines))
              "\nThese accumulated findings have priority over unrelated work. "
              "Repair all of them fail-closed; "
              "do not preserve a bypass for backward compatibility.\n"))
       (when batch-id
         (str "FROZEN BATCH: " batch-id " uses semantic epoch " semantic-epoch ". "
              "Do not alter War Machine ranking, policy support, dispatch, build, "
              "adjudication, grounding semantics, or the semantic epoch in this parcel. "
              "If the selected work requires such a change, refuse with a typed reason.\n"))
       "\n"
       "Requirements:\n"
       "1. Inspect the mission and repository state; choose a bounded implementation parcel "
       "that genuinely advances its open work.\n"
       "2. Preserve global invariants; do not special-case or bypass a gate.\n"
       "3. Run the repository-required static checks and relevant tests.\n"
       "4. Commit only your coherent changes. Do not include unrelated dirty files.\n"
       "5. Finish with: FULL_LOOP_AUTHOR: DONE <commit-sha> and list validations.\n"
       "If no safe substantive parcel is possible, make no commit and finish with "
       "FULL_LOOP_AUTHOR: REFUSE <typed reason>."))

(defn- reviewer-prompt [{:keys [reviewer author]} target construction repo commit
                        author-job stop-lines]
  (str reviewer ": FULL-LOOP INDEPENDENT REVIEW. " author " authored commit " commit
       " for selected target " (pr-str target) ".\n\n"
       "Repository: " repo "\n"
       "CONSTRUCTION CONTRACT: "
       (pr-str (select-keys construction
                            [:construction-kind :selected-action
                             :capability-contract :actuation-contract
                             :shown :semilattice])) "\n"
       "Author job evidence: " (pr-str (select-keys author-job
                                                     [:job-id :state :artifact-ref
                                                      :result-summary :execution])) "\n"
       (when (seq stop-lines)
         (str "Prior STOP-THE-LINE findings to discharge explicitly: "
              (pr-str (prompt-findings stop-lines)) "\n"))
       "\n"
       "Inspect the commit rather than trusting the summary. Verify that it is substantive "
       "rather than artifact-only, is in scope for the selected target, preserves invariants, "
       "and clears the required static checks and relevant tests. Do not edit or commit.\n\n"
       "BEGIN your response with exactly one self-contained verdict line of at most 200 "
       "characters (Agency durably preserves only this response prefix):\n"
       "FULL_LOOP_REVIEW: APPROVE\n"
       "or FULL_LOOP_REVIEW: REQUEST_CHANGES <reason>\n"
       "or FULL_LOOP_REVIEW: REJECT <reason>"))

(defn- find-commit-repo [commit]
  (some (fn [repo]
          (let [r (git repo "cat-file" "-e" (str commit "^{commit}"))]
            (when (zero? (:exit r)) repo)))
        (primary-repos)))

(defn- commit-files [repo commit]
  (let [r (git repo "diff-tree" "--no-commit-id" "--name-only" "-r" commit)]
    (when-not (zero? (:exit r))
      (throw (ex-info "Cannot inspect authored commit"
                      {:outcome :build-failed :repo repo :commit commit :error (:err r)})))
    (vec (remove str/blank? (str/split-lines (:out r))))))

(defn resolve-build
  "Resolve an Agency artifact-ref to one Futon repository and its changed files."
  [commit]
  (when-let [repo (find-commit-repo commit)]
    {:repo repo :files (commit-files repo commit)}))

(defn- artifact-only-files? [files]
  (and (seq files)
       (every? #(or (str/includes? % "data/fold-turns")
                    (str/includes? % "fold-escrow")
                    (str/includes? % "selection-authoring-flights")
                    (str/includes? % "overnight-flights"))
               files)))

(defn- implementation-id [commit]
  (str "full-loop/implementation/" commit))

(defn- discharge-id [attempt-id]
  (str "full-loop/discharge/" attempt-id))

(defn- grounding-construction-props
  "Return durable construction provenance, revalidating production actions at
   the final write boundary. In particular, a fire-pattern artifact may change
   while the author or reviewer is working; a construction-time digest is not
   authority to ground different bytes later."
  [target construction]
  (let [kind (:construction-kind construction)
        selected-action (:selected-action construction)
        actuation-contract (:actuation-contract construction)]
    (when (= :fire-pattern-actuation kind)
      (let [current (patterns/actuation-construction selected-action)]
        (when-not (and current
                       (= target (:mission current))
                       (= selected-action (:selected-action current))
                       (= actuation-contract (:actuation-contract current)))
          (throw (ex-info "Fire-pattern construction is stale or inconsistent"
                          {:outcome :grounding-failed
                           :target target
                           :construction-kind kind
                           :selected-action selected-action})))))
    (cond-> {:implementation/construction-kind kind
             :implementation/selected-action selected-action}
      actuation-contract
      (assoc :implementation/actuation-contract actuation-contract)

      (= :fire-pattern-actuation kind)
      (assoc :implementation/pattern-id (:target actuation-contract)
             :implementation/pattern-path (:pattern-path actuation-contract)
             :implementation/pattern-sha256 (:pattern-sha256 actuation-contract)
             :implementation/pattern-evidence-ids
             (:evidence-ids actuation-contract)))))

(defn ground-commit!
  [attempt-id target author reviewer repo commit files construction review-job opts]
  (let [impl-id (implementation-id commit)
        before (substrate/entity-by-id impl-id opts)
        construction-props (grounding-construction-props target construction)
        implementation (merge
                        {:xt/id impl-id
                         :entity/type :implementation/commit
                         :entity/name (str "Reviewed implementation " commit)
                         :entity/source "wm-full-loop"
                         :implementation/target (str target)
                         :implementation/repository repo
                         :implementation/commit commit
                         :implementation/files files
                         :implementation/author author
                         :implementation/reviewer reviewer
                         :implementation/review-job (:job-id review-job)}
                        construction-props)
        discharge {:xt/id (discharge-id attempt-id)
                   :entity/type :discharge
                   :entity/name (str "Full-loop discharge " attempt-id)
                   :entity/source "wm-full-loop"
                   :discharge/mission (str target)
                   :discharge/endpoint impl-id
                   :discharge/type :implementation/commit
                   :discharge/proof-query (str "GET /api/alpha/entity/" impl-id)
                   :discharge/reviewer reviewer
                   :discharge/review-job (:job-id review-job)
                   :discharge/at (str (Instant/now))}]
    (when before
      (throw (ex-info "Implementation commit already grounded"
                      {:outcome :grounded-no-change :implementation-id impl-id})))
    (substrate/put-doc! implementation opts)
    (substrate/put-doc! discharge opts)
    (let [after (substrate/entity-by-id impl-id opts)]
      {:before {:implementation-entity before}
       :after {:implementation-entity after}
       :resolved? (= commit (get-in after [:props :implementation/commit]))
       :dial-moved? (and (nil? before) (some? after))
       :implementation-id impl-id
       :discharge-id (discharge-id attempt-id)})))

(defn- term [judgment ground]
  {:judgment judgment :ground ground})

(defn- sorry [kind data]
  {:sorry (assoc data :kind kind)})

(defn- outcome-from [e]
  (or (:outcome (ex-data e)) :incomplete))

(defn run-opportunity!
  "Run one opportunity synchronously. Dependencies may be injected in opts for tests."
  [raw-opts]
  (let [{:keys [trigger cohort? semantic-epoch author reviewer window-days]
         :as opts} (config raw-opts)
        started (System/currentTimeMillis)
        opportunity-id (or (:opportunity-id opts)
                           (str (name trigger) "/" (Instant/now) "/" (UUID/randomUUID)))
        phase-context (atom {:opportunity-id opportunity-id :trigger trigger})
        _ (emit-phase! opts @phase-context {:phase :opportunity :transition :start})
        checkpoints (atom {})
        closing? (atom false)
        roster (run-phase! opts @phase-context :agent-readiness
                           #((or (:roster-fn opts) agent-roster) (:agency-base opts)))
        code-state (run-phase! opts @phase-context :code-state
                               #((or (:code-state-fn opts) stack-code-state)))
        time-cell (term {:opportunity-id opportunity-id
                         :trigger trigger
                         :machine-state {:started-at (str (Instant/now))}
                         :agent-roster (select-keys roster [(keyword author) (keyword reviewer)
                                                            author reviewer])
                         :code-state (assoc code-state
                                            :resolved-mode-flags (wm/arena-mode-flags)
                                            :configuration-digest
                                            (sha256 (select-keys opts
                                                                 [:author :reviewer :trigger
                                                                  :semantic-epoch])))
                         :semantic-epoch semantic-epoch}
                        {:kind :trigger-opportunity :id opportunity-id})
        start-event (when cohort? (cohort/start-attempt! time-cell))
        attempt-id (or (:attempt/id start-event)
                       (str "canary-" (UUID/randomUUID)))
        _ (swap! phase-context assoc :attempt-id attempt-id)
        checkpoint! (fn [checkpoint cell]
                      (swap! checkpoints assoc checkpoint cell)
                      (when cohort?
                        (cohort/append-checkpoint! attempt-id checkpoint cell))
                      cell)
        close! (fn [outcome data]
                 (reset! closing? true)
                 (doseq [cp required-checkpoints
                         :when (not (contains? @checkpoints cp))]
                   (checkpoint! cp (sorry (keyword (str "not-reached-" (name cp)))
                                          {:outcome outcome})))
                 (let [selection-judgment (get-in @checkpoints [:selection :judgment])
                       brief-ref ((or (:queue-fn opts) brief/queue-item!)
                                  {:attempt-id attempt-id :opportunity-id opportunity-id
                                   :batch-id (:batch-id opts)
                                   :trigger trigger :selected-target (:target data)
                                   :outcome outcome :author author :reviewer reviewer
                                   :commit (:commit data) :witness (:witness data)
                                   :selection-review
                                   (when selection-judgment
                                     {:question
                                      "Was this the best available selection?"
                                      :selected-mission
                                      (:selected-mission selection-judgment)
                                      :selected-action
                                      (:selected-action selection-judgment)
                                      :ranked-candidates
                                      (:ranked-candidates selection-judgment)
                                      :selection-reasons
                                      (:selection-reasons selection-judgment)})})
                       closed (term (merge {:outcome outcome
                                            :grounded? (= :grounded-change outcome)
                                            :artifact-only? (= :artifact-only outcome)
                                            :morning-brief-ref brief-ref
                                            :duration-ms (- (System/currentTimeMillis) started)
                                            :resource-use {:agent-turns
                                                           (count (filter identity
                                                                          [(:author-job data)
                                                                           (:review-job data)]))}}
                                           (select-keys data [:witness]))
                                    {:kind :full-loop-outcome :attempt-id attempt-id})
                       result {:attempt-id attempt-id :opportunity-id opportunity-id
                               :outcome outcome :checkpoints @checkpoints
                               :morning-brief-ref brief-ref :data data}]
                   (when cohort? (cohort/close-attempt! attempt-id closed))
                   (if-let [path (:canary-out opts)]
                     (do (io/make-parents path)
                         (spit path (with-out-str (pp/pprint result))))
                     nil)
                   (emit-phase! opts @phase-context
                                {:phase :opportunity :transition :end :outcome outcome
                                 :duration-ms (- (System/currentTimeMillis) started)})
                   result))]
    (try
      (when-not (and (available? roster author) (available? roster reviewer))
        (throw (ex-info "Configured author or reviewer is unavailable"
                        {:outcome :agent-unavailable :author author :reviewer reviewer})))
      (run-phase! opts @phase-context :substrate-preflight
                  #((or (:substrate-preflight-fn opts) substrate-preflight!) opts))
      (run-phase! opts @phase-context :preference-refresh
                  #(try ((or (:refresh-fn opts) cv/maybe-refresh!))
                        (catch Throwable _ nil)))
      (let [open-stop-lines
            (run-phase! opts @phase-context :stop-line-memory
                        #((or (:repair-open-fn opts) repair/open-obligations)))
            stop-line (first open-stop-lines)
            stop-lines (if stop-line
                         (filterv #(= (:target stop-line) (:target %))
                                  open-stop-lines)
                         [])
            selection-judge (or (:judge-fn opts)
                                (fn [days]
                                  (wm/generate-war-machine
                                   days {:include-advisory-lanes? false})))
            judgement0 (:judgement
                        (run-phase! opts @phase-context :selection
                                    #(selection-judge window-days)))
            mode-flags ((or (:mode-flags-fn opts) wm/arena-mode-flags))
            judgement (assoc judgement0 :wm-version
                             ((or (:version-stamp-fn opts) trace/wm-version-stamp)
                              (assoc mode-flags
                                     :trigger trigger
                                     :real-actuation? true
                                     :author author
                                     :reviewer reviewer)))
            ordinary-entry (selected-entry judgement)
            entry (or (:selected-entry stop-line) ordinary-entry)
            target (some-> entry selected-target)
            selection-cell (if entry
                             (term {:selected-mission (str target)
                                    :selected-action (:action entry)
                                    :stop-the-line-obligations
                                    (mapv #(select-keys % [:repair/id :attempt-id
                                                           :failed-commit
                                                           :review-verdict])
                                          stop-lines)
                                    :ranked-candidates (mapv #(select-keys % [:rank :action
                                                                              :G-efe
                                                                              :controller-score])
                                                             (take 10 (:ranked-actions judgement)))
                                    :selection-reasons
                                    (select-keys (:decision judgement)
                                                 [:source :rank :controller-score :tau
                                                  :selection-gain :habit-prior-applied?])
                                    :trace-persistence :after-construction}
                                   {:kind :wm-judgement :decision (:decision judgement)})
                             (sorry :no-selection {:decision (:decision judgement)}))]
        (checkpoint! :selection selection-cell)
        (when-not entry
          (throw (ex-info "War Machine abstained or selected no addressable action"
                          {:outcome (if (= :abstain (get-in judgement [:decision :action]))
                                      :abstained :no-selection)})))
        (let [{:keys [mission construction]}
              (run-phase! opts @phase-context :construction
                          #(hash-map
                            :mission (if-let [mission-fn (:mission-fn opts)]
                                       (mission-fn target)
                                       (mission-for-decision entry target))
                            :construction ((or (:construct-fn opts)
                                               construct-for-decision) entry)))]
          (when-not construction
            (throw (ex-info "No construction for selected decision"
                            {:outcome :construction-failed
                             :failure-stage :construction
                             :target target
                             :selected-entry
                             (select-keys entry [:action :controller-score :G-efe])})))
          ;; A selected action enters the canonical trace—and therefore the
          ;; learned habit prior—only after its production construction path
          ;; has been demonstrated. Failed selections remain fully auditable
          ;; in the cohort and stop-line finding, but cannot reinforce E(pi).
          (let [trace-path ((or (:trace-fn opts) trace/write-trace!) judgement)]
          (checkpoint! :construction
                       (term {:mission (str target)
                              :cascade (select-keys construction
                                                    [:psi :cascade-score :semilattice
                                                     :construction-kind
                                                     :selected-action
                                                     :capability-contract
                                                     :actuation-contract])
                              :sorries (vec (:policy-holes construction))
                              :wiring nil
                              :patterns (vec (:shown construction))
                              :deposit nil
                              :trace-path trace-path}
                             {:kind :decision-pinned-construction
                              :selected-action (:action entry)}))
          (let [author-response
                (run-phase! opts @phase-context :author-dispatch
                            #((or (:dispatch-fn opts) dispatch!) opts author
                              "wm-full-loop" target
                              (author-prompt opts target mission construction stop-lines)))
                author-job-id (:job-id author-response)]
            (checkpoint! :dispatch
                         (term {:agent author :availability :invoke-ready
                                :job-id author-job-id
                                :prompt-ref (str "agency-job:" author-job-id)}
                               {:kind :agency-dispatch :response author-response}))
            (let [author-job (run-phase! opts @phase-context :author-wait
                                         #((or (:poll-fn opts) poll-job!)
                                           opts author-job-id))]
              (when-not (= "done" (:state author-job))
                (throw (ex-info "Author job did not complete"
                                {:outcome :build-failed :author-job author-job})))
              (let [commit (:artifact-ref author-job)
                    build (run-phase! opts @phase-context :build-resolution
                                      #(when commit
                                         ((or (:resolve-build-fn opts) resolve-build)
                                          commit)))
                    repo (:repo build)
                    files (:files build)]
                (when-not (and commit repo (vector? files))
                  (throw (ex-info "Author completed without a verifiable commit"
                                  {:outcome :build-failed :author-job author-job})))
                (when (or (empty? files) (artifact-only-files? files))
                  (throw (ex-info "Authored commit is artifact-only"
                                  {:outcome :artifact-only :commit commit :files files})))
                (let [review-response
                      (run-phase! opts @phase-context :reviewer-dispatch
                                  #((or (:dispatch-fn opts) dispatch!) opts reviewer
                                    "wm-full-loop" target
                                    (reviewer-prompt opts target construction repo commit
                                                     author-job stop-lines)))
                      review-job
                      (run-phase! opts @phase-context :reviewer-wait
                                  #((or (:poll-fn opts) poll-job!) opts
                                    (:job-id review-response)))
                      approved? (and (= "done" (:state review-job))
                                     (boolean (re-find #"FULL_LOOP_REVIEW:\s*APPROVE"
                                                       (job-text review-job))))]
                  (checkpoint! :build
                               (term {:artifacts files
                                      :generated-code files
                                      :commits [commit]
                                      :patterns-used (vec (:shown construction))
                                      :inline-improvements []
                                      :validation {:author (:execution author-job)
                                                   :reviewer (:execution review-job)
                                                   :review-job (:job-id review-job)
                                                   :approved? approved?}}
                                     {:kind :git-commit-and-independent-review
                                      :repository repo}))
                  (when-not approved?
                    (throw (ex-info "Independent review did not approve"
                                    {:outcome :build-failed :author-job author-job
                                     :review-job review-job :commit commit
                                     :target target
                                     :selected-entry
                                     (select-keys entry
                                                  [:action :controller-score :G-efe])})))
                  (let [witness
                        (run-phase! opts @phase-context :grounding
                                    #((or (:ground-fn opts) ground-commit!)
                                      attempt-id target author reviewer repo commit files
                                      construction review-job opts))]
                    (when (seq stop-lines)
                      (run-phase! opts @phase-context :stop-line-resolution
                                  #(doseq [obligation stop-lines]
                                     ((or (:repair-resolve-fn opts) repair/resolve!)
                                      obligation
                                      {:attempt-id attempt-id :commit commit
                                       :reviewer reviewer
                                       :review-job (:job-id review-job)
                                       :witness witness}))))
                    (checkpoint! :adjudication
                                 (term {:before (:before witness)
                                        :after (:after witness)
                                        :witness witness
                                        :build-match {:commit commit
                                                      :review-approved? true}
                                        :dial {:moved? (:dial-moved? witness)
                                               :implementation-id
                                               (:implementation-id witness)}}
                                       {:kind :authoritative-substrate-discharge}))
                    (close! (if (and (:resolved? witness) (:dial-moved? witness))
                              :grounded-change
                              :grounded-no-change)
                            {:target target :commit commit :witness witness
                             :author-job author-job :review-job review-job})))))))))
      (catch Throwable e
        (if @closing?
          (throw e)
          (let [failure (ex-data e)
                review-job (:review-job failure)
                verdict (some-> review-job review-verdict)
                review-finding (when (and (:commit failure)
                                   (#{:request-changes :reject} verdict))
                          ((or (:repair-record-fn opts)
                               repair/record-review-failure!)
                           {:attempt-id attempt-id
                            :target (:target failure)
                            :commit (:commit failure)
                            :selected-entry (:selected-entry failure)
                            :reviewer reviewer
                            :review-job (:job-id review-job)
                            :review-verdict verdict
                            :review-text (job-text review-job)}))
                system-finding
                (when (= :construction (:failure-stage failure))
                  ((or (:repair-system-record-fn opts)
                       repair/record-system-failure!)
                   {:attempt-id attempt-id
                    :target (:target failure)
                    :selected-entry (:selected-entry failure)
                    :failure-stage (:failure-stage failure)
                    :outcome (outcome-from e)
                    :error (.getMessage e)
                    :failure-data (dissoc failure :selected-entry)}))
                finding (or review-finding system-finding)]
              (close! (outcome-from e)
                      {:target (or (:target failure)
                                   (some-> @checkpoints :selection :judgment
                                           :selected-mission))
                       :commit (:commit failure)
                       :witness (:witness failure)
                       :author-job (:author-job failure)
                       :review-job review-job
                       :repair-obligation finding
                       :error (.getMessage e)
                       :error-class (.getName (class e))
                       :error-data failure})))))))
