# C157 — kernel-derived ambiguity

Date: 2026-08-31

`ambiguity` is now the expected Shannon entropy of the observation model:
each predicted hidden-state mass weights the entropy of that state's
observation-kernel row.  It uses the normalized finite-support kernel carriers
already introduced for AIF; zero mass follows the standard `0 log 0 = 0`
convention supplied by `Real.log 0 = 0`.

`expectedFreeEnergy` remains parameterized by an ambiguity estimator.  This is
intentional: operational callers may use approximations, but the seam is now a
declared choice and its canonical AIF implementation is the named `ambiguity`
definition rather than an unconstrained function with no referent.

The independent fixture uses the Shannon identity
`H(delta_x) = -(1 * ln 1) = 0` nats.  Its negative control changes the recorded
entropy to one and is rejected.
