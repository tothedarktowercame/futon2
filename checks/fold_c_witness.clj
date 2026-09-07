#!/usr/bin/env bb
(ns checks.fold-c-witness
  (:require [babashka.classpath :as cp]
            [babashka.process :as p]
            [checks.positive-proof-receipt :as receipt]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(cp/add-classpath "/home/joe/code/futon2/src")
(require '[futon2.aif.ruled-outcome-c])

(def mathlib "/home/joe/code/mathlib4")
(def witness "DarkTower/WarMachine/FoldCWitness.lean")
(def fixture-path "holes/labs/wm-contract/fold-c-reference.edn")
(def receipt-path "holes/labs/wm-contract/fold-c-positive-receipt.edn")
(def expected
  {:schema :fold-c-reference/v1
   :ruled-domain :F10/SeedObs
   :runtime-folded-layer-ids []
   :base-at-grounded-change 3
   :ordered-example {:add-then-double 8 :double-then-add 7}
   :evidence-limit :declaration-witness-no-run-identity})

(defn lean-run [file]
  (p/shell {:dir mathlib :continue true :out :string :err :string}
           "lake" "env" "lean" file))

(defn lean-folded-layer-ids [source]
  (let [names (some-> (re-find #"(?s)def runtimeFoldedLayers.*?:=.*?\[(.*?)\]" source)
                      second
                      (str/split #",")
                      (->> (map str/trim) (remove str/blank?)))]
    (set
     (for [n names
           :let [m (re-find (re-pattern
                             (str "(?s)def\\s+" (java.util.regex.Pattern/quote n)
                                  ".*?record\\s*:=\\s*⟨\\\"([^\\\"]+)\\\""))
                            source)]
           :when m]
       (keyword (second m))))))

(defn runtime-folded-layer-ids []
  (->> @(resolve 'futon2.aif.ruled-outcome-c/fold-declaration)
       (filter #(and (:folded? %) (= :yes (:in-ruled-sum %))))
       (map :layer/id) set))

(def negative-files
  {"--negative-order" {:file "DarkTower/WarMachine/FoldCOrderNegative.lean"
                        :needles ["Tactic `rfl` failed" "[addLayer, doubleLayer]" "[doubleLayer, addLayer]"]}
   "--negative-folded" {:file "DarkTower/WarMachine/FoldCFoldedNegative.lean"
                         :needles ["Tactic `rfl` failed" "foldC ruledBase [addLayer]" "ruledBase o"]}})

(defn positive-facts []
  (let [fixture (edn/read-string (slurp fixture-path))
        lean-source (slurp (str mathlib "/" witness))
        runtime-ids (runtime-folded-layer-ids)
        lean-ids (lean-folded-layer-ids lean-source)
        elaboration (lean-run witness)
        receipt-result (receipt/validate (edn/read-string (slurp receipt-path)))]
    {:fixture-equals? (= expected fixture)
     :receipt-valid? (:pass? receipt-result)
     :positive-exit (:exit elaboration)
     :runtime-folded-layer-ids runtime-ids
     :lean-folded-layer-ids lean-ids
     :cross-check? (= runtime-ids lean-ids (set (:runtime-folded-layer-ids fixture)))}))

(defn positive? [facts]
  (and (:fixture-equals? facts) (:receipt-valid? facts)
       (zero? (:positive-exit facts)) (:cross-check? facts)))

(defn -main [& args]
  (let [negative-flag (some (set (keys negative-files)) args)
        null? (some #{"--null-control"} args)
        facts (positive-facts)
        base-ok? (positive? facts)
        result (when negative-flag (lean-run (get-in negative-files [negative-flag :file])))
        output (when result (str (:out result) (:err result)))
        rejected? (if negative-flag
                    (and (not (zero? (:exit result)))
                         (every? #(str/includes? output %)
                                 (get-in negative-files [negative-flag :needles])))
                    true)
        null-identical? (if null? (= facts (positive-facts)) true)
        ok? (and base-ok? rejected? null-identical?)
        exit (if ok? 0 (if negative-flag 2 1))]
    (println "fold-c-witness:"
             (cond negative-flag (if ok? (str "negative-control PASS " negative-flag) "mutation slipped")
                   null? (if ok? "null-control PASS identical" "null-control FAIL")
                   ok? "PASS"
                   :else "FAIL")
             (str "runtime-folded=" (pr-str (:runtime-folded-layer-ids facts)))
             (str "lean-folded=" (pr-str (:lean-folded-layer-ids facts)))
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
