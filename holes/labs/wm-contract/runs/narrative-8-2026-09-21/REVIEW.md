# Fix 8 — cascade habit learns from observed predicted wants

Branch `fix/narrative-8`, base `8658eaba`. Scope is the cascade-prior store;
no migration or reset of its existing counts, no live loads, no WM clicks,
and no canonical checkout edits.

`select-and-record-cascade!` now delegates to the same `cascade-decision`
without a write. `attach-habits`, the store location, prior calculation, and
selection scoring are unchanged. The trace still holds the selected decision;
the run record additionally labels `:selection-event` with `:reinforcement :none`
and `:reason :selection-is-not-outcome`. A run that never selected has a typed
absence instead of an invented selection event.

Close applies `:wm/cascade-habit-observed-want-v1` after durable close and
repair-discharge finalization. The returned result and run record retain
`:habit-reinforcement` with `:rule/id`, `:inputs`, `:reinforcement`, `:delta`, and
`:reason`. Inputs include policy identity, diagnostic outcome label, comparison
schema/status, prediction/action/wants, token comparisons, artifact SHA, and
evidence digest. One or more `:predicted-and-observed` rows yield exactly one
count for the selected policy. The rule checks action/target identity, complete
unique wanted-token coverage, probability agreement, and verdict consistency.
Invalid comparisons are typed non-reinforcements.

There is no fallback to a label: `:grounded-change` with only
`:predicted-not-observed` does not increment (the reference run's false
shared-updater observation). No receipt produces `:reinforcement :none`,
`:reason :no-outcome-comparison`, `:delta 0`, without even creating a store or
lock file. Evidence can warrant reinforcement despite a later non-grounding
failure; the outcome label is recorded but is not an input to the decision rule.

**Live consequence: until fix-10a supplies a comparison, live cascade habit
counts stop changing. This is intentional.** The separate strategic-habit
trace state is outside this fix; no claim is made that all kinds of habit in
the system stopped changing.

## Interface checked against fix-10a

The small executable interface declaration is `comparison-interface` in
`cascade_habit_reinforcement.clj`. Checked the working branch
`/home/joe/code/futon2-fix-10a`, `src/futon2/aif/token_outcome.clj`:

- receipt key on its close result: `:token-outcome-comparison`;
- `:schema :wm/token-outcome-comparison-v1`, `:status :compared`;
- `:prediction` has `:status :frozen`, `:target`, full `:action`, and `:wanted`;
- `:tokens` has target-qualified `:token`, numeric `:predicted`, boolean or typed
  missing `:observed`, and the five declared verdicts.

Runner wiring reads `(:token-outcome-comparison result-base)` at close; it
currently reads nil on this branch's base. That is the exact key fix-10a adds.
No branch code is imported into this implementation. A standalone CLI JVM
compatibility probe loaded fix-10a's actual pure producer and passed its receipts
to this rule (`compat.clj` / `compat.log`): observed true -> increment/1;
observed false -> none/0. It never connected to the shared JVM. The owner should
preserve this result key when merging the concurrent close edits.

## Writers and test adjustments

`record-reinforcement!` reuses the existing monitor, sidecar lock, and atomic
store publication. Its read purpose is `:outcome-reinforcement`; persisted
`:reinforcement-bases` labels the rule, not the old selection basis. Malformed
stored history still refuses. The legacy `record-selection!` remains available
for existing offline-history fixtures. Search of production `src/` and
`scripts/` finds no caller, only its own definition/default arity. This avoids
breaking historical fixture generators while removing all live selection writes.

The old accumulation replay expected selection to increment and used an empty
candidate excluded by the current selector. It now uses two distinct nonempty
acting candidates and explicitly feeds observed outcomes at close. It verifies
selection bytes unchanged before reinforcement, 2/4 counts after six outcomes,
and bounded storage after 106 outcomes. Distinct first actions matter: two
policies sharing the first action are one action marginal and select the first
representative, so they cannot test alternating policy outcomes.

The scoring-receipts test now invokes real persistence directly, removing its
former source-guard override. Repeated selections read the same seeded snapshot,
without a `:selection-update` read. Its stale-snapshot negative case corrupts
the receipt SHA explicitly. The existing habit fallback test now gives its
identity-less candidate an explicit acting `:type`; the current policy refuses
its former no-action fixture before reaching that assertion. Production read
and policy code were not changed.

## Red evidence and validation

`red.log` was run against the unchanged main-base selection/store implementation
before edits. The new test constructed a real assembled cascade decision and a
hermetic temporary store. It failed:

```
selection leaves the habit store byte-identical
expected: (= before (slurp path))
```

The actual store changed from `:samples 0, :counts {}` to `:samples 1` with the
chosen cascade count 1 and a selection basis. A second assertion caught creation
of the `.lock` file. New selection behavior passes both, with the returned
selected action still present.

Linux, OpenJDK 21.0.11, CLI 1.12.5.1664 / Clojure 1.11.1. Test commands, each run
from `/home/joe/code/futon2-narrative-8` as a separate namespace:

```
clojure -M:test -m cognitect.test-runner -n futon2.report.cascade-habit-selection-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.cascade-habit-reinforcement-test
clojure -M:test -m cognitect.test-runner -n futon2.report.cascade-habit-accumulation-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.scoring-input-receipts-test
clojure -M:test -m cognitect.test-runner -n futon2.report.cascade-habit-read-test
clojure -M:test -m cognitect.test-runner -n futon2.report.war-machine-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.cascade-habit-reinforcement-runner-test
clojure -M:test holes/labs/wm-contract/runs/narrative-8-2026-09-21/compat.clj
```

Passing namespace results: selection 1/3; rule/persistence 3/43;
accumulation 5/32; scoring receipts 3/44; habit read 3/14; report 89/511
(tests/assertions). Total 104 tests, 647 assertions, zero failures/errors.
Files and locks are isolated in temporary directories by the store fixtures.
All logs are retained here.

The runner-level test was written and attempted: the unchanged source-authority
guard refuses `:stale-runner-source` before the attempt (1 test, 3 assertions,
0 failures, 1 error). It must run on canonical main after merge. It checks a
reviewed/grounded close without a comparison does not mutate the habit and
persists the exact rule receipt. Positive/negative comparison receipts and the
real run-record persistence are tested directly on this branch. The broad
`full-loop-runner-test` shares that guard and was not rerun through it.

Gates on all changed Clojure files:

```
clj-kondo --lint src/futon2/aif/cascade_habit_store.clj src/futon2/aif/cascade_habit_reinforcement.clj src/futon2/aif/full_loop_runner.clj scripts/futon2/report/war_machine.clj test/futon2/report/cascade_habit_selection_test.clj test/futon2/report/cascade_habit_accumulation_test.clj test/futon2/report/cascade_habit_read_test.clj test/futon2/aif/cascade_habit_reinforcement_test.clj test/futon2/aif/cascade_habit_reinforcement_runner_test.clj test/futon2/aif/scoring_input_receipts_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/cascade_habit_store.clj src/futon2/aif/cascade_habit_reinforcement.clj src/futon2/aif/full_loop_runner.clj scripts/futon2/report/war_machine.clj test/futon2/report/cascade_habit_selection_test.clj test/futon2/report/cascade_habit_accumulation_test.clj test/futon2/report/cascade_habit_read_test.clj test/futon2/aif/cascade_habit_reinforcement_test.clj test/futon2/aif/cascade_habit_reinforcement_runner_test.clj test/futon2/aif/scoring_input_receipts_test.clj
```

Lint: zero errors/warnings (two existing informational messages). Parens: OK.
