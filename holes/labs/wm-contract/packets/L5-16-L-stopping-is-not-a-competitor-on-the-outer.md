# L5-16-L — On the selection kind, stopping is not a competitor (claude-2 → codex-5, 2026-11-01)

Lean owner: claude-2; this packet is the specification. Authority:
`reconciliation/RULING-outer-stopping-is-not-a-competitor-2026-11-01.md` (Joe, verbatim there).
Base: mathlib4 `darktower` (≥ `ddbb3c05fd`), futon2 `main` (≥ this packet). Worktrees only (the
mathlib4 path trap: `~/worktrees/mathlib4-l5-16`, `.lake` symlinked, oleans deleted before the review
build).

## Lean (`DarkTower/WarMachine/Requirements.lean`)

1. `Q11`, `.pick` branch: the abstention comparison `chosen.2 < r.abstentionG` and the
   `predictsOtherThanStopping` gate hold **only when `r.kind = .work`**. For `.selection` the
   pick must be in `comparedPolicies`, in `enumeratedTasks`, and the strict G minimum over every
   other compared policy not in `forbidden`; `comparisonRecorded` and the two-predictions clause
   stay. Write it as a match on `r.kind`, not as a disjunction buried in the decide.
2. `.abstainOnTask` branch already requires `r.kind = .work`; leave it. `.abstainEmptyField`
   unchanged.
3. Theorem: for `r.kind = .work` the new `Q11` equals the old one (state the old body as a
   private def and prove equality by `rfl`/`simp`). Add a `#eval` fixture in `RequirementsReader`
   of a selection-kind pick whose G is above the abstention's and is the strict minimum: Q11 true;
   the same facts with kind work: Q11 false.
4. Docstring on Q11 quoting the ruling's sentence "worth doing should be almost all open tasks,
   the question is order".

## futon2

5. `outer_select.clj`: remove the `:pick-not-below-abstention` refusal on the outer (~line 206);
   compare every admissible class including L5-11's stopping-equivalent ones (undo the exclusion
   from the comparison; keep `:stopping-equivalent` as a diagnostic list of classes whose prediction
   equals the abstention's, and add `:every-class-predicts-stopping? true/false`). Remove the
   `:all-policies-predict-stopping` abstention: on a non-empty field with admissible classes the
   outer always picks the minimum (ties still refuse `:tie-at-minimum`; `:no-admissible-pick`
   still refuses when every class is forbidden). Abstention G stays computed and on the record.
6. `decision_gate.clj`: `:pick-not-below-abstention` stays in the closed set for the inner.
7. Projector: `comparedPolicies`/`policyG`/`policyPrediction` are all classes again on the outer;
   `:stopping-equivalent` remains a diagnostic. Checker: no rendering change beyond what Lean needs.
8. Loop (`scripts/wm_click_repair_loop.py`): `nothing_beats_stopping` and its pause rule become
   dead for the outer; leave the code (it triggers only on the refusal kind, which the outer no
   longer emits) and add one line to its docstring saying so.

## Tests

- Lean: the work-kind agreement theorem; the two `#eval` fixtures; `lake build` of Requirements and
  the reader after deleting their oleans (real build).
- futon2: click 12's record re-selected through `outer-select/select` on its retained observation
  picks M-interim-director-proxy-metric-inventory with G 3.3896 (strict minimum); the export's
  Q11 is `.known` and the checker evaluates holds; the abstention G 3.2782 is on the record. A
  fixture where every class predicts stopping still picks the minimum and sets
  `:every-class-predicts-stopping? true`. A tie still refuses. The inner `pick-not-below-abstention`
  test(s) unchanged and green. Named, outer, projector namespaces vs main; hermetic; data count.

## Gates

kondo, parens, no reload, no merge (either repo), no `data/` writes. Branches `l5-16-outer-stopping`
(mathlib4), `codex5/l5-16` (futon2). Bell claude-2 with both shas, the real build log tail, and
click 12's re-selection and export.
