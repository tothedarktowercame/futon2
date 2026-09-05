/-! ### The 015-015-holes run's selection discrimination (worklist `:RE7`)

Joe's ruling of 2026-09-04 (worklist `:U51`/`:U52`): "the Lean model is
supposed to help by validating logged info. A 55-way tie should be seen as an
obvious defect." This block is the transcription that makes that decidable --
each recorded decision's controller-score tie as data, and the check's verdict
as a proposition about it.

SOURCES, both pinned and both committed:
* `futon2:holes/labs/wm-contract//home/joe/code/futon2/data/wm-step/w1/steps/015-015-holes/rationale` -- 1 rationale records
  for run `015-015-holes`; the tie of each `chosenRank`/`tieCount`/`tieBand` field is
  read from these.
* `futon2:holes/labs/wm-contract//home/joe/code/futon2/data/wm-step/w1/steps/015-015-holes/decision-records.edn` -- the run store's own
  trace extraction; `fieldSize` and `widestPlateauNotChosen` are recomputed from it.
  NOT the file the records' `:rationale/trace-path` names, which is
  ["/home/joe/code/futon2/data/wm-step/w1/sandbox/wm-trace/wm-trace-2026-09-05.edn"] -- the live, untracked
  corpus the tick wrote as it ran, which no reviewer and no other machine can
  read. Control C1 requires this committed file to reproduce every recorded
  tie, so the substitution is checked rather than assumed.

GENERATED from those files by
`futon2:holes/labs/wm-contract/re7_selection_discrimination.bb`; edit the
sources and regenerate rather than editing the literals.
-/

/-- One recorded decision's controller-score tie, transcribed from the run's
rationale record (`:rationale/chosen :controller-score-tie`, written at decision
time by `futon2:src/futon2/aif/selection_rationale.clj:139-153`) together with
the plateau census recomputed from the run store's own committed trace -- which
is not always the file the record names; each run block's SOURCES says which
file its census was computed from. Ranks are 1-based positions in the
controller ranking. -/
structure SelectionTie where
  /-- The tick's `:rationale/tick-id`, verbatim. -/
  tick : String
  /-- The chosen candidate's controller rank. -/
  chosenRank : Nat
  /-- How many candidates share the chosen candidate's controller score. -/
  tieCount : Nat
  /-- The lowest rank of that tie. -/
  tieBandLo : Nat
  /-- The highest rank of that tie. -/
  tieBandHi : Nat
  /-- Candidates scored on this tick. -/
  fieldSize : Nat
  /-- CENSUS, NOT VERDICT: the widest plateau of the field that does NOT hold
  the chosen candidate. -/
  widestPlateauNotChosen : Nat
  deriving DecidableEq, Repr

/-- DERIVED: the choice was made by the sort's tie-break rather than by the
score, i.e. the chosen candidate's score has no unique argmin. 1 is not a
threshold anyone picked: it is the width at which a score decides. -/
def SelectionTie.chosenByTiebreak (t : SelectionTie) : Bool := decide (1 < t.tieCount)

/-- The property `:selection-discrimination` verdicts on: the run recorded at
least one decision, and no decision's choice was a tie-break. Note what is NOT
here -- `widestPlateauNotChosen` is carried by the data and read by no
conjunct, because a plateau the choice is not in is a census and not a defect.

`reducible` because `decide` needs the `Decidable` instance for THIS
conjunction, and instance synthesis does not unfold an irreducible `def` (the
`runConformsToDrawnWiring` precedent). -/
@[reducible] def selectionDiscriminates (ts : List SelectionTie) : Prop :=
  0 < ts.length ∧ ts.all (fun t => !t.chosenByTiebreak) = true

/-- The 1 decisions run `015-015-holes` recorded, ordered by tick. -/
def chk015015holesSelectionTies : List SelectionTie :=
  [{ tick := "2026-09-05T04:30:38.167552854Z", chosenRank := 1, tieCount := 1, tieBandLo := 1, tieBandHi := 1, fieldSize := 48, widestPlateauNotChosen := 6 }]

/-- THE `:selection-discrimination` VERDICT for run `015-015-holes`, decided.
Every one of the 1 recorded decisions chose a candidate whose
controller score no other candidate shared, so no choice was made by the sort's
tie-break.

WHAT IT DOES NOT SHOW, because a reader will otherwise take it for more: the
score field of these ticks still carries a plateau 6 candidates wide that the
chosen candidate is not in (`widestPlateauNotChosen`). The proposition is about
the CHOICES this run made, not about whether the scoring discriminates.

Proved by `decide` over the transcribed data, no `sorry` and no `native_decide`
-- the `wmS5RunConformsToDrawnWiring` precedent. The Clojure side of the same
comparison is `futon2:holes/labs/wm-contract/re7_selection_discrimination.bb`,
whose verdict for this run is `:green`; the plants that move it are in
`/home/joe/code/futon2/data/wm-step/w1/steps/015-015-holes/re7/03-controls.edn`, controls C2-C6. -/
theorem wmChk015015holesSelectionDiscrimination :
    selectionDiscriminates chk015015holesSelectionTies := by
  decide

/-- The census the verdict is stated over, so the numbers a reader checks
against `/home/joe/code/futon2/data/wm-step/w1/steps/015-015-holes/re7/01-decisions.edn`
are themselves decided rather than asserted in prose: 1 decisions, 0 of them
chosen by tie-break, widest chosen tie 1, deepest chosen rank 1, field size
48, and the widest plateau NOT holding the choice 6. -/
theorem wmChk015015holesSelectionTieCensus :
    chk015015holesSelectionTies.length = 1 ∧
      (chk015015holesSelectionTies.filter (fun t => t.chosenByTiebreak)).length = 0 ∧
      (chk015015holesSelectionTies.map (fun t => t.tieCount)).foldl max 0 = 1 ∧
      (chk015015holesSelectionTies.map (fun t => t.chosenRank)).foldl max 0 = 1 ∧
      (chk015015holesSelectionTies.map (fun t => t.fieldSize)).foldl max 0 = 48 ∧
      (chk015015holesSelectionTies.map (fun t => t.widestPlateauNotChosen)).foldl max 0 = 6 := by
  decide
