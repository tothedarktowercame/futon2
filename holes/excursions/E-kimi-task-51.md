# E-kimi-task-51 — Sidecar P2b: narrow re-check + fields projection on indexed type+end reads (futon1b)

**Requisition:** completed — 2026-09-26T11:31:33Z, job invoke-1790421263183-24779-01c2b3e5, state done

**VERDICT (2026-10-09, provisional):** DONE — Header states the requisition is completed with job id and timestamp. _(WM status classification by zai-5, high confidence; not yet confirmed by the author.)_

Clocked in by claude-12 for kimi-8 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet P2b — cheaper re-check and `fields` projection on the indexed type+end path (futon1b)

Follows P2 (futon1b 07256fa, `futon1b_graph.clj` `hyperedges-indexed-type-end`). Live since
the 10:57 restart. Reviewer: claude-12.

## Measured (claude-12, live :7073, 2026-09-26)

For `type=code/v05/edits&end=2d9477d3c103eb83c8268e9ec468eb1b5967f6c9` (575 rows):
- index lookup: < 1 ms;
- `fxt/hydrate-by-ids` (SELECT *, chunked; what the re-check uses now): **9.6 s**;
- one `SELECT * … WHERE _id IN (575 ids)`: **6.0 s**;
- `SELECT _id, hx$type, hx$endpoints … WHERE _id IN (…)`: **2.1 s**.
- Route timings now: 17–21 rows ~1 s; 463 rows 5.0 s; 575 rows 7.7 s. Cost is per returned
  row; the full-document read dominates.
- Hyperedge columns: `:hx/endpoints :hx/id :hx/ends :prop/phase :hx/props :hx/labels
  :xt/id :hx/type :prop/repo`.

## Build

1. Re-check candidates with the narrow read (id, type, endpoints), in IN-chunks sized from a
   measurement (try 100/500/1000/all on a 500+ row request; say which you chose and why —
   note `c95a8f8` switched hydrate-by-ids to equality below 40 ids).
2. Then read full documents only for the rows that pass and will be returned (after the
   window is cut to `limit`), in the chunking you measured best.
3. When the request carries `fields`, read only the columns those fields need instead of
   `SELECT *` (read `parse-hyperedge-fields` / `project-hyperedge-fields` for the mapping),
   and project exactly as the scan path does.
4. Response shape, order, count and `:hx-index` stay as P2 defined them. The scan path is
   unchanged.

## Tests (extend `test_hx_reads.clj`)

- parity with the scan path still holds (rows, order, count), with and without `fields`;
- a stale candidate (deleted unhooked) is still dropped, and one whose endpoints changed
  unhooked is dropped — both by the narrow re-check;
- the full-document read is called only for returned ids (count the ids passed to it via a
  query-fn wrapper);
- **bad case**: make the full read run on all candidates again; the "only returned ids"
  test fails. Say so.

## Live timing (read-only, after tests pass — no reload or restart)

Time the new function directly in the live JVM through `scripts/futon1b-eval.sh`
(requiring your updated namespace into a *throwaway name* is not possible; instead call
the pieces: the narrow read and the full read for the 575-id set above, with the chunk size
you chose) and report the numbers. claude-12 decides on the live reload.

## Constraints

- Master in `/home/joe/code/futon1b`; explicit-path commits; no stash, amend or branch
  switch. Other agents are committing to `futon1b_server.clj` and `futon1b_xt.clj` today:
  do not edit those unless unavoidable, and never revert their changes.
- Gates: clj-kondo (no new warnings), `futon4/dev/check-parens.el`, `test-hx-reads`,
  `test-hx-index`, `test-fts-periodic`.

## Reply

Bell claude-12 back with the commit sha, the chunk-size measurements, test lines, the bad
case, and the live timings.
