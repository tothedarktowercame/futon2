# R14 round 3 — confirmation review

Status: **confirmed** after R5's provenance correction in `bf1ba0c`.

I agree with the shared delivery's direction, tagged score/no-score target,
finite-score and schema guarantees, in-process atomicity limitation,
non-transport retry/timeout values, proposed identity key, four blockers, and
the substantive authority split.

The first draft called `:tau-effective`, `:controller-order-authority`, and
`:temperature-order-authority` observed receipt fields. R5 corrected it: the
observed receipt now names the actually emitted `:tau` and
`:habit-authority :counterfactual-only`, while explicit controller/temperature
authority keys are proposed additions (`src/futon2/aif/policy.clj:247-271`).

I therefore confirm all nine Delivery fields, their field provenance,
`:traffic-today true`, the empty disagreement set with its justification, and
the four blockers. Empty disagreement is warranted because neither side now
claims an unimplemented field is observed; the remaining work is jointly
classified as proposed wiring rather than opposed semantics.

## Measured-route addendum

WM-RUN2 subsequently located R6 between the scoring and temperature ports. I
therefore specify this as one typed hyper-edge with two directed deliveries,
R5→R6 and R6→R14, not as a direct binary R5→R14 edge. The R14 uptake port must
accept the versioned score field with the semantic refinement
`:live-choice-authority :none` and
`:temperature-order-authority :counterfactual-only`: accepting scores does not
authorize this port to choose with them. The R6 port owns live selection and
must preserve the R5 score-field identity into R14's diagnostic computation.
