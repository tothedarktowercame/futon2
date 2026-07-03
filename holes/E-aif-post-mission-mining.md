# E-aif-post-mission-mining — the follow-on ledger after the mining run landed

**Date:** 2026-07-03 · **Status:** LEDGER (so the follow-ons don't evaporate — Joe's ask).
**Context:** the AIF loop closed live and the gold-gated PROOF-MINE sweep landed the same day
(R16+R14 ✓ contract v0.22 · 43 git-validated `:discharged-by` · §11 coverage 138/189, tail 70→51).
This note holds the prioritized follow-ons agreed 2026-07-03. Each is small enough to not need a
mission; anything that grows gets its own E-note.

## In flight

- **#1 — Executor reach the honest way (claude-11, NOW).** The sweep's 72 `rule-candidates` were
  REVIEWED AND REJECTED as executor rules (verdict below); instead, NL→rule extraction done by an
  inhabiting agent over the LIVE-LANE patterns' actual THEN clauses → fold_engine RULES entries.
  Success = realized-G diverging from expected-G in the flight log ⇒ γ's first non-zero perf sample
  ⇒ **R14 variance closure (γ ≠ 1.0 in a live trace, contract v0.22)**.

## Queued (priority order)

- **#2 — §11 step 4: the forward model reads the durable join** (claude-11, one session). For the
  covered 138/189, `advanced-outcome-ids`' token-match is replaced by the store query
  (c-entry →outcome-ref→ mission →discharged-by/mined-move); token-match stays the fallback for
  the 51. Makes the belly's steering auditable to git-validated shas. Then §11 step 5 (reconcile).
- **#3 — c-entry-grain attribution + belly honesty pass** (offer to claude-3 — their
  stale-closed-missions finding). Push mission-keyed discharges down to specific c-entries; flip
  `:status` where evidenced. The 455-open count is measurably overstated; correcting it sharpens
  the live goal-outcome-risk denominator.
- **#4 — The escrow fold-turn ritual** (any inhabiting agent, ~2-3/day during burn-in). Honest
  fold turns for ΔF-positive lane missions: pass variety for the paper + (cascade→wiring) corpus
  pairs (E-wiring-diagram-corpus) + impl-#2 exercise.
- **#5 — Paper harvest checkpoint** (Joe + claude-11, once γ has moved). Wire into
  `p4ng/sequel-notebook.org`: `flight_log_extract.bb` (flights org-table/JSON),
  `proof-mine-gold-eval.json` (mining quality 0.54/0.70/0.70 + the 8-hallucinated-shas story),
  the §11 coverage-delta. Recompute the abstract's "~30% mechanical" number honestly.
- **#6 — The shelf** (in view, not urgent): **R15** — the last open forward-sweep criterion;
  design-shaped (nested model: mission-level belief priors entity-level belief) — charter fresh as
  its own excursion. The DarkTower pair (③ fold-wiring instance + Coverage live gate — both have
  live material now: enacted wirings flow hourly). The Upgrades shelf stays gated (GFN
  reward-shaping/conditioning before compute). M2C "Nelson move" (named the most *available*
  substrate-2 improvement, still true). fold-classical ↔ fold_engine rule-table convergence
  (after #1, the predictor is deliberately behind the executor — reconcile in the §6b bake-off).

## Verdict of record: the sweep's rule-candidates (2026-07-03, claude-11 review)

**REJECTED as executor rules** — kept as mission-content extractions only. The 72 candidates are
singletons whose `box` fields are invariant statements, file refs, and essay phrases (e.g.
"What generates the curriculum?", "∀ agent: invoke-ready? ∨ diagnostic"), with line-ref warrants
("L154-L173") rather than verbatim spans; 0/72 intersect the live lane's cascade patterns. Landing
them in fold_engine would fabricate reach (the honest-NL→rule bar from proof-mine-runner-spec D7).
They may still be useful as CLAIM/invariant material — a possible future lane, not this one.
The *discharge* lane of the same run remains excellent (43 landed, git-validated); the lesson is
per-lane review: one run, two lanes, two different verdicts.
