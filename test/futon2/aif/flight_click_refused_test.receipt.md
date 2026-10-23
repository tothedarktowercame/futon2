# Receipt: futon2.aif.flight-click-refused-test (R1(c-1))

Base: main ed9ce5a15. Code: d8cbd1549 (codex-4, committed by claude-7).
Tests: three by codex-4, two added by claude-7 in review
(refusal-stops-a-flight-whose-observation-moved, a-200-without-click-id-is-a-refusal).
Run by claude-7, 2026-10-23, worktree ~/worktrees/futon2/r1c-click-refused.

## Gates
- clj-kondo --lint src/futon2/aif/flight.clj src/futon2/aif/flight_driver.clj test/futon2/aif/flight_click_refused_test.clj: 0 errors, 0 warnings
- emacs --batch -l ~/code/futon4/dev/check-parens.el (same three files): OK

## New namespace, at d8cbd1549
    clojure -X:test :nses '[futon2.aif.flight-click-refused-test]'
    Ran 5 tests containing 18 assertions. 0 failures, 0 errors.

## Red half (worktree at ed9ce5a15, this test file copied in)
    Ran 5 tests containing 18 assertions. 10 failures, 0 errors.
- server-preflight-refusal, no-response, a-200-without-click-id: :status is
  :no-progress and :refusal is absent.
- refusal-stops-a-flight-whose-observation-moved: :status is not
  :click-refused and more than one POST is made (the flight read :open and
  clicked again). Its "the world did advance" assertion passes on both.
- a-started-click-without-progress-stays-no-progress (control): passes on both.

## Existing namespaces, at d8cbd1549
    clojure -X:test :nses '[futon2.aif.flight-test futon2.aif.flight-driver-test
      futon2.aif.flight-click-refusal-test futon2.aif.flight-click-refused-test
      futon2.aif.flight-operator-stop-test futon2.aif.flight-click-close-test]'
    Ran 39 tests containing 151 assertions. 0 failures, 0 errors.
