# C85 — fixture-indexed find F4

Date: 2026-08-31

`findF4Falsifiable` is narrowed from a universal proposition over the deliberately refused `find` implementation to a proposition on one pinned `FindReceiptRow`. The original scope was false for empty repositories and could not be connected to serialized evidence without assuming that `find` produced the recorded selection.

The repaired proposition requires the row's repository to be nonempty and a declared zero-mass member to occur in that repository while being absent from the row's recorded selection. The six Snatch scenarios satisfy this predicate. `--negative-f4` removes the first scenario's omitted member from the recorded repository and is rejected.

`nonDegenerateAblationLaw` is unchanged and unbound; float-to-`ℝ` semantics remain a separate modelling decision.
