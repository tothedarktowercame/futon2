# R20 valence carry: discovery, 2026-10-22

Scope: current checkout source, not an assertion that all these revisions are
loaded in the serving JVM. No source or tests changed. This answers packet
`b4936878c`; codex-4's affective module is concurrent work and is not treated as
an installed production caller.

## 1. Carry: distinguish existing transport from absent semantics

**There is no existing live R20 valence carry, nor an established R20 d-count
carry on this route.** The ten-click allocation does not select an execution
route. Ordinary clicks use the full-loop runtime; our frozen selection and
construction scripts and subsequent Agency tasks did not issue a click.
`frozen_cascade_steps.clj:17` defines offline stages ending at
`:dispatch-prepared`; `:task-executed false` is retained at line 124.
`full_loop_runtime.clj:570` and `:600` separately define production cascade
resume/continuation, explicitly without selection or spending a click.
The project evidence in `runs/frozen-live-sections-2026-10-22/allocation.json`
and `budget-final.json` records zero project clicks. Thus commits `b96962fce`,
`56c0fe05f`, and `8889d1854` do not demonstrate an adjacent-click carry.

**The useful existing chronological transport is the small run stream.**
`previous_run.clj:36` names `run-stream.edn`. At the beginning of an ordinary
run, `full_loop_runner.clj:8168` calls `previous-run/start!` before selection,
then stores its result as `:q6/predecessor` at line 8187 and binds it at line
8200. `previous_run.clj:145` reads the predecessor before appending this run's
`:started` event. The reader checks the earlier run's sealed terminal and
returns `{:status :present :previous terminal}` (`previous_run.clj:114-142`).
At close, `full_loop_runner.clj:1185` constructs the terminal; line 1404 calls
`previous-run/close!` after the run record is written. The closed stream event
holds `:record-file`, `:q6 terminal`, and optionally `:q8-frontier`
(`previous_run.clj:153-163`). The terminal currently holds choice, outcome and
selection-input digest, **not valence** (`previous_run.clj:295-311`).

**Proposed carrier, explicitly not an existing key:** put a sealed
`:valence-carry` alongside the end-of-click evidence in the run record and
include the same small carrier in the sealed Q6 terminal. The next reader
would then consume `[:q6/predecessor :previous :valence-carry]`. It should name
source run/observation, q-before, q-after, beta-prior, beta-posterior and update
status, and bind the precision evidence used. Implementing this requires
extending the terminal constructor/writer and passing the consumed q into the
outer evidence provider; adding a diagnostic field alone changes no prior.
This transport avoids parsing a huge run file and preserves explicit run
identity. For overlapping runs, the current predecessor rule can report the
latest earlier start as unclosed; it must not silently pick a different q.

Cold start today returns `:absent :no-earlier-started-run`
(`previous_run.clj:108`); a started-but-unclosed predecessor is separately
`:absent :previous-run-not-closed` (line 129). **There is currently no cold-start
q.** A new valence integration must explicitly declare its first q, e.g. the
packet's `{+ 1/2, - 1/2}`, with provenance. Invalid or unclosed carry is not the
same fact as first-ever initialization. Until beta-bar exists, record an
unperformed update, not an invented posterior or a successful carry witness.

Other state paths should not be mistaken for that missing carrier:

* Habit: `enactment_fold_source.clj:36` reads persisted flight increment
  receipts; line 63 folds them. `enactment_habit.clj:87` constructs a witnessed
  increment, line 120 deduplicates folds, and line 140 computes masses.
  `scripts/futon2/report/war_machine.clj:6835` supplies the reconstructed
  `:enactment-fold` to the decision. But `wm/cascade_decision.clj:1332-1341`
  returns the already selected outer-policy carrier before the alternate
  reading-family selector consumes this fold. The recorded
  `:habit-fold-snapshot` (`full_loop_runner.clj:1219`) is not proof that its
  masses entered E. See the earlier habit-count note for flight/run evidence.
* Historical token/d-like state: the report reads the last trace at
  `war_machine.clj:6468-6470` and supplies
  `:token-belief-predecessor-trace` and
  `[:decision :selection-certificate :token-belief-stage :prospective-carry]`
  at lines 6815-6819. `token_belief_carry.clj:22-53` explicitly describes the
  legacy no-update chain and fresh-fact initialization. Its v2 stage at line
  55 awaits observation admission, rather than creating a learned posterior.
  `token_belief_predecessor.clj:146-189` can consume a verified temporal
  posterior, otherwise retaining declared initialization. These facilities
  are not evidence that the selected outer route carries R20 d counts.
* The live outer learner has **different** Dirichlet outcome counts:
  `outer_learning_runtime.clj:19-43` reads pinned run records from the static
  authority plus dynamic manifest and reconstructs beliefs;
  `outer_target_belief.clj:130-158` updates per-target `:alpha` from classified
  outcomes. Runtime supplies the manifest path (`full_loop_runtime.clj:462`)
  and the runner registers a terminal record (`full_loop_runner.clj:8348-8355`;
  `outer_learning_manifest.clj:41`). The cold prior is declared by
  `resources/wm/outer-learning-authority.edn:5`, constructed at
  `outer_target_belief.clj:15`. These outcome concentrations are not the
  two-hidden-state initial belief or R20 d counts. The earlier retained-run
  audit, `NOTE-r20-lean-catchup-2026-10-22-answers.md:43-48`, found no d carrier
  in either October 21 record.

## 2. Precision posterior inventory and missing F

**The live outer-policy route does not compute beta-bar today.**
`outer_policy_objective.clj:59-74` explicitly records F absent, precision
posterior absent, and beta-posterior absent, with
`:no-precision-update-on-live-route`; beta-prior is `1/gamma`. Its policy
selection probabilities are a policy prior for the precision equation, not
an observation-conditioned posterior. Current gamma/G/E alone cannot supply
an evidence-derived beta-bar. Substituting F=0 or copying beta-prior would
manufacture the missing observation update.

Call-site enumeration used searches across `src/` and `scripts/` for
`converge-beta`, `carry-beta`, `cascade-beta-update`, `beta-dark-carry`,
`beta-posterior`, and precision-carry consumers:

| Facility | Required F / evidence | Reachability and consequence |
| --- | --- | --- |
| `policy_precision.clj:95`, `converge-beta` | Aligned G and policy-specific F vectors, plus aligned lnE when required; solves pi0=softmax(lnE-gamma G), pi=softmax(lnE-F-gamma G), residual=(beta-prior-beta)+(pi-pi0) dot G (lines 103-118). | Pure solver; no direct call from the live outer objective. Numeric posterior returned at lines 301/338 does not create its missing F input. |
| `policy_precision.clj:503`, `carry-beta` | Previous ranked policies joined by identity to retrospective F and retained G; solves at line 550, holds prior if not solved (line 556). | Dark carry wrapper, not the outer click update. Default beta for missing state is defined at line 390. |
| `beta_habit.clj:38`, `carry` | Same F/G join, plus sourced candidate habit biases (lines 51-63), put in both distributions. | Adapter around the preceding solver, not an additional observation producer. |
| `war_machine.clj:586`, `beta-dark-carry` | Previous trace's policy-precision-state plus F_pi fields for the previous field/current observation (lines 589-615). | Definition only in production source search; no production invocation found. `policy_free_energy.clj:41` supplies Gaussian predictive-error F for numeric prediction/observation channels and variances, not the outer policy's outcome evidence automatically. |
| `policy_precision.clj:598`, `cascade-beta-update` | Complete candidate-ID -> F mapping, actual retained G and positive habit weights; infinity permitted as exact contradicted-prediction limit (lines 607-626). | Calls solver at line 665. Missing F holds/refuses rather than producing an updated beta. |
| `policy_precision_carry.clj:87`, `advance` | Independently admitted predecessor task observation plus frozen dispatched family/model. `retrospective-f` at line 36 computes each policy's -log probability of the measured event at declared tau, marginalizing unmeasured coordinates. | Calls cascade update at line 136. A real potential retrospective mechanism, but no production invocation of this namespace's `advance` was found. Binding validators in d-task code are not update calls. Outer route has neither this frozen family binding nor its full retrospective F vector wired into the precision row. |
| `reading_policy_precision.clj:294`, `carry-beta` | No F: initializes declared beta, checks model identity/redeclaration, or preserves previous beta (lines 313-359). | Actually called by alternate reading-family certification at `wm/cascade_decision.clj:1122`; reads previous certificate at line 1100, retains receipt at line 1282. It carries a rate, **does not infer beta-bar**, and outer handoff bypasses this branch at line 1332. |

Two similarly named quantities are not substitutes: report microsteps update
per-channel sensory precision (`war_machine.clj:6245`, carry at lines
6521-6523), not the policy inverse-temperature posterior; `zeta_posterior.clj:35`
is a Beta-distribution posterior for another parameter, not beta-bar in this
precision equation. Codex-4's affective module consumes supplied beta values;
it does not create the missing policy-observation F.

The live route does retain terminal outcomes through the manifest and frozen
per-policy predictive evidence. Those are candidate raw material for a future
properly timed, identity-aligned observation model. They are not presently
converted to the required F or used in a solve. Do not equate the current
selection-time G, novelty, risk, or outcome Dirichlet alpha with that F.

## 3. Descending beta changes the selection input

`resources/wm/outer-learning-authority.edn:3` declares gamma=1.0;
`outer_policy_provider_runtime.clj:53` reads it into the evidence declaration,
`outer_policy_evidence.clj:214` seals it into evidence, and
`outer_policy_objective.clj:84,139-145` consumes it in
`objective = gamma*G - lnE` (logit `lnE-gamma*G`). To descend from the specified
valence q, beta=1.25 means gamma=0.8, and this gamma must replace the value in
the evidence **before sealing/scoring**, not merely in the diagnostic precision
row. This changes the recorded softmax probabilities whenever G differs. The actual
selector chooses the minimum objective and uses the draw only among exact ties
(`outer_policy_objective.clj:201-212`, partial-field analogue at lines 154-164);
it does not sample the full softmax. With unequal lnE, changing gamma can alter
the winner or tie set. With equal lnE, positive rescaling preserves G order
and mathematical ties, so the same tie draw preserves the winner (subject to
numeric tie comparisons). Equal-G probabilities can also be unchanged. Thus
this is a selection-input/behaviour change in general, not a recording-only
patch, but a changed winner is not guaranteed for the frozen example.
