# C68 — final unwitnessed audit

Date: 2026-08-31

`wmVerdictsLedgerAlone` and `wmVerdictsDeclared` are discharged from the independently pinned R9-D2 corpus run. They remain distinct tables: the ledger-only run has thirteen explicit `unknown` verdicts; the declared-part run has thirteen explicit `self` verdicts. Removing a row from either table is its declaration-specific negative control.

Two declarations classified as dischargeable in C55 are instead refused after reading their propositions:

- `valueEvidenceRequiresL2` is false as quantified. It permits an arbitrary `valueEvidence`; choosing `fun _ => True` and an L1 witness refutes the conclusion. It needs a named layer-admission hypothesis or a type that only admits L2 value evidence. `Layer.L1` and `Layer.L2` remain distinct and are not coerced.
- `wmRunConformsToWiring` is refuted by its own WM-RUN2 evidence: six measured hops are absent from Figure 4. The tag `route-measured-undrawn` makes the disagreement loud; it does not turn disagreement into conformance.

Thus no unwitnessed declaration remains: the two true fixture declarations are bound, while the two false/currently-refuted propositions are explicit refusals rather than fabricated witnesses.
