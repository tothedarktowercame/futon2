# CLEANUP C32 — inputs to the variance law (2026-08-31)

All three inputs are now recorded in `belief-variance-inputs.edn`, whose named
consumer is C31. This does not implement or strengthen `beliefUpdate`.

The corpus cannot estimate a sensor-noise floor: its residuals mix sensor noise,
model error and bias, and historical absent observations coerced to zero. The
observed fifth-percentile squared residual ranges from approximately `1e-6` to
`0.13` across channels, which is evidence against relabelling any one empirical
quantile as sensor noise. The record therefore declares the existing normalized
channel floor `0.01` as a default and permits no channel overrides until repeated
measurements have an adjudicated latent target.

Likewise, irregular trace intervals and absence of a target posterior do not
identify an EMA rate. The declared default is `2/(20+1) = 0.095238…`, the
conventional EMA equivalent of the live precision tracker's twenty-sample
window. Its consequence is explicit: a roughly twenty-observation memory, not a
twenty-tick or elapsed-time promise.

The provenance mapping is a named conservative decision:

- `:independent` receives multiplier `1.0`.
- `:self` receives multiplier `0.25`: admitted and processed, but four equally
  precise self reports can contribute at most one independent report before
  correlation controls. This is a default policy, not a corpus estimate.
- `:unknown` has no numeric multiplier. It is the tagged absence
  `{:variant :absent, :consumer-action :pass-through-with-loud-absence}` and
  cannot update mean or variance until provenance is supplied.

This settles the choice without collapsing unknown into self. The check rejects
the tempting mutation `:self = :independent`, and also requires unknown to remain
non-numeric. C31 may now implement and falsify the recorded rule in a separate
delivery.
