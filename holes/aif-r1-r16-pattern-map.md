# R1–R16 as 16 patterns — with honest status (the "half-way" exhibit)

**Purpose (for the writeup rethink):** each AIF completeness requirement R1–R16
(`futon2/docs/futon-aif-completeness.md`) stated as a *pattern*, mapped to an existing
`futon3/library/aif/*.flexiarg` where one exists, and — the point — annotated with its
**honest status in our implementation**. The value is precisely that the implementation is
*half-way*: the map lets the paper say **"don't confuse THIS with a real working AIF
implementation"** and show exactly where the seam is.

**Status legend:** ✓ real (built + load-bearing) · ◐ half (present but partial / apparatus-only /
starved) · **◐→ built (dark)** = code exists + reviewed, flag-gated OFF, awaiting an operator arm
(built but not yet load-bearing) · ○ aspirational (claimed or N/A, not actually there). **The sweep
this session moved most R10–R16 rows from ◐/○ to ◐→ built (dark)** — every code box now exists;
arming is the operator surface between built and live.

**The shape of the honesty:** R1–R9 (the *deliberation* core) are largely ✓. R10–R16 (the
*loop* — live operation, hierarchy, accumulation, policy discrimination, precision-with-real-
variance, closure) were where the loop degraded to ○; **this session they were BUILT (dark)** —
every code box now exists, flag-gated, arms pending (only R10 live-operation and R11 multi-agent
stay ○). **R16 is the exhibit** — it was announced *closed* and was in fact a mirror; it is now
grounded (actuation + closure built dark), which is a very different claim from *live*.

| R | requirement | pattern (`library/aif/…`) | status | the honest reality |
|---|---|---|---|---|
| **R1** | Explicit belief state | `belief-state-operational-hypotheses` | ◐→ **easy half done** | Disaggregated (`3347575`): doc's "4/14" is STALE — **8** channels have likelihood models; the other 6 are **N/A-by-design** (externally-measured, no per-entity belief substrate — a fake mapping would be needed). So channel coverage is COMPLETE for the belief-derivable half. |
| **R2** | Observation channel schema | `structured-observation-vector` | ✓ | 13 harmonized channels. Real. |
| **R3** | Predictive-coding belief update | `predictive-coding-belief-update`, `status-gated-belief-update`, `decomposed-prediction-noise` | ◐→ **hard box BUILT (dark)** | R3a–d wired for 8 channels ✓. The hard half — R3d sign-aggregation — is now **WIRED** (`1d330fa`): `*r3d-multichannel?*` flag (default OFF, byte-identical) → 8-channel **signed precision-weighted average** `Σ(sign·prec·err)/Σ(prec)`. Tested (58/1330): the 2026-05-18 case aggregates to **+0.006** (coherent) vs naive **−0.22** (cancels) — the fix works. Residual: only the **fine per-channel** per-entity attribution (research-level — global channels have no per-entity decomposition) + the flip-on; the **coarse per-entity attribution is already BUILT + LIVE** in the judge (`war_machine.clj:3806-3833`, `e5be48f`). No unbuilt code. |
| **R4** | Predictive forward model | `shared-kernel-predictive-forward-model` | ✓ | Shape is real. |
| **R5** | EFE with ≥2 principled terms | `expected-free-energy-scorecard`, `belief-aware-risk-term`, `predictive-entropy-as-ambiguity` | ◐→ **EIG wired (BUILT dark)** | Disaggregated (`b28ebf3`): NOT ◐-in-the-middle. **nats-risk** (`:kl`, the hard part, arena-flipped 2026-07-04) ✓ · **nats-ambiguity** (`:gaussian-entropy`, built/validated/provenance-ready, library-default dark) ✓ · **EIG** = the open third — now **BUILT (dark, `25b1b8f`, codex-2/claude-5)**: `{constellation→stddev} → mission-EIG → −λ·EIG` at pattern grain (194 missions resolve to non-zero EIG). `G = risk + ambiguity − EIG` has all three legs; the hard part of R5 is done. Flag-gated `:pattern-grain-eig?` (default OFF; arm = Joe). Caveat: the EIG is the coarse ~1/√evidence signal (not fine-grained). |
| **R6** | Softmax selection with abstain | `candidate-pattern-action-space` | ✓ | Real. |
| **R7** | Adaptive precision | `evidence-precision-registry`, `two-layer-calibration`, `measurement-window-hygiene` | ✓ | Π = 1/max(var, ε). Real. |
| **R8** | Per-tick trace | `free-energy-as-tick-scalar` | ✓ | Persisted trace. Real. |
| **R9** | Named validation properties | `no-self-certification`, `off-continuity-null-discriminates` | ✓ | Real. |
| **R10** | Live operation | `scheduled-observer-entrypoint` ✎new | ○ | Scheduled-execution-*ready*, but the deliberation cron was **DISABLED** (2026-07-06, cached no-op runs). It is not actually running. |
| **R11** | Hierarchical / multi-agent composition | `hierarchical-budget-aware-action-selection` | ○ | N/A in the WM. The Agency (futon3c) is multi-agent but **not wired as an AIF hierarchy**. No coordination layer. |
| **R12** | Dual-loop / hyperparameter inference | `two-layer-calibration` | ◐→ **FILLED (honest)** | The apparatus exists, and the accumulation now RESOLVES at **pattern grain** (claude-5, verified): the pattern co-application corpus is dense-real, so structure learning is real — but via **embedding-clustering + BMR-EIG**, NOT naive BMR merges. Naive count-BMR **over-merges** at 94-dim (all 6903 pairs accept, collapse to 1 — even disjoint patterns). Concepts = Joe's semilattice **constellations** (embedding); BMR contributes the **EIG** layer {constellation→stddev}; count-BMR corroborates weakly (within>across by 1.55 ΔF), insufficient alone. |
| **R13** | Policy adequacy (multi-step G(π)) | `hierarchical-budget-aware-action-selection` (+ `futon2.aif.rollout`) | ◐→ **G-ties BROKEN (dark)** | Rollout witness passes (2-step beats greedy), but live-ranking integration is partial, acting is HELD, and the **G-ties false-floor** left ranks 2+ tied at 2.09. **FIXED (BUILT dark, `25b1b8f`): the pattern-grain EIG wire (`− λ·EIG`) BREAKS the tie — two missions tied at G-total 0.543 separate to 0.39445 vs 0.41766 (higher EIG → lower G → explores). Discrimination proof holds (≥1 real tie separated; minimal by design). Flag-gated `:pattern-grain-eig?`, arm = Joe.** |
| **R14** | Precision over policies (γ) | `policy-precision-commitment-temperature` | ◐→ **live-wire BUILT (dark)** | The grounding half is done and the **live-wire migration is now coded** (`d36086f`): `*gamma-grounded-feed?*` (default OFF, byte-identical) routes γ's realized feed to the A5 **substrate dial** instead of coverage-ΔG. What remains is **not code** — the two operator arms (`*live-wire?*`, then `*gamma-grounded-feed?*`) + real fold-variance data. γ stays starved of variance until armed. |
| **R15** | Hierarchical / temporal depth | `temporal-depth-beyond-greedy` ✎new | ◐→ **hierarchy SLICE built** | depth-1 apparatus ✓; horizon-H + temporal discount ✓ (`056dcc5`); and the fast/slow **hierarchy now has a built scoped slice** (`32b9429`, `futon2.aif.temporal-hierarchy`): a slow-loop strategic mode parameterizes the fast loop's priors + costs (−ln(weight) KL penalty), flipping the fast-loop decision (exploitation→close-fast, exploration→advance-cap) **horizon-independently** — proved compositional, not sequential depth (`hierarchy-is-not-rollout-depth`). Remainder (research-level): full nested generative model — slow-loop EFE, learned transitions, multi-level. |
| **R16** | Closed action–perception loop | `grounded-actuation-not-reobservation` ✎new | ○→◐→ **built dark** | **THE EXHIBIT.** Announced *closed* 2026-07-02; was **artifact-only** — the executor re-observed the diagram it had just built and called the coverage-ΔG "realized." This session grounded the *actuation* half (provable substrate witness + CLean structure check + build-match), moving it ○→◐. The *closure* half — re-observation changing the next decision — needed R12 + R13, and **both are now BUILT (dark) this session**: R12 filled (embedding + BMR-EIG), R13's G-ties broken by the EIG wire (`25b1b8f`), plus the A6 re-rank (discharge → EIG → re-rank) and an **operational witness** (a `core.logic` relation proving discharge inverts the ranking, `false` on a mirror). So the closure is **built dark** too — what remains is the operator arms, not code. **R16: ○→◐→ built dark (arms pending).** |

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
same table** — it is the *mechanism that has now MOVED* four of the ◐/○ rows above from *dreamed* to
**built (dark)**: R12 (filled), R13 (G-ties broken), R5 (EIG wired), R14 (γ variance leg) — all
flag-gated, arms pending. Written as **design patterns** (the design was proposable before the wiring
landed), with honest, **now-resolved** status (see the honest-status note below — the pattern-grain
run reframed this section, and `25b1b8f` wired it).

| R17 asset | pattern (`library/aif/…`) | status | what it targets above |
|---|---|---|---|
| Structure Learning by Bayesian Model Reduction | `structure-learning-by-model-reduction` ✎new | ◐ **reframed (pattern grain)** | naive count-BMR MISCALIBRATED in high-dim (over-merges → collapse-to-1); structure comes from **embedding-clustering** (constellations), count-BMR corroborates weakly. Targets **R12** ✓, **R13** (ranker-wire BUILT dark, `25b1b8f`) |
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
(the constellations are embedding-derived, semi-authored by Joe's semilattice). **This recovery
ceiling is now EVIDENCED, not asserted:** the one candidate coarser-merge (the "AIF-12" cluster
spanning C2+C8+C1, from cosine-gated BMR) was falsification-tested (claude-5/zai-13) and **rejected**
by two embedding-independent signals — raw mission-Jaccard co-occurrence (within-C2 0.400 vs
cross-boundary 0.163, 2.5× weaker) and live cascade co-application (57.1% vs 8.6%; added members
0/10). Cosine adjacency ≠ co-occurrence ("titles AIF-wash"). The constellation granularity is
**vindicated** — the clustering was right to split them. **Methodological payoff (this sharpens the
thesis):** cosine-gated BMR *over-merges*; the right method is embedding + co-occurrence *jointly* —
which is exactly what Joe's constellations already are (embedding + citation-overlap). So "AIF and
Embeddings" is not "embedding replaces co-occurrence" but "the two together; cosine alone over-merges."

**BUILT (dark, `25b1b8f`, codex-2/claude-5, 2026-07-08):** {constellation → EIG} is wired into the ranker
at pattern grain — claude-4's grain-agnostic `:eig-fn` (seam `(eig-fn mission-id mission)`, `d3533ed`)
+ claude-5's `eig-for-produces` re-keyed to constellations. Inhabits the R5-EIG / R13-signal boxes: the
EFE term now carries real epistemic value and the G-ties floor is broken (≥1 tie separated). Flag-gated
`:pattern-grain-eig?` (default OFF; arm = Joe). Caveat: coarse ~1/√evidence EIG, minimal-discrimination proof.

## ◐ → box-pointers (the fine version — `futon6/holes/clean/R*-decomposition.clean.edn`)

Each ◐ requirement is decomposed into a **`clean_argcheck`-valid CLean**; the unbuilt boxes are
typed holes (build-match-checkable). The ◐ stops being a glyph and points at *exactly* what remains —
and it captures statuses the glyph cannot, like R14's **built-but-arm-gated** box or R15's **slice-built, remainder-named** box. (This is the structural
form of the preregistration: the unbuilt boxes are the precise committed remaining work.)

| R | decomposition | unbuilt boxes | the box that remains |
|---|---|---|---|
| **R1** | `R1-decomposition` | **0** | done-for-derivable — the 6 remaining channels are N/A-by-design (out of scope, not holes) |
| **R3** | `R3-decomposition` | **0** | `sign-aggregation` BUILT (`1d330fa`, dark) + per-entity attribution BUILT+LIVE (`e5be48f`); only fine per-channel (research-level) + the flip remain — no unbuilt code |
| **R5** | `R5-decomposition` | **0** | `EIG` — **BUILT dark** (`25b1b8f`): pattern-grain `{constellation→stddev} → mission-EIG → −λ·EIG`; flag-gated `:pattern-grain-eig?`, arm = Joe |
| **R12** | `R12-decomposition` | **0** | FILLED — `accumulate` ✓ (pattern corpus exists) + `concept-formation` ✓ (embedding + BMR-EIG); the ranker-wire is R5/R13's box, not R12's |
| **R13** | `R13-decomposition` | **0** | `discrimination` — **BUILT dark** (`25b1b8f`): the pattern-grain EIG breaks the G-ties floor (≥1 tie separated); flag-gated, arm = Joe |
| **R14** | `R14-decomposition` | **1** (+arm) | `live-wire` **BUILT dark** (`d36086f`); only `accumulate` (a data dependency) + the operator arms remain — no unbuilt code |
| **R15** | `R15-decomposition` | 1 (partial) | `hierarchy` — **SLICE built** (`32b9429`: slow→fast prior/cost parameterization, horizon-independent); remainder = full nested generative model (slow-loop EFE, learned transitions, multi-level) |

**Of the original 7 unbuilt boxes, ALL are now built or filled** (dark, flag-gated): R1
done-for-derivable; R3 `sign-aggregation` (`1d330fa`) + per-entity attribution (`e5be48f`); R5 + R13
`EIG`/`discrimination` (`25b1b8f` — the last box, a real tie separated 0.543 → 0.39445 vs 0.41766);
R12 FILLED (embedding + BMR-EIG); R14 `live-wire` (`d36086f`); R15 hierarchy SLICE (`32b9429`). **So
the entire R1–R16 loop now has ZERO unbuilt code boxes** — every box is built. What remains is (a) the
operator **ARM surface** — the flag flips (`:pattern-grain-eig?`, `*gamma-grounded-feed?*`,
`*live-wire?*`, `*r3d-multichannel?*`), each Joe's call; and (b) named research-level remainders (R15
full nested model, R3 fine per-channel attribution). **Built, not live** — the honest distinction: the
loop is fully constructed and dark; arming it is the remaining operator decision, not a coding task.
Per-box caveats stand (the EIG is coarse ~1/√evidence; the R13 discrimination proof is the minimal ≥1
separation).

## The headline for the paper

R1–R9 are a real active-inference **deliberator**. R10–R16 are where the loop was **dreamed** — and
this session **built** (dark): a hierarchy slice, accumulation filled, a policy that now discriminates
(the G-ties floor broken), a γ with a grounded substrate feed, and a closure grounded against an
ungameable witness. But **built is not live**: every box is flag-gated dark, and arming them is the
operator's call (only R10 live-operation and R11 multi-agent remain ○). So the honest sequel is *not*
"we closed the loop" and *not yet* "the loop runs" — it is **"here are 16 named requirements; here is
which ones are real, which are built-but-dark, and which we mistook a reflection for — and here is the
single operator surface (the arms) between built and live."** The half-way-to-armed state is the
contribution.
