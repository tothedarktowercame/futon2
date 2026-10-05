# Outer-loop diamond: four arrangement scores — 2026-10-05

Reproduce in a fresh process from `/home/joe/code/futon2`:

```sh
clojure -M scripts/diamond_four_numbers.clj
```

futon2 HEAD measured: `34097791dfa967c44da057ba16c4411b28399b11`. The scorer is `futon2.aif.cascade-shape-g/score-arranged`; the rollout transition is `:co-application-frontier-theta-v1`. The ordering term is reported separately and was not supplied to the scorer.

## Scores

| arrangement | status | G | risk | ambiguity | information gain | horizon | P(all done at horizon) | log2(linear extensions) | G + ordering term |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| A. diamond | `:computed` | 1.279742336087 | 1.586595155529 | 0.000000000000 | 0.306852819443 | 4 | 0.203125000000 | 1.000000000000 (2) | 2.279742336087 |
| B. chain-1 | `:computed` | 1.516585504479 | 1.823438323922 | 0.000000000000 | 0.306852819443 | 4 | 0.062500000000 | 0.000000000000 (1) | 1.516585504479 |
| C. chain-2 | `:computed` | 1.516585504479 | 1.823438323922 | 0.000000000000 | 0.306852819443 | 4 | 0.062500000000 | 0.000000000000 (1) | 1.516585504479 |
| D. bag | `:computed` | 0.392913018968 | 0.699765838411 | 0.000000000000 | 0.306852819443 | 4 | 0.772476196289 | 4.584962500721 (24) | 4.977875519690 |

## Pattern theta

- A. diamond: `meta/fill-meta-policy-slots` = 1/2 (`:no-recorded-trials`)<br>`meta/injury-routes-to-self-heal` = 1/2 (`:no-recorded-trials`)<br>`meta/minimise-g-over-filled-meta-policies` = 1/2 (`:no-recorded-trials`)<br>`meta/observe-the-meta-field` = 1/2 (`:no-recorded-trials`)

- B. chain-1: `meta/fill-meta-policy-slots` = 1/2 (`:no-recorded-trials`)<br>`meta/injury-routes-to-self-heal` = 1/2 (`:no-recorded-trials`)<br>`meta/minimise-g-over-filled-meta-policies` = 1/2 (`:no-recorded-trials`)<br>`meta/observe-the-meta-field` = 1/2 (`:no-recorded-trials`)

- C. chain-2: `meta/fill-meta-policy-slots` = 1/2 (`:no-recorded-trials`)<br>`meta/injury-routes-to-self-heal` = 1/2 (`:no-recorded-trials`)<br>`meta/minimise-g-over-filled-meta-policies` = 1/2 (`:no-recorded-trials`)<br>`meta/observe-the-meta-field` = 1/2 (`:no-recorded-trials`)

- D. bag: `meta/fill-meta-policy-slots` = 1/2 (`:no-recorded-trials`)<br>`meta/injury-routes-to-self-heal` = 1/2 (`:no-recorded-trials`)<br>`meta/minimise-g-over-filled-meta-policies` = 1/2 (`:no-recorded-trials`)<br>`meta/observe-the-meta-field` = 1/2 (`:no-recorded-trials`)

## Per-step preference schedule

The schedule keys are `[completed-pattern-count want-met?]`; probabilities are normalized preferences.

- A. diamond: τ=1: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289<br>τ=2: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289<br>τ=3: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289<br>τ=4: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289

- B. chain-1: τ=1: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289<br>τ=2: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289<br>τ=3: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289<br>τ=4: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289

- C. chain-2: τ=1: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289<br>τ=2: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289<br>τ=3: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289<br>τ=4: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289

- D. bag: τ=1: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289<br>τ=2: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289<br>τ=3: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289<br>τ=4: [0 false]=0.001389456790, [0 true]=0.010266774166, [1 false]=0.003776935143, [1 true]=0.027907985653, [2 false]=0.010266774166, [2 true]=0.075861770270, [3 false]=0.027907985653, [3 true]=0.206213671600, [4 false]=0.075861770270, [4 true]=0.560546876289

## Reading

All four arrangements use the same four-node, four-step progress preference, so their different G values come from when the frontier kernel can co-apply enabled patterns. The bag exposes all four nodes at the first step; the diamond exposes observe, then fill and injury together, then minimise; each chain exposes one node per frontier. Reading the measured values from lowest to highest G gives **D. bag** < **A. diamond** < **B. chain-1** = **C. chain-2**. The identical preference schedule rewards earlier completed-pattern mass at every step; risk and ambiguity reflect each arrangement's rollout under that schedule, while the reported information-gain subtraction uses the same per-pattern theta evidence.

**Does the bag score best on G alone?** Yes. Its G is 0.392913018968.
