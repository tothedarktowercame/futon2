# C357 — authenticated, fence-bound, retryable restoration

Date: 2026-09-01. Fixture-only change; no writer was parked or restored.

The v1 restoration authority admitted five false claims. V2 closes them at the
protocol boundary:

- the manifest is HMAC-SHA256 authenticated with a locally held key and target
  identities have fixed allowed classes, so recomputing an unkeyed digest
  cannot turn the terminal watchdog into a resumable coordinator;
- manifest, action journal, and outcome ledger all carry the exact fence ID;
- missing/empty journals and zero-target manifests refuse as
  `NOTHING-RECORDED`/`manifest-zero-targets`, never successful no-ops;
- each successful inverse is fsynced to an append-only outcome ledger. Retry
  accepts only its exact reverse-prefix, re-observes already restored targets,
  and continues with the remaining actions;
- structural validation does not cache mutable state. Each pending target is
  observed immediately before its inverse and again before success is recorded.

Canonical focused control:

```sh
python3 -m unittest -v test_writer_fence_restore.py
```

The authentication key is deliberately absent from every manifest and journal.
The operator supplies it with `--key-file`; a missing, short, non-regular,
foreign-owned, group-readable, or world-readable key fails before capture,
record, or restore. C319 names the canonical key path, attempt ledger, and
outcome ledger. Existing v1 manifests are intentionally not accepted by v2.

Every inverse now has a durable attempt record written before execution. A
restored target with a missing outcome can be reconciled only when that exact
fence-bound attempt exists; restored reality without an attempt still refuses.

Residual limitation: compare-before-act narrows the validation/execution race
and the postcondition proves final state, but neither proves event-freedom in
the intervening instant. `restore` returns this limitation in its
machine-readable `residual-limitation` field.
