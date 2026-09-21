# fix-10d: versioned occurrence action identity

Branch `fix/narrative-10d`, base `6faa9dab`. Implementation `4c3b4b7a`;
nonvacuous multi-token set-order control `4d53c37a`. No stored digest rewritten.

## Encoding and legacy rule

New `mint-occurrence` values use `:wm/action-transition-occurrence-v2`. The
shape otherwise remains identical. `:action/value-sha256` hashes UTF-8 bytes
of a tagged `[:wm/action-identity-v2 ...]` encoding of the **entire** action.
No fields are omitted; no current candidate adapter reconstructs old actions.
The new `action_identity.clj` owns the encoding:

- Map entries sort by canonical key bytes; set elements sort by canonical
  element bytes. Lists and vectors retain order and have distinct tags.
- Nil, Boolean, string, character, keyword and symbol have explicit tags;
  keyword/symbol namespace and name are separate values.
- Integers normalize width; exact ratios reduce; decimals normalize scale;
  finite doubles use hexadecimal IEEE value strings, including signed zero.
  UUIDs retain their string value, exact `java.util.Date` values their epoch
  milliseconds. No numeric type is silently coerced to a different numeric
  family, except an integral ratio normalizing to an integer.
- Records, metadata, Float, nonfinite doubles, lazy sequences and other objects
  refuse with `:action-identity-value-unsupported`. They are not stringified.
- The printer explicitly binds namespace maps, length, level, metadata, dup
  and readable flags. The canonical form is readable, unlimited and contains
  only the declared tagged values. Caller settings do not enter its digest.

The v1 occurrence remains an immutable value. `validate-occurrence` returns
that original value; `occurrence-identity-receipt` separately records the
encoding and matching legacy modes. Recognized modes are false (Clojure root /
observed serving digest) and true (Clojure main's binding). Both have evidence
in fix-10c. A producer/session's mode was not retained, so this implementation
does not infer it from an agent label. It checks the unchanged stored digest
against these two explicitly fixed serializers. No match refuses; if both
match, the receipt records both rather than inventing a unique producer mode.
This does not attempt arbitrary map permutations or truncate/drop fields.

D-task v1 execution verification is factored without removing any checks.
For a legacy occurrence only, it replays those checks in an action-digest-matched
printer mode. This is needed because the legacy dispatch/prompt hashes used
that same printer. If multiple modes match the action, a successful mode must
also pass **all** existing execution checks; both failing still refuses. V1's
verification result shape is unchanged. V2 observations retain the separate
`:occurrence-identity-verification`, including the actual execution print mode.

This is occurrence identity versioning, not a migration of every envelope
hash in the stack. New-v2 D-task prompt/dispatch, precision-family and other
`value-digest` contracts remain separate; this change does not claim that all
of those hashes are now printer-independent. The legacy replay pins its
identified printer internally so historical reference admission needs no
caller bindings. No observation consumption, G, habit or outcome change.

## Mint/validate/hash census

Search scope: futon2 `src`, `scripts`, `checks` (including non-Clojure scripts),
and futon3c `src`/`scripts`; searches for occurrence schemas, `mint-occurrence`,
`validate-occurrence`, `action/value-sha256`, `pr-str`, `value-digest`, and digest
helpers. Tests were separately enumerated through their retention imports.

| Site | Role |
|---|---|
| `close_retention/mint-occurrence` | Sole production writer of `:action/value-sha256`; now v2. |
| `close_retention/validate-occurrence` | Sole action-hash verifier; branches by occurrence schema. |
| `close_retention/occurrence-identity-receipt` | New read-only encoding/mode receipt; calls the validator. |
| `close_retention` retention-block validation/build | Validates nested occurrences through the same validator. |
| `full_loop_runner/mint-action-occurrence-once!` | Calls the sole mint; cohort close calls retention via the cohort writer. |
| `d_predecessor_task_authority/capture`, `verify-execution!`, `record-file` | Validate occurrence before capture, execution admission and record lookup/write. New `verify-with-identity!` also obtains the identity receipt. |
| `interpretation_evidence/identity!` | Calls occurrence validation for interpretation envelopes. |
| `scripts/interpretation_request_smoke.clj` | Smoke-fixture caller of the same mint, not another encoder. |
| `full_loop_cohort` | Builds/validates retention blocks and reads closed execution; no independent action hash. |

No separate occurrence mint or `:action/value-sha256` encoder was found in
futon3c source/scripts. `repair_obligation/validate-occurrence!` is a distinct
failure-finding occurrence contract, not this action-transition schema.

Related hashes that can contain actions/occurrences, but are **not** this
identity: `interpretation_evidence/value-digest` hashes its recursively sorted
map/vector representation with `pr-str`; D-task `prompt-binding` hashes the
dispatch, verification hashes the record, and `write-claim!` hashes literal
written record bytes. `policy_precision_carry/seal` and `model-identity` hash
families/action candidates through `value-digest`. `token_belief_predecessor`
hashes inspected trace candidate records through the same helper. The runner's
generic `sha256` hashes construction output/patterns with `pr-str`; its
`sha256-bytes` hashes literal source/evidence bytes. None writes the occurrence
field. Sorting/rendering uses of `pr-str action` are not action identity hashes.
The work-target tick's separate canonical action-list writer is also unchanged.

The WM-08 historical rehearsal used the current mint while asserting exact
September 15 bytes. Its test now supplies and validates the **original literal
v1 occurrence**, including its original digest, instead of minting a new-version
identity into a historical artifact. No rehearsal artifact was updated. The
existing grounded runner test from the repaired fix-10a fixture retains its
`closed-execution` assertion and now checks the closed occurrence is v2.

## Tests and fresh gates

All work in `/home/joe/code/futon2-fix-10d`, separate JVMs. Commands use
`clojure -M:test -m cognitect.test-runner -n futon2.aif.<name>-test`:

| Namespace name | Tests / assertions | Result |
|---|---:|---|
| action-identity | 4 / 57 | pass |
| close-retention | 5 / 18 | pass |
| d-predecessor-task-authority | 8 / 43 | pass |
| d-task-observations | 6 / 53 | pass |
| full-loop-cohort | 30 / 170 | pass |
| interpretation-evidence | 5 / 27 | pass |
| policy-precision-carry | 9 / 60 | pass |
| interpretation-request | 6 / 60 | pass |
| receipt-construction | 26 / 315 | pass |
| job-text-dispatch | 1 / 3 | pass |
| h4-production-stage | 1 / 18 | pass |
| token-belief-predecessor | 3 / 25 | pass |
| full-loop-runner | 184 / 575 | 86 source-identity-guard errors, 0 failures |
| interpretation-job | 14 / 35 | 10 source-identity-guard errors, 0 failures |
| wm08-route-a | 2 / 15 | 2 artifact byte-comparison failures, reproduced on base main |

The two source-guard groups contain only `Serving runner source drifts from the
canonical checkout`; the guard was not bypassed. WM-08 builds absolute source
paths from its checkout and checks against a canonical-checkout artifact;
unchanged base worktree `6faa9dab` produces the same two byte failures. The
owner must run these three namespaces on merged main. New untracked companion
files generated by the WM-08 test in this worktree were removed; no canonical
checkout files were touched.

Lint all nine changed Clojure files with `clj-kondo --lint`: 0 errors/warnings.
Run `/home/joe/code/futon4/dev/check-parens.el` via `emacs --batch -Q ... -f
arxana-check-parens-cli -- <same files>`: OK. `git diff --check`: clean.
Logs are `/tmp/fix-10d-<namespace-name>.log`.

The new tests cover opposite caller printers, serialize/read, hostile print
length/level/meta/dup/readable flags, changed tokens/guards/locators/receipts,
map order, and a multi-token set whose two traversals are asserted different.
They also cover type/number rules, unsupported values, the real retained
historical action/digest fixture, both evidenced v1 print modes, and semantic
schema growth. Changed content refuses under the old identity; minting that
content gives a new identity. Legacy D-task tests retain all execution gates
and verify both synthetic writer modes under the opposite reader mode.

## Actual failure on base main

Copied only the new `close_retention_test.clj` into the detached baseline
worktree `/home/joe/code/futon2-fix-10d-baseline` at `6faa9dab`; production code
there was unchanged. Ran:

```sh
clojure -M:test -m cognitect.test-runner -n futon2.aif.close-retention-test
```

**5 tests / 18 assertions; 2 failures, 0 errors; exit 1.** Exact failure:

```clojure
expected: (nil? (binding [*print-namespace-maps* true]
                 (refusal (fn* [] (close/validate-occurrence reread)))))
actual: (not (nil? :occurrence-action-drift))
```

The second assertion expected the v2 schema and received v1. On this branch
both pass. Baseline log `/tmp/fix-10d-baseline.log`.

## Reference acceptance: passed without flag games

Ran `clojure -M /tmp/fix-10b-reference-replay.clj` from this worktree, without
caller bindings. It reads the original D-task record, reconstructs expected
identity/pins from dispatch, and uses the retained author/reviewer snapshots
as the read-job port (a retained-evidence replay, not fresh Agency job fetching).
All original Git, artifact and C3/C4 checks run. Summary retained beside this
note in `fix-10d-reference-result.edn`:

```edn
{:v1-status :admitted :v1-updater-unknown true
 :v2-status :admitted :v2-updater-observed false}
```

The identity receipt records v1, exact stored digest `721dfd52…`, matching modes
`[false]` and `:execution-print-namespace-maps false`. No stored action, identity,
digest, job text or declaration was rewritten.

## Warrants

- Identity: `test-registry-b8c1a3d40dc133c5792b1b02deade5d5102ce0f3e26609cbc69c265267227368` — 4 tests / 57 assertions, exit 0.
- D-task observations: `test-registry-0c0f66b78c5a6a9c3ec295e6297b4ca56f7c4b331be5fd132bc4b4b3a0b0835a` — 6 tests / 53 assertions, exit 0.

Both registry runs and fresh checks against their declared changed-path scopes
returned `:warrant? true`. Configs and results are
`/tmp/fix-10d-{identity,observations}-registry*.edn`; checks use
`clojure -M -m futon3c.test-registry check <config>` from futon3c. These warrant
the named tests, not the guarded runner tests or the historical rehearsal's
path-dependent byte comparisons. Registry writes were the requested evidence
publication; no WM clicks or shared-JVM loads occurred.
