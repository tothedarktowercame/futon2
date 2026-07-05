# E-futon1a-to-futon1b-migration-pipeline — full-store export/import pipeline

**Status: OPEN (excursion, opened 2026-07-04 by operator direction).
Parent: M-futon1b-port (COMPLETE — all P1–P4, E1–E4 MET).
Driver: TBD (operator assigns). Reviewer: claude-16. Operator gate: Joe.**

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

## Open design questions (for the excursion's first turn)

- **Q1 — export mechanism.** HTTP API (paginate via existing endpoints —
  does futon1a expose a "list all docs" or "dump" endpoint?) or open a
  second read-only RocksDB node against the same data directory? The
  latter is faster but needs lock-semantics verification (can two
  processes read the same RocksDB concurrently?). The futon1a health
  check showed `RocksKv` — RocksDB allows multiple readers by default,
  but XTDB's node lifecycle may take a write lock. **This is the first
  thing to check.**
- **Q2 — transform completeness.** The slice exposed `:evidence/body`
  (string keys) and `:hx/props` (nested maps, H4). The full store will
  have more. The transform layer should be designed to fail-loud on
  unknown shapes (reject and log, not silently drop) — the opposite of
  the K2 hazard class.
- **Q3 — verification granularity.** Full checksum (every doc, both
  stores) vs. sampled parity (N queries from the P3 harness, both
  stores). At 247M keys the checksum is expensive but complete; the
  sampled parity is cheaper but less rigorous. Probably: checksum for
  ingest correctness, sampled parity for translation fidelity.

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
