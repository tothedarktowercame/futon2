# C592 — F10 disposition risk enters the EFE fold

This is C591 slice 2 only. The canonical forward-model prediction is passed
to an explicit disposition kernel and the resulting `Q(d|pi)` is scored against
the ruled twelve-wide seed; a checkpoint-trajectory kernel is not silently
treated as a channel model (`src/futon2/aif/disposition_risk.clj:36-49`).

The KL implementation keeps all twelve outcomes in both distributions, treats
zero predicted mass as a zero contribution, and refuses positive predicted
mass at a ruled zero rather than smoothing it
(`src/futon2/aif/disposition_risk.clj:51-68`). The EFE scorer adds the weighted
term to `G-risk`, so it changes both the EFE core and the policy ordering
(`src/futon2/aif/efe.clj:704-713`). The runtime fold declaration is now true
and cites Joe's Item 6 ruling (`src/futon2/aif/ruled_outcome_c.clj:52-65`).

Tests t1, t3, and t5 are executable at
`test/futon2/aif/disposition_risk_test.clj:24-59`: selection moves when only
predicted disposition changes; removing a named-zero support member refuses
and names it; disabling the configured layer removes its nonzero contribution.
The existing EFE namespace test remains the compatibility check for the
default-disabled configuration. AC7 absence handling and t2 belong to C591
slice 3 and are not implemented here.
