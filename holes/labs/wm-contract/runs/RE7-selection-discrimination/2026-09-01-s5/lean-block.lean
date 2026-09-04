/-! ### The 2026-09-01-s5 run's selection discrimination (worklist `:RE7`)

Joe's ruling of 2026-09-04 (worklist `:U51`/`:U52`): "the Lean model is
supposed to help by validating logged info. A 55-way tie should be seen as an
obvious defect." This block is the transcription that makes that decidable --
each recorded decision's controller-score tie as data, and the check's verdict
as a proposition about it.

SOURCES, both pinned and both committed:
* `futon2:holes/labs/wm-contract/runs/RE4-rationale-logging/store` -- 4 rationale records
  for run `2026-09-01-s5`; the tie of each `chosenRank`/`tieCount`/`tieBand` field is
  read from these.
* `futon2:holes/labs/wm-contract/runs/2026-09-01-s5/wm-trace-s5.edn` -- the run store's own
  trace extraction; `fieldSize` and `widestPlateauNotChosen` are recomputed from it.

GENERATED from those files by
`futon2:holes/labs/wm-contract/re7_selection_discrimination.bb`; edit the
sources and regenerate rather than editing the literals.
-/

/-- One recorded decision's controller-score tie, transcribed from the run's
rationale record (`:rationale/chosen :controller-score-tie`, written at decision
time by `futon2:src/futon2/aif/selection_rationale.clj:139-153`) together with
the plateau census recomputed from the run-store trace that record names. Ranks
are 1-based positions in the controller ranking. -/
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

/-- The 4 decisions run `2026-09-01-s5` recorded, ordered by tick. -/
def s5SelectionTies : List SelectionTie :=
  [{ tick := "2026-09-01T22:50:42.709079837Z", chosenRank := 123, tieCount := 55, tieBandLo := 73, tieBandHi := 127, fieldSize := 145, widestPlateauNotChosen := 6 },
   { tick := "2026-09-01T22:52:07.418144767Z", chosenRank := 115, tieCount := 55, tieBandLo := 73, tieBandHi := 127, fieldSize := 145, widestPlateauNotChosen := 6 },
   { tick := "2026-09-01T22:53:32.871991661Z", chosenRank := 123, tieCount := 55, tieBandLo := 73, tieBandHi := 127, fieldSize := 145, widestPlateauNotChosen := 6 },
   { tick := "2026-09-01T22:54:48.942309598Z", chosenRank := 115, tieCount := 55, tieBandLo := 73, tieBandHi := 127, fieldSize := 145, widestPlateauNotChosen := 6 }]

/-- THE `:selection-discrimination` VERDICT for run `2026-09-01-s5`, decided.
4 of the 4 recorded decisions chose a candidate from inside a tie
of up to 55 candidates sharing one controller score to sixteen digits -- so the
score ranked a plateau and the sort's tie-break picked the member. The chosen
rank runs as deep as 123 in a field of 145, which a reader would otherwise take
for a large score gap.

This is the defect Joe's ruling names. The negation is stated rather than a
positive `chosenByTiebreak` conjunct because it is the SAME proposition the
green run satisfies, so the two certificates are comparable.

Proved by `decide` over the transcribed data, no `sorry` and no `native_decide`
-- the `wmS5RunConformsToDrawnWiring` precedent. The Clojure side of the same
comparison is `futon2:holes/labs/wm-contract/re7_selection_discrimination.bb`,
whose verdict for this run is `:defect`; the plants that move it are in
`runs/RE7-selection-discrimination/2026-09-01-s5/03-controls.edn`, controls C2-C6. -/
theorem wmS5SelectionDiscrimination :
    ¬ selectionDiscriminates s5SelectionTies := by
  decide

/-- The census the verdict is stated over, so the numbers a reader checks
against `runs/RE7-selection-discrimination/2026-09-01-s5/01-decisions.edn`
are themselves decided rather than asserted in prose: 4 decisions, 4 of them
chosen by tie-break, widest chosen tie 55, deepest chosen rank 123, field size
145, and the widest plateau NOT holding the choice 6. -/
theorem wmS5SelectionTieCensus :
    s5SelectionTies.length = 4 ∧
      (s5SelectionTies.filter (fun t => t.chosenByTiebreak)).length = 4 ∧
      (s5SelectionTies.map (fun t => t.tieCount)).foldl max 0 = 55 ∧
      (s5SelectionTies.map (fun t => t.chosenRank)).foldl max 0 = 123 ∧
      (s5SelectionTies.map (fun t => t.fieldSize)).foldl max 0 = 145 ∧
      (s5SelectionTies.map (fun t => t.widestPlateauNotChosen)).foldl max 0 = 6 := by
  decide
