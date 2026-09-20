# Factorized terminal C family — 2026-09-20

Implementation: `c4fd0afd`. The rated factorized path validates C members per
step, computes the consumed member's log-marginals, and records that member.
No change to the identity-A arithmetic, belief-factorization guard, pointwise-C
boundary, exclusions boundary, observation model, or declarations.

Warrant: `test-registry-75ffc6b39b5c250847da4131c570b991b27a99c2a881266a8b7bdb7b915fe44f`.
Bound subject: `wm-c/factorized-terminal-family`. Registered result: 2 tests,
148 assertions, zero failures/errors; exit 0. Command/spec and runner log plus
closure are alongside this receipt. Every comparison is printed in the log.

Rated terminal family (factorized values; enumerated values agree within 1e-12):

| Belief | tau | Risk | Ambiguity |
|---|---:|---:|---:|
| Point mass | 1 | 0.3235567929628944 | 1.0627375681569962 |
| Point mass | 2 | 0.6774521304041992 | 1.0627375681569962 |
| Product | 1 | 0.08254652339684884 | 0.9934722771513984 |
| Product | 2 | 1.0831085275048202 | 0.9934722771513984 |

Totals: point 3.1264840596810863; product 3.1525996052044656.
The certificate's consumed C is checked pointwise over all four outcomes:
uniform at tau 1; declared weights {:a 2, :b 1} at tau 2. Both beliefs also
pass constant-C and zero-rate enumeration controls. Constant rated totals:
point 3.480379397122391; product 4.153161609312438. Zero-rate terminal totals:
point 2.8264840596810865; product 1.1407657224062815.

Negative controls' actual results (all :status :missing and :certificate nil):

- Pointwise C: :c-form-unsupported-with-rates.
- Nonempty excluded outcome set: :zeroed-unsupported-with-rates, :zeroed 1.
- Invalid placement :invalid: :invalid-preference-schedule, original schedule retained.
- Correlated state belief {#{} 1/2, #{:a :b} 1/2}: :non-factorizable-belief,
  :step 1, :support 2.

Gates: clj-kondo, zero errors/warnings on both changed files; Emacs batch
check-parens passed on both. Precommit suite passed (AGENTS.md forbids landing
unrun tests). The durable registry requires committed scope, so its execution
followed the implementation commit.

Launch accounting: direct `clojure -M:test -n ...` exited before loading tests
because this checkout's alias has no main opts. The successful precommit
command explicitly used `-m cognitect.test-runner`. Passing that direct command
to the registry then refused `:explicit-namespace-required` before execution
(retained in launch-refusal-*). The registry accepts the logical command
`clojure -M:test -n ...` and supplies its own runner; the corrected spec produced
the warrant in one registered test execution. No failed test was rerun to green.

Serving reload list: `futon2.aif.cascade-model-manifest` only. No serving reload
performed. No WM click. The bounded observation route is unchanged.
