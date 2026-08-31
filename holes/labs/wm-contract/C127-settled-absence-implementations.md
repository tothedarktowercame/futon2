# C127 — settled absence implementations

Date: 2026-08-31. Authority: C98, C120, and the C126 decision register.

## Delivered: three support-aware diagnostic rows

`free_energy.clj` now reads each preference and epistemic channel through a
reason-bearing `:present` / `:absent` boundary. An absent channel contributes
no pragmatic gap and no epistemic term. `:score-support` records the present
set and the absent reasons for both components; `:epistemic-terms` makes the
terms themselves inspectable. A measured zero remains `:present` evidence.

This discharges the three C98-settled rows formerly located at
`free_energy.clj:23`, `:50`, and `:61-63`. The control
`absent-controller-input-contributes-no-score-test` demonstrates absent input,
measured zero, and the scalar decomposition.

## Ranking boundary: deliberately unchanged

Caller tracing found that `compute-controller-diagnostics` is also called by
`efe/compute-efe`, where its per-channel gaps enter action ranking. The
support-aware form is now the diagnostic default, but the live EFE caller
explicitly requests `{:support-aware? false}`. Therefore this delivery changes
diagnostic evidence, not the scores selection compares. The existing R9 EFE
stress and regulator ranking controls pass. No ranking, policy, selection, or
actuation behaviour changed.

That explicit compatibility call is the unswitched C98 boundary; it is not a
claim that fabricated-zero comparison is correct. Removing it remains the
separately authorised selection migration measured by C108.

## Refused as implementations in this pass

The prediction-measurement row (`free_energy.clj:98-100`) feeds the live
prediction-error → precision → belief-update loop in
`war_machine.clj:4345-4370`. The belief-aggregation row
(`belief.clj:1040-1052`) drives that same belief update. Omitting absent terms
there can change the next belief and therefore later rankings. Implementing
either while also asserting “no ranking changed” would be false. They remain
visible and blocked; `fulab` remains excluded by C126's explicit decision.

This is the count gap C127 asked to expose: five rows looked implementation-
only in isolation, but only three remain implementation-only after tracing
their consumers to the selection boundary.

## Gates

- Before: absence lint 11.
- After: `bb -cp . checks/preemptive_absence_coercion_lint.clj` reports 8
  (`{:futon2 8, :futon3 0, :p4ng 0}`), and the disposition guard covers all 18
  C12 rows as `{:fix-now 9, :exempt-with-reason 1, :blocked 8}`.
- Focused command:
  `clojure -X:test :nses '[futon2.aif.free-energy-test futon2.aif.r9-named-validation-test futon2.report.wm-regulator-sweep-test preemptive-repair-lint-test]'`
  — 30 tests, 99 assertions, zero failures/errors.
- Full-suite commands: `clojure -X:test` in futon2 and futon3.
  futon2: 1,038 tests / 6,212 assertions; futon3: 248 tests / 1,518
  assertions; zero failures/errors in both.
