# Mission: M-text-sidecar — free-text recall for futon1b + evidence for XTDB #5637

**Date:** 2026-07-10
**Status:** OPEN — IDENTIFY drafted, awaiting Joe's read
**Owner:** claude (Fable session, 2026-07-10). Driver: Joe.
**Home:** futon2/holes/ (alongside the migration excursion this grows out of)

Cross-refs:

- `E-futon1a-to-futon1b-migration-pipeline.md` — the parent excursion; 147,893 docs
  live in `futon1b/migration-store` as of 2026-07-10.
- https://github.com/xtdb/xtdb/issues/5637 — "Text indexing" (jarohen, 2026-05-23).
  A scoping issue: open items are decisions (feature level, Lucene footprint, data
  model), not code. The single comment is a set of exploratory "chalk" notes.
- https://github.com/xtdb/xtdb/issues/3663 — "Maintaining user-specified secondary
  indices". Still open: the substrate the #5637 design would sit on does not exist
  yet in XTDB 2.
- `futon1b/zai_memory_1b.clj` — the current recall surface the sidecar extends.

## HEAD — operator anchor

Joe (2026-07-10), pointing at #5637: "we're working in another session on porting my
XTDB 1 database to XTDB 2. Here's an open issue on the XTDB repo that we could
potentially tackle — not a small project but having a demo implementation would
surely be valuable for JUXT and I think we could make good use of it too."

Assessment agreed in the same conversation: don't attempt the in-core implementation
cold — the secondary-index substrate (#3663) is unbuilt, and a deep-core PR without
JUXT buy-in is likely unmergeable effort. Instead build the text index we need
anyway as an out-of-process sidecar, instrument it, and contribute the measurements
to #5637. Joe then asked for this mission doc.

## Why a mission and not a task

The goal is clear but the specification is not. Undecided: which index engine, how
the index stays in sync with the store, what the query surface should be, how to
measure the things JUXT's design notes treat as assumptions, and what form the
contribution back to #5637 takes. Those are genuine DERIVE questions — we will only
know what to build after the probes report.

## IDENTIFY

### The gap

1. **futon1b has no free-text recall.** The Zai memory seam (`memory-search` in
   `zai_memory_1b.clj`) filters on structured fields only: type, author, tags,
   since. There is nothing to search the *content* of 147,893 migrated docs.
   Verified 2026-07-10: futon1a never used the XTDB 1 Lucene module (no lucene dep,
   no text-search code), so this is net-new capability that the migration makes
   timely, not lost functionality being restored.

2. **XTDB #5637 is starved of real-world evidence.** The chalk notes propose an
   "ever held" inverted index (partition by hash of token, dedup on (token, iid),
   bitemporal resolver re-checks candidates) and name their own stress case:
   rewrite-heavy, wiki-style documents, where distinct-tokens-ever-held diverges
   from the current token set and the candidate pre-filter floods the resolver with
   false positives. They estimate ~1.5–2× divergence "for typical edit patterns"
   with no data. Our corpus (PlanetMath-adjacent, essays, revision-heavy evidence
   docs) is plausibly exactly that stress shape, and we hold the full bitemporal
   history in futon1a to measure it.

### Deliverables

- **D1 — the sidecar.** An out-of-process text index over the futon1b corpus,
  exposing free-text candidate lookup that composes with the existing seam: the
  sidecar returns candidate ids, the caller re-checks against XTDB 2 (the same
  pre-filter + re-check semantics the chalk notes propose, demonstrated at the
  application layer instead of inside the LSM). Wired into `memory-search` as an
  optional `:text` filter.
- **D2 — the evidence packet.** Measured numbers from the real corpus for the
  quantities the chalk notes care about: ever-held vs current token-set divergence
  per entity, posting-list skew before/after stop-word filtering, candidate
  false-positive rates under the re-check. Written up as a comment on #5637 with
  our requirements profile (what a 147k-doc bitemporal research corpus actually
  needs from text indexing).

### POC scope boundary

In scope: exact-match token lookup and boolean AND/OR composition, one analysis
chain choice per field class, batch index build from the migration export plus a
defined refresh path. Out of scope, deferred to a follow-on mission if JUXT engages:
prefix search, phrase queries, relevance ranking, and any in-core XTDB contribution.
The follow-on gets authored when the boundary is hit, not left implicit.

### Acceptance bar

- D1: a named set of text queries returns correct results against the live
  migration store, verified by exhaustive scan on a sampled subset (the oracle is
  "scan + re-check", the sidecar must agree with it); index rebuild is
  deterministic; the seam envelope contract is preserved and existing
  `memory-search` calls are untouched.
- D2: every number in the #5637 comment is reproducible from a script in the repo;
  the comment states corpus shape and method so JUXT can discount appropriately.
  Posting the comment is gated on Joe's read of the draft.

## Constraints (facts on the ground, verified 2026-07-10)

- futon1a pins XTDB 1.24.0 and futon1b needs 2.0.0 at the same Maven coordinates,
  so nothing here can load in the futon3c serving JVM — the sidecar is
  out-of-process by necessity, not preference. Same constraint already governs the
  seam wiring (see the migration excursion).
- The live futon1a store has the ~30s query-timeout behaviour under load; any
  history extraction for D2 follows the established `open-q` lazy-cursor pattern,
  in a quiet window if it needs the full population.
- There is no local xtdb checkout and this mission does not create one — we consume
  XTDB 2 as a dependency and read their design docs over HTTP.

## MAP / DERIVE — probes, car first

Per car-of-sequence: take P1, observe, then re-rank the rest. These probes are
DERIVE (design-discovery), not INSTANTIATE — their findings decide what D1 actually
is.

- **P1 (first up) — ever-held vs current divergence measurement.** For a sample of
  entities with revision history in futon1a, extract all historical versions of
  the text-bearing fields, tokenise (simple analyzer to start), and compute
  |tokens ever held| / |tokens currently held| per entity, plus the distribution
  over the corpus. This is the headline D2 number, it requires no engine decision,
  and it directly predicts D1's index size and false-positive economics. Also
  decide here *which fields are text-bearing* for our corpus — that field census is
  input to everything downstream.
- **P2 — engine choice.** Candidates: SQLite FTS5 (already proven in the stack via
  futon3a's meme.arrow store; trivially out-of-process; brings BM25 for free later),
  Lucene in a small dedicated JVM (matches the analysis-chain vocabulary of #5637
  exactly, heavier to operate), Tantivy (fast, adds a Rust toolchain). Decision
  criteria: operational weight in our stack, and fidelity of the evidence for
  JUXT (a Lucene analysis chain makes the analyzer findings directly transferable;
  FTS5 tokenizer findings transfer less directly). Logic-model note before any
  build: state the sync contract (what happens on new ingest, what staleness is
  acceptable) before choosing, since the engines differ most there.
- **P3 — sync mechanism.** The corpus is currently batch-migrated, so v0 can index
  from the export snapshot. But the seam will go live: decide poll-since (the
  evidence `:at` field supports it), re-export, or log-tail. Cheapest honest
  answer wins for the POC; the decision and its staleness bound get written down.
- **P4 — query-surface check with the migration session.** What text queries does
  the other session (and the Zai seam's actual consumers) need first? One short
  exchange before D1's interface freezes.

## ARGUE (strategic, plain language)

Why this shape and not the in-core demo: #5637 is at the decide-scope stage, so the
most valuable thing an outsider can hand JUXT is evidence that turns their stated
assumptions into measured facts — especially since our corpus matches their named
worst case. A sidecar that implements the same candidate-prefilter-plus-recheck
semantics at the application layer demonstrates the design's behaviour (analyzer
choices, false-positive profile, hot-term skew) without touching their LSM, and we
keep it as working infrastructure regardless of what JUXT decides. If they engage,
we arrive at the in-core conversation as a known quantity holding relevant data.
The risk we are declining: months in the Kotlin/Clojure core on a substrate (#3663)
that doesn't exist yet, with no signal anyone would merge it.

## VERIFY / INSTANTIATE

Deferred until DERIVE reports — per exploratory-slices-are-DERIVE, no build is
committed yet. When probes settle the design: ARGUE the committed design in a dated
section here, then INSTANTIATE with the usual gates (clj-kondo, check-parens on any
elisp/clj, tests, and the oracle-agreement check from the acceptance bar).
Substantial build legs follow the coding-handoff protocol (belled to Codex, reviewed
here); probes P1/P3/P4 are likely carve-out (b) work since the context lives in
this mission and the migration excursion.

## Log

- 2026-07-10 — mission authored (Fable session) from the #5637 assessment
  conversation with Joe. P1 is the car; nothing dispatched yet.

- 2026-07-10 (later, same session) — **P1a done offline** while the migration
  owned the quiet window. Everything below ran against the export snapshot in
  `futon1b/migration-export/` only; the live store was not touched. Scripts in
  `futon1b/textprobe_*.clj`, outputs in `futon1b/textprobe/`.

  Field census (`textprobe_census.clj` → `census-graph.edn`,
  `census-evidence.edn`): the text-bearing fields are, on the graph side,
  `:entity/source` (40,962 docs, 5.2M chars, max 25.7k) and
  `[:entity/props :anchor/passage]` (14,331 docs, ~0.9M chars) plus short
  `:entity/name` / `:anchor/heading`; on the evidence side,
  `[:evidence/body :text]` (12,350 docs, 18.5M chars, one doc at 608k chars).
  The two largest char-volume paths overall are Emacs HUD screen-captures
  (`... :buffer :visible :text`, ~33M chars of repetitive UI text) — whether to
  index or exclude those is a DERIVE decision (lean: exclude; they would
  dominate the index with UI chrome tokens).

  Update-frequency profile (`textprobe_updates.clj` → `updates.edn`): 6,713 of
  40,974 source-bearing entities (16.4%) have `:entity/seen-count` > 1 — a real
  update-prone population for the ever-held measurement. Caveat: seen-count
  counts observations, not necessarily text edits; whether re-seen entities
  actually change their source text is exactly what the history extraction
  answers. Top re-seen entities are `:pattern/library` docs with short sources.

  History-sample selection (`textprobe_sample.clj` →
  `history-sample-ids.edn`): deterministic (hash-mod, no RNG) — all 6,713
  updated source-bearing entities, 984 stable (seen=1) controls, 473 evidence
  docs (to confirm evidence is append-only). 8,170 ids total.

  Quiet-window artifact ready: `futon1a/scripts/textprobe-history-sample.clj`
  — a Drawbridge side-file (read-only, no shared-namespace reloads, async via
  `future` per the no-sync-heavy-calls rule) that walks `xtdb/entity-history`
  for the sample and writes text-field versions to
  `futon1b/textprobe/history-versions.edn`. It reads the node from
  `futon3c.dev/!f1-sys` and self-reports if internals aren't exposed. NOT yet
  run — needs a quiet window and Joe's go-ahead; the migration's hyperedge
  export was in flight when this was prepared.

  Side observation for the migration session: `evidence.edn` (re-exported
  08:45) contains 90,583 evidence docs of which 56,379 carry
  `:evidence/session-id` — i.e. ~34k sessionless docs ARE present in that
  export file, which may bear on finding F5.
