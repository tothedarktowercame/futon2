# Limb-evidence prompt update receipts

The prompt-only source and controls were committed first at
`9b2d905ffec7a68b96a266df62334a04b128db6c`. The source lint baseline and
post-change gate both report zero findings. The fresh-JVM runner tests use
isolated seams; no prompt was dispatched to a serving JVM and no production
evidence directory was written.

Source pins:

- `src/futon2/aif/full_loop_runner.clj`: `b3cd7743cfeb5ae5e0178740b4b85aeab014981577fe9f671d0ba73536a4e0d8`
- `test/futon2/aif/full_loop_runner_test.clj`: `bad7dc01fd109e90c940989c3cb50dc563ba3ebfac6106221029c6fdac7faaca`

