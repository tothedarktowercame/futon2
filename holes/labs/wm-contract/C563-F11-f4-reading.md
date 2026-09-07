# C563 — `:F11` slice 5: three readings of F4, and the premise this slice was dispatched on turning out to be false

Slice 3 registered `:find-sorry` with five arms priced and a sixth,
`:amend-the-carrier`, left `:unmeasured` because the two questions inside it —
what a `Receipt` holds, and which of two readings F4 is — were not registered as
choices at all. Slice 4 took the first. This slice takes the second, and it is
the last half: both are now registered.

**No ruling is taken and no contract moves.** The new entry
(`aif-equations.edn` `:choices` `:find-f4-reading`) carries `:status
:observed-not-decided` and no `:ruling` key; the registry compares EQUAL as a
value to HEAD outside that one key and `:find-sorry`'s `:amend-the-carrier`
arm, checked both ways. `Holes.lean` is untouched — `find` at `:264` is still a
`sorry`, and the build still reports it.

## 1. The sentence, and the three things it has been taken to mean

`futon2:holes/problems/P-validated-R5.md:488` says: *for a given tension there is
at least one pattern in the repository the finder must not return (a zero-mass
pattern — T1512Z applied to retrieval); a finder that can return anything for
anything is unfalsifiable.*

| reading | declaration | who picks the excluded pattern | quantifies over |
|---|---|---|---|
| A | `FindFalsifiable`, `mathlib4/DarkTower/WarMachine/F11Conformance.lean:31-32` | the finder, per input, after the fact | every repository |
| B | `FindExcludesRecordedZeroMass`, `mathlib4/DarkTower/WarMachine/F11DischargeArm.lean:59-62` | the record, in advance | the recorded repository alone |
| C | `FindRespectsZeroMass`, `mathlib4/DarkTower/WarMachine/F11F4Reading.lean:26-29` | an external designation `zm`, in advance | every repository |

A and B were already in the tree and slice 2 and slice 3 had shown them
disagreeing in both directions. C is this slice's, and it is what the sentence
literally says: *must not* is deontic, so the designation is not the finder's to
choose. Reading A is the existential collapse of that — it keeps the shape and
gives the choice back to the finder.

## 2. The lattice, proved rather than asserted

- **C ⟹ B** (`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:34-42`), because B
  tests the recorded repository only.
- **B ⇏ C** (`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:68-89`). The witness
  `findReplayRecordedElseIdentity` replays on the recorded pattern set and returns
  everything elsewhere; on the repository whose only member is g1's declared
  zero-mass pattern it returns exactly that pattern. It selects there
  (`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:92-104`), so this is not a
  refusal dressed as a disagreement.
- **A ⇏ C at the recorded designation**
  (`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:136-146`): `findAllBut` at
  g1's declared member satisfies A and returns g4's declared member. It selects
  on the recorded repository (`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:153-159`).
- **C ⇏ A** (`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:129-133`).

So **C is strictly stronger than B, and A is incomparable with C in both
directions.** Three readings, no two equivalent, and none derivable from another.

## 3. The premise this slice was dispatched on is false

The dispatch record (`runs/F11-find/08-dispatch.edn`, committed as `02b8126a`
*before* the work was done) expected A and B to pull in opposite directions:
A demands an exclusion from every repository including singletons; the recorded
designation takes three distinct values over six scenarios
(`mathlib4/DarkTower/WarMachine/Holes.lean:342-348`), so no single fixed
exclusion satisfies B across them. The expectation was that requiring both would
cost the record's own selections, and the packet said in as many words that
exhibiting a finder which satisfies both *and* reproduces every recorded
selection would refute it and be the finding.

That is what happened. `findReplayRecordedElseRefuse`
(`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:220-225`) is F1–F3 conformant,
satisfies A and B, and reproduces the record
(`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:237-286`); its selection on the
recorded repository is nonempty (`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:227-231`).

**What it actually costs is paid off the record, by refusing.** On any repository
whose pattern set is not the recorded one the finder returns nothing, which is how
it meets A. And the witness is built from a test against the record — it branches
on `repo.patterns = findSnatchRepository.patterns` — so what is established is
that A and B are jointly satisfiable *while reproducing the record*, not that a
finder written without the record in hand can do it. The entry's arm says both.

Writing the premise down before dispatch is what makes this reportable as a
refutation rather than as a result nobody had predicted.

## 4. Two things review had to add

**(a) The C-not-A separation was proved where C says nothing.** As dispatched,
`findReadingCDoesNotImplyReadingA`
(`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:115-118`) took the **empty**
designation, at which C is vacuous, and paired it with `findIdentity`. That is the
vacuous-witness form the packet's floor was written against — in its *designation*
guise rather than its finder guise, which is why the finder-side floor the packet
stated did not catch it. The strong form was already available in the delivered
module and unstated: replay satisfies C at the **recorded** designation on every
repository (`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:44-49`) and refutes A
(`mathlib4/DarkTower/WarMachine/F11Conformance.lean:207-218`), and it selects on
the recorded repository (`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:107-112`). Added as
`findRecordedReadingCDoesNotImplyReadingA`
(`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:129-133`). The vacuous form is
kept, because the two say different things and the entry cites both.

This is the seventh consecutive slice across `:F11`/`:F12` in which review has had
to supply this floor (C561 §7b counted six). The packet stated it as a property
the witnesses must *carry* rather than as an instruction, and every **finder** in
the delivered module does carry its nonempty-selection theorem. The gap moved to
the **parameter**: no floor asked whether a designation was inhabited. That the
requirement was met exactly where it was stated and evaded exactly where it was
not is the finding worth carrying to the next packet.

**(b) The reproduction claim was about another finder, not about the record.**
`bothReadingFinderReproducesEveryRecordedSelection` states that the hybrid selects
what `findSnatchReplay` selects. Replay intersects with `repo.patterns`
(`mathlib4/DarkTower/WarMachine/F11Conformance.lean:72-78`), so that equality
would still hold if the record had selected a pattern outside its own repository
and replay had silently dropped it. It did not:
`findSnatchRecordedSelectionsAreInRepository`
(`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:289-291`) decides F1 containment
at the record grain over the rows of `Holes.lean:626`, and
`bothReadingFinderSelectionIsTheRecordedSelection`
(`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:307-312`) states the hybrid's
selection on the recorded repository as the recorded scenario-grain selection
itself.

Both repaired here rather than re-dispatched (`mathlib4` `7060ad361e`).

**Every pointer in the dispatched module resolved.** Thirteen mathlib citations
and four futon3 citations were checked by hand against what they name; all
seventeen land on the declaration or form they claim.

## 5. What the record can and cannot decide

`futon3:checks/find_snatch.clj:25-31` declares exactly **one** zero-mass pattern
for each of **six** scenarios, **three** distinct values across the six. The F4
leg tests only that member: presence in the repository and absence from the
selection (`futon3:checks/find_snatch.clj:172-177`), with the run refused at
`futon3:checks/find_snatch.clj:106` when a declared member is selected.

An exclusion recorded for any **other** reason is **not found** — in
`futon3:checks/find_snatch.clj:100-114`, in `futon3:checks/find_snatch.clj:172-177`, or in the scenario F4
fields of `futon3:checks/find-snatch.edn`. So no recorded row distinguishes
reading A from reading B: every recorded exclusion is the declared one, and the
record cannot arbitrate between the readings it is quoted in support of.

## 6. Every reading admits a finder that finds nothing

`findRefusing` is F1–F3 conformant and satisfies A
(`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:323-328`), B (`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:330-336`) and C
at **every** designation (`mathlib4/DarkTower/WarMachine/F11F4Reading.lean:338-343`). So no reading of F4 is what stops a
discharged `find` from finding nothing — that cost, which C561 §2 recorded as
where `find` and `organise` come apart, is untouched by which reading is chosen.

## 7. The mechanical cost, split

Across the 155 `.lean` files under `mathlib4/DarkTower`:

| reading | total | in the pricing module | outside it |
|---|---:|---:|---:|
| A `FindFalsifiable` | 21 | 11 | 10 |
| B `FindExcludesRecordedZeroMass` | 18 | 10 | 8 |
| C `FindRespectsZeroMass` | 10 | 10 | 0 |

The split is the number that matters: a reading used only by the module built to
price it costs nothing to change. Recomputed and **compared** by
`f11_f4_reading_check.bb`, which fails with `:mechanical-cost-changed` rather than
merely reporting. Neither reading is a conjunct of `ConformantFind`
(`mathlib4/DarkTower/WarMachine/F11Conformance.lean:21-28`) — F4 stays a separate
predicate because `FindResult` (`mathlib4/DarkTower/WarMachine/Holes.lean:247-250`)
carries no zero-mass designation to state it from.

## 8. Controls

**Eight against the Lean** (`f11_f4_reading_controls.sh`) — five dispatched, three
added by review for the added declarations. Each locates its target by declaration
name, verifies the planted text present and the replaced text absent **on the
planted line** before building, and re-elaborates:

| plant | exact failure |
|---|---|
| M1 empty designation made universal | unsolved universal identity-exclusion goal |
| M2 replay branch replaced by identity | nonempty replay-selection type mismatch |
| M3 recorded exclusion reversed to inclusion | recorded witness conclusion mismatch |
| M4 refusing C-witness replaced by identity | unsolved zero-mass exclusion goal |
| M5 C's universal quantifier made existential | C hypothesis no longer callable as a function |
| R1 recorded designation dropped back to empty | application type mismatch |
| R2a record-grain containment negated | `decide` proved the proposition false |
| R2b reproduction aimed at a non-recorded repository | type mismatch |

**Five against the checker** (`f11_f4_reading_controls_checker.sh`), each on a
planted copy of one input, each moving **exactly one** verdict:

| plant | verdict |
|---|---|
| reading A's body emptied | `:reading-a-is-no-longer-the-forall-over-inputs-existential` |
| reading B given the repository quantifier | `:reading-b-is-no-longer-record-grain` |
| a scenario declares two zero-mass patterns | `:zero-mass-designation-no-longer-one-pattern-per-scenario` |
| the record's F4 omission test dropped | `:record-f4-omission-test-changed` |
| the sorry at `Holes.lean:264` discharged | `:find-not-declared-as-a-sorry` |

## 9. Gates

- `lake build DarkTower.WarMachine.F11F4Reading`: **2707** jobs, exit 0, no
  warning from the new module. Re-run in review after the additions.
- `#print axioms` generated over all **28** declarations: **0** `sorryAx`, **3**
  depending on no axiom. Run independently in review; the dispatched report said
  2 axiom-free, which was the count before the review additions.
- `sorry` / `axiom` / `native_decide` in the module's code: 0 / 0 / 0.
- `f11_f4_reading_check.bb` PASS, 0 findings, artifact byte-identical over two
  runs (`fd7616a3235253773a9264d33fff779b77bed9200a190a1285b69aec6ffedab7`).
- `negative_controls.sh` PASS (133 negative, 53 positive) and `pointer_check.bb`
  3317 pointers 0 unresolved, both on the working tree before the ledger commit.
- No `gen_aif_dag.bb` (TN §9a). No machine run, so no run-lock.
- `Holes.lean` unchanged: `git log -1` still `61c4825dc3`, and `:264` still
  reports `declaration uses sorry` in the build.
- `lake build DarkTower` whole-tree is NOT this row's gate and stays red at HEAD
  outside this row.

**One gate repair outside the row.** `negative_controls.sh` was **red at HEAD
before this slice began**, for a pointer this lane did not write: slice 4's entry
bounds its construction-site census by naming what it excludes,
`mathlib4:DarkTower/APMCycleMachine.lean:87`, and `pointer_check.bb` resolves a
bare filename against a hand-maintained allowlist of directories that carried
`DarkTower/WarMachine/` and `DarkTower/Contract/` and not `DarkTower/`. This is the
fifteenth recorded occurrence of that defect class in that file. Repaired by
appending the root (`p4ng` `d26d40a`), not by removing the pointer: the census
boundary is exactly the part a reviewer has to be able to check.

## 10. What this does not settle

The entry chooses nothing. Which reading F4 is remains Joe's, and the three are
now stated, related to each other by proof, and priced. `:find-sorry`'s sixth arm
`:amend-the-carrier` still reads `:unmeasured`, and **the reason has changed**: it
is no longer that half of it is unregistered — both halves now are — but that
nobody has priced the arm *as one thing*, which is what the two entries supply the
ingredients for and neither states. That aggregation is the next slice.
