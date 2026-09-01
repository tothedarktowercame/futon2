#!/usr/bin/env bb
(ns checks.transition-kernel-witness
  (:require [babashka.process :as p] [checks.positive-proof-receipt :as receipt] [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/transition-kernel-reference.edn")
(def receipt-path "holes/labs/wm-contract/transition-kernel-positive-receipt.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected
  {:schema :transition-kernel-reference/v1
   :basis "sec-glossary.tex:7 — controlled transition B maps (state, action) to a normalized next-state distribution"
   :kind :hand-derived-deterministic-controlled-rows
   :states [:idle :active]
   :actions [:stay :start]
   :rows {[:idle :stay] :idle
          [:active :stay] :active
          [:idle :start] :active
          [:active :start] :active}
   :excluded {:action-unconditioned-kernel true
              :multivariate-beta-normalizer true}})

(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))

(def negative-files
  {"--negative-uncontrolled" "DarkTower/WarMachine/TransitionKernelUncontrolledNegative.lean"
   "--negative-beta" "DarkTower/WarMachine/TransitionKernelBetaNegative.lean"})

(defn -main [& args]
  (let [negative-flag (some (set (keys negative-files)) args)
        fixture (edn/read-string (slurp fixture-path))
        positive? (and (:pass? (receipt/validate (edn/read-string (slurp receipt-path)))) (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/TransitionKernelWitness.lean")))
        rejected? (if negative-flag
                    (zero? (lean-exit (get negative-files negative-flag)))
                    true)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if negative-flag 2 1))]
    (println "transition-kernel-witness:"
             (cond
               (and negative-flag ok?) (str "negative-control PASS (" (subs negative-flag 11) " rejected)")
               negative-flag "mutation slipped"
               ok? "PASS"
               :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
