# C133 — named legacy observation-vector boundary

Date: 2026-08-31. Decision: C130 item 1 = B, with the coercing compatibility
path retained as a lint-visible exemption.

## Boundary

`observation/sense->vector-legacy` is the deprecated, numeric-only projection.
It preserves the old ABI and still substitutes `0.0` for a missing channel.
That is compatibility, not a repair.

`observation/sense->vector` is the new boundary. It requires both the numeric
observation and its exact `observation-envelope`; a missing argument fails by
arity and a non-matching envelope throws a reason-bearing exception. The
envelope is therefore an executable port requirement, not documentation.

## Caller census

Canonical census:

```sh
rg -n 'obs/sense->vector|observation/sense->vector|sense->vector-legacy' \
  src test scripts checks --glob '*.clj'
```

There are **zero production callers** in `src`, `scripts`, or `checks`. The
only executable callers are in `test/futon2/aif/observation_test.clj`: two ABI
assertions use the named legacy projection, and the boundary control exercises
the legacy/new equality, mismatched-envelope refusal, and missing-envelope
refusal. This is a closed migration population rather than an open-ended one.

## Honest lint status

The disposition changed from `:blocked` to `:exempt-with-reason`, carrying:

- `:exemption :deprecated-legacy-projection`
- `:lint-visible true`
- migration target `sense->vector with the matching observation-envelope`
- deprecation marker `:deprecated-by :C133`

The lint was generalized to retain explicitly lint-visible exemptions in its
finding population. Consequently the absence count remains **8**: seven
blocked sites plus this deprecated coercion. Renaming the path did not claim
the defect disappeared. The full disposition ledger remains exact over all 18
C12 rows: **9 fix-now · 2 exempt-with-reason · 7 blocked**.

## Behaviour and gates

No production caller changed and no ranking, selection, policy, or actuation
path consumes either WM vector function. Focused command:

```sh
clojure -X:test :nses '[futon2.aif.observation-test preemptive-repair-lint-test]'
```

Result: 10 tests, 44 assertions, zero failures/errors. Canonical lint command
`bb -cp . checks/preemptive_absence_coercion_lint.clj` reports 8 findings and
includes the legacy exemption with its migration target.

Full suites: futon2 1,042 tests / 6,224 assertions and futon3 248 / 1,518;
zero failures/errors.
