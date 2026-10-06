# The 3/2-blend carrier for Lean — design note (claude-2, 2026-10-06)

Purpose: CASCADE-SPEC-v3 §6 says obligations 13–17 cannot be stated in
`CascadeSpec.lean` (one `precedes`, untyped overlaps) or in
`ConstructionReceipt.lean` (token edges, `precedence == support`), and asks
for a new module, "conceptually `ThreeHalvesBlend.lean`". This note fixes
what that module carries and in which order the obligations are built, so
the packets that follow are small. Definitions are v3 §1's reading of Goguen
1999 Appendix B and 2006 (not re-derived here). Nothing in this note changes
the runtime; a theorem about a carrier is not an endorsement of a run.

## 1. What the module carries

Objects are **finite theories over a pinned vocabulary**: a theory is a
`Finset Nat` of named elements (sorts, constants, relation names — all
coded as `Nat`, as `ConstructionReceipt` codes units and tokens) together
with a `Finset Nat` of axiom codes. Nothing semantic is attached to a code;
the identity of a blend lives in which codes are identified, which is
Goguen's "names matter" (v3 §1).

```lean
structure Theory where
  elems  : Finset Nat
  axioms : Finset Nat
  deriving DecidableEq

/-- A partial map of theories: an element relation that is functional on
its domain, with the axioms it carries across. -/
structure PMap (A B : Theory) where
  rel     : Finset (Nat × Nat)          -- (a, b) pairs, a ∈ A.elems, b ∈ B.elems
  carried : Finset Nat                  -- axioms of A preserved into B
  functional : ∀ a b b', (a, b) ∈ rel → (a, b') ∈ rel → b = b'
  dom  : ∀ p ∈ rel, p.1 ∈ A.elems ∧ p.2 ∈ B.elems
  axs  : carried ⊆ A.axioms ∩ B.axioms
```

The **quality order** on `PMap A B` is Goguen's three criteria read as
inclusions (v3 §1, Definition 6): `f ≤ g` iff `f.rel ⊆ g.rel` (g preserves
as much content), `f.carried ⊆ g.carried` (every axiom f preserves, g does),
and inclusiveness is `f.rel.image Prod.fst ⊆ g.rel.image Prod.fst`
(implied by the first; stated for the record). Composition is relational
composition with carried axioms intersected through; it preserves the
order; identities (the diagonal on `elems`, all axioms) are *maximal* —
no well-formed endomap strictly extends the diagonal — and not greatest: on
`A.elems = {0,1}` the endomap `{(0,1)}` is well-formed and incomparable with
the identity (codex-33's counterexample, 2026-10-06, to a packet that had
glossed Definition 6's "maximal" as "greatest"). That is the 3/2-category;
`Prop`-level, proved once (obligation 13's first half).

A **span** is `a₁ : PMap G I₁`, `a₂ : PMap G I₂`. A **cone** over it is
`b₁ : PMap I₁ B`, `b₂ : PMap I₂ B`. **Consistency** (Definition 7): some
`d : PMap G B` with `a₁ ≫ b₁ ≤ d` and `a₂ ≫ b₂ ≤ d`. **3/2-pushout**: for
every consistent cone `c₁, c₂` into `C`, the set
`{h : PMap B C // b₁ ≫ h ≤ c₁ ∧ b₂ ≫ h ≤ c₂}` has a maximum. **Auxiliary**
morphisms (v3 §1): a square record carries, per triangle, a flag
`commutes : Bool`; the pushout condition is checked over the triangles not
flagged auxiliary. A **square** is the record the runtime must serialise
(v3 §5): `G I₁ I₂ B`, the four maps, the two flags, and a source pin per
object.

Decidability: for finite theories every `PMap A B` is one of finitely many
(a subset of `A.elems ×ˢ B.elems` with a subset of the axioms), so
consistency and the 3/2-pushout property are decidable by enumeration, and
small witnesses (house/boat: a handful of elements) go through
`native_decide` as the receipt theorems do. The general theorems
(composition, Proposition 8) are proved over the structure, not decided.

## 2. The two orders, as two carriers (obligation 17)

`ConstructionReceipt` conflates them (`precedence == support`). Here:

- **Construction order** is *derived*: a square after its span's objects, a
  glued pair after its shared object. It is a function of the diagram, not a
  field.
- **Application order** is a *separate field* with its own evidence: a
  `Finset (Nat × Nat)` over occurrence ids plus, per edge, an evidence tag
  (`:transition-model`, `:observed`, …) coded as `Nat`. No theorem derives it
  from legs or from construction order. Obligation 17 is discharged by a
  witness, not a theorem: two policies with identical squares and gluing,
  different application-order fields, both well-formed; and a `simp`-checked
  statement that the well-formedness predicate does not mention the
  application field except through its endpoint condition.

v2's rule "a missing application-order edge is not simultaneous success" is
inherited by making the application field's absence mean *no claim*, with
no default.

## 3. Policies, gluing, components (obligations 15 and 16)

A **policy** is a finite list of squares plus a gluing relation: pairs
`(i, role_i, j, role_j)` saying square i's object in `role_i ∈ {G, I₁, I₂, B}`
*is* square j's object in `role_j`, with the identification given as a
`PMap` that must be an isomorphism on `elems` (a shared object, v3 §3). The
**diagram** is the quotient of all objects by gluing; **components** are
the connected components of squares under gluing.

- **Obligation 16 (bag verdict).** `verdict : Policy → Verdict` with
  `Verdict = bag (components : List (List Nat)) | cascade`; a policy with
  two or more components is a bag and the receipt lists them. Witness: the
  2026-10-05 click's four occurrences with no gluing → `bag [[0],[1],[2],[3]]`;
  the outer diamond glued at `observe`'s B → `cascade`. Both `native_decide`.
- **Obligation 15 (pasting can fail).** Two squares each a consistent cone,
  glued at one object (the W of Theorem 13), whose composite figure is not
  D-consistent; the checker returns the offending pair of morphisms. This is
  the finite witness v3 §3.2 asks for; Goguen gives the shape (p. 33) but no
  concrete example in the text, so the witness is ours and the packet must
  construct it by hand (predict the size first: two squares of ≤ 4 elements
  each should suffice, since the failure needs only two axioms that cannot
  both be carried into one object).

## 4. Non-uniqueness (obligation 14) and Proposition 8 (obligation 13)

- **Obligation 14.** House and boat as `Theory`s (elements: `house, boat,
  person, water, land, live-in, ride, on` and the axioms Goguen lists in
  1999 pp. 33–34); the generic space G (`object, person, medium, use, on`);
  two cones `houseboat` and `boathouse` with their maps; both consistent,
  both 3/2-pushouts, not isomorphic as cones (names differ: `live-in` lands
  on the boat in one and on the house in the other). All four facts by
  `native_decide`; the enumeration size is the thing to predict before
  running (number of `PMap`s between two 8-element theories with ~6 axioms
  — state it; if `native_decide` cannot finish in the build budget, shrink
  the theories and say what was dropped).
- **Obligation 13.** The carrier (§1) and Proposition 8: the pasting of two
  3/2-pushouts along a shared leg is a 3/2-pushout. Proved over the
  structure with `Classical`, in the style of `Proof2/CoApplicationKernel`;
  Proposition 9 (the 2×2 grid) and Theorem 13 (the W) follow or are left as
  named open obligations with the reason.

## 5. Order of packets

1. `ThreeHalvesBlend.lean`: §1 carrier + order + composition + identities
   maximal; house/boat theories and the two cones; consistency of both by
   `native_decide`. (One packet; predict the enumeration size.)
2. Both cones are 3/2-pushouts; not isomorphic (obligation 14).
3. Policies, gluing, components, `verdict`; the two bag/cascade witnesses
   (obligation 16). Independent of 2; can run in parallel on a second seat
   if one is free.
4. The pasting-failure witness (obligation 15), after 1.
5. Two-carrier witness for application order (obligation 17), after 3.
6. Proposition 8 over the structure (obligation 13), last; it is the only
   piece that is a proof rather than a witness, and its statement must be
   checked against the 1999 text by eye first (the extraction is garbled).

What this does **not** do: connect a blend diagram to a transition kernel
(v3 §7's open adequacy theorem), say how the quality order enters G (R9's
default stands), or alter `ConstructionReceipt`, which keeps serving the
runtime receipt until the carrier fixes what the runtime must serialise.
