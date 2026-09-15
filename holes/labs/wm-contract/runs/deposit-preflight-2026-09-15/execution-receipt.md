# Deposit preflight execution receipt

Implementation commit: `855953a9`.

## Attempt trail

Development attempts were retained in the agent execution log. The first two
failed on unmatched delimiters in the new namespace. The next focused run
reached the production seam and exposed one expectation error: an empty stray
file refuses as `:evidence-not-single-edn`, not `:schema-mismatch`. That
expectation was corrected before the final gate.

## Final gates

Executed from `/home/joe/code/futon2` after the implementation commit:

```text
clj-kondo --lint src/futon2/aif/deposit_preflight.clj test/futon2/aif/deposit_preflight_test.clj
```

Exit 0: `errors: 0, warnings: 0`.

```text
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/deposit_preflight.clj test/futon2/aif/deposit_preflight_test.clj
```

Exit 0: `OK`.

```text
clojure -X:test :nses '[futon2.aif.full-loop-runner-test futon2.aif.deposit-preflight-test]'
```

Exit 0: 177 tests, 941 assertions, 0 failures, 0 errors. This was the single
final fresh-JVM test invocation after the last source/test change.

The executable template command also exited 0 and classified all 13 deposit
files as `:admitted-as-record` or `:admitted-as-companion`:

```text
clojure -M -m futon2.aif.deposit-preflight holes/labs/wm-contract/deposit-templates/T-addressed repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-002-artifact-binding-mismatch
```

## Pins

```text
23e9ac83bcd294ab06b218bf41a23b1b824a4684e318cc6dfca0f7cbc391d761  src/futon2/aif/deposit_preflight.clj
579f72e0df687c3cf2c87d677aebf4d0161681f9facd206e96dd7da7c5dfec52  test/futon2/aif/deposit_preflight_test.clj
7da51274e48ab5b9c5fd6a2b3051b9ac4ee8253f906bf6e9ccd7bb494401ab9c  src/futon2/aif/full_loop_runner.clj
05b3b520523bf406a4d489a7236f04f083662010207b6ab04a15f31dc52893f0  src/futon2/aif/limb_evidence.clj
1266151c767ae8e51545f3c5f12683cb1830edbe835928cff49b6382733f572c  deposit-templates/T-addressed/subject-before.edn
7819c0ad9a3c2a9a52ffd015e7ef0faabf581a78402d6f8abe569137583b71d1  deposit-templates/T-addressed/subject-after.edn
```

The harness is integrity/admission preflight only. It does not establish
actor authority, execute commands named by template receipts, or qualify a
future close.
