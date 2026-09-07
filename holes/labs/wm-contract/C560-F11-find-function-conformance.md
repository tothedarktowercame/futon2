# C560 — `:F11` slice 2: the F-laws stated of the `find` FUNCTION, and where the carriers run out

Date: 2026-09-07 · worklist row `:F11`, slice 2 · mathlib4 commits
`63dc9d37e0` (the conformance file, dispatched to codex-1), `5937caf3ba` and
`5e43588a0e` (this review's fixes and additions) · futon2 this commit ·
artifact `runs/F11-find/04-conformance.edn` · checker `f11_conformance_check.bb`
with `f11_conformance_controls.sh`.

## 1. What this slice is, and what it is not

`:F11`'s acceptance asks, after the F2 reconciliation slice 1 recorded (C559),
for "a conformant implementation at the s3e interface, laws stated in Lean, each
witnessed on a real find over the committed library".

What `Holes.lean` had before this slice was F1–F4 stated of a **recorded row**
(`findF1Containment` … `findF4Falsifiable`, `Holes.lean:266-283`), narrowed
there by the 2026-08-31 scope amendment precisely because `find` at
`Holes.lean:264` is a deliberate refusal and no serialized evidence could
quantify over an opaque function. This slice states the laws of a **function
variable** at the s3e signature, exactly the move C544 made for `organise`, and
proves them of implementations that replay the committed record.

**No ruling is taken.** `Holes.lean` is untouched — its `sorry` at `:264` is
still a `sorry`, verified by the build's own warning list — nothing was written
to `aif-equations.edn :choices` or `control-map-edges.edn :decisions`, and
`gen_aif_dag.bb` was not run (TN §9a).

## 2. What the function framing carries, and where it stops

`FindType` (`F11Conformance.lean:18`) is the s3e signature, and the checker reads
BOTH it and `find`'s own type out of their files and compares them, so a drift
that would make every theorem below a theorem about a different interface cannot
pass silently (`:find-type` in the artifact: both read
`Tension State → Repository P → FindResult P`).

| law | at the function | why |
|---|---|---|
| F1 | **yes**, both halves | `repo.patterns` is the argument and `FindResult.absence` is a field, so containment and the typed absence are both statable |
| F2 | **presence only** | `FindResult.receipts` is a field, so "every selected pattern carries a receipt" states; the rest of F2 does not — see below |
| F3 | **yes** | `Receipt.nonSelfCertifying` (`Holes.lean:246`) is defined on the carrier |
| F4 | **not as a conjunct** | `FindResult` is `{absence, receipts, selected}` (artifact `:find-result-fields`) with no `zeroMass`; F4 is a property of the FINDER over its inputs, not of one returned value |

So `ConformantFind` (`:21`) holds F1–F3 and F4 is separate, as `FindFalsifiable`
(`:31`). This is the find-side counterpart of C544 §2's O1/O4 rows, where
`admittedBy` and the score fields had nowhere to live: the same shape of answer,
reached independently.

**The F2 row is the sharper finding, and it was codex-1's own report rather than
a review catch.** §3e asks a receipt to carry three things — the tension clause
it acknowledges (IF/HOWEVER overlap), the retrieval route (edge-walk / recall /
sample), and an as-of. `Receipt` (`Holes.lean:242-244`) has two Props,
`citesTextOrEdges` and `scoreAlone` (artifact `:receipt-fields`). The clause, the
route and the as-of have no field. They are all present in the record —
`futon3:checks/find-snatch.edn` carries `:if-text`, `:however-text`, `:route`
and `:as-of`, and C559 §3 measured 96 receipts across those very fields — so
what is missing is not the evidence but the Lean carrier for it.
`ConformantFind.f2Receipted` therefore states receipt *presence* and nothing
more, which is stated in the file rather than papered over. The checker pins
`Receipt`'s field list so this row cannot quietly become false.

## 3. Conformance does not determine the finder

Three implementations at the exact signature, all proved `ConformantFind`:

- `findSnatchReplay` (`:72`, proved `:81`) — the recorded find: the scenario's
  `:selected-union` from `findSnatchScenarios`, intersected with the repository
  argument, with receipts issued to the recorded receipted members;
- `findRefusing` (`:120`, proved `:125`) — `∅` with the typed absence;
- `findIdentity` (`:140`, proved `:147`) — §3e's own "hand-authored (Snatch):
  `find` trivial — identity on the twelve".

`findConformantImplementationsDifferOnSnatch` (`:231`) proves the first two
differ on the recorded `g1Snatcher` input. So F1–F3 conformance does not pick out
a finder, and whatever eventually discharges `Holes.lean:264` will be a **choice**
among conformant implementations — which is what the declaration's own refusal
("does not select one canonical implementation") says, now with witnesses.

Non-vacuity: `findSnatchReplaySelectsNamedPattern` (`:221`) puts the recorded
`askForSurplusNotSurrender` in the replay's selection at `g1Snatcher`, so the
F1–F3 proofs are not proofs about the empty set.

## 4. What F4 separates, and the two readings it leaves open

`findIdentityNotFalsifiable` (`:162`): identity search satisfies F1–F3 and
refutes F4 on the recorded repository. §3e's sentence — "a finder that can return
anything for anything is unfalsifiable" — is therefore not a remark about
hypothetical finders; it excludes an implementation the s3e table itself lists.

`findSnatchReplayExcludesDeclaredZeroMass` (`:176`) is the one that carries F4's
content: for all six recorded scenarios, the pattern `find_snatch.clj:25-31`
declared zero-mass **in advance** is in the repository and out of the selection.
The distinction matters and is this review's second fix: the dispatched version
proved only that *some* repository member was excluded, which every finder that
is not the identity satisfies. Advance declaration is what makes F4 a falsifier
rather than an observation.

**`findSnatchReplayNotFalsifiable` (`:207`) bounds the reading.** Under the
∀-over-inputs reading in `FindFalsifiable`, the recorded replay itself is
*not* falsifiable: give it a repository holding only `askForSurplusNotSurrender`
— a pattern every recorded scenario selects — and it returns the whole
repository. So there are two readings on the table:

1. **∀ over inputs** (`FindFalsifiable`): satisfied by `findRefusing`, refuted by
   `findIdentity` *and* by `findSnatchReplay`;
2. **at the recorded inputs against the declared zero-mass member**
   (`findSnatchReplayExcludesDeclaredZeroMass`, the grain
   `wmFindSnatchF4Falsifiable` uses): satisfied by the replay.

Reading 1 is strong enough to exclude the finder that replays the record, which
is a reason to doubt it is what §3e means; reading 2 is relative to a fixed
repository and a declared zero-mass set, which `FindResult` cannot carry. **This
slice does not choose.** The choice is a `:choices` entry for Joe, and both
alternatives are now theorems rather than positions.

## 5. Review findings, and what was checked

The dispatched commit `63dc9d37e0` built and was `sorry`-free. Three things the
build could not see were found on reading the diff, and fixed here rather than
re-dispatched:

1. **The receipts did not read the record.** `receipts` was
   `fun _ => some findStructuredReceipt` — a receipt for every pattern in the
   type — so F2 and F3 were witnessed about a constant and the fixture's
   `receipted` column went unread. `findSnatchReceipted` (`:50`) now reads that
   column; `findSnatchSelectedSubsetReceipted` (`:57`) is the bridge;
   `findSnatchReplayReceiptsArePartial` (`:110`) shows the receipts are partial
   on the repository — `consultTheRemedyBeforeExiting` is one of the eighteen
   recorded patterns and gets none at `g1Snatcher`. **Its bound, measured:** at
   the scenario grain the recorded receipted column *equals* the recorded
   selection in all six scenarios (artifact `:scenarios`,
   `:selected-minus-receipted` empty everywhere), so what the partiality
   separates is repository from selection, not receipted from selected — the
   same coincidence `Holes.lean:803` records for F3.
2. **F4 was proved with an arbitrary witness** (§4 above).
3. **`FindFalsifiable` was never tested against the replay** (§4 above).

What the review checked, so the gate is auditable:

- `lake build DarkTower.WarMachine.F11Conformance` re-run here: 2705 jobs, exit
  0, no warning from the new module; the `sorry` warnings are `Holes.lean`'s
  own, `:264` among them, unchanged.
- `#print axioms` over all thirteen theorems: `propext`, `Classical.choice`,
  `Quot.sound` only — no `sorryAx`. `grep` for `sorry`/`axiom`/`native_decide`
  in the file: zero.
- Two Lean negative controls against the zero-mass tactic: it fails to prove
  that a pattern the record *selects* is excluded (`askForSurplusNotSurrender`
  at `g1Snatcher`), and fails on another scenario's zero-mass member
  (`consultTheRemedyBeforeExiting` at `g4Snatcher`, which g4 selects). So the
  exclusion claim discriminates.
- `f11_conformance_check.bb`: PASS, with `f11_conformance_controls.sh` planting
  five defects — signature drift, an invented pattern name, a `sorry`, a removed
  declaration, and a mutated record in which a declared zero-mass pattern is
  selected — each rejected by name. Two runs write a byte-identical artifact
  (`5b6479cc30…`).
- `git status` on mathlib4: one file added, nothing else modified.

## 6. Pin versus live, reported and not gated

The checker gates on `futon3:checks/find-snatch.edn` (`839897ef…`), the pinned
fixture the four `findF*` docstrings name and the one `u46_find_transcribe.bb`
transcribed, and reads C559's live re-run only to report the divergence
(`:pin-vs-live`): repository 18 → 24, and the six live-only patterns are
selected by no scenario, so the recorded selections and every declared zero-mass
member agree between pin and live. Gating on the live file would fail this check
for C500 §3's fixture-pin defect rather than for anything about this slice.

## 7. Not done, stated

- The `sorry` at `Holes.lean:264` is **not** discharged. What this slice
  establishes is that discharging it is a choice among conformant
  implementations, and that F4's reading has to be settled first.
- **No ruling** was written. The F4 reading (§4) and the `Receipt` carrier
  question (§2) are both decisions, and both belong to Joe.
- The `Receipt` carrier was **not** widened. Adding `clause`, `route` and
  `asOf` fields would change a declaration in `Holes.lean` and is its own slice.
- `findSnatchRepository.standsOn` is `fun _ _ => False`: the recorded fixture
  carries no authored `standsOn` edges, so F3's "cites text **or authored
  edges**" is witnessed on the text half only.
- The remaining `:F11` work: the `Receipt` carrier decision, the F4 reading, and
  then the discharge or amendment of `Holes.lean:264`.
