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

## codex-1's reply (2026-10-28, job invoke-1793157422530-5738-1862d9d6)

codex-1 agrees with separating mission choice from plan construction and
supports packet 1. Its qualifications, which amend the plan above:

1. **Where it came in.** `ea69853cdd` introduced the objective; `fcf9b6ad27`
   added the Q2 conjuncts equating the outer cascade menu with
   `comparedPolicies`, while Q2 still restricts compared policies to the
   selected task's construction. Keep what those commits were for
   (recomputable scores; selection corresponds to execution); drop the
   requirement that outer menu items carry cascade identities. codex-1 calls
   the defect *premature all-target construction*, not a demonstrated infinite
   regress: the code is finite, but choosing a mission depends on building
   every alternative's inner plans.
2. **The mission outcome model must be specified, not borrowed.**
   `outer_target_score.clj` is not the mathematical authority: it uses
   target-history Dirichlet means, normalised predictive entropy as
   "ambiguity" (predictive entropy is not expected conditional-likelihood
   entropy) and zero information gain. Packet 1 must name the latent state,
   likelihood, horizon and information-gain variable before those terms are
   used, and give cost one declared interpretation (no double counting).
   A need/enable edge is evidence about readiness, not automatically a
   probability or an exclusion. A missing inner construction after choice is
   a recorded outcome, never a fallback to all-target planning.
3. **Non-vacuous Lean statements.** Replace "the score is a function of
   mission observations" (true by construction if the function simply has no
   inner-family argument) with:
   - equal mission observations, parameters, preferences, eligibility and
     selection inputs give equal outer scores and frontiers, for any inner
     families;
   - with the same recorded tie draw, the same mission is selected;
   - a concrete change in relevant mission evidence can change the prediction
     and the ranking; an arbitrary change to an unselected inner family cannot;
   - only the selected mission receives an inner construction.
4. **Q4/Q8** reference the same cascade carrier and need matching updates.
   The algebraic frontier lemmas in `OuterSelectionCanonicalG.lean` stay; the
   cascade-score correspondence theorem cannot be relabelled as a
   mission-model theorem.
5. **The 10-26 pair.** codex-1 would not require the pair to become unequal
   unless a justified model predicts the difference. Open with Joe: his
   standing view is that a tie is an inference defect, which argues for
   finding the discriminating mission evidence rather than accepting the tie.

## Joe's ruling on the tie (2026-10-28)

Joe: "The outer loop is a policy, so it needs its own generative model. It
needs preferences over outcomes like fun, profit or difficulty, beliefs about
what working on each task will lead to, and G computed over task choices at
that level. The inner policies can feed into it, for example as evidence about
how hard a task is likely to be, but they're inputs to the outer model, not a
stand-in for it. ... if every inner policy is 'throw it in the bin', the outer
loop's beliefs about difficulty are flat, so of course everything ties. So the
ties and the backwardness turn out to be the same bug."

Consequences for the plan:

1. **Point 5 is settled.** The 10-26 pair tied because the outer level had no
   beliefs of its own, only cascade structure. Packet 2 passes only if the
   pair is ordered by the outer model (its doability 0.8 vs 0.5 and value
   0.335 vs 0.354 are evidence that model must read). A tie there is a failing
   test, not an accepted result.
2. **Preferences are over task outcomes.** C is declared over an outer outcome
   vocabulary that includes at least fun, profit and difficulty (how hard the
   work turned out to be); Q(o|m) is the outer model's belief about what
   working on m leads to.
3. **Inner policies are evidence, through a declared channel.** codex-1's
   invariance theorem ("an arbitrary change to an unselected inner family
   cannot change the ranking") still holds for families that were never
   built. But evidence derived from inner policies (past attempts on m, how
   hard its last plan was to execute, a cheap difficulty estimate) is allowed
   and wanted. It enters as a named field of the mission observations, so
   changing that field can change the ranking (sensitivity), while changing
   the family object behind it, with the field held fixed, cannot
   (invariance). Packet 1 is reviewed against this; if its world type has no
   such field, the reviewer adds it.

Status: packet 1 (Lean) running on codex-1, job
invoke-1793157541599-5742-8688a721.
