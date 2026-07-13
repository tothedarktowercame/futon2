# Mission: Turn mission gaps into epistemic affordances (M-aif-gap-epistemic-affordance)

**Date:** 2026-07-13
**Status:** **IDENTIFY — dependent on M-aif-policy-conditioned-eig**
**Owner:** Joe + unassigned implementation owner
**Primary repo:** futon2
**Related missions:** `M-aif-policy-conditioned-eig`, `M-aif-faithfulness`,
`M-populate-substrate-2`, `E-have-want-pairs`

## 1. IDENTIFY

### Motivation — the gap

The live `gap-exploration-bonus` is `6.0 × gap-score`: a caller-supplied lookup
that says *this mission has a discrepancy*. It usefully identifies where
attention may be needed, but it does not say which action will resolve
uncertainty, what evidence that action may produce, or how much the evidence is
expected to change belief.

A gap is therefore an **index into a possible epistemic experiment**, not an
information quantity. The repair must preserve that distinction:

```text
gap g -> latent question θ_g
policy π -> possible evidence o about θ_g
affordance(π,g) = EIG about θ_g under π
```

This mission makes gaps select the relevant latent question and candidate
experiment. `M-aif-policy-conditioned-eig` supplies the price of that
experiment. The gap score itself never masquerades as expected uncertainty
reduction.

### Scope in

1. Give each actionable gap a typed latent question and uncertainty state.
2. Map policies to the gap questions they can observe or discriminate.
3. Declare policy-conditioned evidence outcomes, including no-change/failure.
4. Compute expected information gain through the shared EIG contract.
5. Separate admissibility/relevance (can this policy probe this gap?) from
   epistemic value (how informative is it expected to be?).
6. Shadow EIG-priced affordances against the `6.0 × lookup` controller bonus.
7. Retire the lookup from live ranking only after invariants and evidence pass.

### Scope out

- Treating “large gap” as synonymous with “high uncertainty.”
- Rewarding a policy merely because its target string matches a gap id.
- Reusing pragmatic coverage gain as information gain.
- Folding graph ascent, habit frequency, or preference risk into the EIG term.
- Removing gap visibility from telemetry when its selection proxy is retired.

### Completion criteria

- **C1 — Typed question:** every priced gap declares the proposition, model
  parameter, or competing hypotheses `θ_g` about which information is sought.
- **C2 — Evidence alphabet:** possible results include success, falsification,
  ambiguous/no-change, failure, timeout, and unavailable-agent outcomes where
  applicable.
- **C3 — Relevance support:** a reason-bearing relation restricts each gap to
  policies capable of producing evidence about it; unrelated policies receive
  no epistemic credit.
- **C4 — No lookup-as-EIG:** the scalar gap score may set attention/support or a
  prior over questions, but cannot be added as expected information gain.
- **C5 — Shared kernel:** every affordance price is produced by the coherent
  EIG implementation from `M-aif-policy-conditioned-eig`.
- **C6 — Shared posterior update:** simulated and realised gap evidence update
  the same typed question state.
- **C7 — Discrimination witness:** two policies relevant to the same gap have
  different EIG because their predicted evidence differs, not because of an
  arbitrary per-action weight.
- **C8 — Degeneracy honesty:** if all policies predict the same observation or
  leave the posterior unchanged, EIG is zero and the trace records why.
- **C9 — Prospective evaluation:** predicted evidence probabilities and
  expected entropy reductions are compared with realised outcomes on held-out
  full-loop records.
- **C10 — Scale/shadow gate:** report winner/abstain flips, EIG magnitude,
  saturation, and interaction with risk/ambiguity before a live replacement.
- **C11 — Trace provenance:** record gap id, question/model id, policy,
  predicted evidence distribution, prior/posterior entropy and EIG.
- **C12 — Honest retirement:** after a successful flip, the old gap lookup is
  removed from `controller-score` or retained as telemetry; it is not silently
  stacked with EIG.

## 2. MAP

### Existing components

| Component | Location | Status |
|---|---|---|
| Mission gap lookup | `gap-exploration-bonus` path in `src/futon2/aif/efe.clj` | Live engineering control |
| Mission have→want descriptions | mission registry / gap-view wiring | Supplies relevance, not probability |
| Reason-bearing candidate support | R6 policy-support exclusion trail | Precedent for separating support from value |
| Full-loop preregistration | `holes/labs/M-aif-full-loop-40/` | Candidate prospective outcome corpus |
| Coherent EIG kernel | `src/futon2/aif/epistemic_value.clj` | Built; generative input missing |
| Policy forward effects | `src/futon2/aif/forward_model.clj` | Operational predictions, not typed gap evidence yet |

### First viable question family

Start with one gap class whose result already has an independently recorded
verdict. Good candidates are adjudicated review/build/test or agent-availability
questions. A vague mission-health gap with no observable resolution event is
not a viable first family.

For each selected class, define:

```text
question-id
hypotheses / parameter prior
relevant policy classes
possible evidence events
P(evidence | policy, hypothesis)
posterior update
```

The 40 full-loop cohort may supply prospective evaluation, but it must not be
retrofitted after outcomes are inspected. The question family, split and metric
must be preregistered first.

## 3. DERIVE

### Stage 1 — relevance without value

Turn the existing gap lookup into a typed relation from gaps to policies with
explicit inclusion/exclusion reasons. This may constrain the epistemic policy
support, but it contributes no EIG and does not alter live ranking.

### Stage 2 — one typed experiment

For one question family, define a finite hypothesis prior and evidence model.
Use the shared EIG kernel to price every relevant policy. Include a policy that
is relevant but uninformative as a zero-EIG control.

### Stage 3 — prospective evidence

Record predicted evidence distributions before action and the realised typed
event afterward. Evaluate log loss, Brier score, entropy reduction and posterior
calibration. Unavailable agents and grounded no-change remain observations;
they are not dropped as inconvenient failures.

### Stage 4 — dark replacement

Emit the new `:gap-expected-information-gain` beside
`:gap-exploration-bonus`, with zero selection weight. Produce a deterministic
shadow that removes the old signed lookup and inserts EIG at preregistered
scale. Cascade placeholders remain outside the selection pool.

### Stage 5 — operator decision

Joe chooses among replacement, continued shadowing, or intentional-engineering
retention. A flip must atomically remove the old lookup from selection; keeping
both would double-price gap attention.

## 4. VERIFY / INSTANTIATE plan

1. Select and preregister the first gap question family.
2. Add machine-readable question/evidence validators.
3. Implement the reason-bearing gap→policy support relation.
4. Connect the policy observation model and shared posterior updater.
5. Run finite-model EIG reduction and coherence tests.
6. Collect prospective records and publish calibration plus shadow artifacts
   under `holes/labs/M-aif-gap-epistemic-affordance/`.
7. On any flip, update the baseline, trace schema, R18 manifest, explainer and
   paper together.

### Current honest status

The gap lookup remains useful engineering attention. It is not yet epistemic
value. Until a typed question and policy-conditioned evidence model exist, Box
1's `gap-exploration-bonus` residual remains real.
