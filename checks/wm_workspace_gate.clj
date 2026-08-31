#!/usr/bin/env bb
(ns checks.wm-workspace-gate
  (:require [babashka.process :as process]
            [babashka.fs :as fs]
            [clojure.set :as set]
            [clojure.string :as str]
            [cheshire.core :as json]))

(def contract "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json")
(def c167-run "holes/labs/wm-contract/tick-run-record-2026-08-31.edn")
(def c167-resource "holes/labs/wm-contract/C167-v20-certificate-resource.edn")
(def c167-run-sha256 "3d4432d09934517811cda1b1b35d7a5a9c1bbc73f137d76f4aecb30f6ab07875")
(def c167-resource-sha256 "9c9e566a9d5460ec0bbadf59c58b10627e6434a52974ffd62480dc554e4cfdea")

(def repositories
  {:futon2 "/home/joe/code/futon2"
   :mathlib4 "/home/joe/code/mathlib4"
   :p4ng "/home/joe/code/p4ng"
   :futon3 "/home/joe/code/futon3"})

(defn sha256-text [value]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes value "UTF-8"))
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn repository-provenance [[repo dir]]
  (letfn [(git [& argv]
            (apply process/shell {:continue true :out :string :err :string :dir dir}
                   "git" argv))]
    (let [head (git "rev-parse" "HEAD")
          tree (git "rev-parse" "HEAD^{tree}")
          status (git "status" "--porcelain")
          diff (git "diff" "HEAD" "--")]
      {:repository repo :path dir
       :git-sha (some-> (:out head) str/trim)
       :tree-sha (some-> (:out tree) str/trim)
       :dirty? (not (str/blank? (:out status)))
       :tracked-diff-sha256 (sha256-text (:out diff))
       :readable? (every? zero? (map :exit [head tree status diff]))})))

(defn provenance []
  (mapv repository-provenance repositories))

(defn authority []
  (get-in (json/parse-string (slurp contract) true) [:source :git-sha]))

(def known-check-files
  ;; Discovery is a completeness alarm, not an execution policy.  A new file
  ;; must be classified here before the gate can pass; it is never guessed safe.
  #{"ablation_exact_dyadic_witness.clj" "absence_scoring_counterfactual.clj"
    "ambiguity_witness.clj"
    "absent_is_loud_lint.clj" "belief_update_check.clj"
    "belief_variance_inputs.clj" "cascade_diff_witness.clj" "closed_record_pointer_check.clj" "contract_lint.clj"
    "cleanup_queue_correction_index.clj"
    "contract_authority_current.clj"
    "control_map_figure_agreement_check.clj" "control_map_lint.clj"
    "control_organization_check.clj" "expected_free_energy_witness.clj"
    "expected_information_gain_witness.clj" "fold_turn_quarantine_check.clj"
    "fold_witness.clj"
    "generative_model_witness.clj" "holder_check.clj"
    "have_want_arrow_witness.clj"
    "hyper_edge_domain_range_check.clj" "hyper_edge_exemplar_check.clj"
    "lane_registry_check.clj" "log_multivariate_beta_witness.clj"
    "model_uncertainty_eig_witness.clj"
    "lean_sorry_category_check.clj"
    "obligation_ledger_reconciliation_check.clj"
    "preemptive_absence_coercion_lint.clj" "preemptive_acceptance_lint.clj"
    "preemptive_artefact_boundary_lint.clj" "preemptive_era_blind_lint.clj"
    "preemptive_record_conflict_lint.clj" "preemptive_repair_lint.clj"
    "preemptive_repair_suite.clj" "preemptive_stale_baseline_lint.clj"
    "preference_stack_binding_check.clj" "preference_stack_witness_shape_check.clj"
    "r17_generator_disposer_check.clj" "r19_stack_witness.clj"
    "r2_channel_contract.clj" "r2_pinned_snapshot_witness.clj"
    "r8_f_contract.clj" "r8_pinned_snapshot_witness.clj"
    "trace_schema_compatibility.clj"
    "r9_independence.clj" "r9_proof_receipt_check.clj"
    "wm_operational_certificate.clj" "wm_route_conformance.clj"
    "wm_runs_once_witness.clj" "wm_workspace_gate.clj"})

(defn inventory-result []
  (let [found (set (map (comp str fs/file-name) (fs/glob "checks" "*.clj")))
        unknown (sort (set/difference found known-check-files))
        missing (sort (set/difference known-check-files found))]
    {:name :check-inventory
     :exit (if (and (empty? unknown) (empty? missing)) 0 1)
     :unknown unknown :missing missing}))

(defn commands []
  [{:name :strict-contract
    :argv ["bb" "-cp" "." "checks/contract_lint.clj" "--strict"
           "--contract" contract "--registry" "checks/witness-registry.edn"
           "--report" "/tmp/wm-workspace-contract-strict.edn"
           "--authority" (authority)]}
   {:name :contract-authority-current
    :argv ["bb" "checks/contract_authority_current.clj"]}
   {:name :ambiguity :argv ["bb" "checks/ambiguity_witness.clj"]}
   {:name :have-want-arrow :argv ["bb" "checks/have_want_arrow_witness.clj"]}
   {:name :fold :argv ["bb" "checks/fold_witness.clj"]}
   {:name :holder :argv ["bb" "checks/holder_check.clj"]}
   {:name :closed-record-pointers :argv ["bb" "checks/closed_record_pointer_check.clj"]}
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
   {:name :runs-once :argv ["bb" "checks/wm_runs_once_witness.clj"]}
   {:name :lean-sorry-categories
    :argv ["bb" "checks/lean_sorry_category_check.clj"]}
   {:name :model-uncertainty-eig
    :argv ["bb" "checks/model_uncertainty_eig_witness.clj"]}
   {:name :pinned-operational-certificate
    :argv ["bb" "-cp" "." "checks/wm_operational_certificate.clj"
           "--run" c167-run "--resource" c167-resource
           "--run-sha256" c167-run-sha256 "--resource-sha256" c167-resource-sha256]}
   {:name :p4ng-referent-drift :dir "/home/joe/code/p4ng"
    :argv ["python3" "detect_drift.py"]}
   {:name :cleanup-queue-corrections
    :argv ["bb" "checks/cleanup_queue_correction_index.clj"]}])

(defn control-commands []
  [{:name :c157-perturbed-entropy
    :argv ["bb" "checks/ambiguity_witness.clj" "--negative-control"]}
   {:name :c175-stale-contract-authority
    :argv ["bb" "checks/contract_authority_current.clj" "--negative-control"]}
   {:name :c168-malformed-arrow-composition
    :argv ["bb" "checks/have_want_arrow_witness.clj" "--negative-control"]}
   {:name :c172-missing-policy-holes
    :argv ["bb" "checks/fold_witness.clj" "--negative-control"]}
   {:name :c174-reconstructible-quarantine-member
    :argv ["bb" "-cp" "src:." "checks/fold_turn_quarantine_check.clj"
           "--negative-reconstructible-member"]}
   {:name :c116-removed-ledger-row
    :argv ["bb" "-cp" "." "checks/r9_independence.clj" "--negative-ledger"
           "--report" "/tmp/wm-gate-r9-ledger.edn" "--lean" "/tmp/wm-gate-r9-ledger.lean"]}
   {:name :c116-changed-o7-source
    :argv ["bb" "-cp" "." "checks/r9_independence.clj" "--negative-per-row"
           "--report" "/tmp/wm-gate-r9-row.edn" "--lean" "/tmp/wm-gate-r9-row.lean"]}
   {:name :c116-pre-boundary-stored-f
    :argv ["bb" "-cp" "." "checks/r8_f_contract.clj" "--negative"
           "--report" "/tmp/wm-gate-r8-era.edn"]}
   {:name :c117-f1-outside-repository :dir "/home/joe/code/futon3"
    :argv ["clojure" "-Sdeps" "{:paths [\"checks\"]}" "-M" "-m" "find-snatch" "--negative-f1"]}
   {:name :c117-f2-removed-receipt :dir "/home/joe/code/futon3"
    :argv ["clojure" "-Sdeps" "{:paths [\"checks\"]}" "-M" "-m" "find-snatch" "--negative-f2"]}
   {:name :c117-f3-score-only-receipt :dir "/home/joe/code/futon3"
    :argv ["clojure" "-Sdeps" "{:paths [\"checks\"]}" "-M" "-m" "find-snatch" "--negative-f3"]}
   {:name :c134-unlabelled-sorry
    :argv ["bb" "checks/lean_sorry_category_check.clj" "--negative-unlabelled"]}
   {:name :c134-double-labelled
    :argv ["bb" "checks/lean_sorry_category_check.clj" "--negative-double"]}
   {:name :c134-label-on-proved
    :argv ["bb" "checks/lean_sorry_category_check.clj" "--negative-proved-label"]}
   {:name :c134-missing-checker
    :argv ["bb" "checks/lean_sorry_category_check.clj" "--negative-missing-checker"]}
   {:name :c137-missing-obligation-fixture
    :argv ["bb" "checks/lean_sorry_category_check.clj" "--negative-missing-fixture"]}
   {:name :c137-obligation-fixture-drift
    :argv ["bb" "checks/lean_sorry_category_check.clj" "--negative-fixture-drift"]}
   {:name :c138-status-population-sources
    :argv ["python3" "scripts/wm_status_report.py" "--source-control"]}
   {:name :c143-missing-record-pointer
    :argv ["bb" "checks/closed_record_pointer_check.clj" "--negative"]}
   {:name :c147-collapsed-eig-equality
    :argv ["bb" "checks/model_uncertainty_eig_witness.clj" "--negative"]}
   {:name :c154-referent-content-change :dir "/home/joe/code/p4ng"
    :argv ["python3" "detect_drift.py" "--control-unit-change"]}
   {:name :c165-unindexed-correction
    :argv ["bb" "checks/cleanup_queue_correction_index.clj" "--negative-control"]}
   {:name :c173-tampered-operational-run
    :argv ["bb" "-cp" "." "checks/wm_operational_certificate.clj"
           "--run" c167-run "--resource" c167-resource
           "--run-sha256" c167-run-sha256 "--resource-sha256" c167-resource-sha256
           "--negative-run-record"]}])

(defn run-one [{:keys [name argv dir]}]
  (println "wm-workspace-gate: RUN" (clojure.core/name name))
  (let [opts (cond-> {:continue true :out :inherit :err :inherit} dir (assoc :dir dir))
        result (apply process/shell opts argv)]
    {:name name :exit (:exit result)}))

(defn -main [& _]
  (let [basis (provenance)
        _ (println "wm-workspace-gate: PROVENANCE" (pr-str basis))
        inventory (inventory-result)
        _ (println "wm-workspace-gate: INVENTORY" (pr-str inventory))
        results (into [inventory] (map run-one (concat (commands) (control-commands))))
        failures (filterv #(not= 0 (:exit %)) results)]
    (println "wm-workspace-gate: SUMMARY"
             (pr-str {:checks (count results) :executable-checks (dec (count results)) :failures failures
                      :manual-exclusions [:lane-registry :current-live-operational-certificate]
                      :manual-exclusion-reasons
                      {:lane-registry :dispatcher-discipline-not-repository-validity
                       :current-live-operational-certificate :requires-new-operator-run-and-resource-receipt}}))
    (System/exit (if (empty? failures) 0 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
