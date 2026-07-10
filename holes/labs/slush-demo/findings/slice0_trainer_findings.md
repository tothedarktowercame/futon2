# Slice-0 — Trusted GFN trainer harness (gates G0a / G0b)

Pure-numpy tabular GFlowNet trained by trajectory balance on synthetic
submodular-coverage missions small enough to enumerate exactly. This
validates the *trainer* (F1 undertraining, F2 shared-logZ) before the
mission reward is trusted. See `holes/TN-gflownets-fable-review.md`.

## Config

- n items / max_len: 10 / 3  (176 terminal sets, 821 trajectories enumerated)
- full-batch iters / lr: 700 / 0.05
- gates: TV < 0.05, |logZ−true| < 0.1 nat
- missions (mid, n_atoms, beta): [('M-small', 4, 0.35), ('M-mid', 6, 0.6), ('M-large', 9, 0.9), ('M-xl', 12, 1.2)]

## True log-partition spread (F2 precondition)

- Z(m) spread across missions: **5.64 nats** (need ≥ 3 to stress a shared scalar logZ)

| mission | true logZ |
|---|---:|
| M-small | 6.086 |
| M-mid | 7.125 |
| M-large | 10.452 |
| M-xl | 11.724 |

## Gate G0a — conditional logZ(m) recovers the exact Gibbs sampler

- max TV to exact Gibbs: **0.0125** (gate 0.05)
- mean TV: 0.0047
- max |learned logZ − true logZ|: **0.0289** nat (gate 0.1)

| mission | TV | learned logZ | true logZ | logZ err |
|---|---:|---:|---:|---:|
| M-small | 0.0011 | 6.087 | 6.086 | 0.0003 |
| M-mid | 0.0012 | 7.123 | 7.125 | 0.0011 |
| M-large | 0.0041 | 10.441 | 10.452 | 0.0111 |
| M-xl | 0.0125 | 11.696 | 11.724 | 0.0289 |

## Gate G0b — a shared scalar logZ is mis-specified (reproduces F2)

Same policy capacity, only logZ collapsed to one global scalar.

- shared-arm max TV: **0.9865** (must be ≥ 0.05 to demonstrate the bug)
- shared-arm mean TV: 0.6635

| mission | TV (shared logZ) | TV (conditional logZ) |
|---|---:|---:|
| M-small | 0.9865 | 0.0011 |
| M-mid | 0.9692 | 0.0012 |
| M-large | 0.5778 | 0.0041 |
| M-xl | 0.1204 | 0.0125 |

## Verdict

- **G0a (trainer correctness): PASS**
- **G0b (F2 shared-logZ bug demonstrated + fixed): PASS**

G0a passing means the objective + conditional logZ + uniform-P_B set
handling are correct: the trained sampler reproduces the enumerated
Gibbs distribution and recovers the true log-partition. G0b passing
means the shared-scalar-logZ variant provably fails on missions with
different Z — F2 is real, and conditional logZ(m) fixes it. This gate
is now auditable forever.
