# Fix 9a: record declared need-support structure

Branch `fix/narrative-9a`, base `488e5827`. Implements discovery slice 1 only.

`cascade-structure/receipt` is pure. Version `:wm/cascade-structure-v1`, basis
`:declared-need-support-v1`. It records the pattern carrier, token-witnessed
producer→consumer need edges, each pattern's reflexive predecessor support,
the distinct set family, input SHA-256, shape, overlap witnesses, missing
intersections, independent Boolean axes and intersection-graph component count.
Shape precedence is singleton, chain, disjoint antichain, laminar tree,
overlapping intersection-closed semilattice, otherwise unclassified; the empty
family has its own label/finding. Tree includes forests: connectedness is separate.
No meet completion or new authority inference occurs.

Missing interpretations, unsupported guards, duplicate IDs and cyclic needs
(including a self-need) produce typed unavailable findings, not invented axes.
Cyclic receipts retain the carrier and witnessed edges. Valid empty input records
an empty-family finding, zero components and false axes. Authority evidence always
records `{:status :unavailable :reason :authority-graph-not-retained}`.
The digest covers target and the supplied IDs/guards/produces, not scores.

The selected cascade constructor removes its literal `:semilattice []` and adds
`:cascade-structure`. Construction checkpoint projection retains that key.
Legacy constructors/edge consumers are unchanged. Selected actions, precedence,
admission, scoring, habit selection and folding are unchanged. Tests compare the
former constructor payload except the replaced evidence field, serialized policy
keys, and actual fold results. This receipt is not inserted into the action.

`run_narrative.clj` reads the receipt in the construction section and passes its
label/basis caption to `narrative_figures.clj`. Both Markdown and SVG now say:
“shape: singleton (basis: declared need-support; authority structure not recorded)”.
Historical records retain the existing fallback display.

## Regression and checks

Before implementation, the actual selected constructor on base main failed all
three initial assertions (`red.log`), including:

```
expected: (not (contains? c :semilattice))
  actual: (not (not true))
expected: (= :singleton (get-in c [:cascade-structure :shape]))
  actual: (not (= :singleton nil))
```

Fresh standalone results:

- `futon2.aif.cascade-structure-test`: 6 tests / 63 assertions, green. Seven shape
  controls; missing-meet refusal; real P/Q constructor fixture; EoI support/priority
  variants; diamond; malformed/cyclic input; EDN/digest; real fold and policy-key
  parity; reference 1789964661 singleton from the retained narrative fixture.
- `futon2.aif.run-narrative-test`: 21 tests / 110 assertions, green, including
  the complete receipt caption in both written Markdown and SVG.
- `futon2.aif.narrative-figures-test`: 5 tests / 260 assertions, green.
- clj-kondo: zero errors/warnings (one preexisting runner informational message).
- check-parens: OK on all seven changed Clojure files.

`cascade-structure-runner-test` is written against the existing passing
`run-feature-card-attempt` grounded fixture without altering its action. The
fixture contains bare pattern IDs, so it asserts the honest typed unavailable
receipt persists in the construction checkpoint, the old slot is absent, and
its grounded outcome is unchanged. Attempting it here stops at the existing
`:stale-runner-source` authority guard: 1 test / 3 assertions, 0 failures / 1 error.
No guard was bypassed. **Owner must run this namespace on merged canonical main.**
The helper fixture owns temp storage; the runner test also uses the established
hermetic store/trace fixtures. No WM click, shared JVM load or real store write.

Commands, from `/home/joe/code/futon2-narrative-9a` (one namespace at a time):

```sh
clojure -M:test -m cognitect.test-runner -n futon2.aif.cascade-structure-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.run-narrative-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.narrative-figures-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.cascade-structure-runner-test
clj-kondo --lint src/futon2/aif/cascade_structure.clj src/futon2/aif/full_loop_runner.clj src/futon2/aif/run_narrative.clj src/futon2/aif/narrative_figures.clj test/futon2/aif/cascade_structure_test.clj test/futon2/aif/cascade_structure_runner_test.clj test/futon2/aif/run_narrative_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/cascade_structure.clj src/futon2/aif/full_loop_runner.clj src/futon2/aif/run_narrative.clj src/futon2/aif/narrative_figures.clj test/futon2/aif/cascade_structure_test.clj test/futon2/aif/cascade_structure_runner_test.clj test/futon2/aif/run_narrative_test.clj
```

Environment: Linux; Java 21.0.11; Clojure CLI 1.12.5.1664 / Clojure 1.11.1.
Logs adjacent. Registry config scopes the pure/constructor regression namespace;
its warrant does not claim the blocked end-to-end runner check passed.
