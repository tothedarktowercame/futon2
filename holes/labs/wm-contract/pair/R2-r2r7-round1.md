# R2 round 1 — R2→R7 edge (node R2: observation)

Adapted from PAIR-R5-R14.md. All claims observed at file:line unless marked *proposal*.

## What I am
- R2 = `futon2.aif.observation/observe`, stamped on the live route at
  `war_machine.clj:4163` (`route-tag route0 :R2 "futon2.aif.observation/observe"`).
- I normalize scan-data into per-channel scalar observations
  (`observation (obs/observe scan-data)`, war_machine.clj:4162).
- The census calls R2→R7 "derived, measured (triangulated with node-sim), sim,
  not drawn" (`edge-census.edn` rows `[:R2 :R7]`, note at measured-notes). It
  carries live traffic today inside `judge`.

## What actually flows R2→R7 (observed)
- Consumer loop: `raw-errors` built per channel over
  `belief/channels-with-likelihood` (belief.clj:913) with
  `fe/compute-prediction-error (get observation ch 0.0) predictions`
  (war_machine.clj:4316-4322). Each error map carries
  `{:observed :predicted-mean :predicted-variance :error :weighted-error :precision}`
  (free_energy.clj:82-107).
- R7 = `precision/update-precision-state prec-state raw-errors
  {:salience-mode (arena-salience-mode)}` (war_machine.clj:4324-4326;
  route stamp `:R7 "futon2.aif.precision/update-precision-state"` at :4389).
- R7 consumes only `:error` and `:observed` from my payload
  (`(:error error-map 0.0)`, `(:observed error-map 0.0)`, precision.clj:147-173,
  update-channel-precision at :103).
- R7's `:observed` use is the need/salience term via `preference-gap`
  (precision.clj:84-100); production `:salience-mode :separate`
  (arena-salience-mode returns `:separate`, war_machine.clj near :331) keeps
  salience out of Π (precision.clj:127-135).
- Return leg: `precision/weighted-error` re-weights my error maps with
  history-tracked Π and preserves per-call precision as `:per-call-precision`
  (precision.clj:192-210; called war_machine.clj:4327-4330).

## How I need to develop (each: what prevents it now)
1. Absence→0.0 on my side: `(get observation ch 0.0)` (war_machine.clj:4319-4321)
   lets a channel my scan did not produce enter R7 as a real observed 0.0.
   Prevention: R7's channel set is fixed by `channels-with-likelihood`, not by
   what I actually emitted. Need a tagged `{:absent ...}` variant, not a default.
2. No schema on my observation map: `observe` output is convention, not
   validated. R7's uptake validator is only the double-coercion in
   precision.clj:103-145 — silent on shape.
3. My values are per-channel scalars with no unit/provenance metadata, so R7's
   need-scale 5.0 (precision.clj:52) assumes a magnitude regime I never declare.

## What I need from R7 / what I can give
- Need: R7 to tell me which absence encoding it can consume without coercing
  (its `(:error error-map 0.0)` default, precision.clj:162, is the same
  absent→0 defect on the error side).
- Need: R7's windowing bound (20, precision.clj:42) and floor/cap
  (:53-54) documented as part of the edge contract, since they define how much
  my history matters.
- Give: a typed payload `{ch {:observed double | :absent reason}}` (*proposal*)
  and a stable channel vocabulary (`channels-with-likelihood`, belief.clj:913).
- Give: observed behavior — my error maps already reach R7 carrying
  `:predicted-variance`/`:precision` that R7 ignores (it computes its own Π).

## Open facts for R7 to confirm/deny
- Does R7 see any channel NOT in my observation map today (i.e., silently at
  0.0)? Observed risk at war_machine.clj:4319-4321.
- Is the trace round-trip (`:precision-state` in prev-trace-record,
  war_machine.clj:4289-4292) the durable part of this edge's guarantee?

— R2 (zai-5), round 1.
