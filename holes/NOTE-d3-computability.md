# D3 computability from the WM outer-loop archive

Date checked: 2026-08-26. Scope: the 62 attempt directories under
`data/wm-full-loop/wm-outer-loop-40-v1` through
`data/wm-full-loop/wm-outer-loop-46-v1`, across checkpoints
`001-time-step.edn` through `007-closed.edn`.

## Verdict

**No.** An expected cost over successor states cannot be assembled for even one
archived attempt without inventing a term.

The archive contains a probability distribution over the *candidate action
menu* (`:softmax-weights`) and, in six attempts, scalar evidence metadata for
the selected policy (`:e-s :probability`). It contains no distribution
`P(s' | cascade, s)` over successor states of a cascade. It also contains no
realized state-indexed cost `G(s')`: the available `:predicted-g-s :value` is
one scalar forecast for the selected item, not a cost function over possible
successors. Consequently the required assembly

```
Σ s' . P(s' | cascade, s) * G(s')
```

has neither its state-indexed probabilities nor its state-indexed costs.
Multiplying `:e-s :probability` by `:predicted-g-s :value` would instead weight
one scalar by policy-selection evidence; it is not an expectation over
successor states.

Concrete examples are
`data/wm-full-loop/wm-outer-loop-42-v1/attempt-051/002-selection.edn`, which
contains both `:predicted-g-s` and `:e-s`, and
`data/wm-full-loop/wm-outer-loop-46-v1/attempt-060/002-selection.edn`, which
contains a varying candidate-menu `:softmax-weights` map. Neither contains a
successor-state support, transition probability keyed by successor, or cost
keyed by successor.

## Recorded distributional and probability-adjacent quantities

Later checkpoints embed earlier records, so repeated occurrences in
`003-construction.edn`, `006-adjudication.edn`, and `007-closed.edn` are copies,
not new observations. Coverage below counts distinct attempts.

| Quantity | Where first recorded | Coverage and observed range | Varies? | What it is |
|---|---|---:|---|---|
| `:softmax-weights` | `002-selection.edn`; copied into `003-construction.edn` | 48/62 attempts; individual weights range `3.2973104884559817E-57` to `0.5714707411955702` (2,691 distinct values) | Yes | A normalized distribution over the atomic candidate menu. The extrema can be checked in `wm-outer-loop-46-v1/attempt-060/002-selection.edn` and `wm-outer-loop-40-v1/attempt-001/002-selection.edn`. It is not a successor-state distribution and not a cascade rollout. |
| `:e-s :probability` | selected-policy evidence in `002-selection.edn`; copied later | 6/62 attempts; `0.45` to `0.45` | No | Scalar evidence probability for one `:policy-id`; example `wm-outer-loop-42-v1/attempt-051/002-selection.edn`. There is no collection indexed by possible successor states. |
| `:e-s :log-probability` | same `:e-s` map | 6/62; `-0.7985076962177716` to `-0.7985076962177716` | No | Natural logarithm of the same scalar probability, not an additional distribution. |
| `:e-s :posterior-mass` | same `:e-s` map | 6/62; `4.5` to `4.5` | No | Unnormalised scalar posterior mass associated with the selected policy. |
| `:e-s :selection-count` | same `:e-s` map | 6/62; `4` to `4` | No | Count used to produce the evidence metadata, not a distribution. |
| `:e-s :alpha` | same `:e-s` map | 6/62 selected-policy records; `0.5` in those records | No within `:e-s` | Smoothing/prior parameter. Other unrelated archive maps also use the key `:alpha` with value `1.0`; those are not extra policy outcomes. |
| `:predicted-g-s :value` | `002-selection.edn`; copied later | 6/62; `0.4` to `0.4` | No | One predicted strategic-cost scalar with semantics `"predicted-strategic-cost-not-habit"`. |
| `:predicted-g-s :uncertainty :lower` | same map | 6/62; `0.2` to `0.2` | No | Lower endpoint of a fixed uncertainty interval, not probability mass. |
| `:predicted-g-s :uncertainty :upper` | same map | 6/62; `0.8` to `0.8` | No | Upper endpoint of the same fixed interval. No distributional family or mass within the interval is recorded. |
| `:tau-spread` | `002-selection.edn`; copied later | 48/62; `0.1` to `25.1142644778137` (28 distinct values) | Yes | Dispersion diagnostic over candidate scores. The low and high cases are checkable in `wm-outer-loop-40-v1/attempt-038/002-selection.edn` / `attempt-031/002-selection.edn` and `wm-outer-loop-46-v1/attempt-060/002-selection.edn`. It does not identify successor states. |
| `:tau` | `002-selection.edn`; copied later | 48/62; `1.0` to `1.0` | No | Selection temperature/control scalar. |
| `:selection-gain` | `002-selection.edn`; copied later | 48/62; `1.0` to `1.0` | No | Selection scaling scalar. Its constancy means it contributes no empirical distribution. |
| `:tau-effective` | decision explanation in `002-selection.edn`; copied later | 27/62; `1.0` to `1.0` | No | Derived effective selection temperature, also constant where recorded. |

The archive therefore does record one distribution that moves—the softmax over
available atomic actions—and one dispersion diagnostic that moves. All fields
closest to the later `:predicted-g-s`/`:e-s` interface are constant in their six
recorded attempts.

## Missing producer or missing record?

This is a missing **measurement**, not merely a missing record of something
already measured.

Nothing in the seven checkpoints defines or computes a rollout distribution
over successor states for the selected cascade, and nothing measures realized
`G` for those successors. The archive cannot write down values that the current
evaluation never produces. A recording-only change would be sufficient only
if another component already computed both `P(s' | cascade, s)` and `G(s')`;
the attempt records provide no reference, identifier, digest, or omitted-result
field indicating such an existing measurement. Thus expected policy cost is a
registered demand with no producer in this archive, not a produced value lost
at persistence time.
