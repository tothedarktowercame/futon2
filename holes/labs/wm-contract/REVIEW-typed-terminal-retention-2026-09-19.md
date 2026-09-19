# Independent review — typed close-terminal retention

**Reviewer:** codex-23
**Review job:** `invoke-1789844336541-22473-82d2ceff`
**Author:** codex-2
**Reviewed implementation:** `06310fcb16d74c0432125f643dc203983b404576`, together with prerequisite `2c90fa4100730d92678c954a59116f191aaf366e`
**Warrant:** `test-registry-8a13ae4b965011c4794f1b8f02ee0082490f68c008ee8b88048dc2a0b725e22d`

## Verdict

**APPROVED.** The implementation types the formerly bare close exception,
retains its original error payload across containment, and pins the historical
failure as one accepted terminal and one repair finding. No acceptance gate is
weakened.

## Warrant and scope

I consumed the existing warrant through
`POST /api/alpha/test-registry/check`; I did not re-run the suite. The check
returned `:warrant? true`, run HEAD `06310fcb16d74c0432125f643dc203983b404576`,
2 tests / 21 assertions / 0 failures / 0 errors, and the following declared
scope:

- code: `src/futon2/aif/full_loop_cohort.clj`,
  `src/futon2/aif/full_loop_runner.clj`;
- tests: `test/futon2/aif/close_terminal_retention_test.clj`,
  `test/futon2/aif/full_loop_runner_test.clj`, and
  `test/futon2/aif/hermetic_repair_fixture.clj`.

The executable/test paths changed by `06310fcb` and `2c90fa41` are all in that
scope. The check reports only the registration spec
`holes/labs/wm-contract/runs/typed-terminal-retention-2026-09-19/register.edn`
as outside closure; it is the warrant declaration, not implementation or test
behavior.

## Adequacy against each retained finding

Both findings retain the exact payload
`{:errors [[:missing-judgment-key :duration-ms]
           [:missing-judgment-key :resource-use]]}` and the message
`invalid close outcome`.

### Close-exception finding

`repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-close-exception`
records `:failure-stage :close`, `:failure-kind :close-exception`, and
`:failure-outcome :build-failed`. Commit `2c90fa41` adds the two required
production close fields to both containment-cell builders. Commit `06310fcb`
adds typed ex-data at the rejecting producer:
`:failure-kind :invalid-close-outcome`, `:failure-stage :close`, and
`:outcome :build-failed`, while preserving `:errors`.

The production-shaped regression removes exactly `:duration-ms` and
`:resource-use`, observes the exact retained error vector, observes one
rejected malformed write followed by one accepted terminal, and asserts one
durable `:closed` event and one finding callback. It also asserts that the
accepted terminal retains the error vector under
`:judgment/:refusal-data/:errors`. This reproduces and covers the recorded
close failure.

### Initialization-failed finding

`repair-initialization-56485d40-2f54-4882-8917-15a59a473494-initialization-failed`
records the same message and error vector, but the historical containment
collapsed it to `:failure-kind :initialization-failed` and
`:failure-stage :initialization`. The warranted initialization-boundary test
injects the now-typed close data and asserts exactly one finding, retained
`:failure-data`, and `:invalid-close-outcome` in both the finding and returned
result. Its negative control injects the same message with empty ex-data and
still obtains `:initialization-failed`. Thus the repair recognizes typed data,
not a message-string bypass, and prevents the second generic finding shape.

Named boundary: the test does not claim that a failure before attempt creation
can have a close checkpoint. It claims, and pins, that typed close data crossing
the initialization containment boundary remains typed and singular.

## Schema-v3 recording requirements

Both findings have `:repair/schema-version 3`. A later
`record-implementation!` call therefore needs more than legacy
reviewer/job/witness labels. For each finding it must supply:

- `:reviewer "codex-23"` and
  `:review-job "invoke-1789844336541-22473-82d2ceff"`;
- `:review-evidence` whose matching job/reviewer has `:state "done"`,
  `:verdict :approve`, `:valid? true`, and positive executed tool evidence;
- an `:artifact-binding` for replacement commit
  `06310fcb16d74c0432125f643dc203983b404576` in `/home/joe/code/futon2`,
  with fresh-author, descendant, in-author-window, and corroboration true and
  disagreement false;
- a distinct implementation attempt id; and
- witness fields `:resolved? true` and `:dial-moved? true`.

This note supplies the independent adequacy judgment and witness reasoning;
the runner must still provide its independently observed review-evidence and
artifact-binding maps. This review does not itself mutate either finding or
claim the later production-shaped successor-validation limb.

## Witness attestation

- **`:resolved? true`** — within the warranted regression, the historical
  missing-field close shape is retained and accepted as one typed terminal,
  while the original malformed cell is rejected. Typed data crossing the
  initialization boundary remains typed.
- **`:dial-moved? true`** — before these commits, the refusal lost its typed
  cause and containment could emit a second generic initialization finding;
  after them, the exact error vector produces one `:invalid-close-outcome`
  finding and one accepted terminal. The same message without typed ex-data
  remains generic, demonstrating a measured classification change rather than
  a message-based escape hatch.

These attestations are bounded to the warranted regression and the two
retained historical payloads. They do not assert that the repair obligations
are resolved in the store or that a distinct production successor has already
closed them.
