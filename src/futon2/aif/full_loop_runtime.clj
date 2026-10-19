(ns futon2.aif.full-loop-runtime
  "Production composition root for the full-loop runner."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.cascade-feedback :as cascade-feedback]
            [futon2.aif.cascade-revision-producer :as cascade-revision-producer]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.flight-runner :as flight-runner]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-live-outer-selector :as meta-live]
            [futon2.aif.meta-field-observation :as meta-field]
            [futon2.aif.meta-injury-store :as injury-store]
            [futon2.aif.meta-self-heal-production :as meta-self-heal]
            [futon2.aif.outer-learning-manifest :as outer-learning-manifest]
            [futon2.aif.outer-policy-evidence-provider :as policy-evidence-provider]
            [futon2.aif.outer-policy-provider-runtime :as policy-provider-runtime]
            [futon2.aif.reading-policy-precision :as reading-precision]
            [futon2.aif.c-vector :as cv]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.selection-timing :as selection-timing]
            [futon2.aif.selected-target-reading :as selected-target-reading]
            [futon2.aif.target-reading-preparation :as reading-preparation]
            [futon2.aif.occurrence-square-authoring :as square-authoring]
            [futon2.aif.selected-reading :as selected-reading]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.wm.click-ask :as click-ask]
            [futon2.aif.wm.open-cascade-resume :as open-resume]
            [futon2.aif.wm.on-prod-history :as on-prod-history]
            [futon2.aif.wm.cascade-continuation :as cascade-continuation]
            [futon2.aif.wm.cascade-decision :as cascade-decision]
            [futon2.report.war-machine :as wm])
  (:import [java.time Instant]
           [java.util UUID]))

(load-identity/register! *ns* *file*)

(def default-pattern-graph-path
  "/home/joe/code/storage/operator-turns/mined-pattern-graph.json")
(def default-pattern-graph-diff-dir
  "/home/joe/code/futon2/data/wm-pattern-graph-diffs")
(def default-reading-precision-campaign-path
  "/home/joe/code/futon2/resources/wm/reading-precision-beta/wm-readiness-2026-10-17-campaign.edn")
(def default-cascade-continuation-close-root
  "/home/joe/code/futon2/data/wm-cascade-continuation-closes")

(def budget-observation-schema :wm/meta-click-budget-observation-v1)

(defn- valid-click-availability? [availability]
  (and (= :wm/ordinary-click-availability-v1 (:schema availability))
       (= :ordinary-click (:unit availability))
       (map? (:ledger-source availability))
       (string? (get-in availability [:ledger-source :path]))
       (string? (get-in availability [:ledger-source :sha256]))
       (boolean (re-matches #"[0-9a-f]{64}" (get-in availability [:ledger-source :sha256])))
       (every? integer? [(:allocated availability) (:consumed availability)
                         (:available availability)])
       (<= 0 (:consumed availability) (:allocated availability))
       (= (:available availability) (- (:allocated availability) (:consumed availability)))))

(defn observe-ordinary-click-budget
  "Read one pre-admission ordinary-click snapshot without changing its ledger.
  Reader failure and malformed material are unavailable observations, never a
  synthetic zero."
  [opts]
  (let [read (cond
               (contains? opts :ordinary-click-availability) (constantly (:ordinary-click-availability opts))
               (:ordinary-click-availability-fn opts) (:ordinary-click-availability-fn opts)
               :else (try (requiring-resolve 'futon3c.wm.ordinary-click-budget/availability)
                          (catch Throwable _ nil)))]
    (if-not read
      {:schema budget-observation-schema :status :unavailable
       :reason :ordinary-click-budget-reader-unavailable
       :provenance {:status :absent} :observation-time {:status :absent}
       :phase :pre-admission}
      (try
        (let [availability (read)]
          (if-not (valid-click-availability? availability)
            {:schema budget-observation-schema :status :unavailable
             :reason :ordinary-click-budget-observation-malformed
             :provenance {:status :absent} :observation-time {:status :absent}
             :phase :pre-admission}
            {:schema budget-observation-schema
             :status (if (pos? (:available availability)) :available :exhausted)
             :availability availability :ledger-source (:ledger-source availability)
             :provenance {:status :present :ledger-source (:ledger-source availability)}
             :observation-time {:status :present
                                :value (or (:budget-observed-at opts) (Instant/now))}
             :phase :pre-admission}))
        (catch Throwable _
          {:schema budget-observation-schema :status :unavailable
           :reason :ordinary-click-budget-reader-failed
           :provenance {:status :absent} :observation-time {:status :absent}
           :phase :pre-admission})))))

(defn- ensure-reading-precision-campaign!
  [opts]
  (let [path (or (:reading-precision-campaign-path opts)
                 default-reading-precision-campaign-path)
        file (io/file path)]
    (when (.isFile file)
      (let [campaign (edn/read-string (slurp file))
            current (reading-precision/campaign-status)]
        (when-not (= (:sha256 campaign) (:sha256 current))
          (reading-precision/register-campaign! campaign))))))

(defn- designated-agent-answer-fn
  "Agency answer port for machine requests.  It contacts exactly the
  configured author and returns a typed no-contact when that seat is not
  currently invoke-ready; it never consults a fallback roster seat."
  [opts seat prompt-fn invoke-mode]
  (let [author seat
        agency-base (or (:agency-base opts) runner/default-agency-base)
        answer (flight-runner/agency-answer-fn
                {:seat author :caller "wm-full-loop"
                 :opts (assoc opts :agency-base agency-base
                                   :invoke-mode (or invoke-mode "brief"))
                 :prompt-fn prompt-fn})]
    (fn [issued]
      (let [roster (try (runner/agent-roster agency-base)
                        (catch Throwable _ {}))
            record (or (get roster author) (get roster (keyword (str author))))
            status (some-> (:status record) name)]
        (if (and (string? author)
                 (true? (:invoke-ready? record))
                 (contains? #{"idle" "restored"} status))
          (answer issued)
          {:state :not-contacted :reason :designated-author-unavailable
           :author author})))))

(defn- designated-author-answer-fn [opts prompt-fn]
  (designated-agent-answer-fn opts (:author opts) prompt-fn "brief"))

(defn- open-cascade-author-prompt [req]
  (str "Repair the held executable cascade phase without selecting again. "
       "Implement resolutions for exactly the listed holes, commit the source, "
       "and reply with one JSON object containing requisition-id, cascade-digest, "
       "by, decisions, and patch {namespace,var,source-digest}.\n\n"
       (pr-str req)))

(defn- open-cascade-review-prompt [{:keys [requisition author-answer]}]
  (str "Independently review this held-cascade repair. Verify the committed "
       "source and every hole decision. Reply with one JSON object containing "
       "the author's requisition-id, cascade-digest, by, decisions and patch, "
       "plus accepted-by set to your own seat id.\n\nREQUISITION:\n"
       (pr-str requisition) "\n\nAUTHOR ANSWER:\n" (pr-str author-answer)))

(defn- cascade-effect-author-prompt [request]
  (str "Execute exactly this certified cascade occurrence through the ordinary "
       "build/review/close work discipline. Ground any claimed effect in durable "
       "artifacts (commit or content-addressed evidence). Reply with one JSON "
       "object naming request-id, cascade-digest, occurrence-id, by, and effect.\n\n"
       (pr-str request)))

(defn- cascade-effect-review-prompt [{:keys [request author-answer]}]
  (str "Independently verify this occurrence-level build/review/close result. "
       "Inspect the durable artifacts. Reply with one JSON object containing "
       "the exact request-id, cascade-digest, occurrence-id, author's by, your "
       "accepted-by, and effect {status:'verified', grounding:{kind,digest}}. "
       "Do not accept an ungrounded success label.\n\nREQUEST:\n"
       (pr-str request) "\n\nAUTHOR ANSWER:\n" (pr-str author-answer)))

(defn production-cascade-executors
  "The live capability registry for certified cascade continuation."
  [opts]
  (let [author (:author opts)
        reviewer (:reviewer opts)
        author-contact (designated-agent-answer-fn
                        opts author cascade-effect-author-prompt "work")
        reviewer-contact (designated-agent-answer-fn
                          opts reviewer cascade-effect-review-prompt "work")]
    {:wm/pattern-apply
     (cascade-continuation/reviewed-build-review-close-executor
      {:author author :reviewer reviewer
       :author-contact #(handoff/response-map (author-contact %))
       :reviewer-contact #(handoff/response-map (reviewer-contact %))})}))

(defn- patch-loader
  "Resolve a reviewed patch only after reloading its named namespace and
  checking the loaded source digest against the reviewed descriptor."
  [{:keys [namespace var source-digest]}]
  (let [n (symbol namespace)
        v (symbol var)]
    (require n :reload)
    (let [loaded (get @load-identity/registry n)
          resolved (ns-resolve n v)]
      (when-not (= source-digest (:sha256 loaded))
        (throw (ex-info "reviewed patch source digest does not match loaded source"
                        {:kind :patch-source-digest-mismatch
                         :expected source-digest :loaded (:sha256 loaded)})))
      (when-not (and resolved (ifn? @resolved))
        (throw (ex-info "reviewed patch var is not callable"
                        {:kind :patch-var-unavailable :namespace n :var v})))
      @resolved)))

(defn open-cascade-resume-handler
  "Production D4 composition. Contact the designated author and distinct
  reviewer through Agency, validate the reviewed answer, hot-load the exact
  reviewed source, and retry D3's held phase in the same click."
  [base-opts]
  (fn handler
    ([error] (handler error {}))
    ([error live-opts]
      (let [opts (merge base-opts live-opts)
            author (:author opts)
            reviewer (:reviewer opts)
            author-contact (or (:open-cascade-author-answer-fn opts)
                               (designated-agent-answer-fn
                                opts author open-cascade-author-prompt "work"))
            reviewer-contact (or (:open-cascade-reviewer-answer-fn opts)
                                 (designated-agent-answer-fn
                                  opts reviewer open-cascade-review-prompt "work"))
            ctx (open-resume/recognize error)
            req (when (= :held (:status ctx))
                  (open-resume/authoring-requisition ctx author reviewer))]
        (if-not (= :dispatched (:status req))
          (or req ctx)
          (let [author-answer (handoff/response-map (author-contact req))
                reviewed (when author-answer
                           (handoff/response-map
                            (reviewer-contact {:requisition req
                                               :author-answer author-answer})))
                ledger (open-resume/new-ledger (or (:run-id opts) "held-click"))]
            (if-not reviewed
              {:status :error :kind :open-cascade-review-answer-invalid}
              (open-resume/handle-error!
               ledger error author reviewer reviewed
               {:click-budget (:click-budget opts)
                :selection-counter (:selection-counter opts)
                :patch-loader (or (:open-cascade-patch-loader opts)
                                  patch-loader)}))))))))

(defn- selection-judge
  [opts days]
  (wm/generate-war-machine
   days
   (merge (wm/accumulation-config)
          (select-keys opts [:accumulate-strategic-habit? :run-id
                             :loaded-code-identity :cascade-habit-path
                             :cascade-feedback-path :cascade-feedback-metadata
                             :observation-labels-path :flight :trace-dir
                             :outer-task-policy :outer-task-selection-fn
                             :selection-timing/state :nano-time-fn
                             ;; the decision forms every target's policy
                             ;; family against this pinned graph
                             :pattern-graph-path :target-reading-root
                             :occurrence-square-root :pattern-library-root
                             :require-occurrence-squares?
                             ;; an operator's identity-bound beta declaration
                             :reading-precision-declaration])
          (select-keys opts [:meta-field-observation :meta-field-source-pin])
          ;; Construction publishes below. Do not publish twice.
          {:trace? false :include-advisory-lanes? false :defer-render? true})))

(defn retained-outer-selector
  "Retain a chosen outer receipt across reconstruction, never an incomplete
  reading-menu refusal. Preparation changes that menu before any choice exists;
  a sealed selected-policy carrier, however, must never be rebuilt."
  [outer-selector policy-field-config]
  (let [retained (atom ::unselected)]
    (fn [input]
      (let [receipt @retained]
        (if (not= ::unselected receipt)
          receipt
          (let [selected (outer-selector
                          (assoc input :policy-field-config policy-field-config))]
            (when-not (and (contains? #{:incomplete-policy-menu-coverage
                                       :no-canonically-bound-policy}
                                     (get-in selected [:policy :reason]))
                           (nil? (get-in selected [:chosen :id]))
                           (nil? (get-in selected
                                         [:policy :selected-outer-cascade-policy])))
              (reset! retained selected))
            selected))))))

(defn production-defaults
  "Build the report-backed functions for one production invocation."
  [opts]
  (ensure-reading-precision-campaign! opts)
  (let [injury-authority (or (:meta-injury-authority opts)
                             (injury-store/load-authority
                              (or (:meta-injury-root opts)
                                  injury-store/default-root)))
        click-availability (observe-ordinary-click-budget opts)
        meta-observation (or (:meta-field-observation opts)
                             (when injury-authority
                               (meta-field/observe
                                {:catalog-path meta-field/default-algorithm-catalog
                                 :click-availability click-availability
                                 :injury-authority injury-authority})))
        opts (cond-> opts
               meta-observation
               (assoc :meta-field-observation meta-observation
                      :meta-field-source-pin (:source-pin meta-observation)))
        outer-selector (or (:outer-task-selection-fn opts) meta-live/selector)
        evidence-provider (or (:outer-policy-evidence-provider opts)
                              (when-let [path (:outer-policy-evidence-provider-path opts)]
                                (policy-evidence-provider/load-static path))
                              policy-provider-runtime/production-provider)
        outer-selector (fn [input]
                         (outer-selector
                          (assoc input :outer-policy-evidence-provider
                                 evidence-provider)))
        ;; A click may invoke the report judge twice when an interpretation ask
        ;; publishes missing construction input.  Outer state can change while
        ;; that ask runs (including the graph and learning manifest), but the
        ;; second pass is inner cascade reconstruction for the same chosen
        ;; outer action, not a new outer opportunity. Retain the exact first receipt so query-time
        ;; evidence is requested for, and consumed by, one stable target.
        policy-field-config
        (select-keys opts
                     [:target-reading-root :pattern-graph-path
                      :occurrence-square-root
                      :occurrence-square-pattern-texts
                      :pattern-library-root
                      :require-occurrence-squares?
                      :target-policy-retraction])
        pinned-outer-selector
        (retained-outer-selector outer-selector policy-field-config)
        opts (-> opts
                 (update :outer-task-policy #(or % :meta))
                 (assoc :outer-task-selection-fn pinned-outer-selector))
        selected-judgment (atom nil)]
    {:judge-fn (fn [days]
                 (try
                   (let [generated (selection-judge opts days)
                         frontier (get-in generated [:judgement :outer-task-selection
                                                     :policy :frontier])
                         generated (cond-> generated
                                     frontier
                                     (assoc-in [:judgement :decision :selection-certificate
                                                :reading-frontier] frontier))]
                     (reset! selected-judgment (:judgement generated))
                     generated)
                   (catch Throwable e
                     (selection-timing/abort! (:selection-timing/state opts)
                                              (:nano-time-fn opts)
                                              :selection-judge-threw)
                     (throw e))))
     :cascade-revision-proposals-fn
     (cascade-revision-producer/make-proposals-fn
      {:judgment-fn #(deref selected-judgment)})
     :refresh-required? true
     ;; Stop before dispatch when a selection merely names machinery that this
     ;; click did not exercise. The stop is inside the restartable selection
     ;; phase, so a hot repair followed by :retry reruns the decision.
     :require-loop-node-exercise? true
     :refresh-fn (fn []
                   (let [mission-freshness (mission-registry/refresh-mission-substrate!)]
                     (cv/maybe-refresh!)
                     {:freshness mission-freshness}))
     :interpretation-ask-fn (click-ask/click-ask-fn opts)
     :selected-target-reading-fn
     (fn [runner-opts judgement]
       ;; The runner mints :registered-run/job-ledger after production-defaults
       ;; is built. Construct the Agency answer port here, from the merged live
       ;; options, or dispatch! refuses before it can create the author job.
       (let [live-opts (merge opts runner-opts)
             answer-fn (or (:selected-target-reading-answer-fn live-opts)
                           (designated-author-answer-fn
                            live-opts selected-target-reading/prompt))]
         (if-let [field (selected-target-reading/prechoice-field judgement)]
           (let [needs (selected-target-reading/prechoice-needs field)
                 result (reading-preparation/prepare!
                         (assoc live-opts :answer-fn answer-fn) field
                         {:deadline-ms (+ (System/currentTimeMillis) 1800000)
                          :limit (min 3 (max 1 (count needs)))})]
             ;; The existing runner redecides once after publication. There
             ;; is no retained target yet: this pass builds the first menu.
             (assoc result :published (boolean (some :published (:records result)))))
           (selected-target-reading/repair!
            (assoc live-opts :answer-fn answer-fn)
            judgement))))
     :occurrence-square-authoring-fn
     (fn [runner-opts judgement]
       (let [live-opts (merge opts runner-opts)
             answer-fn (or (:occurrence-square-answer-fn live-opts)
                           (designated-agent-answer-fn
                            live-opts (:author live-opts)
                            square-authoring/prompt "brief"))]
         (square-authoring/repair!
          (assoc live-opts :answer-fn answer-fn)
          judgement)))
     ;; Unlike the target-reading phase, this phase receives only REQUEST.
     ;; Delay construction of its contact port until the runner has added its
     ;; run-local ledgers, and make that late binding explicit.
     :selected-reading-ask-builder
     (fn [runner-opts]
       (let [live-opts (merge opts runner-opts)]
         (or (:selected-reading-answer-fn live-opts)
             (designated-author-answer-fn live-opts selected-reading/prompt))))
     :scan-render-fn wm/render-war-machine
     :effective-run-configuration-fn wm/effective-run-configuration
     :mode-flags-fn wm/arena-mode-flags}))

(defn- run-ordinary-opportunity!
  "Run one production opportunity with the report-backed defaults installed."
  [opts]
  ;; Mint before building the composition root.  The runner historically
  ;; minted later, after `production-defaults` had closed over OPTS; the judge
  ;; therefore saw no tick identity and accumulation refused even though the
  ;; eventual run record had one.
  (let [opts (cond-> opts
               (not (:run-id opts))
               (assoc :run-id (str (subs (str (Instant/now)) 0 10)
                                   "-" (UUID/randomUUID))))
        opts (update opts :cascade-feedback-path
                     #(or % cascade-feedback/default-path))
        opts (update opts :cascade-feedback-metadata
                     #(or % (cascade-feedback/load-construction-metadata
                             (:cascade-feedback-path opts))))
        opts (update opts :pattern-graph-path #(or % default-pattern-graph-path))
        opts (update opts :pattern-graph-diff-dir #(or % default-pattern-graph-diff-dir))
        opts (assoc opts :require-occurrence-squares? true)
        opts (update opts :outer-learning-manifest-path
                     #(or % outer-learning-manifest/default-path))
        ;; The production judge closes over OPTS before the runner adds its
        ;; other ledgers. Mint this collector here and pass the same identity
        ;; through, rather than timing into a disconnected atom.
        opts (update opts :selection-timing/state
                     #(or % (selection-timing/new-state)))
        defaults (production-defaults opts)
        ;; Pass production ports on the actual runner option map as well as
        ;; binding the compatibility lookup.  The serving runner must not
        ;; depend on a dynamic binding surviving namespace reload or an
        ;; execution boundary before it decides whether selection is
        ;; terminal.
        runner-opts0 (assoc opts
                           :click-budget (or (:click-budget opts) (atom 0))
                           :selection-counter (or (:selection-counter opts)
                                                  (atom 0))
                           :selected-target-reading-fn
                           (:selected-target-reading-fn defaults)
                           :occurrence-square-authoring-fn
                           (:occurrence-square-authoring-fn defaults)
                           :selected-reading-ask-builder
                           (:selected-reading-ask-builder defaults))
        ;; Construct D4 only now: RUNNER-OPTS carries the live job ledger and
        ;; exact cast.  Closing over the earlier OPTS recreated the stale
        ;; handoff bug this composition root exists to prevent.
        runner-opts (assoc runner-opts0 :open-cascade-resume-fn
                           (or (:open-cascade-resume-fn runner-opts0)
                               (open-cascade-resume-handler runner-opts0)))]
    (binding [runner/*runtime-defaults* defaults]
      (runner/run-opportunity! runner-opts))))

(defn run-opportunity!
  "Arbitrate durable META injuries before constructing any ordinary policy
  field. An active injury consumes the opportunity without fallback."
  [opts]
  (let [opts (assoc opts
                    :meta-injury-ledger-path
                    (or (:meta-injury-ledger-path opts)
                        meta-self-heal/default-ledger-path)
                    :meta-repair-transaction-path
                    (or (:meta-repair-transaction-path opts)
                        meta-self-heal/default-transaction-path))
        retry (:meta-repair-retry opts)
        diagnosis-context (when-not retry (meta-self-heal/diagnosis-context opts))
        diagnosis-observation (when-not retry
                                (meta-self-heal/prepare-diagnosis opts diagnosis-context))
        acceptance-observation (if retry
                                {:status :not-required
                                 :reason :meta-self-heal/acceptance-retry-not-prepared}
                                (meta-self-heal/prepare-acceptance opts diagnosis-observation))
        effective-opts (cond-> opts
                         diagnosis-observation
                         (assoc :meta-diagnosis-observation diagnosis-observation)
                         acceptance-observation
                         (assoc :meta-acceptance-observation acceptance-observation)
                         (= :available (:status diagnosis-observation))
                         (assoc :diagnosis-evidence-pin
                                (:diagnosis-evidence-pin diagnosis-observation)))
        readiness (if retry
                    (meta-self-heal/attributable-retry-readiness effective-opts retry)
                    (meta-self-heal/production-readiness effective-opts diagnosis-context))]
    (cond
      (= :refused (:status readiness))
      (assoc readiness :diagnosis-observation diagnosis-observation
             :acceptance-observation acceptance-observation)
      retry (assoc (run-ordinary-opportunity! effective-opts)
                   :diagnosis-observation diagnosis-observation
                   :acceptance-observation acceptance-observation)
      :else (let [routed (meta-self-heal/route! effective-opts)]
              (assoc (or routed (run-ordinary-opportunity! effective-opts))
                     :diagnosis-observation diagnosis-observation
                     :acceptance-observation acceptance-observation)))))

(defn resume-on-prod-cascade!
  "Resume the production cascade-execution boundary from a sealed checkpoint.

  This deliberately does not call selection, spend a click, or claim a tick.
  It runs the same production execution function under run-phase! so telemetry,
  debugger stops, stores, validation and journal behaviour remain live."
  [{:keys [source-run-id source-attempt-id] :as opts}]
  (let [checkpoint (on-prod-history/load-checkpoint source-run-id
                                                    source-attempt-id)
        entry (:selected-entry checkpoint)
        run-id (or (:run-id opts)
                   (str "on-prod-history-" (UUID/randomUUID)))
        phase-opts (assoc opts :run-id run-id)
        context {:run-id run-id
                 :attempt-id (str "resume-" source-attempt-id)
                 :trigger :on-prod-potted-history
                 :source-run-id source-run-id
                 :source-attempt-id source-attempt-id}
        result (runner/run-phase!
                phase-opts context :cascade-execution
                #(cascade-decision/execute-selected-cascade! entry phase-opts)
                #(select-keys % [:status :kind :cascade-digest
                                 :compiled-digest]))]
    {:schema :wm/on-prod-history-result-v1
     :run-id run-id
     :source-run-id source-run-id
     :source-attempt-id source-attempt-id
     :selection-ran false
     :clicks-spent 0
     :result (cascade-decision/persistable-cascade-execution result)}))

(defn continue-on-prod-cascade!
  "Resume a sealed production selection through cascade compilation and then
  consume its certified frontier through the verified continuation bridge.
  This runs in the serving JVM, never selects, and never spends a click.

  Production callers must supply :cascade-capability-executors and
  :cascade-close-fn. Their absence is a typed frontier, never synthetic
  success."
  [{:keys [cascade-capability-executors cascade-close-fn] :as opts}]
  (let [;; Ordinary run-opportunity! installs this context before any Agency
        ;; contact. A selection-free production continuation must do the same,
        ;; otherwise the first capability dispatch fails outside the ledger.
        opts (cond-> opts
               (nil? (:click-id opts))
               (assoc :click-id
                      (str "potted-history:" (:source-run-id opts) "/"
                           (:source-attempt-id opts)))
               (nil? (:registered-run/job-ledger opts))
               (assoc :registered-run/job-ledger
                      (atom {:order [] :jobs {}}))
               (nil? (:job-liveness/state opts))
               (assoc :job-liveness/state (atom [])))
        resumed (resume-on-prod-cascade! opts)
        ready (:result resumed)]
    (if-not (= :cascade-ready (:status ready))
      (assoc resumed :continuation
             {:status :error :kind :continuation-cascade-not-ready
              :repair {:result ready}})
      (assoc resumed :continuation
             (cascade-continuation/continue-certified-cascade!
              {:ready ready
               :executable-cascade-root
               (or (:executable-cascade-root opts)
                   cascade-decision/default-executable-cascade-root)
               :execution-journal-root
               (or (:execution-journal-root opts)
                   cascade-decision/default-execution-journal-root)
               :executors (or cascade-capability-executors
                              (production-cascade-executors opts))
               :close-fn
               (or cascade-close-fn
                   (cascade-continuation/production-close-adapter
                    {:root (or (:cascade-continuation-close-root opts)
                               default-cascade-continuation-close-root)
                     :source-run-id (:source-run-id opts)
                     :source-attempt-id (:source-attempt-id opts)}))})))))
