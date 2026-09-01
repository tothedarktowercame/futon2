# C315 — hybrid-verdict tail sample

Date: 2026-09-01

Discovery only. No sampled program was converted.

## Sampling method

C293's lexical tail contained 90 programs selected by mutable-boundary tokens
but not examined. Fifteen were sampled before any conversion, stratified as:

- five nominally single-artifact witnesses;
- five corpus/registry checks whose helper structure can hide observations;
- five operational/generator scripts.

“Reachable” means two mutable observations can contribute to one verdict or
readiness claim without a common captured read-set or movement witness. It does
not require reproducing the race.

## Results

### Single-artifact witness stratum

| Program | Classification | Claim |
|---|---|---|
| `ablation_exact_dyadic_witness.clj` | No hybrid window found: one fixture read; mutation is in-memory. | Content-shaped; endpoint content is sufficient. |
| `belief_state_witness.clj` | Reachable: fixture read is combined with a live mathlib Lean invocation. | Content-shaped, but both fixture and Lean basis must be captured/pinned. |
| `cascade_diff_witness.clj` | No hybrid window found: fixture plus `git show` of a named immutable blob. | Content-shaped; pinned object identity supplies the second basis. |
| `channel_witness.clj` | Reachable: fixture plus live Lean invocation. | Content-shaped. |
| `expected_free_energy_witness.clj` | Reachable: fixture plus live Lean invocation. | Content-shaped. |

Rate: 3/5 reachable.

### Corpus/registry stratum

| Program | Classification | Claim |
|---|---|---|
| `cleanup_queue_correction_index.clj` | No hybrid window found: one queue file is read once. | Content-shaped. |
| `fold_turn_quarantine_check.clj` | Reachable: quarantine EDN is read, then `load-deposits` enumerates and reads the mutable deposit corpus. | Content-shaped population claim. |
| `obligation_ledger_reconciliation_check.clj` | No hybrid window found: one ledger read supplies text and digest. | Content-shaped. |
| `r2_pinned_snapshot_witness.clj` | No hybrid window found: one committed fixture read. | Content-shaped. |
| `machine_vocabulary_witness.clj` | Reachable: fixture read plus one or two live Lean invocations; negative mode can observe a third state. | Content-shaped. |

Rate: 2/5 reachable.

### Operational/generator stratum

| Program | Classification | Claim |
|---|---|---|
| `generate_variable_situation_accounting.bb` | Reachable: contract, witness registry, and glossary are read independently; contract/glossary are reread for hashes. | Content-shaped generated artifact. |
| `merge_edges.bb` | Reachable: fragment directory is enumerated/read separately from schema EDN. | Content-shaped generated artifact. |
| `work_units.bb` | Reachable: contract, witness registry, and edge census are three independent live reads. | Content-shaped generated artifact. |
| `wm_preflight.clj` | Reachable: configuration/defaults and a dynamically enumerated deposit corpus form a “right now” readiness verdict. | Event/current-readiness shaped; `:content-current` alone is insufficient. |
| `wm_scheduled_run.clj` | Out of population: this is a production execution entrypoint, not a verification verdict. It intentionally samples live state and emits a trace. | Runtime observation semantics, not a check claim. |

Rate: 4/4 among applicable verification/readiness programs; one lexical false
positive.

## Rate and consequence

Overall applicable rate: **9/14 = 64% reachable**. Including the non-check as
non-reachable would still be 9/15 = 60%. This is comparable to C293's 15/23
high-risk rate (65%), so the lexical triage did not isolate a low-risk tail.
The remaining 75 cannot be closed on the strength of the heuristic.

Five sampled checks are already conceptually content-only and need at most an
explicit `:content-current` declaration around their existing single or
immutable-pinned observation: ablation, cascade diff, cleanup queue,
obligation ledger, and R2 pinned snapshot. That is a classification, not a
conversion performed here.

The nine reachable cases are mostly content checks that can use captured
read-sets, but `wm_preflight` is materially different: endpoint equality does
not prove that no readiness-changing event occurred. It needs a fence/revision
or an explicitly interval-scoped result.

## Tail status

Seventy-five lexical candidates remain unexamined. The original population
also needs normalization to remove production runners before any extrapolated
count is used. The evidence supports continuing the audit or building a
population-aware lint; it does not support leaving the 75 as low-risk.
