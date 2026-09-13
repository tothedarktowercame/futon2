# Row 24 full-predicate rejection precursor — 2026-09-13

Status: additive mathematical specification and rejection proofs only. No full
system certificate, emitter change, registry change, or successful run term.

## Existing carrier and explicit extension

`CertificateStates.lean` remains the typed census authority. Its
`CensusComplete` checks ordered node/connection populations and all seven record
families, while retaining partial/refuted/absent states. Its older
`qualifyingRun` does not require positive records, fired connections, acceptable
selection/enaction, or declaration bindings; the retained lead counterexample
elaborates precisely because those checks are absent.

The new `FullCertificatePredicate.lean` composes, rather than replaces, that
census with these generated inputs:

| Obligation | New typed input and predicate |
| --- | --- |
| Requirement universe | `FullScopeRequirements`, byte-pinned externally; exact required node, connection and equation lists |
| Nodes | declared universe equality, no duplicates, every entry `supportedAtRun` |
| Connections | declared universe equality, no duplicates, every required entry `firedClassified` with evidence |
| Records | every one of the existing seven families `presentAndConsistent` with evidence |
| Selection/enaction | exact nonempty match, or unequal typed divergence in an externally ruled class with grounds/evidence and an exact authority pin |
| Equations | exact ordered required equation/declaration identities, each joined to nonempty run scope and claim plus registry/declaration/witness byte pins |
| Negative scope | exactly one obligation-specific legacy R8 scalar retirement with the delegated ruling and original J2 pins |

There are no whole-node exemptions. The permitted retirement does not retire R8
and does not waive live `F_pi`. All other partial, absent, refuted, unfired,
unbound, missing, or unruled values remain visible in the census and fail full
qualification.

## External evidence boundary

Lean decides equality and the finite positive-state predicates. It does not
read files or authenticate a producer merely because a string contains 64
characters. The F11-owned producer still must derive the requirement universe,
hash actual bytes, establish claim scope at this run, validate record-family
joins, and supply the ruled divergence/retirement authorities. `ExactBytePin`
therefore records expected and actual digests and the Lean predicate requires
equality; construction of those values remains an external witness dependency.

No success fixture is supplied. The module proves rejection of the already
compiled lead counterexample and named failures for a missing record family,
unclosed node, unfired edge, unequal action under the match constructor, absent
equation binding, mismatched declaration identity, and unauthorized negative
scope. These are the row-24 breach-detector precursors, not a full-run claim.

## Remaining construction boundaries

The final producer/checker still needs real per-run records for all seven
families; exact required node and corrected declaration-level connection
universes; scope-applicable positive claims; every equation/declaration/witness
join; selected/enacted evidence or ruled divergence; and the two exact legacy
retirement authority pins. Row 14 real acquisition, Row 18 controller/source/
float obligations, and Row 22 E2–E6 are not waived by this module.
