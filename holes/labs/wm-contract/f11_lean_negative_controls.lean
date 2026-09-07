-- f11_lean_negative_controls.lean -- `:F11` slice 2 review.
-- Run: cd ~/code/mathlib4 && lake env lean <this file>.
-- BOTH examples MUST FAIL to elaborate.  They plant, against the tactic that
-- proves findSnatchReplayExcludesDeclaredZeroMass, a pattern the record
-- SELECTS -- so if either compiles, that theorem's proof does not discriminate
-- between an excluded pattern and a selected one and the F4 witness is empty.

import DarkTower.WarMachine.F11Conformance
open DarkTower.WarMachine.Holes

-- NEGATIVE CONTROL 1: a pattern the record SELECTS must not be provable as excluded.
example :
    SnatchPattern.askForSurplusNotSurrender ∉
      (findSnatchReplay { context := FindSnatchScenario.g1Snatcher, want := True, however := True }
        findSnatchRepository).selected := by
  cases hc : FindSnatchScenario.g1Snatcher <;>
    simp [hc, findSnatchReplay, findSnatchSelected, findSnatchRepository,
      findSnatchScenarios, snatchRepository]

-- NEGATIVE CONTROL 2: a zero-mass member of a DIFFERENT scenario must not go through.
-- g4Snatcher's zero-mass is forcedPlayNeedsALossFloor; g4 SELECTS consultTheRemedyBeforeExiting,
-- which is g1's zero-mass.  So this must fail.
example :
    SnatchPattern.consultTheRemedyBeforeExiting ∉
      (findSnatchReplay { context := FindSnatchScenario.g4Snatcher, want := True, however := True }
        findSnatchRepository).selected := by
  cases hc : FindSnatchScenario.g4Snatcher <;>
    simp [hc, findSnatchReplay, findSnatchSelected, findSnatchRepository,
      findSnatchScenarios, snatchRepository]
