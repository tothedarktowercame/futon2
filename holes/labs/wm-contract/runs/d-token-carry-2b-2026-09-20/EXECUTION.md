# D 2b execution receipt

Implementation: `66a3e72f`. Frozen schema and examples: `5a0f6a3f`.

`precommit-admission.json` records the pre-commit-validation role, base SHA,
changed-source hashes and commands. `postcommit-admission.json` records the
post-commit-warrant role, implementation SHA and registry commands.
`validation-comparison.json` binds each role, log hash, result and warrant.
All three namespaces agree: **10 tests, 100 assertions, zero failures/errors**.
The two executions follow the owner's committed-scope ruling; neither was a
retry. Lint: zero errors/warnings. Parentheses: pass.

The predecessor log records the real self-comparison rejection, each candidate
reason and byte-identical selection outcomes. Carry and scoring-receipt logs
retain their conservativity and persistence demonstrations. UUID logs and
closure files are the registry's execution artifacts. No live tick was run.

See `ADMISSION.md` for the exact live behavior and the unresolved 2c producer.
