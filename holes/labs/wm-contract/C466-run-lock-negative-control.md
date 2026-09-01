# C466 -- RUN12 run-lock negative control (transcript)

Produced by `bash holes/labs/wm-contract/run_lock_negative_control.sh`, re-runnable.
Recorded 2026-09-01T18:01:44Z, tree at bba660c plus the RUN12 working changes.

```
RUN12 negative control -- lock=/tmp/futon2-run-lock-negctl-OyQNYM.lock  sha=bba660c
[1] real run-tick-once against a held lock
  PASS  holder took the lock, pid 1221300
  PASS  second runner exited non-zero (rc=3)
  PASS  stderr says refused
  PASS  refusal names the holder's pid 1221300
  PASS  refusal names the holding agent
  PASS  refusal states it neither waits nor proceeds
  PASS  refused runner wrote no receipt (32)
  PASS  refused runner appended no trace record (52151952B)
  note  refusal took 2s (JVM start; the lock is taken before any store read)
[2] release
  PASS  a completed hold releases the lock
  PASS  lock file gone after release
[3] stale reclaim
holes/labs/wm-contract/run_lock_negative_control.sh: line 83: 1224564 Killed                  clojure -M -m futon2.wm-run-lock hold 180 negctl-doomed > /tmp/negctl-doomed.out 2>&1
  PASS  SIGKILL left the lock behind (pid 1224564)
  PASS  stale lock reclaimed
  PASS  the reclaim log names what it took over
[4] release on SIGTERM
  PASS  SIGTERM released the lock (shutdown hook)
[5] default path
  PASS  default lock path is /home/joe/code/futon2/data/wm-trace/.run-lock

RUN12 negative control: PASS
```

## The refusal, verbatim

`clojure -M -m futon2.run-tick-once 14` against a lock held by `claude-20`, on stderr, exit 3:

```
FUTON2 WM RUN LOCK: refused. /tmp/rl-demo-sroX.lock is held by pid 1228856
(agent claude-20, sha bba660c98e7960de12b998e3830b7f0326f891eb, host zone,
acquired 2026-09-01T18:04:01.875264977Z, run hold). One machine, one runner:
this process will not wait and will not proceed. A lock whose pid is dead is
reclaimed automatically on the next attempt.
```

## What this covers, and what it does not

Covered here, cross-process: the real entrypoint refuses (exit 3) naming the
holder, and writes neither a receipt nor a trace record before doing so
(`src/futon2/wm_run_lock.clj`, `scripts/futon2/run_tick_once.clj:249-262`);
a completed hold, a TERMed hold and a SIGKILLed hold each end correctly
(released, released via shutdown hook, reclaimed-with-a-log).

Covered only in-JVM, in `test/wm_run_lock_test.clj` (10 tests, 42 assertions):
that a tick carrying its own run's `FUTON_WM_RUN_LOCK_TOKEN` passes through
without taking or releasing; that a lock from another host refuses instead of
being reclaimed; that an empty lock file is read as held; that `release!` will
not delete a lock it did not write. The pass-through is not exercised
cross-process because proving it there means running a real tick, which would
append to today's shared trace file — the thing RUN11/RUN3 are trying to stop
happening by accident.

Not covered at all: two hosts. `:host` is recorded and a foreign host is
fail-closed, but nothing here has ever run the war machine from a second
machine, so that path is reasoned, not measured.

Scope of the lock as shipped: `run-tick-once` takes it per tick, so two agents
whose ticks overlap collide loudly (the 2026-09-01 case). Two agents whose ticks
interleave in each other's gaps would not, unless the run is started through
`holes/labs/wm-contract/wm_run.sh`, which takes one lock before the first tick
and releases it after the last.
