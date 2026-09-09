# Separated probability/risk certificate — 2026-09-09

This is a finite execution certificate connecting the recorded cohort and runtime
seed to the concrete Lean KL witness, then checking its contribution to the EFE
controller score. It is not a live-run certificate or a universal proof of the
Clojure implementation. The observation-model bridge remains open.

Joe commissioned this certificate following the era comparison in
`../FOLDC-era-comparison-2026-09-09.md`. The old ordered-fold receipt remains
unchanged; replacing its runtime gate is a separate reviewed integration step.

## Reproduce

First, in mathlib4:

```
lake build DarkTower.WarMachine.PreferenceRiskWitness
```

Then, from futon2, in a separate process:

```
bb -cp .:src -m checks.preference-risk-receipt /tmp/separated-risk-reproduction
```

Compare both generated files with this directory. The runner refuses failed
proofs, failed controls, runtime mismatches, or source/cohort drift during the
run before writing output. It creates a new certificate for the current source
basis; historical acceptance requires comparing the recorded pins and artifacts,
not merely observing PASS from a fresh regeneration.

## Evidence and boundary

- mathlib4 `2af52ef97e`, `PreferenceRiskWitness.lean`: concrete `scalarKL` on
  the existing F10 twelve-outcome seed equals ln 2; both distributions retain
  the same twelve-member support; positive prediction at the named abstained
  zero refuses. Target build: exit 0, 2708 jobs. The three printed theorem
  dependencies contain only propext, Classical.choice, Quot.sound. Existing
  Holes warnings do not occur in these theorems' axiom dependencies.
- `runtime-mass-binding.lean` is generated from the actual seed and fitted
  cohort through the runtime adapter, not copied from the Lean expected values.
  Twenty-four equality proofs bind all twelve preference and prediction masses
  to the Lean definitions. The keyword/constructor mapping is explicit in the
  generator and requires exact support equality. A normalized changed seed
  (grounded 1/4, agent-unavailable 3/8) fails elaboration with unsolved goals.
- Ten runtime checks exercise `efe/compute-efe`: scalar value ln 2, score
  contribution and score delta, weight 2 giving 2 ln 2, byte-identical opt-out,
  constant observation behavior, open-bridge metadata, invalid-support refusal,
  and positive-prediction-at-preferred-zero refusal. Comparisons involving
  floating-point arithmetic use an explicit absolute tolerance of 1e-12.
- Additional controls plant a zero scorer contribution and falsely closed
  observation bridge; both are rejected. Focused certificate tests: 2 tests,
  4 assertions, zero failures/errors. Existing disposition/efe/policy suites:
  95 tests, 479 assertions, zero failures/errors.
- Basis pins cover the named proof, runtime and fitting files and Lean toolchain
  manifest. Holes pins cover Vertex, Outcome, ProbabilityKernel and
  PreferenceDistribution declaration slices, using the existing receipt
  machinery. They do not claim a complete transitive dependency closure and
  deliberately do not incorporate unrelated parked marker edits.

An initial probe ran before the new witness build completed and refused the
missing compiled import; it produced no certificate. After the build completed,
all certificate runs passed. Two final derivations are byte-identical:

| Artifact | SHA256 |
|---|---|
| certificate.edn | 196c7408938610c9ad340b709945c15acec5f5bd9f44cfaacdfb92d754173ff5 |
| runtime-mass-binding.lean | 305222dea73f92c92bf89166be7423e12d1eae04eb3b40e5067b338911fc6659 |

Clojure/EDN lint: 0 errors, 0 warnings; check-parens and git diff --check pass.
No production scorer, worklist, registry, old witness or old receipt was changed.
Independent review must decide integration of this successor check; this commit
does not turn the known old layer-ID comparison failure green.

## Two-axis integration refresh

The follow-up integration pins PreferenceRiskBoundary.lean as well as the
concrete witness, and refreshes the certificate after the runtime declaration
acquires an explicit risk-contribution axis. Computed masses and observed risk
values are unchanged. The fold wrapper now compares BOTH declaration axes and
regenerates this certificate in a temporary directory, demanding byte equality
with the reviewed artifacts; generation alone is not acceptance. The old
ordered-fold receipt is preserved separately.
