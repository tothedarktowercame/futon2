# checked-problem-map execution receipt

Worktree: `/home/joe/worktrees/futon2/checked-map-skeleton`
Baseline commit: `4f13aad76dd66c34742d17c4b8a506227b035764`
Initial implementation commit: `b090903a24060c431e38b151ff62e95767950dec`

## Final gates

```text
clj-kondo --lint src/futon2/aif/checked_problem_map.clj test/futon2/aif/checked_problem_map_test.clj
exit 0 — errors: 0, warnings: 0

emacs --batch -l /home/joe/code/futon4/dev/check-parens.el src/futon2/aif/checked_problem_map.clj test/futon2/aif/checked_problem_map_test.clj
exit 0 — OK

git diff --check
exit 0

python3 /home/joe/code/futon3c/scripts/bg.py launch "clojure -X:test :nses '[futon2.aif.checked-problem-map-test]'" --agent codex-3 --label checked-map-skeleton-followup --dir /home/joe/worktrees/futon2/checked-map-skeleton
exit 0 — job bg-1792851829957-130

python3 /home/joe/code/futon3c/scripts/bg.py tail bg-1792851829957-130 100
exit 0 — Ran 6 tests containing 46 assertions; 0 failures, 0 errors.
log: /tmp/futon3c-bg/bg-1792851829957-130.log
```

## Required source mutations

Both mutations changed the production source temporarily, then were restored
with `apply_patch`; `git diff` afterwards contains only the intended follow-up
test/receipt changes.

1. **AND → OR:** `closure-step` temporarily admitted a nonempty rule when
   *any* tail was in the closed set, while explicitly preserving the empty-tail
   case.  Focused command/job `bg-1792851793891-128` exited nonzero with **7
   failures, 0 errors**.  The failures include
   `foundation_without_adapter_unclosed`, `reduction_not_discharge`, and
   `censored_helper_admitted`.  Log:
   `/tmp/futon3c-bg/bg-1792851793891-128.log`.
2. **Claimed-closed seeding:** `update` temporarily appended
   `:claimed-closed` values as artifacts.  Focused command/job
   `bg-1792851814192-129` exited nonzero with **9 failures, 0 errors**.  The
   failures include `prediction_or_absent_unclosed` and the direct claimed
   closure control.  Log: `/tmp/futon3c-bg/bg-1792851814192-129.log`.

The earlier initial candidate run `bg-1792851498070-126` was a test-fixture
expectation error (it incorrectly expected a seeded consumer to close the
independent cycle obligation).  It is not represented as a source-mutation or
baseline semantic failure.

This receipt covers a Lean-synthetic, adapted fixture only.  The separate Lean
executable-closure equivalence gate remains pending; no test here authenticates
proofs, admits runtime records, or warrants a production caller.
