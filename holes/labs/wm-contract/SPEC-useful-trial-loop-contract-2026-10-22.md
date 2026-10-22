# SPEC — The loop contract for one useful ordinary Trial (Lean-led), 2026-10-22

claude-7, at codex-11's request (job `invoke-1792670224334-2473-37469c11`), following review job 2463. Joe's governing instruction, relayed by codex-11: this work is Lean-led. Fix the literature and the Lean specification first, then align the Clojure to them. No new theory of action for this paper.

This packet states the smallest contract that already exists in Lean and the literature for one useful ordinary Trial. It covers six things: action-dependent observation, the posterior update, prediction from the current belief, policy reselection at a documented boundary, focus preferences, and the prediction horizon. Formal specification and current implementation are kept in separate sections. Unresolved design choices are named (U1–U3) and are not chosen here.

## Evidence for this packet

- **Lean** at mathlib4 `9319ebb5fa2035d8689300c2f9a2c7bbdafe7cef`. On 2026-10-22, claude-7 ran `lake build` of the 15 modules cited below:
  - ObservationProcess
  - Proof2/{BeliefAtMachine, BeliefStepAtMachine, RolloutAtMachine, PrefixFreeEnergyAtMachine, PrefixFreeEnergyPosterior, PolicyPosteriorAtMachine}
  - R15StrategicTarget, R15FocusDistance, R15FocusInfluence
  - CTauClassPreference, R20Run, MachineDepth, CascadeEFE, IncrementalTaskFrontier

  Result: "Build completed successfully". `grep sorry` over the 15 files finds nothing. codex-11 separately ran `lake env lean Proof2/RolloutAtMachine.lean`: exit 0, no `sorryAx`, receipt `/home/joe/notes/codex11-rollout-proof-check-20261022.json`.
- **Clojure** at futon2 `9b597ce85`, by source inspection only. No Clojure test was run for this packet.
- **Literature** (in `refs/`): line numbers below are lines of the `.txt` file. Each quotation was read this session.

## 1. Literature: what the sources say (and only that)

| id | claim | source |
|---|---|---|
| L1 | The generative process, R(õ, s̃, ũ), generates outcomes from hidden states and action. The generative model is over outcomes, states and policies. "Actions … are part of the generative process in the world"; "policies … are part of the generative model". | friston2017.txt:227–245 |
| L2 | The categorical model is: P(o_t\|s_t) = Cat(A); P(s_{t+1}\|s_t, π) = Cat(B(u = π(t))); P(π) = σ(−γ·G(π)). | friston2017.txt:323–340, eq. 2.1 |
| L3 | The policy is a random variable to be inferred. "The agent keeps a separate record of expected states — in the past and future — for each allowable policy". The policy is "sampled from a Gibbs distribution or softmax function of expected free energy". | friston2017.txt:358–372 |
| L4 | "After the free energy of each policy has been evaluated, it is used to predict the subsequent hidden state … This enables an action to be selected … Once an action has been selected, it generates a new observation, and the cycle begins again." | friston2017.txt:917–921 |
| L5 | There are timescales: posterior beliefs about hidden states are updated after each observation; "at the end of each sequence of observations (i.e., trial of observation epochs), the expected (concentration) parameters are updated". | friston2017.txt:714–721 |
| L6 | G(π) is a sum over future epochs τ: expected ambiguity + KL(Q(s_τ\|π) ‖ P(s_τ)) − parameter information gain. | dacosta2020.txt:1403, eq. 42 |
| L7 | A higher level controls a lower one "by setting their reference points or set-points … leaving them free to select the means". Goal selection (C) can depend on higher levels. | parr2022, p. 204 and p. 102, as pinned in `R15StrategicTarget`'s docstring. The page references were not re-read this session. |

**What the literature does not supply:**
- a rule for choosing the next strategic focus;
- a "relatedness horizon";
- a drift bound against the original objective.

## 2. Joe's rulings and preferences (not literature)

| id | ruling | where recorded |
|---|---|---|
| J1 | The class preference over endings, 55/35/5/5 over focused, related, unrelated and stop-the-line, at the horizon, with all mass on not-yet-evaluated before it. | `CTauClassPreference.terminalPreference`, `classPreference` |
| J2 | An injury is decided by the same comparison as ordinary work, with no separate branch. Where G and the branch disagree, G stands. | Joe 2026-10-21; `R15FocusDistance.injuryDeclared`, `R15InjuryTier` |
| J3 | Criteria, wants and horizon are computed from the mission text at click time, cued to spans. The owner's stated value wins over a computed one. | Joe 2026-09-24; futon2 `924a6820`, `81ff1315` |
| J4 | "Finish a task, then move to related work"; earlier predictions are evidence, not a binding sequence. | Joe 2026-10-22, relayed by codex-11. A preference stated in conversation, not yet a recorded ruling. |

## 3. Our modelling choices (neither literature nor ruling)

| id | choice | where |
|---|---|---|
| M1 | The world is a finite categorical process of the same form as eq. 2.1, on the model's carriers (sets of established tokens). | `ObservationProcess` docstring, "Declared stack assumption" |
| M2 | A click is a trial: learning across clicks is L5's end-of-trial update. | `R20Run` docstring ("A click is a trial here") |
| M3 | The strategic focus sets C through each target's class, not the action (a reading of L7). | `R15StrategicTarget` docstring ("Our reading") |
| M4 | Distance to the focus is 1 − total variation between the predicted observations about the focus with and without the work. | `R15FocusInfluence` (Pinsker is cited for the ordering) |
| M5 | A cascade's transition is the co-application of its enabled frontier. | PROOF-2a Clause 0; `Proof2/CoApplicationKernel` |

## 4. The formal contract (Lean)

| # | requirement | Lean (module · decl) | status |
|---|---|---|---|
| C1 | **The observation depends on the action.** P(o_t \| s_{t−1}, u_{t−1}) = Σ_{s_t} B(s_t \| s_{t−1}, u) A(o_t \| s_t), computed from the world's state, not from a belief (L1, L2, M1). | `ObservationProcess` · `observationAfter`, `observationAfter_sum`, `fixture_action_changes_observation` | proved |
| C2 | **Posterior update at the machine's own action.** The next belief is the exact Bayes update on the realised observation. An observation predicted impossible is a typed refusal, never absorbed (L4, L5 fast timescale). | `Proof2/BeliefStepAtMachine` · `machineStep`, `machineStep_eq_update`, `machineStep_updateRefused`, `wrongActionChangesTheBelief`; `Proof2/BeliefAtMachine` · `machineTrajectory`, `machineTrajectory_isDistribution` | proved |
| C3 | **Prediction from the current belief.** The rollout starts at the current posterior μ, not at the model's q₀ (L4: "used to predict the subsequent hidden state"). | `Proof2/RolloutAtMachine` · `machineRollout`, `machineRollout_startsAtCurrentBelief`, `rolloutFromBelief_changes_the_outcome` | proved |
| C4 | **Earlier predictions stay evidence.** Each policy's free energy is summed over its own admitted executed history, and enters the policy posterior beside G and E. A contradicted policy gets weight zero (L3: the per-policy record of past states). | `Proof2/PrefixFreeEnergyAtMachine` · `prefixF`, `stepF_eq_negLogEvidence`; `Proof2/PrefixFreeEnergyPosterior` · `machineWeightsAtPrefixF`, `contradictionHasZeroWeight` | proved. The literature pin is still missing: the module cites SPEC-F §1, not friston2017. |
| C5 | **Reselection at the trial boundary.** Each click starts from what the previous one left: initial-state counts d ← d + posterior, habit counts, β from the valence belief, and the previous focus. Each click then selects afresh (L5, M2). | `R20Run` · `Click`, `Carries`, and the docstring's run theorems | stated and proved for carries. **Within a click, see U1.** |
| C6 | **Focus preferences.** The focus fixes each target's class, focused > related > unrelated; C over classes is J1; an unknown relation is left unscored (L7, M3, M4, J1, J2). | `R15StrategicTarget` · `tacticalPreference`, `changing_focus_changes_terminal_preference`; `R15FocusDistance` · `relationOfDistance`, `nearer_preferred`, `injuryDeclared`; `R15FocusInfluence` · `influence`, `estimatedDistance` | proved (the estimator returns `none` without records) |
| C7 | **One horizon per comparison.** G is summed over epochs τ = 1..T (L6). C(τ) is defined over the same epochs: waiting before H, terminal at H (J1). | `CascadeEFE` · `totalG`; `CTauClassPreference` · `classPreference`, `pre_horizon_risk_zero` | stated. **The relation of T to H is U2.** |

### Numeric rollout depth, preference horizon, relatedness horizon

- **Literature.** There is one horizon. A policy is a sequence of actions over epochs (L1, L2: u_t = π(t)). G sums over the policy's future epochs (L6). The preference is defined at those same epochs. Neither source gives the rollout depth and the preference horizon as two quantities.
- **Lean.** `MachineDepth` records four declared depths that disagree (`declaredDepthsDisagree`: the forward-model default 3, rollout default 2, live tick 3, cascade lane 5). Those belong to older, flat routes. On the cascade route, `CascadeEFE.totalG` takes T, and `classPreference H τ` takes H.
- **Clojure, cascade lane.** At futon2 `9b597ce85`, `cascade_shape_g.clj`:310 sets `horizon = clamp(count patterns, 1, scorer/max-horizon)`. :54–55 then builds the class-preference schedule over the same `horizon`. So in the code, T = H, chosen per candidate as its pattern count.
- **Relatedness horizon (codex-11's proposal, and the R15 note in the revised PLoP draft).** No literature source above, and no Lean module, defines it. Nothing in C1–C7 needs it: "finish, then move to related work" (J4) is what C6 already gives at each click, with the new focus read at the next click (C5). **Recommendation: exclude it from this paper and from the Trial contract.** If drift against the original objective turns out to matter, it is a question about the outer level (U3), to be specified from a source, not added here.

## 5. Unresolved design choices (named, not chosen)

- **U1 — reselection within a click.**
  - What the literature does: L4 reselects after every action. The cycle ("predict, select, observe, update") repeats each epoch, with the policy posterior re-evaluated using C4.
  - What Lean states: per click only (C5). Nothing states whether, inside one click, the cascade is re-selected after each executed pattern, or committed for the click.
  - Options:
    - (a) the click is one epoch, so selection happens once per click and the boundary is the click;
    - (b) the click is a trial of several epochs, and the posterior is re-evaluated after each pattern with the prefix F.
  - (a) needs no Lean change. (b) needs a Lean statement of the within-click loop, which would compose C2, C3 and C4 at each epoch.
- **U2 — what T and H are on the cascade route.** The code uses T = H = clamp(pattern count). Under L6 every candidate in one comparison is scored over the same epochs. Per-candidate pattern counts give different candidates different horizons, so G values over different numbers of epochs are compared. Options:
  - (a) one T = H per comparison, computed from the mission text (J3), with every candidate scored over it;
  - (b) keep the per-candidate horizon and state a normalisation, departing from L6.

  The revised PLoP draft's R13 upgrade proposes (a). That proposal should be reviewed before the code changes.
- **U3 — how the next focus is chosen.**
  - What exists: Lean reads the focus as supplied per click (`R20Run`'s strategic row; Clojure `focus_receipt.clj` `decision-focus`). Nothing in Lean or the literature above specifies the rule for choosing it.
  - Options:
    - (a) leave it with the existing outer selection;
    - (b) specify an outer-level choice from a source not yet pinned.

  This packet does not need it for one useful Trial.

## 6. Mapping to Clojure (futon2 `9b597ce85`), and conformance tests

| # | producer → consumer | conforms? | conformance test (to write) |
|---|---|---|---|
| C1 | observation law: `cascade_model_manifest.clj` `pattern-kernel`, `outcome-risk` | not established by this packet | For a fixture world state and action, the Clojure observation distribution equals `observationAfter` on the same rows, and changing the action changes it (mirrors `fixture_action_changes_observation`). |
| C2 | `belief.clj` `update-step` | the tempering normalisation was fixed in futon2 `5df5853ea` (2026-10-19). **The Lean `MachineBeliefUpdate.categoricalUpdate` is behind it**; this is a Lean fix, claude-7's lane. | Pin one live update record: posterior = Bayes(prior, A, o). An observation with zero predicted probability gives the typed refusal and not a default belief. |
| C3 | the rollout via `pattern-kernel`, from the post-update belief | not established | A rollout from a belief μ ≠ q₀ differs from one from q₀ on a fixture (mirrors `rolloutFromQ0_ne_rolloutFromBelief`). |
| C4 | `policy_prefix_evidence.clj` `production-ranked` → `policy.clj` `select-action-cascades` | **no**: per `PrefixFreeEnergyAtMachine`'s docstring, the live path stamps `:not-supplied` for every entry | A policy whose prefix was contradicted gets weight 0. A policy with a certified empty history is scored without F. An uncertified empty history is typed absence. |
| C5 | the run record's carries | **no** carries were retained in the 10-21 records (codex-4's catch-up answers) | Two consecutive clicks: next habit `before` = this click's `nextConsumed`; next d = d + posterior; next β prior from the valence update; next `previousFocus` = this click's focus. |
| C6 | `focus_receipt.clj` `decision-focus` → `cascade_shape_g.clj` `class-emission-model` | **no**: `class-emission-model` assigns `:focused` to the selected target without reading the focus (stated in `R15StrategicTarget`'s docstring and in the `R20Witness` edge) | Change the focus with everything else held fixed: the target's class, and so the terminal preference, changes (mirrors `changing_focus_changes_terminal_preference`). |
| C7 | `cascade_shape_g.clj`:310 horizon → :54 class-preference schedule | T = H holds; equality across candidates depends on **U2** | Every candidate in one recorded comparison carries the same `:horizon` (only under U2(a)). |

## 7. Lean changes needed

1. `MachineBeliefUpdate.categoricalUpdate`: restate it with the normalised tempered likelihood (`LikelihoodPrecision.precisionLikelihood`) that the code has used since `5df5853ea`. claude-7.
2. The C4 citation: pin friston2017 :358–372 (and the F(π) equation it introduces) in `PrefixFreeEnergyAtMachine`'s docstring. This is a docstring change only.
3. Only if U1(b) is chosen: a module stating the within-click epoch loop as the composition of C2, C3 and C4.
4. Only if U2(a) is chosen: a statement that every candidate in a comparison is scored at one T = H. Then retire or restate `CascadeEFE.scoreCascade` (list kernel) in favour of the co-application kernel (`CascadeCoScoringExamples.join_beats_chain` shows the two differ).

There is no new theory of action. Nothing in this packet adds a horizon or a focus rule.

### Landed (updated as packets merge)

| date | mathlib4 | module · decls | item |
|---|---|---|---|
| 2026-10-22 | `363dbae640` | `Proof2/PrefixFreeEnergyAtMachine` docstring: friston2017 :358–372, :917–921 | §7 item 2 (C4 citation); codex-11 independently checked it (`~/notes/codex11-prefix-f-check-20261022.json`) |
| 2026-10-22 | `c8663f2acb` | `PatternThetaNovelty` · `novelty` (da Costa eq. 57–58, averaged over the predictive: `novelty_eq` = 1/(2(a+b))), `expectedInfoGain` (exact expected posterior KL, defined; not proved equal to the approximation), `novelty_nonneg`, `success_decreases`, `failure_decreases`, `novelty_bound`, `fires_reduce`, `less_tried_lower_G` | the parameter-information-gain term of L6 for a pattern's θ (codex-1, reviewed by claude-7). Not yet composed into cascade G. Applying eq. 57 to θ, rather than to A, is our modelling choice. |
| 2026-10-22 | `e51605fa2d` | `MachineBeliefUpdateNormalised` · `normalisedUpdate` (temper with `precisionLikelihood`, renormalise each state over outcomes, Bayes; typed `UpdateError`: invalidKappa, zeroTemperedColumn, zeroEvidence), `normalisedUpdate_isDistribution`, `normalisedUpdate_eq_exactBayes`, `normalisedUpdate_one_eq_untempered`, `categoricalUpdate_not_always_normalised` (the v1 defect, a 2×2 counterexample: 4/13 vs 5/14), `normalisedUpdateFromWeight_uses_machineKappa` | §7 item 1, C2. Specifies futon2 `5df5853ea` v2 (codex-9, reviewed by claude-7). Non-finite κ cannot occur in ℝ, so it stays a runtime boundary check. |
| 2026-10-22 | — | conformance, C2: codex-11 ran the production `belief/update-step` on the 2×2 case from `normalised_counterexample_value`. At κ = 2 it gives 5/14 and 9/14 (not v1's 4/13); at κ = 1 the untempered update; negative precision is refused. Probe, output and receipt: `~/notes/codex11-normalised-update-{probe.clj,probe.edn,receipt.json}` | a standalone production-function check, not end-to-end |
| 2026-10-22 | `1294a24270` | `CascadeCoApplyG` · `scoreCascadeCo` (co-application kernel over the containment order; refusals missingInterpretation, inadmissibleRisk, cyclicContainment), `coKernel_eq_listKernel_of_chain`, `scoreCascadeCo_eq_scoreCascadeList_of_chain`, `join3_co_differs_from_list`, `compareAtHorizon` + `compareAtHorizon_uses_H` (one H for every candidate, `classPreference H (t+1)`, no length normalisation), `cyclic_order_refused` | C7 and §7 item 4 (codex-3, reviewed by claude-7). Limit: the chain agreement is against the token-state list kernel, not yet `CascadeEFE.scoreCascade` itself; the bridge is in flight, and the canonical pointer in GOverCascades changes after it lands. |
| 2026-10-22 | `ebf2d0d886` | `Proof2/ClosedLoopAtMachine` · `epoch`, `loop n`, `LoopState` (main belief, each policy's `policyBelief`, actions, history), `PolicyEpoch` (admitted / contradicted, no posterior), `encode` + `encode_q_irrelevant`, `contradicted_record_gives_prefix_contradiction`, `loop_belief_eq_machineTrajectory`, `next_rollout_starts_at_loop_belief`, `admitted_stepF_eq_negLogEvidence`, `encoded_epoch_contradiction_has_zero_weight`, `policyEpoch_sPrev`, `nextPolicyBelief_admitted`, `loop_one` | C2–C5 (codex-10, three dispatches; claude-7's review change carries each policy's Q(s_τ\|π) through the loop). Evidence for EVERY policy, under its own transition. The relation of the main belief to the Bayesian model average is NOT proved: it needs weights and an equality hypothesis. U1 is not chosen: `loop 1` is the one-epoch click. |
| 2026-10-22 | `b7c8e6c352` | `CascadeCoApplyGNovelty` · `CandidateN` (a candidate plus Beta counts per pattern), `expectedNovelty` (Σ_s P(s at τ−1) · Σ over the enabled frontier at s of novelty: da Costa eq. 58), `totalNovelty`, `scoreCascadeCoN` = scoreCascadeCo − totalNovelty (same refusals), `scoreCascadeCoN_le_scoreCascadeCo`, `never_enabled_contributes_nothing`, `expectedNovelty_antitone`, `totalNovelty_strict` (general (b), added in review), `less_tried_positive_fire_lower_G` (instance), `compareAtHorizonN_uses_H` | L6's third term composed into cascade G (codex-9; claude-7 added the general ordering). Rests on PatternThetaNovelty's eq. 57 approximation. |

In flight: `CascadeCoApplyGBridge` (codex-3). The closed loop takes evidence for EVERY policy, under its own transition (friston2017 :722–726, :1598–1607). Its contradiction carrier is `PolicyEpoch` plus `encode_q_irrelevant`, because W7's `Step.q` is required although admission never reads it on a contradiction.

## 8. What is NOT claimed

- That the live machine conforms to any of C1–C7. The table in §6 separates proved specification from implementation.
- That the U-choices are settled.
- That any Clojure test was run for this packet.
