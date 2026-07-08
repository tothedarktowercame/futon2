# R1–R16 as 16 patterns — with honest status (the "half-way" exhibit)

**Purpose (for the writeup rethink):** each AIF completeness requirement R1–R16
(`futon2/docs/futon-aif-completeness.md`) stated as a *pattern*, mapped to an existing
`futon3/library/aif/*.flexiarg` where one exists, and — the point — annotated with its
**honest status in our implementation**. The value is precisely that the implementation is
*half-way*: the map lets the paper say **"don't confuse THIS with a real working AIF
implementation"** and show exactly where the seam is.

**Status legend:** ✓ real (built + load-bearing) · ◐ half (present but partial / apparatus-only /
starved) · ○ aspirational (claimed or N/A, not actually there).

**The shape of the honesty:** R1–R9 (the *deliberation* core) are largely ✓. R10–R16 (the
*loop* — live operation, hierarchy, accumulation, policy discrimination, precision-with-real-
variance, closure) degrade from ◐ to ○. **R16 is the exhibit** — it was announced *closed*
and was in fact a mirror.

| R | requirement | pattern (`library/aif/…`) | status | the honest reality |
|---|---|---|---|---|
| **R1** | Explicit belief state | `belief-state-operational-hypotheses` | ◐→ **easy half done** | Disaggregated (`3347575`): doc's "4/14" is STALE — **8** channels have likelihood models; the other 6 are **N/A-by-design** (externally-measured, no per-entity belief substrate — a fake mapping would be needed). So channel coverage is COMPLETE for the belief-derivable half. |
| **R2** | Observation channel schema | `structured-observation-vector` | ✓ | 13 harmonized channels. Real. |
| **R3** | Predictive-coding belief update | `predictive-coding-belief-update`, `status-gated-belief-update`, `decomposed-prediction-noise` | ◐→ **hard box BUILT (dark)** | R3a–d wired for 8 channels ✓. The hard half — R3d sign-aggregation — is now **WIRED** (`1d330fa`): `*r3d-multichannel?*` flag (default OFF, byte-identical) → 8-channel **signed precision-weighted average** `Σ(sign·prec·err)/Σ(prec)`. Tested (58/1330): the 2026-05-18 case aggregates to **+0.006** (coherent) vs naive **−0.22** (cancels) — the fix works. Residual: per-entity attribution + the flip-on decision (still dark). |
| **R4** | Predictive forward model | `shared-kernel-predictive-forward-model` | ✓ | Shape is real. |
| **R5** | EFE with ≥2 principled terms | `expected-free-energy-scorecard`, `belief-aware-risk-term`, `predictive-entropy-as-ambiguity` | ◐ **⅔ (hard third done)** | Disaggregated (`b28ebf3`): NOT ◐-in-the-middle. **nats-risk** (`:kl`, the hard part, arena-flipped 2026-07-04) ✓ · **nats-ambiguity** (`:gaussian-entropy`, built/validated/provenance-ready, library-default dark) ✓ · **EIG** = the open third — now the **pattern-grain ranker-wire, in progress with claude-5** (seam ready: `(eig-fn mission-id mission)`, `d3533ed`). So `G = risk + ambiguity − EIG` is ⅔ built with the hard part done; the EIG third is being wired at pattern grain. |
| **R6** | Softmax selection with abstain | `candidate-pattern-action-space` | ✓ | Real. |
| **R7** | Adaptive precision | `evidence-precision-registry`, `two-layer-calibration`, `measurement-window-hygiene` | ✓ | Π = 1/max(var, ε). Real. |
| **R8** | Per-tick trace | `free-energy-as-tick-scalar` | ✓ | Persisted trace. Real. |
| **R9** | Named validation properties | `no-self-certification`, `off-continuity-null-discriminates` | ✓ | Real. |
| **R10** | Live operation | `scheduled-observer-entrypoint` ✎new | ○ | Scheduled-execution-*ready*, but the deliberation cron was **DISABLED** (2026-07-06, cached no-op runs). It is not actually running. |
| **R11** | Hierarchical / multi-agent composition | `hierarchical-budget-aware-action-selection` | ○ | N/A in the WM. The Agency (futon3c) is multi-agent but **not wired as an AIF hierarchy**. No coordination layer. |
| **R12** | Dual-loop / hyperparameter inference | `two-layer-calibration` | ◐→ **FILLED (honest)** | The apparatus exists, and the accumulation now RESOLVES at **pattern grain** (claude-5, verified): the pattern co-application corpus is dense-real, so structure learning is real — but via **embedding-clustering + BMR-EIG**, NOT naive BMR merges. Naive count-BMR **over-merges** at 94-dim (all 6903 pairs accept, collapse to 1 — even disjoint patterns). Concepts = Joe's semilattice **constellations** (embedding); BMR contributes the **EIG** layer {constellation→stddev}; count-BMR corroborates weakly (within>across by 1.55 ΔF), insufficient alone. |
| **R13** | Policy adequacy (multi-step G(π)) | `hierarchical-budget-aware-action-selection` (+ `futon2.aif.rollout`) | ◐ | Rollout witness passes (2-step beats greedy), but live-ranking integration is partial, acting is HELD, and the **G-ties false-floor** leaves ranks 2+ tied at 2.09 — the policy doesn't actually discriminate. **The fix = the pattern-grain EIG wire (`− λ·EIG` breaks the tie), now in progress with claude-5 (seam `d3533ed`).** |
| **R14** | Precision over policies (γ) | `policy-precision-commitment-temperature` | ◐→ **live-wire BUILT (dark)** | The grounding half is done and the **live-wire migration is now coded** (`d36086f`): `*gamma-grounded-feed?*` (default OFF, byte-identical) routes γ's realized feed to the A5 **substrate dial** instead of coverage-ΔG. What remains is **not code** — the two operator arms (`*live-wire?*`, then `*gamma-grounded-feed?*`) + real fold-variance data. γ stays starved of variance until armed. |
| **R15** | Hierarchical / temporal depth | `temporal-depth-beyond-greedy` ✎new | ◐→ **hierarchy SLICE built** | depth-1 apparatus ✓; horizon-H + temporal discount ✓ (`056dcc5`); and the fast/slow **hierarchy now has a built scoped slice** (`32b9429`, `futon2.aif.temporal-hierarchy`): a slow-loop strategic mode parameterizes the fast loop's priors + costs (−ln(weight) KL penalty), flipping the fast-loop decision (exploitation→close-fast, exploration→advance-cap) **horizon-independently** — proved compositional, not sequential depth (`hierarchy-is-not-rollout-depth`). Remainder (research-level): full nested generative model — slow-loop EFE, learned transitions, multi-level. |
| **R16** | Closed action–perception loop | `grounded-actuation-not-reobservation` ✎new | ○→◐ | **THE EXHIBIT.** Announced *closed* 2026-07-02; was **artifact-only** — the executor re-observed the diagram it had just built and called the coverage-ΔG "realized." This session grounded the *actuation* half (provable substrate witness + CLean structure check + build-match), moving it ○→◐. The *closure* half — re-observation changing the next decision — is still deferred (it needs R12 + R13). |

## The three new patterns (now written, `library/aif/`)

1. **R10 `scheduled-observer-entrypoint.flexiarg`** — a live cron-driven observer, and the honest
   anti-pattern it exposes: *a scheduled no-op is not live operation* (`:tick/fired?` ≠
   `:tick/moved-substrate?`). We disabled ours 2026-07-06.
2. **R15 `temporal-depth-beyond-greedy.flexiarg`** — planning over a horizon + a fast/slow
   timescale hierarchy; depth-1 rollout is reactive control wearing planning's clothes.
3. **R16 `grounded-actuation-not-reobservation.flexiarg`** — the load-bearing pattern, and the whole
   reason the writeup is instructive:
   > **IF** you want a closed action–perception loop, **HOWEVER** re-running your own constructor
   > and measuring how faithfully it reproduced its own diagram is a *mirror*, not an act — the
   > dial cannot move — **THEN** the loop closes only when the act writes an **ungameable witness
   > to a substrate outside the model** (a typed endpoint provably inhabited; a build that matches
   > its CLean structure spec) **AND** that witness feeds the next decision, **BECAUSE** "honesty
   > about a quantity that cannot move is still inertia."

## R17 — Structure Learning: the sequel mechanism (in design)

R1–R16 above is the deliberator and its gaps. **R17 (structure learning) is not a 17th row of the
same table** — it is the *mechanism now being built* to move four of the ◐/○ rows above from
*dreamed* to *real*. Written as **design patterns** (the design is proposable before the wiring
lands), with honest, **now partly-resolved** status (see the honest-status note below — the
pattern-grain run reframed this section).

| R17 asset | pattern (`library/aif/…`) | status | what it targets above |
|---|---|---|---|
| Structure Learning by Bayesian Model Reduction | `structure-learning-by-model-reduction` ✎new | ◐ **reframed (pattern grain)** | naive count-BMR MISCALIBRATED in high-dim (over-merges → collapse-to-1); structure comes from **embedding-clustering** (constellations), count-BMR corroborates weakly. Targets **R12** ✓, **R13** (ranker-wire in progress, claude-5) |
| Posterior variance as **epistemic value (EIG)** | `posterior-variance-as-epistemic-value` ✎new | ✓ **computed** | BMR-EIG {constellation→stddev} real (∝1/√α0; singletons 0.029 → P2/AIF 0.009) — the epistemic layer. Feeds **R13**/**R5** (`− λ·EIG`) once wired to the pattern-grain ranker; **R14** γ variance |

**A4a is the R17 node** in the actuator (`futon2/holes/aif-wiring-actuator.html` +
`M-a4a-structure-node.md`). It reads accumulated `:capability/*` counts, runs BMR concept-formation
offline, and splits downstream: a `{concept → stddev}` **epistemic-value (EIG) leg** → the forward
ranker (3a — subtracted, `− λ·EIG`; breaks the **R13** G-ties floor) and later A5's γ leg (3b —
**R14** variance, magnitude), and a **candidate edge → A1** (the **R16** closure half, the
re-ranking that R16 says "needs R12 + R13").

**Honest status — the result, and it reframes R17.** claude-5's pattern-grain run (zai-13 +
independent verification) settles it: **naive count-BMR is miscalibrated in high-dim** — at 94-dim
all 6903 co-application pairs accept (ΔF −6.5..−33.7) and collapse to a single concept, even disjoint
patterns (the complexity term inflates with outcome-dimension). So "BMR merges fire" is **false at
scale**. What IS real: the concepts are Joe's semilattice **constellations** (P0–P17, embedding +
citation-overlap), **recovered** by embedding-gated BMR (cos ≥ 0.7, cutoff −20) matching AIF /
math-proof / engineering; and BMR contributes the **epistemic-value (EIG) layer** the constellations
lacked — {constellation → posterior stddev}, ∝ 1/√α0, singletons 0.029 (most uncertain) → P2/AIF
0.009 (most settled) — plus weak corroboration (within-constellation pairs 1.55 ΔF stronger than
across; hub patterns bridge and weaken it). **This is the "AIF and Embeddings" thesis, empirically:
the metric space makes structure learning tractable where pure BMR-over-counts collapses.** The
honest verb is *structure-recovery + epistemic-quantification*, not *structure-learned-from-scratch*
(the constellations are embedding-derived, semi-authored by Joe's semilattice). **In progress
(control passed to claude-5, 2026-07-08):** wiring {constellation → EIG} into the ranker at pattern
grain — claude-4's grain-agnostic `:eig-fn` (seam widened to `(eig-fn mission-id mission)`, `d3533ed`)
+ claude-5's `eig-for-produces` re-keyed to constellations. Inhabits the R5-EIG / R13-signal boxes.

## ◐ → box-pointers (the fine version — `futon6/holes/clean/R*-decomposition.clean.edn`)

Each ◐ requirement is decomposed into a **`clean_argcheck`-valid CLean**; the unbuilt boxes are
typed holes (build-match-checkable). The ◐ stops being a glyph and points at *exactly* what remains —
and it captures statuses the glyph cannot, like R13's **built-but-inert** box. (This is the structural
form of the preregistration: the unbuilt boxes are the precise committed remaining work.)

| R | decomposition | unbuilt boxes | the box that remains |
|---|---|---|---|
| **R1** | `R1-decomposition` | **0** | done-for-derivable — the 6 remaining channels are N/A-by-design (out of scope, not holes) |
| **R3** | `R3-decomposition` | 1 | `sign-aggregation` — approved design, DARK; flag-gated live wiring remains |
| **R5** | `R5-decomposition` | 1 | `EIG` — the pattern-grain ranker-wire, **in progress (claude-5)**; seam ready `d3533ed` |
| **R12** | `R12-decomposition` | **0** | FILLED — `accumulate` ✓ (pattern corpus exists) + `concept-formation` ✓ (embedding + BMR-EIG); the ranker-wire is R5/R13's box, not R12's |
| **R13** | `R13-decomposition` | 1 | `discrimination` — **built (3a) but INERT** until the pattern-grain EIG signal wires in (**in progress, claude-5**) |
| **R14** | `R14-decomposition` | **1** (+arm) | `live-wire` **BUILT dark** (`d36086f`); only `accumulate` (a data dependency) + the operator arms remain — no unbuilt code |
| **R15** | `R15-decomposition` | 1 (partial) | `hierarchy` — **SLICE built** (`32b9429`: slow→fast prior/cost parameterization, horizon-independent); remainder = full nested generative model (slow-loop EFE, learned transitions, multi-level) |

**Of the original 7 unbuilt boxes: 2 are now BUILT dark** (R3 `sign-aggregation` `ced3aec`, R14
`live-wire` `d36086f`, both flag-gated default-OFF), **R12 is FILLED** (embedding-clustering +
BMR-EIG at pattern grain — see R17 below), R13 `discrimination` is built-but-inert, and **R15's
hierarchy SLICE is built** (`32b9429`; only its research-level nested-model remainder is open). The
one remaining **unbuilt code box is R5-EIG**. And **R5-EIG + R13-signal converge on one piece —
now IN PROGRESS with claude-5** (control passed 2026-07-08): wiring {constellation → EIG} into the
pattern-grain ranker. The grain-agnostic `:eig-fn` seam is built + widened for pattern grain
(`(eig-fn mission-id mission)`, `d3533ed`); claude-5 builds `eig-for-produces` re-keyed to
constellations + injects it.

## The headline for the paper

R1–R9 are a real active-inference **deliberator**. R10–R16 are where the loop was **dreamed**:
live operation disabled, no hierarchy, no accumulation, a policy that doesn't discriminate, a γ
starved of variance, and a closure that was a mirror. The honest sequel is *not* "we closed the
loop" — it is **"here are 16 named requirements; here is which ones are real, which are half, and
which we mistook a reflection for."** The half-way state is the contribution.
