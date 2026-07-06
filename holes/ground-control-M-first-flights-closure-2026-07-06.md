# Ground Control Demo — M-first-flights Closure Evidence (2026-07-06)

## Operator Prompt

Joe asked not to close `M-first-flights` by fiat. The goal is to decide,
with evidence, whether the War Machine should stop recommending
`M-first-flights` as ordinary `advance-mission` work.

## Current Hypothesis

`M-first-flights` is likely closeable as a mission if:

1. Phase A is closed by its own exits and operator verdict.
2. Phase B is no longer native open work inside `M-first-flights`, but is
   transferred to named live-loop / gamma / realized-outcome machinery.
3. The open sorry `:sorry/first-flights-phase-b-policy-grade-G` is addressed
   or foreclosed with an explicit transfer rationale.
4. The mission source-of-record status changes, rather than adding a
   name-based recommender special case.

No closure edits have been made in this demonstration yet.

## Commissioned Evidence Tasks

| Task | Owner | Bell job | Question | Status |
|---|---|---|---|---|
| A. Acceptance/evidence census | `zai-3` | `invoke-1783340554873-667-9addef69` | Do the mission's own Phase A/Phase B criteria justify closure or hold? | Done — HOLD |
| B. Phase-B transfer proof | `zai-5` | `invoke-1783340554876-668-c4311be0` | Is Phase B transferable to named successor machinery, and how should the sorry registry be marked? | Done — TRANSFERABLE |
| C. Mechanical stop-recommending path | `zai-6` | `invoke-1783340555059-669-93538532` | Which source-of-record edits/gates stop recommendations without special-casing the mission name? | Running |

## Stop Conditions

- If any task finds that Phase B still belongs as open work inside
  `M-first-flights`, do not close the mission.
- If the only way to stop recommendations is a name-based recommender
  exclusion, do not do it.
- If closure requires a live tick, serving-JVM restart, or `:7071` write, stop
  and ask the operator.

## Expected Next Step

After the three bellbacks, ground control should compile a closure verdict:

- `CLOSE/TRANSFER`: edit `M-first-flights.md` and `sorrys.edn`, run gates, and
  commit; or
- `HOLD`: record the missing evidence and leave the mission recommendable.

## Evidence Received

### Task A — Acceptance/Evidence Census (`zai-3`)

Verdict: **HOLD**.

Findings:

- Phase A is complete and closeable: schema, logic model, render lane,
  substrate-2 round-trip, loop-closing stratification, and Joe's side-by-side
  checkpoint all have evidence in `M-first-flights.md`.
- The dF last inch is no longer a blocker: current first-flights lane F is
  +0.385, and the stale -0.024 cards-II snapshot is superseded.
- Phase B is only partially transferred. `W-constructor-df-last-inch` and
  `W-gamma-first-meal` cover the campaign gaps, but the mission doc still
  carries Phase B as a standing obligation.
- The typed-grounds migration / sorry-arrow `arr-7535a5b6-e59` remains open.

Open operator question from the census: accept Phase B transfer to the
gamma/fold-ansatz workstreams, or keep `M-first-flights` open as the tracking
vehicle until typed-grounds migration lands?

### Task B — Phase-B Transfer Proof (`zai-5`)

Verdict: **TRANSFERABLE**.

Evidence note: `futon2/holes/phase-b-transfer-proof-2026-07-06.md`, commit
`652a676`.

Findings:

- Named successor machinery: `futon2.aif.fold-realized`, the fold-realized
  -> γ calibration seam documented in live-loop III.1.
- The successor produces the Phase-B shape at policy grain:
  `{:policy :expected-G :realized-G :tick}` with both G legs on the same
  coverage-to-ΔG scale.
- Zero-coverage semantics makes policy-holes/ghosts measurable: 0 enacted
  boxes against N obligations is realizedG 0.0, while genuinely missing
  wiring remains nil.
- `fold_realized_zero_coverage_test.clj` passes and produces complete samples
  for the three qualifying deposits.

Recommendation from the proof: mark
`:sorry/first-flights-phase-b-policy-grade-G` as `:addressed`, not
`:foreclosed` and not `:open`, because the stated "judgments without
derivations" rationale is no longer true at the fold-realized grain.
