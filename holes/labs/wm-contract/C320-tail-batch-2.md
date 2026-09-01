# C320 — hybrid-verdict tail, second batch

Date: 2026-09-01

Discovery only. No sampled program was converted or given a claim declaration.

## Method

The next twenty verification programs were taken from the 75 candidates left
after C315. A window is “reachable” when one verdict combines two observations
of mutable inputs without a common captured read-set, immutable identity, or
movement witness. The audit classifies the claim the program actually makes;
it does not require reproducing each race.

## Results

| Programs | Count | Reachable mechanism | Claim |
|---|---:|---|---|
| `absence_scoring_counterfactual.clj` | 1 | It enumerates and parses the mutable trace corpus, then separately `slurp`s the same paths to compute `:pin-input`. The classifications and reported digest can therefore describe different bytes; the directory population can also move. | Content-shaped corpus measurement. |
| `ambiguity_witness.clj`, `bayes_factor_threshold_witness.clj`, `bayesian_model_reduction_witness.clj`, `dirichlet_concentrations_witness.clj`, `expected_information_gain_witness.clj`, `have_want_arrow_witness.clj`, `log_multivariate_beta_witness.clj`, `observation_kernel_witness.clj`, `parameter_posterior_kernel_witness.clj`, `parameter_prior_kernel_witness.clj`, `policy_prior_kernel_witness.clj`, `precision_witness.clj`, `prediction_error_witness.clj`, `predictive_outcome_kernel_witness.clj`, `predictive_outcome_risk_witness.clj`, `preference_distribution_witness.clj`, `softmax_witness.clj`, `transition_kernel_witness.clj`, `variational_free_energy_witness.clj` | 19 | Each reads a serialized witness fixture and separately invokes Lean against the live mathlib worktree. The fixture and proof source can change independently, so one verdict can combine states that never coexisted. | Content-shaped fixture/proof agreement. |

Batch rate: **20/20 reachable (100%)**. Classification: **20 content-shaped,
0 event-shaped, 0 neither**. This is not evidence that a bare
`:content-current` declaration is sufficient: each program first needs its
multiple inputs captured as one attributable observation (or pinned by
immutable identity). The declaration then states the intended claim.

The rate is not lower than either earlier population: C293 found 15/23 (65%)
in the nominally high-risk set and C315 found 9/14 (64%) in its first tail
sample. This second batch strengthens the conclusion that “low-risk” carried
no useful information.

## Drain participation

Seventeen of the nineteen fixture/Lean witnesses are executable entries in
`checks/wm_workspace_gate.clj`; `expected_information_gain_witness.clj` and
`log_multivariate_beta_witness.clj` are inventory-known but not commands.
`absence_scoring_counterfactual.clj` is likewise inventory-known rather than
an executable gate entry.

Thus **17/20 participate indirectly in the drain**: they help produce the
workspace-gate receipt that `run-readiness` consumes. None is a direct live
readiness probe like `wm_preflight`, and readiness does not rerun them when it
accepts a current receipt. Their content windows are operationally covered by
the drain's held writer fence, but that conditional protection does not turn
the checks into independently snapshot-consistent instruments.

## Remaining population

Fifty-five candidates remain unexamined. They must not inherit the retired
“low-risk” label. This batch made no conversions, and it does not alter the
standing result that the drain fence covers the known population
operationally while the verification audit remains incomplete.
