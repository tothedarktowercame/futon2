#!/usr/bin/env bb
(ns checks.observation-vector-witness
  (:require [babashka.process :as p] [checks.positive-proof-receipt :as receipt] [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/observation-vector-reference.edn")
(def receipt-path "holes/labs/wm-contract/observation-vector-positive-receipt.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected
  {:schema :observation-vector-reference/v1
   :basis "sec-glossary.tex:12 — the standardized observation vector has all fourteen named channels"
   :kind :hand-derived-complete-coordinate-grid
   :channels [:loop-health :support-coverage :attack-coverage :mission-health
              :stack-pct :consulting-pct :portfolio-pct :mathematics-pct
              :active-repo-ratio :sorry-count-norm :coupling-density
              :ticks-firing-ratio :depositing-signal :annotation-health]
   :values [0 1 2 3 4 5 6 7 8 9 10 11 12 13]
   :excluded {:partial-channel-map true :single-outcome true}})

(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))
(def negative-files
  {"--negative-partial" "DarkTower/WarMachine/ObservationVectorPartialNegative.lean"
   "--negative-outcome" "DarkTower/WarMachine/ObservationVectorOutcomeNegative.lean"})

(defn -main [& args]
  (let [negative-flag (some (set (keys negative-files)) args)
        fixture (edn/read-string (slurp fixture-path))
        positive? (and (:pass? (receipt/validate (edn/read-string (slurp receipt-path)))) (= expected fixture)
                       (= 14 (count (:channels fixture)))
                       (= 14 (count (distinct (:channels fixture))))
                       (= 14 (count (:values fixture)))
                       (zero? (lean-exit "DarkTower/WarMachine/ObservationVectorWitness.lean")))
        rejected? (if negative-flag
                    (zero? (lean-exit (get negative-files negative-flag)))
                    true)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if negative-flag 2 1))]
    (println "observation-vector-witness:"
             (cond
               (and negative-flag ok?) (str "negative-control PASS (" (subs negative-flag 11) " rejected)")
               negative-flag "mutation slipped"
               ok? "PASS"
               :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
