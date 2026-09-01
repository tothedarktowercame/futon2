# C415 — system-journal anchor assessment

Date: 2026-09-01. Assessment only; nothing was implemented.

## Verdict

Journal anchoring would raise the cost of an **undetected same-user rollback
within the journal's retention window**. It would not establish that a run
happened, identify the original copy of a ledger, or provide durable
non-repudiation. It is therefore an operational rollback tripwire, not an
append-only authority suitable for the certificate's trust boundary.

For the present campaign, wiring it as load-bearing evidence is not worth the
claim it could honestly support. Presented as “the ledger is externally
anchored,” it would be theatre: a stronger-looking token for a much narrower
fact.

## What a retained anchor could prove

Assume every transition records the logical ledger identity, ordinal,
predecessor hash, and new head in journald, and a verifier enumerates **all**
matching retained entries rather than looking up a caller-selected one.

While those entries remain retained, the root-owned journal can establish:

- journald accepted a message from a trusted kernel/service context associated
  with uid 1000 at a journal sequence, boot, realtime, and monotonic position;
- a ledger presented with a shorter history than the greatest retained
  anchored ordinal has discarded an anchored suffix;
- a truncate-and-re-extend attack that diverges from a retained anchored head
  creates two continuations and can be reported as a fork rather than accepted
  as one canonical chain;
- editing an already anchored ledger row without reproducing its old digest is
  detectable independently of the mutable ledger file.

That is a real improvement over the current self-contained hash chain. C395's
discarded `click-terminal` and `certified` rows would remain observable in the
journal, and the replacement branch would not erase them.

## What it would not prove

### Copy identity

An unchanged copied ledger has the same logical identity, rows, and heads as
its source. One journal sequence supports both byte-identical files. The
journal can reveal **divergent continuations** of the same identity; it cannot
say which passive copy is “the original.” A logged pathname would record a
caller-controlled name, not physical-file identity or ownership authority.

### Event truth

Journald would attest ingestion of the head, not truth of the transition. The
same process that can currently supply a shape-compatible terminal receipt or
certificate can faithfully log the head of that forged transition. Anchoring
C395's unearned `certified` row would make its existence harder to erase while
doing nothing to show that the run happened.

### Permanent history

The journal is retention-bounded and subject to rotation, vacuuming, disk loss,
boot/machine replacement, and root administration. Once an anchor leaves the
retained population, rollback before the oldest remaining entry becomes
indistinguishable again unless another independent authority retained it.

### Cryptographic non-repudiation

The inspected journal has no forward-secure sealing tags (`Tag objects: 0` and
no FSS key observed). Root can remove or replace journal storage. A process
running as `joe` can submit arbitrary application fields and false message
content. Journald's trusted fields establish ingestion context, not the human
author, semantic truth, or exclusive control of uid 1000.

## Can `joe` write something root cannot repudiate?

No. `joe` can submit a message through journald and cannot subsequently edit
the root-owned journal files, which is the useful asymmetry. Root controls the
daemon, files, retention, and machine clock and can delete or replace the
record. There is no external signature, remote witness, transparency log, or
forward-secure seal that survives that authority.

The strongest honest statement is:

> While retained on this boot/machine, journald reports that it ingested this
> message from this trusted process context at this sequence position.

That is not non-repudiation.

## Cost and failure semantics

A load-bearing anchor would have to be synchronous and fail closed:

1. submit the transition head;
2. obtain or resolve the exact journal entry and its trusted cursor/sequence;
3. verify its content and context;
4. only then commit or acknowledge the state transition.

Socket-write success alone is not durable acknowledgement, and a best-effort
write creates two visually similar populations—anchored and unanchored—whose
difference is easy for a later reader to miss. Proceeding after an unavailable,
rotated, unreadable, or ambiguous journal observation would therefore defeat
the purpose. The state machine would need to refuse the transition.

That introduces a root-service availability dependency into every operator
transition and still leaves event truth and copy identity unsolved. It is a
disproportionate cost for a bounded rollback alarm, particularly because the
blocking C395/C404 failures concern producer and selection authority rather
than erasure alone.

## Decision

Do not wire journald as certificate authority. If it is ever used, describe it
only as a retention-bounded rollback/fork detector and never as proof that the
anchored transition was true or that a presented ledger is the unique original.

In the requested plain wording: **it raises the cost of undetected rollback
within retention and proves only that journald ingested a claimed head. It does
not prove the run or canonical ledger identity.**

## Delivery inventory

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

Result: `{:name :check-inventory, :exit 0, :unknown (), :missing ()}`.
