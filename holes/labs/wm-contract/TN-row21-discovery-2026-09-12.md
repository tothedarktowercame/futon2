# Row 21 discovery: the twelve undrawn connections

Date: 2026-09-12  
Scope: discovery only; no diagram, registry, generator, or production edit.  
Source state read: futon2 `HEAD` and p4ng `HEAD` on 2026-09-12.

## Finding first

The number **twelve is still reproducible at HEAD**, but it has a precise and
narrow meaning: it is the set difference between equation-derived edges and
the 21 base-figure edges, after subtracting the two `:not-realised` edges and
the one `:path-dependent` edge. It is not a fresh census of every connection
added to code since 2026-09-01. The derivation is implemented at
`p4ng/empirics-futon/gen_aif_dag.bb:62-84`, and a disposable run at HEAD
reported 22 theory edges, 21 base-drawn edges, 15 missing pairs, 2
not-realised, 1 path-dependent, and 12 realised-undrawn. The committed output
lists those same twelve at `p4ng/empirics-futon/aif-conformance.edn:1`; its
embedded equation pin is stale (`35cda9ab` versus current `15aa4341`), but a
fresh derivation did not change the population.

The phrase in the work list was introduced by futon2 commit `247a718b` at
`holes/labs/wm-contract/WORK-REMAINING.md:807-815`. Its recoverable numerical
provenance is p4ng commit `eb1c799`, which first generated the
`:realised-undrawn` split. Thus Row 21's twelve are the generator's twelve,
not an undocumented hand count.

## 1. Drawn state and renderer

The authority says it is a lift of `aif-control-map-paper.svg` and defines
`:drawn` as “as in the figure,” not “works in the running system”
(`p4ng/empirics-futon/control-map-edges.edn:1-9`). Its 21 entries comprise:

* control: R1→R4, R2→R3, R3→R1, R4→R5, R5→R6, R6→R13,
  R11→R16, R13→R14, R14→R16, R16→R2
  (`p4ng/empirics-futon/control-map-edges.edn:14-55`);
* support: R6→R11, R7→R3, R7→R8, R7→R14, R8→R5, R9→R16,
  R10→R8, R12→R7, R15→R13, R15→R16, R20→R7
  (`p4ng/empirics-futon/control-map-edges.edn:57-79`).

The R5→R5 support curve is unresolved and therefore is not among the 21
(`p4ng/empirics-futon/control-map-edges.edn:56`). R20→R14 is likewise not a
base edge: it is a chartered theory edge in `:derived-undrawn`
(`p4ng/empirics-futon/control-map-edges.edn:97-110`).

There are three representations, which must not be collapsed:

1. `aif-control-map-paper.svg` is the base source lifted into the ledger.
2. `gen_live_topology.bb` renders that base plus measured routes, decision
   additions, and the three conformance classes
   (`p4ng/empirics-futon/gen_live_topology.bb:400-432`). Consequently the
   twelve already appear as amber dashed relations in
   `aif-control-map-live.svg`; Row 21 concerns promotion to truthful base
   wiring or an explicit edge disposition, not making them visible for the
   first time.
3. `gen_aif_dag.bb` renders symbol dependencies and writes the conformance
   table (`p4ng/empirics-futon/gen_aif_dag.bb:1-7,142-177`).

The papers include the live map and explain that a dependency is not proof a
call occurred at `p4ng/sec-operator.tex:24-51`; the PLoP catalogue also includes
the live map at `p4ng/sec-overview-plop.tex:72-83`.

## 2. Edge-by-edge disposition

“Witness” below means retained evidence of the value/call, not merely the
equation-derived relation. The generator's edge rule is source-symbol node to
importing-equation node (`p4ng/empirics-futon/gen_aif_dag.bb:62-79`).

| edge | code evidence at current source | retained firing evidence | Row 21 verdict |
|---|---|---|---|
| R1→R3 | The carried belief is constructed/read at `war_machine.clj:6105-6110`; the inner update starts from that belief at `war_machine.clj:6179-6184`; R3's driver and update are identified in `aif-equations.edn:113-118`. | F8 belief/update artifacts measure the functions, but no route record names this symbol-grain import; the old route only stamps R7→R3 (`tick-run-record-2026-08-30.edn:1`). | **Draw**, label `mu`. This is the missing read leg of the already drawn R3→R1 recurrence; the accepted code decision already says so at `control-map-edges.edn:214-220`. |
| R1→R3a | `belief/predict-observation` consumes the current categorical at `belief.clj:1199-1228`; the tick calls it with loop `belief` at `war_machine.clj:6181-6191`, before constructing prediction errors. | Real trace records retain `mu` and prediction-error products, but no retained dependency envelope proves which `mu` produced them; F8 is a function replay rather than this live join. | **Draw**, label `mu`, with “source-backed; no joined firing receipt” annotation until Row 22/run evidence closes that distinction. |
| R2→R3a | `compute-prediction-error` explicitly takes observed and predicted values (`free_energy.clj:203-218`); the registry imports `o` into prediction-error at `aif-equations.edn:74-85`. | The pairing receipt is explicitly attached to R2→R3a at `control-map-edges.edn:130-134`. | **Draw**, label `o`; this is the measured first half of the mediator path, not the retired R2→R3 shortcut. |
| R3a→R7 | Precision consumes a prediction-errors map at `precision.clj:160-175`; the registry import is `eps` at `aif-equations.edn:86-91`. | `pair/R2-R7-delivery.edn` is attached to this exact hop at `control-map-edges.edn:135-139`; WM-RUN2 also records the coarser R2→R7 route at `control-map-edges.edn:125-129`. | **Draw**, label `eps`. Do not also assert a direct symbol-grain R2→R7 edge; the ledger retired that interpretation at `control-map-edges.edn:181-187`. |
| R3a→R3 | The R3 aggregate driver consumes weighted prediction errors (`belief.clj:1122-1145`); its registry row imports `eps` at `aif-equations.edn:113-118`. | The old route stamps R7→R3 rather than this dependency (`tick-run-record-2026-08-30.edn:1`); no direct joined receipt was found. | **Draw**, label `eps`, but annotate “dependency, route instrumentation is coarser.” The historical decision used the obsolete R8 host name (`control-map-edges.edn:200-206`); use R3a, the current equation node. |
| R3a→R8 | This arises only because the retired `:free-energy` row still imports `eps` (`aif-equations.edn:92-101`). Its own `:code` says **NO PRODUCER AT HEAD** and points to the deletion. | Historical WM-RUN2 recorded an R3→R8 call to the now-deleted producer (`tick-run-record-2026-08-30.edn:1`); that is not current firing evidence. | **Keep off and annotate `retired-source-row`**. This is the decisive correction to treating all twelve as positive additions. The generator currently subtracts only hole statuses, not retired equations (`gen_aif_dag.bb:73-84`), so this false-positive belongs in the first repair packet. |
| R2→R8 | Current prediction-error semantics import observation (`aif-equations.edn:80-85`), and policy-free-energy independently imports observation (`aif-equations.edn:102-108`). | R2→R3a is measured by the pairing receipt (`control-map-edges.edn:130-134`); RUN9's F-pi branch is described as firing on three ticks at `control-map-edges.edn:172-179`. | **Draw one R2→R8 pair with two bases**: always-on `o→eps` through R3a, plus flag-gated `o→F_pi`. The label/annotation must expose the two grains rather than minting duplicate strokes. |
| R6→R4 | `efe/rank-actions` evaluates every candidate through the predictor; the live call is at `war_machine.clj:6441-6442`, while `compute-efe` calls `fm/predict` and conditionally `predict-multi-horizon` at `efe.clj:631-639`. | Existing trace ranks prove candidates were scored, but the legacy route collapses this into R8→R5 and does not record the candidate→predictor call. The new machine-Q work is opt-in and is not evidence that the live option fired. | **Draw**, label `candidate action` (not `Q(pi)`). The grain warning is already explicit at `control-map-edges.edn:221-227`. |
| R13→R4 | `horizon-steps >= 2` selects `predict-multi-horizon` at `efe.clj:631-639`; the tick computes the effective horizon at `war_machine.clj:6373-6380` and passes it in the EFE opts at `war_machine.clj:6419-6426`. | Historical runs could not answer whether it fired (`control-map-edges.edn:192-199`). Schema-28 retains effective depth for newer records, but that is Row 22 evidence work, not established here. | **Draw conditional**, label `T`, condition `effective horizon ≥ 2`; never show it as unconditional. |
| R4→R8 | F-pi consumes the forward-model's Q(o\|pi); the current registry code chain is pinned at `aif-equations.edn:102-108`. | RUN9 exercised the flag on three of four ticks and records the complete-or-off case (`control-map-edges.edn:172-179`). | **Draw conditional**, label `Q(o\|pi)`, with the previous-tick/readback and flag condition already specified at `control-map-edges.edn:164-171`. Row 14's new machine-Q scorer is a separate R4→R5 scoring path and should get its own later census; it is not grounds to relabel this F-pi edge. |
| R14→R6 | The exact policy score divides G by tau at `policy.clj:157-219`, and softmax normalises it at `policy.clj:221-235`; the registry binds tau→policy-posterior at `aif-equations.edn:171-182`. | The schema-27 details-on R6 fixture retained tau, scores, and all 148 weights; the accepted witness location is `runs/row-16-r6-posterior-2026-09-12/` (the registry's carrier rebinding is at `aif-equations.edn:177-183`). | **Draw**, label `tau`. Annotate that tau is computed inside the selection function rather than delivered by an independently invoked R14 box, the subtlety already recorded at `control-map-edges.edn:228-234`. |
| R8→R6 | With F-pi enabled, `selection-scores` validates and subtracts the aligned term at `policy.clj:168-218`; the registry imports `F-pi` at `aif-equations.edn:177-183`. | RUN9 reports three ticks entered, one declined for incomplete coverage, 133/145 posterior coordinates moved, argmax unchanged (`control-map-edges.edn:172-179`). | **Draw conditional**, label `F_pi`, with its full preconditions. Also annotate that it moves the recorded posterior, not the live-selected action. |

### Count after disposition

At the historical twelve-edge population: **8 should be ordinary drawn wiring,
3 should be drawn as conditional wiring, and 1 should remain off with an
explicit retired-source annotation**. The count remains twelve; the claim that
all twelve “exist in code” does not survive current source because R3a→R8's
sole target producer is retired.

Today's additional work does not silently enlarge this number. R20→R14 is
chartered, not implemented (`control-map-edges.edn:97-110`); the machine-Q
option creates a code path into R5 but requires its own symbol/node ruling; the
reason-bearing selector extends the R16 selection boundary rather than adding
a second R-numbered node. Those are annotations/census candidates, not members
of the generator-defined twelve.

## 3. Row 21 versus Row 22

Row 21 answers **whether and how a relation belongs on the map**. Row 22 asks
whether a drawn relation has fired in retained runs and whether the qualifying
run must exercise it (`WORK-REMAINING.md:809-815`). Therefore “no joined firing
receipt” above is not grounds to suppress a source-realised edge; it is a Row
22 finding. In particular R1→R3a, R3a→R3, R6→R4, and current R13→R4 need
explicit post-schema evidence decisions in Row 22. Conversely R3a→R8 is a Row
21 exclusion because its producer is absent at HEAD, regardless of historical
firing.

## 4. Smallest reviewed implementation split

1. **Classification repair (one generator behaviour).** Make retired equation
   rows unable to enter `:realised-undrawn`; retain R3a→R8 as a typed historical
   annotation. Acceptance: disposable conformance generation reports 11
   current positive members plus one named retired disposition, and a planted
   retired importer cannot render amber.
2. **Always-on base batch (one edge-ledger/figure batch).** Add the eight
   ordinary edges above, with symbol-grain labels and the R6 candidate-space
   qualification. Acceptance: ledger-to-SVG check plus one negative control per
   duplicate/reversed edge; no registry change is bundled.
3. **Conditional base batch (one edge-ledger/figure batch).** Add R13→R4,
   R4→R8, and R8→R6 using a visible conditional style and exact activation
   conditions. Acceptance: generator refuses a conditional edge without its
   condition and renders all three distinctly from always-on wiring.
4. **Row 22 firing audit (separate row).** Join schema-28/live records to the
   resulting base population, classify fired/unfired, and mark aspirational
   edges. It must not rewrite the source/code verdicts above merely because an
   old route recorder lacked symbol-grain instrumentation.

Canonical registry edits, including any reinterpretation of node grain, remain
behind the established second-read gate; this discovery note authorizes none.
