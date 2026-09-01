# C433 — negative-control revalidation census

Date: 2026-09-01

## Result

No additional negative control was found to have stopped detecting on the
current tree.

- 74 controls declared by `checks.wm-workspace-gate/control-commands`: 74
  rejected their mutations, 0 slipped, 0 unavailable.
- 41 additional declared negative modes in 26 executable files containing a
  `mutation-slipped` branch but absent from `control-commands`: 41 rejected
  their mutations, 0 slipped, 0 unavailable.
- Total exercised: 115 control invocations; 115 detected, 0 slipped, 0
  unavailable.

The previously reported
`c277-perturbed-reduction-free-energy` failure is not present on the surveyed
tree.  After C426, both its perturbed-value and wrong-type controls return 0
and explicitly report that the mutation was rejected.  This census did not
repair it.

## Population method

The first population is executable rather than inferred: every entry returned
by `checks.wm-workspace-gate/control-commands` was run using its declared
working directory and argument vector.

The second population was derived by searching executable `checks/` and
`scripts/` sources for branches that can emit `mutation slipped` or
`mutation-slipped`, subtracting files represented in `control-commands`, and
then enumerating every negative mode accepted by each remaining program.
Documentation-only matches were excluded.

This distinction exposed an invocation trap without turning it into a false
finding: several Clojure controls require `clojure -M -m ...` rather than bare
Babashka, and route conformance requires an explicit extant run-record path.
An unavailable invocation was rerun through the program's declared runtime;
it was not counted as either detection or a slip.

## Gate-registered controls

All 74 current entries accepted.  This includes the Lean-backed glossary
controls, empty-subject controls, contract/holder/interface controls, R8/R9
controls, referent mutation and concurrency controls, operational-certificate
tampering, correction-index mutation, and both preemptive-lint self-controls.

The C277 pair now reports:

```text
model-reduction-free-energy-change-witness: negative-control PASS (value rejected)
model-reduction-free-energy-change-witness: negative-control PASS (type rejected)
```

## Controls outside `control-commands`

The 41 exercised modes cover:

- ablation exact-dyadic (1);
- belief update and variance inputs (3);
- cascade diff O1--O4 (4);
- control-map figure and lint (2);
- expected free energy, expected information gain, generative model,
  log-multivariate-beta, and hyper-edge domain/range (5);
- lane registry missing/overdue/done (3);
- obligation reconciliation (1);
- six preemptive-repair lints and their composite gate (7);
- preference-stack binding absent/malformed and witness shape (3);
- R17 disposer and R2 channel contract (2);
- R2 and R8 pinned snapshot pin/census modes (4);
- R9 proof receipt absent/tampered (2);
- trace-schema compatibility and route conformance (2);
- mutable-verdict claim control and run-readiness reviewer control (2).

All returned the control-success exit and described the intended mutation as
rejected.

## Finding about the census itself

The 41 working invocations outside `control-commands` are not stale today, but
they are not protected by the gate's executable control census.  A future
regression in one of them will not necessarily reproduce C433 unless another
suite invokes it.  That is a coverage boundary, not evidence that any control
currently fails.

## Unavailable

None.  No production click, serving-JVM mutation, or live operator action was
required by this population.

No implementation was changed.
