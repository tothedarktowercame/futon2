# Requirements versus the computed R20 step (2026-10-28)

This note classifies the requirements in
`DarkTower/WarMachine/Requirements.lean` against the computed loop in
`DarkTower/WarMachine/R20Step.lean` at `d7649b2f2d6e47df3c0c27628d5da640160b89ee`.
It does not change `Requirements.lean`.  The important boundary is that a
`Makeup.cascades` entry is now a task-selection cascade.  Tactical policies
for the selected task do not exist before selection.

## Q1 — not-yet-modelled (slice 5, with the slice-6 producer)

> Joe: “Every open mission, excursion or ticket is wanted.”

Q1 reads `openMissions`, `openExcursions`, `openTickets`, and
`enumeratedTasks`; `openTasks` is the union of the first three.  None of these
populations exists in `R20Step`.  `Observation.field` is the externally
supplied candidate list, but the model has no canonical registries and no
fact relating that list to all open tasks.  Treating mere membership in
`field` as “wanted” would assume the completeness Q1 is meant to check.

The requirement remains wanted.  Slice 5 should add a field-completeness row
and its theorem; slice 6 must supply the canonical registry snapshot and
enumerator evidence that make the row computable.  A suitable eventual
signature is:

```lean
theorem run_field_covers_open_tasks
    (m : Makeup Policy) (obs : List (Observation Policy)) :
    ∀ o ∈ obs, openTaskIds o.registrySnapshot ⊆ candidateTargets o.field
```

This would read the future registry observation/row plus today's
`Observation.field`; it cannot be stated from slices 1–3c alone.

## Q3 — already-holds

> Joe: “G is defined over policies, and policies are cascades ... full stop.”

Q3 reads `cascadesWithoutG`.  That field referred to constructed tactical
cascades and therefore should not survive as a `RunFacts` carrier for the
task-selection phase.  Its wanted content already has a direct computed-loop
form: every task-selection cascade in `Makeup.cascades` is mapped to one
numeric entry in `Click.cascadeScores`.

`R20Step.step_scores_read_carried_C` states exactly that:

```lean
theorem step_scores_read_carried_C (m : Makeup Policy) (s : State Policy)
    (o : Observation Policy) :
    (step m s o).1.cascadeScores = m.cascades.map
      (cascadeScore o.focus m.relation o.facts o.field (normalizeC s.c))
```

It reads `Makeup.cascades`, `State.c`, `Observation.focus`,
`Observation.facts`, `Observation.field`, `Makeup.relation`, `cascadeScore`,
`normalizeC`, and `Click.cascadeScores`.  `chosenCascade_score_le` additionally
proves that the chosen cascade has no greater score than any menu member.

The old `constructedCascades`, `comparedPolicies`, and `cascadesWithoutG`
interpretation as a pre-choice menu of tactical families is retired.  The
quoted deleted-path coupling is `RunFacts.constructedCascades`: “all arranged
cascades the constructor emitted,” together with `comparedPolicies`: “the
policies actually compared by the posterior.”

## Q4 — not-yet-modelled (slice 5)

> Joe: G must not be risk-only, and C-tau states a preference at every step
> of the horizon. Zero-valued ambiguity/information terms still count when
> their canonical carriers were evaluated. Grading at every step is required
> only for policy families that declare progressive preference semantics.
>
> Ruling (codex-68, 2026-10-16): each compared policy has its own horizon.
> Policies with different horizons are comparable only because their G terms
> share the per-step capacity-and-pattern normalization, so every compared
> policy records one common law, and a law other than that one requires equal
> horizons. The policy records are exactly the compared policies, one each:
> every record is of a compared policy, every compared policy has a record,
> and there are as many records as compared policies.

Q4 reads `gTerms`, `comparedPolicies`, `policyPreferences`,
`policiesWithRiskTerm`, `policiesWithAmbiguityTerm`, and
`policiesWithInformationTerm`, including every `PolicyPreference` field:
`policy`, `horizon`, `preferenceSteps`, `gradedPreferenceSteps`, `semantics`,
`normalization`, and `terms`.

The requirement remains wanted for tactical policies constructed **after**
the chosen task.  Slices 1–3c contain no tactical policy, horizon, ambiguity,
information-gain term, normalization law, or per-step C-tau schedule.
`Observation.G` and `Click.precision.G` concern precision evidence over the
current `Policy` type; they do not establish Q4's complete inner-policy
accounting.  Slice 5 is where the active-inference rows for the selected
task belong.  An eventual signature is:

```lean
theorem selected_task_policy_rows_complete
    (m : Makeup Policy) (s : State Policy) (o : Observation Policy) :
    let click := (step m s o).1
    click.selection.isSome →
      PolicyRowsComplete click.innerPolicies click.innerScores click.cTauRows
```

The old population-wide reading is retired where it depends on
`TargetConstruction` for every target before selection.  The retained
requirement is scoped only to policies for the chosen task.

## Q5 — not-yet-modelled (slice 5)

> Joe: “Go ahead and interpret a pattern, after you select it.”

Q5 reads `interpretationOrder`.  `R20Step.step` now computes task selection,
but it does not construct or interpret tactical patterns for the selected
task, so there is no second event whose order can yet be proved.  This is not
evidence for retiring the ordering requirement: it is precisely the boundary
that prevents the deleted pre-choice `TargetConstruction` path from returning.

Slice 5 should introduce the selected-task construction/interpretation row
and prove:

```lean
theorem interpretation_only_after_selection
    (m : Makeup Policy) (s : State Policy) (o : Observation Policy) :
    let click := (step m s o).1
    click.interpretation.isSome → click.selection.isSome
```

That theorem would read today's computed `Click.selection` and the future
selected-task interpretation row.  The old `InterpretationOrder` census can
then be retired rather than carried as an asserted enum.

## Q6 — not-yet-modelled (slice 4)

> Joe: a failure is a critical incident, never a non-event to be repeated.
> An unchanged refusal may not choose the same target/cascade pair again.

Q6 reads `previousOutcome`, `previousInputDigest`, `previousChoice`,
`currentInputDigest`, and `currentChoice`.  Slices 1–3c compute the current
choice but carry neither a verdict nor the previous choice/digest.  Slice 4
is explicitly “Verdict as observation” and should extend state with the
previous verdict and selection identity/input identity.

An eventual computed-run theorem is:

```lean
theorem unchanged_refusal_not_reselected
    (m : Makeup Policy) (s : State Policy) (o : Observation Policy)
    (refused : s.previousVerdict = .refused)
    (sameInputs : s.previousInputDigest = inputDigest m o) :
    selectionIdentity (step m s o).1.selection ≠ s.previousSelection
```

It must be a property enforced by `step`, not a predicate over an arbitrary
click sequence.

## Q7 — not-yet-modelled (slice 5)

> Joe: an absence, refusal, or “not supplied” on the one selected/enacted
> certificate path is a failure; the working path contains none. This is not
> a completeness claim about rejected alternatives or population-wide scorer
> diagnostics. With no selection, the explicit abstention carrier is the path.

Q7 reads `pathAbsenceCount`.  Slices 1–3c distinguish selection from explicit
no-selection (`Click.selection`, `step_selection_none_iff`) but do not yet
have the selected/enacted certificate path or its typed row carriers.  Slice
5 should replace the asserted count with a theorem over the computed rows:

```lean
theorem successful_click_has_complete_selected_path
    (m : Makeup Policy) (s : State Policy) (o : Observation Policy)
    (working : (step m s o).1.verdict = .changed) :
    selectedPathComplete (step m s o).1.rows
```

A separate theorem should state that `selection = none` produces the typed
abstention row.  `step_selection_none_iff` is useful input, but it does not by
itself establish Q7.

## Q9 — not-yet-modelled (slice 5)

> Joe: completion is structurally prior to conditional relevance. A flat
> four-way probability table is not a Q9 witness.

Q9 reads `factorizedPreference`, `calibrationAuthorizations`, and
`factorizationAuthorizations`, then validates every field of the selected
`FactorizedPreferenceSemantics.Certificate` and the matching retained
authorization records.

`R20Step.State.c`, `normalizeC`, and `Click.C` deliberately implement the
flat learned four-class distribution.  They therefore cannot witness Q9;
the Q9 docstring explicitly rules that out.  Slice 5 needs a separately
authorized factorized-preference row and a theorem such as:

```lean
theorem factorized_preference_row_valid
    (m : Makeup Policy) (s : State Policy) (o : Observation Policy) :
    let click := (step m s o).1
    Certificate.Valid click.calibrationAuthorizations
      click.factorizationAuthorizations click.factorizedPreference
```

The future row must remain distinct from `normalizeC s.c`.  This is also the
place to decide how the spec's later hierarchical-C upgrade supplies the
factorization rather than treating the current terminal-class table as one.

## Theorems available or provable against slices 1–3c

Ordered by dependency:

1. `step_scores_read_carried_C` already proves the restated Q3 score coverage
   directly from `step`.
2. A length corollary is immediately provable from it:

   ```lean
   theorem step_score_count (m : Makeup Policy) (s : State Policy)
       (o : Observation Policy) :
       (step m s o).1.cascadeScores.length = m.cascades.length
   ```

3. `chosenCascade_score_le` then proves the selected menu entry is a minimum
   of those scores; `chosenCascadeEntry_mem` proves it came from the declared
   menu.
4. `step_selection_some` proves a successful pick came from
   `Observation.field`, while `step_selection_none_iff` characterizes explicit
   abstention.  These support future Q1/Q7 rows but do not establish either
   completeness requirement today.
5. `whatOthersWaitOnFirst_pick` and
   `whatOthersWaitOnFirst_none_of_no_ready` prove the Giraffe readiness and
   unblock policy used to form one concrete task-selection cascade.  They add
   no registry-completeness or certificate-path claim.

No Q1, Q4, Q5, Q6, Q7, or Q9 theorem can be proved from the current fields
without adding the missing observation/state/row carrier named above.
