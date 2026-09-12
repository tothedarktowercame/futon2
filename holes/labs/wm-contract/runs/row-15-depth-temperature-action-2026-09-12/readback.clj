(ns row-15-depth-temperature-action.readback
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.policy :as policy]))

(def trace-path "data/wm-trace/wm-trace-2026-09-04.edn")

(defn first-record []
  (with-open [reader (java.io.PushbackReader. (io/reader trace-path))]
    (edn/read {:eof nil} reader)))

(defn measured []
  (let [record (first-record)
        decision (:decision record)
        g-totals (mapv :controller-score (:controller-ranking decision))
        opts {:tau-mode :selection-gain-only}
        spread (policy/adaptive-temperature g-totals opts)
        tau (policy/effective-temperature g-totals (:selection-gain decision) opts)]
    {:schema :wm/row-15-measurement-v1
     :trace {:path trace-path
             :sha256 "f343432d772986ddc2f7fe38107913afc997201ebae9b6240dff152e235b1120"
             :record-index 0}
     :temperature
     {:declaration "DarkTower.WarMachine.MachineTemperature.machineTemperature"
      :production-entrypoints
      ["futon2.aif.policy/adaptive-temperature"
       "futon2.aif.policy/effective-temperature"]
      :inputs {:controller-score-count (count g-totals)
               :selection-gain (:selection-gain decision)
               :tau-mode (:tau-source decision)}
      :coordinates
      {:tau-spread {:retained (:tau-spread decision) :readback spread
                    :delta (- spread (:tau-spread decision))}
       :tau {:retained (:tau decision) :lean-reference 1.0 :readback tau
             :production-delta (- tau (:tau decision))
             :lean-delta (- tau 1.0)}}}
     :not-reconstructible
     {:depth
      {:declaration "DarkTower.WarMachine.MachineDepth.machineDepth"
       :missing-retention
       [:requested-policy-depth-config
        :effective-efe-horizon-steps]
       :detail "The trace retains the anticipation snapshot, but not depth-config or the effective :horizon-steps passed to rank-actions."}
      :action
      {:declaration "DarkTower.WarMachine.MachineAction.machineAction"
       :missing-retention
       [:ranked-action-input-maps :selector-option-packet]
       :detail "The trace retains summary rankings and the decision, but not the full ranked-actions maps consumed by select-action nor its complete opts."}}}))

(let [result (measured)]
  (assert (= 146 (get-in result [:temperature :inputs :controller-score-count])))
  (assert (zero? (get-in result [:temperature :coordinates :tau-spread :delta])))
  (assert (zero? (get-in result [:temperature :coordinates :tau :production-delta])))
  (assert (zero? (get-in result [:temperature :coordinates :tau :lean-delta])))
  (prn result))
