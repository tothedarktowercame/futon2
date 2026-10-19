# Packet 3 — dedicated META repair transaction

Date: 2026-10-19

Scope is deliberately limited to the coordinator seam.  This does **not**
wire serving, Agency, or a live click.

## Lineage and call graph

Base: `200aa4892a894537c6e427266521526c34b827ce`.

```text
meta-injury-store/load-durable-ledger
  -> meta-injury-ledger/validate-ledger + active-injuries
  -> meta-repair-coordinator/prepare
       -> sealed :wm/meta-repair-work-order-v1
       -> sealed :wm/meta-repair-transaction-v1
  -> meta-repair-coordinator/coordinate
       -> ledger claimed
       -> injected :author port -> ledger authored
       -> injected :review port -> ledger reviewed
       -> injected :enact port -> ledger enacted
       -> injected :retry port -> ledger discharged | reopened | abandoned
```

The coordinator namespace requires only `clojure.string`, `action-identity`,
and `meta-injury-ledger`.  It has no loop-node, pattern-fold, policy-family/G,
D-task, serving, Agency, or click dependency.  Instrumented forbidden ports
remain at zero in the happy-path test.

## Carrier schemas

- `:wm/durable-meta-injury-ledger-v1`: `:status :loaded`, exact disk
  `:source-pin`, and the sealed `:ledger`.
- `:wm/meta-repair-work-order-v1`: stable `:injury-id`, `:repair-id`, positive
  `:attempt` and `:attempt-limit`, pinned cause and diagnosis, explicit
  acceptance test, named author/reviewer/repair-reviewer and three distinct
  session ids, repository/worktree, source ledger pin, and `:work-order-pin`.
- `:wm/meta-repair-port-receipt-v1`: phase, injury/repair/attempt,
  predecessor pin, typed status, phase payload, and `:receipt-pin`.
- `:wm/meta-repair-transaction-v1`: sealed work order, append-only vector of
  accepted phase receipts, and `:transaction-pin`.

All seals are canonical content digests.  A port response is accepted only
when its phase and injury/repair/attempt/predecessor projection match the
request.  An accepted receipt is retained before any later phase runs.

## Proved behavior

- Happy path appends exactly
  `open -> claimed -> authored -> reviewed -> enacted -> discharged`.
- Author failure, reviewer rejection, and enact/reload failure stop at their
  preceding durable boundary.
- Malformed or identity-substituted port receipts refuse typedly.
- Retry success lacking repair, patch, or retry-click attribution cannot
  discharge.
- Retry failure reopens; failure on the configured final attempt becomes
  typed `abandoned`.
- Resuming a transaction with a recorded failed receipt does not dispatch the
  same phase again.
- Preparation requires a validated durable ledger, an active injury, and
  distinct fresh sessions.

## Remaining Packet 4 gap

Production has not yet been wired to call this coordinator, persist each
returned ledger/transaction boundary, or adapt actual Agency author/reviewer
transport.  The isolated-JVM fake-seat harness and serving arbitration that
blocks ordinary work while an injury is active remain Packet 4 work.  No live
data was mutated and no click was launched.
