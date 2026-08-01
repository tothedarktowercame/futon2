# M-wm-capability-claim — what the War Machine should be able to do

**Opened:** 2026-08-01. **Owner:** Claude (claude-4). **Operator:** Joe.
**Status:** SPEC. Prerequisite for M-aif-stack S2.

## The gap, with evidence

Joe, 2026-08-01: *"pretty much the whole plop-2026.tex paper is instrument
building. But we haven't really ever come out and said what we think the WM
should do or should be capable of doing."*

Checked rather than assumed. Across every section and appendix of the paper,
searching for `should be able`, `capable of`, `the goal is`, `aims to`,
`intended to`, `success would`, `would count as`: **zero matches.**

What does exist:

- **A mechanism description**, in `war-machine-pilot-explainer.md` only — *"it
  recommends what to work on next"*. The paper does not carry even this.
- **A scoring formula**, `M-wm-three-factor-mission-value.md` (2026-07-18):
  `central × strategic × doable`, gated and decayed.

Neither is a capability claim. A description says what it does; a formula says
how it scores. **Neither can fail.** Nothing anywhere is of the form *the War
Machine should be able to X, and if it cannot, that is a failure.*

## Why this is the load-bearing gap

1. **"Complete but unproven" has no content.** Unproven *of what*? There is no
   claim on record that the evidence has failed to establish.
2. **Every instrument we own measures honesty, not quality.** The completeness
   contract measures faithfulness to a framework. The two-half method measures
   whether machinery is live. The audit measures whether claims match artifacts.
   None of them can tell us whether the recommendations are any *good*.
3. **The "so what" question keeps having no answer** because it is being asked
   of a system with no stated target. The answer to *so what* is always relative
   to a claim, and there is no claim.
4. **The John Henry comparison is unspecifiable.** A race needs an axis. The
   operator has been the de facto control arm all along, but "better than the
   operator" is meaningless until we say better *at what*.

## The retrofitting trap, named before we fall into it

Writing capability targets *after* building the thing is how a target gets
fitted to the capability. Today has three separate instances of the same error
class — an experiment whose contrast could not move, a scorer whose ranking was
an artifact of scale, a Lean file that assumed its conclusion — and all three
were caught only because something was written down first.

So this document separates two things and dates both:

- **Recovered intent** — what the artifacts already committed to, with their
  dates. Not invented today.
- **New aspiration** — stated today, marked as such, and *fixed before the
  relevant ledger is read*. Any rung whose standard is set after looking at the
  data is worthless.

## The ladder

### L0 — Answerable mechanism · RECOVERED (2026-07, the paper)

Every recommendation carries inspectable reasons; the loop closes on a witness
the system did not manufacture.

*Failure:* a recommendation whose reasons cannot be reconstructed.
*Status:* **claimed and demonstrated.** This is what the PLoP paper is about,
and it is the whole of what it claims.

### L4 — Honest abstention · RECOVERED (2026-07, `policy-nondiscrimination`)

When expected free energy cannot discriminate between leading policies, the
selector declines to pick rather than picking badly.

*Failure:* always producing a recommendation.
*Status:* **present and working.** Listed here out of order because it is the
one rung the WM already has and the other two systems audited today do not —
AIF²'s scorer always emits a ranking, and the ants' `default-mode` is a fallback
action, not an abstention. Worth noticing that our most-criticised system is the
only one with the safety property.

### L1 — Competent selection · NEW, 2026-08-01

The WM's top recommendation should be one a competent maintainer would endorse.

*Failure:* the operator routinely overrides it.
*Testable:* **now, and it never has been.** Joe adjudicates recommendations
constantly. The data exists as sessions and does not exist as a ledger. The
instrument is already specified — the correction probe concluded that operator
corrections must be *declared* with an inline mark at the moment of writing
(precision 1 by construction, the lexical detector recovering only ~19% at 0.42
precision). Specified; unbuilt.
*Standard, fixed now:* endorse-rate over a window, with the operator's verdict
recorded at recommendation time and not reconstructed afterwards. **The
threshold must be set before the first window is read.**

### L2 — Improving selection · NEW, 2026-08-01

Endorse-rate should rise across cohorts as run history accumulates.

*Failure:* flat or declining.
*Testable:* only after L1 is recorded. This is the payoff the Memory Model work
is aiming at — run histories teaching better technique — and it is the precise
content of Joe's claim that the WM should get better at *direction* rather than
at chasing through code.
*Note:* this is the first rung where the machine could beat the John Henry arm
on the axis that matters, since a human's selection quality does not improve
from a run ledger.

### L3 — Selection beyond the operator · NEW, 2026-08-01

The WM should sometimes recommend work the operator would not have chosen, and
be vindicated.

*Failure:* every disagreement resolves in the operator's favour.
*Testable:* not yet. Needs L1's ledger **plus** an outcome measure for *was it
right*, adjudicated by something other than the operator who disagreed. That
second requirement is genuinely hard and is the real barrier.
*Why it matters:* this is the **only** rung at which the system exceeds its
control arm rather than matching it. Everything below is a case for the WM being
a good instrument; L3 is the case for it being a good *agent*.

## What this changes immediately

**M-aif-stack S2 (measure the WM's causal authority) is not meaningful without
L1.** Authority asks whether the scoring drives the recommendation; L1 asks
whether the recommendation is any good. A system can have perfect causal
authority over uniformly bad choices. S2 should be re-scoped to run *after* or
*alongside* an L1 ledger, not before.

**The cheapest real work in the whole programme is L1**, and it has been
available the entire time. It needs the operator's verdict recorded at
recommendation time. No new inference, no new experiment, no comparison arm —
just the mark the correction probe already specified.

## Slices

- **S1:** build the L1 ledger — declared operator verdict at recommendation
  time. Fix the endorse-rate standard *before* the first window closes.
- **S2:** one window of L1 data, reported whichever way it lands.
- **S3:** L2 only after two windows exist.
- **S4:** the L3 adjudication design — the hard one, and the one worth a paper.
