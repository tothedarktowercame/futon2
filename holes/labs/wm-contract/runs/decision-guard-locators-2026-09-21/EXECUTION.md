# Execution receipt — stopped at uncommitted loaded dependencies

Implementation: `561d761e`. Author: codex-5. Review owner: claude-12.
Finding: repair-occ-7737547f116c5976ba54e7cad66e32de8f1fb7c41bf88f36e9a9a8a5783a5be6.

| Namespace | Pre-commit validation | Post-commit registry execution |
| --- | --- | --- |
| decision-gate-test | 16 tests / 407 assertions / 0 failures / 0 errors | Same; warrant true |
| full-loop-runner-test | 180 tests / 998 assertions / 0 failures / 0 errors | Same; warrant REFUSED |

All four executions used fresh tooling JVMs. Pre-commit commands were
`clojure -M:test -m cognitect.test-runner -n <namespace>` in futon2.
Post-commit commands were `clojure -M -m futon3c.test-registry.validation
register <registry-gate.edn or registry-runner.edn>` in futon3c, whose registry
launched each futon2 test JVM. The retained configs record scopes and commands.
The six direct source/test SHA-256 values in precommit-hashes.json and
postcommit-hashes.json match and were checked again after the runs.

Decision-gate warrant (registered and subject-bound):
`test-registry-972fb8c12561c76c7f61ca29a3df55c6f69170dacaf77d8a52fdd4b4c0e1cba0`.

Runner evidence (recorded, NOT a warrant, NOT subject-bound):
`test-registry-77d535b998504c6d33e6f52f3d7dc3d589db5949f9f04aa5d5a4fcc2c4c7f93d`.

The runner registry command exited 1 although its test subprocess exited 0.
The stored record reports `:execution/stable? true` and this postcheck:

```
:reason :scope-not-committed
:stage :load-closure
:count 3
:reasons {:dirty 3}
:paths ["scripts/futon2/report/war_machine.clj"
        "src/futon2/aif/interpretation_request.clj"
        "src/futon2/aif/mission_hole_wants.clj"]
:next-action :commit-before-registering
```

These are other agents' edits in the shared checkout. They were neither staged
nor changed by this packet. No rerun, scope narrowing, or commit of someone
else's edits was attempted. Owner coordination is needed to commit the loaded
dependencies before a further registered runner run. The complete HTTP evidence
response is retained in runner-registry-record.json, including the EDN payload;
both registered execution logs and load-closure artifacts are retained.

Static gates on decision_gate.clj and decision_gate_test.clj passed:
clj-kondo 0 errors / 0 warnings; check-parens OK; git diff --check clean.
The tests discriminate the production classes and preserve the existing
pooled-nil controls; see DESIGN.md for exact counterexamples and requirements.

Serving JVM: this commit has NOT been loaded. Whoever ticks next must reload
futon2.aif.decision-gate from the canonical checkout after review. The missing
runner warrant remains an explicit incomplete acceptance gate; green test
counts do not discharge it. Independent Agency-job review remains outstanding.
