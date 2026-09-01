#!/usr/bin/env bb
(ns checks.model-reduction-free-energy-change-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/model-reduction-free-energy-change-reference.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected
  {:schema :model-reduction-free-energy-change-reference/v1
   :basis "sec-glossary.tex:62 — delta-F is the four Dirichlet log-normalizer terms"
   :kind :hand-derived-from-gamma-identities
   :inputs {:A [1 1] :reduced-prior [1 1] :prior [2 1] :reduced-posterior [1 1]}
   :log-beta-identities {"B(1,1)" 0 "B(2,1)" :negative-log-2}
   :expected-change :log-2
   :excluded-quantity :variational-free-energy})

(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))

(def negative-files
  {"--negative-value" "DarkTower/WarMachine/ModelReductionFreeEnergyChangeValueNegative.lean"
   "--negative-type" "DarkTower/WarMachine/ModelReductionFreeEnergyChangeTypeNegative.lean"})

(defn -main [& args]
  (let [negative-flag (some (set (keys negative-files)) args)
        fixture (edn/read-string (slurp fixture-path))
        positive? (and (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/ModelReductionFreeEnergyChangeWitness.lean")))
        rejected? (if negative-flag
                    (if (= negative-flag "--negative-type")
                      (zero? (lean-exit (get negative-files negative-flag)))
                      (not (zero? (lean-exit (get negative-files negative-flag)))))
                    true)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if negative-flag 2 1))]
    (println "model-reduction-free-energy-change-witness:"
             (cond
               (and negative-flag ok?) (str "negative-control PASS (" (subs negative-flag 11) " rejected)")
               negative-flag "mutation slipped"
               ok? "PASS"
               :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
