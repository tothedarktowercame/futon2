#!/usr/bin/env bb
(ns checks.belief-state-witness
  (:require [checks.lean-positive-witness :as lean-positive] [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/belief-state-reference.edn")
(defn state-valid? [state]
  (and (seq (:channels state))
       (every? (fn [[_ v]]
                 (and (number? (:mean v)) (number? (:variance v))
                      (not (neg? (:variance v)))))
               (:channels state))))
(defn fixture-valid? [x]
  (and (= :belief-state-reference/v1 (:schema x))
       (= "holes/labs/wm-contract/C31-variance-update-findings.md" (:basis x))
       (= #{:prior :posterior} (set (map :id (:states x))))
       (every? state-valid? (:states x))))
(defn lean-pass? []
  (lean-positive/pass? "/home/joe/code/mathlib4"
                       "DarkTower/WarMachine/BeliefStateWitness.lean"))
(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp fixture-path))
        tested (if negative? (update-in fixture [:states 1 :channels :loop-health]
                                               dissoc :variance) fixture)
        baseline-valid? (and (fixture-valid? fixture) (lean-pass?))
        mutation-rejected? (not (fixture-valid? tested))
        exit (cond (and negative? (not baseline-valid?)) 1
                   (and negative? (not mutation-rejected?)) 2
                   negative? 0 baseline-valid? 0 :else 1)]
    (println "belief-state-witness:"
             (cond (and negative? (not baseline-valid?)) "BASELINE-INVALID (control reason not established)"
                   (= exit 2) "mutation slipped"
                   negative? "negative-control PASS (missing channel variance rejected)"
                   baseline-valid? "PASS" :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))
(apply -main *command-line-args*)
