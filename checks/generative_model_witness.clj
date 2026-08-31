#!/usr/bin/env bb
(ns checks.generative-model-witness
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.edn :as edn]
            [clojure.string :as str]))

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
  (let [source (slurp (str lean-path))
        mutant (str/replace source "observation := observation\n  transition := transition"
                            "observation := wrongObservation\n  transition := transition")
        tmp (fs/create-temp-file {:dir (fs/parent lean-path)
                                  :prefix "GenerativeModelMutation-" :suffix ".lean"})]
    (try
      (cond
        (= source mutant) 1
        :else (do (spit (str tmp) mutant)
                  (if (zero? (:exit (lean! tmp))) 2 0)))
      (finally (fs/delete-if-exists tmp)))))

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
