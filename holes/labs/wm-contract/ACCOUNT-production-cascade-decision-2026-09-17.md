# Account: what production decides, once the flat path is gone

claude-7, 2026-09-17, for claude-4's removal-and-replacement handoffs (Joe's
ruling: the flat single-action path must be impossible to run). This states
what the examples Joe and claude-7 worked through support. Where they do not
reach, it says so.

## 1. The decision object

Production decides at two levels. Neither is a flat action.

**A. Construction (builds the family).** For each admissible target, a
construction policy extends a partial cascade by moves (read what exists,
propose a pattern from an incident or a sibling cascade, choose grain, order by
needs, run on a recorded case, request meaning review). A move is admitted iff
it lowers G of the resulting partial cascade (Stage 0, SPEC-cascade-policy-
semantics :37–41), where G must include the epistemic value of the move (what
it would teach about uncertain interpretations). Moves:
futon3/library/cascade-construction/ (futon3 29b77c5, 34c8585).
Status: patterns only. No code, no Lean.

**B. Selection (chooses among constructed cascades).**
- Object selected: a cascade on a target, `{:target t :precedence [admitted
  interpretations…]}`.
- Family: for every admissible target, its constructed cascades plus its empty
  cascade (the empty cascade replaces the flat no-op).
- Inputs per target: fact universe with values true, false or :unknown (declared
  grain); admitted interpretations (guard, effect, θ); want; one common universe
  across all candidates compared; declared T and β.
- Score: G by horizon-g-sparse over that universe at T.
- Posterior σ(ln E − F − G/β) (PolicySelection.selectionPosterior).
- The enacted step is the Bayes action: the first acting pattern, maximised over
  the posterior summed across cascades (ActionMarginal.IsBayesAction). One step
  is enacted per tick, but it is a marginal of the cascade posterior, not a
  ranked single action. Under P10 the cascade's guards are re-checked every
  step, so the next tick continues or re-selects.

**Grain.** The target is the unit whose state one run changes: cycle, repo,
item, or handoff × namespace. Choosing it is part of construction
(cascade-construction/choose-the-grain-where-state-lives).

## 2. Worked examples and what each showed

All are in futon2/holes/labs/wm-contract/WORKED-INSTANCES-pattern-interpretation-2026-09-16.md
(2f16128a, 0e4b7ef0).

1. **Buffer cleaner** (futon3/library/buffer-cleaner; zai-7/codex-26 scoring,
   zone wip/cascade-policy-example.html). The live choice was between two
   wirings, aggressive and conservative. Each wiring is a cascade over the same
   five patterns, and the only difference is what the automation decides.
   Interpreted at cycle grain (9 facts), the cascade reproduced the recorded run
   (receipts emitted, act blocked on gate-exists⁻, "no threshold-eligible
   wiring"). The decision is between cascades; no per-buffer flat action is
   needed. Per-item rules stay in the gate.
2. **Inbox zero** (futon3/library/inbox-zero, dc7b821/56ccd8f; gate run
   2026-09-16 21:01). At repo grain the cascade predicts classify → escalate →
   route. Push is blocked for an outlier or an unknown screen. The
   implementation only logged, and the difference named three defects (futon0
   027b55e). The useful output was which steps of a cascade are enabled and why
   others are blocked. A flat "push repo X" ranking would have hidden exactly
   that.
3. **Test registry** (futon3/library/test-registry, dc53a0a; review of futon2
   cb5f8b1288). At handoff × namespace grain, the lane (full rerun or
   spot-check) follows from each namespace's guards (warrant, tests changed,
   mandatory lane). The counterfactual measured 2 of 64 deftests saved, which
   re-targeted the build. The decision came out of cascade structure, not from
   scoring flat options.
4. **"Stop B when permission is withdrawn"** (WALKTHROUGH-beta-authority
   follow-up). The real choice was between interpretations (launch-only vs
   continuing permission). It never reaches a flat selector, and β cannot
   recover an interpretation nobody proposed. So the interesting decision is in
   construction (§1A), and a flat path has nothing to contribute there.
5. **Construction as policy** (Joe, 2026-09-17: "a policy for building
   policies"). The cascade-construction moves carry an @execution kind
   (deterministic, LLM or human). Confidence is split into step noise (θ) and
   evidence about that noise (the concentration), and habit over moves is
   learned from receipted use. This is where learned contextual confidence
   enters, inside the same AIF account.

**What the examples do NOT show:** a production run choosing among cascades
for several WM targets. None was run at that scale.

## 3. What exists, and how it relates to claude-4's pieces

**Lean (all certified in MachineContracts r13, mathlib4 afde3af092, holder
not-live-path unless noted):**
- GOverCascades.CascadePolicy: the conformant carrier; compositionBlind_cannot_separate.
- PolicyHorizon.horizonEFE: live-shadow via shadow-cascade-g.
- PolicySelection.selectionPosterior and ActionMarginal.IsBayesAction.
- CascadeTransition guard, firstEnabled and cascadeKernel (P10): live-enactment via acting-order.
- ExactBeliefTrajectory.
- MachinePolicySet: marked non-conformant (7933b454cb). Registry :policy-set is
  :diverges (futon2 fc6d89c2).
- Missing: any Lean for construction (§1A), and the q0 / f⁺-f⁻ encoding (D2–D5).

**Clojure:**
- cascade_selection.clj: selection-posterior, bayes-choice.
- policy/select-action-cascades.
- cascade_model_manifest: horizon-g-sparse, first-enabled, cascade-kernel.
- receipt_construction/construct and cascade_policy/organise: build one
  up-closure extension from retrieved and interpreted patterns.
- shadow_cascade_g.
- controller-authority/authorize.

**claude-4's pieces:**
- **cascade-lane in war_machine.clj (:6013, entered only on :cascade-problem).**
  This is the right route: R1/R4/R5 cascade candidates, rank-actions cascade
  branch, select-action-cascades at declared β, authorize. Do not supersede it.
  Promote it to the only decision route: every tick builds :cascade-problem per
  admissible target, and the flat block (~:6607 proposers, ~:6691 flat
  rank-actions, ~:6788 select-action / ~:6800 default-mode-select) is deleted.
- **candidate-space (a0a6000d).** It is correct as a fixture generator, and it
  honestly takes interpretations as input. It is not construction: it
  enumerates every non-empty subset (capped at 8, :subset-explosion), where
  construction should extend partial cascades by admitted moves. Keep it for
  tests and small hand-admitted targets. Do not make it production's family
  generator.
- **codex-7's item cascades as production candidates.** Acceptable only as
  proposals entering construction. Each needs facts, admitted interpretations
  (guard, effect, θ) and a want before it can be scored. As pattern-id lists
  they are the same gap as today's :cascade-policies (ids with no
  interpretation, no G), so admitting them directly would bring back an
  unscoreable candidate.

## 4. What must be true before production enacts a cascade decision

Per target:
1. The fact universe is admitted (declare-the-universe rules):
   - progress facts are false at cycle start;
   - :unknown only means unobserved;
   - thresholds carry their source;
   - meanings are unique;
   - grain is declared.
2. Every pattern in the candidate has an admitted, source-cited interpretation.
   Compilation per D2–D5 needs Joe's approval, at least D3. An effect that would
   produce f⁺ over an observed f⁻ is refused.
3. A want is declared, with outcome wants as θ-weighted effects.
4. One common universe is shared by the candidates compared; T is declared.
5. β is declared, recorded :declared, with :no-approved-beta-authority kept open.

Globally: authorize accepts cascade decisions (seam d).

**Missing input.** The target is excluded with a typed refusal naming the
missing input: :no-admitted-interpretation (with the failing clause),
:want-not-declared, :universe-not-admitted, :beta-not-declared,
:no-constructed-candidate. The refusal is routed to whoever can supply the
input (route-the-untranslatable): authoring for interpretations, the fact
owner for facts. It is not merely recorded. If every target is excluded, the
tick emits a typed abstention carrying all per-target refusals. It never falls
back.

## 5. Enforcement

claude-4's three rules are right:
- flat selection code deleted;
- a runtime invariant that every emitted decision is a cascade choice or a typed abstention;
- default-suite tests that fail on reaching a flat entry point.

Additions:
- **Abstention must list its per-target refusals.** A bare abstention would
  become the new silent default.
- **The invariant must also reject a "cascade" of exactly one pattern produced
  by wrapping a flat action.** Require candidates to come from
  construction/organise with interpretation receipts, not from an adapter over
  ap/compose-proposers.
- **Delete default-mode-select's use in the tick, and make the standing test
  (5502ef7d) the acceptance gate.** Also grep-gate the default suite for
  `policy/select-action ` and `default-mode-select` calls under scripts/ and src/
  outside tests of their own deletion.
- **Record the construction receipt with each decision:** moves taken, family
  searched, coverage. Stopping construction is not target success.
