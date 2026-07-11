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

## Other futon* repos on lucy at the boundary (all pulled current 2026-07-10 ~17:40)

Every futon* repo on lucy was fetched and fast-forwarded to its origin
head for the boundary. "Dirty" counts `git status --porcelain` lines
(uncommitted / untracked paths) that survive on lucy — none overlap the
pulled commits.

| Repo | Branch | Commit | Last commit date | Dirty |
|---|---|---|---|---|
| futon0 | classical-nlp/2026-06-08-affect-and-autoclock | `2459665` | 2026-07-10 | 1 |
| futon1 | main | `26ab382` | 2026-03-20 | 0 |
| futon3 | main | `fb51b62` | 2026-07-10 | 0 |
| futon3a | main | `73cd559` | 2026-07-10 | 0 |
| futon3b | main | `ad7c824` | 2026-05-03 | 0 |
| futon4 | main | `6d457c9` | 2026-07-10 | 4 |
| futon5 | main | `b29e190` | 2026-06-10 | 0 |
| futon5a | master | `08260a7` | 2026-05-31 | 0 |
| futon6 | master | `3bdba43` | 2026-07-10 | 0 |
| futon7 | master | `f71edc6` | 2026-06-07 | 0 |
| futon7a | master | `0a1422c` | 2026-07-10 | 0 |

Full SHAs: futon0 `2459665fdc99afe55d6a5ee5e8897738e5bc9d22` ·
futon1 `26ab382e6721342483403e37d4e2ab59cea47e06` ·
futon3 `fb51b6256aaca74acf5b84f1d3c15f2c09bfbe79` ·
futon3a `73cd559a55958fcc3efd865e671f491d4a118d92` ·
futon3b `ad7c8247bebe9c42a033fee5276d109763ea839c` ·
futon4 `6d457c9a5681919d4bf2f248419272d74df6dd96` ·
futon5 `b29e190a50642fb7ca78fe92601e9c7a6f4ef24a` ·
futon5a `08260a792e0c25546791a3c35b60fb93f17af439` ·
futon6 `3bdba439c098a513314336d848ac73f75f8e3c30` ·
futon7 `f71edc62a833006187a8a9bee9bcd54f76e70bfa` ·
futon7a `0a1422cb46a61d05785af12cb8f4c427c9c8354e`.

Dirty paths at the boundary: futon0 `scripts/cr` (local edit; futon0's
other pre-pull local edits — loop-lag.el, futon-config.el additions,
agent-nick.el — turned out to be verbatim-contained in the pulled
commit and were dropped as redundant); futon4 = 4 evidence-viewer
files (~458 lines local, untouched by the pull); futon2 = untracked
`src/futon2/aif/head.clj`; futon3c = local Makefile CLAUDE_BIN tweak
+ a bridge .bak file.

(`futon1b-sqlite-2026-02` — the relocated pre-XTDB project — has a git
dir but no commits; nothing to pin.)

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

## The switchover changes, as actually executed (recorded 2026-07-11, lucy live)

Lucy now runs futon1a-less. Everything below is what another box needs;
narrative + findings live in E-futon1b-operational-switchover and
futon1b/README.md.

### One-time code changes (DONE — just pull)

- **futon1b** ≥ `cfc8607`: full v1 API surface (evidence, entities,
  relations, hyperedge reads, census, types) with penholder + canonical-id
  gates, F4 rescue ladder on every write, `safe-q` fresh-store tolerance,
  **JSON request/response support** (the watchers POST JSON — servers
  older than `cfc8607` silently 500 every watcher write), and
  `-Djava.net.preferIPv4Stack=true` in the `:node` alias (XTDB 2.1.0
  pgwire loopback dies on dual-stack boxes without it).
- **futon3c** ≥ `882fb3f` (branch agency-fixes-2026-06-11): the futon1b
  EvidenceBackend (B1), the `make-evidence-store` selection branch +
  live-probing boot check (B2), the `FUTON1A_PORT=0` disable gate with
  nil-safe consumers (B3), and every hardcoded `:7071` made
  `FUTON1A_URL`-aware (B4).

### Per-box setup (repeat on each box)

1. **Store server JVM** (the transitional second JVM — Phase E folds it
   back): pick a free port (lucy: 7074; nginx owns 7073 there), fresh
   gitignored store dir, run as a systemd user unit
   (`~/.config/systemd/user/futon1b-server.service` on lucy is the
   template — auto-restart matters: setsid orphans die with agent
   sessions). XTDB 2 stores are single-process: one JVM per store dir,
   ever.
2. **Boot env for the futon3c stack** — all four, pre-boot (the URL defs
   are captured at namespace load):

   ```
   FUTON1A_PORT=0                                # disable embedded futon1a
   FUTON3C_EVIDENCE_BACKEND=futon1b              # evidence -> futon1b
   FUTON1B_URL=http://127.0.0.1:<port>           # 127.0.0.1, NOT localhost
   FUTON1A_URL=http://127.0.0.1:<port>           # substrate HTTP -> futon1b
   ```

   `127.0.0.1` is load-bearing: the store JVM binds IPv4-only
   (preferIPv4Stack), and boxes that resolve localhost to ::1 get
   "unreachable" from a healthy server. Plus box-specifics: on lucy
   `FUTON3C_IRC_PORT=0` (ngircd owns 6667) via `make dev-linode`.
3. **Acceptance**: the boot log must show `futon1a DISABLED` →
   `evidence backend: futon1b (…)` → `I-evidence-per-turn boot check: OK
   (futon1b)` — the boot check live-probes the store server's /health.
   Then watch `/health` counts climb as the watcher's first cycles run
   (cycle 1 = baseline + full commit-history backfill on an empty store;
   file-change dispatch starts cycle 2).

### Not part of the operational switch (deliberate)

- **Data backfill (Phase D)** — the store starts empty by design; the
  proven migration pipeline fills it in a convenient window. Until then
  entity-dependent reads run thin.
- **Retiring futon1a from the deps** — Phase E (one JVM again) comes only
  after cutover + backfill, when nothing imports futon1a code.

### Rollback (any point)

Stop the stack, unset the four env vars, boot as before — the embedded
futon1a and its store were never touched. Lucy's store additionally has
the cold sha256-pinned backup above.

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
