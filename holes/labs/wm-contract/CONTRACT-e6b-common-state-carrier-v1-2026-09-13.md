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
before projection, a future adapter must validate the complete prior schema and
complete computed-next schema: exact key sets, identity types, intrinsic entry
keys/types/timestamps, predecessor/event joins, and every nested state field
produced by `advance-slow-state`. Current selective core checks do not establish
this complete-schema property. Exact original records and raw/value digests are
retained alongside the projection.

The constructor emits an `array-map` in exactly the key order shown above;
nested maps preserve checked parsed iteration order. It serializes exactly once
as UTF-8 `pr-str`, validates strict EDN round-trip equality, retains those bytes,
and defines `carrier-sha256 = SHA-256(retained-carrier-bytes)`. Re-serialization
is not an identity check. This is a pinned convention, not canonical EDN.

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
               :input/digests {:context <value digest of :context>
                               :prior <value digest of :prior-state>
                               :e2b <value digest of :e2b-subject>
                               :outcome <value digest of :outcome>}
               :output/digest <value digest of complete computed-next record>
               :status :committed}
 :authority {:verifier/source-sha256 <proposal validator pin>
             :evidence-source-sha256s <proposal exact seven raw pins>}
 :committed-at <proposal fixed destination time>
 :provenance-sha256 <immutable provenance object digest>}
```

The four-key map is the explicit compatibility view expected exactly by
unchanged retrospective `verify-feedback` (`machine_slow_feedback_evidence.clj:
390-419`). The other three prospective value digests remain mandatory in
provenance. Likewise, application output names the complete next-record value
digest, while HEAD continuity names the next carrier digest. Both subjects and
both digests are retained and cross-bound; neither is copied into the other's
field. The owner, not the candidate, supplies the expected HEAD.

Stable retry lookup by application ID occurs before comparison with the current
HEAD: an existing transaction returns only when its retained proposal,
expected-HEAD binding, carriers, pins and fixed time are identical. A changed
retry refuses. For a new ID, every expected-HEAD field must equal the current
owner-held HEAD before commit. Candidate-supplied HEAD, generation, authority,
status, digest or time fields refuse.

## Durable provenance and atomic relation

The current strict transaction/proposal key sets
(`machine_slow_feedback_store.clj:84-96,219-238`) cannot retain this material
and are not silently extended. Implementation requires a content-addressed
`:wm/e6b-transition-provenance-v1` containing the complete proposal evidence;
all seven original source records, exact base64-encoded bytes, raw and value
digests; complete prior and computed-next records; both carriers, retained
carrier bytes and carrier digests; the four-key retrospective input view; the
complete-next-record output digest; and the owner-issued expected HEAD (store,
generation, transaction, revision and prior-carrier digest).

Every byte field is decoded, hashed, strictly parsed from that same buffer and
compared with its retained record/value digest. Every duplicated field must
agree. The provenance object is strict-round-tripped, serialized once and
content-addressed. A strict `:wm/e6b-state-transaction-v2` and proposal schema
add exactly one mandatory `:provenance-sha256`; recovery refuses an absent,
corrupt, schema-invalid or disagreeing provenance object.

Publication order is: sync and no-overwrite-publish provenance; sync and
no-overwrite-publish its transaction; atomically replace and directory-sync
HEAD. A pre-HEAD crash may leave unreachable immutable objects but cannot expose
a reachable transaction without provenance. Recovery follows HEAD and validates
transaction then complete provenance before returning; capture includes both
objects. Uncertain publication poisons the owner. Stable retry first resolves
the existing application transaction and provenance and requires exact bytes
and digests; only a new ID proceeds to current-HEAD comparison. This is durable
restart evidence, not a memory-only sidecar.

## Storage-to-retrospective mapping

After independent completeness acceptance, a read adapter must:

1. validate the v2 transaction and provenance bytes;
2. emit the original seven records from retained bytes and the complete
   computed-next record as `:next-state`;
3. emit exactly `{:application/id ... :feedback/event-id ...
   :prior-state/revision ... :status :committed :input/digests ...
   :output/digest ...}` using the transaction prior revision, four-key
   compatibility input view and complete-next-record output digest. Do not
   copy the store-only `:transition/subject` into this exact ledger schema;
4. require transition/application/event/revision fields to equal both the
   transaction and proposal evidence; and
5. accept the universe only from a separate completeness authority binding the
   full capture and ordered application index.

Re-running unchanged `verify-feedback` also requires its configured canonical
E3/E2b inputs and their transitive pinned evidence, not only the two canonical
output digests. Durable provenance must retain or independently resolve that
exact input closure with authenticated configuration bindings. The seven
transition records alone are insufficient for restart replay. Missing canonical
input closure refuses; no default anchor, borrowed current configuration, or
synthetic review may fill it. This closure and store-v2 implementation remain
separate from the first pure carrier codec.

Carrier digests serve HEAD continuity; original record-value digests serve
unchanged retrospective replay. Both subjects are retained and joined.

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
  hashes;
- seven-role digests copied into the four-key ledger view, or a carrier digest
  copied into the complete-next output field;
- missing/corrupt/orphaned provenance, transaction/provenance disagreement,
  HEAD reaching a transaction before provenance is durable, or capture omitting
  provenance bytes; and
- post-commit completeness or retrospective success asserted before an
  independently accepted complete capture.

Production roots, writer ownership, later-prior acquisition, completeness,
rollback freshness, runtime wiring and first-install transition remain open.

## Actual store-v2 to retrospective reconstruction (2026-09-13 refinement)

This section refines, rather than replaces, **Storage-to-retrospective
mapping** above. It is pinned to the exact sources listed in
`storage-retrospective-source-pins.edn`.

### Bytes actually available

`machine-slow-feedback-store-v2/capture` returns transaction bytes keyed by
every ordered chain digest (including genesis), provenance bytes keyed by each
reachable provenance digest, the ordered `:application-universe`, generation,
and `:head-digest` (`machine_slow_feedback_store_v2.clj:355-370`). Capture now
also returns `:head-object`, whose Base64 bytes are made from the exact single
buffer that `recover*` parsed and hashed; it does not reopen HEAD after
validation. Its `:source-sha256` must equal `:head-digest`, and decoding and
hashing those bytes must reproduce both. A digest label without these bytes
would not be a byte witness.

For every non-genesis transaction, recovery has already followed its
`:provenance-sha256`, hashed the exact provenance buffer, and invoked
`provenance/readback` (`store_v2.clj:184-194,256-263`). Readback discards all
cached parsed `:record` values and reconstructs from the encoded original
descriptors (`machine_slow_feedback_provenance.clj:259-300`). Thus the durable
provenance contains:

- all seven original transition-source descriptors and records under
  `[:original-sources]`, retaining distinct raw-source and parsed-value hashes;
- both canonical E3/E2b output descriptors and the full canonical input closure
  under `[:canonical-closure]`, including distinct E3 and E2b lifecycle/event
  identifiers rather than forcing them equal;
- the complete proposal, prior and computed-next records, both carrier
  projections and carrier hashes; and
- the owner-supplied expected parent HEAD and fixed transition time.

### Deterministic pure projection

Given strict capture bytes (including the retained HEAD object) and an external
completeness acceptance, the smallest future adapter has one behavior:
`capture -> verify-feedback input`. It must, in this order:

1. Strictly decode the HEAD from one buffer, require its raw SHA-256 equals
   `:head-digest`, and require store id/generation/current transaction and state
   joins identical to `store-v2/recover` (`store_v2.clj:221-276`).
2. Strictly decode every transaction in `:chain-digests`, hash the same buffers,
   follow every provenance digest, and call `provenance/readback` on the exact
   captured provenance bytes. Missing, extra, unreachable, or unlisted objects
   refuse; the adapter may not consult the current filesystem.
3. For the target transaction, emit the unchanged retrospective application
   ledger entry by selecting exactly
   `{:application/id :feedback/event-id :prior-state/revision :status
     :input/digests :output/digest}` from the transaction application. The
   transaction-only `:transition/subject`, `:transaction-sha256`, and
   `:provenance-sha256` remain provenance and must not enter that exact ledger
   row. `:input/digests` must have exactly `[:context :prior :e2b :outcome]`;
   `:output/digest` is the complete computed-next-record value digest, never the
   carrier or transaction digest (`provenance.clj:234-250`).
4. Recreate the ten-source input required by unchanged `verify-feedback`
   (`machine_slow_feedback_evidence.clj:368-419`): seven exact original record
   bytes from provenance; `:next-state` from the retained complete computed-next
   record, not the next carrier; the ordered full six-field application ledger;
   and a separately accepted complete application universe. Canonical E3/E2b
   configuration must come from the retained closure, never working defaults.
5. Preserve the captured application order exactly. Each capture index row has
   exactly five keys: application ID, feedback event ID, prior-state revision,
   transaction digest, and provenance digest. Removing the last two produces
   only three fields, not a retrospective ledger row. Resolve each indexed
   transaction and provenance by those exact digests; join all three index
   identities to its application and derive the six-field row in step 3 from
   that transaction application. Require exact index schemas, unique ordered
   membership, and one-to-one coverage of all non-genesis chain transactions.
   Never invent status or input/output digests from an index row.

### External completeness subject

The store returns `:completeness-authority :absent` and
`:restart-authorized? false`; it cannot approve its own census. The required
external record must be independently owned and bind at least: format
`:wm/e6b-store-capture-v2`, store id, owner generation, exact HEAD raw digest,
an exact digest of the serialized complete capture, ordered chain digests,
ordered application IDs with their transaction and provenance digests, and an
accepted review outcome over that exact subject. A candidate boolean, count,
or copied application index refuses. No such production acceptance exists.

### Remaining representation and authority gaps

- HEAD bytes are retained from the validated recovery buffer. Their descriptor
  is immutable data; transaction/provenance byte arrays still require explicit
  encoding rather than `pr-str` object identities in any future canonical
  complete-capture serialization.
- No independently owned completeness/freshness acceptance exists; the store's
  `:absent` marker is honest and mandatory.
- Provenance is structural isolated-fixture evidence (`:authority/status
  :none`), not authenticated production evidence or installed-code identity.
- A computed successor remains a computed successor. Nothing in capture turns
  it into the independently acquired prior record required for the next
  transition; that still refuses
  `:e6b-adapter/prior-acquisition-authority-unavailable`.
- Production store ownership, first-install fencing, runtime wiring, and the
  permanently absent historical 20588 commission remain outside this mapping.

After review of this contract and the HEAD-buffer packet, a pure reconstruction
adapter is the next separable executable behavior. It still cannot report a
retrospective success without an externally supplied completeness acceptance,
and it must never synthesize one.
