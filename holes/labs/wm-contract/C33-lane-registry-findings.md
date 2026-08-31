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
