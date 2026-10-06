# Extended outer diamond: validation, structure, and scores — 2026-10-06

Reproduce from `/home/joe/code/futon2` in a fresh process:

```sh
clojure -M scripts/extended_four_numbers.clj
```

futon2 HEAD before generation: `e54e30df3ed729877dc1563db9d11db0b8078680`. The proposal is read unchanged from `holes/labs/wm-contract/proposals/meta-outer-policy-cascade-extended-proposal.edn`; the control is read from `/home/joe/code/futon3/library/meta/meta-outer-policy-cascade.edn`.

## 1. Real contract validator

The exact returned values from `#'futon2.aif.meta-outer-policy/contract-errors` were:

```clojure
proposal => []
original => []
```

The validator accepted the proposal. In particular, its current checks do not reject the extra top-level `:tokens`, the extra `:generative-model :conditioning`, or the added `:operator-demand` outcome; acceptance here records the validator's present boundary, not a ruling on those additions. The original control also returned `[]`.

## 2. Precedence structure

The ten-edge proposal has **120 linear extensions** (computed by subset DP over 7 units), with ordering term `log2(120) = 6.906890595609`.

### ConstructionReceipt direction: common descendant

Here an incomparable pair meets at the common descendant that precedes every other common descendant.

| x | y | meet |
|---|---|---|
| `:meta/read-the-item-sheet` | `:meta/read-revealed-attention` | `:meta/minimise-g-over-filled-meta-policies` |
| `:meta/read-the-item-sheet` | `:meta/read-library-coupling` | `:meta/minimise-g-over-filled-meta-policies` |
| `:meta/read-the-item-sheet` | `:meta/fill-meta-policy-slots` | `:meta/minimise-g-over-filled-meta-policies` |
| `:meta/read-the-item-sheet` | `:meta/injury-routes-to-self-heal` | `:meta/minimise-g-over-filled-meta-policies` |
| `:meta/read-revealed-attention` | `:meta/read-library-coupling` | `:meta/minimise-g-over-filled-meta-policies` |
| `:meta/read-revealed-attention` | `:meta/fill-meta-policy-slots` | `:meta/minimise-g-over-filled-meta-policies` |
| `:meta/read-revealed-attention` | `:meta/injury-routes-to-self-heal` | `:meta/minimise-g-over-filled-meta-policies` |
| `:meta/read-library-coupling` | `:meta/fill-meta-policy-slots` | `:meta/minimise-g-over-filled-meta-policies` |
| `:meta/read-library-coupling` | `:meta/injury-routes-to-self-heal` | `:meta/minimise-g-over-filled-meta-policies` |
| `:meta/fill-meta-policy-slots` | `:meta/injury-routes-to-self-heal` | `:meta/minimise-g-over-filled-meta-policies` |

### CascadeOrder direction: common ancestor

Here an incomparable pair meets at the common ancestor that follows every other common ancestor.

| x | y | meet |
|---|---|---|
| `:meta/read-the-item-sheet` | `:meta/read-revealed-attention` | `:meta/observe-the-meta-field` |
| `:meta/read-the-item-sheet` | `:meta/read-library-coupling` | `:meta/observe-the-meta-field` |
| `:meta/read-the-item-sheet` | `:meta/fill-meta-policy-slots` | `:meta/observe-the-meta-field` |
| `:meta/read-the-item-sheet` | `:meta/injury-routes-to-self-heal` | `:meta/observe-the-meta-field` |
| `:meta/read-revealed-attention` | `:meta/read-library-coupling` | `:meta/observe-the-meta-field` |
| `:meta/read-revealed-attention` | `:meta/fill-meta-policy-slots` | `:meta/observe-the-meta-field` |
| `:meta/read-revealed-attention` | `:meta/injury-routes-to-self-heal` | `:meta/observe-the-meta-field` |
| `:meta/read-library-coupling` | `:meta/fill-meta-policy-slots` | `:meta/observe-the-meta-field` |
| `:meta/read-library-coupling` | `:meta/injury-routes-to-self-heal` | `:meta/observe-the-meta-field` |
| `:meta/fill-meta-policy-slots` | `:meta/injury-routes-to-self-heal` | `:meta/observe-the-meta-field` |

## 3. Existing-scorer measurements

All edges, including `read-library-coupling → minimise`, are passed as `:precedes` in the extended run. Ordering is reported separately and is not added to G.

| arrangement | status | G | risk | ambiguity | information gain | P(all done at horizon) | horizon | linear extensions | ordering term |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| A. original diamond (control) | `:computed` | 1.279742336087 | 1.586595155529 | 0.000000000000 | 0.306852819443 | 0.203125000000 | 4 | 2 | 1.000000000000 |
| B. extended seven-unit shape | `:computed` | 0.732060845075 | 1.038913664518 | 0.000000000000 | 0.306852819443 | 0.522627823055 | 7 | 120 | 6.906890595609 |
| C. extended bag | `:computed` | 0.238107431451 | 0.544960250894 | 0.000000000000 | 0.306852819443 | 0.946577678756 | 7 | 5040 | 12.299208018387 |
| D. extended chain | `:computed` | 1.310492019077 | 1.617344838520 | 0.000000000000 | 0.306852819443 | 0.007812500000 | 7 | 1 | 0.000000000000 |

### Theta by arrangement

- A. original diamond (control): `meta/fill-meta-policy-slots` = 1/2 (`:no-recorded-trials`)<br>`meta/injury-routes-to-self-heal` = 1/2 (`:no-recorded-trials`)<br>`meta/minimise-g-over-filled-meta-policies` = 1/2 (`:no-recorded-trials`)<br>`meta/observe-the-meta-field` = 1/2 (`:no-recorded-trials`)

- B. extended seven-unit shape: `meta/fill-meta-policy-slots` = 1/2 (`:no-recorded-trials`)<br>`meta/injury-routes-to-self-heal` = 1/2 (`:no-recorded-trials`)<br>`meta/minimise-g-over-filled-meta-policies` = 1/2 (`:no-recorded-trials`)<br>`meta/observe-the-meta-field` = 1/2 (`:no-recorded-trials`)<br>`meta/read-library-coupling` = 1/2 (`:no-recorded-trials`)<br>`meta/read-revealed-attention` = 1/2 (`:no-recorded-trials`)<br>`meta/read-the-item-sheet` = 1/2 (`:no-recorded-trials`)

- C. extended bag: `meta/fill-meta-policy-slots` = 1/2 (`:no-recorded-trials`)<br>`meta/injury-routes-to-self-heal` = 1/2 (`:no-recorded-trials`)<br>`meta/minimise-g-over-filled-meta-policies` = 1/2 (`:no-recorded-trials`)<br>`meta/observe-the-meta-field` = 1/2 (`:no-recorded-trials`)<br>`meta/read-library-coupling` = 1/2 (`:no-recorded-trials`)<br>`meta/read-revealed-attention` = 1/2 (`:no-recorded-trials`)<br>`meta/read-the-item-sheet` = 1/2 (`:no-recorded-trials`)

- D. extended chain: `meta/fill-meta-policy-slots` = 1/2 (`:no-recorded-trials`)<br>`meta/injury-routes-to-self-heal` = 1/2 (`:no-recorded-trials`)<br>`meta/minimise-g-over-filled-meta-policies` = 1/2 (`:no-recorded-trials`)<br>`meta/observe-the-meta-field` = 1/2 (`:no-recorded-trials`)<br>`meta/read-library-coupling` = 1/2 (`:no-recorded-trials`)<br>`meta/read-revealed-attention` = 1/2 (`:no-recorded-trials`)<br>`meta/read-the-item-sheet` = 1/2 (`:no-recorded-trials`)

## Reading against the 2026-10-05 four-number result

The earlier four-node measurement ordered G as bag (0.392913018968) < diamond (1.279742336087) < either chain (1.516585504479). The control below reproduces the diamond value. In the seven-unit measurement, the measured table similarly exposes how earlier co-application changes risk and all-done probability: the bag enables all seven immediately, the extended diamond enables five middle readings together after observe, and the chain enables one unit at a time. The ordering-term column is descriptive only and does not change any reported G.
