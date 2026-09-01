# C333 — coordinator writer classes and corrected parking list

Date: 2026-09-01. No coordinator, watchdog, timer, or service was stopped,
started, resumed, or reconfigured during this investigation.

## Finding

A durable `:complete` regulator with no process-local runtime cannot tick,
persist coordinator state, or dispatch. `live-regulator/tick!` returns terminal
states unchanged; `start!` returns a recovered terminal state without creating
an executor. Neither an external bell nor `recover-all!` reopens it. Only
explicit `resume!` can reopen it, through `continue-complete!`.

It was nevertheless still a writer: its independently scheduled semantic
watchdog was live. The watchdog evaluates and persists its observation every
period. The reversible park is therefore to stop that watchdog while leaving
the registration and durable `:complete` state untouched. `start-registered!`
safely re-arms the watchdog and observes the terminal state; `resume!` is
forbidden because it means continue the completed campaign.

The two durable `:running` coordinators are genuine writers. Their schedulers
call `tick!`, which persists a tick claim, executes the adapter (including
dispatch), and persists the resulting state. Their watchdogs also write.
`durable-coordinator/stop!` disables admission, cancels both schedulers, drains
any claim, and writes a quiescence witness; only these two use `resume!`.

## Corrected parking list

1. `jit-queue:jit-m94A03-retry-v3`: stop only
   `semantic-progress:jit-queue:jit-m94A03-retry-v3`; preserve enabled and
   durable `:complete`. Restore with `start-registered!` after reconfirming the
   state remains terminal with no runtime scheduler.
2. `jit-queue:jit-all-open-v2`: `durable-coordinator/stop!`; wait through
   `:draining` to `:stopped` plus quiescence witness. Restore the journalled
   action with `resume!`.
3. `ftriangle-live-smoke-v1`: the same durable stop/drain/witness and journalled
   `resume!` restoration as item 2.

Fence evidence now observes regulator and watchdog scheduler presence. Its
terminal policy accepts `:complete` only with no tick claim, no runtime
scheduler, and no watchdog scheduler. The control proves a completed
coordinator with a live watchdog remains `FENCE-BREACH`; the identical terminal
state with its watchdog parked is verifiable.
