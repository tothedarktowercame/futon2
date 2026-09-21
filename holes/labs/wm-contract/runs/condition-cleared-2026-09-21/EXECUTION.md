# Condition-cleared dismissal

Author: codex-1. Transition commit: 82e8120ddaafd8cea136ecca57b409d00d9a799f.
Authority: claude-12, invoke-1789964167506-22884-f4152906.

The new dismissal retains a dated source and nonempty observation map, actor,
and reason. Missing or malformed evidence refuses; missing/blank actor or
reason refuses. Resolved, dismissed, or implemented findings refuse.
The actor remains accountable for interpreting the observation: the API does
not treat an arbitrary observation map as mathematical proof of recovery.

Gates executed:

- `clj-kondo --lint src/futon2/aif/repair_obligation.clj test/futon2/aif/repair_wontfix_test.clj`: 0 errors/warnings.
- `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- src/futon2/aif/repair_obligation.clj test/futon2/aif/repair_wontfix_test.clj`: OK.
- `clojure -M:test -m cognitect.test-runner -n futon2.aif.repair-wontfix-test`: 7 tests, 118 assertions, 0 failures/errors.
- Two postcommit runs from futon3c of `clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/condition-cleared-2026-09-21/registry.edn`: each 7 tests, 118 assertions, 0 failures/errors.

Warrants, bound to `repair-store/dismiss-condition-cleared`:

1. `test-registry-7cd8724240d5710726f73ba52c741b02fc04787254863a1427a2716d59e74033`
2. `test-registry-4a192b63d5ba953f891123cfb0c7e49948434f25b7177e54590190cfff44dfb8`

## Application

Three explicit findings, individually re-checked immediately before each
`dismiss-condition-cleared!` call. Their retained failures named a busy author.
Each endpoint returned the same named author idle and invoke-ready.
All three calls returned `:dismissed-condition-cleared`, with no refusals.
Original finding file contents were compared before and after each call and
were identical. The full responses and returned dismissal records are retained
under `:condition-cleared-application` in the sibling
`wontfix-sweep-2026-09-21.edn`.

The authoritative `open-obligations` count was 47 before and 44 after.
This is not a repair success; no implementation or resolution records were
created and no WM click ran. The eight unregistered holds and attempt-054
were untouched. This follow-up changes the previous sweep's three
retained-no-applicable-API rows to three condition-cleared dismissals.
