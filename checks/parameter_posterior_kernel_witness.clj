#!/usr/bin/env bb
(ns checks.parameter-posterior-kernel-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/parameter-posterior-kernel-reference.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected
  {:schema :parameter-posterior-kernel-reference/v1
   :basis "sec-glossary.tex:29 — Q(theta|o,pi) is normalized for every joint observation/policy input"
   :kind :hand-derived-total-deterministic-rows
   :policies [:inspect :repair]
   :observations [:clear :blocked]
   :parameters [:cautious :bold]
   :rows {[:inspect :clear] {:support [:cautious] :mass {:cautious 1}}
          [:inspect :blocked] {:support [:cautious] :mass {:cautious 1}}
          [:repair :clear] {:support [:cautious] :mass {:cautious 1}}
          [:repair :blocked] {:support [:cautious] :mass {:cautious 1}}}
   :excluded {:parameter-prior-q true :predictive-outcome-q true}})

(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))

(def negative-files
  {"--negative-prior" "DarkTower/WarMachine/ParameterPosteriorKernelPriorNegative.lean"
   "--negative-outcome" "DarkTower/WarMachine/ParameterPosteriorKernelOutcomeNegative.lean"})

(defn -main [& args]
  (let [negative-flag (some (set (keys negative-files)) args)
        fixture (edn/read-string (slurp fixture-path))
        positive? (and (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/ParameterPosteriorKernelWitness.lean")))
        rejected? (if negative-flag
                    (zero? (lean-exit (get negative-files negative-flag)))
                    true)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if negative-flag 2 1))]
    (println "parameter-posterior-kernel-witness:"
             (cond
               (and negative-flag ok?) (str "negative-control PASS (" (subs negative-flag 11) " rejected)")
               negative-flag "mutation slipped"
               ok? "PASS"
               :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
