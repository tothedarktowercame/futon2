# Learning co-apply shape execution receipt

Date: 2026-10-24 UTC  
Branch: `learning-coapply-shape`  
Worktree: `/home/joe/worktrees/futon2/learning-coapply-shape`  
Base: `e6c9c4b25e037d90f563a7a3009ef34748c52fd2`  
Environment: isolated short-lived Clojure processes; no serving JVM, click, reload, graph, or live-store access.

Fixture: `test/resources/m2w/pattern-prediction-90ca.edn`, SHA-256 `894ef46746d877abbcc7f3156f494c5006e30e0c9e58274ca362ff3857872d0f`.

## Red

Command, before production edits:

```sh
clojure -X:test :nses '[futon2.aif.learning-coapply-shape-test]'
```

Exit 1. Tail:

```text
Ran 4 tests containing 16 assertions.
7 failures, 0 errors.
Execution error (ExceptionInfo) at cognitect.test-runner.api/test (api.clj:30).
Test failures or errors occurred.
```

The failures pinned: zero instead of 27 co-apply trial candidates; wanted effects absent; shadow rollouts lost simultaneous-frontier behavior; malformed co-apply returned untyped `:prediction-input-unavailable`.

## Green

Command after the patch:

```sh
clojure -X:test :nses '[futon2.aif.learning-trial-test futon2.aif.attempt-learning-test futon2.aif.learning-coapply-shape-test]'
```

Exit 0. Full tail:

```text
Testing futon2.aif.attempt-learning-test
Testing futon2.aif.learning-coapply-shape-test
Testing futon2.aif.learning-trial-test

Ran 10 tests containing 87 assertions.
0 failures, 0 errors.
```

## Static gates

```sh
clj-kondo --lint src/futon2/aif/learning_trial.clj \
  src/futon2/aif/attempt_learning.clj \
  test/futon2/aif/learning_coapply_shape_test.clj
```

Exit 0: 0 errors, one pre-existing `attempt_learning.clj:16:3` misplaced-docstring warning. The same warning is present on canonical main before this patch; no new warning.

```sh
emacs --batch -l /home/joe/code/futon4/dev/check-parens.el \
  src/futon2/aif/learning_trial.clj \
  src/futon2/aif/attempt_learning.clj \
  test/futon2/aif/learning_coapply_shape_test.clj
```

Exit 0: `OK`.

```sh
git diff --check
```

Exit 0, no output.

Pre-commit file hashes:

```text
457071f3c077ed8944c6a470ed3ba02fc825a31742be67077eaba562d2edd020  src/futon2/aif/learning_trial.clj
29d9a1c723f0e265af4f0b23f1c36e2b6e4bcd16e170a864652852a56e7ee18c  src/futon2/aif/attempt_learning.clj
7380bd9bac8f90d09d0a2d65a27af7ba1102f2a420f21ad656aceb668eb37c02  test/futon2/aif/learning_coapply_shape_test.clj
```

## Scope check

- Co-apply patterns are enumerated in `:units` order and all declared outputs are retained.
- Rollout and counterfactual shadow keep the co-apply map, descent, units, and pattern map; only the matching pattern value's theta changes.
- Legacy vector effect ordering and behavior remain covered.
- Missing measurements remain held; the fixture produces no admitted/countable row and no ledger is opened.
- Malformed co-apply is typed at the receipt boundary.
- Contract gate/default, priors, horizon, declaration membership, theta eligibility, observation authority, and fire semantics are unchanged.

One diagnostic command was invoked incorrectly as `clojure -M:test -e ...`; because the alias supplies the test runner, it began the full suite. It was terminated (`kill 2469238`) once detected and is not counted as evidence or a gate.
