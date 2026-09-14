# Row 14 close-retention runner wiring receipts

The production source and controls were committed at `2b1f7af30d659de3b73ba9789f5bdaa8a95a23d0`.
The first gate attempt found one unused test binding.  Its raw outputs are
retained as `runner-kondo-after.out`, `test-kondo.out`, `check-parens.out`, and
`tests.out`; the test output itself reports 154 tests / 792 assertions / zero
failures, but the enclosing tool invocation timed out before retaining its
aggregate exit status.  The binding-only repair was committed separately at
`9c877fbd735537e76f407826d04bad39371a6089`.

The `*-final.*` files and EDN receipts are the authoritative final-tree gates.
The fresh JVM gate covers `futon2.aif.full-loop-runner-test`, including the
existing runner controls and the new retained-selection and preselection-
failure controls.  No serving JVM or production data root was contacted.

Source pins at the tested tree:

- `src/futon2/aif/full_loop_runner.clj`: `652b49e90fd70dd4671691c7dc99b13c82363f89b14ca46646a0f3c9da60dc6b`
- `test/futon2/aif/full_loop_runner_test.clj`: `bf2300024134aece6f579032686406de4b7749147f0486f834687a54cded0b0e`

