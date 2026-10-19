# P39 — operator frontier reconciliation (R-first)

**Read pin:** `futon2@a60bf4e80c5a0dd4441ae8d3086a9fcc3c93fd34`
(2026-10-19). This is a source review of P34a–P38/P40. It does not claim a
serving reload, a click, a canonical-store read, or an operator's use of any
renderer.

## Result

The landed code retains the two new measurements in a terminal run record and
the abstention debugger renders the lifecycle census. It does **not** yet make
the reading selection/preparation split visible in that debugger, and a second
operator renderer (`wm_run_validity.bb`) still reduces the legacy
`:open-stop-lines` collection to one unqualified count. Those are source
proven reporting gaps, not evidence that the retained values are wrong.

The P40 flight choice contract is also source-present but not production
reachable: the repository-wide caller census finds `flight/choose-target` only
in `flight.clj` and its two test namespaces. It must not be described to an
operator as a live repair-priority outcome.

## What is retained, and where it reaches a reader

| Measurement | Canonical producer and authority | Retained carrier | Current operator reader | Evidence status |
| --- | --- | --- | --- | --- |
| Repair lifecycle buckets, counts and IDs | `repair-obligation/lifecycle-census` partitions every finding into `:actionable-open`, `:awaiting-validation`, `:environmental-hold`, or `:closed`; it returns schema, finding count and reconciliation. `b683f6419`, `src/futon2/aif/repair_obligation.clj`, `lifecycle-census`, lines 974–1017. | Runner reads it beside—not in place of—operational `open-obligations`, then persists `:repair-lifecycle-census`. `48400e480`, `full_loop_runner.clj`, stop-line memory lines 6495–6512; `persist-run-record!`, lines 1321–1327. | For an abstention, `wm_click_debugger.bb` copies that field (lines 9–38) and `wm_click_repair_loop.py` renders schema, reconciliation, total and all four bucket counts/IDs (lines 528–547, 550–574). | Source-present; isolated P36 case is recorded in the packet ledger, not live evidence. A legacy record is explicitly rendered unavailable, not reinterpreted. |
| Operational validation route | `open-obligations` remains the collection filtered for awaiting-validation/environmental hold (not a lifecycle replacement). `full_loop_runner.clj`, lines 6497–6518. | `:open-stop-lines` remains separately retained (lines 1321–1323). | The validity script counts it as `open-stop-lines` without lifecycle qualification. `scripts/wm_run_validity.bb`, `report-run!`, lines 246–270. | Source-present; this count is not executable-repair authority. |
| Selection frontier versus bounded preparation | `retained-reading-frontiers` preserves producer menu IDs (`eligible`, `assessed`, `reading-absent`, other exclusions) separately from `:preparation-backlog`; it expressly forbids reading pending as a selection verdict. `ef5920b98`/`48603af5d`, `full_loop_runner.clj`, lines 1068–1107. `persist-run-record!` writes it as `:reading-frontiers` at line 1297. | Terminal run record contains both `[:reading-frontiers :selection-frontier]` and `[:reading-frontiers :preparation-backlog]`. | No current debugger or validity renderer reads `:reading-frontiers`: the debugger emits only lifecycle data (above), and validity prints only `:open-stop-lines`. | Source-present; P38 focused isolated-path controls are test evidence, not live measurement. |
| Repair proposal / flight priority | `repair-proposals/supply` takes only actionable IDs, keeps all lifecycle buckets and reconciles each actionable ID to a proposal or pinned blocked record. `072dc0404`, `repair_proposals.clj`, `supply`, lines 70–114. `flight/choose-target` accepts only concrete supplied proposals, retains supply evidence, and deliberately ignores `:open-obligations`. `2c9c4dacd`, `flight.clj`, lines 311–349. | If a caller invokes it, `:chosen-because` retains the supplied read model. | No non-test caller exists: `rg 'choose-target'` returns only `src/futon2/aif/flight.clj`, `test/futon2/aif/flight_test.clj`, and `test/futon2/aif/flight_lifecycle_exits_test.clj`; `rg 'repair-proposals/supply|repair-proposal-supply'` returns source plus `flight_test.clj`. | Static source fact and isolated tests only; no live flight selection is established. |

`repair-proposals/supply` contains a value digest under
`:repair-scan :lifecycle-census-pin` (`repair_proposals.clj`, lines 92–114),
but no production call currently carries it to flight. The debugger's lifecycle
display is instead reproducible from the retained run-record field; it does not
print that digest. Thus this review does not call the displayed number
content-address-pinned beyond the terminal record that contains it.

## Historical counterexample and correction

The retained historical note in `wm_run_validity.bb` says that on 2026-09-20 a
run reported `VALID 4/5` while carrying 45 stop lines, including a construction
group that had been circling (`41655601`, `scripts/wm_run_validity.bb`, lines
259–265). It supports the narrower statement that a headline validity result
can omit unresolved work. It does **not** establish that 45 were executable
repairs, nor does it supply a lifecycle partition for that run.

The correction is therefore: report the retained lifecycle receipt as
`actionable-open`, `awaiting-validation`, `environmental-hold`, and `closed`,
each with its IDs, and retain `open-stop-lines` only under its operational
validation meaning. Never convert an absent lifecycle receipt into zero, and
never label the old aggregate “actionable.”

## Proposed bounded C packet: renderer reconciliation only

Change only the operator renderers, not selection, validation routing,
lifecycle classification, or flight choice.

1. In `scripts/wm_run_validity.bb/report-run!`, render
   `:repair-lifecycle-census` with schema, reconciliation state and the four
   bucket counts/IDs; for legacy records say typed unavailable. Retain any
   `:open-stop-lines` line as “operational validation candidates,” never as
   “open repairs.”
2. In `scripts/wm_click_debugger.bb`, project `:reading-frontiers` exactly
   from the terminal record. In `wm_click_repair_loop.py/debugger_page`, render
   selection eligible, assessed, reading-absent and other exclusions separately
   from preparation status, published/refused/pending records and deadline.
   Render missing or legacy fields as unavailable; do not calculate substitute
   counts.
3. Add renderer-only fixtures that contain (a) a lifecycle census with zero
   actionable and nonzero other buckets, and (b) a 700/2/698 selection frontier
   with a distinct 697-pending preparation receipt. Assert the labels and IDs
   remain separate. These fixtures establish display fidelity only, never live
   frontier values.

The P40 missing production caller is a separate integration decision, not a
renderer edit: a future bounded packet must identify the real flight creation
entrypoint and pass `repair-proposals/supply` there with a caller-path test.
Until then the safe report is “flight priority contract present; no production
caller found.”

## Limits

This review performed no tests by instruction and did not inspect a live run
record or store. The cited 700/2/698 and 697 values occur in the P38 focused
test/packet record; they are not claimed as a live current count. Source
presence and isolated-test coverage do not establish that the serving JVM has
loaded these commits, that a click reached the terminal writer, or that an
operator saw the resulting page.
