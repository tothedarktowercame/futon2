#!/usr/bin/env bb
(ns checks.expected-free-energy-witness
  (:require [babashka.fs :as fs]
            [checks.lean-positive-witness :as lean-positive]
            [checks.positive-proof-receipt :as receipt]
            [clojure.edn :as edn]))

(def root (fs/cwd))
(def mathlib-root (str (fs/normalize (fs/path root "../mathlib4"))))
(def fixture-path (fs/path root "holes/labs/wm-contract/expected-free-energy-reference.edn"))
(def receipt-path (fs/path root "holes/labs/wm-contract/expected-free-energy-positive-receipt.edn"))

(def independent-case
  {:id :one-point :predictive-masses [1] :preference-masses [1]
   :risk 0 :ambiguity 2 :epistemic-gain -2 :expected-free-energy 2})

(defn reference-valid? [x]
  (and (= :expected-free-energy-reference/v1 (:schema x))
       (= [independent-case] (:cases x))))

(defn lean-pass? []
  (lean-positive/pass? mathlib-root
                       "DarkTower/WarMachine/ExpectedFreeEnergyWitness.lean"))

(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp (str fixture-path)))
        tested (if negative? (assoc-in fixture [:cases 0 :expected-free-energy] 3) fixture)
        receipt-ok? (:pass? (receipt/validate (edn/read-string (slurp (str receipt-path)))))
        baseline-valid? (and receipt-ok? (reference-valid? fixture) (lean-pass?))
        mutation-rejected? (not (reference-valid? tested))
        exit (cond (and negative? (not baseline-valid?)) 1
                   (and negative? (not mutation-rejected?)) 2
                   negative? 0 baseline-valid? 0 :else 1)]
    (println (cond
               (and negative? (not baseline-valid?)) "expected-free-energy-witness: BASELINE-INVALID (control reason not established)"
               (= exit 2) "expected-free-energy-witness: mutation slipped"
               negative? "expected-free-energy-witness: negative-control PASS (perturbed value rejected)"
               baseline-valid? "expected-free-energy-witness: PASS"
               :else "expected-free-energy-witness: FAIL"))
    (System/exit exit)))

(apply -main *command-line-args*)
