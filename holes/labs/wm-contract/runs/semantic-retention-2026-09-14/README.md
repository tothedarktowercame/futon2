# Semantic-retention execution receipt

Scope: commits `723fa30e`, `8f053434`, and `2f68d3e4`. No serving JVM or
production data root was used.

## Attempt trail

- `runner-kondo-baseline.out`: pre-change runner baseline, exit 0, no findings.
- The first post-commit isolated runner lint omitted the newly defining source
  from clj-kondo's analysis set and reported one unresolved cross-namespace var.
  `kondo-final.out` records the corrected complete changed-file invocation and
  exits 0 with 0 errors and 0 warnings.
- `tests-final.out` / `.exit`: first retained focused run after the feature
  commit, exit 1. The new review-text assertion was placed on a deliberately
  build-failed fixture and therefore observed nil.
- `tests-after-test-fix.out` / `.exit`: second retained run after moving the
  assertion to the qualifying fixture, exit 1. It exposed that `job-text`
  faithfully includes the retained text event after the result summary.
- `tests-final-green.out` / `.exit`: final run after pinning that complete exact
  text, exit 0: 165 tests, 880 assertions, 0 failures, 0 errors.

## Final commands

```sh
clj-kondo --lint src/futon2/aif/full_loop_runner.clj src/futon2/aif/limb_evidence.clj test/futon2/aif/limb_evidence_test.clj test/futon2/aif/full_loop_runner_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/full_loop_runner.clj src/futon2/aif/limb_evidence.clj test/futon2/aif/full_loop_runner_test.clj test/futon2/aif/limb_evidence_test.clj
clojure -X:test :nses '[futon2.aif.limb-evidence-test futon2.aif.full-loop-runner-test]'
```

The final complete kondo run is 0/0 (`kondo-after-test-fix.out`); the final
test-file-only rerun after the last assertion correction is also 0/0
(`kondo-final-test.out`). Paren checks are `OK` (`parens-final.out` and
`parens-final-test.out`). Raw focused test output and numeric exits are retained
beside this file.
