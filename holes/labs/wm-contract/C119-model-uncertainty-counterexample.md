# C119 — posterior spread is not canonical EIG

Date: 2026-08-31

The proposed unconditional identity is false.  The live engineering bonus is
`Σ_c sd(A_c)`: a sum over current posterior standard deviations, with no policy,
predicted outcome, or simulated posterior-update input.  Canonical EIG is the
policy-conditioned expectation of posterior-to-prior KL.

The Lean counterexample uses one policy, one outcome, and one parameter.  Its
predictive, prior, and posterior kernels are normalized point masses; prior and
posterior are identical, so canonical EIG is `KL(δ || δ) = 0`.  Supplying the
positive posterior-spread list `[1]` makes `modelUncertaintyBonus = 1`.
Therefore the quantities differ in a fully normalized carrier family.

This does not say posterior spread is useless.  It settles the narrower and
load-bearing question: no unconditional theorem identifies the live bonus with
canonical EIG.  Any equality would need assumptions strong enough to connect
the spread inputs to policy-conditioned outcome and update kernels; the live
implementation has no such inputs.  The former refusal is consequently closed
by counterexample, not by promoting the engineering bonus.
