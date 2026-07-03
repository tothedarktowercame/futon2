# Defect report: rollout ΔG has never fired for a production cascade

**From:** M-evaluate-policies (MAP §7.7 / ARGUE §9.5–9.6) → **To:** M-G-over-cascades (C10)
**Date:** 2026-07-03 · **Reporter:** claude-5 (Fable session, mission owner-side)

## The fact

Every `:apply-cascade` row ever persisted to the arena carries `:G-total 0.0` and no term
decomposition: **293/293 rows, across 147 of 674 ticks (2026-05-18 → 2026-07-03), 0 wins.**
Verified by corpus census (`futon2/scripts/wm_trace_census.bb`, artifacts in
`futon2/holes/labs/M-evaluate-policies/`).

## The mechanism (corrected — not broken wiring)

`futon2/scripts/futon2/report/war_machine.clj:3797` sets `:G-total (or dG 0.0)`. The dG
leg comes from cascade-lane seam 2 (`rollout-g-for`), which **abstains by design** when
the mission has no moves in the v2 move-set (`futon6/data/diffsub-moves.edn`; v1
reachable set = 3 summits). Both missions the lane has ever cascaded in production
(M-canon-fingerprint-store, M-bayesian-structure-learning) are outside that set ⇒ dG nil
⇒ placeholder 0.0, always. **A coverage gap in the v2 move-set, not a wiring bug.**

## Why it matters (measured)

1. The placeholder is **load-bearing**: deleting blend terms whose removal raises
   ordinary G-totals by more than ~4.5 units would hand wins to unscored cascade rows
   (M-evaluate-policies IHTB-2 — sequencing constraint on ANY term deletion).
2. The cascade lane computes a real value (wholeness/F) that never reaches the arena;
   the act-gate conjunction ΔF>0 ∧ ΔG<0 has **never once been evaluated in production**
   (first-ever evaluation was the exhibit's recorded LLM fold: ΔF=4.861, ΔG=−0.75).
3. Menu-quality finding (E-evaluate-policies-spikes, spike 2): the two missions the lane
   cascades are the *weakest* ψs in a 13-mission sample; the strongest never enter.

## Asks for M-G-over-cascades

- Treat v2 move-set coverage (or an alternative ΔG path for out-of-set missions) as a
  first-class gap in the G-over-cascades design.
- Declare, in the DERIVE: **sense** (which EFE, i–iv) × **selection semantics**
  (extremum / amortized sample / field-lift bound — extremum is not computable; see
  M-evaluate-policies §9.6) × **conditioning** (what slice of the situation the score
  consumes — the ψ prose scrap starves it today; see §9.7; recommended: the same
  structured observation vector the atomic lane uses).
- Until scoring lands: keep the 0.0 placeholder but mark rows
  `:score-provenance :placeholder` (M-evaluate-policies D1b, being built).
