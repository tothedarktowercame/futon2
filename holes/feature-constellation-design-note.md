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
| `k` = chain expansion/retraction radius (KNN-rooted; k↓ peels leaf rings, k=1 → core interests) | same on features: k↓ peels leaf features ring-by-ring; k=1 = the core (load-bearing anchors) |
| completeness 3-vector | completeness over the feature set (coverage: how much of the stack is mapped) |

## The key insight: the k-slider *is* the trim/coalesce instrument

`k` is a **chain expansion/retraction radius** (rooted in the KNN cache —
`futon4/docs/prototype-1-links-plan.org`: top-k neighbours per node). In the diagram:
high k shows full chains; **k↓ peels leaf rings** (k=3→2 removes leaves); **k=1
retracts every chain to its core anchor**. Over a feature graph, that retraction
surfaces both decisions:

- **Trim candidates = leaf features that peel early AND carry low standing.** The
  first ring to fall (k=3→2) is the leaves; a leaf that is *also* dormant (no live
  consumers, stale) is the "that needs trimming" signal — issue_holes (leaf: nothing
  depends on it; dormant: last-touched March) peels immediately. A *live* leaf is just
  a peripheral feature — it peels for reach, not removal (standing keeps it legible).
- **Coalesce candidates = features that retract into the SAME core anchor.** As chains
  contract toward k=1, features on one chain fold into their anchor; two that land on
  the same anchor are the "these two can coalesce" signal — the hole trio
  (sorries ≈ issue_holes ≈ cascade-holes) contracting to one hole-node; portfolio-
  inference folding toward the WM (same AIF-loop chain).
- **The core = what survives to k=1** — the load-bearing anchors everything retracts
  to (mission-control, the WM, the evidence-bus). Untouchables. That mission-control
  sits here (many consumers → deep anchor) is exactly why we did NOT trim it.

So the operator drags k down and *watches the retraction*: a leaf that peels *and* is
dead = trim; nodes that fold onto the same anchor = coalesce; the k=1 core = hands
off. The decision becomes a gesture along the slider, not a grep.

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

- `k` semantics CONFIRMED (Joe, 2026-07-04): KNN-rooted **chain expansion/retraction**
  — k↓ peels leaf rings, k=1 retracts each chain to its core anchor. The exact
  diagram-retraction algorithm (how a chain contracts to its anchor, and what counts
  as a chain over `depends-on` vs `overlaps-with`) is a diagram convention to read
  from the arxana-browser rendering code at build time; the KNN cache is in
  `links-plan.org`.
- Node granularity: peripheral-level, or finer (per-tool / per-endpoint)?
- Where it lives: extend the WebArxana Interest Network surface (`arxana://view/…`),
  or a sibling surface reusing the same projection?
