# C566 -- :F11 slice 8 review: the remainder mechanised, and the row is not waiting on Joe

Reviewer: claude-wm-edge (wm-edge worklist lane). Author: codex-1, job-id
`invoke-1788790055541-13752-6a1c2cf5`, dispatched by `wm-build-work`.
Premise recorded before the work: `runs/F11-find/14-dispatch.edn` (futon2
`28cf1e74`, job-id named at `317981ce`).

## The result

`runs/F11-find/14-remainder.edn` reports `:remainder-fully-gated? false` with

    :obligations-not-satisfied-and-not-gated
    [:f3-citation-stated-in-lean :witnessed-on-real-find-over-committed-library]

**The dispatch premise is REFUTED, and so is slice 7's closing claim.** Slice 7
ended the row with the prose sentence *"nothing in the acceptance is left that a
lane can take without a ruling"* and *"the row is waiting on Joe, not on work."*
Two obligations of `:F11`'s acceptance are unmet at HEAD and neither is gated on
any registered undecided choice. Registering a choice or completing a pricing is
lane work, not a ruling, so `:F11` is **not** blocked on Joe.

## Independent re-adjudication of the two named candidate refuters

The dispatch named both refuters in advance so that finding them could not be
back-fitted. Both are confirmed, and the reviewer re-measured both from the
artifacts rather than reading the checker's verdict.

### (i) `each witnessed on a real find over the committed library` -- UNMET, UNGATED

- The Lean witnesses are all proved over `snatchRepository`
  (`/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:319-327`), which is
  an **18-element** list, lifted to `findSnatchRepository`
  (`F11Conformance.lean:35-38`).
- The committed library carries **24** patterns: `ls
  /home/joe/code/futon3/library/snatch/*.flexiarg | wc -l` = 24 at futon3
  `7c653bb`. The six the pin does not have are named at
  `runs/F11-find/02-reconciliation.edn:15-21` -- `grim-cuts-the-cascade-and-never-widens-it`,
  `have-a-temperament`, `lead-with-the-exchange-rule`,
  `play-the-authored-order-first`, `promote-the-remedy-before-the-exit`,
  `widen-the-cascade-only-on-evidence`. The reviewer's own directory listing
  agrees with that list member for member.
- The recorded fixture is at the pinned era too: `checks/find-snatch.edn`'s own
  `:repository` field lists the same 18, and its sha256 is
  `839897ef8fe44952403700bd237389449ae4735d3da7df8239b1b94dc7ef4dfa`, unchanged
  since slice 6 recorded it.
- **No registered choice gates it, and all three say so in their own words.** The
  reviewer read the `:not-claimed` of each F11 choice in `aif-equations.edn`:
  `:find-sorry`, `:find-f2-receipt-carrier` and `:find-f4-reading` each end with
  the sentence *"It takes no position on the pin-vs-live divergence C559
  recorded ... which belongs to C500 s3's fixture-pin defect."* A defect that
  three registry entries explicitly disclaim is not gated by any of them.

### (ii) F3's citation attribution -- UNMET, UNGATED

- `structure Receipt` (`Holes.lean:240-242`) carries `citesTextOrEdges : Prop`
  and `scoreAlone : Prop` and no field naming WHICH text is cited;
  `Receipt.nonSelfCertifying` (`:244-245`) is their conjunction.
- Slice 7 proved that no predicate on today's carrier can separate a finder that
  cites each selected pattern's own text from one that cites another recorded
  pattern's: `findCErasuresAreEqual`
  (`F11NonSelfCertifying.lean:351`).
- The one arm that would move the carrier, `:find-sorry`'s
  `:amend-the-carrier`, is priced over the F2 and F4 halves only and carries no
  citation field -- slice 7 recorded this under NOTED NOT ACTED ON, and slice 8
  emits `:registry-choice-for-citation-field :not-found`. The reviewer confirmed
  it: no `:choices` entry in `aif-equations.edn` poses the citation field as a
  question.

## What the review changed

Four things the delivered checker asserted where the packet asked it to measure.
The verdict is unchanged by all four repairs -- which is the point: it is now
*computed* to be what it was *stated* to be.

**R1. Four obligations carried a literal `:satisfied-at-head? false`, one of them
half the headline result.** In the delivered `f11_remainder_check.bb`,
`:f2-content-stated-in-lean` (`:122`), `:f3-citation-stated-in-lean` (`:127`),
`:f4-stated-in-lean` (`:129`) and `:sorry` (`:140`) were constants in the
source, not functions of the tree. The packet's non-degeneracy floor demanded
that "every obligation must be shown SEPARATING, by a control that makes exactly
that obligation flip"; a constant cannot flip in any world. This is the
degenerate shape the packet pre-named, pointing the other way -- vacuously
FALSE rather than vacuously true -- and it lands on the result:
`:f3-citation-stated-in-lean` is one of the two entries in
`:obligations-not-satisfied-and-not-gated`, so half the headline was an
assertion in the checker's own source. The sibling does not do this:
`f12_remainder_check.bb` computes `:sorry`'s satisfaction from the body, which is
why its `:organise-body-replaced` control shows `:satisfied-at-head?` going
false-to-true. Repaired by measuring each:

- `:sorry` from the body, as F12 does -- the existing `:replace-find-body`
  control now flips `:satisfied-at-head?` and not only `:body-is-still-sorry?`;
- `:f2-content-stated-in-lean` and `:f3-citation-stated-in-lean` from the fields
  of `structure Receipt` (`Holes.lean:240-242`), parsed from the struct block:
  F2's three asks and F3's citation datum are absent, which is *why* they are
  unmet, and a control that adds the field flips each;
- `:f4-stated-in-lean` from how many of the three readings are present in the
  tree (`FindFalsifiable` `F11Conformance.lean:31`,
  `FindExcludesRecordedZeroMass` `F11DischargeArm.lean:59`,
  `FindRespectsZeroMass` `F11F4Reading.lean:26`) -- unmet because three
  inequivalent readings stand and none is selected, and a control that removes
  two flips it.

**R2. The two candidate refuters' `:adjudication` values were literals too**
(`:144`, `:150`), so the key the packet added specifically to be able to come
out the other way could not. Repaired by deriving each adjudication from its
obligation's measured satisfaction and its gating vector, so
`:make-live-fixture-match-pin` now moves `:pinned-versus-live-library` from
`:confirmed-unmet-and-ungated` to `:refuted-obligation-met`.

**R3. A pointer that resolves but does not point at the evidence.** Both the
library obligation and its refuter cited
`runs/F11-find/02-reconciliation.edn:13`, which is `:receipts-compared 96`. The
fields the verdict rests on are `:receipts-differing` (`:14`),
`:repository-count-live` (`:22`) and `:repository-count-pin` (`:23`).
`pointer_check.bb` tests resolvability, not aim, so the gate was green on a
pointer aimed one line off. Repaired to cite the counts the claim is about.

**R4. One control's plant was not verified.** `:invent-f3-gate` passed the
literal `true` as its `plant-verified?` argument (`:253`) where the other six
compute it. Repaired to verify the invented fragment is genuinely absent from
the live `:find-sorry` question.

## A limit, recorded and not repaired

The `each witnessed on a real find over the committed library` obligation is
decided from slice 1's run record (`02-reconciliation.edn`), not recomputed from
the live library. The number is right -- the reviewer counted the live directory
independently and got 24, and the six added patterns match the record member for
member -- but the checker inherits it rather than measuring it. Recomputing the
library count in-checker would make this obligation depend on a sibling
repository's working tree at every run, which the lane has not decided to do;
recorded here so the next slice can take it deliberately.

## Gates
Re-run by the reviewer, not inherited from the packet's report:

- `bb f11_remainder_check.bb` clean; artifact byte-identical over two
  consecutive runs, sha256 `6caab325be8f147bea6c1b58039ca4a0a7c91f91b2ec212c1a2b4f894bfa2548`.
- `bash f11_remainder_controls.sh` PASS (nine internal controls, seven with a
  recorded before/after and two with the observed hard failure quoted; the
  delivered seven plus the two the review added).
- `bash f11_remainder_controls_checker.sh` PASS -- seven plants against the
  checker from outside it, each into a copy of a file the checker reads via its
  own env var, each moving EXACTLY ONE obligation verdict.
- `bash p4ng/empirics-futon/negative_controls.sh` PASS (133 negative, 53
  positive; shared registries untouched).
- `bb p4ng/empirics-futon/pointer_check.bb` -- 3362 pointers in 6 files, 0
  unresolved.
- `clj-kondo --lint f11_remainder_check.bb` 0 errors 0 warnings;
  `check-parens` OK.
- mathlib4 is untouched by this slice: `Holes.lean` is still at
  `61c4825dc3e373fd1b761b800814bf85f5770b88` and `:264` is still `:= sorry`.
- `aif-equations.edn` and `control-map-edges.edn` are untouched. This slice
  registers no choice and takes no ruling.

## One obligation the controls cannot separate, recorded

`:f1-stated-in-lean` is the only obligation with no plant that moves it alone.
Its evidence is `structure ConformantFind` (`F11Conformance.lean:21`), which is
also one of `:conformant-implementation`'s three pointers, so removing it moves
both. The two obligations are not independently separable on today's evidence.
That is a fact about the decomposition, not a gap in the controls, and it is
written down rather than worked around.

## What this means for the row

`:F11` stays `:status :open`. The next slice is **lane work, not a ruling**:
either close the pin-vs-live divergence (re-transcribe the repository and the
fixture from the committed 24-pattern library, which is C500 s3's deferred
fixture-pin defect finally biting) or register the F3 citation field as a choice
so that the sixth arm of `:find-sorry` can be priced over all three of its
halves rather than two. Both are things a lane may do without Joe.

Joe's three decisions -- `:find-sorry`, `:find-f2-receipt-carrier`,
`:find-f4-reading` -- remain `:observed-not-decided` with every arm priced, and
nothing here touches them.
