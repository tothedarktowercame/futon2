(ns futon2.aif.full-loop-runtime
  "Production composition root for the full-loop runner."
  (:require [futon2.aif.cascade-feedback :as cascade-feedback]
            [futon2.aif.cascade-revision-producer :as cascade-revision-producer]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-live-outer-selector :as meta-live]
            [futon2.aif.c-vector :as cv]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.selection-timing :as selection-timing]
            [futon2.aif.wm.click-ask :as click-ask]
            [futon2.report.war-machine :as wm])
  (:import [java.time Instant]
           [java.util UUID]))

(load-identity/register! *ns* *file*)

(def default-pattern-graph-path
  "/home/joe/code/storage/operator-turns/mined-pattern-graph.json")
(def default-pattern-graph-diff-dir
  "/home/joe/code/futon2/data/wm-pattern-graph-diffs")

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
                             :selection-timing/state :nano-time-fn])
          ;; Construction publishes below. Do not publish twice.
          {:trace? false :include-advisory-lanes? false :defer-render? true})))

(defn production-defaults
  "Build the report-backed functions for one production invocation."
  [opts]
  (let [opts (-> opts
                 (update :outer-task-policy #(or % :meta))
                 (update :outer-task-selection-fn #(or % meta-live/selector)))
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
        ;; The production judge closes over OPTS before the runner adds its
        ;; other ledgers. Mint this collector here and pass the same identity
        ;; through, rather than timing into a disconnected atom.
        opts (update opts :selection-timing/state
                     #(or % (selection-timing/new-state)))]
    (binding [runner/*runtime-defaults* (production-defaults opts)]
      (runner/run-opportunity! opts))))
