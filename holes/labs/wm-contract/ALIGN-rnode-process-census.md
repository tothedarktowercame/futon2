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

**R10 adjudication (2026-09-08, claude-1, from PA6z landing and the census
exit-4 claude-2 reported — the node-link search establishes NODE linkage,
not CELL linkage, and PA6z's three real `:node :R10` sites made that
coarseness visible for the first time):**

- **[E-10-C] commissioned exists:** `run-scheduled-dispatch!`
  (`futon3c:src/futon3c/social/coordination_ledger.clj:101`) refuses
  `:r10/invalid-commission` absent a commission identity. NOTE FOR PA15z:
  this boundary carries NO `:process/stage` tag, so a stage-tag-only
  crediting rule would uncredit a genuinely established cell — mechanism
  (b) corroborates, never decides alone.
- **[E-10-D] dispatched exists:** the evidence body writes `:node :R10
  :process/stage :dispatched` (`coordination_ledger.clj:121-122`), and the
  receipt must echo `:node` and `:commission/id` or
  `:r10/unlinked-dispatch-receipt` refuses (`:111`). Live pin: dispatch
  record `invoke-1788708049924-13300-38749afe`, commission
  `PA11z-library-annotator-exemplar`.
- **[A-10] parked, returned, checked, recorded, surfaced remain absent:**
  commissioning and dispatching a job parks, returns, checks, records and
  surfaces nothing; the only `:process/stage` in the declared scope is
  `:dispatched` (exactly one occurrence), and the three `:node :R10` hits
  (`coordination_ledger.clj:89,104,121`) serve the two credited cells
  only. Verified against the source this adjudication, not inherited.
- **:serves-cells declaration, for PA15z mechanism (a):**
  `{:sites ["futon3c:src/futon3c/social/coordination_ledger.clj:89"
            "futon3c:src/futon3c/social/coordination_ledger.clj:104"
            "futon3c:src/futon3c/social/coordination_ledger.clj:121"]
    :serves-cells [:commissioned :dispatched]}` — a node-link hit at these
  sites spills into no other cell. The census may keep exiting 4 until
  PA15z consumes this; that exit is the instrument telling the truth.

**R12/R20 adjudications (2026-09-08, claude-1, from claude-2's verified pin
list, bell invoke-1788882067825; every boundary re-read at source for
meaning, not only for pointer resolution):**

- **[E-12-R] returned exists / [E-12-K] checked exists:** `admit!`
  (`src/futon2/aif/calibration_admission.clj:6-44`, PA7z futon2 `62a6cb5f`)
  refuses `:r12/untied-return` unless the return carries `:node :R12` and
  the commission's id, and separately refuses `:r12/unchecked-return`
  unless a check names that exact return AND commission with verdict
  `:approve`; admission emits distinct `:returned` and `:checked` lifecycle
  records tied to the commission. The two typed refusals are the PA7z
  lossy-return bar, met as specified.
- **[E20-S] surfaced exists:** the R20 discharge record carries
  `:status :needs-joe :class :J` (`tripwire.clj:648-662`) and
  `bulletin.clj:190-208` (`tripwire-discharges`, PA8z) reads it into the
  waits-on-Joe section — the board's first `surfaced` cell served by
  running code rather than an operator turn, answering PA11z finding 3.
- **:serves-cells declarations, PA15z mechanism (a):**
  `{:sites ["src/futon2/aif/calibration_admission.clj:6-44"]
    :serves-cells [:returned :checked]}` (R12);
  `{:sites ["src/futon2/aif/tripwire.clj:639-662"]
    :serves-cells [:checked :surfaced]}` and
  `{:sites ["src/futon2/aif/bulletin.clj:190-208"]
    :serves-cells [:surfaced]}` (R20);
**Drift corrections and pin authorization (2026-09-08 evening, claude-1,
from claude-2's PA17z report, bell invoke-1788883503281; each token
re-read at HEAD):**

- **[E-T]:** the `:TRACE` route attachment is at
  `scripts/futon2/report/war_machine.clj:6790` (one line down from the
  prior re-read; construct unchanged — and it now carries the PA14z
  `:trace/reason {:kind :routing-rule ...}` field, so the write site names
  its routing rule). My own `:serves-cells` entry above had the WRONG PATH
  (`src/futon2/aif/` for a file living at `scripts/futon2/report/`) —
  corrected in place, defect mine.
- **[E-16-C]/[E-16-K] runner addresses shifted under PA14z's edit,
  constructs unchanged:** the author-dispatch construct verified at the
  `full_loop_runner.clj:2820` region, the approve-verdict construct at
  `:3028`. The census-ledger's pins `:2788-2818` and `:3007-3025` WERE
  NEVER THIS DOCUMENT'S CITATIONS — work seats re-pointed them
  independently so their rows' gates would pass, a slower quieter
  [E-T-S]-class divergence, surfaced by claude-2 rather than regularised.
  This entry is the authorized citation for the two constructs at HEAD;
  the ledger re-lifts from here.
- **Address-grade citation is ending, not being maintained:** PA17z pins
  by CONTENT (span hash; moved-but-identical satisfies its reading,
  changed refuses and routes here for re-adjudication). ALIGN adopts the
  same grade when PA17z lands; until then this document does not chase
  line arithmetic — six drift incidents, three informative, three noise,
  is the measured case for the change. A tolerance window was considered
  and refused for the reason PA17z's :note-on-what-not-to-do states: a
  window makes the instrument quieter in both directions.

**[E-T-S] TRACE surfaced adjudication (2026-09-08, claude-1, from claude-2's
self-certification finding, bell invoke-1788882522014):**

- **The governance fact first, because it must not be laundered:** the
  census-ledger carried a TRACE-surfaced credit from PA9z's work seat
  (futon2 `dfe4dcfd`) until this adjudication — written straight into the
  instrument's data on a reading this census of record never made, while
  the ledger's own header says its cells ARE this document's cells
  transcribed. For that interval the instrument certified itself: R9's
  problem one layer up, inside the thing built to detect R9's problem, the
  second instance of the shape beside PA5z's refusal. The credit below is
  examined fresh at source, not ratified.
- **[E-T-S] surfaced exists, on the post-PA13z channel:**
  `trace-discharges` (`src/futon2/aif/bulletin.clj:212-236`, moved from
  `:344` by PA13z `d17f1088` — drift instance three of that signature,
  caught by the harness) reads persisted `:TRACE` route hops;
  `untriaged-traces` (`:278-287`) subtracts the append-only discharge
  ledger (`append-trace-review!` `:265`, refuses duplicate dispositions)
  and delivers count + oldest-date into the bulletin as its own field,
  split from the decision sheet. QUALIFICATION: surfacing is
  AGGREGATE-grain — per-record surfacing carries no answerable question
  until PA14z's reason field lands at the write site; what running code
  surfaces today is the queue's existence, size and age, which is the
  grain the triage note specified.
- **:serves-cells (TRACE):**
  `{:sites ["scripts/futon2/report/war_machine.clj:6790"
            "src/futon2/aif/trace.clj:723-745"]
    :serves-cells [:recorded]}` and
  `{:sites ["src/futon2/aif/bulletin.clj:212-287"]
    :serves-cells [:surfaced]}`.

  `{:sites ["src/futon2/aif/full_loop_runner.clj:141-182"
            "src/futon2/aif/full_loop_runner.clj:2362-2379"
            "src/futon2/aif/full_loop_runner.clj:2430"]
    :serves-cells [:parked :surfaced]}` (R16, joining the six cells
  already credited under E-16-C/D/R/K/X).

## 2. Matrix

| control-stages node | commissioned | dispatched | parked | returned | checked | recorded | surfaced |
|---|---|---|---|---|---|---|---|
| R9 — No self-certification | absent [A] | absent [A] | absent [A] | absent [A] | named-only [N9] | absent [A] | absent [A] |
| R10 — Scheduled entrypoint | exists [E-10-C] | exists [E-10-D] | absent [A-10] | absent [A-10] | absent [A-10] | absent [A-10] | absent [A-10] |
| R12 — Two-layer calibration | absent [A] | absent [A] | absent [A] | exists [E-12-R] | exists [E-12-K] | absent [A] | absent [A] |
| R20 — Interoceptive tripwires | absent [A] | absent [A] | absent [A] | absent [A] | exists [E20-C] | absent [A] | exists [E20-S] |
| TRACE — WM trace store | absent [A] | absent [A] | absent [A] | absent [A] | absent [A] | exists [E-T] | exists [E-T-S] |
| R16 — Grounded actuation | exists [E-16-C] | exists [E-16-D] | exists [E-16-P] | exists [E-16-R] | exists [E-16-K] | exists [E-16-X] | exists [E-16-S] |

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
  **PROMOTED to [E20-C], 2026-09-08 — the board's first :named-only →
  :exists transition, the transition this track exists to cause.** What
  promoted it: a node-linked REFUSING boundary now exists — `check!`
  (`src/futon2/aif/tripwire.clj:639-658`, PA8z futon2 `8a8f65c4`) evaluates
  the wire and returns `:tripwire/check :refused` with witnesses on
  violation, an explicit `:passed` on clear, and treats persistence failure
  as grounds to refuse the call. Named became checking the day the name
  gained a boundary that can say no. Original [N20] text kept above as
  history, per the [E-T] convention.
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
  **SUPERSEDED for parked and surfaced, 2026-09-08 (PA10z, futon2
  `ad51aa0d` + repair `c7fadbba`): the recorded absence was deliberately
  falsified by construction — the track's intended outcome.** [E-16-P]
  parked: `park-r16-stop-line!` (`full_loop_runner.clj:141-182`, called at
  `:2362-2365`) refuses closure without a durable repair identity
  (`:r16-park-repair-id-missing`, `:failure-stage :parked`) and registers
  the park with the repair obligation as the awaited dependency. [E-16-S]
  surfaced: the runner attaches `:lifecycle/discharge {:node :R16 :stage
  :surfaced ...}` (`:2366-2379`) and queues it through
  `brief/queue-item!` (`:2430`). Seam note, both cells: the boundaries take
  injectable fns (`:r16-park-fn`, `:queue-fn`) — the default path is the
  credited one; the injection point is where a test can silence it, which
  the census's scope statement should treat as its instrument edge. The
  original [A16] search text stands above as what was true when it ran.
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
- **R10 — commissioned and dispatched:** ESTABLISHED 2026-09-08 (PA6z; see
  [E-10-C]/[E-10-D] adjudication above). The node's remaining missing cells
  are the five ruled absent under [A-10].
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
