**Superseded in part (2026-10-28, Joe).** "The defect" below stands: the
live selector builds every mission's plans before choosing a mission. "The
replacement", its order of work and packet 1 are withdrawn. They built a second,
outer-only active-inference model (`Mission`, `Observations`, its own `G`) when
the outer loop is the same machine applied to a different cascade. See
"Withdrawn" at the end.

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
3. **(Withdrawn, see below.)** An earlier version of this point let evidence
   derived from inner policies (how hard m's last plan was to execute) enter
   the outer observations. Joe's Emacs example rules that out.

## Joe's Emacs example (2026-10-28)

Joe: "you don't select Emacs based on the key commands you are planning to
enter, you select Emacs based on what the job is you intend to do: do you want
to edit a file or do you want to browse a web page? And your policy about how
to edit the file only makes sense inside Emacs; it's not going to be the same
policy if you edit the file in VS Code. ... you could split hairs and say I
prefer VS Code because I don't like the keyboard commands, but that's not the
level at which this is operating."

Consequences:

4. **An inner policy is defined only given a selected mission.** Its type is
   indexed by the mission (keystrokes are Emacs keystrokes). There is nothing
   to compare across unselected missions, so the outer ranking must not take
   inner families as an argument at all. codex-1's invariance theorem becomes
   true by the type: the outer G has no inner-family parameter.
5. **The outer observations are about the job.** What m is for and what
   working on it leads to: value, doability, readiness, ownership, and the
   outcomes (fun, profit, difficulty) recorded after earlier work on m. Those
   recorded outcomes update Q(o|m). They are outcomes at the outer level
   ("that took all day"), not the content of the plan that produced them.
   No field carries plan structure, plan cost or cascade shape upward.
6. Packet 1 is reviewed against 4 and 5: if its world type passes an inner
   family, or a plan-derived field, into the outer ranking, the reviewer
   removes it.

Status: packet 1 (Lean) done, reviewed below.

## Packet 1 review (claude-11, 2026-10-28)

codex-1, job invoke-1793157541599-5742-8688a721, mathlib4 `a0ca1b00fd`:
`DarkTower/WarMachine/OuterMissionModel.lean`, one new file (352 lines), no
other file touched. Checked: `git show --stat`; read the whole file;
`lake env lean` on it (4 s, clean); `#print axioms` for all 13 theorems gives
only propext, Classical.choice, Quot.sound.

Against 4 and 5 above: nothing removed. `G` takes `Inputs` only;
`innerFamilies` sits beside it in `World` and is read only by
`constructSelected`, after selection. `Observations` has offer, readiness
signal, need/enable edges and resource history (recorded use, an outcome);
no field is derived from a plan. Each mission's state prediction reads only
that mission's own observations.

What the tests pin:
- `outer_invariance`, `replace_families`: true by the types, as point 4 says
  they should be. Not vacuous, since `World` does carry families and the
  construction step reads them.
- `evidence_changes_selection`: a real pair. With the readiness signal true
  the frontier is `[0]`; with it false it is `[1]`, families arbitrary in both
  worlds. The scores are computed by the model's own definitions
  (`witness_scores`), not assigned: G0 = -log(7/8) or -log(1/8) against
  G1 = log(1/2) - (log(1/8)+log(7/8))/2.
- `risk_uses_declared_cost`: risk equals sum of p log p + p * (-log pref), so
  cost has one reading; resource use counts only through preferences over O.
- `construction_only_selected`, `missing_is_recorded`: the construction
  contract, by definition.

Gaps (recorded, not fixed here):
- The witness has zero ambiguity and zero information gain (`witness_terms`),
  so only the risk term is exercised. A sign error or a wrong log argument in
  `informationGain` would pass every theorem in the file. Next Lean packet: a
  witness with an uncertain parameter (Θ = Bool, posterior 1/2) where the
  information gain is positive and changes a ranking, or a proof that it is
  nonnegative.
- `horizon` is declared and never read. The terminal-horizon reading lives in
  the docstring (O is the outcome at the horizon), not in any definition.
- The observation fields are schematic: value, doability and ownership from
  point 5 are not named; `offer` stands in for value.
- The 10-26 pair is not touched, as instructed; that question is still with Joe.

## Withdrawn (Joe, 2026-10-28)

Joe: "G is canonical ... the whole machine is canonical. The outer loop has
all the features of the inner loop, it's just applied in a different way."
The mistake was to build the machine twice instead of applying it twice with
the code already written.

The design was already in the library and this note did not cite it:
`futon3/library/meta/meta-outer-policy-cascade.edn`
(`:meta/reason-bearing-meta-selection`). An outer policy is a filled instance
of that cascade (task-kind, target, next-move, resource envelope, evidence
channel, stopping rule), the injury arm admits only self-heal algorithms, and
`:meta/minimise-g-over-filled-meta-policies` is G over the filled policies with
one outcome vocabulary. Tactical construction runs after
`:selected-meta-policy`, which answers "the defect" without a separate model.
Its own rule excludes packet 1's shape: "Comparing task labels or opaque scalar
priorities is not G over policies."

Actions:
- mathlib4 `a0ca1b00fd` (`OuterMissionModel.lean`) reverted in `fe1965d5c0`.
  The packet 1 review above is kept as history; it checked the file's
  theorems, not whether it reused the machine.
- Points 1 to 6 under codex-1's reply and Joe's rulings that ask for a
  mission-level outcome model with its own `G` are void. Joe's Emacs point
  (inner policies exist only given a selected outer policy) still holds; the
  META cascade already has it.
- Next: a discovery packet to codex-1 on whether `GOverCascades` /
  `CascadeEFEPolicies` take the META cascade's slots as they stand, and what
  `OuterCascadePolicyObjective.lean` (2026-10-19) already does. Then one Lean
  file: a filled META policy scored by the existing G definitions, with a
  two-policy example (one target, two next-moves) whose ranking flips on an
  evidence-channel observation.
