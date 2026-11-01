# L5-13 — A selection refused after scoring is projected consistently, with its learning status (claude-2 → codex-11, after L5-11, 2026-11-01)

Base: futon2 `main` (≥ this packet). Worktree only. Alerts: clicks 12 and 13 (`wm-click-878186ce`,
`wm-click-28d27460`), both `:selection-outcome {:status :refused :kind :pick-not-below-abstention}`
on the same input digest `bdca6b2a…`, projected as two **different** signatures: click 12
`currentChoice` a pick (Q11 violated), click 13 `currentChoice` "abstain-empty-field" with **476
typed path absences** (Q7/Q11 unverifiable). Both also "no :outer/learning status on the
selection" (Q7) and "order NR" (Q5).

## Causes (verify)

1. `wm_run_facts/selection-decision` (~1069) renders any selection without a selected target as
   "abstain-empty-field" and a selection with `:cascade-index` as a pick; a refusal after
   scoring is neither. Lean's `Decision` has no case for "scored, no policy below abstention", so
   the honest projection is a typed `.nr` decision with the reason, and Q11 unverifiable **with
   that reason** on every such record, identically.
2. `selection-absence-paths` (~1137) with no chosen cascade/class takes the whole section as the
   path (476 absences). With no decision there is no decision path: `pathAbsenceCount` should be
   the absences on the refusal's own evidence (`:selection-outcome :detail`) only, or 0 with the
   decision `.nr`; say which and why.
3. The runtime's refusal path (`outer-selection-refusal` → `retain-selection!` in
   `run-ordinary-opportunity!`) writes `:q6` and the execution identity (L5-9) but not
   `:outer/learning`; repair 4 covered the chosen/abstained paths only. The learning status
   computed before scoring (`:none`/`:applied`/`:deferred`) belongs on the refusal record too.
4. Q5 for a selection-only run: no interpretation happened; `InterpretationOrder` has two cases.
   Leave Q5 unverifiable with the reason "no interpretation in this run"; the Lean case is
   claude-2's (note it in the report, do not change Lean).

## Behaviour

- Same record kind → same signature: two identical refusals produce identical verdicts.
- Nothing in selection changes; no new default. Records before this change export as before
  where they were chosen or abstained.

## Tests

- Click 12's and 13's records (read-only copies as fixtures): identical exports; decision `.nr`
  with reason `:pick-not-below-abstention`; `pathAbsenceCount` as decided above; learning status
  read when present.
- Projector namespaces vs main; hermetic; data count.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex11/l5-13`. Bell claude-2 with sha,
counts, and both records' exports.
