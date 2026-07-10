# E-futon1a-to-futon1b-migration-pipeline — full-store export/import pipeline

**Status: IN PROGRESS — S1–S3 RUN LIVE for evidence+graph+types (2026-07-10,
see dated section at end): 147,893 docs migrated and parity-spot-checked;
Zai memory seam demonstrated on the real corpus. QUIET WINDOW FULLY STAGED
(2026-07-10 later section): evidence snapshot scope live on :7071, verify
rewritten, runbook ready — awaiting operator "go".
Driver: zai-1. Reviewer: claude-16. 2026-07-10 run + fixes: claude (Fable).
Operator gate: Joe.**

## Why this excursion exists

M-futon1b-port proved the XTDB 1.x → 2.x translation is faithful on a
slice: 33 per-site assertions across 8 idiom families, a 15-query parity
harness showing byte-identical results, and 4 hazards catalogued. What it
did NOT build is the pipeline to move the *entire* live futon1a store into
a futon1b node. That was explicitly deferred — this excursion picks it up.

The pipeline, once built and verified, should be runnable ≈overnight or at
operator convenience — it is infrastructure, not a one-shot migration.

## The live store (measured 2026-07-04)

- **Location:** `/home/joe/code/storage/futon1a/default/` (RocksDB, three
  column families: tx-log, doc-store, index-store).
- **Size:** ~6.7 GB on disk, ~247M estimated keys (from
  `:xtdb.kv/estimate-num-keys` on the `:7071` health endpoint).
- **XTDB version:** 1.24.4 (RocksDB backend).
- **Serving process:** futon3c JVM (PID varies), serving on `:7071`.

## Goal

A reproducible, batched export/import pipeline that:

1. **Exports** all documents from the live futon1a `:7071` store (read-only
   — no writes, no restart, no second serving process). The export path
   must respect I-0 (the serving JVM stays up throughout).
2. **Transforms** 1.x document shapes to 2.x-compatible shapes. This
   includes the tx-op name translations (`:xtdb.api/put` → `put-docs`,
   `:xtdb.api/delete` → `delete-docs`, `:xtdb.api/evict` → `erase-docs`),
   the `:hx/props` denormalization decision (D-5 in M-futon1b-port), and
   the `:evidence/body` pruning (string-keyed maps rejected by XTDB 2).
3. **Ingests** into a futon1b XTDB 2 node via `put-docs`, batched to stay
   within dev-process memory bounds (the `-Xmx1g -XX:MaxDirectMemorySize=1g`
   caps in `futon1b/deps.edn`).
4. **Verifies** via checksum (the `migrate_futon1.clj` canonicalization +
   SHA-256 pattern from futon1a, or a count/sample parity check).

The pipeline should be a script (or small set of scripts) in
`/home/joe/code/futon1b/` that an operator can run with one command.

## Hard constraints (inherited from M-futon1b-port)

- **I-0:** the live `:7071` futon1a store and the futon3c serving JVM must
  not be touched — no writes, no restarts, no second serving process.
  Read-only queries against `:7071` are permitted (the memory layer reads
  that store continuously; reads are not an I-0 violation).
- **One dev JVM at a time:** let each `clojure -M:node` run finish before
  starting the next; never background them. ALWAYS pass
  `{"timeout_ms": 300000}` on run_shell for JVM-booting commands.
- **No commits** unless the operator directs otherwise.
- Gates on any Clojure: clj-kondo (0 errors) and
  `futon4/dev/check-parens.el`.

## What M-futon1b-port already provides (ready vs missing)

### Ready (proven artifacts in `/home/joe/code/futon1b/`)

| Ready | Source | Evidence |
|---|---|---|
| XTDB 2.0.0 in-process node starts and ingests via `put-docs` | `deps.edn`, `p1_ingest.clj` | P1: 1378 hyperedges + 200 entities, 15 assertions |
| All query idioms translate to XTQL (8 families) | `p2_queries.clj`, `p2b_compound_queries.clj`, `p2c_remaining_sites.clj` | 33 assertions, all PASS |
| Tx-op name translations known: put→put-docs, delete→delete-docs, evict→erase-docs | `p2_queries.clj` tx-op forms | 3 assertions PASS |
| Parity harness design (twin-node, strict diff) | `parity_2x.clj`, `parity-1x/`, `p3_parity_harness.sh` | 15 queries / 24 lines byte-identical |
| Hazard catalog H1–H4 | `NOTES.md` | every known silent-wrong-result trap |
| `:hx/props` denormalization fn (`denorm-hx`) | `p2b_compound_queries.clj` | D-5 Option B, proven |
| `:evidence/body` pruning (string-keyed maps rejected by XTDB 2) | `parity_2x.clj` ingest | both parity sides prune identically |
| futon1a migration script with canonicalization + checksum | `futon1a/src/futon1a/scripts/migrate_futon1.clj` | logical export/import with SHA-256 verification |

### Missing (the work this excursion does)

| Missing | Why | Notes |
|---|---|---|
| **Full-store export path** | The slice was hand-built by claude-16. No scripted export-from-live-store path exists. | The `migrate_futon1.clj` `list-entity-ids` + `xtdb/entity` pattern is the reference, but it runs against a local node, not the `:7071` HTTP API. Need either an HTTP-based export or a read-only RocksDB node opened against the same data dir (careful: RocksDB allows multiple readers but the lock semantics need checking). |
| **Batched ingest for ~247M keys** | P1 ingested 1378 docs in one `put-docs` tx. The full store is orders of magnitude larger. | Needs batching (the `migrate_futon1.clj` batch=200 pattern), progress reporting, and memory management within the 1 GB heap cap. |
| **Doc-shape transform layer** | 1.x docs have shapes 2.x rejects: string-keyed maps in `:evidence/body`, nested `:hx/props` (H4), possibly others not surfaced by the slice. | A full export will encounter data types the slice didn't contain (`:doc/book`, `:relation/type`, `:type/kind`, `:model/descriptor`). Each may have its own transform needs. |
| **Full-store verification** | The slice used count assertions against a manifest. The full store needs either a SHA-256 checksum comparison (the migrate pattern) or a sampled parity check. | A full parity harness run (all queries, both nodes) may be impractical at this scale; a checksum + sampled-query parity is the likely compromise. |
| **The ~10 P2 addendum query sites** | `:doc/book`, `:relation/type`, `:type/kind`, `:model/descriptor` — absent from the slice, untested. | These use the same proven XTQL idioms. Once the full store (or a supplementary export) is loaded, they can be asserted. |

## Open design questions — RESOLVED (2026-07-05 by zai-1)

- **Q1 — export mechanism: HTTP API.** A second RocksDB node against the
  same data directory is **I-0 incompatible** — each column family has a
  `LOCK` file held by the serving JVM (PID varies). XTDB 1.24.4 has no
  read-only mode that bypasses the RocksDB file lock. The HTTP path is
  the only safe export:
  - **Entities + relations (~85k docs):** `POST /api/alpha/snapshot` with
    scope "latest" — filesystem write under `data-dir/snapshots/`, NOT an
    XTDB write. I-0 safe.
  - **Hyperedges (~500k+ across types):** `GET /api/alpha/hyperedges?type=X&limit=<count>`
    per type. Census endpoint gives per-type count; set limit=count to get
    all docs of that type in one call.
  - **Evidence (~53k docs):** `GET /api/alpha/evidence/sessions` gives
    session list + counts; then `GET /api/alpha/evidence?session-id=X&limit=N`
    per session. The unscoped `/evidence` endpoint times out (full scan
    + sort); the session-scoped query is indexed and fast.
  - **Type catalog (~207 docs):** `GET /api/alpha/types`.
  - No "list all docs" / "dump" endpoint exists, and adding one would
    require a restart (I-0 violation).

- **Q2 — transform completeness: FAIL LOUD.** Three transform needs
  identified and proven on the seed slice (12/12 assertions PASS):
  1. `:evidence/body` string-keyed maps (JSON keys) → stringify entire map.
  2. `:hx/props` nested maps → denormalize to `:prop/<key>` columns (H4).
  3. **Bare symbols** (from .sexp file parsing — e.g. `graph-symmetry`,
     `operational`) → stringify. XTDB 2 rejects symbols as document values.
     This was found via ingest failure, not predicted by the slice hazard
     catalog. Added to the transform layer.
  The transform walks all nested structures recursively, stringifying
  non-keyword-keyed maps and symbols. Unknown doc types are logged, not
  silently dropped.

- **Q3 — verification: checksum + sampled parity.** Both implemented in
  `migration/verify.clj`. Checksum uses the `migrate_futon1.clj`
  canonicalization + SHA-256 pattern. Sampled parity compares futon1a
  census counts against futon1b XTQL query counts for representative
  hyperedge types.

## Steps (proposed — the driver refines)

- **S1 — export probe.** Determine the export mechanism (Q1). If HTTP:
  find or add a list-all-docs endpoint. If read-only node: verify
  RocksDB lock semantics. Export a medium chunk (say, all entities or
  all hyperedges of one type family — ~10k docs) to prove the path.
- **S2 — transform layer.** Build the 1.x → 2.x doc-shape transform,
  fail-loud on unknown shapes. Test against the medium chunk. Extend
  for the data types the slice didn't cover.
- **S3 — batched ingest.** Scale up: ingest the medium chunk into a
  futon1b node, batched, with progress reporting and memory monitoring.
  Verify via count assertions.
- **S4 — full run.** Export → transform → ingest the entire store. This
  is the overnight run — it may need a larger heap cap (the 1 GB cap is
  for slice workloads; the full ingest may need `-Xmx2g` or more, which
  is an operator decision since it coexists with the serving JVM).
- **S5 — verify.** Checksum comparison (if feasible) + sampled parity
  harness from P3's query set. P2 addendum assertions for the newly-
  available data types.

## Kill criteria

- **K1:** the export path cannot be built without writing to or
  restarting the live store (I-0 violation).
- **K2:** the transform layer encounters a doc shape that cannot be
  faithfully represented in XTDB 2 (a real translation blocker, not a
  pruning-and-log situation).
- **K3:** the full ingest cannot complete within resource bounds (heap,
  disk, time) on this box.
- **K4:** operator recall.

## Relationship to other missions

- **Parent:** M-futon1b-port (COMPLETE). This excursion consumes all its
  proven artifacts, hazards, and design decisions.
- **Feeds:** a future futon1a-succession mission (if the pipeline proves
  the full store migrates cleanly, the operator can decide whether to
  cut over — that decision is explicitly not this excursion's scope).
- **Consumes:** E-futon1b-foothold (CLOSED — the idiom proofs), the
  futon1a `migrate_futon1.clj` pattern (canonicalization + checksum).

## 2026-07-10 — first live run (evidence + graph + types); findings F1–F7

Audit + partial S4 by claude (Fable). All seed-slice gates (T1–T7, A1–A5,
seam-swap A1–A3, transform self-test) re-run and PASS before touching the
live store. Export legs run were the documented safe-under-load ones only;
hyperedges untouched (quiet window).

**Migrated into `futon1b/migration-store` (persistent, on-disk):**
entities 44,700 · relations 46,685 · evidence 56,301/56,301 (sessioned) ·
type-catalog 207. Zai memory seam exercised against the real corpus:
`futon1b/zai_memory_1b.clj` (reusable seam fn, adds `:since`) +
`futon1b/zai_memory_examples.clj` (E1–E4 recall PASS, E6 parity vs live
:7071 3/3 PASS, E5 documents F5 below).

**Findings (each found by running, all fixed in-pipeline except F5/F7):**

- **F1 — double-keywordized keywords in the store.** Type catalog carries
  `(keyword ":pattern/*")`, which prints as `::pattern/*` — unreadable EDN.
  Fix: string-literal-aware sanitizer in `export.clj` (`sanitize-edn-str`),
  applied at both ingress points, every replacement counted into
  `!sanitize-log` → `export-summary.edn`.
- **F2 — `#futon1a/instant` tags unread.** Snapshot files use the server's
  `futon1a.api.snapshot/edn-readers`; export now mirrors it, parsing to
  `java.util.Date` (EDN-native `#inst` round-trip). `transform.clj` passes
  `inst?` values through — probed: XTDB 2 stores them as native temporals
  (read back as ZonedDateTime). ~13.5k entity timestamps stay typed.
- **F3 — unencoded session-ids.** 17 sessions with spaces/parens in ids
  (e.g. `claude-4 (awaiting session)`) 400'd. Fix: URL-encode. Evidence
  export now 0 errors.
- **F4 — Arrow "Unknown type: NULL" is STATEFUL.** 914 cursor-telemetry
  evidence docs failed ingest; bisection showed the same doc passes on a
  fresh table and fails after other docs shape the column's type union —
  no shape-level rule can predict it (the slice has near-identical shapes
  that pass). Fix: runtime rescue ladder in `ingest.clj`
  (`put-doc-with-rescue!`): batch fails → per-doc retry → rescue-1
  (stringify nil-risky values) → rescue-2 (stringify deep colls, keeping
  flat scalar colls like `:evidence/tags` queryable). All 914 rescued at
  stage 1, 0 unrescuable, every rescue shape-logged.
- **F5 — OPEN: sessionless evidence is invisible to the export.** Evidence
  with no `:evidence/session-id` (mission-sync records, author
  `mission-control/sync`, others unknown) is neither listed nor counted by
  `/api/alpha/evidence/sessions` — "56,301 total" is the *sessioned* total.
  The unscoped GET pulls+sorts the whole table server-side (unsafe under
  load). Proper fix for the quiet window: add an `evidence` snapshot scope
  to `futon1a/api/snapshot.clj` using the same `open-q` id-drain already
  added there for hyperedges, then export + ingest that leg.
- **F6 — verify.clj weaknesses (pre-existing, not yet fixed):** (a) the
  "checksum" layer compares full transformed source docs against a 2-column
  `[xt/id hx/type]` projection of different rows — the two hashes can never
  match; it's decorative. (b) The source-vs-dest hyperedge total reads
  `hyperedges-manifest.edn`, which the snapshot path never writes (only the
  superseded census path does) — and prints a misleading PASS on 0=0.
  (c) The final verdict ignores that comparison and rests only on 3 sampled
  census parities. Recommend a rewrite pass before the full S5.
- **F7 — cosmetic:** XTDB 2.0.0 emits an Arrow allocator "Memory was leaked
  by query" IllegalStateException at node close after count queries; occurs
  after commits, data verified intact across reopen. Worth re-checking on
  a newer 2.0.x.

**Architecture constraint confirmed (for the Zai seam wiring):** futon1a
pins `com.xtdb/xtdb-core 1.24.0`; futon1b needs the same coordinates at
2.0.0. One classpath holds one — the futon3c serving JVM (which embeds
futon1a) can NEVER load XTDB 2 in-process. The memory seam
(`futon3c.peripheral.memory-backend`) must reach futon1b out-of-process:
either a thin futon1b query server (I-0 stance on second serving JVMs needs
an operator decision), a per-query subprocess, or full cutover. Decision:
Joe.

**Quiet-window checklist (S4 completion + S5):** (1) hyperedges snapshot
export (~246k, `export-hyperedges-snapshot`, needs quiet :7071);
(2) sessionless-evidence leg (F5 — small server-side scope + reload, then
export/ingest); (3) re-run graph+evidence legs in the same window if
cross-population consistency matters (entities/evidence moved on since
2026-07-10); (4) fix verify.clj (F6) and run full S5 with `--store-dir
migration-store`; (5) ingest may want `-J-Xmx2g` (used today for the 104 MB
evidence file; box stayed healthy).

## 2026-07-10 (later) — quiet-window staging complete, short of "go"

Joe's decisions: sessionless evidence WILL be migrated; second JVM approved
for the Zai seam; checksum fix worth doing. All staged:

- **`evidence` snapshot scope LIVE on :7071** (added to
  `futon1a/api/snapshot.clj`, open-q id-drain mirroring hyperedges;
  Drawbridge-reloaded, verified via invalid-scope introspection — NOT yet
  run). Counts include `:sessionless`.
- **verify.clj REWRITTEN (F6 fixed):** layer 1 counts from the export files
  actually ingested (absent = SKIP, in-verdict); layer 2 real per-doc
  checksum — stride sample, same-id fetch (project source keys; `[*]` binds
  nothing in XTDB 2.0.0), rescue-stage-aware via ingest-summary's new
  `:rescued-{1,2}-ids`, temporals unified to ISO instants, nil map entries
  dropped (XTDB 2 stores nil struct fields as absent — verified). Verdict =
  AND over all three layers.
- **F8 (new): the session-scoped evidence export emits ~918 duplicate-id
  docs** (56,301 file docs → 55,383 unique ids on a fresh single-run store).
  This is what made the earlier multi-run store unverifiable. Resolution:
  the `evidence` snapshot scope is duplicate-free by construction (id-drain)
  — in the quiet window re-export evidence via the scope and the
  session-scoped path becomes bootstrap-only. Why sessions overlap ids is
  still unexplained (~1.6%, cursor-telemetry-heavy) — worth a look.
- **Store rebuilt fresh** (single-run, rescue ladder, summary↔store 1:1):
  entities/relations/type-catalog counts + checksums all PASS; evidence
  FAILs are exactly the F8 duplicates. One checksum edge: double-slash
  keywords (`:futon1a/backfill/orig-type`) can't be projected as XTQL
  columns — 1 entity affected in the sample; treat as known-noise or
  special-case.

### Query handles for exploring migrated evidence (post-migration)

Absent columns bind null in XTQL (verified): sessionless docs =
`(-> (from :evidence [xt/id evidence/author evidence/type evidence/session-id])
     (where (nil? evidence/session-id)))`
— group by author/type in Clojure, or invert with `(not (nil? …))`.
`zai-memory-1b/memory-search` covers tag/author/type/since recall;
`zai_memory_examples.clj` E5 flips from expect-0 to real results once the
evidence-scope leg lands.

### Quiet-window runbook (operator "go" required)

```
cd /home/joe/code/futon1b
# 1. evidence via new scope (also captures sessionless; duplicate-free):
clojure -M:node -e '(require (quote [migration.export :as ex])) (ex/export-via-snapshot "http://localhost:7071" "migration-export" "evidence" "evidence.edn" 600000)'
# 2. hyperedges (~246k, needs quiet JVM):
clojure -M:node -e '(require (quote [migration.export :as ex])) (ex/export-hyperedges-snapshot "http://localhost:7071" "migration-export")'
# 3. optional same-window graph+types re-export for cross-population consistency
# 4. fresh single-run ingest (rm -rf migration-store first):
clojure -J-Xmx2g -M:node -m migration.ingest --input-dir migration-export --store-dir migration-store
# 5. full verify (all three layers + census parity):
clojure -J-Xmx2g -M:node -m migration.verify --input-dir migration-export --store-dir migration-store
```

Next dispatch after the window: thin futon1b query server (second JVM
approved by Joe 2026-07-10) exposing `memory-search` over HTTP for the
futon3c seam — good Codex bell candidate.

## 2026-07-10 (quiet window, part 2) — F9/F10: the 5.74M hyperedges explained; no-op write guard LIVE

- **F9 — duplicate WRITES root cause + fix (Joe's question):** the futon3c
  file watcher re-POSTs unchanged hyperedges every pass; server put them
  unconditionally (sampled: 63 versions / 2 distinct contents). Fixed
  server-side in `routes/compat-upsert-hyperedge`: fetch current doc, skip
  identical plain puts, return `:no-op? true` (retracts + explicit
  valid-time puts unaffected; no entity-history consumers exist).
  **Reloaded + verified live**: repeat POST → no-op, version count stable.
  Test hazard worth remembering: POSTing FLAT endpoints for a doc stored
  with ROLE-carrying `:hx/ends` REPLACES it role-less (upsert-REPLACES);
  restored byte-identically with rich endpoint maps.
- **F10 — the hyperedge population, measured** (`hx-id-type.tsv`, 1GB,
  streaming drain — snapshots dir): 5,745,489 DISTINCT ids, of which
  **5,087,183 (88.5%) are `:code/v05/watcher-event`** — watcher telemetry
  indexed as graph docs (Joe: should have gone to ~/data/storage
  unindexed). Real graph ≈ 658k (edits 230k · var 128k · calls 92k ·
  contains 76k · tests 30k · …). Census's "dead names" (edits, var) were
  in fact the 2nd/3rd biggest types. NOT version-duplication (open-q dups
  ≈ 2.6k only). Unfiltered snapshot fetch OOM'd the serving JVM at ~2.5M
  docs (JVM survived, future contained it) — full-fetch is off the table.
- **Migration decision needed (recommended): exclude watcher-event.**
  Migrate the ~658k real-graph docs via CHUNKED export: filter
  `hx-id-type.tsv` offline → id list; server-side future fetches in 10k
  batches, streams `hyperedges-NNN.edn` chunk files (~50k docs each,
  edn-safe instants); ingest.clj learns to glob `hyperedges-*.edn`. One
  700MB single-form EDN cannot be read at any sane heap.
- **Still open:** stop the watcher writing watcher-event hyperedges into
  the indexed store going forward (route to flat storage) — futon3c
  watcher change, good Codex bell. Also: erase/expire the existing 5.09M
  watcher-event docs in futon1a? (erasure = `erase.bb`, penholder joe,
  dry-run first — operator decision, big win for store size + the ~30s
  query timeouts.)
- Evidence leg re-ingested from the scope export (90,583 incl. 34,204
  sessionless) — fresh single-run store rebuild in progress at write time.

## 2026-07-10 (part 3) — watcher fixed at source; REINDEX-NOT-PORT decision; bulk erase running

Joe's decisions: (1) watcher must not spam the DB — fixed at source;
(2) erase ALL 5.09M watcher-events; (3) **hyperedge migration leg is
CANCELLED — replaced by one-time reindex**: the code graph is derived
data (source + git), so futon1b gets it by pointing ingestion at it
once, not by porting 658k rows. The chunked-export design above is
therefore moot (kept for the record).

- **Watcher heartbeat fixed** (`futon3c.watcher.multi/heartbeat!`,
  reloaded live, cycle 1311, `defonce` state intact): emits only on
  cycles with activity, under a STABLE per-root id (endpoints no longer
  carry cycle-n → server-derived id is constant → replace-in-place).
  Liveness = process-watchdog's job (CYDER), per the README's own design.
  Idle-heartbeat spam (~10 roots × 12 cycles/min ≈ measured ~900/10min)
  → ~0. Real file events (ingest/deletion/rename/move) keep per-event
  records. Client cache in file_ingest + server no-op guard remain as
  backstops (three layers, each verified).
- **Bulk erase RUNNING**: 5,087,183 watcher-event ids (filtered from
  hx-id-type.tsv → watcher-event-ids.txt) through
  `pipeline/run-erase!` as penholder joe, 5k/batch (~1,018 audited txs),
  reason recorded, 3-id CLI dry-run + 100-id live pilot verified first
  (erased + gone + server healthy). Progress via
  `futon1a.api.snapshot/!progress`.
- Follow-ups: re-measure hyperedge growth in ~a day (expect ≈ real
  activity only); the erase leaves ~658k real-graph docs — decide
  whether futon1a keeps serving those or the reindex strategy applies
  there too when futon1b becomes primary.

## 2026-07-10 (part 4) — futon1b server + watcher dual-write BUILT & smoke-tested

The reindex-not-port seam is now real code:

- **`futon1b/futon1b_server.clj`** — the approved second JVM. One XTDB2 node
  owning the store, HTTP/EDN on **:7073** (7072 taken). Endpoints:
  `GET /health` (table counts) · `POST /api/alpha/hyperedge`
  (watcher-compatible EDN; stable ids identical to futon1a's scheme;
  server-side no-op guard; VERIFIED put via the rescue ladder — immune to
  the XTDB 2.0.0 silent batch-drop) · `GET /api/alpha/memory/search`
  (§12.3 Zai envelope — THE out-of-process memory seam).
  Run: `clojure -M:node -m futon1b-server --store-dir migration-store --port 7073`
  (single-process store: never while a dev JVM holds migration-store).
  Smoke-tested on a scratch store: health / put / no-op re-put / changed
  put / retract / memory-search all PASS.
- **Watcher dual-write** (`file-ingest/post-futon1b!`, wired into BOTH
  write paths — file_ingest post-hx* and multi post-hyperedge!): posts EDN
  to futon1b after a successful primary write. Gated on
  `file-ingest/!futon1b-url` (nil = off; RELOADED LIVE dormant).
  Enable: `(reset! futon3c.watcher.file-ingest/!futon1b-url "http://localhost:7073")`.
  futon1a stays primary; futon1b failures log, never fail the write.
  Valid-time replays are primary-only (v1).
- **Switch-over sequence** (when rebuild+verify done): start server on
  migration-store → enable dual-write → cold-scan tick
  (`futon3c.watcher.multi/tick!` after start! with :cold-scan? true, or a
  replay) = the one-time reindex → futon1b hyperedges fresh, no port.
- **Ops finding: erase batches stall :7071 writes** (~2.5 min per 5k-evict
  tx; a simple hyperedge POST wedged behind it). Erase PAUSED via the
  armed watch at the next batch boundary — resume from
  `snapshots/erase-checkpoint.txt` (idempotent, redoes ≤2 batches) via the
  relaunch script in scratchpad / this doc's part-3 section, ideally
  overnight and with SMALLER batches (500/tx ≈ ~15 s stalls instead of
  2.5 min).
- Dual-write e2e test was inconclusive ONLY because the smoke server's
  150 s self-timeout expired while the test's primary POST sat behind an
  erase batch — re-verify e2e at real deployment (step 2 above).

## 2026-07-10 (part 5) — F11: XTDB 2.0.0 DURABILITY LOSS — switch-over BLOCKED pending engine upgrade

**F11 (supersedes the F-series' "silent batch-drop" reading):** rows verified
present during the writing JVM's lifetime are ABSENT after reopen.
Evidence: verified-put rebuild logged ZERO silently-dropped (every id
point-verified present right after its batch, only ~30 batches even needed
the rescue ladder), yet the reopened store scans 84,403–84,404 of 90,583
evidence docs (~7% loss); the count DRIFTS between reads/reopens
(84,615 → 84,404 → 84,403); point-lookups confirm true absence; losses are
scattered uniformly (not tail batches); entities/relations/type-catalog
(smaller, simpler docs) are untouched. Every node close throws the Arrow
"Memory was leaked by query … Allocator(live-index)" IllegalStateException
— now presumed the smoking gun: incomplete live-index flush at shutdown.
Earlier "silent batch-drop" observations were most likely THIS bug seen
through reopens.

**Consequences:** futon1b on XTDB **2.0.0** is NOT trustworthy for
switch-over. The pipeline itself is sound (verified-put proves the writes
land; the loss is at persist/replay). Next actions, in order:
1. Bump `futon1b/deps.edn` to the latest XTDB 2.x and re-run: slice gates →
   full rebuild → reopen-verify (the same three-layer verify). If the loss
   vanishes, proceed to server deploy + dual-write + cold-scan reindex.
2. If it persists on latest: minimal repro (write N docs w/ large nested
   bodies, close, reopen, count) → report upstream; consider the XTDB2
   remote/server module (different persistence path) instead of :local.
3. Until then: futon1a remains the store of record; the Zai seam demo
   remains valid as a READ demo, but no live memory writes to futon1b.

NB the verified-put ingest + rescue ladder remain necessary regardless —
they are the reason F11 could be isolated to the persistence layer at all.

## 2026-07-10 (part 6) — F11 RESOLVED: XTDB 2.1.0 fixes the durability loss

Upgrade `2.0.0 → 2.1.0` (Joe was right to expect newer; 2.1.0 + 2.2.0-beta1
exist on Maven). Drop-in compatible: 12/12 slice gates unchanged. The acid
test — evidence-only ingest into a fresh store, close, REOPEN in a separate
JVM — comes back **90,583/90,583, missing-vs-file 0**, stable across reads.
The whole pathology family vanished together on 2.1.0: no "Unknown type:
NULL" batch failures (0 rescues where 2.0.0 needed 5,827), no silent drops,
no Arrow live-index allocator leak at close. This triangulates F11 to a
fixed-in-2.1.0 engine bug around live-index flush; no upstream PR needed,
though the repro (large nested-body docs → batch put → close → reopen →
count, on 2.0.0) is documented here if a regression report is ever useful.
Verified-put ingest + rescue ladder stay in the pipeline as regression
sentinels (cheap insurance; they now no-op).

**Switch-over UNBLOCKED.** Store of record for futon1b = `migration-store-21`
(XTDB 2.1.0). Remaining sequence: graph+types ingest (running) → full
three-layer verify → Zai examples (E5 mission-sync recall) → futon1b-server
on :7073 over migration-store-21 → enable dual-write → cold-scan reindex.

## 2026-07-10 (part 7) — SWITCH-OVER EXECUTED

- **Full S5 verify on migration-store-21: PASS** — all four populations,
  counts exact (evidence 90,583/90,583 incl. 34,204 sessionless),
  checksums 0 mismatches after two verify refinements: (i) **H5, new
  hazard: XTDB 2 case-folds nested struct keys** — `:G` is stored/returned
  as `:g` (verified on e-portfolio-* bodies); key case is NOT a preserved
  property; consumers beware. (ii) double-slash keys
  (`:futon1a/backfill/orig-type`) are unprojectable in XTQL → reported
  UNVERIFIABLE, not failed.
- **Zai examples: E5 flipped** — mission-sync (sessionless) memories
  recalled through the seam; E6 parity 3/3 vs live :7071.
- **futon1b-server DEPLOYED**: systemd user unit `futon1b-server`
  (survives sessions) on :7073 over migration-store-21;
  `systemctl --user status futon1b-server`. Health + memory-search over
  HTTP verified against the full corpus.
- **Dual-write ENABLED** (`!futon1b-url` → http://localhost:7073) and
  **cold-scan reindex TRIGGERED** (per-root cache cleared at cycle 1661 —
  the next cycle re-dispatches every watched file; hyperedges flow to
  futon1b as the re-mine proceeds; futon1a re-posts are absorbed by the
  no-op guards).
- Remaining background: the 5.09M watcher-event erase (~28h, checkpointed)
  and the reindex itself (watcher pace). Store `migration-store` (2.0.0,
  lossy) is superseded by `migration-store-21` and can be deleted in any
  cleanup pass.
