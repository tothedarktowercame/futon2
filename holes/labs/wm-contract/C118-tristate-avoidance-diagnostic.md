# C118 — honest tri-state avoidance diagnostic

Date: 2026-08-31

## Result

`compute-controller-diagnostics` now emits `:avoidance-by-channel`, whose
entries are `:satisfied`, `:violated`, or `:unknown`. It consults the tagged
status on the exact observation object. A reason-bearing absence becomes
`:unknown`; an explicitly measured `0.0` remains observed and is tested against
the avoided range.

`:avoided-active` remains as a compatibility projection containing measured
violations only. Unknown is neither inserted there nor silently treated as
satisfied. Trace schema v19 records the added tri-state field.

The consumer audit remained unchanged before implementation: only
`war_machine.clj` transforms `:avoided-active` into reader-facing `:losses`;
no policy, selection, or actuator path reads it. `avoidance-losses` now renders
unknown entries as `:avoidance-unknown`, including their reason and the words
“observation absent.” The report heading is correspondingly “Avoidance
diagnostics,” not the falsely narrower “avoided states active.” No guard was
added or armed.

## Controls and gates

- Missing raw data produces five `:unknown` channel verdicts, zero violated
  entries, and five distinguishable `:avoidance-unknown` render records.
- A fully supplied measured-zero fixture still violates the zero-valued
  `:consulting-pct` avoided range, proving zero was not reclassified as absent.
- Targeted gate:
  `clojure -X:test :nses '[futon2.aif.free-energy-test futon2.report.war-machine-test]'`
  — 36 tests / 161 assertions, green.
- Full futon2 gate after both independent disposition movements: 1,037 tests /
  6,203 assertions, zero failures/errors. During the concurrent precision
  migration an earlier run's sole failure was the old hard-coded absence count
  (expected 15, actual 13); the final rerun demonstrates that transient ledger
  disagreement is reconciled. `bb -cp . test/preemptive_repair_lint_test.clj`
  is independently green at 4 tests / 13 assertions.
- Futon3's preceding authoritative gate remains green at 248 tests / 1,518
  assertions; C118 changes futon2 only.

## Lint movement

The two avoidance rows moved from blocked to fixed. In isolation that is
**15 → 13**. Concurrent C120 also fixed two precision rows, so the current
combined live count is **11** (`{:futon2 11, :futon3 0, :p4ng 0}`). The
disposition census is now `{:fix-now 6, :exempt-with-reason 1, :blocked 11}`.

No scoring, policy, selection, or actuation behaviour changed. Only the
diagnostic and its rendering changed.
