# E-kimi-task-57 — Sidecar P3d: endpoint-prefix hyperedge reads (Q3) from the index (futon1b)

**Requisition:** completed — 2026-09-26T13:37:47Z, job invoke-1790429236453-24847-5e471bbd, state done

Clocked in by claude-12 for kimi-8 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet P3d — endpoint-prefix hyperedge reads (Q3) from the sidecar (futon1b)

Design `futon1b/holes/DESIGN-hyperedge-scope-sidecar-2026-09-26.md` §7 item 3, query Q3
("by type + endpoint prefix (repo)", used by repo-level reports; today not supported —
paging + client post-filter). Follows P1–P3e + c07db8f (all live). Reviewer: claude-12.

## Build

1. A way to ask `hyperedges` for a type and an endpoint **prefix**. Read
   `hyperedges-route` (`futon1b_server.clj`) and `hyperedges-query` for the existing
   parameter conventions and pick the parameter name to match them (e.g.
   `end-prefix=`). If adding the parameter needs a server edit, keep it to parsing and
   passing the value (the route fn is captured at startup, so that part goes live only at
   the next restart — say so); the query itself lives in `futon1b_graph.clj`.
2. Candidates: `hx_edge WHERE type=? AND endpoint >= ? AND endpoint < ?` (upper bound =
   the prefix with its last code point incremented; handle the empty prefix by refusing
   it with a 400, not by scanning the type). Re-check each candidate with the narrow read
   as in P2b (type matches AND some endpoint starts with the prefix), full documents or
   `fields` only for the returned window, keyset cursor on hx_id like P3, `:hx-index` in
   the response. There is no scan path for this today: when `hx/reads-usable?` is false,
   answer with an honest typed refusal (503 + reason), not a slow scan.
3. `include-total`: exact count from the sidecar (`count(DISTINCT hx_id)` over the range).

## Tests (`test_hx_reads.clj`)

- membership parity: on a small multi-type store, the prefix read (all pages) returns
  exactly the set a full scan of the type filtered in Clojure by `str/starts-with?` gives;
- prefixes with non-ASCII characters (e.g. `→`, used in `dir:<sha>→…` endpoints) and a
  prefix that is itself a full endpoint;
- cursor paging covers each id exactly once; include-total equals the set size;
- stale candidate dropped; unusable index → the typed refusal;
- **bad case**: compute the upper bound wrongly (e.g. prefix + "z"); a test fails. Say which.

## Live timing (read-only; no reload/restart)

Time the sidecar range + re-check for one real prefix on `code/v05/edits` (a repo-shaped
endpoint prefix — inspect a few endpoints to choose one) and report counts and ms.

## Constraints

- futon1b master; explicit-path commits; no stash, amend, branch switch, or
  `git checkout -- <file>`; never revert others' changes (others edit
  `futon1b_server.clj`/`futon1b_xt.clj` today).
- Gates: clj-kondo (no new warnings), `futon4/dev/check-parens.el`, `test-hx-reads`,
  `test-hx-index`, `test-hx-busy`, `test-fts-periodic`.

## Reply

Bell claude-12 back with sha(s), whether a server edit (restart) is needed, test lines,
bad case, live timing.
