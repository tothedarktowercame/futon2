#!/usr/bin/env bb
(ns checks.predictive-outcome-risk-witness
  (:require [babashka.process :as p] [checks.positive-proof-receipt :as receipt]
            [clojure.edn :as edn]))
(def fixture-path "holes/labs/wm-contract/predictive-outcome-risk-reference.edn")
(def receipt-path "holes/labs/wm-contract/predictive-outcome-risk-positive-receipt.edn")
(defn valid? [x]
  (and (= :predictive-outcome-risk-reference/v1 (:schema x))
       (= 1 (reduce + (vals (:predictive-mass x))))
       (= 1 (reduce + (vals (:preference-mass x))))
       (every? (fn [[o mass]]
                 (or (zero? mass) (pos? (get (:preference-mass x) o 0))))
               (:predictive-mass x))
       (= [:log 2] (:expected-risk x))))
(defn lean-pass? []
  (zero? (:exit (p/shell {:dir "/home/joe/code/mathlib4" :continue true
                          :out :string :err :string}
                         "lake" "env" "lean"
                         "DarkTower/WarMachine/PredictiveOutcomeRiskWitness.lean"))))
(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp fixture-path))
        tested (if negative? (assoc-in fixture [:preference-mass :a] 0) fixture)
        receipt-ok? (:pass? (receipt/validate (edn/read-string (slurp receipt-path))))
        baseline-valid? (and receipt-ok? (valid? fixture) (lean-pass?))
        mutation-rejected? (not (valid? tested))
        exit (cond (and negative? (not baseline-valid?)) 1
                   (and negative? (not mutation-rejected?)) 2
                   negative? 0 baseline-valid? 0 :else 1)]
    (println "predictive-outcome-risk-witness:"
             (cond (and negative? (not baseline-valid?)) "BASELINE-INVALID (control reason not established)"
                   (= exit 2) "mutation slipped"
                   negative? "negative-control PASS (zero preference on predictive support rejected)"
                   baseline-valid? "PASS" :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))
(apply -main *command-line-args*)
