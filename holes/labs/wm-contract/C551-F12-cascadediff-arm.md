# C551 — `:F12` slice 13: the sixth arm run, and what its extra law is worth

Item `:F12` (`worklist.edn`), slice 13. C550 §8 named this slice: run the sixth
arm of `:organise-carrier` — conformance of a FUNCTION at the `CascadeDiff`
codomain — "with the vacuity of its O4 clause on the recorded run as the thing to
measure rather than to assume."

**No ruling is taken and none is implied.** The registry entry still carries
`:status :observed-not-decided` and no `:ruling` key. `Holes.lean` and
`holes-contract.json` are untouched (`git log -1` on both still reads
`61c4825dc3` and `50fa53469a`), so C542's contract pin does not move. The sorry
at `Holes.lean:861` is neither discharged nor amended.

## 1. The arm the registry named is the arm that was run

C550 §3's finding was that `armTwoOrganiseType` (`F12D1Arms.lean:119`) is an
abbrev **no theorem in any F12 module mentions**, so what the build checked of it
was well-formedness. That reproduced at slice start: `grep -rn armTwoOrganiseType
--include=*.lean` over mathlib4 returned its own declaration line and nothing
else. It is now used 15 times in the new module, recomputed by the checker rather
than counted by hand (`:organise-type :arm-two-uses-in-the-arm-file`), and the
checker FAILS if the arm file declares its own signature synonym instead.

## 2. All four laws are jointly statable here, and at no other arm's carrier

`ConformantOrganiseCascadeDiff` (`F12CascadeDiffArm.lean:16`) carries O1 three-way,
O2, O3 in the node-set reading, O4, and two fidelity clauses. All six are
discharged of one witness at the recorded inputs, with no `hrepo` hypothesis of
the kind arm one pays at `F12D1Arms.lean:106` and `:113`
(`cascadeDiffRecordedAllLaws`, `F12CascadeDiffArm.lean:266`).

That the O4 half of this is unique to the carrier is a measurement, not a reading:
the checker takes four field lists out of their own sources — `Cascade` five
(`Holes.lean:29-34`), `Repository` three (`Holes.lean:121-124`), `ArmOneCascade`
seven (`F12D1Arms.lean:74-81`), `CascadeDiff` twelve (`Holes.lean:846-858`) — and
finds the six O4 fields present in `CascadeDiff` and in nothing else. Removing
`scoreAfter` from `CascadeDiff` in a planted copy turns that verdict over.

## 3. The extra law is satisfied vacuously on the recorded run, measured three ways

**Generally, over every function.** `o4VacuousWhenPrecedenceFlat`
(`F12CascadeDiffArm.lean:100`) quantifies over `f`: at any input whose two
precedence fields agree, the O4 implication holds however the acting order and the
score behave.

**By a second witness.** `organiseCascadeDiffRecordedVariant`
(`F12CascadeDiffArm.lean:129`) moves its acting order and its score with the
precedence flat, and is conformant under the full predicate
(`:141`) and under the O4-free one (`:154`); the natural witness is conformant
under both as well (`:49`, `:122`). `recordedVariantsExposeO4Vacuity` (`:161`)
shows the pair agreeing on nodes, organised edges and both precedence fields while
differing on exactly the two fields O4's conclusion reads. So on this run the
clause separates nothing. Written as an explicit second PREDICATE rather than as
"delete the clause and watch the build break" — a deletion is not something a
later reader can cite, which is the repair slice 8 made at
`F12AttributionArm.lean:146`.

**Across the corpus, recomputed from the records rather than read off a
`:holds?` key.** Nine cascade records under `futon3:checks`: **exactly one**
carries an O4 row whose antecedent is true — `ants-cascade.edn`, where the acting
order moves (`pheromone-trail-tuner` and `white-space-scout` swap) and the score
does not, so the disjunction holds on its left. **Six decline with a reason** and
**two carry no `:o4` key at all**. The zaif run every D1 arm is instantiated at is
one of the six that decline
(`:not-exercised-fewer-than-two-members-carry-a-play-grain-rule`).

So the vacuity is a fact about **which run was transcribed**, not about the
statement's shape. Two things fix that reading rather than leave it as a
comment. The clause is **not** vacuous in general:
`organiseCascadeDiffO4Counterexample` (`:172`) moves its precedence with acting
order and score flat, satisfies every other clause (`o4CounterexampleOtherClauses`,
`:184`) and is not conformant (`o4CounterexampleNotConformant`, `:196`) — the pair
locates the failure at O4 and nowhere else. And the vacuity is **not an artefact
of the Lean transcription**: the Clojure law this clause mirrors,
`fo/o4-precedence-governance` (`futon3:checks/find_organise.clj:531`), is written
as a disjunction whose first disjunct is `(= precedence-before precedence-after)`,
read out of the source by the checker — it passes on a flat precedence for the
same reason. The checker also FAILS if
`futon3:checks/construct_cascade.clj:420-421` ever stops writing both vectors as
literals, because the finding would then be stale.

## 4. The dispatch packet's expectation about `oauth` was refuted, and the refutation is now a positive result

`CascadeDiff` is the only carrier in this family with its own `authoredEdges`
field, so the packet expected that without a clause pinning it, O2 and O3 could be
satisfied against a relation the function invented for itself. **They cannot.**
At the function framing O2 and O3 read `repo.standsOn`, the ARGUMENT, so O2 alone
forbids every organised edge unreachable in the supplied repository
(`sansOAuthStillForcesRepositoryReachability`, `F12CascadeDiffArm.lean:218`).

What dropping `oauth` permits is drift in the result's own field
(`organiseCascadeDiffUnpinnedAuthored`, `:225`, conformant sans-`oauth` at `:236`,
denying authored edge `18 → 19` at `:248` while the repository and the organised
relation both contain it). Review turned that into the statement it implies:
`pinAuthored` (`:306`) overwrites that one field and **nothing else** — eleven
`rfl`s at `pinAuthoredAgreesOutsideAuthoredEdges` (`:312`) — and any function
conformant without the clause becomes conformant with it under that overwrite
(`pinAuthoredIsConformant`, `:340`), including the drifting witness
(`pinAuthoredRepairsTheDriftingWitness`, `:354`).

**So at this framing `oauth` constrains no behaviour of `organise`: it pins a
result field that no other clause reads.** That is checked textually, not
asserted — the checker scans every clause of the predicate and finds `oauth` the
only one mentioning the result's `authoredEdges`, and FAILS if a law clause ever
does (a planted `o2` reading the field is caught on three counts). It also checks
the contrast: the value-grain O2 at `Holes.lean:1004-1006` **does** read the
field, because no repository is in scope there to read instead. The honest reading
of the twelfth field at this arm is: carried, and unread by the laws.

## 5. What the `Score` parameter actually costs

C550 §2 and the registry record arm two's price as "a `Score` type parameter the
declared signature does not have", which reads as a notational inconvenience. It
is more than that, and the difference is provable. `organise`'s declared type has
an implementation at every instantiation — `organiseSelectedOnly`
(`F12Conformance.lean:59`) is one — and this arm's type does not: at `Score :=
Empty` it is **empty** (`cascadeDiffArmIsEmptyAtAnEmptyScore`,
`F12CascadeDiffArm.lean:292`), because a total function at this signature must
produce two `Score` values on inputs that carry none, and the recorded run carries
none (`futon3:checks/construct_cascade.clj:402`, fields at `:420-421`). The
comparand is stated beside it (`organiseTypeIsInhabitedAtTheSameArguments`,
`:300`) so the emptiness is a cost rather than a curiosity. That is why the
witness takes `[Inhabited Score]` and writes `default` twice
(`F12CascadeDiffArm.lean:31`, `:45-46`).

## 6. Two pointer defects found in review, and one of them was the packet's

`zaifAdmittedFor` is at `F12AdmittingArm.lean:48`; two docstrings cited it at
`:50`. **That number came from the dispatch packet**, which asserted it, and the
dispatched seat copied it — the packet's pointers are gated by nothing, since
`pointer_check.bb` scans the ledger and the notes and never sees the dispatch
text. The second: `d1Selected` was cited at `F12D1Arms.lean:16`, its docstring
line, rather than at `:17` where it is declared — the same one-line drift C550 §5
tightened in two places. Both repaired here rather than re-dispatched; every
`file:line` in the module and in this note was printed from its file before being
written.

## 7. Controls

**From the dispatched seat, four**, each with the plant verified present in a copy
before building. Two were re-run independently in review and reproduced: breaking
the recorded organised edge fails `organiseCascadeDiffZaifEdge` ("expected type
`False` has no constructors") along with O2 and O3, and flattening the O4
counterexample's precedence fails `o4CounterexampleNotConformant` at its
antecedent — so that non-conformance depends on the precedence moving.

**Against the review additions, three**, each plant read back out of the planted
copy first: clobbering `nodes` inside `pinAuthored` breaks both
`pinAuthoredAgreesOutsideAuthoredEdges` and `pinAuthoredIsConformant`; replacing
`Empty` with `Nat` breaks the emptiness theorem (no `Nat.elim` to project), so it
depends on the empty type and not on the shape of the argument; and making
`pinAuthored` the identity breaks `pinAuthoredIsConformant`'s `oauth` field, so
that theorem depends on the pin.

**Against the checker, six**, each moving the verdict: an O3 that differs between
the full and sans-O4 predicates (`:sans-o4-predicate-differs-outside-o4`), a
fresh signature synonym in place of the registered abbrev, `CascadeDiff` losing
`scoreAfter` (`:o4-is-statable-somewhere-else-too`), the producer's precedence
literal made computed, a declaration renamed with a suffix — which is the
prefix-match trap slice 7 found, here confirmed to report
`:witness-conformant` missing while `organiseCascadeDiffConformantSansO4` sits
beside it — and an `o2` clause rewritten to read the result's `authoredEdges`.

## 8. Gates

`lake build DarkTower.WarMachine.F12CascadeDiffArm`: 2708 jobs, 0 errors, no
warning from the new module. `#print axioms` over all **31** declarations,
generated from the file rather than hand-listed: 0 `sorryAx`, 9 depending on no
axiom, the rest on `propext`, `Classical.choice` and `Quot.sound` only. Zero
`sorry`, `axiom` and `native_decide` tokens. clj-kondo 0/0 and `check-parens` OK
on the new checker; the checker regenerates `08-cascadediff-arm.edn`
byte-identically over two runs. `negative_controls.sh` PASS and `pointer_check.bb`
0 unresolved, run on the working tree before the ledger commit. No
`gen_aif_dag.bb` (TN §9a). No machine run, so no run-lock.

`lake build DarkTower` is red at HEAD outside this row
(`DarkTower/MemoryArmPreregistration.lean`, another lane), as slice 10 established
by moving its file aside; the module target is the gate this row clears.

## 9. What this does not settle

D1 is still open and this slice does not close it — what it adds is that its sixth
arm now carries measured `:buys` and `:costs` where it carried `:unmeasured`, so
every arm of `:organise-carrier` has been run and the entry now carries
`:all-arms-run` as its two sibling F12 entries do. D2 (probe `:not-a-witness`,
gate red at HEAD, C547 §7), D3, and the `:organise-o3-field` choice are untouched.
**The choice is Joe's, and there is nothing left for a slice to run on it.**

**Next slice**: nothing on `:organise-carrier`. What remains in the item is D2,
D3, and the sorry at `Holes.lean:861`, which is still neither discharged nor
amended — and slice 1 established that discharging it changes no proof obligation,
since no term in `DarkTower/*.lean` references `organise` outside its own
declaration.
