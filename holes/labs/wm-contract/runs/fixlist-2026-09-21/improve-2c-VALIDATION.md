# improve-2c: aligned kernel examples (record-only)

Branch `fix/narrative-improve-2c`, base `4c202d27`. Implementation commit
`45f5fed9`. No G, C, selection, habit, observation authority or outcome-label
change; no fit is trained or installed.

## Receipt and close integration

`kernel-example/collect` runs the existing D-task v2 verifier unchanged, then
`align` binds its projection to the frozen selection-time prediction, minted
occurrence, artifact revision/repository, declaration meaning, observation
schedule, evidence identity and close outcome. It checks the prediction's action
against the occurrence and each predicted want's locator against the observation
meaning. New receipt identities use the existing canonical action-identity
encoding; old projection evidence hashes are checked in the two known printer
modes rather than rewritten. The complete v2 projection, including its occurrence
identity verification, is retained.

Every wanted token retains its marginal prediction, observed Boolean or typed
missingness, and the observation's meaning/schedule/evidence. All signed token
observations, including non-wants and pinned historical questions, remain in the
projection. Missing tokens are explicitly enumerated. Unverified execution does
not authorize raw after-token evidence as facts: failed/unavailable verification
is recorded as missing; identity mismatches refuse alignment. A grounded label
never overrides a false wanted token. Prediction unavailable is also explicit.

Each wanted token has `:attestation {:status :absent :want <qualified-token>
:reason :warrant-join-not-implemented}`. This is a reserved slot for improve-4's
future want-bound warrant id; no warrant lookup, warrant attribution or completion
claim is implemented here.

The runner writes `<attempt>/retained/kernel-example.edn` before manifest freeze,
includes its exact byte digest and admission timestamp in the close manifest,
and retains the receipt in the close judgment/result. Existing exact seven-file
attempt-directory rules are unchanged. The new namespace registers with load
identity and appears in `required-sources`.

## The 14/12 join is explicit and lossless

`resources/wm/kernel-example-domain.edn` declares all fourteen input labels under
`:attempt-close-to-disposition-v1`. Twelve map identically to the ruled twelve
flight dispositions. Both administrative labels map explicitly to typed
`:not-a-disposition` entries with their original names and the reason
`:historical-verification-is-not-fresh-production`:

- `:historical-verification-awaiting-validation`
- `:historical-verification-refused`

This follows `ruled-outcome-c/non-disposition-outcomes`; silently translating
these to `:incomplete`, for example, would invent a terminal flight disposition.
The mapping itself and its canonical digest are recorded on every example.
`join-counts` requires all fourteen labels, retains twelve disposition counts
(including zeros) **and both administrative counts**, and reports all three
totals. It never renormalizes or drops mass. Tests cover the actual current
fourteen-wide ledger fit plus a planted nonzero administrative stratum (total 6,
disposition 3, administrative 3). Removing either administrative support member
refuses. Existing live risk/constant-kernel adapters are not changed; a later
fit must explicitly account for the administrative stratum, not pass it off as
twelve-wide unconditional disposition mass.

## Acceptance and evidence

The reference `1789964661` record is replayed read-only through real occurrence,
Git/artifact, independent-review and C3/C4 checks, with retained author/reviewer
snapshots as the job lookup port. Its old tick lacks the later frozen prediction
receipt, so the test reconstructs that receipt from the retained scorer's q0,
horizon, declaration and exact enacted action. Result: admitted v2 observations,
updater **predicted 1 / observed false**, disposition **grounded-change**.
No historical record or digest is changed.

The hermetic fixture uses the real updater declaration and interpreted model in
a temporary Git repository. An unrelated reviewed commit leaves the updater
unchecked. It also includes a genuinely unavailable C5 reader to retain mixed
Boolean/missing observations. Failure attempts with no record or incomplete
execution retain all three predictions with typed missing observations. Controls
refuse wrong occurrence (including the actual verifier's expected occurrence),
artifact, meaning/locator, evidence value and prediction/action identity.

The direct runner retention helper test uses that real fixture, checks the
retained file and manifest digest, then changes the D-task source bytes and gets
`:evidence-digest-mismatch`. The existing grounded two-tick runner fixture gains
close receipt, missingness, retained byte/digest and `closed-execution` coverage.
Its synthetic artifact is not represented as an admitted D-task measurement.
Manifest assertions account for the additional retained entry; the older token
comparison test locates its own entry by name rather than relying on last-entry
position.

## Fresh gates (2026-09-21)

Own worktree/process; Ubuntu Java 21 / Clojure 1.11.1 via the project's alias.
For each namespace below the command was
`clojure -M:test -m cognitect.test-runner -n <namespace>`:

| Namespace | Tests | Assertions | Failures/errors |
|---|---:|---:|---:|
| futon2.aif.kernel-example-test | 6 | 41 | 0/0 |
| futon2.aif.load-identity-test | 3 | 12 | 0/0 |
| futon2.aif.token-outcome-test | 3 | 17 | 0/0 |
| futon2.aif.d-predecessor-task-authority-test | 8 | 43 | 0/0 |
| futon2.aif.d-task-observations-test | 6 | 53 | 0/0 |

An initially misspelled namespace selected zero tests; it is not counted above.
The correct `d-task-observations-test` was subsequently run as listed.

Direct helper test command:

```sh
clojure -M:test -e '(require (quote futon2.aif.full-loop-runner-test)) (let [r (binding [clojure.test/*report-counters* (ref clojure.test/*initial-report-counters*)] (clojure.test/test-vars [(ns-resolve (quote futon2.aif.full-loop-runner-test) (quote aligned-example-retention-binds-real-evidence-and-manifest))]) @clojure.test/*report-counters*)] (prn r) (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))'
```

Fresh output: `{:test 1, :pass 8, :fail 0, :error 0}`. It directly tests storage
and manifest construction, not the guarded attempt entrypoint.

Full runner namespace was attempted and **is not green**: 185 tests / 579
assertions / 0 failures / 87 errors. The errors are the unchanged canonical
source guard, `:failure-kind :stale-runner-source`, before attempts execute:

> Serving runner source drifts from the canonical checkout

No guard was redefined, weakened or bypassed. The requesting owner must run the
updated runner namespace on merged main (now 186 tests, including the new direct
helper test). The full runner outcome/closed-execution assertions are not claimed
as verified here.

Baseline at detached `4c202d27`, with the acceptance file copied in, same new
namespace command: exit 1:

> Could not locate futon2/aif/kernel_example__init.class, futon2/aif/kernel_example.clj or futon2/aif/kernel_example.cljc on classpath.

This is an absent-namespace failure; no assertion could evaluate on main.

All five changed Clojure files: `clj-kondo --lint <files>` returned 0 errors /
0 warnings (existing redundant-str info in the runner); `emacs -Q --batch -l
/home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' --
<files>` returned OK. `git diff --check` clean.

Scoped registry warrant:
`test-registry-4bdd59248c079ffbb2e41a1a14d0a55e03218ad09918911851d7f85781d17055`.
Run and fresh check with `clojure -M -m futon3c.test-registry run
/tmp/improve-2c-registry.edn` and `check /tmp/improve-2c-registry-check.edn`:
`:warrant? true`, `:outside-closure []`, **6 tests / 41 assertions / 0 failures /
0 errors**. Scope pins the three changed production namespaces, the declaration
and new acceptance namespace at `45f5fed9`; it does not warrant the guarded
runner tests. Registry publication is the only external evidence write; no
clicks, serving-JVM evaluations or shared-checkout edits were performed.
