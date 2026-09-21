# Fix 11 — retain the tick's scan account

Branch: `fix/narrative-11`, based on `9635abbf`. No canonical checkout edits,
shared JVM loads, or WM clicks. Owner approved the small generation/selection
changes in addition to persistence on 2026-09-21.

The run record's stable top-level `:scan-report` refers to
`tick-run-record-<run-id>.scan.md` beside that record. Present references contain
`:status :present`, absolute `:path`, SHA-256 of the exact UTF-8 file bytes,
`:encoding :utf-8`, `:format :markdown`, and `:bytes`. No scan data or rendered
body is serialized in the run record. This is the key/path agreed with fix-14a.

The click requests deferred rendering from `generate-war-machine`. Selection
retains its render input in a per-run atom and substitutes the same
post-transform judgement used for the decision. Persistence renders this input
once. No second scan, external lookup, or history reconstruction occurs.
Default generation returns the same three keys and renders the same input map
as before. Deferred generation returns `:render-data` instead of `:markdown`.

A renderer exception yields `{:status :absent :reason :render-failed
:error {:class "java.lang.IllegalStateException" :message "renderer broke"}}`
(example), and the run record still persists. Missing input, empty/non-string
rendering, and file-write failure have distinct typed reasons. Markdown is
written through a sibling temporary file and atomically renamed. A failure
before selection yields `:scan-input-unavailable` rather than a fake scan.

## Consumer inventory

Searched `generate-war-machine|render-war-machine` across futon2 `src/`,
`scripts/`, `test/`, and `/home/joe/code/futon3c/src`.

- `full_loop_runner.clj`: production selection now requests `:defer-render? true`;
  persistence invokes the renderer through the narrow retention helper.
- `war_machine.clj/-main`: reads `:markdown`; unchanged default generation still
  provides it. The generator's own render call is conditional only on the new
  explicit option. Direct `render-war-machine` behavior is unchanged.
- `war_machine_visual.clj`: consumes `:data`/`:judgement`; unchanged.
- `run_tick_once.clj`: consumes generated `:judgement`; unchanged.
- `wm_scheduled_run.clj`: consumes generated `:judgement`; unchanged.
- `futon3c/wm/scheduler.clj`: resolves generation dynamically and calls it with
  `{}` in `refresh-one-window!`; its bundle/HTTP snapshot output is unchanged.
  No other direct generation/render call was found in futon3c `src/`.
- Existing tests: `run_tick_once_test.clj` stubs generation;
  `full_loop_runner_test.clj` stubs generation for selection;
  `selection_nil_receipt_test.clj` tests both production/render paths;
  `war_machine_test.clj` calls the renderer directly. Existing callers require
  no changes. The new namespaces test deferred generation and retention.

## Regression and validation

The new persistence test ran BEFORE modifying runner or generation, against the
unchanged main-base implementation. `red.log` records 3 tests / 18 assertions,
2 failures / 0 errors:

```
FAIL in (run-record-retains-reference-and-survives-render-failure)
expected: (= status (:status ref))
  actual: (not (= :present nil))
```

The other red assertion expected `:absent` and also got nil. Thus both successful
and throwing-renderer records lacked a reference before this fix.

Environment: Linux, OpenJDK 21.0.11, Clojure CLI 1.12.5.1664 / Clojure 1.11.1.
Commands from `/home/joe/code/futon2-narrative-11`, each namespace separately:

```
clojure -M:test -m cognitect.test-runner -n futon2.report.scan-report-test
clojure -M:test -m cognitect.test-runner -n futon2.report.selection-nil-receipt-test
clojure -M:test -m cognitect.test-runner -n futon2.report.war-machine-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.scan-report-runner-test
clj-kondo --lint src/futon2/aif/scan_report.clj src/futon2/aif/full_loop_runner.clj scripts/futon2/report/war_machine.clj test/futon2/report/scan_report_test.clj test/futon2/aif/scan_report_runner_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/scan_report.clj src/futon2/aif/full_loop_runner.clj scripts/futon2/report/war_machine.clj test/futon2/report/scan_report_test.clj test/futon2/aif/scan_report_runner_test.clj
```

Passing: scan-report 4 tests / 28 assertions; producer 2 / 11; report 89 / 511.
Lint: 0 errors, 0 warnings (two existing informational messages). Parens: OK.
Fresh logs are retained here. The scan-report tests render a supplied sentinel
mission with a non-ASCII character, independently hash file bytes, check
idempotent rewriting and typed failures, invoke actual `persist-run-record!`
for successful/throwing renderers, and exercise deferred generation with a
throwing renderer. Each test counts real `data/` files before/after; all writes
are under a temporary directory. Run-record IO is tested on this branch.

The click-level namespace was attempted, but stops before selection at the
unmodified source-authority guard: `:failure-kind :stale-runner-source`.
`runner.log`: 1 test / 4 assertions, 0 failures / 1 error. It remains for the
owner to run on merged canonical main, as explicitly authorized. It checks one
generation, defer option, post-transform sentinel, decision identity, file
location/digest, absence handling, lack of embedded scan data, and unchanged
real store file counts. The broader `full-loop-runner-test` likewise requires
canonical source and was not rerun through this known guard. No guard was
stubbed or bypassed.

## Registered warrant

Registry check returned `:missing-entry` (`registry-check.log`). After committing
implementation `73b115ac2115c937730ef7fe8a1c5a598018cf82`, ran in a separate CLI
process from `/home/joe/code/futon3c`:

```
clojure -M -m futon3c.test-registry run /home/joe/code/futon2-narrative-11/holes/labs/wm-contract/runs/narrative-11-2026-09-21/registry.edn
```

Warrant: `test-registry-3f7868218f1b5ef1333fe62a38cd752ef93a6ce6005dc303376fb3fd1266d6b1`.
`:warrant? true`, 4 tests, 28 assertions, 0 failures, 0 errors, exit 0.
The scope is the scan-report namespace (including direct run-record persistence),
not the guarded click-level namespace. Registration and content-addressed log /
closure are retained here. No source changed after the warranted commit.
