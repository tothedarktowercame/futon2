# PROOF-2a ⟨1⟩2 — Joe's items 1–5

Author: codex-2, 2026-09-27. Implementation on Joe's direct instruction;
independent review remains with claude-8. Item 6 (BMR) was explicitly deferred.
This report does not accept ⟨1⟩2 as a whole.

**Item 5 correction delivered at `2d2a8fd04` (2026-09-27):** stale-predecessor publication now records an accumulation absence and continues the run. The disputed state is omitted; returned and persisted receipts use the published outcome. This supersedes the throwing behavior described in the original delivery below. Independent review remains outstanding.

## Changes and remaining work

| Item | Implementation | Limit |
|---|---|---|
| 1. Three Lean/runtime correspondences | mathlib4 `8d98c63f97`: `Proof2.ChannelIdentity` selects current check name AND source identity; `Proof2.Revision3Rates` represents both-cell minimum 5, authorised Jeffreys estimate and its kernel construction; `Proof2.AccumulationAtRuntime` proves the state-taking outer-product recurrence, concatenation/carry law and positive-prior preservation. futon2 `202b5a026` records the bindings. | These are mathematical correspondences on admitted inputs, not a claim that Lean executed Clojure or that temporal filtering is wired. Raw-count results remain the unsmoothed case. Store admission and uniqueness precede the identity projection. |
| 2. Likelihood precision | `202b5a026`: omission selects ζ=1; explicit nil returns invalid-zeta and no certificate. Registry explicitly says beta-zeta learning is absent and precision is fixed. | Existing support/endpoint rulings unchanged. No beta-learning law invented. |
| 3. Tick-side exact filtering | **Completed at implementation level; no live claim.** Input join `464e47301`; compute/publication `2df5efcf5` + `18009f1d2`; next-selection posterior consumption `bd7d182dc`; closure controls `a3306e2ae`; scoped registry declaration `90af94845`. Reviewed/closed by claude-8, 2026-09-27. | One executed C3 step per update. Undeclared or inconsistent model/domain inputs initialize with a recorded reason; initialization is not filtering. No live witness since increment 2; wiring-map declarations remain with ⟨2⟩2b. |
| 4. Arena absence through readers | `443a24b0f`, `3b76aa8a5`: missing A cells/models refuse, zero evidence has no posterior, refusal survives subsequent entity updates and carry, health/population predictions and trace. Prediction-error receipts preserve the cause. | Arena remains the declared tempered seven-status model. This does not connect counted token A to that carrier or claim exact temporal filtering. |
| 5. Accumulation continuity and forwarding | `fff29cba1`: preserve initialization, bind entity/model lineage, require actually observed coordinates, reject changed predecessor while holding the existing append lock; configuration failures become receipts. Default flight judge forwards configuration and trace directory; its existing post-construction publisher remains the sole publisher. | No live seeded restart/migration or flight executed. Old tails without origin/lineage, or receipt-only tails, cannot silently initialize or skip. A failed publication is described explicitly below. |

## Item 3: implementation closure, 2026-09-27

Claude-8 reports implementation closure at the revisions in row 3. The
enactment record is the published posterior authority: the click trace and run
record precede enactment. The next selection consumes the verified published
posterior as q0. Linkage uses click-id plus a `:consumed-at` citation;
occurrence-id retains its selection meaning. An execution witness admits the
transition; either a true or false stamped C3 check supplies the observation.
`:success` retains its enactment/E/W_c meaning.

The committed closure controls at `a3306e2ae` cover three chronological
selections with non-identity B and policy switches, previous-action and fresh-
observation interventions, unchanged consumed posterior when only the next
candidate changes, event replay/stale cursors, domain/model mismatches,
unchecked-token marginalization, and retained contradictions. The registry at
`90af94845` records `:realisation-scope :executed-c3-temporal-filter` and
`:claim :hermetic-implementation`, with
`:witness {:absent :no-live-flight-since-increment-2}`.

Evidence references: temporal-consume `8ebc2de3`, temporal-update `33fe16f1`,
temporal-input `b2aeb5e3` (closure registry lists these with test pin
`a3306e2ae`); full-loop-runner `924964b8` is reported by claude-8 at
`bd7d182dc`. This documentation update checked the committed registry and
the closure report; it did not rerun tests or independently revalidate warrants.

Limits: one executed C3 step per update; other checker laws and multiple
attempts are not covered. Missing or inconsistent declarations initialize
with typed reasons and do not establish temporal continuity. New admissions
are covered by `3145da84b`; older interpretations are not retroactively
declared. A retained contradiction does not publish a posterior or advance
the cursor; a following selection may explicitly initialize, which is not
counted as filtering. The Lean binding covers the exact kernel and ordering,
not a new equivalence proof for all machineStep premises. Temporal map
declarations and a live-flight witness remain separate obligations.

This closure supersedes the implementation-status claims in the original
producer analysis and the later item-3 proposal below; their requirements
remain as the historical rationale, not outstanding implementation dispatches.

## Item 3: original unresolved-producer analysis (superseded)

`token_belief_carry/stage` explicitly stages initialization. Its v2 carry has
no posterior. `token_belief_predecessor/input-receipt` v3 authorizes signed
observations as **next-selection initialization**, and the observation authority
requires `:consumption :not-authorized` for the temporal claim. The legacy path
records `:conditioning-consumption-not-wired` when task execution is admitted.
`token_initialization_policy/apply-observations` modifies fresh fact initialization;
it does not predict the previous posterior through the previous enacted action.

The arena's micro-loop uses several synthetic events derived from one scan. Those
are not fresh observations at successive prediction times. Its seven statuses
also differ from the token-set carrier of the exact kernel. The existing
flight conditioning is per-policy and is not already a single tick trajectory.

The structural work still needed is one producer/consumer contract binding:

1. A persisted previous posterior and its occurrence, domain and model identity.
2. The independently witnessed previous enacted action, with its transition
   model (selection or a constructed plan is insufficient).
3. A newly observed outcome with occurrence/revision and a consumed-event identity,
   so neither the initial observation nor another tick's evidence is reused.
4. Prediction and conditioning through the existing exact core, followed by
   consumption of that posterior by the next selection and trace.

Tests must distinguish a non-identity previous action from the current candidate,
reject duplicate/stale events and changed model/domain, and retain impossible
observations without fresh initialization. There is no missing arithmetic
primitive; the missing work is the witnessed temporal join. The current
initialization authority must not be promoted by changing its status label.
This is an unresolved implementation requirement, not an optional upgrade and
not a request to waive it by agreement.

## Why each refusal exists, and what it stops

| Case | Mathematical or bookkeeping necessity | Effect |
|---|---|---|
| Explicit nil ζ | A numerical default would contradict a certificate retaining nil. | Scorer returns missing/invalid-zeta and no certificate, as existing score refusals do. |
| Missing/invalid A or B, invalid prior/weight, unknown categorical observation | Missing model cells cannot be asserted zero; malformed quantities do not define the declared filter. | Typed entity refusal; no posterior number. No new exception in the filter. |
| Zero predictive evidence | Normalization is undefined. Uniform is a new prior, not that posterior. | Refusal retained through carry, prediction/error receipts and trace. |
| Missing observation envelope or absent coordinate | The existing observation vector can contain a numeric zero for a missing source. Counting that as observed zero fabricates a trial. | Accumulation absence receipt; selection unchanged. No partial-trial policy is invented. |
| Missing origin, model revision, changed entity/revision or tick identity | A recurrence must update the same model from its declared origin, not mix entities or invent a restart. | Accumulation absence receipt; selection unchanged. |
| Receipt-only or pre-accumulation tail | Skipping the actual predecessor or fresh initialization would break the recorded chain. | Existing migration-required absence retained; no live migration performed. |
| Unreadable configuration | Learning configuration failure does not invalidate selection. | Absence receipt forwarded to the decision rather than configuration-reader exception. |
| Predecessor changed before append / unreadable authoritative history | Two writers must not both publish the same prior as a serial chain. | **Publication throws before append.** Scheduled/flight callers can consequently report run/trace failure. This is the expected-predecessor rejection option from item 5, not merely a passive receipt. No claim that selection continues successfully after this publication failure. |

The publication check is inside `lane_futility/append-indexed-trace!`'s existing
cross-process lock, before updating either trace or index. It does not nest locks.
The test uses two actual disk readers before the first write; the second
publication refuses and the corpus contains only the first successor.

## Evidence

Scoped builds of all three new Lean modules succeeded. Axiom output for the rate
bounds/kernel correspondence and accumulation declarations contains no sorryAx;
existing imported Holes warnings are not described as a clean whole-repository
proof audit.

Registered Lean warrants (all `warrant? true`, postcheck matched):

- ChannelIdentity: `test-registry-1d0fb875d452abee5193ed3d24df6a9a26cacefc15ec615253379888c5a3609a`
- Revision3Rates: `test-registry-04732321ecbea5b690ad6cb1a443397765c8d8b9034a2d394e5c4bf0e24cb919`
- AccumulationAtRuntime: `test-registry-cbf55bdb527c2979bef6143f1caf4b641f448aa8b1dcaa0d8548ca2608332419`

Runtime checks, one namespace at a time: likelihood precision 16 tests/178
assertions; belief 78/2030; refusal continuity 3/31; accumulation kernel 8/250;
WM accumulation and forwarding 12/57; free energy 28/162; trace 40/131;
lane futility 5/18; live arena wiring 3/9. All passed. clj-kondo reports no
errors or warnings on the changed production/new test files; existing belief
tests have an unrelated redundant-let warning. Emacs check-parens passed.
Registry EDN reads successfully; no formal equation was weakened.

At commit time I relied on claude-8's earlier coordination response that no
flight was in the air; I did **not** take a contemporaneous process/park snapshot
at 00:25:43Z. A later process check found only source-inspection commands
containing runner names, not a matching flight process; it cannot establish the
earlier state retrospectively. No live reload, restart, migration, flight or
production observation was performed by this packet.

Final runtime warrants at futon2 `3b76aa8a5` (all warrant true, postcheck matched):

- Likelihood precision: `test-registry-69ddc617a7db5bb93d6e79e9fd4fa654edf66df491001e9e95f9b8f09b4e99bc`
- Arena refusal continuity: `test-registry-0a059c01f2536a584a02b0d05e4d0f1edb314312b1b1b071f45465209077f1ef`
- Accumulation and flight configuration forwarding: `test-registry-590a48d37084c01c8f5238a7131028b7eabdcd52d0e915d086470329fdfd01f0`

The final two warrants supersede earlier registrations made before the explicit
missing-model correction; no obsolete warrant is used as evidence for that fix.

# Proposal for finishing items 3 and 6

Documentation only, requested by Joe on 2026-09-27. Source read at futon2
`d1d529b3b`; this section proposes implementation, not a new authority ruling,
accepted completion or production dispatch. Earlier sections remain delivery
history. In particular, the item-5 stale-predecessor correction described here has since been implemented
at `2d2a8fd04` (review outstanding): record the refusal without halting the run,
and return that SAME receipt to downstream readers. Item 6 must consume the
published result, not the pre-publication proposal.

## Recommended approach

Finish two narrow paths with named consumers. For item 3, follow the actually
enacted sequence and make its posterior the next selection's input. For item 6,
score declared alternative priors against the accumulated parameters and record
the result. Do not combine either with a new arena state representation, a new
scorer, arbitrary channel/status merging, or automatic model-schema migration.

There are two substantive choices to settle in the implementation specification:
**what witnessed transition the token trajectory follows**, and **what likelihood
and simpler models the accumulation's BMR comparison represents**. Below are
concrete recommendations, including where existing evidence is insufficient.
These choices belong in the relevant model declarations, not in another general
agreement document or an extra chain of discovery dispatches.

## Item 3: one posterior along the enacted trajectory

### Boundary and data flow

Use the existing target-qualified token carrier consumed by joint selection.
The temporal chain belongs to a declared model/domain and execution stream;
**it must not reset when the selected policy changes**. Existing flight steps
are indexed by policy and therefore supply useful observations and computations,
but cannot simply become the authority for this cross-policy trajectory.
The seven-status arena remains separate. The per-policy F prefixes also remain
separate: a posterior along the enacted sequence is not every candidate's F.

At selection n, consume q_n and choose an action u_n. After verified execution,
record its observed outcome o_(n+1). Before the next selection, compute:

```
q_pred(x) = sum_s B_u_n(x | s) q_n(s)
Z         = sum_x A(o_(n+1) | x) q_pred(x)
q_(n+1)   = A(o_(n+1) | x) q_pred(x) / Z       when Z > 0
```

Use `exact_belief_core/condition-predicted` through the existing manifest or
finite-kernel adapter. Bind B to the **previous enacted action**, not the new
candidate about to be ranked. Pin A to the actual observation mechanism/version;
a changed A or B needs an explicit compatible model revision, not an unexplained
hash substitution. Unknown coordinates are marginalized, as in flight
conditioning; an unchecked token is not an observed false token.

The producer should retain one replayable event with references to existing
records rather than a second independent observation store:

| Input | Producer/evidence |
|---|---|
| Previous q, predecessor occurrence, consumed-event cursor | Last published trajectory result; initial q0 only at a declared start |
| u and the B used for it | Execution receipt joined to the action and model declaration, including ordered transition semantics |
| Checked token subset, observed values, observation event ID | Actual post-execution check receipt, with source revision and check mechanism |
| Domain and A/B identities | Existing interpretation/measurement receipts, checked against this trajectory |
| Result and next-consumer reference | Exact-update receipt plus the next selection's retained input receipt |

Prefer to extend the existing execution/check receipt producers and the token
input receipt. A small pure temporal-join function is useful; a new orchestration
service is not needed. Extract reusable arithmetic from `flight/conditioning-step`
only if needed, while keeping its per-policy ownership unchanged.

### Execution is the first concrete task

Task-level execution evidence currently says that work ran with artifacts; it
also explicitly does not establish the candidate-to-minted-action join. A
successful command, selected candidate, or supplied pattern precedence alone
cannot certify that the declared token transition was enacted.

For each supported action class, bind its existing executor receipt to the
specific transition interpretation it actually implements. A verified macro
may have a declared B, but it must be labelled as a macro and its outcomes must
be independently checked. If the executor only did one primitive, compose or
select B for that primitive; do not attribute the whole proposed cascade to it.
The closure fixture must expose this difference. An unsupported action records
why this trajectory did not advance; it does not invent a transition.

A fixed-domain fixture is the smallest complete implementation test, **not a
fixed-domain waiver for production**. Production currently derives universes
from assembled problems. Name the model domain independently of menu order,
and handle target/domain change as either a proved mapping or a declared new
trajectory. No carry across incompatible domains; no silent fresh-fact reset
claiming continuity. A missing temporal result leaves this feature absent and
the existing declared initialization route identifiable as initialization.
It need not halt the whole WM run, but that run cannot witness temporal closure.

### Two implementation increments, then one closure check

1. **Produce and replay the temporal input.** Join actual executor/check records,
   prior occurrence and model/domain; deduplicate by event identity, not just
   equal observation values. Save the admitted inputs or typed absence. Do not
   change selection until this join is demonstrably correct.
2. **Compute, publish and consume.** Apply the exact kernel once; publish the
   result and consumed cursor consistently; feed that exact posterior into the
   existing next-selection input. Make replay idempotent. A repeated observation
   with a new valid occurrence can be informative; replay of the same event is
   not a new update. Reuse the publication receipt mechanism rather than adding
   another lock or independent state file.
3. **Close on a real producer-to-consumer test.** Three chronological selections,
   two different enacted policies and non-identity B. Show that changing the
   previous action or fresh observation changes the next consumed q. Show that
   changing only the next candidate does not change the already computed q.

Controls: initial evidence is not conditioned twice; repeated event ID is not
counted twice; distinct same-valued observations are not deduplicated; a failed
execution is not called enacted; policy switches retain the chain; altered
predecessor/domain/model cannot masquerade as continuation; unchecked tokens
are marginalized; impossible evidence yields a retained contradiction with no
posterior and no silent restart. Reread the published result and the next
selection receipt and compare their actual values/digests.

**Acceptance:** the tests use real execution/check adapters with isolated
artifacts, real update arithmetic and real persistence; only external transport
may be replaced. The Lean binding covers the declared kernel/ordering, registry
and wiring map name producer and consumer, and no runtime initialization path
is counted as temporal filtering. A later live-run claim requires an explicitly
identified live witness; these tests alone establish the implementation.

## Item 6: BMR over a declared accumulated model

### Settle the model before writing the adapter

The current matrix contains increments o_c * mu_s for 14 scalar channels and
seven entity statuses. Neither its shape nor that recurrence says whether it
is one 98-category distribution, seven channel-given-status distributions, or
14 status-given-channel distributions. They have different normalizers and
therefore different reduction scores. `a4a`'s capability/mission reducer also
uses a different prior and proposal family; do not reuse that family by renaming
rows. Reuse the numerical `bmr/bayesian-model-reduction` primitive instead.

**Recommended smallest model for these existing statistics:** seven independent
Dirichlet factors over channel coordinates, one per status, with the explicitly
declared weighted likelihood

```
L(theta; history) proportional to
    product_(t,c,s) theta_(c|s) ^ (o_(t,c) * mu_(t,s))
```

This matches the accumulated sufficient statistics and gives a sum of seven
factor scores. However, raw channels are simultaneous scalar signals, not a
single categorical draw. This declaration is a **weighted/power-likelihood
parameter model**, not automatically the measured token A or a calibrated
categorical likelihood for raw scans. Its evidence interpretation must be
stated in the registry. Common likelihood constants cancel in prior comparisons
only when both models use the same data law and weighting. Do not normalize or
rescale historical observations to make them fit; that changes the statistics.

If the required claim is ordinary generative-model evidence for the raw scans,
this recommendation is insufficient. Then specify the observation law and collect
its sufficient statistics first (for example, a bounded-channel model requires
bounds and exposure/failure statistics, not just these 98 positive counts).
That is a substantive model change. Record it as such rather than calling the
current adapter a completed A-learning consumer. The weighted-model option is
adroit only if that explicitly limited claim is the intended item-6 obligation.

### A concrete, bounded proposal family

Start with **same-carrier positive-prior constraints**, not channel/status
merges. One simple family is stronger concentration around a declared simpler
reference profile: for each factor, alpha'_s = kappa'_s * r_s, where r_s is
strictly positive and sums to one, and kappa'_s is a declared concentration.
A common reference profile across statuses expresses the hypothesis that the
channel profile depends less on status, but does **not** enforce exact equality
or remove parameters. Label it a soft prior constraint, not a literal merge.

The model declaration must enumerate a small finite family, its reference
profiles/concentrations, its reason for treating them as simpler, and its author.
Fix the family independently of the posterior being tested; do not manufacture
a' by averaging the same posterior and call that independent model evidence.
A stronger concentration is a design proposal here, not a Joe preference, an
already authorized numerical setting, or automatic evidence of simplification.
If genuine parameter tying/pruning is required, use its appropriate constrained
model evidence; finite positive Dirichlet priors cannot silently implement a
point constraint or a zero prior.

Retain the existing signed delta-F convention. Compare with the kernel's -3
threshold once on the **sum across factors**, not once per factor followed by
an invented vote; document this as the current decision rule, not a newly
calibrated threshold. Include the identity prior as a control, but an empty
proposal family or identity-only run cannot witness a useful BMR consumer.

### Implement the small consumer

1. **Named-coordinate adapter and scorer.** Recover a from the immutable origin,
   A from the newly published successful accumulation, and a' from the explicit
   proposal declaration. Bind entity/revision, factorization and state digest.
   Iterate each factor by declared names, never map insertion order. Compute
   A' = A + a' - a and the sum of factor delta-F values with the existing kernel.
   Validate matching coordinates and positive finite inputs/results at this
   boundary; translate numerical failures into a typed unavailable result.
2. **Attach one result and retain it everywhere.** A computed receipt names the
   factors, origin, accumulation digest, proposal, score and rule outcome. An
   unavailable accumulation produces an absence with its actual cause. Persist
   the same result through scheduled, one-shot and flight receipts; add the
   currently missing decision projection keys. No second scoring on readback,
   fallback to an older accumulated state, or exception that stops selection.
3. **Test the complete path.** Initialize and step the real accumulator, publish,
   read that state into BMR, and reread the same reduction result from each
   route's receipt. Force a publication race: the losing update's BMR must be
   absent too, not a score over unpublished concentrations. This depends on
   correcting item 5's throw and propagating its publication outcome.

Controls: 98 distinct named values survive round-trip and map insertion reorder;
missing/extra coordinates cannot be zipped away; factorized score differs from
an intentionally wrong flat score; A'-a'=A-a; identity proposal yields zero;
zero-data seed A=a yields zero for every valid proposal; nonpositive A' is
unavailable; declared nontrivial alternatives have independently calculated
scores and include an informative discrimination case. Floating score comparisons
need a stated numerical tolerance; named-coordinate and receipt identity checks
are exact. Missing inputs leave the selected action unchanged.

**Acceptance has two distinct levels.** A declared proposal family scored from
the actual accumulated state, retained and replayable on the production route,
closes the *BMR scoring consumer*. It does not establish that the machine adopts
a simpler model. If the plan/theorem requires structural adaptation, add a
separate next-model consumer: nominate the scored proposal, apply it only at a
model boundary with a declared state migration, then demonstrate that the next
inference uses that revision. Record-only scoring must not close that stronger
requirement. Do not silently narrow the theorem to obtain a pass.

## Linear order and scope control

1. Review item 5's non-halting publication receipt propagation (prerequisite;
   subsequently implemented at `2d2a8fd04`, after this proposal was written).
2. Implement item 3's execution/observation join and exact posterior consumer;
   finish its discriminating producer-to-consumer test before opening another
   implementation packet.
3. Write item 6's short model/proposal declaration at its registry authority,
   choosing the evidence claim explicitly; then implement the adapter/scorer
   and receipt consumer in that order.
4. Reconcile registry, map and plan against the actual closure tests. Mark the
   scoring-consumer and model-adoption claims separately. No LoC threshold is
   an acceptance criterion.

Item 6 consumes the status-model accumulation, whereas item 3 consumes token
belief. There is no mathematical dependency requiring one to be converted into
the other. This is a sequential work order for clarity, not an instruction to
force the two carriers together. Existing C_tau/preference and intermediate-
progress obligations remain; neither completion would establish Joe's preference
adequacy by itself.

Sources: this file's delivery pins; `proof2/packets/C-R3s-D.md`,
`proof2/packets/C-R17-BMR-D.md`, `proof2/packets/C-R17-FLIGHT-D.md`;
`token_belief_carry.clj`, `token_belief_predecessor.clj`,
`token_initialization_policy.clj`, `d_predecessor_task_authority.clj`,
`flight.clj`, `exact_belief_core.clj`, `exact_belief_adapter.clj`,
`machine_accumulation.clj`, `bmr.clj`, and `r17_offline.clj` under
`src/futon2/aif/`. Proposals above are distinguished from these implementations.


## Item 5 correction — 2d2a8fd04

Joe directed implementation after reading the proposal. The append's finalizer
runs under the existing trace/index lock. A changed predecessor replaces the
optimistic accumulation receipt with `:status :absent`, reason
`:accumulation-stale-predecessor`, and expected/actual predecessor IDs. It removes
`:accumulation-state`, `:accumulation-update-input` and initialization from that
record, while preserving the selected action. Both competing runs can publish;
only the first can claim the accumulated state.

The writer returns the exact finalized record. WM reconciles its returned
judgement from it (including when a later rationale write fails). The one-shot
receipt therefore sees the same outcome. Flight updates both its pending
selection checkpoint and retained checkpoint before persisting the run receipt.
Scheduled execution emits evidence from the exact published record. An injected
legacy test writer may still return only a path; production uses record-returning
publication. No candidate is rescored or reselected by this correction.

This does not hide independent storage/index failures. A corrupt authoritative
corpus cannot produce the required exact futility index; its existing error
remains reported rather than publishing a fabricated coherent index. The tests
separate that case from a healthy corpus whose predecessor changed. A refused
accumulation tail also remains explicit on the next read: there is no automatic
skip, reinitialization or seeded-tail migration.

Validation: accumulation/receipt propagation 14 tests and 77 assertions; trace
40/131; futility index 5/18; WM 89/511; scheduled flight entry 4/17. Total 152/754,
all passing, one namespace at a time. The race test computes both updates against
the same actual disk predecessor before either publishes. It then checks both
records, absence of disputed state, unchanged selection, one-shot receipt,
flight checkpoint and the reread persisted flight run record. The corruption
control retains the independent index error. clj-kondo: no errors/warnings on
changed files; Emacs check-parens and git diff --check passed.

Immediately before the source commit, a `/proc/*/cmdline` scan at
2026-09-27T00:47:54.558021Z found no matching WM scheduled, one-shot, flight,
outer-loop or full-loop launcher arguments. This is a contemporaneous process
check, not a claim of a production flight witness or a shared-JVM reload.
Source commit `2d2a8fd04` was reported to claude-8 so ⟨2⟩3 warrants could resume.

Registered focused warrant at `2d2a8fd04`:
`test-registry-468b54a8d15e8459da29e1bb76840e8ea0fa8801b7bcf5f84094e30d15b96f12`
(`warrant? true`, postcheck matched; real accumulation/publication/receipt tests).


## Item 4 review correction — local omissions, 2026-09-27

Source commit `445820342`, on claude-8's independent review finding. One refused
entity previously refused every population prediction, and a refused channel
emptied the judge's entire error map. Valid entities could consequently stop
receiving updates indefinitely without the run halting.

Population predictors now omit invalid/refused entities with explicit
`:omitted` records (`:entity-id`, `:cause`, `:reason :belief-unavailable`).
They compute from valid entities only; an all-invalid population returns typed
absence without numeric moments. The original belief map is retained. The
judge collects valid channels independently of refused channels, records the
prediction entity omissions in its micro-step trace, and does not terminate
its micro-loop merely because some channel was refused. Existing attribution
continues to omit refused entities while updating valid ones.

Unknown observation types leave belief coordinates unchanged and produce typed
omitted-event receipts. The population update retains these transiently in
metadata, and `trace-record` explicitly persists them as
`:belief-event-omissions` (metadata alone would not survive EDN serialization).
An unknown event does not create a new entity. Impossible evidence and invalid
models still refuse their entity. **No recovery or reinitialization rule was
added:** refused entities remain refused pending Joe's ruling.

Validation: belief-refusal-continuity 5 tests / 57 assertions; belief 78 / 2030;
arena-attribution 4 / 59, all passed. The new real-dependency cases construct
impossible categorical evidence, show surviving population predictions, then
show a valid entity updating with the refused entity unchanged. The judge
regression evaluates its actual error-selection and attribution bindings with
one refused channel and one valid channel. Unknown-type tests check unchanged
belief, a later valid update, and explicit trace receipt persistence.
Check-parens and diff whitespace checks passed. clj-kondo found no errors or
new warnings (the existing redundant-let warning at belief_test.clj:85 remains).

Pre-commit process check: {'time': '2026-09-27T01:41:07.530687+00:00', 'matching-launchers': []}.
This checks launcher processes, not a flight witness or a JVM reload. The
source commit touched only belief, trace, judge and their three focused tests;
other lanes' flight-runner/temporal-input edits were left untouched.

Registered warrants at `445820342` (all `warrant? true`, postcheck matched):

- belief-refusal-continuity: `test-registry-69320db1021d312f1dac85da91a2ec59f90aef29854a995b68cd999c70dae4ea`
- belief: `test-registry-5e263fa53e2a5eef58267836d1a90601da4a2684eecbe62beb56bd805c1d106b`
- arena-attribution: `test-registry-0e181a9415335d26bd6e195ccc45715bef760bc5e41bce7da2e7299292c3978b`
