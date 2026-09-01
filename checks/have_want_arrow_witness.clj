#!/usr/bin/env bb
(ns checks.have-want-arrow-witness
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [checks.lean-positive-witness :as lean-positive]
            [checks.positive-proof-receipt :as receipt]
            [clojure.edn :as edn]))

(def root (fs/cwd))
(def fixture-path (fs/path root "holes/labs/wm-contract/have-want-arrow-reference.edn"))
(def receipt-path "holes/labs/wm-contract/have-want-arrow-positive-receipt.edn")
(def mathlib-root (str (fs/normalize (fs/path root "../mathlib4"))))

(defn fixture-valid? [x]
  (and (= :have-want-arrow-reference/v1 (:schema x))
       (= :endpoint-pair (:identity x))
       (= [:correlated :open :constructed] (:states x))
       (= {:have :belief-mass-supports-cohort :want :support-coverage-channel}
          (:arrow x))
       (= :left-want-equals-right-have (get-in x [:composition :law]))))

(defn lean-exit [file]
  (:exit (process/shell {:dir mathlib-root :continue true :out :string :err :string}
                        "lake" "env" "lean" file)))

(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp (str fixture-path)))
        fixture-ok? (fixture-valid? fixture)
        positive-ok? (lean-positive/pass? mathlib-root "DarkTower/WarMachine/HaveWantArrowWitness.lean")
        negative-exit (if negative?
                        (lean-exit "DarkTower/WarMachine/HaveWantArrowNegative.lean") 1)
        receipt-ok? (:pass? (receipt/validate (edn/read-string (slurp receipt-path))))
        baseline-valid? (and receipt-ok? fixture-ok? positive-ok?)
        mutation-rejected? (zero? negative-exit)
        accepted? (if negative? (and baseline-valid? mutation-rejected?) baseline-valid?)
        exit (cond (and negative? (not baseline-valid?)) 1
                   accepted? 0 negative? 2 :else 1)]
    (println (cond
               (and negative? (not baseline-valid?)) "have-want-arrow-witness: BASELINE-INVALID (control reason not established)"
               (and negative? accepted?) "have-want-arrow-witness: negative-control PASS (malformed composition rejected)"
               negative? "have-want-arrow-witness: mutation slipped"
               accepted? "have-want-arrow-witness: PASS"
               :else "have-want-arrow-witness: FAIL")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
