/-! ### The 2026-09-01-s5 run's route against the drawn wiring (worklist `:U49`)

Joe's RUN4 ruling (2026-09-03) refuses the permanent-attestation reading of
`wmRunConformsToWiring`: what is wanted is to run the machine and validate in
Lean that the run conforms to the wiring that was drawn. This block is the
transcription that makes the validation decidable -- the drawn map as data and
one pinned run's reassembled route -- and
`wmS5RunConformsToDrawnWiring` below is the certificate over it.

SOURCES, both pinned:
* `p4ng:empirics-futon/control-map-edges.edn`, `:as-of` 2026-08-30, commit `e508ece`,
  sha256 `161d0abffd21551078ac2d7a87427e6cacafbfca0695c09a496260be21fafdec`
  -- 22 `:edges`, 8 `:route-measured-drawn`, 8 retired pairs.
* `futon2:holes/labs/wm-contract/runs/2026-09-01-s5`, the run at futon2 sha `5a66411` --
  4 records selected by `:run/id` (RUN11), extracted trace sha256
  `c3480955286e548be6fd4bbde80e5081446cfaaac1500294e2de1fa44e2d7c67`.

GENERATED from those two files by
`futon2:holes/labs/wm-contract/u49_route_transcribe.bb`; edit the sources and
regenerate rather than editing the literals.
-/

/-- The nodes of the drawn control map (`control-map-edges.edn :nodes`), plus
`TRACE`, the sink the route's last hop reaches. Constructor names are the
recorded ids verbatim. -/
inductive RouteNode where
  | R1
  | R2
  | R3
  | R3a
  | R4
  | R5
  | R6
  | R7
  | R8
  | R9
  | R10
  | R11
  | R12
  | R13
  | R14
  | R15
  | R16
  | R17
  | R20
  | TRACE
  deriving DecidableEq, Repr

/-- A hop, and equally an edge of the figure: an ordered pair of nodes. The
route a tick records is a SEQUENCE of node tags, so a hop is a consecutive
pair (`run3_conformance.bb:113-114`). -/
abbrev WiringEdge := RouteNode × RouteNode

/-- The grounds on which a drawn edge was retired by a `:decisions` entry of
the control map. `code` retirements are claims about the code; `ruling`
retirements are Joe's. A pair may carry both, from different decisions. -/
inductive RetirementGrounds where
  | code
  | ruling
  deriving DecidableEq, Repr

/-- A retired pair with the SET of grounds it was retired on -- the shape
`run3_conformance.bb:57-61` reduces `:decisions` to. -/
structure RetiredWiringEdge where
  edge : WiringEdge
  grounds : List RetirementGrounds
  deriving DecidableEq, Repr

/-- Every pair in the map's `:edges`, in file order. NOTE, and it is a real
difference between the two checkers rather than an oversight: `:status` is NOT
filtered here, because `run3_conformance.bb:55` does not filter it and run3 is
what produced this run's pinned verdict. So the one `:unresolved` self-loop
`R5 -> R5` is in this list, where `checks/wm_route_conformance.clj:28` would
drop it. It is not traversed by this run, so nothing here turns on it. -/
def figureDrawnEdges : List WiringEdge :=
  [(.R1, .R4),
   (.R2, .R3),
   (.R3, .R1),
   (.R4, .R5),
   (.R5, .R6),
   (.R6, .R13),
   (.R11, .R16),
   (.R13, .R14),
   (.R14, .R16),
   (.R16, .R2),
   (.R5, .R5),
   (.R6, .R11),
   (.R7, .R3),
   (.R7, .R8),
   (.R7, .R14),
   (.R8, .R5),
   (.R9, .R16),
   (.R10, .R8),
   (.R12, .R7),
   (.R15, .R13),
   (.R15, .R16),
   (.R20, .R7)]

/-- The `:route-measured-drawn` layer: edges added to Figure 4 because a route
measurement found them. Conformance against this layer is weaker than
conformance against `figureDrawnEdges` and the certificate's docstring says so. -/
def figureMeasuredEdges : List WiringEdge :=
  [(.R20, .R12),
   (.R12, .R2),
   (.R2, .R7),
   (.R2, .R3a),
   (.R3a, .R7),
   (.R3, .R8),
   (.R6, .R14),
   (.R14, .TRACE)]

/-- The retired pairs and their grounds. -/
def figureRetiredEdges : List RetiredWiringEdge :=
  [{ edge := (.R11, .R16), grounds := [.ruling] },
   { edge := (.R13, .R14), grounds := [.code] },
   { edge := (.R14, .R16), grounds := [.code] },
   { edge := (.R2, .R3), grounds := [.code] },
   { edge := (.R2, .R7), grounds := [.code] },
   { edge := (.R5, .R6), grounds := [.ruling] },
   { edge := (.R6, .R13), grounds := [.code, .ruling] },
   { edge := (.R7, .R14), grounds := [.code] }]

/-- Decidable membership without a `BEq` detour. -/
def edgeMem (e : WiringEdge) (es : List WiringEdge) : Bool :=
  es.any (fun x => decide (x = e))

/-- The grounds recorded against a pair; `[]` when it was never retired. -/
def retirementGroundsOf (e : WiringEdge) : List RetirementGrounds :=
  match figureRetiredEdges.find? (fun r => decide (r.edge = e)) with
  | some r => r.grounds
  | none => []

/-- How run3 dispositions one hop. -/
inductive HopClass where
  | refutation
  | rulingUnrealised
  | excludedDependencyGrain
  | drawn
  | routeMeasured
  | unmapped
  deriving DecidableEq, Repr

/-- `run3_conformance.bb:116-124`, transcribed clause for clause. The order
matters and is the script's: a `code` retirement whose pair is ALSO on the
measured layer retired a dependency claim while the route stayed drawn as
measured, so it is excluded rather than a refutation. -/
def classifyHop (e : WiringEdge) : HopClass :=
  let g := retirementGroundsOf e
  let hasCode := g.any (fun x => decide (x = RetirementGrounds.code))
  let hasRuling := g.any (fun x => decide (x = RetirementGrounds.ruling))
  if hasCode && edgeMem e figureMeasuredEdges then .excludedDependencyGrain
  else if hasCode then .refutation
  else if hasRuling then .rulingUnrealised
  else if edgeMem e figureDrawnEdges then .drawn
  else if edgeMem e figureMeasuredEdges then .routeMeasured
  else .unmapped

/-- A recorded route reassembled into hops: the consecutive pairs. Written
with `zip` rather than by recursion so that `decide` reduces it in the kernel
without going through the equation compiler's `brecOn`. -/
def routeHops (r : List RouteNode) : List WiringEdge := r.zip r.tail

/-- THE CONFORMANCE VERDICT, as run3 states it and stripped of nothing:
the run recorded at least one route, no route is empty, no hop is unmapped,
and no hop is a refutation (a code-retired pair traversed at route grain).
The two retired classes run3 does NOT count against a run --
`excludedDependencyGrain` and `rulingUnrealised` -- are absent here for the
same reason they are absent there, and this run hits both, so a flat "no
retired edge traversed" would report it not conformant.

`reducible` because `decide` needs the `Decidable` instance for THIS
conjunction, and instance synthesis does not unfold an irreducible `def`. -/
@[reducible] def runConformsToDrawnWiring (routes : List (List RouteNode)) : Prop :=
  routes ≠ [] ∧
    (∀ r ∈ routes, r ≠ []) ∧
    (∀ h ∈ routes.flatMap routeHops, classifyHop h ≠ HopClass.unmapped) ∧
    (∀ h ∈ routes.flatMap routeHops, classifyHop h ≠ HopClass.refutation)

/-- The 4 routes the run recorded, in the order `:run/id` selection
returns them out of the shared trace:
`4e35e740-8c9f-42c1-b8a9-0cdfc024e9c8`,
`c51a8da3-883e-4038-b493-1b268d5d8357`,
`b69ec193-5aa0-45ca-9cbb-9df45cbb8d82`,
`28da19d2-3a03-40f6-8eef-2818fe5583a9`. -/
def s5Routes : List (List RouteNode) :=
  [[.R20, .R12, .R2, .R7, .R3, .R8, .R5, .R6, .R14, .TRACE],
   [.R20, .R12, .R2, .R7, .R3, .R8, .R5, .R6, .R14, .TRACE],
   [.R20, .R12, .R2, .R7, .R3, .R8, .R5, .R6, .R14, .TRACE],
   [.R20, .R12, .R2, .R7, .R3, .R8, .R5, .R6, .R14, .TRACE]]

/-- The run's 36 hops, 9 of them distinct. -/
def s5Hops : List WiringEdge := s5Routes.flatMap routeHops

/-- The drawn edges this run never traversed. -/
def s5UnfiredDrawnEdges : List WiringEdge :=
  figureDrawnEdges.filter (fun e => !edgeMem e s5Hops)

/-- CLOSED UNDER THE J9 CRITERION · leg (3) · THE RUN-CONFORMANCE CERTIFICATE
for the run `runs/2026-09-01-s5` (futon2 sha `5a66411`),
worklist `:U49` under Joe's RUN4 ruling of 2026-09-03. Every one of the 36
hops the run recorded is an edge of the drawn wiring on run3's own
classification, no route is empty, and no code-retired pair was traversed at
route grain. Proved by `decide` over the transcribed tables, no `sorry` and no
`native_decide` -- the `wmTraceR2`/`wmTraceR8` precedent. The Clojure side of
the same comparison is `futon2:holes/labs/wm-contract/run3_conformance.bb`,
whose pinned verdict for this run is
`runs/2026-09-01-s5/conformance.edn` `:verdict :conformant`; the mutations that
break this proposition are listed at `runs/U49-run-conformance/04-controls.edn`
control C4.

WHAT IT DOES NOT SHOW, because a reader will otherwise take it for more: 5 of
the 9 distinct hops are on the `:route-measured-drawn` layer, which is the
layer a previous route MEASUREMENT put on the figure, so for those the run is
being compared against a record of a run; and 19 of the 22 drawn edges never
fired at all. The certificate says this run stayed inside the union of the two
layers. It does not say the drawn figure predicted the run. -/
theorem wmS5RunConformsToDrawnWiring : runConformsToDrawnWiring s5Routes := by
  decide

/-- The census the certificate is stated over, so the numbers a reader checks
against `runs/2026-09-01-s5/conformance.edn` are themselves decided rather than
asserted in prose: 4 routes, 36 hops, 9 distinct, and the class split
-- 2 drawn, 5 route-measured, 1 excluded at dependency grain, 1 ruling-unrealised,
0 refutations, 0 unmapped -- with 19 of 22 drawn edges unfired. -/
theorem wmS5RouteCensus :
    s5Routes.length = 4 ∧
      s5Hops.length = 36 ∧
      s5Hops.dedup.length = 9 ∧
      (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.drawn))).length = 2 ∧
      (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.routeMeasured))).length = 5 ∧
      (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.excludedDependencyGrain))).length = 1 ∧
      (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.rulingUnrealised))).length = 1 ∧
      (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.refutation))).length = 0 ∧
      (s5Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.unmapped))).length = 0 ∧
      figureDrawnEdges.length = 22 ∧
      s5UnfiredDrawnEdges.length = 19 := by
  decide
