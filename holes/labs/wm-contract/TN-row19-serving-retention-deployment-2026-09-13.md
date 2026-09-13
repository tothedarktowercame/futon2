# Row 19: selective serving-retention deployment procedure

Status: reviewable procedure only. No live evaluation, reload, restart, job creation, or hot/archive write was performed while preparing this note.

## Exact authority and present state

The deployment source is `futon3c` branch `master`, commit `0d40a359` (an ancestor of current `futon3c/master` `7b591a4e71e54dcb2abdf88a4989d8a9481264fc`). `src/futon3c/transport/http.clj` is byte-identical at both revisions, SHA-256 `defb1ed3b0deeb16ba15857f8efe5ac4e53ed6c442723dfae8808b38406f65fb`. The accepted retention lineage is `5fcf9912`, `002c6d27`, `009cb3fb`, `0d40a359`; lead acceptance of the final retry hardening is recorded separately. The evidence/draft repository is `futon2` branch `main`; branch names must never be compared without their repository.

The serving JVM PID observed during discovery was `1942869`, started 2026-09-11, before this lineage. Source availability therefore does not establish loaded identity. Its environment exposes neither `FUTON3C_INVOKE_JOBS_FILE` nor `FUTON3C_INVOKE_COMMISSION_ARCHIVE_DIR`, so the source defaults are `/tmp/futon3c-invoke-jobs.edn` and `/tmp/futon3c-invoke-jobs.edn.commissions` (`http.clj:355-358,436-438`). The current hot file exists; the archive directory did not. The actual job `invoke-1789267707053-20588-bc3b4e37` proves the running creation path is old: it has a request digest but no `:request-commission`. That absence is permanent and must not be backfilled.

## Side-effect census

Requiring or loading the entire namespace is not a narrow retention operation. Top-level evaluation registers a Cheshire `Instant` encoder (`http.clj:131-136`) and redefines roughly 9,000 lines of routes and handlers. `defonce` preserves the executor, hot-ledger atom, active index, sweepers, worker registry, and installed handler/config atoms (`http.clj:149,255-263,1348,2087-2222,8892-8905`), but every ordinary `defn` is replaced. Existing handler closures may call replaced Vars while other definitions are only partly loaded. A broad `require :reload` is therefore rejected for this deployment.

Merely loading the selected definitions must not call `ensure-invoke-jobs-ledger!`: initialization reads the store, recovers in-flight states, runs D13 compaction, archives expired records, and persists the resulting ledger when the store already exists (`http.clj:724-749`). Every normal update also composes the update with compaction and persists under the writer lock (`http.clj:751-787`). `invoke-job-request-commission` itself calls `ensure-invoke-jobs-ledger!` (`http.clj:1452-1498`), so it is a mutating read when the atom is nil. These calls are forbidden during preflight.

The archive path is a separate keyed directory, not a hot-ledger field (`http.clj:420-485`). Publication fsyncs the file and directory hierarchy; idempotent retries repeat the directory barrier; conflicts and corrupt digests refuse (`http.clj:487-539`). D13 archives every to-be-dropped job before removal and retains the existing seven-day expiry/sentinel behavior (`http.clj:541-585`). Creation refuses reuse of an archived requested ID and stores the exact normalized commission before event trimming (`http.clj:1436-1449,1500-1555`).

## Preconditions

1. Independently confirm `futon3c/master` contains `0d40a359`, the HTTP file is clean, and its SHA is exactly the value above. Refuse on any mismatch.
2. Confirm the live process still uses the two default paths from its actual environment. Do not infer these from the operator shell alone.
3. Hold all Agency invoke queues at their existing queue-control boundary, reject new bell/whistle/invoke acceptance, and wait until the active invoke count and worker registry are both zero. In-flight jobs must finish and persist normally; none may be cancelled for deployment.
4. Under the existing private `invoke-jobs-writer-lock`, copy the hot ledger to a new timestamped, read-only backup using open/read/fsync/atomic publication and record its SHA-256, size, owner, mode, and timestamp. If an archive directory exists by execution time, copy it append-only with a manifest; never replace or delete it.
5. Parse the backup, validate the ledger with the production validator, census jobs/order/trace index, and record all jobs with `:request-commission`. Refuse if the live file changes between the quiescence check and lock acquisition.

## Scoped load

Use a reviewed selective-form loader, not `load-file` and not `require :reload`. The loader must read the pinned HTTP source once, verify its whole-file SHA, parse top-level forms, and refuse duplicate/missing allowlisted definitions. While holding `invoke-jobs-writer-lock`, it evaluates only these exact forms in `futon3c.transport.http`:

`compact-invoke-job`, `request-commission-archive-record`, `invoke-commission-archive-dir`, `commission-archive-path`, `validate-commission-archive!`, `read-commission-archive`, `force-directory!`, `force-commission-archive-directories!`, `*force-commission-archive-directories!*`, `persist-commission-archive!`, `*persist-commission-archive!*`, `archive-expired-jobs!`, `compact-invoke-jobs-ledger`, `normalized-invoke-commission`, `invoke-job-request-digest`, `invoke-job-request-commission`, `create-invoke-job!`, and `bind-unbound-invoke-request!`.

The Java imports and namespace aliases used by these forms already exist in the pre-lineage source (verified at `5fcf9912^`), but the loader must check each required class/alias before evaluation. Capture every prior Var root before changing any. Evaluate dependency order as listed, with no calls to the functions. Verify each new Var's form digest and source line against the pinned source before releasing the lock. If any form fails, restore all captured roots while still holding the lock and keep invoke intake held. This selective loader is itself a deployment artifact requiring independent review before execution; hand-entered `eval` is forbidden.

Because job creation and ledger update paths dereference Vars, later requests reach the replaced `create-invoke-job!`, normalizer, compactor, and archive functions without rebuilding the Ring handler. No handler reconfiguration is required.

## Post-load serving verification

Only after the loaded-form receipt passes may intake be opened for one dedicated non-R9 retention probe:

1. Submit through the real invoke boundary with a unique job ID, agent, caller, surface, model, and a nonce-bearing bounded prompt. Capture the submitted normalized values independently.
2. Before event trimming, read the hot record under the writer lock without resetting the atom. Assert byte/value equality of `:request-commission` to `normalized-invoke-commission`, recompute its digest, and assert its job/agent/caller/surface/trace join. The probe is not an R9 author or reviewer commission.
3. Call `invoke-job-request-commission` only after confirming the atom is already initialized. Assert source `:hot-ledger` and exact envelope equality.
4. After the probe terminates, construct its archive record from that exact hot job and publish it through `persist-commission-archive!` without dropping the hot record. Read it back, assert archive/content digests and complete immutable join, then call the public read API and require hot/archive agreement. This verifies separate storage without advancing the global clock or compacting unrelated jobs.
5. Perform one ordinary unrelated hot-ledger update and prove the hot file contains no archive body/prompt duplication. Preserve the archive file. Do not simulate seven-day expiry in the live JVM; D13's already accepted isolated test is the evidence for archive-before-drop and bounded hot retention.
6. Release queues only after a second hot-file backup and archive manifest are durable. Then—and only then—commission a fresh real Row-19 author job. Its exact commission must be read back before commissioning the distinct reviewer.

## Failure and rollback

Before any post-load job is accepted, a selective-load failure may restore the captured Var roots under the lock. After any new commission or archive file exists, rollback to the old functions is prohibited: old compaction could discard the new preimage. Hold invoke intake, retain both hot backups and all archive files, and roll forward with the reviewed definitions or perform a controlled restart at the same pinned source. Never restore an older hot-ledger backup over a newer file, delete an archive, alter the D13 clock, or remove the seven-day policy.

Any archive publication/barrier failure must leave the hot record intact and intake held for idempotent retry. Any hot/archive disagreement, reused ID, digest mismatch, source-pin mismatch, in-flight job, or inability to prove loaded Var identity is a deployment refusal. A process restart is an alternative only after quiescence and durable backups; startup will run recovery/compaction, so archive directory writability and free space must be proven first.

## Review boundary

Root/lead must review the loader artifact, allowlist, lock discipline, backup destination, process environment, and probe commission before authorizing execution. This note authorizes no live action. The pending genesis draft remains in `futon2/main`; job 20588 remains permanently nonqualifying, and no author/reviewer/anchor/admission may be created from it.
