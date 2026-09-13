# Row 24 resolved record and edge evidence

Status: additive structural rejection only; no successful certificate,
authentication, runtime change, or admission.

`FullCertificateResolvedEvidence` repairs the ignored-reference defect. Each
actual record must equal its independently fixed expected record byte-for-byte
at the typed Lean value: family, complete run identity, record id, evidence
source, source pin, and a family-indexed applicable reference. The reference
sum has distinct constructors for checkpoint, trace, tick-run, close/cohort,
dispatch/job, park/continuation, and review/admission schemas; each constructor
requires its own two identifiers nonempty and must correspond to its family.
The ordered actual and expected family lists both remain the frozen seven-item
census.

Each actual connection likewise equals its independently fixed corrected-edge
subject. Edge authority is an opaque `semanticEdgeId` plus separately checked
connection id, endpoints, classification, run, evidence source and pin; it is
not derived from concatenating endpoints. Its nonempty causal record id must
resolve to an exact member of the actual seven-family record census.

Named theorems reject a wrong nonempty applicable reference, same source string
with changed bytes, a cross-run borrowed record, an unresolved nonempty causal
record id, and a wrong corrected-edge subject. The refinement implies the full
predicate through every earlier layer and preserves the sole exact legacy
scalar retirement.

F11 still owns strict byte reading/hashing, independently constructing the
expected values and corrected E1-E6 universe, and proving that source pins and
semantic edge ids came from the authoritative schemas. Lean equality does not
authenticate caller-supplied values. Cross-family relations beyond each typed
pair—such as exact timestamp ordering and payload-pointer resolution—remain
external producer obligations. No current run is asserted to populate all
seven families or corrected edges.

The single targeted compilation at mathlib4 commit
`3616df4e28d7b0d5ff4a0902606d170fbb1915b6` exited zero. Six axiom reports
contain only `propext`, `Classical.choice`, and `Quot.sound`; no `sorryAx` and
no failed attempt.
