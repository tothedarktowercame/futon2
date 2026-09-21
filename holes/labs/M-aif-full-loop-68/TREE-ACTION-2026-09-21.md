# Tree action beside machinery-68 attempt-002's close

For a reader of
`data/wm-full-loop-machinery-68/wm-contract-machinery-68-v1/attempt-002/007-closed.edn`:
what happened to the repository after that close.

## The close

Click `wm-click-cd86df2f-f823-4055-8039-67f393313314`,
run `2026-09-21-1789952479`, attempt-002, 642 s.
Route: `RUNNER -> FULL_LOOP_CLOSE via :build-failed`.

Selection was `:C1` on `M-expressions-of-interest`, a three-pattern cascade,
chosen unaimed. Patterns used: `:coordination/par-as-obligation`,
`:coordination/bounded-execution`, `:futon-theory/stop-the-line`.

The author seat committed, in `/home/joe/code/futon2`:

- `0f5dff6904bae65e2dfbb191522341016dbf60de` — Require cascade guard observation locators
- `7ac238d4a34448501bb4e04173f954b366ba1d18` — Validate cascade observation locator values

Independent review returned `REQUEST_CHANGES` in both rounds
(round 1 job `invoke-1789952798823-22824-84a1bdc7`,
round 2 job `invoke-1789953025306-22828-343c0975`), with tools executed in-job:
author 14 tool-events, reviewer 8.

## The tree action

**`b0f6deee` reverts both commits.** Master is back to `8f60f819`.

Reverted rather than left standing for two reasons. `futon2.aif.decision-gate`
is consumed by the serving tick path (`war_machine.clj`, `strategic_habit.clj`),
so the rejected validation was not inert authored output — it was validation the
next tick would run. And the delivery outcome was `:build-failed`, so a master
carrying the work anyway would make the tree disagree with its own record.

History is intact on purpose: `0f5dff69` and `7ac238d4` remain reachable, and
this dossier is the demonstration evidence.

Ruling: claude-12, 2026-09-21. Executed by claude-4.

## Why this note is HERE and not in the attempt directory

claude-12 asked for the revert shas in the dossier directory. They are not there
deliberately.

`full_loop_runner.clj:1832` states the rule the machine enforces on that
directory: *"EVERY non-EDN file must be referenced by exactly one record's
`:file`/`:stdout-file`/`:stderr-file` field; an unreferenced byproduct
(cohort-55 attempt-003: a stray `derived.stderr`) is parsed as EDN, fails, and
refuses the whole close."*

attempt-002 currently holds exactly its seven referenced records. Dropping an
unreferenced `.md` beside them would risk refusing a future close of the very
dossier this note exists to preserve — the same shape of damage as the
pre-fix `002-selection.edn` that poisoned every tick earlier the same night
(see the open finding on `attempt-events`' unguarded read,
`repair-occ-f9f4e970...`). The lab directory is the companion that can safely
carry prose.

## What remains open

The by-class locator fix is specified by the reviewer's two rounds — per-class
required fields matching the observation handlers, with valid and missing-field
controls per class. It is carried by the open finding
`repair-occ-7737547f116c5976ba54e7cad66e32de8f1fb7c41bf88f36e9a9a8a5783a5be6`,
preferably authored by the machine revising against its own review.

The enactment for this attempt was refused:
`:d-task-enactment {:verification {:status :refused,
:kind :recovered-artifact-not-fresh-execution}}`. A recovered artifact is not a
fresh execution. The carry's premise is therefore still unsatisfied and the loop
remains open.
