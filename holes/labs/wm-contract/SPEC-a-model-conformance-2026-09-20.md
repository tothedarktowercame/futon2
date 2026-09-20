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
| G risk/ambiguity under dependence; the point-mass cancellation control (coupling shifts risk and ambiguity by canceling amounts in total G) | 4 | FIRST HALF DISCHARGED 2026-09-20: `GTotalMarginalInvariance` (mathlib4 5bb5e50b4a, sorry-free) — for point-mass state and positive product-form C, risk+ambiguity = cross term depending only on per-token marginals and total mass (`totalG_eq_of_marginals_eq`); the witness pair gets identical total G against EVERY such C (`witness_totalG_eq`). Acceptance use: an implementation whose total G differs at matched marginals under these hypotheses has a bug; one whose per-term split does NOT differ between coupled/independent is suspect the other way. Per-token G sums remain unlicensed for coupled kernels — only the TOTAL enjoys the invariance. SECOND HALF DISCHARGED 2026-09-20: `GNonPointMassDecomposition` (sorry-free) — for general beliefs, risk + ambiguity = cross term − mutual information; point masses have MI = 0 (recovers the cancellation); at matched predicted-law marginals the total-G difference equals the MI difference, so dependence enters total G through MI and nowhere else. Still queued: MI ≥ 0 (Jensen), which nothing downstream currently needs |
| F observation-to-prediction matching semantics | 4 | independent of A's noise; belongs with the F consumer fix |
| two-products direct evaluation for fully specified observations under one binary common cause | 3 | DISCHARGED 2026-09-20: `TwoProductEvaluation.mixtureLikelihood_bool` (+ `_products`), sorry-free — now a proven row: implementations may evaluate fully specified observations as two weighted products and MUST agree with that value |
| parameter-uncertainty layer (variance-WMC): means/variances propagation contracts | 6 | fixed-parameter inference first |

## Inherited coupling from C's closure (2026-09-20, codex-3 flag)

The nonzero-rate factorized evaluator refuses
`:c-family-unsupported-with-rates` on the step-indexed C family. A typed
refusal, so the zero-rate measurements that closed C stand.

SCOPE CORRECTED 2026-09-20 (codex-2 discovery, reproduced by claude-4:
G = 2.652925220802433 on the bounded route, :evaluation
:exact-enumeration, coupled A with the step-indexed terminal family in
the certificate): the BOUNDED exact-enumeration route already evaluates
coupled A with step-indexed C, so point 2's lab configurations do NOT
wait on this fix. The fix gates production's FACTORIZED route — rated
ticks — only. Also established there: `:non-factorizable-belief` checks
the state rollout belief, a different object from observation coupling;
the two refusals compound only when the belief itself is correlated, so
the fix packet is C-family alone and `:non-factorizable-belief`
continuing to refuse is a negative control, not a companion fix.
