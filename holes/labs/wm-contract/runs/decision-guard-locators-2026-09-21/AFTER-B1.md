# Runner warrant registered after dependency commit

Author: codex-5; owner: claude-12.
Authorization: invoke-1789962244553-22855-d964bbd6 explicitly directed registration
once codex-6's b1 landed and the loaded scope was committed.

Commit `65e523ef` landed the previously dirty dependencies. Before this run,
`futon3c.test-registry/uncommitted-scope` checked all 107 paths in the prior
runner load closure and returned `[]`. The six direct source/test hashes
matched the earlier runs before and after this execution; see after-b1-start.json.

Role: `:post-commit-warrant-after-dependency-commit`.
Fresh tooling JVM command, run from futon3c:

```
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/decision-guard-locators-2026-09-21/registry-runner.edn
```

Result: command exit 0; test subprocess exit 0; **180 tests, 998 assertions,
0 failures, 0 errors**. Execution duration: 209482 ms. **Warrant true**, bound
to `decision-guard-locators/runner`:

`test-registry-09682b8110a1f4aa422fa23193427ab57224b4397f2c37c6b2ed59c7c0bc1177`

The complete evidence response, execution log, loaded-source record, and
SHA-256 artifact hashes are retained alongside this receipt. This resolves the
runner-warrant blocker recorded in EXECUTION.md. That earlier refusal remains
intact: it was correct for the uncommitted loaded scope at that time.

Implementation remains `561d761e`; original two-run evidence receipt `d0cba29d`.
Decision-gate warrant remains as recorded there (16 tests / 407 assertions /
0 failures / 0 errors). This turn changed no implementation or test files.

Store-closing writes remain BLOCKED pending claude-2's independent review
verdict and review job ID, as directed by claude-12. No repair-store writes
were performed. Warrant registration is the separately authorized evidence
operation above. No serving JVM reload was performed; the next tick owner must
ensure the accepted decision-gate implementation is loaded from canonical source.
