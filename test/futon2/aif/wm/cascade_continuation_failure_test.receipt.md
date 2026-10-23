# C5-habit-6 execution receipt

- Worktree: `/home/joe/worktrees/futon2/c5-habit-6`
- Base HEAD: `9d095eb83e86b3b75424f5fe2fe9452741b13264`
- Environment: isolated worktree JVM; no shared-JVM load and no click.
- Command: `clojure -X:test :nses '[futon2.aif.wm.cascade-continuation-failure-test futon2.aif.wm.cascade-continuation-test]'`
- Result: `Ran 8 tests containing 36 assertions. 0 failures, 0 errors.`
- Static gates: `clj-kondo --lint` on the two source files and test had
  `0 errors, 0 warnings`; check-parens reported `OK`; `git diff --check`
  was clean.
- Negative control: temporarily recording `(first (:frontier packet))` as the
  failed occurrence made the failure test red: expected
  `[:test/occurrence 1]`, actual `[:test/occurrence 0]`. The source was
  reverted before the green command above.

