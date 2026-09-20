# D exact categorical adapter — codex-1, 2026-09-20

Implementation: `fec52dcf47779a13f4462f8bd3076a6d0baf1b3e`.
Independent owner review remains required. No judge/tick wiring or WM click.

Warrant: `test-registry-2e35f48be544c3d5b2c6f58d0835a962c9780eaf64a2561e6965f03b058ffb2a`.
Subject: `D/exact-categorical-posterior`. Registry execution: 6 tests,
87 assertions, zero failures/errors, exit 0, reported duration 724 ms.

## Contract and representation

Read ExactBeliefTrajectory.lean in full and the vacuity theorems in
BeliefConditionedRollout.lean. Prediction is sum_s B(s,x)*prior(s);
posterior is A(x,o)*prediction(x)/P(o), precisely the Lean definition on
the supported finite exact-rational domain. A/B are row functions with
nonnegative normalized maps. Invalid domains are `:status :invalid`, not
mathematical refusal. Valid zero-probability observations return
`:status :refused :option :none :kind :impossible-observation`, with no
posterior. Positive observations return `:status :ok :option :some`.
No arbitrary fallback, probability floor or guessed renormalization.

The standalone synthetic wrapper consumes the existing two-component mixture
implementation; A, C, F, Q and their parameters/declarations were not edited.
Every generated control result retains prior, observation, actually consumed
rows, prediction, observation probability and (where defined) posterior.
Its `:model` map carries `:status :declared`, `:parameter-basis :synthetic`,
`:z-semantics :per-step-redraw`, `:calibration-authority :none` and exact
`:components`. This does NOT implement persistent-session latent conditions.

## Five controls

Use states empty-token-set and #{:judgement}. The noisy A mixes a perfect
observer and a half-error observer with weights 1/2 each: effective accuracy
3/4. These are declared synthetic parameters, not empirical rates.

1. Uniform non-point-mass prior, identity B, observe judgement: predicted
   state (1/2,1/2), posterior (1/4,3/4). Both sums exactly 1; all masses
   nonnegative.
2. Perfect A, point prediction at judgement, observe empty: P(o)=0 and typed
   none. The positive noisy case has P(o)=1/2 and some. An additional eight
   configurations check the iff over perfect/noisy A, uniform/point priors,
   and both observations.
3. Posterior (1/4,3/4) differs from prediction (1/2,1/2); returning the
   prediction unchanged fails the test.
4. Perfect A, uniform prediction: predicted observations equal the predicted
   state (1/2,1/2). Conditioning on judgement gives point mass (0,1), not
   (1/2,1/2). These are separate assertions.
5. Identity B, point prior at judgement, noisy A, misleading empty observation
   of probability 1/4: posterior remains (0,1). Contrast: the same point prior
   through mixing B gives uniform prediction and posterior (1/4,3/4).
   A further constant-likelihood case leaves a uniform prediction unchanged.

Additional controls reject non-normalized prior/mixture, negative B, an
out-of-carrier B destination and floating-point prior masses as invalid,
not impossible observations. An asymmetric B verifies transition orientation.
A test-only evaluation of PolicyVariationalFreeEnergy.variationalFreeEnergy
(lines 43–46) confirms F(posterior) = -ln P(o) within 1e-12 and compares
21 alternative rational beliefs on the two-state simplex. This is a numerical
fixture for the theorem, not a new proof or an implementation of the F consumer.

## Executed gates

From futon2:

```sh
clj-kondo --lint src/futon2/aif/exact_belief_adapter.clj test/futon2/aif/exact_belief_adapter_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- src/futon2/aif/exact_belief_adapter.clj test/futon2/aif/exact_belief_adapter_test.clj
clojure -M:test -m cognitect.test-runner -n futon2.aif.exact-belief-adapter-test
```

All exit 0; lint zero errors/warnings, parens OK, tests 6/87 green.
Initial registry request before commit refused `:scope-not-committed`;
no warrant was claimed for that request. After local tests and explicit-path
commit, the following ran successfully from futon3c:

```sh
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/d-exact-adapter-2026-09-20/registry.edn
```

The registry executed the namespace and issued the warrant above. No bypass
or serving reload. Registry receipts/log are in this directory. The six
runtime results in `controls.edn` were generated in a separate short-lived
tooling JVM by requiring the test namespace and evaluating its `controls`
function, not hand-transcribed.

No namespace needs a serving reload for this standalone delivery. A future
integration would require `futon2.aif.exact-belief-adapter` from the canonical
checkout; no such integration or reload is claimed here.
