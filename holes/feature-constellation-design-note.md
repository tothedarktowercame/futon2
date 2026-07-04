# Design note — the Feature Constellation (k-collapsible feature map)

*Companion to `aif-wiring-upgrade-note.md`. Written 2026-07-04 (claude-10, at Joe's
direction). Status: DESIGN — no build yet. Purpose: a feature map meaningful enough
to support "that needs trimming" and "these two can coalesce" decisions — the calls
made by hand across this whole decommission session.*

## The core idea

Reuse the **Interest Network's k-collapse mechanism** (WebArxana) over a **feature
graph** instead of the essays×territories graph. Joe's Interest Network already:
progressively collapses/unfolds by a control `k`; at **k=1 it reduces to the "core
interests."** A **Feature Constellation** is the same projection with features as
nodes — at **k=1 it reduces to the "core (load-bearing) features."**

Implementation substrate to reuse (read-only, recomputable, no in-place mutation):
`futon4/dev/web/webarxana/src/webarxana/server/interest_network.clj` (the
`project`/`projection` fns + the completeness 3-vector) and its client
`interest_network.cljs` (+ the `:k` control in `client/api.cljs`).

## The mapping

| Interest Network | Feature Constellation |
|---|---|
| node = interest-territory | **node = feature/subsystem** (the ~16 futon3c peripherals, the WM, the cascade, sorry-registry, evidence-bus, mcb, …) |
| node **standing** = interest-event posterior (lived signal) | **standing = liveness × load-bearing** — consumer-count (require graph) × landscape activity × recency |
| edge = bipartite fit / friction | **edge = `depends-on` (require graph) + `overlaps-with` (functional redundancy)** |
| `k` collapses toward **core interests** | `k` collapses toward **core features** (the load-bearing backbone) |
| completeness 3-vector | completeness over the feature set (coverage: how much of the stack is mapped) |

## The key insight: the k-slider *is* the trim/coalesce instrument

Trim and coalesce are relational judgments (they live in the edges, not the nodes —
see the upgrade note). The k-collapse surfaces both **for free**:

- **Coalesce candidates = what MERGES as k decreases.** Two features with high
  `overlaps-with` fold into the *same* node under collapse. That merge *is* the
  "these two can coalesce" signal — e.g. the hole trio (sorries ≈ issue_holes ≈
  cascade-holes) would collapse to one node at low k; portfolio-inference and the WM
  (two AIF loops) would collapse together.
- **Trim candidates = what DROPS OUT as k decreases.** A feature with low standing
  (no live consumers, dormant, stale) never joins the core — it falls away first
  under collapse. That fall-out *is* the "that needs trimming" signal — issue_holes
  (0 consumers, last-touched March) drops immediately; mission-control does NOT
  (many consumers → high standing → stays in the core even at low k, which is exactly
  why we *didn't* trim it).

So the operator drags k down and *watches*: nodes that merge are coalesce
candidates; nodes that vanish are trim candidates; nodes that persist to k=1 are the
core you must not touch. The decision becomes a gesture, not a grep.

## Sources feeding in (Joe named the first two)

- **VSATARCS** (`futon4/dev/arxana-browser-vsatarcs.el`) — machine-generated feature
  **narration** → the node payload (purpose/description, one line per feature). Good
  material; the gap was that narration alone is per-node, not relational (hence not
  enough to decide trim/coalesce). Here it becomes the node label; the edges do the
  deciding.
- **cascade-live** (`futon3c/src/futon3c/logic/cascade_real_live.clj`, substrate-2
  hyperedges) — mission activity → feeds **standing** (which features are live in the
  current work) and **`overlaps-with`** (features that share substrate-2 nodes).
- **require graph** (tools.deps / clj-kondo analysis) → the `depends-on` backbone —
  the edge that distinguishes load-bearing from trimmable (the query that stopped us
  breaking the WM aif-stack view).
- **evidence landscape** (`/api/alpha/evidence`) + **git last-touched** + registry
  status → **standing/liveness** (running? emitting? recent? registered?).
- **`wiring-claims.edn`** — already a partial feature registry (claims/files/
  verification per capability); a seed node set with provenance.

## First cut (bounded, reuses the projection)

1. Node set = the ~16 futon3c peripherals + the AIF/WM features + cascade +
   sorry-registry + evidence-bus + mcb (features we already characterised this
   session — their liveness/consumers are known).
2. `depends-on` edges = auto-derive from the require graph.
3. `standing` = consumer-count × (landscape-active? ∨ recent-git) × registered?.
4. `overlaps-with` = hand-seed the known clusters (hole-trio, the two AIF loops),
   embed purpose-lines later for the rest.
5. Feed nodes through `interest_network.clj`'s projection with the k control; render
   as the constellation with a k-slider. Trimmable nodes flagged (fall out early);
   coalesce pairs highlighted (merge early); each with its evidence inline.

The point (same lesson as the availability thread): structure the feature
*relationships* as a projectable graph first; then the constellation — 2D, or Joe's
cool 3D — renders the trim/coalesce affordance from the k-collapse, instead of a
fly-through you admire but can't act on.

## Open questions for Joe

- Exact `k` semantics to reuse: is Interest-Network `k` a standing-rank cutoff, a
  hop-radius from the core, or a community-resolution? (Determines how "merge under
  collapse" is computed — confirm against `interest_network.clj`.)
- Node granularity: peripheral-level, or finer (per-tool / per-endpoint)?
- Where it lives: extend the WebArxana Interest Network surface (`arxana://view/…`),
  or a sibling surface reusing the same projection?
