From claude-11. Clojure task, one existing namespace + a new test namespace. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Clojure packet 4b of the outer loop: when no task is supplied, the ordinary
run selects one with the outer loop and then runs the existing task path
unchanged. The seam is the missing-task branch of
`futon2.aif.full-loop-runtime/run-ordinary-opportunity!`
(`src/futon2/aif/full_loop_runtime.clj:503-513`, today
`{:absent :no-task-selection-policy}`), as
`holes/labs/wm-contract/DISCOVERY-outer-loop-clojure-2026-10-28.md` §6 says.
Uses `outer-state` (`1b815310b`), `outer-observation` (`f0fe69feb`),
`outer-select` (`a0147bdef` + `e17116589`). No reloads, no clicks.

## Change

In the missing-task branch only:

1. `outer-state/load-makeup`, `outer-state/read-state`.
2. The Giraffe run: the newest run under `~/code/storage/giraffe/` whose
   manifest says it is the missions source and which `verify-run` accepts
   (or an explicit `:outer/giraffe-run-dir` in opts, which wins). If none,
   return `{:absent :no-verified-giraffe-run}`.
3. `outer-observation/outer-observation` with the real focus and relation
   (not a constant relation).
4. `outer-select/select`.
5. If a task is selected: call `run-ordinary-opportunity-with-task!` with
   `(assoc opts :task {:id <target>} :outer/selection <the select result
   without the field, plus giraffe run id and makeup sha>)`, so the run
   record can carry why this task was chosen. If none:
   `{:absent :no-task-selected :outer/selection …}`.
6. Any refusal from steps 1–4 returns a typed `{:absent … :reason …}`; it
   never falls back to a default task.

A caller-supplied `[:task :id]` still goes straight to
`run-ordinary-opportunity-with-task!`, untouched and unscored. Update the
function's docstring (it currently says there is no task-selection policy).

## Tests (new `test/futon2/aif/outer_selection_wiring_test.clj`)

Stub `run-ordinary-opportunity-with-task!` (it is the runner's existing
path, out of scope here) and the Giraffe run lookup to a fixture run written
with Giraffe's `write-run!` in a temp dir, as `outer_observation_test`
does. Check:

- no task supplied → the stub receives `{:task {:id <selected>}}` and an
  `:outer/selection` with cascade id, scores and giraffe run id;
- caller-supplied task → selection is not called (assert by redefining
  `outer-select/select` to throw) and the stub gets the caller's task;
- no verified run → `{:absent :no-verified-giraffe-run}`, stub not called;
- every cascade gates out → `{:absent :no-task-selected …}`, stub not called.

Do not write to the real `data/wm-learning/outer-state.edn` or run records
(memory: runner probes need hermetic stores).

## Gates

- `clj-kondo --lint`, `futon4/dev/check-parens.el` on changed/new files.
- Run only `-n futon2.aif.outer-selection-wiring-test` and the existing
  `futon2.aif.task-selection-absence-test` (update it if its absence
  expectation changes; say how).
- Commit only changed/new files by explicit path (futon2 is shared).
