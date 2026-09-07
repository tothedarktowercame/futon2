# C568 — F12 ruled carrier

F12 slice 4a. Joe ruled all six registered `:F12` choices on 2026-09-07
(`futon2:holes/labs/wm-contract/RULINGS-walkthrough-2026-09-07.md:1`, items 4-9;
transcribed into `futon2:holes/labs/wm-contract/aif-equations.edn:1` `:choices`).
This slice writes the ruled specification down in Lean for the first time. It
takes no ruling, edits no contract, and does not build the exemplar the ruling
commissions — that is slice 4b, and section 3 states exactly what it still owes.

## 1. The ruled specification

The ruled signature is at
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:17`: arm six's `CascadeDiff`
codomain (`mathlib4/DarkTower/WarMachine/F12D1Arms.lean:119`) with arm four's
support-grain `Set P` attribution argument
(`mathlib4/DarkTower/WarMachine/F12SupportArm.lean:26`). THIS COMPOSITION IS
CHECKED AND NOT ASSERTED: `futon2:holes/labs/wm-contract/f12_ruled_carrier_check.bb:1`
reads all three signatures out of their own sources and records them side by side
in `futon2:holes/labs/wm-contract/runs/F12-organise/17-ruled-carrier.edn:1`.

`ConformantOrganiseRuled` at
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:21` carries exactly seven
clauses, and the checker asserts that set BY NAME so a clause appearing or
disappearing is a hard failure. Five are arm six's
(`mathlib4/DarkTower/WarMachine/F12CascadeDiffArm.lean:16`), `oattr` is arm
four's (`mathlib4/DarkTower/WarMachine/F12SupportArm.lean:38`), and `o3` at
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:30` is the no-bootstrap
reading ruled at item 6, in slice 11's shape
(`mathlib4/DarkTower/WarMachine/F12O3FieldArm.lean:20`).

Joe's own reading of that set — "At the ruled CascadeDiff carrier the set is
`selected u admittedBy`" — is DERIVED from `o1` and `oattr` at
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:39`, not assumed.

## 2. Inhabited, and discharged on the recorded run

The witness is at `mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:48` and
satisfies all seven clauses at
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:62`. It differs from arm
six's witness (`mathlib4/DarkTower/WarMachine/F12CascadeDiffArm.lean:31`) in
exactly the way arm four's ruling requires: `admittedBy` comes from the ARGUMENT
rather than from the recorded lookup table `zaifAdmittedFor`.

`exists f, ConformantOrganiseRuled f` at
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:74` is the exact content
item 5's ruling names for the staged amendment. THE AMENDMENT IS NOT MADE:
`mathlib4/DarkTower/WarMachine/Holes.lean:861` is untouched and still reads
`:= sorry`; the checker pins `git log -1` on that file at
`61c4825dc3e373fd1b761b800814bf85f5770b88` and fails if it moves.

At the recorded zaif inputs the witness returns the twenty recorded nodes
(`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:79`), retains the eleven
selected (`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:85`), returns the
nine recorded admissions (`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:90`)
and carries the recorded organised edge 18 -> 19
(`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:95`).

## 3. What this slice does NOT witness, proved rather than said

O4's antecedent is false at the witness's recorded instantiation
(`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:101`), so the ruled O4
clause is unexercised there. READ ON ITS OWN THAT IS ONLY A FACT ABOUT THE
WITNESS, which writes both precedence fields as `[]`. What makes it a fact about
the RUN is two further theorems: the witness's two precedence fields equal the
recorded zaif fixture's
(`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:109`), and the fixture's own
precedence does not move
(`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:118`, which is
`armTwoO4AntecedentUnsatisfiable` at
`mathlib4/DarkTower/WarMachine/F12D1Arms.lean:120`). The flatness comes from the
producer: `futon3:checks/construct_cascade.clj:402` writes both vectors as `[]`
literals (`futon3:checks/construct_cascade.clj:420`).

THE CLAUSE IS NOT VACUOUS IN GENERAL, and that is proved: a function whose
precedence moves with acting order and score flat
(`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:136`) satisfies every other
ruled clause (`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:144`, against
the sans-O4 predicate at
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:124`) and is not conformant
(`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:154`), which locates the
failure at O4 alone.

SO WHAT IS STILL OWED IS THE EXEMPLAR, not the specification: item 4's ruling
commissions a real recorded run whose precedence moves and carries a score,
exercised through a conformant implementation at this signature. The corpus
declines it today: `futon3:checks/zaif-cascade.edn:167` carries
`:o4 :not-exercised-fewer-than-two-members-carry-a-play-grain-rule`, emitted at
`futon3c/scripts/zaif_cascade_gate.clj:552` and read through
`futon3:checks/construct_zaif_cascade.clj:192`. That is the two-rule-bearing-members
problem items 8 and 9 opened excursions on, and it is slice 4b's, not this one's.

## 4. Three negative controls, each located at the clause that catches it

- Misfiled admissions — `addedByOrganise` holds what `admittedBy` should
  (`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:161`), the misattribution
  the `:LA2` field amendment at
  `mathlib4/DarkTower/WarMachine/Holes.lean:833` added the field to prevent —
  caught by `oattr` at recorded vertex 11
  (`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:169`).
- A function that discards the attribution argument
  (`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:176`) — caught by `oattr`
  at recorded vertex 11
  (`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:180`). This is what arm
  four's input buys over arm six, so a predicate that did not catch it would not
  be the ruled one.
- A function that adds node 20 itself and fast-forwards THROUGH its own addition
  (`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:187`) — caught by the
  no-bootstrap `o3` (`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:221`),
  because node 20 is gone from `nodes \ addedByOrganise`. THIS IS THE CONTROL
  THAT SEPARATES THE RULED READING FROM THE NODE-SET ONE at
  `mathlib4/DarkTower/WarMachine/F12CascadeDiffArm.lean:23`, which admits it, so
  its failure is located at `o3` alone: every other ruled clause holds of it
  (`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:211`, against the sans-O3
  predicate at `mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:197`).

## 5. Recomputed evidence, and what the review checked

`futon2:holes/labs/wm-contract/f12_ruled_carrier_check.bb:1` reads the three Lean
sources, extracts the three signatures and the ruled clause names, checks the O3
difference textually in both directions (the no-bootstrap form present, the
node-set form absent), counts `sorry` / `axiom` / `native_decide`, pins the last
`Holes.lean` commit, and runs three source-mutation plants — removing `oattr`,
rewriting `o3` to the node-set reading, and inserting a `sorry`. Each plant moves
the verdict; the run fails closed if any does not. Artifact:
`futon2:holes/labs/wm-contract/runs/F12-organise/17-ruled-carrier.edn:1`.

REVIEW BY THE OWNING LANE, so the gate is auditable. The full diff of mathlib4
`2880e3bdec` and futon2 `26de8121` was read; `lake build` was re-run and the
axioms audit recomputed here rather than taken from the dispatch; the recorded
zaif fixture's precedence fields were read out of
`mathlib4/DarkTower/WarMachine/Holes.lean:981` by hand; the corpus's O4 decline
was read out of `futon3:checks/zaif-cascade.edn:167` independently of the Lean,
and its emitter located at `futon3c/scripts/zaif_cascade_gate.clj:552`.

TWO REVIEW FINDINGS, FIXED IN THIS LANE RATHER THAN RE-DISPATCHED. (a) As
delivered, the O4 vacuity was carried by
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:101` ALONE and this document
called it "the recorded O4 antecedent". It is not: that theorem is discharged by
`simp` against the witness's own `[]` literals, so no state of any record could
have made it false. This is the defect slice 24's own review found one file over
— a headline in a form the data cannot falsify — and it recurred here. The two
theorems at `mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:109` and
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:118` were added to tie the
witness to the record, and section 3 now says which theorem carries which claim.
(b) The no-bootstrap control at
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:221` refuted conformance
without showing WHICH clause refuted it, unlike its O4 sibling, so it did not
establish that the ruled `o3` is what separates the reading. The sans-O3
predicate and its other-clauses proof
(`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:197` and
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:211`) were added.
Docstrings recording which ruling each declaration implements were added
throughout; as delivered the module carried one, and the link from Joe's rulings
to the Lean lived only in this document.
