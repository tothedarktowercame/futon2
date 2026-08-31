# C109 — R2 and R9 source discharge

Date: 2026-08-31

## Census

All four declarations are dischargeable; none is false or blocked.

- `wmTraceR2` is the immutable 801-form source snapshot at `b2c3aeb4…`.
  Its extensional compression is valid because the contract consumer observes
  channel presence only: two annotation-health absences and 799 complete rows.
- `r2ContractCensusWmTrace` applies `native_decide` to that source object.  It
  does not prove a generated sibling.
- `r9VerdictConsultsChecker` already has a constructive proof in `Holes.lean`.
  Empty carriers make its membership premise unavailable rather than refuting
  the result; the supplied false checker demonstrates that the checker argument
  is load-bearing.
- `r9WmVerdictsSound` simplifies the source `wmVerdictsDeclared` table directly.
  The older generated-sibling theorem is not used.

None has a degenerate-universal counterexample, and no source/generated equality
is assumed.  The source definitions and proofs predated this delivery; C109
corrects their stale HOLE documentation and emitter classifications to
`mkWitnessedClosed`.

## Controls

- R2 pin mutation and census mutation are independently rejected by
  `r2_pinned_snapshot_witness.clj`.
- R9 proof-receipt absence and tampering are independently rejected by
  `r9_proof_receipt_check.clj`.
- R9 soundness rejects changing an inside producer's verdict to `independent`
  via `r9_independence.clj --negative-sound`.
