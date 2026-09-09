#!/usr/bin/env bb
(ns checks.fold-c-witness
  (:require [babashka.classpath :as cp]
            [babashka.process :as p]
            [checks.positive-proof-receipt :as receipt]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(cp/add-classpath "/home/joe/code/futon2/src")
(require '[futon2.aif.ruled-outcome-c]
         '[checks.fold-c-axes :as axes])

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
  (:preference-layer (axes/runtime-axes)))

(defn risk-certificate-valid? []
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "fold-c-certificate-"
                     (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      (let [result (p/shell {:continue true :out :string :err :string}
                           "bb" "-cp" ".:src" "-m" "checks.preference-risk-receipt" (.getPath dir))]
        (and (zero? (:exit result))
             (every? (fn [name]
                       (= (slurp (str dir "/" name))
                          (slurp (str "holes/labs/wm-contract/runs/separated-risk-certificate/" name))))
                     ["certificate.edn" "runtime-mass-binding.lean"])))
      (finally
        (doseq [f (reverse (file-seq dir))] (.delete f))))))

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
        axis-fixture (edn/read-string (slurp axes/fixture-path))
        risk-ids (axes/lean-risk-ids)
        receipt-result (receipt/validate (edn/read-string (slurp receipt-path)))]
    {:axes-fixture-valid? (= axis-fixture (axes/derived-fixture))
     :risk-cross-check? (axes/risk-matches? (axes/runtime-axes) risk-ids axis-fixture)
     :risk-certificate-valid? (risk-certificate-valid?)
     :risk-lean-exit (:exit (lean-run "DarkTower/WarMachine/PreferenceRiskBoundary.lean"))
     :runtime-risk-ids (:risk-contribution (axes/runtime-axes))
     :lean-risk-ids risk-ids
     :fixture-equals? (= expected fixture)
     :receipt-valid? (:pass? receipt-result)
     :positive-exit (:exit elaboration)
     :runtime-folded-layer-ids runtime-ids
     :lean-folded-layer-ids lean-ids
     :cross-check? (= runtime-ids lean-ids (set (:runtime-folded-layer-ids fixture)))}))

(defn positive? [facts]
  (and (:axes-fixture-valid? facts) (:risk-cross-check? facts)
       (:risk-certificate-valid? facts) (zero? (:risk-lean-exit facts))
       (:fixture-equals? facts) (:receipt-valid? facts)
       (zero? (:positive-exit facts)) (:cross-check? facts)))

(defn -main [& args]
  (let [negative-flag (some (set (keys negative-files)) args)
        risk-negative? (some #{"--negative-risk-missing"} args)
        null? (some #{"--null-control"} args)
        facts (positive-facts)
        base-ok? (positive? facts)
        result (when negative-flag (lean-run (get-in negative-files [negative-flag :file])))
        output (when result (str (:out result) (:err result)))
        risk-rejected? (if risk-negative?
                         (not (axes/risk-matches? (axes/runtime-axes) #{}
                                (edn/read-string (slurp axes/fixture-path)))) true)
        rejected? (if negative-flag
                    (and (not (zero? (:exit result)))
                         (every? #(str/includes? output %)
                                 (get-in negative-files [negative-flag :needles])))
                    true)
        null-identical? (if null? (= facts (positive-facts)) true)
        ok? (and base-ok? rejected? risk-rejected? null-identical?)
        exit (cond (not base-ok?) 1 ok? 0 (or negative-flag risk-negative?) 2 :else 1)]
    (println "fold-c-witness:"
             (cond (not base-ok?) (str "baseline FAIL " (pr-str facts))
                   risk-negative? (if ok? "negative-control PASS --negative-risk-missing" "mutation slipped")
                   negative-flag (if ok? (str "negative-control PASS " negative-flag) "mutation slipped")
                   null? (if ok? "null-control PASS identical" "null-control FAIL")
                   ok? "PASS"
                   :else "FAIL")
             (str "runtime-folded=" (pr-str (:runtime-folded-layer-ids facts)))
             (str "lean-folded=" (pr-str (:lean-folded-layer-ids facts)))
             (str "runtime-risk=" (pr-str (:runtime-risk-ids facts)))
             (str "lean-risk=" (pr-str (:lean-risk-ids facts)))
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit exit)))

(apply -main *command-line-args*)
