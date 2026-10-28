From claude-11. Lean task, one existing module. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Slice 3c of `/home/joe/code/futon2/holes/labs/wm-contract/SPEC-r20-step-function-2026-10-28.md`,
in `DarkTower/WarMachine/R20Step.lean` (`/home/joe/code/mathlib4`, branch
`darktower`, on top of slice 3b `cd9d0af5f`): task-selection cascade steps
that read the facts the Giraffe pass computes, so that the patterns
`only-what-can-start-now` and `what-others-wait-on-first`
(`/home/joe/code/futon3/library/task-selection/`) are expressible.

Why these two: on the 32 recorded selection events that list their
alternatives, Giraffe's readiness and unblock counts separate the candidates
in 13 events, and in all 13 the chosen task is first in Giraffe's order
(futon2 `8ff2e17c0`, `holes/labs/wm-contract/evidence/selection-testset-giraffe-run-2026-10-28.tsv`).
Cost and value are not added: the record gives them for every candidate in
only 3 and 7 events, as words, not numbers.

Do not edit `R15InjuryTier.lean` or any module other than `R20Step.lean`.

## Types

- `GiraffeFacts`: a structure `{ ready : Bool, unblocks : ℕ, cone : ℕ }`.
  Docstring: computed by `futon3c.giraffe.pass` (readiness from need edges,
  one-step unblocks, transitive cone); readiness is recomputed each click,
  never stored on the task (M-giraffe, Joe 2026-10-26).
- `Observation` gains `facts : Target → GiraffeFacts` (supplied from outside,
  like `field`; docstring says so).
- `SelectionStep` gains three constructors: `readyGate` (keep candidates
  whose facts say ready), `byUnblocks` (stable order, most unblocks first),
  `byCone` (stable order, largest cone first). `applySelectionStep` and
  `applySelectionCascade` take the facts as an argument; `step` passes
  `o.facts`. Docstrings name the pattern each constructor expresses.
- `whatOthersWaitOnFirst : SelectionCascade := [readyGate, byCone, byUnblocks]`
  (a stable sort applied last is the primary key, so this orders by unblocks,
  ties by cone; say so in the docstring, or order the list however makes
  "unblocks first, then cone" true, and prove it in theorem 3).

## Theorems

1. All slice 1–3b theorems still build (update their statements only where
   the new facts argument must appear; say which in the summary).
2. Membership still holds for the new steps (pick ∈ field).
3. If `cascadePick whatOthersWaitOnFirst facts field = some c` then
   `(facts c.target).ready = true`, and every ready candidate `d ∈ field` has
   `(facts d.target).unblocks ≤ (facts c.target).unblocks`.
4. If no candidate in the field is ready, the pick is `none` (so
   `selection = none` by 3a's theorem).

## Example

Event K3.2 (`futon3c/holes/missions/M-象-2000.md:557`): candidates P12
"core system working end to end" (ready, unblocks 2), "edge cases:
provisional withdrawal" (not ready), "countersigned voids" (not ready), as
the Giraffe run computed. Show `cascadePick whatOthersWaitOnFirst` picks P12.
Use target ids 1, 2, 3 and say in the docstring which is which.

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`.
- `#print axioms` on theorems 3 and 4: only `propext`, `Classical.choice`,
  `Quot.sound`. Paste the output.
- Commit only `DarkTower/WarMachine/R20Step.lean` (explicit path).
