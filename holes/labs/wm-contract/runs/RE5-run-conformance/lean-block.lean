/-! ### The 2026-09-04-re5 run's route against the drawn wiring (worklist `:RE5`)

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
* `futon2:holes/labs/wm-contract/runs/2026-09-04-re5`, the run at futon2 sha `e0552943` --
  4 records selected by `:run/id` (RUN11), extracted trace sha256
  `f343432d772986ddc2f7fe38107913afc997201ebae9b6240dff152e235b1120`.

GENERATED from those two files by
`futon2:holes/labs/wm-contract/u49_route_transcribe.bb`; edit the sources and
regenerate rather than editing the literals.
-/

/-- The 4 routes the run recorded, in the order `:run/id` selection
returns them out of the shared trace:
`8ae111bc-d758-45f3-9c5b-f98832e10bb6`,
`308d1622-55ca-4671-aab7-273731d3c99e`,
`67c72ac3-5d8b-4ffd-8732-650864d18182`,
`c149f9de-669c-4817-9b0e-ed4aad77db79`. -/
def re5Routes : List (List RouteNode) :=
  [[.R20, .R12, .R2, .R7, .R3, .R8, .R5, .R6, .R14, .TRACE],
   [.R20, .R12, .R2, .R7, .R3, .R8, .R5, .R6, .R14, .TRACE],
   [.R20, .R12, .R2, .R7, .R3, .R8, .R5, .R6, .R14, .TRACE],
   [.R20, .R12, .R2, .R7, .R3, .R8, .R5, .R6, .R14, .TRACE]]

/-- The run's 36 hops, 9 of them distinct. -/
def re5Hops : List WiringEdge := re5Routes.flatMap routeHops

/-- The drawn edges this run never traversed. -/
def re5UnfiredDrawnEdges : List WiringEdge :=
  figureDrawnEdges.filter (fun e => !edgeMem e re5Hops)

/-- CLOSED UNDER THE J9 CRITERION · leg (3) · THE RUN-CONFORMANCE CERTIFICATE
for the run `runs/2026-09-04-re5` (futon2 sha `e0552943`),
worklist `:RE5` under Joe's RUN4 ruling of 2026-09-03. Every one of the 36
hops the run recorded is an edge of the drawn wiring on run3's own
classification, no route is empty, and no code-retired pair was traversed at
route grain. Proved by `decide` over the transcribed tables, no `sorry` and no
`native_decide` -- the `wmTraceR2`/`wmTraceR8` precedent. The Clojure side of
the same comparison is `futon2:holes/labs/wm-contract/run3_conformance.bb`,
whose pinned verdict for this run is
`runs/2026-09-04-re5/conformance.edn` `:verdict :conformant`; the mutations that
break this proposition are listed at `runs/RE5-run-conformance/04-controls.edn`
control C4.

WHAT IT DOES NOT SHOW, because a reader will otherwise take it for more: 5 of
the 9 distinct hops are on the `:route-measured-drawn` layer, which is the
layer a previous route MEASUREMENT put on the figure, so for those the run is
being compared against a record of a run; and 19 of the 22 drawn edges never
fired at all. The certificate says this run stayed inside the union of the two
layers. It does not say the drawn figure predicted the run. -/
theorem wmRe5RunConformsToDrawnWiring : runConformsToDrawnWiring re5Routes := by
  decide

/-- The census the certificate is stated over, so the numbers a reader checks
against `runs/2026-09-04-re5/conformance.edn` are themselves decided rather than
asserted in prose: 4 routes, 36 hops, 9 distinct, and the class split
-- 2 drawn, 5 route-measured, 1 excluded at dependency grain, 1 ruling-unrealised,
0 refutations, 0 unmapped -- with 19 of 22 drawn edges unfired. -/
theorem wmRe5RouteCensus :
    re5Routes.length = 4 ∧
      re5Hops.length = 36 ∧
      re5Hops.dedup.length = 9 ∧
      (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.drawn))).length = 2 ∧
      (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.routeMeasured))).length = 5 ∧
      (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.excludedDependencyGrain))).length = 1 ∧
      (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.rulingUnrealised))).length = 1 ∧
      (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.refutation))).length = 0 ∧
      (re5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.unmapped))).length = 0 ∧
      figureDrawnEdges.length = 22 ∧
      re5UnfiredDrawnEdges.length = 19 := by
  decide
