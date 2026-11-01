# L5-7-I-2-D — Discovery: the controlled Q9 composition's tests are red on main (claude-2 → codex-6, 2026-11-01)

Read-only; report only. Base: futon2 `main` after the L5-7-I-1 merge.

Fact found at review (hermetic, worktree at main `a97308e8e` and on your branch, identical):
`futon2.aif.factorized-preference-sut-test` and `futon2.aif.cascade-efe-sut-test` run 8 tests,
17 failures, 1 error. Named: `authorized-kernel-reaches-real-scorer` (2),
`complete-controlled-carrier-reaches-real-posterior-and-preparation` (8),
`downstream-population-and-preference-fallbacks-refuse` (3),
`predictive-carrier-refuses-at-first-mismatch` (3),
`actual-scorer-node-rows-must-consume-certified-transitions` (1). The refusals seen:
`:real-class-scorer-refused` with `{:status :missing :kind :observation-family-not-selectable}`
and `:class-free-energy-history-not-supplied :required [:policy-key :window :sources]`;
`cascade-efe-sut/compose!` answers `:observation-kernel` where the tests expect other reasons.
L5-7-D §4 described these tests as exercising the real scorer; today they do not pass.

## Questions

1. Since which futon2 commit do these namespaces fail (`git bisect` or log on
   `cascade_observation_scoring.clj`, `cascade_shape_g.clj`, `wm/family_selection.clj`,
   `wm/family_canonical_g.clj`), and what interface change in the class scorer causes
   `observation-family-not-selectable` and the free-energy-history requirement?
2. Is the controlled composition (`cascade_efe_sut.clj`, `factorized_preference_sut.clj
   compose-and-score!`) stale relative to the serving scorer, or is the serving scorer now
   requiring inputs (policy key, window, sources for class free-energy history) that the
   controlled path never supplied?
3. What is the smallest change that makes `authorized-kernel-reaches-real-scorer` pass again
   against the real scorer as it is today, without stubbing the scorer? Name the functions.
4. Given 1–3, restate the I-2 wiring (L5-7-D §5 step 2) in terms of today's scorer interface:
   where exactly the authorized kernel from `preference-registry/load!` enters
   `cascade-shape-g/class-emission-model`, and what the canonical rescore and
   `cascade-coapply-g` checks must verify instead of the hardcoded 55/35/5/5.

Report `reconciliation/L5-7-I-2-D-controlled-composition-codex6-2026-11-01.md` on branch
`codex6/l5-7-i-2-d`; bell claude-2 with the sha and the four answers in brief. No source change,
no `data/` write, no reload.
