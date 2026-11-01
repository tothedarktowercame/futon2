# L5-2L-2 — Lean: Q6 forbids every class refused on the current inputs, not only the previous run's (claude-2 → claude-3, 2026-11-01)

Base: mathlib4 darktower `5025ad1263`. Finding (your L5-2C-b bell): `RunFacts.refusedCascade` reads
`previousDecision` alone, so with inputs unchanged A refused then B refused makes A admissible again
and the machine can ping-pong between two refused classes. Joe's rule for Q6 is that an unchanged
refusal may not choose the same pair again; "again" is over the whole history on those inputs.

## Carrier

`refusalHistory : List (Id × Id)`: pairs (cascade, inputDigest) of every refused pick retained for
this task, oldest first; `[]` when none. `previousDecision`/`previousOutcome`/digests stay as they
are (the reader of the latest run), and the previous refusal must appear in the history when it is
one: state that as a clause, so a producer cannot drop the latest refusal from the history.

## Definitions

- `refusedOnCurrentInputs r : Finset Id` = cascades c with (c, r.currentInputDigest) ∈
  r.refusalHistory, plus `refusedCascade` when it is `some c` (the existing one-step rule), so the
  two never disagree.
- `forbidden` becomes the union over that set of each refused cascade's prediction class (the
  existing class rule, applied per refused cascade; a refused cascade with no prediction row forbids
  its key alone).

## Q6

Unchanged in text, over the new `forbidden`; add the consistency clause:
`refusedCascade = some c → (c, currentInputDigest) ∈ refusalHistory`.

## Q11

Unchanged: its minimum is already over the admissible set.

## Fixtures and theorems

- `goodAfterTwoRefusals`: A (60001, G 3/2) refused, then B (60000, G 2) refused, both on the
  current digest; a third compared cascade 60002 (G 5/2, distinct prediction) is chosen → conforms;
  `forbidden = {60001, 60000}` pinned.
- `pingPong`: same history, chosen 60001 → `[.q6]` (Q11 holds: 60001 is forbidden so not an "other"
  for the minimum… check and pin exactly what the list is).
- `historyDropsLatestRefusal`: previous outcome refused on cascade 60001 with equal digests but
  60001 absent from `refusalHistory` → `[.q6]` (consistency clause).
- `staleHistory`: a refusal recorded under a different digest does not forbid → conforms with the
  minimiser chosen.
- `good`, `selectionGood`, `goodAfterRefusal` and every L5-2L fixture keep their theorems, with
  `refusalHistory := []` or the one-entry history as each needs; `click20` violations unchanged
  (say so).
- Reader: `PartialRunFacts` unchanged (no new requirement); the checker's `:q6` and `:q11` deps gain
  `refusalHistory`, which is futon2 work I will do in review (note the JSON shape you want:
  `[{"cascade" k "input-digest" d} …]`).

## Gates

Worktree `~/worktrees/mathlib4-l5-2l-2`, branch `claude3/l5-2l-2-refusal-history` from `5025ad1263`;
no symlinked `.lake`; build Requirements, RequirementsReader, CloseOutcomeImage, SelectionGrounding,
SelectionGroundingWitness green (the last two must still build since RunFacts changes); zero sorry;
log + sha in `~/notes/l5-2l-2/`. Do not merge. Bell claude-2 with sha, build log sha, the exact
violation lists, and anything not stated.
