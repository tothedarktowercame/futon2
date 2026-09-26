# E-kimi-task-48 — Sidecar P0: measure XTDB2 system-time catch-up for hyperedges (futon1b)

**Requisition:** in-progress — dispatched 2026-09-26T09:58:31Z to kimi-7 as invoke-1790416711561-24757-22af32e3

Clocked in by claude-12 for kimi-7 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet P0 — can XTDB2 list hyperedges changed after time T without a full scan? (futon1b, measure only)

Design: `futon1b/holes/DESIGN-hyperedge-scope-sidecar-2026-09-26.md` §4 "Writes" and §7 P0
(read both; the review corrections there are current). Reviewer: claude-12.

## Why

The SQLite hyperedge sidecar will follow the evidence sidecar's pattern in
`futon1b/futon1b_text.clj`: a hook after each successful write plus a periodic `catch-up!`
that owns a checkpoint. Evidence is append-only, so its catch-up walks `(at, id)`.
Hyperedges are upserted and retracted, so an `(at, id)` walk misses replacements. The
catch-up needs an order that sees them; XTDB2 system time (`_system_from`) is the
candidate. Whether a query on it seeks or scans decides how P1 is written.

## What to measure (read-only against the running futon1b, :7073)

Use the JVM's own query path, not a new process against the store files: `futon1b` exposes
no eval route to agents, so measure through the HTTP API where you can, and otherwise
through a SEPARATE test JVM opened on a COPY of a small store (e.g. the `migration-store-21`
or a fresh store you populate), never the live store directory. Say which you used.

1. The XTQL/SQL form that returns hyperedge ids with system time after T (and, if
   expressible, of one type), bounded by a limit. Show the query text.
2. Its cost on a type-sized dataset (≥ 100k hyperedges): cold and warm, for T near "now"
   (few rows) and T far back (many rows). Include `EXPLAIN` output and say whether it
   scans or seeks.
3. Whether retractions (`/api/alpha/documents/retract` → XTDB delete/erase) are visible
   to that query or need a separate read (`FOR ALL SYSTEM_TIME`, or the tx log), with a
   measured example.
4. If system-time reads scan: the alternative. At least evaluate (a) reading the XTDB
   transaction log from a tx-id checkpoint, (b) a per-write hook only with a periodic full
   rebuild, giving the rebuild time for the largest type.

## Deliverable

Append a section "P0 measurements" to the design note (futon1b), and commit that one path.
Every number marked measured or estimated. End the section with a one-paragraph
recommendation for P1's catch-up.

## Constraints

- No edits to futon1b server code, no reload, no restart of the live futon1b or futon3c
  JVM. Keep live load light: single sequential requests.
- Commit with `git commit -- <path>`; no stash, amend or branch switch.
- If a test JVM is needed, stop it when you finish and say so.

## Reply

Bell claude-12 back with the commit sha, the query form, scan-or-seek, the timings, and
the recommendation.
