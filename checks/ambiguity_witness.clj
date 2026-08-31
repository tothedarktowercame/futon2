#!/usr/bin/env bb
(ns checks.ambiguity-witness
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.edn :as edn]))

(def root (fs/cwd))
(def fixture-path (fs/path root "holes/labs/wm-contract/ambiguity-reference.edn"))
(def expected-case
  {:id :point-mass-observation
   :predicted-state-masses [1]
   :observation-masses [1]
   :expected-ambiguity 0})

(defn fixture-valid? [x]
  (and (= :ambiguity-reference/v1 (:schema x))
       (= [expected-case] (:cases x))))

(defn lean-pass? []
  (zero? (:exit (process/shell
                  {:dir (str (fs/normalize (fs/path root "../mathlib4")))
                   :continue true :out :string :err :string}
                  "lake" "env" "lean"
                  "DarkTower/WarMachine/AmbiguityWitness.lean"))))

(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp (str fixture-path)))
        tested (if negative? (assoc-in fixture [:cases 0 :expected-ambiguity] 1) fixture)
        accepted? (and (fixture-valid? tested) (or negative? (lean-pass?)))
        exit (cond (and negative? accepted?) 2 negative? 0 accepted? 0 :else 1)]
    (println (cond
               (= exit 2) "ambiguity-witness: mutation slipped"
               negative? "ambiguity-witness: negative-control PASS (perturbed entropy rejected)"
               accepted? "ambiguity-witness: PASS"
               :else "ambiguity-witness: FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
