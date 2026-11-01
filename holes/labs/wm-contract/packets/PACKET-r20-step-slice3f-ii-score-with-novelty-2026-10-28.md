From claude-11. Lean task, one existing module. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Slice 3f-ii, in `DarkTower/WarMachine/R20Step.lean` (`/home/joe/code/mathlib4`,
branch `darktower`, on top of `ace037015`): the outer score becomes
G = risk + ambiguity − novelty (Da Costa et al. 2020, eq. 42), with the
informative `outerNovelty` of 3f-i-b, and `step` chooses by it.

## Change

- `outerScore` (and `carriedOuterScore`) = the current `stepG` value minus
  `outerNovelty` of the same pick, computed from the same counts and kernel.
  Keep it `EReal` with `⊤` exactly where it is `⊤` today.
- Restate `outerScore_eq_stepG_of_finite` as
  `outerScore = stepG − outerNovelty` (finite case), and keep
  `click_cascadeScores_eq_outerScore`, `chosenCascade_score_le` and the other
  selection theorems with the same claims.
- No shadow path: the score without novelty is not kept as a second
  selection rule.

## Examples, recomputed

- 3a: say whether it still ties.
- 3b: both picks have reach 1/2 and the same status, so novelty should be
  equal and the strict flip should hold; prove it.
- `ambiguity_changes_outer_choice`: Y (reach 1) is now also more
  informative, so Y should still win; restate. Its risk-only clause stays as
  is.
- New: one example where novelty decides: two picks with equal risk and
  ambiguity (same class, same reach, same gain) but different statuses whose
  counts differ (e.g. `.ready` with large counts vs `.undetermined` with small
  counts); `step` picks the less-known one, strictly.

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`, no
  `native_decide`.
- `#print axioms` on the restated score theorem, `chosenCascade_score_le`, the
  3b theorems, `ambiguity_changes_outer_choice` and the new example: only
  `propext`, `Classical.choice`, `Quot.sound`. Paste the output.
- Commit only `DarkTower/WarMachine/R20Step.lean` (explicit path).
- If too large for one turn: commit the score change with the restated
  general theorems, keep every example building (restated or proved), and
  stop before the new example; do not comment examples out.
