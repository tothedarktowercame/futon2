# Row 24 exact selection/enactment event binding

Status: additive structural rejection layer only. It is not byte
authentication, a producer, a successful certificate, or admission.

## Review correction

The accepted run-binding layer proves that divergence occurrence identifiers
are nonempty; it does not prove they are distinct. The lead correction is
accurate. Distinctness would not solve the actual omission: neither occurrence
was joined to an independently fixed selection or enactment event.

## Refinement

`FullCertificateEventBinding` imports `FullCertificateRunBinding` without
changing it. `ExternallyFixedEventPair` supplies independently fixed selected
and enacted event subjects. Each subject contains the complete run/cohort/
attempt identity, occurrence id, semantic action id, source reference and exact
source pin. `ActualEventPair` must equal those subjects exactly.

For an ordinary `SelectionEnaction.match`, action names must agree as before,
but the selected and enacted occurrence subjects remain separate and must each
equal their expected subject. Thus equal semantic action names do not collapse
two occurrences. No divergence authority may accompany this branch.

For `typedDivergence`, the externally supplied divergence subject must contain
the same complete selected/enacted pair, an already allowed delegated class,
the exact grounds and evidence source, and its own independent authority
reference and pin. This layer neither creates a class nor broadens the accepted
class universe.

`EventBoundQualifyingRun` implies both `RunBoundQualifyingRun` and
`FullQualifyingRun`, preserving all earlier rejection clauses and the exact
sole legacy-scalar retirement. Named theorems reject a cross-run ordinary
match, a same-action wrong occurrence, a borrowed divergence occurrence pair,
and an exact subject-pin mismatch.

## External boundaries still open

F11 must authenticate every supplied byte source and construct the fixed run,
event subjects and immutable requirement universes independently of the
candidate certificate. Caller-provided hash strings are only checked for
structural equality here. Exact run-bound joins for all seven record families
and every required connection remain unrepresented. No real successful run is
constructed or claimed.

## Verification

The owned module was compiled individually at mathlib4 commit
`8493dc2ef7725ce353da032a0afa6cf8829cd6ea`. Six retained axiom reports contain
only `propext`, `Classical.choice`, and `Quot.sound`; there is no `sorryAx`.
The compiler emitted three unused-variable linter warnings, retained verbatim;
they do not weaken the propositions. There were no failed proof attempts.
