# C252 — preference distribution binding

Date: 2026-08-31

The independent fixture is a fair binary preferred-outcome distribution,
conditioned only on `Unit`.  Its two masses are each `1/2`.  Lean separately
rejects a well-formed state-conditioned likelihood and the vertex-local
pragmatic cost function `C`; neither is the canonical preference distribution.

## Closure

The change adds evidence metadata only; `PreferenceDistribution` and every
Q-facing definition retain their bodies.  Q semantics are unchanged and the
`:lean-spine` pin is refreshed to
`d787a015ec7ecb92e0b9ff80c4a0e9f43305c39ba4f5def273920a94c3cb100c`.
The declaration was already classified under `:preferences`; regeneration and
the staged paper build verify that model-area closure remains complete.
