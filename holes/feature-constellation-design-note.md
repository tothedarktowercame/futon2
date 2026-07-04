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

## The retraction algorithm, pinned (2026-07-04)

Traced through the code, `k`-retraction is **not** a k-core/coreness algorithm (there
is no such impl in the stack — searches for k-core/shell/leaf-prune hit only noise).
It is **depth-truncation over the VSATARCS lift hierarchy** `frame ⊃ devmap ⊃ leaf`
(`arxana-vsatarcs-lifting-queue.el`, kind order `'(:leaf :devmap :frame …)`):

| tier | VSATARCS kind | naming convention | feature-constellation meaning |
|---|---|---|---|
| fine | `leaf` | `leaf-6-4-5` (dotted-decimal lineage) | a **feature** (+ its internal chain) |
| mid  | `devmap` | `devmap-futon3` (**named by repo**) | a **repo** (the ≤20-feature cluster) |
| core | `frame` | `frame-*` | the **core concept** (k=1 anchor) |

Joe's semantics map exactly: **k=3→2 "removes leaf nodes"** = peel the `leaf` tier →
devmaps (repos); **k=2→1 "retracts to a core"** = peel the `devmap` tier → frames
(cores). The KNN `k` (`arxana-links` / `links-plan.org` embedding cache) is the *base
graph within a tier*; the dotted-decimal `6-4-5 → 6-4 → 6` is the within-leaf
sub-chain that truncates further at high k.

**So: k-bounded depth-truncation over `frame ⊃ devmap ⊃ leaf`** — formally iterative
leaf-pruning on that hierarchy (general-graph generalisation = k-shell/coreness, but
here the tiers are explicit, so it's plain tier-depth truncation). This is why the
Interest-Network reuse is "almost exactly the same thing": `devmap-futonX` already
*is* the repo node, leaves already *are* features, frames already *are* cores.

**Caveat (honest):** the *structure* (three tiers + KNN base + dotted-decimal
sub-chains) and *semantics* (peel-to-core) are pinned; a single render `defun` that
takes `k` and emits the retracted diagram was **not** found — the tier-collapse is a
convention over the hierarchy. A build writes that renderer (or extends the VSATARCS
reader chrome), reading nodes from the devmaps.

## Node source, the repo level, and the ≤20 budget (Joe, 2026-07-04)

The node set isn't invented — it's already written in the **feature devmaps** (the
futon3-lineage `.devmap` files; 18 today, e.g. `futon5a/futon5a.devmap`,
`futon3/holes/strategy/globe*.devmap`; `mcb/read-all-devmaps` already parses them).
Each `!`-entry is a feature carrying exactly what the constellation needs:

- `:maturity` (`:active`/…) → **standing**
- `:depends-on [...]` → the **`depends-on` edges, already explicit** (no require-graph
  derivation needed at this altitude — the devmap states them)
- the IFR + IF/HOWEVER/THEN/BECAUSE argument → the **qualifiers**. This is where
  "futon1a isn't *just* storage — it's a bitemporal graph DB with invariants, and all
  of that turns out to matter" lives; a one-line label would drop it.
- `:evidence-for-settled` → the completion criterion.

So the devmaps *are* a partial feature constellation already, in text; the build reads
them rather than inventing nodes.

**Repos are the coarsest k-level.** Each futonX repo is a feature cluster — at very low
k the constellation retracts to repo anchors ("futon1a = storage", "futon2 = strategic
AIF", "futon3c = coordination"). Features accrete at a level over time (XTDB 2 → a
native query-layer feature joins the futon1 cluster).

**The ≤20-features/repo budget is a first-class health signal — and a trim trigger.**
Joe's estimate: no futonX repo should carry more than ~20 features. Over budget is not
a display problem, it's a governance signal — the completeness 3-vector flags the repo,
and the k-retraction shows *which* features to coalesce (fold onto an anchor) or trim
(dead leaves) to get back under. **The map that shows the overflow is the same map that
shows the fix** — the closed loop this whole session ran by hand (futon3c was at ~16
peripherals + AIF features; we just took it down by three).

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

- `k` semantics RESOLVED (2026-07-04): see "The retraction algorithm, pinned" above —
  depth-truncation over the `frame ⊃ devmap ⊃ leaf` lift hierarchy (leaf=feature,
  devmap=repo, frame=core), KNN as the within-tier base. Remaining sub-question for a
  build: the render `defun` applying `k` was not found (convention, not code) — write
  it, or extend the VSATARCS reader chrome.
- Node granularity: peripheral-level, or finer (per-tool / per-endpoint)?
- Where it lives: extend the WebArxana Interest Network surface (`arxana://view/…`),
  or a sibling surface reusing the same projection?
