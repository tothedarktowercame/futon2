# C562 — `:F11` slice 4: what F2's receipt carrier would cost, and why the carrier and not the phrasing is what blinds F2

Slice 3 registered `:find-sorry` with five arms priced and a sixth,
`:amend-the-carrier`, reading `:buys :unmeasured` and `:costs :unmeasured` — not
because nobody had looked, but because the two questions inside it, **what a
`Receipt` holds** and **which of two readings F4 is**, were not registered as
choices at all. This slice prices the first. The second is slice 5; splitting
them is deliberate (`runs/F11-find/06-dispatch.edn`), because pricing two
questions in one packet invites the measurement of one to be assumed from the
other, and slice 3 already found the two F4 readings disagreeing in both
directions.

**No ruling is taken and no contract moves.** The new entry
(`aif-equations.edn` `:choices` `:find-f2-receipt-carrier`) carries `:status
:observed-not-decided` and no `:ruling` key. `Holes.lean` is untouched: `find`
at `:264` is still a sorry and `Receipt` at `:240-242` still has its two fields.

## 1. The gap, stated with both ends

`P-validated-R5.md:486` asks each selected pattern's receipt to carry three
things: *the tension clause it acknowledges (IF/HOWEVER overlap), the retrieval
route, and an as-of*. `structure Receipt`
(`mathlib4/DarkTower/WarMachine/Holes.lean:240-242`) carries `citesTextOrEdges :
Prop` and `scoreAlone : Prop` and none of the three, which is why
`ConformantFind.f2Receipted`
(`mathlib4/DarkTower/WarMachine/F11Conformance.lean:25-26`) states receipt
PRESENCE only — `((f t repo).receipts p).isSome`.

The record is the other end, and it is not empty: **96** receipts over **34**
rounds in **6** scenarios (`futon3:checks/find-snatch.edn` at futon3 `7c653bb`),
each carrying `:if`, `:however`, `:route`, `:state-fields` and a `:warrant` with
`:file`, `:if-lines`, `:if-text`, `:however-lines`, `:however-text`. Recomputed
by `f11_receipt_carrier_check.bb`, recorded in
`runs/F11-find/07-receipt-carrier.edn`.

## 2. Presence-F2 is blind to attribution, and the blindness is in the carrier

The first measurement is `findSnatchMisattributingConformant`
(`mathlib4/DarkTower/WarMachine/F11ReceiptCarrier.lean:26`): a finder that
issues every selected pattern a receipt satisfies F1–F3 as stated. F3 does not
help, because `Receipt.nonSelfCertifying` (`Holes.lean:244-245`) reads two
propositions and neither is about which pattern the receipt is for.

The *sharp* form of this is not "presence-F2 accepts a bad finder" — it is that
**no predicate on today's `FindResult` can tell the two apart at all.**
`findRFaithful` and `findRMisattributing`
(`F11ReceiptCarrier.lean:159`, `F11ReceiptCarrier.lean:172`) return receipts
that carry data: one names the pattern it is for, the other names a different
pattern. `findRErasuresAreEqual` (`F11ReceiptCarrier.lean:186`) proves the two
are **equal** after erasure to today's carrier. So the conformance verdict is
not merely the same for both (`findRMisattributingErasureConformant`,
`F11ReceiptCarrier.lean:217`); it *cannot* differ, for any predicate whatever.
What blinds F2 is the carrier and not how F2 was phrased — which is exactly what
an arm about amending the carrier needs to know.

Under a content-stating F2 the pair comes apart: `findRFaithfulContentF2`
(`F11ReceiptCarrier.lean:222`) holds and `findRMisattributingFailsContentF2`
(`F11ReceiptCarrier.lean:230`) fails, the refutation witnessed at
`askForSurplusNotSurrender`, which g1Snatcher's recorded row selects
(`Holes.lean:628-630`), on the full recorded 18-pattern repository
(`F11Conformance.lean:35-38`) and not a degenerate one.

A detail worth recording: the wrongly named owner is
`consultTheRemedyBeforeExiting`, which is g1Snatcher's **declared zero-mass
pattern** in the record (`futon3:checks/find-snatch.edn`, first scenario's
`:f4`). So the receipt a presence-only F2 accepts names the one pattern that
scenario's F4 says the finder must not return.

## 3. Adding the three requirements as propositions buys nothing

`ReceiptWithAssertions` (`F11ReceiptCarrier.lean:73`) is the cheap version of
the amendment: `Receipt` plus `acknowledgesClause : Prop`, `hasRoute : Prop`,
`hasAsOf : Prop`. `assertionFieldsDoNotFixAttribution`
(`F11ReceiptCarrier.lean:87`) proves all three can be true on a receipt whose
owner is the wrong pattern. A `Prop` field says that something is the case
without carrying WHICH, so this arm adds three fields and no discrimination.
The arms are therefore not ordered by how much they add to the structure but by
whether what they add is DATA a law can compare against the tension — which was
written down before the result, in `runs/F11-find/06-dispatch.edn`.

## 4. What the amendment would actually cost

Three costs, each recomputed rather than asserted.

**(a) It is a change to `find`'s return type, not only to `Receipt`.**
`FindResult.receipts` is typed `P → Option Receipt` (`Holes.lean:247-250`), so a
receipt that carries data changes the type of the declaration the row is trying
to discharge. `FindResultR` (`F11ReceiptCarrier.lean:131`) is that shape and
`FindResultR.erase` (`F11ReceiptCarrier.lean:141`) is the map back, which is how
this slice states the measurement without touching `Holes.lean`.

**(b) Mechanically it is small: SEVEN sites, ONE of them outside the modules
built to price this question.** A field without a default breaks every
construction, and the reliable marker is the field rather than the type name —
`: Receipt where` finds only the named-type form and misses `some {
citesTextOrEdges := True, … }`, which is how the finders build theirs. The one
site outside the arm module is `findStructuredReceipt`
(`F11Conformance.lean:66-68`), reused by `F11DischargeArm.lean:107`. The census
excludes `DarkTower/APMCycleMachine.lean:87`, which declares a `Receipt` of its
own, and does not count `Holes.lean:1209`, where `Receipt` is a bound type
parameter of `Handoff` (`Holes.lean:1201`).

**(c) The transcription is the real cost: 96 warrants, none of them in Lean
today.** `FindSnatchRowLit` (`Holes.lean:354-360`) carries scenario, round,
selected, receipted, nonSelfCertifying and absence — no warrant, no route, no
as-of: **not found**. `FindReceiptRow` (`Holes.lean:252-259`) has no such column
either, so stating content-F2 against the row carrier instead of `Receipt` is
not available. This is why the M3 inhabitance witness is hand-carried
(`handCarriedReceipt`, `F11ReceiptCarrier.lean:102`) and says so, rather than
pretending the record is already transcribed.

## 5. Two things the record could not supply if asked today

- **The as-of is not per receipt.** `futon3:checks/find-snatch.edn` carries one
  `:as-of` for the fixture (futon3 `2734ac57…`) and **0** of the 96 receipts
  carry one. A per-receipt as-of field would be a field the record cannot fill.
- **The route would not discriminate.** All **96** receipts record
  `:route :structured-antecedent` — one value. A `route` field would be
  faithful and, on this record, would separate nothing.
- Relatedly, all 96 carry `:state-fields :not-instrumented`.

And one from C559 that bears on *which* warrant fields to carry: the 96 receipts
already fail to reproduce in `:if-lines`/`:however-lines` alone, with no
`:if-text` or `:however-text` differing, because library edits shifted lines. A
data arm that transcribed the LINE NUMBERS would import that fragility; the
clause TEXTS did not drift.

## 6. Review findings

The dispatched module built the first measurement and one that did not hold up.

1. **`ReceiptContentAttributed` (`F11ReceiptCarrier.lean:58`) cannot price F2.**
   It takes its `owner` from outside the finder, so at the identity it holds of
   every finder in the type — now a theorem,
   `receiptContentAttributedHoldsOfEveryFinder` (`F11ReceiptCarrier.lean:126`),
   kept in the module so the reason for the replacement stays auditable rather
   than being silently deleted. `findSnatchMisattributingFailsContentF2`
   (`F11ReceiptCarrier.lean:63`) therefore refutes a choice of auxiliary
   function and not a property of the finder. Repaired by stating F2 of the
   finder's own output (§2), which also produced the erasure-equality result the
   original framing could not reach.
2. **The axiom audit was not run.** codex-1 reported this itself rather than
   claiming the gate. Run at review over all **25** declarations: **0**
   `sorryAx`; 11 axiom-free, 1 `propext` only, 13
   `propext`/`Classical.choice`/`Quot.sound`.
3. **The M3 witness was weaker than the question.** `handCarriedReceipt` shows a
   receipt can hold the right clause; it does not show a FINDER can. Content-F2
   is now inhabited by a finder (`findRFaithfulContentF2`).
4. **The edit-site census undercounted.** Matching `: Receipt where` misses the
   anonymous-constructor form; corrected to the field marker, 2 sites became 7.
5. **One pointer of my own was wrong when written** — `Holes.lean:363-366` for
   g1Snatcher's recorded selection is `FindSnatchRowLit.toRow`; corrected to
   `Holes.lean:628-630` in `f11_receipt_carrier_controls.sh`. All nine distinct
   pointers in the Lean module were then checked against the files at HEAD.

## 7. Controls

`f11_receipt_carrier_controls.sh` — **6** plants against the Lean module, each
of which must make it fail to elaborate, and all 6 do. Each verifies the planted
text is present AND the replaced text gone **on the planted line**, because
these strings recur (`citesTextOrEdges := True` in four receipts,
`.askForSurplusNotSurrender` in seven places) and a file-wide absence test would
pass vacuously — the slice-3 defect. Plants are located by DECLARATION NAME, not
by line number, so appending to the module does not retarget them silently.
Control 4 breaks a field that survives erasure and `findRErasuresAreEqual` fails,
which is what shows that theorem tests the erasure rather than holding of any two
finders; control 6 moves the witness to `consultTheRemedyBeforeExiting`, absent
from g1Snatcher's recorded selection, and the membership claim fails.

`f11_receipt_carrier_controls_checker.sh` — **5** plants against the checker
itself, each of which must move its own verdict: a field added to `Receipt`, a
warrant field added to the transcription, a per-receipt `:as-of` planted in the
fixture, a warrant key renamed, and a cited declaration renamed. All 5 fail on
the named check. The first fires two checks rather than one, both correct: a
field called `acknowledgedClause` changes the field list AND makes the carrier
hold what F2 asks.

## 8. Gates

- `lake build DarkTower.WarMachine.F11ReceiptCarrier`: **2706 jobs, exit 0**, no
  warning from this module (the `sorry` warnings are `Holes.lean`'s ten
  pre-existing ones, `:264` among them).
- `#print axioms` over all **25** declarations: **0** `sorryAx`.
- **0** `sorry`, `axiom`, `native_decide` tokens in module code.
- `f11_receipt_carrier_check.bb`: PASS, artifact byte-identical over two runs
  (`8548be8cffb705…`).
- `negative_controls.sh`: PASS (133 negative, 53 positive; shared registries
  untouched). `pointer_check.bb`: 3250 pointers in 6 files, 0 unresolved.
- `Holes.lean` untouched; `find` at `:264` is still a sorry.

## 9. What this does not settle

It takes no position on whether `Receipt` should be amended — the entry prices
arms and carries no `:ruling`. It says nothing about F4's reading, which remains
unregistered and is slice 5's work; until that entry exists,
`:find-sorry`'s `:amend-the-carrier` arm stays `:unmeasured`, now for one reason
rather than two. It does not claim the arms are exhaustive. And it takes no
position on the pin-vs-live divergence C559 recorded: the Lean transcription is
at the pinned 18-pattern era while the live library carries 24, which belongs to
C500 s3's fixture-pin defect.
