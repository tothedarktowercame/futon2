# C544 — `:F12` slice 6: the O-laws stated of the function, and what that dissolves

Item `:F12` (`worklist.edn`), slice 6. Mathlib4 commits `0c5745c39e` (the
conformance file, dispatched to codex-1) and `0691f74e48` (this review's
additions); futon2 this commit. Artifact `runs/F12-organise/04-conformance.edn`,
checker `f12_conformance_check.bb`.

## 1. Why this slice exists, and what it is not

Slices 1–5 stated the four O-laws of a cascade **value**, and that is where
decision D1 came from: a value has to carry `selected` and `admittedBy` in a
field, and no carrier in the vocabulary carries both. C543 §7 left slice 4 —
the carrier reconciliation and the sorry — waiting on D1's closure.

But `organise` is not a value. Its type at `Holes.lean:861` is

    Cascade Policy → Set P → Repository P → Cascade P

and its second argument **is** the selected set. Stated of the function applied
to its inputs, `selected` needs no field at all. This slice measures exactly how
much of D1 that dissolves and what it leaves standing. It is a fourth framing
next to C539 §4's three arms, not a fourth arm: the arms ask which carrier holds
the laws, and this asks whether the laws need a carrier.

**No ruling is taken.** `Holes.lean` and `holes-contract.json` are untouched
(`git log -1` still reads `61c4825dc3` and `50fa53469a`), the sorry at
`Holes.lean:861` is not discharged, and no existing declaration is modified.

## 2. What the function framing dissolves, and what it does not

`OrganiseType` (`F12Conformance.lean:19`) is the s3e signature, and the checker
reads BOTH it and `organise`'s own type out of their files and compares them, so
a drift that would make every theorem below a theorem about a different
interface cannot pass silently.

| law | at the function | why |
|---|---|---|
| O1 | **partly** — the two-way union only | `sel` is the argument; `addedByOrganise` is a `Cascade` field; `admittedBy` has nowhere to live |
| O2 | **yes**, no carrier change | `repo` is the argument, so `Repository.standsOn` is in scope |
| O3 | **yes**, no carrier change | `sel` and `repo` are both arguments |
| O4 | **no** | `Cascade` has one `precedence` and no acting order or score; the input `Cascade Policy` carries `List Policy`, a different type |

O2 and O3 needed a paired repository under arm (iii) and an extra argument under
arm (i) (C543 §4a); at the function they need neither, because the repository is
already there. So the function framing removes the carrier question for two of
the four laws outright.

It does not remove it for O1, and the O4 row is a claim about which FIELDS
`Cascade` has, which a comment cannot be wrong about in a way `lake build`
notices. The checker therefore reads the field lists of both structures out of
`Holes.lean`: `Cascade` is `{acyclic, addedByOrganise, edges, nodes,
precedence}` and all six O4 fields (`actingOrder*`, `precedence*`, `score*`)
are present in `CascadeDiff` and absent from `Cascade`.

## 3. Swappability, witnessed rather than asserted

The acceptance asks that the swappability clause be honoured — "implementation
conformance, not identity". Two implementations at the exact signature, both
named by `Holes.lean:861`'s own type-amendment note, are defined and both proved
conformant:

- `organiseSelectedOnly` (`:59`, proved at `:68`) — `nodes = sel`, the
  `wmCascadeDiffFixture` temperament;
- `organiseUpClosure` (`:77`, proved at `:86`) — closure under authorship, the
  `checks/playout_snatch.clj` temperament.

`organiseWitnessesDifferOnZaif` (`:107`) proves they differ on the recorded zaif
inputs, at vertex 26. So conformance does not determine the function, and
whatever discharges the sorry will be a **choice** among conformant
implementations rather than a derivation of the only one — which is what the
declaration's refusal ("does not select one canonical implementation") says, now
with two witnesses instead of a sentence.

O1 is non-trivial for the second: `organiseUpClosureAddsSomething` (`:116`) puts
vertex 26 in `addedByOrganise` through the authored edge `6 → 26`, where the
first witness's O1 is `sel ∪ ∅`.

## 4. The two measurements

**(a) The interface cannot tell an admitted node from an organise-added one.**
`admittedIndistinguishableFromAdded` (`:123`): for ANY `f` satisfying the three
laws, if `f` returns the recorded 20 nodes on the recorded selected input, then
all nine admitted nodes are in its `addedByOrganise`. They came from a
policy-grain `admit`, and at this signature they can only be recorded as
organise's own authored closure — the misattribution the `:LA2` field amendment
(`Holes.lean:833-845`) added `admittedBy` to prevent. The two-way O1 is not a
weaker statement of the three-way one; it is a statement that loses an origin.

The review added the unconditional half. `admittedIndistinguishableFromAdded` is
conditional on `f` returning the recorded nodes, and
`organiseUpClosureOmitsAdmittedWitness` (`:216`) shows the up-closure
temperament cannot reach admitted vertex 11 at all — it is not selected and is
not an authored target of anything, so no amount of closing over authorship
gets there. The general form of that (the up-closure of the recorded selected
set is exactly `{20, 21, 25, 26}`, disjoint from all nine admitted nodes) is
recomputed from the record by the checker, not asserted: `:up-closure-of-selected`
in the artifact, with `:edges-from-selected-into-admitted []`.

**(b) The two readings of O3 separate two implementations, not just two
readings of one cascade.** Slice 2 found Lean's O3 quantifying over `.selected`
where the Clojure law and constructor use `nodes`
(`futon3:checks/find_organise.clj:529`, `construct_cascade.clj:415`), and showed
they disagree on the recorded cascade (`Holes.lean:1034`). At the function:

- `selectedReadingCannotYieldZaifEdge` (`:141`): under the selected reading, NO
  conformant `f` returns the recorded edge `18 → 19` on the recorded selected
  input, because both its endpoints were admitted rather than selected.
- `selectedOnlyYieldsZaifEdgeWhenAdmittedSupplied` (`:149`): the same
  implementation does return that edge once all twenty nodes are supplied as
  input. So it is the missing origin, not the edge law, that puts the recorded
  run out of the interface's reach.
- Review addition: `organiseSelectedOnlyConformantNodes` (`:157`) satisfies BOTH
  readings, because `nodes = sel` makes them one proposition — the same
  coincidence that made the C59 fixture unable to tell them apart — while
  `organiseUpClosureNotConformantNodes` (`:174`) satisfies the selected reading
  and REFUTES the node-set one, on the authored edge `6 → 26` running between
  two vertices of its own returned node set. The readings are therefore not
  interchangeable at the implementation, and which one is owed is a question
  about which implementations are admissible.

No ruling is taken on that question, on D1, on D2 or on D3.

## 5. Two defects in this slice's own work, found and repaired

**The dispatched file declared `ConformantOrganiseNodes` and proved nothing of
it.** The second predicate exists to contrast the two O3 readings, and nothing
in the file used it — the same shape as slice 5's defect, where two thirds of a
claim sat in unused `have`s. A declaration no theorem mentions is checked by the
build only for well-formedness. Repaired in `0691f74e48` with the two theorems
in §4b.

**The checker failed on the word `admit` in a docstring.** Its sorry-scan ran
over the whole file, and a docstring describing the policy-grain `admit` edit —
a true sentence about the subject — tripped it. A checker that makes accurate
prose fail teaches the next slice to soften the prose to please the instrument,
so the scan now strips `/-- -/` and `/-! -/` blocks and reads code only, with
the reason written at the call site.

## 6. What was checked

- `lake build DarkTower.WarMachine.F12Conformance`: 2706 jobs, 0 errors, no
  warning from the new module (the ten replayed `sorry` warnings are
  `Holes.lean`'s own).
- `#print axioms` on all **19** declarations: no `sorryAx`. Eight depend on no
  axiom at all; the rest on `propext`, `Classical.choice`, `Quot.sound`.
- **Codex's three controls**, each plant verified present first: `nodes := sel ∪
  {p | p = p}` breaks `organiseSelectedOnlyConformant` with unsolved `⊢ x ∈ sel`;
  a selected set widened to `{n | n < 20}` breaks
  `selectedReadingCannotYieldZaifEdge` (`selected18 : True ⊢ False`); collapsing
  the up-closure to `sel` breaks `organiseWitnessesDifferOnZaif`.
- **Two review controls**, run here. Pointing the node-set refutation at the
  unauthored pair `0 → 26` instead of the authored `6 → 26` fails to elaborate,
  so that refutation depends on the recorded edge and not on the shape of the
  statement. Moving the omission witness from vertex 11 to vertex 19 leaves
  unsolved goals — 19 has rank 1 and IS an authored target (of 18, itself
  unreachable), so the rank-zero argument is doing real work and its scope is
  visible rather than hidden.
- A third review control was run and is reported as **not a clean
  discriminator**: giving `organiseUpClosure` edges that fast-forward over its
  own nodes breaks three declarations, including the `acyclic` field proof, so
  it does not isolate the theorem it was aimed at.
- Checker: signature agrees between the two files; both O3 readings differ in
  exactly the `fastForward` argument and agree in o1 and o2; both zaif vertices
  agree with slice 2's derivation; the up-closure recomputes to `{20, 21, 25,
  26}`; the file imports `F12D1Arms` and re-declares none of its `d1*` data, so
  no new drift surface is opened. Two runs byte-identical.
- clj-kondo 0/0 and `check-parens` OK on the checker.
- `negative_controls.sh` and `pointer_check.bb` — see the ledger row.
- No `gen_aif_dag.bb` (TN §9a).

## 7. What this slice does NOT settle

D1 is not closed, and this slice does not close it: what it adds is that D1's
question is only half a carrier question. Two of the four laws need no carrier
at all once the laws are stated of the function; O4 needs one and has none; O1
needs one for its third origin and the cost of not having it is now measured
rather than described. The sorry at `Holes.lean:861` is not discharged, and §3
is the reason it should not be discharged by simply writing one of these two
functions into it. D2, D3 and the O3 field question are untouched.
