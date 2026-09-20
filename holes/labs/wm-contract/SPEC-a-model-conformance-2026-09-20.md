# SPEC — A model conformance (Lean-first), 2026-09-20

Companion to PLAN-a-programme-2026-09-20.md: what the Lean layer now
REQUIRES of any A implementation, and which programme obligations remain
queued transcription work. Point-2's reference implementation (the
sidebyside enumeration behind the production interface) conforms to the
PROVEN rows; the QUEUED rows are transcription debt in claude-12's lane,
never latitude.

## Proven — implementations conform to these now

| # | requirement | Lean source |
|---|---|---|
| A1 | the coupled model IS the finite latent mixture A(o|s) = Σ_z w(z)·Π_i P(o_i|s_i,z); per-token kernels are the falseNeg/falsePos product form with rates in [0,1] | `TokenObservation.tokenLikelihood`, `AdjudicationRates`; `MixedTokenObservation.mixtureLikelihood` |
| A2 | the mixture is a probability kernel when weights normalize | `mixtureLikelihood_colsum` |
| A3 | checkable tokens are observed EXACTLY inside the coupled model: zero rates on the checkable subset in every latent component force the marginal over judgement reports to report the true checkable part with probability one — coupling licenses no uncertainty on git-decidable facts | `mixtureLikelihood_checkable_marginal` (349951de51); single-kernel form `tokenLikelihood_checkable_marginal` |
| A4 | acceptance MUST test joint events: equal per-token marginals do not determine the joint law (witness instance: matched 1/2 miss marginals, all-missed joint 1/2 vs 1/4) | `MixtureJointSeparationWitness.coupling_separates_only_jointly` (9c63cb9f0a) |

Conformance checks these imply for point 2's implementation: (i) its
likelihood function is pointwise equal to `mixtureLikelihood` on the
enumeration fixtures; (ii) its checkable-channel outputs never deviate
from state regardless of declared coupling (A3 as a test); (iii) its
acceptance harness for WMC compares joint queries, with marginal-only
agreement explicitly insufficient (A4 as a test-design rule).

## Declared, external to the model (contract layer)

Rates and weights are DECLARED named parameters with their basis
labeled (R7 declared-as-such precedent); the 15% bad-day is experimental
configuration, never an empirical claim; rate bases obey the
counted-denominator rule and, when adopted, attach per self-correction
channel (Claude: refused handoffs; Codex: refusal-at-intake; GLM/Zai:
in-turn) — never one agent-assertion blur.

## Queued transcription (claude-12's lane; not latitude)

| item | programme point | note |
|---|---|---|
| G risk/ambiguity under dependence; the point-mass cancellation control (coupling shifts risk and ambiguity by canceling amounts in total G) | 4 | per-token sums are NOT licensed for coupled distributions until proven; exact enumeration is the interim authority |
| F observation-to-prediction matching semantics | 4 | independent of A's noise; belongs with the F consumer fix |
| two-products direct evaluation for fully specified observations under one binary common cause | 3 | cheap lemma over `Fintype.sum_bool`; enables early exact use without the compiler |
| parameter-uncertainty layer (variance-WMC): means/variances propagation contracts | 6 | fixed-parameter inference first |

## Inherited coupling from C's closure (2026-09-20, codex-3 flag)

The nonzero-rate factorized evaluator refuses
`:c-family-unsupported-with-rates` on the step-indexed C family. A typed
refusal, so the zero-rate measurements that closed C stand. Consequence
for this programme: before ANY rated run (point 2's coupled/independent
configurations included), that evaluator must support the step-indexed
family — otherwise the refusal blocks honestly, which is correct
behavior and a known coupling, not a surprise.
