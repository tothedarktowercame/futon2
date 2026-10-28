From claude-11. Lean task, one existing module. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Slice 3d-ii, in `DarkTower/WarMachine/R20Step.lean` (`/home/joe/code/mathlib4`,
branch `darktower`, on top of slice 3d-i): `step` chooses the task-selection
cascade by `outerScore` (3d-i: `CascadeEFE.stepG` over the outer model) in
place of today's risk-only `cascadeScore`. Design:
`/home/joe/code/futon2/holes/labs/wm-contract/NOTE-outer-G-computation-2026-10-28.md`.

## Change

- `chosenCascadeEntry`, `Click.cascadeScores` and `step` use `outerScore`.
  Keep `cascadeScore` as a definition only if something still needs it;
  otherwise remove it (no shadow or comparison path: Joe's standing rule).
- The 3a–3c theorems about selection (`chosenCascade_score_le`,
  `step_selection_some`, `step_selection_none_iff`,
  `step_scores_read_carried_C`, `whatOthersWaitOnFirst_*`) restated over
  `outerScore`; their claims stay the same.

## Examples

Recompute each selection example under `outerScore` and state the new values:

- 3a (menu-order tie): say whether it still ties.
- 3b (learned C flips the choice from A at click 1 to B at click 2): keep the
  strict claim at both clicks if it holds under the new score; if it no
  longer holds, show the values and say so in the summary. Do not weaken the
  statement or change the example's inputs to rescue it without saying so.
- 3c (K3.2 picks P12): should be unaffected (readiness gate).

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`.
- `#print axioms` on the restated `chosenCascade_score_le` and the 3b
  theorems: only `propext`, `Classical.choice`, `Quot.sound`. Paste the output.
- Commit only `DarkTower/WarMachine/R20Step.lean` (explicit path).

If this is too large for one turn, commit the switch with theorems restated
and stop before the examples, saying so; do not leave the file half-changed.
