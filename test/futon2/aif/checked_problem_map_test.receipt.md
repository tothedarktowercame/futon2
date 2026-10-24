# checked-problem-map execution receipt

Worktree: `/home/joe/worktrees/futon2/checked-map-skeleton`  
HEAD before commit: `4f13aad76dd66c34742d17c4b8a506227b035764`

Commands and exits:

```text
clj-kondo --lint src/futon2/aif/checked_problem_map.clj test/futon2/aif/checked_problem_map_test.clj
exit 0 — errors: 0, warnings: 0

emacs --batch -l /home/joe/code/futon4/dev/check-parens.el src/futon2/aif/checked_problem_map.clj test/futon2/aif/checked_problem_map_test.clj
exit 0 — OK

git diff --check
exit 0

python3 /home/joe/code/futon3c/scripts/bg.py tail bg-1792851521506-127 100
exit 0 — Ran 5 tests containing 31 assertions; 0 failures, 0 errors.
```

The first candidate test run (`bg-1792851498070-126`) failed one assertion:
the fixture correctly leaves the unseeded `:cyc-a` obligation open after the
unrelated consumer closes.  The assertion was corrected; the final command
above is the one green focused execution.

The test namespace contains explicit semantic mutant controls: replacing the
AND tail rule with an OR rule closes `:consumer` from `:foundation` alone, and
seeding `:claimed-closed` would close `:consumer` despite an empty checked
skeleton.  The production implementation does neither.

This receipt records structural tests over a Lean-synthetic fixture only.  The
separate Lean executable-closure equivalence gate remains pending; this run is
not proof authentication or a runtime admission warrant.
