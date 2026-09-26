# E-kimi-task-49 — Sidecar P1: hyperedge tables, write hook, system-time catch-up (futon1b)

**Requisition:** completed — 2026-09-26T10:27:48Z, job invoke-1790417759279-24762-207b53c9, state done

Clocked in by claude-12 for kimi-8 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet P1 — hyperedge rows in the SQLite sidecar: tables, write hook, catch-up (futon1b)

Design: `futon1b/holes/DESIGN-hyperedge-scope-sidecar-2026-09-26.md` — §3 schema, §4 as
corrected by review, and "P0 measurements" (kimi-7, a222a4a). Reviewer: claude-12.
This packet fills the index. It does NOT serve any read from it (that is P2).

## Build

1. **Tables** in the existing sidecar file family (`futon1b_text.clj` owns the SQLite file
   and its `init!`; put the hyperedge part in a new namespace, e.g. `futon1b_hxindex.clj`,
   sharing that datasource):
   - `hx_edge(hx_id, type, pos, endpoint)` with indexes `(type, endpoint)`,
     `(endpoint, pos, hx_id)`, `(hx_id, pos)` — the last two are what made the two-hop
     query 2.7 ms instead of not finishing (review of PROTO-neo4j);
   - `hx_meta(k, v)` for the checkpoint(s).
   Intern nothing yet; keep hx_id as text.
2. **Write hook.** Find every code path that puts or deletes rows in the XTDB
   `:hyperedges` table and list them in the commit message. Known so far:
   `futon1b_server.clj` ~236 (evidence + hyperedge in one tx), ~137 (`:delete-docs`),
   `futon1b_graph.clj` ~403 (`:delete-docs table id`), and the POST
   `/api/alpha/hyperedge` path through `migration.ingest` `put-doc-with-rescue!`. After
   the XTDB tx succeeds, upsert (delete rows for that hx_id, insert new) or delete. The
   hook must never fail the request: on error, log it attributably (as `text/on-append!`
   does) and leave repair to catch-up. It never advances the checkpoint.
3. **Catch-up** (`catch-up!`, owning the checkpoint), per P0: keyset-paged
   `FOR ALL SYSTEM_TIME … _system_from > T` for upserts and `_system_to > T` for
   tombstones; for each changed id, re-read its current row (or absence) and upsert/delete.
   A full rebuild function for an empty index.
   **Cadence — review decision, differs from P0's "tens of seconds":** each catch-up query
   scans the whole `hyperedges` table (P0 EXPLAIN), and the live table holds at least
   570k rows (census: 551,794 v05 edits + 18,524 commits, plus other types). The hook is
   the primary path; catch-up runs at boot and then every 15 minutes by default
   (`FUTON1B_HX_CATCHUP_MS`), taking an expensive-read permit like other scans, and is
   skipped when `xt.txs` shows no new transaction since the last run.
4. **Stats**: `hx-stats` returning row count per type, checkpoint, last catch-up time and
   duration, hook failures since start. Expose it on the existing sidecar stats route if
   there is one, else only as a function (no new route in this packet).

## Tests (new `test_hx_index.clj`, `clojure -M:node -m test-hx-index`, throwaway node like
`test_fts_periodic.clj`)

- put a hyperedge → rows present with the right positions; re-put with changed endpoints
  → old rows gone, new present;
- delete → rows gone;
- hook disabled (or made to throw) → the request still succeeds; the row appears after one
  `catch-up!`; the delete of a hooked-out id also repairs through the `_system_to` query;
- rebuild from empty reproduces the per-type counts of a direct XTDB count;
- **bad case**: make catch-up read only `_system_from` (drop the tombstone query); confirm
  the delete-repair test fails; restore. Say so in your reply.

## Constraints

- Work on master in `/home/joe/code/futon1b`; commit with explicit paths
  (`git commit -- <paths>`); no stash, amend or branch switch.
- Do NOT reload or restart the live futon1b JVM (:7073) — claude-12 decides that after
  review. Test JVMs only on throwaway stores; stop them when done.
- Gates: `clj-kondo --lint` on changed Clojure files (no new errors),
  `emacs --batch -l ~/code/futon4/dev/check-parens.el` if that is how this repo runs it
  (see AGENTS.md), and the new test namespace plus `test_fts_periodic` still passing.

## Reply

Bell claude-12 back with the commit sha(s), the list of write paths hooked, the test
result lines, the bad case, and the rebuild time on your test store.
