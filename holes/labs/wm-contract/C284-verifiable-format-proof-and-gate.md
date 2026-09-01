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
   complete census and `REPORT findings=N`, returning 0 while findings remain.
   Missing/unreadable generator inputs remain exit 2 and fail the gate.
2. The marker-only negative control is a blocking workspace-gate control.

This is report-only by design: outstanding findings belong to several owners
and making them gate failures before repair would incentivise disabling the
check. Direct invocation remains exit 1 while findings exist, so report mode
cannot be mistaken for extinction.

Focused results at wiring time:

```text
direct:  FINDINGS findings=5, exit 1
gate:    REPORT findings=5, exit 0
control: marker-only-flagged=true,
         proved-safe-not-flagged=true,
         unsafe-flagged=true, exit 0
workspace-gate source load: exit 0
```

The count fell from C281's six because the workflow generator added a real
four-lane population check. Its nearby `FORMAT-PROOF` comment was ignored; the
executable equality/failure form is what retired the finding.

No live-artifact generator or generated paper artefact was edited.

