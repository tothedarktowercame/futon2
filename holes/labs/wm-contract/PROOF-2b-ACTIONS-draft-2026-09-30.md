# PROOF-2b: agreed-candidate plan (actions, verification, Lean)

Date: 2026-09-30. Parties: claude-1 and codex-6. Status: **CANDIDATE**,
awaiting codex-6's yes/no, then Joe. Nothing here is carried out before Joe
has seen the agreed text. The negotiation is in git: this file's history
(78a06c752 … d4c4067f6) and codex-6's position paper
`PROOF-2b-ACTIONS-codex-6-position-2026-09-30.md` (3ca1f852c, 46b4afa41,
441ac0df0, 018ea2273), which holds the file:line evidence cited below.

## Joe's rulings this plan answers to (2026-09-30)

1. PROOF-2b is authored and agreed as a set of actions, not run as a search.
2. The plan does not involve running clicks. Clicks were used today as
   defect discovery (8 clicks, 1 grounded change) and that is over.
3. A separate plan verifies that the behaviour will work before any click.
4. Extend the Lean proof to show that bad behaviour cannot occur. PROOF-2a
   proved nothing about the machine running: it does not run.
5. "XXXX in every text field" is a probe, not a requirement. It probably
   would not work, because tasks and agent behaviour must be well specified.
   What is owed is why it fails and what XXXX must minimally be replaced by.
6. A failure is a critical incident, and becomes a negative test at the
   right level of abstraction. It is never a non-event to be repeated.
7. Agents demonstrably write missions from Joe's text and complete them in
   the REPL. If that is hard to reproduce without Joe, M-象-cascade can mine
   his saved chats, or the design patterns can be read directly, which is
   the point of the cascade system.
8. The logs so far do not show where the actual difficulty is. Joe wants
   evidence of the form "this step is too hard, what do you suggest?".

## Order of work

D1 → D0 → I0 → M → Plan C → Plan A → Plan B → Record 1 → ⟨2⟩ rows → Record 2.
Handoff size rule: one behaviour with one named bad-case test.

## D1. The difficulty log (ruling 8)

What is known now, from the 76 run records since 2026-09-18: 7 selected a
target; 17 stopped at selection or start-up before any agent was asked to
work; 3 author refusals (one correct "already done", one refusal given
twice); 2 exceptions at close; 1 grounded change; 47 older records and 6
selected-without-failure records not yet classified. No record shows an
agent failing because a step was too hard. The machine rarely reaches an
agent.

Deliverable: for each step of doing a task — choose it, say what done looks
like, pick the method, do it, review it, record it — attempts, successes,
time, and the agent's own words where it could not. Two parts:

- D1a (read-only): classify all 76 records by the step at which they
  stopped and by whether an agent or the machine stopped it.
- D1b (agent jobs, no machine, needs Joe's go-ahead because it spends seat
  time): a fixed sample of open mission files, one job each with a plain
  prompt ("do the next unfinished item; commit; report, or say what stops
  you"), and an independent review job. The sample ids and the expected
  result are committed before it runs.

D1 decides how much of D0's agent-side structure is needed at all.

**Open question for Joe (asked 2026-09-30, unanswered):** in the REPL Joe
chooses the task. The machine chooses among about 600, and its scoring reads
only tasks already turned into formal wants, a class and a validated
interpretation; producing that form is where nearly all of today's failing
machinery sits. May an agent do the choosing too (read the open tasks and the
design patterns, propose what to do next and how), with the AIF terms
ranking those proposals and not gating which tasks may be proposed? Plan A's
selection rows depend on the answer.

## D0. The minimal content specification (ruling 5)

For each field on the path from task file to terminal receipt: who reads
it, what happens with "XXXX", and the least that must replace it. codex-6
checked claude-1's nine rows against the code and added ten (018ea2273);
that table, with file:line, is the working inventory. D0 has two columns the
inventory does not yet separate, and D1 supplies the second:

- what the **current code** demands of the field;
- what an **agent** needs from it to do the work (ruling 7 says: much less).

Where the code demands more than an agent needs, the code's demand is a
candidate for removal, not for formalising.

Results of D0, as agreed in outline:

- **Minimal task specification**, marked by action kind, not required in
  full before any action: stable id, kind, path and content digest; a
  lifecycle state from one closed vocabulary (missing or unrecognised means
  open); at least one criterion with a stable id; at least one eligible next
  action for the task's knowledge state; for observing or constructing, a
  way to check the criterion that returns met / not met / unknown with
  evidence; scope and permitted effects. Relation to focus and an
  interpretation improve choice; their absence leads to a read action or a
  typed failure and never removes the task.
- **Minimal agent contract**: a structured request (ids, input digest,
  scope, criterion, reply schema) and exactly one typed result: `changed`
  (repo, commit, check evidence), `alreadySatisfied` (evidence checked
  independently), `refused` (reason kind, the inputs it depends on),
  `invalid`, `timedOut`, `cancelled`, `transportFailed`. Review has a
  matching envelope and a distinct, machine-issued reviewer identity. Prose
  may accompany any result and never selects it. During migration a parser
  may construct the typed result; every parser outcome, `invalid` included,
  is handled.
- Twelve sites where wording currently decides control flow (441ac0df0),
  among them the exact exception message `"cascade decision refused"`, two
  different status classifiers, feature-card prose shape, and the
  "already satisfied" word search accepted this morning (27709090).

## I0. Incidents (ruling 6)

Rule: a failure, in verification or in any later click, stops the work.
Nothing is rerun to see whether it recurs. An incident record is written and
closed first: what happened (from the record); what the model and plan said
would happen; its level (model gap / binding gap / hypothesis gap / content
gap); the negative test for the class of failure, shown to fail on the old
behaviour; why the verification done did not catch it and what changes so it
would. No click is proposed to Joe while an incident is open.

Deliverable: the register for the failures already incurred (twelve from
clicks 13–20 in d4c4067f6, first cut by claude-1; codex-6 checks levels and
classes), extended to the abstentions of 09-24 to 09-29. One process
incident is on it: clicks 17, 18 and 20 were fired when their failure was
predictable.

## M. The matrix

Every RQ and every `PROOF-2b.md` step → action row, verification case,
record field, owner. No blank cells. (claude-1's first Plan A dropped
⟨2⟩1–⟨2⟩4; this is the check that catches that.)

## Plan C. Lean (ruling 4)

Existing Lean (`mathlib4/DarkTower/WarMachine`, 252 files) covers the AIF
mathematics and has no lifecycle transition system (codex-6, by reading).
Reusable: closed sums and `CensusComplete` in `CertificateStates.lean`, the
route-conformance checker at `Holes.lean:7662-7727`, `#guard_msgs` negative
controls. The new module imports small stable types and uses closed
inductive reasons, not strings.

- **C1**, in slices. First slice: the selection boundary only
  (`selectionStep : SelectionState → JudgeOutcome → SelectionState ×
  ControlRequest`), with refusal retention and no re-dispatch of an
  unchanged refused `(target, action, inputDigest)` while an alternative
  exists; two `#guard_msgs` negative controls. Then ask/read, author,
  review, publication, terminal receipt, one slice each.
- **C2**, theorems. Safety, with no assumption about seats or content:
  T1 receipt exclusivity and terminal invariance over a finite event
  interpreter; T2 every refusal kind and the explicit
  `unexpectedRunnerFailure` input end typed; T3 no repeat of a refused
  attempt while the digests of the inputs it names are unchanged, typed
  exhaustion when no alternative exists; T4 a field with an eligible action
  issues an action request or ends in a typed routed failure; T5 an ask or
  read only for a target in the pinned field with an eligible
  information-gathering action, its request and outcome retained on every
  path; T7 pure step, typed effect requests, only a matching typed result
  advances state; T8 terminal stability under duplicate delivery.
  Conditional: T6 bounded progress per fixed-input episode, and an
  effectiveness theorem stating which useful outcome follows from
  `WellSpecifiedFor action task` (D0) and contract-conforming results.
  T0 and "parametric in content" are withdrawn.
- **C3**, hypotheses, in one readable file: seat, store and review results
  as closed sums; liveness assumptions named (every effect eventually
  returns a result, deadlines produce timeouts, finite field, finite
  retries).
- **C4**, binding, in this order: reducer and wire schema → trace replay
  against hermetic traces of the current runner (shows where it diverges) →
  a reducer artifact generated from Lean and hash-checked, interpreted by
  Clojure, with each old lifecycle branch deleted as its phase is replaced →
  replay kept as an independent check.
- **C5**: Plan A rows each cite the transition they implement and the
  theorem they serve.

What Lean cannot deliver (adopted from codex-6, 46b4afa41): that every
Clojure branch is covered by the event schema; that an adapter did what it
reports; that the live field is complete; that text was tagged honestly;
that a seat ever replies; that an action is useful or well chosen; that the
checker ran on every tick. These are obligations on C4 and its tests.

## Plan A. Actions (rulings 1, 2)

Derived from the model after C1–C2, in codex-6's dependency order, with the
splits and amendments of 3ca1f852c: (1) terminal receipt and binding
integrity; (2) one enumerator, one classifier in use, target manifest,
empty-field failure; (3) refusal memory and routing, as an observation
recorded against `(target, action, input-digest)`; (4) common action-field
schema and one scoring path; (5) `read-criteria` end to end, then removal of
ask-on-abstain; (6) held-out criteria extraction comparison, ids frozen
first; (7) Tornhill and `write-algorithm`, each split; (8) the four
self-model steps ⟨2⟩1–⟨2⟩4. Rows (4)–(5) are reworded once Joe answers the
open question under D1. No row's acceptance uses a click.

## Plan B. Verification before any click (ruling 3)

- Selection evaluation in a fresh test JVM over captured, hashed inputs,
  with every outside write and dispatch set to fail. Nothing is called in
  the serving JVM to find out whether it has side effects.
- Hermetic full ticks with every port enumerated; per new action, raw
  recorded replies through the real parser, and a real temporary git
  transaction for commit and reviewer claims.
- Sequences of consecutive hermetic ticks over the same stores, including
  the changed-input-digest case.
- Fixtures at, below and above each D0 boundary (this replaces the
  universal XXXX run).
- The futon3c boundary test without calling `click!`.
- Named bad cases constructed: unused classifier, two divergent
  enumerations with equal counts, unreachable commit, same author and
  reviewer, repeated unchanged refusal, unexpected external write, empty
  real enumerator.
- Loaded-code identity by source digests, read without reloading.
- Expectations committed before each run.

## Records for Joe

- **Record 1, lifecycle/skeleton verification**: D0, D1, I0, M, the Lean
  lifecycle and its binding, skeleton rows, D0 boundary fixtures, and
  conditions 3–9 of 3ca1f852c. It states that it makes no claim about
  curiosity, self-model coverage, structure learning or good AIF choice,
  and is not acceptance of PROOF-2b.
- **Record 2, model/selection verification**: ⟨2⟩ and RQ-3/4/5.

Whether any click follows either record is Joe's decision.
