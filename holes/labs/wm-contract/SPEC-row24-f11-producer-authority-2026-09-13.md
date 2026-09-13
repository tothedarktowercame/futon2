# Row 24 F11 producer authority and acquisition contract

Status: read-only design for the existing F11 emitter. It authorizes no
certificate, accepting fixture, runtime change, or admission.

## Current seam and trust boundary

`derive_certificate.bb` currently reads its caller config at lines 221-228,
hash-checks configured sources at 229-232, validates the seven checkpoint
files at 82-103 and their times at 105-108, then constructs the certificate at
261-294. The bounded integration seam is after `equation-bindings!` and before
the map construction (lines 257-261): resolve all independent authority inputs,
construct the typed subjects required by the additive Lean chain, and refuse
before any EDN/JSON output. `emit!` at 303-309 must remain downstream of every
refusal. This is the final semantic gate, not the start of byte ownership.
The current implementation hashes paths and later reopens them with slurp,
then rehashes some paths while constructing output. A future implementation
must capture each source once before the initial digest loop, parse and resolve
pointers from that retained buffer, and derive all emitted hashes from it.
Adding only a validator at lines 257-261 would leave those races intact.

Caller certificate/checkpoint values are observations, never expected
authority. Expected run identity, node/connection/equation universes, allowed
divergence classes, corrected E1-E6 semantic edges, reviewer principals and
source digests must come from a separately configured authority bundle whose
own path and digest are fixed outside the candidate run. Hashes embedded in a
candidate do not authenticate themselves.

## Source-to-field acquisition

| Typed field | Byte source and current reader | Required independent check |
|---|---|---|
| fixed run/cohort/attempt | route record plus checkpoint identities; current output lines 262-265 | equality to configured expected run authority; reject mixed run/cohort/attempt |
| seven record families | checkpoint files, route/trace, tick-run, close/cohort, dispatch/job, park/continuation, review/admission | strict byte read, SHA-256, schema validation, then exact ordered family summary and member expansion |
| selected/enacted events | selection judgment at 236-242 and construction `:selection-enaction` | exact occurrence/action/source subject and pin; match requires one occurrence even when source records differ |
| equation/declaration claims | registry, Holes bundle and manifest through 170-219 | exact node/equation/declaration/claim scope and registry/declaration/witness byte pins at the fixed run |
| connections | route bytes and existing drawing ledger `control-map-edges.edn` through 148-168 | configured corrected semantic-edge ID, endpoints, classification, evidence pin and typed causal record key |
| deliverable pointers | construction judgment and pointer/value hashes at 271-289 | resolve pointer against the same pinned source bytes; reject absent, duplicate or value/hash mismatch |
| review/admission | independent review/admission record | authorized distinct reviewer, exact claim/witness subject, timestamp and source pin; candidate status booleans are not authority |

The pinned drawing ledger is discovery input, not the independently accepted
corrected E1-E6 universe. Its own header disclaims running-edge status. The
corrected universe must be constructed from the adopted declaration-edge
specification and reviewed mappings without dropping required obligations.

E1-E6 targets must be supplied as the corrected semantic-edge universe, not
inferred by relabelling R numbers. Each target names a composite retained record
key `(family, run, record-id)`. The earlier connection carrier also requires
its historical `from ++ "->" ++ to` identity; the producer must establish an
explicit mapping between that identifier and the corrected semantic edge. A
conflict is typed `:legacy-edge-identity-conflict`, not silently overwritten.

## Cross-record requirements

The producer must resolve exact trace/tick IDs, checkpoint/close IDs,
dispatch/park IDs, and review-claim/witness IDs from their respective bytes.
For the F11 checkpoint lifecycle, timestamp checks are causal, not merely sorted: selection precedes dispatch,
dispatch precedes build/adjudication, closure follows adjudication, and every
joined event lies inside its independently fixed lifecycle interval. This is
not a global ordering imposed on all WM records: E3 review/admission must
precede its pending authorization/enactment, E4 has dispatch-to-launch-to-tick
relations, and E6 feedback follows the exact witnessed outcome. Each relation
needs an explicitly identified event type and lifecycle; historical authorities
may precede the candidate run. Missing timestamp precision,
timezone, or occurrence identity refuses rather than borrowing a nearby row.

One `RecordFamilySubject` is a summary. For each family, the authority bundle
must declare the member-selection rule; the producer enumerates all matching
records, pins every member, checks duplicate composite keys, and emits the
ordered `SummaryExpansion`. Empty required families, unpinned members, changed
bytes, ambiguous IDs, or a member outside the run refuse. Seven summaries are
not evidence of complete multi-tick membership without this expansion.

## Rejecting order

1. Strictly read the external authority bundle and verify its configured pin.
2. Strictly read every candidate source once; retain those byte buffers and
   verify configured digests before parsing.
3. Validate schemas and fixed run identity; refuse missing/mixed identities.
4. Construct immutable node, connection, equation and divergence universes.
5. Resolve all family members, composite keys, pointers and causal/timestamp
   relations; refuse missing, duplicate, cross-run or stale joins.
6. Resolve node/equation review provenance and exact declaration bytes.
7. Resolve selection/enactment and any delegated divergence authority.
8. Construct the additive Lean inputs and run the checker. Only then may the
   existing output map and `emit!` execute.

Typed missing outcomes include `:authority-bundle-missing`,
`:authority-digest-mismatch`, `:record-family-incomplete`,
`:record-member-ambiguous`, `:causal-reference-unresolved`,
`:timestamp-order-unproved`, `:review-provenance-missing`,
`:corrected-edge-unavailable`, and `:legacy-edge-identity-conflict`.

## Remaining blockers

The current emitter implements neither the external authority bundle nor these
seven-family/member acquisitions. Current F11 fixtures exercise an older
partial scope and cannot become a full-run witness. Row 14 independent
categorical acquisition, Row 18 controller/float correspondence, Row 19
self-certification refusal, Row 22 E1-E6 runtime evidence, and the historical
commission 20588 remain unresolved; none is waived or reconstructible here.

## Pins

- `derive_certificate.bb`: `55cc537828e3a4890e268cb07e0590f3ccc1b4feeb7b2acb622baf69268b1c47`
- `SPEC-run-certificate-v1.md`: `dbfdf6a7efbbbf7198202b2f2f26f10e1e7db6f22fe8d9957e0ce17b15207dec`
- `aif-equations.edn`: `2fed9f7c5d4a3c375807dab5e0e3f24c82852bbbd9cef949a2fac8d7c22479da`
- existing drawing-ledger source (not corrected E1-E6 authority): `0c7ee7579701a7bf4d413b54351deef527c7991644a62af801e43ac453b1595f`
- additive Lean sources, oldest to newest: `b1055f63`, `693d48ff`,
  `783ead47`, `8a8e8aef`, `70e4b40d`, `4b212482` (full hashes retained in
  `source-pins.txt`).
