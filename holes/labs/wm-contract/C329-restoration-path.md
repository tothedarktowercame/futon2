# C329 — restoration-path dry run

Date: 2026-09-01. Discovery and documentation only; no writer was parked,
unparked, stopped, started, disabled, or reconfigured.

## Result

The prior blanket restoration procedure was not executable safely. The live
read-only probe found:

| coordinator | registered | durable state | runtime | restoration consequence |
|---|---|---|---|---|
| `jit-queue:jit-m94A03-retry-v3` | enabled | `:complete` | absent | `resume!` would call `continue-complete!`; this is new work, not restoration |
| `jit-queue:jit-all-open-v2` | enabled/running | `:running`, epoch 119, tick 51313 claimed | no local status returned | park must drain; a later resume restores intent, not the old tick/epoch |
| `ftriangle-live-smoke-v1` | enabled/running | `:running`, epoch 9 | no local status returned | resume can restore activation intent, not runtime identity |

The source settles the first case: `durable-coordinator/resume!` dispatches
durable `:complete` to `regulator/continue-complete!`. Therefore
`enabled? = true` is not a sufficient restoration predicate.

Systemd observation likewise proves exact restoration is unavailable. Starting
a stopped service creates a new InvocationID and loses process-local state;
starting a timer can recompute its next elapse. The honest target is
**restoration of captured activation intent**, with the new epoch/InvocationID
recorded as a transition—not equality to the pre-fence bytes.

## Partial-parking rule

C319 now requires an append-after-success mutation journal. An abort restores
only journalled successful mutations, in reverse order. This handles zero, one,
or many completed parking operations and prevents a half-parked abort from
starting a writer that was never stopped. Coordinator restoration additionally
requires captured durable pre-status `:running`; service restoration requires
captured `ActiveState` active/activating.

Superseded by C333: the terminal coordinator itself cannot tick, but its
independent semantic watchdog was live and writing. The corrected reversible
operation parks only that watchdog and re-arms it with `start-registered!`;
blanket coordinator stop/resume remains refused.

## What was dry-run

- The serving-JVM status query was executed read-only for all three IDs.
- `systemctl show` read the eight unit states, including `Restart`, active/sub
  states, InvocationID, and timer next-elapse data.
- The two restoration command forms resolve to the existing canonical
  `proof-eval.sh`/`resume!` and `systemctl --user start` surfaces.
- No systemd dry-run can prove restoration; it would not exercise the state
  transition. On the day, success is verified by re-running the captured status
  and unit-show probes and recording the new transition identities.

The standalone emergency section in C319 is sufficient for Joe if the
coordinator session disappears: preserve the manifest and journal, reverse only
journalled actions, refuse non-running coordinator resumes, verify, announce
release, and retain any failed undo as an owned finding.
