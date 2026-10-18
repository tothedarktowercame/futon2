# War Machine readiness campaign — 18 October 2026

## Campaign status

Active. Joe authorized five real clicks on 2026-10-18. They run sequentially,
one at a time, with an evidence review and any necessary offline repair between
clicks. This is a new campaign; no October 17 run is relabelled or resumed as a
new click.

## Fixed cast and method

Every launch explicitly uses:

- author: `zai-2`
- reviewer: `zai-6`
- repair reviewer: `zai-7`

For each click the debugger is attached. A repairable error is held and retried
at its addressable `run-phase!` boundary without another click or another G
selection. A defect that cannot safely be repaired in the held process stops
the click, is repaired offline, and is replayed or checked before the next
authorized click. No refusal, abstention, incomplete result, or build failure
is counted as progress merely because diagnostics or code were produced.

The campaign records the exact click/run/attempt/cascade identities, phase and
debugger timing, every repair job and commit, reload/retry evidence, report-card
and run-record paths, grounded effects, and the readiness disposition before
the next click. After click 5, an independent Claude peer review will assess
the full series.

## Preconditions incorporated from the October 17 campaign

- agent JSON representation is normalized by the shared handoff boundary;
- selected-target readings receive bounded semantic correction and explicit
  `pattern_refs` requirements;
- occurrence-square authoring uses one snake-case wire schema;
- selected cascades carry occurrence-keyed four-map squares;
- reviewed D4 decisions become attributed operational authoring on same-click
  retry;
- selected-target reading, square authoring, selected-reading review, cascade
  execution, and cascade resume are addressable `run-phase!` boundaries;
- a same-click repair/resume is tested to retain one G selection.

## Runs

### Click 1 — pending

Authorization: campaign click 1 of 5. Exact launch and result evidence will be
appended from the durable click-loop ledger.
