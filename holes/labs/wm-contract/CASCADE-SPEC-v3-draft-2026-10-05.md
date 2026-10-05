# Cascade specification v3 (draft): a pattern is a blend

Date: 2026-10-05. Author: claude-2. Status: DRAFT for comment (claude-17,
codex-10), then for Joe. Amends `CASCADE-SPEC-v2-2026-10-01.md` (codex-10),
which Joe called "a good working draft but not final" (2026-10-05). Where this
file says nothing, v2 stands. No code or Lean change is authorised by this
file.

## 0. Why a cascade has the shape it does

Joe, 2026-10-05, verbatim: "the fundamental unit, a design pattern, is itself
a conceptual blend, i.e. a pushout. When these are combined you get a
cascade." And, asked whether the blend is a strict pushout or has structure of
its own: "It's a blend with emergent structure. I believe the idea was
described formally with regard to Goguen 1-and-a-half categories but I don't
remember the details. But for example, a houseboat and a boathouse are two
different blends of 'boat' and 'house'."

v2 stipulates three relations (support, meet, generative precedence). This
draft derives them from one construction, the blend, so that what a cascade
is and why it has meets, directions and an order come from the same place.

## 1. The formal reference (read, not remembered)

Goguen, "An Introduction to Algebraic Semiotics, with Application to User
Interface Design" (1999), Appendix B, `refs/goguen1999.pdf` pp. 31-34 (text
extraction is garbled; read by eye). Goguen, "Mathematical Models of
Cognitive Space and Time" (c. 2006), `refs/goguen2006.pdf`.

- **Definition 6 (p. 32).** A 3/2-category ("one and a half", footnote 31) is
  a category in which each hom-set C(A,B) is partially ordered, composition
  preserves the orderings, and identities are maximal. The order is a
  *quality* order on morphisms: for the house/boat example, f ≤ g iff g
  preserves as much content as f, preserves every axiom f does, and is as
  inclusive (p. 34).
- **Definition 7 (p. 32).** Given a V, a_i : G → I_i (i = 1, 2): a cone
  b_1, b_2 over a_1, a_2 is *consistent* iff there is some d : G → B with
  a_1;b_1 ≤ d and a_2;b_2 ≤ d, and is a *3/2-pushout* iff for every consistent
  cone c_i : I_i → C the set {h : B → C | b_1;h ≤ c_1 and b_2;h ≤ c_2} has a
  maximum element. Uniqueness of the mediating morphism is replaced by a
  maximum; "for this notion, the uniqueness property is (fortunately!) lost".
- **Non-uniqueness (p. 33; 2006 Example 3, pp. 7-8).** "Two different
  3/2-pushouts need not be isomorphic; this means that ambiguity is natural in
  this setting." Blends "are not determined uniquely, not even up to
  isomorphism": of house and boat, "houseboat", "boathouse", a boat that
  carries houses, an amphibious RV, a livable boat for transporting livable
  boats, and a boat used on land as a house; the Alloy algorithm found 42 more,
  "most of which are far from optimal". For this example "the most natural
  blend is an ordinary pushout, all the other good blends are 3/2-pushouts,
  and various blends that fail to preserve as much structure as they could
  are not any kind of pushout" (pp. 34-35).
- **Pasting (Propositions 8 and 9, pp. 32-33; Theorem 13, p. 34).** The
  composition of two 3/2-pushouts is a 3/2-pushout; a 2×2 grid of them is one;
  and the 3/2-colimit of a W (two V's sharing their middle top) is the
  3/2-pushout of the two V's 3/2-pushouts: (a_1 ⋈ a_2) ⋈ (a_3 ⋈ a_4) =
  Colim(W). "3/2-pushouts can be used to compute the 3/2-colimit of any
  connected diagram." But "unlike the situation for ordinary pushouts, the
  composition of consistent diamonds need not be consistent" (p. 33).
- **Names matter (p. 31).** "Isomorphic cones do *not* represent the same
  blend" because "the names attached to the elements in a blend are
  important"; the injections should be inclusions as far as possible.
- **Auxiliary morphisms (p. 31).** Not every blend has all its triangles
  commuting: of house and boat, only houseboat does. Morphisms whose triangles
  are not required to commute are *auxiliary*; a blend is a commutative cone
  over the diagram with the auxiliary morphisms removed.
- **Emergent structure.** 1999, p. 32: the ordering "captures what Fauconnier
  and Turner have called 'emergent' structure, without needing any other
  machinery". 2006, pp. 14-15 and 19: emergent structure "arises by
  integrating new triads that match important non-integrated concepts in the
  input spaces" (the Buddhist-monk example recruits a "meeting space" whose
  one new axiom, d(t*) = 0, is the emergent part); "this is a major new
  hypothesis of this paper".

## 2. The unit: an occurrence is a blend square

An occurrence of a pattern in a policy for a pinned problem is a square

    G → I_1, G → I_2 (the span: two inputs over what they share)
    I_1 → B ← I_2, with G → B (the blend)

in a 3/2-category whose objects are finite theories over the pinned
vocabulary (v2 §"Interpretation": the carrier is pinned per problem) and
whose morphisms are partial maps ordered by how much they preserve. Each of
G, I_1, I_2, B is recorded as text with its source (library file and digest,
or the problem's text), together with the four maps (which named elements are
identified) and, for each triangle, whether it commutes or is auxiliary.

The square is supplied by a **reading of the pattern for the problem**, not
by slot position. Evidence (claude-2 and claude-17, 2026-10-05): 109 of 1,436
library patterns state their two poles in IF ("The pattern operates on the
axis: A ↔ B") and 103 follow with "Irreducible: …", which reads as the shared
part; every pattern has `! conclusion`, which names the blend. In a 20-pattern
sample outside that form, the poles sat in IF/HOWEVER (7), both in HOWEVER
(3), in the conclusion (2), both in IF (1), or were not recoverable (2); five
were generated registry entries; none stated the shared part. So no slot
mapping is specified. A reading that cannot name G, I_1, I_2 and B for the
problem is a typed absence on the occurrence, not a default.

Two occurrences of one library pattern in one policy are distinct units
(v2 already says so); two occurrences with the same G, I_1, I_2 and different
maps or different B are distinct occurrences (houseboat / boathouse).

## 3. Combination: a cascade is the 3/2-colimit of a connected diagram

Occurrences combine by sharing an object: one occurrence's blend, input or
ground is another's input or ground. The diagram of all occurrences and
shared objects is the cascade's identity; the cascade's blend is its
3/2-colimit, computable from the 3/2-pushouts of its parts (§1, Theorem 13).

Consequences, each a requirement:

1. **Connected or bag.** A diagram with no shared object between two
   components is a disjoint sum. Its "colimit" is the two parts side by side
   and nothing is blended. A construction whose occurrences share nothing is
   reported as a **bag**, not a cascade (claude-17's amendment 4; v2's
   "disconnected cascades as degenerate"). Run 2026-10-05-c9d25d6a (four
   patterns picked one per want, one dependency) is a bag by this rule.
2. **Consistency is checked, not assumed.** Pasting two acceptable squares
   can fail to be consistent (§1). A pasted figure without a witness d is a
   typed construction defect (v2's "arbitrary overlap is not credited"), with
   the pair named.
3. **The three relations of v2 are derived.**
   - *support(child, parent)*: a leg I → B of some square: the child is an
     input to the blend it helps form. Several parents because one object can
     be an input to several squares.
   - *meet(left, right) = shared*: the object under both, i.e. the G of the
     square in which left and right are the inputs (or, for two squares
     glued at a shared input, that input: the W of Theorem 13). It is named
     as text (claude-17's amendment 3). A meet is required only for pairs
     that are glued (the restricted condition of
     `mathlib4/DarkTower/WarMachine/Proof2/CoApplicationKernel.lean`,
     hasRestrictedMeets), not for every pair.
   - *precedence*: two different orders, kept apart in one sentence
     (claude-17's amendment 2). **Construction order** is forced: a square
     exists only after its span, a glued pair only after its meet. **Application
     order**, v2's before(a, b), says that applying a before b in this problem
     preserves earlier results and needs evidence from the transition model.
     Support direction gives neither. v2's rule stands: the absence of an
     application-order edge is not an assumption of simultaneous success.
4. **Same patterns, different arrangement, different policy** (Requirements
   Q10): the arrangement is in the maps, not in the objects (§1, names
   matter).

## 4. Quality order and emergent structure (open, for comment)

- The order on morphisms says how much of each input a blend keeps. This is
  the natural place for "how well the interpretation fits the circumstance"
  (Joe, 2026-09-30, `NOTE-g-as-fold-2026-09-30.md`), and the 2006 paper
  proposes "the extent to which a mapping preserves source space features"
  as the formal optimality criterion. Whether and how it enters G (as F, as a
  term of ambiguity, or as the order over which construction extends) is NOT
  specified here. v2's ordering-ambiguity term (log of the number of linear
  extensions) is unchanged by this draft.
- Emergent structure, on Goguen's 2006 hypothesis, is what an added space
  contributes when it matches concepts the inputs mention but the ground did
  not integrate. Read against construction: an extension candidate is an
  occurrence whose inputs match such concepts. This is a reading, not a rule;
  it bears on where the pattern graph's neighbours enter construction.
- Whether the house/boat quality order (content, axioms, inclusiveness) is
  the right order for library patterns is open.

## 5. Construction receipt (adds to v2's list)

- per occurrence: G, I_1, I_2, B as text with sources; the four maps as
  element identifications; per triangle, commutes / auxiliary;
- per glued pair: the shared object (text or id) and the consistency witness
  d, or the typed defect;
- the verdict bag / cascade, with the components;
- construction order (derived) and application-order edges (with evidence),
  separately.

## 6. Lean obligations (adds to v2's twelve)

13. An ordered-category carrier (Definition 6) and 3/2-pushout (Definition 7)
    stated; Proposition 8 proved or imported.
14. A finite witness of non-uniqueness: two non-isomorphic 3/2-pushouts of one
    V (house / boat suffices).
15. A finite witness that two consistent squares paste to an inconsistent
    figure, and that the checker names the pair.
16. The bag verdict: a diagram with two components has no shared object, and
    the policy set reports it as a bag.
17. The two orders are separate carriers; no theorem derives application
    order from support or from construction order.

## 7. Not settled by this draft

- Which parts of a library pattern are its inputs and ground, outside the
  axis form (§2 sample).
- The relation between a 3/2-pushout and the existing kernel
  (`CoApplicationKernel.lean`): the kernel is a transition on token states;
  the blend is the structure of the policy. They are not the same object and
  this draft does not identify them.
- How the quality order enters G (§4).
- Whether `@why` in the library is a leg (input → blend) or something else;
  claude-17 reads it as nearest to support. The mined graph is undirected and
  cannot supply legs.
