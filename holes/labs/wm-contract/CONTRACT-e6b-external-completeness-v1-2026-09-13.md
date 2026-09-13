# E6b external completeness acquisition and validation contract v1

Status: design only. No acceptance record exists. This contract does not invoke
the retrospective verifier or confer prospective, storage, production,
freshness, or restart authority.

## Distinct byte subjects

Completeness has three distinct representations which must never be conflated:

1. the complete-capture artifact's exact UTF-8 bytes and raw SHA-256;
2. the derived ordered row-vector bytes and `:ledger/derived` SHA-256; and
3. the full ledger-source bytes
   `{:schema/version :wm/e6b-application-ledger-v1
     :scope :isolated-test :entries [...]}` and `:ledger-source/raw` SHA-256.

The unchanged consumer's universe `:ledger/sha256` binds item 3. Item 2 remains
useful representation evidence but cannot substitute for it.

## Completeness subject

A future subject has schema `:wm/e6b-completeness-subject-v1` and exactly:

```clojure
{:schema :wm/e6b-completeness-subject-v1
 :scope :isolated-test-or-production
 :capture {:bytes/base64 ... :raw-sha256 ...
           :store/id ... :owner/generation ... :head-digest ...
           :ordered-chain-digests [...]}
 :ledger-source {:bytes/base64 ... :raw-sha256 ...}
 :row-vector {:bytes/base64 ... :raw-sha256 ...}
 :target {:application/id ... :transition/subject ...
          :transaction-sha256 ... :provenance-sha256 ...}
 :ordered-universe [{:application/id ... :feedback/event-id ...
                     :prior-state/revision ... :transaction-sha256 ...
                     :provenance-sha256 ...}]
 :acquisition {:boundary/id ... :owner/id ... :owner/generation ...
               :writer-inventory-sha256 ... :closed-at ...
               :capture-finished-at ...}}
```

The validator must decode and hash the supplied capture and ledger buffers,
invoke complete-capture readback and pure retrospective projection, and require
every derived field and ordered entry to equal the subject. It must reject any
extra or missing field. `:scope` is fixed by independent resolver configuration,
not copied from the candidate.

## Acquisition is not validation

Validating a supplied genesis-to-HEAD chain proves only internal closure. An
independent completeness reviewer must acquire evidence that this chain is the
whole census for the declared boundary:

- the store owner held its exclusive lifetime lease through capture;
- intake for the declared owner generation was closed and all accepted,
  executing, delivery, and deferred work was reconciled;
- the independently configured writer inventory establishes that every writer
  for this store participated in that boundary;
- HEAD and object bytes were captured after the close/reconciliation point and
  before ownership release; and
- the ordered universe has one entry for every non-genesis transaction and no
  application/event/prior-revision identity outside it for that generation.

The store, capture codec, projection, candidate, and candidate-supplied counts
cannot attest these premises. Current isolated tests own a temporary store and
exercise structural closure, but do not retain an independently reviewed
acquisition event. Production has neither the participating writer inventory
nor the common close/reconciliation boundary.

## Separately owned acceptance

Acceptance has schema `:wm/e6b-completeness-acceptance-v1` and is resolved from
an independently configured authority root. It binds the exact subject raw
SHA-256, reviewer identity, reviewer job/trace or equivalent host-retained
origin, executed review artifact bytes/SHA, acquisition boundary identity,
outcome `:accepted`, and `reviewed-at` after `capture-finished-at`. Reviewer and
acquisition owner must be nonempty and distinct from the proposal/capture
candidate. The accepted review subject must equal the completeness subject
byte-for-byte; an acceptance for a plausible store, target, or ledger is not
borrowable.

A trusted host-retention origin may be used where independently reviewed under
the explicit host trust model; cryptographic signatures are not universally
required. Hash labels, candidate-chosen paths, synthetic fixture roles, and the
permanently missing historical 20588 commission are not host-origin evidence.

## Resolver roles

The later pure validator receives configured, read-only resolvers for exactly:

- `:capture-artifact` — exact serialized capture bytes and external raw pin;
- `:projection` — recomputed, never trusted as a cached candidate assertion;
- `:completeness-subject` — exact subject bytes and external raw pin;
- `:acquisition-boundary` — close/reconciliation and writer-inventory evidence;
- `:review-commission` — exact independently retained review request;
- `:review-execution` — reviewer identity, job/trace join, and terminal result;
- `:review-artifact` — exact review bytes and SHA; and
- `:acceptance` — exact accepted outcome over the subject digest.

Each resolver parses one exact strict UTF-8 EDN buffer, or a strictly specified
host record format, and reports origin, owner, scope, immutable identity, and
read time. Resolver configuration is external to all candidate records.

## Required refusals

The future validator refuses typed errors for: missing roles; malformed or
changed bytes; stale generation/HEAD; reordered, duplicated, omitted, or extra
applications/transactions/provenance; simultaneous omission from capture and
claimed universe; vector/full-source digest substitution; wrong target subject;
borrowed acceptance; reviewer equals candidate/acquisition owner; unexecuted or
rejected review; review before capture; cross-scope resolution; fixture records
under production configuration; candidate-authored authority; missing writer
coverage; and acquisition without a closed/reconciled generation.

Freshness against rollback and completeness of later live history are separate
claims. Even a valid acceptance for this immutable capture does not establish
either, and production must continue to refuse without their authorities.

## Available evidence and exact blocker

Available now: reviewed pure store-v2 structural laws, immutable complete
capture representation, deterministic full ledger-source representation, and
the typed completeness-subject draft, all limited to isolated fixtures with
authority `:none`.

Missing now: a durably retained capture/acquisition pair, independently owned
writer inventory and closed-generation census, independent reviewer commission
and execution over that exact subject, acceptance bytes, production resolver
configuration, installed-code identity, freshness, and later-prior acquisition.

The smallest next executable packet is therefore a pure **rejecting** validator
for the schemas and resolver joins above. Its positive case may use an
explicitly labelled independently constructed isolated fixture; production
mode must unconditionally refuse until real acquisition and review records
exist. It must not invoke `verify-feedback` or mint an acceptance.
