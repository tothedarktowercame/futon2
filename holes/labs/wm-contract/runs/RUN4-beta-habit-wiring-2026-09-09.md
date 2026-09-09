# RUN4 habit-in-both beta wiring — 2026-09-09

Item 25, implementing SESSION-model-choices section 1. Opt-in
:beta-habit-in-both? in judge opts, then FUTON_WM_RUN_CONFIG's top-level key,
then FUTON_WM_BETA_HABIT_IN_BOTH=1. Explicit false overrides the sheet/env;
absent settings preserve the existing three-argument beta carry byte-for-byte
against the frozen 110-candidate, WM-shaped fixture.

The existing evidence flags remain prerequisites: FUTON_WM_FPI_DARK=1,
FUTON_WM_BETA_DARK=1, FUTON_WM_TRACE_POLICY_DETAILS=1. The opted-in judge
refuses before external reads if any prerequisite is absent. The committed
RUN4 sheet names both the arm opt and required environment. These fields are
configuration/provenance, not an automatic environment exporter. No live run.

beta_habit/carry reuses policy_precision's own identity-resolved F_pi/G subset
and converge-beta implementation via carry-beta, adding aligned log-priors with
:log-prior-placement :both. Missing/invalid/unsourced biases refuse rather than
becoming zero; the existing caller records the refusal as a held beta. Every
opted-in state carries the requested arm, boundary and either actual sourced
ln E records or an explicit unavailable reason. Trace persistence uses the
existing policy-precision-state field. The strategic fixture E_S, separate
habit stores, selection laws and ties are untouched. This selects the beta
arm; it does not turn on variational temperature mode or promote strategic E.

Focused tests: 8 tests / 32 assertions / zero failures/errors. Broader run:
233 tests / 1168 assertions / one failure, zero errors. The single failure is
mission-c-readback-hashes-the-criteria-source-test (U12/U15), expected
51f6de53... versus live a770d000... on M-wm-aif-policy-grain-compliance.md.
It is the same pre-existing red described in claude-1's 7a0fe12b excursion-log
entry: U12's replay emits an obsolete fixed verdict, so re-pinning would lie.
No fixture or producer repaired here. This gate is NOT reported green.
Other relevant pure suites earlier: 82 tests / 451 assertions / zero failures.
Clojure lint 0 errors/0 warnings; check-parens and git diff --check pass.

Coordination: waited for codex-10's 4e76f94f depth commit before editing the
caller. During beta tests, inbox-zero commit e102f155 automatically captured
our 32-line war_machine.clj integration diff, before the new adapter files were
committed. This is why item 1 necessarily spans that externally-created commit
and the companion commit containing this note, adapter, tests and configuration.
Do not review e102f155 alone as a complete implementation. No amend/revert of
someone else's commit, no ledger edits, and no live reload occurred.
