# C5-habit-1 test receipt

- Candidate HEAD before commit: `8bea9696c`
- Worktree: `/home/joe/worktrees/futon2/c5-habit-1`
- Command (run once):

  ```sh
  clojure -X:test :nses '[futon2.aif.c5-habit-identity-test futon2.aif.enactment-habit-test futon2.aif.wm.reading-family-decision-test]'
  ```

- Environment: independent Clojure process in the worktree; no shared JVM,
  endpoint, reload, or click.

## Result

`futon2.aif.c5-habit-identity-test` and
`futon2.aif.enactment-habit-test` completed without reported failures.  The
requested aggregate command exited non-zero because the existing
`futon2.aif.wm.reading-family-decision-test` test
`unread-targets-are-counted-and-never-chosen` reported four failures:

```
expected selector calls = 1; actual 0
expected selected target = "M-current"; actual nil
expected failed target set = #{"T-no-path" "M-unread"}; actual #{}
expected failure kinds = #{:target-source-path-absent :no-current-target-reading}; actual #{}
```

The test runner reported `Ran 26 tests containing 143 assertions. 4 failures,
0 errors.`  This receipt records the failure unchanged; no retry was run.

## Review fix (claude-7)

The precedence test gave the competing recorded key a different policy id, so
the recorded-key branch could not fire whichever branch came first. It now
changes slot 1 (target) and keeps the candidate id at slot 2, so only branch
order decides. Run once after the fix:
`clojure -X:test :nses '[futon2.aif.c5-habit-identity-test]'` → 2 tests,
12 assertions, 0 failures. Mutations, each run once and reverted:
- candidate guard replaced by `true`: 2 failures (lines 50, 51);
- recorded-key branch moved before the `:action` branch: 1 failure (line 66).
