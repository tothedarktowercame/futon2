#!/usr/bin/env bb
(ns checks.expected-information-gain-witness
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.edn :as edn]))

(def root (fs/cwd))
(def mathlib-root (str (fs/normalize (fs/path root "../mathlib4"))))
(def fixture-path
  (fs/path root "holes/labs/wm-contract/expected-information-gain-reference.edn"))

(def independent-case
  {:id :binary-prior-point-posterior
   :predictive-outcome-masses [1]
   :parameter-prior-masses [1/2 1/2]
   :parameter-posterior-masses [1 0]
   :expected-information-gain "log(2)"})

(defn reference-valid? [x]
  (and (= :expected-information-gain-reference/v1 (:schema x))
       (= [independent-case] (:cases x))))

(defn lean-pass? []
  (zero? (:exit (process/shell
                  {:dir mathlib-root :continue true :out :string :err :string}
                  "lake" "env" "lean"
                  "DarkTower/WarMachine/ExpectedInformationGainWitness.lean"))))

(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp (str fixture-path)))
        tested (if negative?
                 (assoc-in fixture [:cases 0 :expected-information-gain] "log(3)")
                 fixture)
        baseline-valid? (and (reference-valid? fixture) (lean-pass?))
        mutation-rejected? (not (reference-valid? tested))
        exit (cond (and negative? (not baseline-valid?)) 1
                   (and negative? (not mutation-rejected?)) 2
                   negative? 0 baseline-valid? 0 :else 1)]
    (println (cond
               (and negative? (not baseline-valid?)) "expected-information-gain-witness: BASELINE-INVALID (control reason not established)"
               (= exit 2) "expected-information-gain-witness: mutation slipped"
               negative? "expected-information-gain-witness: negative-control PASS (perturbed value rejected)"
               baseline-valid? "expected-information-gain-witness: PASS"
               :else "expected-information-gain-witness: FAIL"))
    (System/exit exit)))

(apply -main *command-line-args*)
