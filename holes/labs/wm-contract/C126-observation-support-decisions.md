# C126 — observation-support decision register

Date: 2026-08-31. Status: decisions named, none taken.

This register covers the six blocked observation-support **rows**. They are not
six remaining decisions: C98 already settles three scoring rows. The honest
population is three unresolved decisions plus three implementation migrations.

## 1. Numeric vector projection — `observation.clj:152`

**Today.** `sense->vector` substitutes `0.0` for a missing channel, so a vector
consumer cannot distinguish absent from measured zero.

**Decision.** What is the public vector boundary?

- (a) return values plus a same-order support mask/reason vector;
- (b) return a vector of tagged channel readings;
- (c) refuse vectorisation unless every channel is observed;
- (d) retain the numeric vector only as an explicitly named legacy projection,
  with a separate mandatory envelope argument for new consumers.

**Behaviour changed.** (a)/(b) require downstream consumers to handle partial
support; (c) makes incomplete scans unable to reach vector consumers; (d)
preserves old callers while permitting them to remain blind until migrated.
C98 says absent is not zero, but does not choose the representation. This is an
API/compatibility decision, not itself an action-safety decision.

## 2. Channel-gap nil handling — `free_energy.clj:23`

**Today.** `channel-gap` turns nil into `0.0`, then computes distance from the
preferred range.

**Decision status: settled by C98.** There is no remaining semantic choice:
an absent observation contributes no score term and the score carries support.
The implementation must stop passing absence as a number and return/consume a
tagged absent term. Refusing the entire score is reserved for comparison at the
selection boundary when supports differ. This row is implementation-only and
is not independently safety-relevant.

## 3. Pragmatic per-channel read — `free_energy.clj:50`

**Today.** `compute-controller-diagnostics` supplies `0.0` for every absent
preferred channel before calling `channel-gap`; the persisted per-channel row
therefore claims a fabricated value, gap, and in-range verdict.

**Decision status: settled by C98.** Per-channel diagnostics must carry
observed/absent status; only observed gaps enter the supported score. This is
the caller half of row 2, not a second policy choice. Implementing it changes
diagnostics and the shadow/live score only when the separately staged C98
switch is authorised. It is not independently safety-relevant.

## 4. Epistemic diagnostic — `free_energy.clj:61-63`

**Today.** Missing loop-health, attack-coverage, or support-coverage becomes
zero; `1 - 0` therefore manufactures maximum uncertainty pressure.

**Decision status: settled by C98.** An absent channel supplies no evidence and
contributes no channel term. The diagnostic must carry its support and omit
absent terms; it must not count-normalise unlike channels. This is an
implementation behind the deliberately unswitched scoring migration, not a
new decision. It is not independently safety-relevant.

## 5. Strategic mode inference — `free_energy.clj:138-143`

**Today.** Six missing inputs become zero. Depending on which are missing, the
classifier can fabricate `:dark`, suppress `:depositing`, or satisfy parts of
the `:hermit` predicate.

**Decision.** What does mode inference do when any required feature is absent?

- (a) refuse inference and emit an explicit unknown mode with reasons;
- (b) classify from the observed subset using separately specified partial
  rules and report their support;
- (c) return the prior/highest-prior mode, explicitly marked prior-only;
- (d) retain the last fully observed mode, marked stale with age and missing
  channels.

**Behaviour changed.** (a) removes mode-conditioned output for incomplete
ticks; (b) may select a mode from less evidence; (c) makes prior preference
operational; (d) introduces temporal persistence and staleness. C98 does not
settle classification. This becomes safety-relevant wherever mode controls an
override or actuation path, so choosing the fallback is Joe's decision.

## 6. Fallback sorry-pressure selection — `policy.clj:144-145`

**Today.** Missing `:sorry-count-norm` becomes zero. The fallback therefore
behaves as if sorry pressure were measured low: it can prefer a learn action,
then an address-sorry action, then no-op according to available candidates.

**Decision.** When the fallback selector lacks sorry pressure, does it:

- (a) abstain with a reason-bearing unknown-input verdict;
- (b) continue through the non-pressure branches while explicitly recording
  that the high-pressure branch was not evaluated;
- (c) permit only no-op;
- (d) refuse the fallback and return control to a named upstream selector?

**Behaviour changed.** (a)/(c) can stall work; (b) can act while blind to a
priority signal; (d) changes failure routing and may leave no selector. C98
does not settle fallback action authority. This directly changes action
selection and is safety-relevant; it is Joe's decision.

## Other three families (five sites)

### Prediction measurement — 2 sites

`free_energy.clj:98-100` and `adapters/fulab.clj:81` are only partly settled by
C120. C120 distinguishes legacy and malformed downstream precision records; it
does not decide adapter behaviour when no prediction error exists. Missing
observed/mean/variance should become absent/malformed rather than zero (an
implementation of the producer contract), but whether fulab computes tau from
uncertainty alone or refuses without error remains a behavioural decision. No
discharge occurs here.

### Rollout move score — 2 sites

`rollout.clj:129` and `:158` remain one unresolved decision: does an unscored
move leave rollout support, or does its presence refuse the rollout? A typed
`:unscored` variant is implementation; exclude-versus-refuse changes search
and is a behavioural decision. No discharge occurs here.

### Belief aggregation validity — 1 site

`belief.clj:1040-1052` is substantially settled by C120 plus C98: malformed
prediction-error records may not be aggregated, while genuinely absent
channels contribute no evidence and present channels retain their precision.
The remaining work is a validated collection boundary and reason-bearing
refusal for malformed input. This is implementation-only unless an operator
wants partial malformed collections salvaged, which no standing precept
authorises. No discharge occurs here.

## Gate state

No source or behaviour changed. Canonical lint remains **11**, and
`checks/absence-coercion-dispositions.edn` still exactly covers the 18-row C12
population as `{:fix-now 6, :exempt-with-reason 1, :blocked 11}`. The current
full gates remain green: futon2 1,037 tests / 6,203 assertions and futon3 248
tests / 1,518 assertions.
