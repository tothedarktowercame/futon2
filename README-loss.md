# README-loss.md — the belly's burn-in: what improves, and how to watch it

*Companion to `README-clicks-and-ticks.md` (the clock the loop runs on) and the
excursion `~/code/futon3c/holes/excursions/E-arxana-clock.md` / the C-vector
work in `E-C-vector-live.md`. This note is the **observability** layer for the
one quantity in the WM's belly (Friston's C / R19) that is meant to **improve
over runs**: the probability-weighting of the predictive goal-outcome risk
(R19-KL).*

## What "loss" means here

The WM doesn't train against a labelled loss. The thing that should get
**sharper** over burn-in is the belly's **predictive risk** — how well it
discriminates a policy that will actually discharge a goal from one that won't.

Predictive goal-outcome risk (`futon2.aif.c-vector/predictive-goal-outcome-risk`)
scores each candidate action by the divergence of its **predicted** goal
outcomes from C. An entry the action advances is predicted satisfied with
probability **p**, so it contributes its *expected residual* risk:

```
contribution(advanced entry) = (1 − p) · weight · divergence
p = credit-satisfy-prob(action) = the R12 Beta-credit for the action's class
    (futon2.aif.intrinsic-values/credit-for; in [0,1], 0.5 prior)
```

- **p = 1** (point-mass): the old deterministic flip — an advanced goal is fully
  discounted (contributes 0). This is the `p→1` limit.
- **p < 1**: the goal is only *partially* discounted — the belly is appropriately
  sceptical that an action of a low-follow-through class will really discharge it.

## The burn-in (what we hope to watch improve)

At cold start every action class sits at the **Beta(1,1) prior, p = 0.5**, so the
predictive risk is a uniform half-discount — little discriminative signal. As the
R12 outer loop (`scripts/wm_outer_loop.clj`, the daily `:wm-outer-loop` tick)
accumulates **operator follow-through** per class, each class's Beta posterior
moves:

- a class the operator *acts on* when the WM recommends it → α grows → **p → 1**
  → the belly discounts that class's goals more → it trusts those policies;
- a class the WM over-recommends but the operator ignores → β grows → **p → 0**
  → the belly stops discounting → it down-weights those policies.

So the "loss" curve to watch is **per-class credit sharpening away from 0.5**, and
the downstream effect: **wider goal-outcome-risk margins** between good and bad
candidate policies (the belly steering selection more decisively).

## What to watch, and where

| Signal | Where | Healthy trajectory |
|---|---|---|
| Per-class credit `p` | `intrinsic-values/credit-for <class>`; the `code/v05/wm-hyperparameter-update` hyperedges on :7071 | moves off 0.5 as follow-through accrues |
| Goal-outcome risk spread | `compute-efe`'s `:G-goal-outcome` across `rank-actions` candidates | margin widens (sharper re-rank) |
| Belly composition | `c-vector/current-c-vector` (453 entries, 5 channels) + `:n-source` | stays fresh (freshness guard) |
| Chosen vs credit | the WM trace `:decision` vs the chosen class's credit | chosen classes trend high-credit |

The R12 learner's per-class narrative is printed by `wm_outer_loop` each run
(α/β posterior, intrinsic-value, follow-through rate) — that log **is** the
burn-in trace.

## Honest caveats (read before reading the numbers)

- **Today p = 0.5 everywhere** — no follow-through has accrued, so the
  credit-weighting currently just halves the discount uniformly. The
  *mechanism* is wired and live; the *improvement* requires (a) the WM loop
  running on a clock (cron `:wm-scheduled` + `:wm-outer-loop`, see
  README-clicks-and-ticks) and (b) real operator follow-through over many runs.
  Small effect now, by design.
- **p is per action-CLASS, not per (action, goal)** — a coarse proxy. The
  per-goal probability is the durable `discharged-by` PROOF join (E-C-vector-live
  §11, gated on M-populate-substrate-2 D4). Until then, class-credit is the
  honest stand-in.
- **This is expected-divergence, not full information-theoretic KL.** The
  `(1−p)·divergence` form is the risk in expectation under a Bernoulli predicted
  outcome; the entropy term of the canonical `KL(Q‖C)` is a further refinement,
  not yet taken.

## See also

- `src/futon2/aif/c_vector.clj` — `predictive-goal-outcome-risk`,
  `credit-satisfy-prob` (the implementation).
- `src/futon2/aif/intrinsic_values.clj` — the R12 Beta-credit learner (the `p`).
- `scripts/wm_outer_loop.clj` — the daily learner tick; `wm_scheduled_run.clj` —
  the hourly scoring tick (refreshes the belly first).
- `holes/E-C-vector-live.md` — the belly excursion (§10 predictive, §11 the
  durable join follow-on); `README-clicks-and-ticks.md` — the loop's clock.
