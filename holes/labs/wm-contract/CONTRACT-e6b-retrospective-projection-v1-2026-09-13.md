# E6b pure retrospective projection v1

Input is exactly an externally pinned serialized complete-capture artifact and
one nonblank target application ID. The projection first invokes complete
capture readback, then resolves every ordered five-field index entry through
its exact transaction and provenance descriptors.

The derived ledger has exactly six fields per row: application ID, event ID,
prior revision, committed status, the exact four-key retrospective input digest
view, and complete-next-record output digest. It is encoded deterministically
and labelled derived; it is not an original source byte stream. The target's
complete next-state record (the proposal's `[:next :state]`, not its wrapper)
is independently encoded the same way and its raw digest
must equal the transaction application's output digest.

The projection retains the target's seven original source descriptors, full
canonical input/output closure, carrier projection, distinct transition
subject, transaction/provenance descriptors, capture/head/chain identity, and
separate raw-capture, derived-ledger, derived-next-record, and carrier hashes.

Output status is `:structural-projection-only`, authority is `:none`, restart is
false, and completeness is the typed refusal
`:e6b-retrospective/completeness-authority-unavailable`. A later independently
owned record must bind the exact capture raw hash, derived ledger bytes/hash,
target transition subject, and reviewed outcome before the unchanged ten-source
retrospective verifier can be called. This module neither creates that record
nor invokes the verifier.

## Lead consumer-boundary clarification

The current `:application-ledger` descriptor encodes only the ordered row
vector. It is a draft projection, not yet the unchanged verifier's ledger
source. That source must be the complete map
`{:schema/version :wm/e6b-application-ledger-v1 :scope :isolated-test :entries rows}`.
A bounded follow-up must deterministically encode this map and retain its raw
source digest separately from the row-vector digest. Future completeness
`:ledger/sha256` must bind those full source bytes; accepting the vector digest
alone is insufficient. This clarification grants no completeness authority,
filesystem staging, retrospective success, or production permission.

## Additive ledger-source envelope

The projection now retains both representations. `:application-ledger` remains
the original ordered row-vector descriptor and `:digest-roles :ledger/derived`
keeps its original meaning. `:application-ledger/source` encodes the complete
consumer record, while `:digest-roles :ledger-source/raw` names that distinct
raw SHA. Neither digest is silently repurposed.

`:completeness-subject/draft` binds the capture raw SHA, full ledger-source raw
SHA, and exact target transition subject. Its authority is explicitly `:none`;
it is the subject an independent reviewer must accept, not an acceptance.
