# C561 — `:F11` slice 3: what discharging the `find` sorry costs, and where `find` and `organise` come apart

The `:F11` acceptance has one clause no slice had acted on: *discharge or amend
the sorry at `Holes.lean:264`*. Slice 1 reconciled the F2 falsifier; slice 2
stated F1–F4 of the `find` FUNCTION and ended by noting the sorry was still
neither discharged nor amended, and that two questions — F2's receipt carrier
and which of two readings F4 is — were Joe's. This slice takes the sorry clause,
and it needed no ruling to do so: the arms are Lean elaborations against fixtures
this row already built, which is the case `worklist_check.bb:17-45` sends to the
branches rather than to Joe. The sibling row ran the same arm for `organise` at
C552, and the two rows now differ in a way worth stating.

**No ruling is taken and no contract moves.** The new registry entry
(`aif-equations.edn` `:choices` `:find-sorry`) carries `:status
:observed-not-decided` and no `:ruling` key; the registry compares EQUAL as a
value to HEAD outside that one key, checked both ways. `mathlib4` `git log -1`
still reads `61c4825dc3` on `Holes.lean`. The sorry itself is untouched — the
build still reports it, which is how the check knows.

## 1. The sorry is inert, and that was recomputed

Over the **153** Lean files under `mathlib4/DarkTower` there is exactly **one**
CODE occurrence of the token `find`: the declaration itself at
`mathlib4/DarkTower/WarMachine/Holes.lean:264`. The other **25** are prose in
doc-comments and module headers, plus **one** string literal in the hole registry
(`mathlib4/DarkTower/WarMachine/Holes.lean:8020`, `mkRefused "find"`). Recorded
in `runs/F11-find/05-discharge-arm.edn`, recomputed by `f11_discharge_check.bb`
rather than copied from C552's organise census, and the checker FAILS with
`:find-is-no-longer-inert` if a term reference ever appears.

So the question is not whether a term exists, and not whether discharging would
make some proof easier. What separates the arms is what the declaration SAYS
afterwards.

## 2. Where `find` and `organise` come apart

C552's first arm for `organise` was that discharging with the cheapest inhabitant
produces a declaration the O-laws REFUTE
(`mathlib4/DarkTower/WarMachine/F12DischargeArm.lean:32`) — worse than the sorry,
not equal to it. **That is not true of `find`.** Its cheapest inhabitant,
`findRefusing`, satisfies F1–F3 and F4 under reading A
(`mathlib4/DarkTower/WarMachine/F11DischargeArm.lean:20`). A finder that returns
nothing, with the typed absence F1 requires, is a lawful finder.

The laws do still bite, and the measurement had to be repaired to show it on the
record: `findSilent` — the same empty output with F1's typed absence omitted — is
refuted at `mathlib4/DarkTower/WarMachine/F11DischargeArm.lean:35`. As dispatched
that refutation was witnessed on a repository with **no patterns**, where it is a
fact about a degenerate input; it is now witnessed on the recorded 18-pattern
`findSnatchRepository`, non-vacuous by
`mathlib4/DarkTower/WarMachine/F11DischargeArm.lean:45`.

So what the trivial-discharge arm costs for `find` is not falsity. It is that the
discharged `find` **finds nothing**: every F2 and F3 obligation is met vacuously
and no recorded selection is reproduced.

## 3. The refusal is now a proved fact, under both readings of F4

The `find` docstring (`mathlib4/DarkTower/WarMachine/Holes.lean:263`) refuses to
implement on the ground that the recorded F1–F4 instances *do not select one
canonical implementation*. That sentence is now a theorem, under BOTH readings of
F4, so that proving it does not tacitly decide which reading F4 is:

- **Reading A**, forall-over-inputs (`F11Conformance.lean:31`) —
  `mathlib4/DarkTower/WarMachine/F11DischargeArm.lean:120`;
- **Reading B**, record-grain, the pattern declared zero-mass in advance is in
  the repository and out of the selection (`F11Conformance.lean:176`) —
  `mathlib4/DarkTower/WarMachine/F11DischargeArm.lean:149`.

Existence is likewise stated three times, F1–F3 alone (`:49`), with reading A
(`:54`), with reading B (`:65`).

## 4. The disagreement is located under reading A and demonstrably is not under reading B

**This is what review had to add, and it is the slice's substantive finding.**

As dispatched, both non-uniqueness results paired a real finder against
`findRefusing`. But `findRefusing` excludes *every* pattern, so "both witnesses
exclude the declared zero-mass member" is not a shared commitment — one side of
that agreement is vacuous, and "they differ" reduces to the observation that one
of them refuses. The floor the sibling row's reviews kept having to add (C552 §6b
counted five consecutive slices) was missing again here in the same shape.

Under **reading A** the located form is cheap and is now proved:
`findAllButPairDisagreesExactlyOnTheTwoZeroMassMembers`
(`mathlib4/DarkTower/WarMachine/F11DischargeArm.lean:225`) pairs two `findAllBut`
instances at the two patterns the record declares zero-mass —
`consultTheRemedyBeforeExiting` at `g1Snatcher` and `forcedPlayNeedsALossFloor`
at `g4Snatcher` (`futon3:checks/find_snatch.clj:25-31`, transcribed at
`mathlib4/DarkTower/WarMachine/Holes.lean:342`). Both select the recorded member
`askForSurplusNotSurrender`; each selects exactly the pattern the other drops. So
they agree on sixteen of the eighteen recorded patterns and the disagreement is
exactly two.

Under **reading B** no such pair exists, and rather than being a gap in the
search that is the measurement:
`mathlib4/DarkTower/WarMachine/F11DischargeArm.lean:207` proves `findAllBut` at
the `g1` zero-mass member **satisfies reading A and refutes reading B** — it
still returns `forcedPlayNeedsALossFloor`, which the record declared zero-mass at
`g4Snatcher` before the fact. A finder can exclude one pattern from every
repository, exactly as reading A asks, and still return a pattern the record said
in advance it must not.

**So the two readings are not two phrasings of one law.** Slice 2 had already
shown they disagree in one direction (the replay of the record satisfies reading
B and refutes reading A, `F11Conformance.lean:207`). This slice shows the other
direction, on a different finder. Reading B is strictly the more demanding of the
two on this record, and the price of that is that its only two conformant
discharges here are the finder that replays the record and the finder that
refuses (`mathlib4/DarkTower/WarMachine/F11DischargeArm.lean:169`, whose
docstring now says that the refuser's half of the agreement is vacuous).

## 5. The opacity arm, and the axiom footprint

Reproduced from C552 rather than re-argued, because C552 had already refuted the
claim that `opaque` requires a body:

| declaration | line | axioms |
|---|---:|---|
| `findOpaqueRefusing` (body `findRefusing`) | 242 | none |
| `findOpaqueNoBody` (no body) | 249 | `Classical.choice` |

Either route removes the sorry and neither leaves an F-law provable of the
declaration. The negative control also re-establishes that the `Nonempty`
instance declared LATER in the file does not rescue an earlier bodiless `opaque`.

## 6. The sixth arm is unpriced, and the entry says so

C552's sixth arm pointed at `:organise-carrier`, an entry that exists. F11 has no
such entry: **the F2 receipt carrier and the F4 reading are not registered as
choices at all**, which is why the registry said nothing about them when slice 2
ended. `:find-sorry`'s sixth arm therefore reads `:buys :unmeasured` and `:costs
:unmeasured` and names both questions with their carriers —
`Receipt` at `mathlib4/DarkTower/WarMachine/Holes.lean:240-242` (two `Prop`s
where `futon2:holes/problems/P-validated-R5.md:486` asks for the acknowledged
clause, the retrieval route and an as-of), and the absent `zeroMass` field on
`FindResult` at `mathlib4/DarkTower/WarMachine/Holes.lean:247-250` (it is on the
row carrier at `:258`). Registering those two is the next slice's work. The entry
does not claim its arms are exhaustive.

## 7. Four review findings

**(a) The refutation was on an empty repository.** Repaired at
`F11DischargeArm.lean:35`; see §2.

**(b) The R2 floor was vacuous on one side.** Repaired by adding
`F11DischargeArm.lean:225` and `:207`; see §4. This is the sixth slice across
`:F11`/`:F12` in which review has had to supply this floor, which C552 §6b
already recorded as a hole in what the packets ask for. This slice's packet DID
ask for it in as many words, and the answer supplied a form that was true and
carried nothing — so the packet's asking is not sufficient either.

**(c) Two of the four dispatched negative controls did not establish their
plant.** `opaque_body` and `no_instance` each grepped for a string the UNPLANTED
file also contains, so both "verifications" would have passed on an unmodified
copy. Both now assert the REMOVAL of the text they replace, and two controls were
added for the review additions: `f11_discharge_controls.sh` now runs six. The
narrowing caught one of my own over-broad assertions on the first run.

**(d) My own checker had the same defect in a different place.** Written to
recognise the `find` declaration by a hard-coded committed path, it counted the
declaration itself as a term reference whenever it was pointed at a planted tree
— so three of the five checker plants reported `:find-is-no-longer-inert` as well
as the thing they planted. Repaired to derive the path from `holes-path`; each
plant now moves exactly the verdict it plants.

All four repaired here rather than re-dispatched (mathlib4 `f99ab6bd44`, and this
commit).

**No packet pointer was wrong this time** — the seat reported checking each, and
I verified the module's twenty-odd citations independently. But thirteen were
wrong in the file this row committed LAST slice, repaired separately (mathlib4
`34858bce32`): eleven of them one mistake copied, every citation of the F1–F4
laws pointing into the interface code block at
`futon2:holes/problems/P-validated-R5.md:471-479` instead of the laws at
`:484-488`.

## 8. Controls

Six against the Lean (`f11_discharge_controls.sh`), each plant verified present
AND the replaced text verified absent before building, each an independent
re-elaboration:

| plant | exact failure |
|---|---|
| `findSilent` given F1's typed absence | `this : True ⊢ False` at the refutation |
| `findAllBut` returns the whole repository | reading-A falsifiability unsolved; `hq : q ∈ repo.patterns ⊢ False` |
| `findOpaqueRefusing`'s body removed | `failed to synthesize 'Inhabited' or 'Nonempty' instance for FindType Unit SnatchPattern` — the instance declared LATER does not rescue it |
| the local `Nonempty` instance removed | the same synthesis failure, now at `findOpaqueNoBody` |
| `findAllButFailsReadingB` aimed at `g1Snatcher` | unsolved `⊢ False` — at `g1` the declared zero-mass member IS the one `findAllBut` excludes |
| the located pair given one pattern twice | unsolved `⊢ False` — a finder cannot both select and exclude it |

Five against the checker (`f11_discharge_controls_checker.sh`), each on a copy of
the whole `DarkTower` tree, each moving the verdict and now moving only its own:

| plant | verdict |
|---|---|
| the sorry at `Holes.lean:264` discharged | `:find-not-declared-as-a-sorry` (and `:find-is-no-longer-inert`, correctly — the declaration is no longer the recognised one) |
| a term reference to `find` added | `:find-is-no-longer-inert` |
| `mkRefused "find"` removed | `:hole-registry-no-longer-carries-find` |
| the bodiless `opaque` removed | `:expected-two-opaque-declarations`, `:expected-exactly-one-bodiless-opaque` |
| `findAllBut` renamed, keeping `findAllButConformant` | `:required-declarations-missing` — the prefix trap does not fire |

The last of these is the one that keeps the refuted claim refuted: with only the
body-carrying declaration left, "opaque requires a body" would read as true again.

## 9. Gates

- `lake build DarkTower.WarMachine.F11DischargeArm`: 2706 jobs, exit 0, no
  warning from the new module. Re-run in review after the repairs.
- `#print axioms` generated over all **22** declarations: **0** `sorryAx`, **5**
  depending on no axiom, and the opacity contrast in §5 intact.
- `sorry` / `axiom` / `native_decide` in code: 0 / 0 / 0.
- `f11_discharge_check.bb` PASS, artifact byte-identical over two runs
  (`0838c76e9252fde331ab2596f6188e6754dd0e73334fba875683bfe7196418b3`).
- `negative_controls.sh` and `pointer_check.bb` run on the working tree before
  the ledger commit.
- No `gen_aif_dag.bb` (TN §9a). No machine run, so no run-lock.
- `lake build DarkTower` is NOT this row's gate and stays red at HEAD outside
  this row.

## 10. What this does not settle

The entry chooses nothing. The sorry at
`mathlib4/DarkTower/WarMachine/Holes.lean:264` is still neither discharged nor
amended, and what this slice adds is that leaving it is now a choice with a
measured price rather than a default. What remains open in `:F11` is the F2
receipt carrier and the F4 reading — both Joe's, both still UNREGISTERED, and
registering them is what a slice can still do without a ruling.
