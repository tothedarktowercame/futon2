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

## Addendum 2026-09-18 (later same day): the :zeta seam on the sparse scorer

WIRE-4 landed (98adff8d) and `horizon-g-sparse*` now takes an optional
model-map `:zeta` (default 1, DECLARED FIXED), tempering the per-token kernel
once up front via `likelihood-precision/tempered-rates` — one law, one place.
Recorded numbers, mission-scale spec (want {t0,t1}, evidence {e0}, lam 2,
mu 1/2), rates 1/8·1/16 on all three tokens, one producing cascade, horizon 3:

| ζ (declared fixed) | G (sparse factorized path) |
|---|---|
| 1 | 7.895551077649658 |
| 3 | 7.809189069493589 |

GCertificate provenance at ζ = 3: `{:zeta 3, :zeta-tempered? true,
:evaluation :factorized-nonzero-rates, :universe-size 3}` — a tempered run is
distinguishable from an untempered one even when the numbers coincide.

Cross-check asserted in `cascade-model-manifest-test`
(`r7-zeta-seam-tempers-the-sparse-factorized-path`): sparse(ζ, rates) via
`:zeta` == the existing sparse path run on the transformed rates == the
enumerating `horizon-g` on the same tempered rates, all at 1e-12, for
ζ ∈ {0,2,3}; ζ = 1 is exactly the untempered call; ζ ≠ 1 with all-zero rates
is the typed refusal `:zeta-with-identity-rates`, never a silent no-op
(zai-55/zai-30 ruling).

Remaining gap, unchanged and named in `zeta-declaration`:
`efe/rank-cascade-actions` still constructs all-zero adjudication rates
itself, so the LIVE tick has not yet reached the tempered path — wiring real
observation-rates into the tick is the next WIRE slice.

## Addendum 2 (2026-09-18, later still): the tick path itself — no more hardcoded rates

Per claude-4's dispatch, the zero-rate hardcode at efe/rank-cascade-actions is
gone. `:adjudication-rates` on the scoring opts declares the kernel (typed
refusal `:invalid-adjudication-rates` naming missing tokens when it does not
cover the scored universe — never a silent projection to zero), and `:zeta`
rides the same opts to the scorer. Absent both, the call is byte-identical to
what it always was.

Recorded through the full tick path (`efe/rank-actions` →
`rank-cascade-actions` → `horizon-g-sparse-cert`), same pattern fixture,
horizon 1:

| call | G | meta :rates | certificate |
|---|---|---|---|
| default (no rates) | 8.171700819516008 | :zero-adjudication-identity | :evaluation :identity-A-zero-rates, :zeta 1 |
| declared rates 1/8·1/16 | 8.046700819516008 | :declared-adjudication-rates | :factorized-nonzero-rates, ambiguity :computed |
| declared rates + ζ=3 (FIXED) | 8.171108402454397 | :declared-adjudication-rates, :zeta 3 | :zeta 3, :zeta-tempered? true |

The tempered tick-path G equals the sparse scorer run directly on
`tempered-rates` at the same ζ (asserted at 1e-12 in
`efe-certificate-test/r7-declared-rates-and-zeta-reach-the-tick-path`).

What remains open, named in `zeta-declaration`: no production caller SOURCES
real rates from `futon2.aif.observation-rates` yet (built, zero live
consumers). That sourcing is the next WIRE slice; R7's own clauses — Lean
statement, aligned runtime, demonstrated effect on Q(o|π) and G — are met.

## Addendum 3 (2026-09-18): the declaration rides the default path too

Claude-4's echo gap, closed: the GCertificate now carries `:zeta-status`
alongside `:zeta`. On the identity path it reads
`:declared-fixed-vacuous` — the fixed ζ EXISTS there and is vacuous (no
likelihood matrix is evaluated), which is a different fact from ζ never being
considered (`:absent`, reserved). The tempered path reads
`:declared-fixed-applied`. The status keywords are SOURCED from
`zeta-declaration`'s `:certificate-statuses` (via
`likelihood-precision/zeta-certificate-statuses`), so the certificate and the
declaration cannot drift apart — the same discipline as WIRE-2's
`:computed-not-attached` for F. Also fixed: the tick path passed `:zeta nil`
explicitly when the opts lacked it, which masked the scorer's default of 1 —
now `(get opts :zeta 1)`; a default-path certificate reads
`{:zeta 1, :zeta-status :declared-fixed-vacuous}`. The ns docstring's stale
"implicit and vacuous / R7-3 open" narrative is rewritten to the current
wired state (the census files' dated quotes are historical records and stay
as they are).
