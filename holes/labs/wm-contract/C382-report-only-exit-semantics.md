# C382 — report-only exit semantics

## Finding

At the process-output boundary, a nonempty finding population paired with a
clean exit is indistinguishable from the defect `:nonzero-finding-zero-exit`.
The acceptance lint was therefore right to flag the shape and wrong only in the
assumption that report-only could reuse the clean status.

The repair gives report-only a third outcome:

- exit 0: clean census;
- exit 1: blocking findings in ordinary mode;
- exit 2: instrument/self-test unavailable or slipped;
- exit 3: explicit report-only findings.

The workspace gate accepts exit 3 only on the two command entries declared
report-only. Their blocking negative/self-tests remain separate gate commands;
a normal command returning 3 still fails, and any report-only command returning
1 still fails. This is executable consumption of a distinct verdict, not a
path exemption or a trusted comment.

Current measurements:

- live-artifact format report: zero findings, exit 0; negative control exit 0;
- empty-subject report: two findings, exit 3; blocking self-test exit 0.

C284's historical prose still records the former clean-exit report behavior and
is correctly found by the acceptance scanner. This packet does not edit that
evidence-owned artifact; its owner should amend the historical example to the
new exit-3 vocabulary while retaining the old wording as superseded history.

## Controls

```sh
clojure -M:test -m cognitect.test-runner \
  -n preemptive-repair-lint-test \
  -n workspace-gate-fence-composition-test
```

Result: 8 tests, 25 assertions, zero failures/errors. Controls prove a genuine
finding/clean-exit shape is still detected, the exit-3 report shape is not, the
gate consumes declared exit 3, undeclared exit 3 fails, and declared report-only
does not absorb exit 1.

The current acceptance census has one remaining finding, in the evidence-owned
C284 historical note. No exemption was added.

The delivering inventory is clean:
`{:exit 0, :unknown (), :missing ()}`. Clj-kondo reports zero errors and zero
warnings on the changed Clojure sources.
