# Revision-pair companion admission receipt

Implementation commit: `2ed49ee5`.

Commands executed after commit, once:

```sh
clj-kondo --lint src/futon2/aif/limb_evidence.clj src/futon2/aif/full_loop_runner.clj test/futon2/aif/limb_evidence_test.clj test/futon2/aif/full_loop_runner_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/limb_evidence.clj src/futon2/aif/full_loop_runner.clj test/futon2/aif/limb_evidence_test.clj test/futon2/aif/full_loop_runner_test.clj
clojure -X:test :nses '[futon2.aif.limb-evidence-test futon2.aif.full-loop-runner-test]'
```

Results: clj-kondo 0 errors / 0 warnings; check-parens `OK`; fresh JVM 174
tests, 924 assertions, 0 failures, 0 errors.

Live pins checked from cohort-53 attempt-001:

- `runner.before`: `cdfef17652ee498875b846bf39e0a95ef892df22f48ed166303927a1715ff995`
- `runner.after`: `f6238a7da08bde85e37f12490e0a1202eaccc139bbf5fd1f69942076e4bc4115`

No production files were written or amended by the tests.
