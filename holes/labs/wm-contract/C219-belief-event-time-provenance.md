# C219 — belief-event temporal provenance

Date: 2026-08-31  
Owner: `wm-verbs`

The Morning Brief event carrier is now version 2.  Items record the instant at
which the completed outcome is queued as typed `:evidence/occurred-at`; reviews
record their own instant as typed `:evidence/recorded-at`.  The projected
belief event preserves both and records one of:

- `:same-instant`
- `:occurrence-precedes-recording`
- `:occurrence-follows-recording`
- typed absence with `:predates-field` for legacy events
- typed absence with `:malformed` for a current-contract omission or invalid
  instant

`apply-morning-brief-events` recomputes that provenance at the belief uptake
boundary.  This is the required consumer: storing the fields without reading
them would reproduce the dead-`:timestamp` defect.

The legacy `:timestamp` key remains accepted by `update-entity-belief`, but its
docstring now says explicitly that it is ignored and has no freshness effect.
It is not an alias for occurrence time.

No ageing policy landed.  In particular, the control supplies an occurrence
on 2026-07-01 and a recording time on 2026-08-31, observes
`:occurrence-precedes-recording`, and verifies that `:weight` remains exactly
`1.0`.  Refusal, holding, decay, and historical QA disposition remain outside
this delivery.

Historical Morning Brief items were not rewritten.  Their projected events
are classified `{:status :absent :reason :predates-field}` rather than having
queue, review, filesystem, or tick time substituted as occurrence.

Canonical focused verification:

```sh
clojure -M:test -m cognitect.test-runner \
  -n futon2.aif.morning-brief-test \
  -n futon2.report.war-machine-test
```

The focused result is 33 tests / 165 assertions, zero failures and zero
errors.  A simultaneous full-suite attempt encountered the already-recorded
load-sensitive 30-second construction timeout in unrelated
`full-loop-runner-test` cases; those cases returned `:construction-failed`
before reaching the paths changed here.  The focused tests and workspace gate
are the bounded verdict for this delivery.
