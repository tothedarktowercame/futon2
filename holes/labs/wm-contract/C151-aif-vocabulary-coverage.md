# C151 — AIF vocabulary coverage census

Date: 2026-08-31

## Population and rule

The authoritative paper vocabulary is the 33 `\paragraph{...}` entries in
`p4ng/sec-glossary.tex`, already enumerated by
`holes/labs/wm-contract/glossary-formal-lines.md`. The five terms completed by
C38–C147 were the five formerly open witness targets in
`P-glossary-mathematics`; they were never the whole glossary.

Status below is strict: **defined** means a named Lean declaration states the
entry's carrier or law. Merely accepting an unconstrained function argument,
or having an adjacent carrier, does not define the term.

## The 33 glossary entries

| # | glossary term | current Lean status |
|---:|---|---|
| 1 | Active Inference Framework | **none** — no composite `ActiveInferenceModel` tying A/B/Q/F/G together |
| 2 | Generative model | defined: `GenerativeModel`, `generativeFactorMass` |
| 3 | Belief state μ | defined: `BeliefState`, `beliefUpdate` |
| 4 | Observation vector o | defined: `Channel`, `R2Tick`, `r2WellFormed` |
| 5 | Prediction error ε | defined: `predictionError` |
| 6 | Precision Π | defined: `PrecisionMap` and evidence-weighted update parameters |
| 7 | Variational free energy F | defined: `VariationalFreeEnergyValue`, `variationalFreeEnergy` |
| 8 | Expected free energy G | defined: `expectedFreeEnergy`, `G`, conditional bridge theorem |
| 9 | Risk | defined: `predictiveOutcomeRisk` |
| 10 | Ambiguity | **none** — `expectedFreeEnergy` accepts `PolicyIndex → ℝ`; no declaration constrains it to expected observation entropy |
| 11 | Observation model A | defined: normalized `ProbabilityKernel`, `observationKernel` |
| 12 | Model uncertainty and EIG | defined and distinguished: `modelUncertaintyBonus`, `expectedInformationGain`, counterexample |
| 13 | Softmax/controller calibration | defined: `softmax`; engineering calibration remains a separate seam |
| 14 | Pattern language/cascade | defined as WM vocabulary: `Pattern`, `Cascade` |
| 15 | Control states U/policy vocabulary | **none as stated** — `Pattern` is an explicitly functional analogy, not a `ControlSchema` definition |
| 16 | Policy prior E/habit | defined at theory grain: `PolicyPriorKernel`; live habit values remain deployment data |
| 17 | Policy π | defined at both separated grains: `cascadeGrainPi` and `DecisionRule` |
| 18 | Aliveness L=T·H | **none** |
| 19 | Embedding space | **none** |
| 20 | Bayesian Model Reduction | defined: `bayesianModelReduction` |
| 21 | Dirichlet concentration parameters | defined inline by the strictly-positive, nonempty concentration subtype consumed by log-beta/BMR |
| 22 | Log multivariate beta and ΔF | defined: `logMultivariateBeta`, `ModelReductionFreeEnergyChange` |
| 23 | Bayes-factor threshold | defined: `bayesFactorThreshold` |
| 24 | GFlowNet “slush” | **none** — no `SlushProposalKernel` |
| 25 | Fold | **none** — no `FoldPlan` in the War Machine formal vocabulary |
| 26 | Act-gate | **none** — code and prose predicate exist, no Lean `actGate` |
| 27 | EDN | **none** — no Lean data grammar |
| 28 | Substrate and Drawbridge | **none** |
| 29 | No self-certification | defined: `independent`, `independenceVerdict`, `ValueEvidencePolicy` and soundness laws |
| 30 | Demonstration Foundry/have–want arrows | **none** |
| 31 | Strategic mission selection | **none** — no strategic/tactical policy carriers or conditional selector |
| 32 | Clicks, attempts and cohorts | **none** — adjacent workflow types do not define these measurement units |
| 33 | Shared experimental substrate | **none** |

Tally: **19 defined / 14 without a named Lean definition**. Therefore the
mission's first clause is not complete for the glossary as written. This does
not reopen the 5/5 binding result: all five selected mathematical witness
targets remain bound; they are a subset with a different denominator.

## Lean AIF terms absent as glossary entries

The glossary is also incomplete in the opposite direction. These are not mere
implementation helpers: they are the typed objects denoted by symbols used in
its AIF formulas, and the machine/contract now relies on them:

| omitted entry | Lean carrier | where the paper already uses it |
|---|---|---|
| Outcome | `Outcome := Sigma Obs` | outcomes `o` throughout G/EIG |
| predictive Q(o∣π) | `PredictiveOutcomeKernel` | risk and EIG formulas |
| transition B | `TransitionKernel` | generative-model factorization |
| policy prior P(π)/E | `PolicyPriorKernel` | generative model and habit equation |
| preferences C | `PreferenceDistribution` | KL risk `KL[Q‖C]` |
| parameter prior Q(θ∣π) | `ParameterPriorKernel` | EIG definition |
| parameter posterior Q(θ∣o,π) | `ParameterPosteriorKernel` | EIG definition |

`ProbabilityKernel`, value wrappers, row-mass helpers, and positivity subtypes
are internal formal machinery and do not require reader-facing entries. The
seven rows above are different: they carry the semantics of displayed
equations. Treating them as invisible implementation details would make the
paper use typed AIF nouns without defining them for its reader.

## Verdict

`sec-glossary.tex` is authoritative as the paper's current list, but it is not
a complete inventory of either its own formal dependencies or the machine's
AIF vocabulary. It contains 14 entries still lacking named Lean definitions
and omits seven substantive AIF carriers that now have Lean definitions.

No binding or declaration changes are made in this census. The next decision
is editorial/scope-bearing: either the mission means the narrower core AIF
subset and must name that subset, or “every term” means the current 33-entry
glossary plus its seven displayed-formula carriers. The present 5/5 statement
cannot honestly stand for either population.
