# E-kimi-task-53 — Sidecar P3c: exact type census from the index incl. zero-endpoint hyperedges (futon1b)

**Requisition:** completed — 2026-09-26T13:02:57Z, job invoke-1790426229308-24823-96c809d6, state done

**VERDICT (2026-10-09, provisional):** DONE — Requisition completed with state done; type-census sidecar delivered. _(WM status classification by zai-4, high confidence; not yet confirmed by the author.)_

Clocked in by claude-12 for kimi-8 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet P3c — `census?type=T` from the sidecar, exact (futon1b)

Design `futon1b/holes/DESIGN-hyperedge-scope-sidecar-2026-09-26.md` §7 item 3 (Q4). Follows
P1–P3 (106fa1d … 295ebc0, live). Reviewer: claude-12.

## Today

`graph/census` (`futon1b_graph.clj:1929`) counts `(from :hyperedges [xt/id hx/type])` for a
type: a full typed scan (551,794 for `code/v05/edits`). The route (`census-route`,
`futon1b_server.clj:916`) calls `graph/census` through its var, so a graph change goes live
with a reload — do NOT edit the server.

## The gap to close first

The sidecar only has rows for hyperedges with at least one endpoint (`hx_edge`, one row
per endpoint). A hyperedge with zero endpoints is invisible to it, so `count(DISTINCT hx_id)`
can undercount the census. P3's `type-count` has the same blind spot (docstring says so).

1. **Measure** on the live store, read-only (`scripts/futon1b-eval.sh`): how many current
   hyperedges have empty or missing `hx/endpoints`, per type. Report the numbers.
2. Make the sidecar count every current hyperedge exactly. Your choice, justified by the
   measurement — e.g. a sentinel row (`pos = -1`, `endpoint = ''`) for zero-endpoint
   hyperedges, or a one-row-per-hyperedge `hx_node(hx_id PRIMARY KEY, type)` table kept by
   the same hooks, catch-up and fill. Whatever you choose must keep P2/P3 reads correct
   (a sentinel must never be returned as an endpoint match) and must be written by all
   three paths: write hooks, incremental catch-up (both legs), and the fill.
3. Existing live sidecar rows must get the new data without a restart or a manual step:
   say how (e.g. `init!` backfills when the new table/sentinels are absent, recorded in
   `hx_meta`), and time that backfill piece live, read-only.
4. `graph/census` for `:type` answers from the sidecar when `hx/reads-usable?`, else the
   existing scan. Response shape unchanged plus `:hx-index` (as in P2). `entity-type`
   untouched.

## Tests (`test_hx_reads.clj` or `test_hx_index.clj`)

- census parity with the scan path on a store containing zero-endpoint hyperedges, after
  fill, after hooked writes/deletes, and after unhooked writes + catch-up;
- P3 `include-total` equals census on the same store;
- a zero-endpoint hyperedge is never returned by a type+end read;
- **bad case**: drop the zero-endpoint handling; the parity test fails. Say so.

## Live timing (read-only; no reload/restart)

Time the sidecar count for `code/v05/edits`, `code/v05/commit` and one mission-scope type,
and compare with `curl ':7073/api/alpha/census?type=…'` (the scan) for the same types.

## Constraints

- futon1b master; explicit-path commits; no stash, amend, branch switch; others commit to
  `futon1b_server.clj`/`futon1b_xt.clj` today — don't edit those, never revert theirs.
- Gates: clj-kondo (no new warnings), `futon4/dev/check-parens.el`, `test-hx-reads`,
  `test-hx-index`, `test-fts-periodic`.

## Reply

Bell claude-12 back with sha, the zero-endpoint measurement, the design choice and why,
test lines, bad case, and live timings.
