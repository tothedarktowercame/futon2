# E-evaluate-policies-spikes

**Date:** 2026-07-03
**Status:** two spikes COMPLETE same-day (chartered from M-evaluate-policies ARGUE §9.6–§9.7
offers; Joe: "Let's do the two small offers, possibly written up in E-evaluate-policies-spikes")
**Owner:** Fable (claude-code session) — E-prefix convention: bounded scope-out, single agent
end-to-end.
**Parent:** `holes/M-evaluate-policies.md` (ARGUE phase). **Feeds:**
`E-cascade-sampler-sampler` (spectral entrant), `M-G-over-cascades` / C10 (conditioning +
selection-semantics hand-off).

## Charter

Two bounded experiments arising from the ARGUE passes:
1. **Spike 1 (§9.7 offer d):** make each valuation's *conditioning* explicit in the ARGUE
   exhibit — a "conditions on" column per row, so fit-vs-form is visible per number.
2. **Spike 2 (§9.6 offer):** measure how much of cascade valuation an additive **field-lift**
   recovers — PageRank-style stationary potentials over the pattern phylogeny, lifted by
   summation, ranked against the constructor's non-additive functionals (wholeness T·H,
   F = accuracy − λ·complexity).

## Spike 1 — "conditions on" column (DONE)

`scripts/exhibit_cascade_argue.py` valuation tables now carry a **conditions on (§9.7)**
column; the method note states the asymmetry. Exhibit regenerated
(`holes/labs/M-evaluate-policies/exhibit/argue-exhibit.pdf`, 8 pp, verified). Per-row:
T-intensity/accuracy → ψ (160-char have→want scrap); H-coherence → nothing (intrinsic);
complexity → corpus base-rates (unsituated); F/wholeness → mixed; blend-G and core-G →
*(would)* the live observation vector; rollout ΔG → v2 move-set + capability overlay, not
the tick's observation. The table now shows at a glance that the two lanes consume
different slices of the world, and that two of six native components consume none.

## Spike 2 — spectral potentials vs constructor functionals (DONE)

**Apparatus:** `scripts/spike_spectral_potentials.py` (rerunnable; pure-python PageRank,
damping 0.85, over the phylogeny: 2,721 weighted symmetric co-app edges + 561 directed
descent edges, 506+ patterns) × the production constructor + production ψ recipe over a
13-mission sample. Artifact:
`holes/labs/M-evaluate-policies/spike-spectral/spectral-comparison.json`.

**Results (n = 13, Spearman ρ):**

| pair | ρ |
|---|---|
| potential-sum vs **F** | **0.885** |
| potential-sum vs wholeness | 0.747 |
| potential-**mean** vs F | **0.808** |
| potential-mean vs wholeness | **0.566** |
| size vs wholeness *(confound)* | 0.847 |
| size vs potential-sum *(confound)* | 0.882 |

**Reading (honest, n small):**
1. **The additive shadow is substantial** — a one-shot global fixed point (milliseconds,
   no oracle calls) recovers most of the F-ordering (ρ ≈ 0.81–0.89). As a *menu prior* /
   cheap pre-ranker for a sampler contest, spectral potentials are viable — supporting
   Joe's PageRank instinct.
2. **But much of the sum-signal is size-mediated:** saturation size correlates ~0.85–0.88
   with both wholeness and potential-sum — ψs that grip the library both grow the cascade
   and accrue potential. The size-controlled potential-*mean* keeps ρ 0.808 vs F yet
   drops to 0.566 vs **wholeness** — the coherence product H is precisely where
   additivity fails, exactly §9.6's predicted caveat, now measured.
3. **Divergence at the top matters for selection:** field-lift ranks M-aif2 (Σπ 37.3‰)
   above M-evaluate-policies (31.7‰); F ranks M-evaluate-policies (4.861) first. A
   potential-only selector would pick a different summit — bounds/priors, not extrema.
4. Sample-wide sanity: production's two arena missions land at the bottom on every
   currency (canon-fingerprint: size 1, F −0.122, Σπ 1.0‰; bayesian: size 3 post-
   SUPERSEDED) — the lane has been cascading the *weakest* ψs in the sample while the
   strongest (this mission, aif2, invariant-queue-unstuck) never entered the arena.
   Menu quality bounds selection quality (§9.6), illustrated.

**Caveats:** n = 13 missions, one ψ each, thin ψ conditioning by design (isolates
additive-vs-non-additive, not conditioning); potentials computed on today's phylogeny
snapshot; Spearman on 13 points is indicative, not conclusive.

## Hand-offs

- **E-cascade-sampler-sampler:** spectral potentials enter as the cheap second entrant
  (vs GFlowNet): use as proposal prior / pre-ranker; do NOT use as selector (finding 3).
  Oracle-call budgets per budget-bounds-exploration.
- **M-G-over-cascades / C10:** any real G-over-cascades should declare
  sense × selection-semantics × conditioning (mission §9.6–9.7); finding 4 is direct
  evidence that the current lane's ψ-selection starves the menu.
- **M-evaluate-policies VERIFY:** spike 2 doubles as the first VERIFY-grade evidence for
  the §8.4 D3 decision (documented units + staged experiments over live renorm) — the
  experiment loop works and is cheap.
