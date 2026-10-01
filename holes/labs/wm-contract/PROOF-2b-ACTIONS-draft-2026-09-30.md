# PROOF-2b: agreed-candidate plan (actions, verification, Lean)

Date: 2026-09-30. Parties: claude-1 and codex-6. Status: **RULINGS AND
WORKING NOTES**, not an agreed build plan. Joe's rulings 1–14 below govern.
The negotiation with codex-6 closed on 2026-09-30 with their verdict
a7de6a82f ("yes after five amendments") on a version that rulings 13 and 14
then overtook. Disposition of the five: (3) the thin-path question is
deleted, as is the path; (4) Plan C has fewer selection preconditions, not
necessarily fewer phases — accepted; (5) `question` is a seat result
everywhere (D0, C3, the Lean result sum) — accepted; (1) continuing-run
identity versus per-invocation identity and (2) persisting the last
completed transition were written for a run resumed from saved files, and
are re-examined when codex-5 reports on the debugger, which keeps a failed
run waiting in-process instead. What is being built now: the debugger on the
real machine (typed conditions and restarts at `run-phase!`, codex-5), then
the known failure types raised and handled through it, one at a time, by
the component owners in `WM-TEAM.md`. The history is in git: this file
(78a06c752 …) and codex-6's position paper
`PROOF-2b-ACTIONS-codex-6-position-2026-09-30.md`.

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

9. (Answer to the open question, later the same day.) Every open mission,
   excursion or ticket is wanted; any one of them would be fine to work on.
   Choosing on "formal wants" is the self-constructed difficulty: the
   machine spends its effort deciding among 600 open tasks, then completes
   nothing. An agent may choose among the open tasks; better, an active
   inference model of the open tasks, the completed work and agent
   behaviour should make the choice easy. Joe's own choosing is a small
   model of a few related priorities plus familiar patterns, many of which
   are available in written form. An agent can always ask what a task means
   in context.

### What ruling 9 changes

- **No admission gate.** The field is every open task, and each has at
  least one action from the start: hand it to an agent with "make progress
  on this, or say what you need to know". A question is a valid outcome and
  is recorded on the task. Wants, class, relations and interpretations
  become evidence that sharpens the scores when present; their absence
  removes nothing. This is `PROOF-2b.md`'s own Design section (one softmax
  over the whole field, support never empty), which was not what got built.
- **What active inference says about the behaviour so far.** Not acting is a
  policy, and it has the worst expected free energy available: it realises
  none of the preferred outcomes (risk) and observes nothing (zero epistemic
  value). Not knowing how a task will go is a reason to act on it, because
  acting is what resolves that uncertainty, and it is the only way the
  model's counts over task kinds and agent behaviour get any data. When the
  scores of many options are close, the posterior over policies is flat and
  the choice falls to the prior over policies, i.e. habit: here, Joe's
  stated priorities and written patterns. The machine instead treated
  "cannot score this" as "may not act on this", which inverts the theory.
- **The model is small.** Per task: a knowledge/progress state and outcome
  counts. Shared: a few priorities from Joe (stated preferences), counts
  per (task kind, action, seat) → outcome learned from completed work, and
  the written patterns as the habit prior. No per-task formal translation
  is required before the first action.
- **Consequences for the rest of this plan.** D1b is no longer a side
  measurement: handing an open task to an agent with a plain prompt IS the
  machine's first action kind. Plan A rows (4)–(5) become: the whole-field
  softmax with that action, then counts learned from outcomes. D0's
  code-demands column is mostly a removal list (ask-on-abstain, the
  interpretation validation gate, class-unknown declines, feature-card
  gating). Plan C is unchanged in form and smaller: fewer phases to model.
  Incidents and the verification plan are unchanged.

10. Joe's preference now: the machine runs end to end without getting hung
    up on irrelevant details. Cτ must be taken into account: there are no
    outcomes of any kind until the machine works, so until then the only
    thing worth preferring is progress toward a first complete run. Work
    that does not shorten that path is not on it, however correct.

11. Run the machine in the forward direction until it works, instead of
    resetting it each time and failing each time.

12. Just begin. Which repository or priority a task belongs to does not
    need managing to get the machine to work: every issue is on the list,
    none has been addressed, and once the machine works they all get
    addressed. Do not optimise the choice; start.

    Taken as the go-ahead to build the critical path now (2026-09-30),
    without further rounds on the model. Priorities are out of the first
    machine entirely. Build log: `PROOF-2b.md` LOG.

13. If the forward machine works, it should be no different from the
    regular machine being run in a debugger.

    Correction this forces (claude-1, same day): handoff F1 was dispatched
    as "a new thin path beside the existing runner". That is a second
    machine, and at the end the regular one would still not run. There is
    ONE machine. The step function is the regular machine's controller:
    its phases are the regular run's phases, its effects are carried out by
    the regular runner's own functions (selection, dispatch, wait, review,
    close, record), and "forward" is what a debugger gives: execute one
    phase, save the state, stop at a failure with everything inspectable,
    continue from that phase after the repair. The no-gate choice is a
    change to the regular machine's selection, not a separate chooser.
    F1's result is kept only as that controller: its built-in chooser
    becomes a requested effect answered by the regular selection, and F2 is
    the binding of effects to the existing runner's phases, deleting the
    old control flow phase by phase (Plan C, C4), not new parallel code.

14. Delete the thin path; it is dangerous. Each failure of the REAL machine
    is a critical incident, and there are enough tools to address it. What
    is needed is the debugger: not a thin path, cheat codes, optimisations
    or anything else. After months of work, IT has to work.

    Done 2026-09-30: handoff F1 cancelled (invoke-…28830, operator-cancelled),
    `wm/forward.clj` and its test deleted uncommitted. The sections "The
    small model" and "Critical path" below are NOT a build plan: no separate
    chooser, controller or path is built from them. The work is:

    **The debugger on the real machine.** Run the regular click; at a
    failure it stops at the failed phase with its state kept and
    inspectable; the failure is handled as an incident (ruling 6) with the
    existing tools; the same run continues from that phase. First step is
    discovery, no code: what the runner's phases and checkpoints already
    keep, and what is missing to stop at a phase and continue from it.

15. One click is granted under the new system, and it is to be pushed
    through end to end.

    How claude-1 will spend it: it is not fired until (a) the debugger is
    loaded in the serving JVM and attached, and (b) the failure already
    known to lie ahead (the author's typed refusal of clicks 19 and 20) is
    handled, so the click is not spent rediscovering it. Once fired, the
    run is never abandoned: at each failure it waits at the failed phase;
    the failure is recorded as an incident and assigned per `WM-TEAM.md`;
    the repair is reviewed and loaded; the same run continues. "End to end"
    means this one run reaches a reviewed, grounded change with a terminal
    receipt naming its commit. There is no second click.

16. **Outcome preferences are the selection authority.** Use the small-model
    ordering `closed > progressed > question > refusal > no usable reply`.
    Until the first end-to-end run completes, Cτ makes an outcome that
    completes that run dominate the other outcomes. Predicted likelihood of
    completion is evidence for scoring these outcomes; it is not a replacement
    objective. A cascade may contribute evidence about an action's predicted
    outcome, but no legacy cascade preference model chooses independently or
    gates an otherwise eligible task. Such a parallel chooser would be an
    operational facade.

17. **A first run is a milestone, not the success criterion.** A run that
    ends in a properly recorded question, updates the task state and writes
    its terminal receipt counts as a first end-to-end run: the lifecycle has
    run. It is not thereby a successful War Machine or acceptance of
    PROOF-2b. An answer must make the question actionable by a later run; the
    machine must continue rather than stop after asking once. Reliable
    long-term behaviour is demonstrated by completing work across missions,
    excursions and tickets, not merely by producing well-typed questions or
    by making one run succeed.

18. **Capability requires cross-kind closures; reliability requires more.**
    The finite PROOF-2b capability validation requires the machine to close
    at least one mission, one excursion and one ticket. These closures may
    occur in separate campaigns. They are a floor demonstrating capability,
    not evidence by themselves of reliable long-term behaviour. Operational
    reliability is evaluated over subsequent campaigns and must not be
    reported as established by the three validation closures.

19. **Reliability includes resource efficiency.** Evaluate operational
    reliability over a rolling five-campaign window (100 clicks). Report not
    only closures and progress, but elapsed wall-clock time and token usage.
    At minimum the window records: closures by task kind; tasks progressed by
    kind; closure-or-progress rate per click; elapsed time per closure and per
    closure-or-progress outcome; tokens per closure and per
    closure-or-progress outcome; unresolved questions; repeated refusals; and
    backlog change. A machine that eventually closes work while consuming
    excessive time or tokens does not pass merely because its closure count is
    nonzero. Absolute or comparative efficiency thresholds remain to be set
    from measured runs, not invented here.

20. **Measure efficiency with paired, pinned work.** Preregister one bounded
    mission, one excursion and one ticket, including their acceptance criteria
    and the exact repository commits from which work begins. Complete each in
    an ordinary interactive agent session while recording outcome, elapsed
    wall-clock time and token usage. Then run the War Machine on the same item
    from the same pinned state in a separate isolated worktree and record the
    same measures. Do not destructively rewind the main worktree, and do not
    give the War Machine the direct session transcript or its resulting patch.
    Compare like-for-like successful outcomes and report orchestration overhead
    explicitly. The paired sample calibrates efficiency thresholds; it does
    not replace the rolling 100-click operational report. Candidate selection
    and both benchmark arms are deferred until after the regular machine has
    completed real work. No benchmark candidate may shape the implementation
    or pre-click verification.

21. **The generative model precedes BMR.** The working generative model for
    task choice is the categorical outcome model whose Dirichlet counts are
    keyed by `(task-kind, action-kind, seat)` and updated from every typed run
    result. BMR is optional downstream model comparison or compression after
    sufficient evidence; it neither supplies the model nor substitutes for
    posterior learning. It may not gate action, remove a newly encountered
    task, or prevent model expansion. Any reduction names the compared models,
    evidence window and delta-free-energy threshold and preserves a replayable
    unreduced state.

    Historical provenance: PROOF-2a Plan item 6B-4 used BMR narrowly to choose
    among learned, status-tied and hand-set likelihood rows per scan channel,
    after an exposure floor of 20 admitted ticks and with `ΔF <= -3`. That
    channel-level adoption rule was not an alternative definition of the War
    Machine's generative model. Treating it as one was a scope error.

22. **A disconnected collection is a degenerate cascade, not free
    parallelism.** Retrieval may begin with a collection of applicable
    patterns and that collection meets the working input definition. Cascade
    interpretation must then expose the relations that connect it: precedence
    comes from the interpreted ordering, while genuine incomparability remains
    visible. A multi-unit candidate with no precedence edges is permitted as a
    degenerate baseline for comparison, but its G must be high relative to a
    warranted connected ordering because the ordering is compression. It must
    not acquire a low G merely by treating every unit as a root and proposing
    to apply all of them together. Co-application describes the execution of a
    genuinely enabled incomparable frontier inside an interpreted structure;
    it is not the semantics of an uninterpreted bag. A one-unit cascade is
    trivially exempt from the multi-unit degeneracy.

    The current Lean real example does not yet establish this requirement: its
    three graph retractions have five units and empty `precedes` sets, and the
    proved count treats them as valid policy members. They may remain as
    degenerate comparison members, but acceptance additionally needs a G-level
    theorem/control showing that a warranted connected interpretation outranks
    the disconnected version for the compression reason, rather than because
    edge tokens were added by hand.

### What ruling 11 changes

Every click so far began a new run from nothing: new opportunity id,
readiness checks, selection from scratch. What the previous run had reached
(a target chosen, interpretations published, an author's refusal) was not
the starting state of the next. So each failure discarded the run, and the
next click had to get past every earlier step again before it could reach
the step that had failed.

Forward means one continuing run with a durable state: the field, the
chosen task, the phase reached, and the results so far. An invocation
advances that state by one step. A failure leaves the state at the failed
step; the incident is handled (ruling 6); the same run then continues from
that step. Nothing already achieved is redone. This is also the shape of the
Lean model (a state and a `step`), so the model, the verification and the
running machine describe the same thing: item 8 below verifies `step`
applied repeatedly to one persisted state, not N independent ticks.

It needs two things the runner does not obviously have (to be confirmed
against the code by codex-6): the run state persisted at each phase
boundary, and an entry point that continues from a persisted state.

## The small model (Joe's, 2026-09-30; this is the specification, not a search target)

Joe: "I have told you the small model in the last few chat messages ... If
there were any functions that provided it we would not be having this
conversation." The question to codex-6 about which existing functions
provide it is WITHDRAWN. Nothing provides it. It is built, from this:

- **What it is a model of**: the open tasks, the completed work, and agent
  behaviour. Nothing else.
- **Tasks.** Every open mission, excursion and ticket is in it and is
  wanted. Per task: its kind and a state: untouched / question outstanding / in progress / refused on
  a stated basis / closed.
- **Outcomes** of handing a task to an agent: closed / progressed / asked a
  question / refused / no usable reply.
- **Agent behaviour.** Counts of outcomes per (task kind, action kind, seat),
  starting flat and updated after every run (Dirichlet counts). Completed
  work is where these counts come from.
- **Preferences (C).** Closed is preferred to progressed, progressed to a
  question, a question to a refusal, a refusal to no usable reply. Tasks that
  serve Joe's stated priorities weigh more: finishing the War Machine; work
  that serves the consulting business. HOW a task is known to serve a
  priority is not yet specified (claude-1 first wrote "project, from its
  file path", meaning the repository; that was an invention and is
  removed). Until it is specified, priorities do not enter the score. Cτ: until a run has completed end to
  end, an outcome that completes a run outweighs everything else, so the
  first choices favour tasks most likely to complete the run. Likelihood is a
  prediction used to evaluate the outcome preference, not itself C.
- **Curiosity.** A cell of the counts with little data has more to teach,
  so trying it scores better (information gain). Not knowing how a task
  will go counts for choosing it.
- **Habit (the prior over choices).** Joe's written patterns, read
  directly: for instance, continue what is in progress before starting
  something untouched; stay with the current focus.
- **Choice.** Score = expected shortfall from C, minus information gain,
  with habit as the prior; one softmax over all open tasks. Close scores
  are settled by habit or a draw. There is no case in which nothing is
  chosen.
- **Questions.** An agent that does not know what a task means in context
  asks. The question is recorded on the task; answering it (from the
  patterns, from Joe's mined chats via M-象-cascade, or by Joe) is itself a
  task state the next choice can act on.
- **Update.** After each run: the task's state, the counts, and the
  refused-on-this-basis mark.

Formal wants, classes, relations and interpretations are not part of this
model. Where they exist they may later refine the predicted outcome for a
task; the model does not wait for them.

## Critical path to a first end-to-end run (rulings 10 and 11; governs the order below)

One complete run means: a task is chosen from the open field, an agent is
handed it, the agent's result is reviewed, one receipt is written, and the
next choice takes that result into account. Nothing else is required of the
first-run milestone. A question may be that result, provided the task state
and receipt retain it and a later run can consume its answer. This milestone
does not accept PROOF-2b: acceptance additionally requires sustained completion
across missions, excursions and tickets. Capability validation requires one
closure of each kind, which may be accumulated across separate campaigns;
long-term reliability remains a distinct operational claim. Each item below
is on the path only because the run cannot
happen, or cannot be trusted per rulings 3, 4 and 6, without it.

1. **Field**: every open task, by the open/closed rule. No admission gate.
2. **Choice**: one softmax over the field using the outcome order `closed >
   progressed > question > refusal > no usable reply`, predicted from outcome
   counts that start flat. Until the first run completes, completing that run
   dominates via Cτ. Flat scores are not a reason to stop: the habit prior or
   a draw decides. Legacy cascade preferences neither choose nor gate; cascade
   analysis can only supply evidence to the outcome prediction.
3. **Act**: hand the task to an author seat with a plain prompt (the task
   file; "make progress, commit, report; or say what you need to know").
4. **Result**: exactly one typed result (changed / already satisfied /
   refused / question / invalid / timed out). A question or refusal is
   recorded on the task and is an outcome, not a stop.
5. **Review** by a different seat, for a change.
6. **Receipt and update**: one terminal receipt; counts updated; a refused
   or unanswered attempt is not re-chosen while its inputs are unchanged.
7. **Lean**: this six-phase loop as the lifecycle model, with the safety
   theorems (one receipt; every result typed; no unchanged repeat; a
   non-empty field always yields an action or a typed failure).
8. **Verification without a click**: the loop run hermetically over a
   captured snapshot of the real open field with stub seats, several ticks
   in sequence, expectations committed first; then the record goes to Joe.

Off the critical path until a first run has happened (kept in this plan,
ordered after it): wants derived from checkboxes, interpretation requests
and their validation, relation/class extraction, cascade construction as a
precondition for acting, criteria extraction against the survey, Tornhill,
`write-algorithm`, the self-model steps ⟨2⟩1–⟨2⟩4, the full 19-field content
inventory, parallel asks and the other speed work of 2026-09-30, and the
repair of any existing gate that the path above does not pass through.

## Order of work

The critical path above first. Then, for everything off it:

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

**Question put to Joe, ANSWERED by ruling 9 (yes, and the model should make it easy):** in the REPL Joe
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

Amendments accepted from codex-6 (d6abb23b1): three incidents are
reclassified. Stale-basis reselection is a **model** gap (the lifecycle had
no notion of the basis a choice was made on), not a hypothesis gap. The
`no-execution-evidence` failures of valid answers are a **binding** gap (the
adapter misreported), not a hypothesis gap. The record refresh ordered after
timeout-prone work is a **model** gap (effect ordering was unmodelled), not
a hypothesis gap.

D0 gains a **disposition** column, one of: agent need; machine invariant
(identity, basis digest, reviewer distinct from author); selection-quality
input (improves the choice, never required); removable demand.

Agents may generate typed proposals and the AIF terms rank them; an agent's
choice never removes a task from the common field.

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

The paired efficiency benchmark in ruling 20 follows a working machine; it is
not part of pre-click verification and no candidates are selected here.

## Records for Joe

- **Record 1, lifecycle/skeleton verification**: D0, D1, I0, M, the Lean
  lifecycle and its binding, skeleton rows, D0 boundary fixtures, and
  conditions 3–9 of 3ca1f852c. It states that it makes no claim about
  curiosity, self-model coverage, structure learning or good AIF choice,
  does not establish reliable completion of missions, excursions and tickets,
  and is not acceptance of PROOF-2b.
- **Record 2, model/selection verification**: ⟨2⟩ and RQ-3/4/5.

Whether any click follows either record is Joe's decision.
