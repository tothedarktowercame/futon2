# C120 — versioned prediction-error producer contract

Date: 2026-08-31

The two sites were `precision.clj:166-167` and the former line 206.
`update-precision-state` converted a missing `:error` or `:observed` inside a
present error record to `0.0`; `weighted-error` converted a missing `:error`
to `0.0` and left missing per-call `:precision` as untyped `nil`.  Consequently
an old error-only record and a damaged current record had the same provenance.

Current `free-energy/compute-prediction-error` now stamps
`:producer-contract :prediction-error/v1`. Classification is per record, not
by date:

- an unstamped record missing a field is `{:status :absent :reason
  :legacy-era}`;
- a `:prediction-error/v1` record missing a required field is `{:status
  :absent :reason :malformed}`;
- an explicit numeric zero is `{:status :present :value 0.0}`.

`update-precision-state` retains the error and observation classifications at
`:input-status`. `weighted-error` retains the source-precision classification
at `:per-call-precision-status`. The compatibility arithmetic is deliberately
unchanged: both absent variants still use the prior numeric behavior while
making its provenance inspectable. This delivery changes neither scoring nor
selection.

There is no safe timestamp boundary to infer. The prediction-error producer
and adaptive precision consumer first landed together in commit `28ce486` on
2026-05-25, and historical callers also constructed partial maps directly.
Presence of the producer-contract tag is therefore the only per-record claim
that the current required fields apply; missing timestamps cannot enter a
permissive arm by accident.

The producer tag and typed statuses survive trace EDN serialization. Focused
precision, free-energy, and trace tests cover present zero, legacy absence,
malformed absence, unchanged arithmetic, and round-trip preservation.

The absence lint moved from 15 blocked findings to 13. It remains exit 1 by
design because thirteen separately owned coercions remain.

Canonical gates:

```sh
clojure -M:test -m cognitect.test-runner \
  -n futon2.aif.precision-test -n futon2.aif.free-energy-test \
  -n futon2.aif.trace-test
bb -cp . checks/preemptive_absence_coercion_lint.clj
clj-kondo --lint src/futon2/aif/precision.clj \
  src/futon2/aif/free_energy.clj test/futon2/aif/precision_test.clj
```
