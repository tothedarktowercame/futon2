# C112 — remaining-hole classification audit

Date: 2026-08-31

The emitted contract has 16 holes.  Reading each emitted name back to its exact
source declaration gives:

## Genuine source holes — 14

`modelUncertaintyAndEIG`, `C`, `find`, `findF1Containment`,
`findF2Receipted`, `findF3NonSelfCertifying`, `organise`,
`r9TwoRunCensus`, `r9WmPerRowDeclarations`, `r8EraBoundary`,
`preferenceStackLiveRecorded`, `machineHasNoC`, `wmRunsOnce`, and
`wmRunConformsToWiring` each contain an actual `:= sorry` term.

Five are deliberate implementation/meta-claim refusals (`modelUncertaintyAndEIG`,
`C`, `find`, `organise`, `machineHasNoC`); the other nine remain outstanding
proof or empirical obligations.  “Genuine source hole” here describes the Lean
body, not whether attempting its implementation is currently licensed.

## Deliberate witnessed-instance obligations — 2

- `nonDegenerateAblationLaw` is a complete predicate, deliberately retained as
  a contract hole so the pinned exact-dyadic ablation instance remains tracked.
- `findF4Falsifiable` is a complete fixture-indexed predicate, deliberately
  retained so the pinned `FindReceiptRow` instance remains tracked.

Both have passing bindings.  Their former `CLOSED-BY-RECORD` doc tags obscured
the intentional emitter classification; C112 changes those tags to
`WITNESSED-INSTANCE OBLIGATION · contract kind HOLE intentionally` without
changing the contract.

## Stale classifications — 0

No proved declaration among the remaining sixteen is merely mislabelled.  The
gap between 16 emitted holes and 14 `:= sorry` terms is fully accounted for by
the two named witnessed-instance obligations.  Therefore the current hole count
does not overstate undone work through the C109 stale-classification mechanism.
