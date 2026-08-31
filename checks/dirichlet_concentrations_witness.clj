#!/usr/bin/env bb
(ns checks.dirichlet-concentrations-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/dirichlet-concentrations-reference.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected
  {:schema :dirichlet-concentrations-reference/v1
   :basis "sec-glossary.tex:56 — Dirichlet concentrations are nonempty and strictly positive"
   :kind :hand-derived-from-domain
   :accepted [2 1]
   :rejected {:empty [] :zero [1 0] :negative [1 -1]}})

(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))

(def negative-files
  {"--negative-empty" "DarkTower/WarMachine/DirichletConcentrationsEmptyNegative.lean"
   "--negative-zero" "DarkTower/WarMachine/DirichletConcentrationsZeroNegative.lean"
   "--negative-negative" "DarkTower/WarMachine/DirichletConcentrationsNegative.lean"})

(defn -main [& args]
  (let [negative-flag (some (set (keys negative-files)) args)
        fixture (edn/read-string (slurp fixture-path))
        positive? (and (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/DirichletConcentrationsWitness.lean")))
        rejected? (if negative-flag
                    (not (zero? (lean-exit (get negative-files negative-flag))))
                    true)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if negative-flag 2 1))]
    (println "dirichlet-concentrations-witness:"
             (cond
               (and negative-flag ok?) (str "negative-control PASS (" (subs negative-flag 11) " rejected)")
               negative-flag "mutation slipped"
               ok? "PASS"
               :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
