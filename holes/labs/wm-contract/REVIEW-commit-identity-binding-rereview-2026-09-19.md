# Commit-identity binding: independent re-review

Verdict: **APPROVED**

Reviewer: `codex-15`. Review job: `invoke-1789846701387-22497-88aa2425`.
Author: `codex-24`. This new review supersedes the blockers in
`REVIEW-commit-identity-binding-2026-09-19.md` at `2c0ff289`; the old review
job remains CHANGES-REQUESTED and must not be recorded as approving.

Reviewed implementation `190568dd4296f56189a7e61b1e427eaab381d7af`, earlier
test correction `eb48017c57e9ddea2814e8288940a03e19dbcf57`, ambiguity control
`d296ad02b657a52d244ce4337d7b8dc3d15e0352`, and fixture correction
`35f1c4c096a2db43ffe4937a5599cecf00a7dd01`. Reconciliation receipt:
`runs/commit-identity-binding-2026-09-19/EXECUTION.md` at `ece5ef77`.

## Evidence and adequacy

POST `/api/alpha/test-registry/check` on localhost:7070 consumed warrant
`test-registry-6f5b5b769fffd359564648029ad56ea0a69c5f82dd7b2e3e1b23f2f76876bb0e`
against `/home/joe/code/futon2`, with changed-paths
`src/futon2/aif/full_loop_runner.clj` and
`test/futon2/aif/full_loop_runner_test.clj`. The compact result of the
second check, made to capture the first untruncated, was:

```text
checked-at: 2026-09-19T19:38:52.319148380Z
warrant?: true
chain-length: 2
outside-closure: []
postcheck: {status: matched}
git-head: 35f1c4c096a2db43ffe4937a5599cecf00a7dd01
run/id: 34071459-c83b-4e20-980a-d5ff12168082
results: {tests: 178, assertions: 956, failures: 0, errors: 0,
          exit: 0, duration-ms: 184639}
```

The previous stale-warrant blocker is cleared by this current validity
check. No suites were rerun by the reviewer. The execution receipt records
passing clj-kondo and check-parens gates and honestly retains the first
ambiguity fixture's failed execution before its constants were corrected.

The retained finding is
`repair-ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-001-artifact-binding-mismatch`.
Its structured ref `05d88989`, erroneous DONE expansion
`05d88989975d3f82a74c3d8a1c20822be915282a`, and observed head
`05d8898904e0aeacf0db9fcc7078162f1ddffcf2` are represented exactly in
`artifact-binding-resolves-unambiguous-structured-abbreviation`. The
currently warranted regression establishes canonical full commit binding
through the structured ref, retains the erroneous primary claim, and
replaces nil-commit/disagreement-true with resolved binding/disagreement-false.
That positive regression uses an injected resolver; it does not claim a
production successor execution.

The new `artifact-binding-refuses-an-actually-ambiguous-abbreviation`
creates actual Git commit objects
`bd4109eaa62d7222dc82cec5ff425fdf25d4bbbb` and
`bd4109e0bfc3adec9df7828cef12e2f2febce419` in a temporary repository and
asserts their hashes. It passes their shared prefix `bd4109e` through the
production resolver, with no resolver override. It asserts
`:artifact-ref-unresolved`, retention of both reported/effective ref,
`:claim-resolution :unresolved`, and `:disagreement? false`. Observation,
ancestry and clock are controlled so the failure isolates identity
resolution. Despite an observed head and base being supplied, neither is
borrowed to turn the unresolved claim into success. This closes the
explicit ambiguity-control gap.

The existing resolved-different-commit control still asserts
`:artifact-binding-mismatch` and `:disagreement? true`. Unknown-ref refusal
also remains. Production freshness, ancestry, author-window and
corroboration checks remain intact; the amendments are test-only. No
detector weakening is needed to repair this retained identity failure.

## Bounded witness and schema-3 handoff

I attest `:resolved? true` and `:dial-moved? true` **only for the retained
artifact-binding failure shape under the currently warranted regression**:
the binding moves from nil/mismatch to the canonical commit while both
resolved disagreement and actual ambiguous resolution continue to refuse.
This attestation does not assert durable finding closure, a live machine
click, or a distinct production-shaped successor.

The retained finding has `:repair/schema-version 3`, code-commit artifact
shape, and machine repo `/home/joe/code/futon2`. Codex-24 should follow
`RECEIPT-implementation-records-typed-terminal-2026-09-19.md` to record the
implementation with full SHAs and an attempt-id distinct from the failed
attempt. Obtain own-observed `:review-evidence` for THIS completed job:
matching reviewer/job, `:valid? true`, `:state "done"`, `:verdict :approve`,
and executed evidence with positive tool-event count. Obtain own-observed
`:artifact-binding` matching the repair commit and machine repo, with
fresh-author, descendant, author-window and corroboration true and
disagreement false. Do not transcribe these required booleans as fabricated
observations. Use the witness only with the bounded meaning above.

`record-implementation!` leaves the finding awaiting validation; the
contract still requires the distinct production-shaped successor. After
recording, send claude-12 the final receipts. This reviewer performed no
store mutation, click, code modification or suite execution; only this
review note was written and committed.
