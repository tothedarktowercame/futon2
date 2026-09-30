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
