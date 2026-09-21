# improve-1a — prospective learning trials, record only

Branch `fix/narrative-improve-1a`, base `71e9621a`.

Close now retains `:learning-trial-receipt` both in the close judgment/result
and inside the existing manifest-admitted `retained/token-outcome.edn`. No new
attempt-top-level file is introduced. The new click-path namespace registers
with load-identity and is in its required scope. Narrative adds one compact
line per trial in the closed section only.

The explicit Beta(9,1) prior is `resources/wm/learning-trial-prior.edn`, marked
`:authority :illustrative`, `:mode :record-only`, and production theta unchanged.
It is beside the cascade-source directory, not inside it: the source loader
interprets every EDN inside that directory as a cascade source. No loader
exception or extra candidate was added. The prior value and its digest travel
with the receipt.

Each trial retains occurrence, typed missing performed-step/before-observation,
retained before evidence, initial *model* marginal (not mislabeled observation),
post measurement and Boolean/typed unknown, verification, declared placement,
guard/effect/interpretation/meaning and route digests, and deterministic
occurrence-pattern-effect-grain deduplication identity. `seen-identities` and
`previous-meanings` are explicit pure replay inputs; no mutable ledger or
production count store was added.

**All current trials are held, never admitted/countable.** The declared prior
explicitly says the execution-clock contract is not declared. Even a declared
observation tau alone cannot establish a performed firing or an observed
before-state. There is deliberately no invented admission contract. This is
slice 1, not a partial enablement of slice 2. Replaying a held receipt is
idempotent; passing its deduplication identity identifies `:duplicate-replay`.
A future learning consumer still needs a durable exactly-once ledger and the
execution/clock authority before it can count any row.

A Boolean endpoint with missing placement/clock can retain an explicitly
hypothetical `:if-counted` Beta result and two actual model rollouts, prior and
hypothetical posterior. Unknown, unselected, already-present, duplicate and
revised-meaning trials retain typed holds without a hypothetical count update.
The original comparison retains its own raw measurement evidence. No state
predicted by rollout is asserted to be an actual performed step.

The historical updater fixture is cut from run 1789964661 and its retained
D-task source. It remains held `:observation-placement-not-declared`, with
`:counted? false`, shadow theta 9/11, and terminal updater marginals 99/100
(prior) and 117/121 (if counted). Both shadows use `rollout-evaluation`.
Production ranking and selection are recomputed before/after receipt creation:
printed candidate scores and posterior are byte-identical, and all three Gs
and posteriors equal the frozen recorded values. No scoring/selection code,
production theta, or declaration meaning was changed.

## Negative baseline

The new test namespace and frozen fixture were copied into isolated detached
worktree `/home/joe/code/futon2-improve-1a-baseline`, base `71e9621a`.
The test dynamically resolves the new API so absence yields assertion failures,
not merely an uncompilable test. Exact command:

```
clojure -M:test -m cognitect.test-runner -n futon2.aif.learning-trial-test
```

Fresh base output: **3 tests / 38 assertions, 29 failures, 0 errors**. Examples:

```
FAIL in (historical-updater-is-held-not-learned)
expected: (= :observation-placement-not-declared (:reason row))
  actual: (not (= :observation-placement-not-declared nil))

expected: (= 9/11 (get-in row [:shadow :if-counted-rollout :theta]))
  actual: (not (= 9/11 nil))
```

Log `/tmp/improve-1a-base.log`. On the branch, all 38 assertions pass, including
missing-as-unknown, unselected target, already-present effect, duplicate replay,
revised meaning, invalid prior, exact rollout marginals and unchanged scoring.

## Gates and owner-run boundary

Environment: own worktree/process, OpenJDK 21.0.11, Clojure 1.11.1.
No serving-JVM changes/evals, clicks, or Lean builds.
For each namespace, ran:

```
clojure -M:test -m cognitect.test-runner -n <namespace>
```

| Namespace | Tests / assertions | Fresh result |
|---|---:|---|
| futon2.aif.learning-trial-test | 3 / 38 | pass |
| futon2.aif.run-narrative-test | 22 / 112 | pass |
| futon2.aif.load-identity-test | 3 / 12 | pass |
| futon2.aif.full-loop-runner-test | 186 / 580 | 0 failures, 88 errors, all canonical source drift |

Logs `/tmp/improve-1a-{learning,narrative,load,runner}.log`.
Runner errors all say `Serving runner source drifts from the canonical checkout`;
the guard remains enabled. The new test
`feature-card-close-retains-learning-receipt-without-changing-selection` builds
on `run-feature-card-attempt`, supplies a retained cohort, checks grounded close,
nonempty held trial rows, receipt retention, unchanged recorded posterior, and
`closed-execution`. **Its close assertions have not executed in this worktree**;
claude-3 must run this namespace on canonical main after review/merge. No green
runner claim or bypass is made.

Static commands (all four changed production and three changed test namespaces):

```sh
files=(src/futon2/aif/{learning_trial,full_loop_runner,run_narrative,load_identity}.clj test/futon2/aif/{learning_trial,run_narrative,full_loop_runner}_test.clj)
clj-kondo --lint "${files[@]}"
emacs --batch -Q -l /home/joe/code/futon4/dev/check-parens.el -f arxana-check-parens-cli -- "${files[@]}"
git diff --check
```

Fresh output: kondo 0 errors, 0 warnings (one existing informational `str`
diagnostic in the runner); parens `OK`; diff check clean.
Registry publication follows the implementation commit because the registry
requires committed scope. Its warranted command is the equivalent
`clojure -M:test -n futon2.aif.learning-trial-test`; the explicit `-m` form above
was separately run because registry command validation does not accept it.
