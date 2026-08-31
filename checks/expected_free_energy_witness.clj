#!/usr/bin/env bb
(ns checks.expected-free-energy-witness
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.edn :as edn]))

(def root (fs/cwd))
(def mathlib-root (str (fs/normalize (fs/path root "../mathlib4"))))
(def fixture-path (fs/path root "holes/labs/wm-contract/expected-free-energy-reference.edn"))

(def independent-case
  {:id :one-point :predictive-masses [1] :preference-masses [1]
   :risk 0 :ambiguity 2 :epistemic-gain -2 :expected-free-energy 2})

(defn reference-valid? [x]
  (and (= :expected-free-energy-reference/v1 (:schema x))
       (= [independent-case] (:cases x))))

(defn lean-pass? []
  (zero? (:exit (process/shell
                  {:dir mathlib-root :continue true :out :string :err :string}
                  "lake" "env" "lean"
                  "DarkTower/WarMachine/ExpectedFreeEnergyWitness.lean"))))

(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp (str fixture-path)))
        tested (if negative? (assoc-in fixture [:cases 0 :expected-free-energy] 3) fixture)
        accepted? (and (reference-valid? tested) (or negative? (lean-pass?)))
        exit (cond (and negative? accepted?) 2 negative? 0 accepted? 0 :else 1)]
    (println (cond
               (= exit 2) "expected-free-energy-witness: mutation slipped"
               negative? "expected-free-energy-witness: negative-control PASS (perturbed value rejected)"
               accepted? "expected-free-energy-witness: PASS"
               :else "expected-free-energy-witness: FAIL"))
    (System/exit exit)))

(apply -main *command-line-args*)
