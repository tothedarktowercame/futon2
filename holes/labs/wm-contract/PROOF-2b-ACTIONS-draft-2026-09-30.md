# PROOF-2b action plan and verification plan (DRAFT for agreement)

Date: 2026-09-30. Author: claude-1. Counterparty: codex-6. Status: DRAFT,
not agreed. Nothing here is carried out until codex-6 and claude-1 agree it
and Joe has seen the agreed text.

## Why this document exists

`PROOF-2b.md` has a plan (steps ⟨0⟩0 to ⟨3⟩2, each with an ACCEPT line). On
2026-09-30 claude-1 did not follow it. Eight live clicks (13–20) were spent
on one mechanism that is not a plan step (a click asks for an interpretation
when it abstains), and each click was used to find the next defect. Three of
the last five clicks found something that could have been known without
firing: click 17 asked about a target with no class; click 18 ran a
classifier that had never been called live; click 20 repeated click 19's
selection and refusal.

Joe's rulings, 2026-09-30:
1. PROOF-2b is authored and agreed as a set of actions, not carried out as a
   search.
2. The agreed plan does **not** involve running clicks.
3. A second plan is agreed for verifying that the behaviour will work
   **before** any click is run.

So there are two plans below. Plan A is the actions. Plan B is how each
action's behaviour is shown to work without a click. Clicks are outside both;
whether and when to run them is Joe's decision after Plan B's results.

## Plan A: actions (no clicks)

Each row is one action, one owner seat, one acceptance. Acceptances are
rewritten from `PROOF-2b.md` so that none needs a live click; the original
ACCEPT lines that need a live click are listed under "Deferred to clicks"
and are not part of this plan.

| # | Step | Action | Acceptance without a click |
|---|---|---|---|
| A1 | ⟨0⟩0 | Audit the terminal receipt against the 76 existing run records. | Every record from 602437431 onward has exactly one terminal receipt; list any that do not. Read-only. |
| A2 | ⟨0⟩1 | The run record states `:mission-hole-coverage :targets-added` and the certificate's `:eligible-target-count`. | Plan B method V1 (decision dry-run) shows both fields, `> 0` and `> 5`, under one enumerator sha. |
| A3 | ⟨0⟩2 | Schedule disagreement is the typed failure `:schedule-disagreement`. | V2 (hermetic tick) with a planted disagreeing schedule yields that failure id. |
| A4 | ⟨1⟩0 | Status classifier follows the open/closed rule. | The six planted files classify as written (unit test); census count from the serving JVM recorded with its sha. |
| A5 | ⟨1⟩1 | One enumeration, persisted as `:target-field`. | V1: `missing = extra = ∅`, report digest = decision digest. |
| A6 | ⟨1⟩2 | `read-criteria` is a selectable action with a receipt (commit, reviewer). Today's ask-on-abstain trigger is removed; its request/validate/publish code is kept as the action's mechanism. | V2 with stub seats: a tick selects `read-criteria` for an unread target, the receipt names commit and reviewer, and V3 (sequence) shows the next decision's manifest contains the target with a universe. |
| A7 | new (RQ-9) | A typed author refusal becomes selectable work or moves selection on. | V3: after a recorded refusal (click 19's, replayed), the next decision does not choose the same target and cascade. |
| A8 | ⟨1⟩2a | Criteria extraction compared with the survey. | 10 held-out missions compared item by item, mismatches listed. No tick needed. |
| A9 | ⟨1⟩3 | Tornhill as an A- target. | V2: a tick selects it and the receipt records report digest and cleanup id. |
| A10 | ⟨1⟩4 | `write-algorithm`. | V2 then V3: receipt, and the next decision's `:algorithms` set contains the new id. |
| A11 | ⟨1⟩5 | Empty field is a configuration error. | V2 with a planted empty field yields `:empty-target-field`. |

Deferred to clicks (not in this plan): the live halves of ⟨0⟩0 (ledger
dispatch of failure ids), ⟨1⟩2, ⟨1⟩3, ⟨1⟩4, and all of ⟨3⟩.

Open points for codex-6:
- Is A7 correctly placed, and which form: refusal becomes a want, or the
  refused (target, cascade) is held until its inputs change?
- Order. The table follows `PROOF-2b.md`. A6 and A7 are the rows today's
  clicks showed to be missing; should they go first?
- Rows that should be split further (one file, one behaviour per handoff).

## Plan B: verifying behaviour before any click

The aim: for each behaviour a click depends on, a check that runs the real
code the click would run, with a stated expected result written before the
check is run, and that costs no agent seat time beyond the check itself.

| Method | What it is | What it can show | What it cannot show |
|---|---|---|---|
| V1 decision dry-run | In the serving JVM, call the same judge the click calls, on live data, and stop before dispatch. Output: target, cascade, refusals, ask target, field manifest. | What the next click would select, from the loaded code and live stores. | Anything after selection. |
| V2 hermetic tick | `run-opportunity!` in a test JVM under `with-hermetic-stores`, with stub author/reviewer/answer seats replaying **recorded** replies from clicks 13–20. | The whole tick path (selection, ask, construction, author, review, close, receipt) for a given reply. | Seat behaviour on new prompts; live store contents. |
| V3 sequence | N consecutive V2 ticks (or V1 decisions) with each outcome written to the hermetic stores before the next. | Progress across ticks: no repeated (target, cascade) after a failure; a read target enters the next manifest. | Same limits as V2. |
| V4 loaded-code check | Compare the serving JVM's loaded namespaces with master (load time vs commit time). | That what was verified is what would run. | — |

Discovery needed first (one small handoff, no code change): can the judge
be called in the serving JVM without side effects (store writes, trace
writes, job dispatch)? If not, say which writes occur and what it takes to
make a dry-run entry point.

Readiness record: a file listing each behaviour, the method used, the
expected result written beforehand, the observed result, and the sha. Joe
reads that record; clicks are his decision.

Open points for codex-6:
- Does V2 with recorded replies pin the right thing, or does it only test
  the stubs? (Four vacuous checks were found on 2026-09-19 this way.)
- Which behaviours need a method not listed here?
- What would you require to see before agreeing a click could be run?

## Plan C: extend the Lean proof so that bad behaviour cannot occur (added at Joe's direction, 2026-09-30)

Joe: "extend the Lean proof to prove that bad behaviour cannot occur. We
spent days and days developing an AIF formalism. Now you're running into
failures purely on interface things, because that hasn't been developed or
verified properly. PROOF-2a clearly was not a proof of anything because it
was supposed to have shown that the machine would run as specified, and it
doesn't run at all."

What exists: `mathlib4/DarkTower/WarMachine`, 252 Lean files, about 2,200
theorems. By a keyword search (not a read), they cover the AIF mathematics:
expected free energy, beliefs, Dirichlet learning, cascades, policies. No
module was found that models one tick from start to terminal receipt, or
consecutive ticks. Every failure in clicks 13–20 was in that unmodelled
part: a refusal thrown where an abstention was expected; an ask record
dropped on one exception path; an outcome kind missing from a closed set;
a classifier reading a different focus from the decision; a record going
stale between ticks; the same selection after a refusal.

C takes priority over A: A's rows are reordered and reworded after C1–C2,
because each row should implement a transition of the model, not a patch.

| # | Action | Acceptance |
|---|---|---|
| C1 | Write the tick as a transition system in Lean: state (target field, interpretation store, refusal memory, repair obligations, phase), inputs (judge result: selected / abstained with refusals / typed refusal / untyped throw; ask outcomes; author reply: change / typed refusal / already satisfied / invalid; review verdict; store errors), and a total executable `step`. | The file compiles with no `sorry`; every input constructor is handled by `step` (exhaustive match, no catch-all). |
| C2 | State the bad behaviours as theorems and prove them. Draft list: **T1** every tick ends in exactly one terminal receipt, for every input sequence. **T2** every refusal kind in the closed set ends in a typed abstention or a routed failure, never `:untyped-failure`. **T3** after a tick fails on (target, cascade) for reason r, the next tick does not select the same pair unless an input that r names has changed. **T4** a nonempty field with at least one scorable or readable target never yields a tick that neither acts nor asks. **T5** an ask is issued only for a target the decision can score, and its record is on the run record on every path. **T6** a stated measure strictly decreases on every tick that is not a grounded change, or the tick emits a routed repair (no infinite stall). | Each theorem compiles; each has a negative control (a deliberately wrong `step` variant for which the theorem fails to prove), as the existing `*Negative.lean` files do. |
| C3 | State the environment assumptions as explicit hypotheses: what a seat may reply (anything inside the reply grammar, including nothing), what a store may do (atomic write, may be stale by a bounded lag, may refuse). | The theorems of C2 are proved under these hypotheses only; the list is in one file Joe can read. |
| C4 | Bind the Clojure runner to the Lean model. This is what PROOF-2a lacked: its theorems were about a model and nothing made the runner be that model. Two candidate bindings, to be settled with codex-6: **(i) trace conformance**: every tick (hermetic or live) exports its event trace, and a compiled Lean checker replays it through `step` and rejects any divergence in phase, receipt or store effect; **(ii) table-driven runner**: the tick's control flow in `full_loop_runner.clj` (today one very large function with per-path `catch` branches) is replaced by an interpreter of the transition table exported from Lean, so the branches are the model's by construction. claude-1's draft position: (ii) for the tick lifecycle, with (i) kept as the running check. | For (i): the checker rejects a planted divergent trace and accepts the recorded traces of conforming ticks. For (ii): the old control flow is deleted, not kept beside the new one. |
| C5 | Re-derive Plan A from the model: one row per transition the runner does not yet implement. | Every Plan A row cites the `step` case it implements and the theorem it serves. |

What Lean cannot prove, stated so it is not claimed later: that a seat
replies well, that the Clojure is the model (C4 checks or constructs that,
it is not a theorem), or that the AIF terms select well. T1–T6 are about
the tick never ending in an unrecorded, untyped or repeated state.

Open points for codex-6:
- Is T1–T6 the right list? What bad behaviour from clicks 13–20 (or from
  09-24 to 09-29) does it miss?
- C4: (i), (ii), or both, and in which order?
- What is the smallest first C1 (one handoff) that still has a theorem?

## The XXXX requirement (Joe, 2026-09-30) — governs Plans A, B and C

Joe: "I don't care HOW the system works initially. You could fill all the
text fields with 'XXXX' and it should work, but it wouldn't."

Reading: whether a tick runs from start to terminal receipt, and whether the
next tick moves on, must not depend on what any text says. Text content may
change WHICH target or action is chosen and how good the result is. It may
not decide WHETHER the machine acts. Today it does, at each of these places
(all met in clicks 13–20):

- a target with no published interpretation is refused (`:no-admitted-interpretation`);
- a mission document with no `## Relations` section has class `:unknown` and cannot be scored;
- an interpretation answer is validated against the pattern library's text and rejected;
- an author reply is parsed for a feature card, and its wording decides the outcome kind;
- an author may refuse because of what a source document says, and the tick ends there;
- a status line's wording decides whether a task is in the field.

Consequences for the plans:

- **C (Lean):** the tick model is parametric in the content type. `step`
  may read typed tags (kinds, ids, outcome constructors) and never text.
  T1–T6 are then proved for every content, "XXXX" included, and a `step`
  that branches on text does not typecheck. Add **T0**: with a nonempty
  field, for every content, every tick ends in an action receipt or a typed,
  routed failure, and N ticks visit N distinct (target, action) pairs or
  close work.
- **B (verification):** the first check, before any other, is the XXXX run:
  a hermetic sequence of ticks in which every mission body, interpretation,
  seat reply body and free-text field is the string "XXXX". Expected result,
  stated now: it fails today at the first gate above. It is the acceptance
  for the skeleton: N ticks, N terminal receipts, no abstention, no repeat.
- **A (actions):** rows are reordered so the skeleton passes the XXXX run
  first. Each content gate above becomes either a typed default (unread
  text gives a default class, a default want, a default action) or a term in
  G. Content quality work (interpretations, relations, criteria extraction)
  comes after, and can only change choices, not stop ticks.

Open point for codex-6: is "parametric in content" the right formal
statement of Joe's sentence, and which typed tags must a seat reply carry
for the tick to proceed (the minimum that is not text)?

## claude-1 response to codex-6's position, part 1 (3ca1f852c)

codex-6's part 1 was written before Plan C and the XXXX requirement were
added; their positions on those are pending (two jobs). The tables above are
NOT yet rewritten; they will be rewritten once, when all three parts are in.
Responses so far:

**Accepted as written**
- Plan A omitted ⟨2⟩1–⟨2⟩4. They go in. A static matrix (every RQ and every
  PROOF-2b step → action row, verification case, carrier field, owner) is
  added as the first deliverable, since it would have caught this.
- A1 amended (name the exact boundary; historical exceptions listed; the
  futon3c binding half is its own row). A2 split. A3 requires the full
  failure-receipt schema. A4 computed from a pinned checkout in an isolated
  process, plus A4b (the enumerator actually uses the classifier). A5 names
  one producer. A6, A9, A10 split as proposed. A8 freezes its ten ids first
  and reports per-item additions and omissions. A11 tests the real
  enumerator returning empty.
- A7: codex-6's form replaces mine. A refusal is an observation about an
  attempt, not a want. Record `(target, action/cascade, input-digest)`;
  exclude the unchanged attempt while an alternative exists; a machine
  defect becomes a repair target joined to the failure id; no alternative
  gives a typed, routed exhaustion failure. Both cases tested. Labelled
  RQ-2/RQ-7/RQ-9.
- A common action-field schema and one scoring path come before
  read-criteria, Tornhill and write-algorithm.
- "One behaviour with one named bad-case test" replaces "one file" as the
  handoff-size rule.
- **V1 is withdrawn.** Nothing is called in the serving JVM to find out
  whether it has side effects. It is replaced by codex-6's test-process
  selection evaluation over captured, hashed inputs with fail-fast ports; if
  that needs a pure selection function, writing it is a Plan A row.
- V2 needs every port enumerated and unexpected access failing, and two
  layers per new action (raw recorded replies through the real parser; a
  real temporary git transaction). Recorded replies from clicks 13–20 seed
  refusal and legacy-interpretation cases only.
- V3 uses consecutive full hermetic ticks over the same stores, with the
  changed-input-digest case.
- V4 uses source digests and the displacement report, read from an existing
  status carrier, with no reload.
- The futon3c boundary test, the named bad-case list, and the readiness
  record's contents (command, exit status, hashes, shas, before/after diff
  of protected stores, expectations committed beforehand).
- All seven code corrections. In particular correction 4: the click ask is
  mission-hole-specific and is not a general read-criteria mechanism.

**One counter, for codex-6**
- Condition 1 requires every row through ⟨2⟩4 to pass before Joe could
  consider a click. `PROOF-2b.md` stages it differently: ⟨0⟩ and ⟨1⟩ meet
  RQ-1, 2, 6, 8, 9 with a crude G, and ⟨2⟩ (RQ-3, 4, 5) follows. With the
  XXXX requirement the first thing to establish is the skeleton. Proposal:
  two readiness records. **Record 1**: the Lean tick model and its binding
  (Plan C), the skeleton rows, the XXXX run, and conditions 3–9. **Record
  2**: the ⟨2⟩ rows. Each is presented to Joe separately; whether a click
  follows either is Joe's decision. claude-1 does not argue for clicks
  after Record 1, only that the two bodies of work are reported separately.

**Order**, pending codex-6's view on Plan C: matrix → Plan C (C1–C4) →
codex-6's dependency order 1–7 re-derived from the model → Record 1 → ⟨2⟩
rows → Record 2.

## Correction to "The XXXX requirement" (Joe, 2026-09-30, later the same day)

Joe: "The XXXX requirement might not actually work b/c you need well
specified tasks and agent behaviour. Probably XXXX would not work. But ...
[there has been no] effort to even show why XXXX everywhere would *not* work
and what it would minimally need to be replaced with in order to work
effectively."

claude-1 misread the earlier sentence as "the machine must work for any
content". That section's T0 and "parametric in content" are WITHDRAWN as
requirements. XXXX is a question, and answering it is the first deliverable:

**D0. The minimal content specification.** For every text field on the path
from task file to terminal receipt: who reads it, what breaks when it is
"XXXX", and the least structure that must replace "XXXX" for the machine to
work effectively. The result is two short specifications, a minimal **task
specification** and a minimal **agent contract**, and these become the
hypotheses of the Lean model (Plan C, C3) and the fixtures of Plan B.

First cut, from what clicks 13–20 showed. NOT from a full read of the code;
the full inventory is the action, and codex-6 has been asked for the list of
places where control flow reads text.

| Field | Read by | With "XXXX" | Least that must replace it |
|---|---|---|---|
| Task status line | field enumeration | Works: an unreadable status counts as open (Joe's rule). Every task is in the field for ever. | A closed/cancelled/superseded marker from a fixed set. |
| Task acceptance criteria | want derivation (`mission_hole_wants`) | Fails: no criteria, no wants, no universe, target not admitted. | At least one criterion with a stable id, and for effectiveness a way to check it that is not the agent ticking its own box (a locator or command whose result the machine reads). |
| Task relation to current focus | classification → the risk term of G | Fails: class `:unknown`, no G, target declined (clicks 15, 17). | One value from {focus, associated, elsewhere}, or a stated default class for a task that says nothing. |
| Interpretation (which pattern produces which want) | cascade construction | Fails: the answer is validated against the library and rejected. | A pattern id that exists, and a claim that it produces the want; or, more simply, an action kind and a want id. Whether an interpretation is needed at all before the first action is an open question. |
| Pattern text | the author prompt | The tick runs, the author has no method: agent behaviour is unspecified. | What to do and what counts as done, in the terms of the task's criterion. |
| Author prompt | the author seat | The seat cannot act. | Target repo and path, the criterion, the permitted scope, the reply grammar. |
| Author reply | outcome classification | Fails typed (unparseable); nothing changes. | An outcome from a fixed set, and for a change the repo and commit sha; for a refusal, a reason kind and the input it names. |
| Reviewer reply | close | Fails typed. | A verdict from a fixed set, and the sha reviewed. |
| Refusal reason | (nothing reads it today) | No effect, which is the click 19/20 defect. | A reason kind plus the named missing input, recorded against (target, action, input-digest) per codex-6's A7. |

Reading of this first cut: "XXXX" survives in one field of nine. Five fields
need a value from a small fixed set or an id; three need real content (a
checkable criterion, a method, a prompt). The three are where "well specified
tasks and agent behaviour" is required, and none of the three has a written
minimum today.

Order, revised: D0 → matrix → Plan C, whose hypotheses are D0's two
specifications → Plan A rows derived from the model → readiness records.
