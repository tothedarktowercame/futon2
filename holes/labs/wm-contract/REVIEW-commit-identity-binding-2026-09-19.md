# Independent review: commit-identity artifact binding

Verdict: **CHANGES-REQUESTED**

Reviewer: `codex-15`. Review job: `invoke-1789845672483-22486-44344e2a`.
Author: `codex-24`. Implementation: `190568dd4296f56189a7e61b1e427eaab381d7af`;
test-only correction: `eb48017c57e9ddea2814e8288940a03e19dbcf57`.
Subject: `wm-binding/commit-identity`.

## Finding and adequacy

Reviewed retained finding
`repair-ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-001-artifact-binding-mismatch`
in `data/wm-repair-obligations/findings/`, without mutation. Its author job
retains `:artifact-ref "05d88989"`, but its DONE line reports
`05d88989975d3f82a74c3d8a1c20822be915282a`. The binding retains that erroneous
expansion as `:text-artifact-ref`, `:text-artifact-sha nil`, `:commit nil`,
and `:disagreement? true`, against observed head
`05d8898904e0aeacf0db9fcc7078162f1ddffcf2`.

`artifact-binding-resolves-unambiguous-structured-abbreviation` reproduces
these exact three ref values at the injected resolver boundary. It asserts
canonical full-commit binding, preservation of the erroneous reported ref,
resolution through the structured job ref, and no disagreement. Read-only
Git inspection also resolved `05d88989^{commit}` to the retained observed
head and rejected the erroneous expansion. These observations support the
fixture's relevance; they are not fresh execution of the regression.

The production change resolves the claim before comparing identities,
permits a resolvable structured job-ref fallback, and excludes the
pre-dispatch head as that fallback. Freshness, ancestry, author-window and
corroboration checks remain. A resolved different commit still refuses:
`artifact-binding-distinguishes-unresolved-from-disagreement` explicitly
asserts `:artifact-binding-mismatch` and `:disagreement? true` for it.
Unresolved identity now refuses as `:artifact-ref-unresolved`, rather than
claiming disagreement between a known commit and nil. This is a meaningful
repair of the retained failure shape, by source inspection.

The requested ambiguous-refusal control is not separately demonstrated.
The same test supplies an unknown ref whose injected resolver returns nil;
that covers downstream unresolved refusal. It does not exercise an ambiguous
Git prefix or establish the production resolver's handling of that condition.
The production helper returns nil on a failed `git rev-parse ...^{commit}`,
so inspection suggests ambiguous resolution should refuse when no valid
structured claim resolves. I found no explicit ambiguity control in the
reviewed namespace. This is an evidence gap, not a claim that ambiguous refs
are accepted. Add an explicit ambiguous-resolution control, including
refusal without borrowing the observed or pre-dispatch head.

## Warrant consumption

Consumed the commissioned warrant through POST
`http://localhost:7070/api/alpha/test-registry/check`, with entry-id
`test-registry-ec965c8151fc28266b83969b063e41e38bde8d839d4b0a4955e3b9645405fc0b`,
repo-root `/home/joe/code/futon2`, and changed-paths
`src/futon2/aif/full_loop_runner.clj` and
`test/futon2/aif/full_loop_runner_test.clj`. Exact response:

```json
{"check":{"record/type":"test-registry/refusal","warrant?":false,"reason":"environment-mismatch","details":{"changed-files":["src/futon2/aif/repair_obligation.clj"],"next-action":"rerun-the-declared-namespace"}},"meaning":"validity-now, not the recorded mint verdict. Uncommitted drift usually means a lane is mid-edit; wait, do not re-dispatch."}
```

The execution receipt under `runs/commit-identity-binding-2026-09-19/`
reports a historical passing second attempt (177 tests, 947 assertions,
zero failures/errors), after the test-only expectation correction. The
registry explicitly declines validity now. I cannot call the regression
currently warranted or certify discharge from that historical result.
I did not rerun a suite. The owning execution lane must reconcile the
dependency snapshot and provide a currently valid warrant for re-review;
this review does not diagnose when or why that dependency diverged.

## Schema and recording requirements

The retained finding has `:repair/schema-version 3`, class
`:machine-failure`, machine repo `/home/joe/code/futon2`, and code-commit
artifact shape. Its contract requires distinct repair commit, independent
review, grounded repair, and a distinct production-shaped successor.

For a later approved implementation, `record-implementation!` requires a
distinct implementation attempt-id and valid commit evidence, plus reviewer
and review-job. Schema 3 additionally checks independently observed:

- `:review-evidence`: `:valid? true`, matching `:job-id` and `:reviewer`,
  `:state "done"`, `:verdict :approve`, and execution with `:executed true`
  and a positive integer `:tool-events`.
- `:artifact-binding`: matching full commit and machine repo,
  `:fresh-author? true`, `:descendant? true`, `:in-author-window? true`,
  `:corroborates? true`, and `:disagreement? false`.
- Witness with supported `:resolved?` and `:dial-moved?` values satisfying
  the contract. These are not substitutes for the observed records above.

The lane-3 worked receipt remains the recording recipe after approval.
Recording an implementation produces `:awaiting-validation`; it does not
itself resolve the finding or supply the distinct successor. This job's
CHANGES-REQUESTED verdict cannot honestly furnish `:verdict :approve`.

## Witness attestation and required follow-up

`:resolved?`: **not attested**. `:dial-moved?`: **not attested**.
The inspected fixture describes the intended bounded movement from
nil-commit/mismatch to the canonical commit with disagreement false. The
refused warrant and missing explicit ambiguity control prevent a current
regression-level attestation. No live production-successor claim is made.

Required before approval: provide a valid current registry warrant and the
explicit ambiguous-refusal control, then obtain independent re-review.
No implementation or resolution store writes, clicks, code changes, or
suite reruns were performed by this reviewer. Only this review note is
committed. The CHANGES-REQUESTED handoff goes directly to `claude-12`.
