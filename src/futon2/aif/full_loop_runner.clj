(ns futon2.aif.full-loop-runner
  "Authoritative real-actuation runner for one War Machine opportunity.

  One opportunity selects exactly one strategic action, constructs for that
  decision, dispatches an author, dispatches a distinct reviewer, verifies the
  resulting commit, records a typed implementation/discharge in Futon1b, queues
  Morning Brief QA, and closes every preregistered checkpoint. The deterministic
  fold executor is not an actuator here."
  (:require [futon2.aif.load-identity :as load-identity]
            [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.c-vector :as cv]
            [futon2.aif.cascade-sources :as cascade-sources]
            [futon2.aif.cascade-structure :as cascade-structure]
            [futon2.aif.cascade-habit-store :as cascade-habit]
            [futon2.aif.cascade-habit-reinforcement :as habit-reinforcement]
            [futon2.aif.cascade-plan :as cascade-plan]
            [futon2.aif.scoring-input-receipts :as input-receipts]
            [futon2.aif.scan-report :as scan-report]
            [futon2.aif.close-loop :as close-loop]
            [futon2.aif.close-retention :as close-retention]
            [futon2.aif.token-outcome :as token-outcome]
            [futon2.aif.surprise :as surprise]
            [futon2.aif.route-attestation :as route-attestation]
            [futon2.aif.run-ending-classification :as run-ending]
            [futon2.aif.kernel-example :as kernel-example]
            [futon2.aif.attempt-learning :as attempt-learning]
            [futon2.aif.learning-trial-ledger :as learning-ledger]
            [futon2.aif.evidence-manifest :as evidence-manifest]
            [futon2.aif.fold-classical :as fold-classical]
            [futon2.aif.fold-cascade :as fold-cascade]
            [futon2.aif.fold :as fold]
            [futon2.aif.delivery-qa :as delivery-qa]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.g-term-decomposition :as decomposition]
            [futon2.aif.limb-evidence :as limb-evidence]
            [futon2.aif.job-text-retention :as job-texts]
            [futon2.aif.interpretation-evidence :as interpretation-evidence]
            [futon2.aif.interpretation-job :as interpretation-job]
            [futon2.aif.fact-measurement :as measurement]
            [futon2.aif.task-execution-evidence :as task-execution]
            [futon2.aif.accepted-increment :as accepted-increment]
            [futon2.aif.d-predecessor-task-authority :as d-task]
            [futon2.aif.receipt-construction :as receipt-construction]
            [futon2.aif.mission-registry :as missions]
            [futon2.aif.morning-brief :as brief]
            [futon2.aif.pattern-registry :as patterns]
            [futon2.aif.run-participants :as participants]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.repair-discharge :as repair-discharge]
            [futon2.aif.repair-discharge-receipt :as discharge-receipt]
            [futon2.aif.repair-evaluators :as repair-evaluators]
            [futon2.aif.substrate :as substrate]
            [futon2.aif.trace :as trace]
            [futon2.aif.tripwire :as tripwire]
            [futon2.report.cascade-lane :as cascade]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.security MessageDigest]
           [java.time Instant]
           [java.util UUID]
           [java.util.concurrent Executors ThreadFactory]))

(load-identity/register! *ns* *file*)

(def default-agency-base "http://127.0.0.1:7070")
(def default-substrate-base "http://127.0.0.1:7073")
(def default-author "zai-5")
(def default-reviewer "codex-7")
(def default-repair-reviewer "codex-1")
(def default-phase-log "/home/joe/code/futon2/data/wm-full-loop-phases.edn.log")
(def default-run-record-dir
  "Where a run drops its receipt when the caller names no directory. Under
  data/ (gitignored), not the lab root: 148 receipts accumulated there and NO
  consumer in futon2 could read one of them, because every reader matches
  `^tick-run-record-(\\d{4}-\\d{2}-\\d{2})-(.+)\\.edn$` and a bare-UUID id
  has no date (claude-7's inbox-zero analysis, 2026-09-17). A caller that
  wants a receipt kept as evidence passes :run-record-dir explicitly."
  "/home/joe/code/futon2/data/wm-runs")
(def default-agent-budget-ms (* 45 60 1000))
(def semantic-epoch :full-loop-real-actuation-v6)
(def required-checkpoints [:selection :construction :dispatch :build :adjudication])
(def discrimination-top-k 5)
(def discrimination-epsilon 1.0e-6)
(def artifact-window-tolerance-ms (* 2 60 1000))
(def default-build-cure-retries 1)
(def default-author-infrastructure-retries 1)
(def default-revision-rounds 1)
(def ^:private historical-verification-completion-token (Object.))
(def readiness-wake-timeout-ms 30000)
(def substrate-retry-delay-ms 5000)
(def strategic-selection-retry-delay-ms 5000)

(def substrate-retry-timeouts-ms
  "Escalating per-attempt preflight timeouts. A busy-but-healthy store (the
  attempt-055 signature: shallow health in single-digit ms, entity route
  queued behind running query portals) passes on a later, more patient
  attempt; a dead store still fails the fast first probe."
  [15000 30000 60000])

(def strategic-selection-retry-timeouts-ms
  "Escalating per-attempt budgets for reason-bearing strategic selection.
  The first budget clears the observed 130s loaded success; later attempts
  tolerate a progressively busier serving JVM. An explicit
  :strategic-selection-timeout-ms pins all attempts."
  [150000 210000 270000])

(def default-substrate-health-url "http://127.0.0.1:7072/health")
(def wm-agent-id "war-machine")

(def ^:dynamic *wm-status-reporting?*
  "Production status reporting is on. Tests bind this false so they never
  mutate a live Agency roster."
  true)

(def ^:dynamic *r16-park-fn*
  "Hermetic override for the durable R16 park boundary. Production is nil and
  therefore uses Agency; the runner suite binds an in-memory recorder."
  nil)

(defonce ^:private wm-status-executor
  (Executors/newSingleThreadExecutor
   (reify ThreadFactory
     (newThread [_ runnable]
       (doto (Thread. runnable "wm-status-reporter")
         (.setDaemon true))))))

(defn- wm-status-payload
  [context event]
  (let [phase (:phase event)
        transition (:transition event)
        attempt (or (:attempt-id context) (:opportunity-id context) "pending")
        trigger (or (:trigger context) :unknown)]
    (cond
      (and (= :opportunity phase) (= :end transition))
      {:source "wm-full-loop" :status "idle"}

      (= :start transition)
      {:source "wm-full-loop"
       :status "invoking"
       :activity (str (name phase) " " attempt " (" (name trigger) ")")}

      :else nil)))

(defn- post-wm-status!
  [{:keys [agency-base]} payload]
  (when (and *wm-status-reporting?* payload)
    (.execute
     wm-status-executor
     ^Runnable
     (fn []
       (try
         (http/post (str agency-base "/api/alpha/agents/" wm-agent-id "/status")
                    {:headers {"Content-Type" "application/json"}
                     :body (json/generate-string payload)
                     :timeout 1500
                     :throw false})
         (catch Throwable _ nil))))))

(defn ensure-dispatch-seat!
  "Register the wm-full-loop dispatch seat so child jobs can reply in-thread.

  Attempt-002 of repair-ea1-3f4cac (2026-09-13) recorded TWO delivery losses
  downstream of the same missing registration: the author job's terminal
  delivery answered `caller-not-a-registered-seat`, and the reviewer's
  in-thread reply 404'd with `agent-not-found` for wm-full-loop. The
  build-loop precedent (wm-build-loop.sh ensure_seats) registers its two
  seats at start for exactly this reason; registration is idempotent (a
  duplicate answers 409 and is ignored). Fail-open with a stderr note only:
  a registration outage must not consume the attempt it was protecting."
  [{:keys [agency-base]}]
  (try
    (http/post (str agency-base "/api/alpha/agents")
               {:headers {"Content-Type" "application/json"}
                :body (json/generate-string
                       {:agent-id "wm-full-loop"
                        :type "claude"
                        :delivery-mode "inbox"})
                :timeout 2000
                :throw false})
    (catch Throwable _
      ;; A connection failure is the normal condition of a lap or test
      ;; without a live Agency (round-2 review: the port-1 run printed one
      ;; line per test); only a REACHABLE endpoint answering badly is
      ;; worth stderr.
      nil)))

(defn- report-wm-phase!
  [opts context event]
  (when-let [payload (wm-status-payload context event)]
    (post-wm-status! opts payload))
  (when (and (= :start (:transition event))
             (:wm-phase-state opts))
    (reset! (:wm-phase-state opts)
            {:phase (:phase event)
             :context context})))

(defn- report-wm-wait!
  [opts job first-poll-ms]
  (let [{:keys [phase context]} (or (some-> opts :wm-phase-state deref) {})
        attempt (or (:attempt-id context) (:opportunity-id context) "pending")
        elapsed-s (quot (- (System/currentTimeMillis) first-poll-ms) 1000)]
    (post-wm-status!
     opts
     {:source "wm-full-loop"
      :status "invoking"
      :activity (str (name (or phase :agent-wait)) " " attempt
                     " job " (:job-id job) " " elapsed-s "s")})))

(defn park-r16-stop-line!
  "Register the failed R16 attempt as a durable parked transition.

  The repair obligation is the dependency: resolving it is what permits a
  later continuation. A stop-line record alone is not a parked lifecycle
  transition, so failure to register this record refuses closure."
  [{:keys [agency-base] :as opts} attempt-id finding]
  (if-let [park-fn (or (:r16-park-fn opts) *r16-park-fn*)]
    (park-fn attempt-id finding)
    (let [repair-id (:repair/id finding)
          _ (when-not (and (string? repair-id) (not (str/blank? repair-id)))
              (throw (ex-info "R16 stop-line has no durable repair identity"
                              {:failure-kind :r16-park-repair-id-missing
                               :failure-stage :parked
                               :attempt-id attempt-id})))
          response
          (http/post
           (str agency-base "/api/alpha/park")
           {:headers {"Content-Type" "application/json"}
            :body (json/generate-string
                   {:agent wm-agent-id
                    :surface "morning-brief"
                    :mode "between-turn"
                    :awaiting [repair-id]
                    :payload {:node "R16"
                              :lifecycle/stage "parked"
                              :attempt-id attempt-id
                              :repair-id repair-id}})
            :timeout 5000
            :throw false})
          body (when (string? (:body response))
                 (json/parse-string (:body response) true))]
      (when-not (and (= 200 (:status response)) (:ok body)
                     (= "parked" (some-> (:status body) keyword name)))
        (throw (ex-info "R16 stop-line park registration failed"
                        {:failure-kind :r16-park-registration-failed
                         :failure-stage :parked
                         :attempt-id attempt-id
                         :repair-id repair-id
                         :response/status (:status response)
                         :response/body body})))
      body)))

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
           :build-cure-retries
           (let [env-val (System/getenv "FUTON_WM_BUILD_CURE_RETRIES")
                 parsed (when env-val (try (Integer/parseInt env-val)
                                           (catch Exception _ nil)))]
             (or parsed default-build-cure-retries))
           :revision-rounds default-revision-rounds
           :author-infrastructure-retries default-author-infrastructure-retries
           :trigger :duree-click-on-demand
           :delivery-qa-fn delivery-qa/emit!
           :cohort? true
           :semantic-epoch semantic-epoch}
          opts)))

(defn emit-phase!
  "Emit one line-oriented phase event to stdout and the durable operator log."
  [opts context event]
  (let [record (merge {:at (str (Instant/now))} context event)
        _ (report-wm-phase! opts context event)
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
  ([opts context phase thunk]
   (run-phase! opts context phase thunk nil))
  ([opts context phase thunk result->event]
   (let [started (System/currentTimeMillis)]
     (emit-phase! opts context {:phase phase :transition :start})
     (try
       (let [result (thunk)
             detail (if result->event (or (result->event result) {}) {})]
         (emit-phase! opts context
                      (merge {:phase phase :transition :end :outcome :ok
                              :duration-ms (- (System/currentTimeMillis) started)}
                             detail))
         result)
       (catch Throwable e
         (emit-phase! opts context {:phase phase :transition :end :outcome :error
                                    :duration-ms (- (System/currentTimeMillis) started)
                                    :error-class (.getName (class e))
                                    :error (.getMessage e)})
         (throw e))))))

(defn- sha256 [x]
  (let [bytes (.digest (MessageDigest/getInstance "SHA-256")
                       (.getBytes (pr-str x) "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff %)) bytes))))

(defn- route-node-str [node]
  (cond
    (keyword? node) (name node)
    (symbol? node) (str node)
    (string? node) node
    :else (str node)))

(defn- observed-route [tags]
  (->> tags
       (partition 2 1)
       (mapv (fn [[from to]]
               {:fromNode (route-node-str (:node from))
                :toNode (route-node-str (:node to))
                :via (:via to)
                :at_ (:at to)}))))

(defn- packet-run-route
  "Route hops for a click result, from the selection seam's own :wm/route
   when present, else the observed packet boundary.

   RUN4 operator-selected production packets carry no selection-judgment
   :wm/route (only the trace-selection seam emits one), so without the
   fallback every production click ended grounded with an EMPTY route:
   persist-run-record! wrote nothing and the terminal projection then
   refused :missing-run-record AFTER the work was banked (2026-09-11
   codex20 click; 2026-09-12 u88-zai-successor click wm-click-c398aea7,
   grounded commit 5d595dc9). Historical admissions keep their explicit
   STOP_LINE->HISTORICAL_VERIFICATION hops."
  ([selection-judgment selection-ground outcome trace-path]
   (packet-run-route selection-judgment selection-ground outcome trace-path nil))
  ([selection-judgment selection-ground outcome trace-path
    {:keys [selected-action]}]
  (let [selected-action (or selected-action (:selected-action selection-judgment))
        repair-route? (= :repair-machine-failure (:type selected-action))
        historical-route?
        (and (= :historical-verification-awaiting-validation outcome)
             (= :revalidate-historical-repair
                (get-in selection-ground [:run4/enacted-action :type])))
        run4-production-pin?
        (some-> (get-in selection-ground [:run4/task-pin]) not-empty)]
    (cond-> (vec (:wm/route selection-judgment))
      (and historical-route? (empty? (:wm/route selection-judgment)))
      (into [{:node :STOP_LINE :via :repair-obligation
              :at (str (Instant/now))}
             {:node :HISTORICAL_VERIFICATION
              :via :verified-admission
              :at (str (Instant/now))}])
      (and repair-route? (not historical-route?)
           (empty? (:wm/route selection-judgment)))
      (into [{:node :STOP_LINE :via :repair-obligation
              :at (str (Instant/now))}
             {:node :FULL_LOOP_CLOSE :via outcome
              :at (str (Instant/now))}])
      (and run4-production-pin? (not historical-route?)
           (empty? (:wm/route selection-judgment)))
      (into [{:node :RUN4_PACKET
              :via :operator-selected-packet
              :at (str (Instant/now))}
             {:node :FULL_LOOP_CLOSE
              :via outcome
              :at (str (Instant/now))}])
      trace-path
      (conj {:node :TRACE
             :via "futon2.aif.trace/write-trace!"
             :at (str (Instant/now))})))))

(defn- terminal-record-context [raw-opts result]
  (let [selected-action (get-in result [:checkpoints :selection :judgment
                                        :selected-action])
        requested-pin (:run4/requested-pin raw-opts)
        failure-kind (get-in result [:data :failure-kind])
        failure-stage (get-in result [:data :failure-stage])]
    (cond
      (= :cohort-complete (:outcome result))
      {:kind :cohort-stopping-rule
       :outcome :cohort-complete
       :target (get-in result [:data :target])
       :attempted (get-in result [:data :attempted])}

      (= :initialization failure-stage)
      {:kind (or failure-kind :initialization-failed)
       :outcome :incomplete
       :failure-stage :initialization}

      (= :repair-machine-failure (:type selected-action))
      (cond-> {:kind (or failure-kind (:outcome result))
               :outcome (:outcome result)
               :repair-id (get-in selected-action [:repair-obligation :repair/id])
               :enacted-action selected-action}
        requested-pin (assoc :requested-not-enacted requested-pin))

      :else nil)))

(defn- terminal-fallback-route [result]
  (cond
    (= :cohort-complete (:outcome result))
    [{:node :COHORT :via :stopping-rule-reached :at (str (Instant/now))}
     {:node :STOPPING_RULE :via :cohort-complete :at (str (Instant/now))}]

    (= :initialization (get-in result [:data :failure-stage]))
    [{:node :INITIALIZATION
      :via (or (get-in result [:data :failure-kind]) :initialization-failed)
      :at (str (Instant/now))}
     {:node :FULL_LOOP_CLOSE :via :incomplete :at (str (Instant/now))}]

    :else
    [{:node :RUNNER :via :terminal-result :at (str (Instant/now))}
     {:node :FULL_LOOP_CLOSE :via (or (:outcome result) :unknown)
      :at (str (Instant/now))}]))

(def ^:private canonical-runner-path
  "/home/joe/code/futon2/src/futon2/aif/full_loop_runner.clj")

(defn- sha256-bytes
  "Digest the BYTES themselves. The generic sha256 hashes (pr-str x); a Java
  byte-array's pr-str carries object identity, so equal sources hashed as
  false drift (round-2 review of repair-ea1-3f4cac attempt-003: the fresh-JVM
  suite warned drift on every run)."
  [^bytes b]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") b)]
    (apply str (map #(format "%02x" (bit-and 0xff %)) d))))

(defn- canonical-runner-bytes
  []
  (try (java.nio.file.Files/readAllBytes
        (java.nio.file.Path/of canonical-runner-path (make-array String 0)))
       (catch Throwable _ nil)))

(defn runner-source-drift
  "Compare source digests sampled during namespace loading with canonical disk.
   :namespaces reports the registered and explicitly required decision/close
   scope, including unregistered namespaces. This is not bytecode identity:
   edits during compilation, partial loads and later Var mutation are outside
   the capture guarantee. Only this runner's :drift retains refusal authority."
  ([]
   (runner-source-drift (fn [path]
                          (if (= path canonical-runner-path)
                            (canonical-runner-bytes)
                            (load-identity/read-bytes path)))))
  ([canonical-read]
   (let [reports (load-identity/report load-identity/required-sources canonical-read)
         own (get reports 'futon2.aif.full-loop-runner)
         loaded (get-in own [:loaded-source :sha256])
         disk (:disk-sha256 own)]
     (cond-> {:runner/source-check (case (:status own) :stale :drift (:status own))
              :runner/loaded-present? (some? loaded)
              :runner/canonical-present? (some? disk)
              :runner/loaded-sha256 loaded :runner/canonical-sha256 disk
              :identity-kind :source-digest-at-namespace-load
              :namespaces reports}
       (= :current (:status own)) (assoc :runner/sha256 loaded)))))

(defn- refuse-on-runner-source-drift!
  "Check BEFORE the attempt runs and refuse consumption on drift (round-2
  review: detection after execution lets a stale runner judge the attempt it
  should never have judged). :unavailable stays fail-open -- a dev checkout
  without the canonical file must not brick the loop -- but drift refuses."
  []
  (let [check (runner-source-drift)]
    (when (= :drift (:runner/source-check check))
      (binding [*out* *err*]
        (println "[wm-full-loop] REFUSING: serving runner source drifts from"
                 (str canonical-runner-path "; reload the namespace from the"
                      " canonical checkout before running attempts.")))
      (throw (ex-info "Serving runner source drifts from the canonical checkout"
                      {:outcome :build-failed
                       :failure-kind :stale-runner-source
                       :failure-stage :runner-source
                       :runner/source check})))
    check))

(defn- persist-run-record!
  [raw-opts run-id started-at result]
  (let [observed (observed-route (:wm/route result))
        route (if (seq observed)
                observed
                (observed-route (terminal-fallback-route result)))]
    (if (seq route)
      (let [dir (io/file (or (:run-record-dir raw-opts) default-run-record-dir))
            target (io/file dir (str "tick-run-record-" run-id ".edn"))
            tmp (io/file dir (str "." (.getName target) "." (UUID/randomUUID) ".tmp"))
            pin-identity (or (get-in result [:checkpoints :selection :ground
                                             :run4/task-pin])
                             (get-in result [:checkpoints :construction :judgment
                                             :run4/task-pin]))
            environment-attestation
            (get-in result [:checkpoints :selection :ground
                            :run4/operator-selection :authority-attestation
                            :effective-environment])
            terminal-context (terminal-record-context raw-opts result)
            decision (or (get-in result [:checkpoints :selection :judgment :controller-decision])
                         (get-in result [:checkpoints :selection :judgment :decision]))
            record (cond-> {:run/id run-id
                    :runner/source (:runner/source result)
                    :participants (participants/record-value raw-opts)
                    :habit-reads (input-receipts/habit-log
                                  (some-> (:habit-reads/state raw-opts) deref))
                    :declaration-reads (cascade-sources/provenance
                                        (some-> (:declaration-reads/state raw-opts) deref))
                    :click/id (:click-id raw-opts)
                    :startedAt started-at
                    :selectorSeam "live:validated-selection"
                    :selection-event (habit-reinforcement/selection-event decision)
                    :habit-reinforcement (or (:habit-reinforcement result)
                                             (habit-reinforcement/evaluate decision (:outcome result) nil))
                    :scan-report (scan-report/retain!
                                  target (some-> (:scan-report/state raw-opts) deref)
                                  (or (:scan-render-fn raw-opts) wm/render-war-machine))
                    ;; This tick's accounts travel with its retained decision;
                    ;; never reconstruct them from a newer trace or corpus.
                    :mission-hole-coverage (or (:mission-hole-coverage decision)
                                               {:status :absent :reason :coverage-not-recorded})
                    :live-c-coverage (or (:live-c-coverage decision)
                                         {:status :absent :reason :coverage-not-recorded})
                    :traceWritten (boolean (:trace-path result))
                    ;; Only this run's retained selection supplies validity
                    ;; quantities. No historical checkpoints or trace lookup.
                    :decision (assoc (select-keys decision
                                                  [:selection-law :selection-certificate
                                                   :initial-belief-receipt :enumeration-completeness])
                                     :g-term-decomposition (decomposition/from-result result))
                    :route route
                    :repair/discharge (:repair/discharge result)
                    :repair/publication (:repair/publication result)
                    :d-task-enactment (:d-task-enactment result)
                    :route-attestation-ref (:route-attestation-ref result)
                    :job-liveness (vec (some-> (:job-liveness/state raw-opts) deref))}
                     (get-in result [:checkpoints :selection :judgment :open-stop-lines])
                     (assoc :open-stop-lines
                            (get-in result [:checkpoints :selection :judgment :open-stop-lines]))
                     (:execution-cohort raw-opts)
                     (assoc :execution-cohort
                            (merge (select-keys (:execution-cohort raw-opts)
                                                [:cohort-id :sha256])
                                   (cohort/lineage-history (:execution-cohort raw-opts))))
                     (= :history-identity-unavailable (get-in result [:data :failure-kind]))
                     (assoc :history-admission-refusal (get-in result [:data :error-data]))
                     (:execution-identity result)
                     (assoc :runner-execution/identity
                            (:execution-identity result)
                            :runner-execution/provenance
                            (:execution-provenance result))
                     pin-identity (assoc :run4/task-pin pin-identity)
                     (= :historical-verification-awaiting-validation (:outcome result))
                     (assoc :runner-attempt/id (:attempt-id result)
                            :run4/requested-pin (:run4/requested-pin raw-opts)
                            :run4/controller-attempt-id
                            (:run4/controller-attempt-id raw-opts)
                            :run4/enacted-action
                            (get-in result [:checkpoints :selection :judgment
                                            :selected-action])
                            :historical-verification
                            (get-in result [:data :repair-obligation]))
                     environment-attestation
                     (assoc :run4/effective-environment-attestation
                            environment-attestation)
                     terminal-context
                     (assoc :terminal terminal-context))]
        (io/make-parents target)
        (spit tmp (str (pr-str record) "\n"))
        (java.nio.file.Files/move
         (.toPath tmp) (.toPath target)
         (into-array java.nio.file.StandardCopyOption
                     [java.nio.file.StandardCopyOption/ATOMIC_MOVE
                      java.nio.file.StandardCopyOption/REPLACE_EXISTING]))
        {:run-record-status :present
         :run-record (.getAbsolutePath target)})
      ;; terminal-fallback-route is total for wrapper results. Retain the guard
      ;; as a fail-closed invariant rather than silently publishing no record.
      (throw (ex-info "Terminal result has no recordable route"
                      {:failure-kind :terminal-route-missing
                       :outcome (:outcome result)})))))

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

(defn- observe-repo-head [opts repo] (task-execution/observe-repo-head opts repo))
(defn fresh-artifact-binding [opts repo before job]
  (task-execution/fresh-artifact-binding opts repo before job))

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

(defn- agent-record [roster agent]
  (or (get roster (keyword agent)) (get roster agent)))

(defn- available? [roster agent]
  (let [a (agent-record roster agent)]
    (and a (true? (:invoke-ready? a)) (= "idle" (name (:status a))))))

(defn- restored? [roster agent]
  (some-> (agent-record roster agent) :status name (= "restored")))

(defn- agent-failure-detail [roster agent]
  (let [status (some-> (agent-record roster agent) :status name)]
    (cond
      (= "restored" status) :restored-unwoken
      (= "invoking" status) :busy
      :else :unreachable)))

(defn wake-agent!
  "Send one bounded readiness whistle to an invoke-ready restored agent."
  [opts agent]
  (let [r (http/post (str (:agency-base opts) "/api/alpha/whistle")
                     {:headers {"Content-Type" "application/json"}
                      :body (json/generate-string
                             {:agent-id agent
                              :caller "wm-full-loop"
                              :prompt "ping: readiness wake check - reply ok"
                              :timeout-ms readiness-wake-timeout-ms})
                      :timeout readiness-wake-timeout-ms
                      :throw false})]
    (when (<= 200 (:status r) 299)
      (try (json/parse-string (:body r) true)
           (catch Throwable _ {:ok true :raw (:body r)})))))

(defn- wake-restored-agent!
  "Wake `agent` only when its observed status is restored, then re-read roster."
  [opts roster agent]
  (if-not (restored? roster agent)
    {:roster roster
     :readiness/wake-attempted false
     :readiness/wake-result :not-needed}
    (do
      (try
        ((or (:wake-agent-fn opts) wake-agent!) opts agent)
        (catch Throwable _ nil))
      (let [refreshed (try
                        ((or (:roster-fn opts) agent-roster) (:agency-base opts))
                        (catch Throwable _ roster))]
        {:roster refreshed
         :readiness/wake-attempted true
         :readiness/wake-result (if (available? refreshed agent)
                                  :woken
                                  :no-reply)}))))

(defn agent-readiness!
  "Observe one required agent and attempt one wake when it is restored."
  [opts agent]
  (let [roster ((or (:roster-fn opts) agent-roster) (:agency-base opts))]
    (wake-restored-agent! opts roster agent)))

(defn- readiness-event [result]
  (select-keys result [:readiness/wake-attempted :readiness/wake-result
                       :readiness/substrate-transient]))

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

(defn- substrate-attempt [probe-fn opts]
  (try
    {:value (probe-fn opts)}
    (catch Throwable e {:error e})))

(defn substrate-liveness!
  "Cheap liveness probe against the store's dedicated health port, used only
  to classify a preflight exhaustion — it never rescues one. Returns
  {:alive? bool :latency-ms n} or {:alive? false :error str}."
  [opts]
  (let [url (or (:substrate-health-url opts)
                (System/getenv "FUTON_SUBSTRATE_HEALTH_URL")
                default-substrate-health-url)
        started (System/currentTimeMillis)]
    (try
      (let [r (http/get url {:timeout 2500 :throw false})]
        {:alive? (<= 200 (:status r) 299)
         :status (:status r)
         :latency-ms (- (System/currentTimeMillis) started)})
      (catch Throwable e
        {:alive? false :error (.getMessage e)
         :latency-ms (- (System/currentTimeMillis) started)}))))

(defn- resolve-liveness-fn
  "Real probe only when the preflight itself is real: a stubbed preflight
  means no live substrate is in play, so a real health call would classify
  against the wrong world."
  [opts]
  (or (:substrate-liveness-fn opts)
      (when-not (:substrate-preflight-fn opts) substrate-liveness!)))

(defn substrate-readiness!
  "Run one substrate probe plus exactly two delayed retries before failing.
  Retries escalate the probe timeout (substrate-retry-timeouts-ms) so a
  busy-but-healthy store gets a patient attempt; an explicit
  :substrate-preflight-timeout-ms pins all attempts. On exhaustion the
  thrown ex-data carries :substrate-liveness and :substrate-state
  (:alive-but-slow | :unreachable | :liveness-unknown) so congestion and
  outage stop sharing one label."
  [opts]
  (let [probe-fn (or (:substrate-preflight-fn opts) substrate-preflight!)
        sleep-fn (or (:readiness-sleep-fn opts) #(Thread/sleep %))
        timeout-for (fn [i]
                      (or (:substrate-preflight-timeout-ms opts)
                          (nth substrate-retry-timeouts-ms i
                               (peek substrate-retry-timeouts-ms))))
        attempt (fn [i]
                  (substrate-attempt
                   probe-fn (assoc opts :substrate-preflight-timeout-ms
                                   (timeout-for i))))
        first-attempt (attempt 0)]
    (if-not (:error first-attempt)
      (:value first-attempt)
      (do
        (sleep-fn substrate-retry-delay-ms)
        (let [second-attempt (attempt 1)]
          (if-not (:error second-attempt)
            (assoc (:value second-attempt) :readiness/substrate-transient true)
            (do
              (sleep-fn substrate-retry-delay-ms)
              (let [third-attempt (attempt 2)]
                (if-not (:error third-attempt)
                  (assoc (:value third-attempt) :readiness/substrate-transient true)
                  (let [cause (:error third-attempt)
                        ;; The classifier must never mask the authoritative
                        ;; failure: a throwing liveness fn degrades to
                        ;; :liveness-unknown with the diagnostic retained.
                        liveness (when-let [f (resolve-liveness-fn opts)]
                                   (try (f opts)
                                        (catch Throwable le
                                          {:classifier-error
                                           (str (.getName (class le)) ": "
                                                (.getMessage le))})))
                        state (cond
                                (nil? liveness) :liveness-unknown
                                (:classifier-error liveness) :liveness-unknown
                                (:alive? liveness) :alive-but-slow
                                :else :unreachable)]
                    (throw
                     (ex-info "Authoritative substrate preflight failed after retries"
                              (merge (ex-data cause)
                                     {:outcome :substrate-unavailable
                                      :failure-kind :substrate-unavailable
                                      :failure-stage :substrate-preflight
                                      :failure-detail :transient-exhausted
                                      :substrate-liveness liveness
                                      :substrate-state state})
                              cause))))))))))))

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

(defn- strategic-selection-http-invoke!
  [{:keys [agency-base strategic-selection-timeout-ms]} request]
  (let [url (str agency-base "/api/alpha/war-machine/strategic-selection")
        response
        (http/post url
                   {:headers {"Content-Type" "application/json"}
                    :body (json/generate-string request)
                    :timeout strategic-selection-timeout-ms
                    :throw false})
        body
        (try
          (json/parse-string (str (:body response)) true)
          (catch Throwable _ {}))]
    (when-not (<= 200 (long (or (:status response) 0)) 299)
      (throw
       (ex-info "Reason-bearing strategic selection endpoint failed"
                {:status (:status response)
                 :response body
                 :endpoint url})))
    body))

(defn- selection-response->selection
  [response]
  (when-not (and (true? (:ok response))
                 (map? (:selection response)))
    (throw
     (ex-info "Reason-bearing strategic selection response was invalid"
              {:selection-response-invalid true :response response})))
  (let [selection (:selection response)]
    (cond-> (update selection :status keyword)
      (get-in selection [:actuation :status])
      (update-in [:actuation :status] keyword)
      (get-in selection [:actuation :authority])
      (update-in [:actuation :authority] keyword)
      (get-in selection [:calibration :status])
      (update-in [:calibration :status] keyword))))

(defn- strategic-selection-attempt
  [invoke-fn request]
  (try
    {:value (selection-response->selection (invoke-fn request))}
    (catch Throwable e {:error e})))

(defn- selection-failure-class
  "Classify a strategic-selection failure for retry eligibility. HTTP 5xx and
  429 — the serving JVM failing or refusing under load, e.g. attempt-052's
  projection-cache recheck 503 — and transport-level throws with no HTTP
  status (connect failures, budget timeouts) are :transient. Any other HTTP
  status and an invalid response shape are :deterministic: the endpoint will
  reject the identical request identically, so replaying it burns the whole
  escalating ladder (up to ~10.5 minutes) reproducing one known failure."
  [error]
  (let [{:keys [status] :as data} (ex-data error)]
    (cond
      (:selection-response-invalid data) :deterministic
      (nil? status) :transient
      (<= 500 (long status) 599) :transient
      (= 429 (long status)) :transient
      :else :deterministic)))

(defn- selection-attempt-summary
  "Small typed record of one failed ladder attempt, kept in the final
  ex-data so a repair obligation carries the evidence of EVERY attempt
  rather than only the last throw."
  [i timeout-ms error]
  (let [{:keys [status response]} (ex-data error)]
    (cond-> {:attempt (inc i)
             :timeout-ms timeout-ms
             :error (ex-message error)}
      status (assoc :status status)
      (:err response) (assoc :err (:err response))
      (:message response) (assoc :message (:message response)))))

(defn- bounded-selection-invoke!
  [invoke-fn request timeout-ms]
  (let [timeout-sentinel (Object.)
        task (future (invoke-fn request))
        result (deref task timeout-ms timeout-sentinel)]
    (if (identical? timeout-sentinel result)
      (do
        (future-cancel task)
        (throw
         (ex-info "Injected strategic selection timed out"
                  {:timeout-ms timeout-ms})))
      result)))

(defn strategic-selection!
  "Request the cache-gated reason-bearing selector, with two delayed retries
   for TRANSIENT failures only (see selection-failure-class): a deterministic
   rejection fails fast with :failure-detail :deterministic-rejection instead
   of replaying the identical request across the ladder. Either terminal
   throw carries :attempt-failures, one typed summary per failed attempt.

   The default bridge uses HTTP so Futon2 keeps no source dependency on
   Futon3c. :strategic-selection-invoke-fn replaces only that bridge: it is
   called with the identical request map and returns the parsed endpoint body
   {:ok true :selection {...}}. Both paths share the retry ladder. A success
   after the first attempt carries :readiness/selection-transient."
  [opts request]
  (let [sleep-fn (or (:strategic-selection-sleep-fn opts)
                     #(Thread/sleep %))
        timeout-for (fn [i]
                      (or (:strategic-selection-timeout-ms opts)
                          (nth strategic-selection-retry-timeouts-ms i
                               (peek strategic-selection-retry-timeouts-ms))))
        invoke-for (fn [i]
                     (let [attempt-opts
                           (assoc opts :strategic-selection-timeout-ms
                                  (timeout-for i))]
                       (if-let [invoke-fn
                                (:strategic-selection-invoke-fn opts)]
                         #(bounded-selection-invoke!
                           invoke-fn % (:strategic-selection-timeout-ms
                                        attempt-opts))
                         #(strategic-selection-http-invoke!
                           attempt-opts %))))
        attempt (fn [i]
                  (strategic-selection-attempt (invoke-for i) request))]
    (loop [i 0
           attempt-failures []]
      (let [result (attempt i)]
        (if-not (:error result)
          (cond-> (:value result)
            (pos? i)
            (assoc :readiness/selection-transient true))
          (let [cause (:error result)
                failures (conj attempt-failures
                               (selection-attempt-summary i (timeout-for i)
                                                          cause))
                typed-throw
                (fn [message failure-detail]
                  (throw
                   (ex-info message
                            (merge (ex-data cause)
                                   {:outcome :incomplete
                                    :failure-kind
                                    :strategic-selection-unavailable
                                    :failure-stage :selection
                                    :failure-detail failure-detail
                                    :attempts (inc i)
                                    :attempt-failures failures})
                            cause)))]
            (cond
              (= :deterministic (selection-failure-class cause))
              (typed-throw
               "Reason-bearing strategic selection rejected deterministically"
               :deterministic-rejection)

              (< i (dec (count strategic-selection-retry-timeouts-ms)))
              (do
                (sleep-fn strategic-selection-retry-delay-ms)
                (recur (inc i) failures))

              :else
              (typed-throw
               "Reason-bearing strategic selection failed after retries"
               :transient-exhausted))))))))

(defn dispatch!
  [{:keys [agency-base d-task-dispatch-state]} agent caller mission prompt]
  (let [captured (some-> d-task-dispatch-state deref)
        prompt (if (= :captured (:status captured))
                 (str (d-task/prompt-binding (:dispatch captured)) "\n" prompt) prompt)
        response
        (post-json! (str agency-base "/api/alpha/bell")
                    {:agent-id agent :caller caller :mission-id (str mission)
                     :type "request" :mode "work" :prompt prompt})]
    (when-let [job-id (:job-id response)]
      (println "[wm-cancel] Ctrl-C alone does NOT cancel the Agency job.")
      (println "[wm-cancel] To stop this runner and its Agency job:")
      (println (str "clojure -M:wm-full-loop cancel " job-id
                    " operator-request"))
      (flush))
    (vary-meta response assoc ::job-texts/dispatched-prompt prompt)))

(defn cancel-job!
  "Cancel one Agency job through its single-finalizer endpoint. The Agency
   records cancelled/operator-cancelled before interrupting the process tree."
  [{:keys [agency-base]} job-id caller reason]
  (post-json! (str agency-base "/api/alpha/invoke/jobs/" job-id "/cancel")
              (cond-> {:caller caller}
                (not (str/blank? reason)) (assoc :reason reason))))

(def terminal-states #{"done" "failed" "cancelled" "timed-out"})

(defn throw-if-cancelled!
  "Lift Agency's cancellation terminal state into the full-loop vocabulary.
   Other terminal failures remain for their existing failure boundaries."
  [job failure-stage]
  (when (= "cancelled" (:state job))
    (throw (ex-info "Agency job was cancelled"
                    {:outcome :cancelled
                     :failure-kind :operator-cancelled
                     :failure-stage failure-stage
                     :job-id (:job-id job)
                     :terminal-code (:terminal-code job)
                     :terminal-message (:terminal-message job)})))
  job)

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
  "Wait for the Agency terminal state. Silence is observable evidence, never
  permission to abandon or replace a live author/reviewer job."
  [{:keys [poll-ms] :as opts} job-id]
  (let [clock (or (:now-ms-fn opts) #(System/currentTimeMillis))
        pause (or (:poll-sleep-fn opts) #(Thread/sleep %))
        first-poll-ms (clock)
        threshold (or (:agent-silence-ms opts) default-agent-budget-ms)]
    (loop [reported-activity nil]
      (let [job (read-job! opts job-id)
            now (clock)
            activity (or (job-last-activity-ms job) first-poll-ms)
            silent-for (max 0 (- now activity))]
        (report-wm-wait! opts job first-poll-ms)
        (if (contains? terminal-states (:state job))
          job
          (let [stalled? (and (>= silent-for threshold)
                              (not= activity reported-activity))]
            (when stalled?
              (let [record {:kind :stalled-job :job-id job-id
                            :job-state (:state job) :silent-for-ms silent-for
                            :observed-at (str (Instant/ofEpochMilli now))
                            :activity-basis (if (job-last-activity-ms job)
                                              :agency-timestamp :first-poll)
                            :waiting? true}
                    phase (some-> (:wm-phase-state opts) deref)]
                (when-let [state (:job-liveness/state opts)]
                  (swap! state conj record))
                ;; The ruling makes this observation non-halting even when the
                ;; legacy opt-in tripwire halt switch is set.
                (binding [tripwire/*halt-on-witness?* false]
                  (emit-phase! opts (:context phase)
                               {:phase (or (:phase phase) :agent-wait)
                                :transition :liveness :job-liveness record}))))
            (pause (or poll-ms 2000))
            (recur (if stalled? activity reported-activity))))))))

(defn author-infrastructure-failure?
  "True only for an artifact-free Agency invocation failure. These failures
  happen below the author contract, so the runner may retry them once without
  accepting the failed job as authored work.

  The codes are the invocation LAYER's failure vocabulary, not the author's:
  invoke-error is what futon3c's invoke path emits today (transport/http.clj
  finalize-invoke-job!); invoke-submit-failed is its dispatch-side sibling;
  invoke-exception is the 2026-07-21 attempt-043 spelling this predicate was
  originally written against. That spelling silently rotted: the layer renamed
  its code, the predicate kept matching a string nobody emits, and every
  transient invocation failure became terminal (wm click 2026-09-19-1789780157,
  codex-23 'Exit 1: [No assistant message returned]' 1.3s after dispatch with
  zero execution events, retried by nothing). Codes deliberately NOT here:
  no-execution-evidence is a groundedness gate (retrying it would retry the
  thing the gate refused); generic error/cancelled are not known to be below
  the author contract."
  [job]
  (let [event-code (some->> (:events job)
                            (filter #(= "failed" (:type %)))
                            last
                            :code)
        failure-code (or (:terminal-code job) event-code)]
    (and (= "failed" (:state job))
         (nil? (:artifact-ref job))
         (contains? #{"invoke-error" "invoke-submit-failed" "invoke-exception"}
                    failure-code))))

(defn- selected-entry
  "The tick's selected entry from a cascade-only decision (SPEC
   flat-removal H4, 2026-09-17). A cascade decision yields the chosen
   candidate with its own scores (there is no ranked-actions join); an
   abstention yields nil — nothing was selected."
  [judgement]
  (let [decision (:decision judgement)]
    (cond
      (and (map? decision) (= :abstained (:status decision))) nil
      (= :cascade-selection-posterior (get-in decision [:selection-law :applied]))
      (let [action (:action decision)]
        {:action action
         :rank (or (:rank decision) 1)
         :G-efe (:controller-score decision)
         :controller-score (:controller-score decision)})
      :else nil)))

;; resolve-pinned-selection and pinned-refusal! (RUN4) RETIRED with the flat
;; decision (SPEC flat-removal H4, 2026-09-17): they validated a pinned flat
;; mission action against :ranked-actions/:admissible-actions, neither of
;; which can be produced. A cascade-grain pinned-selection seam would be new
;; work under its own commission.


(defn- selected-target [entry]
  (if (= :cascade-candidate (get-in entry [:action :kind]))
    (get-in entry [:action :target])
    (or (get-in entry [:action :cascade-id])
        (get-in entry [:action :id])
        (get-in entry [:action :target])
        (get-in entry [:action :target-class])
        (get-in entry [:action :type]))))

(defn- selected-cascade [entry]
  (when (= :cascade-candidate (get-in entry [:action :kind]))
    (or (get-in entry [:action :cascade-id])
        (get-in entry [:action :id]))))

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

(defn ranked-candidates
  "All recorded policy candidates, descending by posterior mass. Certificate
  quantities join on the full action, since cascade IDs repeat across targets.
  Equal masses use the declared action-name rule on the first acting pattern
  (as in policy/select-action-cascades); equal first actions then use the full
  printed candidate for deterministic display order. This is a policy ranking,
  not the action marginal that selects the enacted action.

  :G-efe remains an alias of :G for existing CLI readers. Missing certificate
  quantities remain nil, with :f-status :not-recorded; never infer them from
  posterior mass."
  [judgement]
  (let [decision (:decision judgement)
        posterior (get-in decision [:selection-law :posterior])
        rule (get-in decision [:selection-law :tie-break-rule])
        certificates (into {} (map (juxt :id identity)
                                   (get-in decision [:selection-certificate :candidates])))
        first-action (fn [action]
                       (if (and (map? action) (seq (:precedence action)))
                         (first (:precedence action))
                         (if (map? action) (:type action) action)))
        tied? (some #(> % 1) (vals (frequencies (map second posterior))))]
    (when (and tied? (not= :action-name-ascending rule))
      (throw (ex-info "Cannot rank a posterior tie without its declared rule"
                      {:failure-kind :unsupported-ranking-tie-rule :tie-break-rule rule})))
    (->> posterior
         (sort-by (fn [[action probability]]
                    [(- probability) (str (first-action action)) (pr-str action)]))
         (map-indexed
          (fn [i [action probability]]
            (let [certificate (get certificates action)]
              {:rank (inc i) :action action
               :target (:target action)
               :cascade-id (or (:cascade-id action) (:id action))
               :G (:g certificate) :G-efe (:g certificate)
               :habit (:habit certificate) :F (:f certificate)
               :f-status (get certificate :f-status :not-recorded)
               :posterior probability})))
         vec)))

(defn selection-discrimination
  "Diagnostic on the leading posterior masses. Retains the existing epsilon
  and top-k rule: all masses must be finite and two or more candidates need
  at least two epsilon-distinct masses. G is not the quantity this diagnostic
  compares. Near-tie attribution is separate work (narrative fix-7)."
  ([ranked] (selection-discrimination ranked {}))
  ([ranked {:keys [top-k epsilon]
            :or {top-k discrimination-top-k
                 epsilon discrimination-epsilon}}]
   (let [leading (vec (take top-k ranked))
         values (mapv :posterior leading)
         valid (filterv #(and (number? %) (Double/isFinite (double %))) values)
         distinct-values (epsilon-distinct-count valid epsilon)]
     {:candidate-count (count leading)
      :valid-posterior-count (count valid)
      :distinct-posterior distinct-values
      :epsilon epsilon
      :top-k top-k
      :posterior-values values
      :passes? (and (= (count valid) (count leading))
                    (or (< (count leading) 2)
                        (>= distinct-values 2)))})))

;; Retained for explicit repair callers; ordinary clicks no longer divert here.
#_{:clj-kondo/ignore [:unused-private-var]}
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
  mission merely by renaming fields. A cascade candidate dispatches on its
  :kind (SPEC flat-removal H4, 2026-09-17): its construction is its own
  recorded precedence and receipts, not a lane reconstruction."
  (fn [entry]
    (let [action (:action entry)]
      (if (= :cascade-candidate (:kind action))
        :cascade-candidate
        (:type action)))))

(defmethod construct-selected-action :cascade-candidate
  [entry]
  (let [action (:action entry)]
    {:mission (:target action)
     :psi (str "enact cascade " (or (:cascade-id action) (:id action)))
     :construction-kind :selected-cascade
     :selected-action action
     :precedence (vec (:precedence action))
     :construction-receipt (:construction-receipt action)
     :interpretation-receipts (:interpretation-receipts action)
     :shown (mapv (fn [p] (if (map? p)
                            (str (or (:id p) (:cascade-id p)))
                            (str p)))
                  (:precedence action))
     :cascade-structure (cascade-structure/receipt action)
     :policy-holes []}))

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

(declare discharge-contract)

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
      ;; Legacy findings (schema 1 review failures, attempt-054) carried no
      ;; discharge contract; the class-typed contract is the fail-closed
      ;; floor so :discharge is never nil in a repair construction.
      :discharge (or (:discharge-contract obligation)
                     (discharge-contract (:repair/class obligation)))}
     :shown ["futon-theory/stop-the-line" "musn/pause-backtrace"]
     :semilattice [{:from :failure-backtrace :to :repair-implementation}
                   {:from :repair-implementation :to :independent-review}
                   {:from :independent-review :to :successor-validation}]
     :policy-holes []}))

(defn historical-revalidation-entry
  "Admit a verified historical repair as a distinct selectable action. This
  does not execute it or relax ordinary author invariants."
  [obligation admission casting]
  (when (and (= :open (:repair/status obligation))
             (= :machine-failure (:repair/class obligation))
             (= :wm/historical-repair-admission-v1 (:schema admission))
             (= (:repair/id obligation) (:repair/id admission))
             (= :awaiting-validation (:repair/status admission))
             (= (get-in admission [:actors :author]) (:author casting))
             (= (get-in admission [:actors :reviewer]) (:repair-reviewer casting))
             (not= (:author casting) (:repair-reviewer casting)))
    {:rank 0 :action {:type :revalidate-historical-repair
                      :target (:repair/id obligation)
                      :repair-obligation obligation
                      :admission admission}}))

(defmethod construct-selected-action :revalidate-historical-repair
  [entry]
  (let [action (:action entry) admission (:admission action)]
    {:mission (:target action)
     :construction-kind :historical-repair-revalidation
     :selected-action (dissoc action :admission)
     :verification-id (:verification-id admission)
     :verification-artifact (:verification-artifact admission)
     :effect :awaiting-validation
     :production-successor-required? true}))

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

(defn- ground-cascade-policy-holes
  "Translate unfolded patterns to their declared, target-qualified outputs.
  An obligation ID is the EDN spelling of the existing [target fact] token,
  not a claim that a new repair-store obligation exists. Keep the token and
  interpretation receipt alongside it. Missing/ambiguous context stays invalid."
  [construction result]
  (let [action (:selected-action construction)
        target (:target action)
        precedence (:precedence construction)
        receipts (:interpretation-receipts construction)]
    (if (and (= :selected-cascade (:construction-kind construction))
             (map? result) (vector? (:policy-holes result)))
      (update result :policy-holes
              (fn [holes]
                (vec
                 (mapcat
                  (fn [{:keys [unfolded-pattern] :as hole}]
                    (let [matches (filter #(= (str (:id %)) unfolded-pattern)
                                          precedence)
                          pattern (when (= 1 (count matches)) (first matches))
                          receipt (get receipts (:id pattern))
                          outputs (:produces pattern)
                          grounded? (and (string? target) (seq target)
                                         (= target (:mission construction) (:target pattern))
                                         (map? (:construction-receipt construction))
                                         (map? (:source receipt))
                                         (string? (:reading receipt)) (seq (:reading receipt))
                                         (set? outputs) (seq outputs)
                                         (every? #(and (vector? %) (= 2 (count %))
                                                       (= target (first %))
                                                       (keyword? (second %)))
                                                 outputs))]
                      (if (and grounded? (not (contains? hole :obligation/id)))
                        (mapv (fn [token]
                                (assoc hole
                                       :obligation/id (pr-str token)
                                       :obligation/token token
                                       :obligation/source
                                       {:pattern/id (:id pattern)
                                        :interpretation-receipt receipt
                                        :construction-receipt (:construction-receipt construction)}
                                       :free (str "Unconstructed declared output " (pr-str token)
                                                  " from unfolded pattern " unfolded-pattern)
                                       :why (:reading receipt)))
                              (sort-by pr-str outputs))
                        [hole])))
                  holes))))
      result)))

(defn construction-wiring-result
  "Produce and classify fold wiring for a construction.  The optional port is
  for server-owned fold implementations and induced commissioning tests; its
  result is subject to the same gate."
  ([construction] (construction-wiring-result construction nil false))
  ([construction wiring-fn]
   (construction-wiring-result construction wiring-fn false))
  ([construction wiring-fn required?]
   (let [port-missing? (and required? (not (fn? wiring-fn)))
         repair-id (get-in construction [:repair-contract :repair-id])
         result0 (when-not port-missing?
                   (if wiring-fn
                     (wiring-fn construction)
                     (if (and (= :selected-cascade (:construction-kind construction))
                              (seq (:interpretation-receipts construction)))
                       (fold-cascade/realize construction)
                       (let [result (or (:fold (close-loop/act-gate-from-lane-entry construction construction))
                                        (fold-classical/classical-fold (vec (:shown construction)) construction))]
                         (assoc result
                                :fold/route (if (= "semilattice-fold v1 (descent=BV.seq, co_app=BV.copar)"
                                                  (get-in result [:wiring :generated-by]))
                                              :semilattice :classical)
                                :fold/selection-reason :legacy-construction)))))
         result0 (if (nil? wiring-fn)
                   (let [grounded (ground-cascade-policy-holes construction result0)]
                     (if (= :cascade-interpretation (:fold/route grounded))
                       (fold-cascade/evaluate grounded)
                       grounded))
                   result0)
         ;; The classical fold can only name the patterns it could not fold.  A
         ;; stop-line construction additionally owns the real obligation that
         ;; makes each remainder actionable.  Enrich only the production fold;
         ;; injected folds remain unmodified so their contract violations are
         ;; observable at the gate.
         result (if (and (nil? wiring-fn) (map? result0) (seq repair-id)
                         (vector? (:policy-holes result0)))
                  (update result0 :policy-holes
                          (fn [holes]
                            (mapv (fn [{:keys [unfolded-pattern] :as hole}]
                                    (assoc hole
                                           :free (str "Unfolded repair pattern " unfolded-pattern)
                                           :why (str "Required by stop-line obligation " repair-id)
                                           :obligation/id repair-id))
                                  holes)))
                  result0)
         patterns (vec (:shown construction))
         validation (fold/validate-fold-output-v1 result)
         correspondence (fold/validate-fold-correspondence result patterns)
         refusal? (= :refusal (:fold/schema validation))
         output-digest (sha256 result)
         evidence {:fold-output result
                   :shape-validation
                   {:validator "futon2.aif.fold/validate-fold-output-v1"
                    :version (:validator/version validation)
                    :ok (:ok validation) :findings (vec (:findings validation))
                    :input-sha256 output-digest}
                   :correspondence-validation
                   {:validator "futon2.aif.fold/validate-fold-correspondence"
                    :version (:validator/version correspondence)
                    :ok (:ok correspondence) :findings (vec (:findings correspondence))
                    :cascade-sha256 (sha256 patterns)
                    :fold-output-sha256 output-digest}}]
     (merge evidence
            (cond
              port-missing?
              {:status :invalid :failure-kind :construction-wiring-port-missing
               :findings [{:finding :construction-wiring-port-missing
                           :message "Authenticated production construction requires the server-owned fold port"}]}
              (and (:ok validation) refusal?) {:status :refused}
              (and (:ok validation) (:ok correspondence))
              {:status :wired :wiring (:wiring result)}
              :else
              {:status :invalid
               :failure-kind (if (:ok validation) :fold-correspondence-invalid :fold-output-invalid)
               :findings (vec (concat (:findings validation) (:findings correspondence)))})))))

(defn selection-enaction-record
  "Persist the comparison between the selected decision and the action that
  actually entered construction. A non-match is explicit rather than absent."
  [selected enacted evidence]
  {:verdict (if (= selected enacted) :match :typed-divergence)
   :selected selected
   :enacted enacted
   :evidence evidence})

(defn- mission-for-decision [entry target & [repair-root]]
  (let [action (:action entry)]
    (cond
      (and (:repair/id action) (= target (str "T-" (:repair/id action))))
      (:finding (repair-discharge/bind-selected!
                 (or repair-root repair/default-root) action (:interpretation-receipts action)))

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

      (= :advance-ticket (:type action)) (missions/ticket-entry target)
      :else (mission-entry target))))

(defn- job-text [job] (task-execution/job-text job))
(defn- review-verdict [job] (task-execution/review-verdict job))

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

(defn- throw-if-author-refused! [author-job target stage]
  ;; Shared HEAD movement cannot establish an artifact for a refusing author.
  ;; A contradictory artifact claim is a failure, never an environmental hold.
  (let [{:keys [verdict reason]} (author-verdict author-job)]
    (when (= :refuse verdict)
      (if (and (not (str/blank? reason))
               (nil? (:artifact-ref author-job)))
        (throw (ex-info "Author refused with a typed reason"
                        {:outcome :guardrail-refusal
                         :failure-kind :guardrail-refusal
                         :failure-stage stage
                         :refusal-reason reason
                         :target target :author-job author-job}))
        (throw (ex-info "Author refusal lacks a reason or claims an artifact"
                        {:outcome :build-failed
                         :failure-kind :invalid-author-refusal
                         :failure-stage stage
                         :target target :author-job author-job}))))))

(def ^:private feature-card-keys
  [:built :want-coverage :matches-intent? :things-to-try
   :fold-ref :proof-ref :reviewer-note])

(def ^:private feature-card-marker "FULL_LOOP_FEATURE_CARD:")
(def ^:private feature-card-durable-limit 200)

(defn- read-first-edn-form [payload]
  (with-open [reader (java.io.PushbackReader.
                      (java.io.StringReader. payload))]
    (edn/read {:eof nil} reader)))

(defn- summary-feature-card
  "Fast path: the card led the reply and closed within the legacy 200-char
  summary window."
  [job]
  (let [text (:result-summary job)]
    (when (and (string? text)
               (str/starts-with? text feature-card-marker))
      (let [end (min (count text) feature-card-durable-limit)
            payload (subs text (count feature-card-marker) end)]
        (try
          (when-let [card (read-first-edn-form payload)]
            {:card card :source :text})
          (catch Exception _ nil))))))

(defn- result-feature-card
  "Line-anchored search over the job's complete durable reply. Agency stores
  the full reply in :result (capped at 8000 at finalize) and serves it on the
  jobs GET; only the author's own reply payload is searched — terminal
  messages and event prose are not — so a marker quoted there cannot
  masquerade as the card. Line anchoring keeps a marker quoted mid-sentence
  from matching."
  [job]
  (let [text (str (:result job))]
    (when-not (str/blank? text)
      (let [m (re-matcher #"(?m)^FULL_LOOP_FEATURE_CARD:" text)]
        (when (.find m)
          (try
            (when-let [card (read-first-edn-form (subs text (.end m)))]
              {:card card :source :result})
            (catch Exception _ nil)))))))

(defn- events-feature-card
  "Last extraction fallback: the author's own text EVENTS, each checked for the
  marker at its start.

  Agency concatenates the author's separate text blocks into :result with NO
  separator, so a card that DID begin its own reply block can end up abutting
  the previous block's last word — measured on canary-de75cee9, where :result
  reads \"...doing it precisely:FULL_LOOP_FEATURE_CARD: {...}\". The
  line-anchored search over :result then cannot match, and the run is reported
  :marker-not-at-durable-prefix, which says the author put prose before the
  marker. The author did put prose before it, but in an EARLIER block; the
  concatenation is what destroyed the line boundary, so the diagnosis blamed
  the wrong layer and an extractable card was thrown away.

  This does NOT widen the gate. The marker must still begin its block — a
  marker quoted mid-sentence inside an event never matches, exactly as line
  anchoring intends — and only the author's own :text events are read, never
  terminal messages or tool prose."
  [job]
  (some (fn [event]
          (when (= "text" (str (:type event)))
            (let [text (str/triml (str (:text event)))]
              (when (str/starts-with? text feature-card-marker)
                (try
                  (when-let [card (read-first-edn-form
                                   (subs text (count feature-card-marker)))]
                    {:card card :source :events})
                  (catch Exception _ nil))))))
        (:events job)))

(defn- text-feature-card [job]
  ;; :result-summary is a 220-char whitespace-collapsed digest and truncates
  ;; any card whose closing brace falls past the window — attempt-051's valid
  ;; card failed exactly there (2026-07-25), and its cure round failed on
  ;; prose preceding the marker. Prefer the summary fast path, then fall back
  ;; to the complete durable :result; report the legacy typed reasons only
  ;; when no parseable card exists on either path.
  (or (summary-feature-card job)
      (result-feature-card job)
      (events-feature-card job)
      (let [summary (:result-summary job)]
        (cond
          (and (str/blank? summary) (str/blank? (str (:result job))))
          {:reason :missing-marker :source :text}

          (not (str/starts-with? (str summary) feature-card-marker))
          {:reason :marker-not-at-durable-prefix :source :text}

          :else
          {:reason :truncated-or-over-durable-limit :source :text}))))

(defn- observation-shaped-step? [step]
  (when (string? step)
    (when-let [arrow (str/index-of step "->")]
      (and (not (str/blank? (subs step 0 arrow)))
           (not (str/blank? (subs step (+ arrow 2))))))))

(defn- feature-card-validation [job]
  (let [{:keys [card source] :as candidate}
        (if (contains? job :feature-card)
          {:card (:feature-card job) :source :structured}
          (text-feature-card job))
        invalid (fn [reason] {:reason reason :source source})]
    (cond
      (:reason candidate) candidate
      (not (map? card)) (invalid :card-must-be-a-map)
      (not (and (string? (:built card))
                (not (str/blank? (:built card)))))
      (invalid :built-must-be-a-nonblank-string)
      (not (and (string? (:want-coverage card))
                (not (str/blank? (:want-coverage card)))))
      (invalid :want-coverage-must-be-a-nonblank-string)
      (not (instance? Boolean (:matches-intent? card)))
      (invalid :matches-intent-must-be-boolean)
      (not (and (sequential? (:things-to-try card))
                (seq (:things-to-try card))))
      (invalid :things-to-try-must-be-nonempty)
      (not-every? observation-shaped-step? (:things-to-try card))
      (invalid :things-to-try-must-be-observation-shaped)
      :else
      {:card (-> (select-keys card feature-card-keys)
                 (update :things-to-try vec))
       :source source})))

(defn- valid-feature-card [job]
  (:card (feature-card-validation job)))

(defn- review-execution-gate [files job]
  (task-execution/review-execution-gate files job))

(defn independent-review-evidence [files job]
  (task-execution/independent-review-evidence files job))

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

(declare deferred-completion-job-id resolve-build resolve-target-build)

(defn- prompt-findings [stop-lines]
  (mapv (fn [finding]
          (assoc (select-keys finding
                              [:repair/id :repair/class :attempt-id
                               :failed-commit :review-verdict :review-text
                               :target :failure-kind :failure-stage :failure-outcome :failure-error
                               :discharge-contract])
                 :failure-job-id (deferred-completion-job-id finding)))
        stop-lines))

(defn- prompt-selected-action [action]
  (cond-> (select-keys action
                       [:type :target :mission-path :target-class :proposer-id
                        :pattern-title :pattern-summary :evidence-ids])
    (:repair-obligation action)
    (assoc :repair-obligation
           (first (prompt-findings [(:repair-obligation action)])))))

(defn- prompt-construction [construction]
  (cond-> (select-keys construction
                       [:construction-kind :capability-contract
                        :actuation-contract :repair-contract :shown :semilattice])
    (:selected-action construction)
    (assoc :selected-action
           (prompt-selected-action (:selected-action construction)))))

(defn- evidence-deposit-instruction
  ([role evidence-dir author reviewer]
   (evidence-deposit-instruction role evidence-dir author reviewer false))
  ([role evidence-dir author reviewer measured-acquisition?]
   (evidence-deposit-instruction role evidence-dir author reviewer measured-acquisition? nil))
  ([role evidence-dir author reviewer measured-acquisition? target]
  (when evidence-dir
    (str "\nDEPOSIT INSTRUCTION ("
         (if measured-acquisition?
           "REQUIRED FOR THIS MEASURED ATTEMPT"
           "optional for ordinary attempts; required for a close intended for measured-A observation")
         "):\n"
         "DIRECTORY: " evidence-dir "\n"
         "Use flat one-form EDN files only; create no subdirectories. Files are the records; do not echo them in your reply.\n"
         (case role
           :author
           (str "AUTHOR " author " may deposit one actual-command receipt per discharged limb:\n"
                ":wm/limb-receipt-v1 keys [:schema :repair/id :limb :command :exit :stdout-sha256 :stderr-sha256 :recorded-at].\n"
                "Write stdout/stderr bytes to flat companion files and reference them with :stdout-file/:stderr-file.\n"
                "For a repair target, capture its immutable ORIGINAL obligation record bytes under data/wm-repair-obligations/ as :before and the DERIVED-STATE readback record as :after (repair-derived-state output bytes; still :open is honest, but capture the readback record, never a second copy of the finding -- identical before/after bytes refuse :revision-unchanged, which orphaned cohort-54 attempt-001).\n"
                "Name that :wm/entity-revision-pair-v1 file subject-*.edn and set :entity/id to the exact selected repair id.\n"
                "Source-file revision pairs are supporting evidence: name them supporting-*.edn and use the source artifact id. Both roles use keys [:schema :entity/id :before :after :dimensions].\n"
                (when measured-acquisition?
                  (str "MEASURED-ONLY ADDITION: for EVERY repair id under data/wm-repair-obligations/resolutions/ whose resolution embeds a :successor-relation, also deposit a supporting-resolved-*.edn revision pair with :entity/id set to THAT repair id (:before = its findings/ bytes, :after = its resolutions/ bytes; supporting-* naming because it is not the selected target). These retained-store captures are observation evidence for the later boundary; they cannot be reconstructed after the cutoff.\n")))
           :reviewer
           ;; A key list alone was not enough: on 2026-09-21 (run
           ;; 2026-09-21-1790033693) a fresh reviewer seat wrote :decision
           ;; :approve, nested :explanation under a map-valued :evidence and
           ;; used its own :entity/id, and the close refused a grounded change
           ;; as :explanation-invalid. Give the exact shape.
           (str "REVIEWER " reviewer " (not author " author ") deposits the same-target standing decision, exactly this shape (one flat map, these eight keys, no others):\n"
                "{:schema :wm/target-standing-decision-v1\n"
                " :entity/id " (if target (pr-str target) "\"<the selected target, exactly>\"") "\n"
                " :decision :resolved   ; or :still-live -- :resolved = this target's stated work is now done; :still-live = it remains open. Not your review verdict.\n"
                " :decided-by " (pr-str reviewer) "\n"
                " :implementation-author " (pr-str author) "\n"
                " :decided-at \"<ISO-8601 instant, e.g. 2026-09-21T23:43:25Z>\"\n"
                " :evidence [\"<commit sha>\" \"<other evidence id>\"]   ; a non-empty VECTOR of strings\n"
                " :explanation \"<review-grade prose, at least 80 characters>\"}   ; top level, not inside :evidence\n"
                ":entity/id must be the target exactly; :decided-by must differ from :implementation-author (a self-decided record refuses).\n"))
         "Any invalid deposit refuses the whole close: deposit carefully or not at all.\n"
         "EVERY non-EDN file must be referenced by exactly one record's :file/:stdout-file/:stderr-file field; an unreferenced byproduct (cohort-55 attempt-003: a stray derived.stderr) is parsed as EDN, fails, and refuses the whole close.\n"))))

(defn- acceptance-criterion-block
  "PROOF-wm-works ⟨1⟩6 (claude-5 ruling): the dispatch states the acceptance
   criterion VERBATIM. For the SELECTED candidate: each declared produced
   token with its locator (kind, exact path; for C4 the exact required
   declaration head and the line-initial rule), and the target's own
   acceptance locator (cascade-sources/acceptance-of — never restated by
   hand). A candidate whose locators cannot be rendered still dispatches:
   the block is typed-absent with a reason. Nothing here is a gate."
  [{:keys [action]}]
  (try
    (let [target (:target action)
          first-action (first (:precedence action))
          ;; the live qualifier produces [target token] pairs; hand-built
          ;; entries may carry bare tokens — handle both
          produced (vec (sort-by pr-str (map (fn [tok] (if (vector? tok) (second tok) tok))
                                             (:produces first-action))))
          sources (cascade-sources/load-declared)
          render-locator (fn [token locator]
                           (str "  " (pr-str token) " — "
                                (case (:class locator)
                                  :C3 (str "C3 path-exists: the file " (:path locator)
                                           " must exist in the repository")
                                  :C4 (str "C4 declaration-head: the file " (:path locator)
                                           " must contain the exact head \"" (:decl locator)
                                           "\" at the start of a line")
                                  (str "unsupported locator class " (:class locator)))))
          produced-lines (when (seq produced)
                           (for [token produced
                                 :let [qualified (if (vector? token) token [target token])
                                       bare (if (vector? token) (second token) token)
                                       locator (get-in sources [:locators target bare])]]
                             (if (map? locator)
                               (render-locator qualified locator)
                               (str "  " (pr-str qualified)
                                    " — LOCATOR NOT DECLARED: the token has no locator in the source"))))
          acceptance (cascade-sources/acceptance-of target)
          acceptance-line (when acceptance
                            (render-locator [target (:token acceptance)] (:locator acceptance)))]
      (if (or (seq produced-lines) acceptance-line)
        (str "ACCEPTANCE CRITERIA (the tests your work will be measured against):\n"
             (when (seq produced-lines)
               (str "Your action's declared produced tokens, measured at the after-revision:\n"
                    (clojure.string/join "\n" produced-lines) "\n"))
             (when acceptance-line
               (str "The target's own acceptance declaration:\n" acceptance-line "\n")))
        {:status :absent :reason :no-renderable-criteria}))
    (catch Exception e
      {:status :absent :reason :criterion-rendering-failed :message (.getMessage e)})))

(defn- author-prompt [{:keys [author reviewer batch-id target-repository
                             target-repository-head attempt-evidence-dir
                             measured-acquisition? surprise-root
                             surprise-lookup-fn]}
                      target mission cascade-entry stop-lines]
  (let [surprise-result (when surprise-root
                          (try
                            ((or surprise-lookup-fn surprise/records-for-action)
                             surprise-root (:selected-action cascade-entry))
                            (catch Throwable e
                              {:status :unavailable
                               :kind :surprise-store-unreadable
                               :exception-class (.getName (class e))})))
        surprise-text
        (case (:status surprise-result)
          :ok (apply str
                     (for [{:keys [surprise/id token model-part]}
                           (:records surprise-result)]
                       (str "RECORDED SURPRISE: " id
                            " token " (pr-str token)
                            " kind " (pr-str model-part) ". "
                            "If your commit revises this model part, end its commit message "
                            "with this trailer line:\nSurprise: " id "\n")))
          :unavailable
          (str "SURPRISE LOOKUP NOTE: "
               (pr-str (select-keys surprise-result
                                    [:status :kind :path :exception-class])) "\n")
          nil)]
    (str author ": FULL-LOOP IMPLEMENTATION OPPORTUNITY. You are the author; "
       reviewer " is the independent reviewer.\n\n"
       "Implement one bounded, substantive advancement of the selected War Machine action. "
       "This is NOT a request for a fold-turn deposit, wiring diagram, report-only artifact, "
       "or prose claiming that work could be done. Change the actual mission/code world.\n\n"
       "SELECTED TARGET: " (pr-str target) "\n"
       "TARGET REPOSITORY: " (pr-str target-repository) "\n"
       "TARGET REPOSITORY BASE HEAD: " (pr-str target-repository-head) "\n"
       (let [block (acceptance-criterion-block {:action cascade-entry})]
         (if (map? block)
           (str "ACCEPTANCE CRITERIA: not stated (" (name (:reason block)) ")\n")
           block))
       "REPOSITORY ARTIFACT CONTRACT: Make and commit the complete parcel only in "
       "TARGET REPOSITORY. The artifact gate observes only that repository. Paths and "
       "actions nested in the mission record are context, not permission to commit in "
       "another repository; a commit elsewhere is an artifact-binding mismatch. If the "
       "target repository is unresolved or the parcel cannot be completed there, make no "
       "commit and REFUSE with a typed reason.\n"
       "MISSION RECORD: "
       (pr-str (if (:repair/id mission)
                 (first (prompt-findings [mission]))
                 mission)) "\n"
       (when-let [repair-id (:repair/id mission)]
         (str "FULL REPAIR FINDING: "
              (pr-str (str (io/file repair/default-root "findings"
                                   (str repair-id ".edn"))))
              "\nThe mission record above is the compact finding projection. "
              "Read the full finding for its backtrace and nested evidence; "
              "the discharge contract is unchanged.\n"))
       "PATTERN CASCADE:\n" (cascade-plan/cascade-plan-text cascade-entry)
       "Name in your reply which pattern(s) your change enacts.\n"
       "CONSTRUCTION CONTRACT: " (pr-str (select-keys cascade-entry
                                                  [:mission :psi :shown :semilattice
                                                   :cascade-score
                                                   :construction-kind
                                                   :capability-contract
                                                   :actuation-contract
                                                   :repair-contract])) "\n"
       surprise-text
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
       (when measured-acquisition?
         "THIS ATTEMPT IS A DECLARED MEASURED-ACQUISITION ATTEMPT.\n")
       (evidence-deposit-instruction :author attempt-evidence-dir author reviewer
                                     measured-acquisition?)
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
       "   The sha MUST be copied verbatim from `git rev-parse HEAD` output.\n"
       "   Never reconstruct it from a short id or from memory: a fabricated\n"
       "   tail fails artifact binding and closes the attempt (cohort-55\n"
       "   attempt-001 died exactly this way).\n"
       "If no safe substantive parcel is possible, make no commit and finish with "
       "FULL_LOOP_AUTHOR: REFUSE <typed reason>.")))

(defn- build-cure-prompt
  "Construct the cure re-emission prompt sent to the SAME author agent when a
  mechanically-detectable, author-curable build failure occurs. The prompt is
  self-contained: it names the target, the original commit, the exact error,
  and (for card failures) the literal durable prefix text so the author sees
  what Agency actually preserved."
  [author target original-commit error-message {:keys [failure-kind]} job-prefix-text]
  (str author ": FULL-LOOP BUILD CURE. Your previous author turn for selected "
       "target " (pr-str target) " produced a mechanically-detectable build "
       "failure that you can cure with a corrected re-emission.\n\n"
       "ORIGINAL COMMIT: " original-commit "\n"
       "EXACT VALIDATION ERROR: " error-message "\n"
       (when (= :feature-card-missing-or-invalid failure-kind)
         (str "DURABLE PREFIX TEXT (:result-summary) — this is what Agency's "
              "prefix actually preserved from your previous response:\n"
              (pr-str job-prefix-text) "\n\n"))
       "INSTRUCTION: BEGIN your response with one corrected, self-contained "
       "FULL_LOOP_FEATURE_CARD: {:built \"...\" :want-coverage \"...\" "
       ":matches-intent? true :things-to-try [\"command -> observation\"]} "
       "line. All four named fields are required; legacy :target/:change/:sha "
       "cards are invalid. Keep the closing brace within the 200-char durable "
       "prefix. The marker must be the very first "
       "characters of your reply — no prose before it. Then "
       "FULL_LOOP_AUTHOR: DONE <sha>. "
       "Make a new commit ONLY if files must change (artifact-only cure "
       "requires a substantive commit; card cure usually needs no new commit "
       "— re-emitting the card with the existing sha is valid)."))

(defn- reviewer-prompt [{:keys [reviewer author attempt-evidence-dir
                                measured-acquisition?]}
                        target construction repo commit
                        author-job stop-lines]
  (str reviewer ": FULL-LOOP INDEPENDENT REVIEW. " author " authored commit " commit
       " for selected target " (pr-str target) ".\n\n"
       "Repository: " repo "\n"
       "CONSTRUCTION CONTRACT: "
       (pr-str (prompt-construction construction)) "\n"
       "PATTERN CASCADE:\n" (cascade-plan/cascade-plan-text construction)
       "Author job evidence: " (pr-str (select-keys author-job
                                                     [:job-id :state :artifact-ref
                                                      :repo-observed-artifact-ref
                                                      :result-summary :execution])) "\n"
       (when (seq stop-lines)
         (str "Prior STOP-THE-LINE findings to discharge explicitly: "
              (pr-str (prompt-findings stop-lines)) "\n"))
       (evidence-deposit-instruction :reviewer attempt-evidence-dir author reviewer
                                     measured-acquisition? target)
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

(defn- review-findings-text
  "Return the complete reviewer reply only when a typed negative verdict
  carries nonblank findings. The complete reply is retained verbatim for the
  revision author; a bare negative marker does not authorize another turn."
  [review-job]
  (let [text (job-text review-job)
        [_ findings]
        (re-find
         #"(?ms)^FULL_LOOP_REVIEW:\s*(?:REQUEST_CHANGES|REJECT)\b[ \t]*(.*)$"
         text)]
    (when-not (str/blank? findings) text)))

(defn- review-record [round commit review-job review-gate]
  {:round round
   :commit commit
   :job-id (:job-id review-job)
   :verdict (review-verdict review-job)
   :text (job-text review-job)
   :gate review-gate})

(defn- revision-author-prompt
  ([author reviewer evidence-dir target construction prior-commits findings]
   (revision-author-prompt author reviewer evidence-dir false target construction
                           prior-commits findings))
  ([author reviewer evidence-dir measured-acquisition?
    target construction prior-commits findings]
   (str author ": FULL-LOOP REVISION ROUND 2. The independent reviewer requested "
       "changes to your implementation. Amend the same selected target using new "
       "commits in the existing repository.\n\n"
       "SELECTED TARGET: " (pr-str target) "\n"
       "CONSTRUCTION CONTRACT: " (pr-str (prompt-construction construction)) "\n"
       ;; ⟨1⟩6: the retry knows the test it must satisfy — the same block the
       ;; first dispatch stated, rendered from the candidate's own
       ;; declarations (carried on the construction record).
       (let [action (:selected-action construction)]
         (if (map? action)
           (let [block (acceptance-criterion-block {:action action})]
             (if (map? block)
               (str "ACCEPTANCE CRITERIA: not stated (" (name (:reason block)) ")\n")
               block))
           ""))
       "YOUR PRIOR COMMIT SHAS: " (pr-str prior-commits) "\n"
       "REVIEWER VERDICT AND FINDINGS (VERBATIM):\n"
       findings "\n\n"
       (when measured-acquisition?
         "THIS ATTEMPT IS A DECLARED MEASURED-ACQUISITION ATTEMPT.\n")
       (evidence-deposit-instruction :author evidence-dir author reviewer
                                     measured-acquisition?)
       "Address the findings without widening scope. Preserve existing history: "
       "make new commits only; do not force-push, reset, amend, rebase, rewrite, "
       "or otherwise replace prior commits. Run the repository-required gates.\n"
       "Finish with FULL_LOOP_AUTHOR: DONE <new-commit-sha> and list validations. "
        "If no safe correction is possible, make no commit and finish with "
        "FULL_LOOP_AUTHOR: REFUSE <typed reason>.")))

(defn- revision-reviewer-prompt
  [{:keys [reviewer author attempt-evidence-dir measured-acquisition?]}
   target construction repo prior-commit revision-commit
   revision-author-job initial-review-job stop-lines]
  (str reviewer ": FULL-LOOP AMENDMENT RE-REVIEW. You are the same independent "
       "reviewer. " author " authored the bounded amendment. Review only that "
       "amendment and whether it resolves your "
       "original findings without regression.\n\n"
       "SELECTED TARGET: " (pr-str target) "\n"
       "Repository: " repo "\n"
       "CONSTRUCTION CONTRACT: " (pr-str (prompt-construction construction)) "\n"
       "PRIOR REVIEWED COMMIT: " prior-commit "\n"
       "AMENDMENT COMMIT: " revision-commit "\n"
       "ORIGINAL REVIEW (VERBATIM):\n" (job-text initial-review-job) "\n"
       "Revision author evidence: "
       (pr-str (select-keys revision-author-job
                            [:job-id :state :artifact-ref
                             :repo-observed-artifact-ref
                             :result-summary :execution])) "\n"
       (when (seq stop-lines)
         (str "Prior STOP-THE-LINE findings remain in force: "
              (pr-str (prompt-findings stop-lines)) "\n"))
       (evidence-deposit-instruction :reviewer attempt-evidence-dir author reviewer
                                     measured-acquisition? target)
       "\nInspect the amendment commit and its delta from the prior reviewed commit. "
       "Do not edit or commit. Execute the repository-required static checks and "
       "relevant tests yourself; an APPROVE without executed tool evidence is invalid.\n\n"
       "BEGIN with exactly one verdict line:\n"
       "FULL_LOOP_REVIEW: APPROVE\n"
       "or FULL_LOOP_REVIEW: REQUEST_CHANGES <reason>\n"
       "or FULL_LOOP_REVIEW: REJECT <reason>\n"
       "You may add FULL_LOOP_REVIEWER_NOTE: <short note> on a second line."))

(def ^:private commit-ish-pattern
  "A git object name: 7-40 hex digits. Short refs are normal here — an Agency
  artifact-ref is often abbreviated (e.g. \"5818c67\")."
  #"(?i)\A[0-9a-f]{7,40}\z")

(defn commit-ish?
  "Does this even LOOK like a commit?

  find-commit-repo happily shells `git cat-file -e <x>^{commit}` for any string,
  so a value that is not a commit at all fails identically to a well-formed sha
  that does not exist. Those are different faults and were being reported as the
  same one: on 2026-08-20 a cure turn had its claimed commit extracted as
  \"/test_fm001_budgeted_solve.py\" — a file path — and the bounce was rejected
  :cure-commit-unresolved, which blames the author for naming a bad commit when
  in fact nothing ever extracted a commit."
  [commit]
  (boolean (and (string? commit) (re-matches commit-ish-pattern commit))))

(defn- run-revision-round
  "Run at most one revise-and-resubmit round. With no typed findings or a
  zero revision budget, return the original artifact and review unchanged."
  [opts phase-context author reviewer dispatched-turns target construction
   repo commit files author-job artifact-binding review-job review-gate stop-lines]
  (let [findings (review-findings-text review-job)
        revise? (and (pos? (long (or (:revision-rounds opts) 0)))
                     (= "done" (:state review-job))
                     findings)]
    (if-not revise?
      {:commit commit :repo repo :files files :author-job author-job
       :artifact-binding artifact-binding :review-job review-job
       :review-gate review-gate}
      (let [;; The reviewer must be pointed at what the AUTHOR actually
            ;; committed. `commit` can still be the pre-dispatch head when the
            ;; attempt was bound before authoring, and sending that makes the
            ;; reviewer read an unrelated commit and reject the revision on
            ;; artifact-binding grounds — measured on
            ;; repair-canary-067cd51a, where prior-commits carried the base
            ;; head (a trigger-classification test commit) rather than the
            ;; repair under review.
            ;;
            ;; artifact-binding/:commit is set only when the observation was
            ;; VALID (changed, descendant, inside the author window), so it is
            ;; the authored commit or nil — never a guess. commit-ish? keeps a
            ;; malformed binding out of the prompt; anything that is not a git
            ;; object name falls back rather than being sent as one.
            observed-author-commit (:commit artifact-binding)
            prior-commits [(if (commit-ish? observed-author-commit)
                             observed-author-commit
                             commit)]
            pre-revision-head (observe-repo-head opts repo)
            revision-response
            (run-phase!
             opts phase-context :revision-dispatch
             #(do
                (swap! dispatched-turns inc)
                ((or (:dispatch-fn opts) dispatch!) opts author
                 "wm-full-loop" target
                 (revision-author-prompt author reviewer
                                         (:attempt-evidence-dir opts)
                                         (:measured-acquisition? opts)
                                         target construction
                                         prior-commits findings))))
            revision-author-job
            (run-phase!
             opts phase-context :revision-wait
             #((or (:poll-fn opts) poll-job!) opts (:job-id revision-response)))
            _ (throw-if-cancelled! revision-author-job :revision-wait)
            _ (when-not (= "done" (:state revision-author-job))
                (throw
                 (ex-info "Revision author job did not complete"
                          {:outcome :build-failed
                           :failure-stage :revision-wait
                           :author-job revision-author-job
                           :review-job review-job
                           :commit commit
                           :reviews [(review-record 1 commit review-job review-gate)]})))
            _ (throw-if-author-refused! revision-author-job target :revision-wait)
            revision-build
            (run-phase!
             opts phase-context :revision-build
             #(let [binding (fresh-artifact-binding
                             opts repo pre-revision-head revision-author-job)
                    revision-commit (:commit binding)
                    build (when revision-commit
                            (resolve-target-build opts repo revision-commit))]
                (when-not (and revision-commit
                               (not= commit revision-commit)
                               (:repo build)
                               (vector? (:files build)))
                  (throw
                   (ex-info "Revision completed without a verifiable new commit"
                            {:outcome :build-failed
                             :failure-kind :artifact-binding-mismatch
                             :failure-stage :revision-build
                             :author-job revision-author-job
                             :review-job review-job
                             :commit commit
                             :artifact-binding binding
                             :reviews
                             [(review-record 1 commit review-job review-gate)]})))
                (when-not (= repo (:repo binding) (:repo build))
                  (throw
                   (ex-info "Revision artifact resolved outside the reviewed repository"
                            {:outcome :build-failed
                             :failure-kind :artifact-binding-mismatch
                             :failure-stage :revision-build
                             :author-job revision-author-job
                             :review-job review-job
                             :commit commit
                             :artifact-binding binding
                             :resolved-repository (:repo build)
                             :reviews
                             [(review-record 1 commit review-job review-gate)]})))
                {:commit revision-commit
                 :repo (:repo build)
                 :files (:files build)
                 :artifact-binding binding}))
            revision-commit (:commit revision-build)
            effective-author-job
            (-> (merge author-job revision-author-job)
                (assoc :repo-observed-artifact-ref revision-commit
                       :artifact-binding (:artifact-binding revision-build)
                       :revision-of (:job-id author-job)))
            re-review-response
            (run-phase!
             opts phase-context :re-review-dispatch
             #(do
                (swap! dispatched-turns inc)
                ((or (:dispatch-fn opts) dispatch!) opts reviewer
                 "wm-full-loop" target
                 (revision-reviewer-prompt
                  (assoc opts :reviewer reviewer)
                  target construction repo commit revision-commit
                  effective-author-job review-job stop-lines))))
            re-review-job
            (run-phase!
             opts phase-context :re-review-wait
             #((or (:poll-fn opts) poll-job!) opts (:job-id re-review-response)))
            _ (throw-if-cancelled! re-review-job :re-review-wait)
            re-review-gate
            (review-execution-gate (:files revision-build) re-review-job)
            reviews [(review-record 1 commit review-job review-gate)
                     (review-record 2 revision-commit
                                    re-review-job re-review-gate)]]
        {:commit revision-commit
         :repo (:repo revision-build)
         :files (:files revision-build)
         :author-job effective-author-job
         :artifact-binding (:artifact-binding revision-build)
         :review-job re-review-job
         :review-gate re-review-gate
         :reviews reviews
         :revision {:round 2
                    :commits [commit revision-commit]
                    :author-job (:job-id revision-author-job)
                    :review (second reviews)}}))))

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
  "Resolve an Agency artifact-ref to one Futon repository and its changed files.
  Returns nil for anything that is not commit-shaped, rather than probing every
  repository with it."
  ([commit]
   (when (commit-ish? commit)
     (when-let [repo (find-commit-repo commit)]
       {:repo repo :files (commit-files repo commit)})))
  ([commit target-repo]
   ;; Shared Git objects do not identify the commissioned working tree.
   ;; With an explicit target, never fall back to scanning sibling repos.
   (when (and (commit-ish? commit) (string? target-repo)
              (not (str/blank? target-repo)))
     (let [top (git target-repo "rev-parse" "--show-toplevel")]
       (when (and (zero? (:exit top))
                  (= (.getCanonicalPath (io/file target-repo))
                     (.getCanonicalPath (io/file (str/trim (:out top)))))
                  (zero? (:exit (git target-repo "cat-file" "-e"
                                     (str commit "^{commit}")))))
         {:repo target-repo :files (commit-files target-repo commit)})))))

(defn- resolve-target-build [opts target-repo commit]
  (if-let [resolve-fn (:resolve-build-fn opts)]
    (resolve-fn commit)
    (resolve-build commit target-repo)))

(defn- artifact-only-files? [files]
  (and (seq files)
       (every? #(or (str/includes? % "data/fold-turns")
                    (str/includes? % "fold-escrow")
                    (str/includes? % "selection-authoring-flights")
                    (str/includes? % "overnight-flights"))
               files)))

(defn- build-cure-validation
  "Run the two qualifying author-deliverable validations. Returns nil on
  success, or a map describing the failure on failure. Only artifact-only
  and feature-card-missing-or-invalid are curable — nothing else."
  [files author-job commit target]
  (cond
    (or (empty? files) (artifact-only-files? files))
    {:failure-kind :artifact-only
     :error-message "Authored commit is artifact-only"
     :ex-data {:outcome :artifact-only :commit commit :files files}}
    :else
    (let [{:keys [card reason source]} (feature-card-validation author-job)]
      (when-not card
        {:failure-kind :feature-card-missing-or-invalid
         :error-message (str "Author feature card is invalid: " (name reason)
                             " (source " (name source) ")")
         :ex-data {:outcome :build-failed
                   :failure-kind :feature-card-missing-or-invalid
                   :feature-card-invalid-reason reason
                   :feature-card-source source
                   :failure-stage :build-resolution
                   :commit commit
                   :target target
                   :files files
                   :author-job author-job}}))))

(defn- cure-commit-candidate
  "Choose the commit claimed by a bounded cure.

  Prefer repository observation, then the runner's own parsing of the
  FULL_LOOP_AUTHOR terminal marker, before Agency's separately extracted
  :artifact-ref.  The live card-only cure named the existing commit correctly
  in its terminal marker while Agency extracted a path from intervening prose.
  An explicitly malformed terminal claim remains malformed and is rejected by
  the existing typed gate below."
  [commit cure-observed cure-artifact-ref cure-job]
  (let [{:keys [verdict detail]} (author-verdict cure-job)
        terminal-commit (when (= :done verdict)
                          (some-> detail (str/split #"\s+" 2) first))]
    (or cure-observed terminal-commit cure-artifact-ref commit)))

(defn- build-cure-loop
  "Bounded cure loop wrapping the artifact-only and feature-card validations.
  On a qualifying failure, if retries remain, dispatches a cure job to the
  SAME author agent, polls it, re-resolves the build, and re-runs the same
  validations. When retries are exhausted, throws exactly as today (same
  outcome, same failure-kind, same repair-obligation path) with :build-retries
  included in the ex-data. Returns a map with the final :commit, :repo,
  :files, :author-job, and :build-retries.

  When :build-cure-retries is 0, no cure is attempted and the behavior is
  byte-identical to the pre-cure throws."
  [opts phase-context author dispatched-turns
   target commit repo files author-job fresh-author? artifact-binding]
  (let [max-retries (:build-cure-retries opts 0)]
    (loop [commit commit
           repo repo
           files files
           author-job author-job
           retries-left max-retries
           build-retries []]
      (if-let [failure (build-cure-validation files author-job commit target)]
        (if (pos? retries-left)
          ;; Bounce: dispatch cure to the same author agent.
          (let [{:keys [failure-kind error-message]} failure
                ;; Quote the ACTUAL durable prefix (:result-summary), not
                ;; job-text: job-text concatenates event narration, so
                ;; attempt-051's cure prompt buried the preserved prefix in
                ;; pages of prose and the cure reply led with prose too.
                job-prefix-text (:result-summary author-job)
                cure-prompt (build-cure-prompt author target commit error-message
                                               failure job-prefix-text)
                ;; Fresh pre-cure snapshot: the original pre-dispatch window
                ;; cannot bind a commit made during the cure turn, and
                ;; fresh-artifact-binding needs the {:head :observed-at-ms}
                ;; map, not the bare sha the first binding recorded.
                cure-pre-head (when fresh-author?
                                (observe-repo-head opts (:repo artifact-binding)))
                cure-response
                (run-phase! opts phase-context :build-cure-dispatch
                            #(do
                               (swap! dispatched-turns inc)
                               ((or (:dispatch-fn opts) dispatch!) opts author
                                "wm-full-loop" target cure-prompt)))
                cure-job-id (:job-id cure-response)
                cure-job
                (run-phase! opts phase-context :build-cure-wait
                            #((or (:poll-fn opts) poll-job!) opts cure-job-id))
                _ (throw-if-cancelled! cure-job :build-cure-wait)
                _ (throw-if-author-refused! cure-job target :build-cure-wait)
                ;; Re-resolve the build: the commit may have changed.
                cure-artifact-ref (:artifact-ref cure-job)
                cure-binding (when fresh-author?
                               (fresh-artifact-binding opts
                                                       (:repo artifact-binding)
                                                       cure-pre-head
                                                       cure-job))
                cure-observed (:commit cure-binding)
                new-commit (cure-commit-candidate commit cure-observed
                                                  cure-artifact-ref cure-job)
                commit-changed? (and new-commit (not= new-commit commit))
                new-build (when commit-changed?
                            (resolve-target-build opts repo new-commit))]
            (if (and commit-changed? (nil? new-build))
              ;; Fail closed: the cure turn claims a NEW commit that resolves
              ;; to no repository. The old binding of new-repo/new-files fell
              ;; back to the PRIOR file list here, so a card-only validation
              ;; would have declared the bounce cured and bound the phantom
              ;; sha downstream. Reject the bounce wholesale instead.
              ;;
              ;; The rejection is TYPED by which fault it is. A well-formed sha
              ;; that no repository has is :cure-commit-unresolved — the author
              ;; named a commit that is not there. A value that is not
              ;; commit-shaped at all is :cure-commit-malformed — nothing
              ;; extracted a commit, so the fault is upstream in extraction and
              ;; the author is not the one to look at. Both still reject.
              (recur commit repo files author-job
                     (dec retries-left)
                     (conj build-retries
                           {:failure-kind failure-kind
                            :error error-message
                            :cure-job-id cure-job-id
                            :cured? false
                            :cure-rejected (if (commit-ish? new-commit)
                                             :cure-commit-unresolved
                                             :cure-commit-malformed)
                            :claimed-commit new-commit}))
              (let [new-repo (or (:repo new-build) repo)
                    new-files (or (:files new-build) files)
                    new-author-job (cond-> cure-job
                                     (and fresh-author? cure-observed)
                                     (assoc :repo-observed-artifact-ref cure-observed
                                            :artifact-binding cure-binding))
                    ;; Re-run the same validations to determine if cured.
                    still-failing? (build-cure-validation new-files new-author-job
                                                          new-commit target)
                    cured? (nil? still-failing?)
                    retry-entry {:failure-kind failure-kind
                                 :error error-message
                                 :cure-job-id cure-job-id
                                 :cured? cured?}]
                (if cured?
                  ;; Cured: proceed with the new values.
                  {:commit new-commit :repo new-repo :files new-files
                   :author-job new-author-job
                   :build-retries (conj build-retries retry-entry)}
                  ;; Not cured yet: try again if retries remain.
                  (recur new-commit new-repo new-files new-author-job
                         (dec retries-left)
                         (conj build-retries retry-entry))))))
          ;; Retries exhausted: throw exactly as today. When no bounces
          ;; occurred (retries was 0), the ex-data is byte-identical to
          ;; the pre-cure throw — :build-retries is only included when at
          ;; least one bounce was attempted.
          (throw
           (ex-info (:error-message failure)
                    (cond-> (:ex-data failure)
                      (seq build-retries)
                      (assoc :build-retries build-retries)))))
        ;; No failure: pass through with empty (or accumulated) retries.
        {:commit commit :repo repo :files files
         :author-job author-job
         :build-retries build-retries}))))

(defn- implementation-id [commit]
  (str "full-loop/implementation/" commit))

(defn- discharge-id
  "An attempt ordinal is local to a cohort store. Include the run identity as
  well: cohort names are reusable across stores/re-runs. Nil cohort denotes a
  non-cohort opportunity; the run id is still mandatory. Encoding components
  separately prevents slashes or percent escapes from aliasing another id."
  [cohort-id run-id attempt-id]
  (let [component (fn [value]
                    (let [s (if (keyword? value) (subs (str value) 1) value)]
                      (when-not (and (string? s) (not (str/blank? s)))
                        (throw (ex-info "Discharge requires a complete execution identity"
                                        {:failure-kind :discharge-identity-invalid
                                         :cohort-id cohort-id :run-id run-id
                                         :attempt-id attempt-id})))
                      (java.net.URLEncoder/encode s "UTF-8")))]
    (str "full-loop/discharge/"
         (when (some? cohort-id) (str "cohort/" (component cohort-id) "/"))
         "run/" (component run-id) "/attempt/" (component attempt-id))))

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
      (selected-cascade {:action selected-action})
      (assoc :implementation/selected-cascade
             (selected-cascade {:action selected-action}))

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
  (let [discharge-ref (discharge-id (:cohort-id opts) (:run-id opts) attempt-id)
        impl-id (implementation-id commit)
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
        discharge (cond-> {:xt/id discharge-ref
                   :entity/type :discharge
                   :entity/name (str "Full-loop discharge " attempt-id)
                   :entity/source "wm-full-loop"
                   :discharge/mission (str target)
                   :discharge/run-id (:run-id opts)
                   :discharge/attempt-id attempt-id
                   :discharge/endpoint impl-id
                   :discharge/type :implementation/commit
                   :discharge/proof-query (str "GET /api/alpha/entity/" impl-id)
                   :discharge/reviewer reviewer
                   :discharge/review-job (:job-id review-job)
                   :discharge/at (str (Instant/now))}
                    (:cohort-id opts)
                    (assoc :discharge/cohort-id (:cohort-id opts))
                    (selected-cascade {:action (:selected-action construction)})
                    (assoc :discharge/selected-cascade
                           (selected-cascade {:action (:selected-action construction)})))]
    (when before
      (throw (ex-info "Implementation commit already grounded"
                      {:outcome :grounded-no-change :implementation-id impl-id})))
    (substrate/put-doc! implementation opts)
    (substrate/put-doc! discharge opts)
    ;; The substrate indexes asynchronously, so an immediate readback can
    ;; miss a successful write: r5 attempt-001 (2026-09-13) grounded
    ;; f9896cf6, the entity is durably present, but the instant readback saw
    ;; nil and the witness reported {:resolved? false :dial-moved? false},
    ;; refusing an actually-grounded repair.  Await visibility, bounded.
    (let [after (loop [tries 0]
                  (or (substrate/entity-by-id impl-id opts)
                      (when (< tries 20)
                        (Thread/sleep 500)
                        (recur (inc tries)))))]
      (when-not after
        (throw (ex-info "Grounded implementation did not become visible"
                        {:outcome :incomplete
                         :failure-kind :grounding-not-visible
                         :failure-stage :grounding
                         :implementation-id impl-id})))
      {:before {:implementation-entity before}
       :after {:implementation-entity after}
       :resolved? (= commit (get-in after [:props :implementation/commit]))
       :dial-moved? (and (nil? before) (some? after))
       :implementation-id impl-id
       :discharge-id discharge-ref})))

(defn- term [judgment ground]
  {:judgment judgment :ground ground})

(defn entity-state-at-close
  "Retain the selected entity's belief row without declaring argmax to be the
  admissible categorical state estimator.  The row is the observation; the
  derived status is labelled as such.  Ties and unavailable rows stay typed.

  `belief` must be the judgment already in force for this selection.  Callers
  must not substitute a later or temporally-nearest trace row."
  [entity-id belief belief-source recorded-at]
  (if (nil? entity-id)
    {:schema :wm/entity-state-at-close-v1
     :status :absent
     :reason :no-selected-entity
     :recorded-at recorded-at}
    (let [row (get belief entity-id)]
      (if-not (and (map? row) (= 7 (count row)) (every? number? (vals row)))
        {:schema :wm/entity-state-at-close-v1
         :entity/id entity-id
         :status :absent
         :reason :in-force-belief-row-unavailable
         :belief-source belief-source
         :recorded-at recorded-at}
        (let [maximum (apply max (vals row))
              leaders (->> row
                           (keep (fn [[status mass]]
                                   (when (= maximum mass) status)))
                           vec)]
          (cond-> {:schema :wm/entity-state-at-close-v1
                   :entity/id entity-id
                   :belief-row row
                   :status-method :derived-unique-argmax-of-mu-post
                   :belief-source belief-source
                   :recorded-at recorded-at}
            (= 1 (count leaders))
            (assoc :derived-status (first leaders))

            (not= 1 (count leaders))
            (assoc :derived-status :ambiguous-tie
                   :status :refused
                   :reason :ambiguous-tie)))))))

(defn outcome-entity-at-close
  "Name the exact selection target on every close, or retain why none exists.
  Target classes and mission prose are deliberately not fallback identities.
  Packet-1's belief snapshot is an independent copy of the identity and must
  agree before the append-only close record is written."
  [{:keys [selection-reached? selection-made? entity-id]} entity-state]
  (let [outcome-entity
        (cond
          (some? entity-id)
          {:status :present :entity/id entity-id :source :selection-target}

          (not selection-reached?)
          {:status :absent :reason :failed-before-selection}

          (not selection-made?)
          {:status :absent :reason :no-selection-made}

          :else
          {:status :absent :reason :selection-had-no-target})
        state-id (:entity/id entity-state)]
    (when (and state-id
               (or (not= :present (:status outcome-entity))
                   (not= state-id (:entity/id outcome-entity))))
      (throw (ex-info "Close entity identities disagree"
                      {:refusal :entity-identity-mismatch
                       :outcome-entity outcome-entity
                       :entity-state-at-close entity-state})))
    outcome-entity))

(defn- sorry [kind data]
  {:sorry (assoc data :kind kind)})

(defn- outcome-from [e]
  (let [raw (or (:outcome (ex-data e)) :incomplete)]
    (cond
      ;; A pin refusal is a guardrail outcome, not a new disposition carrier.
      ;; Its exact subtype stays in failure-kind/detail and durable finding data.
      (= :pinned-selection-refused raw) :guardrail-refusal
      (#{:agent-job-stalled :construction-failed :grounding-failed
         :policy-nondiscrimination :incomplete} raw) :incomplete
      :else raw)))

(defn- append-checkpoint-or-refusal-sorry!
  "Durably append CELL via APPEND-FN and record the event via RECORD-FN.
  When the cohort refuses an invalid grounded cell, append a TYPED SORRY
  retaining the refusal, record it, and rethrow the original error: the
  attempt still stops on the true failure, but the checkpoint chain stays
  complete, so the failure-path close records the honest outcome instead of
  cascading into a required-checkpoints-missing refusal charged to a
  synthetic initialization identity (repair-initialization-a9177cab,
  2026-09-13: one invalid construction cell produced two stop-line findings
  and orphaned the real attempt id)."
  [append-fn record-fn checkpoint cell]
  (try
    (record-fn (append-fn cell))
    (catch clojure.lang.ExceptionInfo e
      (when (= :invalid-checkpoint-cell (:failure-kind (ex-data e)))
        (record-fn
         (append-fn {:sorry {:kind :invalid-checkpoint-cell
                             :outcome :incomplete
                             :refused-checkpoint checkpoint
                             :cell-errors (:errors (ex-data e))}})))
      (throw e))))

(defn- mint-action-occurrence-once!
  [occurrence-atom inputs]
  (let [occurrence (close-retention/mint-occurrence inputs)]
    (when-not (compare-and-set! occurrence-atom nil occurrence)
      (throw (ex-info "Action occurrence already minted for this attempt"
                      {:close-retention/refusal :action-occurrence-already-minted})))
    occurrence))

(defn- repair-occurrence
  "Create or propagate one repair occurrence across containment boundaries.
  The stable origin and event identity are authority-qualified; local attempt
  ordinals remain display labels only."
  [existing origin event-id failure-kind created-at]
  (or existing
      (repair/occurrence-identity
       {:origin origin :event-id event-id :failure-kind failure-kind
        :created-at created-at})))

(def ^:private limb-record-schemas
  (into #{:wm/limb-receipt-v1 :wm/target-standing-decision-v1
          :wm/entity-revision-pair-v1} interpretation-evidence/schemas))

(defn- parse-attempt-evidence [bytes path]
  (let [eof-marker (Object.)]
    (try
      (let [decoder (doto (.newDecoder java.nio.charset.StandardCharsets/UTF_8)
                      (.onMalformedInput java.nio.charset.CodingErrorAction/REPORT)
                      (.onUnmappableCharacter java.nio.charset.CodingErrorAction/REPORT))
            text (str (.decode decoder (java.nio.ByteBuffer/wrap bytes)))]
        (with-open [reader (java.io.PushbackReader. (java.io.StringReader. text))]
          (let [value (edn/read {:eof eof-marker} reader)
                tail (edn/read {:eof eof-marker} reader)]
            (when (or (identical? eof-marker value)
                      (not (identical? eof-marker tail)))
              (throw (ex-info "Attempt evidence must contain one EDN form"
                              {:limb-evidence/refusal :evidence-not-single-edn
                               :source-path path})))
            value)))
      (catch clojure.lang.ExceptionInfo e (throw e))
      (catch Throwable e
        (throw (ex-info "Attempt evidence EDN invalid"
                        {:limb-evidence/refusal :evidence-edn-invalid
                         :source-path path}
                        e))))))

(defn standing-decision-readback
  "Read the exact-seat standing decision and annotate, without changing its
  verdict, a resolved claim whose required production successor has no
  resolution record. RESOLUTION-READ-FN is the store authority seam and
  receives the exact repair id."
  [evidence-dir target reviewer author discharge-contract resolution-read-fn]
  (when (and evidence-dir (.isDirectory (io/file evidence-dir)))
    (some
     (fn [^java.io.File file]
       (try
         (let [record (parse-attempt-evidence
                       (Files/readAllBytes (.toPath file))
                       (.getAbsolutePath file))
               record (when (= :wm/target-standing-decision-v1 (:schema record))
                        (limb-evidence/validate-standing-decision record))]
           (when (and record (= target (:entity/id record))
                      (= reviewer (:decided-by record))
                      (= author (:implementation-author record)))
             (if (and (= :resolved (:decision record))
                      (some #{:distinct-production-shaped-successor}
                            (:requires discharge-contract)))
               (let [resolution (try
                                  {:record (resolution-read-fn target)}
                                  (catch Throwable e {:error e}))]
                 (cond
                   (:error resolution)
                   (assoc record :standing/store-annotation
                          :resolution-store-unreadable)

                   (nil? (:record resolution))
                   (assoc record :standing/store-annotation
                          :resolution-unsupported-by-store)

                   :else record))
               record)))
         (catch Throwable _ nil)))
     (sort-by #(.getName ^java.io.File %)
              (filter #(.isFile ^java.io.File %)
                      (seq (.listFiles (io/file evidence-dir))))))))

(defn- retained-resolution-record
  [repair-id]
  (let [file (io/file repair/default-root "resolutions" (str repair-id ".edn"))]
    (when (.isFile file)
      (parse-attempt-evidence (Files/readAllBytes (.toPath file))
                              (.getAbsolutePath file)))))

(defn- valid-standing-decision?
  [evidence-dir target reviewer author discharge-contract resolution-read-fn]
  (boolean (standing-decision-readback evidence-dir target reviewer author
                                       discharge-contract resolution-read-fn)))

(defn validate-revision-evidence-role
  "Validate the filename-declared role of a revision pair. `subject-*.edn`
  names the selected repair obligation itself and must use its exact id;
  `supporting-*.edn` may name a source artifact. Any other name is ambiguous
  and refuses before companion bytes are admitted."
  [filename selected-target record]
  (cond
    (str/starts-with? filename "subject-")
    (when-not (= selected-target (:entity/id record))
      (throw (ex-info "Subject revision pair names another entity"
                      {:limb-evidence/refusal :subject-entity-mismatch
                       :selected-target selected-target
                       :entity/id (:entity/id record)
                       :filename filename})))

    (str/starts-with? filename "supporting-") nil

    :else
    (throw (ex-info "Revision pair role is ambiguous"
                    {:limb-evidence/refusal :revision-role-ambiguous
                     :filename filename})))
  record)

(defn- standing-completion-prompt [target evidence-dir]
  (str "Deposit the standing decision for " (pr-str target) " in "
       evidence-dir ". Decision content is yours; this request neither repeats "
       "implementation nor suggests a decision."))

(defn- ensure-standing-decision!
  [evidence-dir target reviewer author discharge-contract resolution-read-fn
   dispatch-completion! wait-completion!]
  (when-not (valid-standing-decision? evidence-dir target reviewer author
                                      discharge-contract resolution-read-fn)
    (let [response (dispatch-completion!
                    (standing-completion-prompt target evidence-dir))]
      (try (wait-completion! response) (catch Throwable _ nil)))
    (when-not (valid-standing-decision? evidence-dir target reviewer author
                                        discharge-contract resolution-read-fn)
      (throw (ex-info "Standing evidence insufficient"
                      {:outcome :incomplete
                       :failure-kind :standing-evidence-insufficient
                       :failure-stage :standing-completion
                       :target target :reviewer reviewer :author author}))))
  ;; Return the annotated readback, not a bare boolean: the ruling
  ;; (RULING-repair-subject-positive-status-2026-09-14 §5, packet 1)
  ;; requires a :resolved claim with no store resolution to be VISIBLE to
  ;; later observers, so the caller persists this into the close.
  (standing-decision-readback evidence-dir target reviewer author
                              discharge-contract resolution-read-fn))

(defn- retain-token-outcome!
  [data-root cohort-id attempt-id prediction d-result artifact-sha & [context]]
  (let [source (:source d-result)
        source-record
        (when-let [path (:path source)]
          (let [bytes (Files/readAllBytes (.toPath (io/file path)))]
            (when-not (= (:sha256 source) (sha256-bytes bytes))
              (throw (ex-info "D-task evidence changed before comparison"
                              {:token-outcome/refusal :evidence-digest-mismatch})))
            (edn/read-string (String. bytes "UTF-8"))))
        measurements (:after-token-evidence source-record)
        receipt (assoc (token-outcome/compare-outcomes prediction measurements artifact-sha)
                       :measurement-source source
                       :measurement-verification (:verification d-result))
        learning (attempt-learning/receipt
                  {:comparison receipt :source-record source-record
                   :occurrence (or (:occurrence context) (get-in source-record [:dispatch :occurrence]))
                   :route (or (:route context) (:route source-record))
                   :expected (:expected context) :read-job (:read-job context)})
        learning (learning-ledger/record! (or (:ledger-root context) learning-ledger/default-root) learning)
        receipt (assoc receipt :learning-trial-receipt learning)
        surprises (surprise/records
                   {:comparison receipt
                    :occurrence (or (:occurrence context) (get-in source-record [:dispatch :occurrence]))
                    :declared-at (:selection-recorded-at context)
                    :observed-at (:observed-at source-record)})
        surprise-file (io/file data-root (name cohort-id) attempt-id "retained" "surprises.edn")
        ;; retained/, not the attempt dir itself (closed-execution's exact
        ;; file set) nor evidence/ (enumerated into the manifest separately).
        file (io/file data-root (name cohort-id) attempt-id "retained" "token-outcome.edn")]
    (io/make-parents file)
    (spit file (pr-str receipt))
    (spit surprise-file (pr-str surprises))
    {:surprises surprises
     :surprise-entry {:evidence/id (str (name cohort-id) "/" attempt-id "/retained/surprises.edn")
                      :source-path (.getAbsolutePath surprise-file)
                      :expected-sha256 (sha256-bytes (Files/readAllBytes (.toPath surprise-file)))
                      :admitted-at (str (Instant/now))}
     :receipt receipt
     :entry {:evidence/id (str (name cohort-id) "/" attempt-id "/retained/token-outcome.edn")
             :source-path (.getAbsolutePath file)
             :expected-sha256 (sha256-bytes (Files/readAllBytes (.toPath file)))
             :admitted-at (str (Instant/now))}}))

(defn- retain-kernel-example!
  [data-root cohort-id attempt-id inputs d-result expected read-job]
  (let [source (:source d-result)
        record (when-let [path (:path source)]
                 (let [bytes (Files/readAllBytes (.toPath (io/file path)))]
                   (when-not (= (:sha256 source) (sha256-bytes bytes))
                     (throw (ex-info "D-task evidence changed before alignment"
                                     {:kernel-example/refusal :evidence-digest-mismatch})))
                   (edn/read-string (String. bytes "UTF-8"))))
        receipt (kernel-example/collect
                 (assoc inputs :record record :source source :domain (kernel-example/declaration))
                 expected read-job)
        file (io/file data-root (name cohort-id) attempt-id "retained" "kernel-example.edn")]
    (io/make-parents file)
    (spit file (pr-str receipt))
    {:receipt receipt
     :entry {:evidence/id (str (name cohort-id) "/" attempt-id "/retained/kernel-example.edn")
             :source-path (.getAbsolutePath file)
             :expected-sha256 (sha256-bytes (Files/readAllBytes (.toPath file)))
             :admitted-at (str (Instant/now))}}))

(defn- retain-run-ending!
  [data-root cohort-id attempt-id input]
  (let [receipt (run-ending/classify input)
        file (io/file data-root (name cohort-id) attempt-id "retained"
                      "run-ending-classification.edn")]
    (io/make-parents file)
    (spit file (pr-str receipt))
    {:receipt receipt
     :entry {:evidence/id (str (name cohort-id) "/" attempt-id
                               "/retained/run-ending-classification.edn")
             :source-path (.getAbsolutePath file)
             :expected-sha256 (sha256-bytes (Files/readAllBytes (.toPath file)))
             :admitted-at (str (Instant/now))}}))

(defn- failure-close-evidence!
  "Reclassify a close-time failure and admit every receipt already retained.

  This boundary deliberately does not re-parse evidence/: malformed limb
  evidence is the failure being closed. Checkpoints and retained receipts are
  immutable inputs to the final failure close; only the run-ending receipt is
  replaced because its projection must describe that final judgment."
  [events data-root cohort-id attempt-id close-judgment]
  (let [cohort-name (name cohort-id)
        attempt-dir (io/file data-root cohort-name attempt-id)
        retained-dir (io/file attempt-dir "retained")
        route-file (io/file retained-dir "route-attestation.edn")
        route (when (.isFile route-file) (cohort/read-edn route-file))
        run-ending (retain-run-ending!
                    data-root cohort-id attempt-id
                    {:close close-judgment
                     :occurrence (:occurrence close-judgment)
                     :route-attestation route})
        checkpoint-entries
        (mapv (fn [{:keys [event/sequence checkpoint/type]}]
                (let [filename (format "%03d-%s.edn" sequence (name type))]
                  {:evidence/id (str cohort-name "/" attempt-id "/" filename)
                   :source-path (.getAbsolutePath (io/file attempt-dir filename))
                   :admitted-at (str (Instant/now))}))
              (sort-by :event/sequence (vals events)))
        retained-files (if (.isDirectory retained-dir)
                         (sort-by #(.getName ^java.io.File %)
                                  (filter #(.isFile ^java.io.File %)
                                          (seq (.listFiles retained-dir))))
                         [])
        retained-entries
        (mapv (fn [^java.io.File file]
                {:evidence/id (str cohort-name "/" attempt-id "/retained/"
                                   (.getName file))
                 :source-path (.getAbsolutePath file)
                 :admitted-at (str (Instant/now))})
              retained-files)
        manifest (evidence-manifest/build-manifest
                  {:entries (into checkpoint-entries retained-entries)
                   :read-bytes #(Files/readAllBytes (.toPath (io/file %)))})]
    {:run-ending (:receipt run-ending)
     :manifest manifest}))

(defn- checkpoint-evidence-manifest
  [events data-root cohort-id attempt-id selected-target & [interpretation-context]]
  (let [cohort-name (name cohort-id)
        ordered-events (sort-by :event/sequence (vals events))
        checkpoint-entries
        (mapv (fn [{:keys [event/sequence checkpoint/type]}]
                (let [filename (format "%03d-%s.edn" sequence (name type))]
                  {:evidence/id (str cohort-name "/" attempt-id "/" filename)
                   :source-path (.getAbsolutePath
                                 (io/file data-root cohort-name attempt-id filename))
                   :admitted-at (str (Instant/now))}))
              ordered-events)
        evidence-dir (io/file data-root cohort-name attempt-id "evidence")
        evidence-files (if (.isDirectory evidence-dir)
                         (sort-by #(.getName ^java.io.File %)
                                  (seq (.listFiles evidence-dir)))
                         [])
        captured (atom {})
        captured-evidence
        (mapv
         (fn [^java.io.File file]
           (let [path (.getAbsolutePath file)
                 admitted-at (str (Instant/now))
                 bytes (try
                         (Files/readAllBytes (.toPath file))
                         (catch Throwable e
                           (throw (ex-info "Attempt evidence source unavailable"
                                           {:evidence-manifest/refusal
                                            :source-unavailable
                                            :source-path path}
                                           e))))]
             (swap! captured assoc path bytes)
             {:filename (.getName file) :path path :bytes bytes
              :admitted-at admitted-at
              :parsed (try
                        {:value (parse-attempt-evidence bytes path)}
                        (catch clojure.lang.ExceptionInfo e {:error e}))}))
         evidence-files)
        record-captures
        (filterv #(contains? limb-record-schemas
                             (get-in % [:parsed :value :schema]))
                 captured-evidence)
        records (mapv #(let [record (get-in % [:parsed :value])]
                        (if (interpretation-evidence/schemas (:schema record))
                          (interpretation-evidence/validate-record record)
                          (limb-evidence/validate-record record)))
                      record-captures)
        _ (doseq [[capture record] (map vector record-captures records)
                  :when (= :wm/entity-revision-pair-v1 (:schema record))]
            (validate-revision-evidence-role (:filename capture)
                                             selected-target record))
        companion-names
        (into #{}
              (mapcat (fn [record]
                        (concat (when (interpretation-evidence/schemas (:schema record))
                                  (interpretation-evidence/companion-files record))
                                (keep record [:stdout-file :stderr-file])
                                (when (= :wm/entity-revision-pair-v1
                                         (:schema record))
                                  (keep #(get-in record [% :file])
                                        [:before :after])))))
              records)
        captures-by-name (into {} (map (juxt :filename :bytes)) captured-evidence)
        _ (doseq [{:keys [filename parsed]} captured-evidence
                  :when (and (not (contains? companion-names filename))
                             (not (contains? limb-record-schemas
                                             (get-in parsed [:value :schema]))))]
            (if-let [error (:error parsed)]
              (throw error)
              (throw (ex-info "Attempt evidence schema invalid"
                              {:limb-evidence/refusal :schema-mismatch
                               :filename filename}))))
        interpretation-records (filterv #(interpretation-evidence/schemas (:schema %)) records)
        _ (when (or (seq interpretation-records)
                    (get-in events [:construction :payload :judgment :interpretation-receipt]))
            (let [start-path (:source-path (first checkpoint-entries))
                  context (assoc interpretation-context
                                 :data-root (.getCanonicalPath (io/file data-root))
                                 :start-event-sha256
                                 (interpretation-evidence/sha256
                                  (Files/readAllBytes (.toPath (io/file start-path)))))
                  by-file (into {} (map (fn [capture]
                                         [(:filename capture) (get-in capture [:parsed :value])]))
                                record-captures)]
              (interpretation-evidence/validate-admission!
               interpretation-records captures-by-name by-file context
               (get-in events [:construction :payload :judgment :interpretation-receipt]))))
        _ (doseq [receipt (filter #(= :wm/limb-receipt-v1 (:schema %)) records)]
            (limb-evidence/validate-limb-receipt-outputs
             receipt #(get captures-by-name %)))
        _ (doseq [pair (filter #(= :wm/entity-revision-pair-v1 (:schema %)) records)]
            (limb-evidence/validate-revision-pair-files
             pair #(get captures-by-name %)))
        evidence-entries
        (mapv (fn [{:keys [filename path admitted-at]}]
                {:evidence/id (str cohort-name "/" attempt-id "/evidence/" filename)
                 :source-path path
                 :admitted-at admitted-at})
              captured-evidence)
        entries (cond-> (into checkpoint-entries evidence-entries)
                  (:token-outcome-entry interpretation-context)
                  (conj (:token-outcome-entry interpretation-context))
                  (:surprise-entry interpretation-context)
                  (conj (:surprise-entry interpretation-context))
                  (:route-attestation-entry interpretation-context)
                  (conj (:route-attestation-entry interpretation-context))
                  (:kernel-example-entry interpretation-context)
                  (conj (:kernel-example-entry interpretation-context))
                  (:run-ending-entry interpretation-context)
                  (conj (:run-ending-entry interpretation-context)))]
    (evidence-manifest/build-manifest
     {:entries entries
      :read-bytes (fn [path]
                    (or (get @captured path)
                        (Files/readAllBytes (.toPath (io/file path)))))})))

(def ^:private transport-failure-classes
  "Recognised transport conditions, matched by CLASS (most specific first), not
  by exact class name. Name-keyed lookup missed every subclass: it is why
  attempt-057/058 (HttpTimeoutException) were repaired while
  initialization-6dd14f9e (SocketException) opened a fresh obligation of the
  same kind, and why java.net.BindException still fell through.

  A transport condition is not the machine's fault, so it must discharge as
  :environmental-hold (:cleared-precondition) rather than :machine-failure,
  which would demand a distinct repair commit and an independent review for a
  network fault no code change can fix.

  DELIBERATELY ABSENT: java.util.concurrent.TimeoutException. It is raised by
  any bounded wait — future/get, executor shutdown, an internal poll — so typing
  it environmental would let a genuinely hung machine path escape the
  machine-failure contract. Job-level stalls already have :agent-job-stalled.

  DELIBERATELY ABSENT: bare java.net.SocketException, handled separately below —
  it is the one entry whose subclasses are all transport but whose own instances
  are not."
  [[java.net.http.HttpTimeoutException :transport-timeout]
   [java.net.SocketTimeoutException :transport-timeout]
   [java.net.ConnectException :transport-unavailable]
   [java.net.BindException :transport-unavailable]
   [java.net.NoRouteToHostException :transport-unavailable]
   [java.net.PortUnreachableException :transport-unavailable]
   [java.net.UnknownHostException :transport-unavailable]])

(def ^:private unavailable-socket-messages
  "Bare SocketException is too broad to type wholesale: its subclasses are all
  transport, but a bare instance can also report LOCAL socket lifecycle misuse,
  which IS a machine fault. Those cases are indistinguishable by type, so this
  set names only the JDK wordings for a channel that went away underneath us.

  \"Socket is closed\" is DELIBERATELY ABSENT, and the omission is one word away
  from a listed entry, so do not \"fix\" it. Measured on this JDK:

      (doto (java.net.Socket.) .close) then .getInputStream / .getOutputStream
      / .setSoTimeout / .bind  ->  SocketException \"Socket is closed\"

  That is OUR code using a socket after closing it — a machine fault, and it
  must keep the machine-failure contract. \"Socket closed\" (no \"is\") is the
  different case: a blocking operation aborted because the channel went away,
  which is the transient condition initialization-6dd14f9e actually hit.

  Matching on message text is a known weakness — this same file refuses to do it
  for stopping-rule recognition, on the grounds that text matching misclassifies
  unrelated failures. It is accepted here only because a bare SocketException
  carries no typed discriminator at all, and the alternative (typing every bare
  SocketException environmental) would hide use-after-close bugs precisely like
  the one above. The set is enumerated rather than substring-matched, and
  transport-message-typing-is-enumerated-not-fuzzy pins what is in and what is
  out."
  #{"socket closed"
    "connection reset"
    "connection reset by peer"
    "broken pipe"})

(defn- cause-chain
  "The throwable and its causes, bounded: cause chains can be self-referential."
  [e]
  (loop [t e acc [] depth 0]
    (if (and t (< depth 16))
      (recur (.getCause ^Throwable t) (conj acc t) (inc depth))
      acc)))

(defn- explicit-failure-kind
  "First ex-data classification ANYWHERE in the cause chain. A phase that typed
  its own failure keeps that classification even when an untyped throw wraps it,
  and even when a transport exception sits deeper in the same chain — otherwise
  the transport walk below could silently outrank a real typed machine failure."
  [e]
  (some (fn [t] (or (:failure-kind (ex-data t)) (:outcome (ex-data t))))
        (cause-chain e)))

(defn- transport-class-kind
  "Class-based lookup, most specific first. instance? rather than name equality,
  so a subclass of a recognised condition is covered without being enumerated."
  [t]
  (some (fn [[klass kind]] (when (instance? klass t) kind))
        transport-failure-classes))

(defn- socket-closure-kind
  "Bare SocketException only, gated on an enumerated message. Subclasses are
  already handled by class above and never reach here."
  [t]
  (when (and (instance? java.net.SocketException t)
             (contains? unavailable-socket-messages
                        (some-> (.getMessage ^Throwable t)
                                str/trim
                                str/lower-case)))
    :transport-unavailable))

(defn- transport-failure-kind
  "First recognised transport condition in the cause chain, because these arrive
  wrapped (ex-info ... cause). Returns nil for anything unrecognised: an unknown
  exception STAYS :untyped-failure and keeps the heavy machine-failure contract.
  This narrows the untyped bucket; it must never widen the escape hatch."
  [e]
  (some (fn [t] (or (transport-class-kind t) (socket-closure-kind t)))
        (cause-chain e)))

(defn- failure-kind-from [e]
  ;; Explicit typing wins at ANY depth; transport typing only fills the gap
  ;; ahead of :untyped-failure.
  (or (explicit-failure-kind e)
      (transport-failure-kind e)
      :untyped-failure))

(defn- repair-class-for [failure-kind]
  (cond
    (#{:agent-unavailable :agent-readiness-failed :substrate-unavailable
       :dispatch-failed :abstained :no-selection :guardrail-refusal
       :transport-timeout :transport-unavailable :trigger-ineligible
       :operator-cancelled}
     failure-kind)
    :environmental-hold

    (#{:agent-budget-expired :agent-job-stalled} failure-kind)
    :incomplete-recoverable

    ;; Only identified content gaps have an environmental discharge.
    ;; Identity/dispatch invariants, machine faults and unknown kinds retain
    ;; the machine-repair contract. Budget and availability map above.
    (#{:interpretation/no-relevant-pattern :interpretation/genesis-required
       :interpretation/source-changed :interpretation/invalid-receipt
       :interpretation/unmeasurable-fact :interpretation/no-citable-tension
       :interpretation/target-unresolved :interpretation/retrieval-unavailable
       :interpretation/source-unavailable :interpretation/source-revision-unavailable
       :interpretation/action-type-unsupported}
     failure-kind)
    :environmental-hold

    :else :machine-failure))

(defn- last-error-phase [phase-events]
  (or (:phase (last (filter #(= :error (:outcome %)) phase-events)))
      (:phase (last phase-events))
      :opportunity))

(defn- discharge-contract [repair-class]
  (assoc
   (case repair-class
     :machine-failure
     {:requires [:distinct-repair-commit :independent-review
                 :grounded-repair :distinct-production-shaped-successor]}
     :independent-review-failure
     ;; Mirrors repair/review-failure-discharge-contract: a rejected commit
     ;; is discharged exactly like a machine failure — a DISTINCT commit
     ;; through the full gate ladder, never a resubmission of the failed one.
     {:requires [:distinct-repair-commit :independent-review
                 :grounded-repair :distinct-production-shaped-successor]}
     :environmental-hold
     {:requires [:cleared-precondition :grounded-production-shaped-successor]}
     :incomplete-recoverable
     {:requires [:complete-existing-author-artifact :independent-review
                 :grounded-existing-commit]}
     {:requires [:grounded-production-shaped-successor]})
   :artifact-shape :code-commit))

(defn- deferred-completion-job-id [obligation]
  (or (get-in obligation [:failure-data :job-id])
      (get-in obligation [:backtrace :job-id])
      (:author-job-id obligation)))

(defn- deferred-completion-snapshot
  "Read, but never restart, the Agency job named by an incomplete-recoverable finding."
  [opts obligation]
  (when (and (= :incomplete-recoverable (:repair/class obligation))
             (deferred-completion-job-id obligation))
    ((or (:read-job-fn opts) read-job!)
     opts (deferred-completion-job-id obligation))))

(defn unvalidated-artifact-failure
  "Which failure, if any, a fresh author's unvalidated artifact-ref represents.

  Two different faults were reported as one. :artifact-binding-mismatch says
  Agency named a commit that repository observation could not validate. But on
  canary-de75cee9 the ref arrived as \"/eoi_network_test.clj\" — a file path
  scraped from author text — so Agency named NO commit, and the fault is
  upstream in extraction rather than in the author's binding.

  Extracted from run-opportunity-core! because the branch was only reachable by
  driving the whole runner; inline, it could not be tested, which is how the
  mislabelling survived. Returns nil when there is nothing to report."
  [fresh-author? text-commit observed-commit]
  (when (and fresh-author? text-commit (nil? observed-commit))
    (if (commit-ish? text-commit)
      {:failure-kind :artifact-binding-mismatch
       :message "Agency claimed an author artifact that repository observation did not validate"}
      {:failure-kind :artifact-ref-malformed
       :artifact-ref text-commit
       :message "Agency artifact-ref is not a commit"})))

(defn deferred-completion-artifact-failure
  "Which failure, if any, a completed author-wait deferred-completion snapshot represents.

  The deferred-completion path adopted a snapshot as the authored turn on the strength of
  `(:artifact-ref snapshot)` being merely PRESENT, and then — because
  fresh-author? is false for a deferred-completion — took that value as the commit with no
  repository observation and no shape check. `unvalidated-artifact-failure`
  guards only the fresh-author side, so this was the one remaining place a
  non-commit ref became the reviewed commit.

  It is not hypothetical: canary-da9681ce's author job carried
  :artifact-ref \"/eoi_network_test.clj\" — a path, stale from an unrelated job
  in another repository — while its actual commit was f285e40 in futon2.
  Completing that job would have adopted the path.

  A missing ref and a non-commit ref are different faults and must not share the
  \"completed without an artifact\" message: one says the turn produced nothing,
  the other says extraction produced something that is not a commit. Returns nil
  when there is nothing to report."
  [deferred-completion-stage snapshot]
  (when (and snapshot
             (= :author-wait deferred-completion-stage)
             (= "done" (:state snapshot)))
    (let [artifact-ref (:artifact-ref snapshot)]
      (cond
        (nil? artifact-ref)
        {:failure-kind :deferred-completion-artifact-missing
         :message "Deferred author job completed without an artifact"}

        (not (commit-ish? artifact-ref))
        {:failure-kind :deferred-completion-artifact-ref-malformed
         :artifact-ref artifact-ref
         :message "Deferred author job artifact-ref is not a commit"}))))

(defn- run-opportunity-core!
  "Run one opportunity synchronously. Dependencies may be injected in opts for tests."
  [raw-opts]
  (let [phase-events (atom [])
        d-task-dispatch (atom nil)
        author-dispatch-route (atom nil)
        {:keys [trigger cohort? semantic-epoch author reviewer repair-reviewer
                window-days]
         :as opts} (assoc (config raw-opts)
                         :phase-events phase-events
                         :d-task-dispatch-state d-task-dispatch
                         :wm-phase-state (atom nil))
        _ (when (and (contains? raw-opts :measured-acquisition?)
                     (not (boolean? (:measured-acquisition? raw-opts))))
            (throw (ex-info "measured-acquisition? must be boolean"
                            {:failure-kind :measured-acquisition-option-invalid})))
        started (System/currentTimeMillis)
        opportunity-id (or (:opportunity-id opts)
                           (str (name trigger) "/" (Instant/now) "/" (UUID/randomUUID)))
        phase-context (atom {:opportunity-id opportunity-id :trigger trigger})
        _ (emit-phase! opts @phase-context {:phase :opportunity :transition :start})
        checkpoints (atom {})
        checkpoint-events (atom {})
        selected-entity-belief (atom nil)
        action-occurrence (atom nil)
        d-task-context (atom nil)
        pending-selection (atom nil)
        selection-persisted? (atom false)
        dispatched-turns (atom 0)
        standing-readback-state (atom nil)
        effective-configuration (atom (wm/effective-run-configuration opts))
        reviewer-of-record (participants/observe! opts)
        closing? (atom false)
        roster-result (try
                        {:value
                         (run-phase! opts @phase-context :agent-readiness
                                     #(agent-readiness! opts author)
                                     readiness-event)}
                        (catch Throwable e {:error e}))
        roster (get-in roster-result [:value :roster])
        code-state-result (try
                            {:value
                             (run-phase! opts @phase-context :code-state
                                         #((or (:code-state-fn opts)
                                               stack-code-state)))}
                            (catch Throwable e {:error e}))
        code-state (:value code-state-result)
        execution-cohort (:execution-cohort opts)
        _ (when (and (contains? opts :execution-cohort) (not (true? cohort?)))
            (throw (ex-info "Explicit execution cohort requires cohort recording"
                            {:reason :execution-cohort-recording-required})))
        execution-context (when execution-cohort
                            (cohort/execution-context execution-cohort))
        execution-authority (:authority execution-context)
        time-cell (term (cond-> {:opportunity-id opportunity-id
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
                          (seq (:history-exclusions (cohort/lineage-history execution-cohort)))
                          (assoc :history-exclusions (:history-exclusions (cohort/lineage-history execution-cohort)))
                          execution-authority
                          (assoc :execution-authority execution-authority))
                        {:kind :trigger-opportunity :id opportunity-id})
        cohort-source (:snapshot execution-context)
        start-event (when cohort?
                      (if cohort-source
                        (cohort/start-attempt! cohort-source (:data-root execution-cohort) time-cell)
                        (cohort/start-attempt! time-cell)))
        _ (when start-event
            (swap! checkpoint-events assoc :time-step start-event))
        attempt-id (or (:attempt/id start-event)
                       (str "canary-" (UUID/randomUUID)))
        attempt-evidence-dir
        (when cohort?
          (.getAbsolutePath
           (io/file (or (:data-root execution-cohort) cohort/default-data-root)
                    (name (:cohort/id start-event)) attempt-id "evidence")))
        job-text-records (atom [])
        ;; Beside, never inside, the attempt directory's checkpoint files:
        ;; closed-execution requires exactly the seven NNN-*.edn files there,
        ;; and evidence/ is enumerated into the close manifest.
        job-text-dir (if attempt-evidence-dir
                       (io/file (.getParentFile (io/file attempt-evidence-dir)) "retained")
                       (io/file (or (:run-record-dir opts) default-run-record-dir)
                                (str (:run-id opts)) attempt-id))
        ;; All author/reviewer/revision/repair ports share this attempt's
        ;; retention, including injected ports. No additional Agency reads.
        opts (job-texts/wrap-ports opts job-text-dir job-text-records
                                   (or (:dispatch-fn opts) dispatch!)
                                   (or (:poll-fn opts) poll-job!)
                                   (or (:read-job-fn opts) read-job!))
        measurement-state (atom nil)
        measurement-artifact (atom nil)
        measurement-end (atom nil)
        observe-end! (fn []
                       (when (and @measurement-state (nil? @measurement-end))
                         (reset! measurement-end
                                 (measurement/finish!
                                  @measurement-state
                                  {:artifact @measurement-artifact
                                   :wiring (select-keys (get-in @checkpoints [:construction :judgment])
                                                        [:wiring :fold-output :shape-validation :correspondence-validation])
                                   ;; Existing author/review prose and approval are not
                                   ;; revision-bound gate or replay receipts.
                                   :receipts []}
                                  #(str (Instant/now))))))
        prompt-opts (cond-> opts
                      attempt-evidence-dir
                      (assoc :attempt-evidence-dir attempt-evidence-dir))
        execution-identity (when execution-authority
                             (cohort/execution-identity execution-authority attempt-id))
        execution-provenance (when execution-authority
                               (cohort/execution-provenance execution-authority attempt-id))
        external-attempt-id (or (:id execution-identity) attempt-id)
        occurrence-origin (str (or (:repo code-state) "wm-runner") "::"
                               (:run-id opts))
        occurrence-for (fn [existing event-id failure-kind]
                         (repair-occurrence existing occurrence-origin
                                            (str event-id) failure-kind
                                            (str (Instant/ofEpochMilli started))))
        observation-for (fn [boundary]
                          {:observation/id (str (name boundary) "::"
                                                opportunity-id)
                           :observed-at (str (Instant/now))
                           :source occurrence-origin})
        _ (swap! phase-context assoc :attempt-id attempt-id
                 :external-attempt-id external-attempt-id
                 :execution-identity execution-identity)
        checkpoint! (fn [checkpoint cell]
                      (let [cell (if (#{:dispatch :build} checkpoint)
                                   (job-texts/checkpoint-cell cell @job-text-records)
                                   cell)]
                        ;; The in-memory checkpoint denotes the same admitted
                        ;; event as the durable cohort.  Never publish it before
                        ;; an enabled durable append has succeeded.
                        (when cohort?
                          (let [append (fn [c]
                                         (if cohort-source
                                           (cohort/append-checkpoint!
                                            cohort-source
                                            (:data-root execution-cohort)
                                            attempt-id checkpoint c)
                                           (cohort/append-checkpoint!
                                            attempt-id checkpoint c)))]
                            (append-checkpoint-or-refusal-sorry!
                             append
                             #(swap! checkpoint-events assoc checkpoint %)
                             checkpoint cell)))
                        (swap! checkpoints assoc checkpoint cell)
                        cell))
        persist-selection!
        (fn [trace-path]
          (when (and @pending-selection (not @selection-persisted?))
            (let [cell (update @pending-selection :judgment assoc
                               :belief-source
                               (cond-> {:run/id (:run-id opts)}
                                 trace-path (assoc :trace-path trace-path)))]
              (swap! checkpoints assoc :selection cell)
              (when cohort?
                (let [event (if cohort-source
                              (cohort/append-checkpoint!
                               cohort-source (:data-root execution-cohort)
                               attempt-id :selection cell)
                              (cohort/append-checkpoint!
                               attempt-id :selection cell))]
                  (swap! checkpoint-events assoc :selection event)))
              (reset! selection-persisted? true)))
          (get @checkpoints :selection))
        close-core! (fn [outcome data]
                 (reset! closing? true)
                 (persist-selection! nil)
                 (doseq [cp required-checkpoints
                         :when (not (contains? @checkpoints cp))]
                   (checkpoint! cp (sorry (keyword (str "not-reached-" (name cp)))
                                          {:outcome outcome})))
                 (let [admitted-verification?
                       (= :historical-verification-awaiting-validation outcome)
                       selection-judgment (get-in @checkpoints [:selection :judgment])
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
                       (when-not (or (= :grounded-change outcome)
                                     admitted-verification?)
                         (or existing-finding
                             ((or (:repair-system-record-fn opts)
                                  repair/record-system-failure!)
                              {:attempt-id external-attempt-id
                               :occurrence
                               (occurrence-for (:repair/occurrence data)
                                               external-attempt-id
                                               (or (:failure-kind data) outcome))
                               :observation (observation-for :close-core)
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
                               :opened-at (get-in time-cell
                                                  [:judgment :machine-state :started-at])
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
                       data (assoc data :effective-run-configuration @effective-configuration
                                   :repair-obligation
                                   (if admitted-verification? existing-finding finding))
                       parked-transition
                       (when (and finding (not admitted-verification?))
                         (park-r16-stop-line! opts external-attempt-id finding))
                       brief-item
                       (cond->
                        {:attempt-id external-attempt-id :opportunity-id opportunity-id
                                   :batch-id (:batch-id opts)
                                   :trigger trigger :selected-target (:target data)
                                   :outcome outcome :author author
                                   :reviewer @reviewer-of-record
                                   :commit (:commit data) :witness (:witness data)
                                   :lifecycle/discharge
                                   {:node :R16
                                   :stage (if admitted-verification?
                                            :verification-admitted :surfaced)
                                    :attempt-id external-attempt-id
                                    :outcome outcome
                                    :parked-transition parked-transition}
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
                                               admitted-verification?
                                               "Historical verification admitted; production successor required"
                                               :else "No grounded achievement")
                                    :build (get-in @checkpoints [:build :judgment])
                                    :adjudication
                                    (get-in @checkpoints [:adjudication :judgment])}
                                   :failure
                                   (when-not (or (= :grounded-change outcome)
                                                 admitted-verification?)
                                     (cond-> {:kind (or (:failure-kind data) outcome)
                                              :stage (or (:failure-stage data)
                                                         (last-error-phase @phase-events))
                                              :error (:error data)
                                              :backtrace (:backtrace finding)
                                              :repair-id (:repair/id finding)
                                              :discharge-contract
                                              (:discharge-contract finding)}
                                       (:build-retries data)
                                       (assoc :build-retries (:build-retries data))))
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
                         (:selected-cascade selection-judgment)
                         (assoc :selected-cascade (:selected-cascade selection-judgment))

                         (seq (:reviews data))
                         (assoc :reviews (:reviews data))

                         (and (= :grounded-change outcome)
                              (:feature-card data))
                         (assoc :feature-card (:feature-card data)))
                       brief-ref ((or (:queue-fn opts) brief/queue-item!) brief-item)
                       delivery-qa-ref
                       (if (str/blank? (str (:commit data)))
                         {:status :not-a-delivery
                          :reason :no-authored-commit}
                         (run-phase!
                          opts @phase-context :delivery-qa
                          #(try
                             ((:delivery-qa-fn opts) opts brief-item)
                             (catch Throwable e
                               (throw
                                (ex-info
                                 "Delivery closed without Field Desk QA notes"
                                 {:failure-kind :delivery-qa-gate-failed
                                  :failure-stage :delivery-qa
                                  :attempt-id attempt-id
                                  :commit (:commit data)}
                                 e))))
                          (fn [ref]
                            {:delivery-qa-ref ref
                             :field-desk-endpoint
                             (delivery-qa/endpoint opts)})))
                       trace-path (get-in @checkpoints
                                          [:construction :judgment :trace-path])
                       close-state (let [{:keys [entity-id belief run-id]}
                                         @selected-entity-belief]
                                     (entity-state-at-close
                                      entity-id belief
                                      (cond-> {:run/id run-id}
                                        trace-path (assoc :trace-path trace-path))
                                      (str (Instant/now))))
                       outcome-entity (outcome-entity-at-close
                                       @selected-entity-belief close-state)
                       d-task-result
                       (when @action-occurrence
                         (d-task/complete!
                          (or (:d-task-evidence-root opts) d-task/default-root)
                          @d-task-dispatch @d-task-context
                          (assoc data :dispatch-route @author-dispatch-route
                                      :artifact-binding (or (:artifact-binding data)
                                                            (get-in @checkpoints [:build :judgment :validation :artifact-binding]))
                                      :files (get-in @checkpoints [:build :judgment :artifacts])
                                      :historical? (= :historical-verification-awaiting-validation outcome))
                          #(read-job! opts %)))
                       token-comparison
                       (when (and cohort? @action-occurrence)
                         (retain-token-outcome!
                          (or (:data-root execution-cohort) cohort/default-data-root)
                          (:cohort/id start-event) attempt-id
                          (get-in @checkpoints [:selection :judgment :token-outcome-prediction])
                          d-task-result (:commit data)
                          {:occurrence @action-occurrence :route @author-dispatch-route
                           :selection-recorded-at (get-in @checkpoint-events [:selection :recorded-at])
                           :expected @d-task-context :read-job #(read-job! opts %)
                           :ledger-root (or (:learning-trial-ledger-root opts) learning-ledger/default-root)}))
                       route-account
                       (route-attestation/retain!
                        (if attempt-evidence-dir
                          (.getParentFile (io/file attempt-evidence-dir))
                          (io/file (or (:run-record-dir opts) default-run-record-dir)
                                   (str (:run-id opts)) attempt-id))
                        (str (if-let [c (:cohort/id start-event)] (name c) (:run-id opts)) "/" attempt-id "/retained/route-attestation.edn")
                        (route-attestation/receipt
                         {:declarations (:route-attestation opts)
                          :events @checkpoint-events
                          :target (:target selected-action)
                          :token-comparison (:receipt token-comparison)}))
                       kernel-example-result
                       (when (and cohort? @action-occurrence)
                         (retain-kernel-example!
                          (or (:data-root execution-cohort) cohort/default-data-root)
                          (:cohort/id start-event) attempt-id
                          {:prediction (get-in @checkpoints [:selection :judgment :token-outcome-prediction])
                           :occurrence @action-occurrence :artifact-sha (:commit data) :outcome outcome}
                          d-task-result @d-task-context #(read-job! opts %)))
                       ;; PROOF-wm-works ⟨1⟩4 (final handoff): evaluate the
                       ;; accepted-increment predicate for THIS occurrence
                       ;; before the close is constructed, reading the
                       ;; producers' own results for its conjuncts: (a) the
                       ;; artifact binding the build checkpoint retained
                       ;; (task-execution-evidence's verdict fields), (b) the
                       ;; token comparison's measured rows (the measurement
                       ;; producer's output), (c) the selected cascade's
                       ;; declared acceptance locator. The typed result is
                       ;; recorded ON the close; a false predicate never
                       ;; refuses anything -- the close proceeds as the
                       ;; failure it is.
                       accepted-increment-result
                       ;; The predicate is evidence, never a gate: any error
                       ;; evaluating it is recorded as a typed :refused
                       ;; result, and the close proceeds.
                       (accepted-increment/evaluate-close
                        {:binding (get-in @checkpoints [:build :judgment :validation :artifact-binding])
                         ;; ⟨1⟩6 ruling: conjunct (b) reads the SELECTED
                         ;; candidate's OWN declared produced tokens (the
                         ;; first action's :produces), measured at the
                         ;; after-revision by the measurement producer (the
                         ;; d-task evidence's after-token rows cover the whole
                         ;; declared universe, including the candidate's own
                         ;; tokens). The horizon's predicted chain stays in
                         ;; the prediction record — it is NOT the measure of
                         ;; what this attempt produced. The prediction-filtered
                         ;; comparison rows above remain the (c)-facing and
                         ;; surprise-facing record, untouched.
                         :token-rows
                         (let [produced (set (get-in selection-judgment
                                                     [:controller-decision :action :precedence 0 :produces]))
                               ;; the d-task source record's after-token rows
                               ;; cover the whole declared universe (the
                               ;; candidate's own tokens included); read
                               ;; through the same source port the comparison
                               ;; used, digested and re-read
                               src (get-in d-task-result [:source :path])
                               record (when src
                                        (try
                                          (let [bytes (Files/readAllBytes (.toPath (io/file src)))]
                                            (when (= (get-in d-task-result [:source :sha256])
                                                     (sha256-bytes bytes))
                                              (edn/read-string (String. bytes "UTF-8"))))
                                          (catch Exception _ nil)))
                               evidence (:after-token-evidence record)]
                           (vec (for [row (vec evidence)
                                      :when (contains? produced (second (:token row)))]
                                  {:token (:token row)
                                   ;; the d-task row itself carries
                                   ;; :after-locator and :result; the
                                   ;; predicate reads
                                   ;; [:measurement :after-locator]
                                   :measurement row})))
                         ;; ⟨1⟩6 part 1: the acceptance declaration travels
                         ;; from the decision to the close. First the slot the
                         ;; class decision may populate; when it does not (the
                         ;; class path populates no action-level acceptance),
                         ;; the SELECTED candidate's own declared acceptance
                         ;; from its cascade source — carried with provenance
                         ;; (source file, target). A target whose source
                         ;; declares none yields nil, and the predicate says
                         ;; :no-acceptance-declared, never invented language.
                         :acceptance (or (get-in selection-judgment
                                                [:controller-decision :action :accepted-increment :acceptance])
                                         (cascade-sources/acceptance-of
                                          (:target (get-in selection-judgment [:controller-decision :action]))))
                         :after-revision (:commit data)})
                       close-judgment-base
                       (merge {:outcome outcome
                               :grounded? (= :grounded-change outcome)
                               :artifact-only? (= :artifact-only outcome)
                               :occurrence @action-occurrence
                               :outcome-entity outcome-entity
                               :entity-state-at-close close-state
                               :surprise-ids (mapv :surprise/id (:surprises token-comparison))
                               :token-outcome-comparison (:receipt token-comparison)
                               :learning-trial-receipt (get-in token-comparison [:receipt :learning-trial-receipt])
                               :accepted-increment accepted-increment-result
                               :route-attestation (:receipt route-account)
                               :route-attestation-ref (:reference route-account)
                               :kernel-example (:receipt kernel-example-result)
                               :morning-brief-ref brief-ref
                               :delivery-qa-ref delivery-qa-ref
                               :job-texts @job-text-records
                               :duration-ms (- (System/currentTimeMillis) started)
                               :resource-use {:agent-turns @dispatched-turns}}
                              (select-keys data
                                           [:witness :effective-run-configuration
                                            :standing-readback :failure-kind]))
                       run-ending-result
                       (when (and cohort? @action-occurrence)
                         (retain-run-ending!
                          (or (:data-root execution-cohort) cohort/default-data-root)
                          (:cohort/id start-event) attempt-id
                          {:close close-judgment-base
                           :occurrence @action-occurrence
                           :route-attestation (:receipt route-account)
                           :focus-receipt (get-in selection-judgment
                                                  [:controller-decision :selection-certificate
                                                   :focus-receipt])}))
                       manifest (when (and cohort? @action-occurrence)
                                  (checkpoint-evidence-manifest
                                   @checkpoint-events
                                   (or (:data-root execution-cohort)
                                       cohort/default-data-root)
                                   (:cohort/id start-event)
                                   attempt-id
                                   (or (:target data)
                                       (:selected-mission selection-judgment))
                                   {:occurrence @action-occurrence
                                    :semantic-epoch semantic-epoch
                                    :token-outcome-entry (:entry token-comparison)
                                    :surprise-entry (:surprise-entry token-comparison)
                                    :route-attestation-entry (:entry route-account)
                                    :kernel-example-entry (:entry kernel-example-result)
                                    :run-ending-entry (:entry run-ending-result)}))
                       admitted-ids (mapv :evidence/id (:entries manifest))
                       closed (cond->
                               (term (assoc close-judgment-base
                                            :run-ending-classification
                                            (:receipt run-ending-result))
                                     {:kind :full-loop-outcome :attempt-id attempt-id})
                                (and cohort? @action-occurrence)
                                (assoc :retention-inputs
                                       {:occurrence @action-occurrence
                                        :state {:status :absent
                                                :reason :independent-observation-unavailable}
                                        :model {:status :absent
                                                :reason :declared-model-identity-unthreaded}
                                        :admitted-evidence admitted-ids}
                                       :evidence-manifest manifest))
                       run-route (packet-run-route selection-judgment
                                                   (get-in @checkpoints
                                                           [:selection :ground])
                                                   outcome
                                                   trace-path
                                                   {:selected-action selected-action
                                                    :requested-pin
                                                    (:run4/requested-pin opts)})
                       result-base (cond-> {:attempt-id attempt-id :opportunity-id opportunity-id
                               :outcome outcome :checkpoints @checkpoints
                               :job-texts @job-text-records
                               :d-task-enactment d-task-result
                               :surprise-ids (mapv :surprise/id (:surprises token-comparison))
                               :token-outcome-comparison (:receipt token-comparison)
                                            :learning-trial-receipt (get-in token-comparison [:receipt :learning-trial-receipt])
                               :accepted-increment accepted-increment-result
                               :route-attestation (:receipt route-account)
                               :route-attestation-ref (:reference route-account)
                               :kernel-example (:receipt kernel-example-result)
                               :run-ending-classification (:receipt run-ending-result)
                               :morning-brief-ref brief-ref
                               :delivery-qa-ref delivery-qa-ref
                               :wm/route run-route
                               :trace-path trace-path
                               :data data}
                                execution-identity
                                (assoc :execution-identity execution-identity
                                       :execution-provenance execution-provenance))
                       closed-event
                       (when cohort?
                         (if cohort-source
                           (cohort/close-attempt! cohort-source
                                                  (:data-root execution-cohort)
                                                  attempt-id closed)
                           (cohort/close-attempt! attempt-id closed)))
                       ;; PROOF-wm-works ⟨1⟩4 (final handoff): the B update
                       ;; is applied AFTER the close is written and ONLY
                       ;; when the predicate result recorded on that close is
                       ;; {:accepted? true} — a close that failed to construct
                       ;; (closed-event nil) or was refused writes no update.
                       ;; Occurrence-keyed exactly once; the ledger's own
                       ;; identity deduplication enforces the second run —
                       ;; this call never bypasses it (b-update reports
                       ;; :already-recorded rather than appending).
                       b-update-result
                       (when (and closed-event
                                  (true? (:accepted? accepted-increment-result)))
                         (try
                           (learning-ledger/b-update
                            {:family (get-in selection-judgment
                                             [:controller-decision :action :precedence 0 :id])
                             :occurrence-identity (get-in accepted-increment-result
                                                           [:evidence :binding :commit])
                             :accepted-verdict accepted-increment-result
                             :ledger-root (or (:learning-trial-ledger-root opts)
                                              learning-ledger/default-root)})
                           (catch Exception e
                             {:status :refused
                              :reason (:learning-ledger/refusal (ex-data e))
                              :message (.getMessage e)})))
                       discharge-result
                       (repair-discharge/finalize-run!
                        {:root (or (:repair-root opts) repair/default-root)
                         :repo (or (:discharge-receipt-repo opts) "/home/joe/code/futon2")
                         :action selected-action
                         :interpretation (:interpretation-receipts selected-action)
                         :b-update b-update-result
                         :closed-event closed-event
                         :close-path (when closed-event
                                       (str (io/file (or (:data-root execution-cohort) cohort/default-data-root)
                                                     (name (:cohort/id closed-event)) attempt-id "007-closed.edn")))
                         :close {:attempt/id (or (:id execution-identity)
                                                 (when closed-event
                                                   (str (name (:cohort/id closed-event)) "--" attempt-id)))
                                 :run/id (:run-id opts) :closed-at (:recorded-at closed-event)
                                 :grounded? (= :grounded-change outcome)}
                         :artifact {:repo (get-in data [:artifact-binding :repo]) :commit (:commit data)}
                         :artifact-binding (:artifact-binding data)
                         :files (get-in @checkpoints [:build :judgment :artifacts])
                         :author author :reviewer @reviewer-of-record
                         :review-job (:review-job data)
                         :read-job #((or (:read-job-fn opts) read-job!) opts %)
                         :evaluators (:repair-evaluators opts) :registry repair-evaluators/registry
                         :producer {:run/id (:run-id opts) :attempt/id external-attempt-id
                                    :runner-source-sha256 (get-in opts [:loaded-code-identity :runner/sha256])}})
                       retained (get-in closed-event [:payload :close-retention])
                       retained-manifest
                       (get-in closed-event [:payload :close-evidence-manifest])
                       result (cond-> (assoc result-base :repair/discharge discharge-result
                                            :habit-reinforcement
                                            (habit-reinforcement/close!
                                             (or (:cascade-habit-path opts) cascade-habit/default-path)
                                             (:controller-decision selection-judgment)
                                             outcome (:token-outcome-comparison result-base)))
                                retained (assoc :close-retention retained)
                                retained-manifest
                                (assoc :close-evidence-manifest retained-manifest))]
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
                   result))
        close! (fn [outcome data]
                 (try
                   (observe-end!)
                   (close-core! outcome data)
                   (catch Throwable e
                     ;; Cohort-53 attempt-001 is retained as the historical
                     ;; counterexample: a close-time evidence refusal escaped
                     ;; and left no 007.  This boundary must always attempt the
                     ;; durable typed close before returning to the caller.
                     (let [failure-data (if (instance? clojure.lang.ExceptionInfo e)
                                          (ex-data e) {})
                           refusal-kind (or (:interpretation-evidence/refusal failure-data)
                                            (:limb-evidence/refusal failure-data)
                                            (:evidence-manifest/refusal failure-data)
                                            (:close-retention/refusal failure-data)
                                            (:failure-kind failure-data)
                                            :close-exception)
                           exception-class (.getName (class e))
                           finding ((or (:repair-system-record-fn opts)
                                        repair/record-system-failure!)
                                    {:attempt-id external-attempt-id
                                     :occurrence
                                     (occurrence-for
                                      (:repair/occurrence failure-data)
                                      external-attempt-id refusal-kind)
                                     :observation
                                     (observation-for :close-fallback)
                                     :repair-class :machine-failure
                                     :machine-repo (:repo code-state)
                                     :target (get-in @checkpoints
                                                     [:selection :judgment
                                                      :selected-mission])
                                     :failure-stage :close
                                     :outcome :build-failed
                                     :failure-kind refusal-kind
                                     :error (.getMessage e)
                                     :failure-data failure-data
                                     :opened-at (get-in time-cell
                                                        [:judgment :machine-state
                                                         :started-at])
                                     :discharge-contract
                                     (discharge-contract :machine-failure)})
                           route-file (when cohort?
                                        (io/file (or (:data-root execution-cohort)
                                                     cohort/default-data-root)
                                                 (name (:cohort/id start-event))
                                                 attempt-id "retained"
                                                 "route-attestation.edn"))
                           route (when (and route-file (.isFile route-file))
                                   (cohort/read-edn route-file))
                           sorry-data {:effective-run-configuration @effective-configuration
                                       :outcome :build-failed
                                       :grounded? false
                                       :artifact-only? false
                                       :occurrence @action-occurrence
                                       :route-attestation route
                                       :failure-kind refusal-kind
                                       :failure-stage :close
                                       :error (.getMessage e)
                                       :exception-class exception-class
                                       :refusal-data failure-data
                                       :repair-obligation finding
                                       ;; the production close contract
                                       ;; requires these; the thin fixture
                                       ;; prereg hid their absence and
                                       ;; cohort-54 attempt-001 orphaned on
                                       ;; "invalid close outcome"
                                       :duration-ms (- (System/currentTimeMillis)
                                                       started)
                                       :resource-use {:agent-turns
                                                      @dispatched-turns}
                                       :sorry {:kind refusal-kind
                                               :refusal-data failure-data}}
                           failure-evidence
                           (when (and cohort? @action-occurrence)
                             (failure-close-evidence!
                              @checkpoint-events
                              (or (:data-root execution-cohort)
                                  cohort/default-data-root)
                              (:cohort/id start-event) attempt-id sorry-data))
                           admitted-ids (mapv :evidence/id
                                              (get-in failure-evidence
                                                      [:manifest :entries]))
                           closed (cond->
                                   (term (assoc sorry-data
                                                :job-texts @job-text-records
                                                :run-ending-classification
                                                (:run-ending failure-evidence))
                                         {:kind :full-loop-close-failure
                                          :attempt-id attempt-id})
                                    failure-evidence
                                    (assoc :retention-inputs
                                           {:occurrence @action-occurrence
                                            :state {:status :absent
                                                    :reason :independent-observation-unavailable}
                                            :model {:status :absent
                                                    :reason :declared-model-identity-unthreaded}
                                            :admitted-evidence admitted-ids}
                                           :evidence-manifest
                                           (:manifest failure-evidence)))
                           closed-event (when cohort?
                                          (if cohort-source
                                            (cohort/close-attempt!
                                             cohort-source
                                             (:data-root execution-cohort)
                                             attempt-id closed)
                                            (cohort/close-attempt!
                                             attempt-id closed)))]
                       {:attempt-id attempt-id
                        :opportunity-id opportunity-id
                        :outcome :build-failed
                        :checkpoints @checkpoints
                        :data sorry-data
                        :closed-event closed-event}))))]
    (try
      (when-let [e (:error roster-result)]
        (throw (ex-info "Agent readiness observation failed"
                        {:outcome :agent-unavailable
                         :failure-kind :agent-readiness-failed
                         :failure-stage :agent-readiness
                         :failure-detail :unreachable}
                        e)))
      (when-let [e (:error code-state-result)]
        (throw (ex-info "Stack code-state observation failed"
                        {:outcome :incomplete
                         :failure-kind :code-state-failed
                         :failure-stage :code-state}
                        e)))
      (when-not (available? roster author)
        (throw (ex-info "Configured author is unavailable"
                        {:outcome :agent-unavailable
                         :failure-kind :agent-unavailable
                         :failure-stage :agent-readiness
                         :failure-detail (agent-failure-detail roster author)
                         :author author})))
      (run-phase! opts @phase-context :substrate-preflight
                  #(substrate-readiness! opts)
                  readiness-event)
      (run-phase! opts @phase-context :preference-refresh
                  #(try ((or (:refresh-fn opts) cv/maybe-refresh!))
                        (catch Throwable _ nil)))
      (let [open-stop-lines
            (run-phase! opts @phase-context :stop-line-memory
                        #((or (:repair-open-fn opts) repair/open-obligations)))
            validation-lines
            (->> open-stop-lines
                 (filter #(or (= :awaiting-validation (:repair/status %))
                              (= :environmental-hold (:repair/class %))))
                 (take 1)
                 vec)
            historical-validation-lines
            (filterv #(and (= :awaiting-validation (:repair/status %))
                           (map? (:repair/verification %)))
                     validation-lines)
            selection-judge (or (:judge-fn opts)
                                (fn [days]
                                  (wm/generate-war-machine
                                   days
                                   (assoc (select-keys opts [:accumulate-strategic-habit?
                                                            :run-id :loaded-code-identity :cascade-habit-path])
                                          :include-advisory-lanes? false
                                          :defer-render? true))))
            judgement0-base
            (run-phase!
             opts @phase-context :selection
             #(let [_ (swap! effective-configuration assoc :evaluation :started)
                    generated (selection-judge window-days)
                    judgement (:judgement generated)
                    _ (reset! effective-configuration
                              (or (:effective-run-configuration judgement)
                                  (assoc @effective-configuration :evaluation :not-retained)))
                    ;; RULING-selection-precedence-2026-09-19.md: ordinary clicks
                    ;; always select; repair memory is evidence, never a divert.
                    judgement ((or (:judgement-transform-fn opts) identity) judgement)]
                (when-let [state (:scan-report/state opts)]
                  ;; Capture exactly the judgement used below, never rescan.
                  (reset! state (assoc (or (:render-data generated) (:data generated))
                                       :judgement judgement)))
                judgement))
            judgement0 judgement0-base
            mode-flags ((or (:mode-flags-fn opts) wm/arena-mode-flags))
            ordinary-entry (selected-entry judgement0)
            entry ordinary-entry
            historical-action? (= :revalidate-historical-repair
                                  (get-in entry [:action :type]))
            repair-action? (contains? #{:repair-machine-failure
                                        :revalidate-historical-repair}
                                      (get-in entry [:action :type]))
            ;; Repair execution binds the selected obligation, never the first
            ;; unrelated member of the observed memory.
            stop-line (when repair-action?
                        (first (filter #(= (:repair/id %)
                                           (get-in entry [:action :target]))
                                       open-stop-lines)))
            supersede-deferred-completion!
            (fn [repair-class failure-kind failure-stage error failure-data]
              (let [successor
                    ((or (:repair-system-record-fn opts)
                         repair/record-system-failure!)
                     {:attempt-id attempt-id
                      :occurrence
                      (occurrence-for (:repair/occurrence failure-data)
                                      (or (get-in failure-data
                                                  [:author-job :job-id])
                                          external-attempt-id)
                                      failure-kind)
                      :observation (observation-for :deferred-completion-supersession)
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
            historical-admission
            (when (and historical-action? stop-line
                       (:historical-verification-candidate-fn opts))
              ((:historical-verification-candidate-fn opts) stop-line))
            stop-lines (if (and repair-action? stop-line) [stop-line] [])
            reviewer (participants/select-reviewer! reviewer-of-record
                                                   repair-action? reviewer repair-reviewer)
            ;; :operator-actions RETIRED with the flat decision (SPEC
            ;; flat-removal H4, 2026-09-17): there are no flat candidates,
            ;; so no operator gates are queued from the judgement.
            operator-action-refs []
            judgement (cond-> (assoc judgement0
                             :run/id (:run-id opts)
                             :operator-action-refs operator-action-refs
                             :wm-version
                             ((or (:version-stamp-fn opts) trace/wm-version-stamp)
                              (assoc mode-flags
                                     :trigger trigger
                                     :real-actuation? true
                                     :author author
                                     :reviewer reviewer
                                     :repair-reviewer repair-reviewer)))
                        (and historical-action? (:run4/requested-pin opts))
                        (assoc :run4/requested-pin (:run4/requested-pin opts)
                               :run4/enacted-action (:action entry))
                        cohort?
                        (assoc :cohort-attempt
                               {:cohort/id (or (:cohort/id start-event)
                                               (:cohort-id execution-cohort))
                                :attempt/id attempt-id}))
            target (some-> entry selected-target)
            _ (reset! selected-entity-belief
                      {:selection-reached? true
                       :selection-made? (boolean entry)
                       :entity-id target
                       :belief (:belief judgement)
                       :run-id (:run/id judgement)})
            ranked-for-review (ranked-candidates judgement)
            discrimination (selection-discrimination ranked-for-review)
            selection-cell (if entry
                             (cond->
                              (term {:selected-mission (str target)
                                     :selected-action (:action entry)
                                     :token-outcome-prediction
                                     (token-outcome/freeze-prediction (:decision judgement))
                                     :controller-decision (:decision judgement)
                                     :ranked-candidates ranked-for-review
                                     :selection-reasons
                                     (assoc
                                      (select-keys (:decision judgement)
                                                   [:rank :controller-score
                                                    :selection-boundary :beta])
                                      :discrimination discrimination)
                                     :trace-persistence (if repair-action?
                                                          :repair-action-not-traced
                                                          :after-construction)}
                                    (cond-> {:kind :wm-judgement
                                             :decision (:decision judgement)}
                                     (and historical-action? (:run4/requested-pin opts))
                                     (assoc :run4/requested-pin (:run4/requested-pin opts)
                                            :run4/enacted-action (:action entry))))
                               (selected-cascade entry)
                               (assoc-in [:judgment :selected-cascade] (selected-cascade entry))
                               (:readiness/selection-transient judgement0)
                               (assoc-in [:judgment
                                          :readiness/selection-transient]
                                         true))
                             (sorry :no-selection {:decision (:decision judgement)}))]
        (let [selection-cell (-> selection-cell
                                 (assoc-in [:judgment :effective-run-configuration]
                                           @effective-configuration)
                                 (assoc-in [:judgment :open-stop-lines]
                                           {:count (count open-stop-lines)
                                            :ids (mapv :repair/id open-stop-lines)}))]
          (reset! pending-selection selection-cell)
          (swap! checkpoints assoc :selection selection-cell))
        (when-not entry
          (throw (ex-info "War Machine abstained or selected no addressable action"
                          {:outcome (if (= :abstained
                                           (get-in judgement [:decision :status]))
                                      :abstained :no-selection)})))
        (let [reviewer-roster
              (if (restored? roster reviewer)
                (:roster
                 (run-phase! opts @phase-context :agent-readiness
                             #(wake-restored-agent! opts roster reviewer)
                             readiness-event))
                roster)]
          (when (or (= author reviewer)
                    (not (available? reviewer-roster reviewer)))
            (throw (ex-info "Selected reviewer is unavailable or is the author"
                            {:outcome :agent-unavailable
                             :failure-kind :agent-unavailable
                             :failure-stage :agent-readiness
                             :failure-detail (if (= author reviewer)
                                               :busy
                                               (agent-failure-detail reviewer-roster
                                                                     reviewer))
                             :author author :reviewer reviewer
                             :review-role (if repair-action?
                                            :ground-control :ordinary)}))))
        (when (and discrimination (not (:passes? discrimination)))
          (throw (ex-info "Leading feasible policies have no posterior discrimination"
                          {:outcome :policy-nondiscrimination
                           :failure-kind :policy-nondiscrimination
                           :failure-stage :selection
                           :target target
                           :selected-entry entry
                           :discrimination discrimination})))
        (mint-action-occurrence-once!
         action-occurrence
         {:run-id (:run-id opts)
          :cohort-id (str (or (:cohort/id start-event)
                              (:cohort-id execution-cohort)
                              "non-cohort"))
          :attempt-id attempt-id
          :selected-action (:action entry)
          :now #(Instant/now)
          :uuid-fn #(UUID/randomUUID)})
        (reset! d-task-context
                (d-task/context (:decision judgement) @action-occurrence
                                (some-> (:declaration-reads/state opts) deref)))
        (let [interpretation
              (when (= :receipt (:interpretation-mode opts))
                (when-not attempt-evidence-dir
                  (throw (ex-info "Interpretation requires an existing cohort evidence directory"
                                  {:outcome :incomplete :failure-kind :interpretation/cohort-required})))
                (run-phase!
                 opts @phase-context :interpretation
                 #(interpretation-job/run!
                   opts (:action entry)
                   {:occurrence @action-occurrence :semantic-epoch semantic-epoch
                    :data-root (.getCanonicalPath
                                (io/file (or (:data-root execution-cohort) cohort/default-data-root)))
                    :start-event-sha256
                    (interpretation-evidence/sha256
                     (Files/readAllBytes (.toPath (io/file (.getParentFile (io/file attempt-evidence-dir))
                                                         "001-time-step.edn"))))
                    :interpreter-job {:status :none :reason :not-dispatched}
                    :author (or (:interpreter opts) author) :schema-version 1}
                   attempt-evidence-dir
                   {:transport-failure-kind transport-failure-kind
                    :ready! (fn [actor] ((or (:interpretation-readiness-fn opts) agent-readiness!) opts actor))
                    :dispatch! (fn [actor prompt]
                                 ((or (:dispatch-fn opts) dispatch!) opts actor "wm-full-loop" target prompt))
                    :poll! (fn [job-id] ((or (:poll-fn opts) poll-job!) opts job-id))
                    :charge! (fn [] (swap! dispatched-turns inc))
                    :construct! (fn [record read-bytes] (receipt-construction/construct! record read-bytes opts))})))
              {:keys [mission construction]}
              (run-phase! opts @phase-context :construction
                          #(hash-map
                            :mission (if-let [mission-fn (:mission-fn opts)]
                                       (mission-fn target)
                                       (mission-for-decision entry target (:repair-root opts)))
                            :construction (if interpretation
                                            (:construction interpretation)
                                            ((or (:construct-fn opts) construct-for-decision) entry))))
              wiring-result (when construction
                              (construction-wiring-result
                               construction
                               (:construction-wiring-fn opts)
                               false))
              ;; Present the same validated observations as the checkpoint;
              ;; prompts must not re-observe mutable repository state.
              construction (when construction
                             (assoc construction :wiring (:wiring wiring-result)
                                                 :fold-output (:fold-output wiring-result)))]
          (when-not construction
            (throw (ex-info "No construction for selected decision"
                            {:outcome :construction-failed
                             :failure-stage :construction
                             :target target
                             :selected-entry
                             (select-keys entry [:action :controller-score :G-efe])})))
          (when (= :invalid (:status wiring-result))
            (throw (ex-info "Construction fold wiring is missing or malformed"
                            {:outcome :construction-failed
                             :failure-kind (:failure-kind wiring-result)
                             :fold-findings (:findings wiring-result)
                             :failure-stage :construction
                             :target target})))
          (when (= :refused (:status wiring-result))
            (throw (ex-info "Construction fold wiring explicitly refused"
                            {:outcome :construction-failed
                             :failure-kind :fold-wiring-refused
                             :failure-stage :construction
                             :target target
                             :wiring-refusal (:fold-output wiring-result)})))
          ;; A constructed selection enters the canonical trace as an event.
          ;; Cascade habit reinforcement requires observed token outcomes at close;
          ;; neither selection nor successful construction reinforces it.
          (let [trace-path (when-not repair-action?
                             ((or (:trace-fn opts) trace/write-trace!)
                              (assoc judgement :d-task-context @d-task-context :trace/reason
                                     {:kind :routing-rule
                                      :rule :constructed-selection-persisted
                                      :question "Does the constructed selection require operator review?"})))
                construction-cell
                (term (cond-> {:mission (str target)
                              :cascade (select-keys construction
                                                    [:psi :cascade-score :semilattice :cascade-structure
                                                     :construction-kind
                                                     :selected-action
                                                     :capability-contract
                                                     :actuation-contract
                                                     :repair-contract])
                              :sorries (let [holes (get-in wiring-result [:fold-output :policy-holes])]
                                         (when (sequential? holes) (vec holes)))
                              :wiring (:wiring wiring-result)
                              :fold-output (:fold-output wiring-result)
                              :shape-validation (:shape-validation wiring-result)
                              :correspondence-validation (:correspondence-validation wiring-result)
                              :selection-enaction
                              (selection-enaction-record
                               (:action entry)
                               (:action entry)
                               {:source :runner-selection})
                              :patterns (vec (:shown construction))
                              :deposit nil
                              :trace-path trace-path}
                               (selected-cascade entry)
                               (assoc :selected-cascade (selected-cascade entry))
                               interpretation
                               (assoc :receipted-construction (:receipted-construction construction)
                                      :interpretation-receipt (:receipt interpretation)
                                      :interpretation-timing (:timing interpretation))

                               (and (nil? interpretation) (:interpretation-receipt construction))
                               (assoc :interpretation-receipt (:interpretation-receipt construction))

                               (= :refused (:status wiring-result))
                               (assoc :wiring-refusal (:fold-output wiring-result)))
                             {:kind :decision-pinned-construction
                              :selected-action (:action entry)})]
          (persist-selection! trace-path)
          (checkpoint! :construction construction-cell)
          (when (:interpretation-receipt (:judgment construction-cell))
            (reset! measurement-state
                    (measurement/begin! attempt-evidence-dir (:judgment construction-cell)
                                        #(str (Instant/now)) author)))
          (when historical-action?
            (when-not (historical-revalidation-entry
                       stop-line historical-admission
                       {:author author :repair-reviewer repair-reviewer})
              (throw (ex-info "Selected historical admission does not match obligation or actors"
                              {:outcome :historical-verification-refused
                               :failure-kind :historical-verification-admission-invalid
                               :failure-stage :construction
                               :repair-obligation stop-line})))
            (when-not (:historical-verification-execute-fn opts)
              (throw (ex-info "Historical verification execution port missing"
                              {:outcome :historical-verification-refused
                               :failure-kind :historical-verification-port-missing
                               :failure-stage :construction
                               :repair-obligation stop-line})))
            (let [execution-identity (or execution-identity
                                         {:kind :runner-execution :id attempt-id})
                  transition ((:historical-verification-execute-fn opts)
                              {:execution-identity execution-identity
                               :obligation stop-line
                               :candidate historical-admission})]
              (when-not (and (= :wm/historical-repair-admission-v1 (:schema transition))
                             (= (:repair/id stop-line) (:repair/id transition))
                             (= :awaiting-validation (:repair/status transition))
                             (= execution-identity (:verification-attempt transition))
                             (= (:verification-id historical-admission)
                                (:verification-id transition))
                             (= (:verification-artifact historical-admission)
                                (:verification-source transition))
                             (let [artifact (:verification-artifact transition)]
                               (and (= #{:path :sha256} (set (keys artifact)))
                                    (string? (:path artifact))
                                    (not (str/blank? (:path artifact)))
                                    (string? (:sha256 artifact))
                                    (re-matches #"[0-9a-f]{64}" (:sha256 artifact)))))
                (throw (ex-info "Historical verification transition malformed"
                                {:outcome :historical-verification-refused
                                 :failure-kind :historical-verification-transition-invalid
                                 :failure-stage :construction
                                 :repair-obligation stop-line})))
              (checkpoint! :dispatch
                           (sorry :historical-verification-no-author-dispatch
                                  {:verification-attempt execution-identity}))
              (checkpoint! :build
                           (sorry :historical-code-identity-unchanged
                                  {:verification-id (:verification-id transition)}))
              (checkpoint! :adjudication
                           (term {:before nil :after nil
                                  :witness {:resolved? false :dial-moved? false}
                                  :build-match {:commit nil :review-approved? false}
                                  :dial {:moved? false :implementation-id nil}
                                  :verification-id (:verification-id transition)
                                  :verification-attempt execution-identity
                                  :repair-id (:repair/id transition)
                                  :repair-status :awaiting-validation
                                  :repair-resolved? false
                                  :production-successor-required? true}
                                 {:kind :historical-verification-admission
                                  :evidence (:verification-artifact transition)}))
              (throw (ex-info "Historical repair verification admitted for validation"
                              {:historical-verification-completion-token
                               historical-verification-completion-token
                               :outcome :historical-verification-awaiting-validation
                               :repair-obligation transition
                               :verification-attempt execution-identity}))))
          (let [snapshot (when stop-line
                           (deferred-completion-snapshot opts stop-line))
                deferred-completion-stage (:failure-stage stop-line)
                _ (when (and snapshot (not= "done" (:state snapshot)))
                    (if (contains? terminal-states (:state snapshot))
                      (let [error (str "Deferred completion job terminated as "
                                       (:state snapshot))
                            successor
                            (supersede-deferred-completion!
                             :machine-failure :deferred-completion-job-terminal
                             deferred-completion-stage error
                             {:job-id (:job-id snapshot)
                              :job-state (:state snapshot)})]
                        (throw (ex-info error
                                        {:outcome :incomplete
                                         :failure-kind :deferred-completion-job-terminal
                                         :failure-stage deferred-completion-stage
                                         :repair-obligation successor})))
                      (throw
                       (ex-info
                        "Deferred completion job is still active; no replacement turn dispatched"
                        {:outcome :incomplete
                         :failure-kind :deferred-completion-job-not-complete
                         :failure-stage deferred-completion-stage
                         :repair-obligation stop-line
                         :job-id (:job-id snapshot)
                         :job-state (:state snapshot)}))))
                deferred-review-job (when (and snapshot
                                                (= :reviewer-wait deferred-completion-stage))
                                       snapshot)
                deferred-author-job
                (cond
                  ;; Deliberately unchanged. A commit-shape check here would be
                  ;; unreachable in effect: deferred-completion-artifact-failure below
                  ;; refuses a malformed ref before this value is ever used as
                  ;; the commit, and a non-done snapshot has already thrown
                  ;; above. A second guard would look like defence and be tested
                  ;; by nothing.
                  (and snapshot (= :author-wait deferred-completion-stage)
                       (:artifact-ref snapshot))
                  snapshot

                  deferred-review-job
                  (get-in stop-line [:failure-data :author-job]))
                _ (when (and deferred-review-job
                             (nil? deferred-author-job))
                    (let [error
                          "Reviewer deferred-completion lacks the original author-job provenance"
                          successor
                          (supersede-deferred-completion!
                           :machine-failure :deferred-completion-provenance-missing
                           :reviewer-wait error
                           {:job-id (:job-id deferred-review-job)})]
                      (throw
                       (ex-info error
                                {:outcome :incomplete
                                 :failure-kind :deferred-completion-provenance-missing
                                 :failure-stage :reviewer-wait
                                 :repair-obligation successor}))))
                _ (when-let [failure (deferred-completion-artifact-failure deferred-completion-stage
                                                                snapshot)]
                    (let [error (:message failure)
                          failure-kind (:failure-kind failure)
                          artifact-ref (:artifact-ref failure)
                          successor
                          (supersede-deferred-completion!
                           :machine-failure failure-kind
                           :author-wait error
                           (cond-> {:job-id (:job-id snapshot)
                                    :job-state (:state snapshot)}
                             artifact-ref (assoc :artifact-ref artifact-ref)))]
                      (throw
                       (ex-info error
                                (cond-> {:outcome :incomplete
                                         :failure-kind failure-kind
                                         :failure-stage :author-wait
                                         :repair-obligation successor}
                                  artifact-ref
                                  (assoc :artifact-ref artifact-ref))))))
                fresh-author? (nil? deferred-author-job)
                _ (reset! author-dispatch-route (if fresh-author? :fresh-author :deferred-completion))
                author-repo (when fresh-author?
                              (target-repository opts entry mission code-state))
                pre-author-head (when fresh-author?
                                  (observe-repo-head opts author-repo))
                prompt-for-head
                (fn [head-observation]
                  (author-prompt (assoc prompt-opts
                                        :reviewer reviewer
                                        :surprise-root
                                        (or (:surprise-root opts)
                                            (:data-root execution-cohort)
                                            cohort/default-data-root)
                                        :target-repository author-repo
                                        :target-repository-head
                                        (:head head-observation))
                                 target mission construction stop-lines))
                _ (reset! d-task-dispatch
                          (d-task/capture-result
                           (assoc @d-task-context
                                  :declaration-reads (some-> (:declaration-reads/state opts) deref)
                                  :before pre-author-head)))
                author-prompt-text
                (prompt-for-head pre-author-head)
                author-response
                (run-phase! opts @phase-context :author-dispatch
                            #(if deferred-author-job
                               {:job-id (:job-id deferred-author-job)
                                :state "done"
                                :deferred-completion? true
                                :completes-attempt (:attempt-id stop-line)}
                               (do
                                 (swap! dispatched-turns inc)
                                 ((or (:dispatch-fn opts) dispatch!) opts author
                                  "wm-full-loop" target
                                  author-prompt-text))))
                author-job-id (:job-id author-response)]
            (checkpoint! :dispatch
                         (term {:agent author
                                :availability (if deferred-author-job
                                                :deferred-completion
                                                :invoke-ready)
                                :job-id author-job-id
                                :prompt-ref (str "agency-job:" author-job-id)
                                :completes-attempt (when deferred-author-job
                                            (:attempt-id stop-line))}
                               {:kind (if deferred-author-job
                                        :agency-deferred-completion
                                        :agency-dispatch)
                                :response author-response}))
            (let [initial-author-job
                  (if deferred-author-job
                    deferred-author-job
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
                                  e)))))
                  retry-author?
                  (and fresh-author?
                       (pos? (:author-infrastructure-retries opts 0))
                       (author-infrastructure-failure? initial-author-job))
                  retry-pre-author-head
                  (when retry-author? (observe-repo-head opts author-repo))
                  retry-author-prompt-text
                  (when retry-author?
                    (prompt-for-head retry-pre-author-head))
                  retry-response
                  (when retry-author?
                    (run-phase! opts @phase-context :author-retry-dispatch
                                #(do
                                   (swap! dispatched-turns inc)
                                   ((or (:dispatch-fn opts) dispatch!)
                                    opts author "wm-full-loop" target
                                    retry-author-prompt-text))))
                  retry-job
                  (when retry-author?
                    (run-phase! opts @phase-context :author-retry-wait
                                #((or (:poll-fn opts) poll-job!)
                                  opts (:job-id retry-response))))
                  author-retries
                  (cond-> []
                    retry-author?
                    (conj (select-keys initial-author-job
                                       [:job-id :state :terminal-code
                                        :terminal-message])))
                  author-job
                  (cond-> (or retry-job initial-author-job)
                    retry-author? (assoc :author-retries author-retries))
                  effective-pre-author-head
                  (if retry-author? retry-pre-author-head pre-author-head)]
              (throw-if-cancelled! author-job :author-wait)
              (when-not (= "done" (:state author-job))
                (throw (ex-info "Author job did not complete"
                                (cond-> {:outcome :build-failed
                                         :author-job author-job}
                                  (seq author-retries)
                                  (assoc :author-retries author-retries)))))
              (throw-if-author-refused! author-job target :author-wait)
              (let [artifact-binding
                    (when fresh-author?
                      (fresh-artifact-binding opts author-repo
                                              effective-pre-author-head
                                              author-job))
                    observed-commit (:commit artifact-binding)
                    text-commit (:artifact-ref author-job)
                    _ (when-let [failure (unvalidated-artifact-failure
                                          fresh-author? text-commit observed-commit)]
                        (throw
                         (ex-info (:message failure)
                                  (cond-> {:outcome :build-failed
                                           :failure-kind (:failure-kind failure)
                                           :failure-stage :author-wait
                                           :target target
                                           :author-job author-job
                                           :artifact-binding artifact-binding}
                                    (:artifact-ref failure)
                                    (assoc :artifact-ref (:artifact-ref failure))))))
                    commit (if fresh-author? observed-commit text-commit)
                    author-job (cond-> author-job
                                 fresh-author?
                                 (assoc :repo-observed-artifact-ref commit
                                        :artifact-binding artifact-binding))
                    build (run-phase! opts @phase-context :build-resolution
                                      #(when commit
                                         (resolve-target-build opts author-repo commit)))
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
                  (throw (ex-info "Author completed without a verifiable commit"
                                  {:outcome :build-failed
                                   :author-verdict (:verdict (author-verdict author-job))
                                   :author-job author-job})))
                (reset! measurement-artifact {:repository repo :commit commit :paths files})
                (let [{:keys [commit repo files author-job build-retries]}
                      (build-cure-loop opts @phase-context author dispatched-turns
                                       target commit repo files author-job
                                       fresh-author? artifact-binding)
                    artifact-snapshot
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
                      _ (reset! measurement-artifact {:repository repo :commit commit :paths files})
                      review-response
                      (run-phase!
                       opts (cond-> @phase-context
                              artifact-snapshot
                              (assoc :tripwire/snapshot artifact-snapshot))
                       :reviewer-dispatch
                       #(if deferred-review-job
                          {:job-id (:job-id deferred-review-job)
                           :state "done" :deferred-completion? true
                           :completes-attempt (:attempt-id stop-line)}
                          (do
                            (swap! dispatched-turns inc)
                            ((or (:dispatch-fn opts) dispatch!) opts reviewer
                             "wm-full-loop" target
                             (reviewer-prompt (assoc prompt-opts :reviewer reviewer)
                                              target construction repo commit
                                              author-job stop-lines)))))
                      review-job
                      (if deferred-review-job
                        deferred-review-job
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
                      _ (throw-if-cancelled! review-job :reviewer-wait)
                      review-gate (review-execution-gate files review-job)
                      initial-commit commit
                      revision-state
                      (run-revision-round
                       prompt-opts @phase-context author reviewer dispatched-turns
                       target construction repo commit files author-job
                       artifact-binding review-job review-gate stop-lines)
                      commit (:commit revision-state)
                      repo (:repo revision-state)
                      files (:files revision-state)
                      author-job (:author-job revision-state)
                      artifact-binding (:artifact-binding revision-state)
                      review-job (:review-job revision-state)
                      review-gate (:review-gate revision-state)
                      reviews (:reviews revision-state)
                      revision (:revision revision-state)
                      approved? (and (= "done" (:state review-job))
                                     (= :approve (review-verdict review-job))
                                     (:passed? review-gate))]
                  (reset! measurement-artifact {:repository repo :commit commit :paths files})
                  (checkpoint! :build
                               (term (cond->
                                      {:artifacts files
                                       :generated-code files
                                       :commits (if revision
                                                  [initial-commit commit]
                                                  [commit])
                                       :patterns-used (vec (:shown construction))
                                       :inline-improvements []
                                       :build-retries (vec build-retries)
                                       :validation
                                       {:author (:execution author-job)
                                        :reviewer (:execution review-gate)
                                        :review-job (:job-id review-job)
                                        :review-text (job-text review-job)
                                        :approved? approved?
                                        :review-gate review-gate
                                        :artifact-binding artifact-binding}}
                                       revision
                                       (assoc :revision revision
                                              :reviews reviews))
                                     {:kind :git-commit-and-independent-review
                                      :repository repo}))
                  (observe-end!)
                  (when (and approved? (:measured-acquisition? opts)
                             attempt-evidence-dir)
                    (let [discharge-contract
                          (or (get-in construction
                                      [:selected-action :repair-obligation
                                       :discharge-contract])
                              (:discharge-contract stop-line)
                              {:requires []})]
                      (reset! standing-readback-state
                              (some->
                               (ensure-standing-decision!
                       attempt-evidence-dir target reviewer author
                       discharge-contract
                       (or (:repair-resolution-read-fn opts)
                           retained-resolution-record)
                     (fn [prompt]
                       (run-phase!
                        opts @phase-context :standing-completion-dispatch
                        #(do (swap! dispatched-turns inc)
                             ((or (:dispatch-fn opts) dispatch!)
                              opts reviewer "wm-full-loop" target prompt))))
                     (fn [response]
                       (run-phase!
                       opts @phase-context :standing-completion-wait
                        #((or (:poll-fn opts) poll-job!)
                          opts (:job-id response)))))
                               (select-keys [:entity/id :decision :decided-by
                                             :standing/store-annotation])))))
                  (when-not approved?
                    (let [failure-data
                          (cond->
                           {:outcome :build-failed :author-job author-job
                           :artifact-binding artifact-binding
                           :review-job review-job :commit commit
                           :target target
                           :selected-entry
                           (select-keys entry
                                        [:action :controller-score :G-efe])}
                            (seq reviews)
                            (assoc :reviews reviews :revision revision)

                            (not (:passed? review-gate))
                            (assoc :failure-kind
                                   :review-execution-evidence-missing
                                   :failure-stage :reviewer-wait
                                   :review-gate review-gate))
                          deferred-completion-rejection
                          (when (and deferred-review-job
                                     (= :incomplete-recoverable
                                        (:repair/class stop-line)))
                            (let [finding
                                  ((or (:repair-record-fn opts)
                                       repair/record-review-failure!)
                                   ;; The finding id derives from this field.
                                   ;; Local attempt ordinals restart per
                                   ;; cohort; only the authority-qualified id
                                   ;; is unique across the shared findings
                                   ;; store (r6, 2026-09-13: bare
                                   ;; "attempt-002" collided with a July
                                   ;; finding and the review verdict was
                                   ;; mistyped :initialization-failed).
                                   {:attempt-id external-attempt-id
                                    :occurrence
                                    (occurrence-for nil (:job-id review-job)
                                                    (case (review-verdict review-job)
                                                      :request-changes
                                                      :review-request-changes
                                                      :reject :review-rejected))
                                    :observation
                                    (observation-for :deferred-completion-review)
                                    :target target
                                    :commit commit
                                    :selected-entry (:selected-entry failure-data)
                                    :reviewer reviewer
                                    :review-job (:job-id review-job)
                                    :review-verdict (review-verdict review-job)
                                    :review-text (job-text review-job)})]
                              ((or (:repair-supersede-fn opts)
                                   repair/supersede!)
                               stop-line finding :deferred-review-rejected)
                              finding))]
                      (throw (ex-info (if (:passed? review-gate)
                                        "Independent review did not approve"
                                        "Independent review lacks execution evidence")
                                      (cond-> failure-data
                                        deferred-completion-rejection
                                        (assoc :repair-obligation
                                               deferred-completion-rejection))))))
                  (let [witness
                        (run-phase! opts @phase-context :grounding
                                    #((or (:ground-fn opts) ground-commit!)
                                      attempt-id target author reviewer repo commit files
                                      construction review-job
                                      (assoc opts :cohort-id (:cohort/id start-event))))]
                    ;; Discharge runs once, AFTER the immutable execution close.
                    ;; A grounded substrate insertion alone never resolves a
                    ;; finding, and an unrelated memory item is not a successor.
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
                                :artifact-binding artifact-binding
                                :build-retries (vec build-retries)}
                                @standing-readback-state
                                (assoc :standing-readback
                                       @standing-readback-state)
                                (seq historical-validation-lines)
                                (assoc :historical-validation-deferred
                                       (mapv #(select-keys
                                              % [:repair/id :repair/status
                                                 :repair/verification])
                                             historical-validation-lines))
                                (seq reviews)
                                (assoc :reviews reviews :revision revision)

                                feature-card
                                (assoc :feature-card feature-card))))))))))))
      (catch Throwable e
        (observe-end!)
        (if @closing?
          ;; Cohort-53 attempt-001 is retained as the historical counterexample:
          ;; a typed evidence refusal escaped this branch and orphaned the attempt
          ;; after adjudication.  Closing is now a last-resort durable boundary.
          (let [failure-data (if (instance? clojure.lang.ExceptionInfo e)
                               (ex-data e) {})
                refusal-kind (or (:interpretation-evidence/refusal failure-data)
                                 (:limb-evidence/refusal failure-data)
                                 (:evidence-manifest/refusal failure-data)
                                 (:close-retention/refusal failure-data)
                                 (:failure-kind failure-data)
                                 :close-exception)
                exception-class (.getName (class e))
                finding ((or (:repair-system-record-fn opts)
                             repair/record-system-failure!)
                         {:attempt-id external-attempt-id
                          :occurrence
                          (occurrence-for (:repair/occurrence failure-data)
                                          external-attempt-id refusal-kind)
                          :observation (observation-for :outer-close-fallback)
                          :repair-class :machine-failure
                          :machine-repo (:repo code-state)
                          :target (get-in @checkpoints
                                          [:selection :judgment :selected-mission])
                          :failure-stage :close
                          :outcome :build-failed
                          :failure-kind refusal-kind
                          :error (.getMessage e)
                          :failure-data failure-data
                          :opened-at (get-in time-cell
                                             [:judgment :machine-state :started-at])
                          :discharge-contract
                          (discharge-contract :machine-failure)})
                sorry-data {:outcome :build-failed
                            :grounded? false
                            :artifact-only? false
                            :failure-kind refusal-kind
                            :failure-stage :close
                            :error (.getMessage e)
                            :exception-class exception-class
                            :refusal-data failure-data
                            :repair-obligation finding
                            :duration-ms (- (System/currentTimeMillis) started)
                            :resource-use {:agent-turns @dispatched-turns}
                            :sorry {:kind refusal-kind
                                    :refusal-data failure-data}}
                closed (term (assoc sorry-data :job-texts @job-text-records)
                             {:kind :full-loop-close-failure
                              :attempt-id attempt-id})
                closed-event (when cohort?
                               (if cohort-source
                                 (cohort/close-attempt!
                                  cohort-source (:data-root execution-cohort)
                                  attempt-id closed)
                                 (cohort/close-attempt! attempt-id closed)))]
            {:attempt-id attempt-id
             :opportunity-id opportunity-id
             :outcome :build-failed
             :checkpoints @checkpoints
             :data sorry-data
             :closed-event closed-event})
          (if (identical? historical-verification-completion-token
                          (:historical-verification-completion-token (ex-data e)))
            (let [completion (ex-data e)]
              (close! :historical-verification-awaiting-validation
                      {:target (get-in @checkpoints [:selection :judgment
                                                     :selected-mission])
                       :repair-obligation (:repair-obligation completion)
                       :verification-attempt (:verification-attempt completion)}))
            (let [failure (ex-data e)
                review-job (:review-job failure)
                verdict (some-> review-job review-verdict)
                review-finding (when (and (nil? (:repair-obligation failure))
                                   (:commit failure)
                                   (#{:request-changes :reject} verdict))
                          ((or (:repair-record-fn opts)
                               repair/record-review-failure!)
                           ;; Authority-qualified for the same reason as the
                           ;; deferred-completion-rejection site above: bare ordinals
                           ;; collide across cohorts in the shared store.
                           {:attempt-id external-attempt-id
                            :occurrence
                            (occurrence-for (:repair/occurrence failure)
                                            (:job-id review-job)
                                            (case verdict
                                              :request-changes
                                              :review-request-changes
                                              :reject :review-rejected))
                            :observation (observation-for :review-failure)
                            :target (:target failure)
                            :commit (:commit failure)
                            :selected-entry (:selected-entry failure)
                            :reviewer @reviewer-of-record
                            :review-job (:job-id review-job)
                            :review-verdict verdict
                            :review-text (job-text review-job)}))
                finding (or review-finding (:repair-obligation failure))]
              (close! (outcome-from e)
                      (cond->
                       {:target (or (:target failure)
                                    (some-> @checkpoints :selection :judgment
                                            :selected-mission))
                        :commit (:commit failure)
                        :artifact-binding (:artifact-binding failure)
                        :witness (:witness failure)
                        :author-job (:author-job failure)
                        :review-job review-job
                        :repair-obligation finding
                        :repair/occurrence
                        (or (:repair/occurrence failure)
                            (:repair/occurrence finding))
                        :failure-kind (failure-kind-from e)
                        :feature-card-invalid-reason
                        (:feature-card-invalid-reason failure)
                        :feature-card-source (:feature-card-source failure)
                        :failure-detail (:failure-detail failure)
                        :failure-stage (or (:failure-stage failure)
                                           (last-error-phase @phase-events))
                        :error (.getMessage e)
                        :error-class (.getName (class e))
                        :error-data failure
                        :build-retries (when (seq (:build-retries failure))
                                         (:build-retries failure))}
                        (seq (:reviews failure))
                        (assoc :reviews (:reviews failure)
                               :revision (:revision failure)))))))))))

(defn run-opportunity!
  "Run one opportunity and ensure initialization failures also become durable
  stop-line findings. Failures after cohort start are closed by the core state
  machine; this outer boundary covers phase-log and cohort-start failures that
  necessarily occur before an ordinary attempt can own them.

  When the cohort stopping rule is reached (all target attempts consumed),
  the exception is NOT a machine failure — it signals normal cohort
  completion. Returning :cohort-complete avoids spurious repair obligations
  that would otherwise fire every time the scheduler probes a finished cohort."
  [raw-opts]
  ;; a missing :run-id gets a DATED id, so the receipt it writes matches the
  ;; pattern every receipt reader in this repo requires
  (let [run-id (or (:run-id raw-opts)
                   (str (subs (str (Instant/now)) 0 10) "-" (UUID/randomUUID)))
        started-at (str (Instant/now))
        raw-opts (assoc raw-opts :participants/state (atom nil)
                                :declaration-reads/state (atom nil)
                                :habit-reads/state (atom [])
                                :job-liveness/state (atom [])
                                :scan-report/state (atom nil))
        _ (ensure-dispatch-seat! (config raw-opts))
        ;; BEFORE the attempt: a stale runner must not consume it, and the
        ;; identity it records must be the identity that judged the run.
        source-check (refuse-on-runner-source-drift!)
        publication (discharge-receipt/catch-up!
                     (or (:repair-root raw-opts) repair/default-root)
                     (or (:discharge-receipt-repo raw-opts) "/home/joe/code/futon2"))
        result
        (try
      (binding [cascade-sources/*read-occurrences* (:declaration-reads/state raw-opts)
                input-receipts/*habit-reads* (:habit-reads/state raw-opts)]
        (run-opportunity-core! (assoc raw-opts :run-id run-id
                                     :loaded-code-identity source-check)))
    (catch Throwable e
      (when (or (= :delivery-qa-gate-failed
                   (:failure-kind (ex-data e)))
                (:evidence-manifest/refusal (ex-data e))
                (:limb-evidence/refusal (ex-data e)))
        (throw e))
      ;; Cohort stopping rule is normal completion, not a machine failure.
      ;; Repair-initialization was caused by this being treated as an
      ;; initialization-failed; it must return cleanly instead.
      ;; Recognition is typed only: the {:cohort/error :stopping-rule-reached}
      ;; marker is sought along the whole cause chain, so a wrapper that
      ;; buries the ex-data is still recognized — but message text is never
      ;; consulted, because substring matching would classify unrelated
      ;; failures that merely mention the phrase as normal completion.
      (let [edata (ex-data e)
            stopping-rule-data
            (loop [t ^Throwable e depth 0]
              (when (and t (< depth 16))
                (let [d (ex-data t)]
                  (if (= :stopping-rule-reached (:cohort/error d))
                    d
                    (recur (.getCause t) (inc depth))))))]
        (if stopping-rule-data
          {:attempt-id (str "cohort-complete-" (UUID/randomUUID))
           :outcome :cohort-complete
           :checkpoints {}
           :data {:cohort/error :stopping-rule-reached
                  :target (:target stopping-rule-data)
                  :attempted (:attempted stopping-rule-data)}}
          (let [trigger (or (:trigger raw-opts) :duree-click-on-demand)
                error (if (str/blank? (str (.getMessage e)))
                        "Full-loop initialization failed"
                        (.getMessage e))
                ;; This boundary used to hardcode :machine-failure /
                ;; :initialization-failed for EVERY throwable, while capturing
                ;; :error-class into the backtrace and then ignoring it. A
                ;; transport timeout landing here — the same condition that
                ;; killed attempt-057 and attempt-058 at :selection, arriving
                ;; slightly earlier, before an attempt can own it — was
                ;; therefore charged to the machine, demanding a repair commit
                ;; and an independent review for a network fault.
                ;; Precedence is identical to failure-kind-from, deliberately:
                ;; explicit ex-data typing at ANY depth wins, transport typing
                ;; only fills the gap, and anything unrecognised falls to
                ;; :initialization-failed and keeps the machine-failure
                ;; contract. Consulting transport FIRST (as this did before)
                ;; was a fail-open: an ex-info{:failure-kind :build-failed}
                ;; wrapping any transport cause had that cause found by the
                ;; chain walk and was downgraded to :environmental-hold, so a
                ;; real typed machine failure escaped its own contract.
                failure-kind (or (explicit-failure-kind e)
                                 (transport-failure-kind e)
                                 :initialization-failed)
                occurrence (repair-occurrence
                            (:repair/occurrence edata)
                            (str "wm-runner::" run-id)
                            (str (or (:job-id edata)
                                     (:trip/id edata)
                                     (:tripwire/report-id edata)
                                     run-id))
                            failure-kind started-at)
                attempt-id (str "initialization-"
                                (subs (:occurrence/id occurrence) 4 16))
                repair-class (repair-class-for failure-kind)
                finding
                ((or (:repair-system-record-fn raw-opts)
                     repair/record-system-failure!)
                 {:attempt-id attempt-id
                  :occurrence occurrence
                  :observation
                  {:observation/id (str "initialization-catch::" started-at)
                   :observed-at started-at
                   :source (str "wm-runner::" run-id)}
                  :repair-class repair-class
                  :failure-stage :initialization
                  :outcome :incomplete
                  :failure-kind failure-kind
                  :error error
                  :failure-data edata
                  :backtrace {:error-class (.getName (class e))}
                  :discharge-contract (discharge-contract repair-class)})
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
                  :failure {:kind failure-kind
                            :stage :initialization
                            :error error
                            :repair-id (:repair/id finding)
                            :discharge-contract (:discharge-contract finding)}})]
            {:attempt-id attempt-id
             :outcome :incomplete
             :checkpoints {}
             :morning-brief-ref brief-ref
             :data {:repair-obligation finding
                    :repair/occurrence occurrence
                    :failure-kind failure-kind
                    :failure-stage :initialization
                    :error error
                    :error-data edata}}))))
      (finally
        (post-wm-status! (config raw-opts)
                         {:source "wm-full-loop" :status "idle"})))
        final-result (assoc result :runner/source source-check :repair/publication publication)]
    ;; The SAME identity annotates the result persist-run-record! sees; the
    ;; tick record therefore carries :runner/source (round-2 review: the
    ;; durable record never included it).
    (merge final-result {:run/id run-id}
           (persist-run-record! raw-opts run-id started-at final-result))))
