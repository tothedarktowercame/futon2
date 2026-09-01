# C294 — glossary negative-control revalidation (2026-09-01)

Scope: the 31 bound glossary witness suites.  Several suites carry more than
one mutation, so the complete inventory is 46 negative executions.

## Result

- 46/46 declared mutations were rejected; no mutation slipped.
- One rejection had weakened.  The C212 same-payload free-energy control
  imported `VariationalFreeEnergyWitness`, whose `.olean` was absent.  Lean
  therefore stopped at the missing object file instead of reaching the
  intended `ExpectedFreeEnergyValue` versus `VariationalFreeEnergyValue` type
  mismatch.
- The one-line restoration changes the negative file to import the stable
  `Holes` module directly.  Direct elaboration now reaches and reports the
  intended type mismatch.

Diagnostic spot checks also reached their intended reasons for:

- Bayes-threshold same-payload rejection;
- model-reduction free-energy same-payload rejection (with an additional
  noncomputability diagnostic, but the required type mismatch is present);
- parameter-posterior rejection of both prior and predictive kernels;
- observation-vector rejection of a partial map and of a single outcome.

## Standing enforcement

`checks/wm_workspace_gate.clj` enumerates the glossary negative invocations,
so a mutation that starts exiting successfully makes the gate fail.  It does
not inspect Lean diagnostics, however.  C212 demonstrates that the standing
gate catches a slipped mutation but cannot detect a control that still rejects
for a weaker, unrelated reason.  Reason-preservation therefore remains a
manual/direct-diagnostic audit rather than an enforced property.

No glossary binding, carrier, contract declaration, or Q-facing definition
changed.  Coverage remains 31/33.
