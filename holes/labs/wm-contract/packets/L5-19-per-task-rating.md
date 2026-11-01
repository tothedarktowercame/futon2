# L5-19 — A rating for every admitted task (claude-2 → codex-11, 2026-11-01)

Authority: Joe 2026-11-01 ~20:40Z: "We should perhaps spatialize the machine's ratings like we did
with the old EFE field, but now using more up to date rankings. … Surely you could find a top ten
using these resources." Basis: L5-17-D (`reconciliation/L5-17-D-order-is-the-product-codex11-2026-11-01.md`
§2, §4): today only the two or three cascade picks carry a G; 591 admitted tasks carry none.
Lean: **no change**. The decision, the compared classes and Q11 are untouched; this adds a
diagnostic carrier that no requirement reads. No new refusal.

## Goal

On every selection record, `[:outer/selection :per-task-rating]`: for each admitted task, the G
the outer kit would assign if a cascade picked it, under the same kernel input, preference and
status prior the click used (`outer-score/outer-model` with that task as the candidate; the
relation from the record's `:cascade-pick-relations` producer for that task, `:unknown` when it is
not known, typed, never defaulted). Each row: `{:target :status :rank :cone :unblocks :relation
:terms {:risk :ambiguity :novelty [:altitude]} :G :prediction-digest}`; tasks whose model cannot
be formed carry a typed `:absence` instead of a G. The record also carries `:top-ten` = the ten
lowest G with their Giraffe ranks beside, and the run facts projector is **untouched**.

Consistency pins (the test that matters): for every class on the record, the per-task G of its
pick equals the class G to 1e-9; a stopping-equivalent pick's G carries the altitude exactly as
the abstention does. Cost: L5-17-D §2 estimates ~9.5k outcome cells for 636 tasks; measure the
wall time on the click-1 record (`selection-record-2026-11-01-wm-click-0068e006-…edn`) and report
it; refuse the slice if it exceeds 5 s, do not optimise.

## Where

`src/futon2/aif/outer_select.clj` where `:classes` is assembled (~157–172); `outer_score.clj` only
if a per-candidate entry point is missing (no change to `apply-selection-cascade`, `cascade-pick`,
`outer-model` semantics). Worktree `~/worktrees/futon2/codex11-l5-19`, branch `codex11/l5-19` from
`main`; never check out a branch in `/home/joe/code/futon2`.

## Tests (live pins from the click-1 record)

1. Pick G == class G for both compared classes; abstention G reproduced.
2. A task with relation `:unknown` is typed, not scored as `:unrelated`.
3. `:top-ten` has ten rows, ascending G, each with the Giraffe rank from the field.
4. Row count == admitted count (591) with `:absence` rows counted and typed.

Gates: clj-kondo, check-parens, `clojure -X:test :nses` for touched namespaces. Commit; bell
claude-2 with sha, receipt, the measured time, and the top ten by G beside the Giraffe top ten.
