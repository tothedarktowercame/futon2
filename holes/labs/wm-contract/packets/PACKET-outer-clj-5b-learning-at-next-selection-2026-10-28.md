From claude-11. Clojure task. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Clojure packet 5b of the outer loop: the outer state learns from each
selected click, using the pure updates of `futon2.aif.outer-state`
(`advance-c`, `advance-gain-counts`, `advance-state-counts`, `next-state`;
`4fd99f5bf`) and the carrier (`read-state` / `write-state!`).

Joe's ruling (2026-10-28): items made ready are observed only when the chosen
task has closed, as seen in a later Giraffe run; otherwise the gain
observation is `:none`. State that limit in the docstring.

## Design: update at the next selection

The outcome of click n is complete only after it ends, and its gain only in a
later Giraffe run, so the update happens when the next selection starts:

1. When the missing-task branch of `run-ordinary-opportunity!`
   (`src/futon2/aif/full_loop_runtime.clj`) selects a task, it writes the
   state with `:pending {:selection <retained :outer/selection>, :run-id …,
   :giraffe/run-id …}` (atomic, via `write-state!`) before running the task.
2. At the start of the next missing-task selection, if `:pending` is present:
   - ending class: from click n's run record (find where the run record
     carries the terminal ending class used by the class preference / focus
     receipt; cite file:line). If the record is absent or carries no class,
     do not invent one: keep `:pending`, record `{:outer/learning :deferred
     :reason …}` in the new selection, and select from the unupdated state.
   - observed gain: compare the chosen task's status in click n's Giraffe run
     with the current verified run. If it is now closed, gain is
     `readyGain` of the number of items ready now that were not ready then;
     otherwise `:none`.
   - apply `next-state` with the makeup's rates; clear `:pending`; write.
3. Caller-supplied tasks do not touch the outer state.

## Tests (hermetic: temp state path, fixture runs; never the real
`data/wm-learning/outer-state.edn`)

- a selection writes `:pending`; the next selection consumes it and the
  counts move as `next-state` says;
- a missing run record keeps `:pending` and records the deferral;
- gain `:none` while the task is open; the right bucket after it closes;
- a caller-supplied task leaves the state file untouched.

## Gates

kondo, check-parens; run only the new test namespace plus
`-n futon2.aif.outer-selection-wiring-test`; commit only changed/new files by
explicit path. No reloads, no clicks.
