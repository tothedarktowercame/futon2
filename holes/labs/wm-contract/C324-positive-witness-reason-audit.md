# C324 — positive witness reason audit (2026-09-01)

This is a census sample, not a repair.  The deterministic sample takes the
highest-consequence positive witnesses from the classes that motivated the
negative-reason migration: same-payload free energies, exact arithmetic
boundaries, complete/conditioned carriers, and shared-state wiring.

Sample (10 of 31):

1. `variationalFreeEnergy`
2. `bayesFactorThreshold`
3. `modelReductionFreeEnergyChange`
4. `bayesianModelReduction`
5. `ObservationVector`
6. `ParameterPosteriorKernel`
7. `ParameterPriorKernel`
8. `PolicyPriorKernel`
9. `GenerativeModel`
10. `modelUncertaintyAndEIG`

## Logical result

All ten positive files elaborate. `#print axioms` on the named theorem/definition
chain reports only `propext`, `Classical.choice`, and `Quot.sound`; none reports
`sorryAx`.  No sampled proposition is an implication with an impossible
antecedent, and no empty carrier makes its stated result vacuous.

Four positives are definition/projection proofs:

- `ObservationVector.completeCoordinateValues` is `rfl` over an explicit
  fourteen-constructor function;
- posterior, parameter-prior, and policy-prior row-mass results project the
  normalization proof stored in `ProbabilityKernel`.

These are not presently false-reason passes: exact enumeration is the vector
claim, and normalization is the carrier claim.  They do show that the theorem
adds no evidence beyond successful construction of the typed carrier.

The other six prove nontrivial exact values or type coherence: Gaussian `F=1`,
both sides of the Bayes threshold, `log 2`, BMR count arithmetic, generative
factor mass `1/2`, and the normalized zero-versus-one EIG counterexample.

## Finding: positive source identity is not pinned

Nine of ten sampled bindings validate two parallel objects:

- an exact EDN fixture checked by Clojure; and
- a separately handwritten Lean witness file that elaborates.

No adapter proves that the serialized fixture is the object interpreted by the
Lean proposition.  More importantly, registry freshness resolves `:run-sha`
against the EDN fixture and `:contract-sha` against `Holes.lean`; it does not pin
the positive witness file or its theorem statement.  A weakened positive
statement that still elaborates can therefore remain `fresh` while the EDN
fixture is unchanged.  The term-specific negative guard does not prevent that
mirror failure.

`modelUncertaintyAndEIG` is the sampled exception: its proof receipt records
hashes of the relevant source declarations, including the theorem/counterexample
basis.

Thus the sampled positives currently pass for their stated mathematical
reasons, but 9/10 lack reason-preserving source identity.  This is the positive
mirror of C294, and it warrants a full 31-entry source-pin/adapter census before
claiming the class closed.  No binding or witness was changed in this packet.
