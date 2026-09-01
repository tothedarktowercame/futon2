#!/usr/bin/env bb
(ns checks.log-multivariate-beta-witness
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.edn :as edn]))

(def root (fs/cwd))
(def mathlib-root (str (fs/normalize (fs/path root "../mathlib4"))))
(def fixture-path
  (fs/path root "holes/labs/wm-contract/log-multivariate-beta-reference.edn"))
(def lean-witness "DarkTower/WarMachine/LogMultivariateBetaWitness.lean")

(def independent-cases
  {[1 1] {:normaliser {:numerator 1 :denominator 1} :expected-log "0"}
   [2 1] {:normaliser {:numerator 1 :denominator 2} :expected-log "-log(2)"}})

(defn reference-valid? [x]
  (and (= :log-multivariate-beta-reference/v1 (:schema x))
       (= {:nonempty true :concentrations :strictly-positive-real} (:domain x))
       (= (set (keys independent-cases)) (set (map :concentrations (:cases x))))
       (every? (fn [{:keys [concentrations normaliser expected-log]}]
                 (and (every? pos? concentrations)
                      (= {:normaliser normaliser :expected-log expected-log}
                         (get independent-cases concentrations))))
               (:cases x))))

(defn lean-pass? []
  (zero? (:exit (process/shell {:dir mathlib-root :continue true
                                :out :string :err :string}
                               "lake" "env" "lean" lean-witness))))

(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp (str fixture-path)))
        tested (if negative?
                 (assoc-in fixture [:cases 1 :normaliser :denominator] 3)
                 fixture)
        baseline-valid? (and (reference-valid? fixture) (lean-pass?))
        mutation-rejected? (not (reference-valid? tested))
        exit (cond
               (and negative? (not baseline-valid?)) 1
               (and negative? (not mutation-rejected?)) 2
               negative? 0
               baseline-valid? 0
               :else 1)]
    (println (cond
               (and negative? (not baseline-valid?)) "log-multivariate-beta-witness: BASELINE-INVALID (control reason not established)"
               (= exit 2) "log-multivariate-beta-witness: mutation slipped"
               negative? "log-multivariate-beta-witness: negative-control PASS (perturbed normaliser rejected)"
               baseline-valid? "log-multivariate-beta-witness: PASS"
               :else "log-multivariate-beta-witness: FAIL"))
    (System/exit exit)))

(apply -main *command-line-args*)
