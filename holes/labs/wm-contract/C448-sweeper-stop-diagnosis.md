# C448 — inbox-zero attribution sweeper stop diagnosis

Date: 2026-09-01. Discovery only; no serving-JVM or Futon3c mutation was
performed.

## Finding

The attribution loop has **not died or been deliberately stopped**. At the time
of inspection it was alive and CPU-bound in the next sweep's 125.7 MB
`state.edn` load/validation. It had not reached either HTTP call.

The live JVM thread was:

```text
"clojure-agent-send-off-pool-1" ... nid=3191879 runnable
  clojure.lang.PersistentHashMap...
  clojure.core/some
  futon3.inbox_zero.state/apply_record          state.clj:224
  futon3.inbox_zero.state/replay                state.clj:264
  futon3.inbox_zero.state/load_state            state.clj:320
  futon3c.inbox_zero.sweeper/sweep_attributions! sweeper.clj:167
  futon3c.inbox_zero.sweeper/start_loop!        sweeper.clj:251
```

`jcmd 3191716 Thread.print -l` succeeded against the serving JVM. Two further
samples showed the same thread RUNNABLE in the same replay branch, moving
between persistent-map hashing and lookup frames. Its reported CPU time also
advanced between samples. This is computation, not a socket wait, file-read
wait, sleep, dead future, or monitor block.

The expensive branch is `state.clj:218-226`. While replaying every commit scan
cursor it searches the records already accumulated to reject a fork:

```clojure
(some #(= (:prior/cursor-id record) (:prior/cursor-id %))
      (filter ... (vals (:records state))))
```

The current snapshot contains approximately 191,073 `:record/type` entries and
1,372 commit-scan cursors (literal-token census, not an EDN semantic count).
`load-state` parses the snapshot and then validates it by sorting and replaying
all records. The live stack establishes that this replay/fork validation is the
operation preventing the loop from returning to its 30-minute sleep/schedule.

## How the alternatives were distinguished

1. **Uncaught throwable killed the loop — no.** The future's thread and complete
   `start-loop!` stack are present in the live JVM. The journal contains no
   `attribution sweep loop stopped`, `loop threw`, or top-level sweep failure
   after the last reported sweep. More decisively, a dead future cannot have
   the live stack above.

2. **Loop alive but blocked — partly, with a more precise cause.** It is alive
   and a sweep is preventing further scheduling, but it is not blocked on I/O.
   It is RUNNABLE and CPU-bound in state replay. `default-roster` and
   `default-deliver` have no explicit timeout, so they remain separate future
   hang risks; neither is on this thread's current stack.

3. **Deliberate `stop-loop!` — no evidence, and that API does not exist in the
   checked source.** Repository-wide search found no `stop-loop!` definition or
   call for this sweeper. The live future also disproves that it is currently
   stopped. An arbitrary REPL user could theoretically manipulate the private
   future, but no such inference is needed and the observed future is running.

4. **Other named cause — yes: replay validation cost.** The 30-minute delay is
   placed *after a sweep returns*. `start-loop!` runs each sweep synchronously
   on the one future, so a sweep that spends hours validating `state.edn`
   suppresses all later scheduled passes; there is no overlapping execution or
   watchdog restart.

## Timeline and observability boundary

The last journal line was 2026-09-01 02:03:57:

```text
[inbox-zero] attribution sweep left 14 path(s) unswept (max-paths=25)
```

That message is printed only after that pass has loaded state and projected
paths. The live stack therefore belongs to a later load (the loop sleeps only
after the prior pass returns). There is no sweep-start, sweep-finish, or
last-progress timestamp, so an observer outside the process cannot establish
the exact instant this later pass entered `load-state`. It can establish the
current cause and liveness from the thread dump, but not reconstruct that
missing start timestamp.

The serving JVM can expose Cyder state at `GET /api/alpha/processes` and
`GET /api/alpha/processes/:id`. `GET /api/alpha/processes/multi-watcher`
reported `running? true` with fresh cycle progress, confirming the watcher is
independent and healthy. The attribution sweeper itself is never registered
with `cyder/register!`, has no `:state-fn`, and is absent from the nine-process
registry. Consequently no existing HTTP route reports its future state; the
JVM thread dump is the only live read-only evidence surface found.

The process environment still records
`FUTON3C_INBOX_ZERO_SWEEPER=true` and interval `1800000`. Environment enables
startup only; it does not supervise or restart the future.

## Scope

This report names the observed cause. It does not propose or implement a
repair, add an endpoint, restart the JVM, reload code, or invoke lifecycle
functions.
