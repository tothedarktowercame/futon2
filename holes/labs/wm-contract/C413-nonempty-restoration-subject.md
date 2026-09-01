# C413 — `restored` requires an active restoration subject

Date: 2026-09-01.

The state machine now distinguishes two empty-subject failures:

- `restoration-manifest-population-incomplete:capture-invalid` means the
  authenticated manifest does not contain the complete enforced coordinator
  and systemd-unit population. C395's one-known-inactive-unit fixture reaches
  this refusal.
- `restoration-not-required:no-active-writers-recorded` means the manifest is
  population-complete, but its captured pre-state records no active writer.
  That can be a valid observation, but it is not a fence that required
  restoration and cannot satisfy the `restored` transition.

The active projection is semantic per target class: terminal coordinator
watchdog present, running coordinator durably running and enabled, or systemd
unit active/activating. It must be nonempty before journal and outcome
population equality can establish restoration.

The empty-subject lint missed this site because it is deliberately a catalogue
of registered acceptance boundaries, not general Python data-flow analysis.
It covered the manifest and journal inside `writer_fence_restore.py`; the
helper-derived active projection in `wm_quiet_run_state.py` was not registered.
That boundary is now registered, and revision `d704401` is a historical
negative control demonstrating that the pre-repair implementation is caught.
The lint's stated helper-hidden/novel-shape limitation remains true.
