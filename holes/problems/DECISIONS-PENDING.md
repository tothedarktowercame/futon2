# Pending operator decisions

Current as of 2026-08-31. This is a pointer-only digest, not a second decision
brief. Questions, recommendations, evidence, and option analysis remain
authoritative in the linked source records.

**Operator boundary, not a pending decision:** cancelling Joe's cohort-46 run
starts a new semantic stratum, because `:cancelled` was not preregistered. Do
not pool it with cohort 46's original outcome classes or amend the
preregistration retrospectively. Full consequence:
[`C206-cohort-cancellation-boundary.md`](../labs/wm-contract/C206-cohort-cancellation-boundary.md).

**Updated by C211 (futon2 `9a1d432`):** cancellation is now *recorded but not
counted*. It no longer consumes cohort 46's attempt denominator or stopping
window; the cancelled attempt is retained under `:post-preregistration/cancelled`
with its own identity, and the next eligible attempt takes a distinct ordinal.
The corpus held 62 attempt directories, 58 closed, **0 cancelled**, so no
published cohort figure required correction. Cancelling a run is therefore safe
for the operator; this paragraph previously read "recorded and counted".

**Wider index:** this file is the operator brief for the fourteen decisions
below. Decisions already *settled* — workspace protocol, campaign method,
declared evidence boundaries — and open operating questions such as check
cadence are indexed in [`DECISIONS-REGISTER.md`](DECISIONS-REGISTER.md), which
points back here rather than restating these.

<!-- CURRENT DECISIONS START -->

**Ordering (C295, 2026-09-01).** Four dependencies make one order materially
better than another:

1. **Cascade semantics** before or with strategic carrier design.
2. **Typed rollout producer** before unscored-move policy.
3. **Prediction-triple behaviour** before belief aggregation.
4. **Morning Brief epoch boundary before Joe's operator run.**

**The fourth one gates the run.** C209 recommended that `:live-pending` begin at
Joe's next operator-triggered run — so **the run is itself the boundary event**.
Performing it before the decision sets the boundary implicitly rather than
deliberately, and the 72 historical items would be fixed as historical by
accident. This corrects my earlier statement that the run was gated only on
quiescence and the reload.


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
- **Scope narrowed by C226 (futon2 `b91f706`):** the value Fulab actually
  receives is not canonical signed ε. It computes
  `max(0, text-score(outcome) - 1)` internally — a nonnegative outcome-size
  surplus — and has **no connection to `free-energy/compute-prediction-error`**.
  Only `compute-tau` reads the key; the generic Fulab branch is its sole
  writer. Further, no static call site in `src/`, `test/` or `checks/`
  constructs the adapter or calls `fulab/new-adapter`, so this decision governs
  a path with no observed live caller. That does not make it moot — the untyped
  seam would silently clamp signed ε if connected later — but it is a smaller
  decision than its placement here suggests.
- **Full analysis:**
  [`C130-absence-decisions.md` §7](../labs/wm-contract/C130-absence-decisions.md).

## Census boundary

### Historical versus live Morning Brief QA

- **Question:** Should all 72 existing Morning Brief items be classified
  `:historical-unanswered`, with `:live-pending` beginning at Joe's next
  operator-triggered run (recommended), or should a subset remain due for
  retrospective QA?
- **Unblocks:** a bounded `DECISION-DUE` population and an explicit disposition
  carrier; no item is dispositioned until Joe chooses.
- **Care:** belief/evidence relevant. Only 15 historical items carry the entity
  target needed to emit independent substantive-achievement evidence.
- **If the recommended boundary is chosen:** the machine permanently receives
  no independent achievement evidence for 21 historical grounded changes, but
  no stale July verdict is injected as current evidence.
- **If retrospective QA is chosen:** the belief carrier first needs an
  occurrence time and historical-evidence policy; today a late answer appears
  to the next update as newly current evidence.
- **Full analysis:**
  [`C209-morning-brief-disposition.md`](../labs/wm-contract/C209-morning-brief-disposition.md).

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

**Pending now: 12 decisions** — one strategic carrier, one outward binding, one
hard-guard authority question, seven observation/belief/ranking migrations,
one Morning Brief disposition boundary, and the now-evidence-bearing
support-typed shadow switch.

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
- **Re-scoped by C295 (2026-09-01):** this now has **two sequentially dependent
  records and directional evidence**; it is no longer waiting for first
  evidence, so "awaiting measurement" no longer describes it.

## Durability

### Invoke-jobs ledger storage backend

- **Question:** Should the invoke-jobs ledger keep its single-EDN-map format
  with atomic snapshot replacement, or move to SQLite WAL with a compatible EDN
  export?
- **Unblocks:** scalable ledger growth beyond 134.6 MB / 6,195 jobs; the
  monolithic full-map rewrite on every mutation.
- **Care:** durability and atomicity semantics; anything reading the ledger file
  directly.
- **Evidence (C251, futon3c `fc03fbba`):** the current writer `spit`s the whole
  file in place — no temp file, rename, `fsync`, checksum, backup or writer
  lock. Concurrent writers can persist out of order, persistence errors are
  printed but not returned to callers, and **a torn file is silently replaced in
  memory by an empty default ledger on restart.**
- **Not waiting on this decision:** the atomic-write repair (C254) preserves the
  current format and is being applied regardless, because silent total loss is
  not a thing to defer.
- **Re-scoped by C295 (2026-09-01):** C254 and C263 have since repaired
  atomicity, schema validation, locking, and memory/disk consistency. **The
  remaining decision is scaling — EDN full-map rewrites versus SQLite — not
  acute durability.** This entry was written while the durability hole was
  open; it is no longer urgent.
- **If deferred:** the format stays EDN and every mutation continues to rewrite
  the whole file; cost grows with history.
- **Full analysis:** `futon3c` C251 discovery commit `fc03fbba`.

## Model semantics

### Cascade carrier: how meets are established

- **Question:** Should the cascade carrier gain (a) an explicit meet
  operation with a witness for each relevant node pair, or (b) a declared
  order derived from co-application plus a proof that greatest lower bounds
  exist?
- **Unblocks:** the last two glossary terms — pattern language / cascade, and
  policy `π`, which aliases the cascade carrier. Glossary coverage is stuck at
  **31/33** until this is decided.
- **Care:** this is a modelling decision about cascade semantics, not a
  refactor. A lane should not invent it.
- **Evidence (C288, C291):** the glossary requires a semilattice with distinct
  sequential `BV.seq` and cross-cutting `BV.copar` composition. Lean's
  `Cascade` has one acyclic edge relation and a precedence list, and
  `CascadeOrder.lean` states it models no overlap, wiring or fold behaviour.
  C291 built the serialized producer establishing both relations with shared
  node identity, then **stopped**: applying `CascadeOrder.hasMeets` to
  symmetric weighted adjacency **would invent semantics**. A partial carrier
  was forbidden, so nothing was added.
- **If deferred:** 31/33 stands as an honest ceiling. The seven notation
  hazards are enumerated and structurally closed (C288); the remaining gap is
  semantic under-modelling, not name conflation, and nothing is being reported
  as bound that is not.
- **Full analysis:**
  [`C291-cascade-carrier-repair-blocked.md`](../labs/wm-contract/C291-cascade-carrier-repair-blocked.md).

<!-- CURRENT DECISIONS END -->
