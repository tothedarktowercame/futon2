# C223 — cascade construction timeout

Date: 2026-08-31
Owner: `wm-verbs`

The 30-second boundary is the hard process ceiling around the Python cascade
constructor in `scripts/futon2/report/cascade_lane.clj`, not a fixture waiting
on an asynchronous click.  The constructor completed in 5.961 seconds alone
during C145 and crossed its unchanged 30-second ceiling only during loaded
suite runs.  That is load-sensitive construction cost; scheduling remains
C222's concern.  This delivery does not extend the deadline.

Before C223, `sh-timed` returned `nil` for both a killed timeout and other
failure.  `cascade-policy-for` propagated that as no construction, and the
full loop reported `:construction-failed`.  It now emits a typed process result
and propagates a timeout as:

```clojure
{:outcome :timed-out
 :failure-kind :construction-timeout
 :failure-stage :construction
 :timeout-ms 30000}
```

Ordinary nonzero constructor exits retain their prior no-construction
behaviour.  The focused control runs a deliberately slow child with a 10 ms
ceiling and verifies the timeout tag and full-loop failure vocabulary.  Thus a
loaded machine and an invalid construction no longer produce the same record.

C214's asynchronous click finishing after the following fixture reset is a
different boundary in Futon3c's runner service.  It does not invoke the Python
cascade timeout and is not repaired here.

Canonical verification:

```sh
clojure -M:test -m cognitect.test-runner \
  -n futon2.report.cascade-lane-timeout-test \
  -n futon2.aif.full-loop-runner-test
```

The full futon2 suite is reported separately and honestly; no workspace gate
is part of C223 because its admission path is concurrently under C222.  The
2026-08-31 run completed under the observed load: 1,052 tests / 6,286
assertions, zero failures and zero errors.
