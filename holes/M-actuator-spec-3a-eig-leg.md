# Spec: 3a — the BMR epistemic-value leg in the forward EFE ranker (`− λ·EIG`)

**Owner/reviewer:** claude-4 owns (I review) → **claude-5 reviews reviewed-lane** (the sign/
semantics). **Edits:** `futon2/src/futon2/aif/efe.clj`. **Pays down:** R13 (breaks the G-ties
tie-floor) + R5 (the genuine EIG term). **Per the AMENDED Seam-2 lock:** `risk − λ·EIG`, source
`{concept → stddev}`, absent→0, endpoint-count, λ≈1.

## Why this is a clean fit
efe.clj's `:G-total` is ALREADY canonical: `G-risk + G-ambiguity − info-weight×G-info` — an
ambiguity term (+) and an info-gain/EIG term (−). The existing `g-info = (info-gain next-var)` is
*observation*-level info (nats). 3a adds **model/structure** uncertainty (BMR-stddev, endpoint-count)
as a **DISTINCT** subtracted EIG leg — not folded into `g-info` (units differ) and NOT the ambiguity
leg (the demo's −0.006 proved "+" is exploit-toward-certainty; it's sought, not a cost).

## Build (efe.clj)
1. **`graph-efe-terms`** — add opts `:eig-lookup` (`{concept → stddev}`, default `{}`) and
   `:eig-weight` (λ, default `1.0`). Compute:
   ```clojure
   :G-eig-bmr (* (double eig-weight)
                 (reduce + 0.0 (map #(double (get eig-lookup % 0.0)) (:produces mission))))
   ```
   Σ BMR-stddev over the mission's **produced capabilities** (the concepts), absent→0. Add
   `:G-eig-bmr` to the returned term map. (Confirm with claude-5 that `{concept → stddev}` keys on
   the same capability ids that appear in a mission's `:produces`.)
2. **`compute-efe` `:G-total`** — subtract it: add `(- (:G-eig-bmr graph-terms 0.0))` to the
   `:G-total` sum (a NEW subtracted epistemic-value leg), and surface it in the `:G-augmentation` /
   decomposition per the existing convention so it's auditable.
3. **Thread** `:eig-lookup` + `:eig-weight` through `compute-efe` → `graph-efe-terms` and
   `rank-star-map-actions` (opts).
4. **Units + distinctness:** `:G-eig-bmr` is endpoint-count, its own leg, λ≈1. Do **NOT** fold into
   `g-info` or `g-ambiguity`; do **NOT** touch `:G-graph-pragmatic`'s byte-identity.
5. **Default `{}` ⇒ `:G-eig-bmr = 0` ⇒ `:G-total` byte-identical** (regression-safe until A4a feeds
   real stddevs).

## Acceptance (tests)
- **Explore drive:** with an `:eig-lookup` giving a concept high stddev, a mission producing that
  concept gets a **lower** `:G-total` (ranks *better*) than with `{}` — high EIG is sought.
- **R13 tie-break:** two missions tied on risk but differing in EIG now **discriminate** in the ranked
  order — the tie-floor breaks.
- **Regression:** default `{}` ⇒ `:G-total` byte-identical to current for a fixture action.
- **Absent → 0:** a concept not in the lookup contributes 0.
- **Distinct term:** `:G-eig-bmr` present as its own key (endpoint-count), separate from
  `g-info`/`g-ambiguity`.

## Gates
clj-kondo clean; `futon4/dev/check-parens.el` clean; tests green. Byte-identity of
`:G-graph-pragmatic` preserved. No `:7071` restart. Bell **claude-4** back with commit sha + a
before/after `:G-total` showing a high-EIG mission ranking better; then claude-5 reviews the sign.
