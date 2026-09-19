# APPROVED — bounded occurrence-identity reconciliation

Reviewer: codex-1; implementation author: codex-24.
Review job: `invoke-1789848960595-22517-5965a733`.
Reviewed implementation: `6b10019f247d56db68ce2d6b14f80a401e5bb052`.
Reviewed receipt update: `6a1252d28ed601b90c1f65e4b5371bf07fa22509`.
Prior review: `0fcb8db559169bd32a8889a8fab8f369b8d19886`.

The reconciliation resolves the three findings in the prior review. This is
approval of the bounded implementation, not authorization to dispose of old
findings or change tripwire witness identity.

## Independent source and test review

1. **Retry timestamps no longer change occurrence identity payload.**
   `repair_obligation.clj:415–435` now returns the timestamp-free occurrence
   tuple and digest. Publication at lines 384–396 compares occurrence records
   modulo only top-level `:opened-at`, retaining the first file rather than
   rewriting it. Both writers read back the published finding (lines 539–541
   and 586–588), so callers receive the original opened-at too. This is the
   explicitly requested separation of observation time from immutable finding
   content, not acceptance of changes to error/target/failure semantics.
   The existing changed-error control still requires a conflict.

2. **The publication boundary enforces derived identity.**
   `validate-occurrence!` at `repair_obligation.clj:440–457` recomputes the
   digest, requires the exact occurrence keys/value, checks the typed failure,
   and refuses a contradictory caller repair ID. Both publication APIs invoke
   it before writing; the system writer prioritizes the derived ID at
   lines 559–564. The new real-writer negative control supplies both a caller
   alias and fabricated occurrence digest and proves zero finding files were
   published. The no-occurrence legacy path remains available; no existing
   finding migration was introduced.

3. **Runner observations are supplied and the real retry path is covered.**
   `full_loop_runner.clj:3400–3404` constructs boundary/opportunity-qualified
   observation IDs, supplied at close-core, close-fallback, recovery,
   outer-close, and review publication sites. Initialization uses its catch
   invocation timestamp for a separate observation ID at lines 4908–4912.
   `initialization-retry-reuses-real-finding-and-appends-observation` in
   `full_loop_runner_test.clj:4017` routes the runner's writer callback into
   the actual repair writer at a temporary root. Two calls separated in time
   assert the same finding ID and opened-at, original bytes unchanged, exactly
   one finding and two observation files. The injected phase failure and
   disabled queue are fixture boundaries; the dependency whose replay failed
   previously is no longer stubbed. The store-level inner/outer observation
   control remains, as do parallel publication and distinct-job T8 controls.

The real runner integration control covers initialization retry specifically;
it is not an executed test of every close/recovery branch. Those additional
observation call sites were inspected in the source diff. No broader coverage
claim or live-run validation is made here.

## Current warrant checked through the endpoint

Executed POST `http://localhost:7070/api/alpha/test-registry/check` with:

- entry ID `test-registry-28846bdeeec462f7c7dd001c6bda3ed6e2f7b770fc2c7df723934c5a34814813`;
- repo root `/home/joe/code/futon2`;
- all five changed source/test paths as `changed-paths`.

At `2026-09-19T20:16:25.187864601Z`, the endpoint returned `warrant? true`,
chain length 2, `outside-closure []`; results: **7 tests, 26 assertions,
0 failures, 0 errors, exit 0, 4740 ms**. The aggregate wrapper contributes one
test to that count. This is a fresh validity check over the current scope,
not merely the receipt's mint-time success claim. No implementation/test
files were dirty when checked.

I inspected the source/test diff and receipt rather than relying on test
counts for adequacy. The warrant was consumed without rerunning its namespace.
Only this re-review note was written and committed. No repair-store mutation,
click, source edit, JVM reload, or restart was performed.
