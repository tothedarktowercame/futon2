# C136 — retire the legacy observation vector

Date: 2026-08-31. C133's compatibility boundary was retired after its caller
census established that it protected no production caller.

## Retirement

`observation/sense->vector-legacy` was deleted. The three executable test uses
were accounted for individually:

1. The ordered-vector ABI test now constructs `observation-envelope` and calls
   `sense->vector` with both objects.
2. The annotation-health position test does the same.
3. The boundary control compares the enveloped result with the explicit
   channel-order projection, then retains the missing-envelope and
   mismatched-envelope refusal checks.

No test existed solely to preserve legacy coercion, so none remains. A source
census over `src`, `test`, `scripts`, and `checks` finds no reference to
`sense->vector-legacy`; the only remaining mentions are the dated C133 history
and its recorded census command.

## Ledger and lint

The live `:exempt-with-reason` row was removed. To keep C12's historical
population reconcilable without pointing a live disposition at deleted code,
the site moved to `:retired-sites` with `:retired-by :C136`, its reason, and its
replacement. The live summary is **9 fix-now · 1 exempt-with-reason · 7
blocked**, plus **1 retired** historical site.

Canonical absence lint fell honestly from **8 to 7**. All seven findings are
live blocked sites; the removed coercion is neither hidden by an exemption nor
left as an orphaned disposition.

## Behaviour and gates

There were zero production callers, so deletion changes no ranking, selection,
policy, actuation, or production runtime behaviour.

Focused command:

```sh
clojure -X:test :nses '[futon2.aif.observation-test preemptive-repair-lint-test]'
```

Result: 10 tests, 44 assertions, zero failures/errors. Canonical lint:
`bb -cp . checks/preemptive_absence_coercion_lint.clj` — 7 findings.

Full suites: futon2 1,042 tests / 6,224 assertions and futon3 248 / 1,518;
zero failures/errors.
