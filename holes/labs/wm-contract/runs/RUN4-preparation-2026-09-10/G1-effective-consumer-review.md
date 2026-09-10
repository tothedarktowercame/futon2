# G1: effective consumer state, not requested environment alone

Codex-17, 2026-09-10. Codex-12 independently accepted loader fix 479aa163.
The remaining flags cannot be established by merely extending runner-options.

Direct source inspection found:
- scripts/futon2/report/war_machine.clj:174 and :190: FPI_DARK and BETA_DARK
  initialize dynamic switches at namespace load; binding is documented for tests.
- src/futon2/aif/trace.clj:67: TRACE_POLICY_DETAILS is similarly captured once.
- holes/labs/wm-contract/wm_step_observe.bb:63: RECORDING_CONTRACT belongs to
  the separate observation script, not the serving JVM scoring namespace.

A trusted deployment check must distinguish required values, process environment,
and effective loaded consumer values. A current environment match cannot prove
what an already loaded namespace captured. No production rebinding of test-only
switches is authorized as a shortcut. The actual recording invocation needs its
own environment/output-contract check; the serving JVM's flags cannot attest a
separate process that has not run.

Codex-12's scope is extended to implement these explicit checks and provenance
plus the RUN4-scoped hierarchy declaration. Shared series/acceptance integration
remains coordinated with Codex-10. This note performs no live restart, reload,
environment mutation, credential provisioning or launch. A deployment mismatch
must refuse before click and report the actual cause.
