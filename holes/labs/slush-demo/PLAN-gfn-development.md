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
- **Slice 1 — RAN; anti-gaming PASS, discrimination INCONCLUSIVE.** (This entry
  previously said BLOCKED on a missing `rollout_execute.py` — stale:
  `rollout_execute.py` exists on futon3a main since commit `8cf687e`, merged
  2026-07-10, and `aliveness_v3_gate.py` runs against it. Note the gate needs
  the `futon3a/resources/notions → ~/code/data/notions` symlink per
  futon3a `README-data.md`; restored on this machine 2026-07-10.) Verified
  results in the TN checkpoint + Fable review-verification block: anti-`1=1`
  PASS; accuracy/aliveness AUC sit exactly on the null-95. Real blockers are
  reward science, not missing files: (a) discharge accuracy too sparse
  (median-0 want-coverage), (b) ground truth too small/imbalanced (10 records,
  8/2).
- **Slice 2 — BLOCKED** on Slice 1's two reward-science prerequisites above.
  `gfn_core.py` is the trainer it will use; it can drop the torch dependency.
- **Slice 3 — BLOCKED** on Slice 2.
