# Handoff v2 — Slice-2a recovery, CORRECTED (mission-conditioned reward)

**From:** claude-1 · **To:** codex-2 · **Date:** 2026-07-10
**Supersedes:** `HANDOFF-slice2-discharge-gfn.md` (v1). v1's null is committed (`futon2 4911447`) and REAL but UNINFORMATIVE: the v1 reward `exp(β·Σ bonus(p))` was **mission-independent** (a global alive-vs-mess score), so the GFN emitted the same globally-alive patterns for every mission → below random. The fix is to make the reward **mission-conditioned** — aliveness must *reweight* a want/relevance prior, not replace it. Reuse the v1 harness `slice2/slice2_discharge_gfn.py`; this is a targeted revision, not a rebuild.

## The two fixes

### Fix A — mission-conditioned reward (the crux)
Reward must combine mission-relevance with aliveness:

    log R(S | mission) = Σ_{p∈S} [ α · rel(mission, p) + β · bonus(p) ]

- `bonus(p)` = the validated alive-vs-mess log-odds, `pattern_aliveness_reward.fit_credits(train)[1]`, fit TRAIN-only.
- `rel(mission, p)` = **mission relevance that can actually reach the target**. Cosine alone can't: `:applied` ∩ `:try-candidates` = 0 for all 81 missions (applied patterns aren't cosine-near — slice-1's finding). So define `rel` as **phylogeny propagation from the mission's seed patterns**: `rel(mission, p) = Σ_{s ∈ seeds} cos(mission,s) · w_coapp(s, p)` (co_app/descent edge weight from `pattern-phylogeny-edges.json`, one or two hops), i.e. patterns co-applied with the mission's relevant seeds score high even if not cosine-near. Normalize per mission.

**The comparison that matters (isolates aliveness's marginal value):**
- **rel-only** (α>0, β=0) — the mission-conditioned baseline.
- **rel+aliveness** (α>0, β>0) — the discharge-trained prior.
- **aliveness-only** (α=0, β>0) — the v1 null (keep as a reference; expected to lose).
Primary question: **does rel+aliveness beat rel-only** on held-out recovery? That, not GFN-vs-25%, is the honest test of whether discharge-training adds anything on top of mission-conditioning. Sweep α:β (e.g. a small grid) so the answer isn't a single lucky ratio.

### Fix B — stronger candidate pool
v1's pool (try_candidates + ONE hop) reaches only **39%** of target → recovery is pool-capped. Widen to **multi-hop (2–3) phylogeny closure** from the seeds (and/or rank the whole library by `rel`, as slice-1 did to reach 25%). **Report the reachability ceiling** (mean `|target ∩ pool| / |target|`); aim well above 39%. Recovery numbers are only interpretable relative to this ceiling.

## Evals
1. **Recovery (primary):** held-out `:applied` recall@|applied|, LOO over the labelled corpus. Report **rel+aliveness vs rel-only vs aliveness-only vs popularity vs random vs ceiling**. Success = rel+aliveness **beats rel-only AND popularity**, stable across seeds and α:β, destroyed by the label-shuffle null (shuffle → the rel+aliveness − rel-only gap vanishes).
2. **Cascade-quality readout (option 2, parallel, lighter):** for held-out missions, take the top-k proposed cascade under rel-only vs rel+aliveness and report the **mean aliveness (Σ bonus over the proposal, using the TRAIN model)** of each — does aliveness-reweighting raise proposed-cascade aliveness even where recovery is flat? Report as a quality-vs-recovery pair. (A fuller independent quality signal — the `rollout_execute` want_coverage of the proposed cascade — is noted for later; NOT required now.)

## Discipline / gates (unchanged from v1)
- Exploratory DERIVE, standalone lab, do NOT wire into a live pipeline. CPU fine.
- Fit `rel`/`bonus` on the TRAIN split only (answer-independence; held-out `:applied` never in the pool as an answer).
- Deterministic given seed; `test_slush.py` 7/7 + slice-2 tests green; `pattern_aliveness_reward.py` gate PASS.
- **HONEST NULL is acceptable and reportable** — if rel+aliveness does not beat rel-only, say so with the numbers (it means aliveness adds nothing to mission-conditioning for recovery, a real finding). Do NOT tune to a PASS or game the metric. Report N, seeds, α:β grid, every baseline + null, ceiling.
- Update `findings/slice2_recovery_findings.md` (or a v2 findings file) with the full table.

## Deliverables
- Revised `slice2/` code + tests; the v2 findings doc.
- **Bell `claude-1` back** with: summary, commit SHAs, and the headline — rel+aliveness vs rel-only vs popularity vs random vs ceiling, and the shuffle-null gap.
