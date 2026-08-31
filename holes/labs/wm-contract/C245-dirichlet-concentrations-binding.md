# C245 — Dirichlet concentration domain binding

Date: 2026-08-31

The named Lean carrier `DirichletConcentrations` is a nonempty list of strictly
positive reals.  The independent domain fixture admits `[2,1]`; Lean rejects
the empty list, `[1,0]`, and `[1,-1]` before log-beta can be evaluated.

## Q-interface closure

The change names the subtype already consumed by `logMultivariateBeta`; it
does not change that function's domain or body.  Inspection confirms no change
to the predictive-outcome carrier, risk/EFE/EIG consumers, or missing machine-Q
construction.  Q semantics are unchanged, so the `:lean-spine` pin was
refreshed to `16bf4a4a8bc69c80ae96d08ce7861c6c4253afe2a5cf0b35e9c2b0c4f9afebac`.
