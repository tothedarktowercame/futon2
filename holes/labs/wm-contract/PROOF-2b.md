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

Numbered RQ-n so they are not confused with the AIF model's R-numbers
(R1–R19); they are not an adaptation of those rows. Each has a metric
computed from a named record, a threshold, and the record field it needs.
The measurement design, with today's values and the commands, is
`NOTE-PROOF-2b-measurable-outcomes-2026-09-29.md` (codex-proof2a, 3631f87a0).
A **campaign** is 20 clicks through the click loop. "Today" is 2026-09-29.

**The action receipt comes first.** RQ-2, RQ-6, RQ-7, RQ-8 and RQ-9 join on
one record: each click ends in exactly one **terminal receipt**, either an
`:action-receipt` (click id, target id and kind, action kind, G terms,
outcome, and for a mutating action the commit sha and reviewer) or a
`:failure` (`:id :kind :source :at`). Both go in the run record and, by
digest, in the click binding; the click loop's ledger cites the failure id.
It is built as step ⟨0⟩0.

**RQ-1. The field is all unfinished work.** Joe: "The machine should have
all unfinished Missions, Excursions, and Tickets to choose from."
*Metric:* each click persists a `:target-field` manifest: ID sets
`:missions :excursions :tickets :algorithms` and the enumerator's HEAD sha,
copied verbatim to the run record and the target-field report. `missing` =
enumerated at HEAD − decision's set; `extra` = decision's set − enumerated.
*Threshold:* `missing = extra = ∅`, and the decision and report digests are
equal, on every click of a campaign. Set equality, not count equality.
*Today:* not recorded. By a read-only census, 221 missions + 373 excursions +
43 tickets are live; the decision enumerated 264 (no excursions) and admitted 5.
*Open vs closed* (Joe, 2026-09-30): a task is open unless its file says it
is closed, cancelled, superseded or abandoned. A missing or unreadable
status line means open, not unknown-and-excluded. A draft is open. Phase
words ("DERIVE-1 DONE", "v0 complete") do not close a task.

**RQ-2. Every click ends in an action or a routed failure.** Joe: "Imagine
if YOU decided to abstain from work when I asked."
*Metric:* `completion = (action receipts + failures with a repair dispatch)
/ clicks`.
*Threshold:* 100% per campaign, and at least one enacted action in the
campaign (a campaign cannot pass by failing 20 times).
*Today:* 6 click bindings, 5 abstained and 1 build-failed; 0 enacted actions.

**RQ-3. Uncertainty about a target counts in favour of acting on it.** Joe:
"a standing curiosity about the world, about its own behaviour, about the
FUTON stack."
*Metric:* every candidate records `:G {:risk :ambiguity :information-gain
:novelty :total}`, and the total equals the declared combination within
1e-12. For every target whose knowledge state is `:unknown`, count the
candidate read-or-check actions (`read-criteria`, `observe`) with
information gain > 0. A paired control
holds risk, ambiguity and habit equal and compares an unread target with a
known one.
*Threshold:* every click, all candidates decomposed and every unknown target
with at least one read-or-check action with IG > 0. The paired control, once per
release: the unread target has the lower G.
*Today:* no candidate decomposition on a click that chose nothing; no
knowledge-state field.

**RQ-4. The model covers the tasks, the stack and the machine itself.** Joe:
"The machine should have a generative model of itself ... that includes its
curiosity."
*Metric:* each click records a versioned factor manifest with a normalised
posterior over every factor, covering the RQ-1 task IDs, a pinned stack-claim
list, and the machine's declared action and failure kinds; each action
receipt names the factors it observed, with their prior and posterior.
*Threshold:* per click, coverage 100% of the three populations and
normalisation error ≤ 1e-12; every enacted action has a factor transition or
a typed no-update. Per campaign, a nonzero transition in each factor family
exercised.
*Today:* `:mu-post` over 417 stack claims and the scan learner's seven
statuses; no task or self factors.

**RQ-5. The model grows as well as shrinks.**
*Metric:* consecutive factor manifests are diffed. An expansion event names
its trigger (new task, or an observation whose highest likelihood under the
old model is below 1e-6) and the new factor or state IDs. A reduction event
carries BMR's ΔF, the threshold, and the IDs before and after.
*Threshold:* once per release, two planted controls: a new task file adds
exactly its factor with prior `unread` on the next click; a planted
unexplained observation yields exactly one expansion event. Removal only with
ΔF ≤ −3.
*Today:* BMR reduces parameters in the scan model; no factor-level expansion
or reduction is recorded.

**RQ-6. Algorithms are targets.** Standing A- targets (a Tornhill pass,
`futon0/analysis/audits/tornhill.py`; `write-algorithm`, which writes a new
algorithm and runs it) are in the field on every click.
*Metric:* the RQ-1 manifest's `:algorithms` set equals a HEAD-pinned
algorithm registry; an algorithm action's receipt names its artifact, commit
and reviewer.
*Threshold:* per click, set difference ∅. Per acceptance campaign, one
successful Tornhill run and one `write-algorithm` run, each with artifact,
commit and a reviewer distinct from the author.
*Today:* no algorithm registry or A- targets.

**RQ-7. Campaigns change the state of the work.** (Replaces "forward
motion", which named no observable.)
*Metric:* per campaign, counts joined to action receipts: enacted actions;
wants flipped false → true; task knowledge or lifecycle steps; algorithm
runs; reviewed commits caused by an action; repairs closed. Wiring commits
count only if a later action receipt cites the wire. Counts are reported
separately, with denominators, not combined into a score.
*Threshold:* RQ-2 met, and at least one of {want closed, task advanced,
reviewed action commit} > 0.
*Today:* 0 on every count. Want and task transitions are not recorded.

**RQ-8. Every action leaves a complete receipt.**
*Metric:* `paper_trail = complete action receipts / enacted actions`, with
each commit sha resolving (`git cat-file -e <sha>^{commit}`) and the reviewer
distinct from the author.
*Threshold:* 100% per campaign; zero enacted actions does not satisfy it.
*Today:* no action receipt exists; the binding has click id, outcome and run
path, and no commit or reviewer join.

**RQ-9. Every failure is typed and routed to repair.**
*Metric:* `untyped = failures without :id and :kind`; `route = failures with
a ledger repair dispatch citing the id / failures`; `closure = dispatches
with a terminal judgment / dispatches`; dispatch latency in seconds.
*Threshold:* per campaign, `untyped = 0`, route and closure 100%, latency ≤
5 min. A no-defect judgment is allowed if it closes the same failure id.
*Today:* the click-loop ledger has 4 abstained clicks, of which 1 was
dispatched and judged (latency 2 s). The other 3 predate the loop's
debugger change (e1a9dd563), and ended at the old stall stop.


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
  trail (RQ-8) with Dirichlet counts, the same form as the Dirichlet bootstrap
  already written live.

### Actions

For each task, the action that moves its knowledge state on:
`read-criteria` (read the file, write its wants and facts: this creates the
universe the admission gate used to demand; the seat interprets the task
as it reads it), `observe`
(run the locator checks), `construct-and-enact` (the existing cascade path),
`close`. For each A- target, `run`. `write-algorithm` is one of the A-
targets. Every action runs as an Agency job with author ≠ reviewer where it
changes code, as clicks do now.

### Likelihoods, transitions, preferences

- **A** (what an action reveals): a `read-criteria` observes the task's
  criteria; `observe` observes tokens; a Tornhill run observes hotspot and
  coupling metrics. Where A is unknown, its Dirichlet counts start flat, and
  the novelty term (RQ-3) rewards learning it.
- **B** (how actions move states): knowledge-state steps as above; enactment
  moves progress states.
- **C** (preferences): Joe's stated preferences stay (focused work to
  completion; Cτ from PROOF-2a ⟨1⟩4). Curiosity is not a preference; it is
  the epistemic and novelty terms of G, so it cannot be tuned away by C.

### Selection

G for every action in the field = risk + ambiguity − information gain −
novelty. One softmax over the whole field. There is always at least one
action per task and one per A- target, so the support is never empty (RQ-2).

### Structure learning

Expansion: at each click, the enumerators' new tasks become new task-factor
instances with the prior `unread`; an observation whose likelihood under
every state is below a stated floor opens a new state for the factor it came
from, recorded as an expansion event. Reduction: BMR, as built, over
parameters and over those added states once they have data.

---

## Plan

Structured as PROOF-2a was (numbered claims with an acceptance checkable
from the record), but every acceptance is a live result or a planted control
on live code. Dispatches are logged under their step.

### ⟨0⟩ The receipt, and what the data slip removed (hours to a day)

⟨0⟩0. The terminal receipt (see Requirements): every click writes exactly
one `:action-receipt` or `:failure` to the run record and, by digest, the
binding; the click loop's ledger cites the failure id.
ACCEPT: over the next 5 live clicks, each click id has exactly one terminal
receipt, and each failure id appears in a ledger dispatch.

⟨0⟩1. The T-repair cascade source declares `:c-schedule` like the other four,
so mission-derived universes are generated again (zai-1's smallest change).
ACCEPT: one run record with `[:mission-hole-coverage :targets-added] > 0`
and a selection certificate with `:eligible-target-count > 5`, both under the
same enumerator sha.

⟨0⟩2. The schedule-agreement gate's refusal is a typed failure
(`:schedule-disagreement`), not `:coverage-not-recorded`.
ACCEPT: a planted disagreeing schedule yields that failure id; the ledger
shows its dispatch within 5 minutes and a terminal judgment.

### ⟨1⟩ The machine always acts (days)

⟨1⟩0. The status classifier follows the open/closed rule under RQ-1.
Today (`mission_registry.clj` `live-mission?`, reused by
`live-excursion?`): of 385 excursions, 326 classify `:unknown`. 231 of
those have no status line; 95 have one the parser does not read, mostly
bold or mid-line (`**Date:** … · **Status:** ✅ **CLOSED**`). At least
E-KL-refinements, E-have-want-pairs and E-precision-over-policies say
CLOSED there and are counted live. `:draft` is counted closed.
ACCEPT: planted files, one each: bold CLOSED, mid-line CANCELLED, SUPERSEDED,
no status line, draft, "DERIVE-1 DONE". The first three classify closed,
the last three open. The census count of open excursions is recorded with
its sha.

⟨1⟩1. One enumeration: the decision's set and the target-field report's set
are the same (missions, tickets, excursions, plus A- targets).
ACCEPT: RQ-1 on one live click: persisted ID sets and HEAD sha, `missing =
extra = ∅`, report digest = decision digest.

⟨1⟩2. `read-criteria` is an action: selecting it dispatches a job that reads
the task file and commits the task's wants, facts and locators as a cascade
source, reviewed.
ACCEPT: an excursion recorded as `:unknown`; a `read-criteria` action receipt
naming it with commit and reviewer; the next click's manifest contains that
excursion with a universe at the recorded sha.

⟨1⟩3. Tornhill as an A- target.
ACCEPT: one Tornhill action receipt recording the report digest, a proposed
cleanup id, and (on the same or a later click) the cleanup's commit, reviewer
and review verdict, all resolving in git.

⟨1⟩4. `write-algorithm`.
ACCEPT: a `write-algorithm` receipt recording the new algorithm's id, path,
commit and reviewer; the next click's `:algorithms` set contains that id.

⟨1⟩5. The empty field is a configuration error.
ACCEPT: a planted empty field yields one `:empty-target-field` failure,
routed per RQ-9; across the next campaign, every click id has exactly one
terminal receipt (RQ-2).

With a crude G at the end of ⟨1⟩ (the existing risk term plus a constant
bonus for read-or-check actions), RQ-1, RQ-2, RQ-6, RQ-8 and RQ-9 can be met on
a campaign. That is the first result Joe sees.

### ⟨2⟩ The self-model (weeks)

⟨2⟩1. Task and self factors as hidden states, carried click to click beside
`:mu-post`.
ACCEPT: RQ-4 on one campaign: normalised prior and posterior for every
factor-manifest ID, and a named task factor whose posterior moves
(L1 distance > 1e-12) after an action receipt that names it.

⟨2⟩2. Information-gain and novelty terms of G from those beliefs and the
Dirichlet A counts, replacing ⟨1⟩'s constant bonus.
ACCEPT: RQ-3 on one campaign, and the paired control (identical risk,
ambiguity and habit): G of the unread target < G of the known one.

⟨2⟩3. Expansion.
ACCEPT: RQ-5's two planted controls, with the pre- and post-shas pinned.

⟨2⟩4. Lean: the self-model's factor structure and G decomposition.
ACCEPT: named Lean declarations and registry rows; compiled checks run on
fixtures exported from the running Clojure model's records, with the schema
version bound. Written after ⟨2⟩1–3 run (RQ-7's rule: wiring evidence
follows use).

### ⟨3⟩ Campaigns (ongoing)

⟨3⟩1. Campaigns of 20 clicks through the click loop, reported with RQ-7's
counts and their denominators.
ACCEPT: RQ-2 and RQ-7 met on a campaign.

⟨3⟩2. A flight on one mission to completion, the PROOF-2a theorem's case.
ACCEPT: a flight id whose mission criterion is met at a pinned sha, with a
progress-check verdict per click and every click's action receipt joined by
click id.


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
| ⟨2⟩3b second-layer wire tests | 51 wires | Written for wires the running design uses (RQ-7) |
| ⟨3⟩1–3 proof flight | not started | ⟨3⟩2 |
| Clause T (target field) | report only | Absorbed: ⟨1⟩1 and ⟨1⟩2 make it the decision |

Completed PROOF-2a rows (⟨1⟩1 equations, ⟨2⟩2a, ⟨2⟩2c, ⟨2⟩3, ⟨3⟩0) stand as
they are.

---

## Answered questions

1. Excursions (Joe, 2026-09-30): all open excursions are in the field
   unless Joe says otherwise; closed or cancelled is "otherwise". So open
   vs closed has to be read correctly: the rule is under RQ-1, the fix
   is ⟨1⟩0.
2. Interpretation (Joe, 2026-09-30): no external approval. The machine
   reads a task and works on it by its own interpretation. `read-criteria`
   commits go through the same author ≠ reviewer path as any click commit;
   nothing comes to Joe. `ask-interpretation` is dropped from the action
   list: an agent seat reads and interprets as part of `read-criteria`.
3. Budget (settled by claude-1, since the question used a term Joe had not
   seen): "gathering" meant a click whose chosen action reads a task or
   runs a check instead of changing code. Every click counts once against
   the budget, whatever its action.

## LOG

LOG: 2026-09-30 (claude-1). Joe answered the three open questions: all open excursions are in the field; no external approval of interpretations; the budget question used an undefined term, now replaced by "read-or-check action", and every click counts once. Added the open/closed rule under RQ-1 and step ⟨1⟩0, because a read-only census in the serving JVM found 326 of 385 excursions classed `:unknown`: 95 of them have a status line the parser misses, some of which say CLOSED, and `:draft` is treated as closed.

LOG: 2026-09-29 (claude-1). Requirements and ACCEPT lines rewritten to the measurable versions in codex-proof2a's note (3631f87a0): RQ-4 and RQ-7 reworded (their old nouns named no observable); the terminal receipt added as ⟨0⟩0, since five requirements join on it; set equality, not count equality, for the field; campaign = 20 clicks. Checked: the note's binding tally (6 today: 5 abstained, 1 build-failed) matches claude-1's own count.

LOG: 2026-09-29 (claude-1). Drafted from the 2026-09-29 session: Joe's
requirements (field coverage, no abstention, A- targets incl. Tornhill and
write-algorithm, curiosity, self-model, BMR's missing expansion half), the
click-loop findings, and zai-1's coverage note (3d6f3adae).
