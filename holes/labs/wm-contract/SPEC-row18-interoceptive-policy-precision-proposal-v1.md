# Row 18 proposal v1: interoceptive policy-precision prior seam

Status: **proposal only; not adopted, configured, or integrated**.  Schema name:
`:wm/interoceptive-policy-precision-proposal-v1`.  This replaces no existing
snapshot law unless separately reviewed and adopted.

## Authority and unchanged law

J1 (`aif-equations.edn` `:choices :temperature-update :ruling`, lines 298-304)
retires the R7-to-R14 precision conflation and requires policy precision
`gamma = 1/beta`, with `beta` updated by Friston 2017 eq. 2.7 over
`P(gamma)=Gamma(1,betaPrior)`. Production realizes the fixed point in
`policy_precision.clj:33-61,95-395`, carries solved/held beta at :497-560,
sets `tau=beta` only in `policy.clj:135-143`, and scores `-G/tau` at
`policy.clj:214-218`.

This proposal does not alter placement of `G`, `F_pi`, or `ln E`; does not
multiply posterior gamma; and never falls back to selection gain.

## Hypothesis assessed

Let `m` be a positive interoceptive confidence factor. For v1's candidate,
`m=1` when no genuine trip is open and `m=1/2` when one or more genuine trips
are open. For a Gamma distribution with shape 1 and rate `betaPrior`, mean
precision is `1/betaPrior`. To scale the **prior mean** by `m`, the proposed
rate is

```
betaPrior' = betaPrior / m
```

and the unchanged posterior fixed point is

```
pi0(beta) = softmax(lnE - G/beta)          ; ruled :both placement
pi(beta)  = softmax(lnE - F_pi - G/beta)
betaPost  = betaPrior' + (pi(betaPost) - pi0(betaPost)) dot G
tau       = betaPost
gamma     = 1 / betaPost
```

At `m=1`, this is exactly the original equation. At `m=1/2`, the prior rate is
doubled and its mean is halved. This is an explicit engineering prior
assumption: no observation likelihood or incident calibration currently says
that an open trip should double the Gamma rate. It must never be described as
measured evidence, and a half prior mean is not a half posterior gamma.

## Monotonicity verdict — refined after independent review

The original proposal's affine example `delta(beta)=2*beta-3` remains useful
only as **unrestricted-domain history**: it shows that prior-rate algebra alone
does not imply posterior monotonicity, but it is unbounded and therefore is not
an actual finite-softmax policy-field reversal.

For the real exact finite-policy model,
`delta(beta)=E_pi[G]-E_pi0[G]` is bounded by `max(G)-min(G)`. The new
`InteroceptivePolicyPrecisionBounded.lean` proves that bound for any two finite
normalised nonnegative weight vectors. It also proves: continuity and global
boundedness of delta, a positive baseline root, a strictly increased prior
rate, and **uniqueness of the new positive root** imply existence of a new root
strictly above the baseline root and hence strictly lower gamma. Baseline-root
uniqueness is unnecessary.

This conditional theorem does not license the Clojure field. Exact-real
softmax continuity follows only after the production score/support model is
represented in Lean; floating evaluation is not continuous as an exact-real
function. Production bisection convergence and opposite signs at two bracket
ends prove a located numerical branch, not global positive-root uniqueness. A
finite sign scan also cannot exclude an even-multiplicity root or variation
between samples. Multiple roots make branch identity ambiguous. A production
`:held-unsolved` or `:held-absent` state does not establish a newly solved
adjusted posterior and cannot be credited as applied reduction.

Therefore v1 is admissible only as a **field-checked hypothesis**:

1. At identical pinned `G`, `F_pi`, `ln E`, candidate identities, solver
   bracket/options, and unadjusted beta prior, solve arms `m=1` and `m=1/2`.
2. Both arms must converge, bracket, and return finite positive beta. The new
   arm additionally needs global positive-root uniqueness established by a
   reviewed analytic theorem (for example strict monotonicity of
   `b-delta(b)` on `b>0`) or a verified interval/root certificate covering the
   whole positive domain. Bisection or a sampled sign scan alone is insufficient.
3. The open-trip arm qualifies only when its applied gamma is strictly lower
   than baseline. Equality or increase is `:counterexample-no-reduction`, not
   firing evidence. Multiple roots are `:multiple-roots`; unsolved and absent
   preserve their production sources and remain non-qualifying.
4. Restoration is a counterfactual at the identical non-trip inputs: the
   discharged arm uses `m=1` and must reproduce the unadjusted production
   result. It does not reverse, erase, or replay task learning; `betaPrior` and
   the policy field are held identical between that arm and baseline.

## Input and refusal signature

The future proposal consumer accepts a versioned record containing the durable
genuine-trip identities and dispositions, exact run/trace identity, source
hashes, `m`, carried beta with `:beta-source`, aligned candidate IDs, `G`,
`F_pi`, `ln E`, and solver options. Before solving it refuses:

| condition | required typed result |
|---|---|
| authority unreadable or source hash mismatch | `:interoception-unreadable` |
| shadow/test/discharged item presented as open genuine trip | `:ineligible-trip` |
| trip record and policy field differ in run identity | `:cross-run-input` |
| nonpositive/nonfinite `m` or beta prior | `:nonpositive-prior-input` / `:nonfinite-prior-input` |
| missing/misaligned `G`, `F_pi`, `ln E`, or candidate support | existing strict alignment refusal, preserved |
| bracket absent, solve unconverged, or multiple roots detected | `:held-unsolved` / `:multiple-roots`, never engineering fallback |
| solved gamma is equal to or above baseline | `:counterexample-no-reduction` |

The output retains the original and adjusted rates, `m`, both solve records,
beta/gamma/tau, beta source, field pins, and the verdict. Held outcomes retain
the prior beta and exact production source label rather than masquerading as a
new posterior.

## Proof boundary

New module
`DarkTower/WarMachine/InteroceptivePolicyPrecisionProposal.lean` proves:

* exact reduction to the old prior/equation at `m=1`;
* positivity of the adjusted rate for positive inputs;
* exact prior-mean scaling and positive `gamma=1/beta` reciprocity;
* a concrete unrestricted-delta counterexample to algebra-only monotonicity;
* the closed typed outcome vocabulary for a later checker.

The refinement module proves the finite-distribution range bound and the
conditional lower-gamma theorem under continuity, boundedness and uniqueness
of the new positive root.

It does not prove the production solver converges, brackets uniquely, or moves
gamma monotonically on a retained field. Those are mandatory commissioned
checks before adoption. No runtime implementation is authorized by this
proposal.
