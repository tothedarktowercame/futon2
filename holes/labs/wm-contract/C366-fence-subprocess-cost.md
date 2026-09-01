# C366 — writer-fence subprocess cost

## Measured cost

All commands ran through the bounded testing service with repository basis
capture (`--dir /home/joe/code/futon2`). The evidence-side measurements used a
structurally complete but deliberately stale receipt; it exercises the same
live `writer_fence_evidence.py` observation and correctly ends unverified.

| Step | No evidence | Evidence path | Delta |
|---|---:|---:|---:|
| WM preflight | 1.63 s | 3.81 s | +2.18 s |
| contract authority | 0.02 s | 2.24 s | +2.22 s |

All four receipts reported `:resource-status :clean`. The evidence preflight
correctly exited inner 1 / outer 125 because the measurement receipt was stale
and the live world was unfenced; the timing is not a positive-fence claim.

The current unfenced workspace gate took 312.00 seconds over 124 checks. Its
basis moved and `mutable-verdict-claims` was concurrently red, so this is a cost
measurement, not a correctness verdict. It is not directly comparable to
C290's older 216-second, smaller gate.

## Invocation count

Static call accounting finds:

- preflight: one fence subprocess;
- standalone contract-authority check: one;
- workspace gate: **three** — positive contract authority, negative contract
  authority, and the outer gate verdict;
- mutable read-set: one only when a caller explicitly asks for an event claim
  with fence coordinates; current content-only consumers supply none.

Thus the current gate adds approximately `3 × 2.2 = 6.6` seconds when fence
evidence is transported. It is proportional to operator steps, not repository
observations, but two of the three gate observations are redundant: the inner
authority invocations establish content claims while the outer gate owns the
event interval.

## Window estimate and caching

Against C290's 6 min 24 s preparation estimate, current code should reserve
about **6 min 35 s** if preflight, standalone authority, and the gate are all
run: `384 + 2.18 + 2.22 + 6.66 ≈ 395 s`. If authority is consumed only through
the gate, the estimate is about **6 min 33 s**.

The verified result must not be cached across operator steps: doing so would
turn a past observation back into a bearer claim and weaken C363. Within the
gate, the honest optimization is not a cache. Run the two contract-authority
constituents content-only and let the outer gate perform the single event-fence
assessment. That would reduce the gate increment to about 2.2 seconds without
sharing an in-process authority. This is a recommendation only; no behavior was
changed in C366.

## Canonical measurement form

```sh
python3 scripts/run_workspace_gate_bounded.py \
  --label c366-preflight-unfenced \
  --command '/usr/bin/time -p clojure -M:wm-preflight'

python3 scripts/run_workspace_gate_bounded.py \
  --label c366-authority-unfenced \
  --command '/usr/bin/time -p bb -cp . checks/contract_authority_current.clj'

python3 scripts/run_workspace_gate_bounded.py \
  --label c366-gate-unfenced \
  --command '/usr/bin/time -p bb -cp . checks/wm_workspace_gate.clj'
```

Evidence-path variants use `c366-measure` and
`test/fixtures/c366-stale-fence-receipt.json`. No positive fence was claimed.
