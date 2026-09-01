# C343 — classification of the 21 unsampled positives (2026-09-01)

This is a classification pass, not a receipt migration.  C324's ten-witness
sample is excluded from this population.

## Result: 10 nontrivial, 11 construction/projection

The ten nontrivial value or coherence witnesses are:

| Binding | Positive claim |
|---|---|
| `actGate` | Complete improving inputs pass; a missing leg abstains. |
| `aliveness` | The recorded factors multiply to exactly `0.48`. |
| `ambiguity` | A point-mass observation model has entropy zero. |
| `expectedFreeEnergy` | The one-point KL plus ambiguity is `2`, and the named decomposition bridge agrees. |
| `expectedInformationGain` | A point posterior against a fair two-parameter prior gives `log 2`. |
| `logMultivariateBeta` | Gamma identities give `0` and `-log 2` for the two recorded concentration vectors. |
| `PrecisionMap` | Swapping precision and signed error changes variational F from `1` to `2`. |
| `predictionError` | The signed difference `1 - 3` is `-2` and differs from either operand. |
| `predictiveOutcomeRisk` | Point mass against a fair binary preference gives KL `log 2`. |
| `softmax` | At temperature `1/3`, the exact weights are `8/9` and `1/9`, normalized and correctly ordered. |

These ten outrank the construction/projection group for receipt migration.
C324's high-consequence sample therefore missed ten additional nontrivial
positives; consequence order cannot be inferred from that sample alone.

The eleven construction, enumeration, or stored-proof witnesses are:

| Binding | Exact evidence limit |
|---|---|
| `BeliefState` | Constructs a total mean/variance carrier and projects nonnegativity; it does not witness an update. |
| `Channel` | Definitionally enumerates the fourteen coordinates and checks their count. |
| `Cohort` | Constructs one finite preregistered cohort; it does not establish cohort outcomes. |
| `ControlVocabulary` | Constructs one policy whose controls lie in one vocabulary. |
| `DirichletConcentrations` | Constructs `[2,1]` and projects its recorded values. |
| `Fold` | Constructs the recorded fold fields and policy holes; no behavioural fold law is proved. |
| `HaveWantArrow` | Constructs two arrows and supplies endpoint equality for their composition. |
| `observationKernel` | Constructs a normalized fair row and recalculates/project its stored normalization. |
| `PredictiveOutcomeKernel` | Constructs two deterministic rows and projects each stored normalization proof. |
| `PreferenceDistribution` | Constructs a fair binary preference and projects stored normalization. |
| `TransitionKernel` | Constructs controlled deterministic rows and projects the start-row normalization. |

For all eleven, replacing the fixture with another appropriately typed trivial
instance would leave the general construction/projection pattern intact.  The
current fixtures still carry exact named values, so this is limited evidence,
not false evidence.  A receipt can prevent silent weakening but cannot make
these bindings evidence of downstream behaviour.  That limit should be added
to their registry metadata when each is migrated.

## Axiom and vacuity audit

All 21 witness modules elaborate.  Named audit wrappers were used for the
anonymous machine-vocabulary, belief-state, and observation-kernel examples;
`#print axioms` was run on those wrappers and every named theorem/definition
chain above.  Results contain only `propext`, `Classical.choice`, and
`Quot.sound` (some construction definitions use fewer or no axioms).
**0/21 depend on `sorryAx`.**

No proposition has an unsatisfiable antecedent.  None obtains its result from
an empty carrier: the value witnesses explicitly construct their finite
supports/inputs, and the carrier witnesses explicitly construct their data.
The point-mass ambiguity and one-point EFE cases are intentionally minimal
mathematical fixtures, not vacuous implications; their exact entropy/KL and
ambiguity conclusions remain the stated claim.

## Migration order

Recommended next order is by semantic consequence:

1. `expectedFreeEnergy`, `expectedInformationGain`, `predictiveOutcomeRisk`,
   `ambiguity`;
2. `logMultivariateBeta`, `softmax`, `PrecisionMap`, `predictionError`;
3. `actGate`, `aliveness`;
4. the eleven construction/projection witnesses, with their evidence-limit
   metadata recorded explicitly.

No binding was migrated in C343.
