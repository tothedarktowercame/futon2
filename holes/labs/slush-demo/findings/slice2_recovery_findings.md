# Slice-2a Discharge-GFN Held-Out Recovery

Standalone exploratory DERIVE lab. Reward uses TRAIN-only `pattern_aliveness_reward.fit_credits(...)[1]` (`bonus`, alive-vs-mess log-odds). Held-out `:applied` is never added to the candidate pool.

## Headline

- N held-out missions: 81
- Seeds: 20260710, 20260711
- Step/K variants: 8/12, 12/12, 12/24
- Max selected patterns per trajectory: 5
- Reachable ceiling from retrieval+one-hop pool: 39.2%

| condition | mean recall@|applied| |
|---|---:|
| discharge GFN | 1.2% |
| slice-1 reference | 25.0% |
| retrieval-prior beam | 1.0% |
| popularity-only | 9.1% |
| random expected | 2.9% |
| label-shuffle null GFN | 1.4% |

## Per-Seed Summary

| labels | steps | K | seed | GFN | retrieval | popularity | random |
|---|---:|---:|---:|---:|---:|---:|---:|
| real | 8 | 12 | 20260710 | 1.0% | 1.0% | 9.1% | 2.9% |
| real | 8 | 12 | 20260711 | 1.3% | 1.0% | 9.1% | 2.9% |
| real | 12 | 12 | 20260710 | 1.2% | 1.0% | 9.1% | 2.9% |
| real | 12 | 12 | 20260711 | 0.9% | 1.0% | 9.1% | 2.9% |
| real | 12 | 24 | 20260710 | 1.6% | 1.0% | 9.1% | 2.9% |
| real | 12 | 24 | 20260711 | 1.4% | 1.0% | 9.1% | 2.9% |
| shuffle-null | 8 | 12 | 20260710 | 1.0% | 1.0% | 9.1% | 2.9% |
| shuffle-null | 8 | 12 | 20260711 | 1.5% | 1.0% | 9.1% | 2.9% |
| shuffle-null | 12 | 12 | 20260710 | 1.4% | 1.0% | 9.1% | 2.9% |
| shuffle-null | 12 | 12 | 20260711 | 1.3% | 1.0% | 9.1% | 2.9% |
| shuffle-null | 12 | 24 | 20260710 | 1.8% | 1.0% | 9.1% | 2.9% |
| shuffle-null | 12 | 24 | 20260711 | 1.6% | 1.0% | 9.1% | 2.9% |

## Verdict

HONEST NULL / NO PASS. Acceptance requires GFN > 25%, > retrieval, > popularity, stable across seeds, and destroyed by label shuffle.

Full machine-readable rows are in `slice2_recovery_results.json`.
