# C311 — complete Lean rejection-reason migration (2026-09-01)

The remaining glossary controls were partitioned by mechanism rather than
forced through one tool:

- 32 controls depend on an intended Lean elaboration/proof failure. Every one
  now uses a checked-in `#guard_msgs` fixture, and every wrapper requires that
  guarded file to exit zero.
- 14 controls mutate EDN/reference data or evaluate a named semantic predicate.
  Their false predicate is already the executable reason; compiler diagnostics
  are not involved and `#guard_msgs` does not apply.

This delivery migrated the remaining 25 Lean controls after C302's first seven.
It found three more controls that had been rejecting for the wrong reason:

1. `PolicyPriorKernelNegative` stopped at a missing witness `.olean`.
2. `ControlVocabularyNegative` stopped at a missing witness `.olean`.
3. `ActGateNegative` failed because `native_decide` could not compile the
   noncomputable gate, not because a missing leg was refused.

The first two now import `Holes` and define only their tiny local carrier.  The
act-gate control uses simplification and guards the resulting false goal.
`CohortNegative` was also narrowed from two simultaneous invalidities to the
single intended zero-window rejection.

Two dynamic temporary-source controls were replaced by checked-in fixtures:
the mis-wired generative-model state carrier and the collapsed model-uncertainty
/ EIG equality.  No glossary wrapper now accepts arbitrary nonzero Lean exit as
successful evidence.

The authoritative metadata lives in `checks/witness-fragments/*.edn`; the
generated registry is rebuilt with `scripts/merge_witnesses.bb`.  C302 had
initially edited the generated registry only, which a future merge would have
erased; this delivery repairs that source/output split for both batches.

The runbook now retires routine manual stderr inspection.  The full mutation
inventory still runs at publication and relevant source movement; manual
diagnostic review is retained for Lean toolchain upgrades, when exact wording
can legitimately change.

No binding, contract declaration, Q-facing definition, or model area changed.
Glossary coverage remains 31/33.
