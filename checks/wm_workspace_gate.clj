#!/usr/bin/env bb
(ns checks.wm-workspace-gate
  (:require [babashka.process :as process]
            [writer-fence-capability :as fence]
            [babashka.fs :as fs]
            [clojure.set :as set]
            [clojure.string :as str]
            [cheshire.core :as json]))

(def contract "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json")
(def c167-run "holes/labs/wm-contract/tick-run-record-2026-08-31.edn")
(def c167-resource "holes/labs/wm-contract/C167-v20-certificate-resource.edn")
(def c167-run-sha256 "3d4432d09934517811cda1b1b35d7a5a9c1bbc73f137d76f4aecb30f6ab07875")
(def c167-resource-sha256 "caaa479309506839b37611d9f2931d77bfb3ef8b75cb4a40826b18bf550319cf")

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

(def provenance-identity-fields
  [:git-sha :tree-sha :dirty? :tracked-diff-sha256 :readable?])

(defn provenance-movement [start finish]
  (let [by-repo #(into {} (map (juxt :repository identity) %))
        starts (by-repo start)
        finishes (by-repo finish)
        names (sort (set/union (set (keys starts)) (set (keys finishes))))
        observations
        (mapv (fn [repo]
                (let [before (get starts repo)
                      after (get finishes repo)
                      unreadable? (or (not (:readable? before))
                                      (not (:readable? after)))
                      changed (when (and before after)
                                (filterv #(not= (get before %) (get after %))
                                         provenance-identity-fields))]
                  {:repository repo
                   :status (cond
                             (or (nil? before) (nil? after) unreadable?) :unavailable
                             (seq changed) :moved
                             :else :stable)
                   :changed-fields (or changed [])}))
              names)]
    {:status (cond
               (some #(= :unavailable (:status %)) observations) :unavailable
               (some #(= :moved (:status %)) observations) :moved
               :else :stable)
     :repositories observations}))

(defn print-provenance-result! [start finish]
  (let [movement (provenance-movement start finish)]
    ;; Keep PROVENANCE as the start observation: run-readiness consumes this
    ;; established line. The finish and comparison make a raw invocation
    ;; readable without changing its check verdict.
    (println "wm-workspace-gate: PROVENANCE-FINISH" (json/generate-string finish))
    (println "wm-workspace-gate: BASIS" (pr-str movement))
    (when (not= :stable (:status movement))
      (println "wm-workspace-gate: BASIS-NOT-STABLE" (pr-str movement)))
    movement))

(defn gate-event-claim [movement writer-fence-id writer-fence-evidence started-at finished-at]
  (fence/assess {:started-at started-at :finished-at finished-at}
                (not= :stable (:status movement))
                writer-fence-id writer-fence-evidence))

(defn provenance-movement-control! []
  (let [tmp (str (fs/create-temp-dir {:prefix "wm-gate-basis-control-"}))
        git (fn [& argv]
              (apply process/shell {:continue true :out :string :err :string :dir tmp}
                     "git" argv))]
    (try
      (git "init" "-q")
      (git "config" "user.email" "wm-gate-control@example.invalid")
      (git "config" "user.name" "WM gate control")
      (spit (str (fs/path tmp "basis.txt")) "before\n")
      (git "add" "basis.txt")
      (git "commit" "-q" "-m" "control basis before")
      (let [start [(repository-provenance [:control tmp])]]
        ;; This real commit is the deliberate mid-run movement. The synthetic
        ;; inner verdict stays passing to prove movement is reported
        ;; independently of check outcomes.
        (spit (str (fs/path tmp "basis.txt")) "after\n")
        (git "add" "basis.txt")
        (git "commit" "-q" "-m" "control basis after")
        (let [finish [(repository-provenance [:control tmp])]
              movement (print-provenance-result! start finish)
              passed? (= :moved (:status movement))]
          (println "wm-workspace-gate: PROVENANCE-CONTROL"
                   (pr-str {:inner-verdict :pass
                            :movement (:status movement)
                            :exit-convention "0-control-rejected/2-control-slipped"}))
          (if passed? 0 2)))
      (finally
        (fs/delete-tree tmp)))))

(defn authority []
  (get-in (json/parse-string (slurp contract) true) [:source :git-sha]))

(def known-check-files
  ;; Discovery is a completeness alarm, not an execution policy.  A new file
  ;; must be classified here before the gate can pass; it is never guessed safe.
  #{"ablation_exact_dyadic_witness.clj" "absence_scoring_counterfactual.clj"
    "c130_immediate_option_measurement.clj"
    "ambiguity_witness.clj" "belief_state_witness.clj" "channel_witness.clj"
    "observation_vector_witness.clj" "observation_kernel_witness.clj"
    "predictive_outcome_risk_witness.clj"
    "policy_prior_kernel_witness.clj"
    "q_interface_completeness_check.clj"
    "variational_free_energy_witness.clj"
    "precision_witness.clj"
    "prediction_error_witness.clj"
    "softmax_witness.clj"
    "bayes_factor_threshold_witness.clj"
    "bayesian_model_reduction_witness.clj"
    "model_reduction_free_energy_change_witness.clj"
    "dirichlet_concentrations_witness.clj"
    "preference_distribution_witness.clj"
    "predictive_outcome_kernel_witness.clj"
    "parameter_prior_kernel_witness.clj"
    "parameter_posterior_kernel_witness.clj"
    "transition_kernel_witness.clj"
    "absent_is_loud_lint.clj" "belief_update_check.clj"
    "belief_variance_inputs.clj" "cascade_diff_witness.clj" "certify_live_run.clj" "closed_record_pointer_check.clj" "contract_lint.clj"
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
    "model_uncertainty_eig_witness.clj" "machine_vocabulary_witness.clj"
    "mutable_read_set.clj"
    "lean_positive_witness.clj"
    "lean_sorry_category_check.clj"
    "obligation_ledger_reconciliation_check.clj"
    "preemptive_absence_coercion_lint.clj" "preemptive_acceptance_lint.clj"
    "preemptive_artefact_boundary_lint.clj" "preemptive_era_blind_lint.clj"
    "preemptive_record_conflict_lint.clj" "preemptive_repair_lint.clj"
    "preemptive_repair_suite.clj" "preemptive_stale_baseline_lint.clj"
    "exit_code_scope_check.clj"
    "repository_census_basis_check.clj"
    "preference_stack_binding_check.clj" "preference_stack_witness_shape_check.clj"
    "positive_proof_receipt.clj"
    "r17_generator_disposer_check.clj" "r19_stack_witness.clj"
    "r2_channel_contract.clj" "r2_pinned_snapshot_witness.clj"
    "r8_f_contract.clj" "r8_pinned_snapshot_witness.clj"
    "trace_schema_compatibility.clj"
    "r9_independence.clj" "r9_proof_receipt_check.clj"
    "wm_click_resource_observer.clj" "wm_operational_certificate.clj" "wm_route_conformance.clj"
    "wm_runs_once_witness.clj" "wm_workspace_gate.clj"})

(defn inventory-result []
  (let [found (set (map (comp str fs/file-name) (fs/glob "checks" "*.clj")))
        unknown (sort (set/difference found known-check-files))
        missing (sort (set/difference known-check-files found))]
    {:name :check-inventory
     :exit (if (and (empty? unknown) (empty? missing)) 0 1)
     :unknown unknown :missing missing}))

(defn content-only-authority-argv [& args]
  (into ["env" "-u" "FUTON_WRITER_FENCE_ID"
         "-u" "FUTON_WRITER_FENCE_EVIDENCE"
         "bb" "checks/contract_authority_current.clj"]
        args))

(defn commands []
  [{:name :strict-contract
    :argv ["bb" "-cp" "." "checks/contract_lint.clj" "--strict"
           "--contract" contract "--registry" "checks/witness-registry.edn"
           "--report" "/tmp/wm-workspace-contract-strict.edn"
           "--authority" (authority)]}
   {:name :contract-authority-current
    :argv (content-only-authority-argv)}
   {:name :mutable-verdict-claims
    :argv ["bb" "-cp" "." "scripts/check_mutable_verdict_claims.bb"]}
   {:name :exit-code-scopes
    :argv ["bb" "-cp" "." "checks/exit_code_scope_check.clj"]}
   {:name :repository-census-bases
    :argv ["bb" "-cp" "." "checks/repository_census_basis_check.clj"]}
   {:name :ambiguity :argv ["bb" "checks/ambiguity_witness.clj"]}
   {:name :have-want-arrow :argv ["bb" "checks/have_want_arrow_witness.clj"]}
   {:name :fold :argv ["bb" "checks/fold_witness.clj"]}
   {:name :belief-state :argv ["bb" "checks/belief_state_witness.clj"]}
   {:name :observation-vector :argv ["bb" "checks/observation_vector_witness.clj"]}
   {:name :channel-vocabulary :argv ["bb" "checks/channel_witness.clj"]}
   {:name :observation-kernel :argv ["bb" "checks/observation_kernel_witness.clj"]}
   {:name :predictive-outcome-risk :argv ["bb" "checks/predictive_outcome_risk_witness.clj"]}
   {:name :policy-prior-kernel :argv ["bb" "checks/policy_prior_kernel_witness.clj"]}
   {:name :q-interface-completeness
    :argv ["bb" "checks/q_interface_completeness_check.clj"]}
   {:name :variational-free-energy :argv ["bb" "checks/variational_free_energy_witness.clj"]}
   {:name :precision :argv ["bb" "checks/precision_witness.clj"]}
   {:name :prediction-error :argv ["bb" "checks/prediction_error_witness.clj"]}
   {:name :softmax :argv ["bb" "checks/softmax_witness.clj"]}
   {:name :bayes-factor-threshold :argv ["bb" "checks/bayes_factor_threshold_witness.clj"]}
   {:name :bayesian-model-reduction :argv ["bb" "checks/bayesian_model_reduction_witness.clj"]}
   {:name :model-reduction-free-energy-change
    :argv ["bb" "checks/model_reduction_free_energy_change_witness.clj"]}
   {:name :dirichlet-concentrations :argv ["bb" "checks/dirichlet_concentrations_witness.clj"]}
   {:name :preference-distribution :argv ["bb" "checks/preference_distribution_witness.clj"]}
   {:name :predictive-outcome-kernel :argv ["bb" "checks/predictive_outcome_kernel_witness.clj"]}
   {:name :parameter-prior-kernel :argv ["bb" "checks/parameter_prior_kernel_witness.clj"]}
   {:name :parameter-posterior-kernel :argv ["bb" "checks/parameter_posterior_kernel_witness.clj"]}
   {:name :transition-kernel :argv ["bb" "checks/transition_kernel_witness.clj"]}
   {:name :control-vocabulary :argv ["bb" "checks/machine_vocabulary_witness.clj" "--term" "control"]}
   {:name :aliveness :argv ["bb" "checks/machine_vocabulary_witness.clj" "--term" "aliveness"]}
   {:name :act-gate-vocabulary :argv ["bb" "checks/machine_vocabulary_witness.clj" "--term" "act-gate"]}
   {:name :cohort-vocabulary :argv ["bb" "checks/machine_vocabulary_witness.clj" "--term" "cohort"]}
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
   {:name :c130-immediate-option-measurement
    :argv ["bb" "-cp" "." "checks/c130_immediate_option_measurement.clj"]}
   {:name :reload-click-certificate-rehearsal
    :dir "/home/joe/code/futon3c"
    :argv ["clojure" "-M:test:test-all" "-i" ":slow" "-n"
           "futon3c.wm.chain-rehearsal-test"]}
   {:name :pinned-operational-certificate
    :argv ["bb" "-cp" "." "checks/wm_operational_certificate.clj"
           "--run" c167-run "--resource" c167-resource
           "--run-sha256" c167-run-sha256 "--resource-sha256" c167-resource-sha256]}
   {:name :p4ng-referent-drift :dir "/home/joe/code/p4ng"
    :argv ["python3" "detect_drift.py"]}
   {:name :cleanup-queue-corrections
    :argv ["bb" "checks/cleanup_queue_correction_index.clj"]}
   ;; Exit 3 is the explicit report-only verdict; 0 remains clean and 2 remains
   ;; unavailable. The blocking self-test is a separate command below.
   {:name :live-artifact-format-boundaries
    :argv ["python3" "checks/live_artifact_format_boundary_lint.py" "--report"]
    :expected-exits #{0 3}}
   ;; Report-only while independently owned acceptance boundaries are repaired.
   ;; Missing inputs remain a blocking instrument failure; findings stay visible.
   {:name :empty-subject-acceptance
    :argv ["python3" "checks/empty_subject_acceptance_lint.py" "--report"]
    :expected-exits #{0 3}}])

(defn control-commands []
  [{:name :c390-report-only-crosses-lossy-boundary
    :argv ["bb" "-cp" "." "checks/exit_code_scope_check.clj" "--negative-control"]}
   {:name :c393-malformed-census-basis
    :argv ["bb" "-cp" "." "checks/repository_census_basis_check.clj" "--negative-control"]}
   {:name :c157-perturbed-entropy
    :argv ["bb" "checks/ambiguity_witness.clj" "--negative-control"]}
   {:name :c192-belief-channel-without-variance
    :argv ["bb" "checks/belief_state_witness.clj" "--negative-control"]}
   {:name :c205-missing-declared-channel
    :argv ["bb" "checks/channel_witness.clj" "--negative-control"]}
   {:name :c196-observation-row-not-normalized
    :argv ["bb" "checks/observation_kernel_witness.clj" "--negative-normalisation"]}
   {:name :c196-observation-negative-mass
    :argv ["bb" "checks/observation_kernel_witness.clj" "--negative-mass"]}
   {:name :c200-zero-preference-on-predictive-support
    :argv ["bb" "checks/predictive_outcome_risk_witness.clj" "--negative-control"]}
   {:name :c208-state-conditioned-policy-prior
    :argv ["bb" "checks/policy_prior_kernel_witness.clj" "--negative-control"]}
   {:name :q-interface-missing-remediation
    :argv ["bb" "checks/q_interface_completeness_check.clj" "--negative-control"]}
   {:name :c212-perturbed-variational-f
    :argv ["bb" "checks/variational_free_energy_witness.clj" "--negative-value"]}
   {:name :c212-expected-f-as-variational-f
    :argv ["bb" "checks/variational_free_energy_witness.clj" "--negative-type"]}
   {:name :c332-weakened-variational-positive
    :argv ["bb" "checks/variational_free_energy_witness.clj" "--negative-weakened-positive"]}
   {:name :c332-unrelated-variational-edit
    :argv ["bb" "checks/variational_free_energy_witness.clj" "--unrelated-positive-edit"]}
   {:name :c217-swapped-precision-and-error
    :argv ["bb" "checks/precision_witness.clj" "--negative-swap"]}
   {:name :c217-signed-error-as-precision
    :argv ["bb" "checks/precision_witness.clj" "--negative-type"]}
   {:name :c224-observation-as-prediction-error
    :argv ["bb" "checks/prediction_error_witness.clj" "--negative-operand"]}
   {:name :c224-reversed-prediction-error-sign
    :argv ["bb" "checks/prediction_error_witness.clj" "--negative-sign"]}
   {:name :c232-softmax-inverted-order
    :argv ["bb" "checks/softmax_witness.clj" "--negative-order"]}
   {:name :c232-softmax-not-normalised
    :argv ["bb" "checks/softmax_witness.clj" "--negative-normalisation"]}
   {:name :c236-bayes-threshold-boundary
    :argv ["bb" "checks/bayes_factor_threshold_witness.clj" "--negative-boundary"]}
   {:name :c236-variational-f-as-bmr-evidence
    :argv ["bb" "checks/bayes_factor_threshold_witness.clj" "--negative-type"]}
   {:name :c240-bmr-count-conservation
    :argv ["bb" "checks/bayesian_model_reduction_witness.clj" "--negative-counts"]}
   {:name :c245-empty-dirichlet-domain
    :argv ["bb" "checks/dirichlet_concentrations_witness.clj" "--negative-empty"]}
   {:name :c245-zero-dirichlet-concentration
    :argv ["bb" "checks/dirichlet_concentrations_witness.clj" "--negative-zero"]}
   {:name :c245-negative-dirichlet-concentration
    :argv ["bb" "checks/dirichlet_concentrations_witness.clj" "--negative-negative"]}
   {:name :c252-state-conditioned-preferences
    :argv ["bb" "checks/preference_distribution_witness.clj" "--negative-conditioning"]}
   {:name :c252-pragmatic-cost-as-preferences
    :argv ["bb" "checks/preference_distribution_witness.clj" "--negative-pragmatic-cost"]}
   {:name :c257-unconditional-as-predictive-q
    :argv ["bb" "checks/predictive_outcome_kernel_witness.clj" "--negative-unconditional"]}
   {:name :c257-softmax-as-predictive-q
    :argv ["bb" "checks/predictive_outcome_kernel_witness.clj" "--negative-softmax"]}
   {:name :c261-uncontrolled-as-transition-b
    :argv ["bb" "checks/transition_kernel_witness.clj" "--negative-uncontrolled"]}
   {:name :c261-beta-normalizer-as-transition-b
    :argv ["bb" "checks/transition_kernel_witness.clj" "--negative-beta"]}
   {:name :c265-outcome-q-as-parameter-prior
    :argv ["bb" "checks/parameter_prior_kernel_witness.clj" "--negative-outcome"]}
   {:name :c265-habit-q-as-parameter-prior
    :argv ["bb" "checks/parameter_prior_kernel_witness.clj" "--negative-habit"]}
   {:name :c270-prior-q-as-parameter-posterior
    :argv ["bb" "checks/parameter_posterior_kernel_witness.clj" "--negative-prior"]}
   {:name :c270-outcome-q-as-parameter-posterior
    :argv ["bb" "checks/parameter_posterior_kernel_witness.clj" "--negative-outcome"]}
   {:name :c274-empty-contract-lint
    :argv ["bb" "checks/contract_lint.clj" "--negative-empty"
           "--contract" "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json"
           "--registry" "checks/witness-registry.edn"
           "--report" "/tmp/wm-gate-empty-contract.edn"
           "--authority" "b475bc72dcf6a91f8a90fb1da1bb8a8058e9a7f4"]}
   {:name :c274-empty-holder-contract
    :argv ["bb" "checks/holder_check.clj" "--negative-empty"]}
   {:name :c274-empty-witness-fragments
    :argv ["bb" "scripts/merge_witnesses.bb" "--negative-empty"]}
   {:name :c274-empty-q-interface
    :argv ["bb" "checks/q_interface_completeness_check.clj" "--negative-empty"]}
   {:name :c274-empty-model-contract
    :argv ["bb" "scripts/generate_variable_situation_accounting.bb" "--negative-empty"]}
   {:name :c277-perturbed-reduction-free-energy
    :argv ["bb" "checks/model_reduction_free_energy_change_witness.clj" "--negative-value"]}
   {:name :c277-variational-f-as-reduction-change
    :argv ["bb" "checks/model_reduction_free_energy_change_witness.clj" "--negative-type"]}
   {:name :c282-partial-observation-vector
    :argv ["bb" "checks/observation_vector_witness.clj" "--negative-partial"]}
   {:name :c282-single-outcome-as-vector
    :argv ["bb" "checks/observation_vector_witness.clj" "--negative-outcome"]}
   {:name :c179-control-outside-vocabulary :argv ["bb" "checks/machine_vocabulary_witness.clj" "--term" "control" "--negative-control"]}
   {:name :c179-negative-aliveness-factor :argv ["bb" "checks/machine_vocabulary_witness.clj" "--term" "aliveness" "--negative-control"]}
   {:name :c179-missing-act-gate-leg :argv ["bb" "checks/machine_vocabulary_witness.clj" "--term" "act-gate" "--negative-control"]}
   {:name :c179-invalid-cohort-window :argv ["bb" "checks/machine_vocabulary_witness.clj" "--term" "cohort" "--negative-control"]}
   {:name :c175-stale-contract-authority
    :argv (content-only-authority-argv "--negative-control")}
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
   {:name :c218-referent-concurrent-modification :dir "/home/joe/code/p4ng"
    :argv ["python3" "detect_drift.py" "--control-concurrent-modification"]}
   {:name :c165-unindexed-correction
    :argv ["bb" "checks/cleanup_queue_correction_index.clj" "--negative-control"]}
   {:name :c173-tampered-operational-run
    :argv ["bb" "-cp" "." "checks/wm_operational_certificate.clj"
           "--run" c167-run "--resource" c167-resource
           "--run-sha256" c167-run-sha256 "--resource-sha256" c167-resource-sha256
           "--negative-run-record"]}
   {:name :c284-format-proof-must-be-executable
    :argv ["python3" "checks/live_artifact_format_boundary_lint.py"
           "--negative-control"]}
   {:name :c361-empty-subject-lint-controls
    :argv ["python3" "checks/empty_subject_acceptance_lint.py" "--self-test"]}])

(defn run-one [{:keys [name argv dir expected-exits]
                :or {expected-exits #{0}}}]
  (println "wm-workspace-gate: RUN" (clojure.core/name name))
  (let [opts (cond-> {:continue true :out :inherit :err :inherit} dir (assoc :dir dir))
        result (apply process/shell opts argv)
        observed (:exit result)]
    {:name name :exit (if (contains? expected-exits observed) 0 observed)
     :observed-exit observed :expected-exits expected-exits}))

(defn -main [& args]
  (if (some #{"--provenance-control"} args)
    (System/exit (provenance-movement-control!))
    (let [basis-start (provenance)
        gate-started-at (str (java.time.Instant/now))
        writer-fence-id (System/getenv "FUTON_WRITER_FENCE_ID")
        writer-fence-evidence (System/getenv "FUTON_WRITER_FENCE_EVIDENCE")
        ;; JSON is part of the bounded receipt's consumable output. Readiness
        ;; must compare all four repositories, not only the wrapper's cwd.
        _ (println "wm-workspace-gate: PROVENANCE" (json/generate-string basis-start))
        inventory (inventory-result)
        _ (println "wm-workspace-gate: INVENTORY" (pr-str inventory))
        results (into [inventory] (map run-one (concat (commands) (control-commands))))
        failures (filterv #(not= 0 (:exit %)) results)
        basis-finish (provenance)
        movement (print-provenance-result! basis-start basis-finish)
        gate-finished-at (str (java.time.Instant/now))
        event-claim (gate-event-claim movement writer-fence-id writer-fence-evidence
                                      gate-started-at gate-finished-at)]
      (println "wm-workspace-gate: SUMMARY"
               (pr-str {:checks (count results) :executable-checks (dec (count results)) :failures failures
                        :basis-status (:status movement)
                        :basis-repositories (:repositories movement)
                        :event-claim event-claim
                        :verdict-qualification
                        (cond
                          (not= :stable (:status movement)) :repository-basis-moved
                          (= true (:event-free? event-claim)) :fence-conditional
                          :else :content-only-event-free-unverified)
                        :manual-exclusions [:lane-registry :current-live-operational-certificate
                                            :production-click-resource-observer
                                            :mutable-read-set-library]
                        :manual-exclusion-reasons
                        {:lane-registry :dispatcher-discipline-not-repository-validity
                         :current-live-operational-certificate :requires-new-operator-run-and-resource-receipt
                         :production-click-resource-observer :joe-only-command-that-performs-production-click
                         :mutable-read-set-library :support-namespace-exercised-by-consumer-controls-and-unit-tests}}))
      ;; Repository movement qualifies the observation but does not turn a
      ;; passing set of checks into a failing set. The bounded wrapper and
      ;; run-readiness impose the stricter stable-basis operator policy.
      (System/exit (if (empty? failures) 0 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
