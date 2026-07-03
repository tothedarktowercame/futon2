# Mission: Evaluate policies honestly — make R5's score what it says it is

**Date:** 2026-07-03
**Status:** IDENTIFY (chartered from the R18 faithfulness audit — Joe: "the items from R5 should
probably be put into the IDENTIFY stage of a new mission")
**Owner:** unassigned (framed by Joe + claude-11)
**Sources:** `holes/E-r18-faithfulness-audit.md` + `data/r18-badges.edn` (claude-2, 2026-07-03,
futon2 `dcbe021`) · `src/futon2/aif/efe.clj` (the code under discussion) ·
`docs/futon-aif-completeness.md` §R5/§R18 (v0.23) · ledger context
`holes/E-aif-post-mission-mining.md` #6.6 (this mission absorbs it).

## 1. The tension (IDENTIFY)

The War Machine selects its actions hourly by softmaxing `G-total`, which the code and the
diagrams call **Expected Free Energy**. The R18 audit examined all eight terms of that sum
against the canonical literature and found: **one principled approximation, seven analogicals,
and a total that is not EFE** — it is a multi-objective score with an EFE-shaped core, in
incommensurate units, at hand-set weights. The loop *works* (it closed live 2026-07-02; γ is
calibrating), but the evaluation layer's self-description is the least honest part of a system
whose other layers just earned their labels.

This matters now for three live reasons:
- **Selection ranks by the blend every tick.** Whatever the blend actually optimizes is what the
  WM actually pursues. Nobody has written down what that IS, in one place, in honest units.
- **The paper** (`p4ng/sequel-notebook.org`) says "EFE-scored action selection." After the audit,
  the honest sentence is "multi-objective score with an EFE core" — unless this mission earns the
  original sentence back.
- **The units problem compounds.** Every new term (G-goal-outcome arrived 2026-07-02) is added
  linearly into a sum whose terms already can't be compared; hand-set weights absorb the damage
  invisibly.

## 2. The evidence — the eight R5 verdicts (verbatim from the audit)

| term | badge | claims to be | actually computes | repair |
|---|---|---|---|---|
| G-total | analogical | Expected Free Energy | linear sum of 8 terms in incommensurate units at hand-set weights; risk+ambiguity core diluted | expose canonical G = risk+ambiguity separately; label the blend a multi-objective score, not EFE |
| G-risk | analogical | EFE risk = D_KL[Q(o‖π) ‖ C] | weighted L1 hinge-distance of predicted point-estimate from preference ranges; no distribution/log/KL | predict outcome distribution Q(o‖π) and KL against a normalised preference C |
| G-ambiguity | principled-approximation | EFE ambiguity E_Q(s‖π)[H[P(o‖s)]] | sum of per-channel predicted variance (monotone Gaussian-entropy proxy) | use ½ln(2πe σ²) under Q(s‖π) → derived-from-FEP |
| G-info | analogical | epistemic info-gain (EIG) | Σ max(0,1−variance) = complement of ambiguity (same next-var); no belief-entropy reduction | compute real EIG over beliefs (as the futon3c portfolio sibling does) |
| G-survival | analogical | survival-pressure EFE term | unweighted Σ hinge-gap over 4 critical channels — a second G-risk on a subset | fold into risk, or rename 'homeostatic pressure' and drop the EFE framing |
| G-structural-pressure | analogical | weighted EFE term | pass-through of a caller-injected scalar × 0.35; not a computed quantity | derive from the generative model, or rename as an exogenous weight |
| G-graph-pragmatic | analogical | pragmatic value | 1000·[not-applicable] + 3·holes − 20·ascent-progress (graph heuristic counts/gates) | split: applicability = feasibility mask; ascent = pragmatic proxy toward C |
| G-gap | analogical | epistemic 'room to grow' | 6.0 × caller-supplied gap-score lookup; no expected-information computation | compute expected uncertainty-reduction from filling the gap |

(`G-goal-outcome` is homed on R19/the belly, not here — its repair is the named W1, tracked with
the PROOF join. It is however the ninth summand of the same blend, so §3's structural questions
include it.)

## 3. What the mission is FOR (the questions, not yet the answers)

Per the method, IDENTIFY names the territory without committing to a construction:

1. **What does the blend actually optimize?** Characterise the current G-total empirically (the
   flight log + traces give per-term values per candidate per tick — real data, accumulating
   hourly) before changing anything. Which terms ever flip a decision? At current weights, is the
   selection effectively 2-term? 3-term? (If six terms never matter, the honest repair may be
   deletion, not derivation.)
2. **Core vs overlay.** The audit's minimal repair: expose canonical `G = risk + ambiguity`
   separately from the heuristic overlay, so "EFE" names only the part that is one. Does the WM
   behave acceptably selecting on the core + a feasibility mask alone?
3. **Commensurability.** If heuristic terms stay, what puts them in common units — normalisation
   per term? everything as (approximate) bits/nats? weights learned rather than hand-set (R12's
   Beta-credit machinery is adjacent)?
4. **Term-by-term dispositions.** Each row's repair is a candidate; some repairs are one-liners
   (G-ambiguity's entropy formula), some are merges (G-survival into risk), some are renames
   (G-structural-pressure as an exogenous weight), some are real builds (G-risk's distributional
   KL — which shares machinery with the belly's W1).
5. **The mask question.** G-graph-pragmatic's 1000·[not-applicable] is a feasibility HARD
   constraint smuggled into a value term. Should infeasibility be a mask before scoring, not a
   penalty inside it?

## 4. Boundaries

- **γ is out of scope and already safe**: its calibration pair is coverage-ΔG vs coverage-ΔG
  (claude-10 review fix, 2026-07-02) — the blend never enters γ. Nothing here touches R14.
- **The belly's W1** (real predictive-KL for G-goal-outcome) stays with R19/E-C-vector-live §11.
- **Reduction safety is a hard constraint**: the WM runs hourly in production; every change lands
  behind the established discipline (behaviour-identical defaults, sim-first, flight-log
  before/after comparison — the same shape as R14's burn-in evidence).

## 5. Exit criteria for IDENTIFY → MAP

1. The empirical characterisation (§3.1) exists: per-term decision-influence measured over ≥1 week
   of flights (the burn-in corpus is accumulating now).
2. The core/overlay split (§3.2) is ARGUE-able with data: what selection-on-core-only would have
   chosen differently, tick by tick.
3. Each of the 8 terms has a proposed disposition (repair / merge / rename / delete) with its cost.
4. The paper's sentence is decided: either the apparatus earns "EFE-scored" or the text says
   "multi-objective score with an EFE core" (we-do discipline — the label follows the code, not
   the aspiration).
