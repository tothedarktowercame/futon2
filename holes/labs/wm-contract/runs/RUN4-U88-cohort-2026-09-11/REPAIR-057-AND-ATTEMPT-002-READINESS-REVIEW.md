# Repair 057 and attempt-002 readiness review

Date: 2026-09-11. This is a read-only review. It creates no repair-store,
cohort, series, registry, credential, or service state.

## What current code repairs

The original finding records a raw `java.net.http.HttpTimeoutException` after
one 100026 ms selection call. Commit `9ab503bd` introduced a three-attempt
patient selection ladder. Commit `3bdc381e` made transport/no-status failures
retryable, deterministic response rejection fail fast, and retained typed
evidence for every attempt. The current `strategic-selection!` implementation
is materially the same ladder: default budgets are 150000, 210000 and 270000
ms with 5000 ms delays; an injected fixed budget applies to all attempts.

An isolated invocation of the actual public consumer, with no endpoint,
established:

- raw `HttpTimeoutException`, then a valid response: two calls, one delay,
  `:readiness/selection-transient true`;
- three raw timeouts: `:strategic-selection-unavailable`, selection stage,
  `:transient-exhausted`, with three per-attempt summaries;
- invalid response: `:deterministic-rejection` after exactly one call.

The full current runner suite also passes: 127 tests / 601 assertions.

Current code additionally differs from those July commits at the outer
failure boundary. Commits `36b3bb1c`, `4457a109`, `24c5ea78`, `64561ef0`,
`cea3ad03`, and `af82d3b8` now recognize `HttpTimeoutException` by class and
type it as `:transport-timeout` / `:environmental-hold`, while retaining
explicit phase typing and fail-closed local socket misuse. Thus a new exhausted
HTTP selection timeout no longer opens the same untyped machine-failure shape.

This repairs single transient timeouts, records an exhausted ladder, and fixes
future failure classification. It cannot make an externally unavailable
selector succeed, and it does not retroactively alter finding 057.

## Existing repair contract

Finding `repair-attempt-057-untyped-failure` remains `:machine-failure` with no
matching implementation or resolution record. For an open machine failure,
the runner selects `:repair-machine-failure`, routes independent review through
the configured repair-reviewer, grounds the reviewed commit, and calls
`record-implementation!`. That immutable transition requires a distinct
implementation attempt and commit, reviewer/job, and a real grounding witness
whose `:resolved?` and `:dial-moved?` are true.

On a later run `open-obligations` exposes that record as
`:awaiting-validation`; it no longer becomes the selected stop-line repair but
is retained in `validation-lines`. A separately selected and successfully
reviewed/grounded production-shaped action then calls `resolve!` with
`:production-shaped? true`. This is the required distinct successor. Neither
the implementation nor validation record may be pre-authored from this review.

The July retry commits are strong candidate implementation artifacts, but the
current API cannot attach a historical commit without a distinct live repair
attempt producing the required review and grounding witness. Whether the
repair mission should ground the already-present commit or author a new
evidence/diagnostic commit is the remaining semantic decision; the runner's
fresh-artifact binding normally requires a newly observed author artifact.

## Staffing and capacity recommendation

For the repair implementation attempt, keep author `zai-2` and ordinary
reviewer `codex-12`, but replace repair-reviewer `codex-17` with `codex-10`.
The roles remain distinct, and the coordinator is no longer selected while it
is invoking. This is a recommendation, not an availability assertion:
startup must capture a fresh roster and require `codex-10` to be idle and
invoke-ready before admission. No current-roster observation is cached here.

Do not reuse consumed cohort `:run4-u88-20260911-v1` target 1 or its root. Mint
a new preregistration, root, series ID and controller attempt ID. Because the
contract needs both a repair-implementation attempt and a later distinct
production-shaped validation successor, provision reviewed capacity for two
attempts (or two separately reviewed target-1 cohorts). Each admission must run
`execution-preflight` with capacity required and bind the exact new
preregistration bytes/root/id. The second attempt must not be admitted until
the first has a valid `:awaiting-validation` implementation record.

The later validation attempt may select the pinned U88 action only after the
stop line is in awaiting-validation and ordinary admissibility/pin freshness
still passes. It must produce its own review and grounding evidence. No age,
reconciliation observation, cohort reset, or caller flag can substitute.

## Remaining uncertainties before enactment

1. Independent review must decide whether a new repair commit is necessary or
   whether a new evidence commit can lawfully ground the already-landed retry
   implementation without pretending it was newly authored.
2. A fresh roster must establish repair-reviewer availability at admission.
3. The new two-attempt capacity document and all successor pins require review
   and explicit activation; the consumed cohort remains immutable.
4. External selector availability is a precondition, not something the retry
   code guarantees. Exhaustion remains an honest environmental hold.
5. The incomplete prior controller attempt has only an observational
   reconciliation record with unknown cross-store association; it must not be
   reused or treated as a terminal trial result.
