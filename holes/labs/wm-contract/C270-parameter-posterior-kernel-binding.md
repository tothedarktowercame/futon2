# C270 — parameter-posterior kernel binding

Date: 2026-09-01

The fixture enumerates the complete two-policy by two-observation input grid;
every joint input has a deterministic normalized parameter row. Lean rejects
both parameter-prior `Q(theta|pi)`, which lacks observation conditioning, and
predictive-outcome `Q(o|pi)`, which has the wrong domain and codomain. This is
the fifth live notation collision in five consecutive bindings.

The fixture witnesses carrier identity and completeness, not a claim that
every observation must change the posterior. Belief-update behavior remains a
separate witnessed declaration.

## Closure

Only evidence metadata changed; the posterior carrier and Q consumers retain
their bodies. Q semantics are unchanged and the `:lean-spine` pin is refreshed
to `5b1a411a29dedc11a1e643eefd2a1cca968573e60cee2e451fdcee1c452083a4`.
`ParameterPosteriorKernel` already has explicit `:scores`
model-area classification.
