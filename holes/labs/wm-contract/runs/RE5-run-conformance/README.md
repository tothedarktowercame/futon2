# RE5 — the run-conformance certificate for `2026-09-04-re5`

Worklist item `:RE5` (`EPIC-run-era.md`), executing leg (3) of Joe's RUN4
ruling (`worklist.edn :run4-lean-ruling`, 2026-09-03) over the first run that
was born instrumented.

    U49_RUN_DIR=runs/2026-09-04-re5 U49_SLUG=re5 U49_EMIT_TABLES=0 \
    U49_CONTROLS_REF=runs/RE5-run-conformance U49_ROW=":RE5" \
      bb holes/labs/wm-contract/u49_route_transcribe.bb runs/RE5-run-conformance

Same producer as U49, five environment variables. Everything but
`certificate.edn` and this file is generated and deterministic — no wall-clock
field, so two runs over unchanged sources write byte-identical artifacts
(checked: `cmp` over all six).

## What was proved

`mathlib4 DarkTower/WarMachine/Holes.lean`, commit `581034b67e`:

* `wmRe5RunConformsToDrawnWiring : runConformsToDrawnWiring re5Routes`
* `wmRe5RouteCensus` — the eleven numbers, decided rather than asserted.

Both `by decide`, no `sorry`, no `native_decide`; `#print axioms` reports that
neither depends on any axioms (and re-reports the same for U49's two, which the
edit did not disturb). The module's sorry count is unchanged at 10.
`holes-contract.json` was re-emitted at the new authority in `c4ccafed32`.

## The block reuses U49's tables, and control C7 is why that is allowed

`RouteNode`, `WiringEdge`, `figureDrawnEdges`, `figureRouteMeasured`,
`figureRetired`, `classifyHop`, `routeHops` and `runConformsToDrawnWiring` are
a function of the drawn control map alone, so a second run's block must not
redefine them — Lean would refuse, and duplicating them would let two blocks
drift apart silently. Reusing them is a *claim*: that the map has not moved
since the U49 block was generated. **C7 is that claim made checkable** — it
compares this run's control-map sha256 against the one recorded in
`runs/U49-run-conformance/00-source.edn`, the artifact that says what the
reused tables were generated from. It passes: both are
`161d0abffd21551078ac2d7a87427e6cacafbfca0695c09a496260be21fafdec`, p4ng
`e508ece`, `:as-of` 2026-08-30.

C7 does *not* check that the U49 block is unedited. That is stated in the
certificate's `:limits` rather than implied.

## The census is identical to s5's, and that is not confirmation

4 routes, 36 hops, 9 distinct; 2 drawn, 5 route-measured, 1 excluded at
dependency grain (`R2→R7`), 1 ruling-unrealised (`R5→R6`), 0 refutations,
0 unmapped, 19 of 22 drawn edges unfired — every number equal to U49's.

Both runs walk the same nine-hop route, `R20 R12 R2 R7 R3 R8 R5 R6 R14 TRACE`.
Two runs agreeing about a route they were always going to take is one
measurement repeated, not two independent ones, and the certificate says so in
`:limits`. What *did* differ between the runs is the selection inside R6/R14 —
s5 ran the `stub:first-ranked-authorized-mission` seam and re-5 ran
`stub:controller-head` — and the route tags do not distinguish those, which is
the same grain point C5 makes about the drawn set.

## Controls

| control | verdict | what it establishes |
|---|---|---|
| C1 reproduces the pinned verdict | PASS | the producer's classifier returns all twelve fields of `runs/2026-09-04-re5/conformance.edn` against the current control map `e508ece` |
| C2 node names round-trip | PASS | every emitted constructor decodes back to the node it came from; the decoded routes equal the recorded routes |
| C3 fabricated node absent | PASS | `R404` is in no node list, no route, no edge table, and not in the emitted text |
| C4 mutations rejected | PASS | four mutations each break the conformance predicate — planted `R99 → R100` (`:unmapped`), a route emptied, `R2 → R3` traversed (`:refutation`), the route table emptied |
| C5 the two checkers disagree on the drawn set | REPORTED | 22 against 21; the difference is the `:unresolved` self-loop `R5 → R5`, not traversed here |
| C6 what the certificate does not show | REPORTED | 5 of 9 distinct hops are on the measured layer; 19 of 22 drawn edges never fired |
| C7 reused tables are still the current map | PASS | new with RE5 — the sha256 the reused definitions were generated from equals this run's |

## What did NOT happen

`wmRunConformsToWiring` is **not closed**. It is run-gated and closes when Joe
accepts a certificate over a run he judges qualifying; the registry row is
still `mkHole` and the contract still reads 124 declarations / 114 closed /
10 holes. There are now **two** certificates awaiting his acceptance, U49's
over s5 and this one.

Full account: `../../C503-first-correlated-run.md`.
