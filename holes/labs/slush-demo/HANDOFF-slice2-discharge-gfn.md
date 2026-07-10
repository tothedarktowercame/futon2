# Handoff — Slice-2a: the discharge-trained pattern-GFN + held-out recovery

**From:** claude-1 · **To:** codex-2 · **Date:** 2026-07-10
**Parent:** `futon2/holes/M-G-over-cascades.md` (slice-2a) · charter `futon2/holes/E-gflownets-fold.md`
**Discipline:** exploratory DERIVE (NOT INSTANTIATE). Standalone lab + findings. Do NOT wire into any live pipeline. CPU is fine (the positive prior GFN ran on a laptop CPU); GPU only if CPU shows signal.

## Goal
Train a **pattern-grain GFlowNet** to sample pattern-compositions ∝ a validated **discharge reward**, then test whether the discharge-trained move-prior lifts **held-out mission `:applied` recovery above slice-1's 25%**. This is the "discharge-train the prior" step the spec names; the novel, already-validated piece is that the reward is finally *informative* (all prior GFN runs nulled out on flat coverage/overlap rewards — see the survey in the parent thread).

## What is already done — REUSE, do not rebuild
1. **The reward is validated (this is the key unlock).**
   `futon2/holes/labs/slush-demo/pattern_aliveness_reward.py` — `fit_credits(train)` returns `(credit, bonus)` per pattern from a TRAIN split.
   - Use **`bonus`** = smoothed alive-vs-mess document-frequency **log-odds** as the reward-bearing signal. Gate result: LOO-AUC **0.706** vs label-shuffle null-max **0.680** → PASS (clean-core ~0.76). Popularity-normalized by construction.
   - `credit` (graded-`L` award) FAILED the null gate (0.583 vs 0.588) — graded scope-`L` is compressed among alive missions. DEFER it (it belongs to the future "aliveness beyond scope" extension). Reward = `bonus` only for now.
   - Running the file re-prints the gate; keep it PASS.
2. **The positive prior GFN to mirror:** `futon2/holes/labs/fold-gfn/fold_gfn.py` + `reward.py` + `FINDING.md` — hand-rolled trajectory-balance GFN (torch/CPU), steep reward `exp(β··)`, leave-one-mission-out recovery eval (got F1 0.69 vs 0.51 random). **Copy this structure.** The lesson: the reward MUST be steep, so use `R(S) = exp(β · Σ_{p∈S} bonus(p))`.
3. **The slush harness / GFN plumbing:** `run_slush_demo.py`, `slush_sweep.py`, `slush_proxy.py` (hydra config init, the `Proxy` subclass shape, `selection_from_proxy_state`). The vendored library is at `/home/joe/code/gflownet`; run everything with `/home/joe/code/gflownet/.venv/bin/python`.
4. **The recovery-experiment shape:** `futon3a/holes/labs/M-memes-arrows/cascade_recovery_experiment.py` — held-out `:applied` recovery vs random; and `cascade_semilattice.py` (`beam_rollout`, `train_logodds`) + `cascade_rollout.py` (move interface). Reuse the eval loop shape (LOO over missions, recall of the true `:used`/`:applied` set).
5. **Data (all real):** `futon6/data/mission-pattern-scopes.edn` (`:applied` ground truth + `:try-candidates [{:pattern :cos}]` retrieval prior), `futon6/data/mission-wholeness.edn` (`:class` alive/mess + graded `:L`), `futon6/data/pattern-phylogeny-edges.json` (co_app 2721 + descent 561 = the semilattice move-graph), `futon3c/holes/excursions/pipeline-semilattice-clusters.edn` (constellations, for the coarse EIG scaffold if used).

## Build
1. **Reward module** (thin wrapper over `pattern_aliveness_reward.fit_credits`): `R(S) = exp(β · Σ_{p∈S} bonus(p))`, optional `+ λ·EIG` (constellation posterior stddev, from `slush_proxy.CONSTELLATION_EIG`) — ablate λ∈{0, small}. **Fit `bonus` on the TRAIN split ONLY** (no held-out mission leaks into its own reward — answer-independence).
2. **Pattern-grain GFN** (mirror `fold-gfn`): state = partial pattern set; actions = add an admissible pattern (a co_app/descent neighbour of the current set via the phylogeny move-graph) or STOP; trajectory-balance loss. Candidate pool per mission = its `:try-candidates` ∪ phylogeny neighbours, seeded from the problem text / magnet. Prefer the hand-rolled TB GFN (fold-gfn) over the vendored `Choices` env (which forces exact-k and hit a NotImplementedError in prior runs).
3. **Eval — the success criterion.** Leave-one-mission-out over the labelled corpus: fit `bonus` on the other missions, run the GFN seeded from the held-out mission's problem text, sample K cascades, measure **recall of its true `:applied` set**. Report mean recall against ALL of: (a) slice-1's **25%**, (b) random draw, (c) the untrained **retrieval-prior beam** (per-pattern `:cos` baseline), (d) a **popularity-only** baseline (pick globally most-used patterns).

## Null controls & generalisation tests (MANDATORY — this is the gate on substance, not a checker)
- **Label-shuffle null:** shuffle alive/mess, refit `bonus`, confirm the recovery lift **vanishes**. (If lift survives a shuffled reward, the lift is an artifact — report and stop.)
- **Answer-independence:** the reward/prior must never see the held-out mission's `:applied` (enforce via the LOO split).
- **Demo-parameter invariance:** vary seed / K / n_steps; the lift must be stable, not a lucky seed.
- **Non-gameability:** the GFN must beat the popularity-only baseline (recovering popular patterns is not learning discharge).

## Acceptance bar
- **PASS:** discharge-trained GFN lifts held-out `:applied` recovery **above 25% AND above the retrieval-prior beam AND above popularity-only**, with the lift **surviving demo-parameter variation and destroyed by the label-shuffle null**.
- **HONEST NULL is an acceptable outcome, not a failure to hide:** if discharge-training does NOT lift recovery, report it plainly with the full numbers — per the spec that is itself a real finding (the pattern-language/link structure, not the prior, is the limit) and motivates the typed-link experiment. Do NOT tune to a PASS or game the metric.
- Report N, seeds, and EVERY baseline + null explicitly. Numbers must come from the artifacts, not be asserted.

## Gates to clear before belling back
- New Python has tests; existing `test_slush.py` stays **7/7 green**; `pattern_aliveness_reward.py` gate stays **PASS**.
- Deterministic given a fixed seed.
- A findings doc `findings/slice2_recovery_findings.md` with the recovery table (all baselines + nulls), same style as `findings/slush_sweep_findings.md`.
- Run under `/home/joe/code/gflownet/.venv/bin/python`.

## Deliverables
- Code in `futon2/holes/labs/slush-demo/` (a `slice2/` subdir is fine).
- The findings doc.
- **Bell `claude-1` back** with a summary + the commit SHAs + the headline recovery numbers (real vs 25% vs baselines vs null).
