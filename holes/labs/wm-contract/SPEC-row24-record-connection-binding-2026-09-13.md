# Row 24 record-family and connection binding

Status: additive structural rejection layer; no byte authentication, successful
certificate, runtime change, or admission.

`FullCertificateRecordConnectionBinding` extends the accepted event-bound
predicate. Its expected record list must have exactly the frozen seven-family
order: full-loop checkpoints, WM trace, tick-run, close/cohort, dispatch/job,
park/continuation, and review/admission. The attestation family order must be
the same. Every expected subject carries fixed run/cohort/attempt identity,
record id, exact evidence-source string, source pin, and typed causal-reference
slots. The matching `presentAndConsistent` constructor must carry that exact
evidence-source string; omissions, extras, duplicates, and reordering cannot
satisfy the two list equalities.

The expected connection list and attestation connection list must both equal
the externally fixed `FullScopeRequirements.requiredConnections` list in
order. Each connection subject supplies explicit from/to endpoints, requires
the canonical `from ++ "->" ++ to` identity, fixed run, classification,
evidence source, source pin, and nonempty causal record id. The exact
`firedClassified` entry must occur in the attestation.

Named theorems reject cross-run family/edge reuse, a missing family, mismatched
family evidence source, wrong endpoint or classification, and an empty/stale
causal reference. The new predicate implies `EventBoundQualifyingRun` and
`FullQualifyingRun`, retaining every earlier rejection and the sole exact
legacy scalar retirement.

## Represented and external joins

Represented in Lean are ordered census identity, run/cohort/attempt equality,
record and edge subject identity, exact evidence-source equality,
classification, endpoint composition, source-pin validity, and a required
edge causal-record reference. The typed record reference slots name the actual
producer relations: tick id, trace run id, close attempt id, dispatch job id,
and review claim id.

Their family-specific applicability and cross-ledger equality remain external:
the frozen attestation stores only an evidence-source string and therefore has
no actual byte pin or structured causal references to compare. F11 must resolve
that source independently to the supplied pin, enforce which reference slots
each producer schema requires, authenticate the immutable node/connection/
equation universes, and construct the expected subjects outside the candidate
certificate. This module does not pretend empty inapplicable fields establish
coverage and does not claim all E1-E6 producer records currently exist.

The first compile failed on six conjunction projections; the second failed on
the endpoint-equality projection. Both raw failures, including temporary
`sorryAx` reports caused by elaboration errors, are retained. After two
follow-up commits, the final targeted compilation at mathlib4
`0f02c59e6f6590798730a3ebaa525e75d8024e95` exited zero. Nine final axiom
reports contain only `propext`, `Classical.choice`, and `Quot.sound`, with no
`sorryAx`.
