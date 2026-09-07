# C564 — `:F11` slice 6: the joint amended carrier priced, and what the F4 conjunct turns out not to buy

Slice 3 registered `:find-sorry` with five arms priced and a sixth,
`:amend-the-carrier`, left `:unmeasured`. Slice 4 registered the F2 half
(`:find-f2-receipt-carrier`), slice 5 the F4 half (`:find-f4-reading`). Neither
says what the two amendments do TOGETHER, and the reason the sixth arm still
read `:unmeasured` had narrowed to *nobody has aggregated them*. This slice
aggregates them, and the aggregation is a measurement rather than a paraphrase:
the joint arm buys less than the two halves suggest, for a reason neither half
could see.

**No ruling is taken and no contract moves.** `:find-sorry`'s sixth arm now
carries `:buys` and `:costs`; `:status` stays `:observed-not-decided` with no
`:ruling` key, and the registry compares EQUAL as a value to HEAD outside that
one arm, checked both ways. `Holes.lean` is untouched at `61c4825dc3` — `find`
at `:264` is still a `sorry`, and the build still reports it.

## 1. What the joint carrier is

`mathlib4/DarkTower/WarMachine/F11AmendedCarrier.lean` defines, without editing
`Holes.lean`:

| declaration | line | what it is |
|---|---|---|
| `AmendedReceipt` | `:18` | `Receipt` (`Holes.lean:240-242`) plus the three things `P-validated-R5.md:486` asks for, as DATA: `acknowledgedClause`, `retrievalRoute`, `asOf` |
| `AmendedFindResult` | `:24` | `selected`, receipts typed by `AmendedReceipt`, `zeroMass`, `absence` — the zero-mass field that today lives only on the ROW carrier `FindReceiptRow` (`Holes.lean:252-259`) |
| `ConformantAmendedFind` | `:35` | F1–F4 stated jointly, F4 now a CONJUNCT rather than a separate predicate over inputs |
| `AmendedFindResult.erase` | `:48` | back to today's `FindResult`, so the amended and the current carrier can be compared on the same finders |

## 2. The dispatch premise was refuted, and this is the slice's finding

`runs/F11-find/10-dispatch.edn`, committed before the work (futon2 `97007e73`,
job-id recorded in `d8d8e20c`), predicted:

> Adding the zero-mass designation to what `find` RETURNS internalises reading A
> and only reading A — A is the reading in which the excluded pattern comes from
> the finder's own output, and a field the finder fills is by construction the
> finder's own nomination.

**That is false, and it is false in the interesting direction.** The returned
designation is incomparable with reading A in BOTH directions:

- **Amended F4 ⇏ A**: `amendedF4DoesNotImplyReadingA` (`:167`). `amendedFaithful`
  is conformant at the amended type and its erasure refutes A, because A demands
  an exclusion on EVERY input including a one-pattern repository, which the
  recorded replay does not give.
- **A ⇏ amended F4**: `readingADoesNotImplyAmendedF4` (`:211`), witnessed by
  `amendedAllButBad` (`:191`), which selects a named member of the recorded
  18-pattern repository (`amendedAllButBadSelectionNonempty`, `:203`).

What the returned field actually internalises is neither A nor, by itself, B or
C. It implies **reading B** when pinned to the recorded designation
(`amendedF4ImpliesRecordedReadingB`, `:60`) and **reading C** when pinned to an
external one (`amendedF4ImpliesReadingC`, `:71`), with the recorded converses at
`:81` and `:93`.

## 3. THE PRICE: the F4 conjunct on its own constrains nothing

This is the review's addition (`amendedF4AloneBuysNoReadingOfF4`, `:440`), and
it is what makes the two implication theorems above readable.

`amendedIdentity` (`:358`) is a conformant amended finder — all four joint laws,
`amendedIdentityConformant` at `:369` — that selects the WHOLE recorded
repository and returns the EMPTY zero-mass designation. It refutes all three
readings at once:

| reading | refutation | line |
|---|---|---|
| A | it returns everything, so no member is excluded | `:406` |
| B | it selects `consultTheRemedyBeforeExiting`, which `find_snatch.clj:25-31` declares zero-mass for `g1Snatcher` | `:416` |
| C | the same witness at the recorded designation | `:425` |

Its selection on the recorded repository is nonempty by
`amendedIdentitySelectionNonempty` (`:398`), so this is not a refusal finder
evading the laws by doing nothing.

**Therefore `amendedF4ImpliesRecordedReadingB` and `amendedF4ImpliesReadingC`
are carried entirely by their pinning hypotheses `hz`.** What amending the
carrier buys for F4 is a PLACE to put a designation, not an obligation that the
designation be the recorded one — the finder still nominates, and may nominate
nothing. F4-as-a-conjunct is self-certifying in exactly the way F3 forbids
receipts to be.

## 4. What the joint carrier DOES buy: two separations today's carrier cannot make

Slice 4's benchmark is `findRErasuresAreEqual`
(`mathlib4/DarkTower/WarMachine/F11ReceiptCarrier.lean:186`): the faithful and
the misattributing finder are EQUAL after erasure to today's `FindResult`, so no
predicate whatever on that carrier separates them. Slice 6 as delivered proved
the two separations at the amended type but asserted the erasure equalities only
in docstrings; **the review added them**, because a separation without the
equality is a difference and not a buy.

| pair | separated at the amended type | equal after erasure |
|---|---|---|
| `amendedFaithful` / `amendedMisattributing` (differ in the carried clause) | `amendedReceiptDataSeparates`, `:259` | `amendedReceiptPairErasuresAreEqual`, `:340` (review) |
| `amendedFaithful` / `amendedDifferentZeroMass` (differ in the returned designation) | `amendedZeroMassSeparates`, `:286` | `amendedZeroMassPairErasuresAreEqual`, `:351` (review, by `rfl`) |

And the two amendments are **independent** in what they separate (M3): the
receipt-separated pair agrees on zero mass (`:298`), the zero-mass-separated
pair agrees on receipts (`:302`). So the arm is two amendments, not one.

Content-F2 also does real work: `amendedMisattributingNotConformant` (`:246`)
refutes the misattributor at a pattern the record selects
(`ask-for-surplus-not-surrender`, in `g1/snatcher`'s recorded
`:selected-union`), which today's presence-only `f2Receipted`
(`F11Conformance.lean:25-26`) accepts.

## 5. The joint mechanical cost, recomputed

`f11_amended_carrier_check.bb` recomputes the census over all Lean files under
`mathlib4/DarkTower` and compares it **site by site**, not by count:

- **1** plain `Receipt` construction: `F11Conformance.lean:67` (`findStructuredReceipt`).
- **8** plain `FindResult` constructions: `F11Conformance.lean:76, 121, 143`;
  `F11DischargeArm.lean:27, 76`; `F11ReceiptCarrier.lean:22, 144`;
  `F11AmendedCarrier.lean:51`.
- **9** total; **0** anywhere under `DarkTower` outside the F11 family, which is
  small only because `find` has no callers at all (slice 3 recomputed that:
  one code occurrence of the token across 153 files, the declaration itself).

Two things the dispatch expected here are corrected:

1. **The joint census is not slice 4's 7.** Slice 4's "7 receipt construction
   sites" counted its own pricing module's constructions of `ReceiptWithAssertions`
   and `RelationalReceipt` — its variant carriers — alongside the one real
   `Receipt`. Type-accurately there is exactly ONE plain `Receipt` site in the
   tree. The joint number is 9 and its composition is different, not larger by
   the same rule.
2. **"0 outside the pricing modules" depends on where the boundary is drawn.**
   Under slice 4's boundary (the pricing module is the one built to price the
   question) 7 of the 9 sites are outside THIS slice's module — `F11Conformance`
   and `F11DischargeArm` are ordinary Lean code that a real amendment breaks. The
   run record carries both readings; the "none elsewhere under `DarkTower`"
   reading is true and is the weaker claim.

The subadditivity the dispatch expected on the regeneration leg is unmeasured
and stays so: `Receipt` and `FindResult` are in the same file, so
`DarkTower/Contract/Emit.lean:52-62` derives one contract-authority change for
both, but no regeneration was run (TN §9a forbids it before review).

## 6. Every reading still admits a finder that finds nothing

`amendedRefusing` (`:314`) is conformant (`:320`) with a returned designation
that is the whole repository and is inhabited on the recorded repository
(`:329`). So the joint amendment shares the limit every other arm of
`:find-sorry` has: it does not buy a `find` that finds something. This is M5,
and it is why the arm is not priced as a fix for the thing it does not fix.

## 7. Gates

- `lake build DarkTower.WarMachine.F11AmendedCarrier`: exit 0, 2709 jobs, no
  warning from the module.
- `#print axioms` over all **43** declarations: 0 `sorryAx`, 10 axiom-free, the
  rest subsets of `propext`, `Classical.choice`, `Quot.sound`.
- 0 `sorry`, 0 `axiom`, 0 `native_decide` in the module.
- `f11_amended_carrier_check.bb`: PASS, 0 findings, artifact byte-identical over
  two runs (`fee95b36a9…`).
- `f11_amended_carrier_controls.sh`: **10** Lean plants, each located by
  DECLARATION NAME, each verified present on the planted line with the replaced
  text verified absent, all failing as required. One plant was DISCARDED during
  the review because it did not fail: replacing `erase`'s receipt map with a
  constant left both erasures equal, so the theorem still held — the plant tested
  nothing. It was replaced by `m2-erasure-is-the-record`, which breaks the tie
  between `amendedFaithful` and the recorded replay.
- `f11_amended_carrier_controls_checker.sh`: **6** checker plants, each moving
  EXACTLY ONE verdict.
- `negative_controls.sh` PASS; `pointer_check.bb` 0 unresolved.
- `Holes.lean` untouched at `61c4825dc3` and `:264` still a `sorry`.

## 8. What is left

`:find-sorry` now has all six arms priced. What is NOT settled, and is Joe's:

- which reading F4 is (`:find-f4-reading`, five arms, no ruling);
- what a `Receipt` carries (`:find-f2-receipt-carrier`, five arms, no ruling);
- what is done with the sorry (`:find-sorry`, six arms, no ruling).

The acceptance clause *discharge or amend the sorry at `Holes.lean:264`* cannot
be met by a lane, because every arm that removes it takes one of those three
decisions. What a lane could still do without a ruling is the F3 half — F3's
`nonSelfCertifying` reads two propositions and has never been measured against
the record's `nonSelfCertifying` column the way F2 has.
