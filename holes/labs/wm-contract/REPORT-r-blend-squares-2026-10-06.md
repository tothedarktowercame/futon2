# The ruled R-cascade read as nineteen blend squares — 2026-10-06

Inputs are the unchanged ruled receipt at `holes/labs/wm-contract/r-cascade/r-cascade-ruled.edn`, the R definitions in `/home/joe/code/ukrn-services-simulation/docs/aif-completeness.md`, and the cited futon3 library files. Token names are obtained only by inverting the receipt's 22-entry `:token->nat` map. “IF = ground, HOWEVER = inputs, THEN = blend” is treated as R10's working answer, not as a ruling.

## Step 1 — nearest written statement

Prediction before reading: **12** of 19 units would have a content-matched flexiarg. Actual: **14**. The threshold of six is met.

- `click-input` — **(b), prose only.** `REPORT-r-cascade-2026-10-05.md:152-160` defines the synthetic root and initial haves. No flexiarg conclusion describes this receipt boundary.
- `CTAU-CLASS` — **(c), nothing found.** It occurs as a graph/receipt node in `r-cascade-ruled.edn`; no IF/HOWEVER/THEN/BECAUSE definition was found in the requested corpus.
- `CTAU-TOKEN` — **(c), nothing found.** It occurs as a graph/receipt node in `r-cascade-ruled.edn`; no written blend statement was found.
- `R1` — **(a), `aif/belief-state-operational-hypotheses`**, `/home/joe/code/futon3/library/aif/belief-state-operational-hypotheses.flexiarg`. Its conclusion directly defines the operational belief map, rather than merely sharing “belief” vocabulary.
- `R13` — **(a), `aif/temporal-depth-beyond-greedy`**, `/home/joe/code/futon3/library/aif/temporal-depth-beyond-greedy.flexiarg`. `aif/hierarchical-and-temporal-depth` is another candidate; the selected pattern explicitly concludes that G must range over a horizon.
- `R14` — **(a), `aif/policy-precision-commitment-temperature`**, `/home/joe/code/futon3/library/aif/policy-precision-commitment-temperature.flexiarg`. Its conclusion directly introduces the commitment-temperature quantity.
- `R16` — **(a), `aif/grounded-actuation-not-reobservation`**, `/home/joe/code/futon3/library/aif/grounded-actuation-not-reobservation.flexiarg`. `aif/scheduled-observer-entrypoint` is adjacent, but the selected pattern specifically requires an external actuation witness.
- `R17` — **(a), `aif/structure-learning-by-model-reduction`**, `/home/joe/code/futon3/library/aif/structure-learning-by-model-reduction.flexiarg`. Its conclusion specifies BMR structure change and the same acceptance evidence.
- `R19` — **(b), prose only.** `/home/joe/code/futon5a/holes/excursions/E-the-dark-tower-3.md:368-369` defines independent make/cancel/exists axioms. No requested library flexiarg concludes that operation.
- `R2` — **(a), `aif/structured-observation-vector`**, `/home/joe/code/futon3/library/aif/structured-observation-vector.flexiarg`. Its conclusion directly normalises heterogeneous evidence into the observation vector.
- `R3` — **(a), `aif/predictive-coding-belief-update`**, `/home/joe/code/futon3/library/aif/predictive-coding-belief-update.flexiarg`. It explicitly concludes with the μ update and evolved variance.
- `R3a` — **(a), `aif/decomposed-prediction-noise`**, `/home/joe/code/futon3/library/aif/decomposed-prediction-noise.flexiarg`. This is a content match rather than an `@holds-at`: its THEN constructs named prediction-noise contributions.
- `R4` — **(a), `aif/shared-kernel-predictive-forward-model`**, `/home/joe/code/futon3/library/aif/shared-kernel-predictive-forward-model.flexiarg`. `aif/the-forward-model-is-a-pattern-cascade` is another candidate; the shared-kernel conclusion is nearer to R4's rollout definition.
- `R5` — **(a), `aif/expected-free-energy-scorecard`**, `/home/joe/code/futon3/library/aif/expected-free-energy-scorecard.flexiarg`. Risk and ambiguity patterns are candidates for individual terms, but this one concludes with the combined named-term G.
- `R6-candidates` — **(a), `aif/candidate-pattern-action-space`**, `/home/joe/code/futon3/library/aif/candidate-pattern-action-space.flexiarg`. It is the nearest statement for the candidate-building half of the split unit.
- `R6-select` — **(a), `aif/candidate-pattern-action-space`**, the same file. `aif/policy-precision-commitment-temperature` is adjacent to selection, but candidate choice is the closer conclusion; the mismatch is preserved below.
- `R7` — **(a), `aif/evidence-precision-registry`**, `/home/joe/code/futon3/library/aif/evidence-precision-registry.flexiarg`. Its conclusion directly makes per-channel trust explicit.
- `R8` — **(a), `aif/free-energy-as-tick-scalar`**, `/home/joe/code/futon3/library/aif/free-energy-as-tick-scalar.flexiarg`. Its conclusion directly emits the present-fit scalar.
- `SCAN` — **(c), nothing found.** It is present in `r-cascade-ruled.edn` and `p4ng/empirics-futon/aif-lean-dag.edn:54221-54241`, but no requested flexiarg or R prose defines it.

## Step 2 — blend-square and receipt comparison

Paraphrases remain below 25 words per cell. A flexiarg ID is a citation to all four blocks in that file. “Absent edge” refers to the ruled graph's `:absent` class, which is excluded from Lean support.

Receipt token authority (22): `A`, `B`, `C`, `C-tau`, `E`, `F-pi`, `G`, `Q-o-pi`, `Q-pi`, `T`, `U-t`, `a-conc`, `eps`, `interp`, `mu`, `o`, `pi`, `r`, `rates`, `ref-label`, `tau`, `wc`.

| unit | source | IF | HOWEVER | THEN | BECAUSE | in-tokens | out-tokens | agreement |
|---|---|---|---|---|---|---|---|---|
| `click-input` | R-prose, `REPORT-r-cascade-2026-10-05.md:152-160` | A click begins with state left by the prior click. | Five former sources otherwise have no common origin. | Emit the initial haves into the click. | One root makes the click a single cascade. | none | `A`, `B`, `C`, `T`, `U-t`, `o`, `tau` | `:no-flexiarg` |
| `CTAU-CLASS` | no definition; `r-cascade-ruled.edn` | No written IF found. | No written HOWEVER found. | No written THEN found. | No written BECAUSE found. | `T` | none | `:no-flexiarg` |
| `CTAU-TOKEN` | no definition; `r-cascade-ruled.edn` | No written IF found. | No written HOWEVER found. | No written THEN found. | No written BECAUSE found. | `T` | none | `:no-flexiarg`; absent edge names `C-tau`, which Lean lacks |
| `R1` | `futon3/library/aif/belief-state-operational-hypotheses.flexiarg` (`@flexiarg aif/belief-state-operational-hypotheses`) | Decisions should depend on tractable operational hypotheses. | Conversation alone cannot be cleanly updated or falsified. | Maintain and update a compact `mu` belief map. | Compact state makes prediction and update testable. | `o` | `mu` | `:flexiarg-silent-on-tokens`; IF does not name `o` |
| `R13` | `futon3/library/aif/temporal-depth-beyond-greedy.flexiarg` (`@flexiarg aif/temporal-depth-beyond-greedy`) | The agent should value outcomes beyond the next action. | Depth-one rollout remains reactive despite planning machinery. | Score policies across a discounted horizon and two timescales. | A policy is a temporally extended sequence. | `T` | `T` | `:disagrees`; text outputs horizon-level G/priors, receipt only passes `T` |
| `R14` | `futon3/library/aif/policy-precision-commitment-temperature.flexiarg` (`@flexiarg aif/policy-precision-commitment-temperature`) | Exploration versus exploitation should reflect operational pressure. | Without explicit `tau`, choice thrashes or tunnels. | Maintain `tau` as softmax commitment temperature. | One dial controls stochasticity without changing scoring. | `tau` | `tau` | `:agrees` |
| `R16` | `futon3/library/aif/grounded-actuation-not-reobservation.flexiarg` (`@flexiarg aif/grounded-actuation-not-reobservation`) | Selected action must materially change the observed world. | Re-observing one's construction can report success without acting. | Write an external typed witness that affects the next decision. | The model must not manufacture its own success. | `Q-pi`, `pi` | none | `:disagrees`; flexiarg THEN emits a witness, receipt is a sink |
| `R17` | `futon3/library/aif/structure-learning-by-model-reduction.flexiarg` (`@flexiarg aif/structure-learning-by-model-reduction`) | The agent should reorganise model structure from accumulated experience. | More concentration counts sharpen structure but never reorganise it. | Use BMR to accept a pruned or merged model. | Model evidence supports structural reorganisation without new data. | `mu`, `o`, `pi`, `wc` | `E`, `a-conc` | `:disagrees`; reduced structure is not either receipt output |
| `R19` | R-prose, `/home/joe/code/futon5a/holes/excursions/E-the-dark-tower-3.md:368-369` | Commitments are evaluated across time. | Making and cancellation are independently defeasible. | Apply make, cancel, and exists axioms with interval conditions. | Event-calculus persistence distinguishes live commitments. | `C`, `U-t` | `C`, `U-t` | `:no-flexiarg` |
| `R2` | `futon3/library/aif/structured-observation-vector.flexiarg` (`@flexiarg aif/structured-observation-vector`) | Observations must be comparable across steps. | Tools and feedback arrive in heterogeneous forms. | Maintain typed normalized observation `o`. | Stable observations enable weighting and scoring. | `o` | `interp`, `o` | `:flexiarg-silent-on-tokens`; `interp` is not named; absent outputs `o/ref-label` and `o` are omitted from Lean |
| `R3` | `futon3/library/aif/predictive-coding-belief-update.flexiarg` (`@flexiarg aif/predictive-coding-belief-update`) | Belief updates should be Bayesian-shaped, tunable, and variance-aware. | Unspecified updates over-trust or over-anchor observations. | Update `mu` using precision-weighted `eps`; evolve variance. | Predictive coding preserves meaningful uncertainty. | `eps`, `mu`, `o` | none | `:disagrees`; flexiarg THEN emits updated `mu`, receipt has no output |
| `R3a` | `futon3/library/aif/decomposed-prediction-noise.flexiarg` (`@flexiarg aif/decomposed-prediction-noise`) | Predictive noise should have named, auditable sources. | One opaque variance conflates distinct uncertainties. | Sum named structural contributions into predictive variance. | Named contributions make uncertainty diagnosable. | `mu`, `o` | `eps` | `:disagrees`; variance is not receipt `eps`; absent `eps→R7` is not carried in Lean |
| `R4` | `futon3/library/aif/shared-kernel-predictive-forward-model.flexiarg` (`@flexiarg aif/shared-kernel-predictive-forward-model`) | Predictive and live dynamics should share verifiable rules. | Separate shadow dynamics inevitably drift. | Wrap one pure dynamics kernel to produce next-state distributions. | One implementation keeps prediction aligned with enactment. | `A`, `B`, `T`, `interp`, `mu`, `pi`, `r`, `rates` | none | `:disagrees`; THEN predicts next state, while absent `A/Q-o-pi` and `A` edges mean Lean records no output |
| `R5` | `futon3/library/aif/expected-free-energy-scorecard.flexiarg` (`@flexiarg aif/expected-free-energy-scorecard`) | Selection should balance named risk, ambiguity, information, and cost. | An opaque scalar hides tuning and justification. | Compute and persist decomposed `G`. | Named terms make score disagreements testable. | `C`, `U-t` | `G` | `:agrees`; preferences `C/U-t` ground the produced score |
| `R6-candidates` | `futon3/library/aif/candidate-pattern-action-space.flexiarg` (`@flexiarg aif/candidate-pattern-action-space`) | Pattern choice should be bounded and legible. | An unrestricted action space makes evaluation arbitrary. | Construct a bounded candidate set `a` with inclusion reasons. | Constrained candidates make scoring meaningful. | `interp` | `pi`, `r` | `:disagrees`; text produces candidate set `a`, receipt produces `pi/r` |
| `R6-select` | `futon3/library/aif/candidate-pattern-action-space.flexiarg` (`@flexiarg aif/candidate-pattern-action-space`) | Pattern choice should be bounded and legible. | An unrestricted action space makes evaluation arbitrary. | Construct a bounded candidate set `a`. | Constrained candidates make downstream scoring meaningful. | `E`, `F-pi`, `G`, `pi`, `tau` | `Q-pi`, `pi` | `:disagrees`; chosen-policy distribution is not the flexiarg's candidate-set output |
| `R7` | `futon3/library/aif/evidence-precision-registry.flexiarg` (`@flexiarg aif/evidence-precision-registry`) | Low-trust evidence should be discounted explicitly. | Implicit weighting becomes unauditable rhetoric. | Maintain per-channel precision rates. | Explicit trust becomes a tunable control surface. | `A` | `rates`, `wc` | `:flexiarg-silent-on-tokens`; it names `rates`, not input `A` or output `wc` |
| `R8` | `futon3/library/aif/free-energy-as-tick-scalar.flexiarg` (`@flexiarg aif/free-energy-as-tick-scalar`) | Current belief fit needs a comparable scalar. | Candidate EFE `G` cannot substitute for present surprise. | Emit `F-pi` from observation, belief, and precision. | Per-tick F exposes surprise and validation movement. | `o`, `pi` | `F-pi` | `:agrees` |
| `SCAN` | no definition; `p4ng/empirics-futon/aif-lean-dag.edn:54221-54241` | No written IF found. | No written HOWEVER found. | No written THEN found. | No written BECAUSE found. | `a-conc` | none | `:no-flexiarg` |

## Agreement counts

| agreement | count |
|---|---:|
| `:agrees` | 3 |
| `:flexiarg-silent-on-tokens` | 3 |
| `:disagrees` | 8 |
| `:no-flexiarg` | 5 |
| **total** | **19** |

## Disagreements, text and receipt side by side

| unit | flexiarg THEN | receipt output |
|---|---|---|
| `R13` | “Score policies across a discounted horizon and two timescales.” | Passes `T` unchanged. |
| `R16` | “Write an external typed witness that affects the next decision.” | No output. |
| `R17` | “Use BMR to accept a pruned or merged model.” | `E`, `a-conc`. |
| `R3` | “Update `mu` using precision-weighted `eps`; evolve variance.” | No output. |
| `R3a` | “Sum named structural contributions into predictive variance.” | `eps`. |
| `R4` | “Produce next-state distributions from the shared kernel.” | No Lean output; graph-only absent edges name `A`, `Q-o-pi`. |
| `R6-candidates` | “Construct a bounded candidate set `a`.” | `pi`, `r`. |
| `R6-select` | “Construct a bounded candidate set `a`.” | `Q-pi`, `pi`. |

## Reading of R10's working answer

The working answer survives cleanly for R5, R8, and R14: their grounds correspond to receipt needs and their blends correspond to produced tokens. It is incomplete for R1, R2, and R7 because the prose omits part of one token boundary. It fails for eight units: some receipt tokens are intermediate wires (`T`, `eps`, `E`, `a-conc`, `pi`, `r`, `Q-pi`) rather than the semantic product named by THEN; R3, R4, and R16 terminate in Lean even though their patterns describe an updated belief, predicted state, or external witness. There the tokens play the narrower role of implementation inputs, evidence summaries, or selection carriers, while the pattern's blend lives outside the ruled receipt's carried-token model.
