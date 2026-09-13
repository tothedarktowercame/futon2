# Manifest audit review — codex-26, 2026-09-13

Reviewed adapter 6fecd35e/3c6a8c36 and receipts 867d48d1/57ff314a.
Adapter/test/reader and retained-output hashes match actual bytes, recorded in
lead-pins.json. Source inspection and retained gates only; no test rerun.

The no-argument production entrypoint resolves owned roots, strict UTF-8/EDN
parsing uses hashed bytes, all four directory contents enter the census and
before/after membership/hash comparison. Production output is correctly labelled
production-audit, excludes constructor-input, and refuses runtime qualification.
The retained output contains 273 manifest records; this is an audit capture,
not an accepted open-trip census. The test fixture's authority is test, so its
confidence 1 is a test-root exclusion, not production discharge evidence.
Retained tests report 2 tests/10 assertions; real parens and kondo gates retained.

Accept this bounded audit result, not a production snapshot adapter admission.
Next requires reader/writer coordination and a clearly stated capture boundary.
All independent writer implementations must participate; repeated enumeration
alone cannot establish a cross-store atomic read. A writer after a completed
snapshot is ordinary later state, so the contract should specify snapshot-time
consistency rather than impossible perpetual freshness.

Hardening owed with that packet: Files/readAllBytes and disappearing or changed
paths can throw raw IO errors; convert them to typed source failures with path
and phase. Root ancestry and allowed publication-lock entries must not permit
symlink authority escape. Enumerate all canonical writers, including scripts or
historical resolution paths, before claiming the coordination protocol complete.
Do not modify retained production data to turn a refusal into confidence.
