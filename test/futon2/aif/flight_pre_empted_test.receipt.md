# Receipt: futon2.aif.flight-pre-empted-test (R1(c-2))

Base: futon2 main `8403d27285b0cd15cce01d1a006effc7c2444a49` (the
lane was rebased from the packet's `7231c36ae` before editing).

Code: `227a9af9632ca73bcb4ef1cbd5e85626f6c6cc5a`.

Worktree: `/home/joe/worktrees/futon2/r1c2-pre-empted`.

Environment:

```text
openjdk version "21.0.12.1" 2026-08-18
OpenJDK Runtime Environment (build 21.0.12.1+1-1-24.04.4-Ubuntu)
OpenJDK 64-Bit Server VM (build 21.0.12.1+1-1-24.04.4-Ubuntu, mixed mode, sharing)
Clojure CLI version 1.12.5.1664
Linux 6.8.0-31-generic #31-Ubuntu SMP PREEMPT_DYNAMIC Sat Apr 20 00:40:06 UTC 2024 x86_64 GNU/Linux
No serving-JVM load and no War Machine POST/click.
```

## Live read used by the test

One authorized read-only GET was projected into the fixture:

```text
GET /api/alpha/wm/click?click-id=wm-click-9603346f-65f8-4d3a-ab6f-88ef4c40480e
{:click-id "wm-click-9603346f-65f8-4d3a-ab6f-88ef4c40480e",
 :status "completed",
 :result
 {:binding-status "absent",
  :runtime-disposition
  {:route "dedicated-meta-self-heal",
   :injury-id "injury/04e14314a0eec3aab88d8e7044be26f3ae5dfe6d",
   :repair-id "repair/d9ca0b41fe22ddc094e3b30816a1e085b6e16481",
   :status "awaiting-retry"}}}
```

The ordinary control is projected from the completed retry whose binding names
`tick-run-record-2026-10-23-90ca5944-3d52-4bd6-940c-fe2f6c2c8c6b.edn`.

## Red on the old source

The new test was run before the code change, while source was still the rebased
main behavior equivalent to packet base `7231c36ae` for this path.

```text
FUTON3C_INVOKE_JOBS_FILE=$(mktemp /home/joe/notes/r1c2-red-jobs.XXXXXX) \
  clojure -X:test :nses '[futon2.aif.flight-pre-empted-test]'

Ran 3 tests containing 14 assertions.
5 failures, 0 errors.
```

The important failures were:

```text
expected: (= 1 @posts)
  actual: (not (= 1 2))
expected: (= :pre-empted (:status f))
  actual: (not (= :pre-empted :no-progress))
expected: (nil? (get-in f [:clicks 0 :abstention]))
  actual: (not (nil? {:kind :run-record-missing, :missing :run-record}))
```

The not-found control retained `:run-record-missing` on old source; only its new
typed `:click-state` evidence was absent. Full output:
`/home/joe/notes/r1c2-pre-empted-red.log`.

## Green gate

Command (one combined run):

```text
FUTON3C_INVOKE_JOBS_FILE=$(mktemp /home/joe/notes/r1c2-green-jobs.XXXXXX) \
  clojure -X:test :nses '[futon2.aif.flight-test futon2.aif.flight-driver-test futon2.aif.flight-runner-test futon2.aif.flight-click-refused-test futon2.aif.flight-click-refusal-test futon2.aif.flight-pre-empted-test]'
```

Result:

```text
Ran 40 tests containing 142 assertions.
0 failures, 0 errors.
```

Full output: `/home/joe/notes/r1c2-pre-empted-green.log`.

Static gates:

```text
clj-kondo: linting took 75ms, errors: 0, warnings: 0
check-parens flight_runner.clj: OK
check-parens flight.clj: OK
check-parens flight_driver.clj: OK
check-parens flight_pre_empted_test.clj: OK
```

## Reader audit

- `flight/run!` reads `:no-progress` only to refine it to
  `:awaiting-answer`; `:pre-empted` is already terminal, so that reader needs
  no change.
- `flight-driver/run-flight!` projected click `:abstention` and top-level
  `:refusal`; it now also returns top-level `:pre-emption`. Its compact click
  list need not duplicate the same map.
- `flight-runner/record-summary` remains the producer of
  `:run-record-missing`; `http-click-fn` now removes that abstention only when
  the durable click state proves dedicated META self-heal. Not-found/read
  failure retains it and adds typed `:click-state` evidence.
- Other futon2 `:abstention` readers (`full-loop-runner`, terminal receipts,
  loop-node exercise) read the WM decision's `[:decision :abstention]`, not
  the flight click summary, so they do not change.
- `progress-read`'s `:run-record-missing` and `progress-check`'s
  `:no-progress` are separate progress/reporting contracts, not flight status
  readers, so they do not change.
- futon3c source matches are all `:abstention-reason` in strategic-outcome
  records; none reads this flight record or click summary, so futon3c needs no
  change.
