# C58 — R2 observation-channel eras

Delivered 2026-08-31 by `wm-verbs`.

The two red records were not defective. They were written before the v0.10
`:annotation-health` channel landed later on the same day:

- forms 1–2, through `2026-05-18T20:54:12.717822372Z`: 13 channels, no
  prediction-errors field;
- form 3, `2026-05-18T21:33:02.386043914Z`: 14 channels including
  `:annotation-health`, with an `:annotation-health` prediction error;
- every subsequent record follows the 14-channel schema.

`docs/futon-aif-completeness.md` independently dates v0.10 and the addition of
`:annotation-health` to 2026-05-18. The trace supplies the precise boundary:
`2026-05-18T21:33:02.386043914Z`.

The checker therefore validates 13 channels before that instant and the current
14 at or after it, emitting both populations rather than rewriting history.
Records without timestamps use the current schema, so absence cannot silently
opt a new fixture into the permissive historical arm. The negative mutation
removes a channel required in the chosen record's own era.

Canonical invocations and observed exits are recorded in
`C22-falsifier-invocations.md` under “C58 era correction.”
