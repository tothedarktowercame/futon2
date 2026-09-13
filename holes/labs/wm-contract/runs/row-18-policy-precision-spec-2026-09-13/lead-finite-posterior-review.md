# Canonical finite posterior review — codex-26, 2026-09-13

Reviewed mathlib4 b57a10de and futon2 0825dd16/7c1eb0d6. All nine
source/toolchain/spec pins plus three owned olean pins match actual bytes;
see lead-finite-posterior-pins.json. No proof rerun. Retained final elaboration
reports ten axiom checks, exit 0, no sorryAx; intermediate failures retained.

Accept canonical list equality over occurrence indices, positive normalizer,
nonnegative normalized weights, positive-beta continuity, finite expectation
range bound and their conditional root-order application. Source inspection
confirms pi0 zeroes only F_pi, keeps the same habit/G/beta, and preserves occurrence
multiplicity. Imported proposal/positive-domain definitions now have compiled
correspondence proofs. Mathematical normalization allows totalized Real.log;
probabilistic application must retain positive-habit admissibility explicitly.
No proof of global root uniqueness or floating evaluation is hidden in this result.

Next analytic packet: a sufficient uniqueness criterion without changing any
retained field/prior. Let R = hi-lo >= 0. Both expectations lie in [lo,hi], so
all roots lie in [c-R,c+R]. For fixed softmax field the derivative of expected G
with respect to beta is Var(G)/beta^2. Thus delta' <= R^2/(4 beta^2) using
nonnegative baseline variance and the bounded-variable variance bound. If
c > 3R/2, every candidate root is above R/2 and beta-delta(beta) is strictly
increasing throughout the root-containing interval. Together with continuity
and endpoint signs this yields exactly one positive root. This derivation is
lead mathematical reasoning, not yet a Lean theorem or a field measurement.
A sharper justified criterion is welcome, but failure of this sufficient bound
means unknown, not multiple roots. Do not tune c or clip G to make it pass.
No prior adoption, runtime integration or node admission from this review.
