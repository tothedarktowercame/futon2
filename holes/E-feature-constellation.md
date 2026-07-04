# E-feature-constellation — a k-collapsible live map of the futon stack's features

**Status: IDENTIFY (2026-07-04). Owner: claude-10 (dispatch, review). Background:
`futon2/holes/feature-constellation-design-note.md`. Ratified by Joe 2026-07-04
(emerged from the 2026-07-04 decommission session — retiring portfolio-inference /
issue_holes, keeping mission-control — which was a feature-trim/coalesce exercise run
entirely by hand).**

A bounded excursion, not a build commitment. IDENTIFY names the gap and the terrain;
it does not yet open a construction phase.

## The gap

We currently have **no way to really understand the features that have been built in
the futon stack** — even though we started with **devmaps**, have developed **VSATARCS
documentation**, and have **substrate-2**. The three aren't well aligned, and none of
them is browseable **"live"** in a way that lets an operator look and say *"that part,
that's where it needs trimming"* or *"these two can coalesce."*

The 2026-07-04 decommission session is the evidence the gap is real and costly: every
call — trim portfolio-inference, trim issue_holes, do **not** trim mission-control
(load-bearing), coalesce the hole-vocabularies — was made by hand-computing, per
feature, its purpose / liveness / consumers / overlaps via grep-archaeology. That is
exactly what a feature map should answer at a glance.

## Background — the assets that exist but don't align

- **devmaps** (`.devmap`, 18 files; `mcb/read-all-devmaps` parses them) — already carry
  features as `!`-entries with `:maturity`, `:depends-on`, and IF/HOWEVER/THEN/BECAUSE
  qualifiers. A partial feature list, in text, per repo (`devmap-futonX`).
- **VSATARCS** — machine-generated feature narration + the `leaf/devmap/frame` node
  taxonomy (`devmap-*` = repo level). Good node *labels*; not aligned to a live graph.
- **substrate-2 / cascade-live** (`cascade_real_live.clj`) — mission clusters over
  hyperedges; live, but mission-shaped, not feature-shaped.
- **The Interest Network graph** (`webarxana/client/graph.cljs`) — a *working*
  k-collapsible magnitude-weighted graph (`restrict-to-pins` at k=2,
  `restrict-to-super-core` at k=1). The reusable collapse engine.

The design note (background) already: proposes the mapping (features = nodes,
standing = magnitude, edges = `depends-on` + `overlaps-with`); pins the collapse
algorithm to `graph.cljs` (read from the actual k-handler, not assumed); identifies the
trim axis (magnitude-median retraction) and the coalesce overlay (overlap edges); and
names the ≤20-features/repo governance budget.

## What IDENTIFY establishes (this phase)

1. The gap is real, recurring, and expensive (the decommission session is the exhibit).
2. The materials to close it already exist but are unaligned: devmaps (nodes+edges),
   VSATARCS (labels+taxonomy), substrate-2 (live), Interest Network (collapse engine).
3. A credible mechanism is already sketched and code-grounded (the design note) — so
   the excursion is an *alignment + surfacing* problem, not a research one.

## What would move it to the next phase (NOT committed here)

A scoping/ARGUE pass would decide: node granularity (peripheral vs finer); how
`magnitude` is computed from consumers/liveness/recency; whether the first cut extends
`graph.cljs` directly or a sibling surface; and the smallest end-to-end slice (likely:
one repo's devmap → feature nodes + magnitudes → the existing pins/super-core path →
render, with the ≤20 budget flagged). No build until that pass.

## Scope boundary (bounded excursion)

- Read-only over the existing stack; reuse `graph.cljs`, don't reinvent it.
- No touching the live serving JVM / stores (futon3c I-0).
- A clean kill is a success: if IDENTIFY/scoping finds the assets can't be aligned
  cheaply (e.g. devmap `:depends-on` is too sparse to form a real graph), that finding
  is the deliverable.
