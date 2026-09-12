(ns capture
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def preregistration
  "/home/joe/code/futon2/holes/labs/M-aif-full-loop-40/cohort.edn")

(def output-path
  "holes/labs/wm-contract/runs/row-14-entity-identity-2026-09-12/readback.edn")

(defn term [judgment]
  {:judgment judgment :ground {:kind :redirected-machinery-capture}})

(defn -main [& _]
  (let [root (.getPath (.toFile (Files/createTempDirectory
                                 "row14-entity-close-"
                                 (make-array FileAttribute 0))))
        prereg (edn/read-string (slurp preregistration))
        cohort-id (name (:cohort/id prereg))
        _ (cohort/activate! preregistration root)
        attempt (:attempt/id
                 (cohort/start-attempt!
                  preregistration root
                  (term {:opportunity-id "row14/entity-redirected"
                         :trigger :wallclock-cron
                         :machine-state {:tick 1}
                         :agent-roster []
                         :code-state {:git-sha "COMMITTED-BEFORE-CAPTURE"
                                      :git-dirty? false
                                      :resolved-mode-flags {}
                                      :configuration-digest "row14-entity"}
                         :semantic-epoch :row14})))
        _ (doseq [checkpoint [:selection :construction :dispatch :build :adjudication]]
            (cohort/append-checkpoint!
             preregistration root attempt checkpoint
             {:sorry {:kind (keyword (str "capture-" (name checkpoint)))}}))
        state {:entity/id "entity-1"}
        identity (runner/outcome-entity-at-close
                  {:selection-reached? true :selection-made? true
                   :entity-id "entity-1"}
                  state)
        _ (cohort/close-attempt!
           preregistration root attempt
           (term {:outcome :agent-unavailable :grounded? false
                  :artifact-only? false :duration-ms 1
                  :resource-use {:agent-turns 0}
                  :outcome-entity identity}))
        close-file (io/file root cohort-id attempt "007-closed.edn")
        persisted (edn/read-string (slurp close-file))
        readback {:source :redirected-machinery-close
                  :live-write? false
                  :attempt-id attempt
                  :outcome-entity
                  (get-in persisted [:payload :judgment :outcome-entity])}]
    (spit output-path (str (pr-str readback) "\n"))
    (prn readback)))

(apply -main *command-line-args*)
