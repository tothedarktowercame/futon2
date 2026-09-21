# improve-8 — run-ending classification kernel discovery

Discovery only, wm-author, 2026-09-21. Branch `fix/narrative-improve-8`, base
`142c898d`. No production changes, clicks, serving-JVM evaluation, or source
store writes. Reproduction: `improve-8-evidence/reproduce.clj`.

## Finding and cohort boundary

A closed attempt cannot presently be assigned to an increment class merely
from `007-closed.edn`. The close is the durable judgment, but `:grounded? true`
and `:outcome :grounded-change` are delivery statuses, not an attested
increment, and contain no focus relation. Conversely, a generic refusal or
failure-shaped close is not Joe's known non-delivery unless the observation
names a failure type.

The read-only source root `/home/joe/code/futon2/data` contains **124**
`007-closed.edn` record instances (including the explicitly retained
`archives/stop-line-2026-07-15` copies). This is the complete on-disk cohort
used below; path, rather than the historically reusable cohort/attempt pair,
is its record identity. There are **zero** sibling `retained/` directories.
Thus none has a route-attestation or kernel-example receipt, and none of the
selection checkpoints has a `focus-receipt`. The result is **2
`:known-typed-failure`, 122 `:unknown`, and zero in each increment class**.
This describes available evidence; it is not fitted to 55/35/5/5.

## 1. Evidence inventory

| Question | Admissible evidence for kernel v1 | What exists in this cohort |
|---|---|---|
| Was an increment delivered? | A matched, active, non-`:may-not` `:increment` row in `retained/route-attestation.edn`, whose `:attestation` is `:present`. Its criterion must identify the want and retain the registered-test warrant/evidence digest. `007-closed.edn` `:grounded?`, `:outcome`, `:witness`, commit count, authored commit, token observation, surprise, and kernel-example prediction are corroboration only. They do not substitute for the attestation. | No route receipts. The 29 `:grounded-change` closes therefore remain unknown, not increments. |
| On which facet? | The exact attested want/candidate joined to the time-valid `:wm/focus-receipt-v1` candidate row produced by `commit-facets-v1`. Only `:focus`, `:associated`, or `:useful-elsewhere` maps to the three increment classes. `:unknown`, absent/expired discovery, a target-name guess, embedding proximity, or prediction is not a facet. | No focus receipts and no attested wants. |
| Is non-delivery known and typed? | `:grounded? false`, `:artifact-only? false`, and an observed keyword `:failure-kind` in the durable close judgment (or a future equally explicit stop-line receipt bound into that close). Kernel v1 accepts the type as data; it does not maintain a semantic allow-list of “good” failures. | `:operator-terminated` on outer-loop-43/attempt-053 and `:evidence-not-single-edn` on machinery-55/attempt-003. |
| What does not count as known? | `:build-failed`, `:agent-unavailable`, `:substrate-unavailable`, `:guardrail-refusal`, `:incomplete`, or `:no-selection` without `:failure-kind`; a refusal status without a type; an unchanged prediction; missing observation; nil witness; or inferred silence. | 122 records lack either attested increment evidence or an explicit observed failure type. |

`kernel-example.edn`, `token-outcome.edn`, `surprises.edn`, learning-trial
receipts, and revision receipts provide aligned observations and later learning
evidence. They may be cited by the output, but they do not themselves establish
that an increment was institutionally attested. `route-attestation-v1` says its
checkpoint evidence is supplied, not independently checked; kernel v1 should
retain that verification level and must not upgrade it. If policy requires a
fresh registry check, that is an additional input and absent here.

## 2. Complete per-record classification

The table is generated in lexical path order. Full SHA-256 values are emitted
by the reproduction script; the compact table names the decisive field or
missing evidence. Archive records are not silently deduplicated because their
paths preserve distinct retained close judgments.

<!-- GENERATED TABLE: reproduce.clj stdout -->

| durable close record | close outcome | typed failure | proposed class | evidence / missing |
|---|---|---|---|---|
| `wm-full-loop-machinery-47/wm-contract-machinery-47-v1/attempt-002/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-47/wm-contract-machinery-47-v1/attempt-003/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-48/wm-contract-machinery-48-v1/attempt-001/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-49/wm-contract-machinery-49-v1/attempt-002/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-50/wm-contract-machinery-50-v1/attempt-001/007-closed.edn` | `:agent-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-50/wm-contract-machinery-50-v1/attempt-002/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-51/wm-contract-machinery-51-v1/attempt-001/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-51/wm-contract-machinery-51-v1/attempt-002/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-52/wm-contract-machinery-52-v1/attempt-001/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-52/wm-contract-machinery-52-v1/attempt-002/007-closed.edn` | `:agent-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-53/wm-contract-machinery-53-v1/attempt-002/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-54/wm-contract-machinery-54-v1/attempt-002/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-54/wm-contract-machinery-54-v1/attempt-003/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-55/wm-contract-machinery-55-v1/attempt-001/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-55/wm-contract-machinery-55-v1/attempt-002/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-55/wm-contract-machinery-55-v1/attempt-003/007-closed.edn` | `:build-failed` | `:evidence-not-single-edn` | `:known-typed-failure` | close sha256=f348df1a778165c6d80f7523043420120dce144d5cf3b865bde9f0cbb40da71c; :failure-kind=:evidence-not-single-edn |
| `wm-full-loop-machinery-56/wm-contract-machinery-56-v1/attempt-001/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-56/wm-contract-machinery-56-v1/attempt-002/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-57/wm-contract-machinery-57-v1/attempt-001/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-57/wm-contract-machinery-57-v1/attempt-002/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-58/wm-contract-machinery-58-v1/attempt-001/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-58/wm-contract-machinery-58-v1/attempt-002/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-59/wm-contract-machinery-59-v1/attempt-001/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-59/wm-contract-machinery-59-v1/attempt-002/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-60/wm-contract-machinery-60-v1/attempt-001/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-60/wm-contract-machinery-60-v1/attempt-002/007-closed.edn` | `:agent-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-61/wm-contract-machinery-61-v1/attempt-001/007-closed.edn` | `:agent-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-61/wm-contract-machinery-61-v1/attempt-002/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-62/wm-contract-machinery-62-v1/attempt-001/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-62/wm-contract-machinery-62-v1/attempt-002/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-63/wm-contract-machinery-63-v1/attempt-001/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-63/wm-contract-machinery-63-v1/attempt-002/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-64/wm-contract-machinery-64-v1/attempt-001/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-64/wm-contract-machinery-64-v1/attempt-002/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-65/wm-contract-machinery-65-v1/attempt-001/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-65/wm-contract-machinery-65-v1/attempt-002/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-66/wm-contract-machinery-66-v1/attempt-001/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-66/wm-contract-machinery-66-v1/attempt-002/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-67/wm-contract-machinery-67-v1/attempt-002/007-closed.edn` | `:guardrail-refusal` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-68/wm-contract-machinery-68-v1/attempt-001/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-68/wm-contract-machinery-68-v1/attempt-002/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-001/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-001/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-002/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-003/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-004/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-005/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-006/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-007/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-008/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-009/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-010/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-011/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-012/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-013/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-014/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-015/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-016/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-017/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-018/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-019/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-020/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-021/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-022/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-023/007-closed.edn` | `:agent-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-024/007-closed.edn` | `:no-selection` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-001/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-002/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-003/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-004/007-closed.edn` | `:agent-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-005/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-006/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-007/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-008/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-009/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-010/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-011/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-012/007-closed.edn` | `:agent-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-013/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-014/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-015/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-016/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-017/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-018/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-019/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-020/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-021/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-022/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-023/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-024/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-025/007-closed.edn` | `:agent-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-026/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-027/007-closed.edn` | `:agent-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-028/007-closed.edn` | `:substrate-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-029/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-030/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-031/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-032/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-033/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-034/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-035/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-036/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-037/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-40-v1/attempt-039/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-41-v1/attempt-041/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-41-v1/attempt-043/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-41-v1/attempt-044/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-41-v1/attempt-045/007-closed.edn` | `:agent-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-41-v1/attempt-046/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-41-v1/attempt-047/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-41-v1/attempt-048/007-closed.edn` | `:agent-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-41-v1/attempt-049/007-closed.edn` | `:agent-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-42-v1/attempt-050/007-closed.edn` | `:agent-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-42-v1/attempt-051/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-43-v1/attempt-052/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-43-v1/attempt-053/007-closed.edn` | `:incomplete` | `:operator-terminated` | `:known-typed-failure` | close sha256=e52c0ed894c76ba42571ad356b1784d466ebb260d11f5f4a9440ce47dc180ef6; :failure-kind=:operator-terminated |
| `wm-full-loop/wm-outer-loop-44-v1/attempt-054/007-closed.edn` | `:build-failed` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-44-v1/attempt-055/007-closed.edn` | `:substrate-unavailable` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-45-v1/attempt-056/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-45-v1/attempt-057/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-45-v1/attempt-058/007-closed.edn` | `:incomplete` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-46-v1/attempt-059/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-46-v1/attempt-060/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |
| `wm-full-loop/wm-outer-loop-46-v1/attempt-061/007-closed.edn` | `:grounded-change` | — | `:unknown` | missing `:attested-increment` |

## 3. Proposed kernel v1

Version id: `:wm/run-ending-classification-kernel-v1`. Mode:
`:record-only`. One output receipt attaches at
`<attempt>/retained/run-ending-classification.edn`, is admitted to the existing
close evidence manifest, and is referenced beside (not inside or in place of)
`route-attestation.edn`, `kernel-example.edn`, `token-outcome.edn`, and
`surprises.edn`.

Inputs are the exact `007-closed.edn` bytes and digest; the attempt occurrence
identity; manifest-bound `route-attestation-v1`; the selection checkpoint's
validated `focus-receipt-v1`; and optional manifest-bound aligned-example,
token-outcome, surprise, and revision receipts. Each source reference carries
path/evidence id, schema, digest, and verification status.

Decision rule, in order:

1. Validate one occurrence/attempt identity and every supplied digest/schema.
2. If exactly one qualifying increment attestation exists, join its exact want
   to exactly one eligible focus-receipt candidate at the attempt time. Map
   `:focus`, `:associated`, `:useful-elsewhere` to
   `:focus-increment`, `:associated-increment`, `:elsewhere-useful`.
3. Otherwise, only when the close explicitly records non-delivery
   (`:grounded? false`, `:artifact-only? false`) and a keyword
   `:failure-kind`, emit `:known-typed-failure` and preserve that type.
4. Otherwise emit `{:class :unknown :missing [...]}` naming the exact absent or
   ambiguous evidence. Unknown is a typed result, not a fifth valued ending.

The kernel refuses rather than returns unknown on identity/digest drift,
unsupported receipt/schema versions, malformed close shape, multiple qualifying
increments, multiple focus rows for the attested want, or contradictory
increment and typed-nondelivery evidence. It returns unknown for honest absence:
no attestation, no/expired focus relation, or nondelivery without a failure
type. It never resolves ambiguity by priority or target-name inference.

Improve-2 slice 4 consumes only admitted kernel receipts as the disposition
label `d` paired with improve-2c's aligned observation `o`; unknown rows remain
explicit missing data and are not normalized into a fitted `P(d|o)`. Improve-7
slice 2 uses the same four-class carrier and the receipt's time-valid focus
relation to place the global .55/.35/.05/.05 masses before constructing local
focus-conditioned C. It holds unavailable class mass; it does not map no-op,
unchanged prediction, or unknown to the 5% failure class.

## 4. First implementation slice

Size: one pure namespace (about 120–180 LOC), one declaration resource, one
focused test namespace (about 10 tests / 45–70 assertions), and a runner
retention/manifest hook (about 25–40 LOC). It is record-only: no scoring,
selection, preference, habit, learning-count, G, posterior, or source-store
semantics change. Frozen replay tests must assert the old decision value,
selection certificate fields, every G component, and posterior bytes are
identical before/after attachment.

Required bad-case fixtures:

| Intended class | Positive fixture | Bad case that must not acquire the class |
|---|---|---|
| `:focus-increment` | one matched increment warrant + exact time-valid `:focus` row | `:grounded-change`/commit/status only, no attestation |
| `:associated-increment` | one matched increment + exact `:associated` row | relation inferred only from target name or embedding |
| `:elsewhere-useful` | one matched increment + explicit `:useful-elsewhere` row | unmatched off-focus work or interruption label without attestation |
| `:known-typed-failure` | explicit `:failure-kind`, false grounded/artifact flags | refusal/build-failed/incomplete without a failure type |
| `:unknown` | absent attestation reports `[:attested-increment]` | unknown must never be rewritten as known failure or no-op |

Also test digest drift, unsupported versions, duplicate attestations, ambiguous
facet rows, contradiction, malformed close, exact output replay, manifest
admission, and preservation of attestation verification level. The scoped
registry warrant should cover only this namespace plus its direct production
closure; no full-suite run is warranted.

## Reproduction and gates

Run from this checkout:

```sh
clojure -M holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-8-evidence/reproduce.clj
```

It reads the source root only and prints the table to stdout with summary counts
to stderr. No research test was added, so the test-registry result is typed
**none**. Fresh results in the isolated CLI worktree:

- reproduction: 124 records; `{:unknown 122, :known-typed-failure 2}`; zero
  retained directories;
- `clj-kondo`: 0 errors / 0 warnings;
- `futon4/dev/check-parens.el`: OK;
- `git diff --check`: clean.
