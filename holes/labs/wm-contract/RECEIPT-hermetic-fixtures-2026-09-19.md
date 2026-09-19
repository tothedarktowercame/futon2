# Receipt: hermetic repair fixtures — 2026-09-19

Fix commit: `6cdb308a`. Registered subject: `repair-store/hermetic-fixtures`.

## Writer location and historical evidence

The leaked fixture shape is `test/futon2/aif/full_loop_runner_test.clj:4633-4654` (`retention-success-opts`): author job `retention-author`, artifact `abc123`, target/build repository the temporary cohort root. It inherits `synthetic-artifact-binding` at `:513-524`, which claims `/repo`. This deliberately mismatched shape reaches the real runner's artifact-binding refusal and close writer (`src/futon2/aif/full_loop_runner.clj:3408-3433`, default `repair/record-system-failure!`). Before this fix, `isolated-runner-opts` had no temporary repair callbacks. The helpers were introduced in `2b1f7af3` (2026-09-14 16:21:58 UTC).

The two original findings show September 14 opened times 23:06:23.128592463 and 23:12:04.888202123 UTC. Their retained `:failure-data` includes the synthetic author/binding. Exact files:

- `data/wm-repair-obligations/findings/repair-ea1-a7a5fc7c81ad45251922d33718170eba32a100cda770df0af9bcd759e28df913--attempt-001-artifact-binding-mismatch.edn:309,327,442`: `/tmp/debug-standing-readback13347244878016165142`.
- `data/wm-repair-obligations/findings/repair-ea1-b28b40fe3c109454107faa2309cfcf0e51abf79e39c436fda76a2d7d665f1913--attempt-001-artifact-binding-mismatch.edn:308,325,439`: `/tmp/debug-baseline6082102988716713796`; author/binding at :408-439.

Existing namespace fixture isolation predates those records: `697bb450` added `hermetic/with-hermetic-stores`, which rebinds the defaults during clojure.test execution (`test/futon2/aif/hermetic_repair_fixture.clj:26-40`; runner suite `:41`). Direct helper calls do not run namespace fixtures. The observed debug prefixes and exact helper payload support a direct-helper/debug invocation bypassing that isolation. The original debug command/script was **not located** in test/scripts or surviving top-level /tmp Clojure scripts; this receipt does not invent its filename or author.

`eec07f38` (September 14 23:41:09) added the standing-readback test and documented the stock `/repo` versus temporary-root mismatch. That commit postdates both leaked records, corroborating the diagnostic workflow, not proving the exact original invocation. The reusable helper is now protected regardless of whether invoked by clojure.test or a debug REPL.

## Fix and adversarial execution

`hermetic_repair_fixture.clj:42-55` now creates a temporary store and supplies explicit-root callbacks for system/review writes, reads, implementation, resolution, supersession and tripwire findings. `full_loop_runner_test.clj:548-550` merges these into the reusable isolated options. Existing suite fixtures remain additional protection. No store contract was weakened and no production source changed. This repairs the located reusable caller; arbitrary external debug code that deliberately removes the callbacks is not certified safe.

`test/futon2/aif/hermetic_retention_test.clj:13` directly calls the offending helper shape for both original debug prefixes with the production repair default unchanged and without the namespace store-rebinding fixture. Both runs retain the artifact-binding refusal and `retention-author` finding in explicit temporary stores. The test snapshots the real production repair file set before/after and asserts equality. Trace/run-record stores are isolated separately. Thus the test proves the actual refusal writer is exercised, rather than only proving no writer ran.

Registered exactly once from futon3c:

```
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/hermetic-fixtures-2026-09-19/register.edn
```

evidence-id test-registry-15940d9edb71eff4d10a9a103d5bd5071a5e8e0f6e9e9d58d4df83ef3560deaf
warrant? true
results {:assertions 11, :duration-ms 4863, :errors 0, :exit 0, :failures 0, :tests 1}
bound repair-store/hermetic-fixtures -> test-registry-15940d9edb71eff4d10a9a103d5bd5071a5e8e0f6e9e9d58d4df83ef3560deaf

Gates: clj-kondo clean, check-parens OK. Execution log, closure, spec, CLI stdout/stderr/exit and gate outputs are in `runs/hermetic-fixtures-2026-09-19/`.

## Store transition: refused, not fabricated

The requested two `record-implementation!` calls were attempted with the fix commit and warrant reference. Both refused BEFORE publication. The implementation gate requires independent executed review evidence and matching observed artifact binding for schema-3 findings (`repair_obligation.clj:112-135,974-1035`), not merely a successful test warrant. This packet has no independent reviewer/job, approved review receipt, or production grounding witness. Moreover the leaked records retain the synthetic `:machine-repo "/futon2"`; supplying a made-up binding to satisfy that field would be dishonest. No fake reviewer, binding, or resolved/dial-moved witness was supplied.

Verbatim result (full exception data retained in adjacent `*.refusal.edn` files):

```clojure
:before-eligible 9
:finding "repair-ea1-a7a5fc7c81ad45251922d33718170eba32a100cda770df0af9bcd759e28df913--attempt-001-artifact-binding-mismatch" :message "Machine repair implementation lacks grounded review evidence" :refusal {:outcome :incomplete, :failure-kind :machine-repair-lacks-grounded-review-evidence, :failure-stage :stop-line-resolution, :failure-detail [:reviewer-missing :review-job-missing :grounded-review-evidence-invalid :witness-not-resolved :witness-dial-not-moved]}
:finding "repair-ea1-b28b40fe3c109454107faa2309cfcf0e51abf79e39c436fda76a2d7d665f1913--attempt-001-artifact-binding-mismatch" :message "Machine repair implementation lacks grounded review evidence" :refusal {:outcome :incomplete, :failure-kind :machine-repair-lacks-grounded-review-evidence, :failure-stage :stop-line-resolution, :failure-detail [:reviewer-missing :review-job-missing :grounded-review-evidence-invalid :witness-not-resolved :witness-dial-not-moved]}
:after-eligible 9
```

**Runner-eligible count: 9 → 9, not 9 → 7.** Both findings remain open; no implementation record paths exist from these attempts. Expected destinations, once valid review/grounding evidence is available, are `data/wm-repair-obligations/implementations/<same finding id>.edn`. The two requested findings are the only attempted targets; no other findings were modified.

`resolve!` was **not attempted**: the user required an honest production-shaped successor, and this packet has neither an accepted implementation nor such a successor (`repair_obligation.clj:1073-1106`). Isolated fixture replay is evidence of the cause repair, not production-shaped validation. This is a remaining contract/evidence blocker before even :awaiting-validation, rather than a successful queue drain. Independent review and a truthful way to bind this fixture-origin cause under the schema-3 artifact contract are the next step; no exception or administrative workaround was introduced.
