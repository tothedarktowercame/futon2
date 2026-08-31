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

## C66 trace persistence addendum

Schema v16 adds a top-level `:preference-stack` envelope sourced from the same
ranked evaluation maps that carry the scores. An empty stack is
`{:status :present :value []}`; a stack not emitted by the evaluator is
`{:status :absent :reason :not-recorded-by-evaluator}`. Partial and conflicting
candidate populations remain explicit and retain their values by rank.

The current serialized stack is 2,646 bytes. Repeating it across the observed
110-candidate scale would add 291,060 bytes per tick. Since every current
candidate receives the same production constant, v16 stores one exact value
with `:scope :all-ranked-actions` and `:candidate-count`; it does not duplicate
the value inside stripped ranked rows. If candidates ever disagree, the
`:conflict` variant preserves each value instead of selecting one silently.

The C58 R2 reader needs no new era arm: it classifies the observation-channel
schema from each record's timestamp and ignores additive trace fields. The
v16 boundary remains directly inspectable in `:wm-version
:trace-schema-version` for scheduled records.

## C83 binding

`preferenceStackLiveRecorded` now binds to the serialized witness rather than
checker source. `checks/preference_stack_binding_check.clj` validates the
committed EDN with the C63 shape checker, then independently invokes production
`compute-efe` and passes that scored evaluation object through trace v16. The
serialized stack, production result, and trace envelope must be equal. This
closes the parallel-artefact risk: the binding points at the value the live
scoring and trace path actually carry.

Binding-level controls are intentionally different from the C63 missing-layer
mutation:

```sh
clojure -M -m checks.preference-stack-binding-check
clojure -M -m checks.preference-stack-binding-check --negative absent
clojure -M -m checks.preference-stack-binding-check --negative malformed
```

The first negative resolves the binding against a nonexistent evidence path;
the second feeds unambiguously incomplete EDN. Either must be rejected with
exit 0 under the C16 control convention; a slipped mutation exits 2.

Canonical C66 verification:

```sh
clojure -M:test -m cognitect.test-runner -n futon2.aif.trace-test
clojure -M:test -m cognitect.test-runner
bb checks/preference_stack_witness_shape_check.clj
bb checks/preference_stack_witness_shape_check.clj --negative
clj-kondo --lint src/futon2/aif/trace.clj test/futon2/aif/trace_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el \
  --eval '(arxana-check-parens-cli)' -- --no-defaults \
  src/futon2/aif/trace.clj test/futon2/aif/trace_test.clj
```
