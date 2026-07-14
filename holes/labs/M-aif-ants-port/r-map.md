# R-Contract Audit: Ant AIF Brain (M-aif-ants-port, Slice 0)

**Status:** DERIVE, read-only audit. No product code changed.
**Date:** 2026-07-14
**Scope:** `src/ants/aif/{observe,perceive,core,policy,pattern_efe,affect,default_mode,pattern_sense}.clj`
**Reference target:** `src/futon2/aif/{efe,precision,belief}.clj`

All line references are 1-based and point into `futon2/src/ants/aif/` unless a
different file is named. Every row was resolved by reading the cited source.

---

## R-map table

| R# | Contract quantity | Current ant seam (fn + file:line) | Exact gap vs contract | Faithfulness note |
|---|---|---|---|---|
| **R1** | belief map μ (operational hypothesis, not raw state) | `perceive/perceive` (`perceive.clj:124`–`166`); μ built by `ensure-mu` (`perceive.clj:55`–`75`). μ is a map `{:pos :goal :h :sens}` where `:sens` is a map of the 14 sensory channels. Updated via `update-sensory` (`perceive.clj:107`–`114`): `sens[k] += α·Π_k·(o_k − sens[k])` — a predictive-coding update. `core/default-aif-config :mu` is **not** where μ lives; `:mu` is per-ant runtime state set by `aif-step` (`core.clj:137`). | μ IS an operational-hypothesis map (predicted sensory means + goal + hunger), not raw state — so R1 is structurally present. Gap: μ is a point estimate (means only); there is **no variance/covariance** on μ itself. The contract's R3 "variance floor" refers to the observation channel variances used in EFE ambiguity, not a posterior covariance on μ. | **FEP-derived** (predictive-coding form). Point-estimate μ is a principled approximation; adequate for v1. |
| **R2** | typed observation o | `observe/g-observe` (`observe.clj:73`–`172`); `observe/sense->vector` (`observe.clj:175`–`179`). Produces a 14-channel map: `[:food :pher :food-trace :pher-trace :home-prox :enemy-prox :h :ingest :friendly-home :trail-grad :novelty :dist-home :reserve-home :cargo]` (the `sensory-keys` vector at `perceive.clj:7`–`9`). `sense->vector` projects to a fixed-order vector. | **No gap — complete.** All 14 channels are normalized to [0,1], typed, and consistently ordered. | **Complete** (FEP-derived observation ABI). |
| **R3** | predictive-coding update μ←μ+αΠε + variance floor | `perceive/compute-errors` (`perceive.clj:81`–`94`): `raw = o − pred`, `weighted = Π_k · raw`. `perceive/update-sensory` (`perceive.clj:107`–`114`): `sens[k] += α · weighted`. So the update is `μ_k ← μ_k + α·Π_k·(o_k − μ_k)`. Precision `Π_k` comes from `default-precisions` (`perceive.clj:11`–`26`, a map of per-channel constants) modulated by `affect/modulate-precisions`. | The update form matches `μ←μ+αΠε`. **Gap (variance floor):** there is no per-channel observation variance σ²_k tracked or floored. `expected-ambiguity` (`policy.clj:473`–`481`) uses `v·(1−v)/Π` as a *pseudo-variance* from the [0,1] channel value — this is a Bernoulli-flavored variance proxy, NOT a tracked/floored Gaussian variance. The contract wants `σ²_ch` that can be floored at a minimum (e.g. 1e-9) and fed to `½ln(2πe·σ²)`. | **Principled approximation** (predictive coding). The pseudo-variance `v(1−v)/Π` is analogical, not a tracked posterior variance. |
| **R4** | one pure forward kernel `(state,action,seed,opts)→next` | **ABSENT as a shared kernel.** `policy/predict-outcome` (`policy.clj:141`–`280`) is a **heuristic one-step predictor** — a large hand-tuned `case` over `{:forage :return :pheromone :hold}` that drifts observation-channel values via `drift` (`policy.clj:137`–`139`). It does NOT call the world step. The actual world step is `war/step` (`war.clj:1352`), which mutates the world in-place (evaporate pheromones, step ants, consume colonies). The ant's policy predictor and the live world step are **two different code paths** with no shared kernel. | **Build required.** Factor a pure `(state, action, seed, opts) → next-state` from `war.clj`'s food-gather / pheromone-decay / move logic. `predict-outcome` must call the same kernel (+ principled noise), not its own heuristic drifts. | **Absent.** The heuristic `predict-outcome` is **non-FEP engineering** — it is a hand-shaped forward model, not a generative-model kernel. |
| **R5** | unit-pure G_efe = KL-risk + entropy-ambiguity | `policy/expected-free-energy` (`policy.clj:485`–`521`). Computes: `risk = risk-from-preferences` (`policy.clj:308`–`316`); `ambiguity = expected-ambiguity` (`policy.clj:473`–`481`); then `G = λ_prag·risk + λ_ambig·ambiguity + λ_colony·colony + λ_survival·survival + prior − λ_info·info + pattern`. | **This is NOT a canonical KL-risk + Gaussian-entropy-ambiguity.** See the dedicated R5 analysis below. | **Non-FEP engineering** (hand-shaped penalty). Core replacement required. |
| **R6** | constructed candidate set a | `policy/default-actions` (`policy.clj:7`–`8`): `[:hold :forage :return :pheromone]`. `policy/actions-by-mode` (`policy.clj:11`–`14`): mode-conditioned ordering. `policy/admissible-actions` (`policy.clj:544`–`580`): filters by cargo/home/food gates. | **No gap — complete.** Small legible 4-action set, mode-conditioned, with principled admissibility filtering. | **Complete.** Legible candidate set (FEP-derived structure). |
| **R7** | per-channel precision Π (adaptive) | `perceive/default-precisions` (`perceive.clj:11`–`26`): static per-channel constants `{:food 1.0 :pher 0.8 ...}`. `affect/modulate-precisions` (`affect.clj:91`–`130`): scales precision by hunger/home-prox at runtime. `affect/anneal-tau` (`affect.clj:132`–`141`): anneals τ over perceive micro-steps. | **Gap:** precisions are static constants + heuristic hunger-scaling — NOT the variance-derived adaptive precision `Π = 1/max(σ², σ²_min)` of `futon2.aif.precision`. No rolling error-variance window, no posterior variance estimate. The WM reference (`precision.clj`) maintains per-channel `{:precision :variance-component :need-component :error-history}` updated via `update-precision-state`. | **Analogical.** `modulate-precisions` is a hand-tuned heuristic, not variance-derived. Port `futon2.aif.precision` required. |
| **R8** | per-tick variational F scalar | `perceive/perceive` returns `:free-energy` (`perceive.clj:162`): `F = 0.5 · (Σ_step Σ_k Π_k·ε_k²) / max-steps` — a mean weighted-MSE over the perceive micro-loop. `core/aif-step` does NOT surface this as `:F` in its return map; it is buried inside `:perception`. | **Partially present.** F IS computed as `½·mean_k(Π_k·ε_k²)` — this matches the contract's `F = ½ mean_k(Π_k ε_k²)`. Gap: it is NOT emitted as a named `:F` field in the top-level trace/diagnostics from `aif-step`; it lives under `(:perception result) :free-energy`. Needs to be surfaced. | **FEP-derived.** The formula is correct; only the plumbing (trace emission) needs work. |
| **R13** | policy horizon S(π), H>1 rollout | `policy/choose-action` (`policy.clj:702`–`821`): evaluates each action with `expected-free-energy` which calls `predict-outcome` **once** (1-step). No rollout loop, no discount ρ, no horizon parameter. | **ABSENT.** Purely greedy 1-step. `eval-policy` (`policy.clj:823`–`839`) just wraps `choose-action` for deterministic ranking — also 1-step. Build H>1 rollout over the R4 kernel with discount ρ. | **Absent.** Build required (FEP-derived). |
| **R14** | commitment temperature τ | `affect/hunger->tau` (`affect.clj:63`–`71`): `τ = τ_min + (τ_max−τ_min)·(1−hunger)` — hungrier ants exploit more (lower τ). `affect/update-tau` (`affect.clj:148`–`194`): adjusts τ by need-error, dhdt, reserve level. `policy/couple-tau` (`policy.clj:456`–`471`): couples τ to reserve-delta and survival-pressure. `policy/choose-tau` (`policy.clj:680`–`700`): final τ selection with nest clamps. Softmax uses `-G/τ` (`policy.clj:810`: `base-logit = (/ (- (:G result)) tau)`). | **Structurally present and coupled to softmax.** τ is hunger-driven, softmax IS over `-G/τ`. Gap: τ is driven by heuristic survival/reserve terms, not cleanly from "hunger + recent-starvation" as the contract specifies. The coupling chain (`couple-tau` → `choose-tau` → softmax) is engineering-heavy but functional. | **Non-FEP engineering** (τ is a control dial). Keep; tighten the coupling. |
| **R16** | external witness | `war/step` (`war.clj:1352`): scores, reserves, events. `war/simulate` (`war.clj:1382`): runs scenario. `compare.clj` (not read this slice) runs comparisons. | The honest harness (Slice 5) IS the witness. Existing `compare.clj` has the three confounds documented in the mission spec. | **Out of scope for this slice.** Slice 5 builds the witness. |
| **R17** | BMR structure learning | **n/a** — not present in any ant AIF namespace. | Out of scope (Port-1 v1). | **Out of scope.** |
| **R19** | preference C-vector | `policy/default-preferences` (`policy.clj:92`–`95`): `{:hunger {:mean 0.40 :sd 0.08} :ingest {:mean 0.70 :sd 0.20}}`. Also `core/default-aif-config :preferences` (`core.clj:16`–`17`): same shape. Used by `risk-from-preferences` (`policy.clj:308`–`316`) via `nll` (`policy.clj:302`–`306`). Separately, `policy/C-prior` (`policy.clj:55`–`60`) is a **mode-conditioned** linear preference map used by `efe-tilt` (`policy.clj:78`–`89`). | **Gap:** C exists but only covers 2 channels (hunger, ingest) as Gaussian targets, NOT the full 14-channel observation ABI. `C-prior` is a separate, incompatible linear-weight map over abstract features (`:gath :ing :cargo+ :near-nest :trail+`), NOT over the obs ABI. The contract wants an explicit C over the obs ABI (fed, reserves↑, cargo-home) so the KL-risk in R5 is against **this** C. | **Analogical.** C is present in spirit but fragmentary — 2 Gaussian channels + a disjoint linear map. Must be unified into one explicit C over the 14-channel ABI. |

---

## R5 deep-dive: does `pattern_efe` / `expected-free-energy` compute canonical KL-risk + entropy-ambiguity?

**No.** Neither the core `expected-free-energy` nor the `pattern_efe` namespace
computes a canonical KL-risk or Gaussian-entropy-ambiguity. Here is the exact
form of each term:

### Risk (`risk-from-preferences`, `policy.clj:308`–`316`)

```clojure
(defn- risk-from-preferences [outcome prefs]
  (let [{:keys [hunger ingest]} (ensure-preferences prefs)
        hunger-x (double (or (:hunger outcome) (:h outcome) 0.0))
        ingest-x (double (or (:ingest outcome) 0.0))]
    (+ (nll hunger-x hunger)
       (nll ingest-x ingest))))
```

where `nll` (`policy.clj:302`–`306`) is:

```clojure
(* 0.5 (/ (- x mean) sd) (/ (- x mean) sd))   ; = ½·z²
```

This is a **quadratic penalty** `½·((x−μ)/σ)²` on two channels only (hunger,
ingest). It is the **negative log-likelihood of a Gaussian** — structurally
related to KL but NOT the canonical `KL(N(μ_ch, σ²_ch) ‖ C_ch)`. The canonical
KL between two Gaussians includes log-variance terms:
`KL = ½·[ln(σ_C²/σ²) + (σ² + (μ−μ_C)²)/σ_C² − 1]`. The ant's `nll` omits all
variance-ratio terms — it is `½·z²`, a Mahalanobis distance, not a KL.

### Ambiguity (`expected-ambiguity`, `policy.clj:473`–`481`)

```clojure
(defn- expected-ambiguity [prec outcome]
  (let [acc (reduce-kv (fn [sum k v]
                         (if (and (number? v) (not= k :hunger))
                           (let [precision (double (get-in prec [:Pi-o k] 1.0))
                                 v (double (clamp (or v 0.0)))]
                             (+ sum (* (/ 1.0 (max precision 0.2)) v (- 1.0 v))))
                           sum))
                       0.0 outcome)]
    (* 0.5 acc)))
```

This computes `Σ_k ½·(1/Π_k)·v_k·(1−v_k)` — a **Bernoulli variance proxy**
(`v(1−v)` is the variance of a Bernoulli with parameter `v`), scaled by inverse
precision. This is NOT the canonical Gaussian entropy `Σ_ch ½·ln(2πe·σ²_ch)`.
The canonical form requires a tracked variance σ²_ch; the ant uses `v(1−v)/Π`
as a stand-in.

### The composite G (`expected-free-energy`, `policy.clj:513`–`521`)

```clojure
G = λ_prag·risk + λ_ambig·ambiguity + λ_colony·colony + λ_survival·survival
  + prior − λ_info·info + pattern_G
```

This is a **hand-shaped multi-term penalty**, not a unit-pure EFE. The canonical
`G_efe = KL(Q(o|π)‖C) + E_Q(s|π)[H[P(o|s)]]` has exactly two terms (risk +
ambiguity). The ant's G has **seven** terms, of which only the first two
(`risk`, `ambiguity`) are even structurally related to EFE, and neither is in
canonical form. The remaining five (`colony`, `survival`, `prior`, `info`,
`pattern`) are engineering augmentations with no FEP derivation.

### `pattern_efe.clj` (`pattern_efe.clj:1`–`169`)

`pattern-efe` (`pattern_efe.clj:139`–`169`) computes `G = λ_pattern·(risk −
info)` where `risk` and `info` are **hand-coded per-pattern constants** from
`case` statements (e.g. `cargo-return-risk` returns `0.4`, `0.3`, `0.15`, or
`0.0` based on hard thresholds; `pattern_efe.clj:20`–`44`). These are
**discrete penalty tables**, not KL divergences or entropies. Default
`:lambda :pattern = 0.0` (`pattern_efe.clj:149`), so this term is inert unless
explicitly enabled.

**Verdict for R5:** `expected-free-energy` computes a hand-shaped per-pattern
penalty, NOT a canonical KL-risk + entropy-ambiguity. Slice 2 must replace the
risk and ambiguity kernels with the canonical forms, and demote all other terms
to named augmentations.

---

## Where the ant already computes a KL or an entropy

**There is no canonical KL divergence computed anywhere in the ant AIF brain.**
The closest thing is `nll` (`policy.clj:302`–`306`), which is `½·z²` — the
quadratic core of a Gaussian NLL, but missing the log-variance term that would
make it a KL.

**There is no canonical Gaussian entropy `½·ln(2πe·σ²)` computed anywhere in
the ant AIF brain.** The closest thing is `expected-ambiguity`
(`policy.clj:473`–`481`), which computes `½·Σ_k v_k(1−v_k)/Π_k` — a Bernoulli
variance proxy, not a Gaussian differential entropy.

For comparison, the WM reference (`futon2/aif/efe.clj`) computes both:
- **KL-risk** (`efe.clj:585`–`595`): `Σ_ch w_ch · KL(N(μ_ch,σ²_ch) ‖ C_ch)` via
  `pref/kl` — the full Gaussian-Gaussian KL including log-variance terms.
- **Gaussian-entropy-ambiguity** (`efe.clj:48`–`58`): `Σ_ch ½·ln(2πe·σ²_ch)`.

Neither formula appears in the ant codebase.

---

## Explicit yes/no answers to the five acceptance questions

| Question | Answer | Seam |
|---|---|---|
| **(a) explicit C/preference vector?** | **Partial.** C exists as `default-preferences` (`policy.clj:92`–`95`) with 2 Gaussian channels (hunger, ingest) + `C-prior` (`policy.clj:55`–`60`) as a disjoint mode-conditioned linear map over abstract features. Neither is an explicit C over the 14-channel obs ABI. | `policy.clj:92`–`95`, `policy.clj:55`–`60` |
| **(b) per-channel precision?** | **Yes** (static + heuristic modulation). `default-precisions` (`perceive.clj:11`–`26`) gives 14 per-channel constants; `modulate-precisions` (`affect.clj:91`–`130`) scales them by hunger/home. NOT variance-derived adaptive precision. | `perceive.clj:11`–`26`, `affect.clj:91`–`130` |
| **(c) commitment temperature τ?** | **Yes.** `hunger->tau` (`affect.clj:63`–`71`), `update-tau` (`affect.clj:148`–`194`), `couple-tau` (`policy.clj:456`–`471`), `choose-tau` (`policy.clj:680`–`700`). Softmax uses `-G/τ` (`policy.clj:810`). | `affect.clj:63`–`71`, `policy.clj:810` |
| **(d) multi-step rollout?** | **No.** `choose-action` is purely 1-step (`policy.clj:743`: one `expected-free-energy` call per action). No horizon, no discount, no rollout loop. | absent |
| **(e) per-tick variational F?** | **Yes (computed but not surfaced).** `perceive` returns `:free-energy = ½·(Σ_step Σ_k Π_k·ε_k²)/max-steps` (`perceive.clj:162`). `aif-step` does NOT emit it as a named `:F` field — it is nested under `(:perception result) :free-energy`. | `perceive.clj:162` |

---

## Surprises / corrections to the spec's draft table

1. **R1 — confirmed, with nuance.** The spec says "confirm μ is operational-hypothesis map, not raw state." **Confirmed:** μ = `{:pos :goal :h :sens}` where `:sens` holds predicted per-channel means (`perceive.clj:70`–`75`). It is NOT raw state. However, μ is a **point estimate** with no variance/covariance — adequate for v1, but the spec should note this.

2. **R3 — the "variance floor" gap is sharper than the spec states.** The spec says "add variance-floor if missing." The deeper issue is that the ant has **no tracked per-channel observation variance at all** — not just a missing floor. The `expected-ambiguity` function (`policy.clj:473`–`481`) fabricates a pseudo-variance from `v(1−v)/Π`, which cannot be floored meaningfully because it is a function of the channel value, not an independent variance estimate. Slice 2 must introduce actual σ²_ch tracking, not just add a floor.

3. **R5 — the spec's "hand-coded per-pattern risk/info-gain" description conflates two separate code paths.** There are TWO EFE-like computations:
   - `expected-free-energy` (`policy.clj:485`–`521`): the main policy scorer. Its risk is `½·z²` (Gaussian NLL core, NOT KL); its ambiguity is `v(1−v)/Π` (Bernoulli proxy, NOT Gaussian entropy).
   - `pattern_efe/pattern-efe` (`pattern_efe.clj:139`–`169`): a per-pattern additive term, inert by default (λ=0). Its "risk" and "info-gain" are hard-coded constants from `case` tables.
   
   The spec's draft says "pattern_efe/* (hand-coded per-pattern risk/info-gain)" — this is accurate for `pattern_efe.clj` but the **main** EFE in `expected-free-energy` is also hand-shaped (just differently). Slice 2's scope covers BOTH: replace the risk/ambiguity kernels in `expected-free-energy` AND demote `pattern_efe` to augmentation.

4. **R7 — the spec's draft is accurate but understates the gap.** The spec says "port `futon2.aif.precision`." Correct, but note that the ant's precision system is **two layers** — static defaults in `perceive.clj` + dynamic modulation in `affect.clj` — and both must be replaced. The `affect/modulate-precisions` function (`affect.clj:91`–`130`) is ~40 lines of heuristic scaling that will be superseded.

5. **R8 — the spec says "absent." This is WRONG.** F IS computed: `perceive.clj:162` returns `:free-energy = ½·mean_k(Π_k·ε_k²)`, which is exactly the contract's `F = ½·mean_k(Π_k·ε_k²)`. The gap is that it is not surfaced as a named `:F` in `aif-step`'s top-level output — it is nested under `:perception`. **Correction: R8 is partially present (formula correct, plumbing incomplete), not absent.**

6. **R14 — the spec's draft is accurate.** τ is present, hunger-coupled, and drives the softmax as `-G/τ`. The coupling chain (`hunger->tau` → `update-tau` → `couple-tau` → `choose-tau`) is engineering-heavy but functional. The spec's "keep; couple softmax to `-G/τ` explicitly" is already done — the softmax at `policy.clj:810` uses `(/ (- (:G result)) tau)`.

7. **R19 — the spec undersells how fragmentary C is.** The draft says "promote to explicit C." In reality there are **two disjoint preference structures**: `default-preferences` (2 Gaussian channels) and `C-prior` (mode-conditioned linear weights over abstract features like `:gath :dep`). These are incompatible representations used by different code paths (`risk-from-preferences` vs `efe-tilt`). Slice 2 must unify them into a single C over the obs ABI.

8. **Additional finding (not in the spec table): `efe-tilt` (`policy.clj:78`–`89`).** There is a **second, separate** action-evaluation path via `efe-tilt` that computes a mode-conditioned extrinsic score and adds it to the softmax logit (`policy.clj:812`: `(+ base-logit aif-logit)`). This is a **third** hand-shaped scorer alongside `expected-free-energy` and `pattern-efe`. It uses `infer-mode` (softmax over modes, `policy.clj:41`–`53`) and `C-prior` (linear weights). This must also be accounted for in the Slice 2/3 controller split — it is currently an undocumentated augmentation baked into the softmax.
