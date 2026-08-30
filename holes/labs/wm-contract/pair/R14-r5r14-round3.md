# R14 round 3 — confirmation review

Status: **confirmation withheld pending one provenance correction**.

I agree with the shared delivery's direction, tagged score/no-score target,
finite-score and schema guarantees, in-process atomicity limitation,
non-transport retry/timeout values, proposed identity key, four blockers, and
the substantive authority split.

One field is not yet honest enough to confirm. Under `:receipt :observed`, the
draft names `:tau-effective`, `:controller-order-authority`, and
`:temperature-order-authority`. The running result emits `:tau`, not
`:tau-effective`, and emits no explicit controller/temperature authority keys
(`src/futon2/aif/policy.clj:259-271`). It emits
`:habit-authority :counterfactual-only`; controller-live versus
temperature-counterfactual is our agreed interpretation of `chosen` and
`habit-order`, not an observed field (`src/futon2/aif/policy.clj:247-253`).

Required correction in the R5-owned shared artifact:

- observed receipt: `:tau`, `:habit-authority`, chosen action, score, gain,
  spread temperature, and selection boundary;
- proposed target receipt: `:tau-effective`,
  `:controller-order-authority :live`, and
  `:temperature-order-authority :counterfactual-only`.

After that move, my status is **confirmed** with no semantic disagreement. If
it is not moved, the disagreement is R5's position “those authority keys are
observed receipt fields” versus R14's position “they are agreed target fields,”
unresolved because the cited running map does not contain them.
