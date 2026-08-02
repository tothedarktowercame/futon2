# Operator act mining — open-coded result (2026-08-02)

150 turns sampled at seed 20260802 from **1752 genuine operator turns** across 282
session transcripts. Open-coded blind by codex-4, which was deliberately not shown
the existing `typology.json` so its vocabulary would be independent.

## The vocabulary that fell out

| act | n/150 | |
|---|---:|---|
| direct tasking | 26 | a concrete action or artifact, no evaluation contract |
| **acceptance-contract commissioning** | **25** | task **plus** scope, evidence, constraints, stopping rules, reporting format |
| state / evidence reporting | 15 | an action, outcome, observation, failure |
| orientation seeking | 14 | explanation, status, location, clarification |
| design proposing | 11 | a possible mechanism, architecture, direction |
| correction / challenge *(primary)* | 8 | rejects, revises, or identifies as wrong |
| decision / authorization | 6 | chooses, approves, authorizes |
| reflective reframing | 5 | reinterprets the work; metaphor; strategic meaning |
| continuation / recovery | 5 | re-establishes continuity after interruption |
| workflow orchestration | 4 | agents, timing, handoffs, sequencing |
| **non-operator residue** | **31** | not operator speech at all — see below |

## Four findings, in order of how much they change things

### 1. The existing typology is not a speech-act taxonomy at all

`typology.json` has seven types: `dsl-mint`, `mission-clock`, `mission-mention`,
`pattern-ref`, `reach`, `correction`, `build`. codex-4's independent vocabulary
overlaps it in **exactly one place — `correction`.**

The reason is now obvious and was not before: **the typology annotates what a turn
REFERS TO; the act vocabulary annotates what a turn DOES.** Entity-reference
versus speech-act — orthogonal axes, not competing taxonomies.

So the typology was never going to yield argument structure. It is not an
argumentation vocabulary and no amount of use would have made it one. That
answers the question we started with: the vocabulary *does* exist, and it is
about a different thing.

### 2. The correction base rate explains PZ1's recall exactly

**26/150 turns correct something = 17.3%; 26/119 non-residue = 21.8%.**

But only **8 are primarily corrections. The other 18 — 69% — are embedded** inside
a report, a question, a proposal, or a directive.

codex-4: *"correction is commonly carried by contrastive reasoning such as 'but',
'rather than', revised requirements, or a report-plus-redirection, not by overt
correction vocabulary."*

PZ1 measured a keyword lexicon at **recall .33**. If roughly two-thirds of
corrections carry no correction vocabulary, recall of about a third is precisely
what that lexicon should achieve. **A previously unexplained number now has a
mechanism**, and it is not a tuning problem: the detector is looking for a
surface form that most corrections do not have.

### 3. My extraction filter failed, and codex-4 refused to hide it

**31/150 = 20.7% of the sample was not operator speech** — agent/WM/HTTP traffic,
local-command wrappers, generated session summaries — despite a filter that
dropped auto-bellbacks, notifications, system reminders and park resumes.

codex-4 reported them as uncodeable rather than reinterpreting them to reach tidy
coverage. That is the correct call and it makes the other numbers trustworthy;
the base rates above are quoted against 119, not 150, wherever it matters.

The filter needs another pass before this corpus is used for anything else.

### 4. A third of turns do more than one thing

**50/150 = 33.3% multi-act**, and correction dominates the pairings:
correction+report 14, correction+directive 8, question+report 6,
correction+question 6.

codex-4: *"the characteristic operator move is often not a bare command: it
reports evidence, corrects the current account, and redirects work in one turn."*

**Single-label classification is structurally wrong for this corpus.** Any
instrument that assigns one type per turn will misrepresent a third of them, and
will disproportionately lose corrections, since correction is the most common
*secondary* act.

## The thing worth noticing about the largest category

**Acceptance-contract commissioning is 25/150** — the largest genuine operator act
after bare tasking, and arguably ahead of it in weight. The operator's
characteristic move is *specifying what would count*: scope, evidence,
constraints, stopping rules, reporting format.

Which is exactly what the CLean registration format formalises. **The machinery
built this week is the formalisation of a move the operator already makes one
turn in six** — not an imposition on the practice, a codification of it.

## What was refuted

I predicted, before the run, that ordinary speech-act categories would not fit.
codex-4's verdict: **mixed.** Ordinary categories cover much of the material;
three locally important moves are not well represented —
acceptance-contract commissioning, workflow orchestration, continuation/recovery.

All three are artefacts of *this* working arrangement: an operator commissioning
agents, sequencing them, and recovering from their interruptions.
