# Slice-2a v2 Mission-Conditioned Recovery

Standalone exploratory DERIVE lab. V2 reward is mission-conditioned: `alpha * rel(mission,p) + beta * bonus(p)`. `bonus` is fit TRAIN-only; `rel` is 1-2/3-hop phylogeny propagation from cosine seed patterns. Held-out `:applied` is never inserted into the pool.

## Headline

- N held-out missions: 81
- Seeds: 20260710, 20260711
- Ratios alpha:beta: 1.0:0.0, 1.0:0.25, 1.0:0.5, 1.0:1.0, 0.0:1.0
- steps/K/max_len: 6 / 12 / 5
- hops/decay/pool_top_n: 3 / 0.35 / 240

| condition | mean recall@|applied| |
|---|---:|
| rel+aliveness | 3.9% |
| rel-only | 3.6% |
| aliveness-only | 2.7% |
| popularity-only | 8.9% |
| random expected | 2.6% |
| reachability ceiling | 94.4% |
| shuffle-null rel+aliveness | 3.3% |

## Marginal Aliveness Readout

- Real rel+alive minus rel-only recovery gap: 0.3%
- Shuffle-null rel+alive minus rel-only recovery gap: -0.3%
- Proposal quality (sum TRAIN bonus): rel+alive 2.136 vs rel-only 0.061

## Ratio / Seed Table

| labels | alpha | beta | seed | recall | rel-rank | popularity | random | proposal bonus |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| rel-only | 1.0 | 0.0 | 20260710 | 3.6% | 9.3% | 8.9% | 2.6% | 0.145 |
| rel-only | 1.0 | 0.0 | 20260711 | 3.7% | 9.3% | 8.9% | 2.6% | -0.024 |
| rel+aliveness | 1.0 | 0.25 | 20260710 | 4.3% | 9.3% | 8.9% | 2.6% | 1.114 |
| rel+aliveness | 1.0 | 0.25 | 20260711 | 4.1% | 9.3% | 8.9% | 2.6% | 1.069 |
| rel+aliveness | 1.0 | 0.5 | 20260710 | 4.1% | 9.3% | 8.9% | 2.6% | 2.055 |
| rel+aliveness | 1.0 | 0.5 | 20260711 | 3.4% | 9.3% | 8.9% | 2.6% | 1.924 |
| rel+aliveness | 1.0 | 1.0 | 20260710 | 3.9% | 9.3% | 8.9% | 2.6% | 3.240 |
| rel+aliveness | 1.0 | 1.0 | 20260711 | 3.8% | 9.3% | 8.9% | 2.6% | 3.417 |
| aliveness-only | 0.0 | 1.0 | 20260710 | 2.6% | 9.3% | 8.9% | 2.6% | 2.498 |
| aliveness-only | 0.0 | 1.0 | 20260711 | 2.9% | 9.3% | 8.9% | 2.6% | 2.671 |
| shuffle-null | 1.0 | 0.25 | 20260710 | 3.2% | 9.3% | 8.9% | 2.6% | -0.322 |
| shuffle-null | 1.0 | 0.25 | 20260711 | 3.3% | 9.3% | 8.9% | 2.6% | -0.562 |
| shuffle-null | 1.0 | 0.5 | 20260710 | 3.0% | 9.3% | 8.9% | 2.6% | 0.331 |
| shuffle-null | 1.0 | 0.5 | 20260711 | 3.6% | 9.3% | 8.9% | 2.6% | 0.238 |
| shuffle-null | 1.0 | 1.0 | 20260710 | 4.2% | 9.3% | 8.9% | 2.6% | 1.182 |
| shuffle-null | 1.0 | 1.0 | 20260711 | 2.8% | 9.3% | 8.9% | 2.6% | 1.211 |

## Verdict

HONEST NULL / NO PASS. Success requires rel+aliveness > rel-only and popularity, stable across ratios/seeds, and the rel+alive minus rel-only gap destroyed by label shuffle.

Full machine-readable rows are in `slice2_v2_recovery_results.json`.
