# E-outer-loop-improvement — recover useful task-selection knowledge after the loop works

**Status:** IDENTIFY (opened 2026-10-02). Live; the unchecked items below are
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

## Ready versus missing

| Ready—reuse or replay | Missing—do not pretend it exists |
|---|---|
| Complete current M/E/T enumeration and an outer-before-inner receipt | Demonstrated useful baseline behaviour across repeated clicks |
| Historical task-selection traces and the I3 extraction | Calibrated task-level transition/outcome model |
| Three-factor controller and hermetic policy-swap tests in git history | Evidence that its hand-set weights improve current outcomes |
| U22 epistemic and U44 doability comparison packets | Current per-task uncertainty and live likelihoods at selection time |
| Explicit feasibility/support distinction in prior code and notes | A current production support predicate covering M/E/T(/A) |
| Strategic-policy mission and dark typed-control-graph kernel | Reviewed relation corpus and a validated live strategic policy |

## Re-entry rule

Begin DERIVE only after registered runs show that the baseline loop can select,
construct, enact, and record useful progress, or when those runs expose a
specific outer-selection failure such as repeated unchanged work, infeasible
selection, starvation, or excessive cost. Choose the smallest historical
mechanism that addresses the observed failure and run it first as a recorded
shadow/counterfactual. A theory that does not affect a decision or improve the
measured outcome remains archived evidence, not live machinery.

