# Row 16 batch A — retained-input audit and split

Date: 2026-09-12. This is a refusal to construct an admission witness from
insufficient or circular reference material. No production behavior changed.

## R4 `:forward-model` — needs an independent exact reference

`runs/row-6-predictive-outcome/readback.clj` calls the actual
`futon2.aif.machine-predictive/predictive-outcome-kernel`, reconstructing its
inputs through the pinned row-9 input. However, it constructs every comparison
as `{:production row :lean-reference row}`. Consequently
`runs/row-6-predictive-outcome/readback.edn` retains no independently computed
Lean reference value.

The retained production decimals also cannot directly instantiate
`DarkTower.WarMachine.Holes.PredictiveOutcomeKernel`: the exact decimal sum for
`advance-twice` is `1.00000000000000005`, and for
`advance-then-cascade` it is `1.00000000000000006`, whereas the Lean carrier's
`normalised` field requires exact equality to one. Production reports `1.0`
because IEEE addition rounds the accumulated value. Contract v1.1 permits a
typed `:float-carried` production row; it does not permit silently replacing
the exact Lean normalization theorem or declaring the production bytes to be
their own Lean reference.

Required capture/reference repair: retain the row-9/model inputs unchanged,
derive independent exact/rational Lean reference masses from those inputs,
and record the production-vs-reference deltas and `:float-carried` admission
explicitly. The model-revision and missing-support controls are already
retained and can be reused.

An attempted witness was committed in mathlib4 at `c86eed2bba`; its first and
only elaboration exited 1, including failure of the exact normalization goal.
The non-elaborating files were removed by a follow-up commit; the failed commit
remains the execution history and was not amended.

## R5 `:risk` — Q/C pair is not retained and positivity is false

`runs/row10-machine-preference-2026-09-12/production-match-witness.edn`
retains C only. It does not retain a complete predictive Q row for a finite KL
calculation. Its negative control records only the typed boundary
`Q-positive/C-zero -> :risk :infinite`.

Moreover the retained C has seven named zero masses. The real-valued Lean
declaration `DarkTower.WarMachine.Holes.predictiveOutcomeRisk` requires strict
positive C mass on every member of Q support. No retained Q support and proof
of that premise exist. The production module exposes
`ruled-outcome-c/unsupported-risk`, which classifies one boundary pair; it does
not compute the full categorical KL. `node_sim/kl-divergence` is a simulation
harness, not the production tick function requested by this packet.

Required capture/implementation seam: retain a complete Q/C pair from the
eventual row-14 consumer, including support and revisions, and call a reviewed
production categorical-risk function that returns either all KL terms and a
finite total or the typed infinite/unsupported outcome. No epsilon is legal.

## R5 `:expected-free-energy` — retained triple is output-only

The pinned `data/wm-trace/wm-trace-2026-09-04.edn` form retains `:G-risk`,
`:G-ambiguity`, `:G-core`/`:G-efe`, and the larger `:controller-score` context.
In production, `src/futon2/aif/efe.clj` constructs `:G-core` and `:G-efe` with
inline `(+ g-risk g-ambig)` inside the full action scorer. There is no
production function accepting the retained two scalars as inputs. Re-adding
two output fields in a witness script would not call the production function,
and rerunning the full scorer is impossible from that trace because its full
input context and revisions are not retained.

Required capture/seam: either retain the complete input map for the actual
production scoring call and replay it, or extract a reviewed production EFE
composer and retain the exact risk/ambiguity inputs at birth. Then compare its
result to `DarkTower.WarMachine.Holes.expectedFreeEnergy` while keeping the
multi-objective `:controller-score` outside the claim.

## Batch result

All three requested admissions split to prerequisites. No witness-carrier
fragment or verification receipt was created, because each would overstate the
available evidence. The inventory's earlier `provable-now` classifications for
these three rows are superseded by this function-signature and exact-reference
audit.
