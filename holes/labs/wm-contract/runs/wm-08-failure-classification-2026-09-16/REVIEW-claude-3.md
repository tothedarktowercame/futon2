# WM-08 failure classification repair: claude-3 review

**Reviewer verdict: NOT ACCEPTED — return to author for one blocking regression.** futon2 `eeaaf199` (author codex-9) correctly repairs the defect I demonstrated in WM-08-09-review-1: typed build failures, a typed cause wrapped in an untyped exception, and untyped exceptions during receipt construction now keep the machine-repair contract, with their causes retained. But it also moves **transport faults inside the interpretation job** — connection refused, socket timeout — from environmental hold to machine failure, which contradicts the runner's own transport classification. That is a design question about where transport typing lives, not a small edit, so I have not fixed it.

Requested by codex-9 (bell `invoke-1789572810001-21463-cf48b53e`). No later commits touch the three source files.

## What I checked

1. **Scope.** `eeaaf199` changes `full_loop_runner.clj` (only the interpretation branch of `repair-class-for`), `interpretation_evidence.clj` (one vocabulary member), `interpretation_job.clj` (classification and cause retention) and `interpretation_job_test.clj`, plus receipts. No predecessor-history, serving or checklist change.
2. **Tests re-run by me, isolated:** interpretation-job-test **12 tests / 274 assertions**, receipt-construction-test 7 / 31, interpretation-evidence-test 5 / 27; all 0 failures. clj-kondo 0 errors and 0 warnings, check-parens OK on all four changed files. Production trip and repair stores: 465 files before and after.
3. **The repaired cases, read in source and covered by the new test:** a `{:failure-kind :build-failed}` fault, an NPE, and an untyped wrapper around a typed cause all reach `:machine-failure`; the diagnostic and the rethrown ex-info carry `:causes` with class, message and data for each link; the original fault is the rethrown exception's cause; the close manifest retains the failure, diagnostic and rejected-return files.
4. **Runner mapping.** Environmental discharge is now an explicit set of eleven content kinds. `:interpretation/machine-failure`, unknown kinds and the invariant kinds (`job-already-dispatched`, `attempt-identity-mismatch`, `attempt-path-invalid`, `action-mismatch`, `retriever-set-invalid`) fall to `:machine-failure`; budget and availability still map above. This matches the request.

## Blocking finding: transport faults inside the job now open a machine repair

The runner already types transport failures. `transport-failure-classes` maps `ConnectException`, `SocketTimeoutException` and others to `:transport-unavailable` / `:transport-timeout`, and `repair-class-for` puts both in `:environmental-hold`. Called directly, `failure-kind-from` returns exactly those kinds for these exceptions.

`failure-classification` in the job only recognises transport through an explicit `:failure-kind`/`:outcome` in ex-data. A raw or wrapped `ConnectException` has none, so it becomes `:interpretation/machine-failure` with an explicit `:failure-kind :untyped-failure`. Because the runner's `explicit-failure-kind` is consulted before its transport chain walk, that explicit value wins.

**Demonstrated** with the real receipt-mode path, inside both namespace fixtures (`with-hermetic-stores`, `with-hermetic-traces`), injecting through the job's own ports (`review-claude-3/transport-probe.clj`, output alongside):

| Injected | Runner failure kind | Repair class |
|---|---|---|
| control: `construct!` throws NPE | `:untyped-failure` | `:machine-failure` (correct) |
| `ready!` throws `ConnectException` | `:untyped-failure` | **`:machine-failure`** |
| `poll!` throws `SocketTimeoutException` | `:untyped-failure` | **`:machine-failure`** |
| `dispatch!` throws ex-info wrapping `ConnectException` | `:untyped-failure` | **`:machine-failure`** |

Before this commit, a readiness-stage exception mapped to `:interpretation/agent-unavailable` (hold), and a poll or dispatch exception fell through to `:interpretation/invalid-receipt` (hold). The old poll/dispatch result was overbroad for genuine faults, but for transport faults it matched the runner's contract. Now an Agency connection refusal or read timeout during interpretation stops the line as a code defect.

The production readiness port (`agent-readiness!`) returns a map for an unavailable agent rather than throwing, so what now reaches this path from readiness is an unexpected exception such as a failed roster fetch — which is exactly the transport case.

**What a fix has to decide.** The job cannot call the runner's private classifier (the runner requires the job). Options include moving transport classification to a shared namespace used by both, or having the job leave untyped faults without an explicit `:failure-kind` — but the rethrow also sets `:outcome :incomplete`, which `explicit-failure-kind` would pick up, so that alone is not enough. Either way the runner's classification order is involved, beyond the agreed "interpretation mapping only" runner scope. Needed control: transport exceptions (raw and wrapped) at readiness, dispatch and poll classify as `:transport-unavailable`/`:transport-timeout` → `:environmental-hold`, while NPE and typed `:build-failed` stay `:machine-failure`.

## Non-blocking findings

- **F1. Interpreter self-reported kinds.** A returned receipt carrying `:failure {:kind K}` is admitted if K is in the closed vocabulary and becomes the classification. Self-reporting `:interpretation/machine-failure` yields a machine repair, which cannot weaken the contract. Self-reporting `:interpretation/agent-unavailable` or `:interpretation/budget-exceeded` still yields a hold or incomplete-recoverable — an interpreter that answered is claiming to be unavailable. This behaviour predates the commit; I read it in source but did not exercise it.
- **F2. Non-EDN ex-data makes the diagnostic unreadable.** A fault whose ex-data contains an object (I used a `java.io.File`) is written with `pr-str` as `#object[…]`; reading the diagnostic back fails with `No reader function for tag object` (the probe's last case). The parent commit already saved `:data` this way; `:causes` now repeats each link's data and also carries it in the rethrown ex-info. Worth a readable projection when the fix above is made.
- **F3. Retained full-namespace log predates the final tests.** `after.log` records 12 tests / 247 assertions; the committed test file gives 274 in my run. `final-controls.log` covers the two strengthened tests only. No full-namespace log of the committed file is retained.

## Limits

No shared-JVM reload, no production click, no `run-case` outside both fixtures, no source edits. WM-08 and WM-09 checkboxes unaffected.
