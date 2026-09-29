# PROOF-2b: a War Machine that always acts, on everything unfinished, from a model of itself

Date: 2026-09-29. Author: claude-1, at Joe's direction ("PROOF-2a needs to be
superseded with PROOF-2b that includes more about the actual requirements and
design that adapts AIF to the FUTON stack setting ... we've spent ages
fine-tuning wiring details without seeing any forward motion at all").
Status: DRAFT for Joe.

**Supersedes** `PROOF-2a-THEOREM-draft-2026-09-24.md` and `PROOF-2a-PLAN.md`.
Both stay in the tree as history. Nothing in PROOF-2a gates a PROOF-2b step;
its unfinished rows are carried over under "Residuals from PROOF-2a" and are
done when a PROOF-2b step needs them, not before.

---

## Why PROOF-2a is superseded

PROOF-2a was planned as components, then wires, then a flight: no flight
before every equation, Lean import and wire test was accepted. Its table on
2026-09-29 shows months of that work (194 wire tests, 33 Lean modules, a
wiring map at function grain) and ⟨3⟩, the part where the machine does
something, still "Not started". Meanwhile the live machine selected nothing
on every click from 2026-09-24 to 2026-09-29 except one.

The reason, established on 2026-09-29, was not a wire:

1. **The field it chose from was five declared targets.** The decision
   enumerates open missions and live tickets (264) but admits only targets
   with a fact universe. Universes come from five hand-written
   `resources/wm/cascade-sources/*.edn` files, plus a deriving path from
   mission checkboxes (`mission_hole_wants.clj`). That path generates nothing
   unless the declared files agree on one `:c-schedule`; the T-repair file
   published on 2026-09-22 (22b1971c8) declared none, so from then on the
   derived universes went from 17 to 0 without any failure being reported.
   Four of the five declared targets were then finished. Excursions (373
   live) are not enumerated by the decision at all, although the target-field
   report lists them. Evidence: `NOTE-target-field-coverage-2026-09-29.md`
   (zai-1, 3d6f3adae).
2. **The design had no action for "find out".** Expected free energy is
   computed only over admitted targets (`cascade_decision.clj` ~650); a
   target the machine knows little about is refused before epistemic value
   could favour it. PROOF-2a's own Clause T (2026-09-25) said every task is
   feasible and carries a next step (`:read-criteria`, `:ask-interpretation`,
   `:observe`, `:construct`, `:ready`), but only `:ready` targets could be
   selected: the next steps were written into a report, not made into
   actions.
3. **The generative model does not contain the targets or the machine.** The
   carried belief (`:mu-post`) covers 417 arxana stack claims over seven
   claim statuses. No Mission, Excursion or Ticket is a hidden state, and
   nothing about the machine's own behaviour is. Learning is Bayesian model
   reduction only, which compares reduced priors inside an existing model and
   never adds a state; the model cannot grow to take in new work.

So the machine was correct, click by click, and did nothing. PROOF-2b
states the requirements that rule that out and a design that meets them.

---

## Requirements

Each is a property of every click or of the machine's model, checkable from
the record. Where Joe said it, his words are quoted.

**R1. The field is all unfinished work.** Every unfinished Mission,
Excursion and Ticket in FUTON is a target on every click, plus the standing
Algorithm targets (R6). The decision and the target-field report enumerate
the same set. "The machine should have all unfinished Missions, Excursions,
and Tickets to choose from. The 'five targets' is at best a placeholder for a
complete target, and we should be getting a failure signal on that basis."
*Check:* the click record's field equals the enumerators' count at HEAD; a
shortfall is a typed failure that goes to repair.

**R2. Every click acts.** A click ends with an action taken and its outcome
recorded (a commit, a written artifact, an observation). Selecting nothing is
not an outcome; an empty action set is a configuration error and goes to
repair. "Imagine if YOU decided to abstain from work when I asked."
*Check:* no click record without an enacted action or a typed failure with a
repair route.

**R3. Standing curiosity.** "The machine should represent a standing
curiosity about the world, about its own behaviour, about the FUTON stack."
Uncertainty about a target counts in favour of acting on it: an action that
would resolve it has positive epistemic value, and G includes that term for
every action in the field.
*Check:* on the record, each candidate action's G shows its epistemic term;
a target whose facts are unknown has a gathering action with nonzero
information gain.

**R4. A generative model of itself.** "The machine should have a generative
model of itself (this is standard AIF stuff) that includes its curiosity."
Hidden states cover the tasks (R1), the stack (the existing 417 claims), and
the machine's own behaviour: how its action kinds turn out, which repairs
held, where it gets stuck.
*Check:* the model's state factors name these three groups; the record
carries the posterior over each factor the chosen action touched.

**R5. The model grows as well as shrinks.** A new task file, or an
observation no state explains (a new failure kind), adds a state or factor
instance with a stated prior (expansion). BMR keeps its present job of
removing structure that does not earn its complexity (reduction).
*Check:* a mission created between two clicks is in the second click's model
without anyone declaring it.

**R6. Algorithms are targets.** Standing A- targets are always in the field:
e.g. a Tornhill pass (`futon0/analysis/audits/tornhill.py`: hotspots,
complexity trend, change coupling) whose output is a cleanup the machine then
makes, and `write-algorithm`, which writes a new algorithm and runs it
("then run the new algorithm, etc., providing a paper trail of actions").
They compete in G with the tasks; they are what the machine does when no
task action is better, and so R2 always has something to take.
*Check:* the field lists the A- targets on every click; an A- run leaves a
commit and a record.

**R7. Forward motion is the measure.** A PROOF-2b step is accepted on what
the live machine did: actions taken, wants closed, commits reviewed. Wiring
evidence (wire tests, Lean imports, map entries) is written afterwards, for
what the machine actually used, and never gates a live step.

**R8. A paper trail for every action.** Each action has a record (target,
action kind, G terms, outcome) and, when it changes code or documents, a
commit. The trail is what makes R4's self-model learnable.

**R9. Failures are signals with a route.** A coverage shortfall, a crashed
action, a refused read: each is typed, recorded, and sent to repair by the
click loop (`scripts/wm_click_repair_loop.py`). Nothing is dropped to "not
generated" silently, as the schedule gate did.

---

## Design: active inference adapted to FUTON

The existing machinery is kept where it fits. The cascade decision (patterns
as rewrites over tokens, scored by G) becomes the inner step for a task whose
knowledge is complete enough to construct; it is no longer the whole
decision.

### State factors (what the machine is uncertain about)

- **Task factors**, one instance per unfinished M/E/T/A. States are the
  machine's knowledge of the task and the task's progress:
  `unread → criteria-read → interpreted → observed → constructed →
  in-progress → done`, with `blocked` beside them. This is PROOF-2a's
  Clause T next-step list, made into a hidden state with a belief over it.
- **Stack factors**: the existing 417 arxana claims over seven statuses,
  unchanged (`:mu-post`, the scan learner, BMR).
- **Self factors**: per action kind, the probability it succeeds and what it
  costs; per repair kind, whether it held. These are learned from the paper
  trail (R8) with Dirichlet counts, the same form as the Dirichlet bootstrap
  already written live.

### Actions

For each task, the action that moves its knowledge state on:
`read-criteria` (read the file, write its wants and facts: this creates the
universe the admission gate used to demand), `ask-interpretation`, `observe`
(run the locator checks), `construct-and-enact` (the existing cascade path),
`close`. For each A- target, `run`. `write-algorithm` is one of the A-
targets. Every action runs as an Agency job with author ≠ reviewer where it
changes code, as clicks do now.

### Likelihoods, transitions, preferences

- **A** (what an action reveals): a `read-criteria` observes the task's
  criteria; `observe` observes tokens; a Tornhill run observes hotspot and
  coupling metrics. Where A is unknown, its Dirichlet counts start flat, and
  the novelty term (R3) rewards learning it.
- **B** (how actions move states): knowledge-state steps as above; enactment
  moves progress states.
- **C** (preferences): Joe's stated preferences stay (focused work to
  completion; Cτ from PROOF-2a ⟨1⟩4). Curiosity is not a preference; it is
  the epistemic and novelty terms of G, so it cannot be tuned away by C.

### Selection

G for every action in the field = risk + ambiguity − information gain −
novelty. One softmax over the whole field. There is always at least one
action per task and one per A- target, so the support is never empty (R2).

### Structure learning

Expansion: at each click, the enumerators' new tasks become new task-factor
instances with the prior `unread`; an observation whose likelihood under
every state is below a stated floor opens a new state for the factor it came
from, recorded as an expansion event. Reduction: BMR, as built, over
parameters and over those added states once they have data.

---

## Plan

Structured as PROOF-2a was (numbered claims with an acceptance someone can
check), but every step's acceptance is a live result. Dispatches are logged
under their step.

### ⟨0⟩ Restore what the data slip removed (hours)

⟨0⟩1. The T-repair cascade source declares `:c-schedule` like the other four,
so mission-derived universes are generated again (zai-1's smallest change;
about 20 missions with checkbox holes return to the field).
ACCEPT: the next click record shows `:targets-added` > 0 and a selection
among more than five targets.

⟨0⟩2. The schedule-agreement gate reports its refusal as a typed failure on
the click record, not `:coverage-not-recorded` (R9).
ACCEPT: planting a disagreeing schedule yields the typed failure and a repair
dispatch from the loop.

### ⟨1⟩ The machine always acts (days)

⟨1⟩1. One enumeration: the decision's targets are exactly the target-field
report's considered set (missions, tickets and excursions) plus A- targets.
ACCEPT: live click record field count = enumerators' count at HEAD.

⟨1⟩2. `read-criteria` is an action: selecting it for a task dispatches a job
that reads the task file and writes the task's wants, facts and locators as
a cascade source (committed, reviewed). ACCEPT: a live click takes it on an
`unread` excursion, and the next click admits that excursion with a universe.

⟨1⟩3. Tornhill as an A- target: running it produces the hotspot report and
proposes one cleanup, which a following click can enact. ACCEPT: one live
run, its report committed, one cleanup commit reviewed.

⟨1⟩4. `write-algorithm`: writes a new A- target (a script plus its
declaration), reviewed, and the field includes it on the next click.
ACCEPT: one live instance with its paper trail.

⟨1⟩5. The empty-support case is a configuration error routed to repair.
ACCEPT: a planted empty field produces the typed error; no click record ends
without an action or such an error.

At the end of ⟨1⟩, with a crude G (for instance the existing risk term plus
a constant bonus for gathering actions), every click does something. That is
the first milestone Joe sees.

### ⟨2⟩ The self-model (weeks)

⟨2⟩1. Task and self factors as hidden states with beliefs carried click to
click, beside `:mu-post`. ACCEPT: the record carries the posteriors; a task's
belief moves after an action on it.

⟨2⟩2. Epistemic and novelty terms of G computed from those beliefs and the
Dirichlet A counts, replacing ⟨1⟩'s constant bonus. ACCEPT: the record shows
the terms; on a field with one well-known and one unread task of equal risk,
the unread one scores better (R3), live or on a recorded field.

⟨2⟩3. Expansion (R5). ACCEPT: a mission created between clicks appears in
the next model with prior `unread`; a planted unexplained observation opens a
recorded state.

⟨2⟩4. Lean: the self-model's factor structure and G decomposition stated,
with a correspondence to the running code at the level PROOF-2a reached for
the scan model. Written for what ⟨2⟩1–3 built, after they run (R7).

### ⟨3⟩ Evidence of forward motion (ongoing)

⟨3⟩1. A campaign of N clicks through the click loop. Reported per campaign:
actions taken by kind, wants closed, commits reviewed, tasks moved from
`unread`, algorithms run, repairs. ACCEPT: every click acts (R2), and the
counts of wants closed and tasks read are nonzero.

⟨3⟩2. A flight on one mission to completion, the PROOF-2a theorem's case,
now as one thing the machine does rather than the only thing the plan aims
at.

---

## Residuals from PROOF-2a (carried over, not gating)

| PROOF-2a row | What remains | In PROOF-2b |
|---|---|---|
| ⟨1⟩2 item 6 (b) scan learner | design packet 6 (adopted channels drive `:mu-post`); 20 carrying ticks | Stack factor learning; continues as clicks carry ticks under ⟨1⟩ |
| ⟨1⟩2 ε (eq. 4.13) | producer 4072ff252, no consumer | Decided when ⟨2⟩2 builds G's ambiguity term |
| ⟨1⟩3 seam documents, W_c enactment | S2 seam documents; a live W_c-admitted enactment | Taken up when a flight needs them (⟨3⟩2) |
| ⟨1⟩4 Cτ, preference audit | multi-click progress check live; PREFERENCE-AUDIT.md still DRAFT | C in the design above; finish when ⟨3⟩2 runs |
| ⟨2⟩1 Lean imports | 19 edges not imported | Folded into ⟨2⟩4, for the edges the running design uses |
| ⟨2⟩2b map join | stale join, 5 `:none`, 3 `:cannot-tell` | Regenerated after ⟨2⟩, for the new wiring |
| ⟨2⟩2d missing code dependencies | ε consumer; registry text stale | As ε above; registry text refreshed with ⟨2⟩4 |
| ⟨2⟩3b second-layer wire tests | 51 wires | Written for wires the running design uses (R7) |
| ⟨3⟩1–3 proof flight | not started | ⟨3⟩2 |
| Clause T (target field) | report only | Absorbed: ⟨1⟩1 and ⟨1⟩2 make it the decision |

Completed PROOF-2a rows (⟨1⟩1 equations, ⟨2⟩2a, ⟨2⟩2c, ⟨2⟩3, ⟨3⟩0) stand as
they are.

---

## Open questions for Joe

1. Excursions: all 373 live ones in the field from ⟨1⟩1, or only those with
   a status line (as Clause T required of missions)?
2. `read-criteria` writes cascade sources that the machine then acts on. Is
   a reviewed commit enough authority for that, or should a new task's
   first interpretation come to you?
3. Budget: ⟨1⟩ spends clicks on gathering actions that change documents, not
   code. Do those count against the ordinary click budget as clicks do now?

## LOG

LOG: 2026-09-29 (claude-1). Drafted from the 2026-09-29 session: Joe's
requirements (field coverage, no abstention, A- targets incl. Tornhill and
write-algorithm, curiosity, self-model, BMR's missing expansion half), the
click-loop findings, and zai-1's coverage note (3d6f3adae).
