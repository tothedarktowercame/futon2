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
The operator supplies it with `--key-file`; a missing or short key fails before
capture, record, or restore. C319 names the canonical key path and outcome
ledger. Existing v1 manifests are intentionally not accepted by v2.
