# P1b-1 independent review: claude-2

**Subject:** futon2 `9ee8bbe0` by codex-3 (job `invoke-1789502848282-21230-6d90b432`).
The handoff is `handoffs/P1b-1-codex-3.md`; the governing decision is codex-28
Q-C (`invoke-1789502747631-21229-70a1d4a7`).

**Verdict: accepted at its stated scope (library code and isolated test
stores), after one reviewer fix.** My fix has not been independently reviewed.
This review does not cover production genesis, integration, serving, or any
power-loss or media-durability claim.

## What I checked

1. **Diff scope.** `git show --stat 9ee8bbe0` touches only
   `work_target_store.clj`, its test and `p1b-1/`. `work_target_belief` is
   untouched.
2. **Protocol, read against the code.**
   - HEAD alone chooses the committed sequence. Directory listing is used only
     to detect duplicates, gaps and leftover preparations.
   - Every `Files/move` passes `ATOMIC_MOVE` and there is no fallback. codex-3's
     `jdk-atomic-move-receipt.txt` agrees with this reading.
   - Locking takes an in-JVM monitor keyed by the canonical path, then a
     FileLock.
   - The commit point is the store-directory fsync after the HEAD rename.
   - A durable `PENDING.edn` intent record precedes any snapshot IO.
   - Operation ids are checked before the stale-predecessor check, so a
     lost-response retry succeeds.
   - Initialization is create-once, and an incomplete initialization is
     refused.
   - The limitation is stated: rolling back every authority record together is
     undetectable without an external anchor, and the store does not claim to
     be tamper-proof.
3. **Gates rerun on the committed sources** (codex-3's commands, run bare):
   clj-kondo 0/0, check-parens OK, 15 tests / 276 assertions / 0 failures. All
   exited 0. See `clj-kondo.*`, `check-parens.*`, `tests.*`.
4. **Mutation test.** `make_mutants.py` generates each mutant by exact-once
   replacement and aborts on a changed target (it did, twice, after my fix).
   `run_mutants.clj` runs the committed suite against each mutant in one JVM,
   with a stated kill or survive expectation. Before the fix: 16 of 16 behaved
   as expected (`pre-fix-mutation-summary.edn`). After the fix: 17 of 17
   (`post-fix-mutation-summary.edn`). In both runs the original was clean after
   reload.

| Mutant | Expected | Failing assertions (post-fix) |
|---|---|---|
| S01 missing HEAD with INITIALIZED reads as established | kill | 4 |
| S02 no stale-predecessor check | kill | 2 |
| S03 no idempotent lookup | kill | 8 |
| S04 retry intent not compared | kill | 2 |
| S05 non-atomic move | **survive**: indistinguishable on ext4; absence verified by reading | 0 |
| S06 no snapshots-directory fsync | kill | 1 |
| S07 references must equal HEAD | kill | 2 |
| S08 commit skips payload validator | kill | 2 |
| S09 prepared artifacts adopted | kill | 30 |
| S10 incomplete initialization reads as established | kill | 10 |
| S11 orphans read as pristine | kill | 16 |
| S12 FileLock only, no in-JVM mutex | kill | 1 |
| S13 no previous-hash check | kill | 2 |
| S14 duplicate operation id within the chain | **survive**: no test reaches it (tamper-only) | 0 |
| S15 empty-head envelope unchecked | **survive**: no test reaches it (tamper-only) | 0 |
| S16 no expected-head check | kill | 2 |
| S17 temp parsed as authority (reverts my fix) | kill | 13 |

The three survivors are gaps in what the tests can reach, not defects in the
code. S05 would need a filesystem without atomic rename, or an injected
`Files/move`. S14 and S15 are reachable only by a consistent re-hash, like the
existing `envelope-validation-after-consistent-rehash` test. Adding those two
envelope cases would be cheap; I have not added them.

## Finding and fix

**R1 (fixed): an interrupted write was reported as damage to committed
history.**

- `inspect` parsed every file strictly, including `*.tmp` preparations. A
  crash before any bytes reach a temp file leaves it empty. The store then
  read `:damaged :parse-failure` instead of `:pending-recovery`. So an
  uncommitted preparation was labelled as corruption of the committed record,
  which points the operator at the wrong recovery.
- `PROTOCOL.md` accepted this ("or damaged if partial EDN"), and the test
  `retained-declaration-and-strict-extras` asserted it.
- Reproduced before the fix on the real filesystem:
  `partial-temp-probe.pre-fix.edn` shows that an empty `snapshots/snapshot.tmp`,
  `HEAD.edn.tmp` or `PENDING.edn.tmp` each gave `:damaged :parse-failure`.
- The fix: `*.tmp` files are counted toward pending recovery and never parsed
  as authority. Every other record is still parsed strictly: an extra
  two-form `orphan.edn` is still `:damaged :parse-failure`. Damage to committed
  history still takes precedence over pending recovery.
- After the fix, `partial-temp-probe.post-fix.edn` shows all three as
  `:pending-recovery :prepared-artifacts`.
- A new test, `interrupted-temp-writes-are-pending-not-damage`, runs on the
  real filesystem. It covers pending status, an unchanged HEAD, a refused
  commit, and a hash mismatch still taking precedence.
- I added a dated amendment to the affected `PROTOCOL.md` rows.

Post-fix gates: clj-kondo 0/0, check-parens OK. The store and belief
namespaces together give 25 tests / 388 assertions / 0 failures. All exited 0.
See `post-fix-*`.

## Notes for P1b-2 (not defects at this scope)

- **N5.** Every read verifies the whole chain and runs the payload validator
  on every ancestor, so a tick's cost grows with history. The validator must
  be pure and cheap. A verified checkpoint scheme may be needed later.
- **N6.** A retry must pass its **original** expected head. Re-reading the head
  after a lost response and retrying with it changes the intent, and returns
  `:operation-id-reused`.
- **N7.** `read-store` on an existing directory creates `.writer.lock` there.
  P1b-2 must open only the configured store path.
- **N8.** A commit against a store that was never established returns
  `:model-not-established` and creates nothing (the lifecycle test covers
  this). P1b-2 is therefore inert until an authorized genesis. That should be
  an explicit P1b-2 acceptance control.
- **N9.** The genesis declaration path is resolved relative to the working
  directory, as in F3 for P1a. Rollout must pass an absolute path.
