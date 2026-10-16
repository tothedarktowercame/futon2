# Q2 diagnosis: construction scope conflict (2026-10-16)

Requisition: codex-68, "E-wm-learning-loop — diagnose and repair Q2
construction census". Branches: futon2 `claude-10-q5-family` at `dc2d93792`,
mathlib4 `darktower` at `6cd7a19a59`. Stopped before implementation, as the
requisition directs when a theory amendment is required.

## What Lean Q2 requires

`DarkTower/WarMachine/Requirements.lean:213`:

- every `TargetConstruction` entry: `targets ⊆ openTasks`, `pool = slice`,
  `sliceFromWholeLibrary`, `slice.card ≤ libraryPatternCount`,
  `0 < policyCount`;
- the union of all entries' `targets` **equals `openTasks`**;
- `constructorPatternCount` = card of the union of pools.

Its comment quotes Joe: "every open target has an action from the start"
(ruling 9, `PROOF-2b-ACTIONS-draft-2026-09-30.md:52`). So Q2 as written is
option (a): every open task, including tasks Q8 excludes, carries a
whole-library slice and at least one constructed policy.

## What production does

- The outer selector chooses one task before any cascade material exists
  (`outer_task_selection.clj`, ns docstring; `select-outer-task`,
  `scripts/futon2/report/war_machine.clj:1540`). Every task in its support
  carries a task-level action (`:advance-mission`, `:advance-excursion`,
  `:advance-ticket`, `:run-algorithm`). Excluded tasks
  (`:unsupported-task-shape`, `:requisition/in-progress|completed`) carry none.
- Inner construction is then restricted to that task:
  `cascade-targets (if (string? selected-task-id) [selected-task-id] [])`
  (`war_machine.clj:6674`, comment: "Inner cascade construction is
  target-local"). `target-source-declarations`, `target-policy-field` and the
  reading-policy families are built for that one target only.
- The reading route records `:target-construction {:status :absent :reason
  :reading-policy-construction-census-owed}` (`cascade_decision.clj:1337`).

## The conflict

On every production run with more than one open task, no truthful census can
make the union of constructed targets equal `openTasks`: unselected tasks
have no slice, pool or family. Producing entries for them would fabricate
construction. Q2 is also stricter than Q8 ruling 2b, which allows excluded
tasks with typed reasons and no comparison key: Q2 still demands a positive
policy count for those tasks. So option (b) (every eligible task) does not
match production either; it would need inner construction for the whole
eligible frontier.

## Options

1. **(a) Keep Lean.** Q2 stays population-wide. The exporter carries an exact
   census of the one constructed target, and Q2 is reported *violated* (not
   NR) whenever `openTasks` has more than that target. Truthful today; Q2
   holds only if the runner later constructs families for every open task
   before outer selection (about 600 tasks per click).
2. **(b) Amend to the eligible frontier.** Replace `openTasks` with
   `frontierCensus.eligible`. Matches ruling 2b's exclusions, but production
   still violates it for the same reason as (a).
3. **(c) Amend to selected-target construction plus outer actions.** Split
   Q2 into
   - outer action coverage: every task in `frontierCensus.eligible` has a
     task-level action in the outer support (this is how production meets
     "an action from the start"), and
   - exact construction for `frontierSelection.selected.task` only: one
     entry, `targets = {selected}`, the pin/slice/pool/policy conditions,
     the policy identities bound to `selectedCascade` (Q5/Q6), typed failure
     when the family is empty.

   This narrows the reading of ruling 9 from "a constructed cascade per open
   task" to "a task-level action per eligible task", so it needs an explicit
   ruling. It is not a silent narrowing only if that ruling is recorded.

Recommendation: (c). It matches the code's own design comments (outer
selection is ignorant of inner cascades by construction) and keeps ruling
9's intent, an action for every task from the start, at the level where
production actually provides it. If the ruling should hold out for
population-wide construction, take (a): it is truthful now and records the
gap as a violation.
