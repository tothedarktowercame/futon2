# Pending operator decisions

Current as of 2026-08-31. This is a pointer-only digest, not a second decision
brief. Questions, recommendations, evidence, and option analysis remain
authoritative in the linked source records.

**Operator boundary, not a pending decision:** cancelling Joe's cohort-46 run
is recorded and counted, but starts a new semantic stratum because
`:cancelled` was not preregistered. Do not pool it with cohort 46's original
outcome classes or amend the preregistration retrospectively. Full consequence:
[`C206-cohort-cancellation-boundary.md`](../labs/wm-contract/C206-cohort-cancellation-boundary.md).

## 1. Strategic carrier dependency

### Strategic outcome vocabulary

- **Question:** Is `StrategicOutcome` a mission-state transition, a delivery
  disposition, a cohort outcome label, or an explicitly defined composition of
  those carriers, and what independent rule makes one “useful”?
- **Unblocks:** `StrategicOutcome` → strategic predictive-outcome and policy-prior
  kernels → canonical `G_S` / `E_S` → strategic mission selection.
- **Care:** ranking/model authority; not an armed safety gate.
- **If deferred:** nothing breaks; the strategic carrier family remains unbuilt
  and the live `central + strategic + doable` blend remains an explicitly
  noncanonical engineering baseline.
- **Full analysis:**
  [`C166-strategic-outcome-stop.md`](../labs/wm-contract/C166-strategic-outcome-stop.md).

## 2. Outward-effect authority

### Authored R16 outward-act binding

- **Question:** Which one bounded external operation, authority/arming rule,
  independently owned read-back query, and next-belief observation binding is
  approved as R16's first outward act?
- **Unblocks:** C78 → outward actuator and independent effect witness → the
  currently construction-only R16 boundary.
- **Care:** **safety/authority relevant**; this grants the machine a bounded
  outward effect and therefore requires explicit arming and an independent
  observer.
- **If deferred:** nothing breaks; R16 remains honestly refused as outward
  actuation, while its typed construction result continues to operate.
- **Full analysis:**
  [`C78-outward-act-refusal.md`](../labs/wm-contract/C78-outward-act-refusal.md).

## 3. Hard-guard authority

### Avoided-range diagnostics as action guards

- **Question:** Should any named R5 avoided-range diagnostic become a hard
  action guard, and for each authorized channel must `:unknown` force
  abstention?
- **Unblocks:** a separately armed avoidance-guard proposal; it does not block
  the already-correct tri-state diagnostic.
- **Care:** **safety/authority relevant**; this creates a veto over action where
  none exists today.
- **If deferred:** nothing breaks; avoidance remains informational,
  `:unknown` stays visible, and no new guard is armed.
- **Full analysis:**
  [`C113-avoidance-unknown-safety-design.md`](../labs/wm-contract/C113-avoidance-unknown-safety-design.md).

## 4. Observation, belief, and ranking migrations

These seven decisions are ordered by the boundary they unblock. All seven keep
the corresponding absence-coercion site honestly red until decided; deferral
does not create a new failure, but the legacy coercion remains live.

**Evidence status (C195): these are judgement calls, not decisions awaiting a
future measurement delivery.** Directional immediate-boundary evidence exists
for belief aggregation and missing sorry pressure only. The other five have no
option-effect measurement. C191 established that downstream propagation would
require five coherent persistence additions plus a replay harness and would
still not establish production-selected-action divergence. That replay is not
being built. Full terminal boundary:
[`C195-c130-evidence-terminal.md`](../labs/wm-contract/C195-c130-evidence-terminal.md).

### Prediction triple

- **Question:** When observation, predicted mean, or variance is missing, does
  the producer omit the channel with a typed reason or refuse the whole update?
- **Unblocks:** typed prediction records and removal of the coercions at
  `free_energy.clj:98-100`.
- **Care:** belief/ranking relevant; malformed model output must remain loud.
- **Evidence:** no option-effect measurement; judgement call.
- **If deferred:** current zero-coercing belief update remains unchanged and the
  lint finding remains red.
- **Full analysis:**
  [`C130-absence-decisions.md` §2](../labs/wm-contract/C130-absence-decisions.md).

### Belief aggregation

- **Question:** Does aggregation omit honestly absent channels while rejecting
  malformed entries, or refuse every incomplete collection?
- **Unblocks:** support-preserving aggregation and removal of the coercions at
  `belief.clj:1040-1052`.
- **Care:** belief/ranking relevant.
- **Evidence:** directional only: two serially dependent diagnostic records;
  omitting the absent channel changed the immediate driver twice without a sign
  change. Downstream ranking is unmeasured.
- **If deferred:** current absent/malformed/measured-zero collapse remains and
  the lint finding remains red.
- **Full analysis:**
  [`C130-absence-decisions.md` §8](../labs/wm-contract/C130-absence-decisions.md).

### Strategic-mode inference

- **Question:** With a required feature absent, does mode inference emit
  reason-bearing `:unknown` or use an explicitly specified partial/prior rule?
- **Unblocks:** the tagged-envelope migration at `free_energy.clj:138-143`.
- **Care:** **safety and ranking relevant**; a fallback can select while blind.
- **Evidence:** no option-effect measurement; judgement call.
- **If deferred:** missing features continue to enter mode inference as zero and
  the lint finding remains red.
- **Full analysis:**
  [`C130-absence-decisions.md` §3](../labs/wm-contract/C130-absence-decisions.md).

### Missing sorry pressure

- **Question:** Does the fallback selector abstain and return control when sorry
  pressure is unknown, or continue only through branches that do not inspect
  that signal?
- **Unblocks:** status-consuming policy selection at `policy.clj:144-145`.
- **Care:** **safety and action-selection relevant**; the current zero can act
  while blind to a declared priority.
- **Evidence:** directional conditional evaluation only: over two serially
  dependent diagnostic records, A abstained and B selected. The trace does not
  establish that the fallback was invoked in either tick.
- **If deferred:** unknown pressure continues to look low and the lint finding
  remains red.
- **Full analysis:**
  [`C130-absence-decisions.md` §4](../labs/wm-contract/C130-absence-decisions.md).

### Validated rollout-step producer

- **Question:** Must every proposed move be a validated `:scored` or
  reason-bearing `:unscored` variant, or may producers remain partial maps with
  numeric fallbacks?
- **Unblocks:** a typed rollout producer and removal of the coercion at
  `rollout.clj:129`.
- **Care:** ranking-population relevant.
- **Evidence:** no option-effect measurement; judgement call.
- **If deferred:** moves with fabricated zero scores may still enter ranking and
  the lint finding remains red.
- **Full analysis:**
  [`C130-absence-decisions.md` §5](../labs/wm-contract/C130-absence-decisions.md).

### Unscored rollout moves

- **Question:** Once `:unscored` is explicit, should rollout exclude that move
  and continue or refuse the rollout?
- **Unblocks:** the refusal/exclusion variant at `rollout.clj:158`.
- **Care:** **safety and selection relevant** wherever rollout authorizes an
  action.
- **Evidence:** no option-effect measurement; judgement call.
- **If deferred:** missing delta/absolute scores continue to become zero cost
  and the lint finding remains red.
- **Full analysis:**
  [`C130-absence-decisions.md` §6](../labs/wm-contract/C130-absence-decisions.md).

### Fulab temperature without prediction error

- **Question:** Without prediction error, should fulab compute temperature from
  uncertainty alone or refuse to sample?
- **Unblocks:** the no-error measurement variant at
  `adapters/fulab.clj:81`.
- **Care:** sampling/ranking relevant; uncertainty-only temperature would be a
  new model.
- **Evidence:** no option-effect measurement; judgement call.
- **If deferred:** missing error remains indistinguishable from perfect
  prediction and the lint finding remains red.
- **Full analysis:**
  [`C130-absence-decisions.md` §7](../labs/wm-contract/C130-absence-decisions.md).

## Census boundary

### Support-typed shadow as the live selection authority

- **Question:** Should the support-typed scoring path remain shadow-only or
  replace legacy comparison at the selection boundary?
- **Unblocks:** live refusal of unequal-support comparisons and retirement of
  legacy support-blind ranking.
- **Care:** ranking/selection relevant; it changes which candidates are
  comparable.
- **If deferred:** nothing breaks; live selection retains legacy semantics and
  the support-typed result continues to be emitted as shadow evidence.
- **Full analysis:**
  [`C108-support-typed-scoring-shadow.md`](../labs/wm-contract/C108-support-typed-scoring-shadow.md).

**Pending now: 11 decisions** — one strategic carrier, one outward binding, one
hard-guard authority question, seven observation/belief/ranking migrations,
and the now-evidence-bearing support-typed shadow switch.

The following were checked and are not counted:

- C130 item 1, the public numeric-vector representation, was decided and its
  unused legacy projection retired by C133/C136.
- C108's support-typed shadow-to-live switch was excluded at C169 because it
  required post-v18 evidence. C167 has now produced two shadow-bearing records;
  C171 therefore moves it into the pending population without asserting that
  two diagnostic ticks are an adequate empirical denominator.
- Older `BUILD-PLAN-0831.md` / `BUILD-status.md` decision lists are historical
  snapshots containing calls since settled or reframed; this digest takes only
  decisions whose current source record still states an unresolved owner call.

Deferral is therefore not equivalent to failure or obligation. Each source
continues to state its honest red/refused/baseline condition until the named
authority decides it.
