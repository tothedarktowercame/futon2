# Run participants retained

Futon2 implementation `c53b18bb`, merged to main as `06cb8290`; nil-provenance
follow-up `9f3aedb3`. Futon3c implementation `5c4f17e1`, merged to master as
`80ed103d`.

The run record now contains `:participants`, schema `:wm/run-participants-v1`.
Its `:roles` map retains author, configured reviewer, reviewer of record, repair
reviewer, and issuing caller. These are assigned roles, not claims that the
agents were invoked. Resolved runner configuration initializes the observation;
a watch on the actual reviewer atom retains subsequent repair reassignment.
The outer runner owns the snapshot so close and catch paths use the same state.
A writer reached without that observation records `:not-observed`.

Each observed actor has `{:status :present :identity ...}`; an explicitly
unfilled observed role has `{:status :absent}`. `read-role` treats missing
participant records/roles as `:not-observed`, and unknown schema versions as
`:incompatible-meaning`. It does not infer absence from an old record.

The HTTP click boundary records supplied issuer identity, or `:caller-unknown`
when caller is omitted, with source `:wm-click-http-boundary`. The same provenance
survives ordinary, RUN4, and commissioned handoff composition. It grants no
commission authority. No provenance (including explicit nil) at the writer
remains `:not-observed`.

## Controls and scope

- Repair selection uses the production reviewer-selection/reset function, real
  atom watch, and real run writer. The configured reviewer stays "configured"
  while reviewer of record becomes "repair". Replacing the latter with the former
  changes the record and fails the expected-role comparison. A separate test
  exercises the outer runner's state handoff with the core actuator stubbed.
  These tests do not execute a complete repair opportunity or dispatch agents.
- HTTP tests use the real handler, synchronous runner worker, and run writer;
  dispatch creation, budget consumption, cohort binding and close side effects
  are stubbed. Named and omitted caller cases are checked. Deliberately stripping
  provenance at the worker handoff produces `:not-observed` and fails the expected
  provenance comparison.
- Removing participants converts the explicit absent reviewer to unobserved.
  Every corruption asserts that the relevant role differs, not only that the
  records differ (whose timestamps could otherwise provide a vacuous difference).
- Existing RUN4 boundary namespace: 4 tests, 29 assertions; commissioned adapter:
  6 tests, 32 assertions. Both passed, including commissioned issuer retention.
- clj-kondo: 0 errors, 0 warnings on all changed Clojure files in both repos.
  check-parens: OK. Existing good record validity output is byte-identical:
  VALID (3/5 ok), retaining its prior C and F flags.

Registry registrations and HTTP warrant checks are retained beside this report
and in futon3c/holes/evidence/run-participants-2026-09-19.

No WM click issued. No serving reload performed. A serving reload from canonical
master (including the changed Futon2 runner dependency) is required for activation.

Final runner warrant (4 tests / 25 assertions): `test-registry-b015dbe2b9a3e5eb7b40e89f73704c17c1a0a3cca6398e9e11a866269e693eca`.
Boundary warrant (1 test / 14 assertions):
`test-registry-b1bcd8d5d44a3b1f9e6023390159a456f9fa07bcdd99f42977707e7363b44fa9`.
Both HTTP checks returned `warrant? true`.
