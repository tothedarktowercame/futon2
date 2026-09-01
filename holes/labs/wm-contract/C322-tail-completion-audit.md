# C322 — mutable-verdict tail completion audit

Date: 2026-09-01

Discovery result: the residual is **not uniformly content-shaped**, and the
reported population of 55 cannot be reconstructed from an authoritative
candidate list. No check was converted and no event-shaped check was relabelled
as content.

## Population accounting

C293 recorded a lexical total (113 candidates, 90 in the tail), but did not
persist the candidate names. Its stated scope was executable programs under
`checks/`; C315 subsequently sampled `scripts/wm_scheduled_run.clj`, outside
that scope, and found it was not a check. Consequently the arithmetic tail of
55 is not an addressable population.

Reconstructing from the current top-level executable `checks` population gives:

- 79 programs, excluding the shared `mutable_read_set.clj` library;
- 58 historical program names extracted from C293, C315, and C320, of which
  five are outside or absent from the current top-level `checks` population;
- 53 current programs named by those audits and 26 current programs not named.

The 29-name difference between “55” and the reproducible 26 cannot be audited
without inventing membership. This is a population-provenance defect: a count
was retained while its members were not.

## Batch A — content-shaped witnesses and bounded records (12/12)

| Programs | Classification |
|---|---|
| `belief_update_check.clj`, `belief_variance_inputs.clj` | Content: bounded source/fixture evaluation. |
| `fold_witness.clj`, `model_reduction_free_energy_change_witness.clj`, `observation_vector_witness.clj` | Content: fixture plus live Lean source; reachable hybrid basis. |
| `preference_stack_witness_shape_check.clj`, `r19_stack_witness.clj` | Content: serialized witness shape/value. |
| `hyper_edge_domain_range_check.clj`, `hyper_edge_exemplar_check.clj` | Content: schema/instance agreement. |
| `lean_sorry_category_check.clj`, `r9_independence.clj` | Content: contract/source or pinned-corpus agreement. |
| `wm_runs_once_witness.clj` | Content: selected run-record witness. |

Distinct result: **12 content, 0 event, 0 neither**.

## Batch B — content populations and generated artefacts (12/14 content)

| Programs | Classification |
|---|---|
| `c130_immediate_option_measurement.clj`, `r8_f_contract.clj`, `trace_schema_compatibility.clj` | Content-shaped trace-corpus measurements. Directory movement can hybridise the population, but event freedom is not their claim. |
| `closed_record_pointer_check.clj`, `live_artifact_format_boundary_lint.py` | Content-shaped cross-file/source agreement. |
| `preemptive_absence_coercion_lint.clj`, `preemptive_acceptance_lint.clj`, `preemptive_artefact_boundary_lint.clj`, `preemptive_era_blind_lint.clj`, `preemptive_record_conflict_lint.clj`, `preemptive_stale_baseline_lint.clj` | Content-shaped scans over the captured preemptive-repair corpus. |
| `wm_operational_certificate.clj` | Content-shaped agreement among run, resource receipt, and pinned topology. |
| `contract_authority_current.clj` | **Event/current-authority shaped.** It combines several live Git observations and the contract file into a “current” verdict without one repository-basis interval. It is executable in the workspace gate. |
| `writer_fence_evidence.py` | **Event-shaped by design.** It measures whether the drain fence remained effective over an interval; endpoint content equality cannot express its claim. |

Distinct result: **12 content, 2 event, 0 neither**.

## Final split and consequence

The reproducible residual split is **24 content-shaped, 2 event-shaped, 0
neither**. Combined with C315 and C320, the examined tail is therefore not
uniform. `contract_authority_current.clj` is the material exception because it
feeds the workspace-gate receipt used by the drain. `writer_fence_evidence.py`
is already interval-shaped; it is evidence for the fence rather than an
unqualified point verdict.

The content-shaped programs are eligible for C310 `:content-current`
declarations, but this audit does not add dead metadata: a declaration must be
consumed and emitted by its caller. Bulk declarations are deferred until the
candidate registry is made authoritative and the 29 unnamed candidates are
either recovered or explicitly retired. Adding declarations to 24 known names
while calling an unreconstructable 55 complete would recreate the silently
lowered-population defect from C285.

Accordingly C317's limitation cannot be retired. It narrows to:

> The reproducible tail contains 24 content claims and two event claims.
> `contract_authority_current` still lacks an interval/basis witness, and the
> original lexical tail lacks a persisted 29-member remainder. Drain-fence
> conditionality covers concurrent writes operationally but does not establish
> a complete, independently attributable audit.
