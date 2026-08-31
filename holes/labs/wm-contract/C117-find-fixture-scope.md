# C117 — F1–F3 fixture scope amendments

Date: 2026-08-31

F1–F3 all fall on the same side as F4.  They are behavioral invariants whose
facts are completely represented by `FindReceiptRow`; they are not occurrence
claims like `wmRunsOnce`.  Narrowing loses only the unsupported assertion that
the serialized rows correspond to every call of opaque, deliberately refused
`find`.

- F1 now states that a row's selected set is contained in its repository and
  that empty selection has the typed `noPatternAddressesThisTension` absence.
- F2 now states that every selected member belongs to the row's receipted set.
- F3 now states that every selected member belongs to the row's
  non-self-certifying set.

The original universal scopes remain legible in each dated source amendment.
All three remain contract-kind holes intentionally: definitions are complete,
while the pinned recorded instances remain evidence obligations.  The existing
checker has isolated controls: outside-repository selection for F1, removed
receipt for F2, and score-only receipt for F3.
