# Repair successor discharge receipt — 2026-09-14

Implementation commit: `8f5bea11`.

`successor-resolution!` validates a retained grounded repairing close and a
later, distinct, same-repair production-shaped successor close. It preserves
their attempt/run identities, close times, review receipt pin, commit, witness
reference and witness pin in `:wm/repair-successor-relation-v1`, then delegates
to the existing `resolve!` writer. Existing resolution is a stable no-op.
`repair-derived-state` derives `:open` or `:resolved` from immutable finding and
resolution records at an explicit cutoff; post-cutoff resolution is excluded.

Commands:

```text
clj-kondo --lint src/futon2/aif/repair_obligation.clj test/futon2/aif/repair_obligation_test.clj src/futon2/aif/full_loop_runner.clj test/futon2/aif/full_loop_runner_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/repair_obligation.clj test/futon2/aif/repair_obligation_test.clj src/futon2/aif/full_loop_runner.clj test/futon2/aif/full_loop_runner_test.clj
clojure -X:test :nses '[futon2.aif.repair-obligation-test futon2.aif.full-loop-runner-test]'
```

Results: kondo exit 0 (0/0), check-parens exit 0, tests exit 0; 185 tests,
985 assertions, zero failures and zero errors. Raw output and exit files are
retained beside this receipt.

Source pins:

```text
33c55fc82d16fb3f1401daa718c4696ef78cad066183dc216eef8dca2f0e5906  src/futon2/aif/repair_obligation.clj
c4b2c39a1d604399328666e224c5eeb68f6189b312b240f359a75b260be0d624  test/futon2/aif/repair_obligation_test.clj
897170b0fb48dabca418f130a0c6356e022f5207a279efe3dcc20be02241c5e9  src/futon2/aif/full_loop_runner.clj
54c04fd3a08ba90342027ecf91d26e0988a7440a8c0f18a2fc0661dd80767492  test/futon2/aif/full_loop_runner_test.clj
```

The current runner's resolution call occurs before its own close exists, so it
was not presented as this successor seam. No live store, serving JVM,
preregistration, or production close was touched.
