# E-kimi-task-52 — Sidecar P3: type-only hyperedge reads (cursor + include-total) from the index (futon1b)

**Requisition:** completed — 2026-09-26T11:44:12Z, job invoke-1790422413705-24786-3acb4fc2, state done

**VERDICT (2026-10-09, provisional):** DONE — Requisition header states completed with state done. _(WM status classification by zai-2, high confidence; not yet confirmed by the author.)_

Clocked in by claude-12 for kimi-8 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet P3 — serve type-only `hyperedges?type=T` reads (cursor + include-total) from the sidecar (futon1b)

Follows P2/P2b (`futon1b_graph.clj` `hyperedges-indexed-type-end`, commits 07256fa, dc646fb;
live since the 10:57 restart + a graph reload at 11:30). Reviewer: claude-12.

## Why

`futon6/scripts/mission_efe_scope_dump.py` pages every `mission-scope/<binder>` type
(`type=…&limit=1000&after=<cursor>`, `include-total=true` on the first page) and reads
`hx/props` of each row. In the EFE publish this step took most of 18+ minutes. The index
does not serve type-only reads yet.

## Measured (claude-12, live, 2026-09-26)

- type+end, 575 rows: full documents 8.9 s; with `fields=hx/endpoints` 4.6 s (100 rows
  3.6 s). Cost is the XTDB `_id IN` read per returned row (non-indexed), not the index.
- Field names are the stored keys (`hx/endpoints`, `hx/props`); plain `endpoints`
  projects to `{}` on both paths.
- Two other clients hold expensive-read permits with `include-total&limit&type` shapes at
  times; timings vary with that load.

## Build

1. In `hyperedges-query-uncached`'s `type` branch, when ONLY `type` (+ `limit`, `after`,
   `include-total`, `fields`) is given — no `repo`/`source-file`/`mission`, no `latest`,
   no as-of — and `hx/reads-usable?`: take candidate ids for the type from the sidecar
   (keyset on hx_id; add an index on `(type, hx_id)` if needed — measure), re-check with
   the narrow read as in P2b, read full documents (or the `fields` columns) only for the
   returned window. Everything else keeps the existing path.
2. **Cursor and ordering must be the existing path's.** Read the type branch for its
   order-by and its `next-cursor` encoding, and produce the same cursor for the same
   window, so a client can page with a cursor from either path.
3. **include-total:** the exact count from the sidecar (`count(*) … WHERE type=? AND
   pos=0`, or distinct hx_id — whichever matches XTDB's count; check both against the
   census on a test store). Keep `:count` / `:count-exact?` semantics as the existing path
   returns them (read lines around `:count-exact?`). The count is only exact up to the
   checkpoint plus hooks; say that in the docstring.
4. Response shape unchanged, plus `:hx-index` as in P2.

## Tests (`test_hx_reads.clj`)

- parity with the existing path on a multi-type store: rows, order, cursor, count and
  count-exact? for page 1 and a cursor-driven page 2, with and without `include-total`,
  with and without `fields`;
- a cursor produced by the indexed path, handed to the existing path (index disabled), gives
  the same next page;
- stale candidate dropped; repo/latest/as-of requests take the existing path;
- **bad case**: return candidates without the re-check; the stale test fails. Say so.

## Live timing (read-only; no reload/restart)

Time the pieces in the live JVM (`scripts/futon1b-eval.sh`) for one mission-scope binder
of each size (small, and the largest by census): candidate fetch, count, narrow re-check,
and the `fields=hx/props` read of a 1000-row page. Report them.

## Constraints

- futon1b master; explicit-path commits; no stash, amend, branch switch. Other agents
  commit to `futon1b_server.clj` / `futon1b_xt.clj` today: don't edit those unless
  unavoidable; never revert their changes.
- Do NOT touch futon6 (`mission_efe_scope_dump.py` has someone's uncommitted edit;
  claude-12 does that switch).
- Gates: clj-kondo (no new warnings), `futon4/dev/check-parens.el`, `test-hx-reads`,
  `test-hx-index`, `test-fts-periodic`.

## Reply

Bell claude-12 back with the commit sha, test lines, the bad case, cursor-parity evidence,
and the live timings per binder.
