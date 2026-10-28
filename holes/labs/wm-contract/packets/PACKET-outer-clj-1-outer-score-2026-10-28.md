From claude-11. Clojure task, one new namespace + its test. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Clojure packet 1 of the outer (task-selection) loop: the pure outer score,
aligned name for name with the Lean in
`/home/joe/code/mathlib4/DarkTower/WarMachine/R20Step.lean` (head
`d845c84fb` or later on branch `darktower`). Lean is the specification; the
Clojure follows it. No I/O, no runner wiring, no reloads, no clicks. Design:
`/home/joe/code/futon2/holes/labs/wm-contract/NOTE-outer-G-computation-2026-10-28.md`;
reuse map: `/home/joe/code/futon2/holes/labs/wm-contract/DISCOVERY-outer-loop-clojure-2026-10-28.md`.

## New namespace `futon2.aif.outer-score` (`src/futon2/aif/outer_score.clj`)

Pure functions, each docstring naming its Lean counterpart:

- `apply-selection-step`, `apply-selection-cascade`, `cascade-pick`
  (Lean `applySelectionStep`, `applySelectionCascade`, `cascadePick`): steps
  `{:gate pred}`, `{:order key}` (stable, least first), `:ready-gate`,
  `:by-unblocks`, `:by-cone` (stable, most first) over a field of candidates
  `{:target id :reach r}` with facts `{id {:status :ready|:not-ready|:undetermined :unblocks n :cone n}}`.
- `outer-model` (Lean `outerModel`): q over `#{:doable :blocked}` from the
  status's counts; kernel per `pickedOuterKernel` / `stoppedOuterKernel`
  (landing = (class from relation to focus, ready-gain of unblocks:
  `:none`/`:one`/`:several`), reach clamped to [0,1] as `boundedReach`).
- `outer-preference` (Lean `outerPreferenceKernel`): product of normalised
  ending-class counts and normalised gain counts.
- `outer-novelty` (Lean `outerKernelNovelty` / `outerNovelty`): the
  Fisher-information form, ½·Var(θ)·Σ_o (A(o|doable)−A(o|blocked))²/P(o),
  Var(θ)=ab/((a+b)²(a+b+1)). Do NOT use `cascade-coapply-g/beta-novelty`
  (it is the eq.-57 form that does not depend on the outcome's
  informativeness; the Lean replaced it for this reason).
- `outer-score` (Lean `outerScore`): risk + ambiguity − novelty, computing
  risk + ambiguity with `futon2.aif.cascade-g/step-g` if its contracts
  (`:model`, `:context`, authority checks) can be met by an outer model
  without faking provenance; otherwise compute them directly from the Lean
  definitions (`CascadeEFE.risk`, `ambiguity`) and say in the summary why
  `step-g` did not fit. `:inadmissible` (Lean `⊤`) where the Lean returns `⊤`.

## Tests (`test/futon2/aif/outer_score_test.clj`)

Reproduce the Lean examples with the same inputs and assert the same
orderings (and, where the Lean gives closed forms, the values to 1e-9):
`outer_ambiguity_matters`, `outer_ready_gain_matters`,
`ambiguity_changes_outer_choice` (risk alone prefers X, score prefers Y),
the 3b flip (`learned_selection_click_zero/one`), the novelty theorems
(uninformative = 0; revealing = 1/(2(a+b+1)); reach 1 > reach 1/2), and
K3.2 (`whatOthersWaitOnFirst` picks P12). Each test names its Lean theorem.

## Gates

- `clj-kondo --lint` on both files: no warnings.
- `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el` on both.
- `clojure -M:test -m cognitect.test-runner -n futon2.aif.outer-score-test`
  only (not the suite).
- Commit only the two new files, by explicit path (futon2 is shared; never
  `git commit -a`).
