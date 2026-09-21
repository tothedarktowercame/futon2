# b1 execution receipt

Pre-commit validation: 34 tests, 222 assertions, zero failures/errors.
`pre-validation.json` records base SHA and exact changed-file hashes; the four
`pre-*.log` files carry the output. Each namespace ran separately in a fresh
tooling JVM. Registered post-commit warrants are retained in this evidence commit.

| Namespace | Tests | Assertions |
|---|---:|---:|
| cascade-proposals-test | 6 | 36 |
| interpretation-request-test | 6 | 60 |
| mission-hole-wants-test | 9 | 35 |
| cascade-decision-test | 13 | 91 |

clj-kondo: zero errors/warnings (one existing informational str diagnostic).
check-parens: OK for all five changed Clojure files. git diff --check: clean.

Exploratory executions are not warrants: the initial four proposal tests
passed 4/23 before integration controls were added. One invocation omitted
`-m cognitect.test-runner` and exited before loading tests (`-n` was treated as
a filename); the corrected command and final validation logs are explicit.

Real retrieval demonstration: M-a-wmc-scaling generated 48 distinct pinned
proposals, all `:retrieval-proposed` / `:proposed`; no declines and no agent
admission. Read-back reproduced all 48. It retained 1,393 sources / 21,447,163
bytes in the canonical local proposal store. The summary and bundle byte hash
are recorded alongside; snapshots remain at the named local path, not in Git.
This exercised both production retriever subprocesses, not a stub. Scores are
not relevance decisions. No statement that any of the 48 applies is made.

Code is not loaded into the serving JVM. No author action was dispatched.

## Registered committed-byte validation

Implementation: `65e523ef`. All four registered runs passed at
`d0cba29d1400d1287420dbe6009710b3750e490f` (the intervening commit added another
lane's receipts). Each run agrees with pre-commit validation: total 34 tests,
222 assertions, zero failures/errors. `validation-comparison.json` retains
both execution roles and SHAs. An audit compared each changed file's committed
bytes at the registered SHA against the pre-validation hash: all match.

The four `warrant-*.json` files retain the evidence-store envelopes. Their
payload hashes were independently checked on retrieval. `register-*.log`
contains warrant IDs and subject bindings; `registry-*.edn` reproduces each
registration command; `post-*` retains the logs and dependency closures.

- Proposals: `test-registry-f02006ccbe1e3b9dae242dceeac76bb4536cf53a6764febffe5a9ecdcd0d5c12`
- Requests: `test-registry-975f71c580e32b6f8d3758034885dc4156296947b6f9c0ac0c7e63e50cf15ccd`
- Wants: `test-registry-05a32cf9755f69ae88e8c323aa06b4cad8650836a17561c87cdba1901ab65139`
- Decision: `test-registry-5d2e14d6696365f13ac1dfadcf82bf0fa866ce881824e46212954f750778cc3b`
