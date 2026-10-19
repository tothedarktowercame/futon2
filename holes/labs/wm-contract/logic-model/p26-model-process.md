# P26 — R4 model/process distinction

**Disposition:** A16/R4 is a documentation discrepancy. This is a static source
reading at futon2 `b16534506f5fc0aecb62b9e35266e8c7a54a6ff3` (2026-10-19), not a
new serving observation, route probe, test run, or calibration result. It does
not change the forward model, the execution path, a ledger, or the p4ng paper.

## Finding

The R4 paper prescription is refuted as a description of this system. The paper
currently promises that a live deterministic kernel is factored out and called
by the predictive model (`/home/joe/code/p4ng/sec-catalog.tex:253-254`). The
actual R4 model is a pure function that produces a distributional prediction;
the live generative process is an author/reviewer workflow that can change an
external repository and is then observed through an artifact binding. There is
no source call from the latter into `forward-model/predict`, and no shared live
kernel has been found.

This preserves the AIF-relevant invariant more accurately: scoring paths share
one prediction model for their candidate forecasts; any difference between a
frozen prediction and a subsequently witnessed outcome is a calibration datum.
It is not evidence that the world ran a wrong function, nor evidence that the
model or an external action has been validated.

The PLOP A16 annotation is unreviewed, but its disposition follows its stated
revision policy: the implementation is not made wrong merely because the
simulation-oriented paper sentence is wrong. Its two source anchors at
`futon2@54d1deffd797` are ancestors of this reading commit; current line
locations and the current commit are used below. The current `forward-model`
docstring still contains the future-tense shared-live-step statement at
`src/futon2/aif/forward_model.clj:4-13`; that comment is itself stale prose,
not evidence that such a step exists.

## What R4 actually computes

`src/futon2/aif/forward_model.clj::predict:372-399` is pure: for a channel
action it returns an `:action`, `:predicted-events`, an updated belief
distribution, and `:next-observation {:mean ... :variance ...
:variance-status ...}`; for a cascade candidate it returns a token-state
distribution rather than a channel mean (`predict-cascade-step:348-370`).
`predict-channel-state:401-443` derives its mean by applying a declared
action-type effect to the supplied observation, supplies hand-modelled
per-channel variance, and records whether a zero variance was supplied or is
an absent deterministic-by-action-model term (`:408-428`). It performs no I/O,
repository mutation, agent dispatch, or world observation.

`predict-multi-horizon:308-346` repeats this model under the assumption that
the **same action repeats**, feeding each predicted state into the next model
step. Its `:trajectory`, `:final-state`, and `:horizon-steps` are forecast
objects. They are not a simulation of a future author job or an assertion that
the external process will take repeated identical steps.

The model's actual supplied state/action contract is documented by
`predict:375-394`; validity checks and action-specific effects are local to the
model (`valid-action?:50-63`, `predict-effects:74-81`). Therefore an agreement
between this output and a later artifact is a comparison across two distinct
systems, not proof of implementation identity.

## Predictive-consumer census

A repository search for direct calls to `forward-model/predict` and
`predict-multi-horizon` at the pinned commit found the following production
calls. Other namespaces may require `forward-model`, but are not counted as
consumers without a call site.

| consumer boundary | current source pin | use of prediction |
|---|---|---|
| EFE scoring | `src/futon2/aif/efe.clj::compute-efe:481-585,637-650` | Calls `predict` for every scored state/action. With a declared horizon of at least two it also calls `predict-multi-horizon`; the final predicted observation feeds risk/homeostatic terms while first-step variance remains the ambiguity/info input. |
| Cascade lane R4 stage | `src/futon2/aif/wm/cascade_decision.clj::cascade-lane:127-167,215-248` | Creates a cascade candidate for each candidate policy and calls `predict-multi-horizon` with the declared common horizon before R5 ranking. The same function explicitly says R16 here is only an enactment plan and real source edits are builder work. |

This establishes shared model use only among these cited scoring/selection
boundaries. It does not establish that every proposer uses the model, that a
prediction reaches every live execution, or that a scorer's output is calibrated
to the external world.

## Distinct live generative process and its observation

The authoritative ordinary path is explicitly described as real actuation in
`src/futon2/aif/full_loop_runner.clj:1-8`. It selects a strategic action,
dispatches an author and a distinct reviewer, verifies a resulting commit, and
records a typed result. At the operational boundary it reads the target
repository head (`:7327-7332`), dispatches the author through `dispatch!`
(`:7356-7369`), waits for the job (`:7384-7445`), and obtains an independent
artifact binding by comparing the author result against the repository
(`:7455-7501`). `task_execution_evidence.clj::fresh-artifact-binding:150-214`
keeps the observed head, claimed commit, timing window, and corroboration
separate; an unresolved or disagreeing claim is typed failure, not a predicted
state.

There is a separate selected-cascade execution seam, but it is not R4's live
kernel either. `wm/cascade_decision.clj::execute-selected-cascade!:1512-1523`
retains the selected identity, materialises and validates a cascade, compiles
it, saves it, and writes journal entries (`:1618-1694`). The full-loop runner
calls this seam after selection at `full_loop_runner.clj:6790-6806`. Its return
is a prepared/held execution frontier; it does not make
`forward-model/predict` the executor. The related close-loop adapter similarly
records an enacted result only after its own act gate and engine path
(`enact.clj::close-loop!:323-374`), and trace persistence retains that
`:realized-outcome` only when supplied (`trace.clj:578-584`).

Accordingly, the source establishes three different objects:

| object | producer | status of relation |
|---|---|---|
| predicted observation/belief or token-state distribution | `forward-model/predict` / `predict-multi-horizon` | pure model output used by the two cited scoring boundaries |
| execution attempt / repository artifact | full-loop author dispatch plus `fresh-artifact-binding` | external-process observation, independently bound to repository state when the path reaches it |
| realized-outcome record | close-loop/enactor seam and trace | a separate, present-only outcome carrier; its presence is not implied by a prediction |

No live call, reload, probe, or click was made for this packet. Hence this is
static reachability, not evidence that a particular author dispatch, cascade
execution, artifact binding, or outcome record occurred in the serving JVM.

## Prediction-versus-witness: calibration, not identity

The repository contains a structured route for scoring frozen predictions
against witnessed outcomes: `held_out_calibration.clj::observe-realised:587-609`
reads a token's declared locator at the run's artifact SHA, and
`::calibration-row:615-684` retains typed hygiene reasons whenever the run,
close, frozen prediction, or observed outcome cannot be joined. Its comments
explicitly call the realised outcome observed rather than asserted and warn
against resolving a prior run at later `HEAD` (`:587-592`). This is a suitable
model/process comparison shape, but reading its code does **not** establish a
current calibration sample or passing disposition.

The stronger realized-recording contract makes the same separation explicit:
calibration admission requires independently observed expected and realized
legs, forecast/realization evidence, time ordering, matching quantity/units,
and aligned windows (`realized_recording.clj::calibration-admitted?:162-180`).
It refuses the tempting shortcut of treating a missing or mismatched leg as a
numeric comparison.

Therefore the correct interpretation of a retained pair is narrow:

* predicted state differs from witnessed repository/outcome state: a model
  mismatch available for calibration, subject to the pair's pins and admission;
* predicted state agrees: one compatible observation, not proof that the world
  used the prediction function or that the model is universally calibrated;
* either leg absent, unpinned, or unaligned: no calibration conclusion.

This is consistent with the frontier ruling: missing evidence is retained as a
measured absence, not substituted with a successful effect or a zero.

## Exact proposed paper replacement

**Target:** replace the complete R4 paragraph beginning
`\paragraph{Shared Kernel for the Predictive Forward Model (R4).}` at
`/home/joe/code/p4ng/sec-catalog.tex:253-254`. This packet does not edit p4ng.

> \paragraph{Forward Model Compared with the Generative Process (R4).}
> \textbf{If} you want candidate actions to be evaluated against explicit
> expectations while the live work changes repositories and other external
> state. \textbf{However} a pure forecast and the world are different systems:
> making them textually identical would hide both uncertainty in the model and
> mismatch in the process, while separately maintained scoring models could
> disagree about the same candidate. \textbf{Then} use one explicit forward
> model for the scoring boundaries that predict candidate outcomes, and retain a
> separately pinned observation of the realised artifact or outcome whenever it
> is available. Compare a frozen prediction with that witnessed outcome as a
> calibration observation, retaining missing or unaligned legs as such; do not
> claim that the live process calls the prediction function. \textbf{Because}
> the forward model is the agent's generative model and external execution is
> the generative process: their discrepancy is information about calibration,
> not proof that either side secretly ran the other's implementation.

The catalogue summary row at `sec-catalog.tex:112` should correspondingly be
renamed from “Shared Kernel for the Predictive Forward Model” to “Forward Model
Compared with the Generative Process”, with brief role “Shares forecasts across
scoring and measures their mismatch to witnessed outcomes.” This is proposed
paper prose only, not a claim that the measurement has occurred.

## Proposed correlation annotation update

Replace the A16/R4 correlation entry with the following two-row mapping, so the
existence claim and the unavailable calibration evidence do not collapse:

| annotation | status | paper assertion | source anchor | evidence / limit | disposition |
|---|---|---|---|---|---|
| A16/R4, forward model | exists, not newly validated | one pure predictive model is shared by the cited scoring boundaries | `futon2@b16534506`, `forward_model.clj::predict:372-443`, `::predict-multi-horizon:308-346`; consumers `efe.clj::compute-efe:637-650` and `wm/cascade_decision.clj::cascade-lane:215-248` | static source reading only; no serving-route or calibration claim | document shared predictive consumers, not a shared live kernel |
| A16/R4, model/process relation | paper prescription refuted; calibration remains unvalidated here | live step and predictive model call one deterministic kernel | live process `full_loop_runner.clj:1-8,7327-7501`; outcome comparison protocol `held_out_calibration.clj::observe-realised:587-609`, `::calibration-row:615-684`; admission `realized_recording.clj::calibration-admitted?:162-180` | no retained pair was evaluated in this packet; agreement/disagreement has only the calibrated interpretation stated above | replace paper's shared-kernel claim with model-versus-witness comparison; do not manufacture a common live kernel |

The annotation remains unreviewed and its R4 status remains a documentation
correction. Nothing here upgrades the forward model, an execution route, or a
calibration mechanism to end-to-end validated.
