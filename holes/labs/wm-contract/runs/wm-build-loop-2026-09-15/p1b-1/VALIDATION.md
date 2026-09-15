# P1b-1 validation and integration handoff

Author: codex-3. Independent reviewer: claude-2.
Commission: invoke-1789502848282-21230-6d90b432.

## Delivered behavior

`futon2.aif.work-target-store` provides explicit create-once initialization,
HEAD-authoritative strict chain reads, predecessor-checked durable commits,
operation-intent idempotency, historical ancestor resolution and explicit
incomplete/damaged/pending states. It retains literal declaration bytes.
Temporary files and prepared snapshots cannot advance the head. No committed
snapshot is overwritten. No recovery mutation command is supplied: recovery
requires the explicit evidence-preserving procedure in PROTOCOL.md and a
separately reviewed repair commission.

## API for P1b-2

```clojure
(def s (store/open-store directory {:payload-validator lineage-validator}))
(def current (store/read-store s))
;; Only proceed for :established-no-snapshots or :committed.
(store/commit! s (:head current)
               {:id operation-id
                :kind :carry-and-introduce
                :caller-identity-type :agency-session
                :information-cutoff cutoff}
               payload)
;; Success carries :ref {:store/id <uuid> :seq n :sha256 <hex>}.
(store/resolve-reference s historical-ref)
```

The caller supplies the stable operation ID, original expected-head and exact
intent on retry. A retry can return an older committed reference after later
commits. `resolve-reference` accepts that ancestor. It refuses an ahead-of-head
reference; broken underlying authority returns its damage/recovery status.
Payload-validator takes one payload map and returns :ok or a typed refusal;
it runs for every committed snapshot on read and for every new proposed payload.
Genesis does not invoke a payload validator because it has no model payload.
`initialize!` is a separate explicitly authorized rollout operation. No API
conflict identified; P1b-2 must use the above handle and operation envelope.

## Gates and scope

All required gates exited 0. Test namespace: **15 tests, 276 assertions**, zero
failures/errors. `gate-receipts.json` retains each direct argv, exit status,
elapsed time, cwd, and tested source hashes. `*.stdout` and `*.stderr` retain
unabridged streams. `run-gates.py` runs commands directly without shell pipelines.
The filesystem probe is also linted and checked for parentheses.

| Test | Kind of evidence |
|---|---|
| lifecycle-and-create-once | Real filesystem: absent, established empty, valid empty payload; exact/different genesis retries. |
| incomplete-initialization-and-missing-head | Injected initialization interruption; real file deletions simulate completed-head damage and ambiguous initialization. |
| chain-damage-controls | Real file edits/deletions simulate duplicate/non-contiguous sequences, changed bytes, middle gaps, missing tail/head, wrong genesis/store/declaration, multiple forms and parse failure. |
| threads-share-one-writer-lock | Real concurrent threads, canonical-path aliases and FileLock: one commit, one stale predecessor. |
| lost-response-and-operation-intent | Injected response loss after durable commit; real retry after another commit; changed payload/cutoff refuses. |
| interrupted-commit-boundaries | Injected exception at every matrix boundary; real read verifies pending/committed state and immutable acknowledged bytes. No power-loss experiment. |
| reference-ancestry | Real committed old ancestor accepted; off-chain/ahead/missing identity refused; deletion simulation returns damage. |
| validators-and-declaration-admission | Real writes with injected rejecting payload-validator; bad declaration hash creates no store. |
| orphan-artifacts-never-reset | Real genesis deletion simulates data loss; surviving snapshots force pending recovery. |
| real-force-order-and-simulated-io-failures | Instrumented real file-force, atomic moves, directory-force in order. Injected exceptions simulate ENOSPC, unsupported atomic move and directory fsync failure. |
| production-shaped-opaque-payload | Real round trip of the P1b packet/declaration shape, including seven rational masses, lineage and excluded candidates. Fixture only; no production acceptance claim. |
| initialization-interruption-controls | Injected interruption at 13 initialization boundaries; reads/retries never reset or implicitly finish initialization. |
| retained-declaration-and-strict-extras | Real deletion of original declaration proves retained bytes suffice; extra multi-form file is rejected; deleted snapshots directory is damage. |
| envelope-validation-after-consistent-rehash | Simulated malicious envelope edits with recomputed byte hash test semantic validation separately from hash checks. |
| intent-is-independent-of-caller-print-settings | Real writes/retries under different Clojure printer bindings retain identical intent/reference. |

The real IO event sequence is in tests.stdout. filesystem.stdout and the
filesystem-receipt.txt record the actual JDK/filesystem probe. The installed
bytecode extract in jdk-atomic-move-receipt.txt shows the atomic branch calling
UnixNativeDispatcher.rename and refusing EXDEV; no non-atomic fallback is used.
There is no physical power-failure test or external monotonic anchor.

Only temporary store directories were initialized; fixture directories are
removed after each test. The production declaration was read and SHA-verified.
No production genesis, shared-JVM load, runtime integration, click or scheduler
activation was performed. Files belonging to codex-2 were not changed.
