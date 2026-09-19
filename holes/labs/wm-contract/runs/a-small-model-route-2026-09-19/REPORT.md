# A-small-model-route

Implementation: `0d32c6e0f70e11550f2fa7d0d427d0bd22199bb8`.
Author: codex-3. Independent review/acceptance belongs to claude-4.
Branch: `codex-3/a-small-model-route`; shared futon2 checkout not modified.

**A producing cascade has finite F under the coupled model through the
production scorer AND selector.** In the ten-token joint-failure control it
produces five established tokens and observes none. Coupled F is
**5.413103575948838 nats**, consumed unchanged by selection. The independent
configuration gives **11.563177144237736**. Exact observation of the produced
state gives F=0; exact observation contradicting that state remains a typed
model contradiction, with probability zero and infinite F retained.

## Interface and route

`futon2.aif.observation-model/query` accepts a declared EDN model and a query:
`:row`, `:likelihood`, `:predict`, `:condition`, or `:score`. It returns the
model, including all parameters and provenance, on computations and refusals.
The backend is `:exact-enumeration`. The selectable configurations are
`:exact-checks`, `:independent-judgement`, and `:coupled-judgement`; the latter
is a mixture of conditionally independent rows under a shared latent cause.
This is the reference implementation to compare future compiled backends
against, not a separate test-only implementation.

The existing `efe/rank-actions` -> `efe/rank-cascade-actions` interface selects
the new scorer on **presence** of `:observation-model` in opts. Explicit nil
refuses. The only edit to existing code is that dispatch and its docstring;
the historic branch is retained. The opt-in opts also supply
`:prediction-context {:occurrence-id ... :tau T}`, a matching `:observation`,
`:horizon-steps`, and the explicit `:cascade-spec`. Ambiguous simultaneous
`:adjudication-rates` or non-unit `:zeta` refuse instead of being ignored.

`cascade-observation-route/run` accepts `{:state ... :candidates ... :opts ...
:selection-opts ...}`. It uses the production scorer and
`policy/select-action-cascades`, returns an EDN outcome record, and supplies
`:next-state` from the selected hypothesis's conditioned posterior. Tests feed
that state into a second prediction. The selector reads only the explicit
habit path; tests use a temporary absent path and never access live habits.
No actuator is called.

The timing convention is explicit: G is the prior-predictive horizon sum;
F explains the supplied observation at that occurrence/horizon. Conditioning
does not retroactively replace the prediction being evaluated. The observed
event carries explicit `:present` and `:absent` sets; unmentioned variables
are marginalized, not marked absent. No event is inferred from C's
`:evidence`. Missing observations and occurrence/horizon mismatches refuse.
A missing or impossible candidate observation refuses the entire comparison
with per-candidate evidence retained; candidates are neither silently dropped
nor assigned neutral F.

This packet admits **synthetic experiments only**: every model must carry
`:provenance {:status :synthetic :calibrated false :source ...}`. Every route
record says `:scope :synthetic-bounded-replay` and `:actuated? false`.
The new G certificate uses its own bounded-observation schema; it does not
claim a new Lean witness or invent independent marginal rates for the old
G-term census. The existing selection certificate retains the complete model
in its scoring provenance and records F actually consumed. Live activation,
calibration, expanded census semantics, WMC, and production admission are not
claimed. No DAG or flag was changed.

Limits: at most 10 tokens, 10 horizon steps, 16 candidates. Probabilities and
conditioning use exact rational arithmetic; logarithmic quantities use
doubles. Full distributions are enumerated at this bounded size.

## Non-vacuous controls

All use the declared side-by-side model: bad-day probability 3/20, good-day
false-negative rate 1/34, bad-day rate 1/2, false-positive rate 1/100.
No parameter is represented as calibrated.

* **No tokens reported among ten, with five actually established:** independent
  probability `9509900499/1000000000000000`, coupled
  `595709677157859/133633600000000000`; ratio `39150625/83521`, about 469.
* **All five established tokens missed, remaining observations unspecified:**
  the joint miss query marginalizes the other five tokens. Its relation to
  the full no-report event is checked exactly through the `(99/100)^5`
  no-false-positive factor.
* **Two established tokens both missed:** independent `1/100`, coupled
  `13/340`. Single-token miss probabilities are both exactly `1/10`, showing
  why marginal-only agreement would be vacuous.
* **Inference changes:** starting with equal probability of none/five
  established, the all-missed observation leaves over 400 times as much
  posterior mass on five-established under coupling as under independence.
* **Legitimate G invariance:** point-mass state, product C. Total correlation
  is `0.23105926744402128` nats; the risk increase equals the ambiguity
  decrease to the asserted 1e-9 tolerance. G is approximately
  `2.391404927489034` in both configurations. Both terms are independently
  computed. For point masses the scorer evaluates their sum by the exact
  cross-entropy identity, grouping equal C values with rational masses before
  taking logs; this prevents floating reduction order from splitting symmetric
  policies. Distributed beliefs use risk-plus-ambiguity and are tested too.

The recorded WM-shaped fixture is a pinned transcription of
`vm-test/futon2/vm/tick_001_s06_r5_test.clj` at `8fb7a5c1`: six tokens, four
candidates, horizon three, original q0/guards/productions/C. Its attached
observations are explicitly synthetic, not fabricated historical evidence.
Exact-model G matches all four recorded values. Independent and coupled
models preserve the C1/C2 tie and the family's G order. Coupled F for the two
fully producing candidates is `0.3383438959014662` and reaches selection.

## Validation and retained evidence

Warrant: **test-registry-3897ac30cd090abcf4d0511fbf66dbea5fb9467687515bdf2c1828461594b645**.
Registered by codex-3 and bound to **A-small-model-route** through the registry
validation CLI. Registered execution: **7 tests, 132 assertions, zero failures
or errors**, 4608 ms. Only `futon2.aif.observation-model-route-test` ran.

The registered execution root is the clean **detached** worktree
`/home/joe/code/futon2-a-small-model-route-warrant-0d32c6e0`, at the implementation
commit above. It remained clean after execution. Artifacts were written to
this report's directory in the separate development worktree.

* `registry.edn`: exact registration specification.
* `registration.txt`: successful mint and subject binding.
* `41bdd7aa-b9ba-43ee-9dd3-313fe4a170ff.log` and `.closure.edn`: registered run
  and loaded-source closure.
* `receipt.edn`: replay inputs, complete model, actual selection and outcome,
  extracted from the registered log's `ROUTE-RECORD`. The test also round-trips
  the full route record through a temporary EDN file before deleting it.
* `check.json`: subsequent HTTP `/api/alpha/test-registry/check` response with
  `warrant? true`, empty diff, and no outside-closure paths. No JVM was launched
  to consume the warrant.
* `binding.edn`: copy of the actual appended subject binding.

All changed Clojure passed clj-kondo (0 errors, 0 warnings) and
`futon4/dev/check-parens.el` (OK); `git diff --check` passed.

The initial registration specification copied the retired `:test-environment`
field from an older local example. The registry refused it before execution
(`registration-refused.txt`). Removing that retired field lets the registry
impose its canonical environment; the successful registered test ran once.

No calibration collection, SDD compilation, production click, shared-JVM
reload, or mutation under canonical futon2/data occurred.
