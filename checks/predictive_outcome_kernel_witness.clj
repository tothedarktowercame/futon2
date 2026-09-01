#!/usr/bin/env bb
(ns checks.predictive-outcome-kernel-witness
  (:require [babashka.process :as p] [checks.positive-proof-receipt :as receipt] [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/predictive-outcome-kernel-reference.edn")
(def receipt-path "holes/labs/wm-contract/predictive-outcome-kernel-positive-receipt.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected
  {:schema :predictive-outcome-kernel-reference/v1
   :basis "sec-glossary.tex:21-29 — Q(o|pi) is a normalized outcome distribution for each policy"
   :kind :hand-derived-deterministic-policy-rows
   :policies [:inspect :repair]
   :rows {:inspect {:support [:clear] :mass {:clear 1}}
          :repair {:support [:fixed] :mass {:fixed 1}}}
   :excluded {:unconditional-outcome-distribution true
              :softmax-policy-vector true}})

(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))

(def negative-files
  {"--negative-unconditional" "DarkTower/WarMachine/PredictiveOutcomeKernelUnconditionalNegative.lean"
   "--negative-softmax" "DarkTower/WarMachine/PredictiveOutcomeKernelSoftmaxNegative.lean"})

(defn -main [& args]
  (let [negative-flag (some (set (keys negative-files)) args)
        fixture (edn/read-string (slurp fixture-path))
        positive? (and (:pass? (receipt/validate (edn/read-string (slurp receipt-path)))) (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/PredictiveOutcomeKernelWitness.lean")))
        rejected? (if negative-flag
                    (zero? (lean-exit (get negative-files negative-flag)))
                    true)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if negative-flag 2 1))]
    (println "predictive-outcome-kernel-witness:"
             (cond
               (and negative-flag ok?) (str "negative-control PASS (" (subs negative-flag 11) " rejected)")
               negative-flag "mutation slipped"
               ok? "PASS"
               :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
