# `dismiss-unexecuted!` registered execution

Implementation commit: `333ec13e6d49e7ff86afc5ae173295e4aa347bc4`.

No live repair finding was dismissed. Tests used temporary store roots only;
no click or serving-JVM interaction occurred.

## Static gates

Executed before the implementation commit:

```text
clj-kondo --lint src/futon2/aif/repair_obligation.clj test/futon2/aif/repair_obligation_test.clj
linting took 49ms, errors: 0, warnings: 0

emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/repair_obligation.clj test/futon2/aif/repair_obligation_test.clj
OK

git diff --check -- src/futon2/aif/repair_obligation.clj test/futon2/aif/repair_obligation_test.clj
exit 0
```

## Single registered execution

Executed once from `/home/joe/code/futon3c`:

```text
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/dismiss-unexecuted-2026-09-19/register.edn

evidence-id test-registry-108e2264e772dd152ec6ea14e0bac0e02ff63584f6d4357d484c338fcf0a3f65
warrant? true
results {:assertions 97, :duration-ms 1400, :errors 0, :exit 0, :failures 0, :tests 21}
bound repair-store/dismiss-unexecuted -> test-registry-108e2264e772dd152ec6ea14e0bac0e02ff63584f6d4357d484c338fcf0a3f65
```

Registry artifact paths and SHA-256 pins:

- `/home/joe/code/storage/test-registry/repair-store-dismiss-unexecuted-2026-09-19/053db85a-0fab-4217-a376-17466d6f3d9e.closure.edn`
  — `79363b652857b4c985f3a2040705ce4dab490201eaad96ca30a8dadd59c2b835`
- `/home/joe/code/storage/test-registry/repair-store-dismiss-unexecuted-2026-09-19/053db85a-0fab-4217-a376-17466d6f3d9e.log`
  — `01c01e3ab1e485766983879400a2c68b334a8f6ce8a14385d8f8096fb47ca976`

Source pins at execution:

- `src/futon2/aif/repair_obligation.clj` —
  `3f85a11e50b18f161593d4689a4b99939eec02012ea38c77b26c28257da3825e`
- `test/futon2/aif/repair_obligation_test.clj` —
  `7bc1c15f0ec41e4eaf4518c58287b2866b31d798f41a1d3db07830ee8848118f`
- `register.edn` —
  `233f6bcd88250c3665a0b2bc9bc28feb1a0451a433e841c942ce545275fd8957`

## Covered contract

The controls prove that an executed finding refuses `:finding-executed`, a
missing/ambiguous execution record refuses `:execution-not-retained`, a
never-executed finding is excluded by the runner's exact eligible predicate
while remaining audit-readable as `:dismissed-unexecuted`, the finding bytes
remain identical, and a second disposition refuses `:already-dismissed`.
