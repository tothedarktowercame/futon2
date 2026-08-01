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
