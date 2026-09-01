#!/usr/bin/env bb
(ns checks.variational-free-energy-witness
  (:require [babashka.process :as p]
            [checks.positive-proof-receipt :as receipt]
            [clojure.edn :as edn]
            [clojure.string :as str]))
(def fixture-path "holes/labs/wm-contract/variational-free-energy-reference.edn")
(def receipt-path "holes/labs/wm-contract/variational-free-energy-positive-receipt.edn")
(def witness-source "/home/joe/code/mathlib4/DarkTower/WarMachine/VariationalFreeEnergyWitness.lean")
(def mathlib "/home/joe/code/mathlib4")
(def expected {:schema :variational-free-energy-reference/v1
               :basis "sec-glossary.tex:19 — F = 1/2 mean_k(Pi_k epsilon_k^2)"
               :kind :hand-derived-from-formula :channel-count 14
               :precision 2 :prediction-error 1 :expected-variational-F 1})
(defn lean-exit [f]
  (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                  "lake" "env" "lean" f)))
(defn lean-source-exit [source]
  (let [tmp (java.io.File/createTempFile "weakened-variational-free-energy-" ".lean")]
    (try
      (spit tmp source)
      (:exit (p/shell {:dir mathlib :continue true :out :string :err :string}
                      "lake" "env" "lean" (.getAbsolutePath tmp)))
      (finally (.delete tmp)))))
(defn -main [& args]
  (let [value-neg? (some #{"--negative" "--negative-value"} args)
        type-neg? (some #{"--negative-type"} args)
        weakened? (some #{"--negative-weakened-positive"} args)
        unrelated? (some #{"--unrelated-positive-edit"} args)
        fixture (edn/read-string (slurp fixture-path))
        proof-receipt (edn/read-string (slurp receipt-path))
        source (slurp witness-source)
        weakened-source
        (str/replace source
          "=\n      ⟨gaussianReference.expectedVariationalF⟩ := by\n  norm_num [gaussianReference, variationalFreeEnergy, Channel.all]"
          "=\n      variationalFreeEnergy (fun _ => gaussianReference.precision)\n        (fun _ => gaussianReference.predictionError) := by\n  rfl")
        overrides (cond weakened? {["mathlib4" "DarkTower/WarMachine/VariationalFreeEnergyWitness.lean"] weakened-source}
                        unrelated? {["mathlib4" "DarkTower/WarMachine/VariationalFreeEnergyWitness.lean"]
                                    (str source "\n-- unrelated positive-receipt control\n")}
                        :else {})
        receipt-report (receipt/validate proof-receipt overrides)
        tested (if value-neg? (assoc fixture :expected-variational-F 2) fixture)
        positive? (and (= expected fixture)
                       (zero? (lean-exit "DarkTower/WarMachine/VariationalFreeEnergyWitness.lean"))
                       (:pass? receipt-report))
        rejected? (cond value-neg? (not= expected tested)
                        type-neg? (zero? (lean-exit "DarkTower/WarMachine/VariationalFreeEnergyNegative.lean"))
                        weakened? (and (not= source weakened-source)
                                       (zero? (lean-source-exit weakened-source))
                                       (not (:pass? receipt-report)))
                        unrelated? (:pass? receipt-report)
                        :else true)
        negative? (or value-neg? type-neg? weakened?)
        ok? (if (or weakened? unrelated?) rejected? (and positive? rejected?))
        exit (if ok? 0 (if negative? 2 1))]
    (println "variational-free-energy-witness:"
             (cond (and value-neg? ok?) "negative-control PASS (perturbed F rejected)"
                   (and type-neg? ok?) "negative-control PASS (expected F type rejected)"
                   (and weakened? ok?) "negative-control PASS (weakened positive rejected)"
                   (and unrelated? ok?) "unrelated-edit PASS (declaration basis stable)"
                   negative? "mutation slipped" ok? "PASS" :else "FAIL")
             (when (or weakened? unrelated? (not ok?)) (pr-str receipt-report))
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))
(apply -main *command-line-args*)
