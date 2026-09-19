# `dismiss-echo!` registered execution

Implementation commit: `da42cf3b34ea39542d195df3f10ba91805aa3bed`.
Dedicated registry-suite commit: `48947cec517642915bcf430c23465d1877569b37`.

No live repair finding was dismissed. All mutation controls used temporary
store roots; no click or serving-JVM interaction occurred.

## Static gates

```text
clj-kondo --lint src/futon2/aif/repair_obligation.clj src/futon2/aif/tripwire.clj test/futon2/aif/repair_obligation_test.clj test/futon2/aif/tripwire_test.clj
linting took 104ms, errors: 0, warnings: 0

emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/repair_obligation.clj src/futon2/aif/tripwire.clj test/futon2/aif/repair_obligation_test.clj test/futon2/aif/tripwire_test.clj
OK

clj-kondo --lint test/futon2/aif/dismiss_echo_test.clj
linting took 8ms, errors: 0, warnings: 0

emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults test/futon2/aif/dismiss_echo_test.clj
OK
```

## Focused fresh-JVM control run

```text
clojure -X:test :nses '[futon2.aif.repair-obligation-test futon2.aif.tripwire-test]'
Ran 63 tests containing 234 assertions.
0 failures, 0 errors.
```

## Registry attempts and warrant

Two pre-execution registration attempts were typed refusals. The first used
the unsupported multi-namespace `-X:test :nses` form and the second used two
`-n` flags; both refused `explicit-namespace-required`. After adding the
single-namespace wrapper, one attempt refused `scope-not-committed`. None of
those three refusals executed a test or minted a warrant.

After committing the wrapper, this command executed once from
`/home/joe/code/futon3c`:

```text
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/dismiss-echo-2026-09-19/register.edn

evidence-id test-registry-57b14638125e1c200b925621226c11ce0060cc53233a8102c26d21431fb276be
warrant? true
results {:assertions 9, :duration-ms 1877, :errors 0, :exit 0, :failures 0, :tests 1}
bound repair-store/dismiss-echo -> test-registry-57b14638125e1c200b925621226c11ce0060cc53233a8102c26d21431fb276be
```

Registry artifacts:

- `/home/joe/code/storage/test-registry/repair-store-dismiss-echo-2026-09-19/5d1fdfc2-98c8-4e03-81ae-e64c76553bcd.closure.edn`
  — `34a93e7b3ce588a705f4dfeba3c5752550c7dd546a8e60387b45203285c649b2`
- `/home/joe/code/storage/test-registry/repair-store-dismiss-echo-2026-09-19/5d1fdfc2-98c8-4e03-81ae-e64c76553bcd.log`
  — `7423d99797e5fe44580ba6a39457de1b97d2981fa42706830469248a245d308a`

The warrant covers the disposed-source precondition, live-source refusal,
missing-witness refusal, source-status readback, append-only finding bytes,
double-dismissal refusal, open-obligation exclusion, and the T8 closure
directory invariant for `:dismissed-echo`.
