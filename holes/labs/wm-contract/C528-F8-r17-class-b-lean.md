# C528 — :F8 leg 1, slice 10: the R17 class-(b) divergence and its repair path

**Row:** `:F8` (`:loop-mode :one-slice-per-invocation`), leg 1 (Lean
completion). Slices 1–9 stated the nine class-(a) quantities — Π (C518), ε
(C519), μ-next (C520), F_π (C521), o (C523), μ (C524), T (C525), τ (C526), u
(C527), with the `:code` pointer sweep at C522 and the U35 join refresh at C517.
**This slice is the other half of leg 1: the class-(b) defect** — "formalized but
runtime-divergent", the declared Dirichlet accumulation over the tick model's
`o` and `μ` against what A4a actually accumulates (EPIC-run-era.md:788-792).
**With it, leg 1 is complete.** The next slice is leg 2.

`:policy-set` is not a leg-1 item and is not taken here: the problem map types
it class (c) — genuinely empty on the runtime leg, which is F7's build
(EPIC-run-era.md:750-753). C525–C527 called it "class-(a)" loosely; it is the
tenth Box-5 row, not one of the nine.

**Two seats wrote this slice.** The Lean modules as first delivered are
codex-9's, under Agency job `invoke-1788627108487-7974-dddf4c60`; that packet
excluded the registry, the ledger and this report by instruction. Both
independent production probes, the review, the six repairs it produced, the
readback rewrite, the registry annotation, the ledger work and this report are
the reviewing seat's. Sent `--from wm-build-loop`, which is not on the roster;
the packet said so, asked for no bellback, and the job was polled to `done`.

## 1. The registry line, and what is wrong with it

`aif-equations.edn`, `:id :dirichlet-accumulation`, node R17, `:imports [:o :mu]`,
`:realised false`:

    :formal "a = a + sum_tau o_tau (x) s_tau"
    :eq     "dacosta2020 eq. 21 (A-learning)"

The row already said the edges R2→R17 and R1→R17 have no realisation found, and
`Holes.dirichletAccumulationImportAbsent` (`Holes.lean:7679`) already holds the
absence by-record. What this slice adds is that **the gap is not a missing wire
between two boxes that otherwise agree** — the two rules differ in the
increment, in the recurrence and in the coordinates, so re-sourcing the records
would not make them agree — and a **checkable obligation** for a repair.

## 2. The independent probes, run before the dispatch went out

Both probes and their transcripts were committed at `579bdeca` **before** the
packet was sent, so the packet's premises are derivable rather than asserted
(`runs/F8-dirichlet-accumulation/review-independent-probe{,-2}.clj` and `.txt`).
Every number in §3–§6 was produced twice: once by these probes, once by the
readback.

## 3. The increment: one record, one cell — and that is the one-hot special case

`increment-cell` does `(update-in matrix [capability outcome-index] + 1.0)`
(`a4a.clj:78-83`) over a matrix whose cells all start at the uniform `prior`
`0.1` (`a4a.clj:11-13`). Stated over **all** corpora:
`machineAppendRaisesItsOwnCell` (the record's cell rises by exactly
`a4aIncrement`) and `machineAppendMovesNoOtherCell` (nothing else moves), and
from the pair, `noSingleRecordProducesASoftUpdate`: **no record moves two
cells.**

Eq. 21 adds an outer product. For a Kronecker `o` and `s` that is exactly the
machine's unit increment (`oneHotDeclaredUpdateIsTheUnitIncrement`); for an `o`
carrying mass on two channels it raises two cells strictly
(`softDeclaredUpdateRaisesTwoCells`). **So A4a's rule is the one-hot special
case of the declared rule, and nothing else.** This is the substantive form of
the class-(b) defect and is sharper than "different sources": feeding A4a the
tick model's `o` and `μ` would still not realise eq. 21, because a
non-degenerate posterior spreads mass and a record cannot.

**Measured at HEAD:** prior `0.1`; one record `1.1`; the same record twice
`2.1`; the untouched cell of the same row `0.1`; the delta at the record's own
cell exactly `1.0`; and across the whole matrix exactly **one** cell moved.

## 4. The recurrence: there is no `a` on the left-hand side

`a = a + …` is a recurrence. `corpus->concentration` (`a4a.clj:85-113`) takes
the corpus and nothing else, and every cell starts at the prior — so it is a
**recount**, not an accumulation. That is a fact about a signature, and it is
stated as one in the module's header rather than dressed as a theorem. What is
provable is stronger and is proved: an accumulation of the **recount shape** —
one that rebuilds from this tick's data and discards whatever concentration it
was given — fails the declared contract **on the empty tick list**, before any
data is seen (`recountShapedDoesNotRealise`). Whatever such an implementation is
fed, and wherever the records come from, it cannot realise eq. 21.

**Measured:** the same corpus twice gives an identical result; and neither
accumulation entry point takes a previous concentration together with new
records — `corpus->concentration ([{:keys [capabilities edges discharges]}])`,
`reduce-concepts ([corpus-or-concentrations])`. `reduce-concepts` *will* accept
an already-built concentration map (`a4a.clj:115-119`), but that skips the
accumulation rather than extending it: reducing from the corpus and reducing
from its concentration give the same result.

## 5. The coordinates, and why they move

The declared rule's indices are the machine's own fixed carriers —
`Holes.Channel` and `MachineBeliefState.Status` — and they are genuinely two
index sets rather than one relabelled (`statusIndexDiffersFromChannelIndex`,
imported from slice 6 and used here rather than only cited). A4a's outcome
coordinate is a **position** in a list that `sorted-ids` re-sorts on every call
(`a4a.clj:74-76,102-104`), so the same coordinate names a different outcome once
the corpus changes.

**Measured:** outcomes `["m1" "m2"]` become `["aaa" "m1" "m2"]` when one record
with an earlier-sorting mission id is added, and `m1` moves from column 0 to
column 1. Any cross-run comparison of these vectors by index is therefore
unsound — which matters because `r17_offline/run` emits
`:concept-concentrations` in a replay envelope "a later proposal-construction
tick should consume" (`r17_offline.clj:74`).

## 6. The repair path — what this slice adds over the existing absence claim

`RealisesDeclaredAccumulation` is the obligation a repaired implementation must
discharge: on the machine's own `Channel`/`Status` coordinates, agree with
eq. 21 for every previous concentration and every tick list. It is non-vacuous
(`declaredAccumulationRealisesItself`) and not trivially satisfiable
(`recountShapedDoesNotRealise`).

`Holes.dirichletAccumulationImportAbsent` (`Holes.lean:7679`) stays **open and
untouched**. Lean cannot prove the absence of a code path — which is exactly why
that claim is by-record — and no theorem here asserts one. The predicate is what
makes its falsifier checkable.

**Not stated as a theorem, on purpose:** that R17's accumulation has no
production caller. It has none — the only reference into the a4a family from
outside is `actuator_a6.clj:127`, calling a different function
(`load-pattern-grain-model-uncertainty`), and nothing in `src/` requires
`actuator-a6` — but that is a claim about a corpus and belongs with the
by-record hole. The census is committed in the probe transcript.

## 7. The review was a real gate and found six things

All six fixed by this seat rather than re-dispatched (mathlib4 `4d89779d08`).
Codex-9's gate claims were verified first, not taken: both modules read in full
and `lake env lean` re-run on each at the delivered sha `6f5df367ec` — exit 0,
0 errors, 0 warnings, 0 `sorryAx` — before any repair.

**(a) The sharpest: the repair predicate was a hand-written triple of
Booleans.** As delivered, `machineRealisationEvidence : RealisationEvidence :=
⟨false, false, false⟩`, and `machineSatisfiesNoneOfRepairPredicate` proved
`false = false` three times and the negation that follows from it.
`machineDirichletAccumulation` does not occur in that statement at all; the
entire claim lived in three record-field *names*. This is the **sixth
consecutive slice** whose review found a claim asserted by a name and not by a
statement (slices 5–9). Replaced by a predicate over an accumulation *function*
plus the two theorems that give it teeth.

**(b) `machineRecountWithPreviousIgnored` was a function production does not
have** — invented so that discarding an argument could be proved to discard it.
Removed; §4 states the signature fact as a signature fact, and `recountShaped`
now appears only as a *counterexample to the predicate*, labelled as a shape.

**(c) `bmrThresholdReference` was `(-3 : ℝ) = -3 := rfl`,** and the readback
transcribed its expected value from it. It now states
`Holes.bayesFactorThreshold` (`Holes.lean:6957`), the carrier of the separate
`:model-reduction` row.

**(d) `nonOneHotOuterProductIsNotUnitCell` proved two numbers positive** and
never mentioned the machine, so the contrast its name promises was unstated.
Replaced by `softDeclaredUpdateRaisesTwoCells` beside
`noSingleRecordProducesASoftUpdate`, which is the contrast.

**(e) `oneHotOuterProductIsUnitCell` multiplied three numerals** and never
reached `machineDirichletAccumulation`. Replaced by a statement quantified over
`a`, `c₀`, `s₀` with both arms (the joint cell, and every other cell unmoved).

**(f) `statusIndexDiffersFromChannelIndex` was imported and named in the module
header but used in no statement,** so the one obligation the two carriers were
imported for went unstated. Now `declaredCoordinatesAreFixedAndMachineCoordinatesAreNot`.

**And four pointers were wrong at HEAD** while the delivered report said "all
packet premises checked out at HEAD": `increment-cell` is `78-83` (cited
`76-82`), `corpus->concentration` `85-113` (cited `83-113`), `sorted-ids`
`74-76` (cited `71-74`), the outcome index map `102-104` (cited `100-101`). Two
of the four were wrong **in the dispatch packet** and were copied through
unverified — which is the checkpoint the review exists to be.

**The readback was rewritten.** As delivered it measured nine values, two of
them against the tautologies in (c) and (b), and none against the theorems that
carry this slice's content. It now measures twelve, including both arms of the
one-record-one-cell claim, and separates three kinds of line: values transcribed
from a named Lean theorem; `PRODUCTION-ONLY` facts Lean cannot state (a Lean
function is deterministic by definition; the absence of an entry point is a fact
about a namespace); and `NOT MEASURED` for the declared-side theorems production
has no counterpart for. One `PRODUCTION-ONLY` line as first written listed the
two arity-2 publics under a label implying they were accumulation candidates —
corrected, since that is the same defect one level down.

## 8. The registry write, and the carrier that deliberately did not move

**`:lean-status` stays `:carrier-only` and `:realised` stays `false`.**
`machineDirichletAccumulation` is a carrier for *what the machine does instead*,
not for the `a-conc` the row declares — the module proves the two are different
objects, so naming it in `:lean` would assert exactly what it refutes. Slices
1–9 could declare a `machineX` carrier because the machine inhabits those
quantities; this row is the class-(b) defect and its quantity has no production
inhabitant. R17's published Lean-binding hole (`gen_rnode_dossiers.py:415-418`
turns `:carrier-only` into that hole) is therefore unchanged, and the equation
join stays at **17 of 18 carriers declared / 17 resolving**, `:no-carrier-declared
[:policy-set]`, `:carrier-declared-but-not-found []` — this slice does not move
those counts and does not claim to. What the corpus figures do show is the two
new modules: **91 modules** (89 before), **1101 declarations** (1077 before,
+24, exactly this slice's count), 14958 source lines (14650 before); **91/91
elaborate exit 0, 0 error diagnostics**; sorry 10 source / 10 Lean-reported and
axioms 0, unchanged — these modules add neither. Before and after are committed
at `runs/F8-dirichlet-accumulation/lean-state-join-{before,after}.edn`.

What did move: `:lean-at` and `:lean-note` are new, and `:lean`'s **parenthetical
pointer** was corrected from `Holes.lean:6380-6385` to `Holes.lean:6935-6936`.
The old pointer named a record literal in an unrelated table; `lean_state_probe.bb`
resolves `DirichletConcentrations` to `:6936` by itself, so the correction is
machine-derived rather than a preference. The carrier *name* in that field is
unchanged.

**The ledger consequence was met rather than routed around.** `:C8` (`:done`,
claude-20, `b815800`) signs this entry, so the write stales its signature — the
second time `:F8` has done that (slice 4 staled `:RUN9`) and the first time the
staled row covered keys **outside** the slice's reach: C8 also signs
`[:equations {:id :model-reduction}]` and `[:holes {:equation
:dirichlet-accumulation}]`, neither of which this slice read. `:C27` is minted,
`:supersedes :C8`, carrying C8's **full** key set forward so that superseding for
one entry does not switch the check off for the other two.

## 9. Gates, bare exits

- `lake env lean` on both modules — exit 0, **0 errors, 0 warnings** each, at
  the repaired sha (and at the delivered sha before repair, also 0/0).
- `lake build DarkTower.WarMachine.MachineDirichletAccumulationWitness` — exit
  0, 8502/8502.
- `#print axioms` on **all 24 declarations**, enumerated from the sources
  (codex-9 delivered 21) — 4 axiom-free, 1 `[propext]`, 19 `[propext,
  Classical.choice, Quot.sound]`. **0 `sorryAx`, 0 `sorry` tokens.** Every
  declared name printed and no printed name undeclared, checked by `comm` in
  both directions, empty each way.
- The registry edit verified key by key against `git show HEAD:` read as EDN:
  **one** row changed, **two** keys added, none removed, **one** value changed
  (the pointer), row order preserved, no top-level key other than `:equations`
  touched — `:choices` and `:references` byte-unchanged.
- The readback — exit 0, **12 MATCH, 0 MISMATCH**, run twice with identical
  `sha256` `6240a69ab0c3d6adc652e17919dce88c2b984c2e5869e4f1aef00167e31b9dd4`.
- `clj-kondo` — 0 errors, 0 warnings on the readback and on each probe
  separately. (Linting the three together reports 8 warnings, all of them
  duplicate-`require` and redefined-`user/…` artefacts of loading several
  scripts into one implicit namespace, not defects in any file.)
- `check-parens.el` — OK on the readback, both probes, `aif-equations.edn` and
  `worklist.edn`.
- `bb p4ng/empirics-futon/pointer_check.bb` and
  `bash p4ng/empirics-futon/negative_controls.sh` — see §11.

## 10. Found and not repaired

**(a) `a4a.clj`, `a4a_substrate.clj`, `bmr.clj` and `r17_offline.clj` are
untouched, on purpose.** The recount, the unstable coordinates and the missing
recurrence are *stated* here and left standing. Changing any of them is a change
to the machine, and this is a leg-1 "state it in Lean" slice; they are findings
for an I-track item.

**(b) The row's `:code` pointer to `read-corpus` has drifted, and is left
byte-unchanged.** It cites `a4a_substrate.clj:46-60`; at HEAD `read-corpus` is
`45-74`, so the cited span stops inside the edges expression and excludes the
discharge read at `:64-71` that the same sentence names. Stated in `:lean-note`,
not repaired — moving it would change a key this slice is only annotating.

**(c) The cross-run coordinate instability is stated, not traced to a
consumer.** §5 shows the columns move; whether any consumer actually compares
`:concept-concentrations` across corpus revisions was not established.

**(d) The published section was not regenerated.** TN §9a gates regeneration;
`gen_aif_dag.bb` was not run.

**Not claimed.** No live tick, no run lock, nothing written under `data/`. No
production source changed. No ruling; `:choices` is byte-unchanged. Legs 2
(name discipline) and 3 (convergence ledger) remain unstarted.
