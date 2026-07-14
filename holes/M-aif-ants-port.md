# M-aif-ants-port — Port the modern AIF (R1–R19) into the ant forager

**Status:** SPEC / DERIVE (2026-07-14). Owner: Claude. Build: staged Zai handoffs, Claude reviews each.
**Context:** [[project_m_sci_reproduction_complete]] cyberants post-mortem. This is **Port 1** of a
two-port plan (Port 2 = AIF-as-tokamak-controller for MetaCAs, blocked on this).

## Why this exists (the reframe)

The cyberant "domain transfer" result did not measure what it claimed. Three ground-truth findings
(all verified in code, 2026-07-14):

1. **The transferred wiring was never operative.** `futon2/src/ants/cyber.clj:204` (`config->aif-delta`)
   merges **only** `:precision` (`:Pi-o`, `:tau`) into the live ant. `:pattern-sense`, `:adapt-config`,
   `:switch-to`, `:policy-priors` are stored under `:cyber-pattern → :config` via `select-keys` and
   **never read** (only `:id` + `:ticks-active` are consumed anywhere). The `random-wiring` control only
   permutes those inert fields, so it is **operationally byte-identical to L5** — the r03 "null" was a
   tautology, not evidence.
2. **The baseline was degenerate + the harness confounded.** `sigil-gradient` starves 0.0/30-30; the
   `土` config has `trail-follow 0, gradient-use 0` (a blind wanderer). Patchy food uses a **fixed seed
   (42)** (`war.clj:136`), so 30 "unseeded" runs are one board replayed; `:cyber` always spawns left
   `[2,2]`, `:cyber-sigil` right `[w-3,h-3]` → a spawn-position artifact on symmetric-vs-patchy food.
3. **"Classic ants" were never in the experiment.** `compare.clj` runs `:cyber` vs `:cyber-sigil` (both
   cyberants). A real `:classic` brain exists (`war.clj:212`) but was not in the loop.

**The deep point.** In a properly specified AIF, *explore-vs-exploit gating is the ambiguity/epistemic
term of G, not a bolted-on switch.* An agent minimizing `G_efe = KL(Q(o|π)‖C) + E_Q(s|π) H[P(o|s)]`
explores exactly when resolving uncertainty about where reward is lowers expected surprise. In
patchy/sparse food the info-gain term dominates on its own. So the cyberant's *intended* behavior
(explore-when-patchy) should fall out of a real AIF forager **for free, from first principles, and
auditably** — no wiring transfer required. This mission builds that forager.

## Hypothesis (pre-registered, plain language)

> A forager whose action selection minimizes the unit-pure `G_efe` (canonical KL-risk + entropy-
> ambiguity) against a survival/preference C-vector will, **without any hand-coded exploration switch**,
> forage robustly in patchy/sparse environments — and an **epistemic-ablated** variant (ambiguity term
> zeroed) will collapse toward the degenerate gradient-follower on patchy/sparse while remaining fine on
> snowdrift. If that dissociation appears, the explore/exploit regulator is *native to the free energy*,
> which is the claim cyberants were groping at.

Two honest outcomes are both informative: (a) the dissociation appears → AIF alone explains the
original effect, cyberants add nothing here; (b) it does not → the AIF forager is missing structure the
CA-derived controller supplied, which sharpens what a cyberant would have to be.

## Faithfulness discipline (non-negotiable — mirrors the paper)

Follow `p4ng/main-2026.tex` §Catalog + Table `tab:aif-retract` exactly:

- **`G_efe` is unit-pure**: `risk = Σ_ch w_ch·KL(N(μ_ch,σ²_ch) ‖ C_ch)` + `ambiguity = Σ_ch ½ln(2πe·σ²_ch)`.
  Nothing else may be called EFE.
- The ant's existing **colony-cost / survival-cost / action-cost** terms (`policy/default-action-costs`,
  `default-colony-config`, `default-survival-config`, `default-efe-lambda`) are **engineering
  augmentation**. They move to a named `controller-score = G_efe + Σ augmentation`, each augmentation
  carrying a one-line typed **residual** ("why this is not EFE"), logged in a retract table in the
  mission doc. Winner-changing ablation, not vibes, establishes that an augmentation is load-bearing.
- Every new quantity gets a faithfulness tag: FEP-derived / principled-approximation / analogical /
  non-FEP-engineering. No green without a test.

## Current state → R-contract map (to be confirmed in Slice 0)

| R | Contract quantity | Ant seam today | Gap |
|---|---|---|---|
| R1 | belief map μ | `core/default-aif-config` `:mu`; `perceive/perceive` | confirm μ is operational-hypothesis map, not raw state |
| R2 | typed obs o | `observe/g-observe`, `observe/sense->vector` (14-ch ABI) | ✅ likely complete |
| R3 | predictive-coding update μ←μ+αΠε + var floor | `perceive/perceive`, `perceive/default-precisions` | confirm form; add variance-floor if missing |
| R4 | one pure forward kernel | **absent** — `war.clj` steps world; ant does not predict with same kernel | **build**: factor pure `(state,action,seed,opts)→next` kernel shared by live-step + prediction |
| R5 | unit-pure `G_efe` | `policy/choose-action` + `pattern_efe/*` (hand-coded per-pattern risk/info-gain) | **replace core** with KL-risk + entropy-ambiguity; demote pattern_efe terms to augmentation |
| R6 | constructed candidate set a | `policy/default-actions`, `actions-by-mode` | ✅ small legible set; keep |
| R7 | per-channel precision Π | `perceive/default-precisions`, `affect/modulate-precisions` | **port** `futon2.aif.precision` (`update-precision-state`, `precision-for`, `salience-for`) |
| R8 | per-tick F scalar | **absent** | **build**: `F = ½ mean_k(Π_k ε_k²)` emitted in trace |
| R13 | policy horizon S(π), H>1 | `policy/choose-action` greedy; `eval-policy` exists | **build**: rollout to H>1 with discount ρ using R4 kernel |
| R14 | commitment temperature τ | `affect/hunger->tau`, `update-tau`, `anneal-tau` | keep; couple softmax to `-G/τ` explicitly; τ from hunger + recent-starvation |
| R19 | preference C-vector | `policy/default-preferences` | promote to explicit C over the obs ABI (fed, reserves↑, cargo-home); risk KL is against **this** |
| R16 | external witness | `war.clj` scores/reserves; `compare.clj` | the **honest harness** below is the witness |
| R17 | BMR structure learning | n/a this mission | out of scope (Port-1 v1) |

## Slice 0 findings (2026-07-14, zai-9, commit 46bb77a — adjudicated by re-check against source)

Ledger: `futon2/holes/labs/M-aif-ants-port/r-map.md`. Corrections that revise the plan below:

- **R5 is worse than the draft said.** `policy/expected-free-energy` (`policy.clj:485-521`) is a **7-term
  hand-shaped sum**; `risk` is a Gaussian-NLL core `½z²` (missing the log-variance term → not a KL),
  `ambiguity` is a Bernoulli variance proxy `v(1-v)/Π` (not `½ln(2πe·σ²)`). No KL or Gaussian entropy is
  computed anywhere in the ant brain. Slice 2 replaces the **core**, not just relabels.
- **Two disjoint C structures + a hidden second scorer.** `default-preferences` (2 Gaussian channels, used
  by `risk-from-preferences`) AND a mode-conditioned linear `C-prior`, the latter injected as `efe-tilt`
  (`policy.clj:78-89`) directly into the softmax logit (`policy.clj:810`), *outside* G. Slice 2/3 must
  **unify the C representation** and either fold `efe-tilt` into the named controller augmentation (with a
  residual) or delete it — no preference route may bypass the audited G.
- **R3 gap is deeper.** There is **no tracked per-channel variance**; `expected-ambiguity` fabricates a
  pseudo-variance from `v(1-v)/Π`. Slice 2 must introduce real per-channel σ² (a prerequisite for both
  KL-risk and entropy-ambiguity), so R3's variance work is *inside* Slice 2, not a footnote.
- **R8 is nearly free.** `F = ½·mean(Π_k·ε_k²)` is already computed (`perceive.clj:162`), just not surfaced
  as a named `:F` in `aif-step` output. Slice 4's F work collapses to a surfacing + trace task.
- **R14 τ confirmed present** (`hunger→tau`, softmax uses `-G/τ` at `policy.clj:810`); **rollout absent**
  (purely 1-step) as expected.

## Decision (2026-07-14, Joe): mode-conditioned C lives in the core

S2 landed a **flat** C and deferred mode-conditioning to "augmentation." Owner ruling: cargo-dependent
home-seeking is a **genuine preference**, so mode-conditioned C folds into the **unit-pure core** (R19),
NOT an engineering augmentation — moving a real FEP quantity out of the audited G would invert the
faithfulness discipline. **Correction (ants "S2b"):** make C mode-indexed (outbound/homebound/maintain);
the *caller* (`policy.clj`) selects the mode-appropriate `c-means`/`c-variances` and passes them to the
**unchanged** domain-agnostic `g-efe` (mode logic must NOT leak into `efe.clj` — the tokamak reuses that
signature verbatim). Restore the 3 regression tests S2 weakened, now as real behavioral assertions.

## The build, staged as Zai handoffs

Dependency order. Each slice is one handoff packet. **Gates every slice must clear** (per `AGENTS.md`):
`clj-kondo --lint` on touched Clojure, `futon4/dev/check-parens.el` on touched files, the named tests
green, and **re-run any claimed PASS** (no self-certification — Zai reports get adjudicated by re-running,
not by trusting the summary; [[feedback_zai_bell_handoff_economics]]). Bell `<owner-id>` back with a
summary + commit shas. Park on the job-id (deadline ≥ 45 min).

### Slice 0 — DERIVE: R-map audit + gap ledger (no product code)
- **Goal:** read `ants.aif.{observe,perceive,core,policy,pattern_efe,affect,default_mode,pattern_sense}`
  and produce `futon2/holes/labs/M-aif-ants-port/r-map.md`: for each R1–R19, the exact current form
  (function + line), verbatim, and the precise gap. Confirm/refute the table above. Flag any place the
  ant already computes a KL or an entropy.
- **Files:** read-only over `src/ants/aif/*`; write the ledger.
- **Acceptance:** every row cites a real seam or says "absent"; no claim without a line reference.
- **Gate:** doc only; reviewer spot-checks 4 rows against source.

### Slice 1 — R4: pure forward kernel
- **Goal:** factor the ant world-step into a pure `ant-kernel (state action seed opts) → next-state`
  (deterministic; food-gather, pheromone-decay, move) with `war.clj` live-step as a thin wrapper calling
  it. Prediction path calls the **same** kernel + principled noise.
- **Files:** `src/ants/war.clj` (extract), new `src/ants/aif/forward.clj`.
- **Acceptance:** golden test — live-step trajectory == kernel trajectory for a fixed seed over 50 ticks
  (bit-identical); a `forward-predict` produces a distribution whose mean tracks the kernel.
- **Gate:** `test/ants/aif/forward_test.clj` green; kondo; parens. Faithfulness tag: FEP-derived (R4).

### Slice 2 — R19 + R5 core: explicit C-vector and unit-pure G_efe
- **Goal:** (a) promote `policy/default-preferences` to an explicit C over the obs ABI; (b) implement
  `g-efe {:risk-mode :kl :ambiguity-mode :gaussian-entropy}` mirroring `futon2.aif.efe` risk/ambiguity
  math (reuse the formulas; the mission wrapper in `efe.clj` is WM-coupled, so port the two kernels, do
  not import the graph controller). Keep it **pure**: `(belief, predicted-obs-dist, C, Π) → {:risk :ambiguity :g-efe}`.
- **Files:** new `src/ants/aif/efe.clj`; `src/ants/aif/policy.clj` (wire in).
- **Acceptance:** unit tests pinning risk=KL and ambiguity=entropy on hand-checked Gaussians; `g-efe`
  matches `futon2.aif.efe` on a shared fixture to 1e-9.
- **Gate:** `test/ants/aif/efe_test.clj`; kondo; parens. Tag: FEP-derived (R5).

#### Slice 2 retract table (2026-07-14, zai-9)

The following terms were demoted from the old 7-term hand-shaped G to **named
controller augmentation** with typed residuals. Only `risk + ambiguity` may be
called EFE. Each augmentation is tagged with its faithfulness class and a
one-line residual explaining why it is not EFE.

| Term | Old role in G | New role | Faithfulness tag | Residual (why this is not EFE) |
|---|---|---|---|---|
| risk (KL) | `λ_prag · ½·z²` (2-channel NLL) | **EFE core** — `Σ_ch w_ch · KL(N(μ,σ²)‖N(C_μ,σ²_C))` | FEP-derived | — (canonical EFE) |
| ambiguity | `λ_ambig · Σ v(1-v)/Π` (Bernoulli proxy) | **EFE core** — `Σ_ch ½·ln(2πe·σ²)` | FEP-derived | — (canonical EFE) |
| colony-cost | `λ_colony · colony` | augmentation | non-FEP-engineering | Colony reserve pressure is a resource-management heuristic, not a divergence from C. |
| survival-cost | `λ_survival · survival` | augmentation | non-FEP-engineering | Homeostatic hinge-loss on hunger/distance, not an expectation over a generative model. |
| action-prior-cost | `prior` (unweighted) | augmentation | non-FEP-engineering | Per-action engineering penalty (base-cost, hunger-mult, etc.), not derived from the free energy. |
| info-gain | `−λ_info · info` | augmentation (subtracted) | analogical | Novelty/trail-gradient bonus approximates epistemic value but is not `E_Q[H[P(o\|s)]]` (that is the ambiguity term). |
| pattern-efe | `pattern_G` (if active, λ=0 default) | augmentation | non-FEP-engineering | Hand-coded per-pattern penalty table, not a KL or entropy. |
| efe-tilt (REMOVED) | `+λ·extrinsic` injected into softmax logit OUTSIDE G | **deleted** | — | Bypassed G entirely; preferences now enter ONLY through g-efe's KL-risk. |

**Behavioral change note:** Removing `efe-tilt` and replacing the risk/ambiguity
kernels changed action rankings in 3 regression tests (policy_test:
`preference-risk-favors-return-when-hungry`, `pheromone-penalized-when-ingest-absent`;
world_test: `no-empty-return-at-home`). These tests encoded behavioral invariants
tuned to the old 2-channel NLL + extrinsic logit. The new canonical EFE
distributes risk across all 14 channels. Slice 3's augmentation layer
(controller-split + residual tuning) will restore action-specific penalties.

#### Slice 3 update (2026-07-14, zai-9)

The controller split is done: `controller-score = g-efe + Σ augmentation`
with separated `:g-efe`, `:augmentation`, and `:augmentation-map` fields
(policy.clj:583-589). The augmentations are:

- **colony-cost** (non-FEP-engineering): colony reserve pressure. Ablation: load-bearing (winner changes when removed).
- **survival-cost** (non-FEP-engineering): hunger/distance homeostatic pressure. Ablation: load-bearing.
- **action-cost** (non-FEP-engineering): per-action engineering penalty. Ablation: load-bearing.
- **info-gain** (analogical): novelty/trail exploration bonus. Retained (subtracted from G).
- **pattern-efe** (non-FEP-engineering): hand-coded per-pattern penalty (λ=0 default). Retained.

R7 precision: the ant now consumes the shared `futon2.aif.precision`
(update-precision-state, precision-for) for per-channel Π — variance-derived
adaptive precision replaces the old static `default-precisions` +
`affect/modulate-precisions` heuristics. The precision-state is persisted on
the ant and threaded through the perceive loop.

R14 τ: `choose-tau` now explicitly couples to hunger (lower τ → exploit) and
recent-starvation via `since-ingest` (higher τ → explore when starved).

No augmentation was found to be a genuine FEP quantity that belongs in the
core (unlike mode-conditioned C in S2b). All augmentations carry their typed
residuals as documented in the retract table above.

### Slice 3 — faithful controller split + R7 precision + R14 τ
- **Goal:** `controller-score = g-efe + Σ augmentation`, augmentations = existing colony/survival/action
  cost, each with a typed residual (write the retract table into the mission doc). Port
  `futon2.aif.precision` to replace `affect` ad-hoc precision; couple softmax selection to `-G/τ` with
  τ from hunger + recent-starvation. `choose-action` now ranks by `controller-score`, logs the breakdown.
- **Files:** `policy.clj`, `affect.clj` (or new `precision.clj`), `pattern_efe.clj` (demote to augmentation).
- **Acceptance:** trace shows separated `:g-efe` vs `:augmentation` per candidate; a winner-changing
  ablation test proves each augmentation's causal role (or it is deleted). τ moves with starvation in a test.
- **Gate:** tests; kondo; parens. Tags: R7 principled-approx, R14 non-FEP-engineering (τ is a control dial).

### Slice 4 — R13 horizon + R8 F
- **Goal:** `S(π) = Σ_t ρ^t s(s_t)` rollout to H>1 over the R4 kernel (replace greedy `choose-action` for
  the strategic step; keep a fast tactical inner step). Emit per-tick `F = ½ mean_k(Π_k ε_k²)` in trace.
- **Files:** `policy.clj`, `core.clj`, new `src/ants/aif/rollout.clj`.
- **Acceptance:** a planted 2-step-payoff scenario where greedy loses and H=3 wins; F decreases on
  average over a learning run and spikes on a planted surprise.
- **Gate:** tests; kondo; parens. Tags: R13 FEP-derived, R8 FEP-derived.

### Slice 5 — R16 honest harness + the decisive experiment
- **Also absorbs (deferred from Slice 1):** thread a seed through the live per-ant path (currently hardcoded
  `:rand-fn rand-nth` → unseeded `Math/random`, so no reproducible trajectory). This enables both the
  per-run-seed harness AND the true **bit-identical R4 golden** (old-path vs kernel) that Slice 1 could not
  construct. Until then, R4 behavior-preservation rests on the full 61-test regression suite (verified green,
  2026-07-14, commit 5b31af8).
- **Goal:** a new comparison harness fixing all three confounds: **per-run food seeds** (randomize
  `initial-food-patchy` seed per run, log it), **counterbalanced spawn** (swap left/right assignment
  across runs, or single-army absolute-yield), and a **real `:classic` baseline** plus an
  **epistemic-ablated AIF** arm (ambiguity term zeroed). Pre-register arms + directions in the doc.
- **Arms:** `:aif-full`, `:aif-no-epistemic`, `:classic`, (optional) `:cyber-sigil` legacy for continuity.
- **Scenarios:** snowdrift / patchy / sparse. ≥ 30 **independently seeded** runs/arm/scenario.
- **Acceptance (the hypothesis test):** report per-arm mean yield + starvation with 95% CIs on
  independently-seeded environments. The pre-registered contrast: `aif-full − aif-no-epistemic > 0` on
  patchy/sparse (CI excludes 0), `≈ 0` on snowdrift. Report honestly whichever way it lands.
- **Gate:** the harness is the R16 witness — numbers must be reproducible from logged seeds by an
  independent re-run (reviewer re-runs, does not trust the summary).

## Out of scope (Port-1 v1)
R17 BMR structure learning; the explicit A/B/D categorical filter (`belief.clj`) — the ant's Gaussian
`perceive` is adequate for v1; MetaCA tokamak controller (Port 2, separate mission, depends on Slices 1–4
being reusable as domain-agnostic `aif/{forward,efe,precision,rollout}`).

## Review protocol (Claude owner)
For each returned slice: `git show <sha>`, re-run the named tests + kondo + parens locally, spot-check the
claimed numbers against artifacts, state what was checked. Fix small findings directly; re-dispatch only
substantial new work. ([[feedback_zai_bell_handoff_economics]], workspace CLAUDE.md handoff protocol.)
