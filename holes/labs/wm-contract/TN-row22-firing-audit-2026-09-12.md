# Row 22 firing audit over the completed edge dispositions

Date: 2026-09-12  
Scope: read-only discovery at futon2 and p4ng HEAD; no ledger, figure, registry,
or production edit.

## Finding first: the population is 32, not 25

The request's parenthetical is authoritative about the populations but its
arithmetic is not: 21 original base edges + 8 always-on additions + 3
conditional additions = **32 base edges**, not 25. The ledger contains exactly
29 `:drawn` and 3 `:conditional` base entries
(`p4ng/empirics-futon/control-map-edges.edn:9-150`). The unresolved R5→R5
support curve is outside that population (`:112`), and the retired-source
R3a→R8 relation is a conformance disposition, not a base edge
(`p4ng/empirics-futon/aif-conformance.edn:1`). Thus this audit covers all 32;
silently auditing 25 would leave seven drawn strokes undisposed.

“Fired” below requires a retained execution fact. Static code/dependency
evidence is insufficient. The evidence grain is stated explicitly because the
old route recorder observes box-level sequencing, whereas recent witnesses and
trace fields can establish a symbol-level value at pins. WM-RUN2's full route
is retained at `tick-run-record-2026-08-30.edn:1`; its nine-hop repetition is
also present at `tick-run-record-2026-08-31.edn:1`. The exact mediator split is
the separately retained pairing named by the ledger at
`p4ng/empirics-futon/control-map-edges.edn:239-260`.

## Edge-by-edge audit

| base edge | retained firing evidence and grain | verdict |
|---|---|---|
| R1→R4 | Row-9 predictive production-match material retained a real model/parameter snapshot and production prediction (`TN-row21-discovery-2026-09-12.md:69-70`); **witness-at-pins**. | fired |
| R2→R3 | The row-7 belief/update production witness applies the retained observation to the prior/posterior pair (`TN-row21-discovery-2026-09-12.md:55-57`); **witness-at-pins**. | fired |
| R3→R1 | The same retained applied belief step carries `mu-post` as the next belief; **witness-at-pins**, not an old route hop (`TN-row21-discovery-2026-09-12.md:55-57`). | fired |
| R1→R3 | The applied update retains the carried belief read by R3; **witness-at-pins** (`TN-row21-discovery-2026-09-12.md:55-57`). | fired |
| R1→R3a | Predictive witness retains the belief and predicted observation at identical pins; **witness-at-pins**. This closes the older “no joined firing receipt” annotation for the pinned replay, not for route instrumentation (`TN-row21-discovery-2026-09-12.md:58-60`). | fired |
| R2→R3a | `pair/R2-R7-delivery.edn`, attached to this exact half at `control-map-edges.edn:239-244`; **witness-at-pins**. | fired |
| R3a→R7 | The same pair's second half, attached at `control-map-edges.edn:245-250`; **witness-at-pins**. | fired |
| R3a→R3 | The applied belief witness retains prediction-error inputs and posterior output; **witness-at-pins**. The old route's R7→R3 remains coarser (`tick-run-record-2026-08-30.edn:1`; `TN-row21-discovery-2026-09-12.md:67-68`). | fired |
| R2→R8 | RUN9 S4 retained three ticks in which observation entered F_pi; **field-presence plus replay-at-pins** (`aif-equations.edn:95-101`; `C470-s4-live-f-pi.md:1-4,128`). The always-on eps grain is independently covered by R2→R3a. | fired |
| R6→R4 | Retained ranked-action prediction fields show each candidate was evaluated by the predictor; **field-presence**, not a route hop (`TN-row21-discovery-2026-09-12.md:77-78`). | fired |
| R14→R6 | The schema-27 148-coordinate posterior fixture retains tau, scores and weights and the admitted witness replays them; **witness-at-pins** (`TN-row21-discovery-2026-09-12.md:80`; `aif-equations.edn:177-183`). | fired |
| R13→R4 | No retained record has `effective horizon >= 2`. Historical records omitted depth, and the post-capture wild records inspected retain `:horizon-steps nil` (`TN-row21-discovery-2026-09-12.md:78-79`; `runs/2026-09-05-u59-b/delta.edn:4081-4215`). | **never fired** (conditional) |
| R4→R8 | RUN9 S4 exercised previous-tick Q(o\|pi) readback on three ticks; **field-presence plus replay-at-pins** (`control-map-edges.edn:316-324`; `C470-s4-live-f-pi.md:1-4`). | fired (conditional) |
| R8→R6 | RUN9 S4 measured 133/145 posterior coordinates moving on three ticks; **witness-at-pins** (`C470-s4-live-f-pi.md:128`; `aif-equations.edn:183`). | fired (conditional) |
| R4→R5 | Ranked records retain prediction-derived per-candidate scores; **field-presence**. WM-RUN2's R8→R5 hop is only the old/coarser box label (`tick-run-record-2026-08-30.edn:1`). | fired |
| R5→R6 | Exact WM-RUN2 route hop through `policy/select-action`; **route-hop** (`tick-run-record-2026-08-30.edn:1`). | fired |
| R6→R13 | No retained route hop or joined construction record proves candidate/selection delivery into cascade construction. Static ordering is not firing (`TN-row21-discovery-2026-09-12.md:32-36`). | **never fired** |
| R11→R16 | No retained arbitration-to-enactment delivery. The ledger itself says no implementation carries an R9 verdict into `enact!`, and the drawn R11 boundary has no run receipt (`control-map-edges.edn:151-175`). | **never fired** |
| R13→R14 | No retained route hop or joined field envelope for cascade-to-gain delivery. | **never fired** |
| R14→R16 | No retained route hop or joined selector-to-enactment delivery at this R-number grain. | **never fired** |
| R16→R2 | No two-tick retained receipt shows a construction enacted and then observed. Its ledger schema remains largely unspecified and its producer is an in-model wiring map (`control-map-edges.edn:118-150`). | **never fired** |
| R6→R11 | No retained arbitration input/delivery receipt. | **never fired** |
| R7→R3 | Exact WM-RUN2 `apply-arena-belief-events` hop; **route-hop** (`tick-run-record-2026-08-30.edn:1`). | fired |
| R7→R8 | Precision and F_pi inputs coexist in the retained RUN9 replay; **field-presence at pins**, not a route hop (`C470-s4-live-f-pi.md:118-133`). | fired |
| R7→R14 | No retained joined precision-to-temperature input. Tau evidence proves R14→R6, not this upstream edge. | **never fired** |
| R8→R5 | Exact WM-RUN2 route hop; **route-hop only**. The ledger correctly warns that no value crosses it (`control-map-edges.edn:113-117`; `tick-run-record-2026-08-30.edn:1`). | fired as route sequencing |
| R9→R16 | No implementation carries the delivered verdict into enactment; this is stated in the ledger's schema note (`control-map-edges.edn:151-175`). | **never fired** |
| R10→R8 | No retained edge-grain scheduler-to-F/F_pi delivery. A scheduled run starting a tick is not proof that the scheduler was an input to R8 (`control-map-edges.edn:198-199`). | **never fired** |
| R12→R7 | WM-RUN2 records coarser R20→R12→R2→R7; **route-hop at apparatus grain**, not symbol-grain direct delivery (`control-map-edges.edn:225-238`; `tick-run-record-2026-08-30.edn:1`). | fired at coarse route grain |
| R15→R13 | No retained slow-state-to-cascade joined receipt. | **never fired** |
| R15→R16 | No retained tactical-outcome-to-enactment delivery; the catalogue audit calls the connected slow/fast runtime receipt unlocated (`COMPLETION-LIST-plop2026-2026-09-12.md:125`). | **never fired** |
| R20→R7 | WM-RUN2 retains R20→R12→R2→R7; **coarse route path**, explicitly decomposed rather than asserted as a direct symbol call (`control-map-edges.edn:225-250`). | fired at coarse route grain |

Result: **20 fired at at least one declared grain; 12 never fired**. Of the
three conditionals, R4→R8 and R8→R6 fired in RUN9; R13→R4 has no retained
activation. This count does not promote coarse route evidence into a
symbol-level measurement: the table preserves that qualification.

## Never-fired verdicts for the qualifying run

The row's default makes edges traversed by the full loop mandatory, even where
today's instrumentation must be extended to prove them.

* **(a) Full-loop-traversed; mandatory by default:** R6→R13, R13→R14,
  R14→R16, and R16→R2. These form the selection/cascade/enact/re-observe
  continuation claimed by the complete loop. The qualifying run must retain
  joined evidence for each; merely finishing does not discharge them.
* **(b) Conditional; Joe must decide the run configuration:** R13→R4. Enable
  a real anticipation state giving effective horizon ≥2, or explicitly rule
  that depth arm out of the qualifying run. For completeness, the two already
  fired conditionals still need an explicit row-28 choice: R4→R8 requires
  previous-tick policy details plus `FUTON_WM_FPI_DARK=1`; R8→R6 additionally
  requires `FUTON_WM_FPI_POSTERIOR=1` and complete coverage
  (`control-map-edges.edn:49-70`). This three-item flag/depth list feeds row
  24's negative-scope ruling; prior firing does not silently set the future
  configuration.
* **(c) Genuinely aspirational candidates; Joe's marking ruling required:**
  R11→R16, R6→R11, R7→R14, R9→R16, R10→R8, R15→R13, and R15→R16. Current
  retained evidence either explicitly says the consumer is absent (R9→R16),
  identifies a conflation (R7→R14), or supplies no invocation receipt. They
  should not become “fired” merely because the figure contains them.

These classes enumerate rather than make Joe's two choices: whether to enable
the conditional depth/F_pi family in the qualifying configuration, and which
class-(c) strokes receive an aspirational figure mark.

## Implementation split and row 28 handoff

1. **One marking packet:** add evidence-grain-aware `:firing` annotations to
   all 32 ledger rows, apply `:aspirational` only to Joe-approved class-(c)
   rows, extend the hand-maintained figure edit specification, and make the
   generator refuse an unclassified base edge. No registry edit belongs in
   that packet.
2. **One qualifying-run configuration decision:** row 28 must state effective
   horizon policy and the three F_pi/detail flags, and must require retained
   joined evidence for the four full-loop continuation edges. Cross-reference
   this as row-24 inventory item 17: its negative scope is the explicit set of
   conditionals/aspirational edges Joe elects not to exercise, never an
   inferred omission.

The row-21 retired-source R3a→R8 remains outside both packets: historical
firing of a deleted producer does not make it a current qualifying-run edge.
