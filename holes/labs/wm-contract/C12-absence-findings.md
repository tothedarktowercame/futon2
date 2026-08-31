# CLEANUP C12 — absence census findings (2026-08-31)

Automatability score: **6/7**. Ports and the tagged EDN output are typed;
acceptance, falsifier, consumer, pinned reads, and reversible blast radius were
named in advance. Criterion 7 is intentionally refused at the sixteen live
consumer sites: pass-through versus refusal changes ranking or update semantics,
so each remains `:blocked-on` instead of receiving an invented default.

The semantic census found **27 input-boundary sites**, not four. Seventeen
unsafely turn a missing measurement or score into a legitimate value; nine are
declared model/configuration or algebraic defaults; one is a trace compatibility
default. The machine-wide defect is concentrated but real:
observation produces the first zero, then free-energy, policy, precision, and
belief diagnostics independently default again.

The safe fix is deliberately narrow. `observe` still returns the identical
14-number map, but it attaches a tagged `:observed`/`:absent` status for every
channel. `observation-envelope` makes that tag an ordinary EDN value so it
survives persistence. The regression proves that measured zero and absent input
remain numerically equal for legacy consumers but are distinguishable in the
tagged form, including after an EDN round trip.

This does **not** make the downstream computations correct. Fifteen sites are
recorded as blocked because changing them to refuse or pass through absence can
change policy ranking, precision, or action generation. Producer tagging without
consumer uptake would be cosmetic, so the census names the required consumer at
each site.

Recorded results are potentially affected and cannot be repaired retrospectively:
old traces persisted only numbers, so a zero measurement and an absent source
are observationally identical. The honest follow-up is a versioned tagged trace
envelope and a new-tick comparison, not rewriting old results. This warrants its
own evidence/paper queue item before any historical mean, ratio, or policy-effect
claim treats zero as measured.

`sec-catalog.tex` does need a later gate amendment at R2 and R7: R2 must promise
presence provenance in addition to normalization, and R7 must require tagged
absence to pass through rather than update precision. I did not edit the paper in
this delivery because the consumer migration and post-fix evidence do not exist
yet; claiming the stronger pattern now would outrun the implementation.
