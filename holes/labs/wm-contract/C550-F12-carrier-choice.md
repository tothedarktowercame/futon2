# C550 — `:F12` slice 12: D1 registered, with the arm nobody ran named as unrun

Item `:F12` (`worklist.edn`), slice 12. No Lean, no new checker: this slice
writes D1 — which carrier the four O-laws are stated of — into
`aif-equations.edn :choices` as `:organise-carrier`, with its five run arms,
their measured costs, and a **sixth arm that is not run**.

**No ruling is taken and none is implied.** The entry carries `:status
:observed-not-decided` and no `:ruling` key. `Holes.lean` and
`holes-contract.json` are untouched (`git log -1` on both still reads
`61c4825dc3` and `50fa53469a`), and the registry file compares **equal as a
value** to its previous state outside the one added key.

## 1. Why registration was this slice

The row's slice-11 entry named four things left open — D1, D2, D3, O4 at the arm
carrier — and said registering D1 was the cheapest and needed no new Lean. It is
also the one this ledger's own checker asks for: `worklist_check.bb:17-45`
transcribes Joe's rule of 2026-09-01 that a choice the theory does not settle is
a **choice point in `:choices` with the arms named and the measurement that
separates them**, branches built and run. D1's branches were built and run by
slices 5, 6 and 7. It has been recorded as *blocking* by slices 2, 3 and 4 and
as *open* by every slice since, and the registry the machine and the reviewer
read said nothing about it at all — the same gap slice 9 closed for
`:organise-third-origin`.

## 2. What the entry says the arms cost

Five arms, each with a run and a pointer. The shape worth seeing is that the
payments differ in **kind**, not in size:

| arm | pays |
|---|---|
| widen `Cascade` (`F12D1Arms.lean:74-81`) | O2 and O3 need the repository as an extra hypothesis (`:106`, `:113`); O4 unstatable |
| codomain `CascadeDiff` (`F12D1Arms.lean:119`) | a `Score` parameter the declared signature lacks; O4 unexercised on the recorded run (`:123`, `:129`, `:134`) |
| the `Cascade`×`Repository` pair | O1 not writable at all, and its one substitution is refuted on the record (`F12D1Arms.lean:180`); O4 unstatable |
| laws of the function (`F12Conformance.lean:19`) | O1 loses its third origin, measured: nine admitted nodes are forced under `addedByOrganise` (`:123`) |
| function with the result widened (`F12AdmittingArm.lean:17`) | the third origin is statable and **undetermined** (`:120`); the only witness is a lookup on the recorded input |

## 3. The one thing this slice adds that the five notes did not

**The sixth arm.** `armTwoOrganiseType` (`F12D1Arms.lean:119-120`) is

    Cascade Policy → Set P → Repository P → CascadeDiff P Score

— the *function* framing at the `CascadeDiff` codomain — and **no theorem in any
`F12` module mentions it** (`grep -rn armTwoOrganiseType --include=*.lean` over
mathlib4 returns its own declaration line and nothing else). So what the build
checks of it is well-formedness.

That matters because it is the only framing at which **all four laws are jointly
statable of one thing**: the repository is an argument, so O2 and O3 need nothing
supplied; the result carries `selected`, `addedByOrganise` and `admittedBy`, so
O1 is three-way; and the result carries the six before/after fields, so O4 is
statable, which no other arm can say. Every payment recorded in §2 has the form
*this law is not statable here* — an arm at which all four are statable is not a
refinement of the five, it is the one that could make their payments unnecessary.

Registering D1 with five arms and calling them all would therefore have been an
overstatement, and this is the same defect class the row has caught in four
consecutive slices: a declaration nothing proves anything of (slice 5's unused
`have`s, slice 6's `ConformantOrganiseNodes`, slice 11's three aliases). Here it
is one of the arm signatures itself. The entry names it `:not-run` with `:buys
:unmeasured` and `:costs :unmeasured` — the shape C547 §6 used for the
support-grain free hand, which slice 10 then ran — and says why its cost is not
obviously zero: it inherits arm two's `Score` parameter, and O4's antecedent is
false on the recorded run, so a conformance predicate carrying an O4 clause would
be satisfied there **vacuously**.

Consequence for the entry's own form: `:organise-carrier` carries **no
`:all-arms-run` key**, where both sibling F12 entries do. The difference is
deliberate and is stated in `:not-claimed`.

## 4. The measurement, recomputed rather than cited

The three checkers that measure these arms were re-run today. Each PASSes and
regenerates its artifact **byte-identically** (sha256 compared before and after;
`git status` clean on `runs/`):

- `f12_d1_arms_check.bb` → `03-d1-arms.edn` (`15e60ea4…`)
- `f12_conformance_check.bb` → `04-conformance.edn` (`3763533c…`)
- `f12_admitting_check.bb` → `05-admitting-arm.edn` (`eabec4da…`)

What they make measurements rather than readings: the three carriers' field
lists are read out of the sources (`Cascade` five, `ArmOneCascade` seven,
`CascadeDiff` twelve), so every "O4 is unstatable here" row is checked; each
arm's signature is read out of its own file and compared with `organise`'s own
type, so no arm's verdict can quietly be a verdict about a different interface;
both O3 readings are read out and differ in exactly the `fastForward` argument,
so arms four and five are not tacitly deciding the field question; and the
recorded numbers each arm is instantiated at (20 nodes, 11 selected, 9 admitted,
13 authored edges, 27-vertex closure) agree with slice 2's derivation at futon3
`1b8b1d1`.

**One correction to how arm two's cost has been stated.** C543 §4(b) reads that
the arm "buys an O4 that cannot fire". Unexercised is not unexercisable, and the
difference is checkable: O4 *is* discharged on the C59 fixture, whose precedence
moves (`Holes.lean:916-921`), and one of the nine recorded cascades carries a
satisfied O4 row — `futon3:checks/ants-cascade.edn:79-105`, read out of the
record today: `:precedence-changed? true`, the acting order moves
(`pheromone-trail-tuner` and `white-space-scout` swap), `:score-changed? false`,
so the disjunction holds on its left. The honest cost is that O4 is unexercised
**on the run every D1 arm is instantiated at**. Whether that record's acting
order is the denominator O4 means is D2's question, and D2 stays unregistered
(its gate is red at HEAD, C547 §7).

## 5. Every pointer in the entry was read back out of its file

Not spot-checked: each `file:line` in the new entry was printed from the file
before the entry was written — all 47 distinct pointers, being 11 in
`Holes.lean`, 14 in `F12D1Arms.lean`, 9 in `F12Conformance.lean`, 11 in
`F12AdmittingArm.lean`, `construct_cascade.clj:402` and `:420-421`, and
`ants-cascade.edn:79-105`. Two
ranges quoted from earlier notes were tightened where they ran one line past the
declaration they name (`ArmOneCascade` is `:74-81`, not `:74-82`;
`ConformantOrganiseSelected` is `:23-28`, not `:23-37`).

## 6. The registry entry was spliced, not re-emitted

`aif-equations.edn` was edited by inserting the new key's text ahead of the
`:choices` closing brace, so the file's 204-line header and its hand formatting
survive. Checked afterwards: the file parses, and `(= (dissoc choices
:organise-carrier) old-choices)` and `(= (dissoc m :choices) (dissoc old
:choices))` are both true — the file compares **equal as a value** to HEAD
outside the one key it adds. (Slice 11 recorded why this matters: a `pprint`
rewrite also compares equal as a value and drops the header, which is a defect a
value check cannot see.)

## 7. Gates

`negative_controls.sh` and `pointer_check.bb` — see the ledger row; both were
run on the working tree *before* the ledger commit, which is the order slice 3
got wrong. No Lean touched, so no `lake build`. No new script, so no clj-kondo
and no `check-parens` subject. No `gen_aif_dag.bb` (TN §9a). No machine run, so
no run-lock.

## 8. What this slice does NOT settle

D1 is open and this slice does not close it: what it adds is that D1 is now a
registered choice point with measured arms and a named blank. The sorry at
`Holes.lean:861` is neither discharged nor amended, and slice 4 stays blocked.
D2, D3, and O4 at the arm carrier are untouched. **Next slice**: run the sixth
arm — conformance of a function at the `CascadeDiff` codomain, the first framing
that could state all four laws of one thing, with the vacuity of its O4 clause
on the recorded run as the thing to measure rather than to assume.
