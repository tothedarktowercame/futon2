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

## IDENTIFY addendum — a fifth asset: the pattern-road graph (2026-07-04, Joe via claude-18)

Operator observation from the live EFE map session: the **mission × applied-pattern
bipartite graph** (surface form: `futon6/data/mission-carpet-roads.json`, 711
attestation-weighted roads; computed in `mission_carpet.py` from `.flexiarg` names in
mission texts × `pattern-attestation.json`) could be **folded in to find features
structurally** — used *with* the other sources, not instead of them.

What it adds that devmaps/VSATARCS/substrate-2 don't:
- **Enacted features.** Pattern clusters that co-travel across missions are
  features-of-practice — what activity treats as a unit vs what authors declared.
  Declared-but-never-enacted vs enacted-but-never-declared are both diagnostics
  (disagreement between sources IS the signal).
- **Magnitude input.** Turn-attestation is a ready-made liveness/recency term for the
  design note's `magnitude` computation.
- **Overlap edges.** Pattern co-application supplies `overlaps-with` candidates where
  devmap `:depends-on`/overlap data is too sparse — the named kill-risk of this
  excursion.

Retro-diction check (one data point, but it is this excursion's own exhibit):
**portfolio-inference**, hand-trimmed in the 2026-07-04 decommission session, shows the
exact "much-cited hub, faint fan" signature on the map — 27 roads, best shared-pattern
attestation 16, four roads at 0. The structural signal flags at a glance what the
decommission session established by grep-archaeology.

For the scoping/ARGUE pass this implies a fourth open question: how pattern-cluster
(enacted) granularity aligns with devmap `!`-entry (declared) granularity.

## v0 addendum — first computed constellation (2026-07-05, Joe + Claude, live session)

Joe redirected the seed away from devmaps ("I wonder if the devmaps are out of
date") to the **live cascade's mission clusters** as the core to grow from /
retract to. `feature_constellation.py` (this dir) now computes, zero hand-typed
rows: seed = `GET :7070/api/alpha/cascade-real/graph` clusters (172 missions,
12 clusters); grow = `futon6/data/mission-carpet-roads.json` attestation-weighted
pattern roads (440 joined); magnitude = :applied warrants + log1p(incident road
attestation) — *enacted only*; retract = faithful `graph.cljs`
`restrict-to-super-core` (strictly > median) + largest component → **72-mission
core, 439 roads**; hub layout = BGE semantic centroids (Futon City coords) +
repulsion. Outputs: `feature-constellation.{json,png}` — a two-level feature
network (10 surviving cluster-features, aggregated inter-feature attestation
edges). Honest retro-diction: **07-essays-arxana and 10-substrate-metric retract
away entirely** (10 = the cascade's hole-basin; 0 confirmed warrants — the
structural signal and the dashboard agree). First consumer: the expanded skills
summary (`powerbi-tui/skills-expanded.pdf`, Figure 3). Open (unchanged from
IDENTIFY): devmap alignment, magnitude refinement, live/browseable surface —
this v0 is a static projection, not the browseable map.

## Scope boundary (bounded excursion)

- Read-only over the existing stack; reuse `graph.cljs`, don't reinvent it.
- No touching the live serving JVM / stores (futon3c I-0).
- A clean kill is a success: if IDENTIFY/scoping finds the assets can't be aligned
  cheaply (e.g. devmap `:depends-on` is too sparse to form a real graph), that finding
  is the deliverable.
