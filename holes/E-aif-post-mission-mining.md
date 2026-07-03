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
- **#6.5 — R18 badge RENDER** — **explainer half DONE 2026-07-03** (badge dots on nodes +
  plain-language panel; `r18_badges_to_js.bb` regenerates the embed). Remaining half: the live
  readout so every quantity permanently carries its provenance badge (0 FEP-derived · 7
  principled-approx · 9 analogical today — and each badge's `:repair` field is a costed upgrade
  path; the sharpest, :G-goal-outcome's non-KL, is exactly the named W1).
- **#6.6 — ABSORBED into `M-evaluate-policies.md` (IDENTIFY, 2026-07-03)** — Joe: the R5 items
  are mission-shaped, not fix-shaped. The mission carries all 8 R5 verdicts + the structural
  questions (what does the blend actually optimize; core/overlay split; commensurability;
  per-term dispositions; the feasibility-mask question). Original note kept for context:
  (green-path items 2-3;
  no new math): (a) **rename the 8-term G-total blend** — it is risk+ambiguity (canonical core)
  plus six heuristic terms in incommensurate units; a struct split (`:G-efe-core` vs
  `:G-heuristic-terms`) + rename makes the selection honest ("multi-objective score with an EFE
  core", which is what the paper should say); NB γ is already INSULATED from this (its perf pair
  is coverage-ΔG vs coverage-ΔG by the claude-10 review fix — the blend never enters γ's
  calibration). (b) **real EIG for G-info** — today it double-counts the ambiguity variance
  vector; the audit notes working futon3c siblings to copy. Belly's real predictive-KL = W1,
  already tracked.
- **#6 — The shelf** (in view, not urgent): **R15** — the last open forward-sweep criterion;
  design-shaped (nested model: mission-level belief priors entity-level belief) — charter fresh as
  its own excursion. The DarkTower pair (③ fold-wiring instance + Coverage live gate — both have
  live material now: enacted wirings flow hourly). The Upgrades shelf's GFN lane is now REOPENED as `E-gflownets-fold.md`
  (2026-07-03 — executable reward unblocked it; CPU rungs first, kill criterion armed). M2C "Nelson move" (named the most *available*
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
- **#7 — REACH GOAL (Joe, 2026-07-03): formally prove we have an AIF implementation.** Not
  AIF-the-physics (the FEP-as-principle isn't a provable proposition) but the mathematical
  stratum: Da Costa et al.'s discrete-state active inference is well-defined algorithms on
  POMDPs — formalizable. Sketch of rungs, each standing alone: **(A)** AIFSpec.lean — the
  canonical definitions (VFE, EFE = risk+ambiguity, softmax selection, the γ update) atop
  Mathlib probability, in DarkTower (the Lean machinery + 0-sorry gate already exist and are
  exercised — CLean renders mission wirings today); **(B)** spec-level theorems = the R9
  validation properties lifted from runtime checks to proofs (perf ∈ [−1,1], γ clamped,
  τ_eff reduction-safe at γ=1, Bayes-update properties); **(C)** CONFORMANCE — a verified
  reference core in Lean, differentially tested against futon2 on flight-log inputs (the burn-in
  corpus = the test vectors): "the production loop agrees with the verified reference on N
  flights to ε" — the honest meaning of 'proved implementation' for a live Clojure system;
  **(D)** R18 badges become Lean-checked correspondences (:derived-from-FEP = a compiled
  theorem, not a reviewed claim). SEQUENCING: gated on M-evaluate-policies' core/overlay split
  (one cannot prove the blend is EFE — it isn't; prove the CORE after the split). The R-contract
  (v0.23 checkable forms) is the informal spec; the badge :repair fields are the lemma inventory;
  the peradam 3-witness frame is the natural certificate shape.

