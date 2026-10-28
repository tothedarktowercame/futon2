# Filled META policies and the existing cascade G

2026-10-28, codex-1 for claude-11. Discovery only. Read the withdrawal at
`NOTE-outer-selects-missions-not-plans-2026-10-28.md:237–267`; the reverted
OuterMissionModel is not proposed as a dependency. Source pins inspected:
mathlib4 `fe1965d5c0f22a322629f45440e0aeb091ed53e5`, futon2
`8463ca3ddfbfebb4510da0dc4e2649720fdf009b`, futon3
`6899c80349a9851fb80e9e17763f0083552eeea9`.
Paths below are relative to the named repository. No scripts, tests, Lean builds,
live evaluations, reloads or clicks were run for this discovery.

## 1. Which existing G can score the instance?

**Yes, after interpretation into its existing typed inputs; not directly from
the six-slot EDN. There is an important outcome-vocabulary restriction on the
current canonical function.**

* mathlib4 `DarkTower/WarMachine/Holes.lean:29–35`: `Cascade P` contains
  `nodes : Set P`, `addedByOrganise`, `edges`, an acyclicity proof, and a
  `precedence : List P`. P is unrestricted; META pattern identities are not
  excluded. It has no six-slot binding record or injury-arm validation.
* `CascadeEFE.lean:50–64`: `Policy P` adds decidable membership; `Model P S O`
  supplies initial distribution, observation kernel, guards and optional
  interpreted state-transition kernels. S and O are generic types. The finite
  scoring interface requires finite S/O and decidable S, not mission/tactical
  classes. `resolve`/`policyKernel:66–99` refuses missing interpretations and
  executes the **first enabled** entry, not the whole enabled frontier.
* `CascadeEFE.lean:101–146,371–411`: prediction rolls the kernel from initial
  belief; observation is pushed through `ProbabilityKernel S O`; C is
  `Fin T → ProbabilityKernel Unit O`. `stepG` is **risk + ambiguity**;
  `totalG` sums it at t+1 over T. Risk is categorical KL with a support
  admissibility proof; ambiguity is expected observation entropy. Information
  is the state/outcome mutual information. `step_decomposition:318–323` and
  `total_decomposition:378–386` prove the equivalent **pragmatic cost minus
  state information** expression. One must NOT subtract that same state
  information again from risk + ambiguity. `scoreCascade` returns typed
  missing-interpretation/inadmissible-risk refusals.
* `CascadeEFEPolicies.lean:15–32,34–102` adds precedence admissibility and
  finite duplicate-free nonempty successfully scored families, proves refusal
  exclusion, normalized policy priors and lower-G/higher-prior ordering with
  equal habit factors. It does not fill slots, infer posteriors, or prove live
  correspondence. Its `CandidateFamily` uses the older list-kernel score.
* **Current canonical alias:** `GOverCascades.lean:35–76` makes
  `cascadeGrainG = CascadeCoApplyGNovelty.scoreCascadeCoN`. The older
  `cascadeGrainGListKernel` remains explicitly historical. The chain bridge
  only equates its risk+ambiguity part with co-application on chains, not on
  the META diamond. Using the old score merely to get generic O would change
  the specified semantics.
* `CascadeCoApplyG.lean:51–61,91–114`: `Candidate ι V` has optional finite
  interpreted patterns, an initial distribution over `Finset V`, and
  `observation : ProbabilityKernel (Finset V) EndingClass`. Co-application
  derives containment order from the patterns, rejects cycles and bad risk
  support, and uses the same `CascadeEFE.totalG` internally.
* `CascadeTransition.lean:22–44`: interpreted patterns have consumes,
  produces, forbids and bounded theta. The kernel adds produced tokens with
  probability theta. `PatternSlot:141–156` has an optional theta and token
  sets; it is **not** the META task-kind/target/etc slot structure.
* `CascadeCoApplyGNovelty.lean:40–69`: `CandidateN` adds positive Beta counts;
  the scorer subtracts expected novelty over the enabled frontier at each
  epoch. `PatternThetaNovelty.lean:43–52` distinguishes this closed-form
  approximation from exact expected posterior KL. The existing positive
  novelty witnesses are at `CascadeCoApplyGNovelty.lean:233–263`.
  `CoherentThetaCandidate.lean:43–48` additionally binds transition theta to
  the mean of those same counts; raw CandidateN does not enforce that itself.

Precise gaps for the META declaration:

1. No typed adapter here binds/validates the six slots, the two injury guards,
   the extra self-heal rearm obligation, and their evidence provenance.
   These are specified in futon3 `library/meta/meta-outer-policy-cascade.edn:19–63`.
2. No supplied interpretation maps each filled pattern to consumes/produces/
   forbids, theta/counts, initial belief and observation kernel. The EDN diamond
   (`:13–17`) also needs a proved correspondence to the inferred containment
   order; arbitrary token encodings are not automatically that diamond.
3. The canonical co-application score fixes O to the five classes
   `focused/related/unrelated/stopTheLine/notYetEvaluated`
   (`CTauClassPreference.lean:43–49`). The EDN's eleven named outcomes include
   resource measurements (`:65–78`); no faithful mapping of that whole vocabulary
   to five classes was found in the inspected interfaces. A class-compatible
   example can use the existing score unchanged. Full vocabulary support needs
   a justified projection or generic-outcome generalization of the **existing**
   score, not a new G and not silently dropping cost channels.
4. Numeric C, stochastic predictions, coherent counts, horizon and evidence
   update are not supplied by the slot labels. The EDN itself marks its numeric
   examples illustrative and inadmissible (`:95–99`). This remains a model-input
   obligation, not permission to treat example numbers as learned evidence.

## 2. What the 10-19 objective already establishes

`OuterCascadePolicyObjective.lean:8–42` defines identities
`{occurrencePolicy,target,cascadeDigest}` and a rational categorical information
certificate carrying concentrations, probabilities and supplied psi/log values.
Its checks enforce lengths, probability ratios, formula and nonnegative EIG;
they do not prove the supplied rational psi/log values are evaluations of the
analytic functions. `Evidence:51–68` carries risk, ambiguity, novelty, lnE and
gamma; risk and ambiguity are numbers, not derived from a cascade kernel.
`Evidence.G` is their arithmetic combination; objective is gamma*G-lnE.

`Carrier.Valid:70–82` proves complete duplicate-free menu/evidence correspondence.
`SelectionCertificate.Valid:84–89` requires a unique strict minimum of that
objective. Theorems `:91–96,103–115,125–154` cover strict optimality/logit order,
the information formula and tampering, novelty changing G, lnE changing only
the objective, unclamped G, rejection of exact ties and digest substitution.

It does **not** represent a filled META cascade: no slots, arms, token kernel,
or dependence on the cascade's composition. Nor does its type force tactical
meaning: its digest could identify an interpreted META policy. The tactical
cross-target meaning comes from its integration. `Requirements.lean:267–289`
equates its menu identities with compared policies and its selected digest with
the selected constructed cascade. The Clojure menu construction is below.
Therefore calling this file already a model of filled META policies would be
wrong; calling its generic identity record intrinsically tactical is also too strong.

Reuse identity/provenance and menu-completeness ideas, or the existing
objective-order algebra after its hypotheses are established. Do not use its
free numerical Evidence fields as a substitute for `cascadeGrainG`. Its strict
unique-minimum certificate excludes ties; the current finite frontier/tie-draw
theorems in `Proof2/OuterSelectionCanonicalG.lean:28–89` have different semantics.
Its lnE/gamma objective agrees with pure G ordering only under the stated
common-positive-gamma/constant-lnE hypotheses (`:73–89`). Existing Q2/Q4/Q8
correspondences need separate revision; no edit is proposed here.

## 3. Clojure readers, slot fillers, and actual scoring

There **is** earlier slot-filling code. Literal keyword searches alone miss it:

* futon3 `scripts/check-meta-outer-policy.clj:1–6` reads the actual library EDN;
  `:41–87` checks example slots/arms and computes hand-supplied g-terms. This is
  an illustrative contract checker, not a live prediction producer.
* futon2 `src/futon2/aif/meta_policy_constructor.clj:1–4,27–112` constructs
  six-slot templates from verified field rows and explicit adapters/resource
  envelopes, or typed exclusions. It expressly supplies no prediction/C/EIG/G.
  It currently emits one template per row, ID based on target (`:85–99`), not
  two moves for one target. `verify:115–210` recomputes against external authority.
* `src/futon2/aif/meta_outer_policy.clj:12–24,47–92,119–156,201–315` validates
  the supplied contract, slots, field and model, then scores Gaussian prediction
  inputs with `core-efe/g-efe` plus `epistemic-value/expected-information-gain`
  (`:281–292`). It expects candidates already filled. Its field-coverage check
  (`:158–199`) also requires one representation per target. This is not the
  co-application score and not a general same-target/two-move family today.
* Offline callers exist: `scripts/meta_slot_census.clj:144`;
  `scripts/rank_with_readings.clj:206–220` constructs templates and `:113–118`
  evaluates them. That script reads a *proposal* contract (`:12`), supplies
  explicitly provisional feature formulas (`:25–41`) and a zero-information
  model (`:42–45`), not the new requested nonzero novelty witness.
* `scripts/extended_four_numbers.clj:7–10` reads the original contract and a
  proposal to inspect the evaluator; `diamond_four_numbers.clj:9–29` and
  `consumption_four_numbers.clj:21–23` use the META pattern names for structural
  probes. None was executed during this discovery.

**Live path:** `meta_live_outer_selector.clj:874–912` constructs all eligible
target families, seals their menu, calls `outer-canonical-g/produce`, and
`outer-canonical-selection/evaluate`. `:925–930` seals the canonical selected
carrier. `wm/family_selection.clj:142–206,404` constructs task-local families
and menu entries. `outer_canonical_g.clj:35–88` uses their actions/class-emission
models and calls `cascade-coapply-g/compare-at-horizon` with `{#{} 1}` and a
common horizon. `wm/family_canonical_g.clj:62–89,139` calls that same function
for family rescoring/certification. Thus **yes: the tactical family and live
outer menu already share the numerical scorer; the supplied candidates are
the wrong level for Joe's declared META skeleton**, not two different G functions.
`cascade_coapply_g.clj:56–94` gets cumulative risk/ambiguity from the observation
scorer and subtracts enabled-frontier Beta novelty, retaining its certificate.

Search scope: recursive `rg` across futon2, futon3c and futon3 Clojure/CLJS/CLJC/bb
files for the four requested literals, then for `meta-outer-policy`,
`meta-policy-constructor` and their call aliases. I found the readers and
offline producers above, but **no runtime `src/` caller** of those META
constructor/evaluator namespaces in futon2 or futon3c. I found no adapter from
those six-slot templates to canonical `CandidateN`, and no existing Lean
six-slot binder in the named G modules. This is a scoped search result, not a
claim that no dynamically resolved or external caller can exist.

## 4. One proposed Lean file (12 lines; no new score)

1. `FilledMetaCascadeInstance.lean` imports `GOverCascades`, `CoherentThetaCandidate`, and `Proof2.OuterSelectionCanonicalG`.
2. Define typed six-slot fillings, ordinary/self-heal admission, and the self-heal rearm obligation.
3. Retain the library's four pattern identities and diamond; prove the interpreted token dependencies match it.
4. Interpret a valid filling into existing `CandidateN`, with an explicit `Coherent` proof; refuse missing interpretations.
5. Bind source evidence to initial belief/guards and declare a common horizon and existing `EndingClass` C schedule.
6. Use `GOverCascades.cascadeGrainG` directly; do not introduce another risk, ambiguity, G, or information formula.
7. Make two illustrative ordinary fillings for one target, differing in next-move `advance` versus `unblock`.
8. Give a single evidence-channel observation two values that change initial readiness tokens, not candidate support or preferences.
9. Prove both resulting candidates score successfully and their strict G ordering reverses after that observation.
10. Prove `totalNovelty > 0` for the witness, with coherent positive counts, rather than merely claiming an epistemic channel.
11. Use the existing frontier algebra for the selection flip; keep construction downstream and unselected tactical families absent.
12. Explicitly limit this to a class-compatible instance: full eleven-channel preservation needs the unresolved projection/generalization above.

No production probabilities or example theorem have been manufactured or tested
in this discovery. The proposal is a finite instance to prove, not a claim that
the EDN alone already supplies an executable generative model.
