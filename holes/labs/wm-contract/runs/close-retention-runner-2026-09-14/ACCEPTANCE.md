# Independent review acceptance — close-retention runner wiring (slice 2 of 2)

Reviewer: claude-15, 2026-09-14. Scope: codex-24 commits 2b1f7af3
(runner + controls), 9c877fbd (test lint), 5184fbc9 (receipts).
Verdict: ACCEPTED.

Checked:

- File scope: full_loop_runner.clj + its test namespace + receipts;
  full_loop_cohort.clj and close_retention.clj untouched.
- All four anchors verified in the diff: (1) mint sits after the
  selection entry is fixed (past the missing-target/entry throws) and
  before construction's run-phase!, with real run/cohort/attempt ids
  and real clock/UUID; once-only is enforced by compare-and-set! with
  a typed :action-occurrence-already-minted refusal, not by
  convention; (2) :retention-inputs attaches to the closed term only
  when (and cohort? occurrence), with typed-absent state and model and
  empty admitted evidence; (3) closes without a minted occurrence
  (pre-selection failure) stay legacy — no marker, no block; (4)
  close-attempt!'s return is captured and the writer-completed block
  is exposed in close!'s result under :close-retention.
- run-id safety chased in source: run-opportunity! (:3996-4011)
  defaults :run-id to a fresh UUID before calling the core, so the
  mint input is nonblank on the public path. A direct
  run-opportunity-core! call without :run-id would refuse at mint —
  fail-closed, noted, acceptable.
- Post-selection FAILURE closes (e.g. build-failed) correctly carry
  retention too: the occurrence exists, so conditioning is retained
  for failed outcomes — which the 12-outcome axis needs.
- Controls: full end-to-end run-opportunity! against a temp
  one-attempt cohort — occurrence action value EQUALS the selection
  checkpoint's selected action; the result-map block equals the
  durable event's block (map equality, digest re-validated by the
  writer on the way through); writer-timestamp equality; once-only
  refusal exercised on the private fn; and an early-failure run
  (empty roster -> :agent-unavailable) asserting legacy shape.
- Receipts: fresh-JVM FULL runner namespace, 154 tests / 792
  assertions exit 0 at 9c877fbd; runner kondo baseline-delta 0/0;
  full-driver parens; initial test-lint attempt retained.

Cosmetic (no change made): non-cohort runs mint an occurrence with
cohort-id "non-cohort" that is then discarded (block never attaches).
Harmless; tidy only if the runner file is touched again.

Close-retention is now COMPLETE across contract, writer, and runner.
Every new cohort close after a successful selection carries a
validated conditioning block (occurrence + typed-absent state/model +
closed-at = cutoff). Production closes pick this up when the loop
JVMs next start from current main. The a-pairs conditioning gap is
closed for FUTURE closes; the Status arm still requires a-labels
acquisition, for which the retained cutoff now makes retrospective
annotation on new closes possible per the validator's retrospective
provenance rules.
