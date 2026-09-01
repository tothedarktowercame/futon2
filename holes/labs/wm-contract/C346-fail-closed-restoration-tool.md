# C346 — fail-closed writer-fence restoration

Date: 2026-09-01. The implementation and controls used fixtures only. No live
writer was parked, resumed, re-armed, started, stopped, or reconfigured.

Canonical tool: `scripts/writer_fence_restore.py`.

- `capture` atomically writes a digest-bound JSON manifest containing each
  coordinator's class, durable status, tick claim, runtime/watchdog scheduler
  presence, and each systemd unit's enabled and active states.
- `record` derives the only allowed inverse from the manifest, independently
  observes that the park actually happened, and then appends one fsynced JSONL
  action. Failed/unobserved parks create no row.
- `restore` validates every row's schema, manifest digest, ordinal, unique
  target, typed action, captured pre-state, and current parked state before any
  mutation. It then executes exactly the recorded prefix in reverse order.

The executable class boundary is:

- captured/current terminal `:complete`, watchdog changed live→parked:
  `rearm-terminal-coordinator` → `start-registered!`;
- captured `:running`, current disabled/witnessed `:stopped`:
  `resume-coordinator` → `resume!`;
- captured active/activating unit, current inactive:
  `start-unit` → `systemctl --user start`.

No fallback guesses a verb. A missing, duplicate, reordered, hand-edited,
foreign-manifest, wrong-class, or current-state-mismatched row refuses before
the first inverse.

Fixture controls (`python3 -m unittest -v test_writer_fence_restore.py`) prove:

1. a journal claiming an unperformed park is rejected;
2. a swapped coordinator verb and contradictory current state are rejected;
3. a mixed two-action partial prefix restores only those two actions, in exact
   reverse order, without touching the unparked unit;
4. `record` creates no journal after an unobserved park and appends exactly one
   row after the parked state is observed.

C319 now invokes the tool for capture, post-success records, ordinary restore,
and Joe's standalone emergency restore. Other independently reviewed C319
findings are outside this change.
