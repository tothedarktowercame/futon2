# Excursion: E-aif-ants-epistemics — bring the epistemic half online

**Type:** E-prefix excursion (bounded scope-out, single-agent-owned end-to-end).
**Spawned:** 2026-08-01, on the close of M-aif-stack S1 (Slice 5 confirmation).
**Owner:** TBD. **Operator:** Joe.
**Parent:** `M-aif-stack.md`. **Registration machinery:** `M-experiment-build-match.md`.

## IDENTIFY — what the measurement established

The ant controller has **high causal authority and an inert epistemic half**.

- Replacing the scorer with uniform-random over the same admissible set costs
  `133.63 [94.13, 173.13]` yield on patchy. The scoring drives behaviour.
- Ablating the epistemic terms leaves the run **bit-identical** on 30/30 seeds
  (directed-EIG, sparse and snowdrift), 28/30 (directed-EIG, patchy), and 25–28/30
  (info-gain, all scenarios).
- Ablating the risk term changes the run on 28/30, 26/30 and 30/30.

So what the implementation calls expected free energy is, causally, its risk leg.

**Joe's framing, which is the right one:** this is not separable from the policy
concern he raised first. In AIF the policy is the argmin of risk + ambiguity, and
the ambiguity term is the *entire* reason a policy explores rather than exploits.
An inert epistemic term does not make the policy slightly worse — it makes it
**structurally a greedy risk minimiser**. Policy selection is where the failure
shows; the epistemic inertness is where it comes from.

Note there are **two** exploration channels and both are dead:

1. the ambiguity term, which cancels because it is action-independent;
2. the commitment temperature τ, annihilated because selection is `max-key`.

## Scope

**IN** — four repairs, each with a machine-checkable precondition and an
empirical postcondition:

| # | repair | what is wrong now | contract req |
|---|---|---|---|
| R-a | carry action-dependent predicted variance into EFE | `pred-variances` is `(:var mu)`, the *current* belief variance, identical across candidates (`policy.clj:612-624`). `forward-predict` supplies variance for four channels and `predict-observation` discards it. | R4, R5 |
| R-b | replace the EIG proxy with expected posterior entropy reduction | "directed EIG" computes `food-prob × uncertainty` (`food_belief.clj:101-149`), which is not an expectation over induced posterior updates | R5 |
| R-c | make τ live, or stop claiming it | selection is `(apply max-key :p policies)`; argmax over `−G/τ` is argmax over `−G` for every τ>0, so `choose-tau`'s whole apparatus is annihilated one line later | R6, R14 |
| R-d | soften the mode gate | `affect/next-mode` is a deterministic hysteresis FSM over eight hardcoded thresholds that hard-gates the action set; the advertised `q(m|o)` posterior (`infer-mode`) is reachable only through dead `efe-tilt` | R1, R6 |

**OUT** — anything about whether the ants get *better*. See the guard below.

## The acceptance criterion is already machine-checked

This is the part that makes the excursion cheap. Each repair's success condition
is **an axis flipping from non-navigable to navigable**, and that is exactly what
`clean_to_lean.py`'s render gate already tests:

- **Before:** `:canonical-ambiguity` carries `:score-varies? false` with a
  justification, and the render *refuses* it as a treatment. That refusal is the
  current, correct state.
- **After R-a:** the same axis must carry `:score-varies? true` and the
  registration must render **as a treatment arm**, which requires a proof of
  `Axis.Navigable`. A repair that does not change behaviour cannot pass, because
  the wrong proof burden is undischargeable — verified by attack, 2026-08-01.

The empirical postcondition uses claude-7's relativised form: after the repair,
ablating the term must change the run on **≫ floor** of seeds, where the floor
for this deterministic simulation is 100% identity. Before: 30/30 identical.
Target: a stated, preregistered fraction, fixed before the repair is measured.

## The guard — do not conflate faithfulness with capability

A repaired epistemic term will make the ants an **honest** AIF implementation.
It may not make them a **better** forager, and the confirmation gives specific
reason to expect it might not: `classic` beats `aif-full` on yield in every
scenario, and the AIF controller's demonstrated virtue is *survival*, which the
risk leg already supplies.

So the excursion registers two claims separately, and neither is allowed to stand
in for the other:

- **Faithfulness claim** (the goal): the epistemic term is causally live, the
  axis is navigable, and the label is earned. Testable, and this is what the
  excursion is for.
- **Capability claim** (open): the repaired controller forages better on some
  named endpoint. Not assumed, not required for the excursion to succeed, and
  subject to `M-wm-capability-claim.md` — *say what it should be good at before
  measuring, or the measure will be whatever is measurable.*

If R-a lands and the ants forage no better, that is a **result**, not a failure:
it would say the canonical epistemic term earns its place on honesty grounds and
not on performance, which is worth knowing and is not currently known for any
system in `M-aif-stack`.

## The capability claim, supplied by Joe (2026-08-01) — and the hole it exposes

> *"If it makes them honest not better, that's fine with me. Actually this is
> pretty much Spinoza's conatus in a nutshell — it's quite obvious that an honest
> ant is more 'free', which means that even if they aren't strictly better at the
> task that is set them, they should be better at 'learning'."*

This is not a gloss. It is the capability claim the plan above was missing, and
`M-wm-capability-claim.md` says a capability claim must exist *before* measuring
or the measure becomes whatever is measurable.

**The mapping is exact, not analogical.** The risk term already *is* conatus in
the narrow sense — it is the survival regulator, and the confirmation measured it:
sparse starvation `0.133` with it, `0.633` without. What Spinoza separates is
*passive* perseverance, determined by external causes, from *active*
perseverance, following from one's own adequate ideas. The ants persevere
**passively**: a hysteresis FSM over eight hardcoded thresholds does it for them.
An epistemic term is the machinery that produces adequate ideas — it acts to
reduce uncertainty about the agent's own model. So the repair converts passive
perseverance into active perseverance, and *that* is what "more free" names here.

### The empirical signature, and why the current design cannot see it

A greedy risk minimiser optimises against a **fixed** preference and has no drive
to improve its model. On a task it is tuned for it can be excellent — the
confirmation shows exactly that, with `classic` beating `aif-full` on yield in
every scenario. What it cannot do is **adapt**.

An agent with a live epistemic term spends yield on uncertainty reduction. On a
fixed task that is a cost, which is why R-a may well *lower* yield. The payoff is
under **distribution shift**.

**Our design has no shift.** Every arm runs 300 ticks in one fixed environment,
food layout drawn once from a seed. There is no learning to be better at. So:

> The current experiment is structurally incapable of detecting what the
> epistemic term is for.

That is this morning's finding — *the outcome measure may not see what the thing
is good at* — arriving one level up. We measured yield on a fixed environment
because yield on a fixed environment was measurable.

### R-e: the regime-shift arm

Registered before R-a is measured, so the capability endpoint exists before the
faithfulness repair lands:

- **Manipulation:** the food regime changes mid-run — patch locations move, or
  `patchy → sparse` — at a fixed tick, identical across arms and seeds.
- **Endpoints:** post-shift yield; **time-to-recovery** (ticks until yield rate
  returns to a stated fraction of pre-shift); and starvation in the post-shift
  window.
- **Prediction, stated now:** risk-only arms degrade at the shift and *stay*
  degraded, because nothing drives them to re-explore. An arm with a live
  epistemic term recovers. `classic`'s advantage on the fixed task should shrink
  or invert after the shift.
- **Falsifier:** if repaired-epistemic and risk-only recover identically, the
  epistemic term buys honesty and nothing else — which is a real finding and the
  one the guard above already permits.

**The discipline that keeps this honest:** "better at learning" is unfalsifiable
if it floats. The shift tick, the recovery threshold and the post-shift window
are fixed in the registration **before R-a is measured**, and the sensor
declaration must show the trajectory data exists to compute time-to-recovery —
which the pilot's `:selection-divergence` endpoint already flagged as
`:coverage-check {:status :unmet :reason :sensor-absent}`. That gap is now
load-bearing rather than decorative, and closing it is a precondition of R-e.

## Sequencing

R-a first, alone. It is the root: with an action-dependent predicted variance the
canonical ambiguity term becomes a real quantity, and R-b's proper EIG needs the
same predicted distribution to take an expectation over. R-c is independent and
cheap. R-d is the largest and touches Joe's original concern most directly, so it
goes last, when the three cheaper repairs have established whether an honest
epistemic term does anything at all.

Each repair is registered before it is measured, through the pipeline that
proved itself today. The Slice 5 confirmation (`964462c7…`, 540 cells) is the
baseline every repair is measured against.

## Exit condition

R-a through R-e landed or explicitly abandoned with reasons; each repaired axis
either renders as a navigable treatment or is withdrawn from the catalogue with
its non-navigability recorded as a finding; and the faithfulness and capability
claims reported separately, whichever way each lands.
