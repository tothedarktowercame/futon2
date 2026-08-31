# C60 — `find-snatch` acceptance-strength decision

**Decision: nonzero drift mismatches must reject the witness.** Do not renew
`findF1Containment`, `findF2Receipted`, `findF3NonSelfCertifying`, or
`findF4Falsifiable` against the current exit-0 result.

**Pins:** contract authority
`26a66d88ca2ad67e779406be9bb0faddb283837f`; C56 R2 corpus
`b2c3aeb408cc4de59947ad93f9c1ea17b735fc0da26e188ada7c24609bffbca1`;
`find-snatch` binding/run `594127059d6a31c5b21f5d34ebfac5b14b1ad993`;
current inspected futon3 `83f92b37905457b3860f3395e56cb97a202a44cd`.

## What the 21 are

The report contains **21 exact-text mismatches over 14 patterns: 14 IF clauses and
7 HOWEVER clauses**. In every row, `:file-text` is the authored clause parsed from
`library/snatch/*.flexiarg`, while `:runner-text` is a shorter hand-written sentence
stored beside the executable predicate in `checks/playout_snatch.clj`.

This is not C39-style snapshot/live movement. The mismatch population was already
21 in the first structured-find commit, `f1998a5` (2026-08-30), remained 21 at the
binding commit `5941270`, remained 21 after `8762ba7`, and is 21 now. The latest
authored-library clause work (`2734ac5`, 2026-08-27) also supplied the runner's
HOWEVER fields. The two representations were born unequal; no later population
move made a once-valid snapshot stale.

The shorter runner strings are generally paraphrases, not contradictions. That
does not make them an acceptable snapshot: the runner evaluates separately
hand-written predicates while the receipts warrant selection with the authored
clauses. Exact drift therefore shows that the evaluated antecedent and the cited
antecedent are not the same carried object. A reader cannot determine equivalence
from the report, and the executable cannot establish it.

## Acceptance consequence

`find-snatch` currently throws for F1 (selection outside the repository) and F4
(selection of a declared zero-mass pattern), but merely prints its antecedent drift
count. That is too weak for the four bindings, especially F2: a receipt citing the
authored clause does not establish that the authored clause was what fired.

The next implementation pass should make nonzero drift a failing acceptance (with
a negative control), or eliminate the duplicate antecedent representation so the
runner consumes the authored clause through one typed source. Choosing the latter
may require modelling work; this decision does not prescribe it. Strengthening and
rebinding remain separate deliveries.

## Binding state

All four `findF*` bindings remain stale. C56's exit-0 rerun is recorded as an
executed result, not accepted as renewal evidence. No checker, witness fragment, or
generated registry was changed in C60.
