# C121 — workspace-gate reachability audit

Date: 2026-08-31

## Result

Before this change, `bb -cp . checks/wm_workspace_gate.clj` ran 12 commands and
passed 12/12 (exit 0).  C116 and C117 had subsequently landed six isolated
semantic controls which it did not reach.  Each was first run independently,
then added.  The gate now reports 18 executable checks plus one inventory
check, 0 failures, exit 0.  Absence coercion remains reporting-only by the
announced C81 policy.  Its population was 15 at dispatch and was reduced
concurrently by C119; this delivery does not promote it to hard failure.

The added controls are:

- C116: removed ledger row, changed O7 declaration source, and injected
  pre-boundary stored F;
- C117: F1 outside-repository selection, F2 removed receipt, and F3 score-only
  receipt.

All six reject the intended semantic mutation.  None was wired red.

## What is executed

The 12 prior positive commands remain: strict contract, holder ownership,
control-map figure agreement, control organization, both hyper-edge checks,
fold quarantine, preference shape and JVM binding, R9 proof receipt, route
conformance, and runs-once.  The six controls above are now additional gate
commands.  The gate still records two manual exclusions:

- lane registry measures dispatcher/operator discipline, not repository
  validity, and is run at dispatch and closure boundaries;
- the live operational certificate consumes an operator run plus resource
  receipt which a repository gate must not manufacture.

The normal futon2 suite separately reaches `contract_lint`, `control_map_lint`,
obligation-ledger reconciliation, the preemptive-repair suite,
`r2_channel_contract`, `r8_f_contract`, `r9_independence`, and certificate
fixture logic.  The six preemptive wrapper files are exercised through their
shared suite.  Historical witness replayers remain event-triggered by changes
to their pinned source/fixture.  `r17_generator_disposer_check` remains
activation-triggered by a live R17 path.  `absence_scoring_counterfactual` is
a diagnostic measurement, not a pass predicate.

## Discovery policy

Blind self-execution is not safe: a filename does not reveal whether a check
needs Babashka or the JVM, live operator input, a cross-repository checkout, or
whether it is a historical replayer rather than a build predicate.  Instead,
the gate now discovers every top-level `checks/*.clj` file and compares it with
an explicit classified inventory.  An added or removed check makes
`:check-inventory` fail loudly.  The cost is one deliberate classification
entry per new check; the benefit is that adding a file cannot silently leave
the gate stale, while discovery never guesses that a script is safe to run.

This is self-updating for completeness detection, not for execution policy.
That boundary preserves the two documented manual exclusions and the C81
reporting-only policy.
