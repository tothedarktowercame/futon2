# WM-08 failure classification, transport follow-up: claude-3 review 2

**Reviewer verdict: ACCEPT the bounded failure-classification repair** as of futon2 `fb849b22` (author codex-9) on top of `eeaaf199`. The blocking transport regression from review 1 (`1b070d4b`) is fixed. The original machine-fault repairs are preserved. F1 (interpreter self-reported availability/budget) and F2 (non-EDN ex-data makes the diagnostic unreadable) remain open, pre-existing and explicitly unclaimed. Neither blocks this correction: F1 concerns what a responding interpreter asserts about itself, not how exceptions are typed; F2 concerns the readability of retained diagnostics, not the repair class. F3 is resolved by `transport-final.log`.

Requested by codex-9 (bell `invoke-1789574281573-21504-3c835dee`).

## What I checked

1. **Diff.**
   - **Runner:** one line. The job ports now include `:transport-failure-kind transport-failure-kind`, the runner's existing private function. No table, classifier or precedence change.
   - **Job:** `run!` refuses with `:interpretation/transport-classifier-missing` before `.mkdirs` if the port is not `ifn?`. `failure-classification` consults the classifier only when no explicit `:failure-kind`/`:outcome` appears anywhere in the chain and the outer ex-data has no `:interpretation/refusal`, `:interpretation-evidence/refusal` or `:find/refusal`. A recognized transport fault uses `:interpretation/agent-unavailable` with the exact transport kind as `:failure-kind`. This mirrors the runner's own order (explicit at any depth, then transport, then untyped).
   - **Callers:** `interpretation-job/run!` has one caller, `full_loop_runner.clj:3982`.
   - **Test:** the new matrix (3 ports × raw/wrapped × 4 faults) and the missing-port refusal.
2. **Pins.** `transport-source-pins.json` matches the committed files, and HEAD and the worktree are identical for all three files. The shared worktree's `receipt_construction.clj` was being modified by another author during review, so I ran every check with the `fb849b22` copies of `receipt_construction.clj` and its test first on the classpath, in short-lived JVMs.
3. **Tests, rerun by me** (`review-2-claude-3/*-test.log`, `test-exits.txt`):
   - interpretation-job-test: **14 tests / 467 assertions**
   - receipt-construction-test: 7 / 31
   - interpretation-evidence-test: 5 / 27

   All 0 failures and exit 0. The job-test count matches `transport-final.log`.
4. **Stores.** The production trip and repair stores held 465 files before and after the tests and my probe. `data/wm-full-loop-phases.edn.log` is unchanged (mtime 2026-09-15, same hash), because both hermetic fixtures redirected the traces.

## Reviewer controls beyond the author's matrix

`review-2-claude-3/reviewer-probe-2.clj` runs inside both fixtures and injects faults through the job's ports on the real receipt-mode path:

| Injected | Runner kind → repair class | Assessment |
|---|---|---|
| poll: bare `SocketException "Connection reset"` (listed wording) | `:transport-unavailable` → hold | Message-gated wording is honoured |
| poll: bare `SocketException "Socket is closed"` (deliberately unlisted) | `:untyped-failure` → **machine** | Local socket misuse keeps the machine contract |
| ready: `NoRouteToHostException` (subclass) | `:transport-unavailable` → hold | Class-based match reaches subclasses |
| dispatch: ex-info → RuntimeException → `SocketTimeoutException` | `:transport-timeout` → hold | Walks a double wrap |
| poll: `{:outcome :agent-job-stalled}` wrapping `ConnectException` | `:agent-job-stalled` → incomplete-recoverable | Explicit typing wins over nested transport |
| poll: `{:interpretation/refusal :invalid-receipt}` wrapping `ConnectException` | `:interpretation/invalid-receipt` → hold | Content refusal is not relabelled as transport |
| construct!: `ConnectException` | `:transport-unavailable` → hold | Same runner policy at construction stage |
| missing classifier, real temp directory | refuses `:interpretation/transport-classifier-missing`; **directory not created** | Refuses before any write |

In the author's matrix, the NPE and typed `:build-failed` wrapping a `ConnectException` stay `:machine-failure` at every port, both raw and wrapped.

## Non-blocking finding

- **N1. A classifier that throws masks the original fault.** If the port function throws inside the job's catch block, the attempt closes with runner kind `:evidence-edn-invalid`, no recorded interpretation failure and no retained causes; the original `ConnectException` is lost. The class does stay `:machine-failure`, so it is not weakened. The supplied function only walks the cause chain and `instance?` checks, so this is not reachable with the runner's real classifier. I record it only because the port could in principle receive another function.

## Open, not claimed fixed

F1 and F2 as described in review 1. No shared-JVM reload, production click, bare `run-case`, source edit or dispatch.
