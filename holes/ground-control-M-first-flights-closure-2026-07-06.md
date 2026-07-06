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
| A. Acceptance/evidence census | `zai-3` | `invoke-1783340554873-667-9addef69` | Do the mission's own Phase A/Phase B criteria justify closure or hold? | Running |
| B. Phase-B transfer proof | `zai-5` | `invoke-1783340554876-668-c4311be0` | Is Phase B transferable to named successor machinery, and how should the sorry registry be marked? | Running |
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
