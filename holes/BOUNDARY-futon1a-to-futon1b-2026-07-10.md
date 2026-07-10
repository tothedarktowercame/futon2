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

## Other futon* repos on lucy at the boundary (recorded as-found; not pulled)

Sync = lucy's checkout vs its origin at recording time (2026-07-10
~17:15). "Dirty" counts `git status --porcelain` lines (uncommitted /
untracked paths). These repos are logged just in case; only the four
core repos above were deliberately synced for the boundary.

| Repo | Branch | Commit | Last commit date | Sync | Dirty |
|---|---|---|---|---|---|
| futon0 | classical-nlp/2026-06-08-affect-and-autoclock | `e9254e2` | 2026-07-04 | behind 1 | 4 |
| futon1 | main | `26ab382` | 2026-03-20 | in sync | 0 |
| futon3 | main | `fb51b62` | 2026-07-10 | in sync | 0 |
| futon3a | main | `32dcb09` | 2026-07-10 | behind 2 | 0 |
| futon3b | main | `ad7c824` | 2026-05-03 | in sync | 0 |
| futon4 | main | `6d457c9` | 2026-07-10 | in sync (pulled for boundary 17:30) | 4 |
| futon5 | main | `2e0b1be` | 2026-05-30 | behind 7 | 0 |
| futon5a | master | `08260a7` | 2026-05-31 | in sync | 0 |
| futon6 | master | `ffa6f85` | 2026-07-10 | behind 2 | 0 |
| futon7 | master | `f71edc6` | 2026-06-07 | in sync (re-verified 17:30) | 0 |
| futon7a | master | `136b8fc` | 2026-06-12 | behind 1 | 0 |

Full SHAs: futon0 `e9254e2614b6051fb7f4d63887b1dab8f9be6562` ·
futon1 `26ab382e6721342483403e37d4e2ab59cea47e06` ·
futon3 `fb51b6256aaca74acf5b84f1d3c15f2c09bfbe79` ·
futon3a `32dcb097bc1107fa6caba162d562bac097aa24b8` ·
futon3b `ad7c8247bebe9c42a033fee5276d109763ea839c` ·
futon4 `6d457c9a5681919d4bf2f248419272d74df6dd96` ·
futon5 `2e0b1bec110d111a825ec51a1748885a62beb18e` ·
futon5a `08260a792e0c25546791a3c35b60fb93f17af439` ·
futon6 `ffa6f85a9ae045569603933a7b5339b743411448` ·
futon7 `f71edc62a833006187a8a9bee9bcd54f76e70bfa` ·
futon7a `136b8fc0a2f3f6a1b4afbe00969f3ef5380b965a`.

(`futon1b-sqlite-2026-02` — the relocated pre-XTDB project — has a git
dir but no commits; nothing to pin. futon2's dirty:1 is the untracked
`src/futon2/aif/head.clj`; futon3c's dirty:2 are the local Makefile
CLAUDE_BIN tweak and a bridge .bak file.)

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
