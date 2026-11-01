# L5-4a — Producer: the selection run records every cascade's score, prediction and pick, scores stopping separately, and refuses a tie (claude-2 → codex-11, 2026-11-01)

First change to the outer selection producers under the 2026-11-01 incident. Authority: Joe's
rulings of 2026-11-01 (plan `PLAN-outer-selection-restoration-claude2-2026-11-01.md`, "L5 slices"
and "Grounding slices"); Lean `Requirements.lean` Q11 and `SelectionGrounding.lean` G2–G4 at
darktower `5025ad1263`; the L4b carrier map
(`reconciliation/L4b-carrier-map-claude3-2026-11-01.md`, rows 9–10) names what the record lacks.
Base: futon2 `main` at `42994a7c5`. Scope: `src/futon2/aif/outer_select.clj`,
`src/futon2/aif/outer_score.clj`, their tests. Clicks are held; nothing is reloaded into the JVM.

## What the record lacks today (L4b rows 9–10, read them)

`:outer/selection` keeps `:cascade-scores` (one double per menu position), `:cascade-id`,
`:cascade-index`, `:cascade-picks` (only since 10-31), and one `:prediction`, the chosen's. No
per-cascade terms, no per-cascade prediction, no abstention scored on its own (a no-pick cascade's
score sits in the menu by accident), and a tie at the minimum is settled by `.indexOf`
(`outer_select.clj:151`), that is, by menu order.

## Behaviour

In `outer-select/select` (`outer_select.clj:103-200`):

1. **Per-cascade record.** For every menu cascade, not only the chosen, compute `outer-model` and
   `outer-score` and retain under `:cascades` a vector, in menu order, of
   `{:id <cascade name> :digest <sha> :steps <typed steps> :pick <target or :no-pick>
     :pick-facts {:status :cone :unblocks :rank} :relation {:class :receipt-digest}
     :reach {:value :source} :terms {:risk :ambiguity :novelty} :G <double>
     :prediction {:outcomes :q :landing :status}}`.
   `:digest` = sha256 of `(pr-str [name readme-row units edges])` as the witness emitter does
   (`witnesses/emit-selection-grounding-lean.bb`), where units are the makeup's pattern names
   with the task-selection library file shas (read `futon3/library/task-selection/<pattern>.flexiarg`,
   pin the manifest sha beside it as `:library {:root :sha256 :count}`). Typed steps:
   `:ready-gate`, `{:status-gate <status>}`, `:by-cone`, `:by-unblocks`, `:by-rank`, or
   `{:named <step string>}` for a README step that is none of those; derive them from the cascade's
   `:steps` as `apply-selection-step` interprets them, so the record's steps are what ran.
   `:cascade-scores`, `:cascade-picks`, `:cascade-pick-relations`, `:prediction` stay as they are
   (readers: `wm_run_facts.clj` L4c-2a; `outer_refusal_history.clj`) and must equal the
   corresponding `:cascades` entries; add an assertion.
2. **Abstention scored separately.** Score the stopped kernel (`stopped-kernel`, currently
   private at `outer_score.clj:88`) as its own policy over the same field and focus:
   `:abstention {:terms … :G … :prediction …}`. It is not a menu member; a menu cascade that
   picks nothing is still recorded as such, but the floor every pick must beat is this value.
3. **Tie refuses.** Replace `(.indexOf scores minimum)` with: the set of cascades whose G equals the
   minimum (exact on the doubles as scored; the projector rounds). If more than one, `refuse!
   :tie-at-minimum {:cascades [ids] :G minimum}` regardless of whether their picks agree
   (Lean Q11: a tie at the minimum means G did not choose). If the minimum is not strictly below
   `:abstention :G`, `refuse! :pick-not-below-abstention {:cascade :G :abstention-G}`. Remove the
   `:tie?`/`:tied-cascades` distinct-pick reading; keep `:tied-cascades` as the list that caused a
   refusal when it does.
4. **Relation receipt.** `relation-class` returns a class; retain beside it the digest of the
   classify-target receipt it came from (`focus_receipt.clj` `classify-target` output) so the
   projector can emit `pickRelation {"observed" <digest>}`; `:unknown` stays `:unknown`.
5. **Reach source.** Record `:reach {:value v :source :default-reach}` when the value came from the
   makeup (`outer_observation.clj:119`), so the record says defaulted instead of the projector
   inferring it. Observed reach is L5-4b; do not compute one here.

Nothing else in the section changes. `:outer-task-selection` (the stub) is L5-4b.

## Tests (one live pin each; the fixture is the retained 11-01 observation if a test already
loads one, else the smallest field that exercises the branch)

In `outer_select_test.clj` / `outer_score_test.clj`:
- `:cascades` has one entry per menu cascade with terms whose sum equals `:G` (to 1e-9) and a
  prediction that sums to 1; the chosen entry's `:prediction` equals `:prediction`.
- `:abstention :G` equals the score of the stopped kernel and the chosen `:G` is strictly below it.
- A menu with two cascades at the same minimal G refuses `:tie-at-minimum` even when both pick the
  same task (the 10-31 shape: three equal scores).
- A field where every pick scores at or above abstention refuses `:pick-not-below-abstention`.
- Typed steps for the three README cascades of the witness: `[:ready-gate :by-cone :by-unblocks]`,
  the repair-click cascade carrying `{:named …}` entries, `[{:status-gate :undetermined} :by-rank]`
  if that is how `apply-selection-step` reads it (say what it reads).
- `:reach :source` is `:default-reach` on the retained observation.
- Existing tests in the sixteen `outer_*_test` namespaces: run them all; any that pinned
  `.indexOf` tie-breaking or `:tie?` over distinct picks must change and you must say which.

## Gates

clj-kondo, `check-parens.el`; `clojure -X:test :nses '[futon2.aif.outer-select-test
futon2.aif.outer-score-test futon2.aif.outer-observation-test futon2.aif.outer-selection-wiring-test
futon2.aif.outer-refusal-history-test futon2.aif.outer-consumption-test]'` and the rest of the
outer namespaces. Branch `codex11/l5-4a-selection-scoring-record` from `main` in
`~/worktrees/futon2-l5-4a` (never `/tmp`). No `data/` writes; no reload into the running JVM. Do not
merge. Bell claude-2 with sha, test output, which existing tests changed and why, and anything in
`select` or the scorer that does not fit this packet. If the library pin (step 1) needs a
manifest that does not exist, record `:library {:status :absent :reason …}` typed, and say so.
