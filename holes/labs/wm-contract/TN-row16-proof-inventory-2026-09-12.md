# Row 16 measurement-proof inventory — 2026-09-12

## Method and scope

This inventory treats every entry in `aif-equations.edn` having all of `:node`,
`:defines`, and `:lean` as equation-bearing: 18 rows. The short declaration in
`:lean` is resolved through the machine rows in
`variable-situation-accounting.edn` and the corresponding Lean source; no name
is inferred from prose. “Admitted” means an `:admitted` node claim in the
generated `checks/witness-registry.edn`, not merely a fixture theorem or a
source-module production-match witness. “In flight” is limited to the four
declarations explicitly assigned to the unfinished part of WORK-REMAINING row
15. Everything else is `NO PROOF` for carrier-admission purposes.

Authority pointers: `aif-equations.edn:104-219` (equation rows),
`variable-situation-accounting.edn:1313-2090,3127-3335` (generic and machine
declarations), `checks/witness-registry.edn:727-2776` (currently admitted R1,
R2, R3a and R7 claims), `WORK-REMAINING.md:151-174` (rows 12-16), and
`production-caller-manifest.edn:1-20` (the narrow caller census; it names only
ruled outcome C, prediction error, and predictive outcome and therefore is not
evidence of callers for the omitted equations).

## Inventory

| Node · equation · authority-bound Lean declaration | Measurement-proof status today | Identical-pin reconstructibility for `NO PROOF` | F8 witness shape |
|---|---|---|---|
| R2 · `:observe` · `DarkTower.WarMachine.MachineObservation.machineObservation` | **ADMITTED** — `R2-observe-emptyObservationHasFourteenZeros-v1` (`checks/witness-registry.edn:1906-2000`). | — | Retained production-reader input, fourteen-coordinate readback, and exact Lean reference vector. |
| R3a · `:prediction-error` · `DarkTower.WarMachine.MachinePredictionError.machineChannelPredictionError` | **ADMITTED** — 13 claims from `R3a-prediction-error-basicTriple-v1` through `R3a-prediction-error-bothModelMembersNamed-v1` (`checks/witness-registry.edn:855-1905`). | — | Captured channel observation/model members, production call, coordinate result/refusals, and equal Lean arithmetic. |
| R7 · `:precision` · `DarkTower.WarMachine.MachinePrecision.machinePrecision` | **ADMITTED** — 10 claims from `R7-precision-oneStillError-v1` through `R7-precision-floorIsReached-v1` (`checks/witness-registry.edn:2001-2776`). | — | Captured error history, production precision calculation, boundary controls, and Lean values. |
| R8 · `:free-energy` · `DarkTower.WarMachine.Holes.variationalFreeEnergy` | **NO PROOF — needs capture.** | `data/wm-trace/wm-trace-2026-09-04.edn` retains `:free-energy` output and `:prediction-errors`, but not the identical per-channel precision/error pair passed to this declaration (the generic fixture is explicitly non-run evidence: `variable-situation-accounting.edn:1819-1847`). Retain the exact ordered precision and error maps plus implementation revision. | Call the production variational-F function on that captured pair, compare every channel term and total to a Lean theorem, and retain malformed/support controls. |
| R8 · `:policy-free-energy` · `DarkTower.WarMachine.MachinePolicyFreeEnergy.machinePolicyFreeEnergy` | **NO PROOF — needs capture.** | `data/wm-trace/wm-trace-2026-09-04.edn` retains ranked scores and `:f-pi-dark-readback`-related trace material, while `runs/F8-policy-free-energy/` is fixture/readback evidence without a pinned real call input. Missing are the identical ordered `PolicyChannelReading` inputs, their provenance, and implementation revision. | Capture the actual ordered channel readings for one real policy, invoke production F-pi, compare all terms/total to Lean, and induce absent/foreign-channel refusals. |
| R3 · `:belief-update` · `DarkTower.WarMachine.MachineBeliefUpdate.machineBeliefUpdate` | **IN FLIGHT / capture-blocked under row 15** (`WORK-REMAINING.md:157-169`). | — | After retaining pre-row, attributed per-entity events, A/B bytes or hashes and revision, mode, and post-row, replay the actual update and compare every coordinate to Lean. |
| R1 · `:belief-state` · `DarkTower.WarMachine.MachineBeliefState.machineBeliefState` | **ADMITTED** — `R1-belief-state-carried-trace-coordinates-v1` and `R1-belief-state-bootstrapped-trace-coordinates-v1` (`checks/witness-registry.edn:727-854`; row-15 admission noted at `WORK-REMAINING.md:169-172`). | — | Pinned real `:mu-post`/`:mu-pre` row, production-facing reader, seven coordinate deltas, and exact Lean decimals. |
| R4 · `:forward-model` · `DarkTower.WarMachine.Holes.PredictiveOutcomeKernel` | **NO PROOF — provable now.** | `holes/labs/wm-contract/runs/row-6-predictive-outcome/readback.edn` pins the real model/policy snapshot and production readback used by the completed row-6 witness (`WORK-REMAINING.md:83-91`); its source/model pins can be carried unchanged into a node-witness receipt. | Reuse the row-6 capture, call the actual predictive-outcome constructor, compare every policy/outcome mass to Lean, and retain model-revision/support refusals. |
| R5 · `:risk` · `DarkTower.WarMachine.Holes.predictiveOutcomeRisk` | **NO PROOF — provable now.** | The identical Q and C pins and every tagged mass are retained in `holes/labs/wm-contract/runs/row10-machine-preference-2026-09-12/production-match-witness.edn` and its receipt siblings; row 10 records the Q-positive/C-zero infinite-risk case (`WORK-REMAINING.md:104-111`). | Feed the retained Q/C pair to the production risk function, compare each KL contribution and total to Lean, and preserve infinite/unsupported risk without epsilon. |
| R5 · `:ambiguity` · `DarkTower.WarMachine.Holes.ambiguity` | **NO PROOF — needs capture.** | `data/wm-trace/wm-trace-2026-09-04.edn` retains each ranked action's `:G-ambiguity` output, but not the complete policy-conditioned state masses and matching observation-kernel entropy rows consumed at that call; `ambiguity-reference.edn` is fixture-only (`variable-situation-accounting.edn:1664-1692`). | Retain Q(s\|pi), A entropy rows and revisions at a real ranking call, run production ambiguity, and compare every weighted term and sum to Lean. |
| R5 · `:expected-free-energy` · `DarkTower.WarMachine.Holes.expectedFreeEnergy` | **NO PROOF — provable now.** | `data/wm-trace/wm-trace-2026-09-04.edn` retains `:G-risk`, `:G-ambiguity`, and the corresponding ranked total for each action at one immutable trace form, so the actual production composition can be replayed at identical scalar pins (the older fixture limitation is recorded at `variable-situation-accounting.edn:1637-1663`). | Read one retained ranked triple, call the production G composer on its risk/ambiguity values, compare the sum to Lean, and retain nonfinite/missing-component refusals. |
| R6 · `:policy-set` · `DarkTower.WarMachine.MachinePolicySet.machinePolicySet` | **NO PROOF — needs capture.** | The trace retains downstream `:ranked-actions` and `:policy-support-exclusions`, but not the complete pre-ranking candidate sequence with the gating/context inputs and revision that produced it; an output list cannot reconstruct the generator call. Retain those inputs beside the resulting ordered policy set. | Capture a real candidate/gate context, invoke the production policy-set constructor, compare ordered membership/support to Lean, and mutate order/support as controls. |
| R13 · `:depth` · `DarkTower.WarMachine.MachineDepth.machineDepth` | **IN FLIGHT under row 15** (`WORK-REMAINING.md:172-173`; accounting `variable-situation-accounting.edn:3232-3255`). | — | Pin a real anticipation/context record, call the production depth chooser, and prove its exact branch/value in Lean with boundary controls. |
| R14 · `:temperature` · `DarkTower.WarMachine.MachineTemperature.machineTemperature` | **IN FLIGHT under row 15** (`WORK-REMAINING.md:172-173`; accounting `variable-situation-accounting.edn:3257-3281`). | — | Pin a real selection-gain/temperature record, call production tau, compare the scalar/status to Lean, and retain typed absence/nonfinite controls. |
| R6 · `:policy-posterior` · `DarkTower.WarMachine.Holes.softmax` | **NO PROOF — provable now.** | `data/wm-trace/wm-trace-2026-09-04.edn` retains the complete ordered ranking, effective tau/selection gain, habit terms, decision explanation, and posterior status at a run id; these are sufficient to replay the production selector's softmax branch or its typed `:absent` outcome at identical pins. | Replay production softmax over the retained ordered scores and tau, compare every mass and normalization to Lean, and commission order, zero-temperature, and absent-branch controls. |
| R16 · `:action` · `DarkTower.WarMachine.MachineAction.machineAction` | **IN FLIGHT under row 15** (`WORK-REMAINING.md:172-173`; accounting `variable-situation-accounting.edn:3283-3307`). | — | Pin a real decision/ranked set, run the production chooser, and prove the selected action/rank or typed divergence against Lean. |
| R17 · `:dirichlet-accumulation` · `DarkTower.WarMachine.Holes.DirichletConcentrations` | **NO PROOF — provable now.** | `holes/labs/wm-contract/runs/row-12-accumulation-2026-09-12/witness.edn` retains three real ticks in order, all 294 pre/event/post coordinates and revisions, with maximum delta 0.0 (`WORK-REMAINING.md:146-151`). | Bind the existing three-tick production recurrence readback to the carrier, state the 294 exact Lean reference equations, and retain recount/drop/support-mutation refusals. |
| R17 · `:model-reduction` · `DarkTower.WarMachine.Holes.bayesFactorThreshold` | **NO PROOF — needs capture.** | No retained WM trace row was found containing a model-reduction invocation with the unreduced/reduced model identities, both free-energy values (or their exact delta), threshold revision, and decision. `bayes-factor-threshold-reference.edn` is explicitly fixture-only (`variable-situation-accounting.edn:1926-1954`). | Retain one real reduction comparison, call the production threshold predicate, prove the exact delta/decision in Lean, and test both sides of −3 plus missing-model refusal. |

## Counts and resulting row-16 scope

- Equation/declaration rows inventoried: **18**.
- Admitted today: **4 rows** (26 admitted claims: R1 2, R2 1, R3a 13, R7 10).
- Covered by row 15 in flight: **4 rows** (belief update, depth, temperature, action).
- `NO PROOF`, provable now from retained identical pins: **5 rows**.
- `NO PROOF`, needs an additive production-input capture: **5 rows**.
- Total `NO PROOF` rows, hence row 16's measurement-proof scope today: **10**.

This count does not promote the completed row-6/10/12 construction witnesses:
until their claims pass the row-2 admission carrier they remain useful pinned
inputs, not admitted measurement proofs.
