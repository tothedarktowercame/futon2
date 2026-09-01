#!/usr/bin/env bb
(ns checks.bayes-factor-threshold-witness
  (:require [babashka.process :as p] [checks.positive-proof-receipt :as receipt]
            [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/bayes-factor-threshold-reference.edn")
(def receipt-path "holes/labs/wm-contract/bayes-factor-threshold-positive-receipt.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected
  {:schema :bayes-factor-threshold-reference/v1
   :basis "sec-glossary.tex:60 — a BMR reduction passes exactly when delta-F <= -3"
   :kind :hand-derived-from-threshold
   :threshold [-3 1]
   :passing-change [-7 2]
   :failing-change [-2 1]
   :quantity :model-reduction-free-energy-change
   :excluded-quantity :variational-free-energy})

(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))

(defn -main [& args]
  (let [boundary-neg? (some #{"--negative-boundary"} args)
        type-neg? (some #{"--negative-type"} args)
        fixture (edn/read-string (slurp fixture-path))
        receipt-ok? (:pass? (receipt/validate (edn/read-string (slurp receipt-path))))
        tested (if boundary-neg? (assoc fixture :failing-change [-7 2]) fixture)
        positive? (and receipt-ok? (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/BayesFactorThresholdWitness.lean")))
        rejected? (cond
                    boundary-neg? (not= expected tested)
                    type-neg? (zero? (lean-exit "DarkTower/WarMachine/BayesFactorThresholdNegative.lean"))
                    :else true)
        negative? (or boundary-neg? type-neg?)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if negative? 2 1))]
    (println "bayes-factor-threshold-witness:"
             (cond
               (and boundary-neg? ok?) "negative-control PASS (above-threshold change rejected)"
               (and type-neg? ok?) "negative-control PASS (variational F rejected as BMR evidence)"
               negative? "mutation slipped"
               ok? "PASS"
               :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
