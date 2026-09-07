# C545 — `:F12` slice 7: the third origin becomes statable and stays undetermined

Item `:F12` (`worklist.edn`), slice 7. Mathlib4 commit `7d4f6ffca3` (the
admitting-arm file, dispatched to codex-1) plus this review's additions in the
follow-on commit; futon2 this commit. Artifact
`runs/F12-organise/05-admitting-arm.edn`, checker `f12_admitting_check.bb`.

## 1. What this slice is

C544 §2 measured the function framing and found one gap it could not close: O1
gets `sel` and `addedByOrganise` from the argument list and the `Cascade` result,
and has **nowhere to put `admittedBy`**. C544's `admittedIndistinguishableFromAdded`
(`F12Conformance.lean:123`) then proved the cost — any conformant `f` returning
the recorded nodes files all nine policy-admitted nodes under
`addedByOrganise`, which is the misattribution the `:LA2` field amendment
(`Holes.lean:833-845`) added `admittedBy` to prevent.

This slice runs the obvious repair: keep the argument list of `Holes.lean:861`
and widen only the **result** carrier, to slice 5's `ArmOneCascade`
(`F12D1Arms.lean:74-82`), which has both origin fields. The question it answers
is whether giving the third origin a field is enough to make it determined.

**No ruling is taken.** `Holes.lean` and `holes-contract.json` are untouched
(`git log -1` still reads `61c4825dc3` and `50fa53469a`), the sorry at
`Holes.lean:861` is not discharged, and no existing declaration is modified.
D1, D2, D3 and the O3 field question are all left open.

## 2. The signature edit is the codomain and nothing else

`AdmittingOrganiseType` (`F12AdmittingArm.lean:17`) is

    Cascade Policy → Set P → Repository P → ArmOneCascade P

against `organise`'s `… → Cascade P`. The checker reads both types out of their
own files, substitutes only the final carrier, and compares — so the slice's
"same arguments, wider result" reading cannot silently become false. This is
what separates the arm from C539 §4's arm (i), whose `armOneOrganiseType`
(`F12D1Arms.lean:84`) widened the **first argument** too.

The consequence C543 §4(a) predicted holds: `admittingArmO2AtZaifRepo`
(`F12AdmittingArm.lean:33`) and `admittingArmO3AtZaifRepo` (`:40`) discharge O2
and O3 at the recorded repository with **no `hrepo` argument**, where arm (i)'s
value framing needed one, because at the function the repository is already an
argument.

## 3. The third origin: statable, provable, undetermined

Statable and provable on the recorded run:

| declaration | what it establishes |
|---|---|
| `organiseAdmittingZaifAdmitsNine` (`:73`) | the witness returns exactly the nine recorded admissions in `admittedBy` |
| `organiseAdmittingZaifNodes` (`:79`) | its nodes are exactly the recorded twenty |
| `organiseAdmittingConformant` (`:63`) | it satisfies all four conformance clauses |

Undetermined, and this is the slice's result. `organiseAdmittingMirror` (`:87`)
attributes the **same nine nodes** to `addedByOrganise` and leaves `admittedBy`
empty. It is conformant too (`:98`). The two agree on nodes (`:108`) and on the
whole edge relation (`:114`) and differ on `admittedBy` at recorded vertex 11
(`admittingSplitUnderdetermined`, `:120`).

So the field does not buy the distinction. C544 proved the interface cannot tell
an admitted node from an organise-added one **when there is no field for it**;
this slice proves it still cannot **when there is one**. Whatever fixes the
attribution is not a law in this set.

`admittingThirdOriginUnconstrainedGeneral` (`:129`) states the general form, and
its name reads stronger than its content: what it proves is non-uniqueness
across implementations (two conformant witnesses at the recorded nodes, one with
empty admission and one with the recorded nine), not that some fixed `f`'s field
is undetermined. Codex flagged that gap itself rather than closing it with a
definition of "determined" it would have had to invent.

## 4. What the review added: the result survives the open O3 question

The dispatched predicate reads O3 over the function's **returned nodes**, which
is the reading `futon3:checks/find_organise.clj:529` and
`construct_cascade.clj:415` use. Which field O3 quantifies over is an open
question beside D1 (C540 §b), so the arm's result was proved under one arm of a
question nobody has answered. The review states the other one:

- `ConformantOrganiseAdmittingSelected` (`:170`) differs from
  `ConformantOrganiseAdmitting` in **exactly `o3`** — checked clause by clause
  by the checker, not by eye — reading it over `sel`, as `Holes.lean:909` does.
- `admittingSplitUnderdeterminedSelectedReading` (`:230`) proves the same split
  under it: nodes equal, edges equal, `admittedBy` different at vertex 11.

So the underdetermination does not depend on the field question, and the field
question does not have to be answered for slice 7's result to stand.

**What the two readings do NOT share, measured rather than argued.** Under the
node-set reading the agreement is witnessed on a non-empty relation: the one
recorded fast-forward `18 → 19` is in both witnesses' edges
(`admittingZaifEdgeNonVacuous`, `:152`). Under the selected reading the recorded
edge relation is **empty** — `admittingSelectedReadingEdgesEmptyOnRecorded`
(`:262`) proves no fast-forward runs between two selected vertices at all, via
`d1Rank_eq_zero_of_selected` (`:251`) and the rank that already proves the
authored relation acyclic (`F12D1Arms.lean:35-51`). Both readings leave the third
origin undetermined; they differ in whether there is any edge for the agreement
to be about. That is the same divergence slice 2 found on the recorded cascade
(`Holes.lean:1034`), now visible at the level of the implementations.

Without this addition the dispatched file's `admittingAgreeOnEdges` would have
been an agreement provable by `rfl` — both witnesses were written with the same
edge expression — with nothing anywhere saying the relation it agrees on is
inhabited. That is the same shape as slice 5's unused `have`s and slice 6's
undischarged predicate: a claim machine-checked by nothing a later reader could
cite.

## 5. Two things the artifact records that the prose could have carried instead

**A clause the record's O-laws do not have.** `ConformantOrganiseAdmitting`
carries four clauses, and `osel` — `(f t sel repo).selected = sel` — is not one
of O1–O4. It is needed because the widened carrier has a `selected` field that
could otherwise drift from the argument, and it makes the underdetermination
result *stronger* (one more constraint, still split). The checker requires that
`osel` is the only clause added over slice 6's predicates and that it is exactly
the field-fidelity equation, so the addition cannot grow unremarked.

**Neither witness is an algorithm.** Both are recorded-data lookups:
`zaifAdmittedFor` (`:48`) is `if sel = d1Selected then d1Admitted else ∅`. Slice
6's two witnesses were named by the `Holes.lean:861` type-amendment note and
compute something on any input; these compute the record on one input and
nothing anywhere else. That is what makes this arm's split cheap to obtain, and
a reader should not take either witness as a candidate implementation. The
checker reads the lookup's body and records `:witness-shape
:recorded-lookup-table` in the artifact, so the caveat lives where the numbers
live.

## 6. O4 is still not statable

`ArmOneCascade` has `Cascade`'s five fields plus `selected` and `admittedBy`, and
**none** of the six O4 fields (`precedenceBefore/After`, `actingOrderBefore/After`,
`scoreBefore/After`), which live only on `CascadeDiff`. The checker reads all
three field lists and requires exactly that, and separately requires the arm file
to state no `o4` clause. Widening for O1's third origin does not move O4 at all;
that remains where C541 left it, blocked on D2/D3 and the gate's
then-correspondence.

## 7. A defect in this slice's own instrument, found and fixed

The checker's presence test used `str/includes?`, which matches a **prefix**. A
plant renaming `admittingZaifEdgeNonVacuous` to `admittingZaifEdgeNonVacuousRENAMED`
left the check green, and the same hole would have let `:split`'s needle
`theorem admittingSplitUnderdetermined` be satisfied by the longer
`admittingSplitUnderdeterminedSelectedReading` sitting beside it — the check
would have reported a deleted theorem as present because its neighbour's name
starts the same way. Fixed by requiring the needle to be followed by something
that cannot continue a Lean identifier, with the reason written at the call site.
Third occurrence of this family in three slices: slice 5's `(some? false)`, slice
6's docstring `admit`, this one. Each was found by a plant aimed somewhere else.

## 8. Controls

Codex's four, all re-run here and reproducing the reported failure:

| control | plant | observed |
|---|---|---|
| C1 | `nodes := sel ∪ ∅` | `organiseAdmittingConformant` unsolved `sel = sel ∪ zaifAdmittedFor sel`; o3 also fails |
| C2 | mirror `admittedBy := zaifAdmittedFor sel` | `admittingSplitUnderdetermined` gets `h11 : True ⊢ False`; mirror o1 also fails |
| C3 | copied-data bound `11 ≤ n ∧ n < 19` | `organiseAdmittingZaifNodes`: omega cannot prove the goal |
| C4 | raw `edges := repo.standsOn` | o2/o3 fail; a raw edge has no `.2.2` projection |

Three against the review's own Lean, each verified to have landed first:

- emptiness claimed of the node-set-reading witness instead → type mismatch, the
  membership it needs is over the widened node set, so the emptiness result is a
  fact about the *reading*;
- non-vacuity aimed at the unauthored pair `17 → 19` → `trivial` does not have
  type `d1Repo.standsOn 17 19`, so the witness depends on the recorded edge;
- the split read at selected vertex 5 instead of admitted vertex 11 → `h11 : True`,
  so the disagreement is read where an admission actually is.

Eight against the checker, each moving the verdict: signature edited outside the
codomain; an O4 field added to `ArmOneCascade`; the node-set O3 replaced by the
selected one; the recorded edge moved to `[18 21]`; `:selected-below` widened to
20; a declaration renamed; a declaration deleted; the witness lookup returning
`d1Nodes`.

## 9. Gates

- `lake build DarkTower.WarMachine.F12AdmittingArm`: 2707 jobs, 0 errors, no
  warning from the new module; `Holes.lean`'s own known `sorry` warnings replay.
- `#print axioms` over all **24** declarations, generated from the file rather
  than typed: 0 `sorryAx`, 3 depend on no axioms, the rest on `propext`,
  `Classical.choice`, `Quot.sound` only.
- clj-kondo 0/0 and check-parens OK on `f12_admitting_check.bb`.
- Checker byte-identical over two runs.
- `negative_controls.sh` PASS (133 negative, 53 positive); `pointer_check.bb`
  2487 pointers, 0 unresolved, run on the working ledger *before* the ledger
  commit.
- No `gen_aif_dag.bb` (TN §9a).

**A pointer form that is not gated, hit again.** The row's citations were first
written in the bare continuation form — `admittingZaifEdgeNonVacuous, :152` —
and a plant proved they were not being checked at all: rewriting `:262` to a line
beyond the file left `pointer_check` at 0 unresolved, because its pointer regex
needs the file name on each citation. C544 §5 recorded the same thing and spelled
its eleven citations out; this row was written in the short form anyway and only
the plant caught it. All fifteen are now spelled out (2471 → 2487 pointers), and
the same plant against the rewritten row — one citation rewritten to line 88888
— reports `UNRESOLVED … (end beyond file)`. The file name is deliberately not
repeated in that sentence: written out in full it would itself be scanned as a
pointer, which is how this check first came back red.

## 10. Where this leaves the row

Slice 4 — the carrier reconciliation and the sorry at `Holes.lean:861` — is what
D1 was blocking, and D1 now has four measured framings rather than three
descriptions: the three arms of C543, plus the function framing of C544 with this
slice's widened-result variant. What slices 6 and 7 together establish is that
**no framing in this set determines the third origin**: with no `admittedBy`
field the interface misfiles admissions as authored closure, and with one it
leaves the attribution free. A slice cannot close that by choosing; either the
laws gain a clause that pins the attribution to something outside them (the
policy-grain rule that emitted the admit, which is recorded on the Clojure side
and is not what O1 quantifies over — `Holes.lean:833-845`), or the sorry is
amended to say so. Both are rulings, and rulings are Joe's or the reviewer's.
