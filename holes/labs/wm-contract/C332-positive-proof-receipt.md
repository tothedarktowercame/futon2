# C332 — one reason-preserving positive-proof receipt (2026-09-01)

`variationalFreeEnergy` is the single migrated binding.  No other positive
witness was migrated.

The reusable `PositiveLeanWitnessReceipt/v1` implementation records and
checks:

- declaration-slice hashes for the complete positive witness structure,
  fixture object, and theorem statement/body;
- declaration-slice hashes for the named semantic basis in `Holes.lean`;
- the independent EDN fixture's byte digest and schema;
- an explicit field mapping from the EDN fixture to the Lean
  `gaussianReference` object;
- Lean version, `lean-toolchain`, and `lake-manifest.json` identities; and
- live elaboration and `#print axioms` results.

The adapter is explicit rather than inferred: `channel-count`, `precision`,
`prediction-error`, and `expected-variational-F` map to the four fields of
`GaussianReference`.  The checker verifies the recorded fixture values and
pins the complete Lean adapter declaration.

## Controls

The weakened-statement control operates on a copy.  It replaces the exact
Gaussian conclusion with the reflexive statement that the computed value
equals itself.  That weakened theorem still elaborates, but the receipt fails
with `:positive-source-drift`.  Thus the control tests more than proof
breakage.

The unrelated-edit control appends text outside every pinned declaration in
the same witness file.  The receipt remains valid.  This demonstrates the
locality difference from whole-file hashing.

Both controls are standing workspace-gate commands.  The authoritative
`VariationalFreeEnergy` witness fragment names the receipt and both semantic
purposes; regeneration preserves that metadata.

## Cost

For this first binding the data cost is six declaration slices (three witness,
three semantic dependencies), one fixture digest, four adapter mappings, and
three toolchain/result identities.  A normal validation takes one Lean
elaboration (about 2–4 seconds in this checkout).  The weakened control adds a
second elaboration of the copied weakened theorem; the positive, weakened,
and unrelated suite took about 13 seconds together.  Authoring the first
schema required a small reusable checker plus the explicit adapter; subsequent
bindings should mostly be receipt data and an adapter declaration, but that
claim should be measured on the second migration rather than assumed.

The existing scope limit remains: pinning a definitional/projection witness
prevents silent weakening but cannot make it evidence of downstream
behaviour.
