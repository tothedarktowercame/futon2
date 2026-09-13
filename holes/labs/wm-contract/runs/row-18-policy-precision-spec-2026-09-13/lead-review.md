# Interoceptive beta proposal review — codex-26

2026-09-13. Reviewed `d48d6843`, receipt `e6f7ae8b`, and pinned Lean
proposal `779027d3eeaf887d67d0f29cb89e793e2ad95805880c9d04bb8beb6f164d0298`.
Source and MachineTemperature hashes match. Existing Lean execution not rerun.
Disposition: prior-rate algebra is useful; proposal not yet adopted or ready
for production implementation.

The explicit distinction between half prior mean and posterior gamma is
correct. Zero-factor reduction, positivity, and reciprocal identities have the
stated narrow algebraic scope. The :both habit placement is supported by the
September-9 ruling at `aif-equations.edn :choices :pi-zero-form`.

## The unrestricted-delta counterexample is not a policy-field counterexample

`delta(beta)=2*beta-3` proves a fact about an unrestricted implicit equation.
For the actual mathematical policy family at a fixed finite field,
`delta(beta)=E_pi[G]-E_pi0[G]` is bounded by `max(G)-min(G)` because both
weights are probability distributions. The supplied affine delta is unbounded
on positive beta and has not been represented by any such policy field.
Do not use this counterexample to assert that the unique-root policy family
can reverse the prior ordering.

There is a stronger conditional theorem to investigate. Write
`f(beta)=beta-delta(beta)`. Let a positive baseline root b1 satisfy f(b1)=c1,
and increase the prior rate to c2>c1. For continuous bounded delta, f eventually
exceeds c2; since f(b1)<c2, the intermediate value theorem yields a c2-root
strictly above b1. If the new positive root is unique, it must therefore be
larger than b1, and reciprocal gamma is strictly smaller. This argument does
not assume a derivative bound or uniqueness of the baseline root.

Prove the bounded-continuous conditional theorem and its finite-distribution
bound in a new Lean module if feasible. Distinguish that exact mathematical
model from floating Clojure execution. If any stated hypothesis fails for the
real mathematical softmax family, exhibit that failure precisely.

## Root evidence remains an implementation prerequisite

The existing solve's bracket and convergence do not certify a unique positive
root. Specify the needed root/branch evidence and numerical correspondence;
a finite scan for sign changes is not a global uniqueness proof. An honest
conditional theorem is progress, but no field is licensed by asserting its
hypotheses. Preserve held-state refusals, fixed non-trip inputs during
restoration, and source identity through both arms.

No empirical calibration or observation likelihood has been supplied; the
prior-rate intervention remains a declared model hypothesis. No runtime
integration, qualified edge, or operator evidence follows from this review.
