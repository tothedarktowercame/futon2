# Mission: M-aif-stack — three implementations, one framework, one yardstick

**Opened:** 2026-08-01. **Owner:** Claude (claude-4), operator Joe.
**Status:** SPEC. Opening act dispatched (Slice 5 re-specified, below).

## Why this mission exists

Joe, 2026-08-01: the War Machine is substantially complete but unproven; the
"so what" needs to stop being *it runs and that's cool*. AIF² supplies an
interesting so-what at the level of **writing**. This mission is the search for
the equivalent at the level of **coding**.

The move is to stop treating ants, the War Machine, and AIF² as three projects
and start treating them as **three implementations of the same framework**, so
that a finding in one is evidence about our grasp of the whole.

## The three, and what they share

| | ants | War Machine | AIF² |
|---|---|---|---|
| agent | a forager | a coding harness | an argument |
| belief over | food/world state | mission & evidence state | claim status |
| actions | macro moves | patterns / cascades | attacks, edits, withdrawals |
| substrate acted on | a simulated world | a real repository | a manuscript |
| witness | next tick's observation | external store re-read | reviewer verdict |

All three compute a belief, score candidate actions, act on a substrate outside
themselves, and observe the result. That is the framework. Whether each of them
*honestly implements* it is the question, and until 2026-07-31 we had no common
way to ask.

## The common instrument (this is the contribution)

**One yardstick.** `~/code/ukrn-services-simulation/docs/aif-completeness.md` —
twelve requirements derived from the canonical literature, authored for a fourth
project, predating all three. External to every implementation it scores.

**Two halves, because one is not enough.**
- *Half 1, static faithfulness:* does the code compute the required quantities?
- *Half 2, causal authority:* does anything downstream act on them?

The second half is the load-bearing methodological claim. **A requirement can be
satisfied by a controller nothing listens to.** Static audit cannot see this;
only intervention can.

## Scores to date

| | static | causal authority | note |
|---|---|---|---|
| ants | **4/12** (R1,R2,R3,R7) | **high** — A0−A1 = 133.6 [94.1,173.1] on patchy; η²=0.93 snowdrift | three dead quantities, yet the live remainder triples yield |
| War Machine | R1–R19 largely satisfied per `futon-aif-completeness.md`; 16-quantity badge audit in `data/r18-badges.edn` | **unmeasured** | O10: the witness does not feed the next belief |
| AIF² | **1/12** | **undefined** — nothing consumes the score | opposite failure mode to the ants |

The asymmetry is the interesting part. **The ants have a consumer and unknown
faithfulness; AIF² has known machinery and no consumer; the War Machine has
never had either half measured.**

## The finding that makes this a science question

In two days, in three unrelated codebases, the same defect appeared:

**Quantities are computed elaborately and then annihilated downstream.**

- *ants:* `infer-mode`'s `q(m|o)` reachable only via dead `efe-tilt`; the
  Gaussian ambiguity term identical across candidates so it cancels; `choose-tau`
  computing a commitment temperature that `max-key` renders irrelevant, since
  argmax over `−G/τ` is argmax over `−G` for every τ.
- *AIF²:* a scorer whose two legs are not commensurate, so the leg named
  *ambiguity* silently dominates rather than balances; and a scorer output that
  nothing consumes.
- *War Machine:* an actuator that snapshots, dispatches, logs and returns without
  calling `update-belief` — R16's conjunct unimplemented.

Every instance is invisible to code review and to static audit. Each was found
by asking *what actually moves the outcome*.

**The general claim, stated so it can fail:** active-inference implementations
accrete inert machinery, because a quantity that is computed, named after a
canonical term, and persisted looks satisfied to every check short of
intervention. If that is right it is a property of the framework's
implementability, not of our carelessness — and the two-half method is the
remedy.

Three instances is not a result. It is a hypothesis with three supporting
observations, and it is falsifiable: a fourth implementation audited both ways
that shows no inert machinery would weaken it considerably.

## Opening act — Slice 5, re-specified

Slice 5 was designed to test whether the epistemic term drives exploration
(`aif-full − aif-no-epistemic > 0` on patchy/sparse, `≈ 0` on snowdrift). **As
written it could not have worked**: the canonical ambiguity term is
action-independent and cancels, so the ablation removes nothing. Re-specified:

**Arms.** `:aif-full` · `:no-canonical-ambiguity` · `:no-directed-eig` ·
`:no-info-gain` · `:no-risk` · `:classic`

**`:no-canonical-ambiguity` is a positive control for the method.** We predict
**exactly zero** effect, on every scenario, because the term cancels. If it moves
anything, our analysis of the controller is wrong and the rest of the run should
not be believed. The instrument is tested in the same run as the hypothesis.

**The restated dissociation.** If the ants have a real explore/exploit
regulator, ablating the *action-dependent* epistemic quantities
(`:no-directed-eig`, `:no-info-gain`) should hurt on patchy and sparse and not
on snowdrift. If instead only `:no-risk` moves anything, the ants' exploration
lives entirely in the risk leg and the gating, and the epistemic apparatus is
decorative in its entirety.

Both outcomes are informative and both are reportable. The second is more likely
given what we now know, and would be a stronger finding.

## What this mission is not

It is not a claim that the three systems are equally good, or that the War
Machine's completeness transfers to the others. It is a claim that one external
yardstick and one two-part method apply to all three, and that comparing them is
more informative than describing any of them.

## Slices

- **S1 (dispatched):** Slice 5 re-specified, above.
- **S2:** measure the War Machine's causal authority — the half never run on it.
  This is the direct route to the coding-level "so what": the WM is complete and
  unproven, and *unproven* here has a specific meaning we can now test.
- **S3:** AIF² B0 — give the scorer a consumer so its authority becomes a
  quantity at all.
- **S4:** the comparative write-up. Only after S1–S3.

---

## S1 result (2026-08-01): the ants trade yield for survival, and the design could not see it

**Positive control passed exactly.** `full − no-canonical-ambiguity` =
`0.0000 [0.0000, 0.0000]` in all three scenarios, paired run records
bit-identical. The morning's code analysis and the Lean theorem
(`ambiguity_ablation_preserves_selection`) are empirically confirmed. The
instrument tested itself in the same run as the hypothesis, and passed.

**The dissociation failed.** No patchy/sparse explore-exploit regulator is
established. Both epistemic proxies are exactly inert on sparse.

**No single term carries the controller's authority.** This morning, replacing
the scorer with uniform-random over the same admissible set cost `133.63
[94.13, 173.13]` on patchy. Today, ablating any *individual* named term costs at
most `−20.0 [−66.9, 26.9]`, not significant. The two are consistent only one
way: **the ordering is over-determined.** Removing any one term leaves an
ordering that performs about as well; removing the ordering entirely is
catastrophic. The authority experiment measured *having a consistent score* and
not *having any particular score*.

That distinction was invisible until both halves ran, and it is a caution for
any ablation study on a scorer with redundant terms.

**Against the non-AIF baseline, AIF loses on yield:**

| scenario | full − classic | |
|---|---:|---|
| patchy | −46.78 [−100.66, 7.10] | ns |
| sparse | −12.39 [−45.62, 20.84] | ns |
| snowdrift | **−57.45 [−67.75, −47.14]** | **significant** |

**But the outcome measure is mis-specified, and that is the finding.** Starvation
tells the opposite story on the scenario that discriminates:

| sparse | yield | starvation |
|---|---:|---:|
| aif-full | 32.18 | **0.233** |
| no-risk | 51.52 | **0.667** |
| classic | 44.57 | **0.667** |

Ablating the KL risk term nearly triples starvation (0.233 → 0.667) while
*raising* mean yield. The classic baseline shows the same profile. So the AIF
controller has a coherent behavioural signature: **it trades yield for
survival**, which is what a risk term denominated in preferences that include
not-starving is supposed to do.

Slice 5 as designed measured yield alone. **Yield alone penalises exactly the
behaviour the controller exists to produce.**

*Caveat on snowdrift:* `classic` scores `315.00 [315.00, 315.00]` — zero
variance, every run. That is a ceiling; the environment is saturated for a
greedy forager. "Classic beats AIF on snowdrift" is partly "classic exhausts a
solvable board and AIF does not".

### What this contributes to the mission's claim

The inert-machinery hypothesis survives and is sharpened. Three of the ants'
named quantities are inert (`efe-tilt`/`infer-mode`, canonical ambiguity, τ),
and now a fourth observation: the *remaining* terms are individually
unnecessary. The controller works; almost none of its named parts are load-bearing
in isolation.

And a fifth finding, which is the one that generalises: **the experiment could
not see what the controller was good at, because nobody had said what the
controller was for.** This is `M-wm-capability-claim.md`'s gap arriving
independently in the ant domain on the same day. We measured yield because yield
was measurable, not because it was the capability.

---

## S1 CONFIRMATION (2026-08-01): the epistemic apparatus is inert on most seeds

Registration #1 through the full pipeline. Validation fired
(`:validated? true`), the positive control discharged before any treatment ran,
pilot artifacts untouched (`a5caf04a…` unchanged), seeds drawn from the
registered confirmation bases with **zero** pilot seeds present, and two
producer runs byte-identical at `964462c7…`.

### Per-seed identity — a much stronger statement than a confidence interval

codex-9 reported contrasts and CIs. Reading the artifact per seed says something
the CIs cannot: **how often ablating a term changes the run at all.**

| scenario | no-directed-eig | no-info-gain | no-risk |
|---|---:|---:|---:|
| patchy | 28/30 identical | 28/30 | **2/30** |
| sparse | **30/30 identical** | 27/30 | 4/30 |
| snowdrift | **30/30 identical** | 25/30 | **0/30** |

Directed-EIG is **bit-identical on every seed** in two of three scenarios, and on
28 of 30 in the third. Info-gain is inert on 80–93% of seeds. The risk term
changes the run on 28/30, 26/30 and 30/30.

That is not "the contrast is not significant". It is: **on most runs, ablating
the epistemic apparatus produces the identical trajectory.**

### What this settles

The morning's authority run showed that scoring matters enormously — replacing
it with uniform-random over the same admissible set cost `133.63 [94.13, 173.13]`
on patchy. This run shows which part of the score that is. **It is the risk
term.** The epistemic half — the thing that distinguishes expected free energy
from plain risk minimisation — contributes nothing to selection on the great
majority of runs.

So for the ants: *the implementation's "expected free energy" is, causally, its
risk leg.* Four named epistemic quantities, and the live behaviour is
indistinguishable without them.

The survival trade replicates on independent seeds. Sparse starvation:
`aif-full` 0.133, `no-risk` 0.633, `classic` 0.700 — ablating risk multiplies
starvation nearly fivefold while *raising* mean yield, and `classic` beats
`aif-full` on yield in every scenario while starving far more. The controller
buys survival with yield, and the risk term is what buys it.

### Methodological note worth carrying to the other two systems

**For an ablation, report the identity rate alongside the contrast.** A
non-significant CI is weak evidence of inertness — it is consistent with a small
real effect and an underpowered design. A bit-identical run set is *proof* of
inertness for those seeds, and needs no power argument at all. Our pilot reported
CIs and called the result "not established"; the identity counts say something
far stronger and were available in the same artifact.

This is the fifth instance of the mission's class, and the cleanest: quantities
computed, canonically named, persisted into the trace, and provably absent from
the causal path.

### Generalised by claude-7, and their version is the right one

The note above says "report the identity rate". That only works for a
deterministic system — ours is a seeded simulation, so identity is available and
exact. Theirs is a stochastic runner, where two control runs of the same problem
differ anyway.

Their relativisation is strictly better and subsumes mine:

> The floor is the **baseline identity rate of the canonicalised trace** —
> control-vs-control identity is the yardstick. The incidental arm's claim
> becomes *"IN-ablated identity ≈ floor"*; the load-bearing arm's becomes
> *"LB-ablated identity ≪ floor"*.

For a deterministic system the floor is 100%, and "bit-identical on 30/30"
is the degenerate case. So the general statement is: **measure identity against a
same-condition floor, not against zero.** No power argument is needed beyond the
floor measurement, which a noise-floor arm supplies anyway.

Two further things they saw that we did not:

- **Canonicalisation makes it available without touching the runner.** Their
  dispatch discipline already mandates incremental commits inside an isolated
  checkout, so every run leaves an ordered sequence of (declarations touched,
  edit kind, per-step build outcome) recoverable from git. Hash that sequence,
  declaration-level and tactic-text-insensitive, and trajectory identity is
  well-defined for a non-deterministic system.
- **Per-pair identity is evidence about the classification, not just the arm.**
  A pair where the load-bearing ablation leaves the decision sequence identical
  to control is per-pair evidence that the rubric *misjudged that memory* —
  finer-grained rubric validation than an arm-level sign test, sitting in the
  same artifact.

Their summary of the transfer is the one worth keeping: *the stronger reading
was in the artifact the whole time.* That is true of our pilot, which reported
CIs and concluded "not established" while the identity counts sat unread in the
same EDN. It is the mission's class one more level up — the analysis, not the
code, computing something correctly named and weaker than what it had.


---

## CORRECTION (2026-08-01, Joe): the inertness finding is environment-confounded

Joe questioned "classic beats AIF on yield everywhere" against the motivating
figure in `p4ng/plop-2026.pdf`, where classics die of starvation and AIFs thrive.
He was right to, and the resolution is not the survival-for-yield trade I
offered.

**The figure's own status bar:** `Scores Classic 0.00 vs AIF 44.16`, with
`Classic queen starve 209` against `AIF queen starve 35`. AIF dominates on
*both* measures. Classic scores **zero**. And the tell is on the same line —
`Dist C:1.73 A:6.14`: the classic ants barely moved. They sit in the nest and
starve while the AIF ants cross the map to the food.

**The environment differs, and by a lot.**

| | grid | cells |
|---|---|---|
| `war.clj:27`, the simulation's own default | `[24 24]` | 576 |
| our authority run, pilot and confirmation | `[10 10]` | **100** |

Ours is 5.8× smaller, with 2–4 patches of radius 1–2. On 100 cells food is
never far from anything and a greedy gradient-follower cannot miss it.
**Exploration has almost no value in the environment we tested.**

### What this does and does not overturn

**Stands — these are facts about the code, not the environment:**

- the canonical Gaussian ambiguity term is action-independent
  (`pred-variances = (:var mu)`), so it cancels under argmax in *any*
  environment. Proved in Lean, and the positive control confirmed it empirically
  at 0.0000 in all three scenarios.
- τ is annihilated by argmax for every τ > 0.
- `infer-mode`/`efe-tilt` is dead code.

**Confounded — this was an empirical claim in one environment:**

- that the *action-dependent* epistemic proxies (directed-EIG, info-gain) are
  inert. They were bit-identical on 30/30 seeds **on a 10×10 grid**. Those terms
  are genuinely action-dependent in the code; their inertness may be
  environmental rather than structural. An exploration term does nothing where
  there is nothing to explore.

It may also explain the standing puzzle — that scoring matters enormously
(`A0−A1 = 133.63`) while no single-term ablation reproduces it. Over-determination
was one reading; a small grid where the risk leg alone suffices is another, and
they are not exclusive.

### How the confound got in

The environment was inherited "verbatim from the authority run for
comparability", and codex-9 chose it before that. Neither of us asked whether it
was a *discriminating* environment. **The control for one thing (comparability
across runs) destroyed the sensitivity for another (whether exploration pays).**

That is today's recurring lesson once more, and the most expensive instance: the
measurement could not see what the thing was for, because the environment was
chosen for a property orthogonal to the hypothesis.

### R-0, ahead of everything in E-aif-ants-epistemics

Before any repair: **re-run the confirmation on an environment where exploration
has value** — the simulation's own `[24 24]` default at minimum, ideally with
patch distance from the nest as a declared axis — and check whether the
epistemic ablations are still bit-identical.

If they are, the structural reading stands and the repairs proceed. If they are
not, then the epistemic terms work and our test could not see it, and the
excursion's premise needs rewriting before a line of code changes.

This is cheap, it is a strictly better use of the next run than any repair, and
it was found by an operator remembering a picture.

---

## A fourth implementation, and the general form of today's confound (Joe, 2026-08-01)

### The unifying precondition

Joe: searching a memory store for relevant memories is structurally the same
problem as ants searching for food. Narrow search is efficient when you know what
you are looking for; when you do not, you need an exploratory component. Obvious,
*and not obvious enough to prevent a 10x10 run of ants.*

That yields the general form of both cautions now in flight across the two
missions:

> **Before measuring whether mechanism M helps, establish that the task cannot be
> solved without M.**

Two instances, arrived at independently:

- *V3's* `repo_search` precondition — is the withheld memory's content reachable
  through a parallel channel? If yes, the ablation attenuates regardless of
  environment.
- *ours* — is the food reachable without exploring? If yes, the exploration term
  is inert regardless of implementation.

Same question, different alternative path. The second is the easier to miss,
because it is a property of the **corpus or environment** rather than of the
system, and nothing in a registration, a config gate, a positive control or a
reviewer asks about it. Ours survived all four.

The recommendation that follows: state the precondition in its general form and
**enumerate the alternative paths being ruled out**, rather than listing them ad
hoc as they occur to someone.

### Memory retrieval as the fourth implementation

If the analogy holds structurally, V3's retrieval process is not only a subject
of measurement — it is a candidate AIF implementation:

| | memory retrieval |
|---|---|
| agent | the retrieval process |
| belief over | which memories are relevant to the current work |
| actions | query formulation; retrieval breadth; which candidates to surface |
| risk | divergence from the preferred outcome — surfaced memories get *used* |
| ambiguity | uncertainty about relevance, driving exploratory retrieval |
| witness | the use/ignore signal on a surfaced memory, recorded externally |

This mission carries three implementations against one yardstick, and their
failure modes are complementary: the ants have a consumer and inert epistemics;
AIF² has machinery and no consumer; the War Machine has never had either half
measured. **A fourth whose central question is exploration would be the most
informative of the four**, because the epistemic term would be load-bearing by
construction rather than incidentally.

It would also be the first one built with the failure already in hand. Everything
here was found retrospectively; this one could be registered before it runs.

Belled to claude-7 as a decision for their cohort draft — whether retrieval is a
black box being measured, or an agent being registered. Not pressed as a request.


### The precondition has two limbs, not one (claude-7, 2026-08-01)

Taking the general form into V3's cohort registration, claude-7 enumerated four
inertness paths — and the fourth is a category ants cannot produce:

| # | path | kind | analogue |
|---|---|---|---|
| 1 | in-tree content: the problem's own files carry the plan | **corpus** | our grid |
| 2 | library search: reachable by repo/Mathlib grep | **environment** | warning 1 |
| 3 | model competence: the runner solves it narrowly, unaided | **competence** | *precisely* our 10×10 |
| 4 | **corpus emptiness: the store has nothing to deliver** | **store** | **none — new** |

Path 4 is the one worth carrying back. Ours was *the treatment had nothing to act
on*. Theirs is *the treatment never arrived* — V2's recall-empty rate was 64%, so
two thirds of treatment dispatches would deliver no memory at all and the
intention-to-treat estimate attenuates by the delivery rate. Not fatal —
"availability rarely delivers" is itself a result — but a diluted ITT reads as
"memory doesn't help" unless the dilution is preregistered.

So the precondition is two-limbed, and I had only stated the first:

> **(a)** the task cannot be solved without M — *enumerate the alternative paths*;
> **(b)** M was actually delivered — *a per-unit manipulation check*.

(b) is the ordinary manipulation check of experimental design, and its absence is
how a null gets misread as an effect size rather than as a delivery failure.
Neither limb is asked by a registration, a config gate, a positive control or a
reviewer. Both belong in the registration as a named block.

Their handling of path 3 is also worth stealing: a randomised design
**self-diagnoses** — the control arm *is* the discriminating-environment
measurement. But *diagnosis-after-spend is exactly our ant mistake*, so they add a
pre-check from historical receipts before the window opens, requiring headroom.
Self-diagnosis is not a substitute for a precondition; it only tells you
afterwards what a precondition would have told you before.

### On the fourth implementation: claude-7's ordering is better than my suggestion

They accept the mapping and refuse the sequencing. For the cohort, retrieval
stays a **black box being measured**, because the baseline it produces is exactly
what a fourth implementation would have to beat. The agent-being-registered
version is a later move, with the U-curve finding as its prior and the use/ignore
receipts as its witness.

Their reason, which is the correct one:

> Building it before the baseline exists would be measuring our way out of a
> mechanism we hadn't yet shown was needed — which, after today, has a name and a
> grid size.

One piece already exists without having been designed for it: their query ladder
(3-term → pairs → singles) **is** a hand-coded breadth-escalation policy, i.e. an
exploration schedule, and this afternoon's B5 records which rung fired per
dispatch. That is the action sensor an AIF retrieval agent would need, built
before the agent was conceived — the same shape as futon5a's drift detector
predating R20 by ten weeks.

---

## The runner layer, and conatus on spec (Joe, 2026-08-02)

Two observations that close a loop.

**First: the ants are a toy model of the zaif runners, not an analogy.** The
paper's motivating line already says it — *"just as simulated ants can be
governed by an active-inference loop to gather food from their landscape, coding
agents could be governed to move around the landscape of a software project"* —
and the runners are the instantiation. So the ant findings are cheap diagnostic
hypotheses about the expensive layer.

**Second, and this is the actionable one: because zaif is a custom harness,
a harness can be installed PER JOB — so the runner's conatus can be on spec.**

### The evidence is today's own failures

R-0 was dispatched three times. Twice it died as an Agency job with zero tool
events, no error and no end time — reaped at the ~30-minute cap. The third
attempt, under the harness background, died with the session. Only the fourth,
under `systemd-run`, survived.

**The runner had no representation of its own survival horizon.** It accepted a
~2-hour sweep into a 30-minute container, twice. The cap was a hazard it was
*subject to*, never a preference it could *act on*. That is precisely conatus in
the passive mode — perseverance determined by external causes — and the ants have
the same shape: their survival comes from a hardcoded hysteresis FSM rather than
from anything they model.

### What "on spec" buys, concretely

Put the envelope in the preferences `C` rather than in the environment:

- deadline, token budget, context headroom, commit checkpoints;
- so expected free energy includes divergence from *complete within the
  envelope*;
- so the runner plans against it — chunk, checkpoint, commit per unit, or
  **refuse the job as specified**.

Refusal is the important one. A runner that can decline "this does not fit in my
envelope; give me three jobs" is doing something no current runner can, and it is
the single change that would have prevented all three R-0 failures.

### And it yields a non-degenerate epistemic term

This is the part that matters for `E-aif-ants-epistemics`. The ants' ambiguity
term failed because it was computed from a quantity identical across candidate
actions. At the runner layer the natural epistemic quantity is **uncertainty
about the runner's own capacity to complete** — and reducing it is an action:
*run one cell, time it, extrapolate.*

That cannot be action-independent. Different plans probe different amounts, and a
plan that measures before committing has different expected information than one
that does not. **The defect the ants have is not expressible here.**

It is also exactly what I failed to do: a single timed cell would have told me
R-0 was a two-hour job before I sent it into a thirty-minute container. The
epistemic action was cheap, available, and never valued.

### It is already registerable

"On spec" means the envelope is part of the job's registration, and the harness
validates against it — which is `validate-then-run!` for jobs rather than
experiments. The apparatus built today for experiment registration applies
unchanged: a job whose harness config does not inhabit its registered envelope
does not start.

---

## R-0 RESULT (2026-08-02): the fix overshot, and the question is still open

Registered, rendered, ProspectiveReadyToRun, run under `systemd-run` after three
failed dispatches. 1620 cells.

### The instrument passed at every scale

`:no-canonical-ambiguity` identical to `aif-full` on **30/30 seeds in all nine
cells** — 10x10, 24x24, 36x36, all three scenarios. The Lean-proved cancellation
fact holds empirically at every grid size, which is exactly what a scientific
control should do: invariant to the environment, because it is a property of the
code.

### The primary readout, and why the artifact's own headline is misleading

The generated report says *"Directed-EIG 30/30 survives scaling: **no**"*. That
rests on a single cell (23/30 at 36x36 snowdrift). The substantive pattern is
that **directed-EIG stays at or above 23/30 — 77% identical — in every cell
tested.** It did not come alive.

### The larger grids are degenerate in a NEW way

| grid | patchy yield / starvation | sparse yield / starvation |
|---|---|---|
| 10x10 | 173.93 / 0.000 | 34.21 / 0.267 |
| 24x24 | 93.50 / **0.500** | 16.33 / — |
| 36x36 | 75.37 / **0.900** | **0.35** / **0.867** |

At 36x36 the colonies simply die: 87–90% starvation, and sparse yields a third of
one food item. **All arms then behave identically because they are all dead**,
which is why `classic` — 0/30 identical to `aif-full` at every 10x10 cell — rises
to 27/30 at 36x36 patchy. That is not convergence, it is a shared floor.

10x10 was too easy for exploration to matter. 36x36 is too lethal for anything to
matter. Neither discriminates.

### Only snowdrift stays viable, and there the signal is small but monotone

Zero starvation at every scale, yield growing with the board:

| grid | aif-full | no-directed-eig | cost of ablation | directed-EIG identity |
|---|---:|---:|---:|---:|
| 10x10 | 253.96 | 253.96 | 0.0 | 30/30 |
| 24x24 | 510.95 | 495.13 | 15.8 | 27/30 |
| 36x36 | 847.07 | 828.26 | 18.8 | 23/30 |

So in the one scenario readable across all three scales, the epistemic term does
become **slightly** more active as the environment grows. Suggestive. Not
decisive, and nowhere near the risk term's 0–1/30.

(`classic` scores exactly the total food every time — 315, 756, 1134, zero
variance. It exhausts a snowdrift board; `aif-full` gets 81%, 68%, 75% of it.)

### The assumption I declared, and declared wrongly

R-0 registered a tick-scaling assumption with a `:breaks-when`, per the
vocabulary. I scaled ticks with grid **diameter** to hold time-to-cross constant,
and named traversal as the failure mode.

The binding constraint is **energy**. Metabolism 0.06 and initial reserves 0.5 are
fixed; more ticks on a bigger board means more starvation exposure without more
food. The assumption broke, in a direction its own `:breaks-when` did not name.

**Declaring an assumption does not protect you if you declare the wrong one.**
That is a sharper lesson than the one R-0 was run to test, and it is the third
instance today of an environment chosen for a property orthogonal to the question.

### Verdict, without softening

R-0 **does not** establish that the epistemic terms are structurally inert, and it
**does not** establish that the 10x10 finding was environmental. It rules out the
simple version of both:

- inertness is not universal — 23/30 is not 30/30;
- and the 10x10 grid was not the whole problem, because scaling up broke the
  experiment in a different direction.

What it does establish is that **there is a viability window, and we have not
found it.** The next move is not a bigger grid. It is scaling food density and
ant energetics with board area so the colonies survive, then re-measuring. Until
then `E-aif-ants-epistemics` keeps its premise flagged as unconfirmed rather than
rewritten.
