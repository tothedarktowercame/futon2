# Close containment execution receipt — 2026-09-14

Scope: `src/futon2/aif/full_loop_runner.clj` and
`test/futon2/aif/full_loop_runner_test.clj`.  No live runner or serving JVM
was contacted.

Implementation commits, in order:

- `10090021` — initial durable typed-close implementation and controls.
- `57601d6b` — remove redundant test bindings found by kondo.
- `b4412667` — put containment at the `close!` boundary and update the
  delivery-QA close expectation.
- `fc15743f` — repair the close binding's delimiter found by the first
  boundary gate.
- `578ee155` — keep the sibling checkpoint durable while injecting its read
  refusal, so the fallback can lawfully produce `007-closed.edn`.

The retained `tests.out`/`tests-final.out` runs exposed that a close invoked
from an existing catch cannot be caught by that same catch.  The retained
`*-boundary` run then exposed the delimiter error.  The retained
`tests-final2.out` run had one failing control because that control deleted a
required durable checkpoint, independently preventing `close-attempt!` from
writing 007.  These failures are preserved rather than overwritten.

Final commands (each exit is retained beside its raw output):

```text
clj-kondo --lint src/futon2/aif/full_loop_runner.clj test/futon2/aif/full_loop_runner_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/full_loop_runner.clj test/futon2/aif/full_loop_runner_test.clj
clojure -X:test :nses '[futon2.aif.full-loop-runner-test]'
```

Final results:

- `kondo-final3.exit`: 0 (0 errors, 0 warnings).
- `parens-final3.exit`: 0.
- `tests-final3.exit`: 0; 167 tests, 906 assertions, 0 failures, 0 errors.

The tests use isolated temporary cohort roots. Typed `ExceptionInfo` data is
retained under `:refusal-data`; arbitrary throwables retain
`:exception-class`. Both close as `:build-failed` with a typed `:sorry`.
