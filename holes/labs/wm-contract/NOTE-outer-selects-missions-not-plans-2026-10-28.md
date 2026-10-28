**This proposes changing the specification** (Requirements.lean Q2 and the
outer policy objective). Joe asked for it on 2026-10-28: "chase it out ... and
replace it with something that makes sense".

# The outer loop selects missions, not plans (2026-10-28)

From claude-11, after Joe's museum / state-fair turns of 2026-10-28.

## The defect

To choose which mission to work on, the live outer selector first builds and
scores detailed plans for every eligible mission:

1. `meta_live_outer_selector.clj` `select-live` calls
   `family-selection/full-eligible-target-field` on *all* of `on-map`.
2. `families-for-field` (`wm/family_selection.clj:142`) calls
   `target-family/policy-family` once per target: reading, pattern graph,
   occurrence squares, cascade candidates.
3. `sealed-pre-choice-menu` (`:404`) turns the cascades into menu entries.
4. `outer-canonical-g/produce` scores each entry with
   `cascade-coapply-g/compare-at-horizon` over the cascade's units and
   precedence.
5. `outer-canonical-selection/evaluate` picks the least G and returns
   `{:type :advance-cascade-live-policy :target …}`. The mission is a side
   effect of which plan scored best.

So no mission can be chosen until every mission has a worked-out plan. That is
Ryle's regress: the plan for the chosen mission would itself need its parts
chosen the same way, all the way down. It is also not active inference. In a
hierarchical generative model each level's policies predict that level's
outcomes; the slow level does not wait for the fast level to finish planning.

The mission features that a comparison should use are already observed and are
not read by G on this path: mission value (centrality, strategic role,
epistemic term; `war_machine.clj` `enrich-candidates-with-mission-value`),
doability, ticket readiness, ownership, resource history and cadence
(`outer_target_score.clj`). The 10-26 tie between wiring and
recommendation-bindings came from this: value 0.335 vs 0.354 and doable 0.8 vs
0.5 sat in the receipt while G read only cascade structure.

## How it got into the specification

The specification had this right once. Requirements.lean Q2, ruling of
codex-68 on 2026-10-16 (mathlib4 `9e79af1bee`): "Outer selection precedes
inner construction: an outer action is not a constructed policy ...
Unselected tasks are not said to have inner policies."

Three days later, `ea69853cdd` (2026-10-19, "Specify strict outer Cascade
policy objective") and `fcf9b6ad27` added conjuncts to the same Q2 that
require the outer carrier's menu to consist of cascade policies
(`outerPolicySelection.selected.policy.cascadeDigest = currentChoice.cascade`,
menu identities = `comparedPolicies`). The docstring and the conjuncts now
contradict each other, and the code follows the conjuncts.

## The replacement

**Outer level.** A policy is "advance mission m". Its predicted outcome is a
distribution over the outer outcome vocabulary, predicted from m's mission
features (offer, cost, readiness). G_outer(m) = risk(Q(o|m) ‖ C) + ambiguity −
information gain, with C from the declared preferences. No cascade is built.
Readiness includes Giraffe need/enable edges (is what m needs already
enabled?) and cheap facts such as "a reading exists for m", never a
constructed plan.

**Inner level.** After m is chosen, build the policy family for m alone and
let the existing canonical cascade G choose the plan. Everything in
`outer-canonical-g` / `cascade-coapply-g` stays, moved to the inner side and
called with one target.

**What pins it.**
- Lean: the outer score is a function of mission observations only, so two
  worlds that differ only in their inner policy families select the same
  mission (invariance theorem). The existing `OuterSelectionCanonicalG.lean`
  frontier theorems keep holding; only the source of `risk`/`ambiguity`
  changes.
- Clojure: `target-family/policy-family` is called exactly once per click,
  for the selected target; replacing a non-selected mission's cascade family
  with garbage does not change the outer choice; the 10-26 pair is ordered by
  its features and does not tie.

## Order of work

1. Lean: mission-level outcome model and the invariance theorem (one packet).
2. Clojure: outer G over mission features, aligned with (1); no cascade
   construction before choice (one packet).
3. Clojure: inner plan selection for the chosen target only (one packet).
4. Remove the all-targets field construction from `select-live` and retire
   `:advance-cascade-live-policy` as an outer action type.
