# L5-4d-5 — Producer and projector: same-prediction same-G cascades are one policy on the outer path too (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` at `3c6add76d`. Authority: Joe's ruling 1 as accepted (2026-11-01, record
`reconciliation/RULING-inner-selection-enacted-object-2026-11-01.md`): policies in a generative
model are distinguished by what they predict; same-prediction variants are duplicates of one
policy, not separate policies. L5-2C-a2 applied it on the inner path with the class key
= prediction digest + G units. Your L5-4d-4 replay shows the outer path needs the same rule: on the
11-01 field `only-what-can-start-now` and `giraffe-order` pick `M-pattern-retrieval-calibration`
with identical prediction and G 2.768527879893126, strictly below abstention 4.787491742782046, and
the selector refuses `:tie-at-minimum` between two arrangements of one policy.

## Behaviour (`outer_select.clj`)

1. After scoring every menu cascade, group them into classes by `[prediction-digest g-units]`
   where the prediction digest is `action-identity/digest` of the cascade's recorded `:prediction`
   `:outcomes` (exact; sort keys) and g-units is G at scale 10^6 (reuse the inner path's shared
   `g-units` and digest functions in `cascade_selection` if they are importable without dragging the
   inner namespace in; otherwise mirror them and say so).
2. The minimum is over classes. `:tie-at-minimum` refuses only when two or more *classes* share the
   minimal G. A class with members of different G cannot exist by construction; assert it.
3. The chosen class's representative arrangement is its first member in menu order, recorded as
   such: `:classes [{:id <class digest = sha256 of [prediction-digest g-units]> :members [cascade
   ids] :representative <id> :pick :G :terms :prediction}]`, `:chosen-class <id>`,
   `:representative-rule :first-in-menu-order-within-one-prediction-class`. `:cascades` keeps every
   arrangement's row; `:cascade-id`/`:cascade-index` remain the representative's.
4. The abstention floor and `pick-not-below-abstention` are unchanged (class G strictly below).
5. `:construction :policy-count` becomes the number of classes on this field (Q2 counts
   "extensionally distinct valid policies"); `:construction :cascades` keeps every arrangement with
   its `:class`.

## Projector (`scripts/wm_run_facts.clj`, selection export)

`comparedPolicies` = `constructedCascades` = `targetConstruction.policies` = the class ids;
`policyCount` = classes; `policyG` and `policyPrediction` keyed by class id (one row each);
`currentChoice.cascade` = the chosen class id. Keep a `"classMembers"` fact listing each class's
arrangement ids for the report card. Old records: unchanged fallback.

## Tests (one live pin each)

- The 11-01 retained observation with the live context (your `~/notes/l5-4d-4/` replay script,
  turned into a test fixture under `test/fixtures/` if small enough; else the smallest fixture that
  reproduces two cascades picking the same task): two classes, chosen class G 2768528/10^6 with two
  members, no refusal; the chosen pick is `M-pattern-retrieval-calibration`.
- Two cascades with different picks at equal G still refuse `:tie-at-minimum`.
- Projector: policyG has one row per class; comparedPolicies = constructedCascades.
- Run the checker and Lean by hand on the produced export (record copy of ac5bc477 carrying the new
  section) and paste the nine selection-run outcomes.

## Gates

kondo, parens, 12 outer + 2 projector namespaces vs `main`, data counts, overrides to scratch, no
reload, no merge. Branch `codex11/l5-4d-5-outer-classes` in `~/worktrees/futon2-l5-4d-5`. Bell
claude-2 with sha, the nine outcomes, the class record, test counts vs main, data counts.
