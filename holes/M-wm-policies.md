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
3. **Sweep, don't guess.** Run [[E-possible-world-regulator]] over the *whole field* (not just this
   snapshot) to pin a robust `:graph-off-map-penalty` (and body-mode); P≈4 is the snapshot estimate —
   verify it generalizes. (NB: hand-guessing weights was refuted earlier this session; the regulator
   exists for exactly this.)
4. **Apply live with consent.** Set the pinned values in `live-star-map-efe-opts`
   (futon3c `report/war_machine.clj`). The live EFE is the operator's consent locus.
5. **Tests.** `efe_test` for the off-map penalty (off-map action gets the penalty; on-map unchanged) and
   the leaf-aware body (a big on-ascent mission no longer outranked by a small off-map one).

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

---

## 4. ARGUE / VERIFY / INSTANTIATE — _pending_

Track 1 INSTANTIATE = the regulator sweep + live apply (consent). Track 2 = the forward-model contract,
then a ≥2-step rollout witness.

### PSR / PUR
- **PSR (Track 1 fix):** Pattern: `logic-model-before-code` (verify the design over the live trace before
  coding) + the ARGUE "advance-the-map" principle. Alternatives considered: thread `:capability-graph`
  into opts (refuted — already there); raise ascent-weight only (insufficient — body still swamps).
  Rationale: the off-map escape + body-granularity are the two terms actually inverting the ranking,
  shown on live numbers. Confidence: high (demonstrated sandboxed).
- **PUR:** _pending implementation + sweep._
