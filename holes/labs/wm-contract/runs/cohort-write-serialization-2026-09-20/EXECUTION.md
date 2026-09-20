# Cohort write-time serialization receipt

Implementation: `10d3dc9087de835c366add77141d6aa64f95a969`.
Implementer/execution actor: codex-5. Independent review owner: claude-12;
implementation review remains the owner's responsibility.
Authority: Joe via bell `invoke-1789940633988-22789-4eb16b00`.

| Namespace | Pre-commit validation | Post-commit registered run |
| --- | --- | --- |
| full-loop-cohort-test | 28 tests / 153 assertions / 0 failures / 0 errors | Same; warrant true |
| full-loop-runner-test | 180 tests / 998 assertions / 0 failures / 0 errors | Same; warrant true |

Cohort warrant: `test-registry-2e4f5347a6d969d8d98125ed34ba1cb996da073611c58ca1d7362f701c29d59e`.
Runner warrant: `test-registry-0c2d84a3fd1ecffa1dd3a8cd20fbde2f0e173495098e170fb05b8b9aa16212f3`.
Both were registered and bound to the subjects in the retained registry specs.

Each execution used a fresh tooling JVM against canonical futon2, one namespace
at a time. The registry utility ran from futon3c and launched the declared futon2
namespace; no live JVM was loaded or restarted. All four recorded source/test
hashes match across the two roles and remained unchanged during execution.
`validation-comparison.json` retains both roles, commands, implementation SHA,
source/test hashes, actual counts, statuses, and registry/test log hashes.
Loaded-closure artifacts are retained for both registered runs. The baseline
bad event is also declared explicitly in the cohort warrant's test scope.

Kondo and check-parens pass; commands and log hashes are in static-checks.json.
No tests were skipped, no acceptance threshold was weakened, and no commit was
amended. The development cohort run preceded the added tagged-record regression
and is retained separately; it is not substituted for the final precommit gate.

## Acceptance evidence

The test verifies the retained baseline file SHA-256
`850cb9737d9c22ad6d5b35e0e987c8fe27ba2e664a7bd127aaf90026292ec536`,
reconstructs its runtime argument layout with actual functions/atoms, and proves
its emitted string still fails the production EDN reader before writing. The
fixed writer emits a typed placeholder at the dispatch response while retaining
event identity and readable siblings. The unchanged production reader then reads
it successfully. Public checkpoint append, subsequent writes, close, and ledger
read all complete through the real cohort store. No production custom reader
or stubbed writer is used.

Normal EDN is compared byte-for-byte against the previous UTF-8 writer formula.
Separate negative controls cover functions, atoms, plain objects, unreadable map
keys, and a tagged record with readable fields; each becomes a typed placeholder.
A valid sibling remains unchanged and diagnostic summaries remain bounded and
sanitized. The runner namespace, including the historical fixture family,
retains exactly 180 tests / 998 assertions / zero failures or errors.

## Scope of resolution

The writer now tests its emitted string with `clojure.edn/read-string` and
records unreadable values with `:payload/status :non-edn-payload`, reason
`:not-round-trippable`, and a sanitized prefix bounded to 256 characters.
Valid writes reuse their original `pr-str` string. All sibling calls through
`write-new!` use this behavior. Event metadata/readable map fields are preserved;
keys that cannot be serialized replace the containing map rather than collide.
No reader, checkpoint validator, selector, or runner source was changed.

This resolves the specific write-time boundary finding documented in the fixture
migration report. It does not rewrite already-corrupt historical files, modify
other writers, or treat the placeholder as the missing evidence. Filesystem
failures retain their existing behavior. DESIGN.md records the implementation
choices and the retained baseline artifact path.
