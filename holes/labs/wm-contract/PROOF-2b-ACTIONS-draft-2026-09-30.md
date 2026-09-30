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
