# Proof ⟨1⟩3 ⟨2⟩2: prospective ending kernel and C — discovery (read-only)

zai-1, 2026-09-22. Sources read: `run_ending_classification.clj` (classify*
:63-108; declaration `resources/wm/run-ending-classification-v1.edn`),
`focus_receipt.clj` (:12-56 discover, facets regex :15-17), `live_c.clj`
(:104-230 read-sources/derive-live-c), `cascade_model_manifest.clj`
(preference-distribution :491, horizon-g :542 = Lean `PolicyHorizon.horizonEFE`,
with-pattern-theta :271, q-support-outside-c-universe refusal ~:782),
`war_machine.clj` joint decision (:6105-6180), the frozen reference input.

## Answers

**1. Class assignment — YES, both candidates land in the SAME class with
probability 1, so class-space C alone cannot pass the ⟨1⟩3 counterfactual.**
Mechanics: an increment's class comes from the focus-receipt's facet rows
(`classify*` :85-96, `:facet-map` in the declaration). Facets are computed
from the commit's PATHS by the regex at `focus_receipt.clj:15-17`
(`(^|/)wm_` requires an underscore; `war_machine`, `M-wm-`, … likewise).
Both candidates' predicted commits are `resources/wm/rechecks/…edn` (C1) and
`resources/wm/eig/held-out-split.edn` (C2) — NEITHER matches the WM facet
regex (`wm/rechecks` has no underscore after wm), so both facet as
`other/unattributed`, both land in the same class (`elsewhere-useful` if the
current focus is WM), and under the point-mass declared prior both succeed
deterministically. (Discovery note, not fixed: `resources/wm/…` paths
faceting as non-WM looks like a regex gap — flag for Joe, no change here.)
Symmetry is total: each candidate produces exactly one of the two wanted
tokens (coverage 1/2 each), same class, θ = documented default 1, no
recorded rates for either family, equal (zero) habit history.

**2. What can differ in the declared prior.** The only runtime carrier of a
non-point-mass rollout is pattern θ (`with-pattern-theta` :271-280; kernel
:282-300: θ to the produced state, 1−θ stay). Lawful θ values are recorded
success rates (NONE exist for either family — addendum 1 of the q0 file) or
the documented default 1. **Any per-candidate θ < 1 would be an invented
number.** The remaining lawful difference is structural, not numeric: C2's
declared effect explicitly produces NO measured outcomes (a precondition
artefact whose own acceptance names no-result/failure/timeout classes),
while C1's recheck produces a measured disposition. That is a declared,
checkable difference in each action's PREDICTED EVIDENCE OBLIGATIONS, not a
tuned probability. The contracted risk path accepts it: `horizon-g`/step
scoring consume a per-τ preference map via `om/query`
(`cascade_observation_scoring.clj:70-76,88-91`); nothing forces the
preference to be a point mass.

**3. Carrier today.** The live joint decision computes TOKEN-space G:
`war_machine.clj` joint decision derives C via `live-c/derive-live-c`
(mission-id tokens: `:alive/*`, `:closed/*`, `:star/*` — weights from
wholeness L, status, star map — NOT Joe's 55/35/5/5) and scores through
`efe/rank-actions` → `cascade_observation_scoring/score-candidate` →
`om/query {:op :score …}` with `cascade_model_manifest/horizon-g`
(Lean `PolicyHorizon.horizonEFE`). Class-space risk is computed by NO live
caller today; the class kernel (`run_ending_classification.clj`) is
record-only and runs at close, not at selection. Note both produced
`:repair/*` tokens appear in NO live-c want set, so today's token C is also
flat between C1 and C2.

**4. Unknown mass.** Today the scorer refuses rather than renormalising:
a belief state carrying tokens outside C's universe is the typed refusal
`:q-support-outside-c-universe` (`cascade_model_manifest.clj` ~:782-790,
the WM-06 domain-meeting rule). That is the right spine for the kernel:
unknown mass must be an EXPLICIT outcome in the preference map with a stated
value, never dropped-and-renormalised (which would flatter the
least-observed action — the exact trap ⟨2⟩2 names).

## Smallest design that discriminates without inventing numbers

1. **Prospective kernel** (new, pure, record-only-adjacent): for each
   candidate, map its PREDICTED post-build evidence to a class distribution
   `{class p, :unknown u}` where `u` is the declared share of outcomes the
   action's acceptance explicitly leaves unmeasured — computed from the
   candidate's own declared structure (C2's no-result classes ⇒ u > 0; C1's
   measured disposition ⇒ u = 0). Numbers come from the declarations, not
   from tuning.
2. **C = Joe's 55/35/5/5, fixed, over the four classes**, plus ONE extra
   explicit outcome `:unknown` whose preference value **Joe sets once, as a
   ruling** (a single global number, e.g. "unknown counts as elsewhere" or
   "unknown is slightly worse than elsewhere"). This is a Joe decision of
   the same kind as 55/35/5/5 — not a per-candidate parameter.
3. **Risk through the contracted functions:** the kernel's per-τ preference
   map feeds the existing `om/query :score` / `horizon-g` path in place of
   the live-c token weights (both are contracted carriers; aggregation
   choice — class carrier — declared on the decision, per the plan's
   "aggregation changes KL" note).
4. Everything else unchanged (habit E, F, β, the tie-break, the law).

**Effect:** C1 scores class c with u=0; C2 scores the same class c with
u>0 ⇒ different expected log-preference ⇒ different G ⇒ the counterfactual
passes WITHOUT any tuned number. **If Joe declines to rule a value for
:unknown, then no design exists that discriminates these two candidates
without inventing numbers** — the declared effects are perfectly symmetric
(same class, same coverage, same θ, no recorded rates) — and under execution
rule 2 the proof stops for Joe's decision, exactly as it did at ⟨1⟩2.
