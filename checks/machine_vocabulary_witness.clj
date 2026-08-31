#!/usr/bin/env bb
(ns checks.machine-vocabulary-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))

(def root "/home/joe/code/mathlib4")
(def specs
  {:control ["holes/labs/wm-contract/control-vocabulary-reference.edn"
             :control-vocabulary-reference/v1 "ControlVocabularyNegative.lean"]
   :aliveness ["holes/labs/wm-contract/aliveness-reference.edn"
               :aliveness-reference/v1 "AlivenessNegative.lean"]
   :act-gate ["holes/labs/wm-contract/act-gate-reference.edn"
              :act-gate-reference/v1 "ActGateNegative.lean"]
   :cohort ["holes/labs/wm-contract/cohort-reference.edn"
            :cohort-reference/v1 "CohortNegative.lean"]})
(defn lean [f] (:exit (p/shell {:dir root :continue true :out :string :err :string}
                               "lake" "env" "lean" (str "DarkTower/WarMachine/" f))))
(defn -main [& args]
  (let [term (keyword (or (second (drop-while #(not= % "--term") args)) ""))
        neg? (some #((set ["--negative" "--negative-control"]) %) args)
        [path schema negative] (get specs term)
        fixture (when path (edn/read-string (slurp path)))
        shape? (= schema (:schema fixture))
        ok? (and shape? (zero? (lean "MachineVocabularyWitness.lean"))
                 (or (not neg?) (not (zero? (lean negative)))))
        exit (if ok? 0 (if neg? 2 1))]
    (println "machine-vocabulary-witness" term
             (if ok? (if neg? "negative-control PASS" "PASS") "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))
(apply -main *command-line-args*)
