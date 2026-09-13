# Row 22 E6b revisioned state/application protocol

Status: design only. No store was created or read, and no runtime path was
changed. Production outcome authority, external completeness acceptance,
writer participation, and the first-install fence remain absent.

## Existing boundaries and limits

`advance-slow-state` is pure and explicitly leaves persistence to an outer
caller (`src/futon2/aif/temporal_hierarchy.clj:190-203`). It updates one named
Beta entry and constructs the complete next slow state at lines 214-237. The
pure E6b verifier already derives and checks the full next record, stable
application identity, prior/next revisions and exact input/output digests
(`src/futon2/aif/machine_slow_feedback_evidence.clj:259-305`). It verifies a
ledger; it does not own one.

The prospective ingress controller demonstrates lifetime file ownership with
`FileChannel.tryLock` (`futon3c/agency/invoke_ingress_controller.clj:71-96`)
and force-temp, atomic-rename, force-directory persistence at lines 97-130.
That ownership covers only deferred resumes. Its accepted/executing/delivery
sets are atoms (`:181-201`) and therefore are not an E6b state store.

The lifecycle snapshot boundary excludes cooperative mutations while capturing
six providers, checks before/after revisions, and emits immutable bytes plus a
coverage subject (`invoke_lifecycle_snapshot.clj:71-132`). It poisons itself
after an uncertain mutation (`:71-90`). This is accepted only for isolated,
cooperating providers; it does not coordinate existing production writers.
The reconciliation reader demands a closed generation and exact source
digests, but production completeness refuses unconditionally
(`invoke_lifecycle_reconciliation.clj:82-142`).

The serving invoke ledger has a process-local writer lock and atomic durable
replacement (`transport/http.clj:255-261,587-610,751-785`). The commission
archive is immutable/create-only with directory forcing (`:505-537`). These
are useful mechanisms, not shared ownership: neither lock covers slow state,
and reusing either store would incorrectly couple E6b state to job retention.

## One compare-and-commit owner

One `SlowFeedbackStore` instance owns an OS lock for its whole lifetime. Its
configured root is not request data. All reads, compare-and-commit operations,
recovery and snapshot capture occur under the same in-process reentrant lock
and lifetime file lease, in this order:

1. acquire process lock;
2. acquire/confirm the lifetime file lease;
3. read the current HEAD and its immutable transaction bytes once;
4. validate schema, byte digest, generation, state revision and application
   index;
5. perform the pure computation outside no lock (callers may prepare a
   proposal), then reacquire both locks and repeat step 3;
6. compare the proposal's exact prior revision and prior transaction digest;
7. publish one transaction and advance HEAD as described below.

No other component may write these files. A second same-JVM or cross-process
owner refuses before reading mutable state. A stale prior refuses
`:e6b-store/stale-prior`; an existing application id with identical complete
transaction content is an idempotent readback; the same id with different
content refuses `:e6b-store/application-conflict`. A different application for
the same feedback event or prior revision also refuses.

## Reviewed on-disk representation

```
ROOT/
  owner.lock
  HEAD.edn
  transactions/<transaction-sha256>.edn
```

An immutable transaction has schema `:wm/e6b-state-transaction-v1` and exactly:

```
{:store/id string
 :generation nonnegative-integer
 :prior {:revision string :transaction-sha256 hex64 :state-sha256 hex64}
 :next  {:revision string :state <complete E6b next-state>
         :state-sha256 hex64}
 :application {:application/id string :feedback/event-id string
               :transition/subject <complete fixed subject>
               :input/digests <all resolved E6b inputs>
               :output/digest hex64 :status :committed}
 :authority {:verifier/source-sha256 hex64
             :evidence-source-sha256s {keyword hex64}}
 :committed-at RFC3339}
```

Its filename is the SHA-256 of the exact strict UTF-8 EDN bytes. Publication is
CREATE_NEW into `transactions/`, file sync, and transaction-directory sync.
Existing identical bytes are an idempotent retry; an existing name with
different bytes is a digest conflict.

HEAD is `:wm/e6b-state-head-v1` containing store id, generation, current state
revision, transaction digest, state digest, and the complete ordered
`application-index` of `{application/id, feedback/event-id, prior revision,
transaction digest}`. It is written to a same-directory temporary file, synced,
atomically renamed over HEAD and the root directory synced. State and
application become visible together because both live in the immutable
transaction selected by the single HEAD pointer. There is no state write
followed by a separate ledger write.

## Crash and retry rules

- Before transaction sync: no authoritative change; remove/ignore temp bytes.
- After transaction sync but before HEAD rename: the transaction is an orphan.
  Recovery validates it but does not apply it; an identical retry may reuse it.
- After HEAD rename but before root-directory sync: publication may have
  occurred but durability is unconfirmed. The caller receives a typed
  `:commit-outcome-unknown`; memory is discarded and recovery is mandatory.
- Recovery reads HEAD once, validates its digest-selected transaction, walks
  the prior digest chain, validates every state/application/revision link and
  rejects forks, missing parents, duplicate ids/events/prior revisions, or
  malformed/trailing/non-UTF-8 bytes. It never chooses the newest orphan.
- A retry under the stable application id returns the already committed exact
  transaction if HEAD contains it. It never applies the Beta update twice.
- Corruption or a failed callback poisons the owner. There is no caller-facing
  clear-poison operation; a separately reviewed reconciliation procedure must
  establish a new owner generation.

Initialization is a separate CREATE_NEW genesis transaction and HEAD, allowed
only with an independently reviewed explicit initial slow state. Existing
bytes always refuse initialization. This is not a migration default.

## Complete snapshot and external acceptance

While intake is closed and all participating E6b writers are under the store
owner, `capture` reads HEAD and every reachable transaction into private byte
buffers under the owner lock. It returns a generation-bound subject containing
HEAD digest, ordered transaction digests, state revision, complete application
universe, and provider/source revisions. Returned buffers are defensive copies.

That subject is not completeness authority. A separate configured resolver
must return an independently retained `:wm/e6b-store-completeness-v1` acceptance
binding the exact capture digest, owner/store id, generation, application
universe, writer inventory, source pins, reviewer identity and outcome. The
store producer cannot mint this record. Missing, borrowed, stale, fixture, or
rejected acceptance refuses production reconciliation. Snapshot output always
states `:restart-authorized? false`.

## First installation and runtime obligations

The first installation cannot be protected by the controller it installs.
An independently reviewed external fence must stop every E6b writer, drain
accepted work, establish the explicit genesis/migration record, acquire the
store lease, and then start only code whose source/classpath snapshot was
reviewed. No such fence or production writer inventory exists at this commit.

Later wiring must place the compare-and-commit call after exact outcome review
and before any consumer can observe the t+1 state. Every outcome/retry path must
use the same stable application id. The ingress controller and lifecycle
snapshot can participate only after every relevant writer is routed through
their common generation; current atoms and locks do not provide that fact.

## Required isolated implementation controls

1. successful genesis then two ordered compare-and-commits; full restart
   readback equals exact state, ledger order and byte digests;
2. stale prior, duplicate event, duplicate prior revision, and same application
   id with changed input/output refuse without changing HEAD;
3. identical retry before and after restart returns one transaction;
4. fault injection before transaction sync, after transaction sync, after HEAD
   rename and after root-directory sync, with the recovery outcomes above;
5. truncated, malformed UTF-8, trailing-form, digest mismatch, missing parent,
   fork and reordered application-index controls;
6. competing same-JVM and separate-process owner refuse before mutation;
7. invalid/unserializable state or application refuses before publication;
8. capture versus commit concurrency yields either complete generation, never
   mixed state/application bytes; exposed-buffer mutation cannot alter replay;
9. missing/borrowed/stale completeness acceptance and incomplete writer
   inventory refuse; producer-generated self-acceptance refuses;
10. initialization over existing evidence and silent fresh-state fallback
    refuse; poisoned ownership cannot be cleared by an ordinary caller.

