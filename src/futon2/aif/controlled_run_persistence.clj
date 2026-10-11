(ns futon2.aif.controlled-run-persistence
  "Production-owned persistence boundary for a validated controlled closed run.

  This composes the existing trace, run-record, and Morning Brief writers. It
  never selects, dispatches, publishes repairs, or consults default paths."
  (:require [clojure.java.io :as io]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.morning-brief :as brief]
            [futon2.aif.run-record-io :as run-record-io]
            [futon2.aif.trace :as trace]))

(def schema :wm/controlled-closed-run-persistence-v1)

(defn- canonical [path] (.getCanonicalPath (io/file path)))
(defn- below? [root path]
  (let [root (str (canonical root) java.io.File/separator)
        path (canonical path)]
    (.startsWith path root)))

(defn- validate-paths! [sut-root paths]
  (when-not (and sut-root
                 (= #{:trace-dir :checkpoint-dir :run-record-dir :morning-brief-root}
                    (set (keys paths)))
                 (every? #(below? sut-root %) (vals paths)))
    (throw (ex-info "Controlled persistence paths escape the SUT root"
                    {:kind :controlled-path-isolation-failed
                     :sut-root (some-> sut-root canonical) :paths paths})))
  (when (some #{(canonical runner/default-run-record-dir)
                (canonical trace/default-trace-dir)
                (canonical brief/default-root)}
              (map canonical (vals paths)))
    (throw (ex-info "Controlled persistence named a production default"
                    {:kind :production-default-path-refused :paths paths}))))

(defn persist-closed-run!
  "Persist a validated `:wm/closed-run-v1` without invoking the live runner.
  WRITER-OPTS are the same explicit states/functions consumed by the existing
  run-record builder. The checkpoint writer remains caller-supplied because
  checkpoint validity is owned by the preregistered cohort."
  [{:keys [schema run-id started-at result trace-output trace-date brief-item
           checkpoint-write] :as closed-run}
    {:keys [sut-root paths writer-opts]}]
  (validate-paths! sut-root paths)
  (when-not (and (= :wm/closed-run-v1 schema) run-id started-at (map? result)
                 trace-output brief-item checkpoint-write)
    (throw (ex-info "Invalid controlled closed-run carrier"
                    {:kind :closed-run-carrier-invalid
                     :present (set (keys closed-run))})))
  (let [checkpoint (checkpoint-write (:checkpoint-dir paths) closed-run)
        trace-write (trace/write-trace! trace-output :dir (:trace-dir paths)
                                        :date-str trace-date :return-record? true)
        result (assoc result :trace-path (:path trace-write))
        run-write (runner/persist-run-record!
                   (assoc writer-opts
                          :run-record-dir (:run-record-dir paths)
                          :observation-labels-path
                          (str (io/file sut-root "observation-labels.edn")))
                   run-id started-at result)
        brief-path (brief/queue-item! (:morning-brief-root paths) brief-item)
        record (run-record-io/hydrate-run-record (:run-record run-write))
        traces (trace/read-trace :dir (:trace-dir paths) :date-str trace-date)
        briefs (brief/items (:morning-brief-root paths))]
    {:schema schema :status :persisted
     :checkpoint checkpoint :trace trace-write :run-record run-write
     :morning-brief brief-path
     :hydrated {:run-record record :trace (last traces) :morning-brief (last briefs)}
     :report-card {:status :absent :reason :writer-not-in-futon2}}))
