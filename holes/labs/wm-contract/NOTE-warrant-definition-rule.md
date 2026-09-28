# NOTE — what a warrant depends on: definitions, not loaded files

claude-8, 2026-09-28. Status: proposed rule, measured on prototypes, not yet applied.
Prototypes: futon3c `scripts/warrant_reach.py` (a6898528, f0374405);
`test-registry-runner/src/futon3c/test_registry/exercised_runner.clj` (6a448bf8 … dfae20fc).
Reports: `/home/joe/code/storage/test-registry/{exercised-d,reach-d,exercised-d2,reach-i1}/REPORT.md`.

## The request
Joe, 2026-09-28: a change should make stale only the tests it can affect. 12 is acceptable, 194 is not.

## What was measured
Under the present rule a warrant records every file loaded during the run. For the 194 wire
tests the median is 175 files, and 170 product files are each recorded by 100 or more tests.

## The rule
A warrant depends on these, and is stale when any of them differs from what was recorded:
1. The test's own files and the resources the run read, whole.
2. Every definition reachable from the test by following references in the source
   (functions, constants, macros; every method of a reached multimethod and every
   implementation of a reached protocol, in whichever file it sits).
3. Every definition the run called, including calls made while files were loading, and
   every definition reachable from those.
4. For each file with a definition in 2 or 3: the text of that file outside its definitions.
5. A file the source reading cannot analyse: whole.
6. A test that reaches a name computed at run time keeps the present whole-file rule.

The set in 2 is computed again at check time from the current source and compared with the
recorded set, so a new method or a new reference is seen. The file list stays on every warrant.

## What the rule does not see
Code built and run through `eval`; Java interop that loads code; a load-time side effect in a
file from which nothing is reached or called. The call recorder cannot record calls to
multimethods and protocol functions; the source reading covers those.

## Measured effect (source reading only, all 194 wire tests, futon3c f0374405)
182 tests narrowed, 12 keep the whole-file rule. Median 38.5 files and 483.5 definitions per test.
Of 7,243 product definitions reached by any test, 4,977 are reached by 12 or fewer tests and
92 by 100 or more; the median definition is reached by 8 tests.

| Edit made on 2026-09-28 | stale, present rule | stale, this rule |
|---|---|---|
| runner `failure-detail` | 136 | 44 |
| runner, the two historical refusals | 136 | 34 |
| flight runner `record-summary` | 122 | 36 |
| cohort `closed-execution` | 194 | 12 |
| warrant system (`currentness`) | 194 | 12 |

Item 3 was measured on eight tests only: it adds 9 to 15 definitions per test.
Every wire test calls the SQLite store at load time through `wm_wire.clj`; until the lookup
function is moved out of that file, an edit to the store's called functions reaches all 194.
