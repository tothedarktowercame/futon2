# S2 cache/relay capability probe

Frozen before the run: 24×24 patchy world, 720 ticks, 23 density-scaled radius-2 patches, metabolism 0.06, initial reserves 0.5, three ants, five paired seed triples. Dropped food is ordinary cell food; provenance is a parallel measurement ledger. No EFE lambda or drop bonus was added.

Seed triples `(food, move, choice)`: `(202612110, 202612111, 202662110)`, `(202612112, 202612113, 202662111)`, `(202612114, 202612115, 202662112)`, `(202612116, 202612117, 202662113)`, `(202612118, 202612119, 202662114)`.

| condition | mean yield | starvation | cache drops | cross-ant pickups | completed relays |
|---|---:|---:|---:|---:|---:|
| aif-drop-disabled | 363.4400 | 0.4000 | 0 | 0 | 0 |
| aif-drop-enabled | 471.9400 | 0.2000 | 16 | 0 | 0 |
| classic-drop-disabled | 453.7400 | 0.3333 | 0 | 0 | 0 |
| classic-drop-enabled | 453.7400 | 0.3333 | 0 | 0 | 0 |

Paired enabled−disabled differences (mean, two-sided 95% t interval):

- aif-yield: 108.5000 [-199.0282, 416.0282]
- aif-starvation: -0.2000 [-0.7552, 0.3552]
- classic-yield: 0.0000 [0.0000, 0.0000]
- classic-starvation: 0.0000 [0.0000, 0.0000]

**Verdict:** carrying has a visible model cost and AIF selected 16 cache drops, but no other ant picked one up and no relay completed. The capability claim is therefore false in this probe. Enabling drop changed mean AIF yield by 108.5000 (95% CI [-199.0282, 416.0282]) and starvation by -0.2000 (95% CI [-0.7552, 0.3552]); both intervals include zero. Classic never selected drop and its enabled/disabled records were identical, so it did not benefit equally—but the zero-relay result prevents any AIF capability interpretation.

The existing within-distance-4 return teleport remains unchanged; cache drops were admitted only away from home and onto cells containing less than 0.10 food.
