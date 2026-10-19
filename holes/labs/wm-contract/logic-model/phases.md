# WM click runner: phase order (static reading)

P17 phase 0a helper increment: verified diagnosis receipt can feed a prospective
acceptance-specification request. Receipt binds diagnosis pin, injury and source;
it remains proposed/unreviewed/not-run. Runtime preparation does not yet invoke
this helper, and no returned command is executed. Approval/admission remains open.

P16b update: phase 0a runtime preparation now invokes the injectable diagnosis
producer and carries its observation into readiness/routing. Before coordination,
produced evidence must match work-order injury ID, causal source pin and diagnosis
pin, including resumed transactions. Absent ledger is not-required; unreadable
ledger is unavailable. Transport/P17 and serving verification remain outstanding.

P16 phase 0a increment: a standalone diagnosis producer can turn pinned injury
source into an injected request and content-addressed persisted response.
The serving phase does NOT invoke/thread it yet. No runtime continuation,
acceptance-input production or repair execution is established by this change.

Reviewed P14 update (worker `1077757b6` + `732b2f0c4`): phases 32→33–35
retain independent post-record publication observations in a linked sidecar.
The pinned run bytes remain unchanged. Without readable linkage all sinks are
explicitly not attempted; otherwise sink exceptions are isolated and later
sinks proceed. Legacy runner result fields remain available. No serving probe
or live click has verified this revised integration.

Reviewed P11 update (`5874be666` + `89e689716`): observed selected judgement is
available before validation/construction. Early close can publish it with explicit
early-terminal phase/stage/outcome; normal construction and close share one
per-run publication-attempt state. Failed attempts are recorded, not retried at
close or allowed to replace original failure. This adds observation to phases
15/20, not a claim that construction/dispatch/learning completed.

futon2 commit read: `f76041cc7` (master). Date: 2026-10-19.
Scope: order of phases only. Every file:line below was read this session. Paths are under
`/home/joe/code/futon2/` (`R` = `src/futon2/aif/full_loop_runner.clj`, `RT` = `src/futon2/aif/full_loop_runtime.clj`,
`WM` = `scripts/futon2/report/war_machine.clj`, `CD` = `src/futon2/aif/wm/cascade_decision.clj`,
`SEL` = `src/futon2/aif/meta_live_outer_selector.clj`, `SH` = `src/futon2/aif/meta_self_heal_production.clj`).

## How phases are recorded

- Runner phases are `run-phase!` calls (R:379). Each emits a `:start` and an `:end` phase event
  (R:389, R:406) to the phase log file and the `:phase-events` atom (R:370-375). On a throw, with the
  debugger attached and the failure stoppable (R:416), it waits for a restart (R:430) and acts on `:retry` (R:441) / `:continue` or `:use-value` (R:456) / `:abort` (R:465); otherwise it emits the error event and rethrows (R:417-428).
- The selection sub-phases (`:strategic-scans` ... `:receipt-serialization`) are NOT `run-phase!` phases. They are
  `selection-timing` checkpoints inside the report code. `checkpoint!` takes the NEXT name (selection_timing.clj:30-35, docstring
  "Close the current subphase and begin NEXT"), so the name on a checkpoint line labels the work that follows it.
- Checkpoints `:selection :construction :dispatch :build :adjudication` (R:148 `required-checkpoints`) are cohort evidence
  files written by `checkpoint!` (R:5386), not phases. `close-core!` (R:5430) fills any missing one with a `not-reached-*` sorry cell (R:5434).

## Phase table (execution order, ordinary click)

"Run record" = the record written by `persist-run-record!` (R:1010). Inputs/outputs listed are those visible at the call site; the
callee bodies were not traced (see Uncertain).

| order | phase | file:line (entered) | function | inputs it reads | outputs it writes | can it end the run? | notes |
|---|---|---|---|---|---|---|---|
| 0a | (before runner) injury/readiness arbitration | RT:413-420 | `run-opportunity!` (runtime) -> `meta-self-heal/production-readiness`, `route!` | `:meta-injury-ledger-path`, `:meta-repair-transaction-path`, `:meta-repair-retry`; ledger file | returns a map directly when it refuses or routes | yes: returns `:status :refused` (`:meta-self-heal/ledger-unreadable`, `:meta-self-heal/production-config-required`, SH:77-107); `route!` (SH:133) returns a map and the ordinary click never runs | not a `run-phase!`. See Branches. |
| 0b | (before runner) composition root | RT:350-399 (`run-ordinary-opportunity!`), RT:226 (`production-defaults`) | `production-defaults` | `:meta-injury-authority`/`injury-store/load-authority` (RT:231), `ordinary-click-budget/availability` (RT:237), `meta-field/observe` (RT:241), `:outer-task-selection-fn` (RT:249), `:outer-policy-evidence-provider`, `-path`, else `policy-provider-runtime/production-provider` (RT:250-253) | returns `:judge-fn` etc. map bound to `runner/*runtime-defaults*` (RT:398); `:meta-field-observation`, `:meta-field-source-pin` added to opts (RT:245-248) | exception only (not run-phase wrapped) | The provider wraps the outer selector at RT:254-257; the first outer receipt is pinned for the click by `retained-outer-selector` (RT:205-224, used at RT:272-273). |
| 0c | (before core) q6 predecessor, seat, drift, discharge catch-up | R:7858, R:7879, R:7882, R:7883 | `run-opportunity!` (runner) | run-record dir, run-id | `previous-run/start!` appends start; `refuse-on-runner-source-drift!` | yes: drift refusal (R:664); exception becomes `:initialization-failed`-style finding in the catch at R:7893 ff | not `run-phase!`. Runs before any attempt exists. |
| 1 | `:opportunity` start | R:5262 | `run-opportunity-core!` | `trigger`, `opportunity-id` | phase event | no | `:opportunity` end is emitted in `close-core!` (R:6123). |
| 2 | `:agent-readiness` (author) | R:5279 | `agent-readiness-for-author!` (R:1416) | `opts`, `author` | `roster-result` (R:5277, errors captured, not thrown here) | deferred: error thrown later at R:6250 (`:agent-unavailable`, `:agent-readiness-failed`) | Wrapped in `try`; failure surfaces only after code-state and cohort start. |
| 3 | `:code-state` | R:5286 | `stack-code-state` (R:1341) or `:code-state-fn` | none visible | `code-state-result` | deferred: thrown at R:6257 (`:incomplete`, `:code-state-failed`) | Same deferral. Cohort start (`cohort/start-attempt!`, near R:5323) and `time-cell` happen between here and phase 4, outside any phase. |
| 4 | `:ration-admission` | R:6266 | `:readiness-admitted-fn` | `opts` | none | exception from `admit!` (kind not read) | First call inside the main `try`. |
| 5 | `:substrate-preflight` | R:6269 | `substrate-readiness!` (R:1512) | `opts` | `readiness-event` fields | exception | |
| 6 | `:preference-refresh` | R:6275 | `refresh-record` of `:refresh-fn` | `:refresh-fn` (runtime default `mission-registry/refresh-mission-substrate!` + `cv/maybe-refresh!`, RT:302 ff) | `:preference-refresh/state` atom (R:6280); event `{:refresh r}` | yes if refresh not `:ok` and `:refresh-required?` (true in production, RT:297): `:decision-input-freshness-unestablished` (R:6286) | The phase itself reports `:ok` either way. |
| 7 | `:stop-line-memory` | R:6292 | `repair/open-obligations` | none visible | `open-stop-lines` (local) | exception | Result used later for `stop-line` (R:6683) and `validation-lines`. |
| 8 | `:selection` (outer + inner decision) | R:6374 | `selection-judge` -> `wm/generate-war-machine` (RT:183-203; WM:7304) | `window-days`, opts keys listed at RT:187-199 | `judgement0-base`; `:scan-report/state` (R:6388); `effective-configuration` atom | yes: typed judge/gate refusal -> `selection-refusal!` (defined R:6312, called R:6413) sets `pending-selection`, throws abstention; `selection-terminal-condition` (defn R:2301, used R:6406) builds the debugger condition only when attached; `require-loop-node-exercise?` failure (R:6385, true in production RT:301) | Sub-phases below. The two `:no-*` early stops of 2026-10-19 both fall inside this phase's thunk. |
| 8.1 | `:strategic-scans` | WM:7315-7318 | `generate-war-machine` | evidence store, missions, many scans (WM:7316 ff) | `scan-data` | exception | `selection-timing/begin!`. |
| 8.2 | `:strategic-judgement` | WM:6406-6408 | `judge` | `scan-data`, `judge-opts` | `loaded-configuration`... | exception | First checkpoint in `judge`. Called at WM:7390 after scans. |
| 8.3 | `:registry-loading` | WM:6619-6621 | `judge` | `mission-registry/load-missions`, `load-tickets`, `load-excursions` (WM:6622-6624) | `loaded-missions/-tickets/-excursions` | exception | |
| 8.4 | `:meta-field-ranking` | WM:6626-6628 | `judge` -> `select-outer-task` (WM:1540, called WM:6654) | `outer-task-population` (WM:6633), `:outer-task-seed`, `:meta-field-observation`, `:meta-field-source-pin` (WM:1551-1555) | `outer-task-selection` receipt | yes: throws `:meta-outer-selector-unavailable` (WM:1560), `:meta-outer-receipt-invalid` (WM:1565, 1570); a nil `[:chosen :id]` is carried forward as a refusal at WM:6765, not thrown | The outer selector runs here, BEFORE any declaration or reading is loaded (comment WM:6630-6632). Detail in "Order inside the outer selector". |
| 8.5 | `:declaration-interpretation-loading` | WM:6657-6659 | `judge` | `cascade-sources/load-declared`, `mission-hole-wants/merge-*` (WM:6663 ff), `selected-task-id` | `declared-sources`, `cascade-sources` | exception | Entered after the outer choice (`selected-task-id`, WM:6655). |
| 8.6 | `:inner-cascade-assembly` | WM:6744-6746 | `judge` -> `assemble-cascade-problems-with-published` (WM:6748) | `:machine-interpretations-dir`, `cascade-assembly-input` | `raw-cascade-assembled`; outer refusal appended to `:refusals` (WM:6765); `cascade-proposals/record-supply` (WM:6772) | exception | |
| 8.7 | `:g-admission` | WM:6776-6778 | `judge` -> `cd/select-and-record-cascade!` (WM:6792) = `cascade-decision` (CD:1322) | `outer-task-selection` `[:policy :selected-outer-cascade-policy]` (WM:6798), `:outer-policy-selection-claimed?` (WM:6801), `cascade-sources`, prior trace, enactment fold | `cascade-result` `{:decision ...}` | typed abstention (not an exception) in all branches of CD:1322 ff; a META algorithm choice skips it (WM:6779-6790, `meta-algorithm-result` WM:1590) | Order inside `cascade-decision` below. |
| 8.8 | `:completeness-audit-projection` | WM:6846-6848 | `judge` | `cascade-result` | `wm-decision`, strategic-habit state, heads, inventory (WM:6849 ff) | exception | |
| 8.9 | `:receipt-serialization` | WM:7039-7041 | `judge` | terminal projections (immediately before WM:7039) | `result0` | exception | Followed by `selection-timing/finish!` (WM:7083-7085) which checkpoints `:receipt-complete`. |
| 9 | `:selected-target-reading` | R:6421 | `selected-target-reading-step` (R:2132) | `judgement0-base`, `:selected-target-reading-fn` (only when `:flight` nil), `selected-target-reading/selected-need` | `judgement0-reading`, `selected-target-reading-record` | no direct end: repair record `:refused` is kept; redecision failure throws `:selected-target-reading-redecision-failed` (R:6448) | Runs after phase 8 has finished, so readings absent during phase 8 are produced only here, then phase 8's `selection-judge` is called again (below). Skipped (pass-through) when no need. |
| 9.1 | `:selected-target-reading-redecision` | R:6429 | `selection-judge` again | `window-days` | `judgement`, `effective-configuration` | terminal condition only when attached (R:6441) | Re-runs the whole `generate-war-machine` but `retained-outer-selector` returns the first outer receipt (RT:213-214). Exception: it refetches nothing from the outer selector unless the first receipt was an incomplete-menu refusal (RT:215-223). |
| 10 | `:occurrence-square-authoring` | R:6457 | `occurrence-square-authoring-step` (R:2159) | `judgement0-reading`, `:occurrence-square-authoring-fn` (flight nil), `square-authoring/selected-need` | `judgement0-squares`, `occurrence-square-authoring-record` | yes: record not `:published` -> throws "Occurrence-square authoring failed before G" with `:failure-stage :occurrence-square-authoring` (R:6492) | |
| 10.1 | `:occurrence-square-redecision` | R:6465 | `selection-judge` again | `window-days` | `judgement` | wraps failure as `:occurrence-square-redecision-failed` (R:6482) | |
| 11 | (no phase) interpretation ask | R:6505-6535 | `interpretation-ask-fn` | refusals of kind `:no-query-time-slice` / `:no-admitted-interpretation` (R:1966-1976) | `interpretation-ask-record` | ask exceptions are caught -> `:ask-threw` (R:6531) | At most once; never for a flight. |
| 11.1 | `:selection-redecision` | R:6549 | `selection-judge` again | | `judgement0` | typed refusal -> `selection-refusal!`; other -> `:selection-redecision-failed` (R:6341 `retain-redecision-failure!`, R:6572-6575) | Only if the ask `:published` (R:6537). |
| 12 | `:selected-reading` | R:6599 (name at R:6600) | `selected-reading-phase` (R:2036) | `judgement0`, `selection-clock`, `selection-completed-at` | `selected-reading-record` | exception | Skipped with `:status :not-applicable` when the selected action is `:run-algorithm` (R:6592-6595). |
| 13 | `:selection-world` | R:486 (called R:6605) | `capture-selection-world!` (R:481) | decision, roster, ask/reading record, outer selection | `world-at-selection` | exception | |
| 14 | `:cascade-execution` | R:6638 (name R:6639) | `wm-cascade-decision/execute-selected-cascade!` (CD:1512) | `ordinary-entry` | `cascade-execution0` | held (not thrown) with `:kind :wm/open-cascade-execution-error` (tested R:6648); later `entry` becomes nil unless `:cascade-ready` (R:6670-6671) and the click throws "abstained or selected no addressable action" (R:6827) | Only when `[:action :precedence :co-apply]` is a map (R:6618, R:6637). |
| 14.1 | `:cascade-resume` | R:6653 | `:open-cascade-resume-fn` | `cascade-execution0` | `cascade-execution` | exception | Only if held and a resume fn exists (R:6648). |
| 15 | selection checkpoint written; no-entry stop | R:6827 (throw), R:6994 (`persist-selection!`) | (in core `let`) | `entry`, `judgement` | `checkpoints :selection` via `persist-selection!` (defined R:5408) | yes: "abstained or selected no addressable action" (R:6827) | The first `persist-selection!` call with a trace path is at R:6994, after construction; the pending cell is set earlier (see Uncertain). |
| 16 | `:agent-readiness` (reviewer wake) | R:6840 (call R:6841) | `wake-restored-agent!` | roster, reviewer | `reviewer-roster` | yes: `:agent-unavailable` for uncast seat (R:6832) or reviewer busy/same as author (R:6846) | Only if `restored?`. The availability throw is not in a phase. |
| 17 | (policy discrimination guard) | R:6859 | inline `when` | `discrimination` | none | yes: `:policy-nondiscrimination` (R:6859-6860) | |
| 18 | action occurrence minted | R:6865 | `mint-action-occurrence-once!` (R:4458) | selected action | `action-occurrence` atom, `d-task-context` atom (R:6876) | exception | not a phase. |
| 19 | `:interpretation` (receipt mode only) | R:6883 (mode test R:6879) | `interpretation-job/run!` | `:interpretation-mode` = `:receipt` | `interpretation` | exception | Only when `(:interpretation-mode opts)` is `:receipt`. |
| 20 | `:construction` | R:6905 | `mission-for-decision`, `construct-for-decision` (or `:construct-fn`) | `entry`, `target`, `:repair-root` | `mission`, `construction`; then `construction-wiring-result` | yes: `:construction-failed` (no construction R:6924; wiring invalid R:6931; wiring refused R:6938) | Then `publish-selection-trace!` (R:6948, skipped for repair actions), `persist-selection!` (R:6994), `checkpoint! :construction` (R:6995). |
| 21 | (historical-repair branch) | R:7000 | inline | `stop-line`, `historical-admission` | cohort `:dispatch`/`:build`/`:adjudication` sorry cells (R:7040, 7043, 7046) | always ends: throws the completion token (R:7060), closed as `:historical-verification-awaiting-validation` (R:7772) | Skips author/review entirely. See Branches. |
| 22 | `:author-dispatch` | R:7199 | `dispatch!` or deferred job | `author-prompt-text`; d-task capture (R:7186) | `author-response`; `checkpoint! :dispatch` (R:7211) | yes: unbound D task -> `(:kind @d-task-dispatch)` (R:7191); dispatch exception | Deferred-completion jobs replace dispatch with a recorded job (R:7199-7204 region). |
| 23 | `:author-wait` | R:7228 | `poll-job!` | `author-job-id` | `initial-author-job` | yes: non-"done" -> `:build-failed` (R:7280); cancelled/refused checks (R:7278, 7285) | Optional retry pair `:author-retry-dispatch` R:7256 / `:author-retry-wait` R:7264. |
| 24 | `:build-resolution` | R:7320 | `resolve-target-build` | `author-repo`, `commit` | `build` -> `repo`, `files` | yes: `:artifact-binding-mismatch` (R:7328-7331), "no verifiable commit" `:build-failed` (R:7337) | |
| 24.1 | build-cure loop | R:3946 (called R:7344) | `build-cure-loop` | `:build-cure-retries` (default 0; read in `build-cure-loop`, R:3946) | `:build-cure-dispatch` R:3987, `:build-cure-wait` R:3994 | validation failure after retries | |
| 25 | `:reviewer-dispatch` | R:7366 (name R:7370; code name, log name `:review-dispatch` not found) | `dispatch!` | `reviewer-prompt-text` | `review-response` | exception | |
| 26 | `:reviewer-wait` | R:7384 | `poll-job!` | `review-response :job-id` | `initial-review-job` | exception; `throw-if-cancelled!` R:7415 | Retry pair `:reviewer-retry-dispatch` R:7405 / `:reviewer-retry-wait` R:7411. |
| 27 | review gate, revision round | R:7419 | `review-execution-gate`, `run-revision-round` (R:3607) | `files`, `review-job` | `:revision-dispatch` R:3690, `:revision-wait` R:3702, `:revision-build` R:3717, `:re-review-dispatch` R:3768, `:re-review-wait` R:3776, retry pair R:3782/R:3789 | yes via later `approved?` check | Inline; no separate `:review` phase seen. |
| 28 | `checkpoint! :build` | R:7504 | inline | `files`, commits | cohort build cell | no | |
| 29 | approval check | R:7574 (`when-not approved?`) | inline | `approved?`, `review-gate`, falsifier receipt | | yes: "Independent review did not approve" `:build-failed` with `:review-execution-evidence-missing` / `:reviewer-falsifier-failed` (R:7574-7641) | Optional `:standing-completion-dispatch` R:7563, `:standing-completion-wait` R:7569 before it. |
| 30 | `:grounding` | R:7647 | `ground-commit!` | `attempt-id target author reviewer repo commit files construction review-job` | `witness`; `checkpoint! :adjudication` (R:7655) | exception | |
| 31 | close | R:7671 -> `close!` (R:6132) -> `close-core!` (R:5430) | `close!` | outcome, data | cohort close, run result | not a phase; see below | Failure path also reaches `close!` at R:7805 via the outer `catch` (R:7693). |
| 31.1 | `:delivery-qa` | R:5605 | `:delivery-qa-fn` | `brief-item` | `delivery-qa-ref` | yes: `:delivery-qa-gate-failed`, rethrown past the runner boundary (R:7894-7899, catch at R:7893) | Skipped when no `:commit` (R:5601). |
| 31.2 | `:close-evidence-manifest` | R:5917 | `checkpoint-evidence-manifest` | `@checkpoint-events` etc. | `manifest` | yes: evidence-manifest refusals escape (R:7894-7899) | Only `cohort?` and an action occurrence. |
| 31.3 | `:opportunity` end | R:6123 | `close-core!` | | phase event with `:outcome` | | |
| 32 | persist run record | R:8003 | `persist-run-record!` (R:1010) | `final-result`, `:phase-events/state` deref (R:1021) | run record file; `:run-record-status` | | Not a `run-phase!`. |
| 33 | `:meta-injury-deposit` | R:8006 | `meta-injury-store/deposit!` | `(:run-record persisted)` | injury store head | exception | Only when `:run-record-status` is `:present`. |
| 34 | `:meta-terminal-injury-ingest` | R:8017 | `meta-injury-store/ingest-record-file!` | `:meta-injury-ledger-path` | ledger entry | exception | Same guard. |
| 35 | `:outer-learning-publication` | R:8030 | `outer-learning-manifest/record!` | `:outer-learning-manifest-path` | manifest | exception | Only when the manifest path is set (default set in RT:369-370). |

## Order inside the outer selector (phase 8.4)

Called as `(selector {:tasks :seed :meta-field-observation :meta-field-source-pin})` (WM:1551), wrapped twice:
`retained-outer-selector` adds `:policy-field-config` (RT:215-216), then the provider wrapper adds `:outer-policy-evidence-provider` (RT:254-257).
`meta-live/selector` (SEL:784) -> `select-live` (SEL:519). `select-live` is one `let`, evaluated top to bottom:

1. `fetch-snapshot` and `meta/validate-snapshot` (SEL:532-533).
2. Standing filter, then on-map filter, `attach-work-attribution`, ownership filter (SEL:534-591).
3. `injury?` = any task with `:kind :algorithm` (SEL:592); tasks of that kind come from `meta-field-observation` rows in `selector` (SEL:792).
4. Ordinary branch only: `policy-field` = `policy-field-fn on-map policy-field-config` (SEL:602); needs verified snapshot and a map config.
5. `sealed-menu` = `policy-menu-fn` (SEL:612).
6. `provided` = `evidence-provider/resolve-provider provider sealed-menu` (SEL:614), only when the menu is `:sealed`.
7. `policy-evidence` = `policy-evidence/join` (SEL:617), only when `provided`.
8. `policy-objective` = `policy-objective/evaluate` (SEL:620), only when evidence `:computed`.
9. `selected-carrier` = `selected-policy/seal` (SEL:622), then `carrier-defect` (SEL:627).
10. `ordinary-result` refusal cascade in the order: snapshot unverified, no `policy-field-config`, menu not sealed, provider not provided, evidence not computed, objective, abstain, carrier defect (SEL:631-644; `selection` cond SEL:645-653).
11. Receipt assembly with `:policy {... :branch :ordinary-cascade-policy ...}` (SEL:703); or `:verified-machine-injury-algorithm` (SEL:698).

So each step's input is the previous step's output; a missing earlier output yields the nil-gated `when` and a typed refusal at step 10 naming the first absent stage. `resolve-provider` returns `:outer-policy-evidence-provider-required` only when the provider is neither map nor fn (outer_policy_evidence_provider.clj:40-43).

## Order inside `cascade-decision` (phase 8.7, CD:1322)

1. `(:selected-outer-cascade-policy opts)` present -> `selected-outer-policy/decision` of that carrier (CD:1332-1340; `selected-outer-policy/decision` at CD:1333). No field is rebuilt.
2. else `(:outer-policy-selection-claimed? opts)` -> abstain `:claimed-policy-selection-without-carrier` (CD:1343-1347).
3. else (legacy/non-claimed): `target-policy-field`, `observation-label-view`, `ticket-queue/validate!`, `reading-family-decision` (CD:1349-1355; call at CD:1355). Inside it (CD:1190 ff) the cond order is: field not `:formed`, empty families, no computed family, declared-beta refusal, then `family-selection/select-over-families`, then `controller-authority/authorize` and `decision-gate/emit!`.

## Branches

- Injury vs ordinary, level 1 (runtime): `meta-self-heal/production-readiness`, then `(or (meta-self-heal/route! opts) (run-ordinary-opportunity! opts))` (RT:413-420). `route!` returns nil when the injury ledger file is absent or has no active injuries (SH:133-139) so the ordinary click proceeds; a map consumes the opportunity. Decider: durable ledger at `:meta-injury-ledger-path` and `:meta-repair-retry` (RT:412, 418).
- Injury vs ordinary, level 2 (selection): `injury?` in `select-live` is true iff the task list contains `:kind :algorithm` rows (SEL:592), which `selector` adds from a verified `:meta-field-observation` (SEL:792). Injury branch: `meta/select` over `algorithm-candidates` (SEL:593, `meta/select` SEL:649) and no policy field is built (SEL:602 gates on `(not injury?)`). If chosen, `WM:6779-6790` takes `meta-algorithm-result` instead of `cascade-decision`, and the runner skips `:selected-reading` (R:6592) and `:cascade-execution` (R:6637 needs `:co-apply` map).
- Unverified meta field: `selector` returns a refused receipt without calling `select-live` (SEL:798-801).
- Debugger restarts, per `run-phase!`: attached AND `stoppable-failure?` (R:430) -> `await-restart!`; `:retry` re-runs the thunk (R:441), `:continue`/`:use-value` returns the supplied value as success (R:456), `:abort` rethrows (R:465). Detached: rethrow immediately. Also `[:after phase]` breakpoints and `result->debugger-condition` conditions (R:394-404) turn a normal result into a stop. `:selection` supplies `selection-terminal-condition` as that condition (R:6406) so a no-entry result becomes restartable when attached.
- Interpretation ask second pass: decided at R:6505 (not a flight, and `interpretation-needed-refusals` non-empty on `judgement0-squares`); ask outcome at R:6508-6531; `:selection-redecision` runs only if `(:published record)` (R:6537). The second `selection-judge` pass reuses the first outer receipt (RT:213-214) except for the incomplete-menu refusal reasons (RT:217-223).
- Reading and square passes: `:selected-target-reading` runs if `selected-need` is true and the fn is non-nil (R:2132), then calls `:selected-target-reading-redecision` once; `:occurrence-square-authoring` likewise (R:2159, R:6465).
- Flight vs click: `(:flight opts)` non-nil disables target-reading, square-authoring and ask (`(nil? (:flight opts))` at R:6399, 6401, 6403, 6426, 6462, 6508) and narrows the outer population to the flight target (WM:6644).
- Historical repair action (`:revalidate-historical-repair`): the `(when historical-action? ...)` block (R:7000) ends the click without an author or reviewer.
- Deferred completion: a `stop-line` with a snapshot replaces author dispatch (R:7199-7204) and may replace reviewer dispatch (R:7366-7370).
- Retries: author (R:7256, R:7264), reviewer (R:7405, R:7411), re-review (R:3782-3789), build-cure (R:3987-3994).
- Failure funnel: any throw in the main `try` reaches `catch Throwable` (R:7693); `close!` then runs `close-core!` (R:7671 / R:7805). A throw before the main `try` (phases 0c, 1, up to cohort start) reaches the `run-opportunity!` catch (R:7893 ff) and becomes an initialization finding.

## Observations that bear on wiring

- Phases 2 and 3 are run before the main `try`; their failures are held and thrown after cohort start (R:6250, R:6257). Phase 4 onward are inside it.
- Phase 8 (`:selection`) completes before phase 9 (`:selected-target-reading`), and the production target-reading and square-authoring fns are installed in `runner-opts0` (RT:382-392) and used only on that second pass. If the first outer pass needs a reading the menu lacks, it ends in the incomplete-menu refusal reasons that `retained-outer-selector` declines to cache (RT:217-223), so that is the branch designed to let the second pass choose again.
- The reading cannot be consumed before phase 9 in the same click, and the evidence provider is resolved in step 6 of the outer selector, before phase 9 can publish anything (SEL:614).
- `select-live` receives `:outer-policy-evidence-provider` only through the RT:254-257 wrapper; `select-outer-task` itself does not pass it (WM:1551-1555).
- The three post-persist phases (33-35) run after the run record has been written (R:8003 before R:8006), so their events cannot appear in that record's phase-event list unless they are appended to the same atom (see Uncertain).

## Uncertain (no verified file:line, or inferred)

- Phase 15 cell: the exact line where the pending selection cell is first set after the `:selection` phases was read inside the `selection-cell` block (just above R:6827) but not individually numbered; `persist-selection!` is defined at R:5408 and its first call with a trace path is R:6994 (after `:construction`). R:6827 is the no-entry throw.
- Log phase names `:review-dispatch`, `:review-wait`, `:review`, `:build`, `:close` from the task brief: no `run-phase!` with those keywords exists. Closest in code: `:reviewer-dispatch` (R:7370), `:reviewer-wait` (R:7384), `:build-resolution` (R:7320), `checkpoint! :build` (R:7504), and `close!` (R:6132) with `:close-evidence-manifest` (R:5917). `:review` as a phase: not found.
- Whether post-persist phase events (33-35) enter the run record: `emit-phase!` conj's to `(:phase-events opts)` (R:373); the raw-opts built at R:7871 carries `:phase-events/state`, not `:phase-events`, so they probably reach only the phase log file; `config` was not read. `persist-run-record!` derefs `:phase-events/state` at R:1021, before they run.
- Bodies not read: `agent-readiness-for-author!` (R:1416), `substrate-readiness!` (R:1512), `selected-reading-phase` (R:2036), `selection-world/capture`, `execute-selected-cascade!` (CD:1512), `ground-commit!`, `persist-run-record!` (R:1010), `construct-for-decision`. Their internal order and error kinds are not in the table; "exception" in the "can it end the run" column for these means only that `run-phase!` rethrows (or the debugger stops).
- Which phase surfaced `:no-current-target-reading` in the two live clicks: not traced. The kind is produced at `src/futon2/aif/target_reading_registry.clj:191` and consumed at `selected_target_reading.clj:215,244`, `target_reading_preparation.clj:11` and `wm/family_selection.clj:121`.
- Rows 2 and 3 call `run-phase!` inside a `try` that captures the error (R:5277-5289) and the error is thrown at R:6250/R:6257, not at the phase; so a phase event with `:outcome :error` for `:agent-readiness` or `:code-state` precedes `:ration-admission` only if those phases completed (the later throw happens before R:6266).
- WM lines after the `:completeness-audit-projection` checkpoint (WM:6846) up to WM:7039 were read only at the checkpoint lines.
- Rows marked "exception (kind not read)" were not traced into their callee.
