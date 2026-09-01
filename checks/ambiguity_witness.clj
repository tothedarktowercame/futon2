#!/usr/bin/env bb
(ns checks.ambiguity-witness
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [checks.positive-proof-receipt :as receipt]
            [clojure.edn :as edn]))

(def root (fs/cwd))
(def fixture-path (fs/path root "holes/labs/wm-contract/ambiguity-reference.edn"))
(def receipt-path (fs/path root "holes/labs/wm-contract/ambiguity-positive-receipt.edn"))
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
        receipt-ok? (:pass? (receipt/validate (edn/read-string (slurp (str receipt-path)))))
        baseline-valid? (and receipt-ok? (fixture-valid? fixture) (lean-pass?))
        mutation-rejected? (not (fixture-valid? tested))
        exit (cond (and negative? (not baseline-valid?)) 1
                   (and negative? (not mutation-rejected?)) 2
                   negative? 0 baseline-valid? 0 :else 1)]
    (println (cond
               (and negative? (not baseline-valid?)) "ambiguity-witness: BASELINE-INVALID (control reason not established)"
               (= exit 2) "ambiguity-witness: mutation slipped"
               negative? "ambiguity-witness: negative-control PASS (perturbed entropy rejected)"
               baseline-valid? "ambiguity-witness: PASS"
               :else "ambiguity-witness: FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
