# P28 — R13 cascade scoring: horizon sum and named normalisation

## Scope and source pin

This documentation-only packet reconciles A28/R13. It records the current
reading-family cascade route as static source evidence and keeps older rollout
separate. No scorer, paper, shared model table, test, warrant, ledger, serving
process, or click was changed.

Source pin: futon2 commit a3603cb4804d82eb0dee792689505df9f7897995. The
requested historical anchor 5b4526f95ed6 is an ancestor of this commit.

| Role | Current pin |
| --- | --- |
| bound and declared normalisation | src/futon2/aif/cascade_observation_scoring.clj: max-horizon line 12; g-normalization-laws lines 47–56; normalize-g-terms lines 66–81 |
| horizon traversal, raw sum, certificate | same file: score-candidate lines 176–260 |
| arrangement route | src/futon2/aif/cascade_shape_g.clj: score-arranged lines 270–380; score-policy lines 518–535 |
| family consumer | src/futon2/aif/wm/family_selection.clj: score-family-field lines 259–289 |
| decision caller | src/futon2/aif/wm/cascade_decision.clj: reading-family-decision lines 1190–1211 |
| separate legacy discount | src/futon2/aif/rollout.clj: rollout-discount lines 481–485; project-policy lines 487–539; score-policies lines 665–689 |

## Finding

The R13 statement that later cascade steps are discounted is not an accurate
description of the current cascade_shape_g route. score-arranged sets
H = max(1, min(10, count(patterns))), provides step schedule 1,...,H, and
requests :per-step-capacity-and-pattern normalisation
(cascade_shape_g.clj lines 308–342).

For a candidate, score-candidate visits every integer tau from 1 through H,
gets a score at each step, and forms raw terms

    R = sum(tau=1..H) r_tau
    A = sum(tau=1..H) a_tau
    I = I_candidate.

There is no gamma^tau multiplier in that loop
(cascade_observation_scoring.clj lines 179–212). Under the route-selected
declaration, its controller objective is

    G = R / (H log |O|) + A / H - I / P,

where |O| is outcome support and P is the candidate's distinct-pattern count.
The declaration and raw terms travel in the certificate (lines 246–257).
:raw-cumulative is supported too, but source limits its comparison to equal
horizons (lines 47–56); score-arranged does not request it.

This is a route-specific **static** fact, not a calibration claim or evidence
that a live run ranked a particular policy. The arrangement model marks itself
:synthetic and :calibrated false (cascade_shape_g.clj lines 311–317), and the
certificate scope is :synthetic-bounded-replay
(cascade_observation_scoring.clj lines 218–223).

## Caller census and route boundary

The relevant current source caller chain is:

1. cascade_shape_g/score-arranged builds the bounded observation model and
   calls cascade_observation_scoring/rank-cascade-actions.
2. cascade_shape_g/score-policy consumes that result and adds stated
   circumstance-fit ambiguity.
3. wm.family-selection/score-family-field calls shape-g/score-policy once for
   each constructed family policy, retaining computed rows and typed failures.
4. wm.cascade-decision/reading-family-decision documents this as the one
   reading-family G selection before family selection.

This is the reading-family cascade path. It must not be collapsed into the
older rollout path: project-policy explicitly accumulates
S(pi) = sum gamma^t g(s_t), starts its discount at 1.0, and multiplies it by
gamma after each step (rollout.clj lines 487–539); score-policies obtains and
passes gamma (lines 665–689). The old default gamma is 0.9 (lines 481–485).
That route's existence does not insert a discount into cascade_shape_g.

## Synthetic two-step ordering check

This is algebra only: **not** a calibration result, retained observation, live
run, or claim about production policy values. Let both candidates have H=2,
|O|=2, zero ambiguity and information gain, and one pattern:

| policy | r_1 | r_2 | current raw R |
| --- | ---: | ---: | ---: |
| A | 0 | 10 | 10 |
| B | 6 | 3 | 9 |

The current cascade route gives G_A = 10/(2 log 2) approximately 7.2135 and
G_B = 9/(2 log 2) approximately 6.4921; lower-G B ranks ahead of A. Applying
the separate rollout equation at gamma=0.5 instead gives S_A = 0+0.5(10)=5
and S_B = 6+0.5(3)=7.5; lower-score A ranks ahead of B. Discounting can
therefore reverse ordering. The cited current equation is the first equation,
so stale prose is not grounds to alter scoring.

## Evidence status

- **Static source evidence:** the line-pinned routes and equations above.
- **Historical documentation evidence:** the R13 annotations in
  p4ng/plop-2026-code-annotations.json identify the same distinction at
  historical pin 5b4526f95ed6; they are not a new runtime receipt.
- **Live evidence:** none was collected for this packet. No horizon count,
  normalisation declaration, ranking, or calibration outcome is asserted to
  have occurred live. A future assessed record can report its retained
  horizon and normalisation declaration; until then that evidence is absent,
  not a numeric default.

## Exact proposed R13 replacement (do not apply here)

Target: p4ng/sec-catalog.tex, the paragraph with label pat:r13 (currently
lines 421–425 in the checked p4ng tree).

Replace the prose body with:

> \textbf{If} you want the agent to plan --- to score whole pattern cascades
> as policies, not single steps, and to prefer an action whose payoff is two
> or three ticks out over a greedy one-step win. \textbf{However} a rollout
> that looks one step ahead demonstrates the machinery but not depth; without
> a declared temporal hierarchy the horizon stays shallow. \textbf{Then}
> score the whole cascade over a declared bounded horizon and retain both the
> raw horizon terms and the normalisation declaration. In the current
> reading-family cascade route, every horizon step contributes to the risk and
> ambiguity sums without a temporal discount; per-step, outcome-capacity, and
> pattern normalisation make the declared controller terms comparable across
> cascade shapes. The older discounted rollout score is a separate route, not
> this cascade G. \textbf{Because} active inference plans over a
> \emph{policy} --- here, the pattern cascade for the selected mission --- not
> over the next action alone. \emph{Invariant to preserve:} the thing being
> scored is the cascade, not its first move.

Do not carry forward the current worknote's claim that production resolves
horizon nil or that the depth arm has never fired live: this packet obtained no
live receipt either way. A replacement worknote should cite an actual assessed
record when one exists.

## Proposed correlation annotation update (do not apply here)

| Correlation | proposed status | proposed annotation |
| --- | --- | --- |
| A28 / R13, whole-cascade horizon | EXISTS-UNVALIDATED | Static pin: score-arranged derives H from pattern count capped by scorer/max-horizon and passes a full step schedule; score-candidate sums steps 1..H. Model/certificate are synthetic-bounded-replay and uncalibrated. No live horizon receipt in this packet. |
| A28 / R13, “later ones discounted” | REFUTED for the reading-family cascade route; EXISTS-UNVALIDATED as a separate legacy rollout capability | Static pin: cascade route requests :per-step-capacity-and-pattern and has no gamma multiplier; rollout/project-policy separately uses sum gamma^t g(s_t). The synthetic two-step example demonstrates possible rank reversal, not calibration or live behaviour. |

The existing JSON correlation annotation can retain its historical 5b4526f95ed6
anchors, but should add the current source pin and route boundary above. This
packet makes no shared-live-kernel inference and does not upgrade any
unvalidated claim.

