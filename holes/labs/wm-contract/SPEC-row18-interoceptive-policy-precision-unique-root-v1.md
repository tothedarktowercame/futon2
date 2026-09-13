# Row 18 analytic refinement: a sufficient unique-root criterion

Status: **proved conditional specification; not adopted or integrated**. This
refines, but does not replace,
`SPEC-row18-interoceptive-policy-precision-proposal-v1.md`.

## Exact field and criterion

For the already formalized canonical finite `:both` field, let

```
delta(beta) = E_pi(beta)[G] - E_pi0(beta)[G]
R           = hi - lo
f(beta)     = beta - delta(beta)
```

where every `G(i)` lies in `[lo,hi]`, the support is nonempty, habit is
strictly positive, `pi` has score `log(habit)-G/beta-F_pi`, and `pi0` has the
same score with only `F_pi` zeroed.  The new Lean theorem proves existence and
uniqueness of a positive solution to `f(beta)=c` under:

1. `lo <= hi` and `c > 3R/2`;
2. for every positive beta, the exact derivative identity
   `delta'(beta)=(Var_pi(G)-Var_pi0(G))/beta^2`;
3. `Var_pi0(G) >= 0` and `Var_pi(G) <= R^2/4`.

The derivative and variance premises are packaged as
`DerivativeCertificate`; they are obligations, not axioms or claims about a
floating solver. The canonical finite module already discharges positive-beta
continuity and `|delta(beta)| <= R`.

Every root is in `[c-R,c+R]`. The criterion places that interval strictly
above `R/2`. On that positive interval the certificate gives
`delta'(beta)<1`, hence `f'(beta)>0`; endpoint signs and continuity give a
root, and strict increase makes it unique. The same proof includes `R=0`:
then `c>0`, the range bound forces `delta=0` on positive beta, and the unique
root is at `c`.

## What remains unproved

The theorem does **not** yet construct `DerivativeCertificate` from the
canonical softmax definition. That requires additive exact-real lemmas for:

- differentiation of the normalized finite exponential weights, yielding the
  expectation derivative `Var(G)/beta^2` for both posterior arms; and
- the bounded-variable variance inequality `Var(G) <= (hi-lo)^2/4`
  (Popoviciu) on the occurrence-indexed finite distribution.

Those are the next mathematical packet. Until they are proved, the result is
a sufficient conditional criterion, not a uniqueness certificate for a
retained field. Failure of `c > 3R/2` would mean only that this sufficient test
does not decide uniqueness; it would not establish multiple roots.

Positive habit remains an explicit application premise so that
`exp(log(habit(i)))=habit(i)` has its probabilistic meaning. Empty support is
outside the theorem's `Nonempty (Fin n)` carrier. No claim is made about
Clojure floating continuity, bisection correctness, solved/held branch
selection, calibration, adoption, runtime integration, or admission.

## Proof artifact

`DarkTower/WarMachine/InteroceptivePolicyPrecisionUniqueRoot.lean` adds:

- `root_localisation`;
- `derivative_lt_one_above_half_range`; and
- `canonical_unique_positive_root`, specialized to the canonical finite
  posterior's `evidenceDelta` and explicitly carrying positive habit; and
- `canonical_prior_rate_root_order`, which supplies the resulting uniqueness
  to the already accepted `finiteFieldRootOrdering` theorem and obtains the
  strictly larger beta / strictly smaller gamma conclusion.

The module imports the accepted finite-posterior layer and changes no frozen
carrier.
