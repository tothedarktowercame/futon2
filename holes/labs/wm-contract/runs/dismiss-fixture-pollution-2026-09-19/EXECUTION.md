# `dismiss-fixture-pollution!` registered execution

Implementation and controls: `02387be6d05b594009fb4b5c06b7071a718a4362`.
Committed registry scope: `05893b6403ba5535578dcb2fabe4070277a61bf3`.

No live finding was dismissed. Tests used temporary repair-store roots only;
no click or serving-JVM interaction occurred.

## Static gates

```text
clj-kondo --lint src/futon2/aif/repair_obligation.clj test/futon2/aif/repair_obligation_test.clj test/futon2/aif/tripwire_test.clj
linting took 127ms, errors: 0, warnings: 0

emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/repair_obligation.clj test/futon2/aif/repair_obligation_test.clj test/futon2/aif/tripwire_test.clj
OK
```

## Registered repair-store suite

Executed once from `/home/joe/code/futon3c`:

```text
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/dismiss-fixture-pollution-2026-09-19/register.edn

evidence-id test-registry-81279a57e10a87a3351c115a860f2e78f714a4091d3215ee4b86a9fb5c9a6acf
warrant? true
results {:assertions 114, :duration-ms 1424, :errors 0, :exit 0, :failures 0, :tests 23}
bound repair-store/dismiss-fixture-pollution -> test-registry-81279a57e10a87a3351c115a860f2e78f714a4091d3215ee4b86a9fb5c9a6acf
```

Registry artifacts:

- `/home/joe/code/storage/test-registry/repair-store-dismiss-fixture-pollution-2026-09-19/5c3b591b-9cb3-421e-a662-1c7fc055bcdd.closure.edn`
  — `79363b652857b4c985f3a2040705ce4dab490201eaad96ca30a8dadd59c2b835`
- `/home/joe/code/storage/test-registry/repair-store-dismiss-fixture-pollution-2026-09-19/5c3b591b-9cb3-421e-a662-1c7fc055bcdd.log`
  — `badbebfc34fd5342cb412438fb6396ed99cb126b3bd97dbbbb017ad29691f2db`

## T8 closure control

The fresh-JVM `futon2.aif.tripwire-test` run passed 41 tests and 129
assertions. Its directory-invariant control proves both
`:dismissed-fixture-pollution` and `:dismissed-echo` records close their IDs
for T8 without adding either keyword to a status enumeration.

The repair-store controls cover retained external resolution, the production
shape's `:subject-is-production` refusal, missing resolution refusal,
open-obligation exclusion, exact copied evidence, immutable finding bytes,
and double-dismissal refusal.
