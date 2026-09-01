# C301 — Agency snapshot revision design

Date: 2026-09-01

Design only. No futon3c source, live JVM, ledger, or caller was changed.

## Existing authority

The serving authority is the immutable Clojure value held by
`futon3c.transport.http/!invoke-jobs-ledger`
(`futon3c/src/futon3c/transport/http.clj:251`). All mutations pass through the
writer-locked update boundaries at lines 485–519. The active-job index is a
process-local projection whose identity is tied to that ledger value
(lines 259–289).

The current list handler (lines 5565–5574) and single-job handler
(lines 5576–5584) return job data but no identity for the ledger value from
which it came. A caller can compare job bodies, but cannot detect an ABA
transition or prove that several responses coexisted.

`:next-seq` is not the required token. It advances when jobs are created, but
not on every transition of an existing job, so it cannot identify job-state
snapshots.

## Minimal contract

Add a durable nonnegative integer `:revision` to the invoke-jobs ledger.

1. A successful semantic ledger mutation increments `:revision` exactly once
   inside the same writer-locked state transition that persists the ledger.
   No-op updates do not increment it.
2. Startup migration treats a schema-valid legacy ledger with no revision as
   revision 0, then persists the field only with the next real mutation. An
   unreadable or otherwise schema-invalid ledger remains a loud failure.
3. Every read endpoint dereferences the ledger once, derives its entire payload
   from that immutable value, and includes:

   ```clojure
   {:snapshot/revision 42
    :snapshot/authority :invoke-jobs-ledger/v1
    ...}
   ```

   This applies at least to the job list, one-job read, and active-count
   projection. The response must not fetch the revision and jobs through
   separate dereferences.
4. A caller needing several Agency responses brackets them with revision reads.
   Equal start, response, and finish revisions prove that no committed Agency
   job mutation occurred during the interval. A mismatch is
   `:snapshot-moved`/unavailable, never a job verdict.

A 64-bit monotonically increasing integer is sufficient. A UUID per mutation,
wall clock, content hash of the 134 MB ledger, or MVCC history would add cost
without strengthening this use case. Overflow is not operationally reachable,
but must fail loudly rather than wrap.

The best API ergonomics would also let one list request return all job rows and
active counts from one ledger dereference. That removes multiple reads for the
common case; the revision remains necessary for compositions with other
Agency-derived responses.

## What is unsound today

- `lane_registry_check.clj` obtains each held job through a separate Agency
  request. Its combined lane verdict can contain job states that never
  coexisted. This is reachable in practice when several lanes finish during a
  status check, not only theoretical.
- C292's `quiescence_check.py` compares two full caller-side samples. Equal
  samples detect ordinary movement but not ABA: a job can start and finish
  between the samples. During an active multi-lane campaign that is reachable
  in practice. During a genuinely drained quiet window it is less likely, but
  the check is intended to establish that very premise and cannot assume it.
- APM pollers and any future check composing multiple `/invoke/jobs/:id`
  responses have the same in-principle gap. A single-job verdict is not hybrid;
  a multi-job aggregate is.

The severity is therefore correctness-critical for quiescence/readiness, but
not evidence that individual job endpoints currently return wrong states.

## C292 caller-side approximation

Once the revision exists, C292 can bracket all Agency-derived reads:

```text
revision N -> read Agency jobs/counts -> revision N
```

That is sufficient for the Agency ledger portion with no service-side snapshot
session. It is not sufficient for C292 as a whole:

- ordinary background jobs and bounded systemd jobs are separate mutable
  authorities read by `bg.py`;
- repository and lane-registry files are separate authorities;
- an Agency revision does not advance when those external states change.

The existing two-sample sandwich detects many such changes but still permits
ABA. A caller-only approximation can honestly report “two equal endpoint
samples over interval T”; it cannot upgrade that to an instantaneous global
snapshot. Fully closing C292 requires either per-authority revisions bracketed
together, or one coordinator endpoint that captures all quiescence populations
under an agreed observation protocol. There is no truthful caller-only
workaround for authorities that expose neither a snapshot nor a revision.

## Landing and reload boundary

The code can land dormant in futon3c with unit/handler tests and a backward-
compatible ledger migration. It will not affect the shared serving JVM until
that namespace is reloaded or the process restarts. Because `http.clj` owns
live atoms, routes, persistence, and writer locks, a coordinated reload is
required before callers may rely on the token. Reload remains Joe's decision;
this design neither performs nor schedules one.

Reload acceptance must demonstrate:

- old-ledger load at revision 0;
- one increment per committed create/state/delivery/cancel mutation;
- no increment on a no-op or failed pre-commit persistence;
- post-rename durability-warning semantics retain the committed revision;
- list payload and revision come from one ledger identity;
- a bracketed mutation yields different revisions, while an unchanged bracket
  yields equal revisions.
