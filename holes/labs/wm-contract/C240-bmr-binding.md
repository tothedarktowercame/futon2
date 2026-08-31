# C240 — Bayesian Model Reduction binding

Date: 2026-08-31

The independent fixture derives the accumulated counts as `A-a = [9,3]`.
Replacing the prior `[1,1]` with `[1,1/100]` therefore forces the reduced
posterior `[10,301/100]`.  The falsifier uses `[10,401/100]`, which adds the
new prior without removing the old one and does not preserve the evidence.

## Q-interface closure

The Lean change adds evidence/falsifier metadata and a witnessed-contract row
for `bayesianModelReduction`; its formula is unchanged.  Inspection confirms
that `PredictiveOutcomeKernel`, its risk and EIG consumers, and the missing
machine-Q construction are unchanged.  The Q-interface semantics therefore
remain unchanged, and the `:lean-spine` content pin was refreshed to
`d66c31e4d1b1c42cb57c57bbd5f89ef7d95a64b5c098ca50849ad9329c7097b0`.
