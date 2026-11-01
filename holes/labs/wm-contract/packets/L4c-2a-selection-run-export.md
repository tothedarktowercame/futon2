# L4c-2a — Projector: the selection run as its own RunFacts export (claude-2 → claude-3, 2026-11-01)

Plan slice L4, third part, second packet. Source of truth: your L4b report, rows 8–13
(`reconciliation/L4b-carrier-map-claude3-2026-11-01.md`). Base: futon2 `main` at `c3c5279b6`
(L4c-1 merged with one review fix: `cascadesWithoutG` now shares the constructed id space);
mathlib4 darktower `55878e243a`. Scope: `scripts/wm_run_facts.clj` and its tests. The alert
learning to submit two exports per record is L4c-2b, not this packet; here the acceptance runs
the checker and Lean by hand on the new export.

## Behaviour

`clojure -M -m wm-run-facts --run <record> --kind selection` emits a RunFacts export for the
selection run recorded in the same tick record's `[:outer/selection]` (OS below), with
`"kind" "selection"`. Without `--kind`, or with `--kind work`, the output is exactly today's.
The projector labels the kind; nothing in the record does.

## Field by field (decisions taken, so you are not guessing)

- **Q1.** `openMissions`/`openExcursions`/`openTickets` as the work export. `enumeratedTasks` =
  `OS :outer-field/admitted` ∪ targets of `OS :outer-field/excluded` (row 8). Empty OS or
  absent OS → every selection carrier `.nr "no :outer/selection section"`.
- **Q2.** `targetConstruction`, `constructorPatternCount`, `libraryPatternCount`:
  `.nr "task-selection library pin, slice and cascade digests absent from the record (L4b row 11; L5)"`.
  Do not read the task-selection library directory to fill them.
- **The menu.** The record has scores by position and the chosen `:cascade-id`, but the menu's
  names live only in `outer-makeup.edn`, pinned by `OS :makeup/sha256`. Rule: read
  `data/wm-learning/outer-makeup.edn` only if its sha256 equals the pin; then the menu is its
  `:cascades` in order, and a policy id is `action-identity/digest` of the cascade name. If the
  sha differs or the file is absent: `comparedPolicies`, `constructedCascades`, `policyG`,
  `cascadesWithoutG` are `.nr "outer-makeup.edn at pinned sha <sha> not available"`.
  `comparedPolicies` = `constructedCascades` = all menu cascades (the scores are one per menu
  position, row 9b).
- **`policyG`** = one row per menu position from `OS :cascade-scores`, scale 10^6 HALF_EVEN with
  `"scale"` and `"double"` as in L4c-1. A score count that differs from the menu length → `.nr`
  naming both counts.
- **`cascadesWithoutG`** = menu cascades with no score (expected `[]`).
- **`policyPrediction`**: `.nr "only the chosen cascade has a prediction (L4b row 9; L5)"`.
- **`abstentionG`, `abstentionPrediction`**: `.nr "no separately scored stopped kernel on the
  selection path (L4b row 10; L5)"` in every record, including the three with `:cascade-picks`.
  A no-pick cascade's score is a menu member, not the abstention carrier.
- **Decisions.** `currentChoice` = `{"target" (OS :selection :selected :target), "cascade"
  (digest of OS :cascade-id)}` when a target was selected. When the chosen cascade picked nothing
  (`:cascade-picks` at `:cascade-index` is `:no-pick`, or `:selection :selected` is absent):
  `"abstain-empty-field"`. Lean then reads the 626-task field as a Q11 violation, which is the
  intended verdict; do not invent another shape.
  `previousChoice`, `previousOutcome`, `previousInputDigest`, `currentInputDigest`:
  `.nr "no selection-level previous decision in the record (L4b row 13; L5)"`. Q6 is unverifiable
  on selection runs until L5; `[:world-at-selection :selection-input-digest]` is too wide and is
  not to be used here.
- **Q4.** `policyPreferences`, `gTerms`, the three term counts: `.nr "no per-cascade terms on the
  selection path (L4b row 9)"`.
- **Q5.** `interpretationOrder`: `.nr "no interpretation step on a selection run"`.
- **Q9.** `factorizedPreference` `nil` (the verified negative) as for the work run; the two
  authorization registries as the work export reads them.
- **Q7.** `pathAbsenceCount` = the count of typed absences under `OS` and `[:outer-task-selection]`
  by the same rule the work export applies to its path (`:status :absent`, `:not-supplied`,
  `:classification-receipt-absent`, typed refusals). State the number for ac5bc477 in the bell.
  `selectionPath`:
  - `receipt` = `{"observed" <digest of OS>}`.
  - `pickRelation` = `OS :selected-relation`: `:unknown` → `"absent"`; a class in
    `#{:focus :associated :useful-elsewhere}` → `{"observed" <digest of the relation fact>}`
    (weaker than a classify-target receipt; say so in a code comment); any other value →
    `"absent"`.
  - `pickReach` = `OS :selection :selected :reach`: when it equals the pinned makeup's
    `:default-reach` and the record carries no reach observation receipt → `"defaulted"`; when a
    reach receipt exists (none does today; name the key you would look for) → `{"observed" …}`;
    absent → `"absent"`. The rule is a record comparison, not knowledge of the code.
  - `pendingRun`: `:status :applied` → `[<run-id of OS :outer/learning :receipt>, <provenance of its ending>]`;
    `:deferred` → `[0, "absent"]` (the record has no run id, row 12); learning absent → `.nr` for
    the whole `selectionPath`.
  - `consumedRuns`: from `OS :outer/learning :receipt`: `[[run-id, "absent"]]` when
    `:classification-absence` is present, `[[run-id, {"observed" <digest of the ending receipt>}]]`
    when a classified ending is present; `[]` when deferred.
  - `learningApplied`: `:applied` → true, `:deferred` → false.
- **`policyGScale`** 1000000 as in L4c-1.

## Tests (one live pin per behaviour)

On the `ac5bc477-carriers.edn` fixture extended with its `:outer/selection` (regenerate the
subset; say its new size) and a copy of the pinned `outer-makeup.edn` under
`test/fixtures/wm-run-facts/` with its sha:
- kind is `"selection"`; `enumeratedTasks` count is admitted + excluded (state the number);
- `comparedPolicies` = `constructedCascades`, 3 ids, equal to the digests of the makeup names;
- `policyG` has 3 rows, numerators `[3212121 3212121 3212121]`, denominators 10^6;
- `currentChoice.cascade` ∈ `comparedPolicies`;
- `selectionPath.pickRelation` is `"absent"` (this record's relation is `:unknown`);
- `selectionPath.pickReach` is `"defaulted"`; with the fixture makeup's `:default-reach` changed,
  it is `"absent"`-or-`"observed"` per your rule (pin what the rule gives);
- `learningApplied` true, `consumedRuns` one row `"absent"`;
- a makeup whose sha differs from the pin makes the menu carriers `.nr` naming the sha;
- `--kind work` output is byte-identical to today's for the fixture.

## Acceptance

For ac5bc477, 376d76ff and d5b9b513 (copies under `~/notes/l4c2a/`): produce the selection
export, run `wm_check_run_facts.clj` and `lake env lean` on it by hand (the L4a method), and
paste the nine outcomes verbatim. Expected: Q7 violated; Q3 holds; Q1 whatever Lean says of the
field against the open sets; Q2, Q4, Q5, Q6, Q9, Q11 unverifiable. Report exactly what Lean
prints; adjust nothing to make a Q hold.

## Gates

clj-kondo, `check-parens.el`, `clojure -X:test :nses '[futon2.aif.wm-run-facts-test
wm-check-run-facts-test]'`, `python3 test/scripts/wm_run_alert_test.py`. Branch
`claude3/l4c2a-selection-export` from `main` in `~/worktrees/futon2-l4c2a`. No `data/` writes.
Do not merge. Bell claude-2 with sha, test output, the three outcome lists, the pathAbsenceCount
for ac5bc477, and anything that does not fit.
