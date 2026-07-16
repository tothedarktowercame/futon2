# M-pattern-authority-gate — run report

**Run:** 2026-07-16, authoritative execution after preregistration commit `9fee3be`

**Verdict:** **AUTHORITY FAIL.** Under the preregistered definition, none of the
four real design patterns is a live yield actuator. The pattern-propagation line
stops here; no propagator, exotype, selection, evolution, or pattern composition
is warranted by this gate.

## Preregistration and protocol

Before running any arm, the verdict was fixed as follows: a pattern is a live
actuator iff its paired yield differs from `off` by a two-sided exact binomial
sign test with `p < .05` on held-out boards. Direction is irrelevant: reliably
worse yield would still pass authority.

The run used 20 paired seeds × 300 ticks, patchy food on 30×30 boards. Boards
were screened only on geometry (`distance(home, nearest food) <= 8`) before any
arm ran. The held-out seeds, never selected against, were:

`[1 2 3 5 6 7 8 10 12 14 17 18 22 23 24 26 27 30 32 33]`

The `off` mean yield was `21.245`. Every arm used the same food and movement
seed per board and ran sequentially. `lambda.pattern` was swept over
`{0.1, 0.5, 1.0}`.

## Load-bearing controls

- **Sham tied exactly:** `:cyber/baseline` matched `off` on every seed at
  λ = 0.1, 0.5, and 1.0. Full observable event traces matched, not only yield.
- **Zero-lambda tied exactly:** all five pattern IDs at λ = 0 matched `off` on
  every seed, again including full observable event traces.

The real-arm numbers are therefore interpretable. Neither control was weakened
or removed.

## Per-arm, per-lambda result

`wins/n` excludes yield ties, as the sign test requires. A row with `n=0`
means all 20 paired yields tied exactly.

| arm | λ | Δ yield vs off | wins/n | sign-p | yield ties | preregistered reading |
|---|---:|---:|---:|---:|---:|---|
| off | 0.0 | +0.000 | 0/0 | 1.00000 | 20 | reference |
| sham | 0.1 | +0.000 | 0/0 | 1.00000 | 20 | exact control |
| sham | 0.5 | +0.000 | 0/0 | 1.00000 | 20 | exact control |
| sham | 1.0 | +0.000 | 0/0 | 1.00000 | 20 | exact control |
| cargo-return | 0.1 | +0.000 | 0/0 | 1.00000 | 20 | no detected difference |
| cargo-return | 0.5 | +0.000 | 0/0 | 1.00000 | 20 | no detected difference |
| cargo-return | 1.0 | +0.000 | 0/0 | 1.00000 | 20 | no detected difference |
| white-space | 0.1 | −0.105 | 0/1 | 1.00000 | 19 | no detected difference |
| white-space | 0.5 | −0.035 | 1/3 | 1.00000 | 17 | no detected difference |
| white-space | 1.0 | −0.035 | 2/5 | 1.00000 | 15 | no detected difference |
| hunger-coupling | 0.1 | +0.000 | 0/0 | 1.00000 | 20 | no detected difference |
| hunger-coupling | 0.5 | +0.000 | 0/0 | 1.00000 | 20 | no detected difference |
| hunger-coupling | 1.0 | +0.000 | 0/0 | 1.00000 | 20 | no detected difference |
| pheromone-tuner | 0.1 | +0.000 | 0/0 | 1.00000 | 20 | no detected difference |
| pheromone-tuner | 0.5 | +0.000 | 0/0 | 1.00000 | 20 | no detected difference |
| pheromone-tuner | 1.0 | +0.000 | 0/0 | 1.00000 | 20 | no detected difference |

## What the patterns actually did

The gate distinguishes three layers that should not be collapsed:

1. a pattern can change controller scores/probabilities;
2. that can change the ant's action/state path;
3. changed paths may or may not produce a sign-testable yield difference.

`cargo-return` and `hunger-coupling` changed controller telemetry in some
conditions but changed no action/state path at the tested scale. Their thresholds
were written for the deprecated CYBER ants; this is direct evidence that their
current calibration does not carry through the modern AIF choice path.

`white-space` changed action/state paths on 6/20, 9/20, and 10/20 boards as λ
rose. Mean action-count deltas versus paired `off` were:

| λ | forage | return | hold | pheromone | forage↔return thrash | yield-informative boards |
|---:|---:|---:|---:|---:|---:|---:|
| 0.1 | +17.05 | −17.20 | −0.10 | +0.25 | −0.25 | 1/20 |
| 0.5 | +13.40 | −17.10 | +2.35 | +3.15 | 0.00 | 3/20 |
| 1.0 | +23.55 | −16.75 | +10.90 | −15.20 | +0.10 | 5/20 |

This is broadly the stated white-space move—more scouting/foraging/holding and
less returning—but it is sparse, mixed in yield direction, and far from the
preregistered authority threshold.

`pheromone-tuner` changed action/state paths on 15/20, 16/20, and 17/20 boards.
It reduced pheromone actions by mean `0.95`, `5.55`, and `6.20` as λ increased,
mostly replacing them with forage (plus `1.60` hold at λ=1). That is exactly the
behavior of a pure penalty restricting inappropriate trail laying. Yield remained
bit-identical to `off` on all 20 boards at all three λ values. It moved behavior,
but it did not pass the preregistered yield-authority gate.

## Bias guard findings

Joe's expectation that “enlightened” should win is not supported here:

1. **CYBER-era thresholds do not transfer automatically.** Cargo-return and
   hunger-coupling did not alter the modern AIF action path at this scale.
2. **Pure penalties restrict rather than discover.** Pheromone-tuner visibly
   suppressed pheromone actions but created no yield difference; hunger-coupling
   did still less.
3. **Yield is not identical to the local pattern objective.** White-space and
   pheromone-tuner can do recognizable things “on the tin” without increasing
   food delivery. White-space's few yield changes were mixed and non-significant.

Authority is not merit, but this gate deliberately required authority in held-out
yield. That requirement was not met.

## Reproduction and validation

```sh
clojure -M scripts/ant_authority_gate.clj pattern 20 300
```

The runner writes its machine-readable result to
`/tmp/pattern-authority-gate.edn`. Focused tests cover ignition, exact-zero
controls, a nonzero triggering fixture for every real pattern, and the two-sided
tie-aware sign test.
