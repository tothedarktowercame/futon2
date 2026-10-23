# M2y-4d — selected mission in the effect request

Branch `m2y4d-effect-target`, worktree `/home/joe/worktrees/futon2/m2y4d`.
Test HEAD/base `577ee17e02a91defaa618cb951877d27f20cfe51`, own JVM, cwd the
worktree. No live continuation, click, shared-JVM load or live-store write.
Owner merges; this branch is not merged into the canonical checkout.

## Contract

`resume-on-prod-cascade!` returns the target from its existing checkpoint read.
`continue-on-prod-cascade!` overrides any caller target with that returned
target before constructing production executors. The real executor builds
a five-argument effect-request whose digest includes target. Missing, empty,
or blank targets become `{:status :absent :reason :selected-target-unavailable}`.
The prompt says the target is the mission, the author must not contact the
reviewer, and the runner sends the answer to the named reviewer.

The four-argument effect-request output is unchanged, including its digest
and omission of :target. `frozen_cascade_steps.clj:119` remains on that API.
Its enclosing run-stage has the selected entry in scope, but I did not change
that offline prepared-work caller: this packet changes production continuation
transport, and the explicit four-argument compatibility assertion and existing
frozen-step tests pin the old output.

## Live projection

Source: canonical futon2
`data/wm-on-prod-history/checkpoints/2026-10-18-f016ed51-62de-426e-88c2-768fa18b00dc/attempt-001.edn`.
Source SHA256 `baee151aeac8ab20309296293ac91f6ccb64a957232a75ceb3e1eaad191d1488`.
Projection `test/resources/m2w/f016ed51-target.edn` contains exactly
`{:target "M-self-improvement-loop"}` from `[:selected-entry :action :target]`.
Projection SHA256 `ed213f09802e56cf0d7c01a5b314fa9f60e5ed2c62219bc54a66906e39e71a6f`.

The new test calls the real resume/wrapper/registry/executor/request functions,
stubbing checkpoint I/O, phase telemetry, compilation, effect iteration and
agent contacts. It asserts the author's received request and prompt, exactly
one checkpoint read, target-bound digest, typed absence, and legacy output.
The caller deliberately supplies a different target; the sealed target wins.
No agent is invoked and the close adapter is never executed by this harness.

## Red/green evidence

Before source changes:
`clojure -X:test :nses '[futon2.aif.m2y4d-effect-target-test]'`
→ exit 1; `red.log`: 3 tests, 11 assertions, 6 failures, 0 errors. The live
target assertion gets nil instead of M-self-improvement-loop; prompt and typed
absence assertions fail as well. This is the pre-change bad-case check.

After source changes:
`clojure -X:test :nses '[futon2.aif.m2y4d-effect-target-test futon2.aif.wm.cascade-continuation-test futon2.aif.m2y4a-effect-retention-test futon2.aif.continue-on-prod-run-id-test futon2.aif.wm.compiled-cascade-construction-test]'`
→ exit 0; `green.log`: 15 tests, 82 assertions, 0 failures, 0 errors.
The last namespace is the test source found by grep for frozen-cascade-steps.

`clj-kondo --lint src/futon2/aif/wm/cascade_continuation.clj src/futon2/aif/full_loop_runtime.clj test/futon2/aif/m2y4d_effect_target_test.clj`
→ 0 errors, 0 warnings (`kondo.log`).

`emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' src/futon2/aif/wm/cascade_continuation.clj src/futon2/aif/full_loop_runtime.clj test/futon2/aif/m2y4d_effect_target_test.clj`
→ OK (`parens.log`). `git diff --check`: clean.

Validated SHA256s:

- full_loop_runtime.clj: `4ffe7e4394161e6c561a0fa8deb3deea7409ba50d353394bf0af3d315f45166c`
- cascade_continuation.clj: `4a83c50b701afb408a29172cb6bd0bab396d7397c7b780e1ce62973837a04699`
- m2y4d_effect_target_test.clj: `6ef2139e8906a5efd54fc985704f75b79bd6f54b1d45773195c53480bf6bc751`
