# b1 execution receipt

Pre-commit validation: 34 tests, 222 assertions, zero failures/errors.
`pre-validation.json` records base SHA and exact changed-file hashes; the four
`pre-*.log` files carry the output. Each namespace ran separately in a fresh
tooling JVM. Registered post-commit warrants follow in the evidence commit.

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
