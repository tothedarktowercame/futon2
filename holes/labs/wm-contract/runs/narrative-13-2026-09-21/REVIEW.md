# Narrative fix-13: retain source coverage and projection units

Branch `fix/narrative-13`, based on main `e9ee4a73`. No canonical checkout
edits, live loads, WM clicks, or merges.

## Data retained

The source merge's `:mission-hole-coverage` now includes retained and projected
hole frequencies by kind, beside the existing unprojected frequencies, live
mission counts, added target count, deferred target IDs and generation reason.
`war-machine/assemble-cascade-problems` carries that exact account into assembly;
`cascade-decision` carries it onto the emitted decision, including abstentions.
The tick uses this assembler in its existing source-assembly location.

`live-c/cascade-spec` attaches `:live-c-coverage` from the projection values it
has JUST computed for scoring. It does not re-read source files or recompute
coverage from the current corpus at persistence time. Its fields distinguish:

- `:source-entries`: unit `:source-entry`, count of derived entries;
- `:source-tokens`: unit `:source-token`, total/projected/reached/unreached counts;
- `:projected-outcome-tokens`: unit `:target-qualified-outcome-token`, count and set;
- `:in-domain-outcome-tokens`: the same outcome unit, count and set after domain restriction;
- `:comparison-domain`: the comparison's complete token domain;
- `:projected-from`: exact source token → outcome set and weight mapping;
- `:unreached-source-tokens`: actual source tokens, not a subtraction of outcomes;
- source signature and projection method.

A reached source token means its projection intersects the comparison domain.
It does NOT mean a mission was completed, or that every projected outcome is
attainable by this menu. Multiple sources may share outcomes; one source may
project to several outcomes. Legacy identity projection explicitly labels its
outcome-domain values `:source-token` instead of calling them target-qualified.

Both accounts appear at the top level of the trace record and run record. The
trace also keeps them in its decision and the hole census in cascade-problems.
Run-record changes are six additive lines at the existing persistence map,
reading this run's selection decision only. No previous trace/corpus lookup.

A projection with no in-domain outcomes still records its computed counts and
mapping alongside the existing no-reachable-want refusal. An all-declined tick
retains the hole account but labels the uncomputed live projection
`:absent / :no-admitted-cascade-problems`. Caller-supplied sources without a
census and old/early-failure decisions without accounts also retain typed
absence rather than invented zeroes. If judgment fails before a decision can
be retained, persistence cannot reconstruct its transient census and says
`:coverage-not-recorded`.

## Can a fresh 113 → 5 drop be explained?

Yes: fresh emitted ticks now retain the source census, projection mappings,
comparison domain, and existing assembly/admission refusals together. Comparing
those fields identifies which source wants lost an outcome projection and
which targets disappeared at admission, without calling 113 or 5 source counts.
The previous expression “113/468 → 5/468 coverage” mixes projected outcome
tokens with live source entries and should not be used as a source fraction.
This change does not retroactively reconstruct the missing historical 441/99
census or establish that one historical commit alone caused the drop.

The new test demonstrates the explanation on two fresh fixture assemblies:
removing M-b's constructed candidate leaves the hole census unchanged, drops
reached source tokens **3 → 2**, and drops in-domain outcome tokens **4 → 3**.
The account identifies `:closed/M-b` as unreached; the assembly records M-b's
`:no-constructed-candidate` refusal. Another fixture distinguishes three
projected outcomes from just one in-domain outcome. No scoring, normalization,
admission, or choice rule changes.

## Regression and hermetic validation

The new `futon2.report.coverage-account-test` runs real mission source merge,
assembly, cascade decision, trace serialization/write/read, and direct
`persist-run-record!`. All source inventories and habit paths are injected;
trace and run files are confined to a temporary directory. It counts real
worktree `data/` files before/after and verifies pure selection creates no
files under the temporary store. This does not call `run-opportunity!`, alter
its source-authority guard, or invoke a live service.

Red baseline: before implementation the initial source/trace/persistence tests
failed on missing accounts. The final regression was also replayed with ONLY
`persist-run-record!` restored from base commit e9ee4a73 in a separate test JVM
(extract its defn from `git show`, evaluate under its namespace, run the test
namespace). `old-persist-red.log`: 5 tests / 41 assertions, 4 failures, 0 errors:

```
expected: (map? (:mission-hole-coverage record))
actual: (not (map? nil))
expected: (map? (:live-c-coverage record))
actual: (not (map? nil))
```

Fresh successful runs, one namespace at a time:

| Namespace | Tests | Assertions | Failures/errors |
|---|---:|---:|---|
| futon2.report.coverage-account-test | 5 | 41 | 0/0 |
| futon2.aif.mission-hole-wants-test | 9 | 35 | 0/0 |
| futon2.aif.live-c-test | 11 | 52 | 0/0 |
| futon2.aif.trace-test | 40 | 131 | 0/0 |
| futon2.report.cascade-decision-test | 13 | 91 | 0/0 |

Each command was `clojure -M:test -m cognitect.test-runner -n <namespace>`
from `/home/joe/code/futon2-narrative-13`. Fresh logs are beside this note.
The requested run-record persistence test HAS run and passed directly; the
broader guarded opportunity-runner tests still require the canonical checkout
and were not rerun here.

Environment: OpenJDK 21.0.11; Clojure CLI 1.12.5.1664; project Clojure 1.11.1.
Static gates (all pass, kondo 0 errors/warnings; two pre-existing info notices):

```
clj-kondo --lint src/futon2/aif/mission_hole_wants.clj src/futon2/aif/live_c.clj scripts/futon2/report/war_machine.clj src/futon2/aif/trace.clj src/futon2/aif/full_loop_runner.clj test/futon2/report/coverage_account_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/mission_hole_wants.clj src/futon2/aif/live_c.clj scripts/futon2/report/war_machine.clj src/futon2/aif/trace.clj src/futon2/aif/full_loop_runner.clj test/futon2/report/coverage_account_test.clj
git diff --check
```
