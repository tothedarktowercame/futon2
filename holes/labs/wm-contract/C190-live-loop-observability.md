# C190 — production-loop operator observability and stop semantics

Date: 2026-08-31

## What is visible during the run

The foreground command is not silent. Every phase transition is printed as a
line beginning `[wm-phase]` and flushed immediately
(`full_loop_runner.clj:168-181`). The same EDN line is appended to
`data/wm-full-loop-phases.edn.log` by default (`:38,152,177-181`). A second
terminal can therefore follow durable progress with:

```sh
tail -f /home/joe/code/futon2/data/wm-full-loop-phases.edn.log
```

Phase starts and each two-second Agency-job poll also asynchronously update the
Agency pseudo-agent `war-machine`: `:status "invoking"`, current phase,
attempt/job id and elapsed wait; opportunity end/finally returns it to `idle`
(`full_loop_runner.clj:81-133,3168-3170`). This is visible from another terminal
through the roster:

```sh
watch -n 2 "curl -s http://127.0.0.1:7070/api/alpha/agents |
  jq '.agents[\"war-machine\"]'"
```

The quoting above is illustrative for an interactive shell; the durable phase
log is the canonical, dependency-free watcher. `clojure -M:wm-full-loop
status` is also read-only and reports the cohort ledger plus configured
author/reviewer readiness (`full_loop_cli.clj:646-651`), but its readiness view
does not include the `war-machine` pseudo-agent (`full_loop_runner.clj:414-432`).

The cohort is a third, durable surface. At start it creates an immutable
`:time-step` event, then appends one EDN event per selection, construction,
dispatch, build, adjudication and close checkpoint
(`full_loop_runner.clj:2182-2190`; `full_loop_cohort.clj:263-328`). Inspect it
while running with:

```sh
clojure -M scripts/wm_full_loop_cohort.clj status
```

## Failure after partial progress

An ordinary in-process failure at minute 30 is not discarded. The core catch
calls `close!`; missing later checkpoints become typed `:not-reached-*`
sorries, the failure carries accumulated phase events and checkpoints into a
repair obligation, a morning-brief item is queued, and a durable close event is
appended (`full_loop_runner.clj:2191-2238,2297-2347,3035-3064`). The Agency job
itself remains separately queryable by the job id stored at the dispatch
checkpoint. Budget expiry is explicitly `:agent-budget-expired` /
`:incomplete` and deliberately does not interrupt the possibly productive job
(`:753-780`).

An abrupt process kill is weaker: already appended phase/checkpoint files
survive, so partial progress is not deleted, but `close!` is not guaranteed to
run. The attempt can remain open without a `:closed` checkpoint. This is loud
in the cohort ledger (`:closed? false`), not a fabricated successful outcome.

## Stopping and cancellation

There is no operator `stop` or `cancel` command in the production CLI; its
surface is `status`, `activate`, `canary`, `once`, `tick`, `continuous`, brief,
feature, review and QA (`full_loop_cli.clj:625-660`). Ctrl-C/SIGTERM stops the
local JVM but does not send an Agency cancellation, and the runner explicitly
avoids interrupting a job at its 45-minute budget. Thus the safe current
operator action is to leave the foreground runner attached and let its typed
budget/terminal-state path close the attempt.

If a job is cancelled externally while the runner remains alive, `cancelled`
is recognized as an Agency terminal state (`full_loop_runner.clj:719,753-764`),
but the subsequent boundary reports the generic `Author job did not complete`
with outcome `:build-failed` (`:2738-2743`). **Cancellation is therefore not
typed distinctly from failure in the full-loop outcome.** This is the material
gap: observation and ordinary failure durability are well covered, but an
operator cannot currently request and durably record a distinct cancellation
through this CLI.

No run, selector call, dispatch, cancellation, or code change was made in this
pass.
