# R5 worked through the glossary — nouns, their Gate-0 class, and what a formal R5 would have to state

**Date:** 2026-08-30. **Author:** claude-15, from `facts-R5.md` (gathered 2026-08-30, spot-checked: `coverage_check.clj` has no caller in `src/`, `scripts/` or `test/`; the paper's R5 paragraph (`sec-catalog.tex:235`) contains neither "coverage" nor "criterion"; `:free-energy` key sets are `{G-total, G-pragmatic, G-epistemic}` ×760 (through 07-09) and `{controller-score, preference-gap-score, coverage-uncertainty-pressure}` ×31 (from 07-14), never both; no coverage/criterion key at any depth in 791 records).
**Status:** worksheet, not specification. Format and classes as `R8-glossary-formalisation.md`.
**Sources:** `p4ng/sec-glossary.tex`; `p4ng/sec-catalog.tex:235`; `futon-aif-completeness.md:125–146`; `promotion-tests.edn:124–146`; `futon2/src/futon2/aif/{efe,core_efe,free_energy,c_vector,coverage_check,epistemic_value}.clj`; `futon2/data/wm-trace/`; `CoverageReport.lean`, `PolicyGrade.lean`; `E-R5-red-ring-fill.md`.

---

## 0. First finding: "R5" is two nouns, and the red one is in no status surface

| | the paper's / completeness R5 | the red-ring R5 |
|---|---|---|
| name | *Expected Free Energy Inside an Auditable Controller* — "compute and persist the canonical core apart from everything wrapped around it" | WR-25: "the evaluate stage reports what the criterion set does **not** cover, with the same discipline it applies to a poor score" (`promotion-tests.edn:124`) |
| quantity | **G_core = risk + ambiguity**, in nats, beside `:controller-score` | a **coverage report**: `Report = scored ∣ uncovered ∣ absent` (`CoverageReport.lean:49`) |
| where it lives | catalogue, completeness doc, `r18-badges.edn` (`:G {:requirement "R5" …}`), pattern map | `promotion-tests.edn`, `E-R5`, `CoverageReport.lean`, `ContractEmitter.lean` (`coverageClauseJson`), `coverage_check.clj`, the mission — **and no status surface** |
| status | badges `:derived-from-FEP` ×2, `:engineering-control` ×5; pattern map "EIG ARMED (latent)" | `wr-overlay.edn:39` `:holds false` — ":warm-customer-pays satisfied, uncounted, unsurfaced" |

The two are not in conflict; they are two requirements on the same stage
(EVALUATE). But the noun the paper and the badges certify is the first, and the
noun the ring is red for is the second, and `coverage_check.clj` — the only
Clojure that speaks the second — is called by nothing and tested by nothing.

## 1. R5's closure in the glossary, classified (Gate 0)

| glossary noun | what the glossary says | class | code · trace · Lean |
|---|---|---|---|
| **Expected free energy G** | G_efe(a) = D_KL[Q(o∣a)‖C] + 𝔼H[P(o∣s)], nats; "the implementation persists this two-term core apart from its controller augmentation" | **theory-defined as a formula, over an action `a`** — and honestly: "the paper uses two grains" | `core_efe.clj:94 g-efe`; `efe.clj:750 :G-core` · `:free-energy` (see §2) · none |
| **Risk** | KL between predicted and preferred outcomes | **theory-defined**; computes Σ_ch w·KL(N(μ,σ²)‖C_ch) — per channel Gaussians, "truncated and renormalised" (badge) | `free_energy.clj:44` · `:G-risk` per candidate · none |
| **Ambiguity** | expected entropy of P(o∣s); "[0.5,0.5] gives 1 bit" | **theory-defined**; computes Σ_ch ½ln(2πeσ²); badge note: "influence MEASURED **0% flips / 674 ticks** (within-tick sd 0.0039)" | `efe.clj:37 ambiguity` · `:G-ambiguity` · none |
| **Observation model A** | "explicit, column-normalised 7×7 matrix over the entity-status vocabulary" | **theory-shaped** (a likelihood matrix); **stack-defined** in content; the *channel* half is 8 of 14 | `belief.clj:929–943` · — · none |
| **Model uncertainty and EIG** | "The structure learner does **not** compute that expectation"; U_model = Σ sd(A_c) is "a model-uncertainty bonus … deliberately excluded from G-efe" | **stack-defined, honestly labelled**; the EIG kernel exists and is "UNWIRED" pending Q(o∣π) | `epistemic_value.clj` · `:model-uncertainty-bonus` · none |
| **controller-score** | (glossary, *G*): "posterior spread, urgency, intrinsic credit, feasibility masks, and coverage bonuses are reported beside it, never hidden inside it" | **stack-defined, honestly labelled**: "linear sum of 8 terms in incommensurate units at hand-set weights; risk+ambiguity core diluted" (badge) | `efe.clj:449` · `:controller-score` · none |
| **Preferences C** | (in *Risk*): "preferred outcomes"; `c_vector.clj`: static channel-range floor + goal-outcome half | **stack-defined**; badge on `:G-goal-outcome`: "SHARPEST relabel — a non-KL explicitly labelled canonical/KL" (repair since built as Bernoulli KL) | `preferences.clj`, `c_vector.clj:406` · — · none (PolicyGrade: "no preferences C") |
| **criterion set / coverage** | not in the glossary as an entry; "coverage" occurs four times, as fold-coverage and cascade-coverage — a different noun | **undefined in the glossary**; **theory-defined in Lean**: `CriterionSet.covers : Outcome → Bool`, `declaresCoverage`, `outsideIsTyped`, `coverageReported` | `coverage_check.clj` (no callers) · none · `CoverageReport.lean:44–116` |
| **Policy π** (for G(π)) | "the AIF name for a cascade when scored" | **borrowed name**; `PolicyGrade` names *when a number may be called G(π)* (S-G2, S-G4) and "contains no probability, no C, no EFE" | — · — · `PolicyGrade.lean:44 Run` |

**Gate-0 verdict for R5.** R5-EFE is the best-classified node in the machine:
risk and ambiguity are theory-defined formulas, honestly scoped to Gaussian
channels, and the engineering terms are labelled engineering *in the glossary
and in the badges*. What fails is one level up — G is over an action, C is a
channel-range floor, and "policy" is borrowed — and one level down: the
ambiguity term, correctly defined, *never changed a winner in 674 ticks*. A
theory-defined quantity that is inert is faithful and decorative at once.
R5-coverage is theory-defined *only in Lean*: the glossary has no noun for it,
the code has no caller for it, and the corpus has no key for it.

## 2. What the corpus holds — checked 2026-08-30

- **The key that held G_core changed name mid-corpus without a receipt.** 760 records carry `{:G-total :G-pragmatic :G-epistemic}` (through 07-09); 31 carry `{:controller-score :preference-gap-score :coverage-uncertainty-pressure}` (from 07-14). None carries both. `:G-total` no longer exists in source (`free_energy.clj:72` renamed it). A reader of the old key after 07-14 gets `nil` and cannot tell it from "no score".
- **No coverage report anywhere**: zero `:coverage-statement` / `:criterion` / `:uncovered` keys in 791 records; `coverage_check.clj`'s `check-coverage` reads `[:payload :coverage-statement]` and returns `:unwitnessable` for every real close (E-R5 review turn).
- **Candidate sets are large**: `:ranked-actions` min 4, median 112, max 218 — G_core is computed 100,292 times across the corpus and, per the badge, the ambiguity leg moved none of those rankings.

## 3. What a formal R5 would have to state — signatures, in dependency order

**Definable now — R5-EFE at channel grain:**

```
Channel      : Type                               -- 14, enumerated
Gaussian     := ℝ × ℝ>0                            -- (μ, σ²)
Pref         := Channel → Gaussian                 -- C at channel grain (the "floor"; stack-defined content)
predict      : Belief → Action → Channel → Gaussian   -- R4's forward model
risk         : (Channel → Gaussian) → Pref → ℝ     -- Σ_ch w_ch · KL(N(μ,σ²) ‖ C_ch)      [free_energy.clj:44]
ambiguity    : (Channel → Gaussian) → ℝ            -- Σ_ch ½ · ln(2πe · σ²_ch)             [efe.clj:37]
gCore        : Belief → Action → ℝ                 -- risk + ambiguity                       [efe.clj:750]
```
Record contracts, statable now: `∀ candidate, candidate.gCore = risk … + ambiguity …`
(invariant I3, "0 violations live"); `∀ candidate, candidate.controllerScore ≠ candidate.gCore`
unless all six augmentation terms are zero — i.e. the core is *persisted apart*,
which is the paper's actual R5 claim and is true. Refusing witness for the
plausible fix: a `:controller-score` that equals `gCore` because the other
terms were zeroed is not "an EFE core governing selection"; and a corpus in
which `argmin gCore = argmin controllerScore` in every tick would show the
augmentation decorative, while one in which ambiguity never changes the argmin
shows the *core* decorative — the badge measured the second.

**Definable now — R5-coverage (already in Lean):**

```
CriterionSet (Outcome) := { covers : Outcome → Bool }          [CoverageReport.lean:44]
Report                 := scored ℤ ∣ uncovered ∣ absent          [:49]
declaresCoverage       : CriterionSet → Prop   -- ∃ o, covers o = false      [:67]
outsideIsTyped         : ∀ o, ¬covers o → evaluate o = uncovered            [:108]
coverageReported       : declaresCoverage ∧ outsideIsTyped ∧ inhabitedHandle ∧ typedAbsence   [:116]
```
Polarities proved: accepting `coverage_reported_nonvacuous`; refusing-broken
`warm_customer_pays_uncovered_and_unrecorded_is_refused`; refusing-plausible-fix
`adding_a_channel_does_not_satisfy_coverage`. What is **blocked** is not the
formalism but the *instance*: an `Outcome` type for a flight (the fourteen
dispositions are named as an intended carrier) and a caller.

**Blocked:**

```
G(π)   : Policy → ℝ            -- needs Policy, Q(o∣π), an outcome space; PolicyGrade gives only the naming conditions
EIG(π) : Policy → ℝ            -- Σ_o Q(o∣π) KL[Q(θ∣o,π)‖Q(θ∣π)]; kernel exists, "UNWIRED" on Q(o∣π)
C over outcomes                -- "an outcome is not a reward, so a C over these is a further stateable choice. Not made."
```

## 4. R5's internal wiring as typed deliveries

| edge | from → to | payload | guarantee as built | the undeclared field |
|---|---|---|---|---|
| e1 | `predict` → `risk`, `ambiguity` | per-channel Gaussians | typed, per channel | — (the well-typed edge) |
| e2 | `gCore` → `controller-score` | ℝ (nats) + six terms in other units | linear sum at hand-set weights; "core diluted" | `payload` type: nats + incommensurate units summed — the delivery that loses the type, labelled honestly and delivered anyway |
| e3 | `rank-actions` → `select-action` | ranked list by `:controller-score` | ascending sort | `receipt`: which term decided (`:governed-by` exists for 4 records only) |
| e4 | `:G-total` → `:controller-score` (07-14 rename) | the same slot | **key renamed, no receipt** | `idem-key`: a consumer of the old key reads `nil` |
| e5 | criterion set → coverage report → close | `:coverage-statement` | **no edge**: `check-coverage` has no caller | the ring itself: an evaluate stage with no delivery for "outside" |

## 5. R5's evidence contract — draft for the apex

```
subject      R5-EFE                                            R5-coverage
claim        the persisted core is risk+ambiguity and it        every close carries a coverage statement; an outcome
             is distinguishable from the controller total        outside the criteria is recorded as uncovered, not absent
artefact     ranked-action entries: :G-risk :G-ambiguity        a flight close with [:payload :coverage-statement]
             :G-core :controller-score
domain       every candidate on every tick                      every close, any outcome (the fourteen dispositions)
corpus       wm-trace (reader loop; note the 07-14 key rename)  data/wm-full-loop closes
method       recompute risk+ambiguity from the stored Gaussians;  run check-coverage over closes; count :unwitnessable
             count ticks where argmin core ≠ argmin total
falsifier    a tick where :G-core ≠ risk+ambiguity (none);       a close whose outcome is outside its criteria and
             ambiguity within-tick sd ≈ 0 across the corpus      whose report is absent  ← every close today
             ← fires (0.0039): the term is defined and inert       (all :unwitnessable)
not-evidence :risk-mode :kl "FLIPPED LIVE" as evidence of        Lean theorems offered as evidence that closes are
             influence (a flag, not a flip count);               covered; a fold's :coverage-score-delta (a different
             the 07-09 records' :G-total after the rename        "coverage") offered for the criterion sense
```

## 6. Glossary → specification: the `Formal:` line each entry would carry

- *Expected free energy G*: `Formal: gCore at channel grain — definable; G(π) BLOCKED on Policy, Q(o∣π)`.
- *Risk*, *Ambiguity*: `Formal: definable (Gaussian channel forms); ambiguity measured inert (0 flips / 674)`.
- *Observation model A*: `Formal: 7×7 status matrix — a record shape; channel half 8/14`.
- *Model uncertainty and EIG*: `Formal: kernel exists (epistemic_value); no Q(o∣π) to feed it`.
- **missing entry** — *Criterion set / coverage report*: `Formal: CoverageReport.lean — defined and proved; no glossary noun, no caller, no key`.

## 7. What this worksheet does not do

It does not decide whether an inert ambiguity term is a defect (the badge's
own reading: "argmin hears σ not μ") or a property of this corpus. It does not
pick the Outcome carrier for the coverage report — the excursion's S1–S3 slices
were drafted for `codex-22` and never sent. It records that the R5 the paper
certifies is real and honest and never moved a decision, and that the R5 the
ring is red for is proved in Lean and unreachable from any running edge.
