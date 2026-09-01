# C452 — the ten theory edges that are not drawn

Date: 2026-09-01. Scope: discovery for query 3b of
`holes/TN-edge-review-aif-wiring.md`; no registry rulings or edits.

## Result

Nine imports are realised in the current production path but are not drawn in
Figure 5A. One is not realised: R8→R17. The BMR implementation builds its own
Dirichlet concentration inputs and never reads the variational free energy
computed at R8.

The two inspected run records contain no exact traversal for any of the ten
edges. In both records, `:route` is present and `:unmapped-hops` is **not
found**. Thus the code evidence below must not be read as run-traversal
evidence; the route instrumentation records a different, coarser sequence.

## Edge census

| theory edge | classification | production evidence / substitute | 2026-08-30 run | 2026-08-31 run |
|---|---|---|---|---|
| **R2→R8** (`o` into `eps`) | **Realised in code but undrawn.** | `scripts/futon2/report/war_machine.clj:4221-4223` constructs `observation`; `scripts/futon2/report/war_machine.clj:4375-4379` passes each value from that same observation to `free-energy/compute-prediction-error`. The target performs `o - mu` at `src/futon2/aif/free_energy.clj:171-180`. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. |
| **R1→R8** (`mu` into `eps`) | **Realised in code but undrawn.** | `scripts/futon2/report/war_machine.clj:4289-4294` obtains the carried belief `wm-belief-pre`; `scripts/futon2/report/war_machine.clj:4368-4379` derives predictions from the current loop belief and passes those predictions into `compute-prediction-error`. The target reads `:mean` as the prediction to subtract at `src/futon2/aif/free_energy.clj:171-180`. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. |
| **R8→R3** (`eps` into belief update) | **Realised in code but undrawn.** | `scripts/futon2/report/war_machine.clj:4375-4388` turns prediction errors into precision-weighted errors; `scripts/futon2/report/war_machine.clj:4392-4400` derives the R3 update driver from them; `scripts/futon2/report/war_machine.clj:4425-4431` constructs and applies belief events. The imported quantity is explicitly described as `precision × error` at `src/futon2/aif/belief.clj:1023-1036`. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. |
| **R14→R6** (`tau` into policy posterior/selection) | **Realised in code but undrawn.** | `scripts/futon2/report/war_machine.clj:4340-4361` reads and updates the selection-gain state, and `scripts/futon2/report/war_machine.clj:4573-4581` supplies its value to `policy/select-action`. R6 turns that input into the effective temperature at `src/futon2/aif/policy.clj:71-80` and uses it in the policy score at `src/futon2/aif/policy.clj:240-246`. This is the documented engineering `selection-gain` realisation of R14, not an unqualified claim of canonical variational policy precision (`src/futon2/aif/policy.clj:46-70`). | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. |
| **R6→R16** (`Q-pi`/selection into action) | **Realised in code but undrawn.** | The production runner reads `judgement[:decision :action]` and resolves it to its selected ranked entry at `src/futon2/aif/full_loop_runner.clj:870-873`; the live run calls that boundary at `src/futon2/aif/full_loop_runner.clj:2512-2527`. The selected entry then determines the mission and construction passed into the actuation path at `src/futon2/aif/full_loop_runner.clj:2619-2633`. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. |
| **R8→R17** (`F` into model reduction) | **Not realised — hole.** | R8 computes `variational-free-energy` at `scripts/futon2/report/war_machine.clj:4450-4452` and only places it in the judgement/trace at `scripts/futon2/report/war_machine.clj:4753`. The R17 implementation instead builds full posterior and reduced-prior vectors from its independent concentration matrix and calls BMR at `src/futon2/aif/a4a.clj:115-131`. A read of the R8 `:variational-free-energy` by R17 was **not found**. Thus the target substitutes a corpus-derived Dirichlet BMR calculation for the registry's imported per-tick `F`. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. |
| **R13→R4** (`T` into forward model) | **Realised in code but undrawn.** | The live judge chooses `wm-horizon-steps` at `scripts/futon2/report/war_machine.clj:4482-4487` and supplies it as `:horizon-steps` at `scripts/futon2/report/war_machine.clj:4516-4522`. R4 reads it to select and execute `predict-multi-horizon` at `src/futon2/aif/efe.clj:601-613`. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. |
| **R6→R4** (`pi` into forward model) | **Realised in code but undrawn.** | `src/futon2/aif/efe.clj:903-920` maps each included candidate policy/action into `compute-efe`; the target passes that action to `forward-model/predict` at `src/futon2/aif/efe.clj:601-609`. The forward model consumes the action to produce effects and the next state at `src/futon2/aif/forward_model.clj:328-355`. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. |
| **R2→R7** (`o` into precision) | **Realised in code but undrawn.** | `scripts/futon2/report/war_machine.clj:4375-4379` places each observed value in the prediction-error record; `scripts/futon2/report/war_machine.clj:4380-4388` passes those records to `precision/update-precision-state`. The producer record explicitly contains `:observed` and `:error` at `src/futon2/aif/free_energy.clj:169-182`. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. |
| **R1→R3** (`mu` into belief update) | **Realised in code but undrawn.** | The inner loop binds the current `belief` at `scripts/futon2/report/war_machine.clj:4363-4367`; R3 uses that same map both to calculate entity-specific update weights at `scripts/futon2/report/war_machine.clj:4413-4428` and as the input to `apply-arena-belief-events` at `scripts/futon2/report/war_machine.clj:4429-4431`. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. | Exact edge in `:route`: **not found**. `:unmapped-hops`: **not found**. |

## Consequence of the priority-three review

The priority chain is implemented: the live judge uses both observation and
belief-derived prediction to form prediction errors, and those errors drive
the subsequent belief update. Figure 5A omits all three theory edges. The run
records do not independently demonstrate them because their recorded route is
`R2→R7→R3→R8`, not the data-dependency edges above.

All ten requested edges were reached. No claim here decides whether Figure 5A
should be changed or whether the R17 formal/implementation mismatch should be
resolved in code or in the equations registry.
