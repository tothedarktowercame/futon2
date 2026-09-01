# TN-edge review: discovery for Joe's rulings

**Date:** 2026-09-01. **Status:** discovery only; this report makes no registry
ruling. **Code basis inspected:** futon2 `d9111a9`, p4ng `278226e`. The two
registries under review were not modified.

Pointers below are to the inspected revisions. “Recorded” means an exact
`fromNode`/`toNode` hop, not merely that both nodes occur in one run. The two
available records have the same nine-hop route:
`tick-run-record-2026-08-30.edn:1` and run
`00f4bf58-4da6-42bc-bb1d-5687e889e717` in
`tick-run-record-2026-08-31.edn:1`.

## 3a. Six drawn edges with no equation

| edge | target reader in current code | recorded run? | suggested reading (not a ruling) |
|---|---|---|---|
| R8→R5 | **No reader of R8's `F` was found.** `war_machine.clj:4450-4452` computes and route-tags F; the `wm-state` passed onward at `:4458-4466` omits F and prediction errors. `efe/rank-actions` is called at `:4536-4537`; `efe/compute-efe` instead predicts from state and each candidate action (`efe.clj:601-619`). | **Yes**, both records, via `futon2.aif.efe/rank-actions` (each record `:1`). | **Plumbing misfiled:** the exact hop is sequential control flow, not an equation import. It must not be read as F contributing to G. |
| R2→R3 | The belief-update microsteps compute error from observation and predicted belief (`war_machine.clj:4368-4379`), precision-weight it (`:4380-4388`), then apply belief events (`:4429-4431`). R3 therefore reads observation only through ε/Π; no direct R2 value bypass was found. | **No.** Both runs record R2→R7→R3, not R2→R3 (`:1`). | **Shortcut through other boxes:** the drawn edge abbreviates the R2→R8/R7→R3 dependency. The runtime route itself abbreviates differently because R8 is tagged only after F is computed. |
| R13→R14 | **Not found.** Horizon is computed independently from the anticipation snapshot (`war_machine.clj:4485-4487`) and sent to EFE (`:4516-4520`); temperature is computed inside policy selection from score spread and selection gain (`policy.clj:242-245`). | **No** (`tick-run-record-2026-08-30.edn:1`; `...08-31.edn:1`). | **Unexplained/legacy SELECT plumbing**, not evidence that depth feeds temperature. |
| R14→R16 | **Not found.** The recorded WM route ends R14→TRACE (`war_machine.clj:4804-4817`). The separate R16 `close-loop!` enacts the first passing act gate from `:ranked-actions` (`enact.clj:287-316`); it does not read τ. | **No**; both records have R14→TRACE (`:1`). | **Plumbing/obsolete sequencing.** No τ→actuation import was found. |
| R6→R13 | **Not found.** Candidate action enters the forward model directly (`efe.clj:601-609`), while horizon is independently supplied by the anticipation snapshot (`war_machine.clj:4485-4520`). | **No** (`:1` in both records). | **Unexplained/legacy SELECT plumbing.** Current code supplies π and T independently. |
| R7→R14 | **Not found.** R7's precision is computed at `war_machine.clj:4380-4388`. τ is computed from controller-score spread and `selection-gain` (`policy.clj:242-245`, `:72-80`), not from R7 Π. | **No.** The actual recorded SELECT hop is R6→R14 (`:1` in both records). | If the edge was intended as Π→τ, current code is a **departure from that variant**; otherwise it is stale plumbing. Evidence cannot distinguish the authorial intent. |

The important counterexample is R8→R5: the route record proves that control
passed from an R8-labelled step to an R5-labelled step, but inspection proves
that the R5 call did not receive F. A measured route is therefore not by itself
an equation dependency.

## 3b. Ten theory edges absent from the drawing

| theory edge | current implementation finding |
|---|---|
| R2→R8 | **Realised, undrawn.** Observation enters channel prediction errors at `war_machine.clj:4368-4379`; those errors enter F at `:4450-4452`. |
| R1→R8 | **Realised, undrawn.** Prior/stored belief is selected at `war_machine.clj:4289-4294`, used to predict observations at `:4368-4374`, then compared to R2 observations at `:4375-4379`. |
| R8→R3 | **Realised at the equation level, but node attribution is not literal.** Raw ε is precision-weighted (`war_machine.clj:4375-4388`) and applied to the belief (`:4429-4431`). The route tags R7→R3 and later R3→R8 (`:4447-4452`), so the recorded route is not the dependency DAG. |
| R14→R6 | **Realised, undrawn.** Effective τ is computed and used in policy scores (`policy.clj:242-245`); the live caller passes `:tau-mode` into `select-action` (`war_machine.clj:4573-4584`). |
| R6→R16 | **Not found as the registered Q(π)→u import.** The WM run records a recommendation and stops at TRACE (`war_machine.clj:4573-4584`, `:4804-4817`). Separate `close-loop!` chooses the first passing cascade gate from ranked actions (`enact.clj:287-316`), not the recorded Q(π) selection. |
| R8→R17 | **Not realised.** Live F is only stored in the result (`war_machine.clj:4753`). R17 is explicitly an offline replay/reduction path (`r17_offline.clj:1-6`, `:64-96`), and A4a computes BMR evidence from Dirichlet models (`a4a.clj:126-159`), not from live R8 F. |
| R13→R4 | **Conditionally realised, undrawn.** `horizon-steps` drives `predict-multi-horizon` when ≥2 (`efe.clj:601-609`; `forward_model.clj:279-324`). |
| R6→R4 | **Realised, undrawn.** Each candidate action is supplied to the forward model (`efe.clj:601-609`) while `rank-actions` maps `compute-efe` over candidates (`:903-921`). |
| R2→R7 | **Realised and recorded.** Precision update consumes observations (`war_machine.clj:4375-4388`); both run records contain R2→R7 (`:1`). |
| R1→R3 | **Realised, undrawn.** The carried belief is the input state (`war_machine.clj:4289-4294`) and belief events produce its successor (`:4429-4431`). |

## 3c. Eight free choices: observed implementation

These are observations for a later ruling, not recommended choices.

1. **Free-energy form.** The live diagnostic is Gaussian/Laplace,
   precision-weighted channel error (`free_energy.clj:184-205`;
   `war_machine.clj:4450-4452`). The policy side separately implements a
   discrete-style expected-free-energy score (`efe.clj:601-619`, `:782-794`).
   No discrete categorical *variational F* implementation or live consumer of
   the computed channel F was found. R17 computes a separate BMR evidence
   change (`a4a.clj:126-159`).

2. **Temperature update.** The live default is `:selection-gain-only`
   (`war_machine.clj:238-248`); `selection_gain/fold-realized-outcome` can
   update gain from a prior realised trace (`selection_gain.clj:173-206`), so
   “fixed-calibrated” is not a complete description. The alternative
   score-spread mode is implemented (`policy.clj:32-80`) and selectable via
   `FUTON_WM_TAU_MODE` (`war_machine.clj:238-248`). A β/γ variational precision
   update was **not found**.

3. **Habit prior.** A learned categorical frequency prior is implemented and
   persisted (`habit_prior.clj:70-89`, `:100-163`) and is the configured live
   source (`war_machine.clj:262-271`, `:4276-4283`). In the actual live
   `:strategic-recommendation` boundary it is inspectable but explicitly
   counterfactual: the first controller entry is chosen and
   `:habit-prior-applied? false` (`policy.clj:234-271`). The two TickRunRecords
   contain no habit state (`tick-run-record-2026-08-30.edn:1` and
   `...08-31.edn:1`), so they do not establish that E was non-uniform.

4. **Policy depth.** In the WM judge, depth is absent/single-step unless an
   anticipation snapshot is loaded, when it is fixed to 3
   (`war_machine.clj:4485-4487`, `:4516-4520`). The reusable rollout path has a
   configurable `:horizon`/`:depth`, default 2 (`rollout.clj:166-171`,
   `:212-230`). It varies by invocation/configuration, not by candidate policy.

5. **Hierarchy.** The rollout code explicitly says it is “flat temporal
   rollout, not nested fast/slow hierarchy” (`rollout.clj:166-176`). A separate
   hierarchical-budget implementation exists
   (`hierarchical_budget.clj:1`; `hierarchical_budget_adapter.clj:1`), but no
   wiring of it as a multi-level AIF generative hierarchy was found. Thus more
   than one AIF level is **not found** in the inspected live judge.

6. **Learning.** Offline BMR is implemented (`r17_offline.clj:1-6`, `:64-96`;
   `a4a.clj:126-159`). `DirichletConcentrations` exists only as a Lean carrier
   and witnesses (`mathlib4/DarkTower/WarMachine/Holes.lean:6380-6385` and
   `DirichletConcentrationsWitness.lean:8`); live Dirichlet learning of A was
   **not found**.

7. **Policy-posterior node.** `policy/select-action` is the R6 implementation
   (`policy.clj:300-438`; route tag `war_machine.clj:4584`). No other R-node
   implementation of Q(π), and no dedicated posterior box, was found.

8. **Selection rule.** The code is deterministic, not sampling. The live
   strategic path chooses the first admissible controller entry
   (`policy.clj:247-250`). The other branch chooses the first G-ranked entry
   (`:387-410`) or `max-key` of `ln E-G/τ` (`:411-438`). Softmax weights are
   calculated and recorded, but no draw from them occurs. A sampling
   alternative in WM actuation was **not found**; R16 independently chooses the
   first passing act gate (`enact.clj:298-316`).

## 3d. Reference verification

I checked primary/open versions where accessible. This verifies bibliographic
identity and formulation family, but not every registry equation as a verbatim
equation from the cited work.

| registry reference | result |
|---|---|
| Buckley et al. 2017 | **Bibliography verified** (JMP 81, 55–79; DOI 10.1016/j.jmp.2017.09.004). The open paper gives general variational F at eq. 9, Laplace-encoded energy at eq. 45, a precision/variance prediction-error model at eqs. 58–60, and recognition dynamics at eq. 59. The registry's channel mean `½ mean Π ε²`, simple `ε=o−μ`, inverse sample-variance Π, and positive-step update are implementation-level reductions, not one verbatim cited equation. Source: `https://arxiv.org/abs/1705.09156`. |
| Da Costa et al. 2020 | **Bibliography and formulation family verified** (JMP 99, 102447): discrete state estimation/free energy, expected free energy, policy posterior/precision, and Dirichlet learning are in scope. The accessible article explicitly labels continuous-time free-energy descent as eq. 9. I did **not** verify a single equation number for every registry row; the registry currently supplies none. Sources: `https://arxiv.org/abs/2001.07203`, `https://pmc.ncbi.nlm.nih.gov/articles/PMC7732703/`. |
| Friston et al. 2017 | **Bibliography and process-theory scope verified** (Neural Computation 29(1), 1–49; DOI 10.1162/NECO_a_00912). It covers discrete-MDP active inference, policy/action selection and precision. Exact equation/section pointers for the registry's stack-defined observe, E, τ and action rows were **not verified**. Source: `https://discovery.ucl.ac.uk/id/eprint/1530701/`. |
| Parr, Pezzulo & Friston 2022 | Bibliographic identity is plausible, but the book text was not available in the inspected workspace and I did **not** verify its section/page numbers. The registry provides none. |
| Friston, Parr & Zeidman 2018 | Bibliographic identity and BMR scope were verified from `https://arxiv.org/abs/1805.07092`; the registry's sign convention `ΔF ≤ −3` was **not verified as an equation from that source**. The local A4a implementation instead supplies the concrete computation (`a4a.clj:126-159`). |
| Kass & Raftery 1995 | **Bibliography and threshold scale verified**, but the wording needs care. Table 2 classifies **2 ln(BF) = 6–10** as “strong”, hence `ln(BF) ≥ 3` in the favoured direction. The registry uses `ΔF ≤ −3`, which is compatible only after declaring the opposite sign/direction for ΔF; Kass & Raftery do not by themselves establish that sign convention. Source: `https://www.stat.cmu.edu/~kass/papers/bayesfactors.pdf`, Table 2. |

### Information still missing before citation rulings

- Exact page/equation assignments for the Da Costa, Friston 2017, Parr 2022,
  and BMR rows remain **not verified**.
- The mixed Gaussian diagnostic F plus discrete policy G is implemented, but no
  inspected reference was found that specifically cites this mixed-grain
  composition as one formalism.
- Π from running observation-channel sample variance and τ from realised
  selection gain/score spread are implementation formulations needing their own
  provenance if Joe retains them as theory rather than stack choices.

This report intentionally stops at evidence and suggested readings. Edge and
choice rulings remain Joe's, to be recorded later in the two registries.
