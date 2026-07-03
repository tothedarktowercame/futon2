# Excursion: the GFlowNet fold sampler, reopened on executable reward (E-gflownets-fold)

**Date:** 2026-07-03 · **Status:** CHARTERED (IDENTIFY/MAP — Joe: "we have the empirical data
that was blocking that before") · **Owner:** TBD (hand-off charter; CPU rungs first).
**Naming note (Joe's question):** plain `E-gflownets-fold`, no JAX — the vendored GFN library is
torch, the reward is a `bb` subprocess, and conditioning features are MiniLM/numpy. `jax_refine`
(differentiable wiring polish) is an OPTIONAL later stage, not a dependency.
**Parents / prior art:** [[E-fold-embed-pipeline]] G.1 (the seed NULL — both rungs, zero
concentration, verdict *fix not kill* with named knobs) · E-cascade-sampler-sampler (the s4
harness + the standing single-best-holds negatives ×4) · the Upgrades shelf discipline
(aif-wiring-explainer: "fix reward-shaping/conditioning BEFORE compute") · FutonZero §5
(semilattice-vs-rollout — native partial-order sampling is this lane's deep question).

## What changed since the null (the empirical unblockers)

The G.1 seed failed for a diagnosed reason: **exact-set overlap against a static gold gives TB
almost no per-trajectory gradient** (k≈6–9 subsets of a 21–70 pool share nearly identical partial
overlap). Since then (2026-07-02/03), the blocking items moved:

1. **An EXECUTABLE reward exists.** The fold executor (`fold_engine.clj`, now 16 rules) + the
   cascade constructor's F score any sampled cascade BY RUNNING IT: realized coverage-ΔG (which
   patterns actually fold into boxes) + ΔF (Bayesian-Occam accept). Per-item shaped — every folded
   box contributes — so the reward is steep AND informative, unlike gold-set membership. And it is
   reality-anchored: the same quantities the live act-gate consumes hourly.
2. **Conditioning features exist and are validated.** The Q-C mechanical have→want psi (want=doc
   title, have=Status line; Q-B scorecard: +0.093 mean ΔF, 58.9% improved) + MiniLM embeddings —
   the per-mission conditioning the seed lacked (its policy had NO input features at all).
3. **A verified corpus exists.** 43 git-validated `:discharged-by` relations (gold-gated sweep,
   8 hallucinated shas rejected) · the A-next gold 10 · escrow fold turns minting (cascade→wiring)
   pairs live · 218 proof-mine dossier records with endpoints. Enough to build held-out evaluation
   at mission grain.
4. **The flight log accumulates ground truth hourly** — per-pattern fold outcomes from real
   enactments (the burn-in corpus), free.

## The reframed experiment

Condition on the mission's psi; sample pattern sets (later: partial orders) with trajectory
balance; **reward = the executable composite** (executor coverage-ΔG + cascade ΔF; optionally the
full gate conjunction). The GFN's job is what greedy can't do: **diverse high-reward cascades per
mission** — sampling ∝ reward, not argmax.

**Success criteria (both required):**
- (i) the trained conditional sampler matches-or-beats the phylogeny-greedy constructor on
  realized fold coverage over HELD-OUT missions;
- (ii) diversity is real: ≥3 distinct cascades per mission above a coverage floor (the
  single-best-holds test, faced head-on — if greedy ties on (i) AND diversity adds nothing
  downstream, the lane dies).

**Kill criterion (standing, sharpened by history):** a fifth single-best-holds result — greedy
matching TB on executable reward with no diversity dividend — closes the GFN lane for the fold,
recorded as a real verdict, not deferred again.

## Rungs (CPU until signal, per the Upgrades discipline)

0. **Reward-shaping probe** — rerun the G.1 seed harness with the per-item/membership reward
   (named knob (a)) in place of exact-set overlap; same gold corpus. Cheap; isolates the reward
   fix from the conditioning fix.
1. **Executable-reward arm** — engine-in-the-loop reward (cache `bb` fold calls per selection);
   still unconditioned. Does TB concentrate when reward = what actually folds?
2. **Conditioned sampler** — psi/MiniLM features into the policy; train across missions; evaluate
   on held-out (the corpus from unblocker #3). This is the real experiment.
3. **(gated)** scale/GPU only if rung 2 shows signal AND CPU wall-clock binds; box per the
   proof-mine runner's D-disciplines. **(optional)** `jax_refine` polish of sampled wirings;
   native partial-order sampling (the FutonZero §5(a) question — note its precondition: move-class
   pricing, still open).

## Boundaries

- The Upgrades-shelf gate holds: **no GPU before CPU signal** (rungs 0–2 are laptop).
- This excursion is the *fix* of "fix not kill" — it does not reopen impl-#3-as-default; the fold
  interface and impls #1/#2 stay the live path regardless of outcome.
- Reuse, don't rebuild: the s4 harness + `gfn_seed_v0.py` + `sorry_proxy.py` are the scaffold;
  the gold corpus loader is canonical (`gold-corpus.json`).

## Exit conditions

1. Rungs 0–2 each produce a committed verdict JSON (the G.1 artifact discipline).
2. Success criteria (i)+(ii) measured on held-out missions — or the kill criterion invoked, with
   the verdict recorded in E-fold-embed-pipeline and the Upgrades card.
3. If successful: a follow-on named for wiring the sampler into the cascade lane as a
   diversity source (behind the same reduction-safe switches as everything else).
