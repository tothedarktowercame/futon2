# Tick path: 13 handoffs, 8 freehand ports, 10 without validated uptake

Observed 2026-08-31. The bounded path is the one-shot entry point in
`scripts/futon2/run_tick_once.clj:192-228`; it delegates the report scan to
`scripts/futon2/report/war_machine.clj:4982-5070`. “Freehand” means no cited
interface or schema checker emits the handoff type. “None” means no uptake
validator is visible at the cited consumer.

| # | handoff | emitter → consumer | deposit | discipline | uptake |
|---:|---|---|---|---|---|
| 1 | capability star map | EDN file → report scan | in-memory map | freehand | readability tracked; no shape check |
| 2 | ratified mission-domain view | EDN file → report scan | in-memory map | freehand | readability tracked; no shape check |
| 3 | mission-fold view | EDN file → report scan | in-memory map | freehand | readability tracked; no shape check |
| 4 | forward-model centrality | JSON file → report scan | in-memory map | freehand | readability tracked; no shape check |
| 5 | forward-model ROI results | EDN file → report scan | in-memory map | freehand | readability tracked; no shape check |
| 6 | invariant model | EDN file → report scan | in-memory map | freehand | readability tracked; no shape check |
| 7 | bounded evidence/store basis | evidence HTTP API → tick/report | in-memory count/latest/sample | freehand | endpoint diagnostics only |
| 8 | selector seam | live resolver or declared stub → `judge` | in-memory option | freehand | selector seam string recorded, not shape-validated |
| 9 | judgement/route | `judge` → one-shot wrapper | in-memory `:judgement` | schema-checked | route conformance check; full judgement shape not checked |
| 10 | C preference stack | `efe/preference-stack-record` → receipt | judgement/receipt map | interface-emitted | witness registry check exists |
| 11 | trace | `trace/write-trace!` → dated trace | `data/wm-trace/wm-trace-<date>.edn` | schema-checked | write success checked; no reader validation in this entry point |
| 12 | run receipt | `tick-run-record`/`write-receipt!` → witness | `holes/labs/wm-contract/tick-run-record-<date>.edn` | interface-emitted (`TickRunRecord`) | `wm_runs_once_witness/validate!` |
| 13 | witness acknowledgement | owner gate → contract lint/registry consumer | `checks/witness-registry.edn` | schema-checked | contract lint |

## Evidence for the rows

- **Observed.** The six tracked file inputs are read at
  `scripts/futon2/report/war_machine.clj:546,585,597,624,647,3861` through
  readers that distinguish unreadable/missing data but do not validate a
  shared domain type (`scripts/futon2/report/war_machine.clj:494-535`).
- **Observed.** `store-basis` performs two JSON requests and constructs its map
  without a schema validator (`scripts/futon2/run_tick_once.clj:107-119`).
  `evidence-sample` performs the bounded evidence fetch and retains endpoint
  diagnostics (`scripts/futon2/run_tick_once.clj:121-129`).
- **Observed.** The report reads evidence and named scan inputs before `judge`;
  it attaches `input-status` to both scan
  data and judgement (`scripts/futon2/report/war_machine.clj:4982-5053`). The
  tracked file readers distinguish missing/unreadable inputs
  (`scripts/futon2/report/war_machine.clj:494-535`), but the in-memory aggregate
  maps have no common emitted interface.
- **Observed.** The selector is dynamically resolved and otherwise replaced by
  a declared stub; the chosen seam is recorded (`scripts/futon2/run_tick_once.clj:55-100,201-212`).
- **Observed.** The trace writer is invoked inside report generation and a
  write failure is made explicit (`scripts/futon2/report/war_machine.clj:4758-4773`;
  `src/futon2/aif/trace.clj:290-304`). This entry point checks file-stat change,
  not trace uptake (`scripts/futon2/run_tick_once.clj:197-217`).
- **Observed.** `tick-run-record` copies the Lean field set and `write-receipt!`
  deposits one EDN form (`scripts/futon2/run_tick_once.clj:166-187`;
  `/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:566-581`). The uptake
  checker validates the fields and trace flag (`checks/wm_runs_once_witness.clj:26-49`),
  and the registry records its passed run (`checks/witness-registry.edn:51-59`).

## Exemplar and loss check

The anchored instance is `:wm/run-once-receipt-chain` in
`/home/joe/code/p4ng/empirics-futon/hyper-edge-schema.edn`. The same file
re-expresses `R16→R2` and `R9→R16` with their payloads, unspecified operational
fields, no-current-traffic status, and R16’s consumer constraint. Nothing in
the binary schemas was lost. The port view adds an honest defect: both edges’
types are currently `:freehand`, and neither has implemented uptake validation.

The census counts port discipline at each named handoff, not every individual
source file read by the report. Splitting row 5 into every file would exceed
the bounded tick-path question without improving its type classification.
