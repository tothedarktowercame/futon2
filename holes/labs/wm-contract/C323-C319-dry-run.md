# C323 — dry run of the canonical quiet-run sheet

Date: 2026-09-01. No unit/coordinator was parked, no bounded job was launched,
and no quiescence claim, reload, click, or certificate was attempted.

## Actual read-only/refusal results

| C319 phase | Command exercised | Actual result versus former text |
|---|---|---|
| Manifest | Literal date, timer, running-service, process, `bg.py list`, and `test-list` commands | Callable. At 01:15:37Z the APM babysitter and serving JVM were active; watchdog next pulse was 01:18:14Z. Output was observation, correctly not treated as pass. |
| Fence evidence | `python3 checks/writer_fence_evidence.py --self-test` | Four controls passed: verifiable→0, unattested→3/indeterminate, active-unit→1/breach, moved→3/indeterminate. Former C319 omitted this C321 command. Live execution was deliberately skipped because this packet forbade a quiescence attempt. |
| Workspace gate | `make -n workspace-gate` | Expanded to `scripts/run_workspace_gate_bounded.py` and the named `workspace-gate: script-exit=...` line as documented. Actual launch was skipped because it creates a bounded unit/receipt. |
| Bounded launcher | `bg.py launch-test --help` | Refused before launch with `repository-basis-required`; this confirms `--dir` is mandatory. Both C319 suite commands include it. No unit/receipt was created. |
| Reload preflight | `make runner-reload-preflight` | Correct refusal: 4/6 passed; repository dirty and no clean bounded receipt for current commit; `REFUSED`, command withheld, `script-exit=1`; Make outer exit 2. The expected READY remains conditional on the quiet sequence. |
| WM preflight | `clojure -M:wm-preflight` | Exit 0 but explicitly `writer-fence: ABSENT`, `:event-free? :unverified`; this exposed C319's missing C316 `--writer-fence ID` step. |
| Readiness | Direct `make run-readiness` | `NOT-READY (needs-you)`, `script-exit=1`, Make outer 2. It named dirty/stale gate and suite receipts plus unloaded serving code, selected `codex-1`, and printed the required observer command—not bare curl. |
| Click observer refusal | `bb -cp . checks/wm_click_resource_observer.clj` with no arguments | Printed `usage: ... RECEIPT REVIEWER`; no POST and no receipt. |
| Certificate refusal | `make certify-run` with no `RUN_ID` | Printed `RUN_ID is required`; Make outer 2; no certificate. |

## Corrections made

1. C321's runnable evidence bundle replaces manual C292/unit reconstruction at
   every checkpoint and preserves observed/attested/unverifiable separately.
2. C316's `clojure -M:wm-preflight --writer-fence "$FENCE_ID"` and
   `FENCE-CONDITIONAL` expectation are now explicit.
3. Parking order now stops the watchdog timer before waiting for its service,
   then explicitly stops `apm-closer.service`; `Restart=always` means “let it
   finish” alone was false.
4. Abort restoration now names the actual resume/start commands and the
   coordinator-status/systemd evidence that confirms undo.

Mutating phases remain unexecuted: coordinator/unit parking, bounded gate and
suites, serving reload, production observer/click, certification, and
restoration. Their commands were syntax/interface checked through read-only
expansion or refusal paths where available; their production semantics retain
the C230/C271 rehearsal evidence rather than a claim made by this dry run.
