# R-a instrumented decision log

This is an internal measurement, not an arm comparison. It ran one seeded
patchy 10x10 world for 300 ticks with the density-scaled probe's first 10x10
seed triple. The selector hook was opt-in; normal runs do not install it.

Command:

```sh
clojure -M holes/labs/ants-faithfulness/r-a-decision-log.clj
```

Configuration: `{:food-seed 202621110, :move-seed 202621111,
:choice-seed 202671110, :size [10 10], :ticks 300, :metabolism 0.06,
:initial-reserves 0.5, :ants-per-side 3,
:food-opts {:num-patches 4 :patch-radius 2}}`.

The run captured 900 decisions. Five had zero selection margin. Contributions
below are weighted by the effective lambdas. Selection minimizes `G`, so the
positive margin used for ratios is `G_runner-up - G_winner`; the requested
winner-minus-runner quantity is the corresponding negative value.

| quantity | min | p25 | median | p75 | p90 | p95 | p99 | max | mean |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| risk spread | 0 | 15.1877 | 27.5418 | 27.9625 | 28.4989 | 28.4989 | 38.5194 | 104.1905 | 21.7346 |
| ambiguity spread | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| epistemic spread | 0 | 0.45 | 0.45 | 0.45 | 0.45 | 0.45 | 0.45 | 0.45 | 0.44188 |
| info spread | 0 | 0 | 0 | 0.0000649 | 0.0003565 | 0.0011696 | 0.0095238 | 0.0666667 | 0.0005244 |
| selection margin | 0 | 14.6239 | 28.1018 | 28.5996 | 29.0660 | 29.0660 | 39.8594 | 91.6736 | 21.7668 |

Ratios exclude the five zero-margin decisions:

| ratio | min | p25 | median | p75 | p90 | p95 | p99 | max | mean |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| ambiguity spread / margin | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| epistemic spread / margin | 0 | 0.0157345 | 0.0157831 | 0.0307716 | 0.0330275 | 0.0330275 | 0.0330288 | 0.0332312 | 0.0230521 |

## Finding

The attempted spatial food-variance path is mechanically action-indexed, but
canonical ambiguity remained exactly candidate-constant in every live decision
sampled here. The earlier “varying but dominated” diagnosis is therefore not
observed on the live path: ambiguity is still structurally constant at the
selector boundary. Directed EIG is genuinely varying but dominated—typically
about 63 times smaller than the selection margin and never more than 3.33% of
it in this run. Nothing downstream is discarding a contribution large enough
to cross a margin.

## Sensorium slice

The follow-up slice adds fixed NW/N/NE/W/self/E/SW/S/SE food and pheromone
fields. They are carried as 18 scalar predictive channels rather than reduced
to a neighbourhood mean. Candidate actions already predicted distinct
locations before this slice (median 2, maximum 3), so no directional action
vocabulary was added: `:forage` now explicitly reads the directional field,
`:return` remains home-directed, and each candidate's shifted field is scored.

Re-running the same producer after the slice gave:

| quantity | min | p25 | median | p75 | p90 | p95 | p99 | max | mean |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| risk spread | 0 | 15.1877 | 27.5418 | 27.9625 | 28.4989 | 28.4989 | 38.5194 | 104.1905 | 21.7346 |
| ambiguity spread | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 2.16778 | 0.0048173 |
| epistemic spread | 0 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.184145 | 0.0987161 |
| info spread | 0 | 0 | 0 | 0.0000649 | 0.0003565 | 0.0011696 | 0.0095238 | 0.0666667 | 0.0005244 |
| selection margin | 0 | 14.2739 | 28.4518 | 28.9496 | 29.4160 | 29.4160 | 39.8594 | 91.5469 | 21.7710 |

| ratio | min | p25 | median | p75 | p90 | p95 | p99 | max | mean |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| ambiguity spread / margin | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0.0487417 | 0.0001089 |
| epistemic spread / margin | 0 | 0.0034543 | 0.0034735 | 0.0070058 | 0.0075329 | 0.0075329 | 0.0075333 | 0.0075806 | 0.0051977 |

Both required spreads are now nonzero and non-degenerate across decisions.
Ambiguity variation is sparse—zero through p99, with a nonzero maximum—while
directed EIG varies routinely. Neither approaches the selection margin; no
lambda was changed.

Runner analogy: the fixed directional map corresponds to preserving
per-candidate evidence rather than collapsing it into one aggregate feature.
Unlike a runner, the ant's evidence is a literal spatial Moore neighbourhood.

Next-slice flag: the existing `deposit-food` path teleports an ant home when its
squared home distance is at most 4. A future cache/drop-food slice must address
that separately; this slice does not alter deposition.
