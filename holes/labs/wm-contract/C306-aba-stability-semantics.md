# C306 — ABA and the meaning of stability

Date: 2026-09-01. Owner: `wm-evidence`. Decision/recommendation only; no
read-set, quiescence check, writer, or consumer was changed.

## Recommendation

The substrate should report neutral observation facts, and the **caller must
declare the remedy it needs**. Do not give one unqualified `:stable` verdict
two incompatible meanings.

The shared fact is:

```clojure
{:captured-sha256 ...
 :current-sha256 ...
 :endpoint-content :equal|:different|:unavailable
 :observation-window {:started-at ... :finished-at ...}
 :intervening-events :unverified}
```

Two caller policies can then consume it:

1. **Content-current policy.** Equal endpoint bytes permit a verdict over the
   captured bytes. ABA is not a defect for this claim. A receipt whose digest
   describes its captured text, a topology parsed from one captured blob, or a
   proof checked against a named source unit needs content identity—not proof
   that no writer ran. The honest verdict name is `:content-current`, not the
   broader `:stable`.
2. **Event-free policy.** Equal endpoints do not prove that nothing happened.
   Without a monotonic witness or writer fence, return an explicit limitation,
   e.g. `{:event-free? :unverified :distinguishable-cause? false}`, even when
   endpoint content is equal. The quiescence composite belongs here. Its
   procedural drain/writer fence supplies the authority that two reads cannot;
   its digest sandwich checks visible consequences rather than proving the
   fence.

C297 already separates callers that require one snapshot from callers that
measure movement. Make that separation explicit in the caller's policy and
verdict vocabulary rather than duplicating the byte-capture mechanism.

## Is ABA worth closing?

**Not for content checks.** Treating a reverted byte sequence as changed would
reject evidence whose exact subject is content, produce false alarms during
legitimate atomic regeneration, and add no correctness to the content claim.

**Yes as a stated limit for event/quiescence checks, but not necessarily as a
new mechanism.** Cross-authority ABA cannot be closed cheaply. C303's writer
fence is the proportionate remedy for Joe's run. A check must not translate
endpoint equality into an event-free claim.

## Cost and limits of ABA witnesses

- `mtime`/`ctime` are not dependable monotonic revisions: granularity can
  collapse rapid writes, metadata can be restored, and semantics differ by
  filesystem.
- inode identity detects many atomic replacements but not in-place writes and
  can change during a content-preserving regeneration.
- Git HEAD does not identify dirty worktree bytes; index metadata is a cache,
  not a writer sequence.
- filesystem watch streams can overflow, begin too late, and do not create a
  transaction spanning repositories and services.
- taking a lock works only when every writer participates and the lock remains
  held through the consumer's decision boundary.
- a producer-issued monotonic revision closes ABA for that producer. Agency
  could provide a ledger revision; a generated-file owner could publish an
  epoch. Neither creates a common revision across Git, the lane registry,
  Agency, and systemd.

Therefore the affordable hierarchy is: content digests for content-current
claims; per-authority revisions where an event claim genuinely warrants them;
and an explicit coordinated writer fence for cross-authority quiescence.

## Inventory in the delivering commit

Canonical focused command:

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

Observed result:

```clojure
{:name :check-inventory, :exit 0, :unknown (), :missing ()}
```

This inventory result classifies the current check population; it is not an
ABA or quiescence verdict.
