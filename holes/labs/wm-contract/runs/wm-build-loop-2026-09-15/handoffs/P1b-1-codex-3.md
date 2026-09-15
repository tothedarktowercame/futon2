# Handoff P1b-1 to codex-3: durable work-target model store

From claude-2, the War Machine build lead under Joe's 2026-09-15 commission. I
will review your work independently. **When done, bell claude-2 back with a
summary, the commit sha(s), your PROTOCOL.md (protocol plus failure/recovery
matrix) and the gate receipt paths.**

## Goal (one behaviour)

Add a new namespace `futon2.aif.work-target-store` in futon2. It is the
**persistence authority** for a new model, the work-target belief. It provides:

- a create-once genesis
- an authoritative durable head
- an append-only, hash-chained sequence of committed snapshots
- a strict reader
- a commit protocol that validates the predecessor
- idempotent retries
- explicit crash-recovery states

Checklist: WM-02 Q4 (lineage) and E02. The patterns this applies are
`belief-keeps-its-lineage` and `complete-acquisition-universe`.

**Not in scope:**

- any call from `war_machine.clj`, `full_loop_runner.clj` or `trace.clj`
  (that is P1b-2)
- writing the production genesis
- any reload, click, scheduler activation or load into the shared JVM

Tests use **isolated temporary store directories only**. No function may
create a genesis implicitly.

## Authority: read these first

In futon2 under `holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/`:

- `P1b-packet.md` §1–2: why the trace cannot serve as this authority.
- codex-28's Q-C decision (`invoke-1789502747631-21229-70a1d4a7`). Its
  requirements are restated below; they are binding.
- `declarations/wm-work-target-interpretation-v1.edn`, SHA-256
  `055d579d4bec9ef52c6a3e2949b730d413624ddbc6b60cc00931810a77675e5e`.
  This is the declaration the genesis pins.
- A precedent **not to copy**: `src/futon2/aif/lane_futility.clj`
  `with-index-lock`. It uses a FileLock only, so two threads in one JVM
  raise `OverlappingFileLockException`. The same file also falls back
  **silently** to a non-atomic move when `ATOMIC_MOVE` is unsupported. It
  does no fsync.

## Store layout and records

The store is a directory. Everything in it is EDN, one form per file, read
strictly: exactly one form, a parse failure is an error, nothing is skipped.

- `genesis.edn`
  - `{:schema :wm/work-target-store-genesis-v1`
  - `:store/id <uuid>`
  - `:storage-protocol/revision "v1"`
  - `:declaration {:path :sha256 :interpretation-revision}`
  - `:decision-refs [..]`
  - `:created-at`
  - `:authorized-by {:actor :commission}`
  - `:statement "rollout genesis, not historical initialization"}`
  - The declaration bytes must be retained, or referenced by an immutable,
    resolvable content reference. The reader re-hashes the declaration.
- `snapshots/<seq>.edn` (immutable)
  - `{:schema :wm/work-target-snapshot-v1 :store/id :seq :genesis-sha256`
  - `:previous-sha256` (nil only for seq 1)
  - `:operation {:id :kind :caller-identity-type}`
  - `:expected-head <ref of the head it was computed from>`
  - `:information-cutoff :committed-intent-sha256 :payload <opaque map>}`
- `HEAD.edn`: the authoritative commit record.
  - `{:store/id :genesis-sha256 :status :established-no-snapshots | :committed`
  - `:seq :snapshot-sha256 :operation/id}`
  - The head is **never** discovered by listing snapshot files.

The payload is opaque to the store. Envelope validation also calls an
**injected** `payload-validator` (a function returning `:ok` or a typed
refusal). P1b-2 will pass the P1a lineage/state validator. Your tests use
their own validator, including one that rejects.

## Read statuses (typed; each distinct)

- `:model-not-established`: no genesis. This never auto-creates one.
- `:initialization-incomplete`: genesis exists but no head.
  - Specify the initialization transaction, so this state is distinguishable
    from damage.
  - If they cannot be distinguished deterministically, refuse with
    `:pending-recovery`.
  - Never treat it as "no snapshots ever".
- `:established-no-snapshots`: head status says so.
- `:committed`: carries `{:seq :sha256 :snapshot}` after full chain
  verification.
- `:damaged`: carries a reason. Reasons include:
  - duplicate or non-contiguous seq
  - a middle gap
  - altered bytes (hash mismatch)
  - a deleted tail with a surviving head
  - a head naming a missing snapshot
  - a wrong genesis, store id or declaration
  - a parse failure
  - envelope or payload validation failure
- `:pending-recovery`: prepared snapshot files beyond the head, which are
  never adopted automatically.

## Commit protocol (write it down first in PROTOCOL.md, then implement)

`commit!` takes `[store expected-head operation payload]`.

- **Locking.** Take a store-wide writer lock: an in-JVM mutex per canonical
  store path **plus** a cross-process FileLock.
- **Predecessor check.** Under the lock, strictly re-read and verify the
  current head. Compare it with `expected-head`. On mismatch, return
  `:stale-predecessor`. Do not merge or rebase.
- **Idempotency.** If `operation/id` is already committed, compare
  `committed-intent-sha256`:
  - equal → return the existing committed reference (a lost-response retry);
  - different → `:operation-id-reused`.
- **Write.** Write the snapshot bytes to a temp file and `FileChannel.force(true)`.
  Rename it to `snapshots/<seq>.edn` with `ATOMIC_MOVE`. **No non-atomic
  fallback**: if the move is unsupported, fail with a typed persistence
  failure. Then fsync the snapshots directory. Then write the head temp,
  force it, rename it atomically over `HEAD.edn`, and fsync the store
  directory.
- **Commit point.** The commit point is the durable head rename, with the
  directory fsynced.
- **Failures.** Any failure before the commit point is a typed
  `:persistence-failed` with the stage named. The caller must not treat the
  proposal as committed.
- **Filesystem facts.** Verify against the actual JDK and filesystem (ext4
  `rw,relatime` here) rather than assuming. Specifically: that a directory
  fsync via `FileChannel.open(dir, READ).force(true)` works on this platform,
  and what `ATOMIC_MOVE` maps to. Record what you verified. Note that the
  disk is 96% full, so ENOSPC is a realistic failure to include.

Genesis has its own create-once transaction under the same lock:

- Exactly the same requested identity and content returns the existing
  genesis.
- Different content refuses.
- It never overwrites and never starts a new epoch after data loss.

`resolve-reference [store ref]` validates that a historical trace or
checkpoint reference `{:store/id :seq :sha256}` names an ancestor on the same
committed chain. An old ancestor is accepted. A reference that is ahead of the
head, off-chain or missing is refused. It must **not** require equality with
the current head.

PROTOCOL.md must contain:

- the commit point
- a failure matrix for a crash or error at **each** boundary: before the
  snapshot write, after the write before the file fsync, after the file fsync
  before the rename, after the rename before the directory fsync, after the
  head temp before the head rename, after the head rename before the
  directory fsync, and after commit with the response lost
- what the reader reports in each row
- the recovery policy for each row
- the trusted persistence boundary

State the limitation plainly: if every authority record and every
independent reference is rolled back together, local hash chaining cannot
detect it. An external monotonic anchor would be needed, and this packet
does not build one. Do not describe the store as tamper-proof.

## Acceptance controls (tests in isolated temp stores)

For each test, state whether it **simulates** a failure (for example an
injected failpoint at a named stage) or **exercises the real filesystem**.
The controls:

- **Lifecycle cases.** Missing genesis; incomplete initialization; an
  established empty store; a valid empty-payload snapshot. All four are
  distinct statuses.
- **Chain damage.** Each of these gives `:damaged` with its specific reason:
  duplicate seq, altered bytes, a middle gap, a deleted tail with a surviving
  head, a missing head, a wrong genesis or declaration.
- **Races.** Two proposals from the same head, run concurrently from threads
  in one JVM: exactly one commits and the other gets `:stale-predecessor`.
  There must be no `OverlappingFileLockException`.
- **Retries.** A retry after a lost success response returns the original
  reference. The same operation id with different intent refuses.
- **Crashes.** A failpoint at every boundary in the matrix, followed by a
  read. No uncommitted tail is silently adopted and nothing resets to
  genesis. An acknowledged snapshot is never overwritten.
- **References.** An old ancestor reference is accepted. Off-chain,
  newer-than-head and missing references are refused.
- **Validators.** A rejecting `payload-validator`, or a declaration hash
  mismatch, commits nothing and initializes nothing.
- **Durability.** One real-filesystem test showing the snapshot file and the
  directory are fsynced. Instrument the calls; don't just assert that no
  exception was thrown.

If the JDK or filesystem APIs cannot meet this durability or ordering
contract, **stop and bell claude-2 with the exact conflict**. Do not implement
a weaker workaround.

## Gates (futon2 root, in your own short-lived process)

Run each gate bare, not piped, and retain its exit status (futon2 `AGENTS.md`):

- `clj-kondo --lint` on the new src and test files
- `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval "(arxana-check-parens-cli)"`
  on both files (see that file's header for how to pass files)
- `clojure -X:test :nses '[futon2.aif.work-target-store-test]'`

Put the outputs and PROTOCOL.md in
`futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/p1b-1/`.

## Commit

The worktree is shared. Stage **explicit paths only**: the new src, the new
test and `p1b-1/`. Never use `commit -a`, and never amend a commit that isn't
yours. codex-2 is concurrently adding `work_target_belief.clj`; do not touch
it.

Bell claude-2 back with:

- the summary
- the commit sha
- the PROTOCOL.md and receipt paths
- any API conflict
