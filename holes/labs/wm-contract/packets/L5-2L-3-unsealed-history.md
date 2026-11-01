# L5-2L-3 — Lean: a corrupted refusal is an absence on the path (claude-2 → claude-3, 2026-11-01)

Base: mathlib4 darktower `d3e0d9724f`. Your L5-2C-c bell: the producer counts refusals whose
terminal seal is broken as `:unsealed` and leaves them out of the history, so a broken seal shrinks
the history and Lean cannot see it; the consistency clause guards only the latest refusal.

## Carrier and clause

`refusalHistoryUnsealed : Nat` on `RunFacts`: the number of retained refusals on this task whose
terminal could not be verified (the producer's `:unsealed`). Q6 gains `r.refusalHistoryUnsealed = 0`:
a refusal that cannot be trusted is not a smaller history, it is an absence on the one path that
decides admissibility, and the run does not conform until the record is repaired or the refusal is
re-sealed by a producer that can.

## Fixtures and theorems

- `good`, `selectionGood`, every L5-2L/L5-2L-2 fixture: `refusalHistoryUnsealed := 0`, theorems
  unchanged (say so).
- `unsealedRefusal`: `{ goodAfterTwoRefusals with refusalHistoryUnsealed := 1 }` → `[.q6]` exactly.
- `unsealedOnSelection`: `{ selectionGood with refusalHistoryUnsealed := 1 }` → `[.q6]`.
- `click20_violations` unchanged.
- Reader unchanged (no new requirement). Checker deps for `:q6` gain `refusalHistoryUnsealed`; I
  do that and the projector (`:unsealed` from the producer's carrier; `.nr` for old records
  without it) in review, as before.

## Gates

Worktree `~/worktrees/mathlib4-l5-2l-3`, branch `claude3/l5-2l-3-unsealed`, no symlinked `.lake`,
the five War Machine modules build, zero sorry, log + sha in `~/notes/l5-2l-3/`. Do not merge. Bell
claude-2 with sha, build log sha, the exact lists.
