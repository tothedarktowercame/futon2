# C454 — discovery: eight free choices in the AIF registry

**Date:** 2026-09-01. **Scope:** query 3c of
`holes/TN-edge-review-aif-wiring.md`. This is observation for Joe's rulings,
not a ruling. Neither registry was edited.

For each choice, “alternative” describes an available formulation, not a
recommendation. Every code claim below has a source pointer; absence claims
use **not found**.

## 1. `:free-energy-form`

### What the code does now

The live R8 diagnostic is a Gaussian/Laplace channel-grain quantity:
`compute-variational-free-energy` requires channel prediction errors with
precision and sums their weighted squared errors
(`src/futon2/aif/free_energy.clj:184-205`). The judge computes it from its
prediction-error map (`scripts/futon2/report/war_machine.clj:4450-4452`) and
later records it (`:4753`). It is not passed into R5: the state supplied to
`efe/rank-actions` omits it (`:4458-4466`, `:4536`).

The policy side separately computes a discrete-style expected-free-energy
controller score from predicted outcomes (`src/futon2/aif/efe.clj:601-619`,
`:782-794`). A discrete categorical *variational F* implementation in the live
War Machine was **not found**.

R17 does not consume this R8 scalar. The live judge only records F
(`war_machine.clj:4753`); offline R17 replays model-reduction cases
(`src/futon2/aif/r17_offline.clj:1-6`, `:64-96`), and A4a calculates BMR
evidence from full/reduced Dirichlet models
(`src/futon2/aif/a4a.clj:126-159`). The R8 disposition therefore does depend on
the chosen form numerically, but no downstream R5 or R17 dependency was found.

### What the formulations offer

- The present Laplace form offers a channel-level diagnostic in the same units
  as Gaussian prediction errors and their learned precisions
  (`free_energy.clj:184-205`). Buckley et al.'s continuous/Laplace family is
  documented in `C453-citation-verification.md:25-31`.
- A categorical F offers state/policy posterior inference in the same discrete
  POMDP representation used by the policy side; Da Costa's formulation is
  documented in `C453-citation-verification.md:32-39`. A local implementation
  is **not found**.

### What would change under the other option

R8's reported value, inputs, and interpretation would change from Gaussian
channel residual energy to a categorical variational bound. Current R5 and R17
would not change merely by replacing R8, because neither reads its value
(`war_machine.clj:4458-4466`, `:4753`; `a4a.clj:126-159`). Making them depend
on it would require new wiring, **not found** today.

## 2. `:temperature-update`

### What the code does now

The live default is `:selection-gain-only`, giving `tau_eff = 1/g`; score
spread is an environment-selected rollback mode
(`scripts/futon2/report/war_machine.clj:238-248`). The gain is explicitly an
engineering control, not variational policy precision
(`src/futon2/aif/selection_gain.clj:1-15`).

There is a per-tick *attempted fold*: the judge reads persisted gain state and
calls `fold-realized-outcome` on the previous trace each tick
(`war_machine.clj:4332-4361`). The value changes only when a well-formed,
previously unseen realised outcome is present; otherwise it holds
(`selection_gain.clj:173-206`). The module states that this field is absent in
today's live path and simulation-only (`:187-193`). Thus code executes an
update check per tick, but observed live τ normally remains at its prior. A
β/γ variational precision update was **not found**.

### What the formulations offer

- Calibration/selection gain offers bounded adaptation to realised-versus-
  expected controller performance (`selection_gain.clj:17-25`, `:165-206`).
- The discrete synthesis's policy precision offers Bayesian/variational
  confidence over policies rather than realised engineering performance;
  Da Costa's inverse-temperature treatment is located in
  `C453-citation-verification.md:32-39`. A corresponding live update is
  **not found**.

### What would change under the other option

The learning signal and state authority would change: τ would be inferred from
policy-posterior precision rather than folded from realised controller
performance. The `selection-gain` state, deduplication, and
`realized-outcome` dependency at `selection_gain.clj:173-206` would no longer
be the temperature update. The selection formula could retain a temperature
input (`src/futon2/aif/policy.clj:72-80`), but its producer would change.

## 3. `:habit-prior`

### What the code does now

E is genuinely non-uniform in recorded runs. The 2026-08-31 trace contains
`:samples 791` and unequal counts (for example 320, 108, 69, 1) in
`data/wm-trace/wm-trace-2026-08-31.edn:1`. The same record's decision
explanation reports `lnE` range `3.0445224377234226` and biases including
`-2.0614230361771577` and `-5.10594547390058` (`:1`).

The code derives those values from a Dirichlet-smoothed frequency table:
selected actions increment counts (`src/futon2/aif/habit_prior.clj:70-89`),
`log-priors` returns posterior-predictive log probabilities (`:100-119`), and
the biases are attached to candidates (`:121-163`). The judge loads persisted
state or folds the trace corpus (`scripts/futon2/report/war_machine.clj:4260-4284`)
and attaches it at `:4539-4546`.

But E is not selection-authoritative at the live strategic boundary. That
branch chooses the first controller-ranked entry and records
`:habit-prior-applied? false` / `:habit-authority :counterfactual-only`
(`src/futon2/aif/policy.clj:234-271`). The 2026-08-31 trace confirms
`:habit-authority :counterfactual-only` (`data/wm-trace/wm-trace-2026-08-31.edn:1`).

### What the formulations offer

- The present learned non-uniform E offers an empirical prior from historical
  selections, inspectable as a counterfactual ranking
  (`habit_prior.clj:70-119`; `policy.clj:234-271`).
- Uniform E offers no learned policy bias: selection reduces to the G-based
  order. The no-prior branch explicitly chooses the best G-ranked entry
  (`policy.clj:387-410`).

### What would change under the other option

Making E uniform would remove the observed `lnE` differences and the
counterfactual habit winner, but would not change today's live strategic
choice because that boundary already abstains from applying E
(`policy.clj:247-271`). Making the existing non-uniform E authoritative would
instead allow `ln E-G/tau` to change the selected policy; that behavior exists
in the non-strategic branch at `policy.clj:411-438` but is not the live
strategic boundary.

## 4. `:policy-depth`

### What the code does now

The live judge is conditionally fixed: `wm-horizon-steps` is `3` when the
anticipation snapshot contains loaded events, otherwise nil/single-step
(`scripts/futon2/report/war_machine.clj:4480-4487`), and is passed into EFE at
`:4516-4522`. EFE invokes multi-horizon prediction only for depth at least 2
(`src/futon2/aif/efe.clj:601-609`). Thus depth varies between invocations based
on anticipation state, but not per candidate policy.

A separate rollout library accepts configurable `:horizon`/`:depth`, default 2
(`src/futon2/aif/rollout.clj:166-171`, `:212-230`).

### What the formulations offer

- Fixed depth offers comparable cost and scores across ticks; today's live
  multi-step value is fixed at 3 (`war_machine.clj:4485-4487`).
- Adaptive/configurable depth offers more or less lookahead according to
  context or budget; the generic mechanism exists in
  `rollout.clj:166-171`, but per-policy adaptive depth is **not found**.

### What would change under the other option

Always-fixed depth would remove the current anticipation-dependent switch.
Per-policy/adaptive depth would change which predicted terminal state feeds G
and the amount of forward-model work (`efe.clj:601-614`), and would require a
new producer for T; such a producer is **not found**.

## 5. `:hierarchy`

### What the code does now

The inspected rollout explicitly says it is “flat temporal rollout, not nested
fast/slow hierarchy” (`src/futon2/aif/rollout.clj:166-176`). The live judge
performs one observation/belief/EFE/selection pass
(`scripts/futon2/report/war_machine.clj:4220-4584`). More than one realised AIF
generative level was **not found**.

There is separate hierarchical budget plumbing
(`src/futon2/aif/hierarchical_budget.clj:1` and
`hierarchical_budget_adapter.clj:1`), but a connection making it a nested AIF
belief/policy hierarchy was **not found**.

### What the formulations offer

- The present single level offers one inspectable loop; R15 can remain
  orchestration/plumbing without adding equation imports.
- A multi-level hierarchy offers fast/slow or lower/higher belief and policy
  loops whose messages become additional imports. No such live wiring was
  found at the pointers above.

### What would change under the other option

Multiple levels would require explicit level state, cross-level messages,
separate update schedules, and trace attribution. Those structures would alter
the dependency graph around R15; they are **not found** in the live judge.

## 6. `:learning`

### What the code does now

BMR is an offline path: `r17_offline` declares pure replay with no live writes
(`src/futon2/aif/r17_offline.clj:1-6`) and runs cases at `:64-96`; A4a performs
Dirichlet BMR and concept reduction (`src/futon2/aif/a4a.clj:126-159`). It is
not driven by live R8 F (`scripts/futon2/report/war_machine.clj:4753`).

Lean defines the positive nonempty carrier `DirichletConcentrations`
(`/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:6380-6385`) and its
witness (`.../DirichletConcentrationsWitness.lean:8`). A live update of the A
observation model from those concentrations was **not found**.

### What the formulations offer

- BMR offers structural reduction by comparing a full model with reduced
  priors/posteriors (`a4a.clj:126-159`; source scope in
  `C453-citation-verification.md:48-52`).
- Dirichlet A learning offers parameter learning of observation likelihoods;
  Da Costa eqs. 17-21 are identified in
  `C453-citation-verification.md:32-39`. Only the Lean carrier/witness is
  present locally; runtime learning is **not found**.

### What would change under the other option

Running Dirichlet A learning would mutate/update the observation model used by
prediction, requiring learned concentration state and a runtime wire into the
forward model. BMR instead changes structural/model hypotheses offline. These
are not interchangeable switches; enabling A learning would add a new live
learning path, **not found** today.

## 7. `:policy-posterior-node`

### What the code does now

R6 owns the executable policy choice: the judge calls `policy/select-action`
and tags R6 (`scripts/futon2/report/war_machine.clj:4573-4584`). The function
computes temperature, softmax weights, and a deterministic selection
(`src/futon2/aif/policy.clj:300-438`). No dedicated Q(pi) R-box and no other
R-node implementation were found.

### What the formulations offer

- Keeping Q(pi) at R6 co-locates posterior scoring with candidate selection;
  this matches the current call and route tag (`war_machine.clj:4573-4584`).
- A dedicated node would distinguish the posterior distribution from the
  candidate-policy set and from the selected action. Such a box is **not
  found** in `control-stages.edn` or the live route (the absence is also stated
  in `holes/labs/wm-contract/aif-equations.edn`, choice
  `:policy-posterior-node`).

### What would change under the other option

A dedicated node would add a named output and edges E/G/tau/pi→Q(pi) and
Q(pi)→selection, separating computation from choice. Keeping R6 requires no
runtime change but leaves those meanings co-located. This report does not rule
which home is better.

## 8. `:selection-rule`

### What the code does now

The code is deterministic; it does not sample Q(pi). The live strategic branch
chooses the first non-no-op controller entry (`src/futon2/aif/policy.clj:247-250`).
The ordinary no-prior branch chooses the first G-ranked entry (`:387-410`), and
the prior-aware branch uses `apply max-key` on `ln E-G/tau` scores
(`:411-438`). Both branches calculate and record softmax weights, but a random
draw from them is **not found**.

The separate R16 enactor does not repair this into posterior sampling: it
chooses the first passing act gate (`src/futon2/aif/enact.clj:287-316`). Thus
actual actuation is “first passing gated ranked action,” while the strategic WM
path only records a pending recommendation (`policy.clj:265-270`).

### What the formulations offer

- Argmax/rank-first offers deterministic reproducibility and always takes the
  highest-scored admissible option. This is the implemented behavior at the
  pointers above.
- Sampling offers stochastic exploration proportional to Q(pi). A WM sampler
  is **not found**. The cited literature also does not settle one unique rule:
  Da Costa eq. 11 and Friston eq. 2.3 differ, as verified in
  `C453-citation-verification.md:63-66`.

### What would change under the other option

Sampling would require an RNG/draw at the selection seam, recorded random
provenance, and tests of distributional rather than deterministic behavior.
It could select a non-maximal policy with nonzero posterior mass. None of that
machinery was found in `policy.clj:300-438` or `enact.clj:287-316`.

## Compact observations for ruling

- R8 uses Laplace channel F, but R5 and R17 do not consume it.
- Tau's update check runs per tick, but its live learning signal is currently
  absent; the implemented signal is engineering selection gain, not
  variational precision.
- E is demonstrably non-uniform in a run, yet deliberately counterfactual at
  the live strategic choice boundary.
- T switches between single-step and fixed depth 3 according to anticipation;
  per-policy depth is not found.
- The live loop is single-level; R15's AIF hierarchy is not found.
- BMR is offline; live Dirichlet learning of A is not found.
- Q(pi) is implemented at R6 because no dedicated box exists.
- Selection and final gate enactment are deterministic; posterior sampling is
  not found.

These are observations only. They do not change
`:status :observed-not-decided` and do not select an alternative.
