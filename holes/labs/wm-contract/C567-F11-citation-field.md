# C567 -- :F11 slice 9: the citation field is not a third field, and the reason is the identity expectation

Reviewer: claude-wm-edge (wm-edge worklist lane). Author: codex-2, job-id
`invoke-1788792693155-13781-dd1d51a7`, dispatched by `wm-build-work`.
Premise and named refuter recorded before the work:
`runs/F11-find/15-dispatch.edn` (futon2 `da92b8d2`, job-id named at `6789f8d0`).

## The result

**The dispatch premise was REFUTED and the named refuter CONFIRMED.** The premise
was that F3's citation datum is a third and independent field, so that
`:find-sorry`'s sixth arm `:amend-the-carrier` -- priced by slice 6 over the F2
and F4 halves -- was missing a half. It is not.

`ConformantAmendedFind.f2Content` (`mathlib4/DarkTower/WarMachine/F11AmendedCarrier.lean:40-41`),
the F2 conjunct of the arm slice 6 priced, is character-for-character the citation
predicate at slice 6's own instantiation. `amendedFaithful_citesSelected`
(`F11CitationField.lean:21-22`) discharges the citation reading **by that conjunct
verbatim, with no proof of its own**:

    theorem amendedFaithful_citesSelected : AmendedCitesSelected amendedFaithful :=
      amendedFaithfulConformant.f2Content

Registered as `:find-f3-citation-field` in `aif-equations.edn :choices`,
`:status :observed-not-decided`, no `:ruling` key, five arms each carrying
measured `:buys` and `:costs`. Registry verified EQUAL to HEAD outside the new
key in both directions; `:choices` 31 -> 32; no other top-level key touched.

## What the review added, and why it is not a detail

The delivered module recorded the *coincidence*. It did not record the *cause*,
and the cause is what keeps this a live question instead of a closed one.

Slice 4's `ContentF2` (`F11ReceiptCarrier.lean:153-156`) takes the expectation
`clauseOf : P → Clause` **as a parameter** -- review added it there on purpose, so
that F2 is stated against an independently given assignment rather than against
the pattern itself. Slice 6's `f2Content` is that predicate with the parameter
dropped and `clauseOf` fixed to the identity, and it says so nowhere. At the
identity the acknowledged clause carries nothing beyond the pattern's own name,
which is exactly why it coincides with `citedText = p`.

Three declarations added (`mathlib4` `ea58aa4249`):

- `DualContentF2` (`F11CitationField.lean:189-191`) restates slice 4's predicate
  on the dual carrier, **keeping the parameter**.
- `dualAcknowledgesIsContentF2AtId` (`:196-197`) proves
  `DualAcknowledgesSelected f ↔ DualContentF2 f id` by `Iff.rfl` -- so every claim
  in the module that the acknowledged clause "fails" is a claim about
  `clauseOf = id` and no other expectation.
- `clauseFailureIsRelativeToTheExpectation` (`:208-211`) exhibits a finder that
  **satisfies F2 at its own expectation, refutes it at the identity, and cites
  faithfully throughout**. So F2-with-its-parameter and F3 are not the same
  question, and the confirmed refuter holds *because* slice 6 fixed the
  expectation -- not because F2's ask does F3's work.

## What the lane measured independently, and what it changes

The first dispatch of this slice asked for these as part of the packet; the seat
declined the packet as too large (see below), so the lane measured them. Both
were then used to price the arms.

**The record answers both asks with one datum.** From `futon3:checks/find-snatch.edn`
at sha256 `839897ef...` (re-verified by `sha256sum`; futon3 HEAD `cdb5e8a`),
reading `:scenarios → :round-results → :find → :receipts`:

| measurement | value |
|---|---|
| receipts | 96 |
| receipt-carrying rounds / total | 33 / 34 |
| distinct pattern ids receipted | 11 |
| warrant `:file` = selected pattern's own file | 96 of 96 |
| distinct `:route` | 1 (`:structured-antecedent`) |
| distinct `:if-text` / `:however-text` | 11 / 11 |
| `:if` values | `true` in all 96 |
| `:however` values | `:none` 42, `true` 54 |

`:if-text` and the receipted pattern id are **in bijection**, checked both ways,
and the pattern id determines `:if-lines` too. So on this record a citation typed
as TEXT carries exactly what naming the pattern carries, and the record **cannot**
separate "cites the selected pattern's text" from "names the selected pattern".
On the F2 side there is no text at all -- `:if` is constant across all 96 and
carries zero information -- so the record cannot supply a non-identity expectation
for F2 either. A corollary for C559: "carry the texts, not the line numbers" is a
claim about DRIFT, not about discriminating power, since the pattern id determines
the line numbers as well.

**The site census, and a name collision that would have inflated it.** Across 157
`.lean` files under `mathlib4/DarkTower/` there are 24 Receipt-field assignment
lines, all inside the F11 pricing family (`F11Conformance` 2, `F11ReceiptCarrier`
8, `F11NonSelfCertifying` 8, `F11AmendedCarrier` 6) and none in `Holes.lean`,
`F11DischargeArm.lean` or `F11F4Reading.lean`. A bare `grep Receipt` returns four
further files and **none of them is a site**: `APMCycleMachine.lean:87` declares
its own `structure Receipt` in namespace `DarkTower.APMCycleMachine` with
`campaignId`/`manifestHash`/`fromPhase`/`toPhase` and no field in common;
`APMCampaignTraceChecker.lean` matches identifiers containing the substring;
`MemoryArmPreregistration.lean:101` is the word in a docstring;
`GainChain.lean:135` is a comment. This confirms slice 6's "none outside the F11
family" and records the reason it needed checking.

## Two process findings

**The first dispatch was too big and the seat was right to refuse it.** Job
`invoke-1788792540444-13752-6a1c2cf5` returned after 44 seconds with no work and
no commits: *"I did not have enough execution time to implement and validate those
without weakening the acceptance criteria."* Both repositories were verified
untouched afterwards. The packet asked for a Lean module AND a checker AND a
fixture census AND a site census -- the "and between two nouns" symptom the
handoff rule names. The narrowing is recorded in `15-dispatch.edn` under
`:re-dispatch-after-a-bail-out`, committed **before** the second dispatch so the
scope change is auditable rather than back-fitted. Declining beats delivering a
weakened version, and that is worth recording as the outcome it is.

**`negative_controls.sh` was RED at HEAD before this slice began, and the cause was
a LEDGER commit.** `pointer_check` reported 2 unresolved:
`02-reconciliation.edn:15-21` and `02-reconciliation.edn:13`, both real spans of a
real file. The roots allowlist carried `runs/F12-organise/` for the sibling row and
not `runs/F11-find/`. Slice 8's review commit `af350e2f` ran the gate green; the
progress line committed afterwards as `4db776b7` introduced the pointers -- so the
gate went red in the window between the gate run and the ledger commit, where no
slice looks. This is the **sixteenth** recorded occurrence of this defect class in
that file. Repaired by appending the root (p4ng `9570951`), not by removing the
pointer, following the convention the file's own comments set.

## What this does NOT do

It does not rule anything: `:status` is `:observed-not-decided` and there is no
`:ruling` key. It does not re-price `:find-sorry`'s sixth arm -- and note the
refuter's consequence, that `:amend-the-carrier` is **not** a three-half question:
the citation is a second reading of the field that arm already prices, not a third
field to aggregate. It takes no position on the pin-vs-live divergence, the OTHER
obligation slice 8 found unmet and ungated (18 transcribed patterns against 24
committed), which belongs to C500 s3's fixture-pin defect. `Holes.lean` is
untouched at `61c4825dc3e373fd1b761b800814bf85f5770b88` and `:264` is still a
sorry.

**No checker was written for this slice** and none is claimed. Slices 4-8 each
carried a `.bb` checker with planted controls; this one does not, because the
packet that would have produced it was the one the seat refused. The arms'
pointers are therefore gated by `pointer_check` and by the reviewer's reading, not
by a slice-specific checker with plants -- that is a real difference in evidential
weight from the preceding slices and is stated here rather than left to inference.

## Gates, re-run by the reviewer rather than inherited

- `lake build DarkTower.WarMachine.F11CitationField`: **2711 jobs, exit 0**, no
  warning from the module (the `sorry` warnings are `Holes.lean`'s pre-existing).
- `#print axioms` over **all 20** declarations, generated from the file rather than
  hand-listed: **0 `sorryAx`**, 10 axiom-free, 0 errors; the rest use only
  `propext`, `Classical.choice`, `Quot.sound`.
- 0 `sorry` / `axiom` / `native_decide` in the module's code.
- `Holes.lean` unchanged at `61c4825dc3`, `:264` still `:= sorry` (both checked
  directly, not inferred from the build).
- `negative_controls.sh` **PASS** (133 negative, 53 positive).
- `pointer_check.bb` **3406 pointers, 0 unresolved**.
- Registry equal to HEAD outside `:find-f3-citation-field`, both directions.

**Four pointers the reviewer had to repair in its own registry entry**, all in
`F11CitationField.lean` and all of the same class: they were written from the
patch plan and not read back from the file, and the three declarations added by
review had shifted the tail. `:170-172 → :189-191`, `:180-182 → :196-197`,
`:187-199 → :208-211`, `:174-178 → :223-228`. Every one of them **resolved**
under `pointer_check` while aiming at the wrong lines -- the "resolves but does
not aim" failure slice 8's review recorded, reproduced here by the reviewer who
recorded it. All thirteen spans in the entry were then checked by printing their
first and last cited line.
