#!/usr/bin/env bb
(ns checks.bayesian-model-reduction-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/bayesian-model-reduction-reference.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected
  {:schema :bayesian-model-reduction-reference/v1
   :basis "sec-glossary.tex:54 — A-prime = A + a-prime - a componentwise"
   :kind :hand-derived-from-count-conservation
   :old-prior [[1 1] [1 1]]
   :old-posterior [[10 1] [4 1]]
   :accumulated-counts [[9 1] [3 1]]
   :reduced-prior [[1 1] [1 100]]
   :reduced-posterior [[10 1] [301 100]]})

(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))

(defn -main [& args]
  (let [counts-neg? (some #{"--negative-counts"} args)
        fixture (edn/read-string (slurp fixture-path))
        tested (if counts-neg?
                 (assoc fixture :reduced-posterior [[10 1] [401 100]])
                 fixture)
        positive? (and (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/BayesianModelReductionWitness.lean")))
        rejected? (if counts-neg?
                    (and (not= expected tested)
                         (zero? (lean-exit "DarkTower/WarMachine/BayesianModelReductionNegative.lean")))
                    true)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if counts-neg? 2 1))]
    (println "bayesian-model-reduction-witness:"
             (cond
               (and counts-neg? ok?) "negative-control PASS (count conservation violation rejected)"
               counts-neg? "mutation slipped"
               ok? "PASS"
               :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
