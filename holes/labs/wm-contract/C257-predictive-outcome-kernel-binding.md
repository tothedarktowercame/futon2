# C257 — predictive outcome kernel binding

Date: 2026-08-31

The independent fixture contains two deterministic policy rows: inspect
predicts `clear`, repair predicts `fixed`, and each row has mass one. Lean
rejects both an unconditional outcome distribution and a softmax vector over
policies as `Q(o|pi)`. This guards the duplicate `Q` notation by type.

## Closure

Only evidence metadata changed; the predictive kernel carrier and all Q
consumers retain their definitions. Q semantics are unchanged and the
`:lean-spine` pin is refreshed to
`8e69786956a834320e7c334351504cfa5c1f43110a232cf1ab1a1b8e48f4a0ec`.
`PredictiveOutcomeKernel` already has the explicit `:scores` model area; the
regenerated registry and staged paper build verify classification closure.
