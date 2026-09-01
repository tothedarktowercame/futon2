# C291 — cascade carrier repair stops at the missing meet semantics

Date: 2026-09-01

The requested repair is not one mechanical carrier packet. Serialized practice
settles two distinct relation families:

- `chosen_semi_lattice` emits directed `descent` pairs, consumed as `BV.seq`;
- it emits undirected weighted `co_app` pairs, consumed as `BV.copar`.

That supports separate carrier fields and an identity-preserving two-axis
fixture. It does **not** settle the requested meet/semilattice law. The emitted
object contains no meet operation, lower-bound relation, meet witnesses, or law
connecting weighted co-application adjacency to `IsMeet`/`hasMeets` in
`CascadeOrder.lean`. Co-application is a symmetric weighted graph in the
producer, while `hasMeets` expects an order relation. Applying `hasMeets`
directly to `co_app` would invent the conclusion the fixture must establish.

Landing only the two fields would be the partial carrier change the packet
forbids: the source would appear to represent the semilattice while enforcing
no semilattice law. Therefore no Lean declaration changes in this pass.

The deciding missing object is a recorded meet semantics: either an explicit
meet operation/witness per node pair, or a declared order derived from the two
serialized relation families with a theorem that every pair has a greatest
lower bound. Once that producer contract exists, the carrier, full-fidelity
fixture, and two later glossary bindings become mechanical.

Counts remain 119 declarations, 108 closed / 11 holes; glossary coverage stays
31/33. Q and model-area closure steps are unchanged because the spine did not
move.
