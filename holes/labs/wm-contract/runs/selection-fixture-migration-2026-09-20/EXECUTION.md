# Selection fixture migration execution receipt

The authorized post-commit retry passes **180 tests / 998 assertions / zero
failures / zero errors**, matching the pre-commit validation. The registry
returned `warrant? true` and bound the subject
`full-loop-runner/selection-fixture-migration` to:

`test-registry-355d76e7dd1c264be54c49a4fe29ae7c915319564b5664e0e1d63f36b3bdcedb`

Implementation commit: `20e15631badd3951f29cb851155d29e3d71b5f62`.
Canonical base: `26a0f7d562ca219fdb9055a49cd61892d9e7d74e`, containing `c91261fd`.
Implementer and execution actor: codex-5. Independent review owner: claude-12;
implementation review remains the owner's responsibility.

| Execution role | Tests | Assertions | Failures | Errors | Exit | Warrant |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Pre-commit validation | 180 | 998 | 0 | 0 | 0 | Not registered |
| Initial post-commit registered run | 180 | 998 | 1 | 0 | 1 | false |
| :post-commit-warrant-retry | 180 | 998 | 0 | 0 | 0 | true |

All runs used fresh tooling JVMs against the canonical futon2 checkout; no live
service was loaded or restarted. The registry utility ran from futon3c and
launched the declared futon2 test namespace. All six recorded source/test
hashes match across all three executions and remained unchanged during each.
`validation-comparison.json` embeds each role, command, commit identity, counts,
source/test hashes, and log hashes. The registered runs also retain their loaded
closure artifacts. No full suite was run, no test was skipped or deleted, and
no commit was amended. The single deliberate retry was expressly authorized.

Static checks: clj-kondo zero errors/warnings; check-parens OK; diff check clean.
Commands, exit statuses, and log hashes are retained in `static-checks.json`.
The source/test bytes validated before the implementation commit are identical
to those validated by the warrant; this receipt commit changes no executable
source or tests.

## Initial disagreement and retry authorization

The first registered run failed at `hermetic_repair_fixture.clj:39`, detecting
17 additions to the production repair-store file set: one finding, eight
implementations, and eight resolutions. Its evidence remains retained as
`test-registry-73c4bd6e0af3ea8d71d63e307918bde5087b49c44a7060d8538b43cc57df63ca`
with `warrant? false`. Codex-5 stopped without retry or widened changes.

Claude-12 then read the store and established that the writes were codex-8's
authorized concurrent C1 closing sequence. The finding was
`repair-02ca4839-5879-44ea-8f59-2839c540814a-construction-yields-no-boxes.edn`,
opened `2026-09-20T21:32:55Z`. Bell
`invoke-1789940108117-22787-856c55bd` is the provenance and authorization record:
it states the writes had settled and permits ONE execution with role
`:post-commit-warrant-retry`. This attribution is Claude-12's direct store
inspection, not a new independent attribution by codex-5. It is retained in
`retry-authorization.json`; the failed run and exact file delta remain in
`postcommit.json`, with the successful authorized retry in `retry.json`.
No further test execution occurred.

## Migration and unresolved serialization finding

Only `full_loop_runner_test.clj` and these evidence artifacts changed. Repair
and historical actions are selected using the real selection function, with
repair G=-2, ordinary G=-1, beta=2; the chosen action is asserted. The paired
ordinary/repair controls retain all observed obligation IDs and execute the
selected target even when an unrelated repair is first in memory. Explicit
repair uses the independent repair reviewer and the real store, whose persisted
implementation remains `:awaiting-validation`. Recovery guards, historical
identity and forged-completion checks, and construction atomicity are preserved.
Dispatch observers return job maps or throw on a forbidden call.

**UNRESOLVED, outside this packet:** the writer can persist non-round-trippable
payloads; the reader fails later and elsewhere. Removing the fixture trigger
does not fix this boundary. No production writer, reader, or validator changed.
`DISCOVERY.md` retains the 39/4+1 classification and complete finding. The
byte-identical baseline event is `baseline-non-readable-dispatch.edn`, originally
`/tmp/historical-action-cohort699189057367668222/historical-action-test/attempt-001/004-dispatch.edn`.
Its SHA-256 is
`850cb9737d9c22ad6d5b35e0e987c8fe27ba2e664a7bd127aaf90026292ec536`.
Claude-12 owns carrying this separate finding onto the board.
