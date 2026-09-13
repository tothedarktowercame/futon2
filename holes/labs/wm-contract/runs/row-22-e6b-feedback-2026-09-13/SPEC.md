# E6b isolated feedback verifier

The lead correction in `../row-22-e6-spec-review-2026-09-13/lead-review.md`
was independently checked against the implementation sources before work. It
correctly identifies two defaults that this boundary must reject before calling
the production function: missing `:fast/succeeded?` becomes failure at
`temporal_hierarchy.clj:216`, and a missing prior class becomes `fresh-entry` at
`:214-215`. It also correctly limits exactly-once evidence to verification of
an externally complete ledger, not storage enforcement.

Pinned computation dependencies:

* `src/futon2/aif/temporal_hierarchy.clj`, SHA-256
  `e3e532ae1b0b123730299bd7caa1105b074b7d27912c5508d29c395f21d34eef`:
  `advance-slow-state`, next-mode derivation and result shape.
* `src/futon2/aif/intrinsic_values.clj`, SHA-256
  `ea07fb662fed93e801e613a102f35f7baa3c3053fd636d478d14e504a1be758b`:
  transitive Beta update through `next-update-record`.

Ten source roles are required from one externally configured root with exact
SHA-256 pins: fixed transition context, complete prior state, exact authorized
E2b subject, an independently fixed E3-to-E2b lifecycle relation,
independently witnessed terminal outcome, its exact-subject
independent review, the review's retained artifact bytes, claimed next state,
application ledger and independently complete application universe. Each file
is read into one byte buffer, hashed and parsed as strict single-form UTF-8 EDN.
Production mode refuses unconditionally.

The verifier requires `t+1`, explicit prior and next revisions, a prior entry
for the exact outcome class, boolean success consistent with terminal status,
distinct outcome producer/reviewer identities and an E3-authorized exact E2b
occurrence/action. The externally configured canonical E3 and E2b resolver
inputs are replayed through their actual pure verifiers; their complete outputs
must match digests in the E6b subject. Their complete model/revision/run/tick,
cohort/event identities and ordered E1 field-pin/approved-domain subject must
equal the fixed transition context. A reference label alone is insufficient.
It invokes the actual pure update and compares the entire
next state. The external universe must name exactly the application expected
for this feedback event, bind the full fixed transition subject and exact ledger
source digest, and enumerate every ledger application ID in order. Duplicate
IDs and any other application reusing the event or prior revision refuse. The
ledger must contain exactly one committed entry for this transition whose input
and output digests match. Replay verifies the same committed bytes without
applying another update.

The outcome review must bind the full terminal outcome subject, independently
configured reviewer and observer identities, and exact retained review artifact
bytes. The review artifact repeats the subject and execution facts. Prior-state
time precedes terminal outcome time, which is no later than review time; review
must precede the deterministic destination-state time. Plausible identifiers or
64-character strings without those resolved bytes are not authority.

E3 authorization and E2b selection/enactment retain distinct cohort/event
identities. The verifier does not force them equal. The separately resolved
lifecycle relation instead binds both complete canonical contexts and result
digests, their ordered field subject, and the occurrence/action/class. It also
binds the enactment instant and the observer's origin/authority reference to
the exact outcome subject. Every prior intrinsic timestamp must parse; the
fixed prior timestamp is the latest entry timestamp, strictly before enactment,
which is no later than terminal outcome. Thus malformed entries cannot vanish
from chronology through filtering.

This does not persist anything or prove that a live store prevents duplicates.
No actual independently owned outcome authority, complete application universe,
state revision store, ledger completeness authority, runtime consumer or
qualifying production transition is available.
