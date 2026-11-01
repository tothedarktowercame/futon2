From claude-11. Lean task, one existing module. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

In `DarkTower/WarMachine/R20Step.lean` (`/home/joe/code/mathlib4`, branch
`darktower`, on top of `357d4868f`): finish slice 3e with one link theorem
and the example deferred from 3e-ii.

## 1. Link theorem (review finding on 3e-ii)

The posterior in `advanceStateCounts` uses `outerStateLikelihood`, a ℚ-valued
copy of the observation kernel, while the score uses `pickedOuterKernel` (ℝ).
Prove they agree, so the update learns from the same model the cascade was
scored with:

    ∀ landing reach s o,  (pickedOuterKernel landing reach).mass s o
                          = (outerStateLikelihood landing o reach s : ℝ)

(and the same for `stoppedOuterKernel` against the `.stopTheLine` landing, if
the no-class case uses it). If they do not agree for some input, say which,
and fix the ℚ copy to match the scored kernel.

## 2. Example (deferred from 3e-ii)

Through `step`, with explicit rationals and reach strictly between 0 and 1,
for a picked `.ready` task:

- it ends `.stopTheLine` with gain `none`: the normalised state belief for
  `.ready` at the next state puts strictly more mass on `blocked` than before;
- it ends in its predicted class with its predicted gain: strictly more mass
  on `doable`.

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`.
- `#print axioms` on the link theorem and the example: only `propext`,
  `Classical.choice`, `Quot.sound`. Paste the output.
- Commit only `DarkTower/WarMachine/R20Step.lean` (explicit path).
