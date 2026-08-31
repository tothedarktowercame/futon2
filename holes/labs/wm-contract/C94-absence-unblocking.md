# C94 — unblock the absence population

Date: 2026-08-31. Starting lint count: 16.

## Groups

| Required change | Sites | Count | State |
|---|---|---:|---|
| observation/status consumption through diagnostics and policy | observation projection; pragmatic risk; epistemic diagnostic; infer-mode; policy; belief aggregation | 7 | blocked on a scoring/pass-through decision |
| absent avoidance semantics | `free_energy.clj:31,70` | 2 | blocked on whether absence abstains or contributes |
| validated precision-error producer | `precision.clj:166-167,206` | 2 | blocked: current public tests intentionally permit error-only records |
| validated prediction triple | `free_energy.clj:98-100` | 1 | blocked on caller migration/refusal |
| variance provenance | `forward_model.clj:339` | 1 | **fixed** |
| validated rollout step | `rollout.clj:129` | 1 | blocked on producer validation |
| unscored-move refusal | `rollout.clj:158` | 1 | blocked on selection semantics |
| adapter no-measurement variant | `adapters/fulab.clj:81` | 1 | blocked on adapter context type |

The largest ratio group is not automatable because it requires deciding how absent observations affect live scores. The cheapest decided group is variance provenance: `predict` already documents channels omitted by the action variance model as deterministic. C94 retains numeric zero for the mathematics and emits a parallel `:variance-status` map. A supplied variance is `{:status :present :value n}`; an omitted one is `{:status :absent :reason :deterministic-by-action-model}`. No historical value is reconstructed and no arithmetic changes.

Control: `variance-presence-is-not-coerced-away-test` requires an omitted `:loop-health` variance to remain numerically zero while carrying the reason-bearing absence. The disposition coverage test requires all 18 C12 rows to remain represented.

Canonical lint: `bb -cp . checks/preemptive_absence_coercion_lint.clj`; expected live result after C94 is 15 and exit 1. Negative control appends `--negative` and exits 0 only when the injected missing-to-zero mutation is rejected.

Gates: forward-model control 24 tests / 72 assertions; futon2 1,024 tests / 6,157 assertions; futon3 248 tests / 1,518 assertions; all completed with zero failures/errors. The preemptive-repair suite remains green while reporting the 15 blocked absence sites.
