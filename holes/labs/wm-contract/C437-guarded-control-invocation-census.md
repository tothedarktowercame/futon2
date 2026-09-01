# C437 — guarded-control invocation census

**Date:** 2026-09-01  
**Basis:** futon2 `97fcaf3b`; mathlib4 `444c22e9`

## Result

The 32 `:lean-guard-msgs` fixtures are individually reachable through 20 focused
wrapper programs, and every negative mode is explicitly invoked by
`checks/wm_workspace_gate.clj`. No fixture is unwired. However, a wrapper's
ordinary positive invocation does **not** run its guarded negative modes. In the
repository's automated wiring, all **32/32 are gate-only negatives**; focused
execution requires a person or packet to supply the particular negative flag.

| Focused wrapper | Guarded fixtures | Automated negative invoker |
|---|---:|---|
| `machine_vocabulary_witness.clj` | 4 | workspace gate only |
| `dirichlet_concentrations_witness.clj` | 3 | workspace gate only |
| `observation_vector_witness.clj` | 2 | workspace gate only |
| `parameter_posterior_kernel_witness.clj` | 2 | workspace gate only |
| `parameter_prior_kernel_witness.clj` | 2 | workspace gate only |
| `predictive_outcome_kernel_witness.clj` | 2 | workspace gate only |
| `preference_distribution_witness.clj` | 2 | workspace gate only |
| `transition_kernel_witness.clj` | 2 | workspace gate only |
| `model_reduction_free_energy_change_witness.clj` | 2 | workspace gate only |
| `fold_witness.clj` | 1 | workspace gate only |
| `generative_model_witness.clj` | 1 | workspace gate only |
| `have_want_arrow_witness.clj` | 1 | workspace gate only |
| `policy_prior_kernel_witness.clj` | 1 | workspace gate only |
| `precision_witness.clj` | 1 | workspace gate only |
| `variational_free_energy_witness.clj` | 1 | workspace gate only |
| `bayes_factor_threshold_witness.clj` | 1 | workspace gate only |
| `bayesian_model_reduction_witness.clj` | 1 | workspace gate only |
| `model_uncertainty_eig_witness.clj` | 1 | workspace gate only |
| `prediction_error_witness.clj` | 1 | workspace gate only |
| `softmax_witness.clj` | 1 | workspace gate only |

The focused entry points are real: each wrapper accepts the mode used by the
gate, and the C434 run established that all 32 guarded fixtures currently exit 0
with their recorded diagnostic. “Gate-only” therefore describes scheduling,
not an inability to run the check independently.

## Interpretation

- **Focused and automatically invoked outside the full gate:** 0.
- **Automatically invoked only by the full gate:** 32.
- **Invoked by nothing:** 0.

C277 occupied the second category. Its negative was soundly guarded and its
wrapper would have reported `mutation-slipped`, but no focused closure command
selected `--negative-value` during the 1:29:08 interval. The full gate was the
first automated caller to do so.

This census does not recommend putting 32 Lean elaborations on every commit.
Gate-only may be the correct cost choice; the measurable risk is that negative
coverage is no fresher than the last completed full gate. A packet that changes a
witness or its dependency can close that local gap cheaply by invoking only the
affected wrapper's registered negative modes.
