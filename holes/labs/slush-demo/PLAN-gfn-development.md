# PLAN — moved

This review + plan now lives at **`futon2/holes/TN-gflownets-fable-review.md`**
(findings F1–F4 on why the slice-2 nulls were guaranteed; Slices 0–3 with gates
G0a/G0b/G1-comp; dispatch mapping). The v3 reward spec remains
`HANDOFF-slice2-v3-aliveness-selfevidencing.md` in this directory.

## Status

- **Slice 0 — DONE (2026-07-10).** Trusted trainer harness
  `slice2/gfn_core.py` (pure-numpy tabular GFlowNet, TB + uniform-P_B set
  handling, conditional logZ(m); deterministic full-batch training over the
  enumerable DAG). Gates **G0a PASS** (max TV 0.0125 to exact Gibbs, max
  |logZ−true| 0.029 nat) and **G0b PASS** (shared-scalar logZ max TV 0.986 —
  F2 reproduced and fixed). Tests: `slice2/test_gfn_core.py` (9, green).
  Findings: `findings/slice0_trainer_findings.md`.
- **Slice 1 — BLOCKED.** The v3 reward's accuracy limb needs
  `rollout_execute.py` (want-coverage discharge), which does not exist on disk
  (the spec says "reuse futon3a's rollout_execute.py" — it was never written),
  and `futon6/data/mission-pattern-scopes.edn` (the mission corpus) is also
  missing. Reconstruct/locate both before running the reward-before-generator
  gate.
- **Slice 2 — BLOCKED** on Slice 1 (and thereby on the same missing infra).
  `gfn_core.py` is the trainer it will use; it can drop the torch dependency.
- **Slice 3 — BLOCKED** on Slice 2.
