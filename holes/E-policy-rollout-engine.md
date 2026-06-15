# Excursion: Policy-rollout engine (`futon2.aif.rollout`) — the SEARCH half of the AlphaZero split

**Type:** Excursion (E-prefix; bounded build, owned end-to-end by claude-4 per [[project_e_prefix_excursions]]).
**Status:** ✅ **LANDED + REVIEWED PASS 2026-06-09.** codex-2 built (futon2 `65f137d` "Add policy
rollout engine" — `futon2.aif.rollout` + test + witness; futon3a `2122daf` "Extract pure meme
transition step" — `meme.step`, the MUST-A kernel). claude-4 review (merge gated):
- **MUST-A ✓** single-source — `promote! → cap-ascent/plan → meme.step/step`; the sim calls the same
  `meme.step/step`. (Traced the call chain; not a mirror.)
- **MUST-B ✓** rollout `write-count=0`; sim path is pure `meme.step`, no `:7071` during search.
- **Witness ✓** 2-step `G=-1.0` beats greedy `G=-0.2`; `:move/terminal?` truncated; consumes the real
  19-move stub; PUCT-prior + moving reachable mask + threaded `:move/id` (R1/R2).
- **No regression** — seam `e-advances-cap-ascent` (5/5) + keystone `h3` (0-conf/5-of-5) still green
  after the `meme.step` extraction; clj-kondo 0/0.
Return channel (realized `G(π)`-per-`:move/id` → claude-3's gradient training) reserved, v1 forward-only.
**Date:** 2026-06-09 · **Owner:** claude-4 (charter + review). Build = codex handoff + claude-4 review (seam pattern).
**Canonical spec:** `futon2/holes/M-wm-policies.md` §3 "Track 2 — the policy layer" (commit `33003e9`) — the
locked contract incl. MUST-A/B + Add-C/D/E. This charter scopes the build; it does not re-derive.
**Cross-refs:** M-wm-policies (Track-2) · `E-wm-policy-arrow-seam` (futon3a — the `promote!` step this shares) ·
claude-3's M-differentiable-substrate (the GRADIENT/prior half) · M-futonzero-capability §22 (AlphaZero map) ·
ukrn `/home/joe/code/ukrn-services-simulation/notebooks/ukrn_v3_efe.clj` `:274-315` (`project-budget-path`,
the `G(π)` accumulator) + `:599` (`select-action`, softmax+abstain). Tests:
`ukrn-services-simulation/test/notebooks/ukrn_v3_efe_test.clj`.

## Warrant (the AlphaZero split)

Per Joe's division of labour (via claude-1): **my rollout = the SEARCH/value; claude-3's gradient = the
PRIOR/policy.** I'm the natural rollout owner by construction — I built the transition layer: the arrow store
*is* the transition model, `meme.identity/promote!` *is* the step, and the `:advances-cap` ascent (E-wm-policy-arrow-seam)
*is* the pragmatic per-step term. EFE is `G(π)` over **policies** (a path integral / geodesic), not a field
over states; today's WM is the degenerate length-1 case (the cursor bug = the field worldview drawn greedily).
This excursion builds the length-K rollout.

## Deliverables (`futon2.aif.rollout`)

1. **Forward-model `step(state, leaf) → state'`**
   - **MUST-A (shared pure kernel, not a mirror):** extract `promote!`'s transition into a **pure** `step`
     that BOTH live `promote!` and the sim call — single source, cannot drift. (Refactors my own futon3a
     `meme.identity/promote!`; the live effectful apply wraps the pure `step`.)
   - **MUST-B (copy-state, no `:7071` writes in the rollout):** the sim takes the cap-overlay as an **input
     snapshot** and returns an updated **copy** — it must **never** mutate a real cap during search. Only the
     *selected* policy's first step applies live. (The dry-run discipline from the seam, now load-bearing —
     a K-step sim that wrote live would launder frontier caps at scale.)
   - The step = close the hole (`:open→:constructed` on the copy) + flip the cap on the copied overlay if the
     leaf carries `:advances-cap` (ordinary→`:satisfied`; frontier→`:claimed`, never auto-satisfied).
2. **Accumulator** — port ukrn `project-budget-path`'s K-tick loop: `G(π) = Σ_t γ^t g(s_t)` over `π=(leaf₁..leaf_K)`,
   discounted, with the sticky `:truncated` absorbing barrier. (Verify the port against the source at build time.)
3. **Per-step `g(s)`** = epistemic (claude-3's substrate-2 `C`/holes) + pragmatic (my Track-1-corrected,
   status-aware graph-ascent: off-map-penalty ≈4 / `:leaf` granularity). `g` is the metric density (Lagrangian),
   **not** EFE.
4. **Move-set consumer (the locked interface):** consume claude-3's **static snapshot ONCE**
   (`[{:have :want :leaf/edit :score :confidence}…]` sorted by score; `:confidence :conjectural` = not-yet-reachable/
   island). No mid-search dep on claude-3/`:7071`. Apply a **moving reachable mask per node** (intersect with
   currently-reachable, renormalize) — the prior is over the **superset** because constructing an arrow mid-rollout
   opens new reachable `:have`s, so deeper-legal moves must already be scored. **(Add-C)** reachable ⟺ the open
   arrow's `have` is reached by some `:constructed` arrow (endpoint-graph reachability, computed from the store).
   Fallback when the snapshot is absent: the reachable open-arrow set, unranked.
5. **Selection** — `argmin G(π)` (the geodesic) / softmax+abstain (ukrn `select-action`; `P(a) ∝ exp(−G/τ)`,
   **abstain when top candidates are indiscriminable** = WM-I4, don't act on a flat field).
6. **(Add-D) Absorbing barriers** — a `:advances-cap` at a **frontier cap with no path** is an *unreachable
   endpoint* (off-map-GOAL); the rollout treats it as a sticky barrier (needs a minting charter, not a leaf).
   Reachability (summit/island) ⟂ witness-class (`:frontier?`) — both axes independent (§3).

## Acceptance bar (the witness = completion-criterion-4)

- **(Add-E) A ≥2-step rollout beats the greedy one-step pick** on a constructed case where they differ — the
  canonical case: step-1 *satisfies the `have`* of a high-ascent step-2 that greedy one-step cannot see.
- **MUST-A verified:** live `promote!` and the sim invoke the *same* `step` (one definition; a test asserts
  identical state transition).
- **MUST-B verified:** a full rollout performs **zero** `:7071` writes (only the selected first step applies live).
- **Consumes the real contract:** the consumer runs against `futon6/data/diffsub-moves-stub.edn` (the locked shape),
  honouring `:conjectural` + the moving reachable mask.
- Gates: clj-kondo · `futon4/dev/check-parens.el` · futon2 tests · never restart the serving JVM.

## Build plan

CHARTERED now; **dispatch to codex on stub-landing** (claude-3 bells when `diffsub-moves-stub.edn` exists).
Codex builds `futon2.aif.rollout` + a worked example (the witness case) against the stub; claude-4 reviews against
this acceptance bar (author≠reviewer); bell claude-1 the excursion name + commit shas on landing.

## Provenance

Two-round whistle salvo (claude-4 ↔ claude-1, M-wm-policies coordination, 2026-06-09): AlphaZero split + move-set
interface + DERIVE ratified, conditional on MUST-A/B (accepted). Move-set refined by claude-3 (static superset
snapshot consumed once + moving reachable mask). Greenlit by claude-1 to charter as claude-4's next excursion.

## Stub validation (2026-06-09)

claude-3's stub `futon6/data/diffsub-moves-stub.edn` landed + validated (19 moves, parses clean,
shape matches the lock). Consumer contract **finalized** modulo one item:
- **(a) class set:** `:close-hole`→promote-on-copy · `:advance-capability`→promote+cap-flip ·
  `:graft-pattern`→**mint**-on-copy (adds an arrow, opens new reachable `:have`s). **OPEN:**
  `:centre-mess` (1 move) has **no v1 transition T** — pending claude-3: define its T, or (my lean)
  scope it OUT of v1 (carried as a terminal/opaque leaf, no expansion past it). **Sole blocker to build dispatch.**
- **(b) namespace:** full substrate-2 scope-ids (`scope/.../<id>`) — REQUIRED; the reachability gate
  joins move `:have` against my `:constructed`-arrow `:want`s in the same scope namespace. *Implication
  I own:* the rollout reasons over the substrate-2-scope-keyed arrow graph, not meme-internal strings.
- **(c) completeness:** the step needs `(have, want, advances-cap, class)` (all carried); cap
  `frontier?`/`status` comes from the cap-overlay **snapshot** (sim input, MUST-B), not the move; the
  construction payload is not needed for the sim. Shape complete.

**Dispatch gate:** claude-3 resolves `:centre-mess` → I dispatch the codex build + review.

## R1 + R2 — build refinements (claude-3 via claude-1, M-wm-policies §3 e3e1abd)

**R1 — `:prior` is the PUCT branching weight, not a top-k cut.** `:prior = softmax(:score)` over the
(reachable-renormalized) survivor set. The search expands moves **weighted by `:prior` (PUCT-style:
the P·U term)** — not uniform expansion over top-k; top-k is mere truncation. AlphaZero's
policy-head-as-MCTS-prior: `:prior` = the policy head guiding expansion, `G(π)` = the backed-up value.
Amends deliverable 4 (the search spine is PUCT-guided) — root selection (5) stays `argmin G(π)` /
softmax+abstain.

**R2 — reserve the return channel (v1 forward-only, build for it).** Thread `:move/id` **stably**
through the rollout (every leaf in a policy keeps its `:move/id`; realized `G(π)` is attributable
per-move-id post-search). v1 does **not** wire the return; v2 reports realized `G(π)` per `:move/id`
back to claude-3 as the gradient-loss training target → the prior becomes **learnable from search
outcomes** (reward = peradam; closes prior→search→train). **Build constraint:** never discard
`:move/id` mid-rollout; the per-leaf policy record carries it so v2 is a pure addition.

**Shape questions (a)/(b):** already answered to claude-3 directly (crossed wires) — class set right
for 3/4 (`:centre-mess` T is the open flag), `:have/:want` = full scope-ids (required for the
reachability join). No new mismatch.

**Dispatch gate unchanged:** still only `:centre-mess` (claude-3) + operator leash (Joe).

## Contract CLOSED (2026-06-09) — :centre-mess resolved; only the operator gate remains

claude-3 ratified (ii): **`:centre-mess` is terminal-opaque in v1** — carry its `g`-cost as a candidate
leaf, **no expansion through it**. Principled, not expedient: it's the only NON-atomic class (a compound
cluster graph-rewrite raising coherence/lowering Salingaros C, whose pattern→wiring→structure mechanism
isn't built — M-memes territory); a toy T would fabricate dynamics the sim can't run (the regulator
lesson). Promotes to a real T when M-memes delivers (claude-3's gap G6).

**Made explicit in the shape (no class-string special-casing):** every move now carries **`:move/terminal?`**
(true on `:centre-mess`, false on the other 18). **Kernel rule: expand iff `(not (:move/terminal? move))`.**
Stub regenerated at the same path (still 19 moves, parses clean). (a)/(b)/(c) all confirmed locked claude-3-side.

**Technical contract: FULLY CLOSED, no open items.** Build is fully specced.
**Sole remaining gate: operator go (Joe).** claude-4 committed to not dispatching the codex build without
Joe's explicit go; holding for it. On go → dispatch + review + bell claude-1 the shas.
