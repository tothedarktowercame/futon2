# C98 — absence in scoring semantics

Date: 2026-08-31. Status: design and measurement only; scoring and selection unchanged.

## Semantics

An absent observation contributes no likelihood term. It is not the numeric observation zero: zero conditions the posterior strongly, while absence supplies no datum on that channel. Operationally, belief update and every evidence-derived diagnostic must range only over present channels and retain the support set that was used.

The seven observation/status consumers do three different jobs:

- pragmatic gap, epistemic pressure, and avoidance compute evidence-derived diagnostics;
- `infer-mode` classifies a state from multiple observations;
- policy and belief aggregation compare/rank downstream results.

The first group may compute over present terms, but its result must be `SupportedScore := {value, channels-present, channels-required, absent-reasons, score-kind}` rather than a bare number. Scores with different support are not silently comparable. At the selection boundary, comparison requires equal support; unequal support yields a typed refusal `:incomparable-support` unless a separately declared projection makes the supports equal. This chooses **(b), with (c) at selection**. It does not choose count-normalisation: dividing by the number present makes magnitudes look comparable without establishing that omitted channels have exchangeable meaning or weight.

Consequence: existing full-support scores compare exactly as today. Partial-support candidates may still carry diagnostics, but cannot change a winner until all candidates in that comparison share a declared support. This makes absence visible without fabricating evidence or smuggling a coverage penalty into AIF.

## Measured impact

Canonical command:

```
bb -cp . checks/absence_scoring_counterfactual.clj
```

At the recorded run it measured 54 trace files, 801 records, and 105,277 ranked candidates at corpus SHA-256 `1467970bf94f486b35844af98f2bbb9181e726106b85cce368591ed66ea2fda7`. **Zero records and zero candidates retain observation presence provenance.** Therefore the number of scored candidates with absent channels is unknown, and whether any ranking would change is unknown. Recomputing from stored numeric zeros would manufacture the distinction C12 says was lost.

The producer now has `observation-envelope`, but `trace-record` persists `:observation` directly. A future shadow measurement must persist the envelope and, per candidate, emit the score support and a counterfactual refusal/winner without switching selection. Only post-change ticks can answer the ranking question.

## The other blocked groups

**Avoidance (two sites) is the same evidence semantics but a different operational decision.** Absence does not activate an avoided state; the diagnostic should emit `:unknown`/`:not-evaluated`, not false. Whether a policy may proceed when an avoidance guard is unknown is a safety decision—fail closed, abstain, or require the channel—not a scoring normalisation decision.

**Precision (two sites) is a different producer-contract problem.** A present error record missing `:error` or `:observed` is malformed, not an absent observation channel. The current public contract and tests intentionally accept error-only records, so migration must either define a versioned `PredictionErrorRecord` with required fields or retain an explicit legacy variant. It does not share the support-comparability decision.

## Staging

1. Persist tagged observation envelopes and per-score support in shadow output.
2. Measure full/equal/unequal support populations and shadow refusals/winner differences.
3. Ask the operator to choose safety handling for unknown avoidance guards.
4. Switch score types and selection only in a separately announced delivery.

No scoring or selection code changes in C98. The absence lint remains 15.
