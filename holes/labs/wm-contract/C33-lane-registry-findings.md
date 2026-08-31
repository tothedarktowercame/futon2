# C33 — four-vertex lane registry

Delivered 2026-08-31 by `wm-verbs` for `P-dispatch-workflow.md` §4.1 and §4.5.

The registry is `lane-registry.edn`. It has exactly one row for each of
`wm-nouns`, `wm-verbs`, `wm-organization`, and `wm-evidence`. Every row carries
all five dispatch fields. Idle is represented by `:holding nil` plus explicit
nil dispatch metadata; it is never inferred from a missing row or key.

`checks/lane_registry_check.clj` reports each lane as `:idle`, `:holding`,
`:overdue`, or `:missing`. Active rows require parseable ISO-8601 dispatch and
deadline timestamps plus a nonblank job id. Missing and duplicate lanes,
incomplete active rows, deadlines not after dispatch, and overdue holdings all
fail closed.

Canonical invocations:

```sh
bb checks/lane_registry_check.clj
bb checks/lane_registry_check.clj --negative missing
bb checks/lane_registry_check.clj --negative overdue
```

Exit convention: `0=pass`, `1=ordinary failure`, `2=mutation slipped`. In a
negative run, rejecting the mutation is the successful result and exits 0.
The two mutations independently prove that a missing lane and an overdue active
holding are observable failures. This is instrumentation only; it sends no
dispatch and makes no scheduling decision.

## C75 completion-state comparison

The checker now reads each active row's Agency job at
`GET /api/alpha/invoke/jobs/<job-id>`. A terminal Agency state (`done`,
`failed`, or `cancelled`) while `:holding` remains set is reported as
`:stale-holding` and fails. An unavailable or malformed Agency response is
`:job-state-unavailable`, also a failure rather than a guessed running state.

Verified failing before the registry transition: Agency reported C73 job
`invoke-1788190333739-5735-06d480ef` done at
`2026-08-31T15:35:40.271184502Z` while `wm-nouns` still held C73. The registry
now records that lane as explicit idle; the transition was based on Agency's
completion event, not inferred from roster liveness.

The final full-suite gate caught the same transition independently for C74:
Agency reported `invoke-1788190442217-5736-9d024c62` done at
`2026-08-31T15:39:14.434945104Z` while `wm-evidence` still claimed holding.
That row was likewise advanced to explicit idle before the positive gate.

Canonical invocations:

```sh
bb checks/lane_registry_check.clj
bb checks/lane_registry_check.clj --negative done
bb checks/lane_registry_check.clj --negative missing
bb checks/lane_registry_check.clj --negative overdue
```

The C16 convention remains `0=pass`, `1=ordinary failure`, and
`2=mutation-slipped`. `--negative done` injects an active row whose Agency
state is terminal; exit 0 means the stale holding was rejected.
