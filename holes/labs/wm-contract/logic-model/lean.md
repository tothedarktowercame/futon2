# Lean logic-model units

## L1 — self-heal route, level 1

Module: `DarkTower.WarMachine.SelfHealRoute`.

This unit transcribes the level-1 injury-versus-ordinary routing table from
`phases.md` under the 2026-10-19 ruling: an active injury prepares diagnosis,
runs admission before readiness, and remains on the dedicated self-heal route;
ordinary selection is available only when there is no active injury. Its
finite-table certificate is decided over the transcribed finite edge set and
its finite state vocabulary. It corresponds to the run-time
`:meta-self-heal/readiness` receipt and the recorded click disposition. It
keeps click 1 as an observed-not-recorded conforming route and certifies the
durable reverted-F2c click-2 fallback as a violation rather than as behaviour.

Still pending: an admission witness carrying its bounded run result, and an
enact witness.  Their states are present in the table but have no record
witness in L1. Finding F7 (click bindings did not retain a refused click's
disposition) is landed in futon3c `baa9d55fa`: from click 3 on, a refused click's
binding carries `:runtime-disposition` and can be a record witness. Click 1's
binding predates it and carries only binding/run-record absence, so click 1
remains an observed-not-recorded witness.

Revision 2 (mathlib4 `darktower` `8a88a93105`, claude-5, 2026-10-19 19:07Z), after claude-2's
L1 reading (job `invoke-1792436329058-17707-36486211`): the first table sent diagnosis and
acceptance refusals straight to `terminalRecord`; the runtime
(`full_loop_runtime.clj:475-505`) runs readiness and then `route!` after every acceptance
observation, and `route!` is the function that refuses with `[:acceptance-test]` (click 4).
Readiness terms and route terms are now separate inductives and are not part of the state,
so `RouteState` is finite; a record's terms are reported beside its path by the producer.
`conformsPath` (starts at an injury state, ends at `terminalRecord`) and `conformsSuffix`
replace bare `conforms` for verdicts; the truncated path `[activeInjury, diagnosisPrepared]`
is pinned as a bad case. `noFallback` is scoped to clicks without `:meta-repair-retry`; the
attributable retry (`full_loop_runtime.clj:492-499`) runs ordinary work under an active
injury and is an open hole, not decided. Click 4 (`wm-click-fca1c921`, binding sha256
`0e3f984b…99e6`) is a record witness of the suffix `[readinessReady, routeRefused,
terminalRecord]`; its prefix is the serving result transcribed in PACKETS.

Revision 5 (mathlib4 `darktower` `d6f4b88ca3`, claude-2, 2026-10-20, packet M4L): adds the coordination states `coordinationHeld`, `coordinationRefused` and `persistRefused` after `routeEnacted`, so `RouteState` has 23 states (rev 4's "22" was a miscount of 20). Beside the path: `CoordinationOutcome`, placed by `CoordinationOutcome.state`, with the carrier `CoordinationObservation`. Theorems: `coordinationStates_terminate`, `coordinationStops_beforeEnactment`, `routeEnacted_reachesOutcomes`, `noFallback_rev5`, `outcomes_reachable`, `coordination_66_masked`. The table is written against M4a's record shape; the witness leg is typed none.

**AIF leg: typed none.** No module under `DarkTower/AIF` or `DarkTower/WarMachine` models
injury, repair or route arbitration (claude-2, 2026-10-19). The earlier sentence "L1 has the
first two" was wrong; L1 has the table and record witnesses.

**L2 (producer):** proposal by claude-2, `L2-route-certificate-producer.md` (futon2
`6e66219cf`): a sidecar `<binding>.route.edn` written from the `result-summary` hook in
futon3c `runner_service.clj`, verdict by evaluating the Lean module (`lake env lean`), gaps
typed, `:not-checked` when Lean returns no verdict. Not built.

## Ownership and the three legs (Joe, 2026-10-19)

claude-5 owns this encoding. Joe's framing: the AIF formalism (DarkTower/WarMachine) is done;
the run-time certificates should be done but may not be; the logic model is the third leg.
Sangha (the logic model, a form of social organisation in Dewey's sense), tathāgata (the AIF
formalism), dharma (the teaching from practice: the run-time certificates). A unit of this
encoding is complete only with all three: the AIF module it rests on (cited, not restated),
the transcribed table, and a producer that certifies real records against the table at run
time, writing into the record's requirements sidecar. L1 has the first two and record
witnesses; its certificate producer is unit L2.
