# Schema/chain repair review and lead revision correction

Worker 77adef59/7566bb79: all six pins match current reviewed bytes and historical source commit (lead-pins.json). Retained raw gates: 13 tests/36 assertions; clean kondo/parens; deliberate failure exit 1. Original invented HEAD and unreadable application controls are now represented by rejecting tests. Source joins HEAD to its current transaction, child prior to parent state, proposal to transaction, and captures the complete chain through genesis. No unchanged passing suite rerun.

Additional executed control (lead-revision-control.clj, exit 0, raw stdout/stderr retained) commits r0 -> r1 -> r0 successfully, then the next commit refuses feedback-conflict because r0 was already consumed. This violates unique revision identity and leaves an accepted head unusable for continuation.

Lead correction: compare-and-commit! rejects a destination revision already present among consumed prior revisions; recovery seeds its revision uniqueness set with the HEAD destination revision, so a self-consistently hashed repeated revision also refuses. Existing prior != next validation covers immediate reuse. Added tests check prepublication refusal, unchanged HEAD, and forged internally consistent repeated-revision recovery refusal.

Changed-source validation: 14 tests/39 assertions, zero failures/errors; kondo and explicit-path parens clean (lead-gates.json and raw files). Independent review of this small lead correction remains required. No production construction, external completeness, rollback freshness, runtime or restart claim. Caller-owned store maps and test callbacks remain a cooperative isolated API. Semantic E6b verification-to-store composition remains separate from this storage schema review.
