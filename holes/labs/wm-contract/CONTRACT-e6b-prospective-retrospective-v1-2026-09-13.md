# E6b prospective/retrospective composition contract v1

Status: design only. This contract does not change or weaken
`verify-feedback`, does not authorize a store write, and provides no production
authority.

## Why the boundary is split

The current verifier resolves all ten sources before replay
(`machine_slow_feedback_evidence.clj:94-122`) and requires an already committed
matching application at lines 281-309. Its success is necessarily
retrospective. Supplying a synthetic committed entry to obtain permission for
that same commit would assume the fact under test.

The implementation must therefore expose two operations with different source
sets and result types:

1. `validate-transition` checks an exact prospective transition but returns
   only proposal evidence.
2. `verify-committed-transition` adapts independently approved, post-commit
   store evidence to the existing `verify-feedback` input and calls that
   verifier unchanged.

## Prospective validation

The prospective source roles are the existing context, prior state, E2b
subject, lifecycle relation, outcome authority, outcome review, and exact
review-artifact bytes. The next state is recomputed, never accepted from the
candidate. Application-ledger and application-universe are forbidden inputs.

Validation must reuse the same checks currently performed before the ledger
block: complete model/revision/run/tick and t+1 context; canonical E3 and E2b
replay and exact digests; ordered field subject; occurrence/action/class;
review/admission < authorization <= enactment <= terminal <= review <
destination; complete timestamped prior; boolean terminal success; exact
outcome review and artifact; and the actual `advance-slow-state` computation
(`machine_slow_feedback_evidence.clj:123-283`). Moving shared checks into a
private pure function is permitted only if the existing verifier calls it and
all existing refusals remain.

The output is exactly:

```
{:schema :wm/e6b-transition-proposal-evidence-v1
 :scope :isolated-test
 :status :prospectively-validated
 :identity {:model/id ... :model/revision ... :run/id ...
            :source/tick-index t :destination/tick-index t+1}
 :transition/subject <the complete subject presently built at lines 274-280>
 :application/id <fixed context id>
 :feedback/event-id <fixed context event>
 :prior {:state/revision ... :state <complete prior state>
         :state-sha256 <digest of canonical strict EDN>}
 :next {:state/revision ... :state <complete recomputed next record>
        :state-sha256 <digest of canonical strict EDN>}
 :input/digests {:context ... :prior ... :e2b ... :lifecycle-relation ...
                 :outcome ... :outcome-review ...
                 :outcome-review-artifact ...}
 :canonical/digests {:e3 ... :e2b ...}
 :committed-at <exact destination/as-of from context>
 :validator {:source-sha256 ...
             :dependency-sha256s {:temporal-hierarchy ...
                                  :intrinsic-values ...}}
 :authority/status :proposal-evidence-only}
```

Every digest is computed from the same resolved byte buffer or canonical EDN
value actually checked. `:committed-at` is fixed before storage, so retry bytes
are stable. The candidate may point at configured evidence identities but may
not supply a resolver root, validation status, source digest, clock value, or
authority root.

This record establishes that the proposed state follows the declared law at
the pinned inputs. It does not establish that those sources are production
authorities, that the proposal was committed, or that the application universe
is complete.

## Owner-held compare and commit

The composition adapter accepts the proposal evidence only from an injected,
configured validator call. While holding the store's lifetime lease and
process lock, it reads and validates HEAD. It requires:

- HEAD store identity and generation match the configured store;
- HEAD state revision equals the proposal prior revision;
- HEAD state digest equals the proposal prior state digest;
- the stable application/event/revision identities are unused; and
- every validator/dependency/source pin is retained in the transaction.

Only the store supplies HEAD transaction digest and generation. The adapter
deterministically projects the proposal into the store transaction proposal;
there is no candidate callback between comparison and publication. The joint
HEAD publication remains the store's existing atomic state/application commit.
An identical stable-ID retry returns the exact committed transaction. Changed
time, pins, input, next state, application id or event is a conflict.

The store transaction must label these pins as provenance, not convert
`:authority/status :proposal-evidence-only` into independent authority.

## Retrospective verification and completeness

After commit, capture obtains the complete genesis-to-HEAD chain and ordered
application index under store ownership. A separately owned resolver supplies
an acceptance record binding exact store id, generation, HEAD digest, every
chain-object digest, ordered application universe, writer inventory, source
pins, reviewer identity and accepted outcome. The store, validator, proposal
candidate and composition adapter cannot mint that acceptance.

Only then may a read adapter construct the existing `:application-ledger` and
`:application-universe` source records from exact captured transaction bytes.
The universe authority names and byte-pins the independent acceptance. The
adapter supplies all original source bytes plus these two post-commit sources
to unchanged `verify-feedback`. A positive result is a retrospective replay of
one committed application in an independently accepted complete universe.

Local capture still cannot prove freshness against rollback. Production mode
continues to refuse until separately reviewed production source authority,
writer ownership, completeness acceptance and freshness policy exist.

## Required controls

Prospective controls must reject ledger/universe inputs, candidate-supplied
status/root/digests/time, missing or altered canonical E3/E2b inputs, borrowed
outcome review, changed chronology, missing prior class/timestamp, non-boolean
success, changed computed next state, and any non-EDN value. The old
retrospective verifier tests must remain unchanged and passing.

Composition controls must reject stale HEAD revision, same revision with a
different state digest, changed proposal after validation, reused
application/event/revision, validator source-pin mismatch, and an injected
candidate boolean in place of a validator result. Faults before publication
leave the old HEAD; uncertain publication poisons the owner; stable retry
returns one transaction.

Retrospective controls must reject missing/rejected/self-authored/borrowed
completeness acceptance, incomplete/reordered/extra application universes,
changed chain bytes, wrong HEAD generation/digest, omitted original source,
and a post-commit record borrowed from another proposal. A positive fixture
must demonstrate: prospective validation with no ledger, one store commit,
independent fixture acceptance issued afterward, then unchanged retrospective
verification.

## Still unresolved

No production source authority, common writer ownership, completeness reviewer,
rollback-freshness authority, first-install fence, runtime consumer, or
qualifying retained transition currently exists. This contract neither
constructs nor approves any of them.

