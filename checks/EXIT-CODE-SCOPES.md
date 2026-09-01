# Exit-code scopes

Exit codes in this repository are scoped to the producing command. They are
not one global enum.

| Scope | Meanings |
|---|---|
| Ordinary checks | `0` pass, `1` fail; a control commonly uses `2` for mutation slipped or instrument failure. |
| Declared report-only checks inside `wm_workspace_gate` | `0` clean, `1` blocking failure, `2` instrument failure, `3` findings reported. Exit `3` is accepted only by a gate command carrying `:expected-exits #{0 3}`. |
| `wm_status_report.py` | `0` OK or accepted degradation, `1` new degradation, `3` `DECISION-DUE`. This `3` is unrelated to report-only findings. |

GNU Make converts every failed recipe to exit 2. The bounded test service
converts every nonzero inner exit to outer 125/`test-failure`. Their output or
receipts retain the inner/script exit for diagnosis, but neither boundary
preserves its semantic type.

Consequently, a report-only command must be consumed by its declared adapter
inside the workspace gate before crossing Make, the bounded service,
readiness, or the quiet-run state machine. Do not invoke a report-only command
directly from a Make recipe or the bounded launcher. If the state must travel
independently, use a structured receipt rather than another nonzero code.

`checks/exit_code_scope_check.clj` enforces the current declared set and these
two lossy entry boundaries.
