# Commit identity binding — execution receipt

Date: 2026-09-19 UTC

## Scope and decision

Implementation commit: `190568dd4296f56189a7e61b1e427eaab381d7af`

Corrective test-only commit: `eb48017c`

Registry specification commit: `058d7b0f840ba1c2845d9ff6cf4b4e0b5b66d34f`

The binding seam resolves an unambiguous abbreviated commit reference through
the repository and records the full commit plus `:resolved-from`.  For the
historical `9b6ce3a4` shape, the malformed long DONE claim does not mask the
structured job artifact ref `05d88989`; that ref resolves to
`05d8898904e0aeacf0db9fcc7078162f1ddffcf2` with
`:resolved-from :job-artifact-ref`.  An unknown or ambiguous commit-shaped ref
refuses as `:artifact-ref-unresolved`, retains the reported ref, and does not
claim `:disagreement? true`.  A different resolved commit still refuses as an
artifact-binding mismatch.  Freshness, ancestry, author-window, and repository
checks remain in force.

No repair-store mutation, machine click, or serving-JVM contact occurred.

## Static gates

Executed after the final correction:

```text
clj-kondo --lint src/futon2/aif/full_loop_runner.clj test/futon2/aif/full_loop_runner_test.clj
```

PASS: 0 errors, 0 warnings (one informational pre-existing `str` finding).

```text
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/full_loop_runner.clj test/futon2/aif/full_loop_runner_test.clj
```

PASS: `OK`.

```text
git diff --check
```

PASS.

## Registered fresh-JVM attempts

Command (each attempt used this same committed registration specification):

```text
cd /home/joe/code/futon3c
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/commit-identity-binding-2026-09-19/register.edn
```

Attempt 1 was retained honestly and failed: evidence id
`test-registry-a2e59bcae636564d9115b7239cfb8b47c28f65d7ec468ca22d7040ba58ced1eb`,
177 tests, 947 assertions, 1 failure, 0 errors.  The sole failure was a stale
test expectation that an unresolved SHA would use the former disagreement
message.  Production behavior correctly returned `:artifact-ref-unresolved`.

After commit `eb48017c` corrected only that expectation, attempt 2 passed:

```text
evidence-id test-registry-ec965c8151fc28266b83969b063e41e38bde8d839d4b0a4955e3b9645405fc0b
warrant? true
results {:assertions 947, :duration-ms 184937, :errors 0, :exit 0, :failures 0, :tests 177}
bound wm-binding/commit-identity -> test-registry-ec965c8151fc28266b83969b063e41e38bde8d839d4b0a4955e3b9645405fc0b
```

Registry artifacts under
`/home/joe/code/storage/test-registry/wm-binding-commit-identity-2026-09-19/`:

- failed closure `41886e40-eedb-4036-98a1-075ab3121d9b.closure.edn` — SHA-256 `734a04659409e808dbc9861e95580f2fd81a01d7d57dfd5a08309f79e035d290`
- failed log `41886e40-eedb-4036-98a1-075ab3121d9b.log` — SHA-256 `71bf8dff27703c89d3ece824e701dfd0fb16578faaa158e769cce8b00164c3a4`
- passing closure `a471eed0-017d-4714-9dc1-1c55f1510152.closure.edn` — SHA-256 `734a04659409e808dbc9861e95580f2fd81a01d7d57dfd5a08309f79e035d290`
- passing log `a471eed0-017d-4714-9dc1-1c55f1510152.log` — SHA-256 `5e28a045afbc35a542aaae6684137f0096c6b3d66b88f641621822e215d6c38a`

## Independent-review reconciliation

Codex-15 requested an explicit ambiguous-abbreviation control and a warrant
refreshed after parallel changes to `repair_obligation.clj`.  Commit
`d296ad02` added a fixture repository containing two real commit objects with
the same seven-hex prefix and exercised the production Git resolver.  The
first registered attempt retained this honest fixture error:

```text
evidence-id test-registry-d7104d8184b45c23e038b24fea5077a088659f86df16106a13f999055055fdf1
warrant? false
results {:assertions 956, :duration-ms 198309, :errors 0, :exit 1, :failures 3, :tests 178}
```

The fixture had mistyped Git's empty-tree SHA, so its commit objects were not
written.  Commit `35f1c4c0` corrected only the fixture constants using the
actual empty tree and the deterministic colliding commits
`bd4109eaa62d7222dc82cec5ff425fdf25d4bbbb` and
`bd4109e0bfc3adec9df7828cef12e2f2febce419`.  The real prefix `bd4109e`
refuses as `:artifact-ref-unresolved`, retains the reported ref, and records
`:disagreement? false`.

Static gates after the correction again passed: clj-kondo 0 errors/0
warnings, check-parens `OK`, and `git diff --check` clean.

The corrective fresh registration passed and rebound the subject:

```text
evidence-id test-registry-6f5b5b769fffd359564648029ad56ea0a69c5f82dd7b2e3e1b23f2f76876bb0e
warrant? true
results {:assertions 956, :duration-ms 184639, :errors 0, :exit 0, :failures 0, :tests 178}
bound wm-binding/commit-identity -> test-registry-6f5b5b769fffd359564648029ad56ea0a69c5f82dd7b2e3e1b23f2f76876bb0e
```

Passing registry artifacts:

- `34071459-c83b-4e20-980a-d5ff12168082.closure.edn` — SHA-256 `734a04659409e808dbc9861e95580f2fd81a01d7d57dfd5a08309f79e035d290`
- `34071459-c83b-4e20-980a-d5ff12168082.log` — SHA-256 `0d146d99c50cb1464c10d881c1ce0a59adaaf6ff0df114b878eb19d0fe1b183a`
