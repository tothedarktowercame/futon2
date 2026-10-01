# E-kimi-task-166 — pattern-stage reading kimi-2-1790889986 (10 patterns)

**Requisition:** in-progress — dispatched 2026-10-01T21:26:26Z to kimi-2 as invoke-1790889986792-29863-b72ef1f0

Clocked in by pattern-stage-read-loop for kimi-2 on 2026-10-01 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

You are labelling design patterns for a count of what stage of an Active Inference control loop Joe's work touched. Label each pattern below from ITS OWN TEXT only (IF / HOWEVER / THEN / BECAUSE, else the `! conclusion`), never from its filename or directory.

kind (one):
- practice: a way of working a person or agent performs (review, plan, record, route, check).
- subject: what the work is about; content, a design decision, a domain technique (math formalisation techniques are always subject, even if imperative).
- mixed: genuinely both.

stage (one), with the gloss the existing 689 labels use:
- perceive: observes or exposes state
- believe: records, represents, or revises what is known
- evaluate: weighs consequences, options, or limits
- select: sets a choice, priority, or admission condition
- act: implements or executes the work
- assurance: makes a claim independently checkable or preserves its trace
- coordination: routes work, roles, or communication between parties
- none: the text states no control-stage operation (a lookup/encoding record, a bare meta-tag)

stage-mode: functional (the pattern PERFORMS that stage), topical (it is ABOUT that stage), or functional-and-topical.
confidence: high (clear from the text), medium, low.
node: optional, only if the text itself names an R-node (e.g. "R9"); else omit.
quote-field: IF, HOWEVER, THEN, BECAUSE or CONCLUSION.
quote: copy a span of 6-40 words EXACTLY from that field of the source shown (it is checked character for character after collapsing whitespace). Do not paraphrase, do not add ellipses.
rationale: one sentence: why this stage, citing the quote.

Answer: write /tmp/claude17/pattern-stage-read/kimi-2-1790889986.answer.json as a JSON array, one object per pattern:
  {"id": ..., "kind": ..., "stage": ..., "stage-mode": ..., "confidence": ..., "node": optional, "quote-field": ..., "quote": ..., "rationale": ...}
Rewrite the file after each pattern and check it parses: python3 -c "import json;print(len(json.load(open('/tmp/claude17/pattern-stage-read/kimi-2-1790889986.answer.json'))))"
Do all 10. Do not write anywhere else, do not commit, do not publish. Nobody needs belling; end with one line: the answer path and the count.


---
## math-formalization-CA/measure-restrict-simplify
source: futon3/library/math-formalization-CA/measure-restrict-simplify.flexiarg

```
@flexiarg math-formalization-CA/measure-restrict-simplify
@title Simplify Restricted Measure on [0,1]
@sigils [📊/測]
@keywords measure, restrict, volume, Icc, one_rpow, finite-measure
@audience lean-provers, analysis-students
@tone heuristic
@difficulty routine
@subjects analysis
@instantiates math-informal/reduce-to-known-result
@examples a00J01
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L8-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: 
  Collapse μ(univ) for volume.restrict (Icc 0 1) to 1, simplifying rpow and mul terms.

  + IF: Working with eLpNorm or integrals over volume.restrict (Icc 0 1).

  + HOWEVER: Lean doesn't auto-simplify μ(univ) = volume(Icc 0 1) = 1.

  + THEN: Chain: Measure.restrict_apply → Real.volume_Icc → ofReal_one → one_rpow → mul_one.
    + LEAN:
      api[Measure.restrict_apply] api[Real.volume_Icc] api[ENNReal.ofReal_one] api[one_rpow]
      tactic[rw [...] → rw [...] → exact h]

  + BECAUSE: On probability-like spaces, μ(univ) = 1 collapses most comparison lemma outputs.
```


---
## math-formalization-FA/lf-map-continuity-via-fixed-support-stages
source: futon3/library/math-formalization-FA/lf-map-continuity-via-fixed-support-stages.flexiarg

```
@flexiarg math-formalization-FA/lf-map-continuity-via-fixed-support-stages
@title Prove LF Test-Function Operator Continuity Stage by Stage
@keywords TestFunction, LF-topology, continuous-linear-map, compact-support, seminorm, limitCLM
@audience lean-provers, functional-analysts
@tone procedural
@difficulty advanced
@subjects applied

! conclusion:
  Prove continuity of an operator on compactly supported smooth test functions
  by factoring its restriction to every fixed compact-support stage through a
  controlled target stage, establishing seminorm bounds there, and invoking
  the LF-space final-topology criterion.

  + IF:
      An algebraic linear operator on `TestFunction` is mathematically
      continuous, but no packaged global continuous linear operator exists and
      direct continuity on the LF topology is opaque.

  + HOWEVER:
      The input support varies globally, while the available derivative and
      integral estimates have constants and output supports that depend on a
      fixed input compact. A single global norm estimate is therefore the wrong
      obligation.

  + THEN:
      For each compact `K`, choose a compact `K'` containing the output support
      of every input supported in `K`. Define the induced linear map from
      `ContDiffMapSupportedIn ... K` to `ContDiffMapSupportedIn ... K'`. Prove
      its continuity with `WithSeminorms.continuous_of_isBounded`, supplying for
      each target seminorm a finite set of source seminorms and a nonnegative
      constant. Finally use `TestFunction.continuous_iff_continuous_comp` to
      reduce global continuity to these stage maps and compose each with
      `TestFunction.ofSupportedInCLM`.

  + BECAUSE:
      The test-function topology is an inductive-limit topology. Its continuity
      API is designed to consume compatible continuous maps on fixed-support
      Fréchet stages, exactly where support-dependent seminorm estimates are
      meaningful.

  + NEXT-STEPS:
    next[Prove a uniform output-support bound for each fixed input compact.]
    next[Separate the order-zero seminorm estimate from successor derivative estimates.]
    next[Package each stage map continuously, then apply the TestFunction final-topology criterion.]
```


---
## math-formalization/additive-principal-parts-from-order-germs
source: futon3/library/math-formalization/additive-principal-parts-from-order-germs.flexiarg

```
@flexiarg math-formalization/additive-principal-parts-from-order-germs
@title Build Additive Principal Parts Locally From Order Germs
@sigils [🧩/分]
@keywords meromorphicOrderAt_eq_int_iff, AnalyticAt.exists_eq_sum_add_pow_mul, principal part, partial fractions, meromorphicOrderAt_add, meromorphicOrderAt_congr, Finset.analyticAt_fun_sum, simple pole, one_div_sub_hasFPowerSeriesOnBall_zero
@audience lean-provers, complex-analysis
@tone procedural
@difficulty hard
@subjects complex-analysis
@instantiates math-informal/construct-auxiliary-object
@see-also math-formalization/normalize-meromorphic-point-values-before-continuation math-formalization/compact-thickening-upgrades-pointwise-analyticity
@examples a95J04
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L8-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion:
  When you must subtract finitely many simple principal parts from a
  meromorphic function and Mathlib's divisor machinery only factorizes,
  build each principal part locally from the order germ and prove the
  remainder has nonnegative order; no global additive decomposition
  theorem is needed.

  + IF: Every boundary pole has order exactly `-1`, the pole set is
    finite, and the goal is `f = g + ∑ ζ ∈ P, c ζ / (z - ζ)` with `g`
    holomorphic on a larger disk (e.g. to bound Taylor coefficients).

  + HOWEVER: Searches of `CanonicalDecomposition`, `FactorizedRational`,
    and `MeromorphicOn.extract_zeros_poles` find only multiplicative
    factorizations; the additive form is absent from the library.

  + THEN: (1) At a pole, `(meromorphicOrderAt_eq_int_iff hF).1 horder`
    gives `q` analytic with `q ζ ≠ 0` and `F =ᶠ (z - ζ)^(-1) • q`; shift
    with `comp_sub` and apply `AnalyticAt.exists_eq_sum_add_pow_mul 1` to
    write `q z = q ζ + (z - ζ) * H z`, so `F =ᶠ[𝓝[≠] ζ] H + q ζ / (z - ζ)`.
    (2) `choose c hc` over all poles. (3) The finite sum `S` of principal
    parts is `AnalyticAt` off `P` (`Finset.analyticAt_fun_sum`,
    `AnalyticAt.div`), so at `z ∉ P` use `meromorphicOrderAt_add` and at
    `z ∈ P` use `meromorphicOrderAt_congr` with `Finset.sum_erase_add`
    to see `F - S` has nonnegative order. (4) Normalize and thicken the
    remainder. (5) Coefficients of `c / (z - ζ)` for `‖ζ‖ = 1` come from
    `Complex.one_div_sub_hasFPowerSeriesOnBall_zero` and all have norm `‖c‖`.
    + LEAN:
      api[meromorphicOrderAt_eq_int_iff] api[AnalyticAt.exists_eq_sum_add_pow_mul]
      api[meromorphicOrderAt_add] api[meromorphicOrderAt_congr]
      api[Finset.analyticAt_fun_sum] api[Finset.sum_erase_add]
      api[Complex.one_div_sub_hasFPowerSeriesOnBall_zero]

  + BECAUSE: A local germ of order `-1` carries exactly the data of its
    Laurent tail, so the classical partial-fractions construction is a
    one-line consequence of the order characterization plus a truncated
    germ Taylor expansion, and order additivity does the bookkeeping.
```


---
## math-formalization/memlp-power-law-scalar-thresholds
source: futon3/library/math-formalization/memlp-power-law-scalar-thresholds.flexiarg

```
@flexiarg math-formalization/memlp-power-law-scalar-thresholds
@title Reduce Concrete MemLp Goals to Scalar Power-Law Summability
@keywords MemLp, Lp, integrable_norm_rpow_iff, summable_nat_rpow, integrableOn_Ioi_rpow_iff, counting measure, power law, counterexample, exponent threshold
@audience lean-provers, analysts
@tone procedural
@difficulty advanced
@see-also math-formalization-CA/measure-integration-api math-formalization-CA/lp-norm-comparison math-formalization/coercion-bridge
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L8-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion:
  To prove or refute `MemLp f p μ` for a concrete power-law f (on ℕ with
  counting measure, or on ℝ with volume), do not reason about `eLpNorm`
  directly: rewrite with `integrable_norm_rpow_iff`, normalize the exponent
  with `(by fun_prop)` measurability and `ofReal`-positivity side goals, push
  the goal to the scalar library (`Real.summable_nat_rpow` on ℕ,
  `integrableOn_Ioi_rpow_iff` on ℝ), and settle the pure-real inequality on
  the exponent.

  + context:
      f is an explicit sequence like (n+1)^(-s) or an indicator-supported
      tail |x|^(a · χ_{|x|≥1}); the goal says ‖f‖_p < ∞ but ‖f‖_q = ∞ (or
      vice versa) for two exponents on either side of the critical threshold.

  + IF:
      The function is nonnegative and explicitly given, so measurability is
      `fun_prop` / `Measurable.ite` from continuity away from one point, and
      the norm power collapses to the same power law with exponent scaled by
      p or q.

  + THEN:
      (a) `rw [← integrable_norm_rpow_iff hmeas (by ... ofReal ≠ 0) (by simp)]`.
      (b) Discharge `(ENNReal.ofReal p).toReal = p` via `simp [hp.le]` with
          `hp : 0 < p` extracted first (`linarith`).
      (c) Restate the integrand by `funext` + `Real.rpow_mul` so the goal is
          exactly the library's n ↦ n^(-s) (or x ↦ x^a on Ioi 1) shape.
      (d) On ℕ, shift the successor index with `summable_nat_add_iff` and a
          `simpa only [Nat.cast_add, Nat.cast_one]` iff-transfer before
          `Real.summable_nat_rpow`; on ℝ, restrict to `Ioi 1` and use
          `integrableOn_Ioi_rpow_iff` (or `..._of_lt` for the positive half),
          composing with `.comp_neg` for the negative half.
      (e) The final goal is `s < -1 ↔ ¬(-1 ≤ s)` — pure algebra, proved once
          and reused for both exponents and both conjuncts.

  + BECAUSE:
      The mathematical content is entirely in the scalar threshold; everything
      between the `MemLp` goal and the scalar library is coercion
      (`ofReal`/`toReal`), index shift, and indicator bookkeeping. Naming the
      reduction keeps each counterexample's proof to a discharge of steps
      (a)-(e), and the same exponent lemma closes both the sequence and the
      real-line case.

  + NEXT-STEPS:
    next[Extract 0 < p, 0 < q by linarith before touching ENNReal.]
    next[Rewrite MemLp with integrable_norm_rpow_iff; prove measurability separately if f is defined by cases.]
    next[Shift/split the domain to hit the library statement exactly.]
    next[Prove the exponent inequality once and reuse for both conjuncts.]
```


---
## math-informal/work-examples-first
source: futon3/library/math-informal/work-examples-first.flexiarg

```
@flexiarg math-informal/work-examples-first
@title Work Examples First
@sigils [🐴/二]
@keywords example, compute, concrete, instance, calculate, test, verify, intuition, experiment
@audience mathematicians, students, problem-solvers
@tone heuristic
@factor Keen investigation (dhammavicaya)
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L8-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: 
  Build intuition from concrete examples before attempting a general proof.

  + context: You have a conjecture or a definition and want to understand what it really says.

  + IF:
      The statement is abstract or involves multiple quantifiers, and you cannot yet see why it
      should be true (or false).

  + HOWEVER:
      Examples alone do not prove theorems, and poorly chosen examples can mislead by being too
      special or too symmetric.

  + THEN:
      Compute several concrete instances. Choose examples that are generic (not too symmetric),
      that test boundary conditions, and that span different regimes of the parameters. Record
      what each example teaches you about the conjecture.

  + BECAUSE:
      Examples are the empirical base of mathematics. A conjecture you cannot verify on a single
      instance is a conjecture you do not understand.

  + NEXT-STEPS:
    next[Pick 3-5 examples spanning different regimes.]
    next[Compute each one explicitly.]
    next[Record which features of the examples matter for the conjecture.]
```


---
## math-strategy/hypothesis-category-check
source: futon3/library/math-strategy/hypothesis-category-check.flexiarg

```
@flexiarg math-strategy/hypothesis-category-check
@title Hypothesis Category Check
@sigils [🔬/类]
@keywords category, smooth, PL, topological, algebraic, scheme, variety, hypothesis, mismatch, regularity, bridge
@audience mathematicians, proof-writers
@tone heuristic
@factor Discernment (panna)
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L8-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: 
  When invoking a theorem, verify that your objects live in the category
  (smooth, PL, topological, algebraic, measurable, ...) that the theorem
  is stated for.

  + context: You want to apply a known result to objects in your proof,
    and the result's statement specifies regularity or categorical
    hypotheses on its inputs.

  + IF:
      The theorem you want to invoke is stated for objects in category C
      (e.g., smooth manifolds, algebraic varieties, measurable functions),
      and your objects live in a different category C' (e.g., PL complexes,
      schemes, continuous functions).

  + HOWEVER:
      Category mismatches are a common source of silent errors in proofs
      that cross domain boundaries. A theorem about smooth Lagrangian
      intersections does not apply to polyhedral creases. A result about
      algebraic varieties may fail for general schemes. The mismatch may
      be bridgeable (via a smoothing lemma, GAGA, regularity bootstrap)
      but the bridge itself requires proof.

  + THEN:
      (a) State the category of the theorem's hypotheses explicitly.
      (b) State the category of your objects explicitly.
      (c) If they differ, provide a bridge: a smoothing lemma, a
          regularity result, a comparison theorem, or a specialization
          argument that places your objects in the required category.
      (d) If no bridge exists, the theorem does not apply -- find an
          alternative or prove a new version in your category.

  + BECAUSE:
      Cross-category theorem application is one of the most common gaps
      in proofs that invoke external results. The fix is mechanical:
      check the fine print. The cost of checking is minutes; the cost
      of not checking is a critical reviewer finding.

  + NEXT-STEPS:
    next[State the theorem's category explicitly.]
    next[State your objects' category explicitly.]
    next[If they differ: prove a bridge or find an alternative.]
    next[Check boundary cases where category differences bite hardest.]
```


---
## mmca/threshold-shaped-events
source: futon3/library/mmca/threshold-shaped-events.flexiarg

```
@flexiarg mmca/threshold-shaped-events
@title Check the Episode Length Against the Detection Threshold
@keywords artifact, threshold, event-counting, turnover, flicker, hysteresis, detector
@audience anyone counting events defined by a threshold crossing
@references [mmca/eye-before-metric mmca/persistence-over-appearance]
@tone declarative
@style pattern

! conclusion: When events are defined by a detector with a threshold — "frozen" means unchanged for W steps, "active" means above level L — plot the distribution of episode DURATIONS before reporting any count. If the median episode length equals the threshold, you are counting detector crossings rather than phenomena, and the count can be inflated by an order of magnitude.

  + context: Measuring turnover, switching, or event rates in a system where the event is defined by a state persisting past some detection threshold.

  + IF:
    Events are counted as transitions of a thresholded indicator.

  + HOWEVER:
    A threshold detector fires on the phenomenon AND on noise that momentarily satisfies it, and the two are indistinguishable in the count. In the MetaCA study "frozen" was defined as unchanged for 4 steps, and the measured turnover was 73 thaws per column. The median frozen episode was 4.0 steps — exactly the threshold — so most counted episodes were threshold crossings. Raising the threshold to 15 steps gave 14.2 per column: the original figure was inflated thirteenfold, and the visual impression of structure survived at the higher threshold only in part.

  + THEN:
    Compute the duration distribution of episodes, and compare its median and its lower quantiles against the threshold W. If median ~ W, raise the threshold until the distribution's mass sits clearly above it, and report the threshold with every derived number. Prefer reporting the tail (episodes > kW) over the raw count.

  + BECAUSE:
    The threshold is a free parameter of the measurement, and any quantity that changes by 13x under a defensible change of it is not a property of the system. Reporting the count without the threshold hides a choice that dominates the result. Duration distributions expose it in one plot.

  + NEXT-STEPS:
    - The same check applies to spatial thresholds (minimum region size) and to hysteresis bands.
    - When the threshold is raised, re-derive EVERY dependent statistic; region counts, mean widths, and turnover all move together.
    - evidence: episode duration median, p90, fraction exceeding 5x threshold, at two thresholds.
    - evidence-shape: {:threshold :int, :episodes :int, :median-duration :float, :p90 :float, :frac-above-5w :float}

  + DOES-NOT-APPLY:
    Detectors whose threshold is physically given rather than chosen (a hardware trigger level, a defined quorum).

  + INSTANCES:
    - MetaCA Part III (futon5): turnover reported as 73.2 thaws/column at W=4 and 14.2 at W=15; the published figure states its threshold in the caption and gives the reason.
```


---
## or/overlay-bridges
source: futon3/library/or/or.multiarg

```
@arg or/overlay-bridges
@sigils [💢/也]
@title Overlay Bridges
@audience journal-editors, platform-designers, research-office-staff
@tone formal-analytic
@style design-pattern
@part Communities & Pedagogy
@up or/communities-pedagogy
@see or/attribution-forward-review, or/bridge-before-portal
@next or/peripheral-to-core-pathways

! conclusion: Overlay Bridges [🚢/门 🌈/互]
  + context: Academic publishing remains dominated by legacy journals even as Open Research grows; authors still need career-legible outputs.
  + if: You want more open practices without severing links to recognised venues.
  + however: “Open-only” reforms can trigger resistance and duplicate labour; authors fear outcomes that “do not count.”
  + then: Build overlay bridges that route manuscripts through open, community-based review while retaining compatibility with target journals. Publish timelines and handoff packets (cover letter, review bundle) that show how reviews and revisions travel.
  + because: Overlay models shorten time-to-feedback, reuse reviewer effort, and preserve promotion-relevant outputs. They demonstrate coexistence rather than rupture between open and traditional systems.
  + next-steps:
      - Pilot an overlay handoff for this catalogue or a related paper.
      - Publish a generic Overlay SOP and handoff packet template.
      - Document one successful overlay and one failed attempt with lessons learned.
  + governance:
      - practice: track open-reviewed submissions that later appear in formal venues.
      - metric: number of reusable review bundles shared across platforms.
```


---
## or3/sustainability-without-enclosure
source: futon3/library/or3/sustainability-without-enclosure.flexiarg

```
@flexiarg or3/sustainability-without-enclosure
@title Sustainability Without Enclosure
@audience software PIs, KE and commercialisation leads, maintainers
@tone formal-analytic
@style design-pattern
@sigils [🔓/续 💷/衡]
@see or/license-laddering, or/maintainer-care, or3/borrow-a-training-network
@why [problems/transferable-open-research-practice]
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L10-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: Sustainability Without Enclosure

  + context: An open research tool has users, no ongoing funding, and a proof-of-concept grant to work out what happens next.

  + IF:
    You maintain openly licensed research software whose value depends on communities being able to adopt and adapt it freely, and you are being asked how it will sustain itself.

  + HOWEVER:
    The commercialisation conversation reaches for the license as the first lever — a closed core, a restricted edition, a paid tier gated by code — which removes precisely the property that made the tool worth adopting. The communities least able to pay are usually the ones the project exists to serve.

  + THEN:
    Treat the licence as fixed and search the rest of the space. Test revenue models that sell scarce things rather than the code: hosting and operation, adaptation and integration work, training and facilitation, partnership with organisations who need the capability delivered rather than built. Use the proof-of-concept phase to test which of these people will actually pay for, and keep the underlying platform, its educational materials, and any community-produced archive open throughout.

  + BECAUSE:
    Openness is what generates the adoption, partnerships and reuse that make the tool worth sustaining, so enclosing it to fund it destroys the asset being funded. Services are also a better match for what institutional and community partners actually want, which is rarely a copy of the source and usually a working thing plus someone to help them run it.

  + NEXT-STEPS:
    next[Write down which scarce good each candidate revenue model actually sells; discard any that sells access to the code.]
    next[Mint a DOI for the software so adoption and reuse become citable evidence for the next funding case.]
```


---
## p4ng/five-p-rhythm
source: futon3/library/p4ng/p4ng-orpatterns.multiarg

```
@arg p4ng/five-p-rhythm
@sigils [🚴/了]
@title Five-P Rhythm for Impact
@audience impact evaluators, project leads
@tone formal-analytic
@style design-pattern
@up p4ng/orpatterns
@next p4ng/fit-for-use

! instantiated-by: Five-P Rhythm for Impact
  + context: Impact planning lacks structured pacing.
  + if: You want credible impact development.
  + however: Ad-hoc timelines obscure progress.
  + then: Use a five-stage rhythm (problem, people, process, product, proof) to organise work.
  + because: Rhythmic structure supports sustained action and clearer evaluation.
  + next-steps:
    - Log one concrete instance of this pattern in the futon3 ledger.
```
