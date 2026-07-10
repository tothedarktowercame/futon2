# Spec — Slice-2 v3: self-evidencing aliveness (accuracy − complexity) as the GFN reward

**Author:** claude-1 · **Date:** 2026-07-10 · **Status:** SPEC (reward-gate to be run by claude-1 first; GFN build → codex)
**Grounding:** `futon3c/holes/excursions/deep-research-AIF-morphogenesis.md` (salvaged deep-research; partially verified — treat as grounded *hypothesis*, and note the doc's own flag: the `L=T·H` ↔ free-energy link is analogical/open. Building this is a first cut at that gap.)

## Why (the morphogenetic reframe)
Prior aliveness signals failed because they sit on the **passive** pole the literature warns about:
- **scope-`L`** (Salingaros harmony over the mission scope-tree) — saturates; failed the null gate; it's mostly H (coherence), no accuracy-against-a-want.
- **curvature-discharge** (Ollivier-Ricci rising over commits) — nulled at N=9; it measures *relaxation* (minimal-surface / energy-floor pole), and Salingaros treats curvature as **life-giving** (T), so discharging it is backwards.

Morphogenesis (Friston–Levin): a living system's parts **infer their place in a shared generative model of a target form**; the minimum is a *differentiated body-plan*, not the flat/energy-floor state. The engine is the **accuracy − complexity** decomposition of free energy (Bayesian model evidence): structure is added only when complexity is *paid back by evidence*. So:

> **aliveness = self-evidencing toward the want = accuracy − λ·complexity.**

This scores `1=1`/zero-sorries **dead** (near-zero accuracy against any real want) and scores a **bloated uniform shell dead too** (complexity not paid back by accuracy — the anti-gaming property). The active/accuracy limb is exactly the one signal that showed teeth this session: `rollout_execute` want-coverage.

## The reward
For a cascade `S` proposed for mission `m`:

    aliveness_v3(S | m) = accuracy(S, m) − λ · complexity(S)
    R(S | m) = exp(β · aliveness_v3(S | m))

- **accuracy(S, m) = execution discharge** — the `rollout_execute.py` semilattice-rollout `want_coverage` of `S` against `m`'s want (the built-artifact accuracy, not embedding-similarity). Reuse `futon3a/holes/labs/M-memes-arrows/rollout_execute.py`. **This is mission-conditioned by construction** — it fixes v1's mission-independence bug automatically (no separate `rel` term needed).
- **complexity(S) = Σ_{p∈S} −log(base-rate inclusion prior of p)** — reuse `cascade_construct.py`'s F-complexity (co-application base-rate; the AIF-grounded Occam term already in the stack). Penalizes unjustified structural elaboration.
- Note: this is the offramp F-score with its **accuracy limb upgraded from passive ψ-embedding-coverage to active execution-discharge** — "extend aliveness into the built code."

Optionally weight accuracy by **want non-triviality** (information content of `m`'s want) so covering a trivial want scores low even at full coverage — the sharpest anti-`1=1` guard. Ablate this term.

## Reward-before-generator GATE (claude-1 runs this FIRST — no GFN until it passes)
This discipline caught the constellation-grain and mission-independence failures early; apply it again.
1. Compute `aliveness_v3` over the historical corpus (rollout_execute discharge per mission's `:applied`, minus complexity). SMOKE-TEST on ~15 missions before the full corpus.
2. **Not-saturated:** unlike scope-`L`, the distribution must spread (not all-alive).
3. **Correlates with outcome:** LOO-AUC vs the independent alive/mess label AND vs Status(held/reopened) — must beat the shuffle null (target: clear scope-`L`'s ~0.71, and — unlike scope-`L` — give the reward teeth).
4. **Anti-`1=1` control (mandatory):** construct trivial cascades (single trivial pattern / a tautological want) and confirm they score **dead** (low aliveness_v3). Construct a bloated-uniform-shell cascade and confirm it scores **dead** (complexity not paid back). If the trivial/gamed controls are NOT scored dead, the reward is wrong — fix before proceeding.

## The GFN (codex, after the gate passes)
Sample cascades `∝ exp(β·aliveness_v3(S|m))` over the pattern semilattice (reuse the `slice2/` harness + `fold-gfn` TB structure). Mission-conditioned by the want-coverage accuracy limb. Ablate λ (the accuracy/complexity trade-off — the "when is added structure worth it" gate) and β.

## Eval (the RIGHT metric this time — quality, not historical-set recovery)
The recovery-of-`:applied` metric was wrong for aliveness (it tests mission-conditioning, not quality). Instead:
- **Cascade quality:** do GFN-proposed cascades have higher **held-out** `aliveness_v3` (execution-discharge − complexity) than (a) retrieval-prior proposals, (b) popularity, (c) the scope-`L`-reward GFN (v1/v2)? This is what aliveness is *for*.
- **Anti-gaming carried through:** confirm the GFN does not win by proposing bloated shells (they'd score low aliveness_v3 by construction) or trivial cascades.
- **Null:** shuffle the aliveness labels/reward → the quality lift vanishes.
- HONEST NULL acceptable and reportable; do not tune to a PASS.

## Discipline / gates / deliverables
- Exploratory DERIVE, standalone lab, no live-pipeline wiring. TRAIN-only fits (answer-independence). CPU; smoke-test before full corpus (rollout_execute per mission is the cost). Deterministic given seed. `test_slush` + slice-2 tests green.
- Deliverables: `slice2/` v3 code + tests; `findings/slice2_v3_aliveness_findings.md` with the gate results, the anti-`1=1` control, and the quality table + null. Bell `claude-1` back with summary + SHAs + headline.
