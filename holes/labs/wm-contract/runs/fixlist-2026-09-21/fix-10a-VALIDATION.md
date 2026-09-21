# fix-10a validation

Branch `fix/narrative-10a`, base `1d0ad46c`; implementation `cab63449`.
Own process/worktree `/home/joe/code/futon2-fix-10a`. No shared checkout edits,
live loads, WM clicks, or gate bypasses. Registry registration/check is the
only external write in this validation workflow.

Selection freezes the actual scorer model's q0/horizon and uses the existing
model rollout to obtain terminal marginals for every target want. Intended
pattern outputs are separate. The verdict rule is explicitly
`:positive-marginal-support` (p > 0), not a hidden 0.5 threshold; exact p is
retained, including fractional predictions. This is prediction support, not
certainty or accepted completion.

D-task completion now precedes manifest assembly. The runner checks the
D-task source byte digest, retains its raw measurements and source/verification
references, and admits `token-outcome.edn` beside the checkpoints. The close
judgment also retains the comparison. A missing prediction gets a typed
refusal; absent, ambiguous, wrong-artifact or typed-missing measurements cannot
become false observations. Grounded outcome, G, belief and habit are unchanged.
The comparison file is admitted explicitly, without weakening the separate
`evidence/` directory's schema checks.

## Fresh gates

From the worktree:

```sh
clj-kondo --lint src/futon2/aif/token_outcome.clj src/futon2/aif/full_loop_runner.clj test/futon2/aif/token_outcome_test.clj test/futon2/aif/full_loop_runner_test.clj
emacs --batch -Q -l /home/joe/code/futon4/dev/check-parens.el -f arxana-check-parens-cli -- src/futon2/aif/token_outcome.clj src/futon2/aif/full_loop_runner.clj test/futon2/aif/token_outcome_test.clj test/futon2/aif/full_loop_runner_test.clj
clojure -M:test -m cognitect.test-runner -n futon2.aif.token-outcome-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.full-loop-runner-test
```

- clj-kondo: 0 errors, 0 warnings (one pre-existing informational `str` message).
- check-parens: `OK`.
- Pure namespace: **3 tests / 17 assertions, 0 failures/errors**. Real source
  declaration, temporary Git repositories, unrelated second commit, actual C4;
  updater `[1 false :predicted-not-observed]`, other two wants `[0 false :neither]`.
  Invalid revision stays `:unknown-sha` / `:observation-missing`. Also exercises
  disabled guard, fractional theta, all five verdicts, duplicate measurements
  and wrong artifact SHA.
- Runner namespace: **184 tests / 575 assertions, 0 failures / 86 errors**.
  Every error is `Serving runner source drifts from the canonical checkout`.
  Log: `/tmp/fix-10a-runner-test.log`. The guard was left enabled as requested.
  The new helper-level test executed successfully: admitted literal comparison
  bytes/digest and rejected D-task source tampering. The full close test is
  present but stopped at the source guard; the owner must run this namespace
  after merge. Existing manifest assertions account for the new final entry.
- `git diff --check`: clean.

## Failure on base main

Detached baseline worktree `/home/joe/code/futon2-fix-10a-baseline` at `1d0ad46c`:

```sh
clojure -Sdeps '{:paths ["src" "resources" "." "scripts" "/home/joe/code/futon2-fix-10a/test"]}' -M:test -m cognitect.test-runner -d /home/joe/code/futon2-fix-10a/test -n futon2.aif.token-outcome-test
```

Exit 1: `Could not locate futon2/aif/token_outcome__init.class,
futon2/aif/token_outcome.clj or futon2/aif/token_outcome.cljc on classpath.`
The comparison namespace does not exist on base main. Thus no assertion could
execute there; this is the honest negative baseline, not an invented failing
assertion. The distinguishing new assertion on this branch is:

```clojure
(is (= [1 false :predicted-not-observed]
       ((juxt :predicted :observed :verdict) (rows updater))))
```

## Pure-namespace warrant

`test-registry-37fd314017d58bdef529732b98ddccf3db3f54f8a8e8a7c6d83c9ac13c40ce00`

Registered using `clojure -M -m futon3c.test-registry run
/tmp/fix-10a-registry.edn` in `/home/joe/code/futon3c`; then checked with
`clojure -M -m futon3c.test-registry check /tmp/fix-10a-registry-check.edn`.
Both returned `:warrant? true`. Registered result: 3 tests, 17 assertions,
0 failures/errors, exit 0. Scope pins the pure namespace, its test, the real
source declaration and mission document; registry closure records its loaded
code dependencies. **This is not a warrant for the guarded runner integration.**
