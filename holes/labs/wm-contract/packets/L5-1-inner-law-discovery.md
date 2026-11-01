# L5-1 — Discovery: the inner selection law's code path (claude-2 → claude-3, 2026-11-01)

**Discovery only: no code changes, no `data/` writes.** Deliverable: one report at
`futon2/holes/labs/wm-contract/reconciliation/L5-1-inner-law-path-claude3-2026-11-01.md`,
committed on `main`, that file only. Bell claude-2 with its path.

## Rulings this serves (Joe, 2026-11-01 11:48Z; record:
`reconciliation/RULING-inner-selection-enacted-object-2026-11-01.md`, read it first)

1. The enacted cascade is the strict G minimiser over extensionally distinct cascades; the
   first-action marginal is retired for the inner run; `:decided-by` must say what ran.
2. (not this packet) Repeated identical-input abstention is a selection-run violation.
3. A policy excluded by Q6 before comparison stays compared; Q6 forbids choosing it.

## Questions (one section each; cite file:line for every claim; "grep suggests" where inferred)

1. **From posterior to enacted policy.** Starting at `src/futon2/aif/policy.clj:609`
   (`:per-policy-argmax`, the action marginal), trace every function between the G values
   (`SC :candidates i :g`) and the policy id written to `[:decision :chosen :id]`. Name each
   hop, its namespace, and what it decides. Include `wm/cascade_decision.clj` and
   `wm/family_selection.clj` if they are on the path, and `[:decision :selection-law
   :policy-comparison]`'s writer (the `:decided-by :G` line that L4b found untruthful).
2. **What "first action" is there.** How is a policy's first action computed from the
   cascade (which unit, which key), and what would the path look like if the enacted object
   were the whole cascade: which hop changes, which disappears.
3. **Tests that pin the marginal.** Every test (namespace + deftest) that asserts the
   action-marginal behaviour or the `:tie-break-rule :action-name-ascending`. For each, say
   whether it would need to change under ruling 1 or is already consistent with argmin.
4. **Extensionally distinct.** How `q2_construction.clj` counts `policyCount` ("extensionally
   distinct valid policies") today: by policy key, by arrangement digest, or by predicted
   outcomes? L4b row 5 found same-G variants with identical step predictions in every
   multi-policy run. State whether those variants are counted as distinct, and what identity
   (the prediction digest of L4c-1, or another) would make them one policy.
5. **Q6 exclusion before comparison.** Where the refused policy is removed (`SC :q6 :status
   :excluded`; d5b9b513, c5590f95 had 5 constructed, 4 compared). What would keep it in the
   comparison with its G and prediction while refusing to choose it, and what the record would
   then carry.
6. **Stopped kernel on the inner path.** What the inner scoring needs to score "stop": where the
   transition kernels and the C preferences live (`cascade_shape_g.clj`,
   `cascade_observation_scoring.clj` per L4b), whether a stopped policy (no units applied) has a
   defined predicted outcome distribution under the existing observation model, and the
   smallest change that gives it a G and a prediction digest comparable with the cascades'.
   Say plainly if the observation model cannot express it.
7. **Blast radius.** Which other readers consume `:per-policy-argmax`, `:action-marginal` or
   `:tie-break-rule` (grep `src/`, `scripts/`, `test/`), so L5-2 can list the readers it must
   change or exempt.

## Gates

Read-only on `data/`. Keep every claim tied to a path you read. Smallest observed sizes:
if the path is longer than ten hops, say so and stop at the point where G is consumed.
