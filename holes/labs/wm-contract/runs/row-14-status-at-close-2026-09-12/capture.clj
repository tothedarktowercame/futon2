(ns capture
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def output-path
  "holes/labs/wm-contract/runs/row-14-status-at-close-2026-09-12/readback.edn")

(def preregistration
  "/home/joe/code/futon2/holes/labs/M-aif-full-loop-40/cohort.edn")

(def row
  {:strengthened 0.4 :addressed 0.1 :falsified 0.1 :reopened 0.1
   :spawned 0.1 :refined 0.1 :foreclosed 0.1})

(defn term [judgment]
  {:judgment judgment :ground {:kind :redirected-machinery-capture}})

(defn -main [& _]
  (let [root (.getPath (.toFile (Files/createTempDirectory
                                 "row14-close-readback-"
                                 (make-array FileAttribute 0))))
        _ (cohort/activate! preregistration root)
        attempt (:attempt/id
                 (cohort/start-attempt!
                  preregistration root
                  (term {:opportunity-id "row14/redirected"
                         :trigger :wallclock-cron
                         :machine-state {:tick 1}
                         :agent-roster []
                         :code-state {:git-sha "d30ed68f"
                                      :git-dirty? false
                                      :resolved-mode-flags {}
                                      :configuration-digest "row14-capture"}
                         :semantic-epoch :row14})))
        _ (doseq [checkpoint [:selection :construction :dispatch :build :adjudication]]
            (cohort/append-checkpoint!
             preregistration root attempt checkpoint
             {:sorry {:kind (keyword (str "capture-" (name checkpoint)))}}))
        state (runner/entity-state-at-close
               "entity-1" {"entity-1" row}
               {:run/id "row14/redirected" :trace-path "redirected://wm-trace/1"}
               "2026-09-12T20:50:00Z")
        _ (cohort/close-attempt!
           preregistration root attempt
           (term {:outcome :agent-unavailable :grounded? false
                  :artifact-only? false :duration-ms 1
                  :resource-use {:agent-turns 0}
                  :entity-state-at-close state}))
        close-file (io/file root "run4-successor-v2-selection-admission-20260911-v1"
                            attempt "007-closed.edn")
        persisted (edn/read-string (slurp close-file))
        readback {:source :redirected-machinery-close
                  :live-write? false
                  :attempt-id attempt
                  :entity-state-at-close
                  (get-in persisted [:cell :judgment :entity-state-at-close])}]
    (spit output-path (str (pr-str readback) "\n"))
    (prn readback)))

(apply -main *command-line-args*)
