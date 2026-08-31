#!/usr/bin/env bb
(ns checks.wm-workspace-gate
  (:require [babashka.process :as process]
            [cheshire.core :as json]))

(def contract "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json")

(defn authority []
  (get-in (json/parse-string (slurp contract) true) [:source :git-sha]))

(defn commands []
  [{:name :strict-contract
    :argv ["bb" "-cp" "." "checks/contract_lint.clj" "--strict"
           "--contract" contract "--registry" "checks/witness-registry.edn"
           "--report" "/tmp/wm-workspace-contract-strict.edn"
           "--authority" (authority)]}
   {:name :holder :argv ["bb" "checks/holder_check.clj"]}
   {:name :figure-agreement :argv ["bb" "checks/control_map_figure_agreement_check.clj"]}
   {:name :organization :argv ["bb" "-cp" "." "checks/control_organization_check.clj"]}
   {:name :hyper-edge-shape :argv ["bb" "-cp" "." "checks/hyper_edge_exemplar_check.clj"]}
   {:name :hyper-edge-domain-range :argv ["bb" "-cp" "." "checks/hyper_edge_domain_range_check.clj"]}
   {:name :fold-quarantine :argv ["bb" "-cp" "src:." "checks/fold_turn_quarantine_check.clj"]}
   {:name :preference-shape :argv ["bb" "-cp" "." "checks/preference_stack_witness_shape_check.clj"]}
   {:name :preference-binding
    :argv ["clojure" "-M" "-m" "checks.preference-stack-binding-check"]}
   {:name :r9-proof-receipt :argv ["bb" "-cp" "." "checks/r9_proof_receipt_check.clj"]}
   {:name :route-conformance
    :argv ["bb" "checks/wm_route_conformance.clj"
           "holes/labs/wm-contract/tick-run-record-2026-08-30.edn"]}
   {:name :runs-once :argv ["bb" "checks/wm_runs_once_witness.clj"]}])

(defn run-one [{:keys [name argv]}]
  (println "wm-workspace-gate: RUN" (clojure.core/name name))
  (let [result (apply process/shell {:continue true :out :inherit :err :inherit} argv)]
    {:name name :exit (:exit result)}))

(defn -main [& _]
  (let [results (mapv run-one (commands))
        failures (filterv #(not= 0 (:exit %)) results)]
    (println "wm-workspace-gate: SUMMARY"
             (pr-str {:checks (count results) :failures failures
                      :manual-exclusions [:lane-registry :live-operational-certificate]}))
    (System/exit (if (empty? failures) 0 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
