# DRAFT — the first A declaration (for Joe's read, 2026-09-20)

STATUS: DRAFT. Nothing here is live. Adoption is Joe's word or edit;
on adoption this becomes the declared observation model for judgement
tokens, recorded with this file as basis. Everything below is
declared-as-such with its measurement query named (the R7 precedent).

## What is being declared

Rates for the JUDGEMENT observation channel — how much the machine
believes an agent's claim that work happened — plus one coupling
parameter. The CHECKABLE channel (path exists at sha) stays rate-zero
exactly: proven immune to any coupling story
(mixtureLikelihood_checkable_marginal); that boundary is a theorem,
not a choice.

## Channel rates (counted denominators, ledger/Landscape populations)

| channel | rate | basis |
|---|---|---|
| codex refusal-at-intake | 0.14 | 9/64, installed convention (MEASUREMENT d594140c) |
| claude corrected-handoffs | 0.09 | 6/68, ad hoc convention, LOWER BOUND |
| zai within-frame self-correction | 0.25 (frame grain) | 5/20, compile-witnessed (APM scribe) |

Declared per-channel where the claiming agent's kind is known.
DEFAULT for a J token whose channel is unknown or mixed:
**false-positive rate 0.15** (the band the three channels share),
false-negative 0.05 (claims of work NOT done that WAS done are rarer
than the reverse in this record; no counted denominator yet — weakest
number here, marked as such).

## Coupling

One latent condition z ("bad window"), Bernoulli **p = 0.15**,
:z-semantics :per-step-redraw (persistent-z is a queued Lean
transcription). Conditional error rates ~0.29 (bad) / ~0.013 (good) —
a ~22x ratio, estimated from cross-channel co-failure concentration
(12/14 events in windows holding 21% of dispatches; MEASUREMENT
9a0c1495 with its caveats, co-detection confound included).
Justification for carrying z at all: independence understates joint
failure by orders of magnitude (the 469x enumeration gap), and the
record shows errors bunching across channels in shared windows.

## What adoption changes, and what it does not

CHANGES: the J locator on the C replacement declaration (reserved,
unrated since 09-19) becomes rated; any future J token inherits its
channel's rate; A's kernel becomes non-identity wherever a J token is
observed; the two-products evaluation applies exactly at live scale
(7 tokens / 128 states, measured).

DOES NOT CHANGE: checkable observations (theorem); today's certified
verdicts; live D/Q movement, which remains gated on the three recorded
prerequisites (scoring-opts forwarding; this declaration; the D 2c
enactment producer). Adopting rates without those is declaring a
model, not yet running one — stated so nobody reads adoption as a
term-table event by itself.

## Revision

Rates revise when better collections exist (programme point 5's
designed calibration, which also inherits the convention-uniformity
requirement: refusal conventions must be installed uniformly across
dispatchers or denominators are not comparable). Revisions are
follow-up declarations citing new measurement records; this file is
never edited in place after adoption.
