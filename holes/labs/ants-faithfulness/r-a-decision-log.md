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
