# Injury 2aef: ledger / transaction divergence

Read-only discovery, 2026-10-28. No click, reload, ledger, transaction, queue,
or service state was changed.

## 1. Which transaction authored `87c10683`

The authoring belongs to repair transaction
`repair/2f6826382458c5b36338189a8623f41abb120f92`, attempt 2. The retained
transaction is
`/home/joe/notes/wm-next/criteria-completeness/repair-iteration-3/transaction.edn`
(the current canonical `data/wm-meta-injuries-live-2026-10-22/transaction.edn`
has the same SHA-256,
`2bcb30290d678e9ecc5e8ea665b7cde1f0ab93b7ae720c9911b881cc13382637`).
Its author receipt is:

- receipt pin
  `meta-repair-port-receipts/repair/2f6826382458c5b36338189a8623f41abb120f92/2/author.edn`,
  SHA-256 `35ffda65fccf2b0b97806ffca0972b426db32623e7490f86d1d531d1d75617b9`;
- patch `87c10683a0788ae6c8c9df185fab0b9649586842`;
- Agency session `zai-1257df9d-aafc-43ff-a1f1-ed8215390c8e`, seat
  `wm-author`.

The Git commit itself has author and commit time `2026-10-28T00:06:07Z` and
trailers `Agent-Id: wm-author`, `Agency-Job:
invoke-1793142252991-5686-9491e828`, and `Dispatched-By: wm-full-loop`.
The machinery dispatch record
`data/wm-full-loop-machinery-154/wm-contract-machinery-154-v1/attempt-002/004-dispatch.edn`
binds that same job id to `wm-author`; its retained reply is named by
`005-build.edn`.

The click that ran the repair transaction was
`wm-click-0f7da372-e078-4845-96a6-3c6014a27662`. Its durable click state is
`/home/joe/code/futon3c/data/wm-click-acceptances/click-wm-click-0f7da372-e078-4845-96a6-3c6014a27662.edn`:
it started at `2026-10-27T23:35:33.196448299Z`, completed at
`2026-10-28T00:07:28.199717690Z`, reports route
`:dedicated-meta-self-heal`, injury `2aef7e0c...`, repair `2f682638...`,
attempt 2, and then bound the retry child bearing patch `87c10683...` at
`00:07:28.288554866Z`. The child state is
`click-wm-retry-repair_2f6826382458c5b36338189a8623f41abb120f92-2-patch-87c10683a0788ae6c8c9df185fab0b9649586842.edn`.

Thus `87c10683...` is not an unrelated repository head. It is the machine's
second authored repair for the same injury, repair id, and attempt.

## 2. Where the ledger append was skipped

The normal author sequence is in
`src/futon2/aif/meta_repair_coordinator.clj:460-492`:

1. `invoke-once` invokes or reuses the author receipt (`460-464`);
2. the coordinator validates the patch and worktree (`477-481`);
3. it appends an `:authored` transition containing the patch and exact author
   receipt pin (`482-489`);
4. the author checkpoint follows (`492`).

For this run, step 3 was skipped by its own guard. The append is performed
only when the latest ledger state is exactly `:claimed` (`482`). The ledger
already ended this repair/attempt at ordinal 58, state `:enacted`, patch and
integration commit `02e9d5e7e11e92479d586ad078ce960902c36fa1`, with an
other-open-injury requeue cause. Therefore the false branch returned the
unchanged ledger (`490`) even though `invoke-once` had just accepted the new
`87c10683...` author receipt.

Production then checkpointed the two stores independently at
`src/futon2/aif/meta_self_heal_production.clj:1561-1582`. At the author
checkpoint it persisted the unchanged ledger (`1570-1571`) and the
transaction containing the new receipt (`1572`), verified both writes, and
continued (`1573-1582`). The later review and enact guards have the same
state-dependent skip shape at `meta_repair_coordinator.clj:503-514` and
`530-545`. The retained transaction consequently contains successful author,
review, enact, and retry receipts for `87c10683...`, while the ledger's only
`:authored` transition for this tuple remains ordinal 53 / `02e9d5e7...`.

This was not a store-busy refusal, crash window, or failed write. The durable
transaction proves the checkpoint succeeded, and the ledger was intentionally
left unchanged by the branch. The enabling mistake happened earlier in
transaction selection: an enacted ledger tuple was allowed to proceed with a
fresh receipt-less transaction. `coordinate` accepts `:enacted` as resumable
at `meta_repair_coordinator.clj:448-456`; before the present identity guard,
that state did not require the transaction to carry the ledger-admitted author
receipt before author dispatch.

The later failures are correct observations of this divergence. The retry
attribution path finds the ledger's latest matching `:authored` transition at
`meta_self_heal_production.clj:1407-1422`, so it expects `02e9d5e7...` and
refuses the `87c10683...` retry. The current coordinator compares the ledger
author receipt pin and patch to the transaction at
`meta_repair_coordinator.clj:125-145` and refuses
`:coordinator/resume-identity-refused` before another author dispatch
(`453-456`).

## 3. Recovery support

There is no supported path that promotes a transaction author receipt into a
missing ledger `:authored` transition for an already `:enacted` tuple.

The available transaction lifecycle operations do different jobs:

- `injury-selection` can defer and later restore a byte-pinned transaction
  when its recorded blocker closes
  (`meta_self_heal_production.clj:1035-1089`), but restoration does not
  reconcile its receipts with the ledger.
- `retire-stale-transaction!` moves a transaction aside only when its attempt
  number is older than the ledger attempt
  (`1149-1163`, called at `1480-1486`). Here both sides are attempt 2.
- `resume-identity-refusal` now stops on the disagreement; it intentionally
  does not choose either history (`meta_repair_coordinator.clj:125-145`).
- Ledger abandonment/reopen machinery handles typed retry outcomes and spent
  attempts. It does not authenticate and import an orphan transaction receipt
  as a historical `:authored` transition.

Nor is there an automatic route that abandons this same-attempt transaction
and resumes ordinal 58 from the ledger alone. The canonical transaction file
occupies the selection path, passes transaction validation, and is stopped by
the identity check. Removing, replacing, or hand-editing it would be an
unsupported state change, not recovery.

## 4. Smallest correct repair (not made)

The ledger must remain the admission authority. Comparing resume identity to
the transaction instead (option **b**) would accept a receipt that the ledger
never admitted and would make the retry-attribution check disagree with the
coordinator. Therefore option **a** is required.

For future runs, enforce one ordered checkpoint rule for the author phase:

1. When the ledger is already `:authored`, `:reviewed`, or `:enacted`, require
   the incoming transaction to contain the exact ledger-admitted author
   receipt pin and patch **before** invoking any port. The existing
   `resume-identity-refusal` is the correct check; transaction selection must
   not create a fresh receipt-less transaction for that same
   `(injury-id, repair-id, attempt)`.
2. From `:claimed`, invoke the author, append the matching `:authored`
   transition, then persist ledger and transaction as a checked pair. On a
   partial write, restart must reconcile only an exact sealed receipt/transition
   pair; it must never invoke a second author for the tuple. The current phase
   checkpoint already provides the ordering location
   (`meta_self_heal_production.clj:1561-1582`), but it needs an explicit
   recoverable pair protocol rather than two writes followed only by a combined
   status check.

For the existing divergence, add a medic-authorized, append-only disposition
that cites both sealed histories and does one of two explicit things: (1)
rejects/archives the unadmitted `87c10683...` transaction and restores the
ledger-admitted `02e9d5e7...` transaction, or (2) abandons attempt 2 and opens
a distinct next attempt whose author transition is recorded normally. It must
not manufacture a backdated `:authored` transition for attempt 2, rewrite
ordinal 53/58/70, or silently treat the transaction receipt as ledger state.
No currently supported coordinator/medic entry point performs this
reconciliation, so the exact disposition requires a reviewed recovery
addition before another click.
