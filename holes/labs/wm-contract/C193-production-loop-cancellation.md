# C193 — production-loop cancellation

Date: 2026-08-31

## Result

The full-loop CLI now exposes one operator operation:

```sh
clojure -M:wm-full-loop cancel JOB-ID [REASON]
```

It calls the Agency's existing single-finalizer endpoint,
`POST /api/alpha/invoke/jobs/JOB-ID/cancel`. Agency records terminal state
`cancelled` with terminal code `operator-cancelled`, then interrupts the job's
process tree. The polling runner lifts that terminal state into the distinct
full-loop outcome `:cancelled` / failure kind `:operator-cancelled`; it is not
reported as `:build-failed`. Cancellation is classified as an
`:environmental-hold`, not a machine repair obligation.

The distinction is applied at every dispatched-job wait boundary: initial and
retry author, reviewer, revision author, re-reviewer, and build cure. Genuine
failed jobs retain the existing `:build-failed` outcome.

## Ctrl-C boundary

No JVM signal trap was added. As soon as a real dispatch returns its Agency job
ID, the runner prints and flushes both facts:

```text
[wm-cancel] Ctrl-C alone does NOT cancel the Agency job.
[wm-cancel] To stop this runner and its Agency job:
clojure -M:wm-full-loop cancel JOB-ID operator-request
```

The cancellation command stops the Agency job; the live runner observes the
terminal state on its next poll and records the typed close outcome. If the
operator has already killed the local runner with Ctrl-C, Agency cancellation
still stops the work, but that dead local process cannot write its own attempt
close. The start-time warning makes this limitation explicit.

## Falsifiers

- A synthetic Agency `cancelled` author job closes as `:cancelled`, preserves
  `:operator-cancelled` and `:author-wait`, and creates an environmental hold.
- A synthetic Agency `failed` author job still closes as `:build-failed`; the
  two states cannot collapse into one vocabulary value.
- The cancellation-command unit verifies the exact Agency endpoint and request
  body without creating or cancelling a live production job.

No production tick or production Agency job was run or cancelled.

## Canonical invocations

```sh
clojure -M:test -m cognitect.test-runner -n futon2.aif.full-loop-runner-test
clj-kondo --lint src/futon2/aif/full_loop_runner.clj src/futon2/aif/full_loop_cohort.clj src/futon2/aif/full_loop_cli.clj test/futon2/aif/full_loop_runner_test.clj
make ci
(cd /home/joe/code/futon3 && clojure -M:test -m cognitect.test-runner)
bb -cp . checks/wm_workspace_gate.clj
```
