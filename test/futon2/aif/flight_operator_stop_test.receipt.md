# Receipt: futon2.aif.flight-operator-stop-test (R1(b))

Base: main 9e52f6ccb. Code: 276ee9bc8 (codex-4) + 42c4e2b79 (claude-7 review fix and tests).
Run by claude-7, 2026-10-23, worktree ~/worktrees/futon2/r1b-operator-stop.

## Gates
- clj-kondo --lint src/futon2/aif/flight.clj src/futon2/aif/flight_driver.clj test/futon2/aif/flight_operator_stop_test.clj: 0 errors, 0 warnings
- emacs --batch -l ~/code/futon4/dev/check-parens.el (same three files): OK

## New namespace, at 42c4e2b79
    clojure -X:test :nses '[futon2.aif.flight-operator-stop-test]'
    Ran 6 tests containing 35 assertions. 0 failures, 0 errors.

## Red halves (src/ swapped to the named commit, tests at 42c4e2b79)
- 276ee9bc8 (stop read at the loop head only): 2 failures, both in
  stop-written-during-the-ask-skips-that-click (lines 75, 83): the click after
  the ask is still made.
- 9e52f6ccb (main, no stop): 21 failures, in stop-after-click-one,
  stop-written-during-the-ask, stop-file-before-the-first-click (both files).
  The controls (three-click world, another flight's .stop, no file) pass.

## Existing namespaces, at 42c4e2b79
    clojure -X:test :nses '[futon2.aif.flight-test futon2.aif.flight-driver-test
      futon2.aif.flight-driver-budget-preflight-test futon2.aif.flight-runner-test
      futon2.aif.flight-ask-test futon2.aif.flight-enact-test futon2.aif.flight-click-close-test]'
    Ran 53 tests containing 211 assertions. 7 failures, 0 errors.
All 7 are in flight-ask-test (lines 102-105, 110, 111, 185). The same 7 fail
with src/ at main 9e52f6ccb, so they predate this change.
