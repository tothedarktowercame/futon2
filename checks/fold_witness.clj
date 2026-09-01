#!/usr/bin/env bb
(ns checks.fold-witness
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.edn :as edn]))

(def root (fs/cwd))
(def fixture-path (fs/path root "holes/labs/wm-contract/fold-reference.edn"))
(def mathlib-root (str (fs/normalize (fs/path root "../mathlib4"))))

(defn fixture-valid? [x]
  (and (= :fold-reference/v1 (:schema x))
       (= {:wiring {:nodes 5 :hyperedges 5 :terminals 1}
           :coverage-score-delta -1 :policy-holes 3}
          (:fold x))))

(defn lean-exit [file]
  (:exit (process/shell {:dir mathlib-root :continue true :out :string :err :string}
                        "lake" "env" "lean" file)))

(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp (str fixture-path)))
        accepted? (and (fixture-valid? fixture)
                       (if negative?
                         (zero? (lean-exit "DarkTower/WarMachine/FoldNegative.lean"))
                         (zero? (lean-exit "DarkTower/WarMachine/FoldWitness.lean"))))
        exit (if accepted? 0 (if negative? 2 1))]
    (println (cond
               (and negative? accepted?) "fold-witness: negative-control PASS (missing policy holes rejected)"
               negative? "fold-witness: mutation slipped"
               accepted? "fold-witness: PASS"
               :else "fold-witness: FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
