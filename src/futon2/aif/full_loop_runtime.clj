(ns futon2.aif.full-loop-runtime
  "Production composition root for the full-loop runner."
  (:require [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.load-identity :as load-identity]
            [futon2.report.war-machine :as wm]))

(load-identity/register! *ns* *file*)

(defn- selection-judge
  [opts days]
  (wm/generate-war-machine
   days
   (merge (wm/accumulation-config)
          (select-keys opts [:accumulate-strategic-habit? :run-id
                             :loaded-code-identity :cascade-habit-path
                             :observation-labels-path :flight :trace-dir])
          ;; Construction publishes below. Do not publish twice.
          {:trace? false :include-advisory-lanes? false :defer-render? true})))

(defn production-defaults
  "Build the report-backed functions for one production invocation."
  [opts]
  {:judge-fn (fn [days] (selection-judge opts days))
   :scan-render-fn wm/render-war-machine
   :effective-run-configuration-fn wm/effective-run-configuration
   :mode-flags-fn wm/arena-mode-flags})

(defn run-opportunity!
  "Run one production opportunity with the report-backed defaults installed."
  [opts]
  (binding [runner/*runtime-defaults* (production-defaults opts)]
    (runner/run-opportunity! opts)))
