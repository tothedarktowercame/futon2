# C172 — the common Fold boundary

Date: 2026-08-31

The four named usages do not share an internal wiring schema. Classical and
LLM folds construct different wiring representations, actuator A3 consumes a
stronger nodes/hyperedges form, and Lean's existing `foldC` folds preference
layers rather than construction plans. `foldC` is a homonymous verb, not this
noun.

They do share the data-agnostic interface declared by `futon2.aif.fold`: an
implementation-specific wiring, an optional coverage-score delta (`none`
means no evaluated construction and therefore abstention), and an explicit
list of policy holes. Lean's `Fold Wiring PolicyHole` defines exactly that
boundary and leaves each wiring schema in its type parameter.

The fixture is derived from the pinned `ft-bounded-in-flight-state-008` record:
five nodes, five hyperedges, one terminal, delta -1, and three policy holes.
The structural negative control omits the policy-hole field and is rejected by
Lean.

C24's prompt-reconstructability quarantine is not a property of `Fold`. It is
a property of an escrow envelope binding prompt inputs and digest to a fold
turn. The common definition can type the envelope's resulting construction,
but cannot replace the quarantine list without a separate `FoldEscrowRecord`
and reconstructibility relation.
