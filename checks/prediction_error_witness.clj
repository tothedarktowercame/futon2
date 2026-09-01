#!/usr/bin/env bb
(ns checks.prediction-error-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/prediction-error-reference.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected
  {:schema :prediction-error-reference/v1
   :basis "sec-glossary.tex:15 — epsilon_k = o_k - mu_k"
   :kind :hand-derived-from-formula
   :observation 1 :prediction 3 :expected-error -2})

(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))

(defn -main [& args]
  (let [operand-neg? (some #{"--negative-operand"} args)
        sign-neg? (some #{"--negative-sign"} args)
        fixture (edn/read-string (slurp fixture-path))
        tested (if operand-neg? (assoc fixture :expected-error (:observation fixture)) fixture)
        positive? (and (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/PredictionErrorWitness.lean")))
        rejected? (cond
                    operand-neg? (not= expected tested)
                    sign-neg? (zero? (lean-exit "DarkTower/WarMachine/PredictionErrorNegative.lean"))
                    :else true)
        negative? (or operand-neg? sign-neg?)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if negative? 2 1))]
    (println "prediction-error-witness:"
             (cond
               (and operand-neg? ok?) "negative-control PASS (observation-as-error rejected)"
               (and sign-neg? ok?) "negative-control PASS (reversed sign rejected)"
               negative? "mutation slipped"
               ok? "PASS"
               :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
