#!/usr/bin/env bb
(ns checks.belief-state-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))

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
  (zero? (:exit (p/shell {:dir "/home/joe/code/mathlib4" :continue true
                          :out :string :err :string}
                         "lake" "env" "lean"
                         "DarkTower/WarMachine/BeliefStateWitness.lean"))))
(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp fixture-path))
        tested (if negative? (update-in fixture [:states 1 :channels :loop-health]
                                               dissoc :variance) fixture)
        accepted? (and (fixture-valid? tested) (or negative? (lean-pass?)))
        exit (cond (and negative? accepted?) 2 negative? 0 accepted? 0 :else 1)]
    (println "belief-state-witness:"
             (cond (= exit 2) "mutation slipped"
                   negative? "negative-control PASS (missing channel variance rejected)"
                   accepted? "PASS" :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))
(apply -main *command-line-args*)
