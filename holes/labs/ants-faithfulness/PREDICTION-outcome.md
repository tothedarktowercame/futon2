# Outcome of the preregistered prediction — FALSIFIED

*Written 2026-08-01 after codex-9's authority result (2e13366). The prediction
file `PREDICTION-before-authority.md` stands as written and is not edited.*

## Scorecard

| # | Prediction | Outcome |
|---|---|---|
| P1 | A0−A1 small, CIs at or near zero on patchy and sparse. *Confidence: high.* | **FALSIFIED.** patchy A0−A1 = **133.63 [94.13, 173.13]**; a0 yield 188.65 vs a1 55.02. The controller more than triples yield. snowdrift = 145.32 [134.37, 156.27], η² = 0.925. Only sparse matched (9.71 [−13.28, 32.70], η² = 0.014). |
| P2 | A3 ≈ A1. *Confidence: high.* | **FALSIFIED**, and for a reason not anticipated: A3 is **exactly** A0 in every run, in every scenario. Not "close" — bit-identical. |
| P3 | A0−A1 not exactly zero. *Confidence: medium.* | Held, trivially and in the opposite spirit to how it was meant. |
| P4 | Authority attributable to the risk leg and hand-written biases, not the epistemic leg. *Confidence: medium.* | **Still standing, now by elimination.** The ambiguity term is action-independent; A2 (score-permutation) destroys the effect as completely as A1. So the authority is in the scores, and the ambiguity leg is not in the scores in any way that matters. |
| P5 | Starvation similar across arms, because survival rides the mode FSM. *Confidence: high.* | **Partially falsified.** patchy 0.000 vs 0.067 and snowdrift 0.000 vs 0.000 held; **sparse 0.233 vs 0.533** did not. The controller halves starvation on sparse — a real effect the yield contrast is too noisy to see. |

**Two clear falsifications, one partial, from five predictions — three of them
stated at high confidence.**

## Where the reasoning went wrong

The *mechanical* claim was correct and survives: the Gaussian ambiguity term is
computed from the current belief variance, is identical across candidate actions,
and therefore contributes nothing to selection. Under argmax it is even more
inert than argued — adding a constant to every candidate cannot change an argmax
either.

The *inference* from it was wrong. It went:

> the epistemic leg is inert **and** hard gates narrow the candidate set
> **and** hand-written biases are added → the controller's scoring has low authority

Every conjunct is true. The conclusion does not follow. **A dead epistemic term
does not imply a dead controller.** The risk leg — KL between predicted outcomes
and the mode-conditioned preferences C — is evidently doing substantial work,
and nothing in the static picture measured how much.

That is exactly the gap the two-half design was built to cover, and the author
then reasoned across it anyway, from a static finding to an empirical
conclusion, with high stated confidence. The empirical half existed precisely
because that inference is not available; writing the prediction down is the only
reason it is now a recorded error rather than a belief.

## The finding neither half reported

**The commitment temperature is dead.** Selection is
`(apply max-key :p policies)` — argmax over the softmax probabilities, not a
sample from them. Softmax is monotonic in the logit, so argmax over `p` = argmax
over `−G/τ` = argmax over `−G` for every τ > 0. **τ cannot affect the choice.**

`choose-tau` computes it from hunger, reserve-delta, survival-pressure, nest
proximity, cargo, trail gradient, and several clamps — and the result is
annihilated one line later. This is the third dead quantity in this controller,
after `efe-tilt`/`infer-mode` and the ambiguity term.

It also **corrects the static scan**: `scan-static.md`'s R14 row says "adaptive
`tau` genuinely controls selection sharpness and is carried tick-to-tick". The
first half of that is false. R7's pass is also partly justified by adaptive
temperature; the channel precision half stands, the temperature half does not.

Neither half found this alone. codex-8 read the docstring and the softmax and
called it present; codex-9 measured A3 = 0 and had to explain it. The
explanation required both. That is the two-half method working as designed, and
it is worth recording as evidence for the method rather than only for the ants.

## Consequences

1. **The decision reverses.** Authority is high on patchy and snowdrift.
   `cyberants-replay`'s null is therefore a real result *about the controller*,
   not an artifact of a controller nothing listens to.
2. **Slice 5 is worth running — but not as written.** Its contrast
   (`aif-full − aif-no-epistemic`) still names a canonical term that is
   structurally inert. Re-specify it to ablate the *action-dependent* epistemic
   quantities (`info-gain`, the directed-EIG proxy) and say so, or it will
   produce a null that means nothing.
3. **A risk-leg ablation is now the most informative single arm** and does not
   exist. P4 is supported only by elimination; an A4 that zeroes risk would
   settle it directly.
4. **Three dead quantities is a pattern, not three incidents.** Every one is
   "computed elaborately, then annihilated downstream" — R17′'s invariant, in
   its causal rather than its naming form.
