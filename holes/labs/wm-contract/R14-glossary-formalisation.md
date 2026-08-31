# R14 worked through the glossary — nouns, their Gate-0 class, and what a formal R14 would have to state

**Date:** 2026-08-30. **Author:** claude-15, from `facts-R14.md` (gathered 2026-08-30, spot-checked: τ ≡ 1.0 in all 31 records since 07-14; `:selection-gain` `{1.0, samples 0}` ×31; `:governed-by` in exactly 4 records, 3 `:habit-prior` / 1 `:G`; `τ_eff = 1/g` at `policy.clj:78`; the completeness doc cites `futon2.aif.policy-precision`, a namespace that does not exist on disk).
**Status:** worksheet, not specification. Format and classes as `R8-glossary-formalisation.md`.
**Sources:** `p4ng/sec-glossary.tex`; `p4ng/sec-catalog.tex:239`; `futon2/docs/futon-aif-completeness.md:407–429`; `futon2/src/futon2/aif/{policy,selection_gain,habit_prior}.clj`; `futon2/data/wm-trace/`; `mathlib4/DarkTower/WarMachine/CommitmentTemperature.lean`; `E-R14-red-ring-fill.md`.


> **Line numbers in `policy.clj` re-mapped 2026-08-30** after merging `origin/main` (16 commits of 08-20/21) into local `main` as `5471f91`; that merge shifted `policy.clj` by +11 lines. Pointers into `facts-R14.md` / `facts-R6.md` and the excursions are pre-merge and are left as dated.

---

## 0. First finding: "R14" is one noun renamed, and the rename was recorded as the repair

| | as commissioned | as published |
|---|---|---|
| name | *Precision over policies (the γ term)* (`futon-aif-completeness.md:407`) | *Selection Gain as Commitment Temperature* (`sec-catalog.tex:239`) |
| quantity | **γ**, variational policy precision — Da Costa 2020's β update from E[G] over policies (the badge's own `:cite`) | **g**, "the outcome-feedback selection gain … an engineering calibration control, not an inferred variational policy precision" (glossary, *Softmax*) |
| status claimed | "**SATISFIED as of v0.22** … γ = 1.0 EARNED" (`:407–429`) | `wr-overlay.edn:45` `:holds false`; pattern map "ARMED (latent until R10 + data)" |
| the repair | B-3b (γ β-update) — commissioned 07-03/04, never landed | `:repair "complete: renamed namespace, state, trace, selection option, and realised-outcome fields so no variational-γ claim remains"` (`r18-badges.edn:227`) |

The rename was honest — the glossary says plainly what g is — and it is also
the whole of what happened to R14 between the audit and the paper. Two faces
survive under the one number (`E-R14` §3c): **gain-in** (R8's realised
outcomes → g) and **temperature-out** (g → τ_eff → the selected action). The
excursion's verdict: red on a *disconnected dial* — "on the enacting path no
value of τ can change the action."

## 1. R14's closure in the glossary, classified (Gate 0)

| glossary noun | what the glossary says | class | code · trace · Lean |
|---|---|---|---|
| **Softmax** p_i = e^{−G_i/τ} / Σ_j e^{−G_j/τ} | worked example; "lower scores are better" | **theory-defined as a formula** | `policy.clj:82 softmax-weights` · scores in `:decision` · none |
| **τ_eff = 1/g** | "The effective temperature is set through the outcome-feedback selection gain g … This is an engineering calibration control, not an inferred variational policy precision" | **stack-defined, honestly labelled** | `policy.clj:78` · `[:decision :tau]` · `CommitmentTemperature.Temperature := Nat` |
| **g** (selection gain) | *Precision Π* footnote names g as "the R14 pattern" | **stack-defined**: clamp(2^(gain·mean-perf), 0.5, 2.0); badge `:engineering-control`; "the estimator is a substitution, not an approximation of the β-update" | `selection_gain.clj:123` · `:selection-gain` · `GainChain.gainAdvances` |
| **γ** (variational policy precision) | not in the glossary by that name; the badge cites Da Costa's β update | **borrowed name**, now retired by rename — the theory's term with no stack object | `policy-precision` ns cited at `futon-aif-completeness.md:353,412`, **absent on disk** · `:policy-precision` key 06-27..07-09 · none |
| **Policy prior E(π), habit** | Q(π) ∝ exp(ln E(π) − G(π)/τ); "implements this form exactly **but not yet at the canonical policy grain**"; Dirichlet posterior predictive over action *categories* k(a) | **theory-defined seam, stack-defined grain** | `habit_prior.clj` · `:habit-prior-state` ×31 · `CommitmentTemperature.habitPrior` |
| **Policy π** | "the active-inference name for a pattern language/cascade" | **borrowed name** — here a scheduler action category `(type, target-class)` | · `[:decision :type]` · `CommitmentTemperature.Action` (an id string) |
| **G** as the selector's input | "G is an opaque ordered controller score. Nothing here defines or validates G(π)" (Lean docstring) | in R14's context, **stack-defined**: `:controller-score` | `policy.clj:380 g-totals (mapv :controller-score …)` |
| **governs** | not in the glossary; the excursion's demand: "g moved ⟹ τ_eff moved ⟹ the selected action is a function of τ_eff" | **theory-defined in Lean**, as the property R14 must have: `governs s := ∃ entries τ₁ τ₂, s τ₁ entries ≠ s τ₂ entries` | · · `CommitmentTemperature.lean:75`; `factorsThroughDiscard` (Markov form, `:251`) |

**Gate-0 verdict for R14.** The formula (softmax) and the seam (ln E − G/τ) are
the theory's; the *quantities* fed into them are the stack's — g by admission,
G as a controller score, π as an action category. The one theory-defined term
in R14's own vocabulary, *governs*, was supplied by the excursion and the Lean
module, not by the glossary. γ — the term the requirement was named for — has
no object in the stack and no longer has a name in it either.

## 2. What the corpus holds — checked 2026-08-30

- **τ ≡ 1.0 in every record since the 07-13 flip** (`arena-tau-mode` → `:selection-gain-only`, so τ_eff = 1/g and g is pinned): 31 of 31. Before the flip, `:spread` (76 records).
- **g never had a sample**: `:selection-gain {:selection-gain 1.0 :samples 0}` in all 31 records that carry it; `:policy-precision` (the older key, 216 records, 06-27..07-09) — samples 0–76 in an era whose "outcomes" were the coverage mirror.
- **The action does not depend on τ, by construction.** `strategic-recommendation` (`policy.clj:234`) computes τ, then sets `chosen` (`:238`) to `(or (first controller-entries) (first ranked-actions))`. τ feeds only `scores → habit-order → counterfactual`. This is the excursion's bound: `I(realized outcomes ; action) ≤ I(g ; action) ≤ I(τ ; action) = 0` — a bound from the code's shape, not a measurement.
- **`:governed-by` exists in exactly four records** (3 `:habit-prior`, 1 `:G`), all with `:habit-prior-applied? true` and the enacted action equal to the explanation's winner. Whether those were live or shadow is "not established" (Lean docstring). Four is not a corpus.
- **The completeness doc's "SATISFIED … γ = 1.0 EARNED"** is a statement about burn-in: γ *is* 1.0 because `min-history` samples never accrued. "Earned" named the prior.

## 3. What a formal R14 would have to state — signatures, in dependency order

**Definable now, from the glossary and the code:**

```
Score        := ℝ                                          -- the controller score, opaque (stack-defined)
Temperature  := ℝ>0
softmax      : List Score → Temperature → Dist Index        -- p_i ∝ exp(−G_i/τ)          [glossary eqanchor softmax]
tauEff       : Gain → Temperature                           -- 1/g                          [policy.clj:78]
seam         : (E : Dist Index) → List Score → Temperature → Dist Index
                                                            -- Q(π) ∝ exp(ln E(π) − G(π)/τ)  [glossary, Policy prior]
governs      : Selector → Prop                              -- ∃ τ₁ τ₂ entries, s τ₁ ≠ s τ₂   [CommitmentTemperature.lean:75]
```
Record contract, statable now: `∀ tick, tick.decision.tau = tauEff tick.selectionGain.g`
(holds, trivially, at 1.0). The R14 *requirement*, statable now and **refused
by the live selector**: `governs liveSelector` — proved false as
`live_selector_does_not_govern` (`:154`), with the plausible-fix refusal
`record_sensitivity_is_not_governance` (`:207`): making the *record* move with
τ is not governance. Nine of the thirteen theorems are about this edge.

**Blocked, and on what:**

```
γ            : Dist Policy → ℝ           -- variational policy precision: needs E[G] over policies
                                          --   → needs G(π) → needs Policy and Q(o∣π)   (BLOCKED, three levels)
E(π)         : Dist Policy               -- the habit prior at policy grain: today at category grain k(a)   (BLOCKED on Policy)
```
The glossary already says which of these is which: "implements this form
exactly but not yet at the canonical policy grain." The form is definable; the
grain is the blocked noun.

## 4. R14's internal wiring as typed deliveries (Gate 1, inside the node)

| edge | from → to | payload | guarantee as built | the undeclared field |
|---|---|---|---|---|
| e1 | R8 `:realized-outcome` → `update-selection-gain` | (expected, realized) | **burn-in until `min-history`; 0 samples ever** | `receipt`: none — starvation reads as g = 1.0 "earned" |
| e2 | g → `effective-temperature` | scalar | `:tau-mode` flipped 07-13 to `:selection-gain-only` ⇒ τ = 1/g ≡ 1.0 | `guarantee`: a mode flip changed the edge's function with no receipt on the record (`:tau-mode` present in 107 of 791) |
| e3 | τ → `softmax-weights` → `scores` | distribution | computed and **recorded** | — the only edge that works |
| e4 | scores → `chosen` | action | **CUT**: `chosen = (first controller-entries)` regardless of τ (`policy.clj:249`) | `atomic-with`: the record's `:tau` is written as if e4 existed |
| e5 | `decision-explanation` → `:governed-by` | verdict | written from `(if (= winner top-g) :G :habit-prior)` | `receipt` about a wire that is cut: *record sensitivity is not governance* |

The failure is e4, and its shape is the same as R8's: a defensible choice
(take the controller head at strategic grain) composed with a record (`:tau`)
that reports the edge as live. `Delivery.receipt` on e4 — "the selected action
is a function of τ, witnessed by a τ-varying pair" — is the field that was
never written, and it is exactly `governs`.

## 5. R14's evidence contract — draft for the apex

```
subject      R14 (temperature-out)                          R14 (gain-in)
claim        the selected action is a function of τ_eff     g is updated from realised outcomes and moves τ_eff
artefact     decision record: :tau, ranked scores, :type   :selection-gain state + the R8 outcome it consumed
domain       every tick; ≥ 2 distinct τ values in the corpus  any mission; ≥ 2 realised outcomes with distinct realized-G
corpus       futon2/data/wm-trace/ (reader loop over all top-level forms)
method       pair ticks with equal ranked lists and unequal τ; compare :type   diff g across ticks; count :samples
falsifier    over the whole corpus, τ is constant OR the      g = 1.0 with samples = 0 after the loop has produced
             action never differs across τ  ← fires today    outcomes  ← fires today (07-14..07-21)
not-evidence :governed-by :habit-prior (record sensitivity);  "γ = 1.0 EARNED" (burn-in named as evidence);
             the B-2d shadow "0 winner-flips" (a no-op        the 18/18 fixture; the :policy-precision era
             witnessed as a no-op and filed DONE)             (outcomes were the coverage mirror)
```

## 6. Glossary → specification: the `Formal:` line each entry would carry

- *Softmax and controller calibration*: `Formal: softmax, tauEff — definable; governs (CommitmentTemperature.lean:75) is the requirement; refused live`.
- *Policy prior E and habit*: `Formal: seam definable; E at policy grain BLOCKED on Policy`.
- *Precision Π* (its footnote naming g): `Formal: g is an engineering controller (r18); no variational referent; the name γ retired by rename 07-14`.
- *Policy π*: `Formal: no referent — borrowed`.

## 7. What this worksheet does not do

It does not define γ, and says why in one line: γ is a function of G over
policies, and neither G(π) nor Policy exists. It does not choose among the
excursion's four repairs for the cut wire (sample P(π); a second non-τ term;
route the gain through the candidate set; convert to an interrupt). It records
that the glossary told the truth about g on 07-03, that the theorem refusing
the live selector exists, and that between them nothing touched the record
that says `:tau` every tick.
