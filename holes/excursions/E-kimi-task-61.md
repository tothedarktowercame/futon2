# E-kimi-task-61 — Sidecar P6b: wire scope index into the futon1b server (futon1b)

**Requisition:** completed — 2026-09-26T14:17:22Z, job invoke-1790431906285-24869-0f3a0a77, state failed

Clocked in by claude-12 for kimi-1 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet P6b — wire the scope index into the futon1b server (init + read route) (futon1b)

`futon1b_scopeindex.clj` (ad7468b) indexes a run directory's scope files into the shared
SQLite sidecar and has read fns Q6 `scopes-of-paper`, Q7 `scopes-overlapping`, Q8
`scopes-of-kind` (read the ns). It is not wired into the server yet. kimi-8 is extending that
ns in parallel (P6a: marks + a `node-join` fn) — do NOT edit `futon1b_scopeindex.clj`.
Reviewer: claude-12.

## Build (`futon1b_server.clj` only, plus a test)

1. At startup, next to `(hx/init!)` (`futon1b_server.clj` ~1148): create the scope tables
   (`init!`-equivalent in the ns — if the ns has none, say so and ask claude-12 rather than
   editing the ns). Index runs listed in an env var `FUTON1B_SCOPE_RUNS` (colon-separated
   run dirs; default empty = none), off the startup path (a background future), logging
   `[scopeindex] run … rows … ms`. Failure to index a run must not stop the server.
2. `GET /api/alpha/scopes` with exactly one of: `paper=P` (+ optional `lines=a-b` → Q7),
   or `kind=K` (Q8); optional `run=R`. Match the parameter-validation and error shapes of
   the neighbouring routes (read `hyperedges-route`, `census-route`): 400 for bad/missing
   params, typed refusal when the sidecar is unavailable. **Register the handler through
   its var** (e.g. `(handler #'scopes-route)` or a fn calling the var) so later route
   changes reload without a restart — note in a comment why, citing
   `TN-entities-speedups-2026-09-26.md` (routes captured by value need a restart).
   Response EDN like other routes: `{:scopes [...] :count n}`.
3. Nothing is live until claude-12 asks Joe for a restart; do not reload or restart.

## Tests

A test that starts the handler against a throwaway sidecar with a small fixture run (reuse
`test_scope_index.clj`'s fixture approach) and checks: Q6/Q7/Q8 via the route; 400 for no
params, both params, malformed `lines`; `FUTON1B_SCOPE_RUNS` empty → no indexing.
**Bad case**: register the route by value and redefine the route fn — the var-registered
version picks up the change, the by-value one does not. Show both.

## Constraints

- futon1b master; explicit-path commits; no stash, amend, branch switch,
  `git checkout -- <file>`; other agents commit to `futon1b_server.clj` today: keep your
  edit minimal and never revert theirs; re-read the file right before committing.
- Gates: clj-kondo (no new warnings), `futon4/dev/check-parens.el`, your test,
  `test-hx-reads`, `test-hx-index`, `test-fts-periodic`.

## Reply

Bell claude-12 back with sha, the route contract, test lines and the bad case.
