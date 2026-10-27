# War Machine observational capability checklist — campaign v1

2026-10-27. Prepared by codex-11 at Joe's request. Adopted as the initial observational index for E-wm-criteria-completeness under Joe’s 2026-10-27 instruction. This file is not an installed runtime gate. No machine state changed.

## Purpose and rules

One stable account of capability across every run: accept an operator-aligned task, choose a justified action, execute it, observe its actual effect, and carry that result honestly into the next decision. Repairs map to these criteria; they do not automatically add new top-level criteria.

Keep the existing Q requirements, R-node wiring and Lean obligations intact beneath this index. Reading-era R1…R30 are defect IDs, not the architectural R-node list; do not conflate them. The mappings below are preliminary evidence pointers, not claims of equivalent predicates or complete formal coverage.

Each row gets status OBSERVED / VIOLATED / UNKNOWN / NOT-EXERCISED, an exact file:key (or retained live read with time), the actual value, expected condition fixed before execution, and any operator intervention. NOT-EXERCISED requires a branch explanation and never certifies that capability. Tests and proofs are supporting warrants, not observations of this run. No silent false/default for absent evidence.

Separate safe handling from useful delivery. A justified refusal can satisfy truthful reporting and containment while leaving execution not exercised; it cannot turn the run into a productive completion. A verified already-satisfied result is a legitimate resolution but must not become newly produced work or duplicate learning credit.

## Five capabilities / twenty criteria

| ID | Capability and observable condition | Observation boundary and retained evidence | Existing anchors / known counterexample |
|---|---|---|---|
| A1 | The issued run has an explicit operator objective, authorized scope and credit; admission happens once and its identity is acknowledged correctly. | Before admission and acknowledgement: request, click/run binding, budget before/after; one consumed credit per admitted issuance. | OPS-ordinary-run/click; R19 admitted run reported unaccepted. |
| A2 | The actual serving composition and resources match the declared run envelope. | Before start: loaded namespace evidence, explicitly listed unregistered/unknown components, heap measurement, seats, queue/stops, provider identity. Pin envelope and thresholds before run. | T10, OPS-serving-activation; R10 stale composition, R25 OOM. Clean registered-only drift is not full coverage. |
| A3 | Known blocking state is accounted for, and no incompatible work overlaps this run. | Before start: authority-aware effective injury set, service/worker/queue/stop state; unresolved defects explicitly dispositioned for this run. | Injury gate; R23 next run blocked. Empty injury set alone does not establish readiness. |
| A4 | The selected task has a retained, checkable definition of completion and before-state at the relevant revision. | Before author dispatch: exact token census, locators, repo/revision, durable baseline or explicit nonmission route. | Mission admission and baseline; R8 domain mismatch; locatorless wants. Missing authority is UNKNOWN, not false-before. |
| B1 | The candidate field represents the intended work; exclusions and unavailable readings are visible. | Selection: field census, task identities, per-target exclusions and reading status. | Q1/Q7/Q8; R1 unread targets unreachable; ticket eligibility findings. |
| B2 | Missing knowledge has an actual producer and the resulting evidence reaches selection. | Preparation/redecision: attempted/refused/pending readings, published file pins, decision input lineage; absence explicitly justified. | R1/R2/R28; no-readings-needed is invalid if preparation actually ran. |
| B3 | The chosen action follows the declared preferences and scoring law, with a replayable comparison. | Selection: chosen policy, constructed/scored/posterior stages, G terms, horizon, candidate rankings, tie draw and pins. | Q2–Q5/Q8–Q10; R3/R4/R12/R13/R15/R16. Individual Q verdicts remain visible. |
| B4 | The choice binds a prospective prediction in the same domain that will be observed. | Before execution: frozen prediction, input/model identity, native tokens and explicit observation plan. | R7/R8/R8a; a mission result cannot substitute for pattern execution. |
| C1 | Execution carries the same task/action/occurrence identity through construction and dispatch. | Selection → construction → author request: joins and exact contract/input bindings. | TRACE/E02; T1/T4; selected identity repairs. |
| C2 | Required baseline/checkpoint evidence is durable before the external effect, and continuation reuses it. | Pre-send sidecar/checkpoint; dispatch/job identity; resumed original origin, no new author send for an already retained completed job. | Baseline/deferred contracts, sidecar close repair. Does not claim crash-exactly-once external delivery. |
| C3 | The author result resolves to a real artifact or a verifiable already-satisfied claim at the stated revision. | Full retained author reply + card, Agency job, artifact-ref, commit/repo/proof checks. Capture reply before rolling retention. | T13; R24 path vs proof-ref; stale DONE/binding repairs. |
| C4 | Review judges the same artifact against the selected completion contract, and execution produces one explicit outcome. | Reviewer inputs/output, reviewed commit, build result or qualified no-build close. | T5, R9 callable-data repair; original-task success cannot be inferred from repair success. |
| D1 | After-observations measure the originally selected wants at the reviewed revision, without rediscovering a more convenient census. | Mission receipt and brief: token,before,after,locator,revision,provenance; unobserved is typed. | Selected-want domain split; close-retention fix. Label reviewed artifact, not deployed state. |
| D2 | Pattern observations and learning credit reflect executable checks and actual evidence, separately from mission completion. | Token outcome, pattern rows/comparison, learning write boundary. | R8/R8a; prose-only checks remain missing, not successful learning. |
| D3 | Close, terminal classification, predecessor carrier and injury ledger agree on what happened. | Run output/failure/terminal; run-stream close; authority-aware ledger delta and record pin. | T2/T3; R22/R23/R24. A successful close must not manufacture injury; an actual failure must not vanish. |
| D4 | The operator receives a complete, joinable account of the run, including unmet requirements. | Script exit, requirements artifact and per-Q verdict, brief, child binding, record pins, telemetry and intervention log. | TRACE/E02; R11/R19/R28; absent REQ is UNKNOWN, not conformance. |
| E1 | The run terminates or reaches an explicit manageable stop within the declared envelope. | Phase durations, heap/resource evidence, debugger stop/disposition, final job/service state. | T3/T9/T11; R25/R26/R27. Planned pauses differ from corrective intervention; failures retain their actual cause. |
| E2 | The next decision can consume this run's retained outcome and changed evidence. | Successor predecessor read, learning input/projection pins, changed reading frontier, Q6 applicability. | R17/R18/R22; declaring not-triggered needs its actual reason. |
| E3 | Repetition does useful work or explains exhaustion; it does not silently repeat the same ineffective action. | Across-run selected targets, measured wants resolved, already-satisfied resolutions, refusal recurrence, duplicate findings and resources. | T7/T8; T12 is currently a disabled chartered stub. A repeated zero-progress run needs a reason, not another credit by default. |
| E4 | Each iteration conserves authority, budget, state and accountability without manual repair between runs. | Across-run budget/store deltas, exact dispositions/replays, no orphan jobs/stops, bounded resource trend; restart evidence when exercised. | T1/T2/T6, medic duplicate/recovery controls. No proof of arbitrary crash recovery or perpetual operation. |

## Three decisions, not one green lamp

1. READY TO ATTEMPT: A1–A4 observed for the intended route; per-run expectations and unresolved limitations frozen. Selection-specific A4 is checked at the pre-author boundary, not falsely claimed before selection. Initial readiness is therefore readiness to enter selection; readiness to dispatch is a second boundary.
2. THIS RUN WORKED: relevant B–D rows observed, E1 observed, outcome explicitly separates changed-and-verified / already-satisfied-and-verified / refused / failed / aborted. Refused/failed/aborted can be safely handled but are not useful-task completion. An expected branch may leave capabilities not exercised.
3. LOOP DEMONSTRATED: consecutive runs under one declared envelope, no corrective code/state intervention between them, E2–E4 observed across boundaries; count and task/branch coverage declared in advance. One successful iteration cannot establish this. Any proposed minimum (for example three) is a demonstration plan, not a theorem or existing authorization.

## Freeze and replay method

First fill this sheet from existing first–fourth reading-era run evidence, including where the evidence is absent. Do not spend a click to rediscover a recorded defect. Freeze checklist revision, expected predicates, branch rules, environmental bounds and evidence extractors before a qualifying attempt. Replay extraction against retained records; preserve historical actual outcomes. A repaired replay or synthetic control belongs in a separate column and never rewrites a historical row to pass.

At each phase boundary observe the applicable rows. On a violation, retain the first divergence and explain downstream rows as blocked/not-exercised; do not treat cascaded symptoms as twenty independent failures. Repair the earliest violated producer/consumer seam, rerun the discriminating replay, and recheck adjacent joins. A live checkpoint is resumable only where current runtime semantics permit; otherwise a new authorized attempt is needed. No arbitrary replay of external side effects.

A new defect must either falsify a row, reveal missing observation coverage, or justify an explicit checklist revision. That keeps the capability through line stable. All rows passing means the declared capability was observed for that run and envelope, not that the observations are infallible or every future input/state/concurrency condition is covered.

## Existing coverage to reuse, and gaps

- src/futon2/aif/tripwire.clj: wire-registry T1–T13. T12 disabled :chartered-stub. Default action :record; observe! returns phase record unchanged and suppresses observer/action exceptions. Therefore silence is not a certificate that all checks ran or that enforcement occurred. Retain enabled wires, firing/completion records and observer failures as checklist coverage.
- scripts/wm_run_alert.py: numbers maps Q1–Q10 to task census, policy construction, scoring, preference/horizon, interpretation, predecessor, path absence, frontier, completion preference and arrangement sensitivity. Keep those individual verdicts rather than replacing them with this table.
- holes/labs/wm-contract/wire-requirements.edn: 27 requirements as wire instances/types including TRACE, E02, E05 and operator acceptance. This checklist is an observational index over them, not their repeal.
- DarkTower/WarMachine/ReadingEraClickPrediction.lean: predicted records give concrete before-run expectations; distinguish predicted failure from capability success.
- notes/wm-next/outer-selection/DEFECTS-reading-era.md and first/second/third/fourth-click-2026-10-26/report.md: historical counterexamples. Historical four-click sequence is not a passing cycle.
- notes/wm-next/offline-repair/authority/49-adoption/coordinator-completion.md: verified medic completion, not qualifying task run.

Not done in this draft: exhaustive architecture R-node crosswalk, extraction script, full historical row-by-row population, current live readiness measurement, or automated enforcement. These must not be implied by the existence of the checklist. No new runtime refusal or tripwire introduced.
