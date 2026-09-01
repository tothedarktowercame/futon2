# C325 — adversarial review of the guarded-control migration

Date: 2026-09-01. Reviewer: `wm-evidence`. Report only; no fixture, wrapper, or
Lean source was changed.

## Verdict

The 32 `#guard_msgs` fixtures guard their exact rendered diagnostics and reject
the tested alternative failures. No coincidentally overlapping message passed.
The 14 non-Lean/predicate controls do **not** all establish their stated reason
when invoked alone: three representative wrappers passed on an unrelated
invalid baseline. The full workspace gate mitigates that failure by running the
corresponding positive check separately, but the negative invocation's own
`PASS` sentence overclaims what that invocation established.

## Guarded population

The War Machine population contains 32 `*Negative.lean` files, all using bare
`#guard_msgs in`; none opts into substring matching. A clean copy of
`VariationalFreeEnergyNegative.lean` exited 0. Four mutations of a temporary
copy produced:

| Mutation | Exit | Observation |
|---|---:|---|
| rename the imported module to a nonexistent module | 1 | missing `.olean`; guard never falsely passed |
| add an independent unknown-identifier diagnostic before the guarded command | 1 | the additional diagnostic remains a file failure |
| change the guarded assignment to a different type mismatch | 1 | `#guard_msgs` printed the exact expected/actual diagnostic diff |
| replace the assignment with an invalid-field error retaining `expectedValue` and `ExpectedFreeEnergyValue` text | 1 | overlapping terms did not satisfy the guard; exact diagnostic diff reported |

The last case directly tests coincidental message overlap. The matcher is
strict enough for the mutations requested: renamed imports, added diagnostics,
changed type names, and a different error sharing vocabulary all make the
fixture fail.

The remaining epistemic limit is narrower: `#guard_msgs` proves equality of
rendered diagnostics, not causal history. A different construction producing
the *identical* diagnostic is observationally equivalent to the guard. For the
current type controls, that diagnostic includes the term and both semantic
types, so this is not a demonstrated escape. Checked-in fixture identity and
source review remain the authority for what construction was mutated.

## Three predicate controls reject for the wrong reason

C311 says the other 14 controls need no diagnostic guard because their named
predicate returning false is the executable rejection reason. That is only
true if the unmutated baseline has first been proved valid in the same evidence
claim.

Temporary copies supplied an unrelated baseline error while leaving each
wrapper's intended mutation untouched:

| Control | Unrelated baseline defect | Negative exit and claim |
|---|---|---|
| `ambiguity_witness.clj --negative-control` | wrong fixture schema, not perturbed entropy | exit 0, `negative-control PASS (perturbed entropy rejected)` |
| `belief_state_witness.clj --negative-control` | wrong `:basis`, not missing variance | exit 0, `negative-control PASS (missing channel variance rejected)` |
| `channel_witness.clj --negative-control` | wrong fixture schema, not missing channel | exit 0, `negative-control PASS (missing declared channel rejected)` |

In each wrapper, negative mode accepts any `false` result from the compound
fixture predicate. It does not separately require that the original fixture
passes before applying the intended mutation. Thus the false predicate names a
rejection boundary but not uniquely the rejection reason.

This is the same shape as the four pre-C311 degeneracies, now at the compound
predicate boundary rather than Lean stderr: the control rejects, but not for
the claim printed beside exit 0.

## Scope and mitigation

The workspace gate contains positive invocations for these witnesses as well
as the negative controls (`checks/wm_workspace_gate.clj:186-191,256-260`). With
the malformed baselines above, the positive invocation fails even though the
negative invocation passes. Therefore the complete gate does not certify the
broken fixture. This is a material mitigation, not a repair of the negative
control's standalone evidence or message.

Only three of the 14 predicate controls were adversarially exercised here, as
requested. Their shared control structure makes the risk a population concern,
but this review does not claim all 14 have the same implementation without a
full wrapper-by-wrapper audit.

## Manual-review policy

Routine Lean stderr inspection can remain retired for the 32 exact guarded
fixtures. The tests support C311's claim for that population, subject to manual
review after Lean diagnostic wording changes as already recorded.

The retirement must not be read as saying every one of the 46 negative
invocations independently proves its reason. For predicate/data controls, a
reason-preserving invocation needs either:

1. baseline-valid and mutated-invalid assertions in the same process, with the
   failing clause named; or
2. a composite receipt binding the passing positive invocation to the negative
   mutation over the same captured fixture.

Until then, the full positive-plus-negative gate is required context whenever
one of these predicate negative controls is cited. A standalone exit 0 is not
reason-preserving evidence.

