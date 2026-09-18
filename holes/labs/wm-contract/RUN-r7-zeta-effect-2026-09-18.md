# RUN — R7 demonstrated effect of a declared FIXED ζ on Q(o|π) and G

Date: 2026-09-18. Owner: zai-30 (R7). Companion source: `futon2.aif.likelihood-precision`
(`tempered-rates`), tests `futon2.aif.likelihood-precision-test`.

## What this run demonstrates

R7's blocking clause is "a demonstrated effect on Q(o|π) and G". This run
evaluates the committed, Lean-aligned consumers
(`cascade-model-manifest/predict-observations` for Q(o|π) and the enumerating
`horizon-g` for G) at non-zero adjudication rates, once with the raw rates and
once with the rates tempered by `tempered-rates` at a declared FIXED ζ. ζ is
DECLARED FIXED in every case; no prior or update law for ζ is invented (R7-2
stays open; the checklist accepts a fixed declared ζ).

## Setup

- Universe: tokens `t0`, `t1`.
- Rates (exact rationals): `t0 {:false-neg 1/10 :false-pos 1/20}`,
  `t1 {:false-neg 1/5 :false-pos 1/10}`.
- Policy π: one fire move producing `t0` at every step; q0 = {∅ 1}; horizon 2.
- C: `preference-spec {:want #{"t0"} :lam 1 :mu 0 :zeroed #{}}` over the same
  universe, supplied to horizon-g as the constant map c(o) = preference(o)
  over all four subsets (horizon-g's c-fn convention: a map, missing key = 0).

## Q(o|π) at state {t0} (predict-observations, exact rationals)

| ζ (declared fixed) | P(o=∅) | P(o={t0}) | P(o={t1}) | P(o={t0,t1}) |
|---|---|---|---|---|
| 0 (uninformative: fair coin per token) | 1/4 | 1/4 | 1/4 | 1/4 |
| 1 (identity — the raw likelihood) | 9/100 | 81/100 | 1/100 | 9/100 |
| 3 (sharpened) | 729/532900 | 531441/532900 | 1/532900 | 729/532900 |

ζ = 1 reproduces the untempered predictive distribution exactly; ζ = 0 is the
declared uninformative kernel; ζ = 3 concentrates 99.7% of Q(o|π) on the
correct observation {t0}. The tempered rates at ζ = 3 are themselves exact
rationals: `t0 {:false-neg 1/730 :false-pos 1/6860}`,
`t1 {:false-neg 1/65 :false-pos 1/730}`.

## G(π), horizon 2 (enumerating horizon-g)

| ζ (declared fixed) | G |
|---|---|
| 0 | 3.012817736156336 |
| 1 | 2.212817736156336 |
| 3 | 2.015557462183733 |

ζ = 1 equals the untempered G to machine precision (asserted at 1e-9 in the
tests); ζ = 3 lowers G by 0.197 (a sharper A is a less ambiguous, more
informative likelihood — ambiguity falls); ζ = 0 raises G by 0.8.

## Why the substitution is the Lean temperedLikelihood, not an approximation

`token-likelihood` factorizes as A(s,o) = ∏_v Bern_v(p_v(s)). Raising to ζ and
normalizing over o ∈ 2^U factorizes the normalizer too,
Z(ζ)_s = ∏_v (p_v^ζ + (1−p_v)^ζ), so the tempered row is the product of
independently tempered Bernoullis — exactly what `tempered-rates` feeds the
existing formula. Asserted two ways in
`tempered-rates-reproduce-temperedLikelihood-exactly`: enumerating
`observation-distribution` at tempered rates equals `temper-row` of the
enumerating raw distribution (all 16 states×ζ, tolerance 1e-12). Lean
carriers: `AIF.Terms.temperedLikelihood` + `temperedLikelihood_sum` +
`temperedLikelihood_one` (mathlib4 DarkTower).

## Scope and remaining gap

This run uses the ENUMERATING horizon-g, which is committed and Lean-aligned.
The live tick scores through `horizon-g-sparse`, whose non-zero-rate factorized
scoring is WIRE-4 (zai-55, in flight in the shared checkout at the time of
writing); the same `tempered-rates` substitution applies there unchanged once
WIRE-4 lands. Until then the live tick's own G is still evaluated at
:zero-adjudication-identity where ζ multiplies nothing — named in
`zeta-declaration`'s gaps, not papered over.
