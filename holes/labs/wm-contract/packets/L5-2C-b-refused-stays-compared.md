# L5-2C-b — Inner run: the Q6-refused policy stays compared; the refusal forbids choosing it (claude-2 → claude-3, 2026-11-01)

Base: futon2 `main` at `c22814975` (L5-2C-a/a2 merged). Authority: Joe's ruling 3 (2026-11-01, record
`reconciliation/RULING-inner-selection-enacted-object-2026-11-01.md`) and Lean Q6/Q11 at mathlib4
`221609fe66` (L5-2L: the forbidden set is the refused cascade's prediction class, derived from
`previousDecision`, the digests and `policyPrediction`; Q11's minimum is over the admissible set).
Your L5-1 report §5 is the map: removal at `previous_run.clj:249` (`exclusion`), applied at
`wm/family_selection.clj:399`, re-certification at `:402`; the four steps you listed there are
this packet.

## Behaviour

1. `previous-run/exclusion` returns `:forbidden` (the refused policy keys, with the refusal's kind,
   the previous run id and the matching input digest) instead of a reduced `:ranked`; `:refusal`
   when nothing would be left stands as today.
2. `select-over-families` passes the whole ranked list to the selector with `:forbidden` beside
   it, and does not re-certify precision: the domain did not shrink.
3. `select-action-cascades` scores everything, forms classes (L5-2C-a), and takes the minimum over
   classes whose members are not all forbidden; a class is forbidden when its prediction digest
   equals a forbidden policy's (the class grain, matching Lean Q6 as restated in L5-2L). If every
   class is forbidden, refuse by name (`:no-admissible-class`), which closes as a typed abstention
   through the existing `:repeat-refused` route.
4. The record carries every policy in `SC :candidates`, `:scoring`, `:posterior`; `SC :q6` becomes
   `{:status :forbidden :forbidden-keys […] :forbidden-classes […] :menu n :removed 0}` (rename
   `:removed`'s meaning, or drop it, so the record does not say something false); `:decided-by`
   names the admissible minimum and the forbidden class it stepped over.
5. `decision_gate` checks: the chosen class is the strict minimum among non-forbidden classes, and
   the chosen is not in a forbidden class.
6. Projector: `comparedPolicies` now includes the forbidden policy (it is compared), so
   `comparedPolicies = constructedCascades` holds on the d5b9b513 shape; `previousChoice`,
   `previousOutcome`, digests as today. Nothing else.

## Tests (one live pin each)

- The d5b9b513 shape (your L4b example: 5 constructed, 4 compared after Q6 removal): on the branch
  5 compared, the refused class forbidden, the chosen is the admissible minimum, and the hand-built
  export runs through the checker and Lean: Q6 holds, Q3 holds (no cascade without G), Q11 as Lean
  says with the real abstention still unverifiable; paste the nine outcomes.
- The e71c7380 → d5b9b513 repeat: the same-class different-key re-choice is now refused at the
  selector and would read Q6 violated if forced into the record.
- Everything forbidden → `:no-admissible-class` and the typed abstention close.
- Existing tests pinning `:excluded`/`:removed` shapes (your §3 list and the golden decisions):
  change with a stated reason; regenerate goldens and say what moved.
- Named namespaces, the inner-law namespaces, both projector namespaces, alert tests; data count
  before and after (the test JVM root now guards this, so just report it).

## Gates

kondo, parens; the `:test` alias (hermetic root); no reload; no merge. Branch
`claude3/l5-2c-b-refused-compared` from `main` in your existing worktree or a new one. Bell
claude-2 with sha, the nine outcomes for the d5b9b513 export, test counts vs main, and anything
that does not fit.
