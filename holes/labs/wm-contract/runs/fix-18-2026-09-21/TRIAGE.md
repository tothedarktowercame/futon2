# Fix-18: stale carry and run-record assertions

Branch `fix/narrative-18`, base `80162c1f`. Only the two commissioned test
namespaces changed; no production changes, fixture recapture, or habit-test edits.

| Assertion (base line) | Failing since | Classification | Cause and correction |
|---|---|---|---|
| carry, line 50: first decision equals serialized baseline | `28a90c73` | (a) stale pin | “Realize selected cascades from witnessed guard conditions” deliberately retained target-qualified `:observation-locators` on candidates. Those maps occur in both the selected action and posterior keys. Compare first/next outcome bytes directly, and independently pin the historical numerical contract after the authorized empty-candidate exclusion. |
| carry, line 51: disagreeing-prior decision equals serialized baseline | `28a90c73` | (a) stale pin | Same metadata addition. Preserve the actual invariant: disagreeing prospective carry cannot alter action, mass, beta, complete selection law or consumed D. Keep all staging/serialization/negative-validator checks. |
| uniform record, line 90: every F status is `:computed-not-attached` | `acc4f3c4` | (a) stale pin | “Stage H4 observed-prefix F and typed-neutral production receipts” deliberately replaced future-rollout F with missing-prefix evidence. Assert three policies, nil F, `:not-supplied`, `:no-admitted-policy-prefix`, policy identity and named pending dependency. |
| uniform record, line 100: no table field is missing/bad | `acc4f3c4` | (a) stale pin | The same H4 change makes consumed F absent, so the consumed-value census is `:missing`; the checker must retain this absence. Assert the exact missing-F evidence, all other census terms present, and the exact five table verdicts. |

## Historical evidence

Fresh standalone processes in the detached worktree
`/home/joe/code/futon2-fix-18-history`, running each revision's own code/tests:

| Revision | Namespace | Tests/assertions | Failures/errors | Log |
|---|---|---:|---:|---|
| `28a90c73^` (`747f86fd`) | token-belief-carry-test | 4/30 | 0/0 | carry-28a-parent.log |
| `28a90c73` | token-belief-carry-test | 4/30 | 2/0 | carry-28a.log |
| `acc4f3c4^` (`385f54dc`) | uniform-run-record-test | 3/36 | 0/0 | uniform-acc-parent.log |
| `acc4f3c4` | uniform-run-record-test | 3/36 | 2/0 | uniform-acc.log |
| base `80162c1f` | token-belief-carry-test | 4/30 | 2/0 | carry-before.log |
| base `80162c1f` | uniform-run-record-test | 3/36 | 2/0 | uniform-before.log |

Exact old failing assertions:

```clojure
(= baseline (str (pr-str (outcomes first-decision)) "\n"))
(= baseline (str (pr-str (outcomes next-decision)) "\n"))
(every? #(= :computed-not-attached (:f-status %))
        (get-in carried [:selection-certificate :policies]))
(not (re-find #"\s(missing|bad)\s" line))
```

For the last assertion the actual result is `(not (not [" missing " "missing"]))`.
For F the actual policy records say `:not-supplied`, with F nil.

H4 authority and rationale are retained in
`holes/labs/wm-contract/runs/h4-staged-prefix-2026-09-21/EXECUTION.md`:
claude-12 scope ruling `invoke-1789966199358-22918-992d2625`; no admitted
history is invented, and “The receipt's F is absent, not measured zero.”
The unchanged census deliberately classifies nil consumed values as missing;
checker commit `4238ec82` (“wm_run_validity.bb: a census that says :missing
must not score ok”) requires it to report that status.

## What the revised tests still establish

The original carry fixture remains unchanged. Its C0 probability is removed
and the other three probabilities are divided by their summed old mass. This
is the explicit migration required by `c155d690` (“Decline empty and
unreceipted cascade proposals before executable selection”), authorized by
claude-12 `invoke-1789961013061-22833-8d700d74` and documented in that commit's
`runs/nonempty-cascades-2026-09-21/LANDING.md`. The earlier `d168d348` ruling
also excludes empties from action mass. There is no new numerical capture.

Expected posterior: C1 and C2 approximately 0.39786472067354167, C3
0.2042705586529165. Numerical tolerance is 1e-12 for the renormalization's
floating arithmetic; the direct first/next comparison remains byte-exact.
The original selected C2's target/precedence, beta and consumed D remain pinned
independently. D must be the old true-fact point mass for all three policies,
and explicitly differs from the staged prior `{#{} 1}`. Thus comparing the
two new decisions cannot silently accept a changed initializer or posterior.

The uniform test keeps producer-to-durable-record equality for the full
selection law, certificate, enumeration and census; the real writer and `bb`
checker still execute. It retains incomplete enumeration, C grain mismatch,
source paths, store isolation and the no-selection negative case. Honest
missing F now means `INVALID (3/5 ok)`, not missing record provenance.
The old `"VALID (3/5 ok)"` substring assertion also matched `"INVALID (3/5 ok)"`;
it is replaced by an anchored full verdict plus exact per-field statuses.
No production validator was weakened to make the test green.

## Reproduction and gates

Run from the relevant worktree, one namespace at a time:

```sh
clojure -M:test -m cognitect.test-runner -n futon2.aif.token-belief-carry-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.uniform-run-record-test
clj-kondo --lint test/futon2/aif/token_belief_carry_test.clj test/futon2/aif/uniform_run_record_test.clj
emacs --batch -Q -l /home/joe/code/futon4/dev/check-parens.el -f arxana-check-parens-cli -- test/futon2/aif/token_belief_carry_test.clj test/futon2/aif/uniform_run_record_test.clj
```

After changes: carry **4 tests / 39 assertions**, uniform **3 / 67**, all green.
clj-kondo: **0 errors, 0 warnings**; check-parens: **OK**; diff-check clean.
These are test-contract repairs: the old assertions fail on main as reproduced
above; no production defect or unresolved (b)/(c) finding was identified.
No WM clicks, shared checkout edits or shared JVM loads occurred.

## Registry warrants

Implementation commit: `792afae2`. Both runs executed committed code in the
isolated worktree, returned warrant=true and stable execution, with matched
postchecks. Configs, complete receipts, logs and closures are retained here.

- carry: `test-registry-737fcb8ac49e9a0a22f08e68e83d43ad77b8af8eb219f28348849cc89c3c81b7`; `{:assertions 39, :duration-ms 4847, :errors 0, :exit 0, :failures 0, :tests 4}`.
- uniform: `test-registry-3598eb4cd0b7ff40fff12996855330eef1c04429465c6b60907326a8d88c51c9`; `{:assertions 67, :duration-ms 3814, :errors 0, :exit 0, :failures 0, :tests 3}`.
