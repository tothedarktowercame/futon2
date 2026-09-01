#!/usr/bin/env bb
(ns checks.preference-distribution-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/preference-distribution-reference.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected
  {:schema :preference-distribution-reference/v1
   :basis "sec-glossary.tex:21-23 — preferred outcomes C form an unconditional normalized distribution"
   :kind :hand-derived-fair-binary-preference
   :conditioning-domain :unit
   :outcomes [:good :bad]
   :mass {:good [1 2] :bad [1 2]}
   :excluded {:state-conditioned-likelihood true
              :vertex-local-pragmatic-cost true}})

(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))

(def negative-files
  {"--negative-conditioning" "DarkTower/WarMachine/PreferenceDistributionConditioningNegative.lean"
   "--negative-pragmatic-cost" "DarkTower/WarMachine/PreferenceDistributionPragmaticCostNegative.lean"})

(defn -main [& args]
  (let [negative-flag (some (set (keys negative-files)) args)
        fixture (edn/read-string (slurp fixture-path))
        positive? (and (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/PreferenceDistributionWitness.lean")))
        rejected? (if negative-flag
                    (zero? (lean-exit (get negative-files negative-flag)))
                    true)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if negative-flag 2 1))]
    (println "preference-distribution-witness:"
             (cond
               (and negative-flag ok?) (str "negative-control PASS (" (subs negative-flag 11) " rejected)")
               negative-flag "mutation slipped"
               ok? "PASS"
               :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
