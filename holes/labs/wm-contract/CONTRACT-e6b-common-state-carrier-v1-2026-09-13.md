# E6b common store-state carrier v1

Status: design only. This contract defines a projection boundary; it does not
implement an adapter, authorize a write, or establish production authority.

## Pinned facts

At futon2 `50ec2792`, the prospective verifier retains the checked prior as the
complete `:wm/e6b-prior-slow-state-v1` record and the computed successor as the
different `:wm/e6b-next-slow-state-v1` envelope
(`src/futon2/aif/machine_slow_feedback_evidence.clj:343-366`; source SHA-256
`83dbdfc91149df8cb21674720ab683ddf61b1a2492f59e195941298100be3500`).
The successor is built by the actual `advance-slow-state` call and carries its
state under `:state` (`machine_slow_feedback_evidence.clj:253-265`). The
isolated store currently accepts a generic EDN `:state`, hashes UTF-8 `pr-str`,
and compares revision, transaction and state hashes at HEAD
(`src/futon2/aif/machine_slow_feedback_store.clj:84-100,266-303`; SHA-256
`d9ee5f848419567e41f328a6b9fe14951f2568efcff4ee2dccbcac87a6bcd1fd`).

These are value hashes, not canonical-EDN hashes. Original evidence-byte
SHA-256s from `:source/digests` and value hashes from `:input/digests` remain
separate and are never replaced by the carrier hash.

## Exact carrier

The sole common projection is:

```clojure
{:schema :wm/e6b-store-state-carrier-v1
 :model/id <keyword-or-nonblank-string>
 :model/revision <nonblank-string>
 :run/id <nonblank-string>
 :tick/index <natural-number>
 :state/revision <nonblank-string>
 :slow/mode <the checked mode>
 :slow/intrinsics <the complete checked class-to-entry map>}
```

Projection from a checked prior record copies the six identity/revision fields
and its top-level `:slow/mode` and `:slow/intrinsics`. Projection from a
computed next record copies its identity/revision fields and
`[:state :slow/mode]` / `[:state :slow/intrinsics]`. No other keys are ignored:
both source records must first satisfy their complete schemas, and their exact
records and raw/value digests are retained alongside the projection.

`carrier-sha256 = SHA-256(UTF-8(pr-str(carrier)))`. The adapter must serialize
once, validate strict EDN round-trip equality, and hash those same bytes. Map
printing order therefore remains a pinned implementation convention, not a
claim of canonical EDN.

## Joins and continuity

For one transition, prior carrier model, model revision, run and tick equal the
validated transition identity; next carrier has the same model/revision/run,
tick `t+1`, the context's exact next revision, and the next envelope's
`:predecessor/revision` equals the prior carrier revision. Application ID,
feedback event ID, occurrence, action and class remain in the complete
transition subject, not in the slow-state carrier.

For consecutive transitions `T` and `T+1`, all of the following are mandatory:

- `T.next` carrier equals `T+1.prior` carrier byte-for-byte under the pinned
  projection, including model, run, tick, revision and every slow coordinate;
- `T+1` names `T.next` revision as its predecessor and advances exactly one
  tick; and
- the owner-held HEAD transaction digest, generation, revision and carrier
  digest all identify `T.next` before `T+1` can commit.

A matching projection does **not** turn a computed successor into a later
observed/source-authoritative prior. A later prior evidence record requires an
independently retained acquisition/readback record that binds the committed
transaction and carrier bytes to the next transition's seven-source evidence
set. No such production acquisition authority exists. Until it is specified
and reviewed, later-prior construction refuses
`:e6b-adapter/prior-acquisition-authority-unavailable`; it must not copy the
next envelope and relabel it as a prior.

## Deterministic adapter projection

Given successful proposal evidence and an owner-captured HEAD, the future
adapter may construct only:

```clojure
{:prior {:revision <prior carrier revision>
         :transaction-sha256 <owner HEAD transaction digest>
         :state-sha256 <prior carrier digest>}
 :next {:revision <next carrier revision>
        :state <complete next carrier>}
 :application {:application/id <proposal application id>
               :feedback/event-id <proposal event id>
               :transition/subject <proposal exact subject>
               :input/digests <proposal exact seven value digests>
               :output/digest <next carrier digest>
               :status :committed}
 :authority {:verifier/source-sha256 <proposal validator pin>
             :evidence-source-sha256s <proposal exact seven raw pins>}
 :committed-at <proposal fixed destination time>}
```

The adapter retains the complete proposal evidence, both original evidence
records, their source/value hashes, both carriers and carrier bytes. The owner,
not the candidate, supplies an immutable expected-HEAD record containing store
ID, generation, HEAD transaction digest, prior revision and prior carrier
digest. That record is compared while holding the store lease.

Stable retry lookup by application ID occurs before comparison with the current
HEAD: an existing transaction returns only when its retained proposal,
expected-HEAD binding, carriers, pins and fixed time are identical. A changed
retry refuses. For a new ID, every expected-HEAD field must equal the current
owner-held HEAD before commit. Candidate-supplied HEAD, generation, authority,
status, digest or time fields refuse.

## Separation of evidence stages

1. `validate-transition` yields cooperative isolated proposal evidence only.
2. Store compare-and-commit may persist the projection under exclusive
   ownership; it does not upgrade authority.
3. A post-commit capture retains the full chain and original evidence.
4. A separately owned completeness acceptance binds that exact capture.
5. Only then may unchanged retrospective `verify-feedback` check the committed
   application and complete universe.

None of these stages substitutes for another.

## Required rejecting controls for the adapter packet

- prior or next record with an extra, missing, malformed, non-EDN or non-finite
  slow coordinate; projection must not hide it;
- changed map/value bytes, carrier digest, model, revision, run, tick,
  predecessor, mode or any intrinsic coordinate;
- non-consecutive tick or a successor revision already consumed;
- missing/borrowed owner HEAD, stale generation, transaction digest, revision
  or state digest;
- candidate-supplied HEAD/authority/status/time/digest fields;
- same application ID with changed subject, pins, carriers or time, while an
  identical retry returns the existing transaction before current-HEAD checks;
- computed-next relabelled as later prior without independently retained
  acquisition/readback authority;
- loss of either original record or confusion of raw-source, value and carrier
  hashes; and
- post-commit completeness or retrospective success asserted before an
  independently accepted complete capture.

Production roots, writer ownership, later-prior acquisition, completeness,
rollback freshness, runtime wiring and first-install transition remain open.
