# A mixture drop 1 — codex-1, 2026-09-20

Implementation: `cee539e64766ed692e12a0956657d6fe3cc1309c`.
Registry command correction: `4b140f705876a423fab6877f80dae822a9944a5a`.
Status: implemented and locally tested; **warrant blocked, not accepted**.

Read the A programme plan, A conformance specification and
MixedTokenObservation.lean. The finite component interface is a sequence of
`{:weight rational :rates token-rate-map}` on one shared token universe.
Weights are nonnegative exact rationals summing to one. Invalid components,
weights and mismatched universes return typed refusals; even zero-weight
components must have valid rates. The sparse result uses only exact arithmetic.
The sidebyside script now consumes this function. Its 15% bad-day weight remains
explicitly declared experimental configuration, not a calibration estimate.

## Executed gates

From `/home/joe/code/futon2`:

```
clj-kondo --lint src/futon2/aif/cascade_model_manifest.clj test/futon2/aif/mixture_observation_test.clj holes/labs/wm-contract/runs/coupling-sidebyside-2026-09-18/sidebyside.clj
```

Exit 0, zero errors/warnings.

```
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- src/futon2/aif/cascade_model_manifest.clj test/futon2/aif/mixture_observation_test.clj holes/labs/wm-contract/runs/coupling-sidebyside-2026-09-18/sidebyside.clj
clojure -M:test -m cognitect.test-runner -n futon2.aif.mixture-observation-test
```

Both exit 0. Parens OK. **4 tests, 42 assertions, 0 failures/errors.**
No whole-suite run; short-lived tooling JVMs only.

The test reruns sidebyside and compares the complete output to output.txt.
Separately executed `clojure -M holes/labs/wm-contract/runs/coupling-sidebyside-2026-09-18/sidebyside.clj`
using Python subprocess capture, feeding stdout to
`diff -u holes/labs/wm-contract/runs/coupling-sidebyside-2026-09-18/output.txt -`.
**Diff exit 0, no differences, 475 stdout bytes.** Oracle file unchanged.

A2: four input states on two tokens each sum to exactly 1; every observation
probability agrees with the weighted sum of token-likelihood calls. Changing
weights from [2/3,1/3] to [1/3,1/3] returns
`:mixture-weights-not-normalized`.

A3: two judgement tokens are perfectly coupled (both reported or neither,
each with probability 1/2); one present and one absent checkable token remain
exact. Marginal is `{#{:git-present} 1}`. Negative controls independently set
false-neg on the present checkable or false-pos on the absent checkable to 1
in one component: both break that marginal, reducing its correct mass to 1/2.
Thus the exactness assertion can detect violation of either zero-rate premise.

## Registry blocker

From `/home/joe/code/futon3c`:

```
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/a-mixture-2026-09-20/registry.edn
```

First attempt refused `:explicit-namespace-required`: the registry requires
logical command `["clojure" "-M:test" "-n" namespace]`, without the direct
runner's `-m` argument. Corrected and committed the config, then retried.

Second attempt refused `:registration-failed`: `HTTP append failed: 400`,
invariant `I-evidence-per-turn`, violation kind `:shape`.
Rejected evidence ID:
`test-registry-84c2db30ea3e5ac641034515b6fa22b07947e0cd77db4978fcd733d58ae1da9f`.
**This is a rejected intent ID, NOT a warrant.** The rejected payload has
`:kind :intent`, run ID `ae69fdb0-6601-4ba6-9101-dbc4119a78e2`; registry tests
were not reached. Full exception retained by tooling at
`/tmp/clojure-3441008459590838783.edn`.

No bypass, live reload, click, calibration, declaration change, or change to
the factorized evaluator/C/D/F/Q. Owner action needed: resolve registry append
refusal and rerun the committed registry specification before acceptance.
