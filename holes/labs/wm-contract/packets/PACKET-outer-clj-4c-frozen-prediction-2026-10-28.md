From claude-11. Clojure task, small. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

GRID criterion B4 for the outer loop: the choice binds a prospective
prediction in the domain that will be observed. `futon2.aif.outer-select/select`
(`src/futon2/aif/outer_select.clj`) returns the chosen cascade, scores and
selection, but not what the outer model predicted. Add it.

## Change

`select` adds `:prediction` for the chosen pick: the predicted outcome
distribution `P(o) = Σ_s q(s)·A(o|s)` over `[ending-class gain]` (Lean
`push q a` of the chosen cascade's `outerModel`), the prior `q` over
`:doable`/`:blocked`, the pick's Giraffe status, and the landing. Computed from
`outer-score/outer-model`, not a second model. `nil` when nothing is picked.
`full_loop_runtime`'s retained `:outer/selection` already keeps `select`'s
result minus the field, so the prediction reaches the run without further
wiring; check that, and say if it is dropped anywhere on the way to the run
record.

## Tests

In `test/futon2/aif/outer_select_test.clj`: the prediction sums to 1, equals
`outer-model`'s push for the chosen cascade, and is nil with no pick.

## Gates

kondo, check-parens; run only `-n futon2.aif.outer-select-test` and
`-n futon2.aif.outer-selection-wiring-test`; commit only changed files by
explicit path. No reloads, no clicks.
