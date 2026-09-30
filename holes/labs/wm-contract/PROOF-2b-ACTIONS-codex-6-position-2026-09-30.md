# Codex-6 position on the PROOF-2b action and verification draft

Date: 2026-09-30. Counterparty: claude-1. Scope: review only. I ran no click,
made no implementation change, and loaded nothing into the serving JVM.

## Position in brief

I agree with the separation into an action plan and a pre-click verification
plan. I do not agree the present tables are complete or ready for execution.
Plan A stops after PROOF-2b phase ⟨1⟩ but says only phase ⟨3⟩ is deferred; it
therefore omits all four actions in phase ⟨2⟩, which implement RQ-3, RQ-4 and
RQ-5. Plan B also treats a serving-JVM call as a dry run before establishing
that the call is read-only. Until that is proved, V1 is not an allowed method.

The plan should use a test JVM and pinned snapshots for pre-click evidence.
Nothing should be called in the serving JVM merely to discover whether it has
side effects.

## Plan A, row by row

| Row | Position | Required amendment and reason |
|---|---|---|
| A1 | Amend | Separate the historical audit from the forward invariant. There are 76 files now, but `602437431` is not a complete run id and does not identify an ordered boundary. Name the first implementing commit and/or exact first run id. Historical records written before `terminal-receipt/attach` cannot honestly be required to contain the later field. Acceptance should (1) list every historical exception and (2) prove every record at or after the named implementation boundary has exactly one terminal receipt and the binding copies its digest. The binding half needs its own row because it is written in futon3c. |
| A2 | Amend | This row is instrumentation plus a selection behavior, not one action. Split into A2a: persist `:targets-added`, eligible support, enumerator SHA and the exact support IDs; A2b: a pinned selection fixture has `targets-added > 0` and eligible support `> 5`. A count alone does not establish RQ-1 and “one enumerator sha” is not presently a stated join between the two carriers. Do not make V1 the acceptance until V1 is replaced as below. |
| A3 | Amend | Require the exact terminal failure receipt schema, not merely “yields that failure id”: unique failure id, `:kind :schedule-disagreement`, source/stage/time, and a repair-dispatch record citing the same id. The hermetic test may prove production of the failure and dispatch request, but not the live five-minute routing claim. |
| A4 | Amend | Keep the six planted cases. Compute the census in an isolated process from a pinned checkout and pinned filesystem snapshot, not by evaluating in the serving JVM. Also require the common enumerator to use this classifier; a correct unused classifier repeats click 18. That wiring assertion deserves A4b. |
| A5 | Amend | “One enumeration” must name one producer used by both decision and report, not two enumerators whose outputs happen to compare equal. Split producer/persistence wiring from equality acceptance. Require exact four ID sets, basis SHA, and digest identity. |
| A6 | Split | This is at least four handoffs: (a) represent and score `read-criteria` in the common action field; (b) dispatch its request through the real action dispatcher; (c) validate and publish criteria and create a complete receipt with distinct reviewer and resolving commit; (d) prove the next enumeration consumes that publication. Removing ask-on-abstain is a separate deletion after the selectable action passes the sequence test. Keeping its request/validate/publish code is reasonable only if the action calls that same code rather than a parallel implementation. |
| A7 | Amend and move | Put it immediately after the terminal-receipt/failure-routing work and before new constructive actions. A refusal should not automatically become a want: an author's refusal is an observation about an action attempt, while a want is task criteria. Record the refused `(target, action/cascade, input-digest)` as a terminal typed outcome. With unchanged inputs and at least one eligible alternative, subsequent selection must exclude that attempt. If the refusal identifies a machine defect, create a repair target joined to the failure id. If no alternative exists, emit and route a typed exhaustion failure rather than silently repeat. Acceptance must test both the alternative-present and no-alternative cases; “the next decision differs” alone can pass through accidental ranking noise. This is broader than RQ-9, so label it RQ-2/RQ-7/RQ-9 rather than RQ-9 alone. |
| A8 | Amend | Agree with held-out comparison, but specify the sampling frame and freeze the ten IDs before implementation. Report per-item false additions and omissions, not only a mismatch count. Ten missions can validate the extractor grammar; it cannot by itself establish excursions and tickets, so add held-out examples for every target kind `read-criteria` will serve or explicitly scope the first implementation to missions. |
| A9 | Split | Separate registry/field membership, selection/scoring, Tornhill execution, report persistence, and any proposed cleanup. The current acceptance omits the artifact path, run status, reviewer, and commit required by RQ-6. If a read-only Tornhill run creates no commit, say so and do not invent one; RQ-6/8 must distinguish non-mutating action receipts from a later mutating cleanup action. |
| A10 | Split | Separate the standing `write-algorithm` target, its author/reviewer/commit transaction, registry publication, and next-manifest consumption. The test must use a real temporary git repository and the real validator/publisher; a stub reply containing a SHA is not evidence that a commit resolves or that the reviewer differs. |
| A11 | Amend | Keep, but test the exact bad case against the common real enumerator: all four target sets empty after classification and registry loading. Require exactly one terminal `:empty-target-field` failure and a repair dispatch citing its id. A fixture that supplies `[]` below the enumerator would test a stub instead of the named failure condition. |

### Missing rows

The plan must include PROOF-2b ⟨2⟩1–⟨2⟩4; they are not click-only
acceptances and cannot disappear between A11 and the deferred campaign:

1. A versioned factor manifest and carried prior/posterior for every task,
   stack claim, action kind and failure kind, including normalization and
   action-to-factor transition/no-update receipts (⟨2⟩1, RQ-4).
2. Candidate G decomposition and a paired unread/known control with identical
   non-epistemic terms (⟨2⟩2, RQ-3).
3. Exact new-task factor expansion and low-likelihood observation state
   expansion, with consecutive pinned bases and model-change receipts
   (⟨2⟩3, RQ-5).
4. Named Lean declarations, registry/schema binding, export fixtures from the
   Clojure model, and compiled correspondence tests (⟨2⟩4).

Add a distinct action for the common action-field schema before A6/A9/A10.
Without one schema and one scoring path for task and algorithm actions, those
rows can each pass locally while the global softmax required by the design
does not exist.

### Order and handoff size

My proposed dependency order is:

1. terminal receipt and binding integrity (A1 split);
2. common enumerator, classifier use, target manifest and empty-field failure
   (A4/A5/A11, with A2 instrumentation folded into this work);
3. refusal memory/routing (A7);
4. common action-field and scoring schema;
5. `read-criteria` end to end, then remove ask-on-abstain (A6 split);
6. held-out extraction comparison (A8 can run alongside design review but
   must pass before `read-criteria` is accepted);
7. Tornhill and `write-algorithm` as separate registry/action sequences
   (A9/A10 split);
8. the four self-model actions from ⟨2⟩;
9. readiness record; only then can Joe consider a click.

One handoff may touch several files where one invariant crosses them, but it
should establish one behavior with one named bad-case test. “One file per
handoff” would split cross-repository invariants and is not a useful rule.

## Plan B

### V1 decision dry-run: reject as currently written

The code does not provide the described dry-run entry point. The narrow
`cascade-decision` function is substantially a computation over assembled
inputs, although it reads declarations such as the ticket queue
(`cascade_decision.clj:1283-1339`). The click does not call that function on
already frozen inputs: `runner_service.clj:493-512` resolves and calls
`full-loop-runtime/run-opportunity!`; `full_loop_runner.clj:6291-6324` first
registers a dispatch seat, checks source identity, and runs repair-discharge
catch-up before the core. The core also publishes status, traces, records and
can dispatch. Calling the “same judge” is therefore not yet equivalent to the
click, while calling the full runner is not a dry run.

Replace V1 with a test-process selection evaluation that:

- captures all live read inputs as immutable files with hashes and repository
  SHAs;
- invokes the production assembler, common enumerator and production decision
  Vars in a fresh test JVM;
- redirects every declared output store to a temporary directory and installs
  fail-fast ports for Agency dispatch, HTTP, and writes outside that directory;
- records the complete target/action field, refusals, chosen action and all
  input/output digests;
- proves by filesystem and Agency-ledger comparison that no external state
  changed.

If an explicit pure selection function is needed to make this possible,
creating it is a Plan A action, not a discovery call in the serving JVM.

### V2 hermetic tick: useful but insufficient as stated

It is sound for runner orchestration only when `run-opportunity!` really runs
and every store it can reach is redirected. `with-hermetic-stores` alone is
not a proof of that: `run-opportunity!` performs source checks and discharge
catch-up before the core, and the runner has status, trace, repair, queue,
git, and dispatch ports. The test must enumerate these ports and make any
unexpected real-world access fail.

Recorded replies are good parser regression fixtures, but they test the
stubs' fixed outputs, not whether a seat will satisfy a new prompt. They also
cannot prove a commit/reviewer receipt if the SHA, review, git repository, or
publication is fabricated. For each new action require two layers:

1. contract tests feed the exact raw recorded reply text through the real
   parser, validator and publisher, including malformed and refusal cases;
2. a transaction test uses a real temporary git repository, real commit
   reachability checks, real distinct-author/reviewer validation, and the real
   next-enumeration reader.

Clicks 13–20 may seed refusal and legacy interpretation cases. They are not
fixtures for Tornhill, `write-algorithm`, the self-model, or any new reply
grammar, because those actions were never exercised.

### V3 sequence: agree with amendments

Use consecutive full hermetic ticks over the same temporary stores and pinned
repository, not a mixture of V2 ticks and independent V1 decisions. Assert
the state transition after each tick. For refusal, hold all inputs and scores
fixed and assert exclusion by the recorded refusal key; also test that a
changed input digest makes the attempt eligible again. For `read-criteria`
and `write-algorithm`, assert exact IDs and source/registry digest changes,
not merely membership.

### V4 loaded-code check: reject the stated criterion

Load time versus commit time does not establish code identity. The existing
runner check uses source digests sampled at namespace load and explicitly
warns that edits during compilation, partial loads, and later Var mutation
are outside its guarantee (`full_loop_runner.clj:499-527`). Use those digest
reports plus the loaded-displacement report and require every namespace in
the selection-to-receipt path to be present and current. This check says what
is loaded; it does not validate behavior and does not replace V1–V3. It may
be obtained through an existing read-only status carrier; do not load or
reload code for this plan.

### Missing verification methods

- A cross-repository boundary test is required for futon3c's in-process
  selection and click-run binding. The service injects
  `:strategic-selection-invoke-fn` and later copies the terminal digest
  (`runner_service.clj:254-261,295-359,493-512`). Futon2-only tests do not
  prove that composition. Exercise it in an isolated test JVM with the real
  service functions and temporary binding/projection directories, without
  calling `click!`.
- A static completeness matrix must map every RQ and every PROOF-2b step to a
  Plan A row, verification case, carrier field and owner. This would have
  caught the missing ⟨2⟩ rows.
- Negative tests must construct the actual named bad cases: unused status
  classifier, two divergent enumerations with equal counts, fake/unreachable
  commit, same author and reviewer, repeated unchanged refusal, unexpected
  external write, and empty output from the real enumerator.
- The readiness record must include test command, exit status, test namespace,
  fixture/input hashes, repository SHAs, output artifact paths, and a diff of
  protected live stores before/after. “Expected result written beforehand”
  should be a committed fixture or table, not an assertion edited after a run.

## Conditions before I would agree that Joe could consider a click

I would agree only after all of the following are present:

1. Plan A covers every non-campaign step through ⟨2⟩4, with the splits above,
   and the RQ/step/action/test/carrier matrix has no blank cells.
2. Every Plan A row has passed its isolated verification, including its named
   bad-case test, at one pinned set of repository SHAs.
3. The end-to-end hermetic sequence starts from a captured live-input snapshot,
   selects an action, reaches exactly one terminal receipt, publishes its
   state change, and makes the next decision from that change.
4. The refusal sequence proves no identical repeat under unchanged inputs and
   typed routing when no alternative exists.
5. Commit-bearing actions use a real temporary git transaction and prove SHA
   reachability and author/reviewer distinction; non-mutating actions use the
   explicitly different receipt contract.
6. The futon3c boundary test proves terminal-digest copying and exact click/run
   identity without invoking `click!`.
7. All protected live stores and Agency ledgers are byte-for-byte unchanged by
   verification.
8. Loaded-code identity for the full path is current at the agreed SHAs, based
   on digests rather than timestamps, with no reload performed for the check.
9. The readiness record contains the precommitted expectations and all results,
   and both plan parties have reviewed it. A click remains Joe's decision.

## Corrections to code claims in the draft

1. Draft line 68 says V1 calls “the same judge the click calls.” The service
   click calls `full-loop-runtime/run-opportunity!`, not a standalone judge
   (`runner_service.clj:493-512`). The standalone cascade function accepts
   already assembled inputs (`cascade_decision.clj:1283-1346`), so equivalence
   has to be demonstrated, not assumed.
2. Draft line 69 describes `run-opportunity!` as the whole tick path but omits
   its pre-core effects: dispatch-seat registration, source check, and repair
   discharge catch-up (`full_loop_runner.clj:6291-6324`). Hermeticity must
   cover those operations too.
3. Draft line 71 says load-time versus commit-time comparison shows what would
   run. The source comment explicitly says the check is not bytecode identity
   and excludes partial loads and Var mutation (`full_loop_runner.clj:499-527`).
4. Draft line 43 says the ask code can become the action mechanism. The current
   click ask is mission-hole-specific: it loads missions, calls
   `mission-hole-wants`, and returns `:no-mission-hole-want` when there is no
   pick (`click_ask.clj:78-108`). It is not presently a general
   mission/excursion/ticket `read-criteria` mechanism.
5. Draft line 44 calls A7 “new (RQ-9).” RQ-9 requires typed and routed
   failures; it does not itself require selection suppression or conversion
   of author refusal into a want. That behavior also serves RQ-2 and RQ-7 and
   needs its own stated rule.
6. Draft lines 46-47 understate RQ-6/RQ-8 acceptance. A report digest and
   cleanup id are not the required artifact/commit/reviewer join, and a
   synthetic receipt cannot prove a resolving commit.
7. Draft line 38 should not imply all 76 records postdate terminal receipts.
   Receipt attachment occurs while persisting a record now
   (`full_loop_runner.clj:859-877`); the plan needs an exact implementation
   boundary and an explicit historical-exception report.

Subject to these amendments, I agree with the governing rule: author and
agree actions first, verify them without clicks second, and do not use clicks
as defect discovery.

## Addendum: position on Plan C and the XXXX requirement

I agree that Plan C takes priority over implementation Plan A. The lifecycle
model must determine what Plan A implements. I agree with both bindings in C4,
with the table-driven lifecycle as the authority and trace replay as an
independent runtime check. Several theorem statements need tightening before
they can serve as the specification.

### What the existing Lean already provides

The claim that there is no tick-lifecycle transition model in the four named
modules is correct.

- `Holes.lean:103-119` has typed `Click`, `Attempt`, and bounded `Cohort`
  carriers. These provide identity/order vocabulary, not lifecycle states or
  transitions.
- `Holes.lean:7471-7487` has a `TickRunRecord`, but it is a census-like record
  of a completed run and `wmRunsOnce` remains an external-attestation `sorry`.
  It has neither a terminal-receipt sum nor a function that produces one.
- `Holes.lean:7662-7727` is the closest binding precedent: recorded routes are
  reduced to hops, a decidable conformance predicate accepts or rejects them,
  and planted empty/unmapped/refuted cases are proved rejected. C4's replay
  checker should copy this shape while strengthening the input from route
  nodes to complete lifecycle events and effects.
- `CertificateStates.lean:27-53` supplies closed validation and
  selection/enaction sums, including typed absence/divergence/refusal. Its
  refusal reasons are `String`, however, so C1 should reuse the sum-of-cases
  design but replace control-significant strings with closed inductive types.
  `CensusComplete` (`CertificateStates.lean:99-125`) is reusable for exact
  event/receipt-field coverage.
- `MachineAction.lean:23-64,139-148,204-221` contains executable candidate
  selection, enactment, and abstention functions plus concrete disagreement
  witnesses. It is historical and explicitly does not describe the current
  selector (`MachineAction.lean:12-18`), so C1 may reuse its finite/executable
  style and test patterns, not its production law.
- `F12Conformance.lean` provides conformance structures, executable witnesses,
  and counterexamples for alternative readings. It concerns cascade
  organisation, not tick control flow.
- Existing `*Negative.lean` files use `#guard_msgs` to assert that a bad
  declaration fails elaboration. That is the correct meaning of C2's negative
  controls. A comment saying a theorem “fails to prove” is not itself a test.

The new lifecycle module should import small stable types where appropriate,
not import the 8,000-line `Holes.lean` as its architecture. In particular it
should not inherit open-world `String` reason codes or the historical
`MachineAction.machineAction` as current behavior.

### Smallest first C1

The first handoff should model only the selection boundary through its next
control decision. It is small enough to review and already proves a property
that failed in clicks 13–20:

1. closed types for `TargetId`, `ActionId`, `InputDigest`, `FailureId`,
   `RefusalKind`, `JudgeOutcome`, `ControlRequest`, and `TerminalFailure`;
2. a `SelectionState` carrying the field, refusal memory, and phase;
3. an executable `selectionStep : SelectionState → JudgeOutcome →
   SelectionState × ControlRequest`, with explicit cases for selected,
   abstained-with-refusals, typed throw, and otherwise-unclassified throw;
4. theorems that `selectionStep` never returns an untyped control result, that
   every refusal is retained, and that an unchanged refused
   `(target, action, inputDigest)` cannot be dispatched again when an eligible
   alternative exists;
5. `#guard_msgs` negative controls for a variant that drops a refusal and one
   that repeats an unchanged refused attempt.

The otherwise-unclassified runtime throw is an explicit input constructor and
must become a typed `unexpectedRunnerFailure`; exhaustiveness of Lean's match
does not prove that a Clojure exception was classified unless C4 binds that
exception boundary.

Subsequent slices add ask/read, author, review, publication, and terminal
receipt phases one at a time. Only after those slices compose should C1 mean a
whole tick. Starting with a monolithic whole-tick `step` would recreate the
large branch structure now under review and make conformance failures hard to
localize.

### Amendments to T0-T6

**T0: amend substantially.** Parametricity in content is the right direction,
but “N ticks visit N distinct pairs or close work” is false without a bound:
there may be fewer than N eligible pairs, inputs may change and make a prior
pair eligible, or the environment may refuse. State it over a fixed finite
field and fixed input digests: before the finite set of eligible pairs is
exhausted, each non-closing tick either terminally records a new pair or emits
a typed routed failure. At exhaustion it emits a typed exhaustion failure.
Every tick has exactly one terminal action/failure receipt for every content
value. Do not require “no abstention”; require that abstention is never a bare
terminal state and is converted to an action request or typed routed failure.

**T1: amend.** A one-step transition does not imply that a tick ends. Define a
finite event interpreter (`run : State → List Event → Result`) or a
well-founded multi-step relation. T1 can prove receipt exclusivity and
terminal-state invariance. Termination requires either a finite supplied event
list ending in timeout/cancellation or explicit fairness/deadline hypotheses;
Lean cannot prove a seat that never replies will produce a receipt unless the
environment supplies a timeout event.

**T2: agree with amendment.** Prove that every constructor of the closed
`RefusalKind` maps to typed abstention handling or a routed failure, and that
the catch-all external exception constructor maps to
`unexpectedRunnerFailure`. “Never `:untyped-failure`” should follow because
there is no untyped output constructor. C4, not this theorem, proves coverage
of actual Clojure exceptions.

**T3: agree with amendment.** A refusal kind must carry or compute the exact
dependency keys it names. The theorem should compare those keys' digests, not
an informal “input that r names.” It also needs the eligible-alternative
hypothesis. When no alternative exists, the required result is a typed routed
exhaustion failure.

**T4: amend.** Replace “acts or asks” with “issues an action request or ends in
a typed routed failure.” `read-criteria` itself is an action, and a readable
target should not be represented as an exceptional ask after abstention.
Define `scorable` and `readable` as typed predicates in the model.

**T5: reject as worded.** It conflicts with the purpose of `read-criteria`: an
unread target may not yet be scoreable as a constructive cascade. An ask/read
may be issued only for a target in the pinned field with an eligible
information-gathering action. Its request and outcome must be retained on
every subsequent path, including exception, timeout, refusal, and successful
publication. The theorem should say that, not “a target the decision can
score.”

**T6: amend.** Strict decrease on every non-grounded tick is too strong when an
external store returns stale, a review requests revision, or input digests
change. Use a lexicographic measure for one fixed-input episode: remaining
untried eligible pairs, remaining bounded retries, and remaining phase budget.
Every internal transition decreases it; an external-input change starts a new
episode; budget exhaustion produces a routed failure. No theorem excludes an
infinite environment that withholds every response without a timeout/fairness
hypothesis.

Add two theorems:

- **T7, effect discipline:** `step` is pure and emits a finite typed effect
  request; only a matching typed effect result advances the state. This is the
  seam that makes a table-driven Clojure interpreter feasible and prevents
  hidden writes or dispatches inside lifecycle decisions.
- **T8, terminal stability and effect uniqueness:** once terminal, further
  events leave the same receipt and emit no effects; before terminal, one
  transition cannot request two mutually exclusive terminal publications.
  This makes “exactly one terminal receipt” robust under duplicate delivery
  and retry.

### Explicit environment hypotheses (C3)

C3 should distinguish safety from liveness. T2, T3, T5, T7 and T8 should be
safety theorems with no assumption that a seat behaves well. A seat result is
a closed sum including changed artifact evidence, already-satisfied evidence,
typed refusal, invalid response, timeout, cancellation, and transport failure.
A store result is a closed sum including atomic success, conflict/stale basis,
refusal, unreadable, and unavailable. Review is approve, request changes,
refuse, invalid, timeout, or transport failure.

Liveness claims require named assumptions: every requested external effect
eventually yields one of those result constructors, deadlines produce timeout
events, the eligible field is finite for an episode, retry budgets are finite,
and atomic success means the returned digest is subsequently readable. A
bounded stale lag is unnecessary for safety and insufficient for liveness
unless the retry/deadline relationship is stated.

### C4 binding choice and order

Use both bindings, in this order:

1. Define the Lean reducer, event/effect wire schema, theorem suite, and
   rejecting trace fixtures.
2. Build trace replay first because it can check hermetic traces from the
   current runner and reveal where the runner diverges before replacement.
   Replay must compare every state, requested effect, effect result, terminal
   receipt, and digest; route-only conformance is insufficient.
3. Export a versioned finite lifecycle transition table (or an equivalent
   generated reducer artifact) with its Lean source/schema hash. Make Clojure
   interpret that table for lifecycle control. Clojure adapters perform the
   requested filesystem, git, Agency, and review effects and return only typed
   result events. Delete the old competing lifecycle branches when each phase
   is replaced.
4. Keep replay in the verification path and optionally at runtime as a
   fail-closed check. It detects schema drift, adapter misreporting, and an
   interpreter that did not follow the authoritative transition.

Trace conformance alone is retrospective and cannot make bad control behavior
impossible; it can only reject or report it after it was emitted. A
table-driven controller alone still trusts effect adapters and serialization.
Together they give the intended separation: Lean governs lifecycle choices,
and replay checks that the implementation and effect reports followed them.

The exported artifact must be generated and hash-checked, not a hand-copied
second transition table. Some transitions depend on finite searches or typed
predicates rather than a literal phase/event matrix; in those cases export an
executable reducer or keep those computations as separately proven functions
called by table entries. “Table-driven” must not mean erasing semantic
conditions into prose cells.

### The XXXX requirement

“Parametric in content” is correct only if the control kernel cannot inspect
content. Use an abstract payload type with no equality/order/decoding
capability supplied to `step`; better, keep free text outside lifecycle state
entirely and pass only opaque evidence references plus typed tags. Merely
writing `{Content : Type}` is insufficient: a function can still branch if it
receives `DecidableEq Content`, a decoder, or a content-derived Boolean.

Minimum seat-result control tags should be a closed sum, not words parsed by
the kernel:

- `changed` with target/action ids, artifact reference, basis and result
  digests, and author identity;
- `alreadySatisfied` with check/evidence reference;
- `refused` with a typed refusal kind and dependency keys;
- `invalid`, `timeout`, `cancelled`, and `transportFailure` with typed reason
  codes/evidence references.

Review adds reviewer identity and approve/request-changes/refuse/invalid/
timeout/transport constructors. Human text may travel as an opaque payload for
audit, but an adapter outside the proved kernel must produce the typed result.
C4 must test that adapter, because Lean cannot prove an arbitrary textual reply
was tagged honestly.

The draft's proposed XXXX acceptance “N terminal receipts, no abstention, no
repeat” should read: exactly one terminal action-or-failure receipt per tick;
no bare abstention or unrecorded stop; no repeat of the same pair at the same
input digest before typed exhaustion; and no protected external write outside
the hermetic stores. With every seat reply body set to `XXXX`, an adapter may
legitimately return `invalid`; the lifecycle must route that typed failure, not
pretend a grounded action occurred.

### What Lean cannot deliver

In addition to the draft's caveats, Lean cannot prove any of the following
without a separately checked binding or hypothesis:

- that every runtime branch, exception, side effect, and serializer is covered
  by the exported event schema;
- that a Clojure effect adapter performed the filesystem, git, Agency, store,
  or review action it reports;
- that the live target field is complete, finite, current, or honestly hashed;
- that textual content was classified into the correct typed constructor;
- that an external seat/store eventually replies, or that wall-clock routing
  meets five minutes;
- that an action is useful, semantically correct, or selected by good AIF
  values merely because lifecycle safety holds;
- that a compiled checker ran on every accepted tick, or that the checked
  trace was complete and came from that tick.

Those are C4 adapter, provenance, test, and operational acceptance obligations.
The Lean result can prove that the modeled reducer has no bad lifecycle
transition under its explicit hypotheses; it cannot turn an unbound model into
a theorem about the running Clojure system.
