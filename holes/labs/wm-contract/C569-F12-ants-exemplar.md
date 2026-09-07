# C569 — F12 ants exemplar at the ruled carrier

## 1. What this recorded run exercises

The ants record says O4 holds and its precedence changed at
`futon3:checks/ants-cascade.edn:79-105`.  At the ruled signature,
`organiseAntsPrecedenceMoves` proves the antecedent true at
`mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean:54`.  This is what slice 4a's
zaif instantiation could not exercise: its record-anchored antecedent is false at
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:118`.

The ants run discharges O4 through the acting-order disjunct, not the score
disjunct.  The single theorem `organiseAntsActingMovesScoreFlat` proves both the
acting-order inequality and score equality at
`mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean:60`; the source record reports
`:score-changed? false` at `futon3:checks/ants-cascade.edn:105`.
`organiseAntsScoreMovesInstead` at
`mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean:132` is a CONSTRUCTED
alternative showing O4's right disjunct is reachable, NOT a second record.

## 2. What ants cannot stress

The record contains zero authored `@why` edges at
`futon3:checks/ants-cascade.edn:2` and explicitly marks
`:O2-invented-edge :no-authored-edge-in-library-ants` and
`:O3-dropped-edge :no-authored-edge-in-library-ants` at
`futon3:checks/ants-cascade.edn:58-60`.  Consequently
`organiseAntsNoOrganisedEdges` proves every organised-edge proposition false at
`mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean:72`; O2 and O3 are therefore
vacuous on this run.

## 3. Complementarity, bounded to the two fixtures

`zaifAntsComplementary` at
`mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean:82` states only a conjunction
about the zaif and ants fixtures: zaif carries organised edge 18 -> 19 while its
recorded precedence is flat; ants has moving precedence and no organised edge.
The zaif half is anchored in `wmZaifCascadeDiffFixture`, via
`ruledO4UnexercisedOnTheRecordedRun` at
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:118`, and the witness is tied
to that fixture by `organiseRuledZaifPrecedenceIsTheRecordedOne` at
`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:109`.  The claim that these
are the two relevant recorded runs belongs to the census at
`futon2:holes/labs/wm-contract/C539-F12-organise-census.md:69`, not to this proof.

## 4. Controls and review findings

The flat-acting negative control satisfies every clause except O4 at
`mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean:110` and is refuted by O4 at
`mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean:124`.  The score-moving
positive control is conformant at
`mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean:136`.

The owning-lane review corrected seven wrong citation spans covering eight
record values (four orders, two sets, and two score fields) in
`mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean:5` and replaced a
witness-literal account of zaif flatness with the record-anchored conjunction at
`mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean:82`.  The checker at
`futon2:holes/labs/wm-contract/f12_ants_exemplar_check.bb:1` now recomputes every
record span, record-to-index-map order, and score transcription.  Its four
source mutations flatten precedence, flatten acting order, insert `sorry`, and
shift a record pointer; every mutation changes the verdict in run record
`futon2:holes/labs/wm-contract/runs/F12-organise/18-ants-exemplar.edn:1`.

## 5. Still owed

Joe's item-4 library-scale exemplar remains owed.  The staged declaration at
`mathlib4/DarkTower/WarMachine/Holes.lean:861` is untouched; no ruling is taken
and no choice status is moved here.  The present library run declines O4 with
`:not-exercised-fewer-than-two-members-carry-a-play-grain-rule` at
`futon3:checks/zaif-cascade.edn:167`.  The condition is implemented at
`futon3c/scripts/zaif_cascade_gate.clj:442` and read into the constructor at
`futon3:checks/construct_zaif_cascade.clj:193`.  Material with three
rule-bearing members has since landed at
`futon2:holes/labs/library-loop/runs/mining-exemplar/cascade.edn:71`; running it
through the library-scale exemplar is not part of this slice.
