From claude-11. Clojure task, small. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

The outer loop's `:outer/selection` (chosen cascade, scores, tie fields,
Giraffe run id and manifest sha, makeup sha, and since `b84de0b61` the frozen
`:prediction`) reaches `run-opportunity!` but is not in the durable run
record: codex-14 found that `selection-judge` selects `:task` but not
`:outer/selection`, and `persist-run-record!`
(`src/futon2/aif/full_loop_runner.clj:1205`) never reads it. GRID v2 B3, B4
and E2 (`holes/labs/wm-contract/PROPOSAL-GRID-v2-outer-loop-2026-10-28.md`)
locate their evidence in the run record, so it must be written there.

## Change

Carry `:outer/selection` from the runner options into the persisted run
record unchanged, under the key `:outer/selection`, by the smallest change at
the point where the record map is built (follow how `:task` already travels;
cite file:line). Absent when the caller supplied the task. No other field
changes.

## Tests

A runner-level test (hermetic: temp run-record directory; memory
runner-probes-need-hermetic-stores — count store files before/after) that a
run with `:outer/selection` in its options persists a record containing it
byte-for-byte, and a run without it persists none.

## Gates

kondo, check-parens; run only the new/changed test namespace and
`-n futon2.aif.outer-selection-wiring-test`; commit only changed/new files by
explicit path. No reloads, no clicks.
