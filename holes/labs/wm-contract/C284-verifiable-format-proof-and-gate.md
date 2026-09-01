# C284 — verifiable format proof and workspace-gate wiring

Date: 2026-09-01

## Proof rule

`FORMAT-PROOF` comments no longer satisfy
`checks/live_artifact_format_boundary_lint.py`. They are declarations by the
code's author and can be added without executable validation. The lint now
accepts only source shapes it can recognise as executable presence or
population checks.

This deliberately prefers an honest false positive over self-certification. A
validation hidden behind an arbitrary helper stays review-required until the
lint learns that helper's executable shape. No marker escape hatch exists.

Control:

```text
unsafe formatter                         flagged
FORMAT-PROOF comment + unsafe formatter  flagged
same-scope non-nil assertion + formatter not flagged
```

## Gate wiring

The workspace gate runs two focused commands:

1. `python3 checks/live_artifact_format_boundary_lint.py --report` emits the
   complete census and `REPORT findings=N`. At the time of C284 a nonempty
   report used the process's clean status. That was deliberately report-only
   so outstanding findings owned by several lanes would remain visible without
   turning their unfinished repairs into blocking failures.
2. The marker-only negative control is a blocking workspace-gate control.

**Dated amendment, 2026-09-01 (C382).** The report-only pattern remains, but
its clean exit did not distinguish “no findings” from “findings reported but
non-blocking.” The executable contract is now: exit 0 clean, exit 1 blocking
findings, exit 2 instrument/self-test failure, and exit 3 explicit report-only
findings. The workspace gate accepts exit 3 only from structurally declared
report-only commands; an undeclared exit 3 and a report-only command returning
exit 1 both fail. Missing or unreadable lint inputs remain exit 2 and fail the
gate. Direct ordinary invocation remains exit 1 while findings exist.

Thus C284's ownership reason for report-only was retained, while its former
pairing of a nonempty report with the process's clean status is superseded.
Report-only is a distinct verdict, not a clean result or a path exemption.

Focused results at wiring time, retained as history:

```text
direct:  five findings, blocking
gate:    the same five findings, non-blocking (historical clean-status encoding)
control: marker-only-flagged=true,
         proved-safe-not-flagged=true,
         unsafe-flagged=true, accepted
workspace-gate source load: accepted
```

Under C382's current contract the corresponding nonempty report is
`REPORT findings=5, exit 3`.

The count fell from C281's six because the workflow generator added a real
four-lane population check. Its nearby `FORMAT-PROOF` comment was ignored; the
executable equality/failure form is what retired the finding.

No live-artifact generator or generated paper artefact was edited.
