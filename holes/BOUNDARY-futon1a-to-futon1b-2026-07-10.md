# BOUNDARY — futon1a history stops here; futon1b history starts (2026-07-10)

Written on lucy at the start of the operational conversion (the stack
switches to futon1b *operationally first*; data backfills later through
the proven migration pipeline). Everything before this line is futon1a
history; everything the stack writes after the switchover lands in
futon1b. This file is the reference point for "which store owns which
records" questions later.

## Repo state at the boundary (all in sync with origin)

| Repo | Branch | Commit | Subject |
|---|---|---|---|
| futon1a | substrate-2-liveness-2026-06-25 | `a71c399a78cdad46d0aba5f6e39567db7d8465ed` | Evidence snapshot scope; export progress atom; hyperedge no-op write guard |
| futon1b | master | `b619cd2210151cbb5daa26cc7587e178e56b4398` | textprobe: P1 measured — divergence + token-economics scripts and results |
| futon2 | main | `a3f4c38f3c0cad2f38a707b313ba7b5699988270` | M-text-sidecar: day-one checkpoint — P1 answered on lucy store copy |
| futon3c | agency-fixes-2026-06-11 | `26413024d6477ddcd2552b7ee92e73398890da87` | Watcher: no-op client cache; futon1b dual-write; heartbeat spam fix |

Short forms: futon1a `a71c399` · futon1b `b619cd2` · futon2 `a3f4c38` ·
futon3c `2641302`.

## Data state at the boundary (lucy)

- **Lucy's futon1a store** (`~/code/storage/futon1a/default`, 6.4G:
  doc-store 615M, index-store 2.9G, tx-log 303M, `proof-path.log.edn`
  2.6G) backed up cold (no JVM running, no open file handles) to:
  - `~/code/storage/futon1a-backup-2026-07-10.tar.zst` (1.6G,
    zstd -6, integrity-tested)
  - sha256 `6afade7fe7b274d4d8e88be258806b7f297fa4307d4796d6ee71d5b7faad9769`
- `~/code/storage/futon1a-copy` (9.3G) is NOT lucy's store — it is a
  private copy of the primary box's store used for textprobe history
  extraction (M-text-sidecar). Distinct provenance; do not confuse the
  two when backfilling.

## Local-state footnotes (lucy)

- futon3c local WIP (busy badge, site-qualified framing, London
  federation) is parked on branch `irc-federation-wip-2026-07-10`;
  superseded working-tree drafts are in stash
  `pre-pull local edits 2026-07-10`. Not part of the boundary.
- The pre-XTDB futon1b (Feb-2026 SQLite project, never committed) was
  moved intact to `~/code/futon1b-sqlite-2026-02`.
- nginx holds :7073 on lucy — the futon1b server needs a non-default
  port here (e.g. `--port 7074`).

## What "conversion" means from here

1. Grow `futon1b_server.clj` to the ~15-route substrate surface the
   stack actually calls (evidence POST/get/query/count/sessions;
   hyperedge POST/get/query; entity POST/get/latest; relation POST;
   census; types + types/parent + types/merge; snapshot), carrying the
   futon1a write-path model logic (penholder gate, L4 id-contract,
   sanitize) and the F4 rescue ladder from `migration/ingest.clj`.
2. Boot the futon3c stack pointed at the futon1b server; iterate on
   404s/500s until it runs clean.
3. Backfill futon1a data whenever convenient (pipeline is proven;
   expect more F4 rescue-stage hits when backfilling into a store
   already shaped by live writes — keep the rescue logs).
