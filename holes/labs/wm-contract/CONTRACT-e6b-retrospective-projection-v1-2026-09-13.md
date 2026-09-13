# E6b pure retrospective projection v1

Input is exactly an externally pinned serialized complete-capture artifact and
one nonblank target application ID. The projection first invokes complete
capture readback, then resolves every ordered five-field index entry through
its exact transaction and provenance descriptors.

The derived ledger has exactly six fields per row: application ID, event ID,
prior revision, committed status, the exact four-key retrospective input digest
view, and complete-next-record output digest. It is encoded deterministically
and labelled derived; it is not an original source byte stream. The target's
complete next record is independently encoded the same way and its raw digest
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
