# Independent review acceptance — categorical ambiguity estimator

Reviewer: claude-15, 2026-09-14. Scope: codex-22 commits 602ff836
(estimator + tests), a796f6df (receipts). Verdict: ACCEPTED, no fixes.

Checked:

- Diffs read in full; exactly two source files plus receipts — the
  packet's zero-mass declarations hold (no efe.clj, no registry, no
  wiring, no A content).
- Formula and convention: sum_s Q(s)*H[A(o|s)] with 0*ln0=0 via a
  positive-mass guard; documented in the ns docstring.
- Validation posture mirrors machine_q_risk.clj as required: ordered
  distinct supports as contracts (reorder refuses — tested both for Q
  state-support and per-row outcome support), full model identity
  equality across Q and A, every mass map routed through
  machine-model/row-sum-admission with no local tolerance and no
  renormalization; invented-outcome mass refuses.
- Authority: :declared-prior refuses by name; anything other than
  :observed-estimate refuses :authority-unlicensed. Verified
  :observed-estimate is the ESTABLISHED machine-model vocabulary
  (machine_model.clj:91), not new coinage — the estimator adopts the
  existing licensed keyword and stays fail-closed against unknowns.
- Reference independence: all three value tests use closed-form ln
  expressions written inline (deterministic rows exactly zero; 0.25*ln2;
  a mixed case with the hand-derived expression in a comment) — none
  call the production entropy path.
- Receipts validated (not re-run): 3 tests/14 assertions exit 0 at tree
  602ff836, kondo 0/0, parens OK, stdout/stderr sha-pinned, empty
  failed-attempts trail. Source-pin sha256s recomputed and match
  (6ffef96a..., af1ff5dc...).

Notes recorded for downstream packets (not defects):

1. machine_model.clj:93 additionally demands a measurement pointer when
   a KERNEL claims :observed-estimate. The supplied-A shape here has no
   such field; when the packet-1 licensed A artifact fixes its shape,
   the estimator's field contract may need to adopt the measurement
   pointer. Fail-closed refusals make the interim safe.
2. The value tests assert exact double equality between closed forms
   and the reduce; green and receipt-pinned on the canonical machine,
   but platform-sensitive in principle. If they ever break on a
   toolchain move, loosen to an epsilon with the closed form retained.
