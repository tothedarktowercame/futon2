/-! ### The 2026-09-05-u59-a run's route against the drawn wiring (worklist `:RE5`)

The SECOND run certified against the drawn wiring, under Joe's RUN4 ruling.
The transcription's shared definitions -- `RouteNode`, `WiringEdge`,
`figureDrawnEdges`, `figureRouteMeasured`, `figureRetired`, `classifyHop`,
`routeHops`, `runConformsToDrawnWiring` -- are the ones the `:U49` block
above defines, and are NOT redefined here. They are a function of the drawn
map alone, so reusing them is a claim that the map has not moved since that
block was generated; the producer checks it (control C7) rather than
assuming it.

SOURCES, both pinned:
* `p4ng:empirics-futon/control-map-edges.edn`, `:as-of` 2026-08-30, commit `e508ece`,
  sha256 `161d0abffd21551078ac2d7a87427e6cacafbfca0695c09a496260be21fafdec`
  -- 22 `:edges`, 8 `:route-measured-drawn`, 8 retired pairs.
* `futon2:holes/labs/wm-contract/runs/2026-09-05-u59-a`, the run at futon2 sha `a491b2d9bc6ca636b50f9a2bc053a08b294564d5` --
  1 records selected by `:run/id` (RUN11), extracted trace sha256
  `d257bd6fee9f09259d0af4dbd2573f4d55fff10b83dd267a10460e3235e47cf1`.

GENERATED from those two files by
`futon2:holes/labs/wm-contract/u49_route_transcribe.bb`; edit the sources and
regenerate rather than editing the literals.
-/

/-- The 1 routes the run recorded, in the order `:run/id` selection
returns them out of the shared trace:
`feec6327-e0b0-41fc-9697-2fc46bff2830`. -/
def step20260905u59aRoutes : List (List RouteNode) :=
  [[.R20, .R12, .R2, .R7, .R3, .R8, .R5, .R6, .R14, .TRACE]]

/-- The run's 9 hops, 9 of them distinct. -/
def step20260905u59aHops : List WiringEdge := step20260905u59aRoutes.flatMap routeHops

/-- The drawn edges this run never traversed. -/
def step20260905u59aUnfiredDrawnEdges : List WiringEdge :=
  figureDrawnEdges.filter (fun e => !edgeMem e step20260905u59aHops)

/-- CLOSED UNDER THE J9 CRITERION · leg (3) · THE RUN-CONFORMANCE CERTIFICATE
for the run `runs/2026-09-05-u59-a` (futon2 sha `a491b2d9bc6ca636b50f9a2bc053a08b294564d5`),
worklist `:U49` under Joe's RUN4 ruling of 2026-09-03. Every one of the 9
hops the run recorded is an edge of the drawn wiring on run3's own
classification, no route is empty, and no code-retired pair was traversed at
route grain. Proved by `decide` over the transcribed tables, no `sorry` and no
`native_decide` -- the `wmTraceR2`/`wmTraceR8` precedent. The Clojure side of
the same comparison is `futon2:holes/labs/wm-contract/run3_conformance.bb`,
whose pinned verdict for this run is
`runs/2026-09-05-u59-a/conformance.edn` `:verdict :conformant`; the mutations that
break this proposition are listed at `runs/U49-run-conformance/04-controls.edn`
control C4.

WHAT IT DOES NOT SHOW, because a reader will otherwise take it for more: 5 of
the 9 distinct hops are on the `:route-measured-drawn` layer, which is the
layer a previous route MEASUREMENT put on the figure, so for those the run is
being compared against a record of a run; and 19 of the 22 drawn edges never
fired at all. The certificate says this run stayed inside the union of the two
layers. It does not say the drawn figure predicted the run. -/
theorem wmStep20260905u59aRunConformsToDrawnWiring : runConformsToDrawnWiring step20260905u59aRoutes := by
  decide

/-- The census the certificate is stated over, so the numbers a reader checks
against `runs/2026-09-05-u59-a/conformance.edn` are themselves decided rather than
asserted in prose: 1 routes, 9 hops, 9 distinct, and the class split
-- 2 drawn, 5 route-measured, 1 excluded at dependency grain, 1 ruling-unrealised,
0 refutations, 0 unmapped -- with 19 of 22 drawn edges unfired. -/
theorem wmStep20260905u59aRouteCensus :
    step20260905u59aRoutes.length = 1 ∧
      step20260905u59aHops.length = 9 ∧
      step20260905u59aHops.dedup.length = 9 ∧
      (step20260905u59aHops.dedup.filter (fun h => decide (classifyHop h = HopClass.drawn))).length = 2 ∧
      (step20260905u59aHops.dedup.filter (fun h => decide (classifyHop h = HopClass.routeMeasured))).length = 5 ∧
      (step20260905u59aHops.dedup.filter (fun h => decide (classifyHop h = HopClass.excludedDependencyGrain))).length = 1 ∧
      (step20260905u59aHops.dedup.filter (fun h => decide (classifyHop h = HopClass.rulingUnrealised))).length = 1 ∧
      (step20260905u59aHops.dedup.filter (fun h => decide (classifyHop h = HopClass.refutation))).length = 0 ∧
      (step20260905u59aHops.dedup.filter (fun h => decide (classifyHop h = HopClass.unmapped))).length = 0 ∧
      figureDrawnEdges.length = 22 ∧
      step20260905u59aUnfiredDrawnEdges.length = 19 := by
  decide
