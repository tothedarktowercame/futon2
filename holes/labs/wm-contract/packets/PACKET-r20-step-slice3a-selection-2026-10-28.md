From claude-11. Lean task, one existing module. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Slice 3a of `/home/joe/code/futon2/holes/labs/wm-contract/SPEC-r20-step-function-2026-10-28.md`
("Menu and selection from State"), in `DarkTower/WarMachine/R20Step.lean`
(`/home/joe/code/mathlib4`, branch `darktower`, on top of `7699fbafa`).
`step` chooses a task. The policies it chooses between are task-SELECTION
cascades (Joe, 2026-10-28: the outer loop is the same loop with a different
policy; a policy for selecting tasks is a cascade of the patterns in
`/home/joe/code/futon3/library/task-selection/`). Each cascade is applied to
the observed task field and picks one task. `step` scores each cascade by the
risk of its pick's predicted ending class against the C carried in `State`,
and takes the least.

No inner (tactical) policies are built or scored before selection. Do not
edit `R20Run.lean`, `R20FocusedSelection.lean`, `R15InjuryTier.lean` or
`CTauClassPreference.lean`; reuse `R15InjuryTier.Candidate`,
`candidatePrediction`, `R15StrategicTarget.outcomeClass`,
`CTauClassPreference.classRisk`, `R20FocusedSelection.OuterSelection`.

## Types

- `SelectionStep`: an inductive with `gate (keep : Candidate → Bool)` (a
  filter) and `order (key : Candidate → ℚ)` (a stable sort, least key first).
  Docstring: gates and orders are the roles in the task-selection README.
- `SelectionCascade := List SelectionStep`; `apply` runs the steps left to
  right on the field; `pick` is the head of the result (`Option Candidate`).
- `Makeup` gains `cascades : List SelectionCascade` (nonempty; carry a proof)
  and `relation : Target → Target → TargetRelation`.
- `Observation` gains `field : List Candidate` (supplied from outside for now;
  docstring says so).
- `Click` gains `selection : OuterSelection` (candidates = the field,
  selected = the chosen cascade's pick), the chosen cascade's index, and the
  list of cascade scores.

## Score and choice inside `step`

- Predicted distribution of a cascade: if `pick = some c` and
  `outcomeClass o.focus m.relation (some c.target) = some k`, then
  `candidatePrediction k c.reach`; otherwise all mass on `.stopTheLine`.
- Score `G` = `classRisk predicted (normalizeC s.c)`: the learned C from
  slice 1b, read from `State`, not the fixed kit.
- Choice: the first cascade in `m.cascades` attaining the minimum score.
  The docstring must say that a tie is resolved by menu order only so that
  `step` is a function, and that ties are a defect to be removed by better
  evidence, not a rule (Joe).
- If the chosen pick is `none`, `selected` needs a value: make
  `OuterSelection.selected` come from `pick` only when it is `some`; if the
  existing structure forces a value, stop and bell back with the question
  rather than inventing a default candidate.

## Theorems (every makeup, every observation)

1. The chosen cascade's score is ≤ every cascade's score in `m.cascades`.
2. If the chosen pick is `some c`, then `c ∈ o.field` (gates filter, orders
   permute; prove `apply` returns a sublist-permutation of the field).
3. The scores read the carried C: each click's score list equals the map of
   `classRisk (prediction …) (normalizeC s.c)` over the cascades (by
   unfolding `step`).
4. Keep every slice 1/1b/2/2b theorem building.

## Example

A three-candidate field with focus 7 and `relation` from an existing module
(e.g. `R15StrategicTarget` / `R20FocusedSelection` examples), and two
cascades that pick different candidates, e.g.
`[gate (reach ≥ 1/2), order (−reach)]` and `[order target-id]`. Show which
cascade `step` chooses at click 0, the two scores, and the selected candidate.
(Showing that learned C changes the choice at a later click is slice 3b.)

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`.
- `#print axioms` on theorems 1 and 2: only `propext`, `Classical.choice`,
  `Quot.sound`. Paste the output.
- Commit only `DarkTower/WarMachine/R20Step.lean` (explicit path).

If the existing types do not fit (e.g. `classRisk` is `EReal` and the
minimum needs a linear order you cannot get), stop and bell back with the
question.
