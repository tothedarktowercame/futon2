# C145 — daily-scan frame read boundary

Date: 2026-08-31

`scan-frames` now reads persisted frames with `clojure.edn/read-string`. A
failed parse becomes `{:unreadable path :cause message}` and remains in the
supplied population instead of becoming `nil` and disappearing through
`filterv`.

The report distinguishes:

- `:frames-count` — every supplied `.edn` file;
- `:readable-frames-count`;
- `:unreadable-frames-count`;
- `:unreadable-frames` — the typed path/cause records;
- `:daily-scan-count` — readable daily-scan frames used by the numeric trend.

The control supplies two valid frames and one incomplete EDN form. The result
is 2 readable of 3, one named unreadable record, two daily scans, and the last
valid frame remains the numeric consumer's latest frame.

The portability lint falls from 12 to 11 findings. The remaining five boundary
groups fail loudly on parse; none has an exception-to-absence plus filtering
path like this site. They are not migrated in C145.

## Load-sensitive suite finding

During C140, one full-suite invocation let a mocked construction call run for
30,030 ms against a 30-second timeout. The test then reported
`:construction-failed` instead of reaching the assertion it was designed to
exercise. The exact test passed alone (5,961 ms), and the subsequent full suite
passed. That first invocation used `clojure -T:build ci` directly, so it had no
C91 bounded-wrapper resource receipt tying CPU/thread pressure to the verdict.
This is an instrumentation gap: a load-sensitive failure can still reach CI
without the resource event needed to classify it. C145 records the gap but does
not alter timeout or wrapper policy.

## Canonical invocations

```sh
clojure -M:test -m cognitect.test-runner \
  -v futon2.report.war-machine-test/scan-frames-retains-unreadable-population-members-test
bb checks/reader_portability_lint.bb # expected exit 1, findings=11
clojure -T:build ci
cd /home/joe/code/futon3 && clojure -X:test
```
