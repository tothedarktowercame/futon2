(ns futon2.aif.full-loop-runtime
  "Production composition root for the full-loop runner."
  (:require [futon2.aif.cascade-feedback :as cascade-feedback]
            [futon2.aif.cascade-revision-producer :as cascade-revision-producer]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.flight-runner :as flight-runner]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-live-outer-selector :as meta-live]
            [futon2.aif.outer-learning-manifest :as outer-learning-manifest]
            [futon2.aif.c-vector :as cv]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.selection-timing :as selection-timing]
            [futon2.aif.selected-target-reading :as selected-target-reading]
            [futon2.aif.occurrence-square-authoring :as square-authoring]
            [futon2.aif.selected-reading :as selected-reading]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.wm.click-ask :as click-ask]
            [futon2.aif.wm.open-cascade-resume :as open-resume]
            [futon2.report.war-machine :as wm])
  (:import [java.time Instant]
           [java.util UUID]))

(load-identity/register! *ns* *file*)

(def default-pattern-graph-path
  "/home/joe/code/storage/operator-turns/mined-pattern-graph.json")
(def default-pattern-graph-diff-dir
  "/home/joe/code/futon2/data/wm-pattern-graph-diffs")

(defn- designated-agent-answer-fn
  "Agency answer port for machine requests.  It contacts exactly the
  configured author and returns a typed no-contact when that seat is not
  currently invoke-ready; it never consults a fallback roster seat."
  [opts seat prompt-fn invoke-mode]
  (let [author seat
        agency-base (or (:agency-base opts) runner/default-agency-base)
        answer (flight-runner/agency-answer-fn
                {:seat author :caller "wm-full-loop"
                 :opts (assoc opts :invoke-mode (or invoke-mode "brief"))
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
  [opts]
  (let [author (:author opts)
        reviewer (:reviewer opts)
        author-contact (or (:open-cascade-author-answer-fn opts)
                           (designated-agent-answer-fn
                            opts author open-cascade-author-prompt "work"))
        reviewer-contact (or (:open-cascade-reviewer-answer-fn opts)
                             (designated-agent-answer-fn
                              opts reviewer open-cascade-review-prompt "work"))]
    (fn [error]
      (let [ctx (open-resume/recognize error)
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
               {:patch-loader (or (:open-cascade-patch-loader opts)
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
          ;; Construction publishes below. Do not publish twice.
          {:trace? false :include-advisory-lanes? false :defer-render? true})))

(defn production-defaults
  "Build the report-backed functions for one production invocation."
  [opts]
  (let [outer-selector (or (:outer-task-selection-fn opts) meta-live/selector)
        ;; A click may invoke the report judge twice when an interpretation ask
        ;; publishes missing construction input.  Outer state can change while
        ;; that ask runs (including the graph and learning manifest), but the
        ;; second pass is inner cascade reconstruction for the same chosen
        ;; outer action, not a new outer opportunity. Retain the exact first receipt so query-time
        ;; evidence is requested for, and consumed by, one stable target.
        outer-receipt (atom ::unselected)
        pinned-outer-selector
        (fn [input]
          (let [retained @outer-receipt]
            (if (not= ::unselected retained)
              retained
              (let [selected (outer-selector input)]
                (reset! outer-receipt selected)
                selected))))
        opts (-> opts
                 (update :outer-task-policy #(or % :meta))
                 (assoc :outer-task-selection-fn pinned-outer-selector))
        selected-judgment (atom nil)]
    {:judge-fn (fn [days]
                 (try
                   (let [generated (selection-judge opts days)]
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
         (selected-target-reading/repair!
          (assoc live-opts :answer-fn answer-fn)
          judgement)))
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

(defn run-opportunity!
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
