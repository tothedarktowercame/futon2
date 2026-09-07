# C565 — :F11 slice 7: F3 stated of the finder, and what its carrier cannot say

Row `:F11`, slice 7. Dispatched to codex-2 as
`invoke-1788787984911-13717-3a5b64a0`, premise recorded before the work in
`runs/F11-find/12-dispatch.edn`. Build: mathlib4 `37f245c9c2`. Review: this
slice's second mathlib4 commit. Run record:
`runs/F11-find/13-non-self-certifying.edn`.

**No ruling is taken and no choice is registered.** The three decisions that
would remove the sorry at `Holes.lean:264` (`:find-sorry`,
`:find-f2-receipt-carrier`, `:find-f4-reading`) remain Joe's and remain unruled.

## What was measured

F3 says a receipt cites **the pattern's** text or authored edges, never the
finder's score alone (`holes/problems/P-validated-R5.md:487`). Slice 4 measured
F2's carrier and found the blindness was in the carrier rather than in F2's
phrasing. F3 had never been measured that way; it is the half of the acceptance
a lane can still do without a ruling.

### 1. F3 is self-asserted (M1)

`Receipt` (`mathlib4 DarkTower/WarMachine/Holes.lean:240-242`) is two
propositions, and `Receipt.nonSelfCertifying` (`:244-245`) is their conjunction.
Both are the finder's to fill. `findAssertingF3`
(`F11NonSelfCertifying.lean:33`) fills them with `True` and `False`, carries no
citation at all, selects a recorded pattern on the 18-pattern repository
(`:42`), and satisfies F1–F3 (`:51`). So today's F3 costs a finder nothing: it
is the shape slice 6 named when it found the F4 conjunct self-certifying in the
way F3 forbids receipts to be.

### 2. The carrier cannot say WHICH text (M2, M4)

`CitesTheSelectedPattern` (`:267`) states F3's own WHICH off the finder's own
returned receipt. `findCFaithful` (`:274`) cites each selected pattern's own
text — the record's discipline, since the producer builds every warrant as
`warrant repository id`, the selected pattern's own entry
(`futon3/checks/find_organise.clj:213-220`, `:241-252`). `findCMisciting`
(`:288`) cites another recorded pattern's. Both select nonempty on the recorded
repository (`:301`, `:309`). The predicate separates them (`:317`, `:325`).

**And the separation dies at the carrier**: `findCErasuresAreEqual` (`:351`)
proves the two finders are EQUAL after erasing the citation, so no predicate
whatever on today's `FindResult` distinguishes a finder that cites the selected
pattern's text from one that cites a different pattern's. Today's F3 returns the
same verdict on both (`:393`).

This is a **second** blindness on a **second** field, not slice 4's re-exported —
see the review finding below.

### 3. Two F3 predicates live in the code, and the record cannot arbitrate (M3)

| where | conjuncts | body |
|---|---|---|
| the check's F3 leg, `futon3/checks/find_snatch.clj:167-171` | 2 | route ≠ `:score-alone`; `warrant.file` is a string |
| `receipt-cites-text?`, `futon3/checks/find_organise.clj:567-571` | 4 | the same two, plus `warrant.if-lines` is a vector and `warrant.if-text` is a string |
| the Lean column's producer, `u46_find_transcribe.bb:113-121` | 2 | transcribes **the check's** reading, not `find_organise`'s |

They are not equivalent as predicates (`F11NonSelfCertifying.lean:134`). On the
pinned fixture (sha256 `839897ef…`) both are true of all 96 receipts — 96 with
`:route :structured-antecedent`, 0 score-alone, 96 each carrying `:file`,
`:if-lines`, `:if-text`, `:however-lines` and `:however-text` — so **the record
cannot choose between them**. Note that `find_organise`'s two extra conjuncts
include `:if-lines`, which is exactly the field C559 measured as unstable: all
96 receipts differ in `:if-lines`/`:however-lines` alone when the fixture is
regenerated.

### 4. F3 and F2 are independent in the law (M5)

`findF2NotF3` (`:400`) selects recorded members with score-alone receipts:
satisfies presence-F2 (`:419`), refutes F3 (`:427`). `findF3NotF2` (`:444`)
selects and returns no receipts: satisfies F3 **vacuously** (`:462`), refutes F2
(`:468`). Both directions hold in the law. The record witnesses neither — see
R3 and R4 below.

## What the review changed

The handoff protocol makes the review a gate, not a rubber stamp. Six findings;
all fixed in review rather than re-dispatched.

- **R1 — the M2/M4 witnesses were slice 4's.** The delivered module proved its
  central results of `findRFaithful`/`findRMisattributing`, which differ in
  `acknowledgedClause`. That is F2's first ask, and `RelationalReceipt`
  (`F11ReceiptCarrier.lean:95-99`) carries clause, route and as-of and **no
  citation field** — so a finder that names the wrong clause is not a finder
  that cites the wrong text, and the delivered `AttributedF3` was slice 4's
  `ContentF2` renamed, with slice 4's two theorems verbatim beneath it. Fixed
  by building the citing carrier (`:240-261`) and its finders and proving the
  erasure equality there. The re-export is kept under its true name
  `ClauseAttributedF2` (`:219`) so the substitution stays auditable.
- **R2 — a decide over the record that read nothing from it.**
  `recordedRounds_pass_both_f3_readings` bound `row` and `p` and used neither;
  its body was a constant about a hand-authored literal whose `file` was the
  fixture path and whose `if-text` was invented text. Replaced by a statement of
  the one receipt it is about, transcribed field by field from the fixture
  (`file`, `:if-lines [15 16]`, the `:if-text` string), plus the reason it can
  only be one receipt: **`FindSnatchRowLit` (`Holes.lean:354-361`) carries no
  warrant, route or as-of field**, so no theorem over `findSnatchRounds` can
  decide either reading. The record's F3 data is exactly what the transcription
  drops.
- **R3 — only one of M5's two separations was ever available to the record.**
  The report says either could have been witnessed. The `nonSelfCertifying`
  column is a *filter* of the receipts map (`u46_find_transcribe.bb:113-121`),
  so it is a subset of `receipted` by construction and a round witnessing
  F3-without-receipted cannot be produced by that transcriber at all.
- **R4 — added: all three columns coincide.** On the record
  `selected = receipted = nonSelfCertifying` in all 34 rounds (`:200`), because
  the producer receipts exactly the firing patterns. So the record separates no
  two of F1, F2 and F3 from each other, and every separation in this module is
  carried by a constructed finder, never by a row.
- **R5 — two of the review's own controls did not fail, and were discarded.**
  Planting `ifLines := some (15, 16)` on `twoOnlyWarrant` left `F3Four` false,
  so the witness still separated the readings; planting
  `row.receipted = row.receipted` *weakens* the theorem and therefore compiles.
  Both were replaced and the reason is recorded in the control script.
- **R6 — the checker's own line-number defect.** Its `find`-is-still-a-sorry leg
  read line 264 by index, so a control that inserted one line above it read as a
  discharged sorry. Re-anchored to the declaration. This is the C559 line-shift
  class, caught inside this slice's apparatus.

## Gates

`lake build DarkTower.WarMachine.F11NonSelfCertifying` exit 0, 2707 jobs, zero
warnings from the new module. `#print axioms` over all 48 declarations: 19
axiom-free, 29 using `propext` alone or `propext, Classical.choice, Quot.sound`,
0 `sorryAx`. Zero `sorry`, `axiom`, `native_decide` in the module. `Holes.lean`
unchanged at `61c4825dc3e373fd1b761b800814bf85f5770b88`, line 264 still
`:= sorry`. `f11_non_self_certifying_check.bb` PASS, 0 findings, byte-identical
over two runs. 11 Lean plants all failing as required; 6 checker plants each
moving exactly one verdict.

## What this does not settle

Which reading F3 is, and whether the citing carrier should be adopted. Note that
`:find-sorry`'s sixth arm `:amend-the-carrier` is priced over the F2 and F4
halves and carries **no citation field** — so the amendment as priced would
leave the blindness measured here in place. That is a fact about the pricing,
not a proposal to change it.
