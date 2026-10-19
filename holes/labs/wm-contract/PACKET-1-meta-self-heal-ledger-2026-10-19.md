# Dedicated META self-heal — Packet 1 receipt

Base: `6a937fa9269ac8410e7ec991f9ab15f0b33ec092`.

Scope is intentionally limited to the pure append-only injury ledger and total
validator. No runner, serving route, ordinary node/fold/D-task path, click, or
mutable production data was changed.

## Carrier schemas

Ledger:

```clojure
{:schema :wm/meta-injury-ledger-v1
 :attempt-limit positive-int
 :entries [transition-or-observation ...]
 :ledger-pin {:path "meta-injury-ledger/ledger.edn" :sha256 64hex}}
```

Transition common envelope:

```clojure
{:schema :wm/meta-injury-transition-v1
 :ordinal positive-int
 :injury-id nonblank-string
 :state :open|:claimed|:authored|:reviewed|:enacted|
        :discharged|:reopened|:abandoned
 :attempt positive-int
 :predecessor {:entry-id 64hex :entry-pin full-pin} ; global append chain
 :injury-predecessor {:entry-id 64hex :entry-pin full-pin} ; cause chain
 :entry-id 64hex
 :entry-pin {:path string :sha256 64hex}}
```

State payloads:

- `:open`: `:cause-record-pin`, `:failure-stage`, `:failure-kind`,
  `:detected-click`; attempt exactly 1.
- `:claimed`: stable `:repair-id`, algorithm, distinct fresh author/reviewer
  session identities, and attempt.
- `:authored`: repair id, patch SHA, worktree.
- `:reviewed`: repair id, verdict, reviewer SHA, and the claimed reviewer
  session.
- `:enacted`: repair id, exactly one pinned reload receipt or integration
  commit, plus the authorized retry-click identity.
- `:discharged`: repair id, exact authored patch SHA, exact enacted retry click,
  and successful outcome.
- `:reopened`: repair id, exact patch/retry binding, new cause pin, attempt+1.
- `:abandoned`: stable repair id, attempt equal to configured limit, and typed
  `:attempt-limit-exhausted` reason.

Inert external record:

```clojure
{:schema :wm/meta-injury-observation-v1
 :ordinal positive-int
 :record-pin {:path string :sha256 64hex}
 :observation-kind keyword
 :predecessor global-entry-reference
 :entry-id 64hex :entry-pin full-pin}
```

## Proved invariants

- Both the complete ledger and every entry are content sealed; validation
  recomputes every seal and both predecessor chains.
- Injury and repair identities cannot change across a transaction.
- Only legal state successors are admitted. Replays, duplicated entry IDs,
  malformed values, tampering, illegal order, and identity substitutions return
  typed refusals rather than throwing.
- Claimed attempts use distinct author/reviewer identities and cannot reuse any
  prior claimed session.
- Discharge requires the exact authored patch and exact retry click authorized
  by enactment. A successful unrelated click is refused.
- Reopen increments the attempt. A retry beyond the configured limit appends a
  typed `:abandoned` transition.
- External/run-record observations are append-only audit entries and have no
  state-machine authority. A 64-case property-style append sequence preserves
  the active injury set.
- Production-shaped fixtures use the exact three ticketed 2026-10-19 run paths
  and their observed content SHA-256 values. The first record is the opening
  cause and is also appended explicitly when replaying the external campaign
  stream: cause attribution does not implicitly substitute for an observation
  audit entry. All three external appends remain enumerable while the active
  injury set is unchanged.

## Remaining work — Packet 2 and later

Packet 2 must classify terminal algorithm/apparatus failures and append
`open`/`reopened` entries through this API, using explicit non-injury reasons
rather than stage allowlists. Later packets still owe the coordinator, sealed
work order, Agency session dispatch, review/enactment/retry transport, durable
storage/locking, serving integration, isolated-JVM harness, and quarantine of
the ordinary-route bypasses. This receipt does not claim autonomous self-heal.

No live click and no full suite were run.
