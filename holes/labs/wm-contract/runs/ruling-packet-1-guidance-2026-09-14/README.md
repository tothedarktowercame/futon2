# Repair-subject ruling packet 1 receipt — 2026-09-14

Implementation commit: `81ce8543`.

The author guidance distinguishes `subject-*.edn` obligation-state pairs from
`supporting-*.edn` source-artifact pairs. Admission refuses an ambiguous role
or a subject whose entity differs from the selected target. The B-prime
readback preserves the reviewer's verdict and adds
`:resolution-unsupported-by-store` when the finite contract requires a
production-shaped successor but the injected resolution reader finds none.
An unreadable reader is separately annotated `:resolution-store-unreadable`;
neither annotation blocks close.

Commands were executed after the implementation commit:

```text
clj-kondo --lint src/futon2/aif/full_loop_runner.clj test/futon2/aif/full_loop_runner_test.clj test/futon2/aif/limb_evidence_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/full_loop_runner.clj test/futon2/aif/full_loop_runner_test.clj test/futon2/aif/limb_evidence_test.clj
clojure -X:test :nses '[futon2.aif.limb-evidence-test futon2.aif.full-loop-runner-test]'
```

Results:

- clj-kondo: exit 0, 0 errors and 0 warnings (`clj-kondo.out`).
- check-parens: exit 0 (`check-parens.out`).
- fresh JVM: exit 0, 178 tests, 946 assertions, no failures or errors
  (`tests.out`).

Pins at execution:

```text
e18c9a7ca5898cfa2908e7e3468086b24fea89faa5178a0c2d2e112b979f7a48  src/futon2/aif/full_loop_runner.clj
f3df44a499e8c25d88d0669fa455f72c56a59b1771d89cfc4d5f501bc3975dd4  test/futon2/aif/full_loop_runner_test.clj
76ab7bf1b79405debe784d986877cea782c69532ab0cde0c1f56eed96c363849  test/futon2/aif/limb_evidence_test.clj
b26e508c5937e273d8743f0cdcbca942b5fd89b21a9b3c010c5d34b42099d938  holes/labs/wm-contract/RULING-repair-subject-positive-status-2026-09-14.md
```

No live store, serving JVM, preregistration, or resolution writer was used.
