# H-G-TARGET-PRIOR-D — a prior over targets before any candidate exists

claude-11, 2026-09-25. PROOF-2a hole H-G-target, part 2, on claude-8's
requisition. Read-only discovery: no code edited, nothing run, nothing loaded
into a shared JVM, nothing written under `data/`.

Read at: futon2 `f6d4d707`, mathlib4 `759b8ca884` (cited WarMachine files
last changed `b3d8afa6a0`), futon3c `ac93ed46`, futon4 lifecycle `4139c74`.

**Answers up front.**
1. The prior before G is E, `PolicyPriorKernel : 1 ⇝ Π`; a prior over targets
   is the same object indexed by targets. E alone cannot choose:
   `selectionPosterior` needs a G for every member, absent on all 343.
2. Candidate A is not a prior but the G (epistemic term) of a different
   policy, "take t's next step". Only step kinds and decline records exist;
   no observation model, no probability.
3. Candidate B: no document states a priority across targets; the designed
   carrier, the portfolio `:upvote`, returns 0.0 at HEAD.
4. Clause T cannot be witnessed in this state. The honest record abstains
   with a typed absence and fails Wₜ by Xₜ(b); Wₜ is not a gate, so not a
   stop. The missing definition is named in §4.

## 1. What AIF already has: E, and what it needs beside it

**The prior before G.** The definition is `Holes.PolicyPriorKernel
(PolicyIndex) := ProbabilityKernel Unit PolicyIndex` (mathlib4
`DarkTower/WarMachine/Holes.lean:7101-7103`). Its docstring: "The normalized
policy prior `E : 1 ⇝ Π`; its Unit domain prevents contextual likelihoods from
masquerading as the prior." The Unit domain is what makes it a prior *before*
anything about the policy is scored. Its witness is
`PolicyPriorKernelWitness.reference`, a fair 1/2–1/2 prior over two policies
(`PolicyPriorKernelWitness.lean:9-15`).

**How it enters selection.**
`PriorForm.habit` logits are `Real.log (E i) - G i`
(`CascadeEFEPolicies.lean:62-73`); `selectionWeight` is
`E(π) · exp(−γ G(π) − F(π))`, G in `EReal`, normalised by
`selectionPosterior` (`PolicySelection.lean:23-31`), which Clause T's chosen
target maximises over `T_f` (PROOF-2a `:102`).

**Is a prior over targets the same object?** Yes, at another grain. Put
`PolicyIndex := T_f` and E is a `PolicyPriorKernel T_f`. Nothing in the
definition restricts the index to cascades. H-E's fold (futon2 `47842175`,
`enactment_habit.clj:34-46`) writes E at *cascade* grain, keyed by
`cascade-prior/policy-key` = [mission, ordered pattern ids, semilattice]. So
target-grain E could be obtained from it by marginalising over each target's
keys. At HEAD: one enactment record (click-001, M-futon-seams, COMPLETE and
not in the field, PROOF-2a `:568`, fixture `f57402d0`), so zero counts on
every member of T_f.

With `cascade_prior.clj:14` `default-alpha 1.0` as the stated pseudo-count,
target E is uniform over 343. That is the defined prior with no data, not a
substituted value. It still names no target.

**Why E alone cannot choose.** `selectionPosterior t habit F G` takes
`G : ι → EReal` for every `ι` (`PolicySelection.lean:29`). "E alone decides"
would mean `G ≡ c` for some constant c, because only a constant shift leaves
the softmax equal to E. Reading "absent" as "equal" substitutes a value for
an absence, which is barred. So a target prior is necessary but not
sufficient. Clause T also needs a G, or an explicit statement of what the law
is when G is undefined (§4).

**Relation to the theorem's other objects.**
- **Clause 5** (PROOF-2-THEOREM `:78-82`) is learned B, the transition
  model, not a prior over policies.
- **AR-35** (PROOF-2a `:309-314`) is a prior over lower-level patterns
  conditioned by higher-level ones. A target prior is the opposite:
  unconditioned (Unit domain) at the highest grain, with no parent to prime
  it, so AR-35's priming-edge falsifier has no analogue.

## 2. Candidate A: the next step's expected information gain

**What it is.** A target t with no candidate still has a next step
(`target_field.clj:224`: `#{:read-criteria :ask-interpretation :observe
:construct :ready}`). That step is an action with an observable outcome. So
the AIF object here is not E. It is G of the policy "take t's next step", with
the step's value carried by the epistemic term:
- `EpistemicValue.activeHorizonEFE` has a per-action observation channel
  `Aᵤ` (`EpistemicValue.lean:80-111`).
- Its fixture shows the check preferred over no-op by exactly `ln 2`
  (`fixture_check_strictly_better`, `:199-204`).
- Under a single shared channel the check has no value
  (`fixture_p7_check_no_value`, `:206-208`).

The canonical EIG is `Holes.expectedInformationGain Q prior posterior`
(`Holes.lean:7258-7275`). It is the Q-expectation of the KL divergence from
posterior to prior, witnessed by `ExpectedInformationGainWitness.binaryFixture`
= `log 2` (`:66`).

**What it would be a function of**, for the step of target t:
(a) the θ the step informs (criteria stated or not; per named want,
interpretation given or refused; for `:observe`, each token's value);
(b) the prior `Q(θ | step)`; (c) the outcome channel `Q(o | step)`;
(d) the posterior `Q(θ | o, step)`; (e) a common unit across targets, since
each result is nats about that target's own θ.

**What exists at HEAD.**

| input | at HEAD | record |
|---|---|---|
| step kind per target | yes, all 343 | `:next-step` in the field (7bd17dfb; 331 `:read-criteria`, 12 `:ask-interpretation`) |
| the finding behind `:read-criteria` | yes | `target_field.clj:181-182`: `:criteria-not-stated` or `:no-wants`; `:want-already-observed` at `:193` |
| outcomes of past reading asks | yes, as individual records | `mission_reading.clj:415-424` `record-locator-decline!` stores each decline with `:mission-sha`; `flight.clj:88-93` surfaces them as `:reason :locator-declined`; `:readings-needed` at `flight.clj:97` |
| the wants an ask would resolve | for the 12 `:ask-interpretation` targets | `:interpretations-for` (`target_field.clj:145,195`) |
| `Q(θ | step)`, `Q(o | step)` | **no** | nothing records a probability of a reading or an ask succeeding; the decline records are counts per target, and no reader turns them into a rate |
| a common unit across targets | **no** | same problem as ΔG's universes (row H-G-target, claude-10's option (a)) |

**Typed absences:** `{:absent :no-step-outcome-model :step <kind>}` for
(b)–(d) on every target; `{:absent :incommensurable-theta}` for (e).

The decline records could train such a model; that would be a new model,
not a reading of one. No number is proposed.

## 3. Candidate B: an owner-stated priority

Searched: the lifecycle doc, and every `holes/missions/*.md` in the
canonical futon0, 2, 3, 3a, 3b, 3c, 4, 5, 5a, 6, 7 checkouts (not worktrees)
for `Priority`, `priority:`, `top/highest priority`, `urgent`, `P0`,
`Blocked by`, `Depends`, `Prerequisite`.

| found | where | is it a priority across targets? |
|---|---|---|
| "**Portfolio inference** (M-portfolio-inference) decides *which* mission to work on next. The mission lifecycle says *how* to work on it." | futon4 `mission-lifecycle.md:546-548` | Names the process that owns a cross-mission choice. States no priority. |
| `:upvote` — "Express desire for a mission (Nonstarter-style)"; "upvoted missions have higher pragmatic value" | futon3c `M-portfolio-inference.md:312-318` (`697201b9`) | This is the designed carrier for an owner-stated priority. |
| `upvote-value` — "Placeholder: returns 0.0 until Nonstarter integration." | futon3c `src/futon3c/portfolio/policy.clj:119-124` (`ac93ed46`) | The carrier exists and holds no value at HEAD. Typed absence, not zero preference. |
| `**Gate:** operator-…` lines | `mission-lifecycle.md:321-331`; 4 mission files | A gate says the operator must act. It is not a ranking, and Clause T makes every lifecycle mission feasible whatever its gate. |
| `**Blocked by**` (26 files), `**Depends**` (20), "Prerequisite" (17) | mission headers | Dependencies: a partial order on *when* a target can finish, not a statement of which is wanted more. I did not check which of these files are in T_f. |
| "Highest priority" | futon3c `E-cheesemonger.md:87` | Ranks items inside one excursion, not targets. |
| "Listed by priority:" | futon3 `M-pattern-mining.md:470` | Inside one mission. |
| `:mission/depends` | futon3 `QUEUE-FORMAT.md` (`6f8483b`, Status: Draft) | Dependencies again. |
| bid/clear "nonstarter mechanics" for time allocation | futon5a `futon5a.devmap:9-11` | Personal hours, "Private data … lives elsewhere". No record over futon targets. |

**Answer: none is stated.** No document states a priority or value ordering
over missions, tickets or excursions. The one mechanism designed to carry
one, the portfolio `:upvote` (Nonstarter), is a placeholder returning 0.0.
Dependency lines exist in 46 files. They are precedence, which Clause T
treats as feasibility/support, not as a term of G (PROOF-2a `:102-105`).

## 4. The state with no ΔG and no prior: what `:chosen` can be

Today: 343 in T_f; ΔG `{:absent :no-constructed-candidate …}` on all 343;
target E uniform with no data (§1); no stated priority (§3). Clause T's text:
- "The chosen target maximises `selectionPosterior` over `T_f`" (`:102`);
  with G undefined on every member (§1) no member is the maximiser.
- A non-abstaining record without a prior has no honest form: any named
  member is a substituted G or an unstated tie-break.
- Abstaining with non-empty T_f "must fail" Wₜ (Xₜ(b), `:134-137`).
- "Wₜ is a witness condition, not a gate: a click that fails it still runs;
  it is not credited" (`:139-140`).

**So `:chosen` is the typed absence**
`{:absent :no-target-prior :g {:absent :no-constructed-candidate} :e :uniform-no-data}`.
The field is recorded in full (`:considered`, `:feasible` with each
`:next-step`, `:exclusions`). Wₜ fails, and the record says why. This is not a
stop: the text makes Wₜ a credit condition, and the field is still the
record. It is also not a defect in the record. On this field Clause T is
unsatisfiable, and Xₜ(b) correctly reports that.

**The missing definition, named.** Clause T defines the law over targets
whose G exists. It does not say what the policy at target grain *is* before
any candidate exists. Two definitions would make the law satisfiable without
substituting a value:
- **(i) The target-grain policy is the target's next step.** G is that
  step's G, with its epistemic term (§2). The law is then defined wherever a
  step-outcome model exists. It needs the model (b)–(d) and a common unit
  (e). Neither exists.
- **(ii) A stated target prior E** (candidate B filled), with a stated rule
  for a G that is undefined on all of T_f. For example: "when G is undefined
  on every member, the law is E". That rule would have to be written into
  Clause T, and it is a new clause, not a reading of the current one. Its
  case would be the 343-target field above.

Neither is adopted here. (i) is AIF-native ("before G is known" = G of an
earlier action); (ii) puts an owner statement where the model has nothing.

## 5. Falsifiers

For the absent-choice record (§4):
- (a) A record in the no-prior state that names a `:chosen` member must fail
  Wₜ. A checker that accepts it is accepting a choice the law did not make.
- (b) The absent-choice record must carry the full field. Dropping one
  feasible target must fail Wₜ (Xₜ(a) unchanged).
- (c) Construct one `:ready` target with a finite ΔG and leave the rest
  absent. The record must still not choose it over the absent ones unless
  Clause T says absent-G targets leave the support. If it does say so, then
  removing that sentence must change `:chosen`.

For definition (i), next-step G:
- (d) Two targets with the same step kind and identical decline histories
  must get equal step-G. A step-G that differs between them reads something
  other than the model's inputs.
- (e) Under a single shared observation channel, every step's epistemic
  value must be 0, as in `fixture_p7_check_no_value`. A positive value there
  means the channel is not per-action.
- (f) Remove every decline record for one target. Its step-outcome prior
  must change. If it does not, the records were not an input.

For definition (ii), a stated prior:
- (g) Change one `:upvote` weight, or the stated priority for one target.
  The recorded E must change, and with G undefined everywhere, `:chosen` must
  follow E's argmax.
- (h) A target E that is non-uniform with zero enactment records and no
  stated priority must fail. The record has invented a preference.
- (i) E must be a Unit-domain kernel. An E conditioned on the target's text
  or next step fails `PolicyPriorKernel`'s own falsifier (`Holes.lean:7101`).
