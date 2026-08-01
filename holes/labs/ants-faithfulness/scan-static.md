# Static AIF faithfulness scan — ants

**Date:** 2026-08-01

**Scope:** `src/ants/aif/{core,policy,efe,perceive,forward,observe,affect,default_mode,food_belief,pattern_efe,pattern_sense}.clj` and the `src/ants/war.clj` consumer.

**Method:** static code audit against the predecessor R1–R12 contract and the Futon R13–R19 extensions. This half asks what the controller computes and what downstream *should* consume; the separately commissioned causal-authority scan asks what empirically moves behaviour.

## Headline

On a strict pass/fail reading, the ant controller satisfies **4 of R1–R12**: R1, R2, R3, and R7. R4, R5, R6, R8, and R9—the predecessor contract's required core—do not all pass, so the ants do **not** presently earn an unqualified AIF label. R10–R12 also do not pass (R10 is at most batch-live/partial).

The single worst policy-selection finding is that the documented mode posterior is not the live mode mechanism. `infer-mode` computes a softmax-shaped `q(m|o)` but is reachable only through dead `efe-tilt`; the live path calls the deterministic hysteresis controller `affect/next-mode` (`core.clj:152-155`, `affect.clj:21-56`). That hard mode then conditions C, adds a hand-written per-mode action bias, and participates in hard admissibility gates. The softmax is real, but it acts after a behaviour tree has already supplied much of the answer.

## Step 0 — search for the unbuilt rework specification

I searched:

- the names and contents of `futon2/holes/`, `futon2/holes/missions/`, and `futon0/holes/` for ant/AIF/policy/faithfulness/rework terms;
- focused references to `mode-thresholds`, `infer-mode`, `actions-by-mode`, “policy selection,” “posterior,” and “hardcoded”;
- `git log --all`, path history, patch history (`-G`), and deleted/historic path names across the ant sources and hole documents.

**Result: no single proposed-but-unbuilt “more AIF-faithful ant policy-selection rework” specification was found.** The closest documents are distinct and must not be silently substituted for it:

1. `holes/M-aif-ants-port.md` is the thorough R-contract rework, but it was substantially built in commits `5b31af8` through `79ac385`; it is not the missing unbuilt proposal.
2. `holes/F-propagator-on-c-vector-NEGATIVE.md:167-184` leaves an **unbuilt propagator experiment** over precision, mode conditioning / `actions-by-mode`, or colony-distributed C. Those are propagator-transfer requirements, not a replacement AIF policy-inference design.
3. `holes/cascade-ants.edn:136-152` leaves **unbuilt action-vocabulary and pattern-composition repairs**: patterns cannot add actions, only one pattern is active, and parameter composition is incoherent. Those are cascade requirements, not a generative policy-selection specification.

The tables below therefore score against the external contracts only. The two adjacent proposals are retained in the ranked gaps under an explicit “adjacent spec” label.

## R1–R12 contract scan

The predecessor contract itself says R1–R9 are required and R10–R12 deployment-dependent (`aif-completeness.md:12`); “4/12” below is a deliberately stricter all-row headline, not a claim that the source contract calls all twelve mandatory.

| R | Verdict | Static evidence and reason |
|---|---|---|
| R1 explicit belief distribution | **Pass** | `perceive/ensure-mu` creates persistent `:sens`, `:h`, position and goal (`perceive.clj:41-62`); `:var` is evolved and persisted (`perceive.clj:102-116,205-210`; `core.clj:205-221`). Mean and variance survive tick-to-tick on the ant. |
| R2 fixed observation schema | **Pass** | Fourteen named, normalised channels are emitted by `g-observe` and ordered by `sense->vector` (`observe.clj:96-114,161-184`); the same keys drive perception and C (`perceive.clj:11-30`; `policy.clj:97-100`). Extra telemetry (`:recent-gather`, `:white?`) does not change that core ABI. |
| R3 predictive-coding update | **Pass** | Raw and precision-weighted errors are explicit (`perceive.clj:117-129`), sensory means update by `alpha * weighted-error` (`157-166`), microsteps run at `183-223`, and `F = 0.5 * mean(weighted squared error)` is returned (`131-138,211-222`). This is a Gaussian predictive-coding approximation, not full variational message passing, but it meets this contract's stated approximation. |
| R4 predictive forward model | **Fail (partial apparatus)** | The strong part is real: pure `ant-kernel` is shared by prediction (`forward.clj:597-622`) and enactment (`war.clj:1283-1301`), and policy obtains predicted observations through it (`policy.clj:535-584,602-604`). The distribution is not actually carried into scoring: `forward-predict` supplies variance for only four channels (`forward.clj:593-622`), `predict-observation` discards it, and EFE substitutes the *current belief variance*, identical across candidate actions (`policy.clj:612-624`). At H>1, rollout explicitly reverts to the separate hand-shaped `predict-outcome` (`policy.clj:907-923`). Thus the forward distribution is not good enough to keep EFE/policy rollout from collapsing toward heuristics. |
| R5 two principled EFE terms | **Fail** | Gaussian KL risk and Gaussian entropy code are mathematically recognisable (`efe.clj:17-21`; `policy.clj:618-627`), but ambiguity receives the same current `mu.var` for every action, rather than `Q(o|π)` variance. It therefore cannot favour informative actions as R5b requires. The quantity called directed EIG is `food-prob * uncertainty` (`food_belief.clj:101-112,137-149`), not expected posterior entropy reduction. After the honest `controller-score` split, selection adds a second unreported heuristic layer directly to `:G` (`policy.clj:926-970`), so selected `:G` no longer equals the emitted `:controller-score`. |
| R6 softmax plus abstain | **Fail** | Temperature-scaled softmax over `-G/tau` is present (`policy.clj:669-682,989-1001`), but there is no uncertainty/indifference abstain branch. Hard candidate filtering precedes it (`691-729`). `default-mode` is a deterministic fallback action, not abstention (`default_mode.clj:23-68`). |
| R7 adaptive precision | **Pass** | Prediction-error histories update a shared per-channel precision state on every perceptual microstep (`perceive.clj:73-95,192-199`); the state persists on the ant (`core.clj:205-209`). Temperature also adapts, though its extensive hunger/reserve clamps are engineering controls (`affect.clj:196-239`; `policy.clj:831-863`). |
| R8 reconstructable per-tick trace | **Fail (partial telemetry)** | `aif-step` returns observation, policy candidates, chosen action, G/P/F and diagnostics (`core.clj:224-248`), and `war.clj` emits an action event (`1369-1383`). But the perceptual trace contains only `tau`, `h`, and aggregate error (`perceive.clj:212-222`), not explicit `mu_pre`, channel `epsilon`, and `mu_post`; no ant-specific durable trace schema in this scope reconstructs the complete decision required by R8. |
| R9 named validation properties | **Fail** | Ant tests cover formula identities, purity, ranking goldens, tau and rollout, but there is no ant harness naming and quantitatively checking all four contract properties V-shrink, F-decrease, EFE-stress, and Abstain-fires. The repository's named R9 suite is for `futon2.aif`, not `ants.aif` (`test/futon2/aif/r9_named_validation_test.clj`). Abstain-fires cannot pass while R6 lacks abstention. |
| R10 live operation | **Fail / batch-partial** | The controller runs each simulation tick (`war.clj:1385-1391,1448+`) and emits in-process telemetry, but this scope shows neither a recurring deployment schedule nor the queryable persistent trace store required by R10. |
| R11 multi-agent composition | **Fail (engineering composition only)** | Multiple ants share a world and the kernel enforces occupancy/resource effects (`war.clj:1202-1250`), which is useful simulator engineering. There is no hierarchical or AIF coordination layer whose joint beliefs/policies ensure coherent action composition; agents are stepped against shared mutable state. |
| R12 dual-loop inference | **Fail** | Preferences, action costs, EFE weights, mode thresholds, and temperature gains are configuration/constants (`core.clj:11-51`; `policy.clj:25-30,102-206`). Error-adaptive channel precision is R7, not a slower loop inferring preferences, learning rates, or EFE weights. |

## Futon-native R13–R19 scan

| R | Verdict | Static evidence and reason |
|---|---|---|
| R13 multi-step policy adequacy | **On paper only; live fail** | `rollout-score` and a test witness exist, but `core/aif-step` never passes `:horizon`, so `choose-action` defaults to 1 (`policy.clj:905-923`; `core.clj:184-195`). Worse, H>1 uses the legacy heuristic transition rather than the R4 shared kernel (`policy.clj:915`). A live caller should supply H>1 and the rollout should consume the shared predicted distribution. |
| R14 policy precision | **Partial / engineering** | Adaptive `tau` genuinely controls selection sharpness and is carried tick-to-tick (`policy.clj:831-863,978-994`). It is driven by hand-set hunger, starvation, nest, reserve and survival clamps, not inferred variational policy precision from realised policy outcomes. Badge: engineering, not canonical gamma. |
| R15 hierarchical/temporal depth | **Fail** | No upper generative model supplies priors to a lower model. Optional rollout depth is neither live nor belief-over-future-beliefs. |
| R16 closed action–perception loop | **Static path present** | `aif-step` chooses an action, `war/apply-action` enacts it via the shared kernel, world effects are applied, and the next tick observes the changed world (`war.clj:1283-1311,1385-1391`). This is what downstream should listen to. Whether selections exert causal authority is deliberately left to the concurrent half-2 scan. |
| R17 structure learning | **Fail** | `food-belief` learns state/parameters over a fixed cell map, not new latent causes, policies, or action classes. The separate `:learning` brain in `war.clj:1393-1413` is not the AIF brain and revises a fixed local cascade by a separate rule. |
| R18 per-quantity faithfulness | **Fail before this audit; explicit debt now** | No ant-native badge manifest existed. The table below exposes several analogical AIF names and engineering controls. The most serious are `q(m|o)` (dead), action-independent “ambiguity,” and “directed EIG” without expected belief update. |
| R19 prior preferences C | **Partial** | First-class mode-conditioned Gaussian preference vectors exist and feed KL risk (`policy.clj:102-172,618-624`). They are hand-authored prior structure, which is legitimate in AIF. The problem is upstream: deterministic mode selection chooses which C applies, while additional hand-coded biases can override C-mediated ranking. |

## Per-quantity badge table

The badge vocabulary follows the requested ant convention. “Named quantity” means a model/control quantity that the source names or exposes, not every mechanical helper such as `clamp` or `merge-deep`.

| Quantity | Badge | Code evidence | What it actually computes |
|---|---|---|---|
| Observation `o` / 14-channel ABI | `:principled-approximation` | `observe.clj:96-184` | Fixed normalised sufficient-statistic-like sensor map; chosen engineering channels, but a valid declared observation model interface. |
| Belief mean `mu.sens` | `:principled-approximation` | `perceive.clj:41-62,157-166` | Persistent channel point means updated by precision-weighted prediction error. |
| Belief variance `mu.var` | `:principled-approximation` | `perceive.clj:97-116,205-210` | EMA of squared residual plus a floor; valid recursive uncertainty approximation, though the implementation passes the wrong `mu'` argument and does not use it. |
| Prediction error `epsilon` | `:derived-from-FEP` | `perceive.clj:117-129` | `o - mu` and `Pi * epsilon` per channel. |
| Variational free energy `F` | `:principled-approximation` | `perceive.clj:131-138,211-222` | Half the mean precision-weighted squared error across microsteps; Gaussian accuracy term without a full complexity term. |
| Channel precision `Pi-o` / precision-state | `:principled-approximation` | `perceive.clj:73-95,192-199` | Error-history-derived inverse uncertainty, subsequently mixed with need/safety modulation. |
| Latent behavioural `mode` | `:non-FEP-engineering` | `affect.clj:21-56`; `core.clj:152-155` | Deterministic hysteretic finite-state controller. |
| Claimed mode posterior `q(m|o)` | `:analogical` | `policy.clj:25-58,83-95` | Softmax of thresholded Boolean feature scores, but it is reachable only inside dead `efe-tilt`; it is not the live mode posterior. |
| Legacy linear `C-prior` | `:analogical` | `policy.clj:60-95` | Hand-set linear feature reward used only by dead `efe-tilt`; remove with that dead block. |
| Gaussian C-vectors | `:principled-approximation` | `policy.clj:102-172` | Hand-authored mode-conditioned Gaussian preferences over outcome channels; legitimate prior structure, not learned behaviour. |
| Shared-kernel predicted mean | `:principled-approximation` | `forward.clj:399-622`; `policy.clj:535-584` | Pure predicted next ant/world observation using the same local mechanics as enactment. |
| Predictive variance | `:analogical` | `forward.clj:593-622`; `policy.clj:614-624` | Four fixed noise variances are produced then discarded; scoring substitutes current belief variance for all actions. |
| KL `risk` | `:principled-approximation` | `policy.clj:618-627`; `efe.clj:17-21` | Gaussian KL of predicted means/current variances to C. Formula is principled, but Q's variance is not policy-conditioned. |
| Gaussian `ambiguity` | `:analogical` | `policy.clj:614-627` | Entropy of current belief variance repeated for each candidate, not expected observation entropy under the candidate policy. |
| `g-efe` | `:analogical` | `policy.clj:622-627` | Risk plus the action-invariant ambiguity above; mathematically shaped but not a complete policy-conditioned EFE. |
| `controller-score` / `g-efe-weighted` / `augmentation-total` | `:non-FEP-engineering` | `policy.clj:644-667` | Honest weighted multi-objective controller split. Selection later mutates `:G` without updating these fields (`926-970`), so the trace decomposition is incomplete. |
| Action prior/cost | `:non-FEP-engineering` | `policy.clj:174-185,380-437` | Hand-set macro-action penalties and conditional hinges. |
| Novelty/trail `info-gain` | `:analogical` | `policy.clj:439-447` | Decrease in novelty plus trail-gradient increase; no expected posterior KL/entropy reduction. |
| Food-location belief | `:principled-approximation` | `food_belief.clj:28-95` | Persistent per-cell food probability, uncertainty and visits, but neighbour values are read directly from the world (`69-85`), leaking hidden state beyond the declared local observation. |
| “Directed EIG” / epistemic value | `:analogical` | `food_belief.clj:101-149`; `policy.clj:629-639` | `food-probability * uncertainty`; no outcome distribution, simulated posterior, or expected entropy reduction. Predicted actions frequently retain the same location, further weakening policy conditioning. |
| Colony cost | `:non-FEP-engineering` | `policy.clj:449-461` | Reserve-deficit action penalty. |
| Survival cost / pressure | `:non-FEP-engineering` | `policy.clj:463-516` | Hunger-distance-ingest hinges and max-cost pressure, not a variational quantity. |
| Pattern risk | `:non-FEP-engineering` | `pattern_efe.clj:14-123` | Pattern-id-specific behaviour table of fixed penalties. |
| Pattern “info gain” | `:analogical` | `pattern_efe.clj:128-173` | Fixed rewards for named action/observation conjunctions, not information gain. |
| Pattern EFE | `:analogical` | `pattern_efe.clj:178-199` | Weighted pattern risk minus the analogical info reward. Default weight zero limits damage but not the naming fault. |
| Pattern mode affinity / constraints / switch cost | `:non-FEP-engineering` | `pattern_sense.clj:15-119` | Fixed lookup tables, Boolean constraints, and a log tick-count cost. |
| Policy temperature `tau` | `:non-FEP-engineering` | `affect.clj:110-175,196-239`; `policy.clj:518-533,831-863` | Adaptive selection-temperature controller with many hand-set homeostatic clamps; functionally real, not inferred gamma. |
| Action distribution `P(a)` | `:principled-approximation` | `policy.clj:669-682,989-1001` | Correct softmax weights over `-G/tau`; the R6 contract permits sampling or argmax, but there is no abstain. |
| Candidate support / `admissible-actions` | `:non-FEP-engineering` | `policy.clj:691-729` | Hard cargo/home/reserve/food/trail gates that can delete actions before inference. |
| Mode/base/situation/visit/white-space adjustments | `:non-FEP-engineering` | `policy.clj:733-827,926-970` | A large hand-coded behaviour table added directly to selected G after the reported controller decomposition. |
| Rollout score `S(pi)` | `:principled-approximation` on paper; `:analogical` live | `policy.clj:902-923`; `rollout.clj:1-18` | Discounted multi-step score exists when requested, but live H defaults to one and H>1 transitions use the heuristic predictor. |
| Default-mode action | `:non-FEP-engineering` | `default_mode.clj:17-68`; `core.clj:178-198` | Deterministic tropism tree used only on policy exceptions; sensible safety fallback, not AIF. |
| Macro action vocabulary | `:non-FEP-engineering` | `policy.clj:12-19`; `forward.clj:399-575` | Four fixed controller labels whose detailed motor semantics are implemented by the simulator kernel. |

## Policy selection: prior structure versus hardcoded behaviour wearing a posterior

### Mode inference

`mode-thresholds` are hard cut-points. `derive-mode-features` converts continuous observations to Booleans (`policy.clj:25-44`), and `infer-mode` softmaxes fixed scores of those Booleans (`46-58`). Even if live, this would be a discriminative soft template, not Bayesian inversion of an explicit likelihood `P(o|m)` and prior `P(m)`.

It is not live. Reference-count inspection finds `infer-mode` called only by `efe-tilt`; `C-prior` and `predict-outcomes` are likewise confined to that block, while `efe-tilt` itself has no call site. Live mode is the `cond`/`case` cascade in `affect/next-mode`. The dead block (`policy.clj:23-95`) should be removed, including `efe-tilt`, so probabilistic notation no longer overstates the live design.

### Does mode gate the action set?

`actions-by-mode` lists all four actions in every mode (`policy.clj:16-19`), so mode changes only order, not membership. With argmax selection, order normally matters only for exact ties. Actual hard gating occurs in `admissible-actions`: high cargo restricts support to return/hold, being on home deletes forage/pheromone, and further Boolean rules delete forage or return (`691-729`). The mode then:

- chooses one of three hand-authored C-vectors (`618-621`), which is legitimate prior structure only if the mode belief is legitimate;
- adds a fixed mode/action score table (`933-967`), which is hardcoded behaviour;
- was itself selected by a deterministic FSM before inference (`core.clj:152-155`).

So `actions-by-mode` is not itself the branch gate suggested by its name, but the surrounding policy is still a hardcoded behaviour tree. The softmax chooses inside support and after score shaping that the physics/controller author has largely predetermined.

### C and the forward model

The live Gaussian C-vectors are valid *prior structure*: AIF does not require preferences to be learned. The legacy `C-prior`/`predict-outcomes` block is dead and should not count either way.

The live one-step forward mean is stronger than the old “intentionally light” predictor because it calls the shared kernel. The distributional seam remains broken: predicted variance is discarded and current belief variance is reused across all actions. Consequently ambiguity cannot discriminate policies, and the “epistemic” terms are heuristics. For H>1, the controller goes back to the hand-shaped `predict-outcome`, so R4 degrades exactly where policy selection becomes policy-grained.

### What macro actions leave to the physics engine

The controller selects one of `:hold`, `:forage`, `:return`, or `:pheromone`. It does **not** select a destination, route, turn, pickup amount, deposit amount, collision resolution, ally swap, water consequence, or pheromone deposition geometry.

Those are decided inside `forward/ant-kernel` and its helpers: richest/random neighbour choice, step-toward-home/goal, whether movement succeeds or wanders, food gathering, nest deposit, pheromone drops, hunger/ingest updates, occupancy swaps, and defaults (`forward.clj:74-575`). `war/apply-kernel-effects` applies food, pheromone, score, reserve, and occupancy changes (`war.clj:1202-1250`), and `apply-water-failure` imposes washback/cargo loss (`1252-1281`).

This is a legitimate actuator/environment boundary only if claims stay at the macro-policy level. It is not evidence that the AIF controller learned navigation or foraging mechanics. A causal test should intervene on the selected macro action while holding the kernel fixed, and separately test whether changing C/EFE terms changes that selection distribution.

## Mission/code disagreements

- `M-aif-ants-port.md` treats R6's candidate set as complete. The code has no abstain and relies on several hard support gates, so R6 is not complete under the external contract.
- The port says preferences enter selection only through G. The dead `efe-tilt` no longer violates that, but the live mode FSM and post-controller adjustment table still inject behaviour outside the reported C/risk decomposition (`policy.clj:926-970`).
- The port's R4 shared-kernel claim holds for one-step mean prediction, not for the variance used by EFE or for H>1 rollout.
- The port's R13 apparatus exists, but live `aif-step` supplies no horizon; this is paper-only satisfaction.
- The port says R8 F is surfaced. F is surfaced, but the full reconstructable R8 trace is not.
- The comment “soft posterior” at `policy.clj:47` describes dead code; the live implementation is deterministic hysteresis.

## Ranked gap list — cheapest first

1. **Delete the dead posterior facade** (`policy.clj:23-95`): `modes`, `mode-thresholds`, `derive-mode-features`, `infer-mode`, `C-prior`, `predict-outcomes`, and `efe-tilt`, unless a real generative mode model is immediately wired. This is cheap truth-in-labelling and includes the already-identified dead `efe-tilt` removal.
2. **Make trace arithmetic honest:** either fold `base-adjust`/`situation-adjust`/`visit-bias`/white-space/mode adjustments into a named `:augmentation-map`, updating `:controller-score`, or remove them. Add a test that selected `:G == :controller-score == g-efe-weighted + augmentation-total` after all adjustments.
3. **Add abstain semantics** based on policy indistinguishability/uncertainty and a quantitative `Abstain-fires` test. Keep deterministic default mode as an explicitly separate failure fallback.
4. **Add an ant-native R9 harness** naming V-shrink, F-decrease, EFE-stress, and Abstain-fires with numerical criteria. Existing formula/golden tests are useful but do not substitute for these properties.
5. **Complete the R8 trace** with `mu_pre`, `o`, per-channel epsilon/precision, `mu_post`, complete candidate decompositions, chosen action, tau, and F, durably keyed by ant/tick.
6. **Carry `forward-predict` variance through `predict-observation` into EFE for every ABI channel.** Demonstrate action-differentiated ambiguity. Do not use current `mu.var` as every policy's predicted outcome variance.
7. **Use the shared R4 kernel/distribution in H>1 rollout and make H>1 live.** Add the required case where a multi-step policy beats greedy through the production `aif-step` path.
8. **Replace analogical epistemic terms with policy-conditioned EIG:** declare possible observations for each macro policy, simulate the same posterior update used after real observations, and compute expected KL/entropy reduction. Until then rename `directed-eig`, `info-gain`, and pattern info gain as exploration bonuses.
9. **Replace the deterministic mode FSM with an explicit generative mode model** if mode is to be a latent AIF state: publish `P(m)`, `P(o|m)`, infer `Q(m|o)`, propagate uncertainty into policy evaluation, and test smooth response near boundaries. Otherwise call it an engineering phase controller and keep it outside AIF claims.
10. **Reduce hard policy shaping:** move necessary safety/resource constraints to reason-bearing admissibility; express preferences through C and predictions; ablate the direct adjustment table. This is the main repair for “hardcoded behaviour wearing a posterior.”
11. **Calibrate the generative model prospectively:** compare predicted next-observation distributions and realised kernel outcomes per action, including Brier/log scores and variance coverage. Shared code prevents some drift but does not establish probabilistic adequacy.
12. **Adjacent unbuilt cascade spec:** permit reason-bearing action-class expansion and coherent multi-pattern composition rather than last-write-wins (`cascade-ants.edn:120-152`). This is R17/action-vocabulary work, not a substitute for items 6–10.
13. **Adjacent unbuilt propagator spec:** only after the base controller is honest, test the proposed precision, mode-ranking, or colony-distributed-C loci (`F-propagator-on-c-vector-NEGATIVE.md:167-184`). Do not perturb a stable preference prior per tick again.

## Static conclusion

The ants contain real AIF-shaped components: persistent uncertain beliefs, prediction errors, adaptive channel precision, a shared pure action kernel, Gaussian KL/entropy functions, and softmax action selection. The label fails at their joins. Policy-conditioned uncertainty is discarded, live temporal depth is one, abstention and contract validation are absent, and a deterministic mode/controller tree plus extensive handwritten score adjustments carries much of the behaviour. The honest current description is therefore **“a hand-engineered macro-action controller with predictive-coding state and an EFE-shaped scoring core,”** not yet a faithful Active Inference controller.
