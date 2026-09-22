# Plan: a War Machine worth using (claude-5, 2026-09-22, for Joe)

Joe, 2026-09-22: "I actually want this thing to work. And perform and have behavior that's
worth using. It's not just an exhibit for a museum ... I need to see a plan. Step by step from
beginning to end."

## How we got here

1. **No test of the whole machine against its purpose.** Every paradigm had its own success
   test, and every one of them passed:
   - the DAG: tasks closed;
   - the Lean model: proofs check;
   - the contract bundle: verify PASS;
   - the fix list: merged with tests green;
   - the improve items: receipts recorded.

   None of these asks whether a click chose work Joe wanted and got it done. All of them can
   pass while the machine does nothing useful, and they did.
2. **Almost no feedback on the model itself.** The only signal has been live clicks. They are
   scarce (5 per grant) and slow (hours), and each one fails on apparatus before it reaches the
   model:
   - click r4-1: the reviewer template had the wrong shape;
   - click r4-2: the acceptance needed a file outside the artifact scope.

   Months of work, and the preference and transition parts have almost never been scored on
   anything.
3. **Safety rules became the default.** Record-only slices, byte-identical selection, refusals
   and guardrails are each reasonable. Together they guarantee that behaviour does not change.
   Yesterday's 13 improve slices all left selection byte-identical.
4. **Too few real alternatives.** Click r4-2 had one admitted candidate, and fix-5 found 3
   hand-written cascades while 441 wants yielded none. With one to three options, no
   preference model can matter. G tying at 0.0013 nats and habit deciding follows from this
   together with a near-flat C.
5. **Nobody owned "does it work".** Each agent, including claude-5, owned a slice and reported
   the slice's own test.

## Strategy

Use Joe's own recorded history as the test the machine has to pass, offline, where feedback
takes minutes instead of hours. Fix things in the order that test exposes them. Live clicks
come last, to confirm. The Lean model and contract bundle stay as they are: they check that
the functions we fit are the model's functions. They are not the measure of success.

Imitating Joe's choices is the floor, not the goal. A machine that picks what Joe would pick,
and gets it done without him, is already worth using. Beating his choices (novelty,
capability, closing the backlog) is scored by the same method once the floor is reached.

## Steps (each has an exit criterion; do not start a step until the previous one exits)

**1. Define the job and the score.**
- The job: from the open obligations, choose the next piece of work and get it delivered and
  accepted.
- The score has two parts:
  - (a) offline: agreement with what Joe actually chose, on held-out past decision points;
  - (b) live: the share of clicks that end in an accepted increment Joe answers "yes, I'd
    have picked that" to.
- Joe sets the target numbers.
- *Exit:* one page, agreed by Joe.

**2. Build the evaluation set from the record.**
- Decision points from the last ~6 weeks. Each one has:
  - the state then (open missions/holes/tickets, recent work);
  - the options available;
  - what Joe chose. Sources: commits by mission, clock decisions, operator turns, stance
    labels.
- Split by time: earlier points to fit on, later points to test on.
- *Exit:* at least 200 decision points; Joe spot-checks 20 and agrees the "chosen" labels are
  right.

**3. Baselines.**
- Score four simple rules on the test split: random, most recent, habit, "same as yesterday".
- *Exit:* numbers in a table. Anything the machine does must beat these.

**4. Candidate recall. This comes before preferences.**
- Run the machine's own candidate construction at every decision point, and count how often
  Joe's actual choice is among the candidates.
- Fix construction (tickets, missions, holes, repairs as ordinary tickets) until recall
  reaches the target (proposed: 80%).
- *Exit:* recall target met on the test split.
- If this cannot be met, it is the central problem, and we say so plainly. No amount of work
  on C or B helps when the right option is never offered.

**5. Fit C and B to the record.**
- Fit C from Joe's revealed choices (including his 55/35/5/5 classes through the improve-8
  kernel).
- Fit B from recorded outcomes of past actions.
- Use the existing, contract-verified functions (`TokenPreference`, `PolicyHorizon`,
  `PolicySelection`, `ActionMarginal`).
- *Exit:*
  - G's choice beats every baseline on the test split;
  - most choices are decided by G, not by habit or a tie.

**6. Put the fitted model on the live path.**
- Selection uses the fitted C and B through the same functions.
- Turn off record-only for these parts.
- The contract bundle's holder labels move to live enactment for what selection actually
  calls, confirmed from a click's receipts.
- *Exit:* the bundle shows live enactment for every function the click uses to decide.

**7. Live clicks, scored.**
- After each click Joe answers one question: would you have picked this?
- Apparatus failures (template shapes, scope mismatches) are fixed the same day and are not
  counted against the model. A check that the acceptance's files fall within the artifact
  scope is added at admission.
- *Exit:* the live target from step 1 is met over a run of clicks.

**8. Learning.**
- After each click, update B and C from the outcome.
- Re-score offline: the update should improve test agreement or prediction of outcomes.
- *Exit:* a measurable improvement across a series of clicks.

## Rules while the plan runs

- Nothing is dispatched unless it moves the current step's exit number. No new record-only
  receipts, Lean modules or contracts unless a step requires them.
- One owner holds the score from start to finish and reports it after every piece of work:
  claude-5, unless Joe assigns otherwise.
- Steps 1–5 are offline and need no clicks. Clicks resume at step 7.
