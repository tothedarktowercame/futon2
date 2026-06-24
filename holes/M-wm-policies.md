# Mission: WM selects over policies, not just next steps (M-wm-policies)

**Date:** 2026-06-09
**Status:** **CLOSED (Joe, 2026-06-24).** All 4 completion criteria MET + live-verified; Track 1 LIVE, Track 2 (rollout/R13) LANDED+witnessed, Track 3 (niche-construction/R17 + grounding/R18) LIVE+committed; DOCUMENT done (§5 + `M-wm-policies-documented-run-2026-06-24.md`). Drove the AIF contract **R12→R18** (created R13; surfaced R16/R17/R18). Gated next-phases continue under their own homes: Car-3 acting → operator arming (R16 loop-closure + consent gate); `E-prove-salingaros-cascade-scorer` (R18 proof); route-(a) island foothold → operator go.
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

### Checkpoint: policies → proof strategies (2026-06-22, Joe + claude-1)

*A pointer left for another agent to pick up, while the futon6 side focuses on the post-IATC
Lean↔NL alignment. It says: the CT proof-strategy work is a concrete, field-is-real proving ground
for this mission's `G(π)`.*

**The use-case that makes `G(π)` concrete.** A *proof strategy* (induction, diagonalisation,
construct-an-auxiliary-object, reduce-to-known) is an **AIF runner** — a policy `π`. "Verified" = its
**path integral** `G(π)`: a tactic-trace that discharges the goal is a low-`G` path; a strategy applied
outside its domain "runs out of road" (high `G` — induction wants countable+recursive structure;
diagonalisation wants reals-like). So strategy selection/verification over proofs **is** the `G(π)`
policy-evaluation this mission charters — the same frontier, approached from mathematics instead of the
WM backlog.

**Where it already runs (futon6, this session).** The CT/IATC → CLean → comprehension pipeline is a
low-stakes Track-2 testbed:
- a per-proof **comprehension floor** = R2d (noun grounding) ⊕ rung-3 (strategy grounding) — a graded
  `g(s)`-style per-step score (grounded / thin / ungrounded), corpus-relative.
- the **strategy axis is the policy-verification problem**: a `thin` move = a recognised-but-unverified
  strategy (a policy proposed, `G(π)` not yet evaluated); `grounded` = verified discharge; converting
  thin→grounded **is** doing the path integral.
- **the field is real here** (the §1 regulator lesson — don't fabricate dynamics): Lean's kernel
  furnishes verification for free. A Lean tactic is a verified runner; a tactic-trace to QED is `G(π)`
  already evaluated. Seeding the strategy vocabulary from Lean tactics (ProofBridge / NuminaMath-Lean-PF,
  ~39k NL↔Lean 4 proof pairs) is a verified policy library with no invented field.

**The mapping (WM ↔ proofs):**

| M-wm-policies | proof-strategy instance |
|---|---|
| policy `π` (sequence of leaves) | proof strategy / tactic-trace (sequence of moves) |
| `g(s)` per-step integrand | per-move comprehension score (R2d ⊕ rung-3) |
| `G(π)` path integral | does the strategy discharge (Lean kernel / verifier) |
| field-simulator must be real | Lean kernel **is** the real verifier (no fabricated dynamics) |
| a policy's domain | a strategy's basin (tactic preconditions / typeclasses) |

**Open question handed off.** Can the CT proving-ground **close Track 2's forward-model contract
empirically** — use Lean-verified tactic-traces as ground-truth `G(π)` rollouts to validate the
policy-beats-greedy witness (completion criterion 4) on a domain where the field is genuinely real,
then port the lesson back to the WM backlog field? Track 1 (the three coordinated moves, §3) is
unchanged by this checkpoint. Cross-refs: `futon6/holes/excursions/{E-comprehension-foundation,E-clean}.md`,
`futon6/holes/clean/CLEAN-LEAN-RELATION.md`, ProofBridge (arXiv:2510.15681).

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
  claude-4 owns it end-to-end (scope/name/dispatch-to-codex-2/review/land). **LANDED:
  `E-wm-policy-arrow-seam`, futon3a `9e9d446` "Wire meme arrows to capability ascent" (codex-2,
  claude-4-reviewed)** — touches `meme/{schema (advances_cap column), identity (promote! validate/route/
  write), cap_ascent (NEW — validate-against-:7071-overlay + ordinary-flip / frontier-proposed-flip), arrow,
  writer}` + a worked example. My halves (rollout leaf stamps `:advances-cap`; status-aware per-step reads
  `:satisfied`) integrate at the rollout-engine build.

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

**Track 2 — the rollout engine LANDED + reviewed-pass + independently verified (2026-06-09).** The search
half of the AlphaZero loop now exists and the witness passes.
- **Shas:** futon2 `65f137d` "Add policy rollout engine" (`futon2.aif.rollout` + `rollout_test.clj` +
  witness `holes/labs/e-rollout-witness.clj`); futon3a `2122daf` "Extract pure meme transition step"
  (`src/meme/step.clj` — the MUST-A shared pure kernel). Codex-authored, **claude-4-reviewed** (traced).
- **claude-1 independent verification (the real gate, not a rubber-stamp — what I ran):**
  - `rollout-test`: **3 tests / 9 assertions / 0 failures.**
  - Full futon2 suite: **326 / 1004, 1 failure** = the *same pre-existing* `sorry-registry` red (was 323/1;
    the 3 new rollout tests added cleanly — **no new regression**).
  - **Witness (independent lab run):** greedy one-step `G = −0.2` vs **2-step rollout `G = −1.0`** — the
    multi-step policy (`root→bridge`, `bridge→agency`, where step-1 unlocks step-2's `have`) **beats greedy**,
    which greedy can't see. **This is the proof policy-EFE works.**
  - **MUST-A ✓** live-plan-target `:claimed` = sim-target `:claimed` (same `meme.step/step` kernel, no drift).
  - **MUST-B ✓** rollout write-count = **0** (zero `:7071` writes during search — the safety invariant holds).
  - `:move/terminal?` carried + truncated (centre-mess); consumes the real 19-move stub honoring
    `:conjectural` reachability; PUCT-prior branching (R1) + threaded `:move/id` return-channel (R2).
- **State:** the SEARCH half is live against claude-3's stub. The full loop needs claude-3's real grad-loop
  producer (building in parallel) as the live prior, + v2 (the return channel — claude-4 emits realized
  `G(π)`-per-`:move/id` back to train the prior, on demand). **Gated on operator merge** (branch
  `wm-outing/2026-06-08-regulator-sweep`).
- Still open: route-(a) island-foothold (aligned, build held for operator go).

### Make-it-live plan — value-correction + cascade-surfacing (2026-06-09)

Joe's framing: a next-step is a **degenerate (length-1) policy**, so the rollout *generalizes* the live
selector rather than replacing it — and he wants to **see** non-degenerate policies, not have them collapse
back into a re-ranked single action. Two layers:
- **(A) Value-correction (the floor):** `generate-war-machine` scores each candidate by `G(π*)` = best
  policy starting with it; length-1 = today's single-step EFE (always-available fallback).
- **(B) Cascade-surfacing (the *visible* non-degenerate policies):** a **cascade** = `:correlated` chain
  (`arrowᵢ.want == arrowᵢ₊₁.have`); `π = "use a cascade"` is a ready-made multi-step policy (no
  search-from-scratch). The rollout scores `G(π)` over the *given* chain (the accumulator already does this);
  the WM **displays** top cascade-policies as their own lane.

**claude-4's empirical findings (scoping whistle):**
- **Store is empty of cascades** — live `meme.db` has 1 arrow; worked-example seeds were transient. So (B)
  needs a **generation step 0**. Real source found: `pattern_phylogeny`'s **2538 co-application edges**
  (mission-co-occurring pattern-pairs = ready `:correlated` arrows; co-app weight = the prior). Single arrows
  first; chains emerge as paths over the co-app graph.
- **Chain-follow unroll** = DFS over `:correlated` edges from each root `have`; single-arrow = length-1,
  maximal path = the length-K non-degenerate policy; surface top-`G(π)`.
- **Namespace flag (a design fork):** cascades live over **pattern-ids**, the value-corrected candidates over
  **mission/scope-ids** → (B) is a **distinct "pattern-cascade policy" lane** shown *alongside* the
  value-corrected mission actions, not conflated. (That separation is what makes the non-degenerate policy
  visible *as its own thing*.)

**Build order / ownership:** (0) batch-gen cascades from `pattern_phylogeny` → `:correlated` arrows ·
(1) chain-extraction `cascades-as-candidate-policies` · (2) rollout scores `G(π)` per cascade — **claude-4**
(codex + review, charter `E-cascade-policies` or extend `E-policy-rollout-engine`); (3) wire into
`generate-war-machine` + the dual-lane display — **claude-1** (holds the live-judge context). MUST-B holds
(scoring sim-only; `:correlated` arrows never promoted during scoring).

### The policy framework — a cascade is an on-the-fly pattern-semilattice (Joe, 2026-06-09; SUPERSEDES the stored-chain plan)

The stored-chain reading above is the *degenerate, linear* case. The real object (Joe):
- **A cascade is constructed *on-the-fly* for a circumstance** — Christopher Alexander's original name for a
  Pattern Language. It is precisely **the ARGUE move**: "what patterns make the case for *this* design?"
  Every mission's ARGUE phase already constructs one; we make it a first-class, scored object. The
  `pattern_phylogeny` is the **prior** (what tends to combine), not the cascade itself.
- **A policy is a SEMILATTICE of patterns, not a sequence** (Alexander, *A City is Not a Tree* — patterns
  overlap, they don't nest in a tree). So the rollout's linear `G(π)=Σγ^t g(s_t)` is the *linearized*
  degenerate case; the real `G(π)` is a **wholeness-functional over the semilattice** = Joe's "any more, too
  complicated; any less, not expressive enough" = **Salingaros `C = T·(10−H)`** (expressiveness `T` vs
  parsimony `H`). **claude-3's epistemic metric `C` *is* the cascade-scoring, one scale up.**
- **Each pattern is itself a policy** → policies compose recursively; "move" and "policy" are the same kind
  of thing at different grains.
- **The bra `⟨ψ|` (the circumstance) is heterogeneous and extensible** — `⟨ψ=mission|`, `⟨ψ=scope|`, … i.e.
  **substrate-2's node-types** (the unified state already holds scope/mission/capability/pattern/PSR). Moves
  are an **extensible operator-registry over that state** (not the fixed 4 `:move/class`) — find-a-messy-
  mission · run-a-cascade-over-a-scope · match-a-pattern-across-missions · annotate-a-PSR · … — and they
  **compose across ψ-types**. (That extensible operator-space is the **M-aif2 extensible-registry /
  niche-construction** primitive — the make-it-live work and aif2 converge here.)
- **Complexity is expressible but NOT rewarded** (Joe): the `C`/wholeness score selects the *simplest
  adequate* policy for the circumstance — Occam via `C`. A very-complex composed policy is *representable*
  but would usually score *low* (too complicated). So the policy-space is rich; the scoring keeps it honest.

**Notation (Joe, refined):** patterns/policies are **operators**, missions/scopes are the **state-vectors**
`|ψ⟩` they act on — a cascade is a product `O₃O₂O₁|ψ⟩`, scored by the wholeness of the result. (Container
stays **M-wm-policies**; a **Campaign** spanning WM + aif2 + differentiable-substrate may be registered
later. For now: **experiments, not formalism.**)

**Experiment menu (candidate useful policies — affordances, not constraints; simple → composed →
self-improving):**
1. *Discharge-the-readiest-sorry* — `|ψ=open sorry, have satisfied⟩` → BHK-construction pattern →
   `:open→:constructed`. (pudding-prover; simplest useful non-degenerate move — it *completes*.)
2. *Clean-a-mess* (the seed) — `|ψ=low-coherence district / messy mission⟩` → mission-coherence cascade →
   raise `C`. (Futon City + `C`.)
3. *Unblock-then-do* — `|ψ=mission blocked by an unbuilt dependency⟩` → discharge dependency, then mission.
   (The canonical non-degenerate policy greedy can't see — the witness shape.)
4. *Walk-an-ascent* — `|ψ=on-ascent mission w/ claimed substrate⟩` → leaf-chain from substrate to a summit
   cap (wm-overnight / ai-passes-prelims). (Star map + rollout.)
5. *Propagate-a-win* — clean a mission → find another using the same pattern (co-application) → annotate its
   PSR. (Joe's cross-ψ composition; phylogeny + PSR surface.)
6. *Mint-a-peradam* — `|ψ=mission near a 3-witness certificate⟩` → complete labor+arrow+fruit → mint.
   (Ties policies to the reward.)
7. *Canalize-a-proven-pattern* — `|ψ=pattern applied across missions⟩` → EXPLORE→ASSIMILATE→CANALIZE → a
   new library operator. (**Policies that extend the operator-registry = aif2 niche-construction — the
   deepest one: policies that create policies.**)

**The seed already exists: E-warranted-play = the length-1 cascade** (given a mission, select ONE pattern,
apply it — the selector built this session). So we **generalize E-warranted-play (1-pattern) → small
pattern-semilattice**, scored by `C`, growing the operator-registry as real circumstances demand it
(niche-construction, not big-design-up-front). Spectrum: `1 pattern (E-warranted-play) → small semilattice
→ full ARGUE-grade language for a hard design`.

**Proposed concrete seed (claude-1):** take a *real messy mission* (e.g. `interim-director-proxy-metric-
inventory`, ~29 open holes), construct the **mission-coherence cascade** (a small pattern-semilattice) over
it on-the-fly, score it by `C` (show the too-much/too-little knee biting on real data), surface it in the WM
as a *visible non-degenerate policy*. Smallest thing that demonstrates a *grown* policy + the scoring
discriminating — and it's Joe's own first example.

**Seed experiment RESULT (claude-4, `futon3a/holes/labs/M-memes-arrows/cascade_wholeness_experiment.py`,
2026-06-09).** `|ψ = M-interim-director-proxy-metric-inventory⟩` (broad messy mission; 30 patterns retrieved
via the repaired notions minilm index, scored by a wholeness *proxy* — claude-3 owns the real `C`). **The
knee EXISTS but the scoring FORM is load-bearing:**
- *Naive proxy* (coverage − redundancy·k): **degenerate — k=1 wins, no knee.** This is **the cursor-bug at
  the policy level** (greedy/pointwise reproduces the single-step degeneracy one scale up).
- *Submodular / coherence-greedy proxy* (`W = Σrel − Σpairwise-sim`): **KNEE at k=2.** `W(1)=0.39 <
  W(2)=0.58 > W(3)=0.38 >> W(30)=−31`. A **2-pattern cascade beats the length-1 baseline AND every larger
  one.** The too-MUCH side craters decisively (over-articulation punished — Joe's "any more, too
  complicated"). The too-LITTLE side is **shallow** (1 vs 2 only +0.18) because this broad mission's
  relevances are nearly flat (0.39→0.32).
- **Findings:** (1) **cascade-construction must be COHERENCE-GREEDY (submodular), not relevance-top-k** — the
  latter is degenerate (the recurring lesson: reward wholeness, not pointwise-greedy, at every scale). (2)
  the too-little side wants claude-3's **real structural Salingaros `C`** (mutual reinforcement, one scale
  up) to sharpen — and/or a more *focused* `|ψ⟩` (broad mission ⇒ flat relevances ⇒ shallow knee). The
  constructed cascades (sizes 1..k) are ready for claude-3's real-`C` re-score = **the decisive test of
  whether the knee deepens.** [→ handed to claude-3]

**Real-`C` re-score (claude-3) — REDIRECT, not greenlight (2026-06-09). The cheap verify killed a false
knee.** Under real structural Salingaros `C` (harmony `H` rewarding coherent mutual reinforcement),
claude-4's k=2 knee is a **proxy artifact** — it knees only because `W = Σrel − Σsim` *mis-counts coherence
as redundancy-cost*. **Real `C` is MONOTONE to the whole pool** (argmax=20, ratio 14×): there is **no
interior too-much knee** under any reward-faithful form — **coherent reinforcement does not self-limit.**
Consequence (this corrects Joe's "any more, too complicated" — it's real, but *not* in the wholeness score):
**size-control is EXTERNAL to `C`**, two separate bounds —
- **too-LITTLE = coverage-saturation of `|ψ⟩`** (stop when marginal coverage of the mission < ε = "the
  argument is now expressed"). **claude-4** adds this to the coherence-greedy construction.
- **too-MUCH = a parsimony / BUDGET prior** (a cost on #centres the consumer/argument can bear) — a
  **budget decision on the live-judge / operator side (claude-1)**, NOT an internal wholeness knee.
- Construction stays coherence-greedy (locked ✓); `C` (claude-3) ranks wholeness, it does **not** bound size.

**Don't wire a redundancy-penalty proxy as the judge** — it truncates good coherent cascades at ~2 for the
wrong reason. **Ownership:** claude-4 = coherence-greedy construction + coverage-saturation stop · claude-3 =
`C` (done, monotone) · claude-1 = budget prior + live-judge wiring. **Budget plan:** don't guess it — wire
coverage-saturation as the primary control, observe real cascade sizes on real missions, set the budget
*from data* (operator's call, informed).

**Callable ready + eyeballed (claude-4 `futon3a/holes/labs/M-memes-arrows/cascade_construct.py`; claude-1
ran it, 2026-06-09).** `construct_cascade(psi_query, ε=0.15) → {:cascade :size :C(=T×H) :H :T :trajectory}`,
coherence-greedy order + coverage-saturation stop, no budget. Three real cascades:
- **FOCUSED `|ψ=AIF/EFE policy selection⟩` size=17, C=5.16** — *self-referentially apt*: the constructed
  ARGUE contains `aif/hierarchical-budget-aware-action-selection`, `aif/shared-kernel-predictive-forward-
  model`, `aif/candidate-pattern-action-space`, `aif/niche-construction` — **the framework's own patterns**.
  The policy framework's ARGUE, built by the framework.
- **BROAD `|ψ=interim-director proxy-metrics⟩` size=21** (sprawling business/impact patterns).
- **TECHNICAL `|ψ=substrate ground-metric⟩` size=4, C=0.57** (tight, self-limits fast).
- Marginal-coverage decays `0.4→0.15`; the **top ~5–8 are the strong centres**, the tail is marginal —
  supports a budget ~5–8 (or ε~0.18). Size is data-driven, scales with circumstance breadth.
- **Integration:** the callable is **Python** (minilm + pattern embeddings); `generate-war-machine` is
  Clojure → wiring is a **subprocess/thin-service shell-out** (the existing `notions_search.py` path), not
  in-JVM. [claude-1 wires the visible cascade-policy lane.]

**BOTH AlphaZero halves now REAL — the gradient prior LANDED (claude-3, 2026-06-09).** codex-2 built the
producer (`futon6/scripts/diffsub_emit.py`, `29fe492`); claude-3 reviewed (author≠reviewer, PASS); **claude-1
independently spot-checked.** `futon6/data/diffsub-moves.edn` is `:emit/stub? false`, **shape identical to
the stub → claude-4's consumer does not move** (the contract-first payoff: real producer swaps in behind the
stub transparently). Conditioning sane (grad-norm max/med 1.37, tracks structure not size). **Anchor signal
real:** summits `full-arxiv-mining` 0.279 / `ai-passes-prelims` 0.271 ≫ `:conjectural` islands ~0.045
(reachable frontier caps get strong gradient; islands near-zero — the reachability axis, confirmed). v1 grain
= mission+capability (230 nodes); G4 = Salingaros-`C` (κ deferred).
- **Caveat (propagated to claude-4):** `:advance-capability` moves carry real `scope/capability/<id>` →
  join fine; `:close-hole` moves are v1 **mission-grain placeholders** (`scope/<stem>/detached#open`,
  scope-grain deferred) → won't match `:constructed`-arrow `:want`s until scope-grain v2 → they read as
  *unreachable* (safe — filtered, not broken). Capability moves are the real reachable signal for v1.
- **R2 ready:** claude-3 will wire the return channel (realized `G(π)` per `:move/id` → trains the prior)
  whenever the rollout's return channel is up — a **v2** step (the learnable prior), not the immediate path.

### Pre-go-live readiness ledger (claude-1 coordinator synthesis, 2026-06-09)

Joe's pre-go-live review: claude-3 **confident** (its producer is audited), claude-4 **cautious**
("half-finished substrate woven together"). **Both are right at different scopes.** The honest ledger —
*don't read "all three engines are real" as "the integrated thing is real."*

**REAL — go-live-ready:**
- **Track 1** (cursor fix) — LIVE.
- **The cascade lane** — the **v1 visible-non-degenerate-policy path** (real coherent pattern-semilattices,
  eyeballed: sizes 4/17/21, the self-referential AIF cascade). **This is v1's star.**
- The three engines (rollout / gradient prior / cascade constructor), each real + reviewed *in isolation*;
  the interface + the **transparent stub→real swap** (claude-4 verified per-move key-parity *identical*).

**REAL-BUT-v1-THIN — label honestly:**
- The **rollout-over-the-prior**. claude-4's precise count: **the v1 reachable branching set = exactly 3
  summits** (`full-arxiv-mining` 0.279, `ai-passes-prelims` 0.271, `wm-overnight-unsupervised` 0.184) —
  16/19 moves correctly mask-filter (8 close-holes are mission-grain placeholders + 7 conjectural-island
  footholds, all unreachable in v1). So the rollout's **multi-step value-add is ~0 in v1** (it ranks 3
  summits); the search **deepens at scope-grain v2**. Honest framing: *"the generalized selector with the
  single-step floor; multi-step search richens at v2"* — **not** "the WM now searches multi-step policies."

**DEFERRED — v2 (name it, don't fold in and claim):** scope-grain (richens the reachable graph past 3
summits) · R2 (the learnable-prior return loop) · pattern-grain `:graft-pattern` · the cascade-lane ↔
rollout reconciliation (today two distinct lanes/grains).

**Honest minimal go-live:** wire the **cascade lane** (the real v1 win) + the **rollout value-correction**
(labeled v1-thin = ranks 3 summits) + keep **Track 1** live. Label v1's edges; name the v2 deferrals. The
caution and the coordinator read *agree*, now with a hard number — so go live on the honest slice, not the
scaffold.

### v1 cascade lane — GO-LIVE (claude-1, 2026-06-09)

Merged the session's work to **main** (futon2 `a87424d`; futon3a `meme.step` already on main; cross-repo
`{:local/root}` dep satisfied). Built + shipped the **v1 visible cascade-policy lane** — additive, separate
from the judge ranking, **loaded in the live JVM and demonstrated over the live ranking**:
- `futon3a/holes/labs/M-memes-arrows/cascade_serve.py` (`5e52a02`) — CLI over `construct_cascade`, emits JSON.
- `futon2.report.cascade-lane` (`108141c`) — `cascade-lane` builds the budget-truncated cascade-policy for
  the top-n live missions; shells out to Python/minilm (the notions path), accepts the JSON-string `:type`,
  enriches `|ψ⟩` with the action rationale.
- **Live result** (over the live judgement): `M-capability-star-map` → a 6-pattern cascade-policy (C=9.88);
  `M-essay-corpus-substrate` → 6 patterns (C=9.98) — budget-6 of a ~32 coverage-saturated full cascade. **Real
  non-degenerate policies, visible over the live WM ranking.**
- **Honest v1 edges:** the rollout value-correction is *deferred* (3-summit-thin in v1 — adds ~nothing yet,
  touches the ranking — not wired). Budget=6 from the marginal-coverage data.

**Auto-serve DONE (claude-1, `8ab229e`).** `generate-war-machine` attaches `:cascade-policies` to the
judgement (top-3, budget-6) via `requiring-resolve` + a defensive `try` (a cascade failure can't break the
scan) + memoized constructor (no re-shell-out across windows/ticks). **Confirmed on the served judgement
(tick 80, clean):** the lane auto-appears, 3 entries. **Useful finding — the cascade `C`-score is a second,
orthogonal signal:** on-ascent missions carry *rich* coherent cascades (`M-capability-star-map` C=9.88 /
`M-essay-corpus-substrate` C=9.98, full-size 32), while the silly cursor's cascade is *thin* (C=1.145,
self-limits at 6) — so the wholeness of the argument *agrees with the ranking* and adds "how strong is the
case for advancing this," not just "advance this." The cursor's thin low-`C` cascade is another tell it's
weak. (The lane is **served/human-visible**; the *pilot's automated turn* doesn't *consume* it yet — a small
follow-up if we want the pilot to act on the cascade, not just display it.)

**v2 merged + smoke-tested (claude-1, 2026-06-09).** Merged `wm-rollout-v2-root-seed` → main (FF —
claude-4's scope-grain rollout + R1 prior-consumption + the docs). **Smoke test PASS** (the v2 e2e witness
`e-rollout-v2-e2e.clj`, against the real **k=55** move-set `diffsub-moves.edn`): zero drift (roots
21/3/7/0); **44/44 close-holes reachable** (vs v1's 3 summits — the rollout is *un-thinned*); 3/3 summits;
7/7 islands dark; and a real **depth-5 policy** — `hypergraph-operator: derive→argue→verify→document→
instantiate` (the mission lifecycle as a multi-step policy). **Remaining for full live roll-out:** point the
rollout default at the v2 move-set + wire the now-non-thin value-correction into `generate-war-machine`
(coordinate with claude-4's incoming CH1). **Part B — the WM *driving* a real action (`promote!` on a chosen
move, closing a real sorry) — HELD for explicit operator arming** ("smoke test now, arm later"). The honest
line stays: the search is now live-rich and proven; the *acting* waits on the gate.

### Track 2 — the rollout engine (DERIVE, 2026-06-09)

**G1 reconciliation with `M-differentiable-substrate` (claude-3) — the AlphaZero split** *(proposed, to
ratify with the gradient-route owner before either builds; resolves that mission's G1 + success-criterion-3).*
`M-differentiable-substrate` builds a **second route to `G(π)`**: a gradient over the materialised metric
(`grad(loss)(A)` = ranked edit-proposals = the differentiable geodesic — the `code_diff_jax_pilot` /
`jax_refine` port, already numerically healthy on code). Its G1 asks how that relates to my discrete
rollout. Resolution:
- **The gradient route is the POLICY PRIOR; the discrete rollout is the SEARCH** — exactly AlphaZero. The
  gradient pass is fast, global, *first-order*: it ranks candidate moves (which single edits descend the
  metric toward goal-anchors). The rollout is deliberate, *multi-step*: it takes the gradient's top-k
  proposed moves as its **branching set** and evaluates actual **paths** (`G(π)` over sequences), capturing
  the combinatorial structure a first-order gradient misses (a move bad alone but good as step-1 of a path).
- So: **gradient = prior over moves (policy net) · rollout = lookahead over paths (MCTS) · value = `G(π)` ·
  reward = peradam.** They compose as prior+search, not competitors — G1 options (b)+(c) (the gradient
  *seeds* the move-set / is the continuous *relaxation* the rollout discretises).
- **Cross-check (a) as a diagnostic:** where the gradient's first-order ranking disagrees with the
  rollout's path-integral ranking = signal (combining-methods-as-diagnostic) — a move the gradient likes
  but the rollout finds dead-ends, or a path the local gradient couldn't see.

**Engine design** (inputs all landed; refined by claude-4's ratification 2026-06-09):
- **Forward-model `step(state, leaf) → state'`** — **MUST-A (shared pure kernel, not a mirror):** refactor
  `promote!`'s transition into ONE pure `step` that *both* the live path and the sim call — single source,
  cannot drift (vs a mirror that can). **MUST-B (copy-state, no `:7071` writes in the rollout):** the sim
  takes the cap-overlay as an *input snapshot* and returns an updated *copy* — it must **never** mutate a
  real cap during simulation, else a K-step rollout launders frontier caps at scale. Only the **selected
  policy's first step** applies live (claude-4's dry-run discipline becomes the rollout's foundation).
- **Accumulator** — port ukrn `project-budget-path`'s K-step loop: `G(π) = Σ_t γ^t g(s_t)` over
  `π = (leaf₁ … leaf_K)`, discounted, with absorbing barriers.
- **Per-step `g(s)`** = epistemic(`C`/holes, claude-3) + pragmatic(Track-1-corrected graph-ascent, live).
- **Move-set** = the gradient prior's top-k (`M-differentiable-substrate`), or — until that lands —
  **(Add-C)** the canonical fallback: an open arrow is reachable iff its `have` is reached by some
  `:constructed` arrow (endpoint-graph reachability, computable from the store); the gradient top-k is the
  prior *over that reachable set*.
- **Barriers — (Add-D):** an `:advances-cap` at a *frontier* cap with no constructed path = "an endpoint
  with no terrain" = an absorbing barrier the rollout can't close — it needs a *minting charter*, not a leaf
  (wires the §4 three-kinds-of-off-map / island finding into the geodesic).
- **Selection** — argmin `G(π)` (the geodesic) / softmax+abstain (ukrn `select-action`; abstain on
  indiscriminable policies = WM-I4). The selected policies = the geodesics claude-3's render draws.
- **Witness — (Add-E):** a 2-step where step-1 *satisfies the `have`* of a high-ascent step-2 that
  greedy-one-step can't see — the cleanest "policy beats greedy," exercising the transition chain directly.

**Ownership (Joe, 2026-06-09):** claude-1 coordinates · **claude-4 owns the rollout** (`futon2.aif.rollout`,
its next E-excursion — codex handoff + claude-4 review, the seam pattern; MUST-A refactors claude-4's own
`promote!`) · **claude-3 owns the gradient route** (`M-differentiable-substrate` = the policy prior).

**Rollout CHARTERED (claude-4, 2026-06-09):** `E-policy-rollout-engine`
(`futon2/holes/E-policy-rollout-engine.md`) — scopes `futon2.aif.rollout` to the locked contract (MUST-A
shared pure kernel · MUST-B copy-state/zero-`:7071`-writes/only-selected-first-step-live · project-budget-path
accumulator · per-step epistemic(C)+pragmatic(status-aware ascent) · superset-snapshot-consumed-once + moving
reachable mask · argmin/softmax+abstain · Add-D frontier-no-path absorbing barrier · witness = ≥2-step beats
greedy). Build (codex handoff + claude-4 review) is **gated on claude-3's stub**
(`futon6/data/diffsub-moves-stub.edn`); claude-4 dispatches to codex when the stub lands and bells shas on
landing. MUST-A is intra-futon3a (claude-4's own `promote!` → shared pure `step`), no cross-repo
coordination. Charter ratifies the interface as-locked — no further changes from claude-4.

**R1/R2 accepted into the charter (claude-4):** search expands `:prior`-weighted (renormalized over
reachable survivors; top-k = truncation only; `:prior` = policy head, `G(π)` = backed-up value; root
selection stays argmin/softmax+abstain); `:move/id` threaded stably per-leaf for the v2 return channel
(v1 forward-only). `:have`/`:want` confirmed as **full scope-ids** (claude-4's reachability join). **Two
gates before codex dispatch:** (i) **`:centre-mess`** — **RESOLVED** (claude-3 ↔ claude-4, futon6
`b8564fb`, claude-3's gap G6): every move now carries **`:move/terminal?`** (true on `:centre-mess` — the
only non-atomic class, a compound cluster graph-rewrite whose mechanism is M-memes; false on the other 18),
meaning "no expansion through it." claude-4's impl choice — hard-skip (not in branching set) or terminal
leaf carrying its g-cost (claude-3 leans the latter, to preserve the diffuse-disorder signal in the
policy); both honor no-fabricated-`T` and keep the stub valid. v2 promotes `:centre-mess` to a real
single-step `T` once its state-delta is defined (e.g. "reduces the district's open-hole-entropy by X").
(ii) **operator go** — claude-4 flagged an autonomy/leash question to Joe and awaits his call before
dispatching the codex build. **This is now the *only* remaining gate.** On it clearing, claude-4 dispatches
+ reviews + bells the shas.

**Operator GO given (Joe, 2026-06-09) — both builds dispatched in parallel (codex pool codex-1..4 open).**
Two framing corrections from Joe:
- **The two routes are NOT a cross-check — they are the two *required* halves of the AlphaZero featureset**
  (gradient prior ‖ rollout search); both are needed, not redundant. Disagreement-as-diagnostic is a minor
  side-benefit, not the rationale for two routes.
- **Consumer confirmed — not architecture-ahead-of-need.** The overnight WM runs **now** with
  **E-warranted-play as the default policy** (a live baseline); `M-wm-policies` (the rollout) lets it
  *explore the benefits of different policies and improve within a run*. So the rollout is a measurable
  improvement on a live capability, with the witness (≥2-step beats greedy) as the proof it helps.

Parallelization: claude-4 grabs a codex for the rollout build; claude-3 grabs a codex for the grad-loop
producer (concurrent, not serialized behind the stub); claude-1 coordinates.
Ratified by claude-4 (split + interface + DERIVE, conditional on MUST-A/B — accepted) **and claude-3**
(gradient side; `M-differentiable-substrate` IDENTIFY→DERIVE, contract in its §3.1).

**Move-set interface — LOCKED (claude-3 ↔ claude-4 ↔ claude-1, 2026-06-09).**
- claude-3's gradient **emits a static snapshot artifact** (scored moves + `:emit/metric` snapshot,
  consumed once) — a ranked move-set over the **full candidate space** (NOT just currently-reachable).
  Canonical move shape (minimal core `{:have :want :score :prior}` sorted by `:score` desc; full move):
  ```clojure
  {:move/id "<have>-><want>"  :move/class :close-hole|:graft-pattern|:advance-capability|:centre-mess
   :have "scope/<id>"  :want "scope/<id>|scope/capability/<id>"  :advances-cap "<cap-id>"|nil
   :score <f>  :prior <softmax∈[0,1]>  :delta-g <first-order Δ>
   :confidence :claimed-substrate|:conjectural  :rank <int>}
  ```
  `:advances-cap` = the cap-overlay read-contract hook (claude-4's `promote!` routes on
  `:capability/frontier?`/`:status`, unchanged); `:delta-g` = claude-3's first-order number, so its `:rank`
  is comparable to the rollout's path-rank (the cross-check); `:confidence :conjectural` flags
  not-yet-reachable / island moves; claude-4's `{:leaf/edit}` maps to `:move/class` + `(have, want)`.
- claude-4's rollout **consumes it once** (zero mid-search dependency on claude-3 or `:7071`) and applies a
  **moving reachable mask** per node — intersect with the *currently*-reachable set, renormalize. The key
  insight (claude-3): the prior must cover the **superset**, because *constructing an arrow mid-rollout opens
  new reachable `:have`s*, so moves that are only legal deeper in the path must already be scored. **Prior =
  a function over the candidate space; the rollout supplies the moving reachable mask.**
- The return channel (training the gradient from rollout outcomes) is strictly **post-search**.
- **Stub unblock — LANDED + verified (futon6 `38eb583`, 2026-06-09):** `futon6/data/diffsub-moves-stub.edn`
  — 19 moves, valid EDN, locked shape confirmed by claude-1: 3 summits (`full-arxiv-mining`,
  `wm-overnight-unsupervised`, `ai-passes-prelims`) as `:claimed-substrate` with real cap-ids + real
  ascent-parents; 9 `kit-*`/`cold-*` islands as `:conjectural` via `scope/conjectural/<x>-foothold` tails;
  plus hole-close / graft-pattern / centre-mess moves (`:advances-cap nil`). claude-4 builds + tests its
  rollout consumer against this *real contract* immediately; claude-3 builds the grad-loop producer behind it
  (same output shape, so the consumer won't move).

**Two refinements that make it the *full* AlphaZero loop (claude-3, R1+R2):**
- **R1 — `:prior` is the PUCT branching weight, not just a top-k cut.** AlphaZero's policy head outputs
  `P(s,a)` over actions; MCTS uses it as the PUCT prior. So `:prior = softmax(:score)` over the set, and the
  rollout uses `:prior` as its **branching weight** (top-k is mere truncation). The rollout's search is
  prior-weighted, not uniform over the top-k.
- **R2 — reserve the return channel (the loop closes).** In AlphaZero the *search result trains the policy
  net*. Stable `:move/id` lets claude-4's rollout report **realized `G(π)` per move** back as the training
  target for claude-3's gradient loss → the prior becomes **learnable from rollout outcomes, not static**;
  **`reward = peradam` enters here.** **v1 is forward-only** (prior→search); just keep `:move/id` stable so
  v2 can close the loop (prior→search→train-prior). This is the piece that makes it the full loop, not a
  one-way pipeline.

**Build scope** (codex handoff *after* the G1 reconciliation is ratified): a futon2 ns `futon2.aif.rollout`
— `step` (pure forward-model) · `rollout`/`G-of-policy` (accumulator) · `select-policy`. Test = a ≥2-step
rollout beats the greedy one-step on a constructed case (success-criterion-4). Gated on: reconciliation
ratified + the move-set source pinned.

### Track 3 — "not being stuck" is a niche-construction action (Joe, 2026-06-24; the VWM made live)

*The VWM excursion ([[E-vwm]]) showed, on real/current data, that **68% of missions (132/194) sit in a
flat-cosine stall** (mean top match 0.386) and that for **91%** a cluster **cascade** (the "downward growth"
of what missions-like-this applied) offers a warranted move. Joe's framing: build that into the **real** WM,
and justify it on AIF terms — "anticipation and reducing surprisal are big parts of AIF, but so is **not being
stuck**." This is Track 3.*

**The stall, located in the live code.** Two selectors freeze on a flat field:
- `futon3c.portfolio.policy/choose-action` — the autonomous loop's tick selector. `abstain? = (< τ 0.55)` →
  action **`:wait`** → `{:effect :none}`. The arena is `[:work-on :review :consolidate :upvote :wait]`.
- the WM mission-EFE / cascade lane — flat-low candidate cosines (the VWM's 68%).

**The AIF diagnosis (the heart of Track 3): the arena has no epistemic / model-expanding action.** EFE
minimisation is *over policies*; it presupposes a policy set rich enough to contain a low-`G` option. A flat
field means the *current* set has no good move — and the five arena actions are all **pragmatic** (act on
existing structure). So when pragmatic value is flat (τ low), nothing wins → abstain → `:wait` → `:none`.
**Freezing is AIF-wrong**: you cannot lower `G` by not acting. The FEP-correct response is **structure
learning / niche construction** — expand the generative model's policy space so a low-`G` policy *comes into
being* (Friston/Levin/Pezzulo "Knowing one's place"; morphogenesis raises complexity to lower EFE — the
session's soap-bubble-vs-daisy thread: don't *flatten*, *organise*). The daisy proved this is learnable
(evolution succeeds where REINFORCE collapsed — [[project_aif_daisyworld]]). This is **experiment-menu item 7**
("policies that create policies = aif2 niche-construction") promoted from menu to live behaviour, and the
**M-aif2 `extensible-registry`** primitive ([[project_aif2]]) instantiated at the WM stratum.

**Two beliefs the WM must distinguish (Joe).**
- **"I don't have the right patterns *yet*"** — an epistemic gap the agent can close *itself* (find/build a
  cascade for this class). Actionable autonomously. → the niche-construction move.
- **"I need the operator"** — reserved for *semantic / consent* decisions (WM-I4: don't act *unilaterally*; the
  consent-gate is the locus). A pattern-gap is **not** this. Today's bug: `abstain → :wait` collapses both into
  one degenerate freeze, so a closable gap masquerades as needing Joe.

**Two triggers (Joe).**
- **Reactive (on stall).** When `choose-action` would abstain, instead of `:wait` emit **`:acquire-patterns`**:
  run the VWM read on the current `|ψ⟩`, construct a candidate cascade (the existing coherence-greedy
  `cascade_construct.py` / `report.cascade-lane`), and either *propose* it (if its `C` clears) or record "no
  good-enough patterns for this class — foothold needed" (the island/gap case, §4 three-kinds-of-off-map).
- **Proactive (on completion / idle — defensive driving).** When a task wraps and the field is quiet, run the
  VWM **gap-map** over open missions, find the pattern-gap clusters (v1: cluster 4 math-reasoning/ai4ci is the
  robust gap; the mature core is well-covered), and **build good-enough cascades for those classes before
  they're hit** — the relaxation of "seed the perfect pattern" (slice-1) to "seed good-enough-for-the-class."

**Why it falls out of EFE, not bolted on.** `:acquire-patterns` carries high *epistemic* value (it reduces
uncertainty over which policies are available — pure info gain) and ~zero pragmatic value. So in EFE
`G = effort − (λ_p·prag + λ_e·epis + …)` it is dominated whenever a pragmatic action is available, and **wins
exactly when the pragmatic field is flat** — i.e. the abstain regime. Adding it to the arena turns "get unstuck
by learning" into the EFE-optimal move precisely when stuck. (The abstain threshold stays as the *guard* — but
it now routes to a *constructive* action, not `:none`.)

**Build plan (wiring, not new infra — the machinery exists).**
1. `:acquire-patterns` added to `portfolio-arena` + an `expected-free-energy` epistemic term for it (high when
   the rest of the field is flat) — `portfolio/policy.clj`.
2. Effect handler `:acquire-patterns → :portfolio-acquire` — `portfolio/effect.clj`: shells the existing
   cascade constructor over the stalled `|ψ⟩` (the `cascade-lane` Python path), returns the proposed cascade or
   the foothold-needed record. (MUST mirror cascade-lane's *propose-don't-promote*: zero `:7071` writes; an
   actually-applied cascade still goes through the consent gate — WM-I4 intact.)
3. The abstain branch in `choose-action` splits: flat-but-closable → `:acquire-patterns`; genuinely-nothing /
   operator-semantic → `:wait`.
4. Proactive hook: on cycle-completion, `generate-war-machine` (already attaches `:cascade-policies`) also runs
   the VWM gap-map and attaches `:pattern-gaps` (the defensive-driving lane) — served/visible first, acted-on
   behind the gate.
5. Tests: `policy_test` — when the field is flat, `:acquire-patterns` is selected over `:wait`; when a pragmatic
   action is available, it is **not** (the epistemic term must not hijack normal operation).

**BUILD STATUS — reactive path LANDED + live-verified (claude-2, 2026-06-24; built directly, Codex paused).**
Steps 1–3 (the reactive niche-construction path) are implemented, tested, gated, reloaded into the live JVM, and
demonstrated end-to-end:
- `futon3c/portfolio/policy.clj`: `:acquire-patterns` added to `portfolio-arena`; pragmatic term (small, stall/gap
  warranted), epistemic term (high in the abstain regime — **τ-gated**: `0.9·flatness·low-confidence`, exactly 0
  when τ≥0.55 so it cannot hijack normal operation), effort 0.3. The abstain branch no longer hardcodes `:wait` —
  it selects argmin-`G` over `{:acquire-patterns :wait}` (the two-beliefs split falls out of EFE).
- `futon3c/portfolio/effect.clj`: `:acquire-patterns → :acquire-patterns` effect; `execute-effect!` calls
  `futon2.report.cascade-lane/cascade-policy-for` (read-only / sim-only / never promotes — WM-I4 intact) via
  `requiring-resolve`, defensively (a constructor failure surfaces in `:note`, never breaks the loop, never
  silently swallowed). Returns `:proposed-cascade` or a foothold-needed note.
- **What I checked (auditable):** clj-kondo **0 errors** (3 warnings, all pre-existing — `observation`/`mission-state`/`work-efe`,
  not introduced); `check-parens` OK on all 3 files; `policy-test` **13 tests / 39 assertions / 0 failures**
  (new: stuck-flat→`:acquire-patterns`, calm→`:wait`, confident→no-hijack); `core-test` **8 / 36 / 0** (no
  regression); effect.clj loads clean. Reloaded both ns via Drawbridge (no JVM restart — I-0); live arena now
  `[:work-on :review :consolidate :upvote :acquire-patterns :wait]`. Live demo in the running JVM: stuck+flat →
  abstain + `:acquire-patterns`; calm → abstain + `:wait`; confident → `:review` (no hijack). End-to-end: stalled
  `M-substrate-metric` → proposed cascade `math-formalization/metric-cauchy-convergence` (C=0.311), zero `:7071`
  writes. **The WM no longer freezes-and-escalates on a flat field; it constructs a candidate policy.**
- **Still open this mission:** step 4 (proactive completion/idle gap-map hook → `:pattern-gaps` lane in
  `generate-war-machine`); Part B (the WM autonomously *applying* an acquired cascade) — HELD for operator arming.

**BUILD STATUS — proactive horizon gap-map LANDED + live-verified (claude-2, 2026-06-24).** Step 4 (the
defensive-driving lane):
- `futon2/report/cascade-lane`: new `gap-lane` (reuses `cascade-policy-for` + its memo, read-only/sim-only) —
  for the top-n open missions, flags those whose coverage-saturated cascade is THIN as pattern-gaps.
- `futon2/report/war-machine`: `:pattern-gaps` attached to the judgement beside `:cascade-policies` (same
  defensive `try`/`requiring-resolve` pattern; a failure never breaks the scan).
- **Live-verified end-to-end:** the served judgement (`/api/alpha/war-machine`) carries `:pattern-gaps` over the
  real rationale-enriched `ranked-actions`. Both current open missions (`M-canon-fingerprint-store`,
  `M-bayesian-structure-learning`) flag as gaps — and they are *futon6 math/Bayesian* missions, i.e. exactly the
  VWM's independent gap cluster (cluster 4, math-reasoning), corroborating the signal.
- **Finding that forced a fix (auditable, honesty-first):** I first thresholded on absolute wholesomeness `C`
  (< 2.0, taken from this mission's 2026-06-09 go-live numbers C≈9.9). A known-rich-mission control showed the
  **absolute C-scale has DRIFTED ~4–5× down** since then (`M-capability-star-map` 9.88→**2.20**; essay-corpus
  9.98→**0.48**), so a stale-C threshold mislabels rich missions as gaps — I had hand-guessed off stale data. Fix:
  gap criterion is now **coverage-saturated SIZE** (≤2 = thin), which is **scale-independent** (how many patterns
  the constructor found relevant enough to add). Re-verified discrimination: capability-star-map size 7 / wm-pilot
  size 5 → not-gap; canon-fingerprint size 1 → gap. clj-kondo 0/0, check-parens OK, reloaded live (next scheduled
  tick serves the size-based lane). **Residual caveat:** the verdict is sensitive to ψ text for *borderline*
  missions (size 2–3); clear cases (1 vs 5–7) are robust. The threshold should still be set from the full current
  size distribution across all open missions, not the 2 currently ranked (a small follow-up).

**Ownership / handoff.** Design + AIF justification + spec (this section) = Claude owner (done). The `.clj`
build (steps 1–5) is **substantial → bell to an idle Codex with the gate bar** (clj-kondo, `check-parens`,
`policy_test`), then author≠reviewer review by the Claude owner — *unless* Joe opts for a live-JVM interactive
build (the JVM is a standing asset; Drawbridge reload, no restart — I-0). **Part B — the WM autonomously
*applying* an acquired cascade — stays HELD for explicit operator arming** (same line as Track 2's `promote!`:
"build + display now, arm later"). The reactive/proactive *surfacing* needs no arming; only the *acting* does.

### How G(π) is computed in the new regime — the Car-3 gate (claude-2, 2026-06-24)

*Joe before arming Part B: "I'm not even sure how G(π) is computed; before we go live I'd like to see how
that works." This is the honest account. The key: there is **no single G(π)** — there are **three scorers at
three grains**, and the design's coherence is the claim that they are the **same quantity (expected free energy /
wholeness) recursively applied** ("move" and "policy" are the same kind of thing at different grains, §4).*

**Grain 1 — `G(a)`, the per-tick action selector (LIVE; what Track 3 just changed).** In
`futon3c/portfolio/policy.clj` `expected-free-energy`: for a single arena action `a`,
`G(a) = λ_eff·effort(a) − λ_prag·pragmatic(a) − λ_epis·epistemic(a) − λ_upv·upvote(a)` (lower = better; pick by
argmin / softmax+abstain). This is a **length-1 policy** EFE — the degenerate case the whole mission is about
(§1: "the field view is exact *only* for length-1 policies"). `:acquire-patterns` is just a new action here; its
epistemic term (expected info gain = reduces uncertainty over *which policies exist*) is the standard EXPLORE
pole, τ-gated so it only dominates in the abstain regime. **Answers: "what do I do THIS tick?"**

**Grain 2 — `C`, the cascade wholeness (LIVE; what `:acquire-patterns` produces).** The cascade a stuck WM
builds is a **semilattice of patterns** (Alexander), and it is scored **not by a path-sum but by Salingaros
`C = T·(10−H)`** — expressiveness `T` vs parsimony `H` (§4: "C IS the cascade-scoring, one scale up"). Computed
by the coherence-greedy Python constructor (`cascade-policy-for`). **Answers: "how whole is THIS policy (this
argument) for the circumstance?"** This is the score `gap-lane` reads to flag thin coverage.

**Grain 3 — `G(π)`, the rollout path-integral (LIVE for SEARCH, ACTING held; Track 2).** For a policy that is a
*sequence* of moves `π = (leaf₁ … leaf_K)` (e.g. *unblock-then-do*), `G(π) = Σ_t γ^t g(s_t)` accumulated over a
rollout through the shared forward-model (`futon2.aif.rollout`), with per-step `g(s) = epistemic(C/holes) +
pragmatic(status-aware graph-ascent)`. This is the genuine **path integral** (§1: a functional over the
trajectory, NOT a field). v2 is merged + smoke-tested (a real depth-5 policy witnessed); MUST-B holds (zero
`:7071` writes during search). **Answers: "does applying these moves *in sequence* descend free energy more than
greedy?"**

**The relationship (why it's one principle, not three hacks).** Grain 1 is a length-1 policy; Grain 2 is a
*set*-policy (a pattern semilattice scored by wholeness); Grain 3 is a *sequence*-policy (a path scored by least
action). `:acquire-patterns` is the **bridge**: it is *selected* at Grain 1 by `G(a)`, and what it *does* is
*construct* a Grain-2 object (a cascade) scored by `C`. Applying that cascade's patterns over time would be
*evaluated* at Grain 3 by `G(π)`.

**The Car-3 gate (the honest open question).** Before the WM autonomously *acts* on an acquired cascade, we must
say **which scorer governs the ACT**: the cascade's wholeness `C` ("is this a good argument?") or the rollout
`G(π)` ("does applying these patterns in sequence actually descend free energy?"). These are **today two distinct
lanes at two grains** — the mission already flags "the cascade-lane ↔ rollout reconciliation" as a v2 deferral
(§"DEFERRED — v2"). **Recommendation: Car 3 does NOT go live until that reconciliation is specified** — i.e.
until the act-gate is defined as a concrete function of `C` and/or `G(π)` (and the consent gate sits on top of
it, WM-I4). Joe's instinct is exactly right: the *acting* is gated on settling this; the *proposing* (Cars 1–2)
is not, which is why it could land now. The clean next step for Car 3 is an excursion to **reconcile `C` and
`G(π)` into a single act-gate** (do they agree on real cascades? where they disagree = the diagnostic signal,
combining-methods-as-diagnostic).

### Car-3 act-gate, settled by SIMULATION not arming (claude-2 + Joe, 2026-06-24)

*Joe: "this is where the VWM should come in handy — we could simulate the process of a few runs without
actually doing it." So instead of arming Part B and watching, the VWM computes BOTH candidate act-gates for a
few stalled missions without acting (`futon3c/holes/labs/vwm/vwm_sim_runs.clj`; both scorers read-only/sim-only):
grain-2 cascade `C`/size (`cascade-policy-for`) and grain-3 rollout `G(π)`/depth (`futon2.aif.rollout` over the
v2 k=55 move-set, MUST-B zero `:7071` writes). Six simulated runs (enriched live-shape ψ):*

| mission | grain-2 (C/size) | grain-3 (G(π)/depth) | both |
|---|---|---|---|
| substrate-metric | 1.55 / 6 | −0.0015 / 2 | act ✓ |
| hypergraph-operator | 1.01 / 3 | −0.0043 / 5 | act ✓ |
| war-machine | 2.05 / 6 | −0.0008 / 1 | act ✓ |
| canon-fingerprint-store | 0.36 / 1 | no path | don't ✓ |
| capability-star-map | 0.44 / 1 | no path | don't ✓ |
| bayesian-structure-learning | 1.19 / 4 | no path | disagree ✗ |

**Findings (the reconciliation, empirical):**
1. **Where both grains have coverage they AGREE** (the three act rows) — `C` and `G(π)` are **not deeply
   contradictory**. This is the key result: the gate is buildable.
2. **The disagreements are COVERAGE / STABILITY artifacts, not a real C-vs-G(π) tension:**
   - grain-3 reads "no path" for `canon`/`bayesian`/`capability-star-map` only because the v2 move-set doesn't
     *contain* those missions — a move-set **coverage** gap, not true unreachability.
   - grain-2 is **ψ-text-fragile**: `capability-star-map` flipped size 7→1 on a minor ψ wording change; plus the
     library-scale drift (C 9.9→~2) found in Car 2. So cascade `C`/size is an **unstable** gate alone.
   - grain-3's `G(π)` magnitudes are tiny (~1e-3) — a weak preference signal.
3. **Act-gate the simulation argues for:** **act only where both grains have coverage AND concur on
   "actionable"** (conjunction) — robust to each scorer's blind spot. On disagreement, **do not act**: treat it
   as a coverage/stability flag (fix coverage, or route to operator). Combining-methods-as-diagnostic *is* the
   gate. WM-I4: the consent gate still sits on top.
4. **The real blockers to arming Car 3 are now concrete and un-mysterious** — close grain-2's ψ-stability (+
   library coverage) and grain-3's move-set coverage (+ signal magnitude) — **not** "reconcile an irreconcilable
   tension." Learned with **zero real runs** — exactly the VWM's purpose.

**AIF grounding audit (deep-research-AIF-morphogenesis.md §"Grounding audit", 2026-06-24).** Before closing those
blockers, the act-gate was checked against the real literature. Verdict: the **conjunctive gate is AIF-correct in
form** — it maps to structure-learning's "accept an expanded model only if ΔF (marginal likelihood) AND ΔG
(expected free energy) both improve" — and **grain-3 `G(π)` is the real ΔG (genuine EFE)**. But **grain-2 `C`
(Salingaros wholeness) is the ANALOGICAL leg**: the report's headline finding is that L=T·H is *not* formally
linked to free energy anywhere in the literature. So the principled Car-3 re-scope is **`ΔF ∧ ΔG`** with grain-3
kept as ΔG and grain-2 either labelled an explicit heuristic or upgraded to a real **ΔF = marginal-likelihood
gain** of the acquired cascade; and the `:acquire-patterns` epistemic term upgraded from a flatness-proxy toward
real **expected information gain**. These are *model* decisions, not technical fixes — hence the pause before
closing blockers is correct.

**Omission 1 GROUNDED + LIVE (claude-2, 2026-06-24).** The `:acquire-patterns` epistemic term is now the real
**expected information gain** = normalized Shannon entropy of the action-posterior `P(a) ∝ exp(−G/τ)` over the
existing (pragmatic) actions (`futon3c/portfolio/policy.clj`: new `posterior-entropy`; `choose-action` computes
it over the non-acquire evals and injects it via `expected-free-energy`'s new `:epistemic-override`; the old
flatness-from-precision proxy is demoted to a labelled fallback). τ-gated (×low-confidence) so it is exactly 0
when confident (no hijack). Verified: entropy = 1.0 on a flat field, ~0 when peaked; behavior preserved
(stuck-flat→`:acquire-patterns`, calm→`:wait`, confident→`:review`). Checked: clj-kondo 0 errors (1 pre-existing
warning), check-parens OK, policy-test + core-test **21/75/0**, reloaded live. *This is the Millidge-correct
form: the epistemic drive is added (not derived from projecting VFE forward) and its magnitude is a genuine
information-theoretic quantity (entropy of the belief over which action helps), not a heuristic.*

**Omission 2 GROUNDED + LIVE (claude-2, 2026-06-24) — grain-2 C → real marginal-likelihood F.** The cascade
act-gate leg is now **F = accuracy − λ·complexity** (Bayesian Occam; F>0 = accept the expansion), replacing the
Salingaros-`C` analogy. This *is* the report's open-Q3: **accuracy** = total ψ-coverage (`T`); **complexity** =
Σ −log(co-application **base-rate inclusion prior**) of the selected patterns (the `(10−H)` "architectural
entropy" → KL-of-selection-from-prior map). Implemented in `cascade_construct.py` (`base_rate_prior` +
accuracy/complexity/F) and surfaced through `cascade_serve.py`; `gap-lane` now gates on **F ≤ 0** (the AIF accept
criterion), not size/C.
- **Bug caught by the data sweep (auditable, honesty-first):** the first formulation used a *categorical* prior
  (mass/total over ~300 patterns) → every −log P ≈ log N bits → complexity swamped accuracy → F collapsed to
  size-1 and *penalized rich cascades* (the degenerate "no-knee" failure the §4 cheap-verify warned of). Fix: a
  **Bernoulli inclusion prior** `(mass+f)/(mass+f+K)`, K = median co-app mass → complexity ~O(1), commensurate
  with coverage → F gets a real interior knee.
- **λ set from data, not hand-guessed:** on the spectrum sweep the size-1 thin mission crosses F=0 at λ≈0.216 and
  a rich size-7 at λ≈0.287, so **λ=0.25** puts the F=0 boundary exactly between thin and rich (re-confirm on the
  full live distribution — same discipline as the size threshold).
- **Live-verified discrimination:** canon-fingerprint F=−0.02 → gap; capability-star-map 0.28, bayesian 0.41,
  hypergraph 1.68 → not gaps. clj-kondo 0/0, py_compile OK, check-parens OK, reloaded live (cache cleared).
- **Both omissions now closed:** grain-2 is a real ΔF and the epistemic term is real EIG — so the Car-3 gate
  `ΔF ∧ ΔG` is now genuinely AIF on *both* legs (grain-3 G(π) was already real EFE). The coverage blockers can
  now be closed as real-model fixes, not proxy-hardening.
- **Formal residual chartered separately:** O2 grounds `(10−H)→complexity` *operationally*; *proving*
  `L=T·H = accuracy×(−complexity)` as a theorem (or showing they're distinct) is
  `futon3c/holes/excursions/E-prove-salingaros-cascade-scorer.md` (the live data already hints `F` and `C=T·H`
  don't co-rank — a clean positive identity is unlikely).

## 5. DOCUMENT — completion check, AIF-theory touch-map, close recommendation (claude-2, 2026-06-24)

**→ Real documented run of the live WM (every new feature firing + the difference it makes):
`M-wm-policies-documented-run-2026-06-24.md`** — Track-1 off-map demotion (cursor #1→55/63), Track-3
niche-construction on a flat field (`:wait`/freeze → `:acquire-patterns`/propose+diagnose, EIG=0.429 wins),
the proactive `:pattern-gaps` lane, the cascade `F-free-energy` act-gate, and the R13 multi-step witness
(greedy −0.20 vs best −1.01), all on live/real data.

**Completion criteria (§1.4) — all four MET, live-verified 2026-06-24:**
1. Live WM no longer ranks off-map junk above on-ascent work — ✓ (served judgement: cursor at rank **55/63**,
   top = `learn-action-class` / `address-sorry` / `advance-mission`; Track-1 off-map-penalty + leaf-aware +
   status-aware live).
2. Off-map-penalty swept (regulator) + applied with consent — ✓ (P=4; `live-star-map-efe-weights`).
3. Forward-model contract exists — ✓ (state/transition/per-step/rollout resolved; claude-3 field + claude-4 arrows joined).
4. Policy rollout (≥2 steps) beats greedy — ✓ (witness `G(π)=−1.0` vs greedy `−0.2`; v2 depth-5 mission-lifecycle policy).

**Track-3 deepening (Joe added it this session) — landed + committed:** reactive niche-construction lane
(`:acquire-patterns`, real EIG) + proactive defensive-driving gap-map both LIVE; the C↔G(π) act-gate settled by
VWM simulation; both AIF-grounding omissions closed (real EIG; real ΔF). Shas: futon3c `ac93ed4`, futon2
`af26ef6`, futon3a `5708281` (+ daisy/VWM `4bec7f9`/`da26ee1`).

**AIF-theory touch-map — which parts of the theory M-wm-policies actually exercised** (the purpose of the
R-audit; see `futon2/docs/futon-aif-completeness.md` §"Are R1–R13 enough?"):

| R-criterion | How M-wm-policies touched it |
|---|---|
| **R5** (EFE terms) | Track 1 corrected the pragmatic graph-EFE term (off-map penalty + leaf-aware + status-aware); Track 3 added the real **info-gain epistemic** term (EIG = action-posterior entropy) |
| **R6** (action selection + abstain) | Track 3 turned abstain from *freeze* (`:wait`) into *niche-construction* (`:acquire-patterns`); WM-I4 abstain on flat fields |
| **R13** (policy adequacy — multi-step `G(π)`) | **M-wm-policies CREATED R13** — Track 2 built the rollout engine + the 2-step-beats-greedy witness; the field-vs-path-integral correction (§1) is its theoretical core |
| **R16** (closed action–perception loop) | **surfaced it** — defined the act-gate (ΔF∧ΔG), settled it by VWM simulation; *acting* HELD for arming = the R16 consent-gate first-class step (the loop's remaining closure) |
| **R17** (structure learning / niche construction) | **implemented it** — `:acquire-patterns` = model expansion on a flat field; the gap-map = which classes lack patterns; lifted `:learn-action-class` from addendum to live instance |
| **R18** (faithfulness) | **forced it into the contract** — the grounding audit (real EIG, real ΔF; `L=T·H` flagged analogical → `E-prove-salingaros-cascade-scorer` chartered) |
| R4 (forward model) | used by Track 2's rollout (the `step` kernel). R7 / R14 / R15 — not touched. |

**The finding (Joe's purpose).** This one mission drove the AIF contract **from R12 → R18** — it *created* R13
and *surfaced* R16/R17/R18. So M-wm-policies is not a narrow tuning mission: it exercised, and in three cases
*originated*, the **policy / structure-learning / faithfulness frontier** of the theory. That is the substantive
answer to "which parts of AIF does it touch."

**Gated next-phases (NOT close-blockers — each has its own home):**
- **Car-3 acting (Part B)** → operator arming; the R16 loop-closure (`ΔF∧ΔG` act-gate + consent gate / WM-I4).
- **`E-prove-salingaros-cascade-scorer`** (R18 formal proof of `L=T·H ↔ F`) → its own excursion.
- **Route-(a) island foothold** → operator go.
- Rollout value-correction full live-ranking integration → follow-up (the cascade lane is today's visible non-degenerate policy).

**Close recommendation:** criteria met + live-verified; Track-3 deepening landed + committed; the theory-touch-map
shows R5/R6/R13/R16/R17/R18 exercised (three *originated*); the remaining items are gated next-phases with their
own homes, not blockers. **Recommend CLOSE.** Per discipline, mission-close is the operator's call — Joe. **→ CLOSED by Joe, 2026-06-24.**

### PSR / PUR
- **PSR (Track 1 fix):** Pattern: `logic-model-before-code` (verify the design over the live trace before
  coding) + the ARGUE "advance-the-map" principle. Alternatives considered: thread `:capability-graph`
  into opts (refuted — already there); raise ascent-weight only (insufficient — body still swamps).
  Rationale: the off-map escape + body-granularity are the two terms actually inverting the ranking,
  shown on live numbers. Confidence: high (demonstrated sandboxed).
- **PUR:** _pending implementation + sweep._
