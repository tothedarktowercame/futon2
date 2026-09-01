# C303 — quiescence across independent authorities

Date: 2026-09-01. Owner: `wm-organization`. Design/decision only; no token,
quiescence attempt, job transition, or repository mutation was performed.

## Claim the composite can make

`quiescence_check.py` takes two complete caller-side observations. Each reads:

1. five Git HEADs and exact porcelain outputs;
2. the lane-registry digest and validator output, whose held rows consult
   Agency job state through separate requests;
3. active ordinary jobs from `bg.py list`;
4. active bounded units from `bg.py test-list`.

It compares the complete first and second values. A persistent endpoint change
in any of these representations returns `UNAVAILABLE`; dirty, active, stale,
or non-idle endpoint state returns `NOT-QUIESCENT`. Rechecking only the cheapest
authority would be weaker than the existing second full observation.

This is not an instantaneous global snapshot. Git repositories, the registry,
the Agency ledger, and systemd expose no common revision or lock. Equal
endpoints cannot exclude ABA: a job can start and finish, or a file can change
and be restored, between reads. `lane_registry_check` can additionally combine
separate Agency job responses that never coexisted. An Agency ledger revision
would close that last Agency-local hybrid and Agency ABA; it would not version
Git, the registry file, or systemd.

## Practical soundness during the drain

The composite is operationally sufficient only under a procedural writer
fence: the coordinator freezes dispatch; each lane has finished and
acknowledged it will not write or launch work; generated artifacts are settled;
no scheduled generator/watcher is armed; and no operator or service outside
the protocol can mutate the repositories or job populations. Under that
precondition, the interval between sequential reads is uninteresting because
the set of permitted writers is empty. The two observations then check that
the fence's visible consequences hold and catch ordinary violations.

The check cannot prove its own writer-fence premise. The gap becomes practical,
not merely theoretical, if any of these remains possible:

- an Agency dispatch begins and completes between job observations;
- a bounded or ordinary transient unit starts and exits between list calls;
- a lane, operator shell, timer, watcher, or paper generator writes and
  restores/commits repository content during the sandwich;
- a process acts outside the lane registry or after its lane was marked idle;
- any writer starts after C292 returns and before or during the operator run.

The last case is the existing readiness time-of-check/time-of-use boundary.
Neither two samples nor an Agency token locks the run interval.

## Run decision

**Do not wait for the Agency revision token.** It is valuable and
correctness-critical for multi-job Agency aggregates, but it cannot make the
cross-authority quiescence claim atomic. Waiting would delay Joe's coordinated
reload without closing the decisive Git/systemd boundary.

Proceed after the explicit drain protocol establishes the writer fence, C292
returns `QUIESCENT`, the bounded gate/suite receipts retain stable clean
content identities, and readiness returns `READY`. Keep the freeze in force
through reload, click, and certification. Any unexpected writer or changed
basis closes the window and restarts the sequence; it is not retried until
green inside the same window.

Focused self-test remained green: clean → `QUIESCENT`; dirty tree, active or
stale holding, and active ordinary/bounded job → `NOT-QUIESCENT`; changed
endpoint state → `UNAVAILABLE` (7 controls, exit 0).
