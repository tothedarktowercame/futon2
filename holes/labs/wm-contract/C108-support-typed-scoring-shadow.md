# C108 — support-typed scoring shadow

Date: 2026-08-31. Status: shadow evidence only; selection unchanged.

## Output contract

Trace schema v18 adds `:support-typed-scoring-shadow`:

```clojure
{:status :measured|:partial
 :authority :shadow-only
 :score-kind :multi-objective-controller-score
 :candidates
 [{:candidate-index Nat
   :action map
   :rank Nat
   :current-score number
   :support-typed-score number
   :support [channel ...]
   :required-support [channel ...]
   :absent-reasons {channel {:reason keyword :paths [...]}}
   :support-typed-rank Nat
   :would-rank-differently boolean
   :status :measured}]
 :comparison
 {:candidate-pairs Nat
  :incomparable-support-pairs Nat
  :ranking-comparable? boolean
  :winner-changed? boolean|nil}}
```

The evaluator emits an additive decomposition of the already-computed
controller score into exact per-observation-channel terms plus a non-channel
residual. At the trace boundary, the shadow retains only terms whose channel
is `:observed` in the exact persisted observation envelope. A measured zero is
supported; a reason-bearing absence contributes no channel term. The residual
retains intrinsic, graph, goal-outcome, capability-zone, and other terms that
are not claims about an observed channel.

The full-support decomposition reconstructs the live score within `1e-12`.
This is a decomposition of the existing evaluation, not a second evaluation,
so it does not introduce another representation of scoring inputs.

Comparisons occur only when all candidate supports are equal. Unequal support
is counted under `:incomparable-support-pairs`; no shadow ranking or winner is
reported for that record. Nothing reads the shadow into `policy/select-action`,
and `:authority :shadow-only` makes that boundary explicit.

## Current measurement

Canonical census:

```
bb -cp . checks/absence_scoring_counterfactual.clj
```

Against corpus SHA-256
`1467970bf94f486b35844af98f2bbb9181e726106b85cce368591ed66ea2fda7`:
54 files, 801 records, 105,277 ranked candidates; **0 observation envelopes,
0 shadow records, 0 classifiable candidates**. Therefore candidates with
absent channels, incomparable pairs, rank changes, and winner changes remain
**unknown**. The shadow measures only ticks written under schema v18. It does
not silently generalise from test fixtures or reconstruct historical zeros.

## Controls and gates

- An absent `:loop-health` fixture removes its term and changes the shadow
  winner while the persisted live decision remains byte-for-byte unchanged.
- A measured zero fixture retains `:loop-health` in support and reconstructs
  the current score.
- `clojure -X:test :nses '[futon2.aif.efe-test futon2.aif.trace-test]'`:
  64 tests, 218 assertions, 0 failures/errors.
- `bb -cp . checks/preemptive_absence_coercion_lint.clj`: expected live exit 1,
  exactly 15 findings; this delivery adds no default.

No scoring, ranking, policy selection, or actuation behavior changes in C108.
Whether to switch remains an operator decision after post-v18 evidence exists.

## Automatability

The implementation unit scores 7/7: input/output types and consumer are named;
acceptance and executable falsifiers precede use; historical absence is loud;
the shadow is additive and reversible; and incomparable or unavailable cases
are explicitly refused rather than guessed.
