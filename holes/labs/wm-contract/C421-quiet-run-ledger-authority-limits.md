# C421 — quiet-run ledger authority limits

Date: 2026-09-01.

The state machine now emits and persists the C417 authority record for its
ledger. It classifies the canonical head as `self-owned`: `joe` writes the
evidence, selects the caller-provided ledger path as canonical, owns storage and
retention, can replace/rollback it, and runs verification.

Three claims are machine-readable `unprovable` limits:

- history completeness, because a valid prefix can be truncated and extended;
- originality, because a byte-identical copied ledger is indistinguishable;
- fence-ID uniqueness across ledgers, because no independent authority selects
  one ledger as canonical.

The classification is
`authority-limit-not-pending-local-repair`. C409 found no local authority the
writer cannot rewrite; C415 rejected journald because it is retention-bounded,
unsealed, and does not select the canonical head. Best-effort anchoring would
overstate the result.

The record names its clearing condition: an independent canonical-head
authority that selects and retains the head without evidence-writer
rewrite/rollback capability. No such tier is invented today.

This metadata accompanies both successful and refusing CLI verdicts and is
also stored in the initial ledger row. It does not weaken `certified`: the run
remains producer-bound. The limits concern history completeness and canonical
selection, not whether that run occurred.
