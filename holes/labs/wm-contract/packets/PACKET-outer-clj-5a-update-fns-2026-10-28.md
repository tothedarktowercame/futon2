From claude-11. Clojure task, pure functions + tests. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Clojure packet 5a of the outer loop: the learning updates, as pure
functions aligned name for name with the Lean
(`/home/joe/code/mathlib4/DarkTower/WarMachine/R20Step.lean`, branch
`darktower`, head `2ad839ecc` or later): `advanceC`, `advanceGainCounts`,
`outerStateLikelihood`, `outerStatePosterior`, `advanceStateCounts`, and the
state part of `step` that combines them. No I/O and no runner wiring (that is
5b).

## New functions in `futon2.aif.outer-state`

- `advance-c` (Lean `advanceC`): `ω_c · c + η_c · δ(ending)`, no addition for
  `:not-yet-evaluated`.
- `advance-gain-counts` (Lean `advanceGainCounts`): decay always, `+ η_gain`
  on the observed gain only when a task was selected.
- `outer-state-likelihood`, `outer-state-posterior` (Lean names): same
  landing and `boundedReach` as `futon2.aif.outer-score/outer-model`; reuse
  that namespace's kernel rather than writing a second copy, so the Lean
  link theorem `pickedOuterKernel_mass_eq_outerStateLikelihood` holds by
  construction here too.
- `advance-state-counts` (Lean `advanceStateCounts`): for the picked task's
  status, `ω · old + η · posterior`; other statuses `ω · old`; posterior 0 on
  an impossible observation; no pick: decay only.
- `next-state` (the state part of Lean `step`): given makeup, state, the
  selection result from `outer-select/select`, the observation, and the
  observed `{:ending-class … :observed-gain …}`, return the next state with
  the click count incremented. Rates come from the makeup.

## Tests (`test/futon2/aif/outer_state_update_test.clj`)

Mirror the Lean results with the same inputs:
`advanceGainCounts_pos` (positivity), the selected-status closed form and
other-status decay, `state_learning_moves_belief` (stop-the-line moves
toward blocked; landing moves toward doable, strictly), and the 3b C update
(kit + one related ending with ω = 9/10, κ = 1 → (99, 263, 9, 9)/380). Each
test names its Lean theorem.

## Gates

- `clj-kondo --lint`, `futon4/dev/check-parens.el`.
- `clojure -M:test -n futon2.aif.outer-state-update-test -n futon2.aif.outer-state-test`.
- Commit only changed/new files by explicit path (futon2 is shared).
