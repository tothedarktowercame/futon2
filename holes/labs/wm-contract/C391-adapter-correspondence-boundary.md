# C391 — adapter correspondence boundary

Date: 2026-09-01

The v1 adapter check establishes four things: the fixture path exists and has the recorded expected value; the named Lean adapter declaration is among the pinned declaration slices; the `lean-field` string occurs inside those retained slices; and the expected value's broad shape is recorded correctly. It does **not** establish that the fixture value denotes, parses to, or is equal to the Lean expression near that occurrence.

Harder string matching would not close that gap. A longer substring is still occurrence evidence. Exact correspondence requires an executable decoder from the serialized fixture value into a Lean value plus a theorem equating that decoded value with the witness declaration—the identity-preserving adapter used by the strongest fixture-indexed proofs.

Every `PositiveLeanWitnessReceipt/v1` now records:

```edn
:correspondence
{:boundary/type :declared-not-derived
 :subject :fixture-to-lean-correspondence
 :pinned :fixture-identity-shape-and-retained-slice-occurrence
 :not-pinned :semantic-value-correspondence
 :derivation-status :derivable-not-adopted
 :reason :identity-preserving-adapter-and-proof-not-provided}
```

Each mapping also records one of `:scalar`, `:collection`, or `:structured`, derived and checked from `:expected`. Thus a four-row posterior table is visibly `:structured`, while a count or individual mass is `:scalar`. This classification describes evidential grain; it does not upgrade occurrence into correspondence.

Positive verification remains 31/31 under the v1 meaning: successful pinned proofs with load-bearing recorded components, author-declared dependency slices, and occurrence-anchored fixture mappings. It is not a claim that all 31 fixtures have machine-proved EDN-to-Lean identity.
