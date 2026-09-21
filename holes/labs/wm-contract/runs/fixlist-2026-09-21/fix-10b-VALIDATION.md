# fix-10b: signed observation projection, reference replay blocked

Branch `fix/narrative-10b`, base `0ff2c6ab`, implementation `5d4c3648`.
Read the merged fix-10a repairs: auxiliary attempt files belong in `retained/`,
and a grounded runner fixture must also pass `closed-execution`. This slice
writes no attempt files and does not change the runner or continuation.

## Implemented API

`d-predecessor-task-authority/verify-observations-v2` accepts the same
`[record expected read-job]` as v1. It first invokes the unchanged `verify!`;
only admitted execution gets a new projection. New schema and authority:
`:wm/d-task-token-observations-v2`, `:d-task-token-observations-v2`.
“Signed” means Boolean polarity, not a cryptographic signature.

For every token in the bound universe it retains:

- A meaning descriptor binding qualified token, source declaration digest and
  original locator, plus its own digest. The source digest pins the complete
  captured declaration, including interpretation readings. Unknown tokens and
  duplicate target declarations refuse rather than acquiring invented meanings.
- The declared observation schedule and digest; omission remains
  `{:status :held :reason :observation-placement-not-declared}`.
- `:artifact-observation`: verified artifact SHA, explicit temporal scope,
  true/false/typed missing, raw measurement and evidence digest.
- For a non-HEAD declared revision, a separate
  `:declared-revision-observation`, retaining the original locator, its resolved
  result and digest. A historically stated task is not overwritten by its
  artifact-revision recheck. Relative refs retain their resolved evidence too;
  they are not claimed to be immutable refs.

The outer result retains execution verification, occurrence, carry identity,
revision pair and record digest. Every row says `:consumption :not-authorized`.
There is no causal claim, scheduled tick assignment, or belief update. C3/C4
measure existence/declaration presence, not whether the work is correct.
An all-negative record still fails v1's positive-execution-evidence requirement;
this slice does not relax that requirement to admit signed observations.

## V1 compatibility and consumers

The entire pre-existing production file is a byte-identical prefix of the
changed file (compared against `git show 0ff2c6ab:src/futon2/aif/d_predecessor_task_authority.clj`).
All new production definitions are appended. `claim`, `verify!`, `verify`,
`produce!`, `read-predecessor`, `complete!`, `authority` and `scope` are unchanged.
The tests compare v1's printed result before/after v2 and assert the old
`:absent #{}` / false-as-`:unknown` behavior.

Existing consumers found in futon2 source/scripts and futon3c source:

1. `full_loop_runner.clj`: capture/context/prompt binding and `complete!`;
   persists the unchanged v1 enactment/verification. Its fix-10a comparison
   continues reading raw `:after-token-evidence` from that record.
2. `token_belief_predecessor.clj`: `production-authority` calls v1
   `read-predecessor`; its admission validation and no-conditioning policy are
   unchanged. It does not call v2.
3. `policy_precision_carry.clj:advance`: indirect consumer of that v1 admission's
   `:present`, `:absent`, `:unknown` and precision family, supplied by the serving
   selection path. No new projection is fed to it.

The new API has no production consumer in this slice.

## Unmet reference-record acceptance: existing identity refusal

The raw `1789964661` D-task record contains updater `:observed false`, and the
historically retained v1 verification in its tick record contains the updater
in `:unknown`. However, **fresh v1 verification now rejects this exact record
before observation replay**, both on untouched base main and this branch:

```edn
{:status :invalid :kind :verification-input-invalid
 :detail {:close-retention/refusal :occurrence-action-drift
          :path [:occurrence :action/value-sha256]
          :expected "721dfd52def94b1d53ff22cf8e11cb40e98b9e2acca77cb699741e51182e9e2e"
          :actual "53528c495fe523feb20dd4d22f018d07d74c22124ff07a91735dc28f366c3c7f"}}
```

V2 returns `:refused`, `:observation-input-invalid`, retaining that same detail.
It does NOT expose an admitted updater row. Thus the requested real-record v2
acceptance remains blocked. No digest was replaced, no validator disabled,
and no stored assertion substituted for current execution verification.

Reproduction: `/tmp/fix-10b-reference-replay.clj`, run with
`clojure -M /tmp/fix-10b-reference-replay.clj` from both worktrees. It reads the
named record, reconstructs expected occurrence/carry/universe/declaration pins
from its dispatch, and resolves jobs from the retained author/reviewer job
snapshots. It calls v1 and, when present, v2. This is a retained-evidence replay,
not a claim of fresh Agency-owned job retrieval. Refusal occurs at the
occurrence check, before either job port is read. Outputs:
`/tmp/fix-10b-base-reference.edn`, `/tmp/fix-10b-reference.edn`.

The next separate identity task should first reproduce mint→serialize→read→
validate on this action shape and compare the actual identity bytes with the
retained hash. It must establish whether serialization or actual action drift
caused the disagreement. Do not repair this by recalculating the record's hash.
If identity serialization must change, it needs explicit versioning and a
historical verification rule, with changed-action negatives. That work is
outside the signed-observation slice.

## Validation

Own worktree `/home/joe/code/futon2-fix-10b`; separate Clojure processes, temporary
Git repositories. No live loads, clicks, next-tick consumption or production
store writes. Registry publication is the explicitly requested evidence write.

```sh
clj-kondo --lint src/futon2/aif/d_predecessor_task_authority.clj test/futon2/aif/d_predecessor_task_authority_test.clj test/futon2/aif/d_task_observations_test.clj
emacs --batch -Q -l /home/joe/code/futon4/dev/check-parens.el -f arxana-check-parens-cli -- src/futon2/aif/d_predecessor_task_authority.clj test/futon2/aif/d_predecessor_task_authority_test.clj test/futon2/aif/d_task_observations_test.clj
clojure -M:test -m cognitect.test-runner -n futon2.aif.d-task-observations-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.d-predecessor-task-authority-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.token-belief-predecessor-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.policy-precision-carry-test
```

Lint: 0 errors/warnings. Parens: OK. Tests respectively **5/43, 8/43, 3/25,
9/60** (tests/assertions), all passing. Total **25 tests / 171 assertions**.
The new tests use actual Git/C3/C4 and the full v1 execution verifier. They
cover missing file, missing declaration, missing declared revision, historical
versus artifact observations, unsupported checks, declared versus held clocks,
wrong domain/occurrence/artifact/declaration pins, forged observations,
unrelated reviewer identity, recovery refusal, and unbound token refusal.

New tests against detached base `0ff2c6ab`:

```sh
clojure -Sdeps '{:paths ["src" "resources" "." "scripts" "/home/joe/code/futon2-fix-10b/test"]}' -M:test -m cognitect.test-runner -d /home/joe/code/futon2-fix-10b/test -n futon2.aif.d-task-observations-test
```

Exit 1: `No such var: task/verify-observations-v2`. No assertion runs on base
because the API does not exist. The distinguishing new assertions are
`(is (false? (observed :missing-file)))` and
`(is (false? (observed :missing-decl)))`, while v1 keeps both in `:unknown`.

Registered new-test warrant:
`test-registry-de9d1294ddf7a69e567a7ed4769736f189b4414c26899a152d551e72a34e15e8`.
Registry `run /tmp/fix-10b-registry.edn` returned `:warrant? true`, 5 tests /
43 assertions, exit 0. A fresh registry `check /tmp/fix-10b-registry-check.edn`
also returned `:warrant? true`. It warrants the hermetic projection tests, not the
blocked historical acceptance. Registry config/result files remain under
`/tmp/fix-10b-registry*` for review.
