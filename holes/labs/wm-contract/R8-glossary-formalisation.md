# R8 worked through the glossary — nouns, their Gate-0 class, and what a formal R8 would have to state

**Date:** 2026-08-30. **Author:** claude-15, at Joe's direction: *"we had done a
considerable amount of relevant work towards specifying the nouns, because we
had developed that glossary … it could have quite easily been turned into a
formal specification of what these AIF things actually are, and then the
glossary could have been the documentation of that formal specification …
look into the glossary, which components relate to R8, and try to work through
R8 in the same level of formality that we'd like to work through the whole
system eventually."*
**Status:** a worksheet, not a specification. Every "definable now / blocked"
verdict below is checkable against the cited line.
**Sources:** `p4ng/sec-glossary.tex` (33 entries); `p4ng/sec-catalog.tex` R8
paragraph; `futon2/src/futon2/aif/{free_energy,selection_gain,fold_realized,trace}.clj`;
`futon2/data/wm-trace/`; `mathlib4/DarkTower/WarMachine/GainChain.lean`;
`futon4/holes/delivery-lifecycle.md` §0.5–§0.9 (Gate 0, Gate 1, the apex).

---

## 0. First finding: "R8" is two nouns under one number

| | the paper's R8 | the excursion's R8 |
|---|---|---|
| name | *Present-Fit Mismatch as a Per-Tick Scalar* (`sec-catalog.tex:200`) | the WR-27 gain reading — "a loop is born instrumented for its gain" (`wr-overlay.edn`, `E-R8-red-ring-fill`, step ⑨) |
| quantity | **F**, variational free energy: "how badly the current belief explains the current observation" | **g**, the outcome-feedback selection gain, fed by expected-vs-realised outcomes |
| glossary entry | *Variational free energy F* | *Softmax and controller calibration* (τ_eff = 1/g) and *No self-certification* (L1/L2) |
| code | `free_energy.clj:110 compute-variational-free-energy` | `selection_gain.clj:124 update-selection-gain`, `fold_realized.clj` |
| trace key | `:variational-free-energy` (schema v7+) | `:selection-gain`, `:realized-outcome` |

The R8 promotion note (`promotion-tests.edn:98`) states R8 as "the per-tick
mismatch is recorded … routed into the outer-loop update" — F's noun with g's
wiring. The pattern map's "R8 ✓ Persisted trace. Real." was about a trace key
called `:free-energy`. Neither noun is wrong; the number conflates them, and
everything below is done twice.

## 1. R8's closure in the glossary, classified (Gate 0)

Classes from `delivery-lifecycle.md` §0.5: **theory-defined** / **stack-defined**
/ **borrowed name** / **undefined**.

| glossary noun | what the glossary says | class | code · trace · Lean |
|---|---|---|---|
| **Belief state μ** | "compact map of operational hypotheses … belief variance as EMA of squared miss + noise floor" | **stack-defined** — the hypotheses are the stack's 13 channels, 8 with likelihoods | `belief.clj` · `:mu-pre :mu-post` · none |
| **Observation vector o** | "standardized summary of what happened … normalized into named channels" | **stack-defined** | `observation.clj` · `:observation` · none |
| **Prediction error ε** | μ ← μ + αΠε, "one gradient step of predictive coding … Kalman gain" | **theory-defined as a formula**, over the stack-defined μ and o | `free_energy.clj:82` · `:prediction-errors` · none |
| **Precision Π** | "confidence expressed as a weight" | **theory-defined** (inverse variance; R7 badge `:derived-from-FEP`) | `precision.clj` · `:precision-state` · none |
| **Variational free energy F** | F = ½ mean_k(Π_k ε_k²), "the Laplace/Gaussian collapse … an operational alarm" | **theory-defined as a formula**, and honestly scoped | `free_energy.clj:110` · `:variational-free-energy` **only from schema v7** · none |
| **Expected free energy G** | G_efe(a) = D_KL[Q(o∣a)‖C] + 𝔼H[P(o∣s)] | theory-defined formula **over an action `a`**; the RealizedOutcome's `:expected-G` is the fold's coverage-ΔG, **not** G_efe | `efe.clj` · `:free-energy {:G-total …}` · none |
| **realized-G** | not in the glossary | **stack-defined**: coverage→ΔG over enacted boxes; from 07-08, bound−inhabited over reviewed endpoints | `fold_realized.clj:80` · `[:realized-outcome :realized-G]` · `GainChain.RealizedOutcome` (as a `Measurement`, not a number) |
| **Selection gain g** | "engineering calibration control, **not** an inferred variational policy precision"; τ_eff = 1/g | **stack-defined, honestly labelled** — and family 8 (`I(τ;action)=0`) says τ cannot move the pick | `selection_gain.clj` · `:selection-gain` · `GainChain.gainAdvances`, `CommitmentTemperature.governs` |
| **Policy π** | "the active-inference name for a pattern language/cascade when that composition is being scored" | **borrowed name** — in every RealizedOutcome, `:policy` is a **mission id string** (`"M-bayesian-structure-learning"`), not a cascade | `fold_realized.clj:79` · `[:realized-outcome :policy]` · `GainChain.Mission` |
| **tick** (*Clicks, attempts, cohorts*) | "a click is a single trigger of the loop; an attempt is the forward pass" | **stack-defined**; the record has `:timestamp`, the outcome has `:tick` = `currentTimeMillis` at a different moment (family 1, two clocks) | `wm_scheduled_run.clj:108` · `[:realized-outcome :tick]` · `GainChain.Tick`, `threadedIdentity` |
| **Fold** | "a checked construction plan … boxes, wires, terminals, policy-holes" | **stack-defined**, precisely | `fold_escrow.clj` · deposits · `GainChain.FoldOccurrence` |
| **Act-gate** | act iff S_cascade > 0 ∧ ΔS_coverage < 0; "engineering control quantities, not F or G" | **stack-defined, honestly labelled** | `enact.clj` · `:act-gate-verdicts`; `:cascade-score`/`:coverage-score-delta` all `nil` in the corpus (family 7) · none |
| **No self-certification** (L1/L2) | L1 = predicted G vs own realised G — "self-referential, so it **may never be reported as value evidence**"; only L2 (outcomes the model did not produce) certifies value | **theory-defined as a rule** | `reward_red_team.clj`, `flight_record_test.clj` · — · `PolicyGrade` S-G1–G4 (naming only) |

**Gate-0 verdict for R8.** Of twelve nouns, four are theory-defined (ε, Π, F as
formulas; L1/L2 as a rule), one is a borrowed name (π), the rest are
stack-defined — three of them *honestly labelled* as engineering in the glossary
itself (g, act-gate, realized-G). The gate does not pass, but it fails
differently for the two R8s:

- **R8-F** fails only on its *inputs* (μ and o are stack-defined) — the formula
  is the theory's. This is the better case.
- **R8-g** fails on its *own* noun (g is engineering by the glossary's
  admission) and on its conditioning (π is a mission id). It also violates a
  theory-defined rule the glossary states: g is built from L1 (predicted-vs-
  realised G) and used as value evidence for selection sharpness — exactly what
  *No self-certification* says L1 "may never be reported as." The glossary
  contains the refusal; nothing read it.

## 2. What the corpus actually holds — checked 2026-08-30

- **F is absent from every record that carries a realized outcome.** The 88
  outcome records (07-02..07-06) predate trace schema v7; their `:free-energy`
  map holds `:G-total :G-pragmatic :G-epistemic` — **G, under F's key**.
  `:variational-free-energy` first appears in later records (07-17: present).
  So "R8 ✓ Persisted trace. Real." (pattern map, 07-13) was true of a key,
  not of F.
- **g never moved.** 07-17: `:selection-gain {:selection-gain 1.0 :perf-history [] :samples 0}`.
  Family 8's `I(τ;action)=0` is not a hypothesis on this corpus; it is the
  state.
- **Every `:expected-G` is coverage-ΔG**, not G_efe. The "scale-match pin"
  (`fold_realized.clj`, claude-10 review 07-02) *prefers* the fold's delta-g —
  correct for scale, and the reason the expected leg is not the glossary's G.

## 3. What a formal R8 would have to state — signatures, in dependency order

Marked **now** (definable from the glossary + code today) or **blocked** (on a
noun that fails Gate 0), so the order of work is the order of this list.

**R8-F (present-fit).**

```
Channel      : Type                                   -- now: the 13 named channels (stack-defined, but finite and listed)
Belief       := Channel → ℝ × ℝ                       -- mean, variance      now (as a record shape; its semantics stack-defined)
Observation  := Channel → ℝ                            -- now
Precision    := Channel → ℝ≥0                          -- now
predErr      : Belief → Observation → Channel → ℝ      -- now: o_k − μ_k
F            : Belief → Observation → Precision → ℝ    -- now: ½ · mean_k (Π_k · (predErr k)²)   [glossary, eqanchor F]
```
Record contract: `∀ tick, tick.variationalFreeEnergy = F tick.muPre tick.observation tick.precision`.
Refusing witness: a record whose `:free-energy` holds `:G-total` and no F —
i.e. every one of the 88. **Definable now.** What it does *not* give: any claim
that F is *the* variational free energy of a generative model, because μ and o
are stack-defined; the glossary already says "operational alarm", and the
formal statement should say no more.

**R8-g (gain chain).**

```
Outcome      : Type                        -- BLOCKED: no outcome space for the WM (claude-13 08-29 §5 open 3);
                                           --   candidates: the 14 flight dispositions, or coverage ∈ [0,1]
Policy       : Type                        -- BLOCKED: borrowed name; today a mission id
Q            : Policy → Dist Outcome       -- BLOCKED on both; T1456Z gives an empirical instance over dispositions
expected     : Policy → ℝ                  -- now, but as coverage-ΔG, NOT G_efe — must be named `expectedCoverage`
realized     : Outcome → ℝ                 -- now, same caveat
perf         : ℝ → ℝ → ℝ                   -- now: (e−r)/(|e|+|r|+ε)   [selection_gain.clj:83]
g            : List (ℝ × ℝ) → ℝ            -- now: clamp(2^(gain·mean perf), 0.5, 2.0)   [r18-badges, selection_gain.clj:123]
τ_eff        : ℝ                           -- now: 1/g
```
Record contracts that *are* statable now (and are `GainChain.lean`'s content):
threadedIdentity (one tick), inhabitedHandle, durableBeforeFold, declaredDomain,
typedAbsence — plus **the L2 constraint the glossary demands and the code
lacks**: `g` may be updated only from (expected, realized) pairs whose
`realized` the model did not produce. Under the current wiring every pair is
L1 (coverage of the model's own fold), so the constraint's refusing witness is
the whole corpus. **Blocked** for anything that calls g a precision or π a
policy; **definable now** as an honestly-named engineering controller with its
record contract and its L2 refusal.

## 4. R8's internal wiring as typed deliveries (Gate 1, inside the node)

The silence chain of `E-R8-red-ring-fill`, with the field that was undeclared:

| edge | from → to | payload | guarantee as built | the undeclared field |
|---|---|---|---|---|
| e1 | `deposit-for-mission` → `deposits-by-id` | deposit set | **corpus-wide throw on one rejected deposit** (`actuator_a3.clj:149`) | `atomic-with`: all-or-nothing, never written down |
| e2 | `deposits-by-id` → `realized-outcome-grounded` | mission → CLean path | **`nil` outside a 4-entry map** (`fold_realized.clj:113`) | `receipt`: none; needs `RealizedOutcome ∣ DomainMismatch` |
| e3 | `close-loop!` → trace | judgement | **`(catch Throwable _ judgement)`** (`enact.clj:255`) | `receipt`: absent is indistinguishable from unchanged |
| e4 | trace → step ⑨ (fold into g) | `:realized-outcome` | **no-op on absence** (`sec-system` ⑨) | `guarantee`: at-least-once was assumed, never stated |
| e5 | ⑨ → `selection_gain` → τ_eff | (expected, realized) | pinned at 1.0 with 0 samples | `idem-key`: `:tick`, from a different clock than the record's (family 1) |

Each edge's guarantee was individually defensible; the composition is seven
weeks of silence. Every one of these is a field in `Delivery` (§0.6).

## 5. R8's evidence contract — draft for the apex (§0.8)

```
subject      R8-F                                   R8-g
claim        every tick carries F = ½ mean(Π ε²)    g is updated from L2 outcomes and moves τ_eff
artefact     tick record (schema ≥ 7)               RealizedOutcome record + :selection-gain state
domain       every tick                             any mission the selector can choose; ≥ 2 policies
corpus       futon2/data/wm-trace/ (wm_scheduled_run writes it; wm_outer_loop does NOT)
method       reader loop over top-level forms (NOT edn/read-string); recompute F from
             :prediction-errors and :precision-state; count outcomes; diff g across ticks
falsifier    a tick with :variational-free-energy    g = 1.0 after ≥ 2 outcomes with varying
             absent or ≠ recomputed F                realized-G  ← fires on the whole corpus today
not-evidence a key named :free-energy (holds G);     the 18/18 fixture test (−0.6 → −0.6);
             any record from schema < 7              88 outcomes on one policy offered for "any mission";
                                                     tests on `with-redefs` producers
```

## 6. Glossary → specification: what would change

If the glossary were the documentation of a formal spec rather than a tutorial,
each entry would carry one line it currently lacks — `Formal: <Lean name> |
no formal referent yet` — and the entries in §1 would read, today:

- ε, Π, F: `Formal: DarkTower.WarMachine.PresentFit.{predErr, precision, F}` — **writable this week from the glossary alone.**
- μ, o: `Formal: record shapes only; semantics stack-defined (13 channels)`.
- g, act-gate, realized-G: `Formal: engineering controller; record contract in GainChain; no AIF referent (by the glossary's own statement)`.
- π: `Formal: no referent — borrowed name; see delivery-lifecycle §0.5`.
- No self-certification: `Formal: L2 constraint on g — required, absent`.

That one line per entry is the Gate-0 inventory, kept where the nouns are
already defined, by the person who defined them.

## 7. What this worksheet does not do

It does not define an outcome space, a policy, or Q(o∣π) — those are the three
blocked items and the order they must land in (claude-13, 08-27/08-29). It does
not propose repairs. It records that R8-F is one Lean file away from a formal
statement with a refusing witness, that R8-g is an honestly-labelled controller
whose own glossary entry forbids the use it was put to, and that the two were
never distinguished by the number that names them.
