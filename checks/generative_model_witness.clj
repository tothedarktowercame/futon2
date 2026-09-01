#!/usr/bin/env bb
(ns checks.generative-model-witness
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.edn :as edn]))

(def root (fs/cwd))
(def mathlib-root (str (fs/normalize (fs/path root "../mathlib4"))))
(def lean-path
  (fs/path mathlib-root "DarkTower/WarMachine/GenerativeModelWitness.lean"))
(def fixture-path (fs/path root "holes/labs/wm-contract/generative-model-reference.edn"))

(def expected-case
  {:id :binary-observation-deterministic-transition
   :observation-mass 1/2 :transition-mass 1 :policy-prior-mass 1
   :joint-factor-mass 1/2})

(defn reference-valid? [x]
  (and (= :generative-model-reference/v1 (:schema x))
       (= [:observation-mass :transition-mass :policy-prior-mass]
          (:factorisation x))
       (= [expected-case] (:cases x))))

(defn lean! [path]
  (process/shell {:dir mathlib-root :continue true :out :string :err :string}
                 "lake" "env" "lean" (str path)))

(defn structural-negative []
  (if (zero? (:exit (lean! (fs/path mathlib-root
                                    "DarkTower/WarMachine/GenerativeModelNegative.lean"))))
    0 2))

(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp (str fixture-path)))
        exit (if negative?
               (structural-negative)
               (if (and (reference-valid? fixture) (zero? (:exit (lean! lean-path)))) 0 1))]
    (println (cond
               (= exit 2) "generative-model-witness: mutation slipped"
               negative? "generative-model-witness: negative-control PASS (mis-wired state carrier rejected)"
               (zero? exit) "generative-model-witness: PASS"
               :else "generative-model-witness: FAIL"))
    (System/exit exit)))

(apply -main *command-line-args*)
