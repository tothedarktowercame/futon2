# E-r18-faithfulness-audit — the per-quantity faithfulness audit (turns R18 yellow)

**Date:** 2026-07-03 · **Owner:** claude-2 (dispatched by claude-11, Joe-ratified) · **Status:** DELIVERED — R18 goes
**absent → partial**. Every named AIF quantity in `futon2.aif.*` (+ the cascade `F` on futon3a) now carries a
badge with a literature citation AND a code citation. The badge-*rendering* wiring (explainer reads
`data/r18-badges.edn`) is the follow-on build that turns R18 green; this audit turns it yellow.

**R18 in one line:** is each named quantity REALLY the AIF thing it's labelled, or a heuristic wearing the name?

**Method (evidence-first, adversarial).** Each quantity was read in the actual `.clj`/`.py` (verbatim arithmetic,
file:line), matched against the canonical discrete-state-space AIF formulation, and given the WEAKER badge whenever
uncertain. For every candidate `:derived-from-FEP` I first tried to REFUTE it (the proof-mine sweep's 8 hallucinated
shas, 2026-07-03, are why we verify). Prior verified claims from `deep-research-AIF-morphogenesis.md` (2026-06-24) are
reused, not redone.

**Canonical sources.**
- **Da Costa, Parr, Sajid, Veselic, Neacsu, Friston (2020)**, *Active inference on discrete state-spaces: a synthesis*,
  J. Math. Psych. / arXiv:2001.07203 — the reference decomposition: **G(π) = risk + ambiguity**;
  **risk = D_KL[Q(o|π) ‖ C]**; **ambiguity = E_Q(s|π)[H[P(o|s)]]**; epistemic/salience = E[D_KL[Q(s|o,π) ‖ Q(s|π)]];
  policy prior **P(π) = σ(−γ·G(π))**; precision **γ = 1/β** with β updated from the expected G over policies; state
  belief by **marginal message passing** Q(s) = σ(ln Aᵀo + ln B…).
- **Millidge, Tschantz, Buckley (2021)**, *Whence the Expected Free Energy?*, Neural Computation 33(2) — EFE = risk +
  ambiguity, and the epistemic drive must be *added in* (EFE = FEF − EIG); it does not fall out of projecting VFE forward.
- **Friston et al. (2023)**, *Supervised Structure Learning*, arXiv:2311.10300 — **VFE = complexity − accuracy**
  (Bayesian Occam), model expansion accepted only if it raises marginal likelihood ΔF and expected free energy ΔG.
- **Friston, Lin, Frith, Pezzulo, Hobson, Ondobaka (2017)**, *Active inference, curiosity and insight*, Neural
  Computation 29 — the **γ/β precision update** over policies.

## Verdict table

| # | Quantity (code) | Claims to be | Badge | One-line reason |
|---|---|---|---|---|
| 1 | `G-ambiguity` — `efe.clj:38` | EFE ambiguity E[H[P(o\|s)]] | **:principled-approximation** | Σ predicted variance = monotone Gaussian-entropy proxy; self-flagged |
| 2 | `Π` channel precision — `precision.clj:92` | AIF precision (inverse variance) | **:principled-approximation** | `1/max(var,ε)` leg is canonical; `+ need-component` is an affect modulation |
| 3 | belief update — `belief.clj:68` | variational/marginal message passing | **:principled-approximation** | exact categorical Bayes *structure* but a hand-set `(1+w)` likelihood, no A-matrix |
| 4 | `F-free-energy` cascade — `cascade_construct.py:214` | VFE accuracy − complexity | **:principled-approximation** | complexity = Σ−log P(include) genuine; accuracy = coverage-sum, not a log-likelihood |
| 5 | coverage→`ΔG` — `fold_eval.clj:30` | rollout EFE G(π) | **:principled-approximation** | rollout accumulator genuine; per-move g = −coverage is a proxy cost |
| 6 | `γ` policy precision — `policy_precision.clj:123` | Friston γ over policies | **:principled-approximation** | plays γ's role + softmax; but learns from realized-perf, not E[G]-over-policies |
| 7 | `τ_eff = τ/γ`, softmax — `policy.clj:35` | P(π)=σ(−γG) | **:principled-approximation** | softmax + −γG faithful; adaptive τ_spread=range(G)/k is a non-canonical layer |
| 8 | `G-total` as "EFE" — `efe.clj:449` | Expected Free Energy | **:analogical** | risk+ambiguity core diluted by 6 hand-weighted heuristic terms in mixed units |
| 9 | `G-risk` — `efe.clj:415` / `free_energy.clj:44` | EFE risk = KL[Q(o)‖C] | **:analogical** | weighted L1 hinge-distance on point estimates; no distribution, no log/KL |
| 10 | `G-info` — `efe.clj:256` | epistemic info-gain (EIG) | **:analogical** | Σmax(0,1−var) = complement of ambiguity, not belief-entropy reduction |
| 11 | `G-goal-outcome` (belly risk) — `c_vector.clj:406` | "CANONICAL EFE … KL" | **:analogical** | mean of (mostly binary) goal divergences; docstring mislabels a non-KL as KL |
| 12 | `G-survival` — `efe.clj:275` | survival pressure (EFE term) | **:analogical** | unweighted hinge on 4 channels = a second G-risk; no canonical EFE counterpart |
| 13 | `G-graph-pragmatic` — `efe.clj:118` | pragmatic value | **:analogical** | `1000·[!applicable] + 3·holes − 20·ascent`; graph heuristic, no log-preference |
| 14 | `G-gap` — `efe.clj:176` | epistemic "room to grow" | **:analogical** | `6.0 × gap-score` table lookup; no expected-information computation |
| 15 | `G-structural-pressure` — `efe.clj:419` | EFE term (weighted) | **:analogical** | pass-through of a caller-supplied scalar × 0.35; not a computed quantity |
| 16 | `policy-performance` ratio — `policy_precision.clj:82` | (γ's learning signal) | **:analogical** | (expected−realized)/(\|·\|+\|·\|+ε); honest control-loop feedback, not variational — but **honestly named**, so no relabeling fault |

**Headline finding.** With the adversarial rule applied, **no quantity earns `:derived-from-FEP` outright** — every
named term carries a documented proxy or deviation. The seven `:principled-approximation`s are the honest,
one-repair-from-canonical core (ambiguity, precision, belief, cascade-F, rollout-ΔG, γ, softmax); the nine
`:analogical`s are heuristics wearing AIF names. The single most acute R18 fault is **#11**: `predictive-goal-outcome-risk`'s
docstring calls itself "the CANONICAL EFE goal-outcome risk … KL" while computing a mean of binary divergences — a
relabel exactly of the kind R18 exists to catch (the ns docstring itself concedes the real KL is a deferred follow-on).

## Per-quantity detail

Each entry: **what it claims · what it actually computes (code:line) · canonical form (cite) · adversarial verdict ·
repair that would raise the badge.**

### Principled approximations (one repair from canonical)

**1. G-ambiguity** — `efe.clj:38-42`, `(reduce + (vals variance-map))`. Computes Σ per-channel predicted variance.
Canonical ambiguity = E_Q(s|π)[H[P(o|s)]] (Da Costa 2020). Gaussian entropy H = ½ln(2πe σ²) is *monotone* in σ², so
Σσ² is a faithful order-preserving proxy — but linear-in-variance, not log-variance, and summed over channels rather
than an expectation under Q(s). The ns docstring (efe.clj:27-32) already names it "a simple Gaussian-flavoured proxy."
**:principled-approximation.** Repair → use ½ln(2πeσ²) under Q(s|π); then `:derived-from-FEP`.

**2. Π channel precision** — `precision.clj:92-115`. `variance-component = 1/max(var, 0.01)` is textbook inverse-variance
precision from a rolling prediction-error window (canonical). It then *adds* `need-component = 5.0 × preference-gap`
(affect-modulated, ported from `ants/aif/affect.clj`) and clamps to [0.1, 200]. The variance leg alone is canonical;
the additive need term is a documented salience modulation, not a variance. **:principled-approximation.** Repair →
gate the need term as a separate salience channel rather than summing it into the precision scalar.

**3. belief update** — `belief.clj:68-87`. `p'(k) = [p(k)·(1+w) if k=t else p(k)] / Σ`. This is *exact* categorical Bayes
updating (the categorical family contains the true posterior, so it is the no-approximation case of variational/marginal
message passing) — but the likelihood `(1+w)` is hand-specified, with no observation model A and no free-energy-gradient
message passing (Da Costa 2020 §belief updating). Update *rule* faithful; likelihood heuristic. **:principled-approximation.**
Repair → derive `(1+w)` from an explicit likelihood matrix A.

**4. F-free-energy (cascade)** — `cascade_construct.py:214`, `free_energy = accuracy − lam*complexity`. **complexity =
Σ −log P(include p)** over chosen patterns (a genuine −log-prior / description-length term, ≈ KL of the selection from
the co-application base-rate prior) — this side is real. **accuracy = Σ ψ-coverage** — a coverage sum, NOT a model
log-likelihood E_Q[ln P(o|s)]. λ=0.25 is a fitted "F=0 knee," not a canonical unit. Correct Occam SHAPE (Friston 2023,
VFE=complexity−accuracy), half-genuine. **This is a real UPGRADE from the retired Salingaros `L=T·H` analogy** (the
2026-06-24 headline gap) — that `:analogical` has already been repaired toward here. **:principled-approximation.**
Repair → make accuracy an actual expected log-likelihood.

**5. coverage→ΔG** — `fold_eval.clj:30-37`. `coverage = folded/(folded+holes)`; ΔG = `(:G (rollout/project-policy … [{:delta-g (− coverage)}]))`.
The rollout accumulator `project-policy` is the genuine forward-looking EFE ledger (confirmed grounded in the prior
deep-research, grain-3). The per-move local cost fed in, `g = −coverage`, is a proxy for "this discharge reduces EFE."
Machinery faithful, input heuristic. **:principled-approximation.** Repair → derive the move's g from actual ΔF/evidence,
not a coverage fraction.

**6. γ policy precision** — `policy_precision.clj:113-159`. `γ = clamp(2^(gain·perf̄), 0.5, 2.0)`, perf̄ = windowed mean of a
realized-vs-expected performance ratio (§16). Canonical γ=1/β is updated from the **expected free energy G over the
policy set** (prospective; Da Costa 2020, Friston 2017). The code substitutes a **retrospective realized-outcome control
loop** — a different estimator, not an approximation of the β-update — but γ genuinely plays its role (bounded inverse
selection-temperature over policies, entering selection as exp(−γG/τ), reduction-safe at γ=1). **:principled-approximation**
(borderline; the *estimator* is a substitution, flagged). Repair → also fold the prospective G-spread-over-policies into
γ's prior, making the realized loop a correction rather than the whole signal.

**7. τ_eff = τ/γ + softmax** — `policy.clj:35-64`. `P(a) ∝ exp(−G(a)/τ_eff)`, `τ_eff = τ_spread/γ`, `τ_spread = range(G)/k`
(k=5). The softmax over −γ·G and its direction are exactly canonical P(π)=σ(−γG). The extra `τ_spread = range(G)/k`
adaptive-temperature layer (scale-invariant sharpness) has no canonical counterpart. **:principled-approximation.**
Repair → let γ carry the sharpness alone (canonical) and treat spread-normalisation as an explicit, separately-justified
calibration.

### Analogical (heuristics wearing the name)

**8. G-total as "Expected Free Energy"** — `efe.clj:449-457`. A linear sum
`G-risk + G-ambig − 0.4·G-info + 1.2·G-survival − 0.35·G-struct + G-graph-pragmatic − G-gap + G-goal-outcome`. Canonical
EFE = risk + ambiguity (two terms, both in nats, unit-commensurate). Here two semi-canonical legs are blended with six
heuristic terms in **incommensurate units** at hand-set weights. Calling the blend "Expected Free Energy" is the aggregate
R18 fault. **:analogical.** Repair → expose the canonical G = risk+ambiguity separately; label the augmented sum an
"EFE-shaped multi-objective action score," not EFE.

**9. G-risk** — `efe.clj:415`, `free_energy.clj:44-58`. `Σ_ch weight_ch · max(0, lo−v, v−hi)` on the predicted point-estimate
mean, minus intrinsic-value, × urgency. Canonical risk = D_KL[Q(o|π) ‖ C] over an outcome *distribution* (Da Costa 2020).
The code is a weighted L1 hinge distance on point estimates — monotone-aligned with the pragmatic drive but no distribution,
no log, no KL. **:analogical.** Repair → predict an outcome distribution Q(o|π) and compute KL against a normalised
preference C (same repair as #11).

**10. G-info** — `efe.clj:256-273`. `Σ max(0, 1 − variance)`. Canonical epistemic value = E[D_KL[Q(s|o,π) ‖ Q(s|π)]]
(expected reduction of belief entropy). The code is the per-channel complement of the *observation* variance — literally
anti-correlated by construction with G-ambiguity (same `next-var` vector), so it double-counts that signal and computes no
belief-entropy reduction at all. The prior audit already flagged EIG as an omission. **:analogical.** Repair → compute real
expected information gain over beliefs (as the futon3c portfolio sibling does with a genuine EIG).

**11. G-goal-outcome / belly `predictive-goal-outcome-risk`** — `c_vector.clj:406-440`. Mean over open C-entries of a
residual risk: advanced entries contribute `(1−p)·risk-of`, others `risk-of`, ÷ fixed open-count; `risk-of = entry-weight ·
divergence`, and `divergence` for the dominant `:becomes` op is a **binary 0/1** goal-met indicator (range ops → fractional
shortfall). Under the default learner `p=1`, so it's a deterministic flip, not an expectation. Canonical risk = KL[Q(o|π)‖C].
The function docstring calls it "the CANONICAL EFE goal-outcome risk … (R19-KL)" — but it is a mean of binary divergences,
not a KL; the *ns* docstring candidly admits the real KL term is the deferred W1 follow-on (§11 / D4). **:analogical — and the
sharpest relabel in the apparatus.** Repair → the named W1: predict a goal-outcome distribution under π and compute KL
against a normalised C-vector.

**12. G-survival** — `efe.clj:275-304`. `Σ max(0, gap)` over exactly 4 channels, unweighted, × 1.2. Same hinge form as
G-risk on a channel subset; there is no "survival" term in canonical EFE. Best honest reading: a homeostatic/allostatic
pressure toward preferred states (a second preference-distance term). **:analogical** (futon-native, no canonical
counterpart). Repair → fold into risk, or name it "homeostatic pressure" and drop the EFE framing.

**13. G-graph-pragmatic** — `efe.clj:118-159`. `1000·[not-applicable] + 3·body-size − 20·ascent-progress`
(ascent = Σ 1/(1+depth) over goal-relevant produced capabilities). Graph heuristic: a hard applicability gate + hole-count
penalty − discounted goal-relevance count. Canonical pragmatic value = E[ln C(o)]. The ascent leg has a legitimate
pragmatic reading (progress toward preferred states); the 1000-magnitude gate is a hard *constraint*, not EFE.
**:analogical.** Repair → split: applicability = a feasibility mask; ascent = a pragmatic proxy toward C.

**14. G-gap** — `efe.clj:176-192`. `6.0 × gap-score`, a caller-supplied [0,1] lookup per mission. Framed as "epistemic room
to grow" but computes no expected information. **:analogical.** Repair → make it the expected uncertainty-reduction from
filling the gap (a real epistemic term).

**15. G-structural-pressure** — `efe.clj:419`. `(double (or (:structural-pressure-per-action action) 0.0))` × 0.35 — a
pass-through of a caller-injected scalar. Not a computed quantity. **:analogical.** Repair → derive from the generative
model (e.g. as novelty/salience), or rename to mark it an exogenous heuristic weight.

**16. policy-performance ratio** — `policy_precision.clj:82-96`. `(expected−realized)/(|expected|+|realized|+ε) ∈ [−1,1]`,
signed + scale-free. Not a variational quantity; it is a control-loop feedback signal (the substituted estimator behind
γ, #6). **:analogical as an AIF quantity — but honestly named** ("policy-performance", not an AIF term), so it carries **no
R18 relabeling fault**. Repair (if it is to feed γ canonically) → replace/augment with the E[G]-over-policies signal.

## Reused prior claims (deep-research-AIF-morphogenesis.md, 2026-06-24)

- **ΔF (VFE = accuracy − complexity)** — the prior audit called it genuine; this closer read *refines* that to
  `:principled-approximation` for the cascade `F` (#4): the complexity leg is a real −log-prior, but the accuracy leg is a
  coverage sum, not a log-likelihood. (More adversarial than the first pass — the point of R18.)
- **Salingaros L = T·H ↔ free energy** — `:analogical`, "the biggest genuine gap … analogical and unbuilt" (line 70/96).
  Now **retired in code**: `cascade_construct.py` replaced the `C`/wholeness key with `F = accuracy − λ·complexity` (#4).
  The badge moved analogical → principled-approximation by construction. `E-prove-salingaros-cascade-scorer` remains the
  open theorem (is L=T·H recoverable as accuracy×(−complexity)?).
- **rollout G(π)** — grounded EFE (grain-3); reused for #5 (the accumulator is faithful; the coverage input is the proxy).

## Green path (what turns R18 from yellow to green)

1. **Render the badges** — the follow-on build: the explainer + any readout reads `data/r18-badges.edn` and paints each
   node with its badge (green `:derived-from-FEP` · amber `:principled-approximation` · red `:analogical`), so the labels
   on the wiring diagram are trustworthy at a glance. That wiring is what flips R18 partial → done.
2. **Discharge the two sharpest faults toward canonical** — #11 (the belly's "KL" that isn't) via the named W1 predictive
   KL, and #10 (G-info) via a real EIG. Both have existing siblings to copy (the futon3c portfolio surface computes a real
   EIG). Each repair upgrades a badge and is independently checkable.
3. **Stop calling the blend "Expected Free Energy"** (#8) — expose canonical G = risk + ambiguity as a first-class value and
   demote the 6 augmentation terms to a named multi-objective layer. This is a rename + a struct split, no new math.
