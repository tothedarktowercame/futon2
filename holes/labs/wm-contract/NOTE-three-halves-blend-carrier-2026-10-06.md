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
`Finset Nat` of named elements (sorts, constants, relation names, all coded
as `Nat`, as `ConstructionReceipt` codes units and tokens) together with a
`Finset (Nat × Nat × Nat)` of axioms, each `(r, x, y)` reading "relation `r`
holds of `(x, y)`" over the theory's own elements. Nothing semantic is
attached to a code; the identity of a blend lives in which codes are
identified, which is Goguen's "names matter" (v3 §1).

```lean
structure Theory where
  elems  : Finset Nat
  axioms : Finset (Nat × Nat × Nat)      -- (relation, x, y), all three in elems

/-- A partial map of theories is its graph; functionality and domain are
`wellFormed`, decided. Axiom preservation is *derived*, never declared. -/
structure PMap where
  rel : Finset (Nat × Nat)
def PMap.wellFormed (f : PMap) (A B : Theory) : Bool   -- functional; rel ⊆ A.elems ×ˢ B.elems
def PMap.carries (f : PMap) (A B : Theory) : Finset (Nat × Nat × Nat)
  -- the axioms (r, x, y) of A with r, x, y all in f's domain whose image (f r, f x, f y) is an axiom of B
```

The **quality order** on maps `A → B` is graph inclusion, `f ≤ g iff
f.rel ⊆ g.rel`. Goguen's three criteria (Definition 6; v3 §1) follow from
it once axiom preservation is derived: `g` preserves as much content
(inclusion), preserves every axiom `f` does (`carries` is monotone in the
graph for well-formed maps: a theorem, `carries_monotone`), and is as
inclusive (domain inclusion). Composition is relational composition;
identities are the diagonal on `elems`. Identities are *maximal* (no
well-formed endomap strictly extends the diagonal) and not greatest: on
`A.elems = {0,1}` the endomap `{(0,1)}` is well-formed and incomparable with
the identity (codex-33's counterexample, 2026-10-06, to a packet that had
glossed Definition 6's "maximal" as "greatest"). That is the 3/2-category;
`Prop`-level, proved once (obligation 13's first half).

Revision history. Packet 1 (mathlib4 `0ae83ba6d9`) carried axioms as
opaque codes with a declared `carried` field, so "preserves axioms" was
literal code sharing: House, Boat and Houseboat all used codes {100, 101}
and the legs "carried" them by convention, while Boathouse's legs carried
nothing. The order's axiom clause then measured nothing. Packet 1b replaces
this with the triple form above; the loss Goguen's order is about becomes
measurable (the House → Houseboat leg carries `livein(resident, house)`
but not `on(house, land)`, since `land` is dropped).

A **span** is `a₁ : PMap G I₁`, `a₂ : PMap G I₂`. A **cone** over it is
`b₁ : PMap I₁ B`, `b₂ : PMap I₂ B` with two flags `aux₁ aux₂ : Bool`: a
flagged triangle is **auxiliary** (v3 §1) and not required to commute.
**Consistency** (Definition 7): some `d : PMap G B` with `a₁ ≫ b₁ ≤ d` and
`a₂ ≫ b₂ ≤ d` over the non-auxiliary triangles; since the order is
inclusion, the join of the composites is the least such `d`, so consistency
is "the join is well-formed" (packet 1). A cone with both flags set is
vacuously consistent, stated as a theorem so the degenerate case is on the
record. **3/2-pushout**: for every consistent cone `c₁, c₂` into `C`, the
set `{h : PMap B C // b₁ ≫ h ≤ c₁ ∧ b₂ ≫ h ≤ c₂}` has a maximum. A
**square** is the record the runtime must serialise (v3 §5): `G I₁ I₂ B`,
the four maps, the two flags, and a source pin per object.

Witnesses (Goguen 2006, Figures 1, 4, 5, read from the rendered pages):
houseboat identifies `resident/passenger`, `house/boat` and `live in/ride`,
drops `land`, and commutes with no auxiliary triangle; boathouse keeps
`ride` and `livein` apart, identifies `resident/boat`, and commutes on no
element but `on`, so it is consistent only with one triangle auxiliary
(either side works). The two bad cases are Goguen's own statements made
checkable: boathouse with no auxiliary flag, and houseboat with the two
relations kept distinct, are inconsistent.

Decidability: for finite theories every `PMap A B` is one of finitely many
subsets of `A.elems ×ˢ B.elems`, so the 3/2-pushout property is decidable by
enumeration, and small witnesses go through `native_decide` as the receipt
theorems do. The general theorems (composition, Proposition 8) are proved
over the structure, not decided.

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

## 4a. What the 3/2-pushout is on this carrier (discovery, 2026-10-06; defaults)

`NOTE-three-halves-pushout-discovery-2026-10-06.md` (codex-33, futon2
`18ece513b`) settles the literal Definition 7 on this carrier: the set of
mediators into a comparison cone is not union-closed as maps, but a maximum
exists iff the union of all pairwise-admissible pairs is a function, and that
holds iff **every element of B lies in the image of a non-auxiliary leg**
(coverage). The quantifier over C disappears; the empty map is always a
mediator. Under coverage houseboat is a pushout, boathouse with its auxiliary
House leg is not, and so are the one-point blend and a houseboat that drops
the water axiom — the last two against Goguen 2006 p. 8. The discrimination
Goguen draws therefore does not come from the universal property on named
sets with partial maps; it needs either his richer morphisms (sorts,
constructors) or an order over cones.

Default settings (claude-2, under the 2026-10-06 defaults policy; revisable
by name as carrier items K1–K3):

- **K1 (revised K1′, 2026-10-06, after packet 2a r2's stop).** `Cone.isPushout`
  is consistency plus coverage: every element of B lies in the image of b₁
  or b₂ — **both legs, whatever the auxiliary flags**. The flags remove a
  triangle's commutation from *consistency* only. Reason: Goguen's sentence
  is "a blend is a commutative cone over the diagram with the auxiliary
  morphisms removed" (1999 p. 31; v3 §1); the diagram is the span, so the
  removed morphism is a span arrow `aᵢ : G → Iᵢ`, which leaves `Iᵢ` in the
  diagram as an object and the cone leg `bᵢ : Iᵢ → B` in place; the
  mediator constraints `bᵢ ≫ h ≤ cᵢ` and hence coverage still range over
  both legs. The first K1 (and the discovery note §2) dropped the auxiliary
  leg from coverage too; under that reading neither boathouse nor the
  amphibious RV could ever be a pushout, since each has an element reached
  only through its auxiliary side, against Goguen 2006 p. 7–8 which counts
  both among the good 3/2-pushouts. The RV's own square does not commute:
  the generic `medium` reaches `land` through House and `water` through
  Boat, and the RV keeps both, so one triangle is auxiliary (either side;
  the House side by convention, as for boathouse) — codex-33's finding.
  Under K1′: houseboat, boathouse (House leg auxiliary), RV (House leg
  auxiliary), the one-point blend and the water-dropping houseboat are all
  pushouts; the two degenerate ones are separated by K2′. `pushout_iff_coverage`
  (packet 2b, mathlib4 `7c9cb26040`) is proved for coverage over both legs; its
  proof uses neither consistency nor the hom-set hypotheses, so on this carrier
  Definition 7's universal property *is* coverage and consistency is the
  separate conjunct. It is named for what it
  is and the docstring says it is not Goguen's optimality distinction.
- **K2 (revised K2′, 2026-10-06, after packet 2a's stop).** The optimality
  distinction lives in a declared **quality order over cones of one span**,
  **lexicographic**: first fewer identifications on each leg (the number of
  pairs of distinct input elements a leg sends to one element; "the
  injections should be inclusions as far as possible", names matter), then,
  at equal identifications, more carried axioms and larger domains on each
  leg (conjunctive on the two legs). The first version was conjunctive over
  all three; codex-33's quality records showed that under it the one-point
  blend is *incomparable* with houseboat — collapsing everything to one
  element makes every axiom "carried" — and that the amphibious RV strictly
  dominates houseboat (it keeps `land` and `on(house,land)` at no extra
  identification), which is Goguen's own description of the RV ("very good
  preservation properties"), so that part stands. Under K2′: RV > houseboat
  > water-dropping houseboat, and the one-point blend is below all three.
  Incomparability is still possible at equal identifications with
  incomparable carries (houseboat against a mirror blend that keeps `land`
  and drops `water`); that is the "several maximal blends the order cannot
  rank" that R9's default counts in ambiguity, and packet 2a carries that
  witness.
- **K3.** Obligation 14's witness is houseboat against the **amphibious RV**
  (Goguen 2006 p. 7, "very good preservation properties": keeps `land` and
  `water`, `on(house/boat, land)` and `on(house/boat, water)`; its House
  triangle auxiliary, since `medium` reaches both): both consistent,
  both pushouts under K1, RV above houseboat under K2′, non-isomorphic as
  cones (non-uniqueness does not need incomparability).
  Houseboat against the one-point blend would also witness non-uniqueness but
  says nothing.

Open, for v3 §7 (not blocking): whether Goguen's "not any kind of pushout"
for structure-dropping blends depends on the richer 1999 morphisms, which
the text extraction does not let us read; the typed-vocabulary admissibility
codex-33 notes (the carrier cannot forbid mapping `land` to `water`) is part
of the same question and is deferred with it.

## 5. Order of packets

1. `ThreeHalvesBlend.lean`: §1 carrier + order + composition + identities
   maximal; house/boat theories and the two cones; consistency of both by
   `native_decide`. (One packet; predict the enumeration size.)
2. Packet 2a (done, mathlib4 `a82d6342e1`): `isPushout` (K1′), the cone quality
   order (K2′), non-isomorphism of houseboat and the RV (K3), the land-houseboat
   incomparable pair. Packet 2b (done, `7c9cb26040`): `pushout_iff_coverage`.
3. (done, mathlib4 `e561bf9205`) Policies, gluing, components, `verdict`; the
   click-bag and diamond-cascade witnesses (obligation 16); application order as
   a separate carrier with `applicationOrder_independent` (obligation 17).
4. (done, mathlib4 `8ba60c15c1`) The pasting-failure witness (obligation 15):
   Proposition 8's vertical shape with a partial leg; `comp_wellFormed` and the
   witness/consistency iff over the hom-set added. The W (Theorem 13) preserves
   consistency when its middle pushout cone is consistent.
5. Two-carrier witness for application order (obligation 17), after 3.
6. (done, mathlib4 `b978697a16`, `e348beb6e7`) Proposition 8 over the structure
   (obligation 13): holds for commuting squares with well-formed legs; fails for
   merely consistent ones (counterexample recorded). Goguen's proof uses the
   on-the-nose equality `b₁;c₂ = a₃;c₁`.

What this does **not** do: connect a blend diagram to a transition kernel
(v3 §7's open adequacy theorem), say how the quality order enters G (R9's
default stands), or alter `ConstructionReceipt`, which keeps serving the
runtime receipt until the carrier fixes what the runtime must serialise.

## 6. Closure (2026-10-06, ~06:45Z): obligations 13–17 on this carrier

All five of CASCADE-SPEC-v3 §6's obligations have a witness or a proof in
`mathlib4/DarkTower/WarMachine/ThreeHalvesBlend.lean` (branch `darktower`,
`e348beb6e7`; 711-job build; no `sorry`), produced by codex-33 in eight
commits over nine packets and reviewed here. What was learned, each item a
departure from the text as first read, recorded by name:

| obligation | state | where the text and the carrier part company |
|---|---|---|
| 13 carrier + Proposition 8 | carrier: Definition 6's laws proved (identities *maximal*, not greatest — `id_not_greatest`); Prop. 8 proved for **commuting** squares (`prop8`), refuted for merely consistent ones (`prop8_fails_without_commutation`) | Goguen's proof uses `b₁;c₂ = a₃;c₁`; Definition 7 gives only ≤-consistency, weaker for partial maps |
| 14 non-uniqueness | houseboat and the amphibious RV: both pushouts of one span, non-isomorphic (`isoCones` by bijection search; renamed copy isomorphic as control) | the RV's square does not commute on `medium`; one triangle auxiliary (K1′) |
| 15 pasting failure | vertical pasting with a partial leg: both diamonds consistent, composite not; `inconsistencyWitness` names `(0, 5, 6)` | needs partiality; the W (Thm 13) preserves consistency when its middle cone is consistent |
| 16 bag verdict | `Policy.verdict`: the 2026-10-05 click is `bag [[0],[1],[2],[3]]`; the outer diamond glued at observe's blend is `cascade`, construction order derived = its support | — |
| 17 two orders | `applicationOrder` a separate evidence-tagged field; `applicationOrder_independent` by unfolding | — |

The pushout notion itself (packet 2b, `pushout_iff_coverage`): on named
sets with partial maps under graph inclusion, Definition 7's universal
property is coverage of the blend by the legs, and uses neither
consistency nor the hom-set hypotheses. Goguen's optimality distinctions
therefore live in the declared quality order over cones (K2′: identifications
first, then preservation), which ranks RV > houseboat > water-dropping
houseboat, puts the one-point blend below all, and leaves houseboat and the
land-keeping mirror incomparable — the concrete pair R9's default counts in
ambiguity.

Open after this: v3 §7's adequacy question (how a blend diagram determines a
transition kernel; the `CoApplicationKernel` layer is untouched); the
richer-morphism question (§4a, "not any kind of pushout"); `components_cover`
in its exact-once form; source pins on `Square` for the runtime receipt.
