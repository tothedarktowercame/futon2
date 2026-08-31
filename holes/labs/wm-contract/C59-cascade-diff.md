# C59 — `CascadeDiff` fixture and falsifier

The fixture is hand-derived from the four organise laws in the pinned R5
record, not emitted by an organise/diff implementation.  Two selected endpoints
(`probe`, `remedy`) are joined in the authored graph through one unselected
intermediate.  The derived organised graph therefore contains the single
fast-forward edge `probe → remedy`; its node set is exactly selected union
recorded additions.  Two precedence variants retain that collection while
changing acting order and the record's measured score from `+3` to `-5`.

The executable checker independently recomputes O1's node union, O2's authored
reachability, O3's exact selected-endpoint fast-forward set, and O4's
precedence-sensitive acting-order-or-score condition.  Its negative control
adds the unsupported reverse edge `remedy → probe`; both O2 and O3 reject it.

All four organise holes are now dischargeable from one fixture/check pair, but
none is discharged or bound in C59.  The next pass must transcribe the fixture
to Lean and bind each declaration with its own falsifier acceptance.

The two C27 tail cases remain genuinely different:

- `r9VerdictConsultsChecker` is checkable by Lean elaboration and axiom
  inspection, but not by an EDN shape predicate; it needs a separately named
  proof artefact rather than a table-shaped check.
- `preferenceStackLiveRecorded` is executable-checkable, but its registry
  fixture is checker source code, not a serialized `PreferenceStackWitness`.
  It cannot become structurally shape-checkable until such evidence is emitted.
