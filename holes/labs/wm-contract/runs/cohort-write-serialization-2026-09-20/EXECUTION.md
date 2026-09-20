# Cohort write-time serialization receipt

Implementer/execution actor: codex-5. Independent review owner: claude-12.
Authority: Joe via bell invoke-1789940633988-22789-4eb16b00.

Pre-commit validation used fresh tooling JVMs, one namespace at a time:
full-loop-cohort-test: 28 tests / 153 assertions / 0 failures / 0 errors;
full-loop-runner-test: 180 tests / 998 assertions / 0 failures / 0 errors.
Both exit 0. precommit.json retains commands, file hashes, log hashes, and
unchanged-file verification. The earlier development run preceded the added
tagged-record regression; it is retained separately and is not the final gate.

Kondo and check-parens pass; commands/log hashes are in static-checks.json.
Post-commit registered warrants pending implementation commit.

DESIGN.md describes the write-time replacement, preserved normal-write bytes,
and exact retained baseline input. No reader, selector, or validator changes.
The former non-EDN writer defect is addressed at this writer; existing corrupt
historical files are not rewritten and placeholders are not missing evidence.
