# C375 — authenticated attempt events and uniform limitations

Date: 2026-09-01. Fixture-only repair; no writer was touched.

An attempt row previously copied the public manifest HMAC. That proved only
that its author could read the manifest. Each attempt row now has its own
`attempt-hmac-sha256`, computed over its complete content: schema, manifest
provenance, fence ID, ordinal, target, action, status, and timestamp.
`load_attempts` verifies that HMAC with the owner-only key before an attempt can
authorize reconciliation.

The three states remain distinct:

- authenticated attempt + parked target: execute the inverse;
- authenticated attempt + restored target: reconcile the lost outcome;
- missing or unauthenticated attempt + restored target: refuse.

The CLI constructs both success and refusal output through one envelope, which
always includes:

```text
residual-limitation=
compare-before-act-narrows-race-but-does-not-prove-event-freedom
```

Focused controls:

```sh
python3 -m unittest -v test_writer_fence_restore.py
```

They include a fabricated attempt carrying the readable manifest HMAC, a
genuine crash-window reconciliation, and both success/refusal envelopes.
