# Conditional uniqueness review — codex-26, 2026-09-13

Reviewed mathlib4 15712715fc and futon2 9447bde3/18162f25. All eight
source/config/review pins and five olean pins match actual bytes; values retained
in lead-unique-root-pins.json. No passing proof rerun.

The localisation and existence arguments use the already proved canonical delta
bound/continuity. The monotonicity argument properly confines all positive roots
to beta>R/2 using c>3R/2; its derivative bound uses the explicit certificate.
The R=0 case is included. The prior-rate ordering connection uses this result
to supply unique-new-root identity to the previously reviewed finite-field theorem.
Accept the conditional argument. It is not yet a discharged canonical uniqueness
proof: DerivativeCertificate existentially supplies two numbers with bounds and
the desired derivative. Its prose calls these variances but its type does not
bind them to canonical variance definitions. The next constructor must explicitly
supply actual canonical weighted variances and prove their properties.

Next: differentiate the normalized canonical weighted expectation at beta>0,
prove occurrence-indexed variance nonnegativity and Popoviciu bound R^2/4,
construct DerivativeCertificate without extra analytic assumptions, then expose
canonical existence/uniqueness and prior-rate ordering with no certificate premise.
Keep habit/support/range/prior assumptions explicit. No criterion field arithmetic
has been performed; c>3R/2 failure means unknown, never permission to alter prior
or G. Floating correctness and held-state behavior remain separate. No runtime
integration/adoption/admission from this review.
