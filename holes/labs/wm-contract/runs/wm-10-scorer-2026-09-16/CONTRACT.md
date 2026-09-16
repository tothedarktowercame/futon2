# WM-10 C4: canonical finite-categorical scorer

Author: codex-28, directly commissioned by Joe after the zai-18 closure-plan agreement (p4ng a727b02). No delegation. This is the first bounded implementation of deliverable D2, not WM-10 completion. Independent agent review is not claimed.

## Real interface, and why a new scorer

`step-g` accepts q and A in the existing `categorical-ambiguity/ambiguity` shape and reuses that function's validation unchanged. In particular, A must be `:observed-estimate`; the function does not grant this authority or authenticate a supplied tag. Synthetic tests exercising this contract are not measured A.

C has the existing machine-Q-risk shape (`:model :support :mass :provenance`). The existing risk adapter requires exactly twelve outcomes and uses binary64 division/logarithms. It cannot provide a general finite-domain scorer or rational error enclosure; its source and public behavior are unchanged. The new scorer reuses `machine-model/distribution-admission` for C and for represented-value row evidence.

Input q is the predicted state distribution at the supplied observation point, NOT implicitly the initial belief. WM-05 must produce each point's q from justified B/guards/history. This scorer constructs J(s,o)=q(s)A(o|s) and Q(o)=sum_s J(s,o), using exact represented rational arithmetic. It does not invent A, B, C, a horizon, or a production model.

`total-g` requires a nonempty explicit observation schedule and one supplied step per point. Model, state/outcome support, policy and occurrence must agree across steps; C and predicted q may vary with time. Empty pattern constructions still need predictions on that same schedule: an empty step list does not score as zero. Policy/occurrence/point are caller-provided identity assertions, not authenticated binding evidence. D1/D3 must bind them to the real producer.

Actual eventual consumer: the receipt construction scoring seam currently emitting `:cascade-g-not-computed`, followed by WM-11's choice law. Neither is changed in this delivery. Whole WM-10 delivery continues to depend on WM-05/06 producers, consistent firing semantics, candidate coverage and actual choice.

## Numeric contract `rational-atanh-24-v1`

Integers/ratios denote themselves; decimals their exact decimal rational; IEEE values their exact binary rational via BigDecimal(double). No renormalization. All J, Q and interval arithmetic is exact rational arithmetic.

For positive x, reduce x=2^k*m with 1<=m<=2. Let z=(m-1)/(m+1), hence 0<=z<=1/3. Use

    ln(m) = 2 * sum_{i>=0} z^(2i+1)/(2i+1).

The first n=24 terms give lower L. The omitted positive tail is bounded above by

    2*z^(2n+1) / ((2n+1)*(1-z^2)).

This follows by replacing every remaining denominator by 2n+1 and summing the geometric series. The same construction encloses ln(2). Signed interval multiplication and addition enclose ln(x)=ln(m)+k*ln(2). No binary logarithm or fixed floating epsilon occurs in the result. Range reduction beyond |k|=4096 refuses explicitly instead of returning a truncated result.

Risk sums Q*(ln Q-ln C); ambiguity sums -J*ln A. Independently assembled cross-entropy sums -Q*ln C and mutual information sums J*(ln J-ln q-ln Q). Their two resulting G intervals need not be identical but must enclose the same exact expression. Zero terms are omitted only when their exact weight is zero. Positive Q with zero C refuses as infinite risk.

Bounds concern the mathematical expression evaluated at the supplied represented values. They are NOT confidence intervals for model accuracy. Near-normalized admitted inputs remain marked `:exactly-normalized-inputs? false`; they do not acquire the premises of Lean's exact kernels. Resolving that model-to-kernel relation is still required for full correspondence. The analytic derivation above is not a Lean proof and has not had independent human/agent review.

Later delta-G admission must subtract the parent/extension intervals and require an upper endpoint strictly below zero. Overlap with zero cannot establish improvement. This packet does not implement or activate that admission/choice consumer.

## Acceptance evidence

- `reference.py` derives the two G expressions separately using Python Decimal at 90 digits on a nontrivial rational joint; `reference.edn` is reproducible output. Every component is enclosed by the Clojure interval. This is independent arithmetic, not independent authorship; Decimal rounding is far below these enclosure widths but is not itself a formally verified oracle.
- Deterministic/zero-mass tests, authority refusals, zero-C refusal, support/model/schedule controls, represented-float preservation, and a seven-state/twelve-outcome fixture with the five positive/seven-zero C shape. Its values and observation rows are synthetic.
- No production labels, observations, scorer activation, selection, click or checklist closure.

No existing witness or witness dependency was edited. No store or shared JVM is used by these tests.
