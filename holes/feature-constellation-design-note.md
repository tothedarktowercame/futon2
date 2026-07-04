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
| `k` = BFS-ring depth over pinned focus (`graph.cljs`): k=3 all rings; k=2 `restrict-to-pins`; k=1 `restrict-to-super-core` (above-median magnitude) + largest-component | same on features: k=2 keeps the pinned core; k=1 keeps above-median-**standing** features (the super-core) in the focus's component |
| completeness 3-vector | completeness over the feature set (coverage: how much of the stack is mapped) |

## The k-slider is the TRIM instrument; coalesce is an overlay

Reading the real algorithm (below) sharpens *and corrects* the earlier framing. The
retraction is a **magnitude threshold**, not a chain-merge — so it gives one axis
cleanly and the other by overlay:

- **Trim candidates fall out of the k=1 cut directly.** `restrict-to-super-core` keeps
  only stars ABOVE the median magnitude; **everything below the median drops.**
  Magnitude = standing (consumer-count × liveness × recency), so a dormant feature with
  no live consumers has low magnitude and is the *first thing to vanish* at k=1.
  issue_holes (0 consumers, last-touched March) → below median → drops; mission-control
  (many consumers) → above median → stays in the super-core, which is *exactly* why we
  didn't trim it. **Drag to k=1; whatever vanishes is a trim candidate.**
- **Coalesce is NOT what the k-collapse gives** (the earlier note was wrong here — the
  algorithm *drops* nodes, it never *merges* them). Coalesce is a **separate overlay**
  on the `overlaps-with` edges: two high-overlap features that survive *adjacent* in
  the super-core are the "these two can coalesce" candidates (the hole trio; the two
  AIF loops). The slider surfaces trim for free; coalesce needs the overlap edges drawn.
- **The core = the super-core** — above-median-magnitude survivors in the focus's
  connected component (mission-control, the WM, the evidence-bus). Untouchables.

So: drag k to 1 and *what vanishes is a trim candidate*; light up the overlap edges and
*adjacent survivors are coalesce candidates*. Trim is the retraction; coalesce is the
overlay — not one gesture, but two cheap reads instead of a session of grepping.

## The retraction algorithm, pinned — from the actual k-handler (2026-07-04)

Corrected after reading what the `—` control *runs* (`webarxana/client/graph.cljs`,
the interest-star graph) rather than reasoning about hierarchies (Joe's redirect:
read the handler). It is **neither** k-core **nor** the VSATARCS leaf/devmap/frame
tiers. The graph is a **BFS neighbourhood of pinned/focus nodes**; each node ("star")
carries a **magnitude** = its per-node coreness weight (also its radius). `k`
dispatches three operations:

| k | function | what it does |
|---|---|---|
| **3** show all | full BFS | all rings + subscopes (`scope-fold-depth` re-fetches the focus at full depth) |
| **2** fold to main | `restrict-to-pins` | keep only pinned-core nodes + links whose **both** endpoints are pins; drop the depth-0 "gray" neighbour ring and every edge touching it. **← Joe's "removing leaf nodes"** |
| **1** retraction | `restrict-to-super-core` → `keep-largest-component` | keep only stars whose magnitude is **strictly above the core's MEDIAN magnitude** (+ the always-keep focus) and the links between kept stars; then drop disconnected fragments, keeping the focus's connected component. Islands fall away. **← Joe's "retracts to a core"** |

Two design choices to carry over verbatim: the k=1 cut is **median-relative** — scale-
adaptive to each constellation's own weight spread, not a hardcoded threshold (the
code notes: *"tune the comparator: median → mean → top-N → fixed"*), and the final
**largest-connected-component** pass makes the super-core read as one cluster.

**Magnitude is the whole game.** It's the per-node coreness weight that the k=1 cut
thresholds on — i.e. exactly the **standing** this note has been calling "liveness ×
load-bearing." So the feature port is: compute a magnitude per feature (consumer-
count / landscape-activity / recency), pin the focus/anchor, and reuse
`restrict-to-pins` (k=2) + `restrict-to-super-core` (k=1) unchanged.

(The VSATARCS `frame ⊃ devmap ⊃ leaf` hierarchy — `devmap-futonX` = repo, `leaf-*` =
feature — is still the right *node taxonomy* and the repo/coarse level; it is just
NOT the collapse mechanism. Node taxonomy from VSATARCS/devmaps; collapse mechanism
from `graph.cljs`.)

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

- `k` semantics RESOLVED (2026-07-04) — the real code is `webarxana/client/graph.cljs`
  (`restrict-to-pins` at k=2, `restrict-to-super-core` + `keep-largest-component` at
  k=1; median-magnitude cut). See "The retraction algorithm, pinned" above. The port is
  small: supply a **magnitude per feature** (= standing) and reuse those functions; the
  render already exists (unlike my earlier guess, this is located code, not a
  convention). VSATARCS/devmaps supply the node taxonomy + repo level, not the collapse.
- Node granularity: peripheral-level, or finer (per-tool / per-endpoint)?
- Where it lives: extend the WebArxana Interest Network surface (`arxana://view/…`),
  or a sibling surface reusing the same projection?
