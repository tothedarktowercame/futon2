(ns futon2.aif.full-loop-runner
  "Authoritative real-actuation runner for one War Machine opportunity.

  One opportunity selects exactly one strategic action, constructs for that
  decision, dispatches an author, dispatches a distinct reviewer, verifies the
  resulting commit, records a typed implementation/discharge in Futon1b, queues
  Morning Brief QA, and closes every preregistered checkpoint. The deterministic
  fold executor is not an actuator here."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
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
            [futon2.aif.tripwire :as tripwire]
            [futon2.report.cascade-lane :as cascade]
            [futon2.report.war-machine :as wm])
  (:import [java.security MessageDigest]
           [java.time Instant]
           [java.util UUID]))

(def default-agency-base "http://127.0.0.1:7070")
(def default-substrate-base "http://127.0.0.1:7073")
(def default-author "zai-5")
(def default-reviewer "codex-7")
(def default-repair-reviewer "codex-1")
(def default-phase-log "/home/joe/code/futon2/data/wm-full-loop-phases.edn.log")
(def default-agent-budget-ms (* 45 60 1000))
(def semantic-epoch :full-loop-real-actuation-v6)
(def required-checkpoints [:selection :construction :dispatch :build :adjudication])
(def discrimination-top-k 5)
(def discrimination-epsilon 1.0e-6)
(def artifact-window-tolerance-ms (* 2 60 1000))

(defn config
  ([] (config {}))
  ([opts]
   (merge {:agency-base (or (System/getenv "FUTON_WM_AGENCY_BASE")
                            default-agency-base)
           :author (or (System/getenv "FUTON_WM_AUTHOR_AGENT") default-author)
           :reviewer (or (System/getenv "FUTON_WM_REVIEWER_AGENT") default-reviewer)
           :repair-reviewer
           (or (System/getenv "FUTON_WM_REPAIR_REVIEWER_AGENT")
               default-repair-reviewer)
           :substrate-url (or (System/getenv "FUTON_SUBSTRATE_URL")
                              (System/getenv "FUTON1B_URL")
                              default-substrate-base)
           :agent-budget-ms
           (or (some-> (System/getenv "FUTON_WM_AGENT_BUDGET_MS")
                       parse-long)
               default-agent-budget-ms)
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
        _ (tripwire/observe! opts record)
        line (pr-str record)]
    (println "[wm-phase]" line)
    (flush)
    (if-let [log-fn (:phase-log-fn opts)]
      (log-fn record)
      (when-let [path (:phase-log opts)]
        (io/make-parents path)
        (spit path (str line "\n") :append true)))
    (when-let [events (:phase-events opts)]
      (swap! events conj record))
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

(defn- git-repo-for-path [path]
  (when-not (str/blank? (str path))
    (let [file (io/file path)
          directory (cond
                      (.isDirectory file) file
                      (.exists file) (.getParentFile file)
                      :else (.getParentFile file))
          result (when directory
                   (git (.getAbsolutePath directory)
                        "rev-parse" "--show-toplevel"))]
      (when (and result (zero? (:exit result)))
        (str/trim (:out result))))))

(defn- action-paths [action]
  (let [repaired-action (get-in action [:repair-obligation :selected-entry :action])]
    (remove nil?
            [(:mission-path action) (:pattern-path action)
             (:mission-path repaired-action) (:pattern-path repaired-action)])))

(defn- machine-repair-repository [entry]
  (let [action (:action entry)
        obligation (:repair-obligation action)]
    (when (and (= :repair-machine-failure (:type action))
               (= :machine-failure (:repair/class obligation)))
      (or (:machine-repo obligation)
          (get-in obligation [:backtrace :code-state :repo])))))

(defn- target-repository [opts entry mission code-state]
  (or (when-let [f (:target-repo-fn opts)] (f entry mission code-state))
      ;; A machine repair belongs to the repository containing the failed
      ;; machine, not necessarily the repository named by the action that
      ;; happened to expose the fault.  The latter remains useful context in
      ;; action-paths, but letting it win caused attempt-021 to inspect futon5a
      ;; for a repair committed to the War Machine in futon2.
      (machine-repair-repository entry)
      (some git-repo-for-path
            (concat (action-paths (:action entry)) [(:path mission)]))
      (:repo code-state)))

(defn- observe-repo-head [opts repo]
  (if-let [f (:repo-head-observation-fn opts)]
    (f repo)
    (let [head (when repo (git repo "rev-parse" "HEAD"))]
      {:repo repo
       :head (when (and head (zero? (:exit head))) (str/trim (:out head)))
       :observed-at-ms (System/currentTimeMillis)})))

(defn- resolve-commit-sha [opts repo commit]
  (when (and repo (not (str/blank? (str commit))))
    (if-let [f (:resolve-commit-sha-fn opts)]
      (f repo commit)
      (let [result (git repo "rev-parse" (str commit "^{commit}"))]
        (when (zero? (:exit result)) (str/trim (:out result)))))))

(defn- commit-time-ms [opts repo commit]
  (if-let [f (:commit-time-ms-fn opts)]
    (f repo commit)
    (let [result (git repo "show" "-s" "--format=%cI" commit)]
      (when (zero? (:exit result))
        (try (.toEpochMilli (Instant/parse (str/trim (:out result))))
             (catch Throwable _ nil))))))

(defn- ancestor? [opts repo ancestor descendant]
  (if-let [f (:ancestor-fn opts)]
    (boolean (f repo ancestor descendant))
    (zero? (:exit (git repo "merge-base" "--is-ancestor" ancestor descendant)))))

(defn fresh-artifact-binding
  "Observe and validate the commit produced by one fresh author dispatch.
  Agency narration is retained only as corroboration of the repository HEAD."
  [opts repo before author-job]
  (if-let [f (:author-artifact-observer-fn opts)]
    (f repo before author-job)
    (let [after (observe-repo-head opts repo)
          before-head (:head before)
          observed-head (:head after)
          text-ref (:artifact-ref author-job)
          start-ms (:observed-at-ms before)
          end-ms (:observed-at-ms after)
          changed? (and before-head observed-head (not= before-head observed-head))
          descendant? (and changed?
                           (ancestor? opts repo before-head observed-head))
          timestamp-ms (when changed? (commit-time-ms opts repo observed-head))
          in-window? (and timestamp-ms start-ms end-ms
                          (<= (- start-ms artifact-window-tolerance-ms)
                              timestamp-ms
                              (+ end-ms artifact-window-tolerance-ms)))
          observed-valid? (and changed? descendant? in-window?)
          text-sha (resolve-commit-sha opts repo text-ref)
          corroborates? (and observed-valid? text-sha (= observed-head text-sha))]
      {:fresh-author? true
       :repo repo
       :pre-dispatch-head before-head
       :observed-head observed-head
       :observed-commit-time-ms timestamp-ms
       :author-window-start-ms start-ms
       :author-window-end-ms end-ms
       :text-artifact-ref text-ref
       :text-artifact-sha text-sha
       :descendant? (boolean descendant?)
       :in-author-window? (boolean in-window?)
       :corroborates? (boolean corroborates?)
       :disagreement? (and observed-valid? (not corroborates?))
       :commit (when observed-valid? observed-head)})))

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
  "Read-only readiness view for the configured author and reviewer roles."
  ([] (readiness {}))
  ([raw-opts]
   (let [{:keys [agency-base author reviewer repair-reviewer] :as opts}
         (config raw-opts)
         roster (agent-roster agency-base)]
     {:configuration (select-keys opts [:agency-base :substrate-url :author :reviewer
                                        :repair-reviewer
                                        :agent-budget-ms
                                        :phase-log :window-days])
      :agents (into {}
                    (for [agent (distinct [author reviewer repair-reviewer])
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
               :type "request" :mode "work" :prompt prompt}))

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

(defn- job-start-ms [job]
  (->> (keep job [:created-at :started-at])
       (keep (fn [timestamp]
               (try
                 (.toEpochMilli (Instant/parse timestamp))
                 (catch Exception _ nil))))
       seq
       (#(when % (apply min %)))))

(defn read-job!
  [{:keys [agency-base]} job-id]
  (let [r (http/get (str agency-base "/api/alpha/invoke/jobs/" job-id)
                    {:timeout 10000 :throw false})]
    (when-not (= 200 (:status r))
      (throw (ex-info "Agency job read failed"
                      {:outcome :dispatch-failed :job-id job-id
                       :status (:status r)})))
    (:job (json/parse-string (:body r) true))))

(defn poll-job!
  "Wait for an Agency terminal state. Agency does not currently emit a
  trustworthy activity heartbeat, so the bound is honestly an absolute job
  budget. Expiry suspends the loop as recoverable `:incomplete`; it never
  interrupts or destroys a possibly productive job."
  [{:keys [agent-budget-ms poll-ms] :as opts} job-id]
  (let [first-poll-ms (System/currentTimeMillis)]
  (loop []
    (let [job (read-job! opts job-id)]
      (cond
        (contains? terminal-states (:state job)) job

        (and agent-budget-ms
             (pos? agent-budget-ms)
             (some-> (or (job-start-ms job) first-poll-ms)
                     (+ agent-budget-ms)
                     (<= (System/currentTimeMillis))))
        (throw (ex-info "Agency job exceeded the absolute recovery budget"
                        {:outcome :incomplete
                         :failure-kind :agent-budget-expired
                         :job-id job-id
                         :agent-id (:agent-id job)
                         :agent-budget-ms agent-budget-ms
                         :job-state (:state job)
                         :last-observed-activity
                         (some-> (job-last-activity-ms job)
                                 Instant/ofEpochMilli str)}))

        :else (do (Thread/sleep poll-ms) (recur)))))))

(defn- selected-entry [judgement]
  (let [action (get-in judgement [:decision :action])]
    (when (map? action)
      (first (filter #(= action (:action %)) (:ranked-actions judgement))))))

(defn- selected-target [entry]
  (or (get-in entry [:action :target])
      (get-in entry [:action :target-class])
      (get-in entry [:action :type])))

(defn epsilon-distinct-count
  "Count numerically distinct finite values modulo epsilon."
  [values epsilon]
  (count
   (reduce (fn [representatives value]
             (let [v (double value)]
               (if (some #(<= (Math/abs (- v (double %))) epsilon)
                         representatives)
                 representatives
                 (conj representatives v))))
           []
           values)))

(defn selection-discrimination
  "Fail-closed diagnostic for the leading feasible policy set. A single
  candidate needs no discrimination; two or more candidates must contain at
  least two epsilon-distinct G values. Habit priors are intentionally excluded:
  E(pi) may break a genuine near-tie but must not hide a flat estimator."
  ([ranked] (selection-discrimination ranked {}))
  ([ranked {:keys [top-k epsilon]
            :or {top-k discrimination-top-k
                 epsilon discrimination-epsilon}}]
   (let [leading (vec (take top-k ranked))
         g-values (mapv :G-efe leading)
         valid-g (filterv #(and (number? %)
                                (Double/isFinite (double %)))
                          g-values)
         distinct-g (epsilon-distinct-count valid-g epsilon)]
     {:candidate-count (count leading)
      :valid-g-count (count valid-g)
      :distinct-g distinct-g
      :epsilon epsilon
      :top-k top-k
      :g-values g-values
      :passes? (and (= (count valid-g) (count leading))
                    (or (< (count leading) 2)
                        (>= distinct-g 2)))})))

(defn- repair-entry [obligation]
  {:action {:type :repair-machine-failure
            :target (:repair/id obligation)
            :repair-obligation obligation
            :rationale (str "stop-the-line: " (name (:repair/class obligation))
                            " from " (:attempt-id obligation))}
   :controller-score ##-Inf
   :G-efe ##-Inf
   :selection-source :stop-the-line})

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

(defmethod construct-selected-action :repair-machine-failure
  [entry]
  (let [action (:action entry)
        obligation (:repair-obligation action)]
    {:mission (:repair/id obligation)
     :psi (str "repair and validate stop-the-line obligation "
               (:repair/id obligation))
     :construction-kind :machine-stop-line-repair
     :selected-action action
     :repair-contract
     {:repair-id (:repair/id obligation)
      :repair-class (:repair/class obligation)
      :failed-attempt (:attempt-id obligation)
      :failure-stage (:failure-stage obligation)
      :failure-kind (:failure-kind obligation)
      :discharge (:discharge-contract obligation)}
     :shown ["futon-theory/stop-the-line" "musn/pause-backtrace"]
     :semilattice [{:from :failure-backtrace :to :repair-implementation}
                   {:from :repair-implementation :to :independent-review}
                   {:from :independent-review :to :successor-validation}]
     :policy-holes []}))

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
    (cond
      (= :repair-machine-failure (:type action))
      (:repair-obligation action)

      (= :learn-action-class (:type action))
      {:id (str "capability-gap/" (name (:target-class action)))
       :type :capability-gap
       :target-class (:target-class action)
       :status :open
       :rationale (:rationale action)
       :required-transition
       {:from :not-addressable
        :to :proposable-and-executable}}

      :else (mission-entry target))))

(defn- job-text [job]
  ;; The prompt itself names all verdict markers, so including it makes every
  ;; review look approved. Agency persists the response prefix in
  ;; :result-summary; require the reviewer to put its verdict first.
  (str/join "\n"
            (concat (keep identity [(:result-summary job) (:terminal-message job)])
                    (keep :text (remove #(= "prompt" (:type %)) (:events job))))))

(defn- review-verdict [job]
  (let [text (job-text job)
        marker (some-> (re-find #"(?m)^FULL_LOOP_REVIEW:\s*(APPROVE|REQUEST_CHANGES|REJECT)\b"
                                text)
                       second)]
    (case marker
      "APPROVE" :approve
      "REQUEST_CHANGES" :request-changes
      "REJECT" :reject
      :unverifiable)))

(defn- author-verdict
  "Parse the author contract's terminal marker (line-anchored, first match —
  same conventions as review-verdict). The author prompt names REFUSE as a
  legal no-commit ending; a runner that cannot read it misfiles every typed
  refusal as a machine failure (attempt-020, 2026-07-16)."
  [job]
  (let [text (job-text job)
        [_ marker detail] (re-find #"(?m)^FULL_LOOP_AUTHOR:\s*(DONE|REFUSE)\b[ \t]*(.*)$"
                                   text)]
    (case marker
      "DONE" {:verdict :done :detail (str/trim (str detail))}
      "REFUSE" {:verdict :refuse :reason (str/trim (str detail))}
      {:verdict :unverifiable})))

(def ^:private feature-card-keys
  [:built :want-coverage :matches-intent? :things-to-try
   :fold-ref :proof-ref :reviewer-note])

(defn- feature-card-from-text [job]
  ;; No end-of-line anchor: Agency's durable response prefix can squash the
  ;; author's next line onto the card line (observed attempt-026), so read one
  ;; EDN form after the marker and ignore trailing prose. A card truncated
  ;; mid-map still fails to read and stays invalid.
  (let [text (job-text job)
        marker "FULL_LOOP_FEATURE_CARD:"]
    (when-let [idx (str/index-of text marker)]
      (try
        (let [form (edn/read-string (subs text (+ idx (count marker))))]
          (when (map? form) form))
        (catch Exception _ nil)))))

(defn- valid-feature-card [job]
  (let [card (or (:feature-card job) (feature-card-from-text job))]
    (when (and (map? card)
               (not (str/blank? (str (:built card))))
               (contains? card :want-coverage)
               (contains? card :matches-intent?)
               (sequential? (:things-to-try card)))
      (-> (select-keys card feature-card-keys)
          (update :things-to-try vec)))))

(def ^:private code-file-pattern
  #"(?i)\.(?:clj|cljc|cljs|bb|el|lean|py|js|jsx|ts|tsx|java|go|rs|c|cc|cpp|h|hpp|sh)$")

(defn- review-execution-gate [files review-job]
  (let [code-files (vec (filter #(re-find code-file-pattern (str %)) files))
        required? (boolean (seq code-files))
        execution (:execution review-job)
        executed? (true? (:executed execution))
        tool-events (long (or (:tool-events execution) 0))
        passed? (or (not required?)
                    (and executed? (pos? tool-events)))]
    {:required? required?
     :code-files code-files
     :executed? executed?
     :tool-events tool-events
     :passed? passed?
     :failure-kind (when-not passed? :review-execution-evidence-missing)}))

(defn- reviewer-note [job]
  (or (:reviewer-note job)
      (some-> (re-find #"(?m)^FULL_LOOP_REVIEWER_NOTE:\s*(.+)$"
                       (job-text job))
              second
              str/trim
              not-empty)))

(defn- existing-file-ref [repo ref]
  (when-not (str/blank? (str ref))
    (let [file (io/file (str ref))
          resolved (if (or (.isAbsolute file) (nil? repo))
                     file
                     (io/file repo (str ref)))]
      (when (.isFile resolved) (.getPath resolved)))))

(defn- mission-fold-candidate [mission entry]
  (when-let [mission-path (or (:path mission)
                              (get-in entry [:action :mission-path]))]
    (let [mission-path (str mission-path)]
      (when (str/ends-with? mission-path ".md")
        (str/replace mission-path #"\.md$" ".executed.edn")))))

(defn- feature-artifact-refs [repo files mission entry card]
  (let [fold-ref (some #(existing-file-ref repo %)
                       [(:fold-ref card)
                        (mission-fold-candidate mission entry)])
        proof-candidates
        (concat [(:proof-ref card)
                 (:proof-ref mission)
                 (:logic-witness mission)
                 (:witness-path mission)]
                (filter #(re-find #"(?i)(?:proof|witness|darktower).*\.(?:edn|clj|lean)$"
                                  (str %))
                        files))
        proof-ref (some #(existing-file-ref repo %) proof-candidates)]
    {:fold-ref fold-ref :proof-ref proof-ref}))

(defn- grounded-feature-card
  [repo files mission entry author-job review-job]
  (when-let [card (valid-feature-card author-job)]
    (let [{:keys [fold-ref proof-ref]}
          (feature-artifact-refs repo files mission entry card)
          note (reviewer-note review-job)]
      (cond-> (dissoc card :fold-ref :proof-ref :reviewer-note)
        fold-ref (assoc :fold-ref fold-ref)
        proof-ref (assoc :proof-ref proof-ref)
        note (assoc :reviewer-note note)))))

(declare recovery-job-id)

(defn- prompt-findings [stop-lines]
  (mapv (fn [finding]
          (assoc (select-keys finding
                              [:repair/id :repair/class :attempt-id
                               :failed-commit :review-verdict :review-text
                               :failure-stage :failure-outcome :failure-error
                               :discharge-contract])
                 :failure-job-id (recovery-job-id finding)))
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
                                                   :actuation-contract
                                                   :repair-contract])) "\n"
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
       "5. BEGIN your response with one compact, self-contained line of at most 200 "
       "characters (Agency durably preserves only this response prefix): "
       "FULL_LOOP_FEATURE_CARD: {:built \"...\" :want-coverage \"...\" "
       ":matches-intent? true :things-to-try [\"command -> observation\"]}. "
       "This line is the structured :feature-card; keep its four required values concise "
       "enough that the closing brace is inside the 200-character limit. It is your "
       "replayable claim about the feature, not the operator's acceptance verdict. "
       "Every :things-to-try entry must be "
       "observation-shaped: \"command or action -> expected observation\" (what to run "
       "and what you should see). Include :fold-ref or :proof-ref only for "
       "artifacts that already exist; the runner will verify and discover links.\n"
       "6. Finish with: FULL_LOOP_AUTHOR: DONE <commit-sha> and list validations.\n"
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
                             :capability-contract :actuation-contract :repair-contract
                             :shown :semilattice])) "\n"
       "Author job evidence: " (pr-str (select-keys author-job
                                                     [:job-id :state :artifact-ref
                                                      :repo-observed-artifact-ref
                                                      :result-summary :execution])) "\n"
       (when (seq stop-lines)
         (str "Prior STOP-THE-LINE findings to discharge explicitly: "
              (pr-str (prompt-findings stop-lines)) "\n"))
       "\n"
       "Inspect the commit rather than trusting the summary. Verify that it is substantive "
       "rather than artifact-only, is in scope for the selected target, preserves invariants, "
       "and clears the required static checks and relevant tests. Do not edit or commit.\n"
       "For every code change you MUST execute the repository gates yourself: run clj-kondo "
       "on every changed .clj/.cljc/.cljs file, run futon4/dev/check-parens.el on every changed "
       "Lisp/Clojure file, and run the relevant tests in a fresh JVM. Report the exact commands "
       "and pass/fail results. An APPROVE without executed tool evidence is invalid.\n\n"
       "BEGIN your response with exactly one self-contained verdict line of at most 200 "
       "characters (Agency durably preserves only this response prefix):\n"
       "FULL_LOOP_REVIEW: APPROVE\n"
       "or FULL_LOOP_REVIEW: REQUEST_CHANGES <reason>\n"
       "or FULL_LOOP_REVIEW: REJECT <reason>\n"
       "You may add a second line FULL_LOOP_REVIEWER_NOTE: <short note>; it is context, "
       "not the operator's feature verdict."))

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
        actuation-contract (:actuation-contract construction)
        repair-contract (:repair-contract construction)]
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

      repair-contract
      (assoc :implementation/repair-contract repair-contract)

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
  (let [raw (or (:outcome (ex-data e)) :incomplete)]
    (if (#{:agent-job-stalled :construction-failed :grounding-failed
           :policy-nondiscrimination :incomplete} raw)
      :incomplete
      raw)))

(defn- failure-kind-from [e]
  (or (:failure-kind (ex-data e)) (:outcome (ex-data e)) :untyped-failure))

(defn- repair-class-for [failure-kind]
  (cond
    (#{:agent-unavailable :agent-readiness-failed :substrate-unavailable
       :dispatch-failed :abstained :no-selection :guardrail-refusal}
     failure-kind)
    :environmental-hold

    (#{:agent-budget-expired :agent-job-stalled} failure-kind)
    :incomplete-recoverable

    :else :machine-failure))

(defn- last-error-phase [phase-events]
  (or (:phase (last (filter #(= :error (:outcome %)) phase-events)))
      (:phase (last phase-events))
      :opportunity))

(defn- discharge-contract [repair-class]
  (case repair-class
    :machine-failure
    {:requires [:distinct-repair-commit :independent-review
                :grounded-repair :distinct-production-shaped-successor]}
    :environmental-hold
    {:requires [:cleared-precondition :grounded-production-shaped-successor]}
    :incomplete-recoverable
    {:requires [:recover-existing-author-artifact :independent-review
                :grounded-existing-commit]}
    {:requires [:grounded-production-shaped-successor]}))

(defn- recovery-job-id [obligation]
  (or (get-in obligation [:failure-data :job-id])
      (get-in obligation [:backtrace :job-id])
      (:author-job-id obligation)))

(defn- recovery-snapshot
  "Read, but never restart, the Agency job named by a recoverable finding."
  [opts obligation]
  (when (and (= :incomplete-recoverable (:repair/class obligation))
             (recovery-job-id obligation))
    ((or (:read-job-fn opts) read-job!)
     opts (recovery-job-id obligation))))

(defn- run-opportunity-core!
  "Run one opportunity synchronously. Dependencies may be injected in opts for tests."
  [raw-opts]
  (let [phase-events (atom [])
        {:keys [trigger cohort? semantic-epoch author reviewer repair-reviewer
                window-days]
         :as opts} (assoc (config raw-opts) :phase-events phase-events)
        started (System/currentTimeMillis)
        opportunity-id (or (:opportunity-id opts)
                           (str (name trigger) "/" (Instant/now) "/" (UUID/randomUUID)))
        phase-context (atom {:opportunity-id opportunity-id :trigger trigger})
        _ (emit-phase! opts @phase-context {:phase :opportunity :transition :start})
        checkpoints (atom {})
        dispatched-turns (atom 0)
        reviewer-of-record (atom reviewer)
        closing? (atom false)
        roster-result (try
                        {:value
                         (run-phase! opts @phase-context :agent-readiness
                                     #((or (:roster-fn opts) agent-roster)
                                       (:agency-base opts)))}
                        (catch Throwable e {:error e}))
        roster (:value roster-result)
        code-state-result (try
                            {:value
                             (run-phase! opts @phase-context :code-state
                                         #((or (:code-state-fn opts)
                                               stack-code-state)))}
                            (catch Throwable e {:error e}))
        code-state (:value code-state-result)
        time-cell (term {:opportunity-id opportunity-id
                         :trigger trigger
                         :machine-state {:started-at (str (Instant/now))}
                         :agent-roster
                         (select-keys roster
                                      [(keyword author) (keyword reviewer)
                                       (keyword repair-reviewer)
                                       author reviewer repair-reviewer])
                         :code-state (assoc code-state
                                            :resolved-mode-flags (wm/arena-mode-flags)
                                            :configuration-digest
                                            (sha256
                                             (select-keys opts
                                                          [:author :reviewer
                                                           :repair-reviewer :trigger
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
                       selected-action (:selected-action selection-judgment)
                       selected-entry (when selected-action
                                        {:action selected-action
                                         :controller-score
                                         (get-in selection-judgment
                                                 [:selection-reasons
                                                  :controller-score])})
                       existing-finding (:repair-obligation data)
                       repair-class (or (:repair/class existing-finding)
                                        (repair-class-for
                                         (or (:failure-kind data) outcome)))
                       finding
                       (when-not (= :grounded-change outcome)
                         (or existing-finding
                             ((or (:repair-system-record-fn opts)
                                  repair/record-system-failure!)
                              {:attempt-id attempt-id
                               :repair-class repair-class
                               :machine-repo (:repo code-state)
                               :target (or (:target data)
                                           (:selected-mission selection-judgment))
                               :selected-entry selected-entry
                               :failure-stage (or (:failure-stage data)
                                                  (last-error-phase @phase-events))
                               :outcome outcome
                               :failure-kind (or (:failure-kind data) outcome)
                               :error (if (str/blank? (str (:error data)))
                                        (str "zero-achievement outcome " outcome)
                                        (:error data))
                               :failure-data (:error-data data)
                               :backtrace
                               {:phase-events @phase-events
                                :last-completed-checkpoint
                                (last (filter #(contains? @checkpoints %)
                                              required-checkpoints))
                                :checkpoints @checkpoints
                                :code-state (get-in time-cell
                                                    [:judgment :code-state])}
                               :discharge-contract
                               (discharge-contract repair-class)})))
                       data (assoc data :repair-obligation finding)
                       brief-item
                       (cond->
                        {:attempt-id attempt-id :opportunity-id opportunity-id
                                   :batch-id (:batch-id opts)
                                   :trigger trigger :selected-target (:target data)
                                   :outcome outcome :author author
                                   :reviewer @reviewer-of-record
                                   :commit (:commit data) :witness (:witness data)
                                   :achievement
                                   {:tier (cond
                                            (= :grounded-change outcome)
                                            :fully-grounded
                                            (:commit data) :partial-authored
                                            :else :none)
                                    :summary (cond
                                               (= :grounded-change outcome)
                                               "Independently reviewed and grounded change"
                                               (:commit data)
                                               "Authored commit exists but the loop is incomplete"
                                               :else "No grounded achievement")
                                    :build (get-in @checkpoints [:build :judgment])
                                    :adjudication
                                    (get-in @checkpoints [:adjudication :judgment])}
                                   :failure
                                   (when-not (= :grounded-change outcome)
                                     {:kind (or (:failure-kind data) outcome)
                                      :stage (or (:failure-stage data)
                                                 (last-error-phase @phase-events))
                                      :error (:error data)
                                      :backtrace (:backtrace finding)
                                      :repair-id (:repair/id finding)
                                      :discharge-contract
                                      (:discharge-contract finding)})
                                   :qa-targets
                                   {:selection {:policy selected-action}
                                    :achievement
                                    {:entity-id (get-in data [:witness
                                                              :implementation-id])}}
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
                                      (:selection-reasons selection-judgment)})}
                         (and (= :grounded-change outcome)
                              (:feature-card data))
                         (assoc :feature-card (:feature-card data)))
                       brief-ref ((or (:queue-fn opts) brief/queue-item!) brief-item)
                       closed (term (merge {:outcome outcome
                                            :grounded? (= :grounded-change outcome)
                                            :artifact-only? (= :artifact-only outcome)
                                            :morning-brief-ref brief-ref
                                            :duration-ms (- (System/currentTimeMillis) started)
                                            :resource-use
                                            {:agent-turns @dispatched-turns}}
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
                                 :duration-ms (- (System/currentTimeMillis) started)
                                 :tripwire/snapshot
                                 {:grounded-commit
                                  (when (:witness data) (:commit data))
                                  :reviewer-job (:review-job data)
                                  :grounding-witnesses
                                  (cond-> [] (:witness data) (conj (:witness data)))}})
                   result))]
    (try
      (when-let [e (:error roster-result)]
        (throw (ex-info "Agent readiness observation failed"
                        {:outcome :agent-unavailable
                         :failure-kind :agent-readiness-failed
                         :failure-stage :agent-readiness}
                        e)))
      (when-let [e (:error code-state-result)]
        (throw (ex-info "Stack code-state observation failed"
                        {:outcome :incomplete
                         :failure-kind :code-state-failed
                         :failure-stage :code-state}
                        e)))
      (when-not (available? roster author)
        (throw (ex-info "Configured author is unavailable"
                        {:outcome :agent-unavailable :author author})))
      (run-phase! opts @phase-context :substrate-preflight
                  #((or (:substrate-preflight-fn opts) substrate-preflight!) opts))
      (run-phase! opts @phase-context :preference-refresh
                  #(try ((or (:refresh-fn opts) cv/maybe-refresh!))
                        (catch Throwable _ nil)))
      (let [open-stop-lines
            (run-phase! opts @phase-context :stop-line-memory
                        #((or (:repair-open-fn opts) repair/open-obligations)))
            stop-line (first (filter #(and (= :open (:repair/status %))
                                           (not= :environmental-hold
                                                 (:repair/class %)))
                                     open-stop-lines))
            validation-lines
            (->> open-stop-lines
                 (filter #(or (= :awaiting-validation (:repair/status %))
                              (= :environmental-hold (:repair/class %))))
                 (take 1)
                 vec)
            stop-lines (if stop-line
                         [stop-line]
                         [])
            supersede-recovery!
            (fn [repair-class failure-kind failure-stage error failure-data]
              (let [successor
                    ((or (:repair-system-record-fn opts)
                         repair/record-system-failure!)
                     {:attempt-id attempt-id
                      :repair-class repair-class
                      :target (:target stop-line)
                      :selected-entry (:selected-entry stop-line)
                      :failure-stage failure-stage
                      :outcome :incomplete
                      :failure-kind failure-kind
                      :error error
                      :failure-data failure-data
                      :backtrace {:supersedes (:repair/id stop-line)}
                      :discharge-contract (discharge-contract repair-class)})]
                ((or (:repair-supersede-fn opts) repair/supersede!)
                 stop-line successor failure-kind)
                successor))
            selection-judge (or (:judge-fn opts)
                                (fn [days]
                                  (wm/generate-war-machine
                                   days {:include-advisory-lanes? false})))
            judgement0 (:judgement
                        (run-phase! opts @phase-context :selection
                                    #(selection-judge window-days)))
            mode-flags ((or (:mode-flags-fn opts) wm/arena-mode-flags))
            ordinary-entry (selected-entry judgement0)
            entry (if stop-line (repair-entry stop-line) ordinary-entry)
            repair-action? (= :repair-machine-failure
                              (get-in entry [:action :type]))
            reviewer (if repair-action? repair-reviewer reviewer)
            _ (reset! reviewer-of-record reviewer)
            judgement (assoc judgement0 :wm-version
                             ((or (:version-stamp-fn opts) trace/wm-version-stamp)
                              (assoc mode-flags
                                     :trigger trigger
                                     :real-actuation? true
                                     :author author
                                     :reviewer reviewer
                                     :repair-reviewer repair-reviewer)))
            target (some-> entry selected-target)
            ranked-for-review (if stop-line
                                [entry]
                                (or (:admissible-actions judgement)
                                    (:ranked-actions judgement)))
            discrimination (when-not stop-line
                             (selection-discrimination ranked-for-review))
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
                                                                              :controller-score
                                                                              :habit-prior-bias])
                                                             (take 10 ranked-for-review))
                                    :selection-reasons
                                    (if stop-line
                                      {:source :stop-the-line
                                       :repair-id (:repair/id stop-line)
                                       :repair-class (:repair/class stop-line)}
                                      (assoc
                                       (select-keys (:decision judgement)
                                                    [:source :rank :controller-score :tau
                                                     :selection-gain
                                                     :habit-prior-applied?])
                                       :discrimination discrimination))
                                    :trace-persistence :after-construction}
                                   {:kind :wm-judgement :decision (:decision judgement)})
                             (sorry :no-selection {:decision (:decision judgement)}))]
        (checkpoint! :selection selection-cell)
        (when-not entry
          (throw (ex-info "War Machine abstained or selected no addressable action"
                          {:outcome (if (= :abstain (get-in judgement [:decision :action]))
                                      :abstained :no-selection)})))
        (when (or (= author reviewer) (not (available? roster reviewer)))
          (throw (ex-info "Selected reviewer is unavailable or is the author"
                          {:outcome :agent-unavailable
                           :failure-kind :agent-unavailable
                           :failure-stage :agent-readiness
                           :author author :reviewer reviewer
                           :review-role (if repair-action?
                                          :ground-control :ordinary)})))
        (when (and discrimination (not (:passes? discrimination)))
          (throw (ex-info "Leading feasible policies have no G discrimination"
                          {:outcome :policy-nondiscrimination
                           :failure-kind :policy-nondiscrimination
                           :failure-stage :selection
                           :target target
                           :selected-entry entry
                           :discrimination discrimination})))
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
          (let [trace-path (when-not repair-action?
                             ((or (:trace-fn opts) trace/write-trace!) judgement))]
          (checkpoint! :construction
                       (term {:mission (str target)
                              :cascade (select-keys construction
                                                    [:psi :cascade-score :semilattice
                                                     :construction-kind
                                                     :selected-action
                                                     :capability-contract
                                                     :actuation-contract
                                                     :repair-contract])
                              :sorries (vec (:policy-holes construction))
                              :wiring nil
                              :patterns (vec (:shown construction))
                              :deposit nil
                              :trace-path trace-path}
                             {:kind :decision-pinned-construction
                              :selected-action (:action entry)}))
          (let [snapshot (when stop-line
                           (recovery-snapshot opts stop-line))
                recovery-stage (:failure-stage stop-line)
                _ (when (and snapshot (not= "done" (:state snapshot)))
                    (if (contains? terminal-states (:state snapshot))
                      (let [error (str "Recovery job terminated as "
                                       (:state snapshot))
                            successor
                            (supersede-recovery!
                             :machine-failure :recovery-job-terminal
                             recovery-stage error
                             {:job-id (:job-id snapshot)
                              :job-state (:state snapshot)})]
                        (throw (ex-info error
                                        {:outcome :incomplete
                                         :failure-kind :recovery-job-terminal
                                         :failure-stage recovery-stage
                                         :repair-obligation successor})))
                      (throw
                       (ex-info
                        "Recovery job is still active; no replacement turn dispatched"
                        {:outcome :incomplete
                         :failure-kind :recovery-job-not-complete
                         :failure-stage recovery-stage
                         :repair-obligation stop-line
                         :job-id (:job-id snapshot)
                         :job-state (:state snapshot)}))))
                recovered-review-job (when (and snapshot
                                                (= :reviewer-wait recovery-stage))
                                       snapshot)
                recovered-author-job
                (cond
                  (and snapshot (= :author-wait recovery-stage)
                       (:artifact-ref snapshot))
                  snapshot

                  recovered-review-job
                  (get-in stop-line [:failure-data :author-job]))
                _ (when (and recovered-review-job
                             (nil? recovered-author-job))
                    (let [error
                          "Reviewer recovery lacks the original author-job provenance"
                          successor
                          (supersede-recovery!
                           :machine-failure :recovery-provenance-missing
                           :reviewer-wait error
                           {:job-id (:job-id recovered-review-job)})]
                      (throw
                       (ex-info error
                                {:outcome :incomplete
                                 :failure-kind :recovery-provenance-missing
                                 :failure-stage :reviewer-wait
                                 :repair-obligation successor}))))
                _ (when (and snapshot (= :author-wait recovery-stage)
                             (= "done" (:state snapshot))
                             (nil? recovered-author-job))
                    (let [error "Recovered author job completed without an artifact"
                          successor
                          (supersede-recovery!
                           :machine-failure :recovery-artifact-missing
                           :author-wait error
                           {:job-id (:job-id snapshot)
                            :job-state (:state snapshot)})]
                      (throw
                       (ex-info error
                                {:outcome :incomplete
                                 :failure-kind :recovery-artifact-missing
                                 :failure-stage :author-wait
                                 :repair-obligation successor}))))
                fresh-author? (nil? recovered-author-job)
                author-repo (when fresh-author?
                              (target-repository opts entry mission code-state))
                pre-author-head (when fresh-author?
                                  (observe-repo-head opts author-repo))
                author-response
                (run-phase! opts @phase-context :author-dispatch
                            #(if recovered-author-job
                               {:job-id (:job-id recovered-author-job)
                                :state "done"
                                :recovered? true
                                :recovers (:attempt-id stop-line)}
                               (do
                                 (swap! dispatched-turns inc)
                                 ((or (:dispatch-fn opts) dispatch!) opts author
                                  "wm-full-loop" target
                                  (author-prompt (assoc opts :reviewer reviewer)
                                                 target mission construction
                                                 stop-lines)))))
                author-job-id (:job-id author-response)]
            (checkpoint! :dispatch
                         (term {:agent author
                                :availability (if recovered-author-job
                                                :recovered-completion
                                                :invoke-ready)
                                :job-id author-job-id
                                :prompt-ref (str "agency-job:" author-job-id)
                                :recovers (when recovered-author-job
                                            (:attempt-id stop-line))}
                               {:kind (if recovered-author-job
                                        :agency-recovered-completion
                                        :agency-dispatch)
                                :response author-response}))
            (let [author-job
                  (if recovered-author-job
                    recovered-author-job
                    (try
                      (run-phase! opts @phase-context :author-wait
                                  #((or (:poll-fn opts) poll-job!)
                                    opts author-job-id))
                      (catch Throwable e
                        (throw
                         (ex-info (.getMessage e)
                                  (merge (ex-data e)
                                         {:failure-stage :author-wait
                                          :author-job-id author-job-id})
                                  e)))))]
              (when-not (= "done" (:state author-job))
                (throw (ex-info "Author job did not complete"
                                {:outcome :build-failed :author-job author-job})))
              (let [artifact-binding
                    (when fresh-author?
                      (fresh-artifact-binding opts author-repo pre-author-head
                                              author-job))
                    observed-commit (:commit artifact-binding)
                    text-commit (:artifact-ref author-job)
                    _ (when (and fresh-author? text-commit (nil? observed-commit))
                        (throw
                         (ex-info
                          "Agency claimed an author artifact that repository observation did not validate"
                          {:outcome :build-failed
                           :failure-kind :artifact-binding-mismatch
                           :failure-stage :author-wait
                           :target target
                           :author-job author-job
                           :artifact-binding artifact-binding})))
                    commit (if fresh-author? observed-commit text-commit)
                    author-job (cond-> author-job
                                 fresh-author?
                                 (assoc :repo-observed-artifact-ref commit
                                        :artifact-binding artifact-binding))
                    build (run-phase! opts @phase-context :build-resolution
                                      #(when commit
                                         ((or (:resolve-build-fn opts) resolve-build)
                                          commit)))
                    repo (:repo build)
                    files (:files build)]
                (when (and fresh-author? repo
                           (not= repo (:repo artifact-binding)))
                  (throw
                   (ex-info "Observed author artifact resolved outside the target repository"
                            {:outcome :build-failed
                             :failure-kind :artifact-binding-mismatch
                             :failure-stage :build-resolution
                             :target target :commit commit
                             :author-job author-job
                             :artifact-binding artifact-binding
                             :resolved-repository repo})))
                (when-not (and commit repo (vector? files))
                  ;; The author contract offers exactly one legal no-commit
                  ;; ending: a line-anchored REFUSE with a typed reason. That
                  ;; is an agent declining, not a broken machine — class it
                  ;; like :abstained (environmental hold), so the line does
                  ;; not demand a repair commit for a refusal. Fail-closed
                  ;; boundaries: an observed commit outranks any marker (a
                  ;; refusal cannot suppress review of real work), a bare
                  ;; REFUSE without a reason is unverifiable, and a DONE
                  ;; claim without a verifiable commit stays a build failure.
                  (let [{:keys [verdict reason]} (author-verdict author-job)]
                    (when (and (nil? commit)
                               (= :refuse verdict)
                               (not (str/blank? reason)))
                      (throw (ex-info "Author refused with a typed reason"
                                      {:outcome :guardrail-refusal
                                       :failure-kind :guardrail-refusal
                                       :failure-stage :build-resolution
                                       :refusal-reason reason
                                       :target target
                                       :author-job author-job})))
                    (throw (ex-info "Author completed without a verifiable commit"
                                    {:outcome :build-failed
                                     :author-verdict verdict
                                     :author-job author-job}))))
                (when (or (empty? files) (artifact-only-files? files))
                  (throw (ex-info "Authored commit is artifact-only"
                                  {:outcome :artifact-only :commit commit :files files})))
                (when-not (valid-feature-card author-job)
                  (throw
                   (ex-info "Author deliverable is missing a valid feature card"
                            {:outcome :build-failed
                             :failure-kind :feature-card-missing-or-invalid
                             :failure-stage :build-resolution
                             :commit commit
                             :target target
                             :files files
                             :author-job author-job})))
                (let [artifact-snapshot
                      (when fresh-author?
                        {:artifact-binding/fresh-author? true
                         :artifact-binding/repo (:repo artifact-binding)
                         :artifact-binding/reviewer-commit commit
                         :artifact-binding/pre-dispatch-head
                         (:pre-dispatch-head artifact-binding)
                         :artifact-binding/author-window-start-ms
                         (:author-window-start-ms artifact-binding)
                         :artifact-binding/author-window-end-ms
                         (:author-window-end-ms artifact-binding)
                         :artifact-binding/failed-commits
                         (vec (keep :failed-commit stop-lines))})
                      review-response
                      (run-phase!
                       opts (cond-> @phase-context
                              artifact-snapshot
                              (assoc :tripwire/snapshot artifact-snapshot))
                       :reviewer-dispatch
                       #(if recovered-review-job
                          {:job-id (:job-id recovered-review-job)
                           :state "done" :recovered? true
                           :recovers (:attempt-id stop-line)}
                          (do
                            (swap! dispatched-turns inc)
                            ((or (:dispatch-fn opts) dispatch!) opts reviewer
                             "wm-full-loop" target
                             (reviewer-prompt (assoc opts :reviewer reviewer)
                                              target construction repo commit
                                              author-job stop-lines)))))
                      review-job
                      (if recovered-review-job
                        recovered-review-job
                        (try
                          (run-phase! opts @phase-context :reviewer-wait
                                      #((or (:poll-fn opts) poll-job!) opts
                                        (:job-id review-response)))
                          (catch Throwable e
                            (throw
                             (ex-info (.getMessage e)
                                      (merge (ex-data e)
                                             {:failure-stage :reviewer-wait
                                              :review-job-id
                                              (:job-id review-response)
                                              :author-job author-job
                                              :commit commit
                                              :repository repo
                                              :files files})
                                      e)))))
                      review-gate (review-execution-gate files review-job)
                      approved? (and (= "done" (:state review-job))
                                     (= :approve (review-verdict review-job))
                                     (:passed? review-gate))]
                  (checkpoint! :build
                               (term {:artifacts files
                                      :generated-code files
                                      :commits [commit]
                                      :patterns-used (vec (:shown construction))
                                      :inline-improvements []
                                      :validation {:author (:execution author-job)
                                                   :reviewer (:execution review-job)
                                                   :review-job (:job-id review-job)
                                                   :approved? approved?
                                                   :review-gate review-gate
                                                   :artifact-binding
                                                   artifact-binding}}
                                     {:kind :git-commit-and-independent-review
                                      :repository repo}))
                  (when-not approved?
                    (let [failure-data
                          (cond->
                           {:outcome :build-failed :author-job author-job
                           :review-job review-job :commit commit
                           :target target
                           :selected-entry
                           (select-keys entry
                                        [:action :controller-score :G-efe])}
                            (not (:passed? review-gate))
                            (assoc :failure-kind
                                   :review-execution-evidence-missing
                                   :failure-stage :reviewer-wait
                                   :review-gate review-gate))
                          recovery-rejection
                          (when (and recovered-review-job
                                     (= :incomplete-recoverable
                                        (:repair/class stop-line)))
                            (let [finding
                                  ((or (:repair-record-fn opts)
                                       repair/record-review-failure!)
                                   {:attempt-id attempt-id
                                    :target target
                                    :commit commit
                                    :selected-entry (:selected-entry failure-data)
                                    :reviewer reviewer
                                    :review-job (:job-id review-job)
                                    :review-verdict (review-verdict review-job)
                                    :review-text (job-text review-job)})]
                              ((or (:repair-supersede-fn opts)
                                   repair/supersede!)
                               stop-line finding :recovered-review-rejected)
                              finding))]
                      (throw (ex-info (if (:passed? review-gate)
                                        "Independent review did not approve"
                                        "Independent review lacks execution evidence")
                                      (cond-> failure-data
                                        recovery-rejection
                                        (assoc :repair-obligation
                                               recovery-rejection))))))
                  (let [witness
                        (run-phase! opts @phase-context :grounding
                                    #((or (:ground-fn opts) ground-commit!)
                                      attempt-id target author reviewer repo commit files
                                      construction review-job opts))]
                    (when (seq stop-lines)
                      (run-phase!
                       opts @phase-context :stop-line-resolution
                       #(doseq [obligation stop-lines]
                          (if (= :incomplete-recoverable
                                 (:repair/class obligation))
                            ((or (:repair-resolve-fn opts) repair/resolve!)
                             obligation
                             {:attempt-id attempt-id :commit commit
                              :reviewer reviewer
                              :review-job (:job-id review-job)
                              :witness witness
                              :validation {:kind :recovered-existing-artifact
                                           :production-shaped? true
                                           :recovers (:attempt-id obligation)}})
                            ((or (:repair-implement-fn opts)
                                 repair/record-implementation!)
                             obligation
                             {:attempt-id attempt-id :commit commit
                              :reviewer reviewer
                              :review-job (:job-id review-job)
                              :witness witness})))))
                    ;; A successfully grounded recovery is itself a real,
                    ;; production-shaped successor.  It may therefore validate
                    ;; an older implemented machine repair while discharging
                    ;; its own recoverable obligation.
                    (when (and (seq validation-lines)
                               (:resolved? witness) (:dial-moved? witness))
                      (run-phase!
                       opts @phase-context :stop-line-validation
                       #(doseq [obligation validation-lines]
                          ((or (:repair-resolve-fn opts) repair/resolve!)
                           obligation
                           {:attempt-id attempt-id :commit commit
                            :reviewer @reviewer-of-record
                            :review-job (:job-id review-job)
                            :witness witness
                            :validation {:kind :production-shaped-successor
                                         :production-shaped? true}}))))
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
                    (let [grounded? (and (:resolved? witness)
                                         (:dial-moved? witness))
                          feature-card
                          (when grounded?
                            (grounded-feature-card repo files mission entry
                                                   author-job review-job))]
                      (close! (if grounded?
                                :grounded-change
                                :grounded-no-change)
                              (cond->
                               {:target target :commit commit :witness witness
                                :author-job author-job :review-job review-job
                                :artifact-binding artifact-binding}
                                feature-card
                                (assoc :feature-card feature-card))))))))))))
      (catch Throwable e
        (if @closing?
          (throw e)
          (let [failure (ex-data e)
                review-job (:review-job failure)
                verdict (some-> review-job review-verdict)
                review-finding (when (and (nil? (:repair-obligation failure))
                                   (:commit failure)
                                   (#{:request-changes :reject} verdict))
                          ((or (:repair-record-fn opts)
                               repair/record-review-failure!)
                           {:attempt-id attempt-id
                            :target (:target failure)
                            :commit (:commit failure)
                            :selected-entry (:selected-entry failure)
                            :reviewer @reviewer-of-record
                            :review-job (:job-id review-job)
                            :review-verdict verdict
                            :review-text (job-text review-job)}))
                finding (or review-finding (:repair-obligation failure))]
              (close! (outcome-from e)
                      {:target (or (:target failure)
                                   (some-> @checkpoints :selection :judgment
                                           :selected-mission))
                       :commit (:commit failure)
                       :witness (:witness failure)
                       :author-job (:author-job failure)
                       :review-job review-job
                       :repair-obligation finding
                       :failure-kind (failure-kind-from e)
                       :failure-stage (or (:failure-stage failure)
                                          (last-error-phase @phase-events))
                       :error (.getMessage e)
                       :error-class (.getName (class e))
                       :error-data failure})))))))

(defn run-opportunity!
  "Run one opportunity and ensure initialization failures also become durable
  stop-line findings. Failures after cohort start are closed by the core state
  machine; this outer boundary covers phase-log and cohort-start failures that
  necessarily occur before an ordinary attempt can own them."
  [raw-opts]
  (try
    (run-opportunity-core! raw-opts)
    (catch Throwable e
      (let [attempt-id (str "initialization-" (UUID/randomUUID))
            trigger (or (:trigger raw-opts) :duree-click-on-demand)
            error (if (str/blank? (str (.getMessage e)))
                    "Full-loop initialization failed"
                    (.getMessage e))
            finding
            ((or (:repair-system-record-fn raw-opts)
                 repair/record-system-failure!)
             {:attempt-id attempt-id
              :repair-class :machine-failure
              :failure-stage :initialization
              :outcome :incomplete
              :failure-kind :initialization-failed
              :error error
              :failure-data (ex-data e)
              :backtrace {:error-class (.getName (class e))}
              :discharge-contract (discharge-contract :machine-failure)})
            brief-ref
            ((or (:queue-fn raw-opts) brief/queue-item!)
             {:attempt-id attempt-id
              :trigger trigger
              :batch-id (:batch-id raw-opts)
              :outcome :incomplete
              :author (or (:author raw-opts) default-author)
              :reviewer (or (:reviewer raw-opts) default-reviewer)
              :achievement {:tier :none
                            :summary "No achievement; initialization stopped the line"}
              :failure {:kind :initialization-failed
                        :stage :initialization
                        :error error
                        :repair-id (:repair/id finding)
                        :discharge-contract (:discharge-contract finding)}})]
        {:attempt-id attempt-id
         :outcome :incomplete
         :checkpoints {}
         :morning-brief-ref brief-ref
         :data {:repair-obligation finding
                :failure-kind :initialization-failed
                :failure-stage :initialization
                :error error
                :error-data (ex-data e)}}))))
