#!/usr/bin/env bb
(ns checks.channel-witness
  (:require [babashka.process :as p] [clojure.edn :as edn]))
(def fixture-path "holes/labs/wm-contract/channel-vocabulary-reference.edn")
(def expected
  [:loop-health :support-coverage :attack-coverage :mission-health :stack-pct
   :consulting-pct :portfolio-pct :mathematics-pct :active-repo-ratio
   :sorry-count-norm :coupling-density :ticks-firing-ratio :depositing-signal
   :annotation-health])
(defn valid? [x]
  (and (= :channel-vocabulary-reference/v1 (:schema x))
       (= "holes/labs/wm-contract/C58-r2-channel-eras.md" (:basis x))
       (= expected (:channels x))))
(defn lean-pass? []
  (zero? (:exit (p/shell {:dir "/home/joe/code/mathlib4" :continue true
                          :out :string :err :string}
                         "lake" "env" "lean"
                         "DarkTower/WarMachine/ChannelWitness.lean"))))
(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp fixture-path))
        tested (if negative? (update fixture :channels pop) fixture)
        baseline-valid? (and (valid? fixture) (lean-pass?))
        mutation-rejected? (not (valid? tested))
        exit (cond (and negative? (not baseline-valid?)) 1
                   (and negative? (not mutation-rejected?)) 2
                   negative? 0 baseline-valid? 0 :else 1)]
    (println "channel-witness:"
             (cond (and negative? (not baseline-valid?)) "BASELINE-INVALID (control reason not established)"
                   (= exit 2) "mutation slipped"
                   negative? "negative-control PASS (missing declared channel rejected)"
                   baseline-valid? "PASS" :else "FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))
(apply -main *command-line-args*)
