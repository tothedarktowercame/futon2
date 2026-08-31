# CLEANUP C31 — variance update (2026-08-31)

`beliefUpdate` now takes the C32 inputs as parameters: learning rate,
sensor-noise floor, and a per-channel optional evidence weight. No C32 number is
hard-coded into the law.

For known provenance `some w`, the effective EMA rate is `αw`:

```
variance' = (1 - αw) variance + αw (predictionError² + sensorNoiseFloor)
```

The same weight scales the mean correction. Learning rate and supplied weights
must lie in `[0,1]`. For unknown provenance, represented by `none`, both mean and
variance pass through unchanged. Thus the recorded `:unknown` absence never
acquires a numeric default inside Lean.

The positive fixture starts with variance 2 and reaches variance 1 under a
full-weight observation. Two independent mutation controls now fail:

- replacing the theorem that rejects the unchanged prior makes Lean reject an
  inert mean update;
- replacing the theorem that rejects a corrected mean paired with unchanged
  variance makes Lean reject precision-unresponsive variance.

Canonical invocations from the futon2 root:

```sh
bb checks/belief_update_check.clj
bb checks/belief_update_check.clj --negative
bb checks/belief_update_check.clj --negative-variance
```

All three return zero only when the positive fixture compiles and each mutation
is rejected. Exit 1 is an ordinary checker failure; exit 2 means a mutation
slipped through.
