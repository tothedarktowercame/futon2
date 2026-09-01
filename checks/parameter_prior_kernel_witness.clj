#!/usr/bin/env bb
(ns checks.parameter-prior-kernel-witness
  (:require [babashka.process :as p] [checks.positive-proof-receipt :as receipt] [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/parameter-prior-kernel-reference.edn")
(def receipt-path "holes/labs/wm-contract/parameter-prior-kernel-positive-receipt.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected
  {:schema :parameter-prior-kernel-reference/v1
   :basis "sec-glossary.tex:29 — Q(theta|pi) is a normalized parameter distribution for each policy"
   :kind :hand-derived-deterministic-policy-rows
   :policies [:inspect :repair]
   :parameters [:cautious :bold]
   :rows {:inspect {:support [:cautious] :mass {:cautious 1}}
          :repair {:support [:bold] :mass {:bold 1}}}
   :excluded {:predictive-outcome-q true :policy-habit-q true}})

(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))

(def negative-files
  {"--negative-outcome" "DarkTower/WarMachine/ParameterPriorKernelOutcomeNegative.lean"
   "--negative-habit" "DarkTower/WarMachine/ParameterPriorKernelHabitNegative.lean"})

(defn -main [& args]
  (let [negative-flag (some (set (keys negative-files)) args)
        fixture (edn/read-string (slurp fixture-path))
        positive? (and (:pass? (receipt/validate (edn/read-string (slurp receipt-path)))) (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/ParameterPriorKernelWitness.lean")))
        rejected? (if negative-flag
                    (zero? (lean-exit (get negative-files negative-flag)))
                    true)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if negative-flag 2 1))]
    (println "parameter-prior-kernel-witness:"
             (cond
               (and negative-flag ok?) (str "negative-control PASS (" (subs negative-flag 11) " rejected)")
               negative-flag "mutation slipped"
               ok? "PASS"
               :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
