# Row 18 analytic refinement: discharged canonical variance certificate

Status: **exact-real proof; not runtime adoption or admission**. This is the
analytic dependency identified by
`SPEC-row18-interoceptive-policy-precision-unique-root-v1.md`.

The additive module
`DarkTower/WarMachine/InteroceptivePolicyPrecisionVariance.lean` defines the
occurrence-indexed canonical expectation and weighted variance directly from
`InteroceptivePolicyPosteriorFinite.weight`. Thus repeated policy values remain
separate candidates and the certificate's witnesses are the actual variances,
not existential placeholders.

For normalized nonnegative weights it proves:

```
variance = sum_i w_i (G_i - E[G])^2 >= 0
variance = E[G^2] - E[G]^2
lo <= G_i <= hi  ->  variance <= (hi-lo)^2 / 4
```

The Popoviciu proof first sums
`G_i^2 <= (lo+hi)G_i-lo*hi`, then bounds the resulting quadratic in the
already bounded mean. No smoothing, set conversion, or support deduplication
is used.

For fixed finite `G`, `F_pi`, and habit, and positive beta, it differentiates
the exact canonical score, raw exponential weight, normalizer, normalized
expectation, and evidence difference. The resulting identities are:

```
d E_pi[G] / d beta  = Var_pi(G) / beta^2
d delta / d beta    = (Var_pi(G)-Var_pi0(G)) / beta^2
```

Here `pi0` retains the same habit and `G/beta` term and zeroes only `F_pi`, as
required by the accepted `:both` placement. The theorem
`canonical_derivativeCertificate` constructs the earlier certificate from
these actual variances and their proved bounds. Consequently
`canonical_unique_positive_root_discharged` and
`canonical_prior_rate_root_order_discharged` require no caller-supplied
analytic certificate.

The exposed sufficient theorem still requires nonempty occurrence support,
strictly positive habit, a fixed field, `lo <= G_i <= hi`, `lo <= hi`, and
`c > 3(hi-lo)/2`. Positive habit is retained because Lean's total `Real.log`
outside the positive domain is not a probabilistic habit interpretation.
Failure of the sufficient inequality leaves uniqueness unknown; it does not
prove multiple roots.

This exact-real result does not attest Clojure floating derivatives, bisection
correctness, branch selection, solved/held source behavior, a retained field,
calibration, runtime integration, adoption, or node admission. No optional
field arithmetic was performed in this packet.
