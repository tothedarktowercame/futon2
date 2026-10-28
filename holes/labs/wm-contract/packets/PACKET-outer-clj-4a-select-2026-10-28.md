From claude-11. Clojure task. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Clojure packet 4a of the outer loop: choose a task, aligned with Lean
`R20Step.chosenCascadeEntry` / `step`'s selection (`cascadeIndex`,
`cascadeScores`, `selection : Option OuterSelection`), using
`futon2.aif.outer-score` (`e6c39ef7e`), `futon2.aif.outer-state`
(`1b815310b`) and `futon2.aif.outer-observation` (`f0fe69feb`). Pure: no
runner wiring (that is 4b), no reloads, no clicks.

## Changes

- `data/wm-learning/outer-makeup.edn` gains `:cascades`, a menu of named
  task-selection cascades as data, each with the library pattern ids it
  expresses (`futon3/library/task-selection/*.flexiarg`):
  - `:what-others-wait-on-first` = `[:ready-gate :by-cone :by-unblocks]`
    (patterns `only-what-can-start-now`, `what-others-wait-on-first`);
  - `:only-what-can-start-now` = `[:ready-gate]`;
  - `:find-out-before-building` = `[{:gate-status :undetermined} :by-cone]`
    (pattern `find-out-before-building`: attempt an item whose readiness
    cannot be determined; this is the cascade novelty can favour).
  Each entry's comment says which pattern file it comes from.
- `futon2.aif.outer-score` gains the data step `{:gate-status s}` (keep
  candidates whose fact status is `s`), documented as an instance of Lean
  `SelectionStep.gate`.
- New `futon2.aif.outer-select/select` (Lean `chosenCascadeEntry` + the
  selection fields of `step`): given makeup, state and observation, score
  every menu cascade with `outer-score`, take the first minimum in menu order
  (docstring: a tie is resolved by menu order only so the function is
  defined; ties are a defect, Joe), and return
  `{:cascade-index i :cascade-id k :cascade-scores [...] :selection {:candidates field :selected c} | nil}`
  with `:selection nil` exactly when the chosen cascade picks nothing, plus
  `:tie? true` when the minimum is shared (recorded, not hidden).

## Tests (`test/futon2/aif/outer_select_test.clj`)

- The chosen score is ≤ every menu score (Lean `chosenCascade_score_le`).
- A selected candidate is in the field (Lean `step_selection_some`); a menu
  whose chosen cascade gates everything out yields `:selection nil`.
- On the real Giraffe run `20261028T154151.285Z-b916cd4e` with the makeup's
  initial state: report (in the test output and the summary) which cascade
  and which mission are chosen, the three scores, and whether there is a
  tie. Assert only that a selection is made and the scores are finite; the
  values are the demonstration, not a fixture to match.

## Gates

- `clj-kondo --lint`, `futon4/dev/check-parens.el` on changed/new files.
- `clojure -M:test -n futon2.aif.outer-select-test` and
  `-n futon2.aif.outer-score-test` only.
- Commit only the changed/new files by explicit path (futon2 is shared).
