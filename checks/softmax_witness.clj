#!/usr/bin/env bb
(ns checks.softmax-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))

(def fixture-path "holes/labs/wm-contract/softmax-reference.edn")
(def mathlib "/home/joe/code/mathlib4")
(def expected
  {:schema :softmax-reference/v1
   :basis "sec-glossary.tex:39 — Q(pi) is proportional to exp(log E(pi) - G(pi)/tau)"
   :kind :hand-derived-from-exp-log-identities
   :temperature [1 3]
   :habit {:lower 1 :higher 1}
   :grades {:lower 0 :higher [:log 8 :divided-by 3]}
   :unnormalised {:lower [1 1] :higher [1 8]}
   :probabilities {:lower [8 9] :higher [1 9]}
   :scope {:positive-temperature true
           :tau-zero-limit :not-covered
           :tau-infinity-limit :not-covered}})

(defn lean-exit [file]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" file)))

(defn -main [& args]
  (let [order-neg? (some #{"--negative-order"} args)
        norm-neg? (some #{"--negative-normalisation"} args)
        fixture (edn/read-string (slurp fixture-path))
        tested (cond
                 order-neg? (assoc fixture :probabilities {:lower [1 9] :higher [8 9]})
                 norm-neg? (assoc fixture :probabilities {:lower [1 1] :higher [1 8]})
                 :else fixture)
        positive? (and (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/SoftmaxWitness.lean")))
        rejected? (cond
                    order-neg? (and (not= expected tested)
                                    (zero? (lean-exit "DarkTower/WarMachine/SoftmaxNegative.lean")))
                    norm-neg? (not= expected tested)
                    :else true)
        negative? (or order-neg? norm-neg?)
        ok? (and positive? rejected?)
        exit (if ok? 0 (if negative? 2 1))]
    (println "softmax-witness:"
             (cond
               (and order-neg? ok?) "negative-control PASS (inverted score order rejected)"
               (and norm-neg? ok?) "negative-control PASS (unnormalised exponentials rejected)"
               negative? "mutation slipped"
               ok? "PASS"
               :else "FAIL")
             "tau=1/3 limits=tau-zero-not-covered,tau-infinity-not-covered"
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
