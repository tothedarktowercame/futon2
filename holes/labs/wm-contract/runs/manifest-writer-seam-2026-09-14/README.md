# Evidence-manifest cohort-writer seam receipts

The source and controls were committed first at
`3b46255391a720528924bdb26b01333f584a9fce`. The baseline and final
cohort-source lint gates both report zero errors and zero warnings. The fresh
JVM gate covers the complete `futon2.aif.full-loop-cohort-test` namespace.
All writer controls use temporary data roots; no production record or serving
JVM was touched.

Source pins at the tested tree:

- `src/futon2/aif/full_loop_cohort.clj`: `9e19b4bcc0fc2e995fdfa44d28f4176146bf87b7d4644ba285e83ace9b72361e`
- `test/futon2/aif/full_loop_cohort_test.clj`: `edc811738c3dc4e580a29e844772831b90d4a1e907f34814c92ffe5b41810a0d`

