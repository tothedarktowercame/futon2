# C346 — fail-closed writer-fence restoration

Date: 2026-09-01. The implementation and controls used fixtures only. No live
writer was parked, resumed, re-armed, started, stopped, or reconfigured.

Canonical tool: `scripts/writer_fence_restore.py`.

- `capture` atomically writes an HMAC-authenticated JSON manifest containing each
  coordinator's class, durable status, tick claim, runtime/watchdog scheduler
  presence, and each systemd unit's enabled and active states.
- `record` derives the only allowed inverse from the manifest, independently
  observes that the park actually happened, and then appends one fsynced JSONL
  action. Failed/unobserved parks create no row.
- `restore` binds the authenticated manifest, every journal row, and every
  outcome to the caller-supplied fence ID. It validates schema, ordinal, unique
  target, typed action, and captured pre-state, then re-observes each target
  immediately before its inverse. Successful inverses are appended to a
  fsynced outcome ledger; retry verifies and skips that reverse-prefix.

The executable class boundary is:

- captured/current terminal `:complete`, watchdog changed live→parked:
  `rearm-terminal-coordinator` → `start-registered!`;
- captured `:running`, current disabled/witnessed `:stopped`:
  `resume-coordinator` → `resume!`;
- captured active/activating unit, current inactive:
  `start-unit` → `systemctl --user start`.

No fallback guesses a verb. A missing, duplicate, reordered, hand-edited,
foreign-fence, wrong-class, or current-state-mismatched row refuses. A missing
or empty journal and a validly authenticated zero-target manifest report
`NOTHING-RECORDED`/refusal rather than successful restoration.

Fixture controls (`python3 -m unittest -v test_writer_fence_restore.py`) prove:

1. authentication laundering and a foreign fence identity are rejected;
2. empty journals and zero-target manifests cannot report success;
3. a journal claiming an unperformed park, a swapped coordinator verb, and a
   contradictory current state are rejected;
4. a mixed two-action partial prefix restores only those two actions, in exact
   reverse order, without touching the unparked unit;
5. a failure after one inverse is resumable: retry verifies the outcome and
   executes only the remaining inverse;
6. state changed after validation is caught by the immediate pre-inverse
   observation; and
7. `record` creates no journal after an unobserved park and appends exactly one
   row after the parked state is observed.

C319 now invokes the tool for capture, post-success records, ordinary restore,
and Joe's standalone emergency restore. Other independently reviewed C319
findings are outside this change.
