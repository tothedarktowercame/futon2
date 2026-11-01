# L5-15-D — Discovery: why the picked excursion's completion baseline is absent (claude-2 → codex-11, 2026-11-01)

Read-only; report only; worktree only. Base futon2 `main` (≥ 3eb6fa48a).

Alert: click 11 (`wm-click-7adb0f36`), selection Q7 violated with two typed path absences; one is
`[:outer/selection :pick-fact :completion :baseline]` for the pick E-comprehension-foundation. The
field fact comes from the Giraffe pass row (`outer_observation/giraffe-field` selects
`:completion` from the row). Lean Q7 counts a typed absence on the decision path as a violation,
so every click that picks a task without a completion baseline will read Q7 violated.

## Questions

1. What is `:completion` on a Giraffe pass row, who produces it (futon3c giraffe, which pass), and
   what is `:baseline` meant to be (a count of completed wants at the previous run? a date?). Cite
   the producer and its README/row schema.
2. On the live run used by click 11 (`/home/joe/code/storage/giraffe/20261101T182437.866Z-bb46b649`
   or the one its record pins), how many rows carry `:completion :baseline`, how many carry
   `:completion` without `:baseline`, how many carry neither; is the absence specific to
   excursions (E-*), to tickets (T-*), to tasks first seen in this run, or general?
3. Is the absence a producer gap (the pass never computes a baseline for that class of task), a
   first-observation case (no previous run to baseline against), or a reading failure? Which is
   the honest typed value for each: a baseline of 0 is a value, not an absence, and must not be
   invented.
4. Smallest honest change, if any is in futon2's reach (the field builder's exclusion/typing of the
   fact) versus what belongs to the Giraffe producer; name functions. Do not implement.

## Report

`reconciliation/L5-15-D-completion-baseline-codex11-2026-11-01.md` on branch `codex11/l5-15-d`.
Bell claude-2 with the sha and the four answers in brief.
