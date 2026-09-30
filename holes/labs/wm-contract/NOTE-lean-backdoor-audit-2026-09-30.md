# Lean backdoor audit: runtime-supplied selection inputs

Date: 2026-09-30. Owner: codex-6 (controller). Status: audit only.

This audit reads the Lean adversarially: for every value the runtime may
supply, assume it supplies the least informative value that still typechecks.
The triggering incident is
`incidents/INCIDENT-2026-09-30-one-policy-selection-repeated-refusal.md`.
No theorem about a posterior, action, or rollout is useful if its carrier,
model, or preference was supplied in a form that made the answer inevitable.

No Lean or Clojure was changed, no click was run, and no JVM was loaded.

## Scope read

I read the declarations and proofs that expose runtime inputs in:

- `GOverCascades`, `MachinePolicySet`, `CascadeEFE`,
  `CascadeEFEPolicies`, `TokenPreference`, `CascadeTransition`, and
  `CertificateStates`;
- `Proof2/CascadePolicySet`, `EnactmentHabit`, `PolicyPosteriorAtMachine`,
  `ActionAtMachine`, `ObservationAtMachine`, `BeliefStepAtMachine`,
  `BeliefAtMachine`, `RolloutAtMachine`, `OutcomeRiskAtMachine`,
  `StatePredictionErrorAtMachine`, `ChannelPrecisionAtMachine`,
  `LaplaceUpdateAtMachine`, `ContainmentOrderAtMachine`,
  `CoApplicationAtMachine`, `PrefixFreeEnergyAtMachine`,
  `ExpectedFreeEnergyPosterior`, `TargetUniverse`, and `MissionPreference`;
- the directly imported carrier definitions in `Holes`, `PolicyRollout`,
  `ObservationProcess`, `MachineTemperature`, `PolicyHorizon`,
  `PreferenceRiskSeparation`, `ObservedInterpretation`,
  `AdjudicationCounts`, and `CoApplicationKernel` where they determine one of
  the inputs below.

I did not read all 252 `DarkTower/WarMachine` files line by line. In
particular, scan-learning, revision-rate, and certificate-routing modules not
on the selection-to-action dependency path were not audited beyond imports and
declaration search. This note therefore does not certify the whole directory.

## Existing protections (not gaps)

The following are real constraints already present:

- A `ProbabilityKernel` carries non-negativity, row normalisation, duplicate-
  free support, and zero mass off support (`Holes.lean:7076-7089`). Cascade
  predictions and observations remain normalised (`CascadeEFE.lean:112-120`).
- `CandidateFamily` requires a duplicate-free, non-empty policy list, every
  policy admissible, and every listed policy successfully scored
  (`CascadeEFEPolicies.lean:20-27`). A refused score cannot be smuggled into
  that family (`:33-40`).
- A missing firing-pattern interpretation makes `resolve` fail rather than
  inventing a transition (`CascadeEFE.lean:64-71`); `PatternSlot.interp = none`
  is retained as a typed hole (`CascadeTransition.lean:141-177`).
- `PreferenceSpec.want` is non-empty, its scales have signs, and `zeroed`
  cannot exclude the entire outcome space (`TokenPreference.lean:27-41`).
  Its result is normalised (`:59-96`). Preferred-zero risk is refused
  (`CascadeEFE.lean:150-154`).
- Empty policy menus, non-positive temperature, `F = ⊥`, and the absence of
  any finite candidate are typed failures (`PolicyPosteriorAtMachine.lean:
  109-136,313-325,594-599,869-880`). Zero rollout depth and a policy outside
  the supplied list are also typed failures (`RolloutAtMachine.lean:64-91`).
- `TargetUniverse` distinguishes absent from explicitly empty; it even proves
  that an empty universe flattens target-local delta
  (`TargetUniverse.lean:67-106,132-149`). It does not forbid the caller from
  recording the empty universe.
- `MissionPreference` refuses a missing stated weighting instead of silently
  making it uniform (`MissionPreference.lean:178-216`). It does not establish
  that a supplied weighting expresses the task.
- `CertificateStates.CensusComplete` rejects missing, duplicate, extra, and
  reordered declared entries, and reading two rejects retained refutations
  (`CertificateStates.lean:99-117,141-153,189-198`). It deliberately permits a
  complete census of negative or meaningless declarations under reading one.
- The files contain executable bad-case theorems and fixtures, including
  `emptyMenuHasNoPosterior`, `allTermsInfiniteIsRefused`,
  `noUniverseIsNotEmpty`, and `unstatedWeightingIsNotUniform`. These stop
  defaults; they do not establish provenance or adequacy.

Thus the gaps below are not complaints that Lean permits negative mass,
unnormalised rows, or an empty `CandidateFamily`. They are cases where a
perfectly normalised, non-empty value can still be useless.

## Free inputs, degenerate witnesses, and closures

“Lean” below means a theorem and a negative control can close the mathematical
part. “Record” means the run must carry pinned source values so a checker can
recompute the construction; Lean cannot inspect the live filesystem or prove
that serialized bytes were honest.

### 1. Policy carrier and policy construction

**Free inputs.** `cascadePolicySet` accepts an arbitrary
`menu : List (PolicyKey M P)` (`Proof2/CascadePolicySet.lean:35-41`). Each key's
`mission`, ordered `shown`, and `semilattice` are caller data (`:13-19`). At the
mathematical layer a `Policy` accepts a caller-built `Cascade` and a membership
decision (`CascadeEFE.lean:48-56`); `Cascade` itself contains nodes,
organise-added nodes, edges, an acyclicity proof, and precedence
(`Holes.lean:29-35`). `MachinePolicySet.machinePolicySet` likewise takes an
arbitrary ranked flat list and is explicitly labelled non-conformant
(`MachinePolicySet.lean:9-23,40-55`).

**Degenerate witness.** A singleton menu containing a singleton cascade, or
several duplicate-equivalent policies under different ids. It is non-empty,
can be acyclic and admissible, and its posterior is necessarily one. The
incident establishes that every selection which reached author work used
exactly one candidate and one policy; click 20 had one policy although 195
mission-derived targets were added. All production policy pools came from five
hand-written per-target files plus target-specific interpretation replies, not
the roughly 1,415-pattern library.

**Closure.** Replace “menu is supplied” at the conformance boundary with a
construction result:

> For pinned problem bytes `x`, pinned pattern-library snapshot `L`, and pinned
> construction parameters `k`, `constructPolicies L x k` returns either a
> typed construction failure or a finite family `F`; every member of `F` has a
> provenance path to patterns in `L`, every applicable library result admitted
> by the declared retrieval/slice rule is represented or has a typed exclusion,
> and `F` covers every open target with at least one initial action. The policy
> set is exactly the extensional image of `F`, never an independent parameter.

Coverage must not be stated merely as `F.nonempty`; initially require at least
one candidate action per open target, and require a nontrivial comparison when
the construction produces two extensionally distinct applicable cascades.
Negative controls: the incident's one hand-fed policy over a 195-target field;
a singleton returned while the slice contains two distinct admissible
cascades; a key whose pattern is absent from `L`; and a hand-written menu equal
in contents but lacking construction provenance.

Classification: **Lean + record**. Lean defines construction, soundness,
coverage, exact-image, and failing witnesses. The run record must carry hashes
of problem bytes and library snapshot, retrieval query and parameters, the
ranked slice including exclusions and scores, constructed edges/precedence,
all policy keys, and the construction artifact/version. The checker recomputes
the slice and family and compares exact sets, not only counts.

### 2. Pattern interpretations, guards, and transition kernels

**Free inputs.** `CascadeEFE.Model` accepts `initial`, `observation`, `guard`,
and `interpretation` as functions (`CascadeEFE.lean:58-62`). An interpretation
is any optional normalised kernel. `InterpretedPattern` accepts consumes,
produces, forbids, and any `theta ∈ [0,1]`
(`CascadeTransition.lean:22-31`); precedence is an arbitrary list passed to
`cascadeKernel` (`:102-116`). The more general rollout accepts arbitrary
transition `B`, observation `A`, and initial `q₀` with only distribution laws.

**Degenerate witnesses.** Every guard is false; every interpretation is the
identity kernel; `theta = 0`; consumes and produces are empty; precedence is
empty; or every action has the same constant kernel. All rows normalise. The
identity arm is explicitly the result when no pattern is enabled
(`CascadeTransition.lean:126-129`). Click 20 instead supplied two interpreted
patterns, but one was recorded as producing the mission want; the repeated
refusal did not alter the next G. This is evidence of an unupdated or
uninformative model, not of a malformed kernel.

**Closure.** Define interpretation as the output of a pinned semantic parse or
an observed-count estimator, with soundness tying consumes/produces/forbids to
the cited problem and pattern bytes. For each candidate, require either (a) an
enabled transition with positive probability of changing a state relevant to
a preferred outcome, (b) an explicit information-gathering transition whose
observation distributions differ, or (c) a typed `no-effective-transition`
result. A general theorem must not forbid legitimate identity transitions;
instead it must forbid presenting a family in which every candidate is
observationally and preference-equivalent while claiming a meaningful choice.
Negative controls use all-false guards, all-identity kernels, `theta = 0`, and
two differently named policies with identical kernels and observations.

Classification: **Lean + record**. Lean states relevance, distinguishability,
and typed degeneracy. The record carries interpreted token sets, theta source
or sufficient counts, kernel rows (or a canonical generator), problem/pattern
pins, and the before/after result that updates the estimator.

### 3. Preference C, wants, evidence, and Cτ schedule

**Free inputs.** `PreferenceSpec` takes the non-empty want set, evidence set,
positive `lam`, non-negative `mu`, and a proper ruled-zero set
(`TokenPreference.lean:27-41`). `MissionPreference.StatedWeighting` takes an
arbitrary normalized mass and support (`MissionPreference.lean:178-205`). The
cascade score takes an arbitrary schedule
`c : Fin T → ProbabilityKernel Unit O` and arbitrary horizon `T`
(`CascadeEFEPolicies.lean:20-27`). `machineHorizonEFE` similarly accepts `T > 0`,
a policy, and `C : ℕ → O → ℝ` (`RolloutAtMachine.lean:219-224`).

**Degenerate witnesses.** A one-token want unrelated to completion; uniform C;
all mass on `not-yet-evaluated` until the final step; `lam` arbitrarily close
to zero; empty evidence; or a schedule identical for every policy-relevant
outcome. Click 20 supplied exactly the cited last-step-only schedule: steps
1–3 put probability 1 on `ending/not-yet-evaluated`; step 4 used the class
distribution, and C expressed focus relation rather than completed work.

**Closure.** Construct C from a pinned task criterion and a closed outcome
vocabulary. Require a distinguished completion/progress outcome, strict
preference for completion over unchanged/refusal at every step where either is
reachable, and a schedule provenance theorem connecting each row to that same
criterion. Allow `not-yet-evaluated`, but not probability 1 at every
pre-terminal step when progress is observable. Negative controls are the click
20 schedule, a one-token unrelated want, uniform C, and `lam → 0` represented
by an agreed minimum separation rather than informal “positive”.

Classification: **Lean + record**. The record carries task bytes and pin,
criterion extraction with spans, outcome-token meanings, the full Cτ table,
and its construction version. The checker recomputes both want membership and
strict preference comparisons.

### 4. Observation model and target field

**Free inputs.** The observation kernel is a field of `CascadeEFE.Model`
(`CascadeEFE.lean:58-62`) and of the general forward model. Runtime policy
inputs independently supply the policy list and map each policy to its head
action (`ObservationAtMachine.lean:30-42`). The field target supplies arbitrary
`id` and `recordedUniverse : Option (Finset V)`
(`TargetUniverse.lean:67-83`). Nothing in these declarations derives the field
from open mission/ticket/excursion stores.

**Degenerate witnesses.** A constant observation row for every state/action;
a one-observation type; an empty recorded universe; a one-element target field;
or 600 open tasks with only one represented. All can satisfy normalisation.
The incident records roughly 637 open tasks, no excursions in the decision,
195 mission-derived targets added, three reaching scoring, one scored, and one
policy selected.

**Closure.** Define the target field as the exact result of enumerating pinned
open-store snapshots, with a bijection between open ids and initial task
actions. Define the observation model from a closed result vocabulary that
includes changed, already-satisfied, question, refused, invalid, and timeout;
require that at least two reachable result classes are distinguished whenever
the action contract permits them. Negative controls omit excursions, map 195
targets to one action, record every universe empty, and use a constant
observation kernel.

Classification: **Lean + record**. The record carries source snapshot hashes,
all enumerated ids and closure status, all candidate action ids, explicit
exclusions, observation vocabulary/version, and kernel sufficient statistics.
The checker recomputes exact field equality and row distinctions.

### 5. Belief, world/observation stream, and learned counts

**Free inputs.** `machineTrajectory` and `machineStep` take a forward model,
time-indexed `PolicyInputs`, world states, observations, time, and current
belief; rollout starts from any normalized supplied belief
(`RolloutAtMachine.lean:59-62,77-91`). The counts bridge accepts arbitrary
adjudication records and `classOf`; habit accepts arbitrary enactment records,
keys, verdicts, attempted patterns, and positive concentration alpha
(`EnactmentHabit.lean:102-127,218-226`).

**Degenerate witnesses.** A point belief chosen to force the preferred answer;
a permanently uniform belief; fabricated or empty counts; a constant
`classOf`; all records typed non-verdict; alpha so large that observations have
no practical effect; or a refusal omitted from the learning stream. Empty
habit history is explicitly uniform (`EnactmentHabit.lean:306-319`) and is not
itself invalid. The incident shows the live harmful case: click 19 and click 20
selected the same target/cascade after the same refusal, with unchanged G;
whether the learning ledger omitted or neutralised the refusal was not checked.

**Closure.** Make posterior belief and count state outputs of folds over a
pinned, duplicate-free event prefix. State an update theorem: a typed result
changes the sufficient statistic for its `(task-kind, action, seat)` cell, or a
typed proof explains why it is irrelevant; an unchanged refused
`(target, action, seat/method, input-digest)` cannot receive the same eligible
state on the next transition. Bound concentration relative to observed mass or
require a declared operator prior with a sensitivity test. Negative controls
drop the refusal event, duplicate it, use a constant classifier, and restore
the pre-event state.

Classification: **Lean + record**. The record carries ordered event ids,
deduplication keys, raw typed outcomes, before/after sufficient statistics,
input digests, alpha and estimator version. The checker replays the fold.

### 6. Precision, temperature, habit prior, and F

**Free inputs.** Posterior weights accept arbitrary positive habit values,
grade/G, F, temperature options, and policy list
(`PolicyPosteriorAtMachine.lean:252-272,313-325,741-756,869-880`).
`PolicyInputs` exposes precisely habit, grade, F, temperature options,
policies, and head (`ObservationAtMachine.lean:30-42`). The counted habit's
alpha is supplied (`EnactmentHabit.lean:216-226`). Channel precision accepts a
positive epsilon floor and model/observation stream
(`ChannelPrecisionAtMachine.lean:72-90`).

**Degenerate witnesses.** Constant habit; habit overwhelmingly favouring the
only policy; arbitrarily high precision; temperature approaching zero;
constant G; F omitted or constant; or a singleton carrier. All posterior
normalisation theorems still hold. Click 20 had F `not-supplied` /
`class-model-unconditioned-at-selection`; with one policy, any finite G,
temperature, or habit yields posterior one.

**Closure.** These parameters may remain operator/model inputs only after the
carrier closure. Add sensitivity obligations over a non-singleton fixture:
lower G at equal habit/F increases mass; changed learned counts change habit;
finite F changes mass when enabled; precision/temperature remain inside named
bounds; and no report claims a comparative selection when the carrier has one
extensional policy. Negative controls use singleton support, constant G/F,
zero-information counts, extreme temperature, and identical posterior under a
recorded outcome update.

Classification: arithmetic bounds are **Lean alone**; correspondence is
**record**. Carry every per-policy G/F/habit term before normalization,
temperature/precision inputs and derivation, posterior mass, carrier
cardinality, and extensional-equivalence classes. Recompute the posterior.

### 7. Horizon and action projection

**Free inputs.** Rollout receives arbitrary positive horizon `T`, policy plan
`PolicyIndex → ℕ → U`, and the head map used to marginalise policies to
actions (`RolloutAtMachine.lean:77-91,219-224`; `ActionAtMachine.lean:65-98`).
Ties are resolved by sorted first maximum (`ActionAtMachine.lean:71-83`).

**Degenerate witnesses.** `T = 1` despite effects requiring four steps; every
policy maps to the same head action; a plan becomes identity after step zero;
or policy ids differ but their entire plans are equal. These values are legal,
and a normalized policy posterior then provides no action choice.

**Closure.** Derive the horizon from a declared stopping rule or prove it
covers the first discriminating/reward-bearing step for every compared policy.
Quotient or report policies by extensional plan equivalence before claiming
choice diversity. Require at least two distinct action traces when a report
says policies were compared. Negative controls use equal heads/equal plans and
a horizon ending before click 20's only non-placeholder preference row.

Classification: **Lean + record**. Carry full bounded action traces, stopping
rule, first discriminating step, chosen horizon, and equivalence classes.

### 8. Certificates and external rulings

**Free inputs.** `FullAttestation` accepts both declared universes, their state
lists, selection/enaction value, record-family entries, and negative scope
(`CertificateStates.lean:82-108`). `QualifyingRuling` accepts reading,
ruled-out ids, and any non-empty bytes pin (`:157-169`). Reading three does not
prove that its ruled-out list is legitimate (`:147-153`).

**Degenerate witness.** Declare no nodes and no connections, provide the seven
record-family tags as typed gaps, choose reading one, and use a one-character
pin. Census completeness holds while nothing positive is established. Or use
reading three and rule out every non-positive node.

**Closure.** Define declared node/connection universes as exact outputs of a
pinned contract manifest; require the ruling pin to hash canonical ruling bytes
whose decoded reading and exclusions equal the supplied value; bound reading
three exclusions to ids explicitly authorized by that ruling; and never use
reading one as evidence that the machine worked. Negative controls are empty
self-declaration, all typed gaps, bogus non-empty pin, and rule-out-all.

Classification: finite exactness and exclusion constraints are **Lean**;
manifest/ruling authenticity is **record**. Carry canonical bytes, cryptographic
hashes, manifest version, and checker result.

## Ordered small handoffs

Each handoff adds one construction or obligation and one negative control. No
handoff runs a click.

1. **Policy construction type.** Replace free `menu` at the conformant boundary
   with `constructPolicies library problem params`; prove exact-image and
   library-membership. Negative: a hand-fed singleton cannot inhabit the
   result for the incident fixture.
2. **Policy coverage.** State one initial action per open target and
   nontrivial-extension coverage for a slice with two admissible cascades.
   Negative: 195 targets / one policy.
3. **Policy conformance record.** Specify the pinned bytes and recomputation
   receipt; verify exact policies, edges, precedence, and exclusions.
4. **Field construction.** Derive the field from all three open stores and
   prove id equality. Negative: omitted excursions and a one-element field.
5. **Transition adequacy.** Add effective-or-informative-or-typed-degenerate.
   Negative: all-false guards and all-identity/constant kernels.
6. **Preference construction.** Derive C/Cτ from the pinned criterion and require
   strict progress/completion ordering. Negative: click 20's placeholder-first
   schedule.
7. **Observation and result vocabulary.** Bind the closed agent-result sum to
   distinguishable observation rows. Negative: constant observation.
8. **Belief/count replay.** Fold the pinned event prefix and prove unchanged
   refusal cannot return unchanged eligibility. Negative: clicks 19→20.
9. **Horizon/action diversity.** Derive the horizon from the first
   discriminating step and report extensional plan classes. Negative: equal
   plans or truncation before the only meaningful C row.
10. **Posterior sensitivity and parameter bounds.** Exercise G, F, habit,
    precision, and temperature on a non-singleton carrier. Negative: a
    normalized but comparison-free singleton posterior.
11. **Certificate authority.** Bind manifest and ruling bytes to their hashes
    and forbid empty self-declared universes/all-excluded reading three.
12. **End-to-end trace conformance.** Recompute items 1–11 from one hermetic run
    record and compare every intermediate value, before any click is proposed.

## What Lean cannot make robust

Lean can reject a degenerate value only after the intended construction or
non-degeneracy property is stated in Lean and the runtime value is connected to
it. Lean alone cannot establish:

- that a serialized menu was actually computed from the live pattern library;
- that the recorded library, task, mission text, chat, or ruling bytes are the
  bytes the runtime read;
- that retrieval found every semantically applicable prose pattern;
- that an LLM interpretation is true, useful, or faithful to Joe's meaning;
- that a declared want or preference captures what Joe presently wants;
- that the world, observation, author reply, review, or commit was reported
  honestly;
- that the runtime called the proved construction rather than another branch;
- that source code loaded in a JVM matches the checked source; or
- that a future event outside the closed model cannot occur.

Those require pinned inputs, independently recomputable run records, adapter
tests against real parsers/stores, loaded-code identity, and honest external
participants. A proof over a caller-supplied value proves only a conditional.
The conformance record is what must establish the antecedent; otherwise the
same backdoor remains under a stronger theorem.
