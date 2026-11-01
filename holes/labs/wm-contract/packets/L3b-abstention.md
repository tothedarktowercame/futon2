# L3b — Abstention as a typed decision (claude-2 → claude-3, 2026-11-01)

Plan: `futon2/holes/labs/wm-contract/PLAN-outer-selection-restoration-claude2-2026-11-01.md`.
Base: darktower head `1971aeaf55` (or later, if L4a has not touched Lean it is the same).

## Joe's ruling (2026-11-01, verbatim)

"abstention would apply when there is nothing left to do (outer) or when no action is
available that progresses the work (inner). Both are appropriate stops. I would approach
the latter especially with some skepticism, but it is still valid."

So there are exactly two legitimate abstentions, and each is checkable:

- **Selection run, empty field.** No open task exists. There is nothing to construct
  cascades over, so there is no construction, no compared policy, and no G list.
- **Work run, no progressing action.** Cascades were built over the whole pinned library
  for the selected task and compared, and stopping scored strictly below every one of
  them, with every one predicting something other than stopping. The stop is reached
  through the comparison, not instead of it.

Everything else that picks nothing is a violation: a selection run with a non-empty field
that picks nothing (the machine must not abstain while work remains), a work run that
abstains without a comparison, and the click-12 shape where the pick predicts the same as
stopping. Skepticism about the inner case is expressed by making its conditions strict
and by recording it as a terminal outcome the next selection run must consume.

## Carriers

Replace `Choice` as the current/previous decision with:

```lean
inductive Decision where
  /-- A task and an arranged cascade over it. -/
  | pick (target cascade : Id)
  /-- Work run: cascades over `target` were compared and none progresses it. -/
  | abstainOnTask (target : Id)
  /-- Selection run: no open task exists. -/
  | abstainEmptyField
  deriving DecidableEq, Repr
```

`RunFacts.previousDecision`, `RunFacts.currentDecision : Decision` replace
`previousChoice`/`currentChoice`. Keep `Choice` only if something else needs it; I expect
not (16 uses, all of them these two fields and the fixtures).

`RunOutcome` gains `| abstained`.

`RunFacts` gains `abstentionPrediction : Id`, the digest of the stopped distribution,
`0` a recorded absence. This is the carrier claude-3 noted was missing in L2: predicting
the same as abstention is then caught through the prediction as well as through G.

## Requirements

- `constructionTargets` for `.work` is `{t}` for `pick t _` and `abstainOnTask t`;
  for `.selection` it stays `enumeratedTasks`.
- **Q2** gains a leading case: `kind = .selection ∧ enumeratedTasks = ∅ →
  targetConstruction = [] ∧ comparedPolicies = ∅ ∧ constructedCascades = ∅`, and the
  existing clause applies only when that antecedent is false. An empty field with a
  construction over nothing is a violation (the fixture-in-production tripwire).
- **Q11** by case on `currentDecision`:
  - `pick t c`: as now, plus `∀ p ∈ policyPrediction, p.2 ≠ abstentionPrediction` and
    `abstentionPrediction ≠ 0`.
  - `abstainOnTask t`: `kind = .work`, `comparedPolicies.Nonempty`,
    `∀ g ∈ policyG, abstentionG < g.2`, `∀ p ∈ policyPrediction, p.2 ≠ abstentionPrediction`,
    `abstentionPrediction ≠ 0`, and the id-list clauses (Nodup, keys = compared,
    compared = constructed = construction policies) as for a pick.
  - `abstainEmptyField`: `kind = .selection ∧ enumeratedTasks = ∅ ∧ policyG = [] ∧
    policyPrediction = []`.
- **Q6** unchanged in text (`previousOutcome = .refused …`) but now over `Decision`.
  Do not extend it to `.abstained`; Joe called abstention an appropriate stop, not a
  failure. Note in the bell whether a repeated identical-input `abstainOnTask` should
  be refused by Q6; that is a question for Joe, not a clause to add.
- **Q7**: no change. A work run that abstained has `previousOutcome = .abstained` on the
  next selection run and is consumed like any other ending (L3's `consumedRuns`).

## Fixtures

Positive, each `conforms`:
- `workAbstains`: `{ good with currentDecision := .abstainOnTask <good's target>,
  previousOutcome := .changed, abstentionG := 1, policyG := [(60000, 2), (60001, 3/2)],
  abstentionPrediction := 160999 }`. The comparison stands; stopping wins strictly.
- `selectionEmptyField`: `kind := .selection`, all three open sets and `enumeratedTasks`
  empty, `targetConstruction := []`, `comparedPolicies := ∅`, `constructedCascades := ∅`,
  `policyG := []`, `policyPrediction := []`, `currentDecision := .abstainEmptyField`,
  `selectionPath := some { … }` with observed receipt and reach, `pendingRun := none`,
  `consumedRuns := []`, `learningApplied := false`; `pickRelation` is `.observed` of the
  receipt that says there was no pick to relate (state the choice in the bell if you find
  it should be a fourth constructor instead).
- `good`, `selectionGood`, `workOneCascade` unchanged in meaning, now with `.pick`.

Negative, each fails only Q11 (or only Q2 where stated):
- `selectionAbstainsWithWork`: `{ selectionGood with currentDecision := .abstainEmptyField }`
  (field of 626, picks nothing). Fails Q11.
- `emptyFieldWithConstruction`: `selectionEmptyField` with `selectionGood`'s construction
  and compared policies. Fails Q2.
- `workAbstainsWithoutComparison`: `workAbstains` with `comparedPolicies := ∅,
  policyG := [], policyPrediction := [], constructedCascades := ∅, targetConstruction := []`.
  Fails Q11 (and Q2; state which in the fail-only theorem, both are right).
- `workAbstainsNotMinimum`: `workAbstains` with `abstentionG := 3/2` (ties the better
  cascade). Fails Q11.
- `pickPredictsStopping`: `{ good with policyPrediction := [(60000, 160000), (60001, 160999)],
  abstentionPrediction := 160999 }`: the chosen policy predicts exactly what stopping
  predicts. Fails Q11. This is click 12 seen through predictions.
- `abstentionPredictionAbsent`: `{ good with abstentionPrediction := 0 }`. Fails Q11.
- `abstainOnTaskLabelledSelection`: `{ workAbstains with kind := .selection }`. Fails Q11
  (and Q2).

Theorems: one per fixture, by `native_decide`, plus `abstention_negatives_fail_only`
stating the exact `violations` list for each negative, plus `click20_violations` updated
if it changes.

## Reader

`RequirementsReader.lean`: no new field (no new requirement). Confirm it builds.

## Gates

Worktree `~/worktrees/mathlib4-l3b-abstention`, branch `claude3/l3b-abstention` from the
current darktower head; `.lake` by `lake exe cache get` or a real copy, never a symlink
into the primary. `lake build` of the two modules green, zero `sorry`, log and sha256 in
`~/notes/l3b-abstention/`. Do not merge. Bell claude-2 with sha, build log sha, the
`selectionEmptyField.pickRelation` choice, the Q6 question, and anything else not stated.
