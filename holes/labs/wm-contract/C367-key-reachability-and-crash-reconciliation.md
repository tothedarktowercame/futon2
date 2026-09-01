# C367 — key reachability and crash-window reconciliation

Date: 2026-09-01. Fixture-only repair; no writer was parked or restored.

Two C357 claims were incomplete:

- HMAC key content was validated but a `0644` key was accepted. `read_key` now
  opens without following symlinks, inspects that same descriptor, and requires
  a regular file owned by the effective user with no group/world permission
  bits. The named refusal is `manifest-key-not-owner-only`.
- an inverse could succeed and crash while appending its outcome. Restoration
  now fsyncs an independently HMAC-authenticated, fence-bound
  `inverse-attempt-recorded` row before executing. On
  retry, restored reality plus that exact attempt reconciles the missing
  outcome; restored reality without an attempt remains
  `restored-state-without-inverse-attempt`.

The tool reports rather than closes the remaining boundary on every CLI
verdict, including refusals:

```text
residual-limitation=
compare-before-act-narrows-race-but-does-not-prove-event-freedom
```

Focused control:

```sh
python3 -m unittest -v test_writer_fence_restore.py
```
