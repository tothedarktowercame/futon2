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

## UN-GATED, 2026-08-02 (Joe): the repairs do not wait on more experiments

Joe: *"you were gating E-aif-ants-epistemics on more experiments. I question
whether that's the right angle."* He is right, and the correction matters enough
to record at the top rather than in a footnote.

**Every repair below is justified by a code-level conformance gap, not by a
measured deficiency:**

- R-a — `forward-predict` computes per-channel variance and `predict-observation`
  discards it. Readable in the source.
- R-b — `food-prob × uncertainty` is not an expectation over induced posteriors.
  Definitional.
- R-c — argmax over `−G/τ` equals argmax over `−G` for every τ>0. Proved in Lean.
- R-d — the advertised `q(m|o)` is reachable only through dead `efe-tilt`. Static.

None of these needed the authority run, Slice 5, R-0, or the density probe to
establish. **The experiments were answering a different question** — *is the
machinery causally inert?* — and it is now answered as well as it usefully can
be: a Lean-proved cancellation confirmed at 9/9 cells with zero variance across
three grid sizes, plus identical yields to two decimal places on the density-
corrected probe.

**So experiments are the POSTCONDITION of a repair, not its precondition.** The
excursion already said this and I did not follow it: the acceptance criterion is
*an axis flipping from non-navigable to navigable*, which the render gate tests
in seconds. A full sweep is not the acceptance test and never was.

### The generalisable version

Today's measurements kept producing confounded answers — a grid too easy, a grid
too lethal, a tick-scaling assumption declared against the wrong failure mode.
The **code facts held up perfectly** across all of it: the one quantity we
*proved* inert was inert in every environment, invariant to the confounds that
wrecked everything else.

> For whether machinery is **faithful**, reading and proving beat measuring.
> For whether it **helps**, measuring is unavoidable.

We spent the day using the expensive instrument on the cheap question. R-a
through R-d are faithfulness questions and can proceed now. R-e, the regime-shift
arm, is the capability question and is the one that genuinely needs an
experiment — which is also why it sits outside this excursion's scope.

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


---

## R-a ATTEMPT 1 (2026-08-02): failed its gate, and found a third kind of inertness

codex-9 implemented the re-specified R-a — `pred-variances` for the food channel
derived from `food_belief`'s per-location `:uncertainty` at the **predicted next
location**, which is genuinely action-conditioned. Focused tests confirm the
variance follows the predicted location.

**The ablation did not move.** 15/15 paired records identical across three grid
sizes. codex-9 reverted the change and authored no registration, because claiming
`:score-varies? true` would have been false. Correct on both counts.

### The finding is better than a success would have been

We now have **three distinct mechanisms of inertness**, not one:

| kind | mechanism | instance |
|---|---|---|
| **structural** | the term is action-*independent*, so it cancels under argmax | canonical ambiguity as shipped; τ under `max-key` |
| **dominated** | the term *varies* by action but never crosses the selection margin | canonical ambiguity **after** R-a |
| **unreachable** | the code path is never taken | `infer-mode` behind dead `efe-tilt` |

The second is new and it is the interesting one, because **it is invisible to
every check we have built.** A dominated term passes a navigability inspection —
its score genuinely varies — and a static audit sees a live quantity flowing into
the scorer. Only an ablation reveals it, and only an ablation reported as an
*identity rate* rather than a confidence interval.

### The sign check, now measured rather than assumed

Confirmed: higher destination uncertainty raises Gaussian entropy, raises `G`,
and selection over `−G/τ` therefore prefers **lower-uncertainty, better-visited
cells**. Canonical ambiguity is an **exploitation** pressure.

This was predicted before the run and is worth keeping stated: ambiguity is not
the exploration term. Exploration is the separate `:epistemic` lambda, which is
R-b's target. Any account of this system claiming that its ambiguity term drives
exploration is wrong about the sign.

### The trap, named before anyone walks into it

`default-efe-lambda` is `{:pragmatic 1.0 :ambiguity 0.5 :info 0.4 :epistemic 0.5}`.
Ambiguity is weighted at half of pragmatic — **not negligible**. So the domination
is in the *spread* of the risk term across candidates, not in the weight.

Which means the obvious next move — raise `λ_ambiguity` until the ablation moves
— **manufactures the result rather than discovering it.** Turning a dial until a
term matters is not evidence that the term matters; it is a decision that it
should. If we do it, it needs its own justification and its own registration, and
it must never be reported as "the epistemic term turned out to be load-bearing."

### The cheap next step, and it is a read

Instrument the scorer: log per-candidate `(risk, ambiguity)` for a handful of
decisions and compare the **spreads**, not the means. That answers "by what
factor is ambiguity dominated?" — 2x is a weighting problem, 2000x is structural
— and it is an inspection of internals, not a behavioural experiment.

Consistent with the un-gating principle: this is a faithfulness question, so read
the numbers rather than run a sweep.


---

## THE MEASUREMENT (2026-08-02) — and it refutes the "third kind of inertness"

codex-9 instrumented the selector (opt-in hook, off by default, `da6fdef`). One
300-tick run, 900 decisions. Numbers below are λ-weighted and **independently
reproduced by re-running the producer**; no λ was changed.

| quantity | p25 | median | p95 | max |
|---|---:|---:|---:|---:|
| risk spread | 15.19 | **27.54** | 28.50 | 104.19 |
| **ambiguity spread** | **0** | **0** | **0** | **0** |
| epistemic spread | 0.45 | 0.45 | 0.45 | 0.45 |
| info spread | 0 | 0 | 0.0012 | 0.067 |
| selection margin | 14.62 | **28.10** | 29.07 | 91.67 |

- **ambiguity / margin: exactly 0.0 at every percentile and at the maximum.**
- epistemic / margin: median 0.0158, **max 0.0332**.

### I was wrong about the third kind

The previous section recorded a new taxonomy entry — *dominated*: a term that
varies by action but never crosses the selection margin. **It was not observed.**
Ambiguity's spread at the live selector is not small; it is **exactly zero**,
across all 900 decisions.

That entry was written from an *inference* — codex-9's reasonable reading of a
null ablation — and I promoted it to a finding without measuring it. The
measurement refutes it. Canonical ambiguity remains **structurally constant** at
the selector even with an action-conditioned variance source upstream, which
means the R-a variance path does not reach the ambiguity value the argmax sees.
Why it does not is now the open question, and it is a *read* of `g-efe` and
`c-vectors-for-efe`, not another run.

Recording this rather than quietly amending the table above: the session's whole
subject is quantities that look live and are not, and I added one to the
taxonomy on the strength of a plausible mechanism nobody had checked.

### The decisive result, which does not depend on that correction

**Risk owns the margin.** Risk spread median 27.54 against margin median 28.10 —
risk essentially *is* the selection margin.

**Directed-EIG can never flip a decision.** Its maximum contribution across 895
positive-margin decisions is **3.3% of the margin**; typically 1.6%. A perfect
expected-information-gain implementation, weighted as shipped, would be roughly
30x too small to change what the ant does. **R-b faces the identical wall and
should not be built on the assumption that a better EIG will matter.**

**And the epistemic term is binary, not graded.** Its spread is 0.45 at p25
through p95 — constant. A graded information measure would produce a
distribution; this produces a flag.

### Conclusion for the excursion

The pre-stated reading applies:

> If both ambiguity and epistemic are structurally dominated, the honest
> conclusion is that the ants' EFE cannot be brought online by repairing terms —
> the selection margins are owned by risk — and that is a finding about the
> implementation rather than a failure of the excursion.

That is where we are. R-a and R-b as conceived are dead: no honest change to
either term's *computation* can matter while risk owns ~98% of every margin.
Making them matter requires changing the **weights**, which is a decision that
they should matter rather than evidence that they do, and needs its own
justification and its own registration.

R-c (τ) and R-d (the mode gate) are untouched by this and remain live: both are
about *reachability* and *gating*, not about competing with risk inside a sum.


---

## THE SYNTHESIS QUEUE (2026-08-02) — capability claims stated before the builds

Joe's redirection: *"I don't really know why we have spent time analysing the
ants when we could have spent the time synthesizing more interesting ants."* The
analysis is closed. What follows is the build order, and **each slice states its
capability claim before it is built**, because that discipline has now cost us
twice.

### S1 — 9-cell sensorium *(dispatched, codex-9)*

**Defect:** `:food-trace` / `:pher-trace` are neighbourhood **means**, so
candidates cannot differ in predicted observation. The discrimination is
destroyed upstream of every EFE term.

**Capability claim:** candidates become distinguishable, so a location-derived
epistemic quantity has something to vary over.
**Acceptance:** ambiguity and epistemic spreads in the decision log become
non-zero and vary. Before: exactly 0, and 0.45 flat.

### S2 — drop food anywhere (caching / bucket relays)

**Half-built already:** the grid holds food in cells; `deposit-food`
(war.clj:630-650) already moves cargo out of an ant. Only the destination is
restricted to home.

**Capability claim — and it is the first one the ants have ever had:** *a colony
that discovers relays beats one that does not.* This is the first proposed task a
greedy gradient-follower **cannot solve optimally**, which is why every
measurement to date showed `classic` ≥ `aif-full`: forage-and-return is *solved*
by gradient descent and there was nothing for a deliberative controller to be
better at.

**Confound to decide before building:** `deposit-food` teleports the ant home
from within distance 4. A cache inside that radius is pointless. Remove the
shortcut or place food well outside it — decide, do not discover.

### S3 — two ant architectures: WM-style and Zaif-style *(Joe, 2026-08-02)*

| | preferences `C` | loop |
|---|---|---|
| **WM-style ant** | internal, fixed — as today | closed |
| **Zaif-style ant** | supplied by a driver per episode, in the sensorium | open |

**Small at the implementation:** `c-vectors-for-efe` already takes a C-vector
looked up by mode. A Zaif-style ant takes its C from the task instead. A
substitution at an existing seam, like `:enemy-prox` and drop-food.

**Capability claim — *being told* vs *finding out*:** under a regime shift, the
WM-style ant must discover the change; the Zaif-style ant can be told. So the
Zaif-style ant adapts faster when the driver knows, and **worse when the driver
is wrong, slow, or silent.**

**Why it matters beyond the ants.** It gives a principled candidate answer to the
featuregrid's `?` on the runner's epistemic term: **runners may substitute
instruction for exploration.** If that holds, the ants' inert epistemic apparatus
is not purely a defect — it is partly the correct adaptation for an agent that
receives its goals, and the wrong one for an agent that does not.

**And it is L1 in miniature.** A Zaif-style ant must eventually model *how much
to trust its driver*. L1 asks whether the adjudicator endorses the recommendation;
this asks whether the agent trusts the instruction. Same calibration, at a scale
that costs seconds.

### S4 — fight

**Half-built already:** `:enemy-prox` is sensed (channel 6 of 14) and **no action
can respond to it.**

**Capability claim:** adversaries create uncertainty that is *irreducible by
exploration* — they move and respond, so what you learned decays. Every current
uncertainty is resolvable by visiting, after which an epistemic drive has no
remaining job. This is the first world feature that makes an epistemic term
**permanently** load-bearing.

### S5 — trail-age

Makes uncertainty *earned* rather than visit-count bookkeeping: an old strong
trail is less informative about current food than a fresh weak one.

### The standing constraint

**The ants are a toy of the runners, and whatever the runner layer gains the ant
gains a toy of** — or the toy stops predicting anything. This week inverted that.
The featuregrid (`FEATUREGRID-aif-systems.md`) is the artifact that should make
the inversion visible next time.

### What `classic` is for now

Once these land, `classic` stops being a contrast and becomes an **instrument
check**. Beating a controller that structurally cannot do the task tells us
nothing; *failing* to beat it tells us the task does not require the machinery.
That is `:mechanism-can-exhibit` — a pre-flight check, not an arm.
