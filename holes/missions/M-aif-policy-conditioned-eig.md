# Mission: Policy-conditioned expected information gain (M-aif-policy-conditioned-eig)

**Date:** 2026-07-13
**Status:** **IDENTIFY agreed in principle; pure kernel instantiated; generative contract open**
**Owner:** Joe + unassigned implementation owner
**Primary repo:** futon2
**Related missions:** `M-aif-faithfulness`,
`M-aif-a-matrix-faithfulness`, `M-evaluate-policies`

## 1. IDENTIFY

### Motivation — the gap

The live `model-uncertainty-bonus` is posterior spread. It can identify a model
region whose parameters are uncertain, but it does not answer the policy
question: *if this policy is selected, which observations might occur, how
would each observation update the model, and how much uncertainty would that
update remove in expectation?*

The canonical target is policy-conditioned expected information gain:

```text
EIG(π) = Σ_o Q(o | π) KL[Q(θ | o, π) || Q(θ | π)]
```

where `θ` is the uncertain model object. For the current structure-learning
lane, `θ` should initially be the A4a Dirichlet reliability/constellation
parameters, not the already observed controller score and not an undifferenced
scalar standard deviation.

`futon2.aif.epistemic-value/expected-information-gain` now implements this
calculation for a finite declared hypothesis space and fails closed unless the
predicted posterior mixture reconstructs the prior. This closes the arithmetic
gap only. It does not supply `Q(o|π)` or the simulated posterior updates.

### Explicit non-solution

The futon3c portfolio function called `posterior-entropy` measures entropy of
the *current action posterior*. That can be a useful uncertainty diagnostic,
but it is not expected posterior entropy reduction: it neither predicts an
observation under each policy nor simulates an observation-conditioned update.
It must not be ported as the repair for this mission.

### Two warrants

1. **Formal/model-relative warrant:** the implementation computes expected KL
   from one coherent declared joint model over `(θ,o)` under each policy.
2. **Empirical/world-relative warrant:** the observation model predicts held-out
   evidence and the simulated update matches the update later performed on
   real observations.

The first warrant can be completed before the second. Neither is supplied by a
winner-changing ablation alone.

### Scope in

1. Choose the first model parameter `θ` whose uncertainty is decision-relevant.
2. Declare the observation alphabet for every epistemic policy.
3. Provide `Q(o|π,θ)` and marginalise it to `Q(o|π)`.
4. Simulate `Q(θ|o,π)` using the same update code used after real evidence.
5. Compute EIG with the existing coherence-checked kernel or a proven analytic
   Dirichlet equivalent.
6. Record per-policy prior entropy, expected posterior entropy, EIG, model
   version/hash, and degeneracy reasons.
7. Shadow the new quantity against `model-uncertainty-bonus` before replacement.
8. Retire or demote posterior spread to telemetry once EIG is live.

### Scope out

- Calling current action-posterior entropy EIG.
- Treating posterior spread alone as expected reduction.
- Learning a universal world model for every War Machine channel at once.
- Changing the pragmatic risk or engineering controller terms.
- Wiring an EIG value whose observation distribution is constant, fabricated,
  or copied from the observation that actually occurred.

### Completion criteria

- **C1 — Parameter identity:** `θ` has a versioned, machine-readable domain and
  a documented relationship to A4a/BMR state.
- **C2 — Observation closure:** every policy in the epistemic candidate set has
  a finite or integrable `Q(o|π,θ)`, including no-result, failure, timeout,
  conflicting evidence, and missing evidence.
- **C3 — Normalisation:** priors, predicted observations, and posteriors are
  finite and normalised; structural zeros are explicit.
- **C4 — Bayesian coherence:** `Σ_o Q(o|π)Q(θ|o,π)=Q(θ|π)` within declared
  tolerance, or an analytic proof establishes the equivalent identity.
- **C5 — Shared updater:** simulated and realised posterior updates call the
  same implementation with only the observation source changed.
- **C6 — Policy conditioning:** at least two policies induce demonstrably
  different predicted observation distributions or update magnitudes.
- **C7 — Reduction tests:** an uninformative policy has EIG zero; a perfectly
  discriminating binary experiment has EIG `ln 2`; relabelling observations
  leaves EIG invariant.
- **C8 — Provenance:** traces carry model identity, prior entropy, expected
  posterior entropy, EIG, and any fail-closed reason.
- **C9 — Prospective calibration:** held-out log loss/Brier score evaluates
  `Q(o|π)`; predicted and realised entropy reductions are compared.
- **C10 — Replacement discipline:** `model-uncertainty-bonus` is not removed
  until placeholder/cascade selection invariants remain green and a shadow has
  quantified winner, abstain, and scale effects.
- **C11 — Honest R18 update:** Box 1 changes only after live provenance and
  prospective evidence distinguish “kernel exists” from “epistemic value is
  load-bearing.”

## 2. MAP

### Existing components

| Component | Location | Status |
|---|---|---|
| Coherence-checked finite EIG kernel | `src/futon2/aif/epistemic_value.clj` | Built and tested; unwired |
| Dirichlet/BMR operations | `src/futon2/aif/bmr.clj` | Candidate posterior machinery |
| A4a posterior parameters/spread | `src/futon2/aif/a4a.clj`, `a4a_substrate.clj` | Live uncertainty source; no policy observation model |
| Policy forward model | `src/futon2/aif/forward_model.clj` | Predicts operational effects; does not yet predict parameter-learning observations |
| Categorical belief A/B/D | `src/futon2/aif/belief.clj` | Useful model-coherence precedent; different latent object |
| Existing engineering proxy | `model-uncertainty-bonus` in `efe.clj` | Honest controller augmentation |

### Missing join

The missing object is not another entropy helper. It is a versioned experiment
model:

```text
policy π
  -> possible evidence o with Q(o|π,θ)
  -> simulated shared posterior update θ -> θ'
  -> KL[Q(θ') || Q(θ)]
```

The first implementation should select one narrow policy family with recorded
outcomes—for example, a pattern acquisition/review action whose pass/fail or
adjudication result already feeds a Dirichlet count. If no current policy has
that contract, the correct MAP result is to define the evidence event first,
not to synthesize probabilities from gap scores.

## 3. DERIVE

### Stage 1 — finite reference model

Implement a tiny, inspectable hypothesis model for one policy family. Enumerate
`θ`, enumerate possible evidence, predict the evidence distribution, simulate
the shared posterior update, and call `expected-information-gain`. Use it as the
executable reference even if the production form later becomes analytic.

### Stage 2 — Dirichlet analytic or particle form

For A4a count tables, either derive the expected Dirichlet entropy reduction
analytically or use a finite posterior-predictive enumeration whose convergence
is tested against the finite reference. State exactly whether information is
about a categorical parameter, a model structure, or a hidden state.

### Stage 3 — dark policy wiring

Emit `:expected-information-gain` beside the existing posterior-spread bonus.
It remains outside selection while collecting predicted-versus-realised update
evidence. The mode flag must preserve byte-identical selection when off.

### Stage 4 — shadow and operator decision

Report magnitude, within-tick variation, winner/abstain changes, correlation
with posterior spread, predictive calibration, and realised information gain.
Only then may Joe decide whether EIG replaces the proxy, changes its weight, or
stays diagnostic.

## 4. VERIFY / INSTANTIATE plan

1. Preregister the first `θ`, policy family, evidence alphabet, corpus split,
   and primary calibration metric.
2. Add generative-model validators and reduction tests.
3. Add shared simulated/real updater equivalence tests.
4. Produce a deterministic shadow artifact under
   `holes/labs/M-aif-policy-conditioned-eig/`.
5. Update `wm-baseline.md`, trace schema/provenance, R18 manifest and explainer
   in the same parcel as any live flip.

### Current honest status

The mathematical kernel exists. The generative experiment model does not.
Accordingly, Box 1's `model-uncertainty-bonus` residual remains real.
