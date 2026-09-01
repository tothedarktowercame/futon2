#!/usr/bin/env bb
(ns checks.policy-prior-kernel-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))
(def fixture-path "holes/labs/wm-contract/policy-prior-reference.edn")
(def mathlib "/home/joe/code/mathlib4")
(defn valid? [x]
  (and (= :policy-prior-reference/v1 (:schema x))
       (= :unit (:conditioning-domain x))
       (= [:inspect :repair] (:allowable-policies x))
       (= 1 (reduce + (vals (:mass x))))
       (every? #(not (neg? %)) (vals (:mass x)))))
(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))
(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp fixture-path))
        tested (if negative? (assoc fixture :conditioning-domain :hidden-state) fixture)
        positive? (and (valid? fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/PolicyPriorKernelWitness.lean")))
        rejected? (and (not (valid? tested))
                       (zero? (lean-exit "DarkTower/WarMachine/PolicyPriorKernelNegative.lean")))
        accepted? (if negative? (and positive? rejected?) positive?)
        exit (if accepted? 0 (if negative? 2 1))]
    (println "policy-prior-kernel-witness:"
             (cond (and negative? accepted?) "negative-control PASS (state-conditioned kernel rejected)"
                   negative? "mutation slipped"
                   accepted? "PASS" :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))
(apply -main *command-line-args*)
