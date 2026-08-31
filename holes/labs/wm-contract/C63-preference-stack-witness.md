# C63 — serialized PreferenceStackWitness

Recorded 2026-08-31 by `wm-verbs`.

## Result

`PreferenceStackWitness.edn` is serialized evidence emitted from the production
`futon2.aif.efe/compute-efe` boundary.  Its five-layer `:preference-stack` is the
value returned by that boundary for the named dry-run input.  Generation and
validation are deliberately separate: `scripts/emit_preference_stack_witness.clj`
invokes production and emits EDN, while
`checks/preference_stack_witness_shape_check.clj` only reads the committed EDN.
The validator therefore cannot manufacture the evidence it accepts.

The witness names `preferenceStackLiveRecorded` as its eventual consumer, but
this delivery does **not** alter or rebind that registry entry.  Rebinding stays
a separate evidence-owner pass.

## Falsifier and exit convention

The shape check requires the exact top-level witness envelope, exactly five
unique layers, the six fields of every serialized layer, non-empty provenance,
and exactly `:habit-prior` as unfolded.  `--negative` removes
`:live-goal-outcomes`; rejection demonstrates that an incomplete serialized
stack cannot pass.

The C16 convention is `0=pass`, `1=ordinary failure`, and
`2=mutation-slipped`.  A rejected negative control exits 0 and says
`negative-control PASS (missing-layer mutation rejected)`.

## Canonical invocations

```sh
bb scripts/emit_preference_stack_witness.clj
bb checks/preference_stack_witness_shape_check.clj
bb checks/preference_stack_witness_shape_check.clj --negative
clojure -M:test -m cognitect.test-runner
AUTH=$(python3 -c 'import json; print(json.load(open("/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json"))["source"]["git-sha"])')
bb -cp . checks/contract_lint.clj --contract /home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json --registry checks/witness-registry.edn --report /tmp/C63-contract-lint.edn --authority "$AUTH"
clj-kondo --lint scripts/emit_preference_stack_witness.clj checks/preference_stack_witness_shape_check.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el \
  --eval '(arxana-check-parens-cli)' -- --no-defaults \
  scripts/emit_preference_stack_witness.clj checks/preference_stack_witness_shape_check.clj
```
