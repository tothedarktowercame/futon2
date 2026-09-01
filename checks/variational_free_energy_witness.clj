#!/usr/bin/env bb
(ns checks.variational-free-energy-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))
(def fixture-path "holes/labs/wm-contract/variational-free-energy-reference.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected {:schema :variational-free-energy-reference/v1
               :basis "sec-glossary.tex:19 — F = 1/2 mean_k(Pi_k epsilon_k^2)"
               :kind :hand-derived-from-formula :channel-count 14
               :precision 2 :prediction-error 1 :expected-variational-F 1})
(defn lean-exit [f]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" f)))
(defn -main [& args]
  (let [value-neg? (some #{"--negative" "--negative-value"} args)
        type-neg? (some #{"--negative-type"} args)
        fixture (edn/read-string (slurp fixture-path))
        tested (if value-neg? (assoc fixture :expected-variational-F 2) fixture)
        positive? (and (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/VariationalFreeEnergyWitness.lean")))
        rejected? (cond value-neg? (not= expected tested)
                        type-neg? (zero? (lean-exit "DarkTower/WarMachine/VariationalFreeEnergyNegative.lean"))
                        :else true)
        negative? (or value-neg? type-neg?)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if negative? 2 1))]
    (println "variational-free-energy-witness:"
             (cond (and value-neg? ok?) "negative-control PASS (perturbed F rejected)"
                   (and type-neg? ok?) "negative-control PASS (expected F type rejected)"
                   negative? "mutation slipped" ok? "PASS" :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))
(apply -main *command-line-args*)
