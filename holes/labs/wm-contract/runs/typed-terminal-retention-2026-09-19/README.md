# Typed close-terminal retention — 2026-09-19

Implementation: `06310fcb`.

## Evidence and minimal repair

Read before editing: `full_loop_runner.clj:3645-3707` (close fallback), `:4620-4708` (outer close containment), `:4784-4821` (initialization), and both full retained findings:

- `data/wm-repair-obligations/findings/repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-close-exception.edn`
- `data/wm-repair-obligations/findings/repair-initialization-56485d40-2f54-4882-8917-15a59a473494-initialization-failed.edn`

Both retain exactly `{:errors [[:missing-judgment-key :duration-ms] [:missing-judgment-key :resource-use]]}` and message `invalid close outcome`. This was a missing-field contract failure, not an unknown outcome keyword. Commit `2c90fa41` already supplied duration/resource fields to both fallback terminal builders. Those existing fixes were retained, not duplicated.

The remaining untyped producer was `full_loop_cohort.clj:551`. Its existing exception now includes `:failure-kind :invalid-close-outcome`, `:failure-stage :close`, and `:outcome :build-failed`, preserving the original errors. Existing runner fallback and initialization typing consume this without new close semantics, occurrence indexing, or weakening evidence validation.

## Adversarial registration

`close_terminal_retention_test.clj` constructs the historical bad cell by removing duration/resource fields from an otherwise successful production-contract-shaped close. The actual cohort writer refuses that cell; the real fallback accepts one subsequent typed terminal. Assertions pin two writer calls (one refused, one accepted), one durable closed event, one finding callback, the original errors in the terminal, and no generic initialization finding.

A separate initialization-boundary fixture verifies typed close data remains typed and that the identical message with empty ex-data still produces `:initialization-failed`. This second test proves typed recognition rather than a message-string escape hatch; it does not claim a failed initialization has an existing attempt to close. Namespace fixtures keep repair/trip/trace stores temporary and repair callbacks are isolated.

Registered once from futon3c:

```
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/typed-terminal-retention-2026-09-19/register.edn
```

evidence-id test-registry-8a13ae4b965011c4794f1b8f02ee0082490f68c008ee8b88048dc2a0b725e22d
warrant? true
results {:assertions 21, :duration-ms 4554, :errors 0, :exit 0, :failures 0, :tests 2}
bound wm-close/typed-terminal-retention -> test-registry-8a13ae4b965011c4794f1b8f02ee0082490f68c008ee8b88048dc2a0b725e22d

Gates: clj-kondo clean, check-parens OK. Adjacent files retain gate outputs, spec, CLI stdout/stderr/exit and runner log/closure.

No production repair-store mutation, implementation record, resolution, click, or shared-JVM reload. The only service writes are the requested registry evidence and subject binding. The correctly refused revision-unchanged finding was not modified. Independent review and any finding disposition remain for a later packet.
