#!/usr/bin/env bb
(ns checks.precision-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/precision-reference.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected
  {:schema :precision-reference/v1
   :basis "sec-glossary.tex:17,19 — precision multiplicatively weights squared prediction error"
   :kind :hand-derived-from-formula
   :channel-count 14
   :weighted {:precision 2 :prediction-error 1 :expected-variational-F 1}
   :swapped {:precision 1 :prediction-error 2 :expected-variational-F 2}})

(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))

(defn -main [& args]
  (let [swap-neg? (some #{"--negative-swap"} args)
        type-neg? (some #{"--negative-type"} args)
        fixture (edn/read-string (slurp fixture-path))
        tested (if swap-neg? (assoc-in fixture [:swapped :expected-variational-F] 1) fixture)
        positive? (and (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/PrecisionWitness.lean")))
        rejected? (cond
                    swap-neg? (not= expected tested)
                    type-neg? (zero? (lean-exit "DarkTower/WarMachine/PrecisionNegative.lean"))
                    :else true)
        negative? (or swap-neg? type-neg?)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if negative? 2 1))]
    (println "precision-witness:"
             (cond
               (and swap-neg? ok?) "negative-control PASS (swapped weighting rejected)"
               (and type-neg? ok?) "negative-control PASS (signed error map rejected as precision)"
               negative? "mutation slipped"
               ok? "PASS"
               :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
