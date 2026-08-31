# C62 — organise discharge audit and refusal

No organise hole is discharged.  The C59 fixture has four independent
rejecting mutations, but the Lean declarations are stronger than fixture
claims and cannot honestly be closed from it.

## Four falsifiers

- O1 adds an `addedByOrganise` member absent from `nodes`; only the union law
  fails.
- O2 adds `remedy → probe`, which has no authored path.  O2 fails directly
  (and O3 necessarily also notices that the organised set is no longer exact).
- O3 removes the required `probe → remedy` fast-forward; O2 remains vacuously
  true while exactness fails.
- O4 makes both precedence variants identical, including acting order and
  score; only governance fails.

## Why the fixture cannot close the declarations

O1–O3 are universal statements about `organise`, but `organise` remains an
opaque `sorry` deliberately marked REFUSED as an implementation rather than a
law.  Closing them would require choosing a generic implementation and proving
its acyclicity, node, reachability and exact-fast-forward properties.  The
fixture demonstrates one instance; it does not define that polymorphic
function.

O4 is false as written.  It quantifies over arbitrary `P`, `Score`,
`actingOrder`, and `score`, then asserts two cascades whose precedence affects
one of the latter functions.  Take constant acting-order and score functions
(or an empty `P`): the required effect cannot exist.  O4 must instead quantify
over a recorded `CascadeDiff`/specific policy-score pair, or take an explicit
precedence-sensitivity hypothesis.

Required next decision: either (a) redefine the four declarations as
fixture-indexed witness propositions, preserving the refused implementation,
or (b) define a canonical `organise` and restate O4 with a sensitivity
hypothesis.  Selecting between those changes the contract and is not licensed
by a witness-binding delivery.
