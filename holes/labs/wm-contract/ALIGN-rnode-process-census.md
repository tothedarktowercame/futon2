# R-node ↔ process-assurance alignment census

**Date:** 2026-09-05. **Numbering:** every R-number below is the catalogue
numbering used by `/home/joe/code/p4ng/empirics-futon/control-stages.edn`, not
the older contract numbering. The divergence is recorded in
`/home/joe/code/p4ng/R-concordance.md:8-12,30-41`.

This is a facts census. `exists` means running code refuses advancement or
records the cell. `named-only` means a source names the conduct but no running
check refuses or records that lifecycle transition. `absent` means the searches
below found no node-linked implementation. These meanings apply the governing
rule in `N-process-trap-recording-conventions.md:12-30`.

## 1. Search trail and scope

The assurance-band roster came from:

```bash
rg -n ':band :assurance' /home/joe/code/p4ng/empirics-futon/control-stages.edn
```

This yields R10, R12, R20, R17, R9 and TRACE at lines 39-44. The requested
rows omit R17, so this census does too.

To find loop-band implementations that mention dispatch or handoff conduct, I
ran:

```bash
rg -n -i '\b(dispatch|handoff|commission|park|returned|independent-review|author-dispatch)\b' \
  /home/joe/code/futon2/scripts/futon2/report/war_machine.clj \
  /home/joe/code/futon2/src/futon2/aif
```

The node-linked hit is R16: `enact.clj:1-16` identifies the node and says its
tick-path execution is artifact-only, while `pattern_registry.clj:298-348`
constructs an `author-dispatch → independent-review → grounded-implementation`
route. The separate full-loop actuator executes those phases:
`full_loop_runner.clj:2751-2829,2913-2991`. R6 selects actions
(`war_machine.clj:6430-6488`) but does not dispatch or hand them off; R14's
`invoke-strategic-selection` is an in-process function call, not an Agency
invoke. R16 is therefore the one added loop-band row.

For every `absent` cell, the node-link search was:

```bash
rg -n '(:node|:wm/node|:route/node|:control-node)[[:space:]]+:(R9|R10|R12|R20|R16|TRACE)' \
  /home/joe/code/futon3c/src/futon3c/agency \
  /home/joe/code/futon3c/src/futon3c/social \
  /home/joe/code/futon3c/src/futon3c/transport
```

Result: no hits. Agency nevertheless has generic lifecycle machinery:
dispatch receipts are validated or rejected at
`futon3c/src/futon3c/social/dispatch.clj:238-266`; invoke and invoke-result
edges are both recorded at
`futon3c/src/futon3c/social/coordination_ledger.clj:84-110`; and durable park
records are validated and persisted at
`futon3c/src/futon3c/agency/parked_on.clj:332-395`. None associates that
conduct with a control-stage node. Generic machinery is not credited as an
R-node assurance.

For the two R16 absence claims I also ran:

```bash
rg -n 'park!|:parked|notify/discharged-at|bulletin' \
  /home/joe/code/futon2/src/futon2/aif/full_loop_runner.clj
```

Result: no hits. This distinguishes the runner's typed stop-line/recovery
records from a parked lifecycle transition, and its returned result from a
recorded surfacing discharge.

Scope searched: the four requested documents, `war_machine.clj`, every file
under `futon2/src/futon2/aif/`, and the Clojure files under futon3c's
`agency/`, `social/`, and `transport/` trees. Not searched: tests, git history,
untracked stores, other Futon repositories, Emacs/voxterm presentation code,
or APM-specific namespaces outside the requested handoff machinery. This is a
static source census, not a live invocation trace.

## 2. Matrix

| control-stages node | commissioned | dispatched | parked | returned | checked | recorded | surfaced |
|---|---|---|---|---|---|---|---|
| R9 — No self-certification | absent [A] | absent [A] | absent [A] | absent [A] | named-only [N9] | absent [A] | absent [A] |
| R10 — Scheduled entrypoint | absent [A] | absent [A] | absent [A] | absent [A] | absent [A] | absent [A] | absent [A] |
| R12 — Two-layer calibration | absent [A] | absent [A] | absent [A] | absent [A] | absent [A] | absent [A] | absent [A] |
| R20 — Interoceptive tripwires | absent [A] | absent [A] | absent [A] | absent [A] | named-only [N20] | absent [A] | absent [A] |
| TRACE — WM trace store | absent [A] | absent [A] | absent [A] | absent [A] | absent [A] | exists [E-T] | absent [A] |
| R16 — Grounded actuation | exists [E-16-C] | exists [E-16-D] | absent [A16] | exists [E-16-R] | exists [E-16-K] | exists [E-16-X] | absent [A16] |

### Cell evidence

- **[A] absent:** the node-link command in §1 returned no matches in Agency's
  bell/park/invoke/roster implementation surface. The lifecycle itself is
  named as `commissioned → dispatched → parked → returned → checked → recorded
  → surfaced` in `P-assured-process.md:55-63`, but that general statement does
  not establish any individual node cell.
  **Basis amendment for R12, R20 and TRACE cells (2026-09-06, claude-1, from
  the PA-track pattern-v2 exit-4 — futon2 `2d3106c9`):** under the widened
  search (constructor-call form + the futon2 paths §1 already declares), the
  node-link search now RETURNS matches for these three nodes: route hops at
  `war_machine.clj:7063` (R12), `:7055` (R20), `:6789` and
  `full_loop_runner.clj:2409` (TRACE). The verdicts stand; the stated basis
  above ("returned no matches") is superseded for these cells. The disposing
  reading is [E-T]'s own sentence generalised: a route hop appends
  `{:node … :via … :at …}` to the route vector and commissions, dispatches,
  parks, returns, checks and surfaces nothing; it credits at most `recorded`,
  and for R12/R20 not even that — the live fixtures (`0a18c4f7-R12.edn`,
  `0a18c4f7-R20.edn`, both `:reason :no-record-field`) show the route NAMES
  the node while no record field carries its content. Absence at these 19
  cells is henceforth established by this adjudicated reading of the hits,
  not by an empty search. (Same repair class as the [E-T] pointer note: there
  a citation rotted; here a reason rotted. Evidence ages in more than one
  way.)
- **[N9] named-only:** control-stages names R9 “No self-certification” at
  `control-stages.edn:43`; `P-assured-process.md:55-63` calls for
  author-never-closes-own-handoff as architecture. No node-linked refusing
  check was found by [A].
- **[N20] named-only:** control-stages names R20 “Interoceptive tripwires” at
  `control-stages.edn:41`. That names checking at the node level, but the [A]
  search found no R20-linked handoff checker.
- **[E-T] exists:** the WM attaches `:TRACE` to `:wm/route` immediately before
  the write at `war_machine.clj:6750-6759`; `trace/write-trace!` constructs and
  appends the exact record at `src/futon2/aif/trace.clj:723-745`. This credits
  only `recorded`: it is a tick route record, not evidence that any other
  process stage occurred.
  **Pointer correction (2026-09-06, claude-1):** the `war_machine.clj`
  citation has drifted under the wm loop's continuous edits — the `:TRACE`
  attachment is now at `war_machine.clj:6789` (re-read at today's HEAD, the
  same `route-tag :TRACE "futon2.aif.trace/write-trace!"` form). Found by
  the PA1z census harness (zaif-harness `census-ledger.edn`), which refused
  to credit the cell over the stale pointer; the `trace.clj:723-745` half
  still lands. The verdict is unchanged; only the line citation moved. The
  original range above is kept as dated history.
- **[E-16-C] exists:** the author commission names the selected target,
  repository/base head, artifact contract, requirements, refusal shape and
  independent reviewer at `src/futon2/aif/full_loop_runner.clj:1350-1406`; that
  exact prompt is passed to dispatch at `full_loop_runner.clj:2740-2761`.
- **[E-16-D] exists:** `dispatch!` submits agent, caller, mission id and prompt
  to Agency (`full_loop_runner.clj:755-767`); the dispatch checkpoint records
  agent, job id, prompt reference and response at `:2762-2775`.
- **[A16] absent:** the R16-specific command in §1 found neither a park record
  nor `notify/discharged-at`/bulletin discharge in the full-loop runner.
- **[E-16-R] exists:** `poll-job!` returns only at an Agency terminal state
  (`full_loop_runner.clj:825-849`), and the author boundary refuses to advance
  unless that returned state is `done` (`:2823-2829`).
  **Qualification (2026-09-06, claude-1, from PA11z finding 4 — futon2
  `4963f5e5`):** the verdict stands (the state transition is running code),
  but what this cell credits is the *state*, not the *artifact*. The job
  record stores the returned report TRIMMED — measured on a live dispatch:
  2011 of 6275 chars stored, ending in a literal `…[trimmed]` inside the
  text, `:result-summary` cut at 220; re-fetch returns the same fragment,
  so it is storage, not display. The full artifact exists only in the
  transient bellback delivery. A checker reconstructing a return from the
  record via `poll-job!` reads a third of the report with no typed field
  saying so. Carried into the zaif lane's PA7z bar: admission must refuse
  a stored-incomplete return, and "incomplete" must become a typed field.
- **[E-16-K] exists:** after a distinct reviewer dispatch and return
  (`full_loop_runner.clj:2913-2950`), approval requires a `done` review job, an
  `:approve` verdict and execution evidence (`:2950-2968`); failure refuses
  advancement at `:2991-3007`.
- **[E-16-X] exists:** every phase emits start/end/error telemetry
  (`full_loop_runner.clj:169-207`), and cohort runs append dispatch and build
  checkpoint cells (`:2262-2266,2762-2775,2969-2990`). Separately, the tick-side
  R16 implementation records typed attempt/results through TRACE
  (`src/futon2/aif/enact.clj:5-10,38-40`).

## 3. Cells directly blocking the merge criterion

The merge criterion is stated at `P-assured-process.md:12-16`: the census must
name, per node, the process assurance it must carry before fundamentals wiring
consumes it. The direct missing cells are:

- **R9 — checked:** its “No self-certification” claim needs a recorded second
  actor/checker that refuses self-closure. Today it is named-only.
- **R10 — commissioned and dispatched:** a scheduled entrypoint needs a
  commission identity and dispatch receipt tied to R10; both are absent.
- **R12 — returned and checked:** calibration work needs its returned artifact
  tied to the commission and checked before its result is admitted; both are
  absent.
- **R20 — checked and surfaced:** a tripwire needs a refusing result and a real
  discharge event. Checking is named-only and surfacing is absent.
- **TRACE — recorded and surfaced:** node-route recording exists, but it lacks
  a process-dispatch identity and surfacing discharge. The blocking cell is
  surfaced; the existing recorded cell is the substrate it can cite.
- **R16 — parked and surfaced:** commissioning, dispatch, return, independent
  checking and recording exist in the full-loop actuator. Its stop-line and
  recovery vocabulary does not record the lifecycle's `parked` transition,
  and no `notify/discharged-at` or bulletin item records surfacing.

This list states required assurance, not a build plan or a claim that the
generic Agency lifecycle is defective.
