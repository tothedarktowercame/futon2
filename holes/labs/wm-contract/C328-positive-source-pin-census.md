# C328 — positive witness source-pin census (2026-09-01)

This is a report-only census.  No witness, declaration, fixture, or registry
binding was changed.

## Population

The population is the 31 bound glossary terms.  It includes `Channel` and
excludes `FoldEscrowRecord`: C174 explicitly recorded the latter as support
for `Fold`'s escrow envelope rather than a glossary increment.

## Result

**1/31 pins the positive Lean witness source; 30/31 pin only the serialized
fixture plus the generated contract authority.**

The one source-pinned binding is `modelUncertaintyAndEIG`.  Its
`LeanProofReceipt` records declaration-slice SHA-256 values for
`modelUncertaintyBonus`, `parameterInformationGain`,
`expectedInformationGain`, and `modelUncertaintyAndEIG`; the checker recomputes
those slices, reruns Lean, and checks the axiom report.

The thirty fixture-only bindings are:

```
actGate                       aliveness
ambiguity                     bayesFactorThreshold
bayesianModelReduction        BeliefState
Channel                       Cohort
ControlVocabulary             DirichletConcentrations
expectedFreeEnergy            expectedInformationGain
Fold                          GenerativeModel
HaveWantArrow                 logMultivariateBeta
modelReductionFreeEnergyChange
ObservationVector             observationKernel
ParameterPosteriorKernel      ParameterPriorKernel
PolicyPriorKernel             PrecisionMap
predictionError               PredictiveOutcomeKernel
predictiveOutcomeRisk         PreferenceDistribution
softmax                       TransitionKernel
variationalFreeEnergy
```

For these thirty, `:run-sha` is the serialized fixture's last-change SHA and
`:contract-sha` is the generated `Holes.lean` contract authority.  Neither is
the content identity of the positive witness module.  Most checks read the
EDN fixture and separately invoke a `*Witness.lean` file; the two objects can
therefore change independently.  A weakened theorem or example can continue
to elaborate while the fixture remains current.  Regenerating/rebinding the
contract can then make the registry fresh without recording what positive
statement was accepted.

## What a useful pin must cover

A reason-preserving receipt needs all of:

1. the complete positive witness declaration (statement and body), or the
   complete witness module when declaration slicing is not reliable;
2. hashes of the named semantic declarations on which its meaning depends,
   rather than only a repository revision or generated contract row;
3. the fixture content identity and an explicit adapter/agreement statement
   connecting that fixture to the Lean object;
4. successful elaboration plus an axiom report under an identified Lean
   toolchain/dependency lock.

Hashing only an elaborated result is insufficient: elaboration success is the
condition a weakened statement also satisfies, and compiled artefacts are not
a stable semantic identity across toolchains.  Hashing a whole source file
does detect weakening but is noisy.  The existing `modelUncertaintyAndEIG`
declaration-slice receipt is the useful precedent: exact positive statement,
named semantic basis, elaboration, and axioms.  Witnesses whose construction
is generated dynamically need the generator/check declaration included in
the same basis.

## Definitional/projection witnesses

C324 identified these four in its ten-binding sample:

- `ObservationVector`
- `ParameterPosteriorKernel`
- `ParameterPriorKernel`
- `PolicyPriorKernel`

Their proofs honestly establish construction or stored normalization by
definition/projection; they do not independently validate downstream
behaviour.  A source/basis pin can prevent their already-limited statement
from silently weakening, but cannot make them stronger evidence.  That scope
limit belongs in any future receipt.

## Recommendation

Do not adapt all thirty as one change.  Introduce a positive-proof receipt
schema based on the existing declaration-slice precedent, prove it detects a
weakened positive statement, then migrate in consequence-ordered batches.
Start with the four definitional/projection witnesses and the same-payload and
partial-witness controls audited in C324.  Where no identity-preserving
fixture-to-Lean adapter exists, record that explicitly rather than treating a
source hash as agreement between two parallel objects.
