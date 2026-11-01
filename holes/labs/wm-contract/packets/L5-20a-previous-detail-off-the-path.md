# L5-20a — The predecessor's refusal detail is not on the selection path (claude-2 → codex-13, 2026-11-01)

Lean: no change. futon2 `main`; worktree `~/worktrees/futon2/codex13-l5-20a`, branch `codex13/l5-20a`;
never check out a branch in `/home/joe/code/futon2`. Mode: work. One behaviour.

## The defect

Click 1 of the 21:03Z relaunch (`selection-record-…wm-click-0068e006…edn`): Q7 violated with
**2376 typed path absences**, of which 2373 are under `[:outer/selection :previous :selection-outcome
:detail …]`: the embedded predecessor refusal (click 16's `pick-not-below-abstention` detail with its
whole `:selection-evidence`). Facts export: `clojure -M -m wm-run-facts --run <record> --kind
selection` → `diagnostics.absencePaths`. L5-15 narrowed the path to selection dependencies; the
`:previous` carrier was not narrowed.

## Goal

The selection path counts, under `[:outer/selection :previous]`, only the three facts Q6 reads
(choice, outcome, input digest) and their own absences; nothing nested in the predecessor's
`:selection-outcome :detail` is on this record's path. The remaining absences on 0068e006 are
reported one by one (expected: `[:outer/learning :receipt :ending :classification-absence]` and the
`:previous` choice absence, the latter addressed by L5-20-L).

## Tests

1. Export of 0068e006: `pathAbsenceCount` ≤ 3 and no path contains `:detail` under `:previous`.
2. A constructed record whose own `:selection-outcome :detail` carries an absence still counts it.
3. The work-side projector is untouched (same `pathAbsenceCount` on the click-11 work record).

Gates: clj-kondo, check-parens, `clojure -X:test :nses` for touched namespaces. Commit; bell
claude-2 with sha, receipt, and the remaining absence paths.
