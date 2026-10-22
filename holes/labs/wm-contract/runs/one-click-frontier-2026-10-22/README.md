# One-click frontier — 2026-10-22

Objective: one useful production click, retaining successful sections before
repairing the next failure. This directory does **not** establish that objective
as complete. Between-click learning/carry is a separate effort.

## Observed admissions

Two of Joe's ten project clicks were charged:

- `wm-click-3041d9dc-2084-493a-8735-241ed1b5bd94`: dedicated META repair
  queued child `wm-requeue-7-4-repair/239cf98836afc977cd589141c224ec3f4bca83dc`.
  The child failed before its first attempt because committed canonical runner
  source changed after the serving JVM loaded it. `accepted.json` preserves the
  structured source mismatch; `failed-child.edn` preserves durable acceptance.
- `wm-click-d487dbd2-a56c-431e-b948-a7d7658b7feb`: after canonical reload,
  META returned `:awaiting-retry`. No ordinary selection ran. Durable result is
  `second-parent-acceptance.json`. No run record was returned.

Eight project clicks remain. Global consumed count after these admissions was
149 of 158 (one available click predates this project). No further admission
should be issued merely to repeat the awaiting-retry state.

## Source-entry repair

`check-runner-source-phase!` places the existing source guard inside the normal
restartable phase boundary, before attempt creation. A held source drift can
be rechecked after a recorded canonical reload via `:retry`. A fresh guard
check after the breakpoint prevents a supplied debugger value from becoming
code identity, and detects changes while a successful check was held.

`source-restart-test.log`: new focused namespace, **2 tests / 8 assertions,
0 failures / 0 errors**. The positive test reproduces the actual drift digest
shape, repairs the source observation, retries the same held run, and enters
the next section exactly once without repeating its preceding section.
This is a component test, not evidence of a live completed click.

## Remaining recovery work

The failed child has no run record, so the coordinator never ingested its
outcome. Its acceptance is terminal and idempotent; another parent admission
cannot relaunch it. Zai-8 is implementing a durable child-outcome observation
port in an isolated worktree (`recovery-repair-request.txt` and dispatch).
Existing retry limits and attribution must remain intact. The child is already
at the recorded requeue limit; a legitimate exhaustion outcome must be
reported as incomplete, never relabelled as success.

`wm_capture_held_section.py` is still an unverified live-capture draft; it has
not captured a live boundary and is not part of the verified source repair.
