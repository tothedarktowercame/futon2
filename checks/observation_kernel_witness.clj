#!/usr/bin/env bb
(ns checks.observation-kernel-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))
(def fixture-path "holes/labs/wm-contract/observation-kernel-reference.edn")
(defn valid-row? [row]
  (let [masses (vals (:mass row))]
    (and (seq masses) (every? #(and (number? %) (not (neg? %))) masses)
         (= 1 (reduce + masses)))))
(defn valid? [x]
  (and (= :observation-kernel-reference/v1 (:schema x))
       (= "holes/labs/wm-contract/C7-belief-update-findings.md" (:basis x))
       (seq (:rows x)) (every? valid-row? (:rows x))))
(defn lean-pass? []
  (zero? (:exit (p/shell {:dir "/home/joe/code/mathlib4" :continue true
                          :out :string :err :string}
                         "lake" "env" "lean"
                         "DarkTower/WarMachine/ObservationKernelWitness.lean"))))
(defn -main [& args]
  (let [sum-neg? (some #{"--negative-normalisation" "--negative-control"} args)
        mass-neg? (some #{"--negative-mass"} args)
        fixture (edn/read-string (slurp fixture-path))
        tested (cond sum-neg? (assoc-in fixture [:rows 0 :mass :present] 3/4)
                     mass-neg? (assoc-in fixture [:rows 0 :mass :present] -1/2)
                     :else fixture)
        negative? (or sum-neg? mass-neg?)
        accepted? (and (valid? tested) (or negative? (lean-pass?)))
        exit (cond (and negative? accepted?) 2 negative? 0 accepted? 0 :else 1)]
    (println "observation-kernel-witness:"
             (cond (= exit 2) "mutation slipped"
                   sum-neg? "negative-control PASS (non-normalized row rejected)"
                   mass-neg? "negative-control PASS (negative mass rejected)"
                   accepted? "PASS" :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))
(apply -main *command-line-args*)
