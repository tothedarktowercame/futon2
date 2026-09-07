# C549 — `:F12` slice 11: the no-bootstrap reading of O3's field

Slice 11 runs the third reading of O3's quantified field and registers the
choice the four prior slices kept recording as open. **No ruling is taken.**
`aif-equations.edn :choices :organise-o3-field` carries `:status
:observed-not-decided` and no `:ruling` key; `Holes.lean` and
`holes-contract.json` are untouched (`git log -1` still reads `61c4825dc3` and
`50fa53469a`); the sorry at `Holes.lean:861` is neither discharged nor amended.

## 1. What was undetermined, and why it was not in the registry

O3 says organise's edges are exactly the authored fast-forwards. **Which set the
fast-forward quantifies over** is not fixed by the theory, and the two sources
disagree:

| source | quantifies over |
|---|---|
| `mathlib4/DarkTower/WarMachine/Holes.lean:909` | `.selected` |
| `futon3:checks/find_organise.clj:529` | `nodes` |
| `futon3:checks/construct_cascade.clj:415` (builds the edges) | `nodes` |

On the C59 fixture `nodes = selected`, so the two are one proposition and the
file never had to choose. On the recorded zaif run they are not, and slice 2
proved the disagreement (`Holes.lean:1034`). Slices 6 and 7 then ran both
readings at two further carriers. What none of them did was put the question in
`:choices` — the same gap slice 9 found for the third origin, and the one shape
`worklist_check.bb:17-45` argues against.

## 2. The reading nobody had run

Under the node-set reading a node **organise itself added** may serve as a
fast-forward endpoint. So organise can add a node, author an edge through it,
and have O3 confirm the edge because the node organise added is in `nodes`. The
no-bootstrap reading quantifies over the nodes that came in from outside
organise — `nodes \ addedByOrganise` at `Cascade`, `selected ∪ admittedBy` at
`CascadeDiff` — and refuses that, without giving up the admitted origin the
selected reading gives up.

`F12O3FieldArm.lean` (21 declarations) runs it.

| what | where |
|---|---|
| the arm's conformance predicate | `F12O3FieldArm.lean:16` |
| `addedByOrganise` empty in both recorded fixtures | `:25`, `:38` |
| zaif `selected ∪ admittedBy` is the node set | `:46` |
| the recorded O3 IS its no-bootstrap O3 | `:52` |
| node-set-conformant implementation | `:62`, `:72` |
| outside its added field, precisely the caller's selection | `:81` |
| recorded 26 and 25 both organise-added | `:90` |
| it returns edge `26 → 25` | `:101` |
| that edge is not a no-bootstrap fast-forward | `:108` |
| **so the two readings come apart** | `:119` |
| on the recorded selected input the relation is EMPTY | `:128` |
| selected-only is no-bootstrap conformant | `:145` |
| the relation is inhabited on a constructed input | `:162` |
| no conformant f returns the recorded edge at `Cascade` | `:172` |
| the recorded organised edge set is inhabited | `:186` |
| the same edge IS in the no-bootstrap field at `CascadeDiff` | `:200` |

## 3. The measurement, recomputed rather than cited

`f12_o3_field_check.bb` → `runs/F12-organise/07-o3-field.edn`.

The two readings differ exactly when an edge runs through a node organise added.
Over the **52 O1-shaped cascades** in the nine records slice 1's census names:

| | |
|---|---:|
| cascades separating the two readings | **0** |
| cascades with a non-empty `:added-by-organise` | 8 |
| of those, whose added set is disjoint from their own `:nodes` | **8** |

All eight are in `futon3:checks/snatch-cascade.edn`, and in every one the field
names patterns that are **not among that cascade's own nodes**, with no `:edges`
recorded at all — so the field there does not denote O1's second origin and
cannot exercise the question.

**And the emptiness is a fact about the constructor, not about which runs
happened.** The sole producer of an O1-shaped cascade writes the field as a
literal: `futon3:checks/construct_cascade.clj:412` is `:added-by-organise #{}`,
and `futon3:checks/construct_open_cascade.clj:145` reads that same value. No run
of it can separate the readings. The script FAILS if that literal is ever
replaced by a computed value, because the finding it supports would then be
stale.

So the separating witness has to be **constructed**, and §2 constructs it. This
is the asymmetry between the arms worth seeing: the selected reading is refuted
by the recorded data alone; the node-set and no-bootstrap readings are
indistinguishable on every cascade anyone has recorded.

## 4. What the review found and fixed

The dispatched module was sound on its core result and carried two defects,
both fixed here rather than re-dispatched.

- **Three names for one fact.** `noBootstrapConstructedEdge` was an alias of
  `selectedOnlyYieldsZaifEdgeWhenAdmittedSupplied` (`F12Conformance.lean:149`),
  and `noBootstrapYieldsZaifEdgeWhenAllNodesSupplied` was an alias of *that* —
  so R4's non-vacuity and R5's second half were machine-checked by nothing the
  slice added. Replaced by `noBootstrapRelationInhabitedOnConstructedInput`
  (`:162`), which routes through the no-bootstrap conformance and so shows the
  **field** inhabited rather than an implementation's edge set.
- **The comparison had no floor.** `organiseO3NoBootstrapZaif` (`:52`) could
  have been an equivalence between two empty relations. `zaifRecordedEdgeNonVacuous`
  (`:186`) now proves the recorded organised edge set is inhabited. This is the
  same defect slices 7, 8 and 10 each caught in their own arm.

Added while fixing the second: `noBootstrapYieldsZaifEdgeAtValueCarrier`
(`:200`), which states the half of the carrier contrast the module had left
implicit. **The reading is one proposition with two contents.** At `CascadeDiff`
the field is `selected ∪ admittedBy`, contains the nine recorded admissions, and
the recorded edge `18 → 19` is inside it. At the function carrier `Cascade`
(`Holes.lean:29-35`) has no `admittedBy`, the same reading is just the caller's
selection, and no conformant function reaches that edge (`:172`). Which content
the reading has is decided by D1, not by this choice.

## 5. Controls

Codex reported four, each plant verified present and each failing as required.
Five more were run in review against the module as repaired, each plant read
back out of the file before building:

| plant | observed |
|---|---|
| node-reading edge aimed at the unauthored pair `0 → 1` | `:103` `assumption` failed; `:104`, `:105` application type mismatch |
| emptiness theorem restated over `d1Nodes` (relation not empty there) | `:137`, `:138` type mismatch |
| non-vacuity witness moved to the recorded selected input | `:169` application type mismatch |
| value-carrier edge moved to `18 → 20`, and 20 is not a recorded node | `:205` type mismatch |
| **set difference dropped from the arm's own O3**, collapsing it to the node-set reading | `:124` application type mismatch, `:168` type mismatch, `:178`/`:180` invalid projection |

The last is the one that matters: the separation result depends on exactly the
set difference that defines the arm, not on anything incidental.

Five controls were also run against the checker, each plant verified first:

| plant | verdict moved |
|---|---|
| cascade whose added set overlaps its nodes AND an edge runs through it | separating `0 → 1` |
| same overlap, but the edge does NOT run through the added node | separating stays `0` |
| constructor field made a computed value | FAIL `:constructor-no-longer-fixes-the-field-to-a-literal-empty-set` |
| constructor no longer mentions the field | FAIL `:constructor-does-not-mention-the-field` |
| a census record missing from the tree | FAIL `:records-not-found` |

The second is a discriminator rather than a plant that merely breaks something:
it shows the check requires an edge *through* an organise-added node, not merely
an overlap.

## 6. Gates

`lake build DarkTower.WarMachine.F12O3FieldArm`: 2710 jobs, success, no warning
from the new module (the replayed `sorry` warnings are `Holes.lean`'s own).
`#print axioms` over all 21 declarations, the list generated from the file: 0
`sorryAx`, 4 depend on no axioms. 0 occurrences of `sorry`, `axiom` or
`native_decide`. `clj-kondo` 0/0 and `check-parens` clean on the new script;
checker byte-identical over two runs. No machine run, so no run-lock. No
`gen_aif_dag.bb` (TN §9a).

**`lake build DarkTower` is not this row's gate and is red at HEAD for reasons
outside it** — `DarkTower/MemoryArmPreregistration.lean` fails at 268:18 and
295:19 with `type expected`, in a file last touched on another lane. Slice 10
established this by moving its own new file aside and rebuilding. The module
target is what this row's slices have been clearing: slice 8 reported 2708 jobs,
slice 10 reported 2709, this slice 2710.

The registry compares **equal as a value** to HEAD outside the one new key, so
no signature is disturbed, and the entry was spliced into the file's own
formatting rather than re-emitted (a `pprint` rewrite would have dropped the
file's 204-line header).

## 7. What this does not settle

Every arm of `:organise-o3-field` now carries a measured cost and no slice has
anything left to run on it — separating the node-set and no-bootstrap readings
on real data needs a **producer**, not another arm, and
`construct_cascade.clj:412` is why there is none. The choice is Joe's.

Nothing else about `:F12` moved. D1 (which carrier the four O-laws are stated
of) is still unregistered and still blocks slice 4; D2 and D3 are untouched, with
D2's probe `:not-a-witness` and its gate red at HEAD (C547 §7); O4 is still not
stateable at the arm carrier; and the sorry at `Holes.lean:861` is still neither
discharged nor amended.
