# R1(a) test receipt

Tested implementation HEAD: `07826ef6a7f7716881aa9693a9c132849ee126a2`

Worktree: `/home/joe/worktrees/futon2/r1a-preflight`

Branch: `r1a-budget-preflight`

Environment:

```text
openjdk version "21.0.12.1" 2026-08-18
OpenJDK Runtime Environment (build 21.0.12.1+1-1-24.04.4-Ubuntu)
OpenJDK 64-Bit Server VM (build 21.0.12.1+1-1-24.04.4-Ubuntu, mixed mode, sharing)
Clojure CLI version 1.12.5.1664
Linux 6.8.0-31-generic #31-Ubuntu SMP PREEMPT_DYNAMIC Sat Apr 20 00:40:06 UTC 2024 x86_64 GNU/Linux
FUTON3C_INVOKE_JOBS_FILE=/home/joe/notes/r1a-{red|green}-jobs.<mktemp suffix>
No serving-JVM load; no War Machine click.
```

## Bad case (red)

Construction: on top of the tested HEAD, replace the two preflight guards in
`run-flight!` so `flown` is evaluated unconditionally and the refusal branch
is never selected. This models deleting the preflight without disturbing the
availability read. The worktree was then restored byte-for-byte to HEAD.

Command:

```text
FUTON3C_INVOKE_JOBS_FILE=$(mktemp /home/joe/notes/r1a-red-jobs.XXXXXX) \
  clojure -X:test :nses '[futon2.aif.flight-driver-budget-preflight-test]'
```

Relevant output (full output retained at
`/home/joe/notes/r1a-preflight-red.log`):

```text
Testing futon2.aif.flight-driver-budget-preflight-test

FAIL in (whole-flight-click-budget-is-preflighted) (flight_driver_budget_preflight_test.clj:65)
available 1 cannot admit a two-click flight and posts no click
expected: (zero? calls)
  actual: (not (zero? 1))

FAIL in (whole-flight-click-budget-is-preflighted) (flight_driver_budget_preflight_test.clj:66)
available 1 cannot admit a two-click flight and posts no click
expected: (= :refused (:status result))
  actual: (not (= :refused :no-progress))
```

This pins the requested failure: without preflight, one click is attempted and
the flight stops later as `:no-progress`.

## Restored implementation (green)

Command:

```text
FUTON3C_INVOKE_JOBS_FILE=$(mktemp /home/joe/notes/r1a-green-jobs.XXXXXX) \
  clojure -X:test :nses '[futon2.aif.flight-driver-budget-preflight-test]'
```

Output (also retained at `/home/joe/notes/r1a-preflight-green.log`):

```text
Running tests in #{"test"}

Testing futon2.aif.flight-driver-budget-preflight-test

Ran 1 tests containing 15 assertions.
0 failures, 0 errors.
```

## Static gates

```text
$ clj-kondo --lint src/futon2/aif/flight_driver.clj src/futon2/aif/flight_runner.clj test/futon2/aif/flight_driver_budget_preflight_test.clj
linting took 50ms, errors: 0, warnings: 0

$ emacs --batch -l /home/joe/code/futon4/dev/check-parens.el src/futon2/aif/flight_driver.clj src/futon2/aif/flight_runner.clj test/futon2/aif/flight_driver_budget_preflight_test.clj
OK
```
