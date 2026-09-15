# P1b-1 storage protocol v1

Commission: invoke-1789502848282-21230-6d90b432. Q-C authority:
invoke-1789502747631-21229-70a1d4a7 (binding corrections in handoff).
Implementation scope: isolated tests and library code only; no production genesis.

## API and identity

`open-store path {:payload-validator f}` constructs a handle without IO.
The validator is mandatory, takes the opaque payload map, and returns `:ok`
or a typed refusal map. `read-store`, `initialize!`, `commit!`, and
`resolve-reference` return typed status maps. No read or commit initializes.
`initialize! store genesis` is the sole explicit genesis operation. Genesis
must include the commissioned fields and a declaration path whose literal
bytes hash to the supplied SHA. Those bytes are retained as `declaration.edn`;
the original path is provenance, never the later reader's mutable authority.
`commit! store expected-head operation payload` takes the exact `:head` from
read-store. Operation has `:id`, `:kind`, `:caller-identity-type`, and
`:information-cutoff`; the latter is also copied into the snapshot envelope.
The committed intent hashes a canonical EDN rendering of expected-head,
operation, and payload. Map/set ordering is normalized; all values must
round-trip through strict EDN. Snapshots hash their exact stored bytes.
Historical references are `{:store/id :seq :sha256}` and accept any ancestor.

## Lock and trusted persistence boundary

All reads and writes of an existing directory hold one JVM mutex keyed by
canonical path, followed by a cross-process FileLock on `.writer.lock`.
The mutex registry is defonce and never evicted while a process is alive.
The lock file contains one EDN coordination form and is parsed strictly too;
it is never renamed or removed. A zero-length lock file can be bootstrapped
only in an otherwise pristine directory, never over an existing authority. Directories must not be replaced, aliased by bind mounts, or edited
by writers outside this protocol during an operation. Cooperating processes
must use the same canonical directory and filesystem locking semantics.
The filesystem, JDK, kernel and device must honor successful force calls.
File and directory force failures and ENOSPC are errors, with no fallback.
All publications use same-directory ATOMIC_MOVE; snapshots are checked absent
under the lock before publication. HEAD alone chooses the committed sequence.
Listing checks for duplicates, gaps and unpublished artifacts; it never elects
or advances HEAD. No automatic recovery promotes a prepared snapshot.

## Initialization transaction

Validate the entire requested genesis and declaration before creating anything.
A newly created store directory is fsynced through its parent. Write and force
`INIT.edn` (contains the exact requested genesis), atomically publish it and
force the store directory. Retain the declaration, create snapshots/, force
the store directory, then publish genesis.edn, then the empty HEAD.edn, each
with file force, atomic rename and parent-directory force. Finally publish
`INITIALIZED.edn`, binding the genesis hash, with the same ordering.
Snapshots cannot be committed until INITIALIZED exists. INIT is retained.

Genesis + matching INIT + no INITIALIZED + no HEAD/snapshots means
`:initialization-incomplete`: the initialization transaction never became
available for commits. Missing HEAD with INITIALIZED is `:damaged/:missing-head`.
Without enough matching initialization evidence the reader returns
`:pending-recovery`, never an established empty state. Missing genesis returns
`:model-not-established` only for a pristine directory; surviving authority or
preparation records mean pending recovery. Successful initialization retries
with exactly equal requested content return the existing genesis. Different
content refuses. Incomplete transactions refuse pending recovery even for an
identical retry: they require explicit forensic recovery, never a new epoch.
A declaration mismatch or bad genesis creates no directory or genesis.

## Commit transaction and commit point

Under both locks, verify declaration, genesis, initialization records, HEAD,
every ancestor's byte hash, sequence, predecessor, expected head, operation
identity/intent and injected payload validation. Check committed operation IDs
before stale-predecessor so a lost-response retry succeeds, including after
later commits. Equal intent returns the original reference; changed intent
refuses operation-id-reused. Other predecessor mismatches refuse stale-predecessor.

Before snapshot IO, durably publish `PENDING.edn` binding old HEAD, proposed
sequence, snapshot hash and operation intent. This extra intent record prevents
a head rename that has not been directory-fsynced from looking acknowledged.
Write snapshot temp, force(true), ATOMIC_MOVE to snapshots/<seq>.edn, fsync
snapshots/. Write HEAD temp, force(true), ATOMIC_MOVE over HEAD.edn, fsync store/.
**The commit point is successful store-directory fsync after the HEAD rename.**
Remove PENDING and fsync store/ before returning success. If cleanup fails,
return persistence-failed with `:commit-point-reached? true`; the caller cannot
infer success from the failed response. A retry/read resolves the actual state.
While PENDING or any temporary/prepared file survives, readers return
pending-recovery after checking the committed chain. Corrupt committed history
always takes precedence over pending recovery.

## Failure and recovery matrix

Every injected boundary is a simulation on real temporary ext4 stores, not a
power-loss experiment. A partial or empty temp file is an uncommitted
preparation: it is counted toward pending recovery and never parsed as
authority. All other records are parsed strictly.

**Review amendment (claude-2, 2026-09-15).** The original text said partial
temp writes read as damaged/parse-failure. That misreported an interrupted
write as damage to committed history. `inspect` now treats `*.tmp` files as
preparations, and the two rows below are corrected. See
`../p1b-1-review/REVIEW.md` R1. This amendment has not been independently
reviewed.

| Boundary (crash or IO error) | Reader after interruption | Recovery policy |
|---|---|---|
| Before snapshot write (durable PENDING exists) | pending-recovery, old HEAD | Preserve evidence; explicitly abandon proposal under lock after verifying old HEAD; never adopt tail. |
| After write before file fsync | pending-recovery, old HEAD (a partial or empty temp is a preparation) | Preserve temp/intent; forensic discard of uncommitted artifacts only after verifying old HEAD. |
| After file fsync before rename | pending-recovery, old HEAD | Same; forced temp does not commit. |
| After snapshot rename before directory fsync | pending-recovery, old HEAD | Same; final snapshot filename does not commit. |
| After snapshot directory fsync | pending-recovery, old HEAD | Same; durable prepared snapshot is not committed. |
| After head temp write before force | pending-recovery, old HEAD (a partial or empty temp is a preparation) | Same; retain old head and proposal. |
| After head temp force before head rename | pending-recovery, old HEAD | Same; never substitute temp head on read. |
| After head rename before directory fsync | pending-recovery; HEAD may be old or new after restart | Explicit recovery must verify the complete chain, re-force files/directories and attest the surviving HEAD before clearing PENDING. Never guess whether the caller saw success. |
| After commit before PENDING cleanup | pending-recovery, new durable HEAD | Explicitly confirm/re-force the same HEAD and clear intent; do not recommit or overwrite snapshot. |
| After PENDING removal before cleanup directory fsync | committed or pending-recovery after restart | New HEAD was already durable. If marker reappears use preceding recovery. |
| After commit and cleanup, response lost | committed | Same operation and intent returns its original reference, even with stale expected-head. |
| Unsupported atomic move, ENOSPC, write/force/rename error | persistence-failed at named stage; read follows surviving artifacts above | Repair filesystem/capacity; inspect records under the protocol lock. Never downgrade atomicity/durability. |

Initialization interruptions before genesis publication report pending-recovery
if artifacts survive; after genesis but before empty head report initialization-
incomplete when INIT matches and no completed marker/snapshots exist; after
empty head but before INITIALIZED report pending-recovery. Missing HEAD after
INITIALIZED is damage. No initialization recovery is automatic.

This packet deliberately exposes recovery states without implementing a repair
command: an independently reviewed recovery commission must retain evidence,
validate the surviving authority and authorize any removal/republication.
Deleting a committed tail while HEAD survives is damage, never genesis.

## Limits and validation

If every authority record and every independent reference is rolled back
together, local hash chaining cannot detect it. An external monotonic anchor
would be needed; this packet builds none. This store is not tamper-proof.
Directory fsync and atomic rename are verified against the installed JDK and
filesystem in filesystem-receipt.txt. Test instrumentation records successful
real force/rename calls and checks their ordering; failpoints simulate errors,
including ENOSPC. No power-loss or physical media durability claim is made.
