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

### Open ARGUE finding — two kinds of off-map (claude-3, 2026-06-09)

The 10 unclaimed pre-registered frontier capabilities (the cold-outreach + KIT commercial cluster + the
big aspirationals `ai-passes-prelims`, `wm-overnight-unsupervised`) are **off-map from the goal side**: no
mission mints them, so no candidate action advances them and the ascent-gradient toward them is flat.
This is the Track-1 off-map bug seen from the *destination* end — **the WM cannot be drawn toward a goal
no mission mints.** Consequence: the off-map treatment must distinguish **two kinds of off-map, needing
opposite handling:**
- **off-map WORK** (a candidate mission not on the capability graph) → **penalise** (Track 1's off-map
  penalty — it doesn't advance a registered destination);
- **off-map GOAL** (a pre-registered destination capability with no minting mission) → **do NOT penalise;
  surface as "needs a mission"** — a missing-terrain signal / a call to charter a minting mission.

(Path-integral connection — *ratified, see the §1 path-functional note*: an unminted goal is an *endpoint
with no terrain* — there can be no geodesic to an unreachable destination; the path must first be built by
chartering the mission.)

### VERIFY / INSTANTIATE — _pending_

Track 1 INSTANTIATE = the regulator sweep + live apply (consent). Track 2 = the forward-model contract,
then a ≥2-step rollout witness.

### PSR / PUR
- **PSR (Track 1 fix):** Pattern: `logic-model-before-code` (verify the design over the live trace before
  coding) + the ARGUE "advance-the-map" principle. Alternatives considered: thread `:capability-graph`
  into opts (refuted — already there); raise ascent-weight only (insufficient — body still swamps).
  Rationale: the off-map escape + body-granularity are the two terms actually inverting the ranking,
  shown on live numbers. Confidence: high (demonstrated sandboxed).
- **PUR:** _pending implementation + sweep._
