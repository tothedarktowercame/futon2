# E-outer-loop-improvement — recover useful task-selection knowledge after the loop works

**Status:** DERIVE (opened 2026-10-02; advanced after the 2026-10-02
debugger run exposed completed and unreasoned selections). Live; the unchecked items below are
the work, not admission gates.

**Driver:** War Machine. **Operator:** Joe. **Repository:** `futon2`.

## IDENTIFY

The War Machine has regained the necessary ordering boundary: it selects one
current M/E/T task before constructing and scoring a fresh tactical pattern
cascade for that task (`39f574a39`, `bb67416b0`). Its present outer policy is
deliberately only a reproducible seeded-uniform baseline. Earlier machines and
design work contain much richer ideas about feasibility, structural geometry,
strategic fit, doability, epistemic value, habits, costs, blockers, and
portfolio effects. Those records are useful, but importing them before the
basic select → construct → enact → observe loop works would risk rebuilding an
elaborate selector whose choices the machine cannot usefully enact.

This excursion preserves that prior work and provides a later route for
measured improvement. It does **not** block ordinary clicks, and it does not
make any historical score or cascade canonical.

### Open work and completion conditions

- [ ] A recorded series demonstrates that the baseline outer selector sees
  the complete current M/E/T population, selects before tactical construction,
  and produces more than one task identity across successive eligible runs.
- [ ] For every proposed additional outer-loop signal, a replayable ablation
  names its source, units, missing-value semantics, and at least one recorded
  field on which enabling the signal changes selection or posterior mass.
- [ ] Feasibility is restored as explicit policy support, distinct from
  predicted usefulness and from the availability of a preconstructed cascade.
- [ ] Recent completion, grounded progress, failure, blocker, cost, time, and
  token observations enter the next outer decision through separately visible
  update channels rather than one opaque mission-value scalar.
- [ ] A staged comparison shows that the improved outer policy increases
  closure-or-progress per time/token without reducing task-population coverage
  or causing long unchanged-target runs.
- [ ] The accepted outer-policy receipt and its learned state remain distinct
  from the selected task's tactical cascade/G certificate.

**IDENTIFY exit: Met.** The controlled runs established the gap: seeded-uniform
selection is reproducible but cannot say why its chosen current task is useful,
and completed or unactionable selections consumed clicks before later repairs.

## Minimal reason-bearing META policy

The current seeded-uniform selector is a reproducible inhabitant of the outer
policy's type. It is not an adequate live policy: a seed and an index explain
how an item was drawn, but not why that M/E/T/A item should be worked now.

Before an outer decision may launch tactical cascade construction, its receipt
must answer **why this item?** from task-level evidence. At minimum it records:

1. the complete current M/E/T/A support and typed exclusions;
2. the selected item's admissibility and actionable next surface;
3. each policy term actually consulted, with source, units, value and
   missing-value semantics;
4. the selected item's contribution or rank under each term;
5. at least the nearest rejected alternative and the reason it lost; and
6. the resulting choice probability or deterministic ordering rule.

The minimal first live policy need not recover the historical information-
geometric controller. It may use only explicit priority, actionable status,
blockers, recent progress/failure, repetition, and estimated time/token cost.
But it must use enough of those observations to make the selected item better
supported than an alternative. If no available item is distinguishable on the
declared evidence, uniform choice is retained as a typed fallback
`:no-task-level-preference-evidence`, not presented as a positive rationale.

This receipt must not observe a task's tactical patterns, constructed
cascades, universes, precedence, or G. Those belong to the inner loop after
the M/E/T/A identity is selected. Conversely, an inner construction failure
becomes a next-run task-level observation (failure kind, blocker, time and
cost); it is not silently converted into permanent outer ineligibility.

### Launch gate

An outer selection is launchable only when its receipt has a selected task,
the complete support/exclusion census, and either:

- an evidence-backed comparison showing why it outranks or outweighs an
  alternative; or
- the explicit uniform-fallback absence above, during a registered baseline
  or shadow-policy comparison.

A bare `:seeded-uniform-task-support` receipt outside such a registered
baseline is a typed `:outer-policy-rationale-missing` stop, not permission to
spend an author/reviewer dispatch.

## Scope

In scope: task-level M/E/T(/A) proposal support, ranking or sampling,
feasibility, outcome learning, cost, portfolio balance, and repeat/stuck
behaviour. Out of scope until the baseline loop is viable: selecting one
historical theory as canonical, tuning against benchmark missions, or folding
tactical cascade readiness into task eligibility.

## MAP — prior outer-loop material to keep at hand

### 1. The pre-H5b outer loop and what it actually did

`holes/E-outer-loop.md` is the primary reconstruction. At `5d55e7a0^`
(2026-09-17), six proposers enumerated missions, tickets, patterns, open WM
registry obligations, tensions, and learn-the-action-class moves. Proposals
were enriched with value/history signals, graph feasibility restricted policy
support, and `efe/rank-actions` plus `policy/select-action` chose from the
admissible set. Commit `5d55e7a08` removed this path while introducing the
cascade-only decision; it removed task selection rather than replacing it.

The empirical companion is
`holes/labs/wm-contract/E-outer-loop/I3-records.md` (`1834cc73e`). Across 897
ticks from 2026-05-18 through 2026-09-12, the machine chose only 17 distinct
targets. Its longest unchanged-choice runs were 306, 128, and 108 ticks.
Feasibility exclusions were nearly inert. This establishes both that autonomous
task choice existed and that its persistence/coverage behaviour was poor.

### 2. Structural geometry, strategy, and doability

`holes/M-wm-three-factor-mission-value.md` records the July model implemented
at `568c44d51`:

```text
0.25 central(m) + 0.45 strategic(m | pi_strat) + 0.30 doable(phase m)
```

followed by completion gating and non-progress decay. `central` normalized the
joint-centrality map and was the explicit structural-geometry term;
`strategic` located a mission on a declared strategy cascade; `doable` used a
phase table peaked at INSTANTIATE. The blend was additive because multiplication
made a zero centrality erase independent strategic evidence. This was a useful
engineering controller, not a calibrated generative model.

### 3. Epistemic value and live doability

`holes/labs/wm-contract/C492-mission-epistemic-value.md`, its
`runs/U22-mission-epistemic/` packet, and commit `9e27ad818` preserve a
default-off mission-level epistemic term with before/after rankings and
rank-movement evidence. `C498-doability-liveness.md`,
`runs/U44-doability-liveness/`, and commit `b4bbd26ed` test whether doability
consumed live mission phase rather than an inert default. These are especially
useful as experimental designs: both insist on comparison arms where the term
could actually change a choice.

### 4. Later theoretical audit

`holes/missions/M-wm-strategic-mission-selection.md` is the strongest later
synthesis. It classifies the old factors rather than treating them as
interchangeable numbers:

| Historical field | Candidate future role | Must not silently become |
|---|---|---|
| `central` | structural relevance or proposal potential | preference |
| `strategic` | reason-bearing, context-conditioned proposal support | habit |
| phase `doable` | predictor of useful progress | hard feasibility |
| completion/operator gate | policy-support exclusion | numeric penalty |
| non-progress decay | evidence awaiting an explicit transition model | habit |
| selection frequency | strategic habit | mission quality |

It also distinguishes the strategic task policy from the tactical pattern
cascade. They may eventually share an AIF-shaped algebra, but must not share
identities, candidate support, learned counts, or score semantics.

### 5. Practical scheduling dimensions

`holes/labs/wm-contract/NOTE-task-manager-outer-loop.md` records operational
dimensions that may be useful before elaborate geometry: freshness, ownership,
blockers, downstream blockage, cost, automatability, available capability, and
resource hierarchy. Priority is described as an input/preference rather than a
fact inferred from convenience.

### 6. Geometry material that is adjacent, not yet a task selector

`holes/labs/wm-contract/PILOT-redirection-geometry-2026-09-20.md` measures
retrieval/rank redirection between conversational events. It may later inform
belief change or novelty instrumentation, but it is session/retrieval geometry,
not evidence that a particular mission should be selected. Keep it separate
unless a future DERIVE step establishes that correspondence.

**MAP exit: Met.** The survey above identifies the earlier implementation,
empirical traces, geometric and feasibility models, practical scheduling
signals, and their limits without promoting any of them to current authority.

## Ready versus missing

| Ready—reuse or replay | Missing—do not pretend it exists |
|---|---|
| Complete current M/E/T enumeration and an outer-before-inner receipt | Demonstrated useful baseline behaviour across repeated clicks |
| Historical task-selection traces and the I3 extraction | Calibrated task-level transition/outcome model |
| Three-factor controller and hermetic policy-swap tests in git history | Evidence that its hand-set weights improve current outcomes |
| U22 epistemic and U44 doability comparison packets | Current per-task uncertainty and live likelihoods at selection time |
| Explicit feasibility/support distinction in prior code and notes | A current production support predicate covering M/E/T(/A) |
| Strategic-policy mission and dark typed-control-graph kernel | Reviewed relation corpus and a validated live strategic policy |

## DERIVE exit criteria — launch contract

The outer loop remains in DERIVE until all four conditions below are met.  A
green condition is evidence that the corresponding dependency exists; it is
not permission to weaken or bypass the other three.

- [x] **Numeric G authority exists.** A pinned declaration supplies the units
  and normalization for every outcome channel, numeric preference means with
  positive variances and weights, and the authority under which those numbers
  may be used. The audit
  `futon3/library/meta/meta-outer-g-authority-audit.edn` preserves the original
  `:blocked-missing-numeric-authority` finding and records its resolution as
  `:provisional-prior-declared-predictions-missing`; illustrative numbers are
  not an exit witness.

  Exit evidence 2026-10-02: Joe authorized a transparent provisional prior,
  explicitly not an empirical calibration. Futon3 `7ba5d2208` records the
  five-channel declaration at SHA-256 `e18b3d24…`: closure, grounded progress,
  abstention/failure, elapsed-budget fraction, and token-budget fraction. It
  declares means `[1 1 0 0 0]`, wide positive variances `4.0`, equal weights,
  candidate-envelope-relative clamped resource normalization, typed exclusions
  for unresolved channels, and registered-evidence-only revision. Futon2
  `b18d8a41e` verifies the source and emits preferences/normalization only.
  Independent review passed 4 tests / 21 assertions plus EDN parsing, exact
  digest, and lint; absent prediction and information inputs still refuse.
- [ ] **Every scored policy has complete grounded G inputs.** Each candidate
  carries source-bound predictive distributions and a candidate-specific,
  Bayes-coherent information model over the common outcome vocabulary.  The
  evaluator refuses missing prediction, normalization, preference, or
  information-model inputs, and a mutation of each input class fails a named
  gate.

  Blocker evidence 2026-10-02: Futon3 `7890a839a` records the source-pinned
  decision at SHA-256 `33ac1617…`. The verified historical dataset has 39
  usable rows (M30/E0/T9/A0), all selected by legacy inner fallback, with only
  five elapsed measurements and no token measurements. Current candidate
  fields establish applicability and observation surfaces, not outcome
  probabilities. Therefore no candidate-specific Q, prior, or posterior is
  presently authorized; uniform halves, budget-as-forecast, per-kind fitting,
  and first/singleton selection are prohibited. Independent review confirmed
  the three producer hashes and reran 9 tests / 56 assertions. The criterion
  remains open pending an explicitly authorized acquisition or predictive-
  prior route.

  Operator direction 2026-10-03: take the source-grounded predictive-prior
  route. Candidate features should begin with structural centrality in the
  mission network and explicit feasibility for automated progress. Earlier
  tension/curvature proposals are historical inputs, not default features:
  prefer morphogenetic signals when a source-pinned correspondence to task
  development is available. A separate stratified-acquisition Algorithm may
  update the prior from registered outcomes; it must not bootstrap the initial
  model or silently turn acquisition into ordinary task selection.

  Operator addition 2026-10-03: for the first proof-of-concept, the existing
  Tornhill signals from `M-the-perfect-crime` are an authorized stand-in for
  richer morphogenesis geometry. The proxy must remain named as such. Its
  source surface is the audited per-file report (`revs`, `churn`, complexity,
  `hotspot = revs × complexity.total`, age and trend), joined to a task only
  through a pinned task-to-file mapping. Missing joins are typed absences, not
  zero development pressure. Centrality and automated feasibility remain
  separate features; Tornhill hotspot is not itself an outcome probability.

  Task-state design checkpoint 2026-10-03: Futon3 `988976137` records the
  decision artifact at SHA-256 `5c322278…`. A selected policy may expose
  already-computed centrality, automated-progress surface, and Tornhill proxy
  as deterministic current-task observations; canonical categorical EIG is
  exactly zero because no latent uncertainty is resolved. The Gaussian port
  cannot represent exact determinism without invented observation noise.
  Current progress-surface coverage is M162/E7/T0/A0; centrality is stale and
  mission-only; Tornhill is stale with zero currently admissible joins.
  Terminal/resource predictions remain refused. Implementation awaits the
  operator's task-state preference and missing-kind ruling plus regenerated
  source snapshots.

  Operator ruling 2026-10-03: use the conservative task-state policy, amended
  by freshness. Prefer higher current centrality and a present automated-
  progress surface. A mission on EFE-carpet level set 0 is presumptively
  outdated and de-prioritized into a retirement-review surface rather than
  treated as ordinary attractive work. Tornhill hotspot/trend and Salingaros
  mess are current-work signals only when freshness evidence shows recent
  contact; a high historical value untouched for months is `:museum-work`, not
  a live hotspot or urgent mess. Museum classification does not itself retire
  or delete a mission: a separate, reviewable retirement pass must establish
  lifecycle disposition. Pairwise comparisons still require at least one
  shared, current, authoritative channel; missing channels remain absence.

  Freshness audit checkpoint 2026-10-03: Futon3 `f85eca94b` records
  `meta-task-state-freshness-audit-v1.edn` at SHA-256 `9f978b2c…`. Independent
  review verified the EFE producer's exact 0–6 normalized scope-density band
  calculation and its own warning that the band is not a value judgement.
  It also verified the existing 90-day activity boundary (`<90` recent,
  `>=90` stale). No classifier was admitted: the EFE band has no independent
  pinned snapshot, the wholeness artifact lacks generation/input pins and is
  currently modified by another seat, and the Tornhill/activity snapshots are
  stale (with E/T/A absent). Thus no stale value enters comparison, missing is
  not zero, and neither `:museum-work` nor level-set 0 mutates lifecycle state.
  The next mechanical work is to produce independent current snapshots before
  deriving the review-only task-state classifications.
- [ ] **The verified META policy is the live outer selector.** The complete
  field observation, construction exclusions, comparison receipt, selected
  identity, and rationale reach `full_loop_runtime` / `full_loop_runner`
  before tactical cascade construction.  The legacy seeded-uniform selector
  cannot launch an ordinary click except as the explicitly typed registered
  baseline fallback described above.
- [x] **Approved Algorithms are real, bounded field members.** The pinned
  approval catalog admits `A-self-heal`, exact capability matching controls
  its injured arm, and a positive numeric click budget is available before it
  becomes selectable.  With no click available it is excluded with a typed
  resource reason; approval alone must not fabricate an executable policy.

  Checkpoint 2026-10-02: resource admission is implemented by futon3c
  `db19d4cf5` and futon2 `ca375699a`. A locked, non-consuming, source-pinned
  availability receipt admits `A-self-heal` only with at least one ordinary
  click and otherwise records `:algorithm/click-resource-unavailable`.
  Independent review reran 8 Futon2 tests / 53 assertions and 5 Futon3c tests /
  159 assertions. This condition remains unchecked until exact injury-to-
  capability matching is demonstrated through the same constructed field.

  Review finding 2026-10-02: `8ef8c37fb` added exact keyword matching, but its
  claimed real-injury fixture pins a test-constructed injury map to the bytes
  of `futon3/library/meta/meta-outer-policy-cascade.edn`. Those bytes specify
  the policy; they do not observe an injured machine. Repeating that pin as
  expected authority proves equality only. The condition therefore remains
  open pending a reconstructively verified injury observation derived from
  retained debugger/run-record evidence.

  Exit evidence 2026-10-02: `ed3fb32d0` closes that finding. It independently
  pins the committed projection (`f338af6e…`) and terminal run record
  (`dfac8e2b…`), reconstructs every required projection field from the latter,
  and requires exact equality before capability admission. Independent review
  passed 17 focused tests / 119 assertions, including wrong-run bytes,
  re-pinned altered projection, and changed run/click identity. Together with
  `db19d4cf5`, `ca375699a`, and `46e8922d6`, this demonstrates approval,
  click-resource gating, exact injury matching, and construction of only the
  matching `:run-algorithm` template without G inputs.

**DERIVE exit: Not met.** Joe approved `A-self-heal` on 2026-10-02, allowing
the fourth condition's catalog work to proceed.  The other three conditions,
and the click-availability portion of the fourth, remain to be demonstrated.

### PSR-1: `futon-theory/stop-the-line` for outer-policy launch

- Pattern chosen: futon-theory/stop-the-line
- Candidates: stop-the-line, typed fallback, deploy behind a flag
- Rationale: A random baseline is a useful type inhabitant but cannot justify
  spending an author/reviewer click.  The four conditions above turn missing
  authority, inputs, wiring, or resources into observable refusals.
- Confidence: high — the preceding controlled runs exposed each missing seam
  independently.

## Re-entry rule

Begin DERIVE only after registered runs show that the baseline loop can select,
construct, enact, and record useful progress, or when those runs expose a
specific outer-selection failure such as repeated unchanged work, infeasible
selection, starvation, or excessive cost. Choose the smallest historical
mechanism that addresses the observed failure and run it first as a recorded
shadow/counterfactual. A theory that does not affect a decision or improve the
measured outcome remains archived evidence, not live machinery.
