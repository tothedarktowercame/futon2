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

### Click 1 — selection abstention; live repair transport failed

- Authorization: campaign click 1 of 5.
- Start: `2026-10-18T03:55:43Z`; issued at `03:56:44Z`; terminal at
  `04:01:26Z` (about 282 seconds after issue).
- Click: `wm-click-e6aae912-6192-4e62-90f8-df6abf67c01a`.
- Run: `2026-10-18-d0537eb0-8e61-4766-8a50-ee79343e7e20`;
  attempt `attempt-001`.
- Preflight: five clicks available; fixed cast idle and invoke-ready; 13/13
  tripwires clear. The 48 open obligations were recorded under the standing
  selection-precedence ruling rather than used as a launch veto.
- Trace: `stop-line-memory` at 20 seconds; `selection` from 40 seconds; the
  debugger held selection at `03:59:55Z` on
  `:wm/selection-terminal-abstention`.
- Substantive condition: outer task selection found no unique task-state
  minimum. Its undefeated set was `M-formal-war-machine` and
  `M-digital-nomad-patterns`; therefore there was no addressable action.
- Outcome: `abstained`, no tick carried. This is not counted as readiness
  progress.
- Run record:
  `data/wm-runs/tick-run-record-2026-10-18-d0537eb0-8e61-4766-8a50-ee79343e7e20.edn`.
- Full held-phase evidence:
  `data/wm-click-loop/debugger/held-2026-10-18-d0537eb0-8e61-4766-8a50-ee79343e7e20.md`.
- Terminal debugger page:
  `data/wm-click-loop/debugger/wm-click-e6aae912-6192-4e62-90f8-df6abf67c01a.md`.
- Click log: `data/wm-click-loop/click-001-1792295744.log`.

The live repair dispatch `invoke-1792295995172-16440-eb338e9e` and terminal
repair dispatch `invoke-1792296086410-16441-3f5192ef` both failed before the
repair seat executed. Agency returned HTTP 400/code 1261, `Prompt exceeds max
length`: the loop had embedded a 6.86 MB held-phase record (and later the large
last-result) directly in the prompt. This is an apparatus defect independent of
the selection abstention.

Between-click repair: `wm_click_repair_loop.py` now sends bounded evidence
excerpts plus the durable local artifact paths, omits unbounded last-result
fields, and enforces a 60,000-character dispatch ceiling while retaining the
mandatory repair guide. The regression test plants 200 KB page, log,
last-result, and prior-attempt payloads and checks that the repair packet stays
within the ceiling and retains its evidence addresses. Gate: 3 Python tests
pass; both changed Python files compile. Click 2 must not launch until this
repair is committed and the two-undefeated-task selection condition has been
adjudicated or repaired offline.
