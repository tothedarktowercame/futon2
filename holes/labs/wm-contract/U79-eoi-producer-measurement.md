# U79 — M-expressions-of-interest producer branches

## Inventory and implementation

All six completion criteria now have distinct producer contracts. The inventory
names each criterion, its mission source, and every input required to compute
its boolean (`src/futon2/aif/mission_gauges.clj:246`). The common runner has
four explicit outcomes: undeclared input, unreadable input, missing/disagreeing
verdict, and measured `0.0`/`1.0`; no absence becomes a value
(`src/futon2/aif/mission_gauges.clj:284`). All six producers are registered in
the terminal gauge reading (`src/futon2/aif/mission_gauges.clj:336`).

The six C_mis gauges now name those six observables and retain U28's explanatory
input inventory (`scripts/futon2/report/war_machine.clj:2091`). This is producer
wiring, not a finding that any criterion holds.

## Runnable measurement

The production reading was run on 2026-09-08 with
`clojure -M:test -e "... (g/reading) ..."`. It returned one record for each of
the six observables. Every branch returned `:status :absent`, `:reason
:producer-input-not-declared`, with its criterion-specific `:requires`,
`:would-need`, and mission `file:line` basis. Thus the current measured inventory
is 6 runnable producers, 0 measured criterion values, and 6 undeclared producer
input sets. No produce-versus-revise disposition follows from that result.

The test runs all six production branches and checks that inventory
(`test/futon2/aif/mission_gauges_test.clj:171`). A fixture then exercises both
measured values without installing fixture paths as production authorities
(`test/futon2/aif/mission_gauges_test.clj:181`).

## Validation

`clj-kondo` reported 0 errors and 0 warnings on the two changed Clojure sources
and their tests. `check-parens.el` completed successfully on the same four
files. `futon2.aif.mission-gauges-test` passed 11 tests / 58 assertions.
`futon2.report.war-machine-test` ran 97 tests / 547 assertions with the one
pre-existing live-pin failure at `test/futon2/report/war_machine_test.clj:1749`:
the expected mission document digest is `51f6de53...`, while the current file is
`a770d000...`. That test's own contract says this is source drift, not a code
fault (`test/futon2/report/war_machine_test.clj:1715`).
