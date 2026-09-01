# C355 — revision-bound operator wording

Date: 2026-09-01.

C352 copied a live population cardinality into operator prose. That number was
accurate at its observation and stale after the next check arrived. C319 and
C352 now make the executable reconciliation authoritative: every then-current
member must be classified, zero may be unexplained, and the saved report must
name its revision. The prose carries no fixed population or class counts.

C352 also removed the composites qualification too early. C351 demonstrated
that the repaired three-valued data predicates work across the audited modes,
but affected Lean-positive wrappers accept an empty Lean file as exit 0. The
operator wording again treats those controls as positive+negative composites
until a nonempty, declaration-specific baseline control passes. This clearing
condition is independent of C350's writer-fence capability work; both must pass
before the prepared final wording is used.

The current wording therefore distinguishes:

- reconciled population completeness, measured at gate time rather than copied;
- reason-preserving data mutation from still-insufficient Lean-positive shape;
- content acceptance from C345's temporarily unearned event-freedom field.
