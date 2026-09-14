# Evidence-manifest runner seam receipts

The runner implementation and controls were committed first at
`c2068fdf72790a6ac3d871787b069d7633d74b13`. The initial static gates passed,
but the fresh JVM run retained in `tests.out` failed one of 812 assertions:
the outer runner boundary converted the commissioned missing-sibling refusal
into an initialization result. The narrow typed-refusal passthrough was
committed at `09cc0d5bda46fcb06b71c243f89e64aba7700cab`.

Files named `*-final.*` and the EDN receipts are the authoritative final-tree
gates. The test namespace uses isolated temporary cohort roots. No production
record or serving JVM was touched.

Final source pins:

- `src/futon2/aif/full_loop_runner.clj`: `d84faf7bd6eac4219a4b67f6f89de59a9a2dc9324a7f55db90db2688aa7440f2`
- `test/futon2/aif/full_loop_runner_test.clj`: `356c6fb688b5023892a7eb0726feeb0432ee9622519ead65ff829e18988ef229`

