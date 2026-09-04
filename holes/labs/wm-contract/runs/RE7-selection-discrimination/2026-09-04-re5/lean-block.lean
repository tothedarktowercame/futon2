/-! ### The 2026-09-04-re5 run's selection discrimination (worklist `:RE7`)

Joe's ruling of 2026-09-04 (worklist `:U51`/`:U52`): "the Lean model is
supposed to help by validating logged info. A 55-way tie should be seen as an
obvious defect." This block is the transcription that makes that decidable --
each recorded decision's controller-score tie as data, and the check's verdict
as a proposition about it.

The shared definitions -- `SelectionTie`, `SelectionTie.chosenByTiebreak`,
`selectionDiscriminates` -- are the ones the first `:RE7` block above defines
and are NOT redefined here. They are a function of the producer alone, so
reusing them is a claim that the producer has not moved since that block was
generated; control C9 checks it rather than assuming it.

SOURCES, both pinned and both committed:
* `futon2:holes/labs/wm-contract/runs/2026-09-04-re5/rationale` -- 4 rationale records
  for run `2026-09-04-re5`; the tie of each `chosenRank`/`tieCount`/`tieBand` field is
  read from these.
* `futon2:holes/labs/wm-contract/runs/2026-09-04-re5/wm-trace-re5.edn` -- the run store's own
  trace extraction; `fieldSize` and `widestPlateauNotChosen` are recomputed from it.
  NOT the file the records' `:rationale/trace-path` names, which is
  ["/home/joe/code/futon2/data/wm-trace/wm-trace-2026-09-04.edn"] -- the live, untracked
  corpus the tick wrote as it ran, which no reviewer and no other machine can
  read. Control C1 requires this committed file to reproduce every recorded
  tie, so the substitution is checked rather than assumed.

GENERATED from those files by
`futon2:holes/labs/wm-contract/re7_selection_discrimination.bb`; edit the
sources and regenerate rather than editing the literals.
-/

/-- The 4 decisions run `2026-09-04-re5` recorded, ordered by tick. -/
def re5SelectionTies : List SelectionTie :=
  [{ tick := "2026-09-04T07:46:52.742121237Z", chosenRank := 1, tieCount := 1, tieBandLo := 1, tieBandHi := 1, fieldSize := 146, widestPlateauNotChosen := 56 },
   { tick := "2026-09-04T07:48:10.260151161Z", chosenRank := 1, tieCount := 1, tieBandLo := 1, tieBandHi := 1, fieldSize := 146, widestPlateauNotChosen := 56 },
   { tick := "2026-09-04T07:49:29.060729571Z", chosenRank := 1, tieCount := 1, tieBandLo := 1, tieBandHi := 1, fieldSize := 146, widestPlateauNotChosen := 56 },
   { tick := "2026-09-04T07:50:47.952039102Z", chosenRank := 1, tieCount := 1, tieBandLo := 1, tieBandHi := 1, fieldSize := 146, widestPlateauNotChosen := 56 }]

/-- THE `:selection-discrimination` VERDICT for run `2026-09-04-re5`, decided.
Every one of the 4 recorded decisions chose a candidate whose
controller score no other candidate shared, so no choice was made by the sort's
tie-break.

WHAT IT DOES NOT SHOW, because a reader will otherwise take it for more: the
score field of these ticks still carries a plateau 56 candidates wide that the
chosen candidate is not in (`widestPlateauNotChosen`). The proposition is about
the CHOICES this run made, not about whether the scoring discriminates.

Proved by `decide` over the transcribed data, no `sorry` and no `native_decide`
-- the `wmS5RunConformsToDrawnWiring` precedent. The Clojure side of the same
comparison is `futon2:holes/labs/wm-contract/re7_selection_discrimination.bb`,
whose verdict for this run is `:green`; the plants that move it are in
`runs/RE7-selection-discrimination/2026-09-04-re5/03-controls.edn`, controls C2-C6. -/
theorem wmRe5SelectionDiscrimination :
    selectionDiscriminates re5SelectionTies := by
  decide

/-- The census the verdict is stated over, so the numbers a reader checks
against `runs/RE7-selection-discrimination/2026-09-04-re5/01-decisions.edn`
are themselves decided rather than asserted in prose: 4 decisions, 0 of them
chosen by tie-break, widest chosen tie 1, deepest chosen rank 1, field size
146, and the widest plateau NOT holding the choice 56. -/
theorem wmRe5SelectionTieCensus :
    re5SelectionTies.length = 4 ∧
      (re5SelectionTies.filter (fun t => t.chosenByTiebreak)).length = 0 ∧
      (re5SelectionTies.map (fun t => t.tieCount)).foldl max 0 = 1 ∧
      (re5SelectionTies.map (fun t => t.chosenRank)).foldl max 0 = 1 ∧
      (re5SelectionTies.map (fun t => t.fieldSize)).foldl max 0 = 146 ∧
      (re5SelectionTies.map (fun t => t.widestPlateauNotChosen)).foldl max 0 = 56 := by
  decide
