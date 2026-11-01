# L5-21 — An unknown relation to the focus is a prior, not a stop (claude-2 → codex-12, 2026-11-01)

Authority: Joe 2026-11-01: "worth doing should be almost all open tasks, the question is order";
stopping is "a provably bad" competitor. Lean: **no change**; this makes `predictsOtherThanStopping`
hold on real clicks instead of being dodged by exclusion. futon2 `main`; worktree
`~/worktrees/futon2/codex12-l5-21`, branch `codex12/l5-21`; never check out a branch in
`/home/joe/code/futon2` (serving source). Mode: work. One behaviour.

## The defect (click 1 of the 21:03Z relaunch, `selection-record-…wm-click-0068e006…edn`, and
## click 1 of the 21:22Z relaunch, `…wm-click-3c866bd4…edn`)

`src/futon2/aif/outer_score.clj` `outer-model` (~line 196–218): `ending (ending-class relation focus
target)` is nil when the pick's relation to the focus is `:unknown`; then `landing` falls to the
kernel's `:stopped` landing with mass 1.0, so the class's pushed prediction **equals the abstention's**.
L5-11 then drops the class from the comparison as "stopping-equivalent", which leaves one compared
policy: Q2's selection floor of 2 is violated and Q11 cannot be verified (click 1 verdict: Q2
violated, Q11 unverifiable). On click 3c866bd4 the top ready task M-interim-director was dropped this
way and the machine chose an undetermined task. "Stopping-equivalent" has meant "relation unknown"
on every record today; a task we have not related to the focus is not a certain stop.

## Goal

When the relation is `:unknown`, the landing is a **distribution over the retraction endings**
(`:focused :related :unrelated`) with the gain as computed (`ready-gain` of unblocks, or the discovery
gain), using a declared prior added to `kernel-definition` (`:unknown-relation {:form
:prior-over-retraction-endings :prior {:focused … :related … :unrelated …}}`, first value the
normalised `c-kit` masses of those three endings; pinned like the other kit parameters, Joe's to
change). The reach stays the task's reach; the remainder goes to `:stopped` as now. The model carries
`:relation-basis :prior` and the record's class carries it. Known relations are unchanged byte for
byte. `stopped-model` is unchanged. L5-11's exclusion stays in place (a class may still equal stopping
by coincidence) but is no longer reached on these records.

## Tests (live pins)

1. Record 0068e006 re-scored under the new kernel: both constructed classes compared; the
   interim-director class's prediction digest ≠ abstention's; its G carries no altitude; the proxy
   class's G is unchanged to 1e-9; chosen pick unchanged.
2. Record 3c866bd4: the ready class (M-interim-director, relation `:unknown`) is compared; report
   which class now wins and both Gs (do not assert which; report it).
3. Known-relation class: identical terms before/after (byte-equal prediction).
4. `kernel-definition` carries the new block and the record's `:construction :kernel-input` shows it.
5. Facts export (`clojure -M -m wm-run-facts --run <record> --kind selection`) for the re-scored
   0068e006: `comparedPolicies` has 2, `policyPrediction` digests both ≠ `abstentionPrediction`.

Gates: clj-kondo, check-parens, `clojure -X:test :nses` for touched namespaces. Commit; bell
claude-2 with sha, receipt, and the two Gs on 3c866bd4.
