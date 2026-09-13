# Row 24 exact-run binding refinement

Status: additive rejection interface; not a certificate, producer, admission,
or successful full-run witness.

## Construction

`DarkTower.WarMachine.FullCertificateRunBinding` imports the accepted
`FullCertificatePredicate` and leaves `CertificateStates` unchanged.  An
`ExternallyFixedRun` identifies one run by run/cohort/attempt identity and an
authority record pin.  The Lean predicate checks equality to this input; it
does not authenticate the strings or hashes and does not permit the candidate
attestation to choose the fixed run.

For every required node the producer supplies a `NodeClaimJoin` containing the
exact run, node subject, claim id, scope, claim bytes and review bytes.  The
join must equal a `supportedAtRun` entry in the existing attestation.  For every
required equation it supplies an `EquationClaimJoin`; node, equation and Lean
declaration subjects must equal the requirement, and the complete binding must
equal an existing `EquationBindingState.exact`, including all three byte pins.
List alignment is occurrence-preserving and ordered; it does not turn the
requirements into sets.

A typed divergence additionally requires a `DivergenceAuthorityRecord` bound
to the fixed run, distinct selected and enacted occurrence ids and action ids,
the exact divergence class/grounds/evidence fields, and an external authority
record pin.  An ordinary exact match requires the divergence authority slot to
be empty.  The existing exact sole legacy-scalar retirement remains unchanged;
it does not retire R8 or live `F_pi`.

The named theorems reject cross-run node reuse, cross-run equation reuse,
mismatched declaration subjects, borrowed divergence authority, and an
unauthorized divergence class.  `runBound_implies_full` preserves every prior
`FullQualifyingRun` rejection theorem.

## Inputs still owed by the F11-owned producer

The external producer must construct, from independently read and hashed
bytes rather than candidate booleans:

1. the immutable required-node, required-connection, required-equation and
   allowed-divergence-class universes;
2. the fixed run identity and its authoritative record pin;
3. exact claim, independent-review, registry, declaration and witness byte
   pins and their subject identities;
4. selected and enacted occurrence identities plus an independently authorized
   typed-divergence record when they differ;
5. the seven record-family joins and their cross-ledger consistency evidence;
6. fired/validated connection evidence at the same run.

This module does not yet represent record-family identity joins or
connection-evidence run joins; those remain external requirements rather than
being hidden behind successful strings.  No positive production fixture is
provided, so no full system is certified.

## Verification

At mathlib4 commit `1cfaf67474c27fc8ed81132a1953c19c231a323f`, the owned
module elaborated and its emitted `.olean` was produced individually with Lean
4.31.0-rc1.  Six axiom reports contain only `propext`, `Classical.choice`, and
`Quot.sound`; no `sorryAx` or additional axiom appears.  There were no failed
proof attempts.  Raw outputs and exact pins are retained under
`runs/row-24-run-binding-2026-09-13/`.
