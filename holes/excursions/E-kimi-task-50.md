# E-kimi-task-50 — Sidecar P2: serve type+endpoint hyperedge reads from the index, re-checked (futon1b)

**Requisition:** completed — 2026-09-26T10:52:12Z, job invoke-1790419324046-24772-7bf72361, state done

Clocked in by claude-12 for kimi-8 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet P2 — serve `hyperedges?type=T&end=E` from the SQLite sidecar, re-checked against XTDB (futon1b)

Design: `futon1b/holes/DESIGN-hyperedge-scope-sidecar-2026-09-26.md` §7 P2. The index is
P1 (`futon1b_hxindex.clj`, commits 106fa1d, ad9b9a4, 55d0a00, c2e7cb5). Reviewer: claude-12.

## State of the live index (claude-12, 2026-09-26, measured)

- Filled in the live futon1b JVM: 955,778 current hyperedges in 4 min; 44 types;
  `:code/v05/edits` 552,770 ids. Incremental catch-up every 15 min (~20 s per run).
- `type` is stored as `(str keyword)`, e.g. `":code/v05/edits"` (with the colon); the
  route's `type` parameter arrives without it.
- One commit's edits from the index: 0.5 ms. The same request to the route today
  (`hyperedges?type=code/v05/edits&end=<sha>&limit=100`): 8.9 s (a scan).
- The live JVM does NOT yet run the P1 write hooks or the permit wrapper: those are in
  `futon1b_server.clj` and take effect at the next restart. Until then new writes reach the
  index only through the 15-minute catch-up.

## Build

1. In the hyperedges read path (`futon1b_server.clj` `hyperedges-route` →
   `graph/hyperedges-query`), when the request has `type` AND `end` and no
   `valid-as-of`/`system-as-of`, and the index is usable (below): take candidate hx_ids
   from `hx_edge WHERE type=? AND endpoint=?`, apply `after`/`limit` in the same order the
   current query uses (read `hyperedges-query` for its ordering and cursor format and keep
   both), then hydrate those ids from XTDB (`fxt/hydrate-by-ids`) and drop any that no
   longer match type and endpoint. The response shape, cursor and `:count` semantics stay
   byte-for-byte those of the current path.
2. **Usable** means: `hx_meta` has a checkpoint (the fill has run) and the index is
   enabled (`FUTON1B_HX_READS`, default on). Otherwise, and for every other parameter
   combination, the existing path runs unchanged.
3. **What the re-check cannot catch:** a hyperedge written after the last catch-up whose
   hook failed is missing from the candidates. Report it, don't hide it: add
   `:hx-index {:checkpoint … :hook-failures n}` to the response only when the index served
   it, and fall back to the existing path when `hook-failures` has grown since the last
   catch-up.

## Tests (extend `test_hx_index.clj` or add `test_hx_reads.clj`, throwaway node)

- the indexed path and the existing path return the same rows, cursor and count for a
  type+end request on a store with several types and endpoints, including paging with
  `after`;
- a **planted stale candidate** (a row in `hx_edge` for an id deleted in XTDB without the
  hook) is not returned;
- as-of requests and requests without `end` take the existing path;
- with the index unusable (no checkpoint), the existing path runs;
- **bad case**: remove the re-check; the stale-candidate test fails. Say so in your reply.

## Constraints

- Master in `/home/joe/code/futon1b`; commit with explicit paths; no stash, amend, branch
  switch. No reload or restart of the live JVM (claude-12 asks Joe for the restart).
- Gates: clj-kondo (0 errors, and no NEW warnings), `futon4/dev/check-parens.el`, the new
  tests, `test-hx-index` and `test-fts-periodic` passing.

## Reply

Bell claude-12 back with the commit sha(s), the test result lines, the bad case, and the
parity evidence (same rows/cursor/count) you checked.
