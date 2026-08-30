# P-markov-category-spec — the stochastic part of the War Machine, stated in a Markov category

**Status:** DRAFT problem record, 2026-08-30 (claude-15, from Joe's direction). v2 form
(`futon4/holes/delivery-lifecycle.md`). S1 fields drafted from Joe's words, his to confirm.
Nothing here is a build packet.
**Gate:** operator-acceptance — Joe.

## 0. In Joe's words, and the answer to "which node"

> *"The next promising thing is the stuff about Markov categories. That was less of a gap of
> the nature of a facade, and more a discovery of something that simply hadn't been built at
> all, but that sounded very appropriate. I don't remember which node that was supposed to be
> associated with, or if Markov categories were supposed to span the overall graph. I think we
> should look into that and see if we can get that specification developed."* (2026-08-30)

**Which node.** It was assigned narrowly and then found to be wide:
- **Assigned:** `M-formal-war-machine` §2.1e — *"uncertainty — which action, given a
  distribution — Markov categories — family 8 (`factorsThroughDiscard` is already a
  Markov-shaped statement), ⑯"* — i.e. **R14**, the selection seam.
- **Built there:** `CommitmentTemperature.lean:236–292`, the finite copy–discard section:
  `factorsThroughDiscard`, `factorsThroughDiscard_iff_temperatureInvariant`,
  `not_governs_iff_factorsThroughDiscard` (R14's requirement restated: τ governs exactly when
  the selector does not factor through ε_τ), `discard_absorbs_upstream` (data processing as
  the counit law `f ; ε = ε` — `repairing_r8_changes_no_action` obtained by composition, not
  by hand), `live_selector_factors_through_discard`. Four theorems, no Mathlib, elaborates in
  seconds. This is the one piece of the formalisation that already states a *requirement* in
  the theory's own shape rather than a record's.
- **Then extended** (`NOTE-from-relations-to-kernels.md`, 08-27, Joe's proposal): an `@how`
  edge is a kernel `Edge → D(Outcome)` — support from the `core.logic` witness, mass from the
  edge's Beta posterior; a cascade is **Kleisli composition** of kernels; **ambiguity is the
  entropy of that channel**; and the claim that ties it together: *"the singularity criterion
  and the EFE ambiguity term are two readings of one object — `discriminates` asks whether the
  channel is constant; ambiguity asks how diffuse it is. Both are properties of the morphism,
  in the same category."* Stated there as **a claim, not a theorem**.

So the honest answer is: **it spans the stochastic morphisms of the loop, and nothing else.**
The mission's own table keeps the other formalisms for the other objects — order theory for
the authority structure (cascades as DAGs), BV for the trajectory, plain record predicates
for consistency. The Markov category is the formalism for every arrow in the loop that
carries a *distribution*:

| node / edge | the morphism | status |
|---|---|---|
| R4, forward model | a kernel `State × Action → D(State)` (the paper: "the same kernel and adds principled uncertainty") | stack-defined; not stated categorically |
| R3, belief update | Bayesian inversion of the observation kernel (a Markov-category notion — `Stoch` is a *positive* Markov category, which is what conditionals need) | stack-defined formula; not stated |
| R5, Q(o∣π) and ambiguity | the composite kernel along the cascade; ambiguity = its entropy; risk = KL of its pushforward against C | the composition is **unbuilt** (codex-22, 08-30: "no Kleisli composition is evidenced"); ambiguity is computed, and inert |
| R6/R13, cascades | Kleisli composition of per-pattern kernels; `find` may itself be a kernel (the slush samples) | P-validated-R5 §3e: implementation-swappable behind the laws |
| **R14, selection** | `softmax` is a stochastic morphism; the live `argmax` is a deterministic one post-composed that forgets it; *governs* = ¬ factors through ε_τ | **built, finite case** |
| R12, calibration | a comparison of two kernels (predicted vs witnessed) — L2 requires the witnessed one not to be the model's | not stated |
| the whole loop | a composite morphism `State → D(State)` per tick — what "the behaviour of the overall system" is, categorically | not stated; the prereg's Observables are its predicates |

## 1. The problem record (S1 — Joe's fields)

```
problem:   The loop's stochastic arrows — forward model, belief update, Q(o∣π), ambiguity,
           selection — are each computed by a different piece of code with its own vocabulary,
           and nothing states them as morphisms of one category, so "the channel is constant"
           (R14 red), "the channel is diffuse" (R5's ambiguity), and "the kernel composes along
           the cascade" (R5's Q) cannot be related, proved, or refused together.
                                                                        [Joe: confirm/rewrite]
now:       CommitmentTemperature.lean:236–292 — finite copy–discard structure and 4 theorems (R14 only)
           NOTE-markov-categories-for-sensitivity.md, NOTE-from-relations-to-kernels.md (08-27)
           how_witness_snatch.clj returns a SUPPORT (four bindings), not a distribution
           intrinsic_values.clj / aif2/tension.clj: Beta posterior per class / per entry — the mass
           Mathlib.Probability.Kernel.Category.Stoch: IMPORTS AND ELABORATES in mathlib4 as of
             2026-08-30 (StochHom : MorphismProperty SFinKer; Stoch a wide subcategory; Δ, ε,
             Deterministic; PositiveCategory) — the 08-27 toolchain failure is repaired
           codex-22 (08-30): Kleisli composition "not evidenced" in Snatch; the @why graph is
             authority, not transitions — the DAG is not yet a diagram of kernels

solved:    (a property of the MODEL, before running)
           A Lean module — finite/discrete first, Stoch when P(π) is sampled — in which:
           1. Outcome, State, Action are objects; the forward model, the observation kernel,
              each pattern's kernel, Q(o∣π), and the selector are morphisms; a cascade's Q is
              the Kleisli composite of its patterns' kernels along the DAG;
           2. R14's requirement is ¬ factorsThroughDiscard (done) and the live selector's
              defect is stated as "deterministic post-composition forgets a stochastic
              morphism" — determinism = commutes with copy;
           3. THE TWO-READINGS CLAIM IS A THEOREM: for a kernel k, `discriminates k`
              (not constant; ¬ factors through discard of its input) and `ambiguity k`
              (entropy of its output) are properties of the same morphism, with the
              relation between them stated (a constant kernel has ambiguity = the entropy
              of one distribution; discrimination is a lower bound on the mutual
              information the input carries) — and the R14 finding I(τ;action)=0 and the
              R5 finding "ambiguity moved 0 winners" are ONE statement about ONE morphism;
           4. data processing is the counit law, used (not re-proved) wherever a chain
              passes through a discard;
           5. every normalisation (Rel → Stoch) is a DECLARED prior (S-G3), visible in the
              artefact, never absorbed;
           6. the model REFUSES: a kernel with mass everywhere (the stochastic witness that
              cannot fail — T1512Z); a "kernel" that is a table of scores with no
              normalisation; entropy over undeclared coordinates; a partial relation
              presented as total.                                        [Joe: confirm]

facades:   Rel presented as Stoch — support without a declared normalisation
           a score table called a kernel — no D(Outcome), no composition law
           entropy over the wrong coordinates ("a number, not a measurement" — the note's own bound)
           a deterministic argmax reported as if it were the softmax it forgets (R14's live defect)
           a "composition" that is relational reachability, not Kleisli (codex-22 Q2)
           importing Stoch as evidence that anything is stated in it (dark build)

owner:     joe; definition work by a Claude seat with Lean; the one-edge pricing step by codex
status:    open
deliveries: none
```

## 2. Gate 0 over the terms — and the one that blocks

| term | class | note |
|---|---|---|
| copy Δ, discard ε, Markov category, `factorsThroughDiscard`, Deterministic | **theory-defined** (Fritz 2020; Mathlib; the finite case in Lean) | the only fully theory-defined vocabulary in the whole formalisation |
| kernel `Edge → D(Outcome)` | theory-defined as a type; **blocked on Outcome** (P-validated-R5 step 1) — "entropy over the wrong coordinates is a number, not a measurement" | the same first decision as everywhere else |
| the normalisation (Beta(1,1) per edge) | a **stipulation** — S-G3: declared, never absorbed | fine, if visible |
| ambiguity = entropy of the channel | theory-defined once the channel is | |
| discriminates | theory-defined (`¬ factorsThroughDiscard`) | done |
| Kleisli composition along the DAG | theory-defined operation; **its applicability is unevidenced** (codex-22 Q2): the `@why` DAG is authority, not a wiring of kernels; what composes is the *cascade as constructed for a target* (§3d′ of P-validated-R5), whose edges must be kernels' inputs/outputs | the R6/R14 decision helps: construct-to-match gives the diagram |

**The gate:** everything here is theory-defined except the operand — Outcome. The Markov
category is the first formalism in this programme where the *theory* is not the missing
piece; the *carrier* is. That is why Joe's instinct is right that this is "not a facade":
the vocabulary exists and is proved in one place; what does not exist is the object it
would be applied to.

## 3. Order of work — all definition, priced at one edge first (the notes' own rule)

1. **Outcome carrier** (shared with P-validated-R5 step 1; shape now proposed there, §2a: a tagged sum over the tetrahedron's vertices with C per vertex and a uniform two-kind valuation, pragmatic / epistemic-state / epistemic-parameter) — the fourteen dispositions, or a
   treatment-indexed leaf set for Snatch. Without it, nothing below is a measurement.
2. **The one-edge pricing step** (`NOTE-from-relations-to-kernels` §Next step): rewrite
   `how_witness_snatch.clj` to return a *distribution* — the same relation for the support,
   `credit-for` for the mass, the entropy reported — and check the mirror still has zero
   mass. This prices the Markov step exactly as the `core.logic` step was priced, on one edge,
   before anything is planned on it. A Codex packet; refusal permitted ("the edge's support
   does not admit a normalisation without a choice the record does not contain").
3. **The two-readings theorem, finite case** — in `CommitmentTemperature.lean`'s style:
   kernels as functions `X → Dist Y` over finite types, `discriminates` and `ambiguity` as
   properties of one value, the relation between them proved, the R14 and R5 findings as its
   two refusing witnesses. No Mathlib.
4. **The forward model and Q(o∣π) as morphisms**, composed for one constructed cascade
   (Snatch's G4 run, then a coding case) — the first point at which "the loop is a composite
   morphism" is a statement about a run rather than a diagram.
5. **`Stoch`**, when `P(π)` is sampled (repair option (a) for R14) — the trigger the 08-27
   note named; the library is now importable, so the cost is the measure-theoretic
   statement, not the environment.

## 4. What this record does not decide

Whether the discrete case suffices for the whole programme (the notes say yes until sampling
arrives); whether Bayesian inversion of the observation kernel is the right statement of R3
or over-reaches what `belief.clj` does; and the Outcome carrier, which is Joe's (step 1) and
is the same decision three records now wait on. It records that the Markov-category material
is the one place where the theory is written and the object is missing — the inverse of every
other gap found this week — and that its next step is the cheapest of all of them: one edge,
one distribution, one entropy, one mirror.
