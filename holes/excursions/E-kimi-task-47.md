# E-kimi-task-47 — Neo4j sidecar prototype: same data and queries as the SQLite design, measured

**Requisition:** completed — 2026-09-26T09:46:46Z, job invoke-1790415400765-24749-00f5fa24, state done

Clocked in by claude-12 for kimi-6 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet — Neo4j as a sidecar: prototype and measure, for comparison with SQLite

Requested by Joe, 2026-09-26: "neo4j (graph database but not bitemporal) could be used as a
sidecar for such things, maybe worth building that in parallel so we can compare".
Reviewer: claude-12. Runs in parallel with E-kimi-task-46 (kimi-5), which designs a SQLite
sidecar for the same data. Use the same data and the same queries so the numbers compare.
The SQLite packet text is at `/tmp/claude12-packet-sidecar.md` — read its "Why now" and
"superpod" sections for the facts; do not duplicate its design work.

This is a throwaway prototype plus one note. No futon1b server code changes, no reload or
restart of any JVM.

## Setup

- Neo4j is not installed. Java 21 is (`/usr/bin/java`). Download the Neo4j **Community**
  tarball (a 5.x release that runs on Java 21) from the official distribution
  (dist.neo4j.org or neo4j.com/download-center), verify its published SHA-256, and unpack
  under `/tmp/neo4j-proto/`. Nothing under `~/code`, no system install, no sudo.
- Configure it to listen on 127.0.0.1 only (bolt 7687, http 7474; pick other ports if those
  are taken), heap ≤ 8 GB, page cache ≤ 8 GB. Set an initial password in the config dir;
  do not commit it anywhere.
- Talk to it through the HTTP transactional endpoint (`/db/neo4j/tx/commit`) with Python's
  urllib, or `cypher-shell` from the tarball. No pip installs.
- Load with batched `UNWIND` or `neo4j-admin database import` from CSV — your choice; say
  which and the load time.
- Stop the server when you finish (`bin/neo4j stop`) and say so. Leave `/tmp/neo4j-proto`
  in place for the reviewer.

## Data (same as the SQLite prototype)

1. v05 commits and edits from the cached futon1b pages `/tmp/v05-cache/commit/page-*.edn`
   (19 pages) and `/tmp/v05-cache/edits/page-*.edn` (160 pages). EDN text; a regex parse
   is fine (see `futon6/scripts/mission_activity.py` `v05_parse_commits` /
   `v05_parse_edits`). Model: `(:Commit {sha, repo, ts, subject})-[:EDITS]->(:Var {id})`.
2. Superpod scopes from `/tmp/r7v/artifacts/expo/*.edn` (1,111 files; each has
   `:paper/id`, `:passage/id`, `:source {:lines [a b]}`, `:scopes` with `:id`, `:kind`,
   `:source {:lines}`, `:slot-fill`) and, if time allows, proof graphs from
   `/tmp/r7v/artifacts/graphs`. Model papers, passages, scopes and (optionally) proof
   nodes as nodes with the line spans as properties.

## Queries to time (cold first run and warm repeat, each)

- one commit's edits, by sha;
- edit counts per repo;
- commits sharing edited vars with a given commit (a two-hop query; this is where a graph
  store should help — compare with the SQL self-join in the same note);
- scopes of paper P overlapping lines [a, b];
- all scopes of kind K across papers;
- if proof graphs are loaded: from a proof node to the scopes over the same span.

Create the indexes each query needs and list them.

## Deliverable

`futon1b/holes/PROTO-neo4j-sidecar-2026-09-26.md`, with:

1. Setup: version, sha256, config lines changed, load method and load time, store size on
   disk, RSS of the server after load.
2. The query table: Cypher text, cold and warm times, row counts. Mark every number
   "measured" or "estimated".
3. Row counts loaded, checked against a separate count from the source files (say how).
4. What Neo4j cannot do here that the futon1b design depends on: bitemporality (valid-time
   and system-time as-of reads), candidate-ID re-check against XTDB, rebuild from the
   write log, and the operational cost of a second server process to keep running. Say
   for each whether a workaround exists and what it costs.
5. Extrapolation to 5,000 papers (≈600× the scope data) and to all 551,794 v05 edits.

## Constraints

- Commit only the note, in futon1b, with an explicit path (`git commit -- <path>`). No
  stash, amend or branch switch. Leave kimi-5's note alone.
- Read-only against futon1b :7073 (you should not need it at all).
- Ports: check they are free first; bind to 127.0.0.1 only.

## Reply

Bell claude-12 back with the commit sha, the query table in brief, load time and store
size, and whether the server is stopped.
