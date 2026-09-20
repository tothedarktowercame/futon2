# Observation parameter identity — codex-1, 2026-09-20

Implementation: `056c362a2a36f0f288a75f861b7347d2a5f319c0`.
Warrant: `test-registry-08e75d0849a0b896e05bc2dcc573ff4889783e1a432040b1818d94145b1492dd`,
bound to `A/observation-parameter-identity`: 3 tests, 31 assertions, zero
failures/errors; registry reported 985 ms and exit 0.

The optional model-level parameter map validates keyword identities, exact
probability values and nonblank basis strings. Rate slots admit bare exact
probabilities or exactly `{:param id}`. Validation returns the original model;
queries retain it unchanged in their records. Numerical rows resolve values
without replacing the declaration's references. References work in independent,
exact-check and coupled-component rates. Component weights are unchanged.

The docstring states instrument identity across re-measurement, a new identity
for a changed convention/channel, the six reserved names, and the location of
future variance metadata. A stateless validator cannot prove historical
instrument continuity; it preserves the declaration necessary to review it.
Variance metadata on parameter entries is retained but not interpreted or
propagated. It is never subjected to probability?.

## Six controls, executed through validate!/query

1. Unknown `{:param :absent}`: `:status :missing`,
   `:kind :unknown-observation-parameter`.
2. Two tokens reference `:intake-refusal-fp`: `:computed`; returned model
   retains exactly one parameter entry and two references to that id.
   Joint synthetic false-positive probability is exactly `(9/64)^2`.
3. Equal bare values: `:computed`; returned model has no parameters map and
   retains two literal values. Distribution agrees with (2), identity does not.
4. Inline variance at rates-map, token-entry, or reference-map level:
   `:inline-rate-variance` in all three cases.
5. Floating value 0.140625: `:invalid-model-parameters`; the entry's value
   fails probability?. Negative/out-of-range/nil values also refuse.
6. Existing bare model: validate! returns the unchanged model; row computes.
   The existing route namespace also passes: 7 tests, 132 assertions.

Additional controls cover reference re-measurement with unchanged id,
zero/nonzero referenced exact checks, mixed bare/reference components,
and all four specifically protected refusal kinds. Invalid nil rates still
return the prior `:invalid-model-rates` refusal rather than an exception.

## Gates

Executed from futon2, all exit 0:

```sh
clj-kondo --lint src/futon2/aif/observation_model.clj test/futon2/aif/observation_parameters_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- src/futon2/aif/observation_model.clj test/futon2/aif/observation_parameters_test.clj
clojure -M:test -m cognitect.test-runner -n futon2.aif.observation-parameters-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.observation-model-route-test
```

Lint: zero errors/warnings. Parens: OK. Tests: 3/31 and 7/132 green.
After explicit-path commit, executed from futon3c:

```sh
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/a-parameter-identity-2026-09-20/registry.edn
```

That registration executed the new test namespace and minted the warrant above.
No production rate attachment, measured-rate declaration, calibration claim,
WM click, judge/tick wiring or serving reload. The 9/64 fixture is explicitly
synthetic; no measurement basis is fabricated. Packet 3 remains responsible
for the six actual basis declarations, including the uncounted default FN and
its priority for replacement. No C/D/F/Q/scorer/mission_hole_wants edits.
