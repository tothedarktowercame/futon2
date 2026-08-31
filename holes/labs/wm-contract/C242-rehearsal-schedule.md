# C242 — keep the reload/click/certificate rehearsal live

Date: 2026-08-31. Owner: `wm-organization`.

## Decision

Run `futon3c.wm.chain-rehearsal-test` in the cross-repository workspace gate,
not Futon3's ordinary suite and not as a second direct action inside
`run-readiness`.

The rehearsal intentionally reads a Futon2 run record and certificate checker
plus p4ng topology artefacts. Putting it in Futon3's ordinary suite would make
that suite silently non-hermetic and let a missing sibling checkout look like a
Futon3 defect. The workspace gate already owns those sibling inputs, records
their provenance, and is a required input to `run-readiness`. Its failure is
therefore visible both at review and before an operator click.

Readiness does not launch the rehearsal separately. Doing so would add an
unbounded approximately 1 GB JVM to a command whose test work is deliberately
routed through the bounded service. It would also create two verdicts for the
same check. The existing workspace-gate result is the named readiness evidence.

## Measured cost

Canonical focused command:

```sh
clojure -M:test:test-all -i :slow -n futon3c.wm.chain-rehearsal-test
```

Measured with `/usr/bin/time` after C238:

| Run | Wall | User | System | Peak RSS | Verdict |
|---|---:|---:|---:|---:|---|
| cold | 5.54 s | 22.02 s | 0.96 s | 978,740 KiB | 1 test, 12 assertions, green |
| warm | 5.70 s | 22.51 s | 0.90 s | 993,728 KiB | 1 test, 12 assertions, green |

This is material memory use but modest wall time relative to the measured
cross-repository gate. The `:slow` tag remains because it describes the test's
explicit scheduling class; the gate opts into that class by name.

The mismatched-resource control remains inside the rehearsal and must produce a
failing certificate for the overall test to pass. Thus wiring it does not turn
the chain into an acceptance that cannot reject.

Per dispatch, the already-running C241 repository-wide gate was not duplicated.
Only the focused rehearsal was run for this delivery.
