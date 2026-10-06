# Discovery: the 3/2-pushout condition on the finite `PMap` carrier

Date: 2026-10-06  
Scope: discovery only; no Lean definition of pushout and no claim that the
current carrier captures Goguen's examples.

## Result

For the current carrier, the proposed reduction is almost right but its first
step is false as written. The set of mediating maps is not closed under graph
union: two individually functional maps can send the same source element to
different targets, and their union is then not a `PMap` hom. Nevertheless, a
maximum exists exactly when the union of all individually admissible pairs is
functional. With the ordinary two-leg reading this reduces to coverage of
every element of the blend by at least one leg. If an auxiliary leg is removed
from the mediating equations, it reduces to coverage by the non-auxiliary legs.
This reduction accepts structure-dropping blends that Goguen says are not
pushouts. The current carrier/order therefore does not yet express the cited
quality distinction.

The definitions used are `PMap.wellFormed` at
`mathlib4/DarkTower/WarMachine/ThreeHalvesBlend.lean:38-40`, graph inclusion
at lines 50-51, relational composition at lines 53-55, and empty/join at
lines 60-63. Derived axiom carriage is lines 42-48. The source's quality-order
and universal-property readings are recorded in
`CASCADE-SPEC-v3-draft-2026-10-05.md:33-44`; auxiliary morphisms are recorded
at lines 69-72.

## 1. The four proposed steps

Fix a well-formed `h : B → C`. Call `(u,v) ∈ B.elems × C.elems`
*pairwise admissible* when every `(i,u)` in `b₁.rel` has `(i,v)` in `c₁.rel`
and every `(j,u)` in `b₂.rel` has `(j,v)` in `c₂.rel`. This is exactly the
singleton-map form of `b₁;h ≤ c₁ ∧ b₂;h ≤ c₂`, by relational composition
(Lean lines 53-55) and graph inclusion (lines 50-51).

1. **Refuted as stated.** `M(C,c)` is not closed under unions. Let `B` contain
   `u`, let `C` contain distinct `v₀,v₁`, and take both legs and both comparison
   maps empty. Then `h₀={(u,v₀)}` and `h₁={(u,v₁)}` are well-formed members of
   `M`, but `h₀ ∪ h₁` is non-functional and fails `wellFormed`. The corrected
   statement is: constraints are closed under relational union, and a maximum
   exists iff the union `H` of all pairwise-admissible pairs is functional.
   If functional, `H` is itself a well-formed member containing every member;
   if a maximum existed, it would have to contain every singleton admissible
   map and hence equal `H`.
2. **Holds for the corrected `H`.** Every admissible pair already lies in
   `B.elems × C.elems`; endpoint failure is impossible. Thus `H` can fail
   `wellFormed` only because two pairs share a first coordinate and have
   different second coordinates. This is exactly the functionality check in
   Lean lines 38-40 (expanded propositionally at lines 65-75).
3. **Holds, with a zero-target qualification.** If `(i,u)` is in a leg, an
   admissible `v` must be the value of the corresponding functional `c` at
   `i`; if that value is absent there is no admissible `v`. Several preimages
   or two legs can reduce the set to zero, but never increase it past one in a
   consistent comparison cone. If `u` has no preimage under either constrained
   leg, every `v ∈ C.elems` is admissible. Choosing a two-element `C` makes `H`
   non-functional.
4. **Holds.** The empty graph is functional, has the right endpoints, and its
   composites are empty, so it belongs to every `M`. Thus `M` is never empty
   under the current hom definition.

The counterexample to (1) was the prediction before inspection; inspection
confirmed it. Clauses (2)-(4) hold after replacing “union closure” with the
corrected maximum characterization.

## 2. Quantifier-free predicate and four examples

For a consistent cone with both mediating equations enforced, the resulting
predicate is

```text
pushoutGraph(c) := B.elems ⊆ image(b₁.rel) ∪ image(b₂.rel)
```

Necessity uses the empty consistent comparison cone into a two-element `C`:
an uncovered `u` then has two admissible targets. Sufficiency follows because
each covered `u` has at most one admissible target for every comparison cone,
so `H` is functional. The quantifier over `C` disappears.

Auxiliary flags expose an ambiguity not settled by the current data definition.
Goguen's statement that the auxiliary morphism is removed from the diagram
(`CASCADE-SPEC-v3-draft-2026-10-05.md:69-72`) says its mediating equation
should also be removed. On that reading `pushoutGraph` takes the union of the
images of **non-auxiliary** legs only. Keeping both equations despite an
auxiliary flag would have no textual justification and would make the flag
affect consistency but not the universal property.

| cone | constrained-leg coverage | result on current graph predicate |
|---|---|---|
| Houseboat | both legs cover all five blend elements | true |
| Boathouse, House leg auxiliary | Boat image omits `house`, `land`, and `livein` | false |
| One-point blend | its sole element is in the image | true |
| Houseboat with the water axiom dropped but the water element retained | graphs and coverage are unchanged | true |

If both Boathouse equations were retained, their image union covers all eight
elements and the result would instead be true. This is precisely why the
auxiliary convention must be fixed in the proper packet rather than hidden in
an implementation.

## 3. Candidate repairs to the hom-set/order

The textual standard is stronger than graph inclusion: maps “should be as
defined as possible, should preserve as many axioms as possible, and should be
as inclusive as possible” (`CASCADE-SPEC-v3-draft-2026-10-05.md:33-38`), and
the injections should be inclusions as far as possible (lines 66-68).

| candidate | effect on the four examples | supported by the cited text? |
|---|---|---|
| (a) Require every morphism to carry every source axiom | Rejects the canonical Houseboat leg: `House→Houseboat` deliberately loses `on(house,land)`. It also rejects partial comparison maps, so the present Houseboat, Boathouse, one-point blend, and axiom-dropped Houseboat are not even uniformly cones in the proposed category. | No. “As many as possible” is an order/optimality statement, not preservation of all axioms. |
| (b) Require totality on elements | Rejects canonical Houseboat because `land` is intentionally unmapped; likewise changes which Boathouse auxiliary cones exist. One-point and axiom-dropped variants may remain, so it does not isolate the intended distinction. | No. “As defined as possible” presupposes partial maps and asks for maximality, not totality. |
| (c) Compare `carries` as well as graphs | A conjunctive clause is redundant: `carries_monotone` proves graph inclusion implies carries inclusion (`ThreeHalvesBlend.lean:154-169`). It therefore leaves all four results unchanged. Lexicographic comparison can distinguish maps with incomparable graphs, but neither the text nor the present carrier specifies the priority or a content relation; choosing one now would be a new design. | The three quality dimensions support making axiom preservation observable, but not a particular lexicographic order. |
| (d) Demand a unique, non-empty maximum | A maximum in a partial order is already unique. `M` always contains the empty map, while requiring the maximum graph itself to be non-empty merely rejects comparisons with no admissible pairs. It leaves Houseboat, the one-point blend, and the axiom-dropped blend indistinguishable and does not repair Boathouse. | No; Definition 7 asks for a maximum and explicitly replaces ordinary uniqueness (`CASCADE-SPEC-v3-draft-2026-10-05.md:39-44`). |

There is a further concrete limitation: codes are untyped names. For example,
the current graph formalism can extend the partial House leg by mapping `land`
to `water`; that extension happens to turn the lost `on` axiom into the target
`on` axiom. The intended vocabulary/typing judgment that rules out that map is
not present in `Theory` or `PMap`. None of (a)-(d) supplies it.

## 4. Recommendation for packet 2 proper

Do not certify the coverage predicate as Goguen's 3/2-pushout predicate. It is
a valid theorem about the current graph-inclusion carrier, but its four results
are Houseboat=true, Boathouse=false under removal of its auxiliary House leg,
one-point=true, and axiom-dropped Houseboat=true. The last two conflict with
the recorded claim that blends which could preserve more structure are not
pushouts; the Boathouse result also conflicts with treating the illustrated
Boathouse as one of the good auxiliary blends.

Packet 2 should first make the missing admissibility/quality data explicit:
typed vocabulary constraints on maps, and a declared partial order that can
compare preservation quality without collapsing it to graph inclusion. Then
implement Definition 7 directly by finite enumeration, with auxiliary legs
removed from both consistency and mediating constraints. At these witness
sizes this remains decidable: theories, functional graphs, comparison cones,
and candidate mediators are all finite (`NOTE-three-halves-blend-carrier-2026-10-06.md:80-83`).
Only after enumeration reproduces Houseboat and the intended auxiliary
Boathouse while rejecting the one-point and axiom-dropped cases should a
quantifier-free characterization be proved.

This is a structural stop, not a request to tune a Boolean test: implementing
coverage now would prove the wrong distinction exactly and reproducibly.
