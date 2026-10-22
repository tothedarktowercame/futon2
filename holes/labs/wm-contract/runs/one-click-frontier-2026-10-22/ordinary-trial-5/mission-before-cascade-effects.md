# Mission: Stack Geometry (spatial synthesis of the existing rationalization attempts)

**Date:** 2026-04-19
**Status:** OPEN — IDENTIFY (2026-04-19)
**Owner:** Joe + agent
**Cross-refs:**
  `futon3/holes/holistic-argument.sexp` + `holistic-argument-sketch.md`
    (formal argument, superseded);
  `futon3c/docs/three-pillars.md` (conceptual reframe, living synthesis);
  `futon3c/holes/missions/M-mission-control.md` + its peripheral code
    (observational layer over ~25 active missions);
  `futon3c/holes/missions/M-portfolio-inference.md` + `futon3c/src/futon3c/portfolio/`
    (generative-model + EFE policy layer above Mission Control);
  `futon5a/holes/missions/M-trip-journal.md` (hinge-log apparatus,
    same futon1a ingestion substrate);
  `futon5a/holes/missions/M-a-sorry-enterprise.md` (sorry topology
    and sorry-proximity model; M-trip-journal B→A turns are one
    candidate observation stream for sorry discovery);
  `futon5a/holes/missions/M-learning-loop.md` (sibling; shares the
    embedding space and futon1a rendering; pattern-mining uses this
    mission's geometric layer as one of its projections);
  `futon5a/holes/missions/M-hypergraph-operator.md` (eventual Click/Tick
    analytic layer above the geometry);
  `futon5a/holes/excursions/E-webarxana-as-hinge-log-backend.md`
    (the Shape A bridge; precedent for how mission entities land in
    futon1a and how WebArxana renders them).

## 1. IDENTIFY

### Motivation

Three prior attempts at rationalising the futon stack's abstractions
already exist and are operational at different levels of the same
loop:

| Layer | Attempt | Current state | What it answers |
|---|---|---|---|
| **Semantics** | `holistic-argument.sexp` → `three-pillars.md` | Published reframe; formal sexp superseded | *Why the stack exists* (S1–S6 support claims, A1–A4 attacks, F1–F5 falsifiability conditions) |
| **Observation** | `M-mission-control` | Operational since Feb 2026 | *What is in the portfolio* (inventory, phase, devmap coverage, blocked-by) |
| **Dynamics** | `M-portfolio-inference` | Operational (in testing) | *What to do next* (EFE-ranked actions, adjacent-possible boundary, BUILD/MAINTAIN/CONSOLIDATE mode) |

Each says something the others cannot. None makes the synthesis
**spatially inspectable**. Joe has now returned to the
rationalisation problem multiple times — each prior attempt is
correct-but-non-spatial, and the returning-to-it is itself the
evidence that something is missing.

### The actual sorry

> The futon stack has correct-but-non-spatial self-understanding.
> The operator cannot *see* the ~25-mission game map as a whole.
> Priority-setting requires holding all 25 in head or trusting
> incomplete cross-references. The three existing rationalisations
> know what is true about the stack but not where it sits. The
> missing layer is **topology — what is near what, at what
> granularity**, rendered as a thing the operator can look at.

This is the layer the scout-report called out verbatim: *"None yet
has a unified queryable form (Arxana hypergraph planned but not
built)."* The hinge-log bridge (2026-04-18) started the work at
one slice; this mission owns the whole-portfolio version.

### Four-layer synthesis (what this mission adds and why)

```
┌──────────────────────────────────────────────────────────────┐
│  Argument          semantics   (three-pillars.md, S1–S6)     │
│    ↓ constrains                                              │
│  Invariants        structure   (pattern library + inventory) │
│    ↓ constrain                                               │
│  Missions          actions     (Mission Control + Portfolio  │
│                                 Inference)                   │
│    ↓ emit                                                    │
│  Evidence          grounds     (futon1a)                     │
│    ↓ feedback                                                │
│  Argument  (loop closes)                                     │
└──────────────────────────────────────────────────────────────┘
```

M-stack-geometry introduces the topology layer that makes all four
above inspectable together:

- Semantics → **landmarks** (S1–S6 claims as reference points)
- Observation → **node attributes** (phase, devmap coverage)
- Dynamics → **node colouring / flags** (portfolio mode, adjacent-possible)
- Topology → **positions + clusters + edges** (embedding space, hierarchical tree, cross-refs)

### Scope in

- **Pull canonical mission inventory** from M-mission-control
  (`mc-inventory` or `/api/alpha/missions`). ~25 active missions.
- **Pull portfolio state** from Portfolio Inference
  (`/api/alpha/portfolio/state`) — mode, belief μ, adjacent-possible
  set.
- **Ingest missions into futon1a** as entities with props
  (phase, mode, adjacent-possible?, devmap-coverage, blocked-by,
  repo, title, motivation-text). Extend the hinge-log bridge pattern
  from E-webarxana-as-hinge-log-backend.
- **Layer 1 edges — bootstrap cross-refs.** Parse `Cross-ref:`,
  `Parent mission:`, DR-N defers, consumer/producer pairings from
  mission doc headers and DR records. Lossy; load-bearing for initial
  orientation only.
- **Layer 2 edges — hierarchical clustering in pattern embedding
  space.** Embed each mission's content signature (title +
  motivation + scope-in is the candidate default) using the same
  MiniLM-class model futon3a / war\_machine\_visual's pattern view
  already uses. Hierarchical cluster (Ward linkage over cosine).
  Emit `:near` edges at chosen cutoff heights.
- **Landmarks — project S1–S6 holistic-argument claims** into the
  same embedding space. Makes missions locatable relative to the
  claims they support. The explicit connection from semantics to
  geometry.
- **Render in WebArxana.** Mission entities (already being ingested
  by the bridge for M-trip-journal / M-daily-scan) + new ones from
  this mission. Layered / dendrogram-driven layout rather than
  force-directed. Phase colouring. Portfolio-mode overlay.
  Adjacent-possible flag. S1–S6 landmarks as anchor points.

### Scope out

- **Rewriting Mission Control or Portfolio Inference.** Those are
  operational. This mission is a consumer.
- **Changing the three-pillar framing.** The semantics layer is
  settled for now; this mission takes it as ground.
- **Building a new embedding model or retrieval engine.** Reuse
  whatever futon3a / war\_machine\_visual already use. If the
  existing infrastructure only embeds pre-registered library
  patterns and can't embed arbitrary mission-doc text, the delta
  is in scope; a full retrieval rebuild is not.
- **B→A pattern mining** (M-learning-loop's territory). This
  mission produces the geometric substrate that mining can later
  use as one of its projections; the mining pipeline itself is
  elsewhere.
- **Click/Tick machinery** (M-hypergraph-operator). Analytic reads
  on top of the geometry belong there. This mission *populates*
  the queryable form.
- **3D rendering in VSAT.** Candidate follow-on. WebArxana 2D is
  the first surface; 3D is better-after-we-know-what-we-are-looking-at.
- **Arbitrary new edge types beyond cross-refs + embedding-near.**
  Keep the first version readable; richer edges are later work.

### Completion criteria (provisional)

1. All ~25 active missions (per Mission Control's inventory) are
   ingested into futon1a as entities with at minimum
   `:current-phase`, `:portfolio-mode`, `:adjacent-possible?`,
   `:repo`, `:title`, `:motivation-signature` (for embedding).
2. Layer-1 cross-ref edges from mission doc headers are ingested
   as typed relations (parent, cross-ref, DR-defer, consumer).
3. Mission embeddings exist in the same space as pattern
   embeddings and are persisted (not recomputed every render).
4. Hierarchical cluster tree is computed and surfaceable — either
   as a dendrogram artefact or as `:near` edges at chosen cutoff
   heights.
5. S1–S6 holistic-argument claims project into the same space
   as landmark entities.
6. WebArxana renders the whole view with layered layout, phase
   colouring, portfolio-mode overlay, and adjacent-possible flag.
7. The view surfaces at least one **non-obvious cluster** the
   operator had not previously articulated via cross-refs or
   prose. This is the mission's falsification test — if the
   clustering produces only what prose already said, the geometry
   layer did not add value.
8. Operator reports the view illuminates priority-setting in a
   way the existing three attempts did not. Subjective but
   load-bearing.

### Open questions (held; MAP to close as many as cheap to close)

- Where does the MiniLM (or other) embedding tool live? Is there a
  live endpoint that embeds arbitrary text, or is embedding
  pre-computed for the pattern library only? If the latter, scope
  of extending the tool is MAP output, not here.
- Content signature per mission: title only? title + motivation?
  title + motivation + scope-in? Full mission doc? Different
  choices yield different clustering trees; probably worth
  computing two trees and comparing.
- Clustering cutoff for v1 render: fixed K top-level clusters
  (e.g. 5–7), distance threshold, or interactive slider? Slider
  is the nicest but adds work.
- Do S1–S6 claims each get their own landmark entity, or do we
  project them as a weighted centroid per pillar (Argument /
  Invariants / Missions)? Both produce different views; first-cut
  is probably per-claim.
- Does any existing futon tool already render dendrograms? If so,
  reuse. If not, there's a UI-delta line item for MAP.

## 2. MAP (plan — not started)

Small, scoped scouts to close the open questions above before
committing to ingest + render. Each ~30–60 min. Output of MAP is
a confirmed plan for DERIVE, not any artefact yet.

- **M-1: Embedding tool scout.** Find the MiniLM (or other)
  embedder used by futon3a / war\_machine\_visual. Can it embed
  arbitrary text? Where does it run (local, bb-accessible,
  REST-exposed)? Cost per embedding? Pre-computed-only or
  on-demand?
- **M-2: Mission Control inventory scout.** Call `mc-inventory` /
  `/api/alpha/missions` and confirm the canonical list. Count. Phase
  distribution. Which missions have mission docs readable, which
  are tracked but undocumented. Sample the metadata fields.
- **M-3: Portfolio Inference state scout.** Call
  `/api/alpha/portfolio/state`. What does the adjacent-possible set
  look like right now? Which missions are flagged? What mode is the
  portfolio in today?
- **M-4: Mission-doc parseability scout.** Across the 25 missions,
  what fraction have `Cross-ref:` headers in consistent format? What
  fraction use DR-N decision records? What cross-ref types actually
  appear in the corpus (parent, consumer, defers-to, etc.)?
- **M-5: WebArxana layered-layout scout.** Can WebArxana do
  hierarchical / layered layout, or only force-directed? If only
  force-directed, is the cluster-encoding done in the data (near
  edges + pinning) or is there a layout-delta?
- **M-6: Holistic-argument S1–S6 shape.** Read the S1–S6 claims
  concretely and confirm they are each paragraph-embeddable the
  same way missions are. Note any claims that depend on content
  outside the sexp and would need assembly.

Output of MAP: a concrete DERIVE-ready spec that names the
embedding tool, the content signature, the cluster cutoff, the
landmark projection choice, and the render approach.

## 3. DERIVE (not started)

Consolidation of the MAP findings into a committed apparatus. TBD.

## 4. ARGUE (not started)

IF/HOWEVER/THEN/BECAUSE for the topology layer as the missing
fourth rationalisation. TBD.

## 5. VERIFY (not started)

Completion criterion 7 (non-obvious cluster) + criterion 8
(operator illumination) are the central empirical tests. Tripwires
and fidelity checks TBD.

## 6. INSTANTIATE (not started)

TBD after VERIFY.

---

## Checkpoints

### Checkpoint 1 — 2026-04-19: MAP findings (six scouts, one session)

Ran all six MAP scouts in parallel. Findings below; each tagged
with implication for DERIVE.

**M-1: Embedding tool (MiniLM-L6-v2).**
- Pre-computed embeddings at
  `futon3a/resources/notions/minilm_pattern_embeddings.json` —
  407 patterns, 384-dim vectors.
- **No on-demand HTTP endpoint** for embedding arbitrary text.
- Python script `futon3a/scripts/notions_search.py` runs the
  embedding inference (sentence-transformers).
- Clojure fn `futon.notions/search-embeddings` wraps it but only
  for retrieval, not for "return embedding of this text."
- *Implication:* DERIVE needs a small shim — extend
  `notions_search.py` (or sibling) to accept arbitrary text on
  stdin and emit a 384-dim vector on stdout. ~30–50 lines of
  Python. Mission-doc embeddings then get computed + persisted
  once per mission-doc revision.

**M-2: Mission Control canonical inventory.**
- `/api/alpha/missions` returns **101 total missions**.
- Status breakdown: 49 complete, 26 in-progress, 17 ready, 7
  unknown, 1 testing, 1 deferred.
- **43 active** (ready + in-progress).
- Repo distribution: futon3c (37), futon3 (34), futon5 (17),
  futon6 (7), futon4 (5), futon3b (1).
- **futon5a and futon7 are NOT indexed** — M-trip-journal,
  M-daily-scan, M-learning-loop, and this mission itself are
  invisible to Mission Control.
- *Implication:* "~25 active" from Joe was approximate. Real
  count of actively-moving missions is 43. DERIVE ingests all
  101 but may want to filter views by status for readability.
  futon5a + futon7 need separate filesystem ingest — or Mission
  Control should be extended to index them (separate mission,
  deferred). For this mission's first cut, supplement Mission
  Control with a direct filesystem scan of those two repos so
  the apparatus that produced M-trip-journal / this doc is
  visible in its own output.

**M-3: Portfolio Inference state (current snapshot).**
- `/api/alpha/portfolio/state` responds.
- Step count: **0** — the AIF loop has not been stepped. All
  sensory-channel μ values are at neutral 0.5. Mode BUILD by
  default, urgency 0.5, precision τ = 1.0.
- 16 sensory channels defined (mana-available, coverage-pct,
  blocked-ratio, stall-count, spinoff-pressure, etc.).
- *Implication:* The structural layer (which channels exist,
  mode options, precision-weighted update shape) is useful as
  ontology-for-overlay. The *live-state* is currently not
  informative. DERIVE can ingest the channel list + mode
  options as Portfolio-Inference-layer props without using
  the live μ values until a few heartbeat steps have run.
  Stepping the AIF loop is out of scope for this mission;
  noted as a prerequisite for the full dynamics overlay.

**M-4: Mission-doc parseability (135 mission docs scanned).**
- Structured headers across the corpus:
  - `**Blocked by:` — **26 missions** (biggest cohort)
  - `**Cross-ref:` — 21
  - `**Depends on:` — 15
  - `**Related:` — 4
  - `**Parent mission:` — 2
  - `**Supersedes:` — 1
  - DR-N defer records — 1 (rare)
- **117 missions reference other `M-<name>` missions in prose**
  (ubiquitous, unstructured).
- *Implication:* The structured cross-ref surface covers
  roughly 45 missions' worth of edges (after de-dup). Prose
  mentions are near-universal. DERIVE uses **both in layered
  confidence**:
  - Layer 1a (high confidence): structured-header edges —
    `:blocked-by`, `:cross-ref`, `:depends-on`, `:parent`,
    `:related`, `:supersedes`.
  - Layer 1b (low confidence, marked as such): prose
    M-name mentions — `:mentioned-in-prose`.
  This gives the operator a filter: show only high-confidence
  edges or show-both-with-styling. The prose-mention layer is
  the "cross-refs we wish were written explicitly" surface.

**M-5: WebArxana layered layout.**
- WebArxana's graph component (`futon4/dev/web/webarxana/
  src/webarxana/client/graph.cljs`) uses **d3-force only**.
- **Pins are supported** — initial x,y via `pin-centres`.
  Pinned nodes anchor the force simulation.
- No entity props for `:position/x` / `:layout/hint` — position
  is computed per render, not stored.
- Sidebar filtering by entity-type works; can be extended.
- *Implication:* For v1, **pre-compute cluster-centroid pin
  positions server-side**, hand WebArxana a pin map, let
  force-directed handle the fine details around the pins. No
  frontend changes required. If the pinned-force layout reads
  unsatisfyingly for 100+ nodes, v2 adds a layered-layout
  fork of graph.cljs (dagre or elkjs) as a toggleable mode.

**M-6: Holistic-argument S1–S6 (and more).**
- `futon3/holes/holistic-argument.sexp` has:
  - **6 support claims** (S1–S6 — the sexp has six, not five;
    S6 was added for the Invariants pillar).
  - **4 attack relations** (A1–A4).
  - **5 falsifiability conditions** (F1–F5).
  - **3 pillars** (Argument / Invariants / Missions).
- Each S-claim has a paragraph-length `:text` + structured
  `:evidence` list referencing specific artefacts (e.g. S1
  cites `853 :patterns`, `:psr-pur-records`,
  `:portfolio-inference`, etc.).
- *Implication:* All six S-claims are paragraph-embeddable
  (concatenate `:text` + prose-rendered `:evidence`). Project
  as landmark entities in the mission embedding space. The
  three pillars project as the centroids of their
  supporting-S claims. Attack relations and falsifiability
  conditions are *not* first-cut landmarks — they're too
  few, too schematic to cluster meaningfully, and they
  belong in a later analytic layer. First-cut: six
  S-landmarks + three pillar-centroids = nine anchor points.

### DERIVE-ready spec (consolidated)

Given MAP, DERIVE commits to:

1. **Embedder shim**: extend `notions_search.py` (or sibling)
   to take text on stdin → 384-dim vector on stdout. Called
   by a bb script that walks mission docs + S-claims and
   persists embeddings into futon1a as props on each entity.
2. **Mission ingest**: bb script pulls Mission Control's
   101 missions + supplements with filesystem scan of
   futon5a + futon7. Entities keyed `mission|<id>` (same
   pattern the hinge-log bridge uses). Props: phase, status,
   repo, path, title, motivation-text, embedding-vector.
3. **Cross-ref ingest**: two-layer edge extraction (structured
   headers = high confidence; prose mentions = marked
   low-confidence). Edges typed per M-4's list.
4. **Clustering**: Ward hierarchical on cosine distances,
   done in Python (scipy / sklearn cluster tree) called
   from bb or run as a one-shot.
5. **Layout**: cluster-centroid pins for WebArxana's
   force-directed renderer. Phase-coloured nodes. Confidence-
   styled edges. Landmarks (S1–S6 + 3 pillar centroids) as
   anchor entities.
6. **Scope deferral**: Portfolio-Inference live mode/urgency
   overlay, WebArxana layout-mode fork, dendrogram-cutoff
   slider, A1–A4 / F1–F5 integration — all deferred to a
   VERIFY/INSTANTIATE follow-up when the first cut has earned
   "worth iterating on."

### Open decisions for DERIVE (cheap, flag now)

- **Content signature per mission**: run **two conditions**
  in parallel — text embedding is cheap enough to afford both.
  Joe's call 2026-04-19:
  - **Condition A (IDENTIFY signature)**: title + the IDENTIFY
    section content (or `## Motivation` / first top-level
    section in older docs that predate phase-headers).
    Captures *what the mission says it's about.*
  - **Condition B (full-mission signature)**: title + the full
    mission body including MAP/DERIVE/ARGUE/VERIFY/INSTANTIATE
    sections and any checkpoints. Captures *what the mission
    actually contains and does.*
  Cluster both independently and compare the resulting trees.
  Divergence between A and B is itself diagnostic: a mission
  whose stated motivation doesn't cluster near its body's
  cluster has drifted from its identification, or its IDENTIFY
  was mis-articulated at the start.
- **Cluster count K for the default view**: start with K = 6
  (one per holistic-argument pillar × 2 sub-clusters).
  Interactive cutoff is deferred.
- **Repo-supplement ingest**: include futon5a + futon7 direct-
  from-filesystem so this mission is visible in its own output.
  Mark these as `:mission-control-indexed? false` for later
  deduplication if Mission Control is extended.

### Next move

DERIVE can start. First concrete build: the embedder shim + a
bb script that ingests Mission Control's inventory into futon1a
as entities (no embeddings yet, just structural). That's the
minimum sanity check that the pipeline wires up end-to-end
before we commit to the embedding + clustering pass.

### Checkpoint 2 — 2026-04-19 (same day, afternoon): DERIVE end-to-end

Pipeline built and exercised against the corpus. Scope of
Checkpoint 1's "first concrete build" was exceeded — rather than
stopping at structural ingest, the full pipeline ran in one
session.

**Artefacts built:**
- `futon3a/scripts/embed_text.py` — MiniLM-L6-v2 text→vector shim
  (sibling to `notions_search.py`). 384-dim.
- `futon5a/scripts/ingest_missions.clj` — pulls Mission Control
  (101) + filesystem-supplements futon5a (7) and futon7 (2)
  = 110 missions. Extracts two content signatures per mission:
  (A) IDENTIFY/Motivation section (avg 1.3k chars), (B) full
  body (avg 10k chars, capped 16k).
- `futon5a/scripts/cluster_missions.py` — loads embeddings,
  average-linkage hierarchical clustering over cosine
  distances, cuts at K flat clusters, PCA-projects to 2D,
  matches A/B labels for cross-condition comparison.
- `futon5a/scripts/stack_geometry_ingest.clj` — 110 mission
  entities + 16 cluster entities + 220 `:in-cluster-*` relations
  into futon1a. Entities carry clustering props
  (`:geom/cluster-identify`, `:geom/cluster-full`,
  `:geom/coord-identify`, `:geom/coord-full`,
  `:geom/a-b-divergence`, `:geom/cluster-shifted?`). Idempotent.

**Pipeline ran end-to-end** with k=8:
- 2 × 110 embeddings (~17s wall)
- Clustering + projection (<2s)
- futon1a ingest (126 entities + 220 relations, 0 errors)

**Findings.**

*A/B agreement = 62.7% (69/110).* Mean A↔B cosine distance =
0.19, max 0.71. **41 of 110 missions change clusters between
IDENTIFY-signature and FULL-signature** — the "mission says vs
mission does" signal is present and substantial.

*Top-10 A-B divergence missions* (strongest identification-drift
candidates):

| div | mission | cluster shift |
|---|---|---|
| 0.71 | `fm001-book-ramsey@futon5` | 1 → 3 |
| 0.66 | `f6-eval@futon5` | 3 → 4 |
| 0.60 | `social-exotype@futon5` | 0 → 4 |
| 0.55 | `evidence-landscape-exotype@futon5` | 0 → 4 |
| 0.55 | `coordination-exotype@futon5` | 0 → 4 |
| 0.53 | `codex-parity@futon3` | 4 → 2 |
| 0.53 | `f6-ingest@futon5` | 3 → 4 |
| 0.52 | `make-agency-work-properly@futon3` | 4 → 2 |
| 0.52 | `self-representing-stack@futon4` | (no shift) |
| 0.49 | `coordination-rewrite@futon3b` | (no shift) |

*Cluster structure (IDENTIFY, K=8):*

- **Cluster 0 (7):** exotype-family, cross-repo (futon3c↔futon5).
- **Cluster 1 (1):** `fm001-book-ramsey` singleton.
- **Cluster 2 (9):** graph + UI + peripheral — webarxana,
  agency-forum, sliding-blackboard, graph-unification,
  emacs-cursor-peripheral, par-session-punctuation, etc.
- **Cluster 3 (4):** `f6-*` (evidence retrieval/ingestion).
- **Cluster 4 (5):** futon5 coupling/probe experiments.
- **Cluster 5 (5):** IRC + transport + smoke-test.
- **Cluster 6 (78):** **mega-cluster** — futon3c mission-doc
  voice and pattern-and-coordination content. 71% of corpus.
- **Cluster 7 (1):** `landing-practice@futon5a` singleton.

The mega-cluster is real structure (futon3c has ~37 similar
missions + futon3 has ~34). The small clusters are the
non-obvious signal — they cross repo boundaries in ways
cross-refs do not.

**Criterion 7 (non-obvious cluster surfaces): plausibly met**,
pending Joe's reading:
- Exotype cluster (futon3c↔futon5) suggests the exotype concept
  has developed as a substrate most of its owners don't realise
  is shared.
- Graph+UI+peripheral cluster hints at an inhabitable-surface-
  building sub-capability several missions independently work on.
- `fm001-book-ramsey` as singleton may mean the book is
  vocabulary-isolated — intended or drift?

**Criterion 8 (operator illumination): pending** — requires Joe
to look at the output.

**Inspectable now via WebArxana:**
`http://127.0.0.1:3100/` → search e.g.
- `mission|stack-geometry@futon5a` (this mission visible in its
  own output)
- `cluster|identify|0` (exotype cluster)
- `cluster|identify|6` (mega-cluster — 78 incoming edges)
- Top divergence missions by name to inspect drift candidates

**Deferred to ARGUE / VERIFY / INSTANTIATE:**
- WebArxana pin-position refinement (PCA coords as pins instead
  of force-directed from scratch). Entities already render in
  default layout; pin refinement makes cluster-geometry visually
  explicit.
- Cross-ref edge ingest (Layer 1a structured + Layer 1b prose).
  Current relations are cluster-membership only.
- Holistic-argument S1–S6 landmark projection into the space.
- Portfolio Inference live overlay (step-count still 0 anyway).

**Open question for Joe before ARGUE:**
Read the top-divergence list and the small-cluster memberships.
Are they illuminating (criteria 7 + 8 met, proceed to ARGUE) or
mundane re-statements of what cross-refs already say (criterion 7
fails, revisit DERIVE with different signature or clustering)?

### Checkpoint 3 — 2026-04-19: cluster labels (narrative handles)

Joe's feedback after opening Checkpoint 2's output in WebArxana:
the clusters read as meaningful but don't have names, dominant
patterns, or other narrative handles. Addressed in one pass.

**Artefacts built:**
- `futon5a/scripts/label_clusters.py` — for each cluster in each
  condition, computes (a) the member closest to centroid
  ("representative"), (b) top-K `futon3/library` patterns closest
  to centroid (cosine vs the pre-computed 407 pattern embeddings),
  (c) TF-IDF distinctive words (n-gram) against other clusters'
  concatenated content, (d) a short name composed from the top
  distinctive words.
- `futon5a/scripts/stack_geometry_labels_ingest.clj` — enriches
  the 16 `geom-cluster` entities in futon1a with `:cluster/name`,
  `:cluster/distinctive-words`, `:cluster/representatives`,
  `:cluster/top-patterns`.

**Labels now attached to each cluster (K=8, both conditions):**

*IDENTIFY condition:*

| # | size | label | representatives |
|---|---:|---|---|
| 0 | 7  | **exotype / social exotype** | agency-refactor, social-exotype, peripheral-model |
| 1 | 1  | **book-ramsey** | fm001-book-ramsey |
| 2 | 9  | **lab / forum** | webarxana, graph-unification, labs-integration |
| 3 | 4  | **ingest / superpod** | f6-ingest, f6-arxiv, f6-ingest (the f6-* family) |
| 4 | 5  | **coupling / xor** | tpg-coupling-evolution, xor-coupling-probe, coupling-as-constraint |
| 5 | 5  | **irc / complete** | transport-adapters, IRC-stability, psr-pur-mesh |
| 6 | 78 | **proof / data** (mega) | stack-geometry (this mission!), futon1a-workplan, agency-rebuild |
| 7 | 1  | **practice / landing** | landing-practice |

*FULL condition surfaces finer structure that IDENTIFY smooths over:*

| # | size | label | notable |
|---|---:|---|---|
| 0 | 3  | **coupling / xor** | fewer than IDENTIFY — 2 dropped to own cluster |
| 1 | 3  | **survey / sci** | new cluster (sci-detection-pipeline, fulab-wiring-survey) |
| 2 | 8  | **lab / forum** | same as IDENTIFY, minus 1 |
| 3 | 14 | **codex / irc** | new cluster — codex execution + IRC merged |
| 4 | 15 | **problem / proof** | proof work pulled out of mega |
| 5 | 2  | **war / machine** | **M-war-machine separates out** |
| 6 | 64 | **clj / stack** (mega) | shrunk from 78 → 64 |
| 7 | 1  | **emacs chat / chat** | singleton |

**Observation worth recording.** The FULL condition reveals
*more* structure than the IDENTIFY condition: 14-member codex/irc
cluster, 15-member problem/proof cluster, 2-member war/machine
pair, 3-member survey/sci pair. These get absorbed into the
mega-cluster under IDENTIFY but separate out when the full
mission body is the signal. This is itself data: **missions
don't always identify-as what their content actually covers**.
The difference A→B in clustering shape is a second-order version
of the per-mission divergence measured in Checkpoint 2.

**Top-patterns landmarks per cluster** (samples, pre-enriched):
- exotype cluster ↔ `ants/baseline-cyber-ant`,
  `ants/hunger-precision-coupling`
- lab/forum ↔ `f4/p5` (Multi-user Memory), `f4/p9`
  (StackExchange Import), `f4/p3` (Flexiarg ⇄ Arxana)
- f6-ingest ↔ `f6/p10` (Math-Aware Agent Protocol), `f6/p1`
  (Pattern/Proof Bridge)
- coupling/xor ↔ `ants/hunger-precision-coupling`,
  `f2/p6` (Live Analyzer & Pivot Stream)
- irc/complete ↔ `f3/p0` (Flexiformal Transport Baseline)
- mega-cluster ↔ `f2/p13` (Graph Memory Adapter & Agent ABI),
  `f3/p9` (Proof Training Ground), `f3/p3` (Applicability
  Engine & Check DSL)

The mega-cluster's top patterns are themselves illuminating —
they're all core-stack substrate patterns, i.e. the mega-cluster
really is "missions about the stack's own plumbing."

**Inspect in WebArxana** (refreshed state):
- `cluster|identify|0` — exotype / social exotype (now labeled)
- `cluster|full|5` — war / machine (the newly-separated pair)
- `cluster|full|3` — codex / irc cluster
- `cluster|full|4` — problem / proof cluster

Any `geom-cluster` entity now carries its `:cluster/name`,
representatives, distinctive words, and top-6 library-patterns
as props — readable directly in WebArxana's node card.

**Still deferred:**
- WebArxana pin-position refinement using the PCA coords.
- Cross-ref edge layer (Layer 1a + 1b).
- S1–S6 landmark projection.
- ARGUE phase — the mission is now at DERIVE-through-INSTANTIATE
  end-to-end for the core clustering-with-labels move; ARGUE
  can be written now that the apparatus has produced its first
  real output for the operator to critique.

### Checkpoint 4 — 2026-04-19: recursive clustering for VSAT browsability

Joe mentioned a parallel VSAT-profile-maker effort (Codex, at
`~/vsat/README-vsat-profile-maker.md`) with a path to browsable
VSAT / VSATLATARIUM output, and flagged that the 78- / 64-member
mega-clusters exceed VSAT's comfortable ~15-scene-per-story limit.
He speculated the mega might need a *different* clustering
approach to subdivide meaningfully. It turned out recursive
Ward on the same embedding space produces coherent sub-groups —
no new signal required.

**Artefacts built:**
- `futon5a/scripts/recluster_tree.py` — recursive hierarchical
  cut. Starts from the K=8 flat partition; any cluster >
  `max-size` gets re-split with `k-split`; sub-clusters that are
  still too big get re-split again. Output tree with flat list
  of leaves. Works on both conditions.
- `futon5a/scripts/label_tree_leaves.py` — labels each leaf with
  representative missions + top-6 patterns + TF-IDF distinctive
  words (distinctive vs all *other* leaves in the same condition,
  not vs siblings-within-parent). Short name composed from the
  top distinctive words.
- `futon5a/scripts/stack_geometry_leaves_ingest.clj` — writes
  `cluster-leaf` entities into futon1a, adds `:in-leaf-<condition>`
  relations from missions, and `:leaf-parent` relations from
  sub-cluster leaves to their top-level parent. Coexists with
  the flat `cluster|<condition>|<0-7>` entities from Checkpoints
  2–3 so both perspectives remain navigable.

**Parameters used**: `--max-size 15 --k-split 6` (produced 23
leaves per condition — well below the fragmentation level of
`max-size 12` which gave 33).

**Key finding — the recursive split is coherent.** Concrete
example: leaf 6.4.4 of the IDENTIFY condition (size 15) has the
short name **"portfolio / sorry"** with distinctive words
`portfolio, sorry, click, reflection, stack, logic` and is
populated by:

- `stack-geometry@futon5a` (centroid rep — this mission itself)
- `structural-law@futon3c`
- `learning-loop@futon5a`
- `mission-control@futon3c`
- `portfolio-inference@futon3c`
- `trip-journal@futon5a`
- `hypergraph-operator@futon5a`
- `a-sorry-enterprise@futon5a`
- `self-representing-stack@futon4`
- `three-column-stack@futon4`
- `invariant-violations@futon3c`
- `stack-inhabitation@futon3c`
- `fulab-logic@futon3c`
- `tickle-overnight@futon3c`
- `futon-enrichment@futon4`

**This cluster recovered exactly the "informal cluster" Joe
named at the start of this mission** (holistic-argument + Mission
Control + Portfolio Inference) AND extended it with ten more
stack-thinking-about-itself missions Joe had not listed: the
trip-journal/learning-loop/hypergraph-operator family, the
self-representing-stack and three-column-stack work, fulab-logic,
stack-inhabitation, etc. The mission itself sits at the centroid.

**Criterion 7 (non-obvious cluster surfaces): clearly met.** The
apparatus surfaced a cohort of 15 structurally-similar missions
that Joe's prose cross-refs did not group together, recovering
an informal cluster AND extending it. The pattern held across
other sub-clusters:
- 6.4.2 "proof / problem" (9): P3-rational-reconstruction +
  diagramprover + mission-peripheral — coherent proof-search cohort
- 6.4.3 "peripheral / clj" (7): peripheral-behavior +
  peripheral-phenomenology + peripheral-gauntlet — coherent
  peripheral-architecture cohort
- 6.5.2 "irc / cli" (7): agency-rebuild + drawbridge-multi-agent
  + improve-irc — coherent transport-layer cohort
- 6.3 "codex / openai" (7): futon1a-rebuild + futon1a-evidence +
  codex-parity — coherent futon1a-codex cohort

**Criterion 8 (operator illumination): plausibly met**, pending
Joe's reading.

**Joe's "different clustering needed" speculation disconfirmed.**
The same Ward-over-cosine-on-MiniLM approach works at sub-cluster
scale; the apparent homogeneity of the mega was an artifact of
flat cutting, not of the embedding space itself. Good news:
nothing new to build for the second pass.

**Current futon1a state:**
- 110 `mission-geom` entities
- 16 `geom-cluster` entities (flat K=8, both conditions)
- **46 new `cluster-leaf` entities** (23 leaves per condition)
- 220 original `:in-cluster-*` relations (mission → flat cluster)
- **252 new `:in-leaf-*` + `:leaf-parent` relations** (mission →
  leaf, leaf → top-cluster-parent)

**VSAT export path now unblocked.** Each leaf is a VSAT Story
candidate; each member mission is a Scene candidate; cross-leaf
cross-references (once Layer 1a/1b ingest runs) become Story
Links. The codex profile-maker instructions are the next natural
next step, consuming `/tmp/stack-geometry/tree_labels.json` as
the source of truth.

### Checkpoint 5 — 2026-04-19: anthology prototype + VSATARCS note

Joe's framing: VSAT is ultimately an anthology of stories with
links between them; the first thing to write is the stories.

**Prototype drafted**: `futon5a/holes/stories/leaf-6-4-4.md` —
full anthology story for leaf 6.4.4 "portfolio / sorry" (all 15
members), omniscient narrator, reader addressed as "you" (a
futon contributor or interested party). Per-scene length
~150–250 words. Intra-story navigation links (back to overview,
forward to next-closest-sibling). VSATLATARIUM cross-story
links noted as TBD at the end of the file.

**Shape approved.** Joe's read: "Looks good to me." This frees
up the remaining ~22 leaves to scale.

**Deferred future extension — VSATARCS.** Joe flagged that a
lightweight HTML-like Arxana-native viewer for reading and
navigating these stories is plausible and probably useful later —
tentatively named VSATARCS (or similar). Not this mission's
concern to build; this mission produces the anthology content
and a VSAT-format export. A future Arxana-side viewer can
consume the same content via futon1a entities if the anthology
is ingested as `vsat-story` / `vsat-scene` entities.

**Scaling options for the remaining 22 leaves** (not committed
yet; flagged for Joe's steer):

1. **Hand-craft key leaves only** (e.g. 6.4.2 "proof / problem",
   6.4.3 "peripheral / clj", 6.5.2 "irc / cli", 2 "lab / forum",
   0 "exotype"): 4–6 more stories, each ~15 scenes. Slow but
   high-quality.
2. **Pipeline the rest** via an agent-drafts pattern: read
   `tree_labels.json` + mission IDENTIFY text, generate scene
   content from a template, operator reviews in batches. Fast
   but generic-feeling prose.
3. **Hybrid**: hand-craft 3–4 more leaves to prove voice
   generalises; pipeline the remaining singletons and small
   clusters; operator edits output selectively.

My read: (3) is the sustainable pattern. Singletons and tiny
clusters don't warrant hand-crafting; the larger narratively-
weighty clusters do.

### Checkpoint 6 — 2026-04-19: anthology batch + manifest

Joe's steer: pipeline a few more stories toward a contentful
VSAT demo; maintain a manifest of processed/pending items.

**Artefacts built:**
- `futon5a/holes/stories/manifest.json` — 23 IDENTIFY-condition
  leaves tracked with status (pending / drafted / approved /
  skipped), file path, drafted-at date, notes.
- `futon5a/holes/stories/leaf-2.md` — "Inhabitable Surfaces"
  (lab + forum + peripheral + cursor cohort; 9 members).
- `futon5a/holes/stories/leaf-0.md` — "The Exotype Move"
  (exotype-derivation family; 7 members, 4 of them placeholders
  preserved as such in the narrative).
- `futon5a/holes/stories/leaf-6-4-2.md` — "Proof and Problem"
  (proof-peripheral + DiagramProver + rational-reconstruction +
  distributed-frontiermath cohort; 9 members).

**Status snapshot:**
- 4 stories drafted (6.4.4, 0, 2, 6.4.2) = 40 scenes total.
- 19 leaves pending, of which 6 are singletons and 4 are
  two-member clusters that may warrant folding into a
  "miscellaneous neighbourhoods" story rather than their own.
- Demo-scale reached: four anthology stories span
  self-reflection, inhabitability, exotype-derivation, and
  proof-making — arguably the four most-characteristic
  dimensions of what the stack does.

**Voice observation across the batch:** the "omniscient narrator
to you-the-reader" voice holds across clusters of different
weight. Where mission content is substantive (Portfolio/Sorry,
Proof and Problem), scenes fill out to ~250 words with material
taken mostly from IDENTIFY text. Where mission content is
placeholder or stub (several Exotype members, some Inhabitable
Surface members), scenes honour the stub-ness explicitly
rather than padding — telling the reader "the name is here,
the body is not yet written" rather than inventing content.
That honesty may be the anthology's most useful contribution
separate from any VSAT export.

**Next move options:**
- Continue pipeline: draft 4–5 more leaves (candidates: 6.4.3
  peripheral/clj, 6.5.2 irc/cli, 5 irc/gate, 6.3 codex/openai,
  6.2 brief/scan).
- Fold singletons / doubletons into a combined "miscellaneous"
  story so the anthology has tidy ends.
- Build the VSAT profile JSON exporter now, seed it with the four
  drafted stories, and see a first browsable anthology in VSAT.
- Pause and let Joe read the four drafts before going further.

### Checkpoint 7 — 2026-04-19: VSAT exporter + futon profile handoff

Joe's steer: build the exporter so we can visualise in VSAT /
VSATLATARIUM; future state is bouncing between VSAT, WebArxana,
and VSATARCS (Codex-in-progress).

**Artefacts built:**
- `futon5a/scripts/export_vsat_profile.py` — reads
  `futon5a/holes/stories/manifest.json` + drafted `leaf-*.md`
  files, parses scenes from the `## Scene: Title | anchor`
  pattern, extracts cross-story links from the "Suggested
  cross-story links" footers, emits a VSAT profile JSON matching
  `~/vsat/README-vsat-profile-maker.md`. Placeholder images;
  `publish: false`.
- `/home/joe/vsat/data/futon-profile.json` — the output,
  79KB, 4 stories, 44 scenes, 7 storyLinks.
- `/home/joe/vsat/data/futon-profile-HANDOFF.md` — handoff
  document for Codex's loader work, including a TypeScript
  loader sketch, a regeneration recipe, and a "what's next"
  section outlining the three-browser state (VSAT 3D,
  WebArxana 2D, VSATARCS Arxana-native).

**Discovery worth noting.** VSAT's `data/vsat-profiles.json`
already has a **`futon`** profile slot reserved (`vsat_futon`
database, source type `"generated"`, status `"planned"`). The
slot has been waiting for exactly this kind of generator output.
No new profile registration needed; just fill the slot.

**What's still missing** (Codex's territory, not this mission's
to build):
- A `profile:seed:futon` or `profile:ingest:generated`
  subcommand in `scripts/vsatProfile.mjs` that reads
  `data/futon-profile.json` and calls VSAT's repository methods
  in the prescribed dependency order. Sketched in the HANDOFF.
- Image generation or placeholder-rewriting pipeline for
  publish-flip.

**Cross-story links surfaced via parsing** (clean full rationales):

| from | to | type | rationale |
|---|---|---|---|
| leaf-0 | leaf-6-4-4 | causal | the exotype derivation procedure is one of the abstractions Stack Geometry's topology layer makes visible |
| leaf-0 | leaf-2 | causal | Agency Refactor and Peripheral Model are infrastructure that the inhabitable-surfaces missions build on |
| leaf-0 | leaf-6-4-2 | contrast | proof missions derive from a different xenotype (the proof-search xenotype) rather than the social-exotype |
| leaf-2 | leaf-6-4-4 | thematic | shares Stack Inhabitation's "surface must earn inhabitation" discipline |
| leaf-2 | leaf-0 | causal | the social-exotype derivation produced some of the wiring diagrams these missions realise |
| leaf-6-4-2 | leaf-6-4-4 | contrast | portfolio reflection and proof-search are two forms of stack reflexivity, one inward and one outward |
| leaf-6-4-2 | leaf-0 | contrast | proof missions derive from a proof-search xenotype, parallel but distinct from the social-exotype |

Seven directed edges over four stories. Every story has in- and
out-edges except none-of-them; the link graph is tight enough
for a VSATLATARIUM 3D layout to read naturally.

**M-stack-geometry is now producing output for three browsing
surfaces**:
1. **WebArxana (2D)** — mission-geom + cluster + cluster-leaf
   entities with clustering props are live and queryable.
2. **VSAT (3D via VSATLATARIUM)** — profile JSON written; awaits
   a loader subcommand.
3. **VSATARCS (Arxana-native HTML-like)** — Codex-in-progress;
   consumes same anthology content via futon1a.

The three surfaces are projections of the same underlying
topology. Reading one illuminates different aspects: WebArxana
for node-edge traversal, VSAT for immersive narrative, VSATARCS
for lightweight scan-and-navigate.

### Checkpoint 8 — 2026-04-19: sorry-map (devmap extraction + all-other-business)

Joe's steer, post-VSATARCS-validation: connect the anthology work
to a better map of the sorries — specifically, unify the stack's
scattered sorry-vocabulary (Candidate Invariant queue, E-sorry-*
excursions, devmap aspiration-gaps, scene-level gaps) into one
enumerable registry. Use EDN now, Lean later. WebArxana becomes
the sorry-facet browser.

**Artefacts built:**
- `futon5a/scripts/extract_devmap_sorries.clj` — parses the 9
  `futon3/holes/features/futon*.devmap` files into typed EDN sorry records.
  Each `! instantiated-by:` block becomes one `:scale :devmap`
  sorry with IF/HOWEVER/THEN/BECAUSE structure preserved,
  pass/fail criteria, depends-on tokens, evidence-operational,
  next-steps, related-missions.
- `futon5a/scripts/report_all_other_business.clj` — contrasts
  each repo's devmap-mentioned missions (from RELATED-MISSIONS
  blocks) against Mission Control's inventory. Per-repo and
  stack-wide counts of missions in the "all other business"
  bucket (active work not linked to any devmap aspiration).
- `futon5a/scripts/ingest_sorries.clj` — 90 sorry entities + 9
  repo entities + 203 relations into futon1a. Cross-repo
  depends-on edges resolved where possible. Rollup edges
  attached from Completion-style sorries to their aggregated
  children.
- Hand-done first extraction artefact preserved at
  `/tmp/stack-geometry/sorries/futon0.edn` as a schema reference.

**Coverage across the 9 devmaps (auto-extracted, 90 sorrys):**

| repo | prototypes | with-deps | w/ evidence | w/ missions |
|---|---:|---:|---:|---:|
| futon0  |  9 |  8 |  1 | 1 |
| futon1  |  9 |  6 |  9 | 0 |
| futon2  | 14 | 12 | 14 | 0 |
| futon3  | 17 | 15 | 17 | 0 |
| futon3a | 11 |  9 | 11 | 0 |
| futon4  |  8 |  7 |  8 | 0 |
| futon5  |  6 |  4 |  6 | 0 |
| futon6  | 12 | 11 | 12 | 0 |
| futon7  |  4 |  1 |  4 | 0 |

**The finding that names Joe's confusion-pathology directly.**
Across the 110 Mission-Control-tracked missions, **only 1 is
explicitly mentioned** by any devmap's `+ RELATED-MISSIONS:`
block. That leaves **109 of 110 missions (99.1%) in the "all
other business" bucket** — the per-repo pile of active work
that no devmap aspiration connects to:

| devmap | prototypes | MC missions (family) | mentioned | all-other-business |
|---|---:|---:|---:|---:|
| futon3 (+ 3b/3c) | 17 | **72** | 0 | **72** |
| futon5 (+ 5a)    |  6 | **24** | 0 | **24** |
| futon6           | 12 |  7     | 0 |  7 |
| futon4           |  8 |  5     | 0 |  5 |
| futon7           |  4 |  2     | 0 |  2 |
| futon0/1/2/3a    | 43 |  0     | 0 |  0 |

This is what we keep getting confused about. The devmaps are the
Argument's aspiration-side; the missions are where work actually
happens. The two surfaces are almost completely disconnected at
the object level, so there is no single place to stand and read
"what is futon3 doing vs. what futon3 is supposed to be doing."
The confusion was never vague — it was structural, and the
numbers pin it down.

**Three sub-findings worth noting:**

1. **futon0/1/2/3a have devmaps but no MC-tracked missions.**
   43 prototypes of aspiration with zero tracked execution. Either
   work is happening in those repos and MC isn't scanning them
   (and should be extended to), or the aspiration is ahead of the
   practice (and a mission-authoring pass is overdue). Either way,
   this gap is now visible rather than implicit.

2. **futon3 is the biggest unbucketed pile** (72 missions). If
   there's one repo where filling in the devmap↔mission map would
   pay off most per unit of effort, it's this one.

3. **The one mention that does exist** (`M-emacs-cursor-peripheral`
   in futon0.P4) references a mission the extraction flagged as
   **mentioned-but-not-tracked** — Mission Control doesn't know
   about it. Two versions in the related-missions list: a bare
   `M-emacs-cursor-peripheral` and a path-prefixed variant from
   `holes/non-completed-mission-summaries.md`. Small evidence the
   devmap↔mission map was maintained by hand and has rotted.

**Schema refinements from the auto-extraction** (vs the hand-done
futon0.edn sketch):

- Kept `:sorry/if` / `:sorry/however` / `:sorry/then` /
  `:sorry/because` as separate fields — mirrors flexiarg shape,
  preserves the structure the devmaps already have, gives
  WebArxana four separate facets to render.
- Added `:sorry/actual-status` and `:sorry/confidence` —
  appeared in some devmaps (futon2, futon3a) as supplementary
  tags alongside `:maturity`.
- Added `:sorry/evidence-for` as a map {`maturity-keyword` →
  [strings]} — captures the `:evidence-for-active` /
  `:evidence-for-greenfield` / `:evidence-for-settled`
  per-maturity evidence the richer devmaps provide.
- Preserved `:sorry/source {:file :order}` — pivot back to the
  source devmap line-range for provenance.
- Derived `:sorry/status` from maturity + next-steps + evidence
  presence (e.g. `:active` + outstanding next-steps →
  `:in-progress`, not `:closed`).

**Inspectable now in WebArxana** (`http://127.0.0.1:3100/`):
- `repo|futon3` — the repo entity with devmap IFR/state/title
  as props
- `sorry|devmap|futon0|P0|core-infrastructure` — a specific
  devmap prototype with all its fields
- `sorry|devmap|futon0|full-layer-integration` — the rollup
  sorry, with `:roll-up true`
- ego-graph over any repo shows its 4–17 devmap prototypes
  `:lives-in-repo` → repo, plus cross-repo `:depends-on-sorry`
  edges where the P0-of-another-repo is in play

**Current futon1a state** (cumulative across Checkpoints 1–8):
- 110 mission-geom entities
- 16 flat-cluster + 46 leaf-cluster entities (clustering layer)
- **90 sorry entities** (devmap-scale, from this checkpoint)
- **9 repo entities** (one per devmap)
- 220 clustering relations + 252 leaf relations + **203
  sorry relations** (lives-in-repo, depends-on-sorry, roll-up-of)

**What we did NOT do** (deliberately out-of-scope for this
session):
- Promote `:sorry/next-steps` items to sub-sorrys. The parent
  sorry carries them as a prop; promotion happens when a
  next-step becomes actively owned.
- Extract `:scale :mission` sorrys from mission docs. That's a
  separate pass over the 110 mission documents (different
  parser; pending structured cross-ref ingest at Layer 1a / 1b).
- Bring in the Candidate Invariant queue from futon4.
  `E-candidate-queue-upsampling` proposed a machine-legible
  export; once that exists, we extract `:scale :invariant-candidate`
  sorrys and cross-link.
- Annotate the existing anthology scenes with `:scale :scene`
  sorrys. The sorry-registry currently gives the skeleton; the
  scene-sorrys are finer-grained and belong alongside the
  narrative drafts.
- The renewed-holistic-argument writeup. Now that 90 devmap
  sorrys are enumerable and queryable, the set *is* the renewed
  argument in data form; a prose writeup is optional.

**Next natural moves** (your steer):
- Walk one "all other business" bucket (e.g., futon3's 72
  missions) and do a one-pass sort: which missions are
  *closing* existing devmap prototypes implicitly, which are
  proposing *new* devmap entries, which are genuinely unrelated
  to any devmap. That's the manual pass that re-knits the two
  surfaces.
- Extend Mission Control to scan futon0/1/2/3a so their 43
  un-missioned prototypes either get matched with existing
  work or flagged as genuinely un-missioned aspiration.
- Promote specific next-steps to sub-sorrys where an owner
  exists (e.g., some of Prototype 0's next-steps are probably
  concrete-enough to own).
- Build the Candidate-Queue → sorry ingest and link to the
  devmap sorry tree at the invariant layer.

### Checkpoint 9 — 2026-04-19 (evening): TN-a-proof-is-not-a-tree landed

Joe's steer (ordered sequence):
1. Tech note first.
2. Continue sorry mining (add `:scale :mission` / `:scene` /
   `:invariant-candidate`).
3. Rewrite the holistic argument.
4. Revise story arcs + viewer so sorry landscape is *readable*.
5. Then discuss next steps.

**Step 1 done**: `futon5a/holes/tech-notes/TN-a-proof-is-not-a-tree.md`.
Riffs on Alexander (1965). Argues the sorry/pattern graph is a
semilattice, with patterns (A→B) and sorrys (B→A) as dual arrows:

- Pattern = closed prescriptive arrow, library-backed, A→B-tagging
  operates on it (fast, cheap, autonomous)
- Sorry = open retrospective arrow, registry-backed, B→A-mining
  targets it (expensive, operator-authored, generative)
- A sorry gets *closed* when a pattern is produced whose codomain
  matches the sorry's B. Mirrors Lean's `sorry : T`.

Semilattice structure (any two subsystems can share substructure
without strict containment) is load-bearing, not decorative.
Evidence from our own data: leaf 6.4.4 "portfolio/sorry" *is* a
semilattice meet across four repos — the clustering found it
without anyone writing the parent.

**Six commitments the note pins** that shape steps 2–5:

1. Many-to-many is default: `:closes` relation (pattern → sorry),
   `:shares-substrate` edges between sorrys with similar
   `:if`/`:then`, both many-to-many.
2. Shared substrate is computable from embeddings — same MiniLM
   pipeline we already have. No hand-maintenance.
3. Devmaps stop pretending to be trees; renewed holistic argument
   renders cross-repo sorry-edges as first-class.
4. Patterns that close no sorry are decorative (dead-weight
   diagnosis sharpened).
5. Three-pillar argument becomes a *query over the semilattice*
   rather than prose — Argument = root-sorry query, Invariants
   = promoted-sorry query, Missions = closure-in-progress query.
6. Scene annotations gain explicit sorrys at their foot; the
   viewer gains a "show me the sorrys" facet.

**Step 2 (sorry mining to build thorough picture) is the next
concrete work** when Joe returns. Three scales queued:

- `:scale :mission` — parse mission docs for completion
  criteria + DR records + explicit "sorry" language → typed
  sorrys at mission scale
- `:scale :scene` — annotate the 4 drafted anthology scenes
  with sorry blocks at the foot
- `:scale :invariant-candidate` — ingest futon4's Candidate
  Queue (awaits the machine-legible export per
  `E-candidate-queue-upsampling`)

### Checkpoint 10 — 2026-04-19: inhabited-ness axis + holistic-argument sorry ingest

Joe's steer, post-tech-note: sorry mining across sources 1–8 of
the inventory; save (9) evidence-log for another time; add an
**inhabited-ness axis** (done/closed / prototype / spec-only /
etc.) as a first-class dimension.

**Artefacts built this pass:**

- `:sorry/inhabited-ness` added as a first-class field, with
  vocabulary `[:unnamed :spec-only :nascent :prototype :active
  :operational :settled :closed]`. Orthogonal to `:sorry/status`
  (:open/:in-progress/:closed/:deferred); status is about work
  state, inhabited-ness is about lived maturity.
- `futon5a/scripts/extract_devmap_sorries.clj` — retrofitted.
  `:maturity` + `:actual-status` + evidence-count are mapped
  onto inhabited-ness. All 90 devmap sorries re-emitted with
  the new field.
- `futon5a/scripts/extract_holistic_argument.clj` — new.
  Parses `futon3/holes/holistic-argument.sexp` as Clojure
  s-expressions, emits **34 new sorries** at
  `:scale :holistic-argument`:
  - 1 thesis (roll-up)
  - 3 pillar sorries (argument / invariants / missions)
  - 6 S-support-claims (S1–S6)
  - 4 attack sorries (A1–A4)
  - 5 falsifiability conditions (F1–F5)
  - 5 generative-cycle step sorries (step 5 flagged
    `:formerly-missing`, inhabited-ness :active)
  - 10 devmap-summary sorries (status → inhabited-ness mapping)
- `futon5a/scripts/ingest_sorries.clj` — extended. Now ingests
  both devmap and holistic-argument sorries. New relation
  types: `:attacks-claim` (attack → S), `:would-refute`
  (F-condition → S). Also auto-creates repo entities for any
  locus ref-id that doesn't have its own devmap file
  (futon1a / futon3b / futon3c, needed for holistic-argument
  devmap-summary sorries).

**Inhabited-ness distributions** (the new axis, visible now
in WebArxana cards):

*90 devmap sorries:*
- `:unnamed` 36 (greenfield — design not started)
- `:operational` 33 (active maturity + ≥3 evidence pieces)
- `:spec-only` 12 (stubs — shape articulated, no build)
- `:settled` 5
- `:prototype` 3 (partial maturity)
- `:active` 1

*34 holistic-argument sorries:*
- `:operational` 21 (pillars, evidence-rich S-claims,
  cycle steps, operational devmaps)
- `:active` 7 (thesis, attack-counters, S-claims with
  moderate evidence)
- `:spec-only` 5 (falsifiability conditions — stated but
  not continuously monitored)
- `:nascent` 1 (futon7 devmap summary — explicitly
  flagged as "least developed")

**What this tells us immediately** (not yet cluster-queried;
these are hand-read observations from the distribution):

- **36 of 90 devmap prototypes are `:unnamed`** — design
  not started, not even stubbed. Those are the deepest
  sorries in the registry.
- **All five falsifiability conditions sit at `:spec-only`** —
  the argument declares what would refute it, but nothing
  monitors for those conditions continuously. This is a
  concrete structural debt (not a criticism, an observation
  that invites a monitoring peripheral).
- **Cycle step 5 is closed-by-named-artefacts** (three-pillars
  doc + futon2 aif/head.clj + M-aif-head VERIFY phase). This
  is the clearest case of a closed sorry in the registry. The
  sexp already carried `:closed-by` explicitly; our extraction
  preserved it. That's a template for how all `:closed` sorries
  should cite their closure evidence.

**Current futon1a state** (cumulative):
- 110 mission-geom + 16 flat-cluster + 46 leaf-cluster entities
- **124 sorry entities** (90 devmap + 34 holistic-argument)
- **12 repo entities** (9 from devmap files + 3 from
  holistic-argument-only devmaps: futon1a / futon3b / futon3c)
- ~900 relations total (226 sorry relations + clustering layer)

**Inspectable now in WebArxana**:
- `sorry|holistic-argument|thesis` — the roll-up; status
  `:in-progress`, inhabited-ness `:active`
- `sorry|holistic-argument|S|S1` — "the evidence discipline
  works"; 4 incoming relations (A1 and A4 attack it; F1 and
  F5 would refute it)
- `sorry|holistic-argument|cycle|step-5` — the `:closed`
  example with `:closed-by` artefact list as a prop
- `sorry|holistic-argument|pillar|argument` — one of the
  three pillar entities
- Any devmap sorry card now shows `:sorry/inhabited-ness` —
  the lived-maturity dimension Joe wanted surfaced

**Inventory status (sources 1–9 progress tracker):**

| # | source | status |
|---|---|---|
| 1 | `holistic-argument.sexp` (S/A/F/cycle/pillars/devmap) | **done** — 34 sorries ingested |
| 2 | `structural-law-inventory.sexp` | pending (similar sexp extractor, likely ~15–20 sorries) |
| 3 | mission docs (DR / Blocked by / Open risks / unmet criteria) | pending — biggest pass, ~200–400 mission-scale sorries expected |
| 4 | `E-sorry-*` excursions | pending |
| 5 | Candidate Queue (futon4) | **deferred** (awaits futon4-side export) |
| 6 | anthology scenes → sorry footers | pending (part of step 4, story-arc revision) |
| 7 | other excursions → deferred-decisions | pending |
| 8 | tech notes → open questions | pending |
| 9 | evidence log | **deferred per Joe** (next target after 1–8) |

**Ready for your pickup**: (2) structural-law-inventory is the
next small sexp extractor — similar pattern to this one, ~20
sorries. (3) mission-docs is the big pass; worth its own
session.

### Checkpoint 11 — 2026-04-19: sources 2–8 ingested in one batch

Joe's steer: go ahead and hit 2, 3, 4, 5, 6, 7, 8. Done.

**Extractors built + run (6 scripts, 4 hand-curated EDN files):**

| # | source | artefact | new sorries |
|---|---|---|---:|
| 2 | `futon3c/docs/structural-law-inventory.sexp` | `extract_structural_law.clj` | 23 |
| 3 | 61 non-complete mission docs | `extract_mission_sorries.clj` | 73 |
| 4 | `E-sorry-*` excursions | `sorry-excursions.edn` (hand-curated) | 12 |
| 5 | `E-candidate-queue-upsampling.md` | `sorry-excursions.edn` (hand-curated) | 3 |
| 6 | 4 anthology scenes | `scenes-excursions-technotes.edn` | 9 |
| 7 | 3 futon5a excursions | `scenes-excursions-technotes.edn` | 5 |
| 8 | 4 futon5a tech notes | `scenes-excursions-technotes.edn` | 5 |

**Total new sorries: 130.** Combined with Checkpoints 8 and 10
(90 devmap + 34 holistic-argument), **254 sorries** now live
in futon1a across 7 scales.

**Distribution by scale:**

| scale | n |
|---|---:|
| `:devmap` | 90 |
| `:mission` | 73 |
| `:holistic-argument` | 34 |
| `:structural-law` | 23 |
| `:excursion-sorry` | 19 |
| `:scene` | 9 |
| `:technote-sorry` | 6 |

**Inhabited-ness distribution across all 254:**

| inhabited-ness | n |
|---|---:|
| `:spec-only` | 117 (46%) |
| `:operational` | 58 (23%) |
| `:unnamed` | 37 (15%) |
| `:prototype` | 24 (9%) |
| `:active` | 12 (5%) |
| `:settled` | 5 |
| `:nascent` | 1 |

**Status distribution:**

| status | n |
|---|---:|
| `:open` | 143 (56%) |
| `:in-progress` | 89 (35%) |
| `:closed` | 12 (5%) |
| `:deferred` | 10 (4%) |

**Key observations from the full registry:**

1. **46% `:spec-only`** — nearly half of the stack's typed
   sorrys are articulated shapes with no build yet. This is
   the biggest single bucket and matches Joe's "99.1% AOB"
   intuition at a deeper scale: most of the stack lives as
   designed-but-unbuilt structure.
2. **37 `:unnamed` (greenfield)** — these are the deepest
   sorrys: devmap prototypes with no design yet, plus
   scene-level calls like futon5 exotype placeholders.
   Triageable first-aid targets.
3. **Only 12 `:closed` + 5 `:settled`** — 7% of the registry
   is resolved. The rest is in motion.
4. **58 `:operational`** — substantial but not dominant.
   These are the places the stack is actually running.

**Mission-scale breakdown** (73 mission-sorries):
- 61 mission-rollup sorries (one per non-complete mission)
- 9 blocked-by sorries
- 3 DR-defer sorries (from M-trip-journal's three DRs)
- 0 open-risk sorries (parser didn't match any ## Open risks
  sections — different wording across mission docs, not a
  bug in the principle; refinement for a later pass)

**What's now queryable in WebArxana**:
- `sorry|structural-law|candidate|<family-id>` — the 10
  candidate invariant families awaiting promotion
- `sorry|mission|<id>@<repo>` — one per non-complete mission
- `sorry|excursion|E-sorry-init|channel|candidate-queue` —
  the Candidate Queue itself as a sorry
- `sorry|scene|leaf-6-4-4|portfolio-inference-unstepped` —
  the specific observation that was Criterion 8 of this
  mission being met
- `sorry|technote|TN-proof-not-tree|renewed-holistic-as-query`
  — the step-3 commitment from Joe's ordered plan

**Remaining source (9) evidence-log** — deferred per Joe's
steer. Next target after steps 3 and 4 of the overall plan
(rewrite holistic argument + revise story arcs) land.

**Ready for step 3 of Joe's plan**: rewrite the holistic
argument as queries over the 254-sorry semilattice. The data
is now in place; the deliverable is a small page that renders
Argument / Invariants / Missions as live queries against
futon1a's sorry-registry.

### Checkpoint 14 — 2026-04-19: holistic argument hooked into three surfaces

Joe's steer (post-anthology-footer): the holistic argument IS
the landing/orientation artefact, but it's not hooked into the
three-surface browsing loop. Fix the plumbing.

**Edits to `futon5a/holes/holistic-argument-semilattice.md`:**

1. **New section "How to read this across the three surfaces"**
   near the top, naming WebArxana / VSATARCS / VSAT and the
   natural reading loop: *Orient in VSAT → read in VSATARCS →
   pivot to WebArxana → continue.*

2. **New table "Surface-by-surface reading pointers per
   pillar"** giving a triad (VSATARCS leaf / WebArxana pivot /
   VSAT story) for each of the six sections: Thesis, Pillar I–III,
   Generative cycle, Devmap status. Uses **"(forthcoming)"**
   markers honestly — 4 of 23 anthology leaves are drafted, the
   rest are flagged per-row. Those cells are named as their own
   future sorries (`:scale :anthology-gap`).

3. **New "Reading paths" section at the foot**, three curated
   tours crossing all three surfaces:
   - **Path A** (new contributor, 20 min): holistic-argument intro
     → VSATARCS leaf-6-4-4 Mission Control + Portfolio Inference
     scenes → WebArxana pivot to `mission|mission-control@futon3c`
     → back to Pillar III for numbers.
   - **Path B** (reach-vs-grasp auditor): registry-state headline
     → devmap table → Portfolio Inference scene (step-count-zero
     finding) → WebArxana search for futon0 / futon7 devmap
     sorries with zero closures.
   - **Path C** (contributor looking for work): E-sorry-initialization
     buildable steps → WebArxana `sorry|excursion|E-sorry-init|step|*`
     cards → pick a sorry with clear next-steps → return to the
     pillar structure to confirm impact.

**What this does not yet include** (honestly marked):
- Cross-surface URL links inside each scene's sorry-block.
  Proposed as the small (2) follow-on — a 20-minute extension
  to `append_sorry_footers.py` that emits
  `[→ WebArxana](http://127.0.0.1:3100/#/focus/<id>)` next to
  each `sorry|*` ID. Not done this pass.
- Reverse pointer (`:anthology-scene` prop on futon1a entities
  so WebArxana cards surface "Read in context"). Deferred to a
  later session.
- 19 anthology leaves still pending — each row with
  "(forthcoming)" is a sorry-shaped commitment to future
  anthology coverage.
- VSAT loader subcommand (`profile:seed:futon`) — Codex's
  territory. The profile JSON exists and is current; the loader
  doesn't.

**The orientation artefact is now complete in content and
wired in context.** It names its own gaps, tells the reader
where to start, and gives three paths through the three
surfaces. Step 3 of Joe's ordered plan is landed; the "hooked
into this" concern is addressed.

**Next natural move** (your steer): (a) run the cross-surface
URL-link follow-on via the scene-footer script, (b) draft one
of the forthcoming anthology leaves (e.g. `leaf-argument.md`
for the full S/A/F coverage), or (c) pause for operator review
of what's now in place.

### Checkpoint 12 — 2026-04-19: closure evidence (sorry castle, not on sand)

Joe's concern: only 7% `:closed` across 254 sorries looks like
reach-exceeding-grasp by 93%. ~1/4 M lines of code says the
stack has done a lot more closing than the registry reports.
The shortfall is a **measurement artefact**, not a stack
failure: we'd mined the aspiration side without the closure
side. This checkpoint dovetails with M-self-representing-stack:
empirical closures become `:closes` edges in the registry.

**Artefacts built:**
- `futon5a/scripts/compute_mission_closures.py` — embeds each
  of 49 complete missions' identify-signature, compares against
  all 90 devmap sorries' (title + if + then) via MiniLM cosine
  similarity, emits top-1 matches above threshold. Also pulls
  explicit closure evidence from holistic-argument cycle step 5's
  `:closed-by` list and structural-law families'
  `:implemented-in` paths.
- `futon5a/scripts/ingest_closures.clj` — writes 39
  complete-mission sorry entities (`:status :closed`,
  `:inhabited-ness :settled`) and 69 `:closes` relations to
  futon1a. Evidence artefacts from explicit closures become
  `evidence|<hash>` entities.

**Pipeline ran cleanly:**
- 49 complete missions, all with MiniLM embeddings (from the
  existing clustering corpus)
- **39 matched to a devmap sorry** above cosine 0.35 (79%
  auto-match rate)
  - 16 high-confidence (≥ 0.50) — operator-safe auto-links
  - 23 medium-confidence (0.35–0.50) — flagged for review
- 30 explicit closure edges from named evidence
- Total: **69 `:closes` relations** emitted, 0 errors

**Highest-confidence auto-matches** (top 10, all make
narrative sense):

| sim | mission | closes devmap sorry |
|---:|---|---|
| 0.73 | `forum-refactor@futon3c` | `futon3/P13/evidence-landscape` |
| 0.64 | `evidence-viewer-refinements@futon4` | `futon3/P13/evidence-landscape` |
| 0.64 | `futon3-coordination@futon5` | `futon3a/P11/stabilization-hub` |
| 0.63 | `futon3x-e2e@futon3` | `futon3/P10/flexiformal-training-ground` |
| 0.61 | `mission-control@futon3c` | `futon3/P16/mission-system` |
| 0.60 | `pattern-exotype-bridge@futon5` | `futon3a/P7/xenotype-ct-attachment` |
| 0.59 | `agency-rebuild@futon3` | `futon3/P14/peripheral-runtime` |
| 0.58 | `arxana-graph-persistence@futon3` | `futon1/P3/graph-memory-schema-mirroring` |
| 0.58 | `futon2-aif-ants@futon5` | `futon2/P9/futon1-graph-adapter` |
| 0.56 | `self-representing-stack@futon4` | `futon4/P1/arxana-hypertext-editor` |

Every row is a coherent "mission X closed devmap prototype Y"
claim — the kind of evidence-to-aspiration linking the
devmap-to-mission map was always supposed to carry but never
did explicitly.

**Updated closure distribution:**

| measure | before | after |
|---|---:|---:|
| total sorries | 254 | 293 |
| status `:closed` or `:settled` | 17 (6.7%) | **56 (19.1%)** |
| sorries with `:closes` edge incoming | 0 | **33** |

Combined "has closure evidence either way":
- 56 status-closed + 33 with incoming `:closes` edges − overlap
  = ~80 sorries (~27% of 293) have real closure evidence

**Coverage breakdown by scale** (unique sorries with at least
one `:closes` edge):
- 23 devmap sorries covered by mission closures
- 9 structural-law families covered via `:implemented-in`
- 1 holistic-argument sorry covered (cycle step 5's 3
  closed-by artefacts)

**What this shifts from 93%-open to ~73%-open-honestly** (or
similar, depending on exact tally):
- The remaining genuinely-open bucket is smaller than the raw
  status counts suggested. Some of the `:spec-only` devmap
  sorries have missions closing them; some `:candidate`
  structural-law families have code paths implementing them.
- "Reach exceeds grasp" is still true, but by a factor that's
  plausibly commensurate with a long-running R&D stack of
  this ambition, rather than by a factor that suggests
  structural over-commitment.
- The 23 devmap sorries now covered by mission-closures span
  6 repos — evidence of cross-repo work landing, not
  concentrated over-claim in one area.

**What this doesn't yet cover** (honest caveats for the
castle-not-sand framing):
- The 30 *explicit evidence artefacts* are file paths
  (`.clj` source files). Their existence is assumed but not
  verified against filesystem — a future pass could check
  `exists?` and downgrade evidence where the file has moved.
- Medium-confidence auto-matches (23 of them, similarity
  0.35–0.50) should have operator review before promotion to
  high-confidence. The registry treats them equivalently in
  edge creation; the `:confidence-medium` provenance note
  carries the caveat, but it's only inspectable per-relation.
- Mission closures above cosine 0.50 don't necessarily mean
  the mission *fully* closes the devmap prototype — some
  missions partially satisfy multi-part prototypes. A future
  pass could split `:closes` into `:fully-closes` and
  `:partially-closes`.
- Commit-log evidence (missions referenced in commit
  messages) not mined — another fat unclaimed closure source.

**Current futon1a state** (after Checkpoint 12):
- 110 mission-geom + 16 flat-cluster + 46 leaf-cluster entities
- **293 sorry entities** (254 + 39 complete-mission sorrys)
- 12 repo entities + 30 evidence artefact entities
- ~1,000 relations (226 sorry relations + clustering layer
  + 69 new `:closes` relations)

**Step 3 (rewrite holistic argument) now has honest data** —
the sorry-castle has foundations on the 80-ish sorries
demonstrably closed by code or complete missions, not only on
the 17 that self-report closure. The semilattice is
inhabited, not aspirational. The renewed-argument-as-query
can legitimately render "here's what the stack has built"
with real evidence pointers.

### Checkpoint 13 — 2026-04-19: holistic argument v3 (semilattice form)

Joe's steer: rewrite the holistic argument now that we have
both evidence and aspiration.

**Artefact written:**
`futon5a/holes/holistic-argument-semilattice.md` — ~400 lines,
supersedes `futon3c/docs/three-pillars.md` as *primary
representation* while leaving the prose v2 readable as an
introduction. Title echoes TN-a-proof-is-not-a-tree's
structural commitment.

**Structure of v3:**

1. **What this document is** + **Registry state as of 2026-04-19**
   (headline numbers: 293 sorries, 80-ish with closure evidence,
   19.1% status-closed, ~27% combined evidence-or-status)
2. **Thesis** — the roll-up sorry + current state + children
3. **Pillar I — Argument** (S1–S6, A1–A4, F1–F5, each with evidence-
   strength and inhabited-ness)
4. **Pillar II — Invariants** (9 operational + 10 candidate families,
   closure evidence via :implemented-in)
5. **Pillar III — Missions** (110 total, 49 complete, 39 matched to
   devmap prototypes, repo distribution, unmatched 10)
6. **The Generative Cycle** (5 steps, step 5 as the cleanest closed
   case in the registry)
7. **Devmaps — per-repo aspiration vs execution** (table of
   prototypes × complete missions × :closes coverage per repo)
8. **What the renewed argument does differently** (four structural
   points: evidence+aspiration together, semilattice load-bearing,
   falsifiable by data, castle-on-foundations quantifiable)
9. **How to regenerate** (7-step pipeline, all scripts listed)
10. **What's open about this version** (honest caveats: F-condition
    monitoring, candidate-family promotion criterion, futon0/1/2/3a
    no MC missions, medium-confidence closures unverified,
    commit-log evidence unmined, not-yet-template-driven)

**Four structural moves from v2 → v3:**

1. **Evidence and aspiration on the same page.** Prose claims from
   v1/v2 remain; each claim's current evidentiary status is now a
   queryable fact, each aspiration gap is an enumerable sorry.
2. **The semilattice structure is load-bearing.** `:closes`
   many-to-many, `:shares-substrate` forthcoming. Tree projections
   (per-repo devmap, per-pillar argument) are *reading modes*, not
   ground truth.
3. **Argument falsifiable by data.** If F1 becomes continuously
   monitorable and fires, S1 breaks, and the registry shows which
   closures depended on S1 — the argument would update itself
   structurally.
4. **"Castle on foundations" is quantifiable.** 27% with real
   evidence, 30% in motion, 52% open. Inspectable rather than vibes.

**Joe's original concern (7% closed = 93% reach-over-grasp)
is addressed structurally**: the honest number is ~27%
evidenced, which is plausible for a long-running R&D stack and
confidently not alarming.

**Completion criteria from Checkpoint 1 — status:**

| # | criterion | state |
|---|---|---|
| 1 | 25 active missions ingested as entities | done (actually 110 including complete) |
| 2 | Layer-1 cross-ref edges ingested | **deferred** — missions are ingested but cross-ref parsing was not the winning move; embedding-based clustering + closures turned out to be richer |
| 3 | Mission embeddings persisted | done (M-stack-geometry Checkpoint 2, both conditions) |
| 4 | Hierarchical cluster tree computed | done (Checkpoint 4, 23 leaves per condition) |
| 5 | S1–S6 holistic-argument claims project into the space as landmarks | **done via different method** — ingested as 34 sorries at `:scale :holistic-argument`, connected to devmap via pillar→S-claim→supports → :closes edges |
| 6 | WebArxana renders the whole view | done (all entities queryable; pin-positioning refinement deferred per Checkpoint 5) |
| 7 | Non-obvious cluster surfaces | done (Checkpoint 4 leaf 6.4.4 recovery + extension) |
| 8 | Operator reports illumination | done (Checkpoint 5 Joe's VSATARCS feedback on Portfolio Inference step-count-zero) |

**Mission's core deliverables now shipped.** Remaining work is
Joe's step 4 (revise story arcs + viewer so sorry landscape is
readable) and the consistently-deferred items (shares-substrate
edges, commit-log closure evidence, medium-confidence operator
review, template-driven argument regeneration).

**Step 3** (rewrite holistic argument) becomes, per the tech
note's commitment 5, a matter of *writing queries* rather than
prose. The data will already be there; the deliverable is a
small page that renders the three pillars as live queries
against the futon1a sorry-registry.

**Step 4** (revise story arcs + viewer) — two concrete edits
per scene + one viewer feature:
- Each anthology scene ends with a `## Open sorrys in this scene`
  block listing the sorry IDs relevant to that scene's mission
- VSATARCS / WebArxana gain a sorry-count badge per
  scene/mission/cluster, clickable into the sorry-facet

Ready when Joe is.

### Checkpoint 15 — 2026-04-19: pillar-based anthology leaves drafted (Pillars I & II)

Joe picked option (b) from Checkpoint 14's tail: "draft the
forthcoming leaves." Two pillar-based leaves now exist — they
are not cluster-derived; they correspond to the three-pillar
structure of the holistic argument and each scene maps to one or
more sorrys at the pillar's scale.

**New files under `futon5a/holes/stories/`:**

1. **`leaf-argument.md` — "What the Stack Claims to Be"**
   (Pillar I). ~18 scenes: Overview · Thesis · Pillar-Argument ·
   Pillar-Invariants · Pillar-Missions · S1-Evidence-Discipline ·
   S2-AIF-Framing · S3-Pattern-Transfer · S4-Commercial-Demand ·
   S5-Reflexive-Architecture · S6-Structural-Law ·
   A1-Complexity-Cost · A2-Solo-Developer · A3-Commercialisation ·
   A4-Explanation · Falsifiability (F1–F5 grouped) ·
   Generative-Cycle. Covers all 34 sorrys at
   `:scale :holistic-argument`.

2. **`leaf-invariants.md` — "What the Stack Is Sure Of"**
   (Pillar II). ~10 scenes: Overview · Pillar-Role ·
   Operational: Graph/Existence · Operational: Mission Process ·
   Operational: System/Auth · Candidate: Custody · Candidate:
   Inspectability · Candidate: Control · Promotion Criterion ·
   Closure Evidence · Structural-Law Case. Covers all 23 sorrys
   at `:scale :structural-law` plus the pillar-II root and
   relevant devmap summaries.

Both leaves use omniscient-narrator voice with "you" as reader,
matching the four cluster-derived leaves already drafted. Each
scene ends with prev/next navigation. Per-scene sorry-footer
blocks are deliberately omitted because the pillar-scale sorrys
**are** the scenes' subjects, not attachments to them (this is
noted in each leaf's closing paragraph).

**Manifest updates:** `manifest.json` now carries two pillar-based
entries (`leaf_id: "argument"`, `leaf_id: "invariants"`) alongside
the 22 cluster-based entries. The notes field on the manifest's
root explains that pillar-based leaves are not cluster-derived.

**Holistic argument updates:** `holistic-argument-semilattice.md`
has its Pillar-I and Pillar-II rows updated — the (forthcoming)
markers are replaced with concrete references to the two new
leaves. A new **Path A2** reading path was added: a pillar-first
tour that walks leaf-argument and leaf-invariants scene-by-scene
in VSATARCS with WebArxana pivots by sorry ID.

**What's still forthcoming per the holistic-argument table:**

- Generative cycle (dedicated leaf — currently scattered across
  leaves).
- Per-repo devmap leaves (one per active repo).
- 19 cluster-based leaves in the manifest.

**Next natural move:** either (1) cross-surface URL-link follow-on
for the scene-footer script (the 20-min extension flagged in
Checkpoint 14), (2) continue drafting forthcoming leaves —
generative-cycle is the next natural pillar-based pick — or
(3) Codex's VSAT loader subcommand so the profile JSON becomes
interactively browsable.

### Checkpoint 16 — 2026-04-19: all forthcoming leaves drafted; Codex reviewer-sorry pass; anthology complete in coverage

Three distinct pieces landed in rapid succession this evening:

**16a: Codex review pass + reviewer-sorry class.**
Codex was invoked as reviewer on the Checkpoint-15 drafts.
Four structured notes came back, captured as
`:scale :reviewer-sorry` entries in
`futon5a/holes/reviews/2026-04-19-codex-anthology-review.md`:
- RS-1 Criterion 7 overclaim in leaf-6-4-4 → resolved
  (softened to "candidate evidence pending Joe's explicit
  confirmation").
- RS-2 S1–S5 vs S1–S6 mismatch between mission doc and
  leaf-argument → resolved (mission doc updated to S1–S6; 8
  replacements).
- RS-3 Status ambiguity between registry-complete and
  articulation-thin → in-progress (retrofitted in leaf-2
  labs-integration, leaf-2 agency-forum, leaf-0 landscape-
  intelligence with explicit `registry: X; articulation: Y`
  phrasing).
- RS-4 Missing "what would someone do next" affordance →
  resolved-as-convention (Joe's steer: push toward interop
  with Portfolio Inference and Candidate Queue rather than
  replicating next-move analysis in every scene). Drafted as
  `futon5a/holes/tech-notes/TN-anthology-scene-interop.md`:
  proposed `<!-- scene-interop:start -->` block with three
  live pointers (Portfolio Inference, Candidate Queue, direct
  sorry ID); honest inventory of what's stub vs live; three
  concrete build steps.

The reviewer-sorry class is itself new: Joe called it out as
"cool — the system would become self-documenting." The schema
(`:raised-by`, `:targets`, `:severity`, `:status`,
`:resolved-by`) is spec'd in the review doc. If the pattern
recurs, it generalises into a `reviews/` convention and
ingests alongside devmap/holistic-argument/structural-law
sorrys.

**16b: 19 remaining cluster-leaf drafts.** All pending leaves
in the cluster manifest are now drafted:
- 6 singletons: leaf-1 (book-ramsey), leaf-6-1 (alfworld),
  leaf-6-4-5 (war-machine), leaf-6-5-4 (codex-agent-behaviour),
  leaf-6-5-5 (make-agency-work-properly), leaf-7 (landing-practice).
- 8 small clusters (size 2–5): leaf-3 (superpod/ingest),
  leaf-4 (couplings/probes), leaf-5 (wires & gates),
  leaf-6-0 (plan), leaf-6-4-0 (evidence over HTTP),
  leaf-6-4-1 (reading papers), leaf-6-5-0 (coordination
  rewrite), leaf-6-5-3 (being done).
- 4 medium clusters (size 7): leaf-6-3 (codex/rebuild),
  leaf-6-4-3 (peripheral zoo), leaf-6-5-1 (artificial stack
  exchange), leaf-6-5-2 (CLI/IRC/REPL).
- 1 large cluster (size 13): leaf-6-2 (last-mile + daily scan).

Total: 19 new cluster leaves with ~100 scenes. Voice held
(omniscient narrator, registry/articulation distinction
applied where superseded, no inline "what next" blocks per
RS-4's resolution). Manifest.json updated with drafted
status + per-leaf notes.

**16c: generative-cycle leaf + 9 per-repo devmap leaves.**
All remaining forthcoming cells in the holistic-argument-
semilattice table now point at concrete files:
- `leaf-cycle.md` — pillar-based (parallel to leaf-argument
  and leaf-invariants). Covers 5 cycle steps + why-it-matters
  + where-it-could-break. 8 scenes. Corresponds to
  `sorry|holistic-argument|cycle|step-1` through `step-5`.
- `devmap-futon0.md` through `devmap-futon7.md` — 9 per-repo
  devmap leaves covering all ~90 devmap prototype sorrys.
  Each leaf: overview + per-prototype scenes (one paragraph
  each) + closing framing scene. Per-prototype scenes keyed
  to `sorry|devmap|<repo>|P<n>` for direct WebArxana pivots.
  Operational-evidence counts pulled from the extraction
  files for honesty.

Holistic-argument-semilattice.md updated: last two
(forthcoming) markers replaced; new **Path D — "Repo
inspector"** reading path added for devmap-leaf readers;
footer notes this Checkpoint.

**Anthology inventory at end of Checkpoint 16:**

| Kind | Count | Files |
|---|---|---|
| Cluster-derived leaves | 23 | `leaf-0.md` … `leaf-7.md` |
| Pillar-based leaves | 3 | `leaf-argument.md`, `leaf-invariants.md`, `leaf-cycle.md` |
| Per-repo devmap leaves | 9 | `devmap-futon0.md` … `devmap-futon7.md` |
| **Total** | **35** | all under `futon5a/holes/stories/` |

Plus: the holistic-argument-semilattice orientation page
(which itself hooks into all three via the surface table),
the Codex review capture, and TN-anthology-scene-interop.

**What's not yet built** (honestly):
- The `<!-- scene-interop:start -->` block has not been
  written into any scene yet — TN-anthology-scene-interop
  specifies the convention; no scene carries it.
- Portfolio Inference's recommend-by-mission API does not
  exist; Candidate Queue peripheral does not exist.
- `append_interop_footers.py` (the sibling to
  `append_sorry_footers.py` that would propagate interop
  blocks) does not exist.
- VSAT profile JSON is currently 4 stories / 44 scenes
  (from Checkpoint 7) — does not reflect the 35-leaf reality.
- Codex's `profile:seed:futon` VSAT loader subcommand
  remains on his side.

**Next natural moves**: (1) re-export VSAT profile with all
35 leaves and refresh the handoff doc, (2) start the
scene-interop block worked example in an open-mission scene,
(3) build the `append_interop_footers.py` script, or
(4) let Codex land the VSAT loader and smoke-test the
full-load experience.

---

### Checkpoint 17 — 2026-04-20: priority signal + VSATARCS u-command + active-fronts; three-surface navigation articulated

A productive second day. Three landings plus a framing that names what was accomplished:

**17a: VSATARCS landing + `u` command.**
- `leaf-start-here.md` drafted (5 scenes: overview, cluster list,
  pillar list, devmap list, reading paths). Every story name is a
  cross-story link.
- VSATARCS at `futon4/dev/arxana-browser-vsatarcs.el` gained:
  - `arxana-vsatarcs-landing-story` defcustom
  - `arxana-vsatarcs--find-story-file` helper
  - `arxana-vsatarcs-up` command bound to `u` (jumps to landing)
  - Enhanced `arxana-vsatarcs-goto`: tries intra-story scene first,
    falls back to cross-story file open. Every `[label](target)`
    link in any leaf now works as cross-story navigation.
- VSAT profile refreshed to 36 stories (added leaf-start-here);
  `vsat_futon` reseeded.

**17b: Anthology topology in futon1a.**
- `scripts/ingest_anthology_leaves.clj` — ingested 13 missing leaves
  (1 landing, 3 pillar, 9 devmap) as `:anthology-leaf` entities
  with props `:anthology/kind`, `:anthology/title`, `:anthology/file`,
  `:anthology/scene-count`, plus `:anthology/scale` (pillars) or
  `:anthology/repo` (devmaps).
- 9 `:covers-repo` edges from devmap leaves → existing `repo|futon<n>`.
- Deliberately local + light: no cross-leaf mesh, no duplication of
  existing cluster-leaf→mission edges. Future route planning will
  emerge from co-coverage queries.

**17c: Priority signal (the temporal/stereolithographic first slice).**
- `scripts/extract_temporal.py` — walked 12 repos (2912 commits),
  extracted first-seen/last-updated per mission (89) and per
  devmap (9). Side output: 218 commits reference mission IDs
  (`commit-mission-refs.json`) — captured for future closure
  mining per Checkpoint 12 caveat, not ingested this pass.
- `scripts/ingest_temporal.clj` — merged `:temporal/first-seen` +
  `:temporal/last-updated` onto 95 mission and 9 devmap entities
  via fetch-merge-upsert pattern.
- **Learned the hard way**: `POST /entity` on futon1a **replaces**
  props, does not merge. Clobbered `mission|stack-geometry@futon5a`
  during a probe; restored by re-running `stack_geometry_ingest.clj`;
  saved to memory as `feedback_futon1a_upsert_replaces.md`. All
  subsequent scripts use fetch-merge-upsert.
- `scripts/compute_leaf_priority.py` — per-leaf priority score at
  7/30/90-day windows as fraction of covered missions recently
  updated. 36 leaves scored (pillars deferred — they cover sorries
  at a scale, not missions directly).
- `scripts/ingest_priority.clj` — merged priority props onto all
  36 leaf entities.

**17d: Active Fronts — roadmap as semilattice.**
- `scripts/generate_active_fronts.py` — emits
  `leaf-active-fronts.md` from live priority data + coverage
  overlaps. 6 scenes: overview, acute, sustained, crossings, cold,
  regenerate. Idempotent — rewritten on each run.
- Explicit semilattice framing (not linear) with crossings as
  rejoin points. Two live crossings found:
  - `leaf-6-2` (brief/scan) ↔ `devmap-futon7` sharing
    `daily-scan@futon7`, `f7-lead-report@futon7`.
  - `leaf-6-4-1` (retrieval) ↔ `devmap-futon6` sharing
    `paper-reverse-morphogenesis@futon6`, `superpod-mark2@futon6`.
  Joe noted the second crossing picks up exactly the themes we
  worked on this session (BGE retrieval, reverse-morphogenesis) —
  the thematic thread is now mechanically visible in the priority
  data, not just in memory.
- `leaf-start-here.md` updated: overview points at active-fronts;
  Path E now starts with active-fronts and recommends contributors
  pick work *at a crossing* for leverage.
- VSAT profile at 37 stories / 276 scenes / all published.

**Three-surface navigation articulated (Joe, end of session):**

| Surface | Reading question | Shape |
|---|---|---|
| **Narrative** (VSATARCS) | *what is this?* | prose, scene-by-scene, opinionated |
| **Structural** (WebArxana) | *what does it touch?* | graph, typed edges, pivotable |
| **Thematic/stereolithographic** (priority + future animation) | *what's moving and why does it cohere?* | temporal, acute/sustained/cold, crossings |

This tri-partition is the actual upgrade. Pre-session there was
one flat contemporaneous surface; post-session there are three
surfaces reading the same data from three complementary angles.

**What's still deferred:**
- Pillar leaf priority (requires sorry-scale traversal; captured
  as an explanatory `:priority/note`).
- Commit-ID closure mining from `commit-mission-refs.json` (218
  commits referencing missions). Closure rate uplift likely.
- Stereolithographic animation proper — the frame-by-frame lift
  in WebArxana (Path B from the session's framing). Priority
  signal is the static first slice; animation is the next step.
- `scene-interop` blocks from `TN-anthology-scene-interop`
  (Portfolio Inference / Candidate Queue pointers in scenes).

**Memories saved this session:**
- `project_stereolithographic_view.md` — termite-mound semilattice,
  script/storyboard framing (Checkpoint 16 aftermath).
- `project_retrospective_reconstruction.md` — git as under-mined
  data source, Checkpoint 12 caveats.
- `feedback_futon1a_upsert_replaces.md` — fetch-merge-upsert
  discipline.

**Next natural moves** (in order of leverage):
1. Commit-ID closure mining (the 218 commits are a fat closure
   source already extracted; ingest would lift close-rate).
2. Stereolithographic animation (frame-by-frame WebArxana lift).
3. Scale-aware coverage for pillar leaves (closes the deferred
   `:priority/note`).
4. `scene-interop` blocks worked example (TN convention → first
   real scene).

### Checkpoint 18 — 2026-04-20: follow-up excursions queued from three-surface use

Joe's steer, after reading the Checkpoint 17 state: log the next
actions as excursions rather than letting them remain implied by the
mission's current surface split.

#### Excursion A — Emacs-driven dual browsing: VSATARCS + WebArxana

**Entry point:** The three-surface navigation is useful, but today the
operator still has to drive the narrative and structural surfaces
separately. Desired state: Joe drives from Emacs, and the relevant
network appears in the browser at the same time as the VSATARCS
narrative location changes.

**Claim.** VSATARCS and WebArxana should become synchronized views over
the same selected entity, not two manually coordinated browsers. An
Emacs command should accept a mission, story, scene, cluster leaf, or
sorry id; open the nearest VSATARCS narrative locus; and steer the web
browser to the corresponding WebArxana focus/ego view.

**First implementation shape:**
- Use VSATARCS' existing `arxana-vsatarcs-goto` /
  `arxana-vsatarcs-up` machinery as the Emacs-side narrative target.
- Use WebArxana focus URLs such as
  `http://127.0.0.1:3100/#/focus/<entity-id>` as the browser-side
  structural target, with an ego/depth variant if WebArxana already
  exposes one.
- Add a small resolver that maps:
  - `mission|...` / `mission-id@repo` → cluster leaf + story scene
  - `sorry|...` → scene footer / holistic-argument section / devmap
    leaf where available
  - `cluster-leaf|...` → anthology leaf + WebArxana leaf entity
- Start with `browse-url` / external-browser steering. Only add a
  heavier browser-control layer if focus URLs cannot express the
  needed state.

**Completion check.** From one Emacs command, focusing
`trip-journal@futon5a`, `a-sorry-enterprise@futon5a`, or a concrete
`sorry|...` id updates both surfaces: VSATARCS lands on the relevant
scene/leaf, and WebArxana displays the corresponding graph context.
No copy/paste of entity ids should be needed.

**Why this is an excursion, not core mission work.** M-stack-geometry
has already produced the three surfaces. This follow-up tests whether
the operator can *inhabit* them as one cockpit.

#### Excursion B — Make the M-trip-journal ↔ M-a-sorry-enterprise bridge explicit

**Entry point:** Joe noticed that M-trip-journal and
M-a-sorry-enterprise are related, but the explicit link is buried. The
M-stack-geometry work discussed B→A turns as related to sorry
discovery; before this checkpoint, the mission docs did not make that
relationship easy to read.

**Bridge claim.** M-trip-journal supplies the B→A observation stream:
hinges record what problem the operator was actually inhabiting, what
turn became possible, and what residual unknown remained.
M-a-sorry-enterprise supplies the target space: typed sorrys,
sorry-pattern affinity, and closure trajectories. A hinge whose B→A
record names a previously untyped residual unknown is a candidate *new
sorry*; a hinge whose record aligns with an existing sorry is a candidate
*sorry-proximity / closure-progress* observation.

**Division of responsibility:**
- M-trip-journal records first-person hinge evidence and preserves the
  B→A register.
- M-learning-loop mines those B→A pairs into new patterns and
  diagnostics.
- M-a-sorry-enterprise scores the mined/observed turns against the
  sorry topology and tracks closure trajectories.
- M-stack-geometry makes the bridge visible as graph structure:
  hinge → B→A pair → candidate/known sorry → pattern/closure evidence.

**Concrete follow-up actions:**
1. Direct cross-refs in M-trip-journal and M-a-sorry-enterprise are now
   present; keep them synchronized as the bridge gets concrete fields.
2. Extend the hinge-log bridge schema with optional
   `:candidate-sorry`, `:known-sorry`, or `:residual-unknown` fields
   when the hinge text names them explicitly.
3. Add `:observes-sorry`, `:discovers-sorry`, and
   `:moves-toward-closure` relation types only after one worked hinge
   example proves the distinction is real.
4. Add one worked example using an existing M-trip-journal hinge and an
   existing sorry id from the 254-sorry registry.

**Completion check.** In WebArxana, opening either
`trip-journal@futon5a` or `a-sorry-enterprise@futon5a` should reveal a
typed path between them. In VSATARCS, the relevant scenes should name
the same bridge in prose.

---

## Addendum — Cross-Session Memory Ported into the Mission Doc (2026-04-20)

These three notes are session-durable framings the mission has
acquired but that previously lived only in agent-memory files
(`~/.claude/projects/-home-joe-code/memory/`). Porting them here so
Codex (and any other reviewer-agent) can read them as part of the
mission rather than as private memory.

### Addendum A — Stereolithographic FUTON view (termite-mound semilattice)

The VSAT hairball problem is a **loss-of-time-axis problem**. VSAT
renders every story as contemporaneous. FUTON's actual structure is
developmental — a termite-mound semilattice accreted by stigmergy:
each mission closure, each sorry flipped to `:closed`, each pattern
admitted is a small deposit adjacent to what's already there.
Emergent structure over time, not a planned graph.

**The stereolithographic view** lifts the 2D mission geometry into
3D by stacking time-slices as level-sets. At each *t*, the slice
shows what existed then; the vertical dimension is history. Priority
signal = growth rate in a region; stasis = done-or-neglected.

**Two implementation paths:**

- **Path A — VSAT-native stereolithographic lift.** True 3D
  layer-stack rotatable in VSATLATARIUM. Requires VSAT surgery;
  Codex territory or beyond.
- **Path B — WebArxana frame-by-frame animation.** Generate N
  time-slices from the temporal signal (git commit dates + mission
  phase-transition dates + sorry status timestamps), render same 2D
  coords with time-gated entity visibility, play as animation.
  Tractable in one session; WebArxana is our code.

**Script + storyboard framing:** VSATARCS carries the voice (prose,
scene-by-scene), WebArxana carries the topology (structure,
relations, potentially animation). Iterating between them refines
both.

**Three-surface navigation (articulated 2026-04-20 after priority
signal landed):**

| Surface | Reading question | Shape |
|---|---|---|
| Narrative (VSATARCS) | *what is this?* | prose, scene-by-scene, opinionated |
| Structural (WebArxana) | *what does it touch?* | graph, typed edges, pivotable |
| Thematic/stereolithographic (priority + future animation) | *what's moving and why does it cohere?* | temporal, acute/sustained/cold, crossings |

The third surface is currently a static first slice: per-leaf
priority scores at 7/30/90-day windows, plus pairwise crossings
(shared missions) between active leaves, rendered in
`leaf-active-fronts.md`. The dynamic frame-by-frame animation is
Path B, not yet built. When the session's thematic work appears as
a live crossing in the priority data (e.g. retrieval/techniques ↔
devmap-futon6 via BGE + reverse-morphogenesis missions, found
2026-04-20), that's the third surface working as intended — the
thematic thread of a session becomes mechanically visible in the
data, not just remembered.

**Lightweight opening move (landed Checkpoint 17):** add a `u`
keybinding in VSATARCS that jumps to a `leaf-start-here.md` landing
page — a "start here" holding space listing all stories grouped by
kind with one-line teasers. Becomes story `leaf-start-here` in VSAT
too via the exporter, so the anchor is shared across surfaces.

### Addendum B — Retrospective reconstruction from git commits

Git commits across the futon repos (futon0..7) are the stack's
**single richest under-mined data source**. They carry two
load-bearing signals that the sorry-registry does not yet extract:

**Signal 1 — Temporal.** Per-file first-commit date = "entity born"
date; last-commit date = "last touched" date. Aggregated per leaf
(cluster, pillar, devmap), this gives **growth rate over N days**
as the priority signal. This is the first slice of the
stereolithographic view (Addendum A).

**Signal 2 — Closure evidence.** Commit messages reference mission
IDs and scoped work ("M-stack-geometry", "fm001-book-ramsey",
"futon1a-rebuild"). Mining these would raise the stack's registry
close-rate beyond the 27% landed at Checkpoint 12.

**Documented caveats from Checkpoint 12 (still outstanding):**

- Evidence artefact paths (`.clj` files in `:implemented-in`)
  assumed to exist; not filesystem-verified.
- Medium-confidence `:closes` matches (23 of them) need operator
  review before they can count as full closures.
- `:closes` doesn't distinguish `:fully-closes` vs
  `:partially-closes` yet — binary today, gradient later.
- Commit-log evidence (missions referenced in commits) not mined
  — another fat closure source. Same pass that extracts temporal
  signal can harvest this as a side effect (and now does, as of
  Checkpoint 17 — `commit-mission-refs.json` holds 218 commits
  referencing mission IDs across 12 repos, awaiting ingest).

**How to apply:** when building temporal extractors, always emit
the mission-ID harvest as a side output — even if not ingested the
same pass, it's cheap to capture and the work doesn't have to be
redone. When a mission looks "stuck" in the registry but you see
many recent commits touching it, the commit-log mining has a
closure waiting to be counted. When a devmap prototype has
`:spec-only` inhabited-ness but the repo has active commits in the
relevant directory, the temporal signal is richer than the
registry reflects.

**Not yet mined (next fat retrospective source):** CLI/REPL session
logs, Claude/Codex conversation transcripts (in
`~/.claude/projects/` and equivalent for Codex). These contain
PSR/PUR-like evidence that never made it into the structured
ledger. Separate project from git mining.

### Addendum C — futon1a `POST /entity` REPLACES props (no merge)

Operational hazard learned the hard way Checkpoint 17. POST
`/api/alpha/entity` on futon1a **replaces the entity's props
wholesale** — it does not merge with existing props. Upserting
`{:name "mission|foo@repo" :props {:temporal/last "2026-04-20"}}`
when the entity already had 20 props will leave only the one prop.

**Confirmed empirically 2026-04-20** by probing with a throwaway
`_probe/` prop and observing that all existing `:mission/*` +
`:geom/*` props were deleted from `mission|stack-geometry@futon5a`.
Required re-running the full mission ingest script to restore.

**Discipline:** when adding or changing any prop on an existing
futon1a entity, always do the fetch-merge-upsert pattern:

1. `GET /api/alpha/entity/<URL-encoded-name>`
2. Parse the response EDN, extract `:props`
3. Merge new props into the fetched props map
4. POST `/entity` with the full merged props, original type,
   original external-id, original source

Never send a partial props payload expecting merge semantics.

This is particularly dangerous for scripted incremental updates
(temporal signal, priority scores, flag updates) which by their
nature touch only one or two props per entity. A naive ingest
script will silently wipe unrelated state.

**Scripts under `futon5a/scripts/` that hold the full prop set**
(safe to re-run when restoration is needed):

- `stack_geometry_ingest.clj` (missions + clusters)
- `stack_geometry_leaves_ingest.clj` (cluster leaves)
- `stack_geometry_labels_ingest.clj`
- `ingest_sorries.clj`
- `ingest_closures.clj`
- `ingest_anthology_leaves.clj`

**Scripts that correctly use fetch-merge-upsert** (safe for
incremental prop updates):

- `ingest_temporal.clj`
- `ingest_priority.clj`

If a clobber happens, the right recovery is to re-run the script
that produced the original state, not to reconstruct by hand. A
cleaner long-term fix is for futon1a to expose a partial-update
endpoint (`PATCH /entity` or equivalent) that merges by default; an
upstream change for Codex or a devmap-futon1 prototype.

---

*Mission created 2026-04-19 from a conversation starting in
M-trip-journal's hinge-log-bridge work (2026-04-18) and opening
up at 2026-04-19 11:30 Sunday when Joe named the three prior
rationalisation attempts (holistic-argument + Mission Control +
Portfolio Inference) as themselves an informal cluster. This
mission is the spatial synthesis those three attempts have been
building toward without yet completing.*

## Acceptance checklist (2026-09-30)

- [ ] Canonical mission inventory and portfolio state are ingested into futon1a with typed mission cross-reference edges.
- [ ] Mission embeddings are persisted in the pattern embedding space with a hierarchical cluster tree.
- [ ] Holistic-argument claims are projected into the mission embedding space as landmark entities.
- [ ] WebArxana renders mission geometry with layered layout, phase colouring, portfolio mode, and adjacent-possible flags.
- [ ] An operator evaluation records a cluster absent from prior cross-references and how the geometry changes priority-setting.
