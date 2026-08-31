# C90 — bounded test execution outside the Agency cgroup

Date: 2026-08-31

Status: design for Joe's approval; no unit or service was changed by this
delivery.

## Decision

Run each test job as its own **transient user service**, created by
`systemd-run --user`, beneath a dedicated `futon-testing.slice`.  Do not use a
long-lived `futon-testing.service`, and do not keep test children in
`futon3c-zone.service`.

A transient service is preferable to a scope here.  A scope is a good fit when
the invoking client remains the lifecycle owner, but `bg.py` deliberately makes
jobs survive pouch teardown and needs asynchronous status, logs, cancellation,
and an exit code.  A transient service is owned and reaped by the user systemd
manager after submission; the Agency records its unit name and can disappear
without killing or orphaning the job.  Unlike one long-lived testing service,
one transient unit per job also provides per-run accounting and prevents one
run from consuming another run's allowance.

Proposed shape:

```text
user@1000.service
├── futon-services.slice
│   └── futon3c-zone.service          Agency; TasksMax=2048
└── futon-testing.slice               aggregate test containment
    ├── futon-test-<job-id>.service   one submitted command
    └── futon-test-<job-id>.service   another submitted command
```

The initial submission primitive would be equivalent to:

```sh
systemd-run --user \
  --unit="futon-test-${safe_job_id}.service" \
  --slice=futon-testing.slice \
  --collect \
  --property=TasksMax=256 \
  --property=TimeoutStartSec=45min \
  --property=KillMode=control-group \
  /path/to/futon-test-verdict-wrapper JOB_SPEC
```

The actual implementation must pass an argv/job-spec file, not interpolate an
untrusted command into a shell string.

## Budgets

Set `TasksMax=256` per test job and `TasksMax=1024` on
`futon-testing.slice` initially.  Permit at most four concurrently admitted
jobs.

C89 measured a steady Clojure test JVM at approximately 47–55 tasks.  A limit
of 256 is 4.6 times the observed 55-task steady state, leaving roughly 200
tasks for compiler, Lean, subprocess, and JVM startup bursts while still
bounding a runaway job.  Four jobs at their individual maximum fit exactly
within the slice's 1024-task aggregate ceiling.  These are starting limits, not
claims about an eternal workload: retain `TasksCurrent`, `TasksPeak`, elapsed
time, and memory peak in each receipt and revise the limits only from recorded
high-water data.

C89's motivating measurement was:

```text
futon3c-zone.service pids.max     1024
pre-CI pids.current                600
observed CI peak                  1024
zombies                              0
accumulated bg jobs                  0
```

The futon2 run passed 1,023 tests / 6,155 assertions but emitted
`pthread_create(EAGAIN)` at the ceiling.  The futon3 run passed 248 tests /
1,518 assertions and emitted the same warning after its green summary.  This
is why resource status must participate in the verdict.

## Submission, durability, and cancellation

`bg.py start` submits the transient unit and records both its existing bg job
ID and the systemd unit name.  `bg.py status` reads `ActiveState`, `SubState`,
`Result`, `ExecMainCode`, and `ExecMainStatus`; `bg.py kill` calls
`systemctl --user stop <unit>` and records cancellation distinctly from test
failure.  Journal output or explicit `StandardOutput=append:` and
`StandardError=append:` paths preserve the current durable log contract.

Durability and containment therefore do not conflict: after systemd accepts
the unit, neither the submitting pouch nor the Agency JVM is the process
owner.  Restarting the Agency loses no running test.  On startup, `bg.py list`
reconciles nonterminal records with their recorded units.  A missing unit is a
loud `:lost-unit` result, never an assumed success.

## Boundary and verdict semantics

The wrapper produces one terminal receipt with:

- command exit code and test counts;
- unit `Result`, `ExecMainCode`, and `ExecMainStatus`;
- tasks current/peak and `pids.events` `max` delta;
- timeout, signal, OOM, and cancellation state;
- whether stderr contained native-thread exhaustion (`pthread_create` or
  `Failed to start the native thread`).

The effective verdict is success only when **all** of these are true:

1. the command exits zero;
2. the service result is `success`;
3. the `pids.events:max` counter did not increase during the job;
4. no native-thread-exhaustion marker occurred;
5. the expected test-summary parser found a complete summary.

Thus a suite that prints green and then hits `EAGAIN` is
`:resource-limit-failure`, not pass.  A task-limit hit, timeout, signal, missing
summary, or lost unit gets its own nonzero terminal code and reason.  The raw
test exit remains in the receipt so containment failure cannot be confused
with a failed assertion.  This wrapper needs a negative control: run a fixture
in a unit with a deliberately tiny `TasksMax`, have it print a green-looking
summary before exhausting tasks, and require the outer verdict to reject it.

## Day-one migration

The smallest independently verifiable step is one opt-in `bg.py` execution
mode for **only the futon2 full CI command**:

```text
clojure -T:build ci
```

Acceptance for that step:

1. its unit's `ControlGroup` is under `futon-testing.slice`, not
   `futon3c-zone.service`;
2. the Agency cgroup's task count does not rise by the test JVM's tasks;
3. pouch teardown does not terminate the run;
4. `bg.py status`, logs, cancellation, and exit reporting still work;
5. normal positive execution completes with a complete resource receipt;
6. the deliberately tiny-budget control fails as
   `:resource-limit-failure` despite a green-looking inner summary.

After that acceptance, migrate the futon3 full suite, then other `bg.py` test
and build commands.  Leave agent/model work and ordinary short commands in the
Agency service initially.  Admission to `futon-testing.slice` should refuse a
fifth concurrent job rather than queue it silently or spill it into the Agency
cgroup.

## Authority needed to implement

Implementation requires Joe's approval for:

1. creating and setting properties on `futon-testing.slice`;
2. allowing the Agency user process to create, inspect, stop, and collect
   transient user units;
3. the initial 256-per-job, 1024-aggregate, four-job policy and timeout;
4. changing `bg.py`'s persisted record schema and submission path;
5. choosing the durable log/receipt directory and retention policy.

No implementation should begin until those five decisions are approved.  No
static service is required for the proposed first step.
