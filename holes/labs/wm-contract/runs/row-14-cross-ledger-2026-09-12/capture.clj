(ns capture
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.cross-ledger-identity :as identity]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.trace :as trace])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def preregistration
  "/home/joe/code/futon2/holes/labs/M-aif-full-loop-40/cohort.edn")
(def output
  "holes/labs/wm-contract/runs/row-14-cross-ledger-2026-09-12/readback.edn")
(defn term [judgment] {:judgment judgment :ground {:kind :redirected-capture}})

(defn -main [& _]
  (let [root (.getPath (.toFile (Files/createTempDirectory
                                 "row14-cross-ledger-"
                                 (make-array FileAttribute 0))))
        trace-dir (str root "/trace")
        prereg (edn/read-string (slurp preregistration))
        cohort-id (:cohort/id prereg)
        run-id "row14/cross-ledger/redirected"
        _ (cohort/activate! preregistration root)
        attempt (:attempt/id
                 (cohort/start-attempt!
                  preregistration root
                  (term {:opportunity-id run-id :trigger :wallclock-cron
                         :machine-state {:tick 1} :agent-roster []
                         :code-state {:git-sha "COMMITTED-BEFORE-CAPTURE"
                                      :git-dirty? false :resolved-mode-flags {}
                                      :configuration-digest "row14-cross-ledger"}
                         :semantic-epoch :row14})))
        cohort-attempt {:cohort/id cohort-id :attempt/id attempt}
        trace-result (trace/write-trace!
                      {:run/id run-id :cohort-attempt cohort-attempt
                       :belief {} :observation {} :free-energy {}
                       :ranked-actions [] :decision {} :mode :maintain}
                      :dir trace-dir :date-str "2026-09-12" :return-record? true)
        source {:run/id run-id :trace-path (:path trace-result)}
        _ (cohort/append-checkpoint!
           preregistration root attempt :selection
           (term {:selected-mission "entity-1" :selected-action
                  {:type :address-sorry :target "entity-1"}
                  :belief-source source}))
        _ (doseq [checkpoint [:construction :dispatch :build :adjudication]]
            (cohort/append-checkpoint! preregistration root attempt checkpoint
                                       {:sorry {:kind (keyword (str "capture-" (name checkpoint)))}}))
        _ (cohort/close-attempt!
           preregistration root attempt
           (term {:outcome :agent-unavailable :grounded? false
                  :artifact-only? false :duration-ms 1 :resource-use {:agent-turns 0}
                  :entity-state-at-close {:entity/id "entity-1"
                                          :belief-source source}}))
        close-file (io/file root (name cohort-id) attempt "007-closed.edn")
        selection-file (io/file root (name cohort-id) attempt "002-selection.edn")
        close-record (edn/read-string (slurp close-file))
        selection-record (edn/read-string (slurp selection-file))
        joined (identity/join-close-to-trace close-record [(:record trace-result)])
        readback {:live-write? false
                  :selection-belief-source
                  (get-in selection-record [:payload :judgment :belief-source])
                  :trace-run/id (get-in joined [:trace :run/id])
                  :trace-cohort-attempt (get-in joined [:trace :cohort-attempt])
                  :join-status (:status joined)}]
    (spit output (str (pr-str readback) "\n"))
    (prn readback)))

(apply -main *command-line-args*)
