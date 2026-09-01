# C265 — parameter-prior kernel binding

Date: 2026-09-01

The independent fixture gives deterministic parameter-prior rows for two
policies. Lean rejects both a well-formed predictive-outcome `Q(o|pi)` and an
unconditioned policy-habit `Q(pi)` as parameter prior `Q(theta|pi)`. This is a
fourth live notation collision: the two conditional Qs even share the policy
domain, but their codomains name different objects.

## Closure

Only evidence metadata changed; the parameter-prior carrier and all Q-facing
definitions retain their bodies. Q semantics are unchanged. The `:lean-spine`
pin is refreshed as part of this delivery. `ParameterPriorKernel` already has
the explicit `:scores` model area; registry regeneration and the staged paper
build verify classification closure.
