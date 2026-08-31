# R7 round 1 — precision over prediction errors (independent, pre-R2)

VERBS-D1, R2→R7 pairing. Node R7 (zai-6). Written before reading any R2 seat file.
All claims file:line-observed unless marked **proposal**.

## Who I am

- R7 = history-tracked precision. Live implementation: `futon2.aif.precision`.
- In the judge loop, R2's observation is diffed against predictions into `raw-errors`,
  then I run: `precision/update-precision-state prec-state raw-errors
  {:salience-mode (arena-salience-mode)}` at `scripts/futon2/report/war_machine.clj:4323-4325`,
  route-tagged :R7 at `:4389` (`futon2.aif.precision/update-precision-state`).
- `update-precision-state` recomputes each touched channel's precision from inverse
  rolling error variance, bounded floor/cap; preference-gap emitted separately as
  :salience, never production precision (`src/futon2/aif/precision.clj:147-173`).
- My outputs: `precision-for` (:175-179, default 1.0), `salience-for` (:181-190,
  untracked→0.0), `weighted-error` re-weighting error maps with my precision
  (:192-199). Consumer downstream: `weighted-errors` → R3d aggregation → belief
  events (`war_machine.clj:4327-4360`).
- Census: edge [:R2 :R7] derived AND measured ("triangulated with node-sim"),
  NOT drawn, NO schema (`holes/labs/wm-contract/edge-census.edn`, rows + measured-notes).

## What I receive from R2 today (observed)

- Payload: `observation`, the map returned by `obs/observe scan-data`
  (`war_machine.clj:4162`, tagged :R2 at `:4163`). 13 channels in [0,1]
  (`src/futon2/aif/observation.clj:15-74`; enforced by `observation_test.clj`
  per `docs/futon-aif-completeness.md:72-78`).
- But I do NOT receive it directly. Between us sits R3a error computation:
  `(get observation ch 0.0)` per channel of `belief/channels-with-likelihood`
  (4 channels: `src/futon2/aif/belief.clj:913-926`) at `war_machine.clj:4316-4320`.
  So 9 of 13 R2 channels never reach me, and an absent channel reads as 0.0 —
  the exact absence-as-value defect flagged four times in PAIR-R5-R14.

## How I need to develop

1. **Typed per-channel input, not defaulted numerics.** My error-map reads
   `(:error error-map 0.0)` / `(:observed error-map 0.0)`
   (`precision.clj:166-167`): a missing reading and a true 0.0 are
   indistinguishable. Prevents honest precision today.
2. **Untouched-channel semantics are silent.** Channels absent from errors pass
   through unchanged (`precision.clj:170-172`) — good, but neither producer nor
   consumer can tell "no likelihood model" from "channel dropped".
3. **No receipt.** Nothing tells R2 (or the trace) which channels I updated,
   initialized from defaults (`precision.clj:163-165`), or passed through.
4. **First-contact initialization is undocumented at the edge**: a channel with
   no prior state gets `default-initial-precision` and empty history
   (`precision.clj:163-165`) — precision 1.0 on first sight is a prior, not a
   measurement, and the edge contract should say so.

## What I need from R2 / can give

- **Need:** a declared channel set per tick — which channels are present, which
  are intentionally absent — so absence is a tagged value, not 0.0.
- **Need:** confirmation whether R2 considers the 4-likelihood-channel subset
  (belief.clj:926) part of R2's own schema or an R3a projection. It is currently
  defined in R3's file, not R2's.
- **Give:** a receipt shape `{channel, prior-state-existed?, new-precision,
  pass-through?}` — proposal; cheap to emit from `update-precision-state`.
- **Give:** the bound contract (floor/cap, variance-only precision, :salience
  separate, precision.clj:150-154) as my guarantee on everything I do update.

## Surprise (for the bell)

The R2→R7 traffic is real but routed through R3a's channel filter; R7 sees at
most 4 of R2's 13 channels, twice-defaulted (observation-side `get ... 0.0` at
war_machine.clj:4319, then error-side `:error ... 0.0` at precision.clj:166).
