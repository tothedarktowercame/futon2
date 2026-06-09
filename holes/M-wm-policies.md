# Mission: WM selects over policies, not just next steps (M-wm-policies)

**Date:** 2026-06-09
**Status:** IDENTIFY (with grounded MAP material already in hand)
**Owner:** Joe + claude-1
**Repos:** futon2 (`aif/efe.clj` — the pure scorer), futon3c (`peripheral/war-machine-pilot`,
  `report/war_machine.clj` — the live judgement + opts), futon0 (`M-capability-star-map` — the graph)
**Cross-ref:** [[E-possible-world-regulator]] (the sweep harness + live-acceptance findings),
  [[M-capability-star-map]] (the graph the EFE scores over; corrects its Basecamp fan-out §1.1 hypothesis),
  [[M-futonzero-capability]] §22 (the AlphaZero destination), [[C-pudding-prover]] §11 (brake→engine),
  claude-3's Salingaros / substrate-2 (D2/full-C — the field), claude-4's
  M-memes-arrows-patterns-diagrams (the arrows/transitions)

---

## 1. IDENTIFY

### Motivation

The War Machine currently scores **single next-steps** — it ranks `pursue mission M` as if each mission
were one atomic action — and picks the minimum-EFE one. Two consequences, both live as of 2026-06-09:

1. **The visible symptom:** the WM's top recommendation is `open-mission M-emacs-cursor-peripheral`
   (G=−9.25), long noted to be a silly pick. The diagnosis (§2) shows *why*: the "advance the capability
   map" credit is currently **anti-functional** — it buries every on-map mission at the bottom and lets
   off-map work fill the entire top.
2. **The structural issue Joe named:** the WM should **select over policies (sequences of steps), not
   just next steps**. Scoring a mission by its *whole remaining size* is the single-step-vs-policy
   confusion already biting at the one-step level.

These are the same issue at two depths. This mission fixes the one-step credit (the easy cleanup) **and**
charters the policy layer (the bigger build).

### The two tracks

- **Track 1 — cleanup (easy, do now):** repair the graph-EFE term so on-ascent work outranks off-map
  junk. Still single-step EFE. Demonstrated fix in hand (§3). De-risked via the regulator sweep, applied
  live with operator consent.
- **Track 2 — the policy layer (the mission's reason to exist):** generalize "score the next leaf" to
  "score a *policy* = a sequence of leaves," rolled out over a transition model. Gated on a
  field-simulator (no fabricated dynamics — the regulator's lesson). The two pieces are in flight:
  claude-3's substrate-2/Salingaros field + claude-4's arrows. The join is a **forward-model contract**.

### Theoretical anchoring

- **Active inference over policies:** EFE is properly `G(π)` over policies `π`, not over single actions.
  The WM's one-priority selector is the degenerate one-step case. The exploit pole (pragmatic/risk) and
  explore pole (epistemic/info) both accumulate over the rollout.
- **AlphaZero mapping ([[M-futonzero-capability]] §22):** policy = patterns/arrows, value =
  EFE-over-play-outs, search = strategy, reward = peradam/Dokusan. "No perfect simulator" is dissolved
  by "patterns are invariant-enough" — but the rollout still needs a *field* to roll over.
- **Regulator lesson ([[E-possible-world-regulator]]):** don't fabricate dynamics. The field-simulator
  must be real (claude-3's substrate-2), not invented, or the rollout scores fiction.

### Theoretical correction — EFE is a path functional, not a field (Joe, ratified 2026-06-09)

EFE is a property of **policies**, so `G(π)` is a **path integral** — a functional over the
trajectory-distribution a policy induces — **not** a scalar field over states. A field assigns a value to
each *point*; `G(π)` assigns a value to each *path*. The same map node sits on many policies with
different `G`, so a node does not *own* an EFE: "colour each node by its EFE" is a category slip. Three
objects get conflated under "the EFE field":

- **`g(s)` — the per-step integrand.** A legitimate field: the local cost / **Lagrangian density**
  (claude-3's substrate-2 `C`/holes + my per-step graph-ascent). Fine to render — but **not** EFE.
- **`G(π)` — the policy EFE.** The path integral of `g` over the policy's trajectory. The quantity we
  minimise. **Not a field.**
- **`V(s)` — optimal cost-to-go.** *Is* a scalar field over states — but only as the **output** of
  solving the path-minimisation (Bellman value), obtained by doing the integral, not read locally. This
  is the "can be seen as a field" case.

**Why this is the heart of the mission.** The field view is exact *only* for length-1 policies, where
`G(π) = g(s₁)`. Today's WM is exactly that — `argmin` over single-step actions of a per-state proxy.
**The cursor bug is the field worldview drawn as a picture: greedy-pointwise over a heatmap.** Track 2
(policies, length > 1) *is* the move from field to path integral; past length 1 the field view becomes
the error, not the answer.

**Metric / geodesic reframing (generative).** substrate-2's `C` is the local **metric** (Lagrangian
density); `G(π)` is the **action / path-length** in that metric; policy selection is **geodesic-finding**
— the least-EFE *path*, not the lowest-metric *point*. Consequences:
- **Render naming:** the `g(s)` heatmap is the **metric backdrop** — name it the *per-step cost / metric
  field*, **not** "the EFE field." The *figure* is the **geodesic**: selected policies drawn as
  trajectories/streamlines, ranked by `G(π)`. A heatmap read greedily is the very thing Track 2 exists
  to stop.
- The two-kinds-of-off-map finding (§4) is the same insight from the goal side: an unminted goal is an
  **endpoint with no terrain** — no geodesic can reach it; the path must first be built by chartering a
  minting mission.

### Scope in
- Fix `graph-efe-terms` (off-map escape hatch + body granularity) — Track 1.
- Regulator-sweep the fix across the whole field; apply live with consent.
- Charter + spec the policy rollout — Track 2 — including the forward-model contract.

### Scope out
- Building the field-simulator itself (claude-3's substrate-2 / D2-full-C lane).
- Building the arrow/transition structure (claude-4's M-memes-arrows-patterns-diagrams).
- Changing the gap term, the consent gates, or the survival/structural-pressure terms (separate).

### Completion criteria
1. The live WM no longer ranks off-map junk above on-ascent work (the cursor is demoted; an on-ascent
   mission or leaf takes #1), verified on the live judgement — Track 1.
2. The off-map-penalty + leaf-aware-body settings are **swept** (robust across the field, not a hand
   guess), recorded in the regulator, applied to the live opts with operator consent — Track 1.
3. A forward-model contract exists that claude-3's field and claude-4's arrows can both target — Track 2.
4. A policy rollout (≥2 steps) scores a sequence and beats the equivalent greedy one-step pick on at
   least one case where they differ — Track 2 (the policy-selection witness).

---

## 2. MAP — the live diagnosis (grounded 2026-06-09)

Source: live judgement via `futon3c.peripheral.war-machine-pilot/live-judgement`, 88 candidates.

**The graph term is anti-functional.** `graph-efe-terms` (efe.clj:111-137) computes
`:G-graph-pragmatic = applicability + body − ascent`, where `body = graph-body-weight × open-hole-count`.
The **else-branch (efe.clj:134-137) gives off-graph actions a flat `0.0`.** Live consequence:

- **All 12 of the top-12 ranked actions are off-map** (`:G-graph-pragmatic 0.0`).
- **The first on-map mission sits at rank 83 of 88.** (11 on-map candidates, 77 off-map.)
- The cursor (`M-emacs-cursor-peripheral`): rank 1, G −9.25, graph-prag **0.0**, gap 5.25, ascent 0.

**On-map breakdown (sorted by G-total) — the body penalty swamps the ascent credit:**

| mission | G-total | body | ascent | gap | note |
|---|---|---|---|---|---|
| M-hypergraph-operator | −6.25 | 0 | 0 | 2.25 | best on-map, but ascent 0 |
| M-arxana-roundtrip | −6.21 | 0 | 0 | 2.20 | |
| M-essay-corpus-substrate | −2.31 | 3 | 0 | 1.30 | only `single-cycle-leaf? true` |
| M-war-machine-pilot | **+20.29** | **27** | 2 | 0.70 | **on-ascent, buried by body** |
| M-capability-star-map | **+21.60** | **30** | 3 | 1.40 | **on-ascent, buried by body** |
| M-stack-geometry | +66.00 | **72** | 0 | 2.00 | body penalty alone = +72 |

### The two coupled bugs
- **(a) Off-map escape hatch.** Not being on the capability graph scores `0`. Off-map work is *free*;
  on-map work is *taxed*. This inverts the ARGUE principle ("if it doesn't advance the map, it's not
  worth doing").
- **(b) Body penalty is the wrong granularity.** `body = weight × total-open-hole-count` penalizes a
  mission's *whole remaining size* as if completed in one step. The two `ascent>0` missions are ranked
  *worst* of all on-map. **This is the single-step-vs-policy confusion in miniature** — you don't do a
  whole mission in a step, you take its next *leaf* (`:graph/single-cycle-leaf?` is already computed).

> **Correction to [[M-capability-star-map]] Basecamp fan-out §1.1.** Pilot #1 *hypothesised* the live
> caller omits `:capability-graph` from opts (so only `:G-gap` ranks). The live check **refutes** that:
> `:opts-have-graph? true`, and the marking-rule *is* firing (`ascent-progress 0.5`). The real bug is
> (a)+(b) above, not a missing graph. Verify-before-claim earned its keep.

---

## 3. DERIVE

### Track 1 — the cleanup steps (easy; demonstrated)

**Demonstrated fix (sandboxed re-ranking over the live numbers, no live change):** with (a) an off-map
penalty `P` and (b) the whole-mission body term dropped —

| P | new #1 | new #2 | cursor rank |
|---|---|---|---|
| 3 | M-capability-star-map (on-ascent) | M-war-machine-pilot (on-ascent) | 4 |
| **4** | **M-capability-star-map** | **M-war-machine-pilot** | **9** |
| 5 | M-capability-star-map | M-war-machine-pilot | 9 |

At P≈4 the top-5 become all on-map, the two `ascent>0` missions take #1/#2, and the cursor sinks to 9.

**Cleanup checklist:**
1. **Close the off-map escape.** In `graph-efe-terms` else-branch (efe.clj:134-137), replace
   `:G-graph-pragmatic 0.0` with an off-map penalty, gated by a new opt `:graph-off-map-penalty`
   (default `0.0` to preserve current behavior — additive, safe).
2. **Make the body term leaf-aware.** Replace `body-weight × total-open-hole-count` with a *next-step*
   cost: reward/score the next single-cycle leaf (the `:graph/single-cycle-leaf?` flag exists) rather
   than penalize whole-mission size; gate by opt (e.g. `:graph-body-mode :whole | :leaf`, default
   `:whole` to preserve behavior).
3. **Make ascent status-aware** (hard requirement from the forward-model contract v1, below).
   `mission-ascent-progress` must yield **zero** credit for capabilities already `:satisfied` — only
   not-yet-satisfied capabilities in the goal's transitive scope count. Without this, a rollout farms
   ascent by re-attesting satisfied caps. (Suspected-bug flag from fan-out pilot #1, now a correctness
   requirement.) Test: an already-`:satisfied` produced cap contributes 0.
4. **Sweep, don't guess.** Run [[E-possible-world-regulator]] over the *whole field* (not just this
   snapshot) to pin a robust `:graph-off-map-penalty` (and body-mode); P≈4 is the snapshot estimate —
   verify it generalizes. (NB: hand-guessing weights was refuted earlier this session; the regulator
   exists for exactly this.)
5. **Apply live with consent.** Set the pinned values in `live-star-map-efe-opts`
   (futon3c `report/war_machine.clj`). The live EFE is the operator's consent locus.
6. **Tests.** `efe_test` for the off-map penalty (off-map action gets the penalty; on-map unchanged), the
   leaf-aware body (a big on-ascent mission no longer outranked by a small off-map one), and status-aware
   ascent (already-`:satisfied` cap contributes 0).

> **Flag (from the field-side contribution §3, point 4):** Track 1 fixes the *pragmatic* pole only. The
> additive `:G-gap` term is **provisional** — full-`C` (the substrate-2 epistemic field) becomes the macro
> where-prior and retires it (both-live-gated). Do **not** entrench `:G-gap` in the Track-1 fix or tune the
> off-map penalty *against* it as if permanent; once `C` lands, gap and `C` must not double-count "where."

### Track 2 — the policy layer (the forward-model contract)

A policy `π` is a sequence of leaves; `G(π)` accumulates per-step EFE (the Track-1-corrected credit)
over a rollout, discounted. The rollout needs a **transition model** — and fabricating one is the trap.

**The forward-model contract (the join to draft):** what the field and the arrows must each expose so a
rollout can consume them.
- **State** — from claude-3's substrate-2 / capability-graph (the geometry the rollout scores over).
- **Transition (arrow)** — from claude-4's M-memes-arrows-patterns-diagrams: `state × leaf → next-state`
  (which capability/hole-state a leaf advances to).
- **Per-step score** — the Track-1-corrected graph-EFE credit (advance-the-map, leaf-aware).
- **Rollout** — sequence of (state, leaf, next-state), discounted-`G` accumulation; `G(π)` vs greedy.

Drafting this contract so claude-3 and claude-4 converge on it is the move that gets Track 2 going; the
play-out engine is then a thin layer on top. **Do not build the rollout before the field + arrows land**
(fabricated dynamics = the regulator's lesson). The destination is the convergence
([[M-futonzero-capability]] §22): preference = the marking credit, policy = arrows/patterns,
simulator = substrate-2 field, reward = peradams.

#### Field-side contribution (claude-3, 2026-06-09 — the substrate-2/Salingaros/D2 lane)

claude-3 (the lane Track 2 depends on) supplied the field side of the contract. Five points, integrated:

1. **The two geometries are the two EFE poles, not competitors.** My capability-graph-EFE
   (applicability/body/ascent, Track 1) is the **PRAGMATIC pole** — advance the operator's registered
   ascent. The Salingaros/substrate-2 field (`C = T·(10−H)` over scopes; `L = T·H`) is the **EPISTEMIC
   pole + the named local structure** (the actual open holes). `G(π) = pragmatic(graph-ascent) +
   epistemic(substrate-2 C/holes)`. **Track 1 tunes the pragmatic pole; the rollout needs both.**
2. **The field is REAL now.** D1 of `M-mission-scopes-into-substrate-2` closed — 194 scope-trees +
   patterns/psr-pur in a **unified substrate-2**, computed from actual structure (open holes, attested
   PURs, `:detached` pattern-links), not invented dynamics. The concrete answer to the regulator's "the
   simulator must be real."
3. **The forward-model contract — field side:**
   - **State** = a substrate-2 scope-state (per region: which scope-holes open/closed; which patterns
     attested(PUR) vs near-miss/`:detached`).
   - **Per-step score** = `epistemic(C / open-hole entropy + open-pattern affordances) + pragmatic(L +
     my corrected graph-ascent)`.
   - **Transition** = claude-4's arrow: `state × leaf → next-state` (a leaf closes a hole / discharges a
     sorry / grafts a pattern → updates the scope-state). The field supplies the state-deltas; the arrows
     say which leaf causes which.
   - **The render IS the rollout substrate** — "show the EFE field on the Futon City map" (Joe) = the
     contract made visible.
4. **Gap-term retirement couples the two tracks.** Track 1 keeps the additive `:G-gap` *provisionally*.
   Full-`C` becomes the macro where-prior and **retires the additive gap-term** (both-live-gated, per the
   earlier salvo). So the field landing is also what lets Track 1's gap-term retire — **flag: don't let
   the Track-1 fix entrench `:G-gap`, and don't double-count "where" (gap AND C) once C lands.**
5. **Patterns-as-backdrop → patterns-as-policy is the Track-2 hinge.** D1's psr/pur/pxr records (attested
   applications + outcome + prediction-error) are the **labelled examples** grounding claude-4's arrows —
   the seed of the policy moves. "policy = patterns/arrows" is exactly the M-memes
   pattern→wiring-diagram→sorry moment.

**My alignment + the open contract question (belled to claude-3):** the two poles compose **additively**
into `G_step` (consistent with `compute-efe` already summing terms) — good. The open question is the
**state representation**: my pragmatic pole reads the *capability-graph* (missions→capabilities→goal
ascent, futon0 star-map); claude-3's state is the *substrate-2 scope-state* (holes/patterns). For one
rollout to score both poles, the unified state must carry both, and the arrow must update both jointly
(a leaf closes holes **and** attests capabilities). **Q for claude-3:** is the capability-graph already a
region/overlay inside the unified substrate-2 (D1), or does it need to be joined in? That join is the
contract's keystone. Field side builds the **epistemic pole first** (independent of my Track-1 graph-EFE
defects); composes with the corrected pragmatic pole once Track 1 lands.

#### Forward-model contract v1 — RESOLVED (claude-3 ↔ claude-1, 2026-06-09)

**State-rep keystone answered: ONE state object via a thin materialize+link, NOT a coupled pair.** The
hinge already exists by construction — D1 detected the substrate-2 capability-scopes by matching mission
text against the *same* `M-capability-star-map.graph.edn` `:capabilities` registry that `efe.clj` reads
as `:capability-graph`. So every capability-scope already names a real star-map capability; the
mission↔capability link is already in substrate-2 (the `.graph.edn` capability *nodes/ascent-edges* are
not yet materialized — that is the one small ingest claude-3 owns).

- **State `s`** = a substrate-2 snapshot, one object hinged on capability-scopes:
  - *epistemic part* = per-mission scope-hole set (open/closed + `:detached`).
  - *pragmatic part* = per-capability `{:status, :pre-registered?, :scope/parents}` — the materialized
    `.graph.edn` overlay over the shared mission nodes.
- **Per-step score `g(s)`** = `epistemic(C / open-holes)` + `pragmatic(Track-1-corrected graph-ascent)`.
  **Additive.** claude-1 bells the corrected pragmatic per-step; claude-3 composes it in.
- **Transition `T(s, leaf) → s'`** (claude-4's arrow) = leaf closes scope-hole(s) [epistemic] **and**, if
  it closes a *capability-scope*, flips that capability `:status → satisfied` [pragmatic, via the hinge].
  One arrow updates both edge-families jointly — no separate join object.
- **Rollout `G(π) = Σ discounted g(s_t)`** over `π = (leaf₁, leaf₂, …)`; `G(π)` vs greedy one-step is the
  **completion-criterion-4 witness.**

**The one requirement this puts on Track 1 (claude-1 owns):** the corrected pragmatic per-step must be
**status-aware** — `mission-ascent-progress` currently credits a mission for the capabilities it
`:produces` in the goal's transitive scope *without* checking whether they are already `:satisfied`. In a
rollout that flips capabilities to `:satisfied`, an already-satisfied capability must yield **zero**
further ascent-credit, else a policy farms ascent by re-attesting satisfied caps. (Flagged earlier in the
fan-out pilot #1 as a suspected bug; the rollout makes it a hard correctness requirement.) So Track 1's
deliverable to the field side = the off-map-penalised, leaf-aware **and status-aware** graph-ascent.

**Division of labour:** claude-3 owns materialize+link + the Futon City per-step-cost/metric field render
(epistemic pole built + screenshotted already — NB the *metric backdrop*, not "the EFE field"; the EFE
*figure* is the geodesic/path overlay, per the §1 path-functional note); claude-4 owns the arrows (grounded by D1's psr/pur/pxr labelled
examples); claude-1 owns the corrected status-aware pragmatic per-step (Track 1) + the rollout engine
once the field + arrows land.

**Status (2026-06-09) — materialize+link LANDED** (claude-3, `mission_scope_ingest.clj
--wire-capabilities`). 33 capabilities from `M-capability-star-map.graph.edn` materialized into
substrate-2 as `scope/capability/<cap-id>` nodes, **reusing D1's capability-scope endpoints** (the
by-construction hinge → one state object). Per node: `:capability/{status, pre-registered?, frontier?,
claimed?, minted-by}`; +13 ascent edges, +34 produces edges; 23 claimed / 10 unclaimed. **My status-aware
pragmatic per-step now reads `:capability/status` directly** (GET
`/api/alpha/entity/scope%2Fcapability%2F<cap-id>`); the transition flips it when a leaf closes the
capability-scope. The state side of the contract is delivered; the pragmatic per-step (Track 1) is the
remaining piece on my side.

**Render-side reframe ratified (claude-3, 2026-06-09).** The epistemic render is renamed to the
*per-step-cost / metric field* (substrate-2 `C` + `:detached` = the local metric); the star UI = ⭐/🌟
**endpoints over the metric** (an unclaimed ⭐ = an *unreachable endpoint* = the off-map-GOAL case — "same
insight, two views"). The *figure* will be the **geodesic**: selected policies drawn as trajectories
ranked by `G(π)`, not the heatmap. claude-3 also adopted the shared-kernel rule for the arrow wiring (`T`
IS the live transition). **The geodesic figure + the composed two-pole object are gated on Track-1's
status-aware pragmatic per-step** — i.e. Track 1 is now the critical path for all three lanes.

**Star UI LANDED (claude-3, 2026-06-09)** — `futon6/scripts/mission_efe_field.py → mission-efe-field.html`.
The metric field `g(s)` rendered as a **topographic map** (7 level bands + contour lines, not a smooth
blob — legible level sets: a central massif = the dense mission core, plus a secondary island). 🌟 claimed
capabilities sit on the terrain at their minting missions (link-lined); ⭐ unclaimed sit in a disconnected
**"frontier sky"** strip — *the off-map-GOAL / unreachable-endpoint made literally visual* (the
cold-outreach + KIT commercial cluster + the 3 aspirationals are all in the sky). The geodesic *figure*
(selected policies as streamlines) is the next layer, gated on Track-1's corrected per-step.

#### Transition layer — claude-4's arrow store (M-memes-arrows-patterns-diagrams, reviewed 2026-06-09)

claude-4's deliverable (`futon3a/README-memes-and-arrows.md`, INSTANTIATE H1-H6 complete) supplies the
contract's **transition `T`** — and it is richer than "a bare state-transition." An arrow is a
`(have, want)` endpoint pair at **three maturity stages** (one arrow gaining structure,
`correlation → conjecture → proof`):
- `:correlated` (cascade/hunch) — observed co-occurrence, no committed goal.
- `:open` (sorry) — goal committed, type fixed by the surrounding construction, **method absent**.
- `:constructed` — a runnable method that produces `want` from `have` (a wiring diagram / BHK arrow).

**Mapping onto the rollout** (this refines contract-v1's Transition clause):
- **`:constructed` arrows = available transitions** the rollout may traverse.
- **`:open` arrows = the needed-but-unbuilt transitions = the holes/leaves** a policy aims to close.
- **`:correlated` arrows = candidate hunches** (pre-commitment, not yet a leaf).
- **A rollout leaf = construct an `:open` arrow** whose `have` holds in the current state (promote
  `:open → :constructed`) → `want` becomes available **and** the arrow crosses into substrate-2. This IS
  claude-3's "`T` flips `:status → satisfied`": only `:constructed` arrows promote (the priors↔facts
  boundary, R7), and they land as `code/v05/sorry` hyperedges — the *same* scope-hole convention
  claude-3's epistemic state already reads. So a leaf closing an open arrow updates the epistemic state
  by construction.
- **Endpoint-identity keystone** (`(have, want)` primary key, unify-not-mint, I1+I4): the transition
  graph self-dedups — a freshly-mined sorry unifies onto its existing arrow rather than minting a dup.

**Storage split the rollout must respect:** the arrow store is `futon3a`/SQLite — the *proposals/priors*
layer where an arrow lives its life; substrate-2 (`futon1a`, :7071) holds only `:constructed` arrows +
scope-holes (the *facts*). So the rollout reads **both**: candidate leaves (`:open` arrows) from the meme
store, satisfied state from substrate-2.

**Open seam to resolve with claude-4 (the transition-side analogue of claude-3's state-rep keystone):**
the *epistemic* side aligns by construction (constructed arrows land as `code/v05/sorry` scope-holes).
The remaining question is the **pragmatic side**: does an arrow's `want` endpoint resolve onto a
*capability-graph cap-id* (so closing it advances ascent / flips `:capability/status`)? The example
endpoints are notion/scope-level; `meme.endpoints` already resolves against the live `scan-aif-heads`
registry. So either the `want↔cap-id` mapping already holds, or a thin endpoint→capability resolver is
needed (mirroring claude-3's materialize+link). **This is the next contract question before the rollout
engine can walk the arrows.**

**Seam RESOLVED — claude-1 ↔ claude-4 whistle salvo, 2026-06-09.** The three namespaces are *disjoint*
(star-map cap-ids · arrow `want` endpoints · scan-aif-heads ids) — verified, no lexical bridge — so a
blind `want→cap-id` resolver is out. Resolution = **declare-don't-guess**: the constructing leaf carries
the cap-id it advances.
- **Field:** `:advances-cap <cap-id>` on the arrow (a nullable `advances_cap` column) — OPTIONAL, stamped
  at construction by the **rollout leaf** (its pragmatic target, *never* inferred from the endpoint
  string). Absent ⇒ ordinary arrow: construct + cross to substrate-2 only, no ascent.
- **`promote!` (claude-4's layer), on `:open → :constructed`:** validate `advances_cap` against the
  `:7071` capability overlay (`GET /api/alpha/entity/scope%2Fcapability%2F<cap-id>`) — reject/flag loudly
  if unknown (no silent mislock) — then **route on the cap's class, read from the registry** (data-driven,
  not special-cased): `:capability/frontier? false` ⇒ ordinary ⇒ flip `:status → :satisfied` (the
  construction *is* the evidence); `:capability/frontier? true` ⇒ frontier ⇒ emit `:status :claimed`
  (🟡-pending) + a proposed-flip event, **never auto-satisfy**. *(NB `:pre-registered?` is `true` for
  both — `:frontier?` is the discriminator; verified `:agency` frontier?=false/`:satisfied`,
  `:ai-passes-prelims` frontier?=true/`:held`.)* Idempotent, keyed by `(endpoint-key, cap-id)`.
- **Layer split:** claude-4 = the `advances_cap` column + the `promote!` validate/route/write block;
  claude-1 = wire the rollout leaf to stamp `:advances-cap` + the status-aware per-step reads `:satisfied`;
  **claude-1/Dokusan = the witness gate that turns frontier `:claimed → :satisfied`** — `promote!` never
  closes a frontier cap.
- **Exactly-once loop:** construct flips `:satisfied` (write) ∧ status-aware per-step gives 0 credit for
  satisfied (read) ⇒ a leaf advances ascent exactly once, no farming.
- **New cross-lane contract:** claude-4's `promote!` now reads claude-3's `:7071` overlay as a *validation
  API* — `:capability/frontier?` + `:capability/status` + the `scope/capability/<id>` endpoint are a
  stable contract (FYI'd to claude-3; recorded in `futon3a/README` §4a on claude-4's side).
- **Build status:** claude-4's half (column + `promote!` block + a worked example) is specced +
  handoff-ready (codex-2 + claude-4 review, the H1–H6 pattern); **not on the immediate critical path** (I
  wire my halves after the Track-1 sweep). **GREENLIT as a warranted E-excursion (Joe, 2026-06-09)** —
  claude-4 owns it end-to-end (scope/name/dispatch-to-codex-2/review/land).

---

### Prior art to port — `~/code/ukrn-services-simulation/` (reviewed 2026-06-09)

Joe's UKRN-services sim is AIF-based, Clojure/EDN, **zero external numeric deps** (`java.lang.Math` only)
— so its machinery ports cleanly into `efe.clj`. Honest finding: its EFE is *also* mostly single-step
per-tick (risk + ambiguity + pattern + budget + priority, composed each tick) — like our WM — **except**
the budget term, which is a genuine horizon rollout. That rollout is the gold.

- **The path-integral kernel: `project-budget-path`** (`notebooks/ukrn_v3_efe.clj:274-315`). A K-tick
  horizon loop (default K=4) that accumulates per-step cost, applies a periodic replenishment schedule,
  and has a sticky `:truncated` absorbing barrier. This **is** the `G(π) = Σ_t g(s_t)` accumulator we
  need — port the loop structure as the rollout engine's spine.
- **Shared-kernel pattern: `step-kernel`** (`notebooks/ukrn_v3_kernel.clj:235`). One `T(state, action) →
  next-state` is called by **both** the live step and the predictive rollout — so the simulator *cannot
  drift from reality*. This is the regulator's "the simulator must be real" lesson solved
  *structurally*. **Architectural port:** claude-4's arrow `T` should BE the same transition the live WM
  uses, not a parallel sim. (Ties to our [[E-possible-world-regulator]] no-fabricated-dynamics rule.)
- **Belief update: `update-mu`** (`notebooks/ukrn_v3_belief.clj:12-40`) — Gaussian precision-weighted
  predictive-coding; standard, portable, if we make states probabilistic.
- **Selection: softmax + abstain** (`notebooks/ukrn_v3_efe.clj:599`) — `P(a) ∝ exp(−G/τ)` with an
  abstain guard when top candidates are indiscriminable. *Not* geodesic, but a principled
  policy-sampler; the abstain-on-low-discriminability guard maps onto WM-I4 (don't act when the field is
  flat).
- **Term templates:** `risk-term-v3` / `ambiguity-term-v3` (`ukrn_v3_efe.clj:233-252`) — pragmatic
  (goal-distance) + epistemic (entropy) decomposition, drop-in shape for our two-pole `g(s)`.

**Port plan (Track 2):** lift `project-budget-path`'s loop as the rollout spine; generalise its per-step
cost from "engagement cost" to our `g(s) = epistemic(C) + pragmatic(corrected graph-ascent)`; drive it
with claude-4's arrow as the `step-kernel`; select policies by softmax-over-`G(π)` with the abstain
guard. All pure-Clojure, no new deps.

## 4. ARGUE

### Open ARGUE finding — three kinds of off-map (claude-3 ↔ claude-1, 2026-06-09)

The off-map treatment distinguishes **three kinds, needing different handling** (refined from the original
two when claude-3 placed the unclaimed goal-endpoints in the metric):
- **off-map WORK** (a candidate mission not on the capability graph) → **penalise** (Track 1's off-map
  penalty — it doesn't advance a registered destination).
- **off-map GOAL / summit** (a pre-registered cap that `:scope`-builds on a *claimed* ascent-parent — e.g.
  `full-arxiv-mining`, `wm-overnight-unsupervised`, `ai-passes-prelims`) → **reachable**: a geodesic *can*
  run to it from the claimed substrate below. Finite distance-to-goal; a normal ascent target, just high
  up. Place it on the map above its substrate (dashed link).
- **off-map GOAL / island** (a pre-registered cap with *no claimed anchor anywhere* — the entire `kit-*` +
  `cold-*` commercial cluster; `cold-response-conversion` has **zero edges**) → **unreachable**: distance =
  ∞, **no path exists**. Generates no ascent-gradient (the flat-field problem) — it **cannot be aimed at**
  until terrain is constructed. Surface as **"needs a foothold,"** and treat *constructing the foothold* as
  the first (meta-)leaf — not as a target the per-step scores toward.

**Reachability ⟂ witness-class.** The summit/island axis (can a geodesic run?) is *orthogonal* to the
`:frontier?` axis (promote!'s witness gate). Verified: both `cold-response-conversion` (island) and
`wm-overnight-unsupervised` (summit) are `:frontier? true`. Keep them separate: reachability is the
rollout's concern; `:frontier?` is promote!'s routing.

**Island foothold — route (a), design-pattern-embed (Joe's lean; aligned, build held for operator go).**
Write the commercial cluster as a *design-pattern* and embed it in the same MiniLM space as
patterns+missions. Its job is to **seed candidate `:open` arrows** (M-memes `h4` similarity-join:
similar+co-occurring+no-construction → seeded open sorries) = *conjectural terrain*. Then the rollout's
distance-to-goal is the **path-length over those seeded arrows**, not the raw cosine (the embed seeds
terrain; the path-metric measures it). The path is low-confidence (`:open`/`:correlated`, conjectural),
labelled distinct from a summit's claimed-substrate path. Rejected route (b) — a `:real-mission? false`
stub mission — as fabricating a path prematurely (the cluster is unclaimed *because* it has no defined
path). **Drop-in:** route (a) IS the transition contract above — the seeded arrows are claude-4's arrows;
constructing one toward an island carries `:advances-cap <island-cap>`; island caps are `:frontier?` →
`promote!` routes them to `:claimed`/witness. No new mechanism. *Build of route (a) creates an artifact +
touches commercial/income strategy (M-futon-forward-model) → operator go before building.*

### VERIFY / INSTANTIATE

**Track 1 — INSTANTIATE done (regulator sweep + operator-consented live-apply, 2026-06-09).**

Regulator sweep over the live snapshot (efe.clj reloaded), `:leaf` + status-aware on, varying `P`:

| P | cursor | M-capability-star-map | M-essay-corpus (leaf) | M-futonzero-mvp | M-EOI |
|---|---|---|---|---|---|
| 0 | **1** | 18 | 21 | 2 | 3 |
| 4 | 3 | **1** | **2** | 4 | 10 |
| 6 | 9 | **1** | **2** | 14 | 16 |
| 8 | 14 | **1** | **2** | 18 | 19 |

Findings: (1) the **off-map penalty is the load-bearing knob** — at `P=0` (leaf+status-aware on, no
penalty) the cursor is still #1; leaf-body/status-aware fix the on-map *ordering*, the penalty closes the
*escape hatch*. (2) At any `P≥4` the live top flips to on-ascent work (#1 `M-capability-star-map`, #2
`M-essay-corpus-substrate` leaf), stable. (3) **The penalty is blunt** — it can't distinguish the *silly*
off-map cursor from *valuable-but-unwired* off-map work (`M-futonzero-mvp` moves down *with* the cursor);
no `P` isolates the cursor. **The real lever for unwired-valuable work is wiring it onto the graph** — the
penalty correctly creates that incentive (the ARGUE principle). **`P=4` chosen** (operator) as the gentlest
setting that flips the headline with least collateral (futonzero-mvp stays #4; residual: cursor at #3, no
longer the recommendation).

Live-apply: `live-star-map-efe-weights` (war_machine.clj `c13b5e2`) →
`{:graph-off-map-penalty 4.0, :graph-body-mode :leaf, :graph-ascent-status-aware? true}`; `efe` +
`war-machine` reloaded via Drawbridge (behavior-preserving by default; the new opts are the change). The
in-process rollout under the new live opts confirms **cursor → #3, `M-capability-star-map` #1**. The served
judgement (`!wm-snapshot`, scheduler period 300s) refreshes on the next tick (triggered async via
`request-tick!`). NB the live JVM now runs the reloaded `efe`/`war-machine`; a JVM restart would reload
from the (committed) branch files.

**Track 2 — in progress:** state (claude-3, done) + transition (claude-4 arrow store + the `:advances-cap`
promote! E-excursion, greenlit) + per-step (Track-1, done) → the **rollout engine** (mine) is the remaining
build, plus the route-(a) island-foothold (aligned, build held for operator go). Witness = a ≥2-step
rollout that beats the greedy one-step pick.

### PSR / PUR
- **PSR (Track 1 fix):** Pattern: `logic-model-before-code` (verify the design over the live trace before
  coding) + the ARGUE "advance-the-map" principle. Alternatives considered: thread `:capability-graph`
  into opts (refuted — already there); raise ascent-weight only (insufficient — body still swamps).
  Rationale: the off-map escape + body-granularity are the two terms actually inverting the ranking,
  shown on live numbers. Confidence: high (demonstrated sandboxed).
- **PUR:** _pending implementation + sweep._
