# TN: stop-line terminal paths and run-record persistence

Date: 2026-09-12  
Scope: discovery only; no implementation change.

## Source boundary

The line references below are against Futon2 `14dc45cc7dce50588f94b04b10a2c0f89ed3ee45` (`full_loop_runner.clj`: “Persist versioned fold validator verdicts before construction refusals”). This is the current commit touching that file; it includes codex-18's validator-verdict persistence. The serving-side references are against Futon3c `5acbdcf623e30dcdf83c64599d1cda9b209651c5` for `runner_service.clj` and `12b9c82d31d84eac776e91b072fb089fc46a6477` for `run4_series_service.clj`.

## Exact v3 break

The v3 click entered the stop-line branch because the first open non-environmental obligation is selected at `src/futon2/aif/full_loop_runner.clj:2870-2894`; stop-line precedence bypasses ordinary pinned selection at `:2932-2962`. Consequently `pinned-selection` is nil (`:2947-2951`) and the selection judgment receives no `:run4/task-pin` (`:2984-2989`, `:3032-3035`). The selected and enacted entry was instead `repair-attempt-001`.

The repair ran through author/review/revision, reached grounding at `:3578-3582`, then failed while processing the repair obligation at `:3583-3604`. The common catch converted that exception into the typed incomplete close at `:3657-3709`. `close!` did persist the remaining cohort checkpoints and `007-closed` (`:2651-2656`, `:2798-2828`). Thus this is not a missing cohort-terminal bug.

The missing run record occurs one layer later. `close!` computes `run-route` with `packet-run-route` and returns it as `:wm/route` (`:2808-2824`). That function only synthesizes a route for (a) historical verification, (b) a selection ground containing `:run4/task-pin`, or (c) an existing selection route / trace (`:283-320`). The ordinary task pin is deliberately absent on the stop-line path, the outcome is not historical admission, and this selection supplied no route, so the returned route is empty.

The public wrapper always calls `persist-run-record!` after the core returns (`:3711-3726`, `:3819-3821`), but that function writes only when the observed route is nonempty (`:322-376`). For v3 it therefore returned `{:run-record-status :absent :run-record-absence :runner-did-not-observe-topology-route}` at `:375-376`.

Futon3c does not create the missing record. It calls the resolved Futon2 wrapper at `src/futon3c/wm/runner_service.clj:410-425`, then asks the terminal/historical projection writers to consume its result at `:219-242`. `persist-click-run-binding!` classifies a nil run-record path as `:absent` (`:243-249`) and publishes only an unavailable binding with the runner's absence reason (`:252-282`). `run4_series_service.clj:217-235` requires the strict historical or ordinary bundle; neither can exist without the record. That is why the v3 service retained an unavailable binding rather than a terminal projection.

## Terminal-path inventory

“Writes today” below means a `tick-run-record-<run-id>.edn`, not merely the cohort's `007-closed.edn`.

| Terminal path | Core close / type | Writes today? | Exact persistence path |
|---|---|---|---|
| Ordinary grounded completion | `close! :grounded-change` or `:grounded-no-change` at `full_loop_runner.clj:3632-3656` | Yes for a pinned RUN4 production selection; otherwise only if selection/trace supplied a route | `packet-run-route` synthesizes `RUN4_PACKET -> FULL_LOOP_CLOSE` from the selection-ground task pin (`:295-316`); wrapper writes via `persist-run-record!` (`:322-374`, `:3819-3821`). |
| Guardrail / typed author refusal | Author refusal is typed at `:1423-1430`; common catch closes using `outcome-from` and `failure-kind-from` at `:3657-3709` | Yes on the pinned ordinary production path; no on an unpinned path with no selection route | Same common `close!` (`:2651-2842`) and route-dependent wrapper write (`:322-376`). The guardrail vocabulary normalization is at `:2343-2351`. |
| Build/review failure (including rejected review and exhausted revision) | Failures are thrown with `:outcome :build-failed` at `:3387-3439` and `:3535-3577`, then common-catch closed at `:3657-3709` | Yes on pinned ordinary production selection; no if no route/pin survives | Same route-dependent `close!` then `persist-run-record!` (`:2808-2824`, `:322-376`). |
| Other post-start incomplete/environmental/recoverable failures | Readiness, polling, recovery, construction, grounding, dispatch, transport and untyped exceptions converge on common catch (`:2843-2865`, `:3220-3413`, `:3657-3709`); outcome/failure classification is `:2343-2474` | Conditional: yes with an existing route or pinned ordinary task; otherwise no | `close!` always closes the cohort (`:2651-2656`, `:2825-2828`), but the wrapper record remains gated by nonempty route (`:322-376`). |
| Open stop-line machine repair (v3 path) | Stop-line selected at `:2870-2962`; implementation/grounding/resolution at `:3578-3604`; exception common-catch closes incomplete at `:3657-3709` | **No** when the stop-line selection has no explicit `:wm/route` (the observed v3 case) | Stop-line precedence suppresses the pinned task identity (`:2936-2951`); none of `packet-run-route`'s synthesis predicates applies (`:295-316`), so `persist-run-record!` returns absent (`:322-376`). |
| Historical verification admission | Typed completion is closed as `:historical-verification-awaiting-validation` at `:3660-3667` | Yes | `packet-run-route` synthesizes `STOP_LINE -> HISTORICAL_VERIFICATION` (`:296-308`), then the common wrapper writes (`:322-374`). |
| Cohort stopping rule / already exhausted cohort | Outer wrapper returns `:cohort-complete` without entering a new attempt at `:3731-3753` | **No** | Returned result has empty checkpoints and no `:wm/route`; wrapper calls the writer, which returns absent (`:322-376`, `:3819-3821`). This is a scheduler terminal observation rather than a work attempt, but it is presently a terminal wrapper result without a record. |
| Initialization failure before a cohort attempt owns the failure | Outer wrapper creates a typed `:incomplete` initialization result at `:3754-3815` | **No** | It records the repair finding and morning brief (`:3780-3806`) but returns no route (`:3807-3815`); the writer therefore returns absent (`:322-376`). |

The structural rule today is therefore not “every terminal result writes”; it is “every result reaches the writer, but only results with a nonempty observed/synthesized route write.” The terminal categories above expose three definite gaps: open stop-line repair, initialization failure, and cohort-complete. The other failure categories inherit the same gap whenever they lack a pinned task or explicit route.

## Minimal implementation packet

Put the repair at the common producer boundary, not in Futon3c and not as projection fabrication:

1. Change `packet-run-route` (`full_loop_runner.clj:283-320`) to accept the selected/enacted action plus the authenticated requested-pin context independently of `pinned-selection`. It must synthesize a typed route for every core close:
   - stop-line repair: `STOP_LINE -> FULL_LOOP_CLOSE`, with the repair id/action and an explicit requested-not-enacted task pin when present;
   - ordinary terminal/refusal/build failure: retain `RUN4_PACKET -> FULL_LOOP_CLOSE`;
   - historical admission: retain `STOP_LINE -> HISTORICAL_VERIFICATION`.
   The v3 record's outcome remains `:incomplete`; its typed kind is `:machine-repair-lacks-grounded-review-evidence` (the durable error is “Machine repair implementation lacks grounded review evidence”). It must not be represented as task success or historical admission.
2. Make `persist-run-record!` (`:322-376`) total for terminal wrapper results. For a pre-core initialization failure, synthesize an `INITIALIZATION -> FULL_LOOP_CLOSE` route from the wrapper's typed `:failure-kind/:failure-stage`; write `:outcome :incomplete` and that exact kind. Do not manufacture selection, enaction, task, or checkpoint claims.
3. Treat `:cohort-complete` explicitly. If Item 5 literally includes scheduler terminal observations, write a typed `COHORT -> STOPPING_RULE` record with `:outcome :cohort-complete` and the recorded target/attempted counts from `:3748-3753`. If Item 5 is scoped to started work attempts, codify that exclusion in the record contract and make the service publish a separate typed scheduler observation; do not leave the current silent absence.
4. Keep Futon3c strict. `runner_service.clj:219-292` should continue refusing terminal projection when the producer record is absent or identity-mismatched. Once Futon2 produces the record, the existing binding path should bind it. Add tests at both boundaries: each terminal category above must either yield `:run-record-status :present` with its typed route/outcome, or the explicitly approved cohort-complete observation vocabulary.

This proposal changes record production only. It does not change stop-line precedence, repair resolution, retry, task verdicts, or terminal-evidence validation.
