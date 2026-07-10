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
