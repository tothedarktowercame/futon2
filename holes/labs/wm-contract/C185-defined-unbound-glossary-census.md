# C185 — defined but unbound glossary census

Date: 2026-08-31

## Population and rule

The population is the 33 paragraph entries in `p4ng/sec-glossary.tex`.  After
C157–C179, 26 entries have a named Lean definition and 12 of those entries have
a conformant registry binding.  The remaining population is therefore exactly
14 entries, not an estimate from the declaration count.

`Bindable now` means an independent executable referent or a mathematical
fixture can test the stated definition without choosing new semantics.
`Blocked` means the machine has data nearby but no identity-preserving adapter
from that data to the glossary-grain Lean object.  `Not bindable` would mean the
term names no computed or observed object; none of these fourteen has that
shape.

## Census

| glossary entry | Lean definition(s) | status | existing evidence / remaining work |
|---|---|---|---|
| Belief state μ | `BeliefState`, `beliefUpdate` | **bindable now — witness exists, unregistered for this term** | `checks/belief_update_check.clj` already accepts the updating case and rejects inert mean and variance updates. Register the carrier/update declaration against that fixture. |
| Observation vector o | `Channel`, `R2Tick`, `r2WellFormed` | **bindable now — witness exists, unregistered for the glossary term** | `checks/r2_channel_contract.clj` and the pinned R2 snapshot exercise channel presence and loud absence. Bind the glossary entry to the source object rather than creating another census. |
| Prediction error ε | `predictionError` | **bindable now — executing referent exists** | `compute-prediction-error` emits `:prediction-error/v1`; precision tests preserve it through trace serialization. Add a small source-to-Lean numeric fixture and a sign/mean perturbation control. |
| Precision Π | `PrecisionMap` | **bindable now — executing referent exists** | `precision_test.clj` checks adaptive precision and the producer contract. It needs a registry adapter pinning one channel fixture to the nonnegative Lean carrier. |
| Variational free energy F | `VariationalFreeEnergyValue`, `variationalFreeEnergy` | **bindable now — executing referent exists** | `compute-variational-free-energy` is live and tested. Add an independently hand-computed finite-channel value and perturb one precision/error term. |
| Risk | `predictiveOutcomeRisk` | **bindable now — witness exists, unregistered for this component** | `ExpectedFreeEnergyWitness` already derives the KL risk term independently. Give the component its own binding and risk-only perturbation rather than treating the whole EFE check as implicit coverage. |
| Observation model A | `ProbabilityKernel`, `observationKernel`, `observationKernelRowMass` | **bindable now — witnesses exist, unregistered for this term** | Belief-update and ambiguity fixtures both instantiate normalized rows. Bind a row-mass fixture with a non-normalized mutation. |
| Softmax/controller calibration | `softmax` | **bindable now — executing referent exists** | `policy_test.clj` checks normalization, monotonicity, temperature, and habit input. An adapter must pin the same finite menu in Lean and Clojure. |
| Pattern language/cascade | `Pattern`, `Cascade` | **blocked** | `CascadeDiff` witnesses one organized cascade, but no adapter proves that the serialized pattern/cascade object is the same `Cascade` consumed by the declaration. Needs a full-fidelity fixture-to-Lean adapter; a shape-only tuple would repeat the proof-about-a-copy defect. |
| Policy prior E/habit | `PolicyPriorKernel` | **bindable now — witness exists, unregistered for this carrier** | `GenerativeModelWitness` instantiates the normalized policy prior, and live policy tests exercise habit mass. Add a prior-only binding/control so the carrier is not credited indirectly through the container. |
| Policy π | `cascadeGrainPi`, `DecisionRule` | **blocked** | Lean cleanly separates scored cascade from resulting decision rule, but the live scheduler still operates at action/mission grain. Binding requires an identity-preserving recorded cascade-policy at the glossary grain; scheduler actions must not be promoted to π. |
| Bayesian Model Reduction | `bayesianModelReduction` | **bindable now — executing referent exists** | `bmr.clj` and its tests cover identity, accepted reduction, and rejected reduction. Add the hand-derived count-vector fixture to the registry. |
| Dirichlet concentration parameters | positive nonempty subtype consumed by `logMultivariateBeta` | **bindable now — witness exists, unregistered for the domain term** | `LogMultivariateBetaWitness` already instantiates positive concentrations independently. Give the domain its own zero/negative-entry rejection binding. |
| Bayes-factor threshold | `bayesFactorThreshold` | **bindable now — executing referent exists** | BMR tests pin the `ΔF ≤ -3` sign convention and boundary. Register an exact boundary fixture plus a just-above-threshold control. |

## Partition and consequence

- **12 bindable now**: seven already have adjacent executable evidence that is
  merely unregistered at the glossary-term grain; five need small adapters or
  independent numeric fixtures around existing executing referents.
- **2 blocked**: Pattern/cascade and Policy π, both on object identity at the
  glossary grain rather than on missing implementations.
- **0 not bindable**.

The actionable population is therefore not smaller than fourteen in the way
the undefined-term census was: twelve terms can move.  The important shrinkage
is qualitative—seven are registration/adapter work around evidence that already
exists, while only five require new reference fixtures.  Neither blocked term
should be made green by treating a generated or scheduler-grain sibling as the
declared object.

This census changes no declaration or binding.
