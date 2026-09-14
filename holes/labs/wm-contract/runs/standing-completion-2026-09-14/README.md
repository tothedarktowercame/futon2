# Standing-decision completion receipt

Implementation commits: `26694d1e`, `35ffea78`, `322db5c9`, `ea9ca265`.

The first post-commit gate exposed an unmatched delimiter: kondo exited 3,
check-parens exited 1, and the fresh JVM could not read the namespace. After
`35ffea78`, static gates passed; the runner suite then exposed the changed
private revision-prompt arity (162 tests, 1 error). `322db5c9` restored the
legacy arity. `ea9ca265` added direct one-shot completion controls, requiring
one request for absent/mismatched evidence, accepting a deposit appearing
during the bounded wait, avoiding redispatch when already valid, and retaining
typed `:standing-evidence-insufficient` / `:incomplete` on failure.

Final commands:

```sh
clj-kondo --lint src/futon2/aif/full_loop_runner.clj test/futon2/aif/full_loop_runner_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/full_loop_runner.clj test/futon2/aif/full_loop_runner_test.clj
clojure -X:test :nses '[futon2.aif.full-loop-runner-test]'
```

Final results: kondo 0 errors/0 warnings; parens `OK`; 162 tests, 879
assertions, 0 failures, 0 errors. Raw final test output and exit are retained as
`tests-after-helper.out` and `tests-after-helper.exit`; the preceding green
pre-helper run is retained as `tests-final.out` / `.exit`.
