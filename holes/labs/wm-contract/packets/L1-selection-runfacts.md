# Packet L1 (claude-2 -> claude-3): the selection run as a RunFacts

Context, read first (10 min): futon2/holes/labs/wm-contract/PLAN-outer-selection-restoration-claude2-2026-11-01.md
and the three INCIDENT-outer-selection-*-claude2-2026-11-01.md records in its reconciliation/.
Short form: Requirements.lean today treats the outer choice as given (`RunFacts` doc line 23,
`Choice` examined by no Q). Joe's ruling: task selection is the same loop and meets the same
requirements. This slice makes the selection run expressible as a `RunFacts` and makes the
façade fail.

## Goal (one file, one behaviour)

In `DarkTower/WarMachine/Requirements.lean`, restore Q2 for the selection kind. The deleted
text is at `git show 3a09d2a80f^:DarkTower/WarMachine/Requirements.lean` (the Q2 docstring and
definition). Keep what it said about construction; drop what tied it to the deleted
frontier/carrier modules (Q8, OuterCascadePolicyObjective). Required conjuncts, for a
RunFacts describing a selection run:

- `targetConstruction.length = 1`; for that `tc`: `tc.reading ≠ 0 ∧ tc.library ≠ 0 ∧
  tc.graph ≠ 0 ∧ tc.kernel ≠ 0`, `tc.pool = tc.slice`, `tc.sliceFromWholeLibrary = true`,
  `tc.slice.card ≤ libraryPatternCount`, `0 < tc.policyCount`, `tc.policies.card =
  tc.policyCount`, `comparedPolicies ⊆ tc.policies`, `2 ≤ comparedPolicies.card`.
- `tc.targets = enumeratedTasks` for a selection run (the construction serves the whole
  field), and Q1 as it stands (`enumeratedTasks = openTasks`).
- `constructorPatternCount = tc.pool.card`.

Add `q2` back to `Requirement`, `violations` and `conforms`. Update the doc line of
`RunFacts` so it no longer says the census is "made after the outer choice".

## Witnesses (all `native_decide`)

- `good` still conforms (`good_conforms`, `good_has_no_violations` unchanged in statement).
  If `good` needs a Q2-satisfying construction, extend `goodConstruction` minimally and say so.
- `selectionGood : RunFacts`: a selection run with 3 compared cascades drawn as a slice from a
  19-pattern library, all pins non-zero. `conforms selectionGood = true`.
- Negatives, each `Q2 _ = false`: `handListedMenu` (library pin 0, the outer-makeup.edn shape:
  3 cascades, pool of 3 pattern ids, `sliceFromWholeLibrary = false`); `oneCascadeMenu`
  (`comparedPolicies.card = 1`); `menuNotFromSlice` (`pool ≠ slice`); `unpinnedGiraffe`
  (`reading = 0`).

## Gates

- Worktree: `git worktree add ~/worktrees/mathlib4-l1-selection-runfacts -b claude3/l1-selection-runfacts darktower`
  from `/home/joe/code/mathlib4` (branch `darktower`, head `b5c7da28f4`). Never under
  `~/worktrees/mathlib4/` (that path is a symlink into the primary) and never in /tmp.
  Symlink `.lake -> /home/joe/code/mathlib4/.lake` for the build cache.
- `lake build DarkTower.WarMachine.Requirements DarkTower.WarMachine.RequirementsReader`
  green in the worktree; no `sorry`. If `RequirementsReader.lean` must change because a
  field was added, change it minimally and list the change.
- Do not merge, do not touch the primary checkout, do not touch Clojure.
- Commit on the branch. Bell `claude-2` back with: branch, sha, the list of theorems added,
  the build log path, and anything in the deleted Q2 you chose not to restore and why.

Scope note: L2 (strict minimum, distinct predictions, per-policy G carrier) and L3
(selection-path absences) are separate packets and come after review of this one. Do not
start them.
