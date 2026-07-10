# E-futon1b-operational-switchover — the stack switches to futon1b *operationally first*

**Status: OPEN (plan of record written 2026-07-10, evening). Owner: lucy
(this box) as the proving ground. Parent: E-futon1a-to-futon1b-migration-pipeline
(data leg, proven). Boundary: BOUNDARY-futon1a-to-futon1b-2026-07-10.md.**

## The inversion (Joe, 2026-07-10)

The migration excursion proved the *data* can move. This excursion inverts
the order: get the whole stack to switch over **operationally** — running
against futon1b for everything it does day-to-day — and treat the data
backfill as a separable, already-proven step to run later. If the stack runs
operationally on futon1b, we know the data can follow; the reverse was never
guaranteed.

Lucy's futon1a store is backed up cold (sha256-pinned in the boundary doc),
so the switchover here is reversible by construction.

## What the research established (2026-07-10, two full-source sweeps)

### The stack's substrate dependence has TWO legs, not one

1. **HTTP leg** — ~15 futon3c namespaces hit futon1a's HTTP API (:7071):
   watchers (file_ingest, multi, commit_ingest, flight_ingest, replay,
   freshness), enrichment, arxana bridge, cascade-real, e1 metrics,
   mission-delta-t, clock-lineage, mission-control's substrate-2 inventory.
   Nearly all read `FUTON1A_URL` (default `http://localhost:7071`) into a
   **top-level def at namespace load** — env-switchable, but only pre-boot
   (hot-switch = per-ns re-def via Drawbridge).
2. **In-process leg** — the evidence store does NOT go over HTTP. Bootstrap
   embeds futon1a in the futon3c JVM and hands its raw XTDB node to
   `XtdbBackend` (`FUTON3C_DIRECT_XTDB` defaults true in all roles). **No
   env var can repoint evidence at futon1b**; and futon1b's XTDB 2 can never
   load in that JVM (same Maven coordinates as futon1a's XTDB 1 — classpath
   exclusion, permanent). Evidence must go out-of-process over HTTP to the
   futon1b server.

### The API surface to port is ~15 routes, fully specified

Full route-by-route contract (params, envelopes, status codes, gate
semantics, id-minting, gotchas) extracted from futon1a source and committed
as **`futon1b/API-CONTRACT.md`**. Highlights that shape the port:

- **EDN is the canonical wire format** (JSON only on Accept/Content-Type
  request headers). futon1b server is already EDN-first. The handful of
  read-only python metric scripts that expect JSON are stragglers, not
  drivers.
- The write pipeline is L4→L3→L2→L1→L0: model validation + canonical-id
  gate (400) → penholder allow-list (403) → entity/relation integrity (500)
  → identity uniqueness (409) → durable verified write (503). The
  canonical-id gate only fires for entity writes with a registered
  descriptor (`:mission/doc` seeded at boot). There is NO general sanitize
  layer to port — just per-route normalizations.
- Stable id minting: hyperedges `hx:<type>:<sorted-endpoints>` (already
  mirrored in futon1b_server), relations
  `rel|src|type|dst|note|order`, entities ensure-by-name.
- futon1a's L0 `verify-materialized!` (read-back after write) is the same
  discipline as futon1b_server's verified put — the two designs already
  agree; futon1b adds the F4 rescue ladder on top.
- Contract extensions futon1b adds deliberately: `GET /evidence/count`,
  evidence query params `before` + `include-ephemeral` (the futon3c
  EvidenceBackend protocol grew these 2026-07-10; futon1a silently ignores
  them).

### Boot coupling: the stack cannot currently run without futon1a

`start-futon1a!` is unconditional in bootstrap (no disable gate, unlike
futon5/IRC/drawbridge). If stubbed: evidence falls to an in-memory atom and
the `I-evidence-per-turn` boot check **fails loudly** (this is our built-in
acceptance test); substrate-2 reads degrade to fallbacks; WebArxana's graph
backend (URL derived from `FUTON1A_PORT`/`FUTON4_BASE_URL`) dies; the
shutdown hook and cyder's process registry reference futon1a.

### I-0 note (futon3c CLAUDE.md)

futon3c's I-0 invariant says "one serving JVM". The futon1b server is a
**second JVM by operator decision** (Joe, 2026-07-10, recorded in
E-futon1a-to-futon1b §"Architecture constraint") — forced by the XTDB 1/2
classpath exclusion, not convenience. When the switchover lands, I-0's text
must be amended: one *coordination* JVM (futon3c) + one *store* JVM
(futon1b); `pgrep java` returns two at rest.

### Lucy-specific

nginx owns :7073 (futon1b_server's default port) on lucy. Use **7074** here
(`--port 7074`; `FUTON1B_URL=http://localhost:7074`).

## Plan of record

### Phase A — grow futon1b_server to the contract (the port)

Work in futon1b repo; implement against `API-CONTRACT.md`. Existing assets:
EDN plumbing, hyperedge upsert with stable ids + no-op guard + verified put
+ rescue ladder; `classify-doc`/`doc-table` routing; `transform-doc`;
`memory-search`. To add:

- **A1 evidence**: POST (required-field 400s, duplicate-id 409, defaults,
  append-only) · GET by id · GET query (type, claim-type, subject-type/id,
  session-id, author, since, before*, tags, limit, include-ephemeral*) ·
  GET count* · GET sessions · GET {id}/chain. (* = deliberate extensions.)
- **A2 gates**: penholder L3 (body → x-penholder header → default; env
  `FUTON1B_ALLOWED_PENHOLDERS`, default `api,joe`) · canonical-id L4 for
  entity writes (port the `:mission/doc` id-contract seeded at boot) ·
  layered error envelope `{:error {:layer :reason :context}}` with the
  L4=400/L3=403/L2=500/L1=409/L0=503 status mapping.
- **A3 entities/relations**: POST /entity (ensure-by-name minting, full gate
  stack) · GET /entity/{id} (id → name → external-id fallback) · GET
  /entities/latest · POST /relation (stable rel| ids, both key spellings).
- **A4 graph reads**: GET /hyperedge/{id} (URL-decoded tail) · GET
  /hyperedges?type|end (+repo/source-file props filters; keep count
  semantics per contract §4).
- **A5 census + types**: census as bound-type count pushdown · GET /types ·
  POST /types/parent · /types/merge (body-only penholder) · type
  auto-registration on writes (every :hx/type / :entity/type /
  :relation/type seen → idempotent type doc).
- **A6 restructure**: split the single file into `futon1b/server/` nss as it
  grows (http plumbing / evidence / graph / gates), keep `-main` compatible.
- Non-goals for v1: snapshot/restore endpoints (migration tooling covers
  export), patterns/activation (single consumer, port when its consumer
  moves), valid-time on hyperedge puts (replays stay on futon1a until
  cutover), relations/batch (add when a caller needs it).

### Phase B — futon3c seams

- **B1** New `futon3c.evidence.futon1b-backend`: EvidenceBackend over
  HTTP/EDN against the Phase-A endpoints (-append, -get, -query, -count,
  -exists?, -forks-of). EDN end-to-end — no JSON keyword/instant loss. (The
  existing `http_backend` speaks JSON to the *futon3c agency* API — related
  pattern, different target; reuse its shape, not its encoding.)
- **B2** Bootstrap: `make-evidence-store` grows a
  `FUTON3C_EVIDENCE_BACKEND=futon1b` + `FUTON1B_URL` branch; the
  `I-evidence-per-turn` boot check must pass against it (it appends and
  reads back — this is the acceptance gate).
- **B3** `start-futon1a!` disable gate (env, e.g. `FUTON1A_DISABLE=true`),
  including: shutdown hook, cyder process entry, WebArxana URL handling.
- **B4** Flip the HTTP leg: `FUTON1A_URL=http://localhost:7074` pre-boot +
  fix the hardcoded stragglers — live-path first:
  `scripts/mission_scope_ingest.clj` (hardcoded default, invoked in-JVM by
  scope_reingest), then `enrichment/query.clj`, `logic/arxana_bridge.clj`,
  `dev/fm.clj` fm-conductor fallback, `scripts/mission_scope_view.clj`,
  `scripts/mission-scope-view-fast.sh`; python metric scripts
  (`o4_upward_clusters.py`, `o5_honest_holes.py`,
  `substrate_metric_e1_or_sample.py`) last (read-only). Note
  `portfolio/heartbeat.clj:100`'s `:7071` default is a pre-existing bug
  (targets futon5's nonstarter API on 7072) — fix independently.
- **B5** Dual-write completeness is NOT a goal — dual-write was the
  shadow-mode mechanism; under operational-first it's superseded by cutover.
  Leave the hook dormant.

### Phase C — boot-and-iterate rehearsal (lucy)

1. Start futon1b_server on :7074 against a **fresh store**
   (`--store-dir switchover-store`) — operational-first means empty is fine.
2. Boot futon3c with `FUTON1A_DISABLE=true`,
   `FUTON3C_EVIDENCE_BACKEND=futon1b`, `FUTON1B_URL=…:7074`,
   `FUTON1A_URL=…:7074`.
3. Gate 1: boot completes; `I-evidence-per-turn` check passes.
4. Gate 2: one watcher cycle runs clean (hyperedge + entity writes land,
   no-op guard suppresses re-posts, mission-id index read works).
5. Gate 3: mission-control renders (substrate-2 inventory + live turn
   counts) from futon1b.
6. Gate 4: an agent turn writes evidence and reads it back through the
   store protocol.
7. Iterate on every 404/500 until a full day runs clean. Each gap found =
   either a Phase-A route to finish or a straggler to repoint.

### Phase D — data backfill (later, separately)

The proven pipeline (E-futon1a-to-futon1b) replays lucy's backed-up store
into the switchover store. Notes: expect MORE F4 rescue-stage hits than the
fresh-store run (Arrow type unions already shaped by live writes) — keep
ingest-summary rescue logs; drop the 5.09M heartbeat-spam watcher-event docs
at export (88.5% of the old store, bug fixed 2026-07-10); evidence
duplicate-id guard means backfill must use the duplicate-free snapshot-scope
export (F8).

## 2026-07-10 (late) — A1+A2 landed (futon1b `d171150`)

Evidence routes (POST/get/query/count/sessions/chain) + the gate layer
(layered error envelope, penholder L3 with FUTON1B_ALLOWED_PENHOLDERS,
canonical-id L4 with the :mission/doc contract seeded at boot) are live in
futon1b_server. 26/26 HTTP smoke assertions PASS (`test_a1a2.clj`);
clj-kondo 0/0. Two findings from the work:

- **XTDB 2.1.0 + dual-stack**: 2.1.0 submits txs over an internal pgwire
  loopback to "localhost"; the node binds 127.0.0.1 but lucy resolves
  localhost to ::1 → every xt/q/execute-tx dies with ConnectException.
  Fixed with `-Djava.net.preferIPv4Stack=true` in the :node alias — **any
  box running futon1b needs this** (the laptop presumably resolves
  localhost IPv4-first, masking it there).
- **`[*]` projection works on 2.1.0** (returned full docs, composes with
  `where`) — it bound nothing on 2.0.0. The evidence query layer uses it
  with exact-match pushdown.
- Hyperedge POST is now penholder-gated (contract-faithful). If the
  dormant watcher dual-write hook is ever revived, it sends no x-penholder
  — set FUTON1B_COMPAT_PENHOLDER=api or add the header. The
  operational-first writers send x-penholder via FUTON1A_PENHOLDER already.

Next: A3 (entities/relations — the gate machinery they need is in place),
A4 (graph reads), A5 (census/types), then B1 (the futon3c EDN backend).

## Rollback

At any gate failure: stop futon1b JVM, unset the env flips, boot as before
(futon1a embedded, untouched). The store backup
(`futon1a-backup-2026-07-10.tar.zst`) covers even the catastrophic case.

## Order of work

A1+A2 first (evidence + gates — unblocks B1/B2 and Gate 1), then A3/A4/A5
in parallel with B3/B4, then Phase C gates in order. Phase D whenever
convenient after Gate 4 holds.
