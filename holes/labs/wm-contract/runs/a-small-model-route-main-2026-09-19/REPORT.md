# A-small-model-route — merged-main acceptance

Merge: **914c5d3dd3853922f14e3be64de28c8440cc1341**, on canonical futon2 main.
Rebased `codex-3/a-small-model-route` onto `138b435d`, then merged with
`--no-ff`. No conflicts or implementation edits were needed.

Rebase rewrote reviewed implementation `0d32c6e0` as **0093adcb** and its
evidence commit `76c5b594` as `22d6f528`. `0093adcb` is an ancestor of main.
All six implementation/test files, including the complete efe.clj file, are
byte-identical between the reviewed `0d32c6e0` and the merge. The original
pre-rebase SHA itself is not an ancestor; its unchanged content is now on main.

**No reviewed number moved.** The six `JOINT-CONTROL`, `PRODUCING-CASCADE`
and `PRODUCTION-F` records in the merged run are byte-identical to those in
the reviewed registered run. The comparison is retained in
`number-comparison.json`.

* Coupled producing-cascade F = **5.413103575948838**, consumed unchanged by
  the production selector.
* Coupled pair-missed probability = **13/340**, independent = **1/100**.
* Both single-token miss probabilities remain **1/10** (assertions unchanged
  and passing).
* The joint no-report probability ratio remains **39150625/83521**, about 469.
* The cancellation and changed-posterior tests both pass.

Fresh warrant:
**test-registry-283645f873127df8b3d7393c8c713291b6d27674974c47ad9009a5d63e026899**.

Registered and bound to **A-small-model-route** by codex-3, with
`:repo-root "/home/joe/code/futon2"`, main at the merge commit above.
The registered execution itself is the requested merged-state namespace
rerun: **7 tests, 132 assertions, 0 failures, 0 errors**, 3608 ms.
Only `futon2.aif.observation-model-route-test` ran.

`check.json` records the subsequent HTTP warrant check: `warrant? true`,
canonical root, and the merged git-head. `binding.edn` copies the actual
appended binding; `registry.edn`, `registration.txt`, and the runner's `.log`
and `.closure.edn` retain the execution evidence. No JVM was launched to
consume a warrant.

Merged-state gates: **clj-kondo 0 errors/0 warnings**, **check-parens OK**.
Outputs are adjacent. No reviewed assertion or expected value was adjusted.

Other agents' existing modifications to workflow-report.edn and
repair_obligation.clj were preserved and not staged. No click, serving-JVM
reload, activation, or manual change under futon2/data was performed.
