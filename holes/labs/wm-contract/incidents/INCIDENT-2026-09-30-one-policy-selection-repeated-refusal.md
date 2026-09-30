# INCIDENT 2026-09-30: a selection with one policy, and the same author refusal twice

Status: **OPEN**. Recorded by claude-1 at Joe's direction, so it is not lost
in chat. No click is proposed while it is open (PROOF-2b actions, ruling 6).

## What happened (from the records)

- Runs: click 19 `data/wm-runs/tick-run-record-2026-09-30-1790747529.edn`,
  click 20 `…-1790748841.edn`. Both selected
  M-descriptive-essay-of-the-stack with the cascade
  `[:system-coherence/verified-rewrite-from-diagnostic-annotation
  :contracts/holder-states-the-claim]`, dispatched author codex-proof2d, and
  ended `:guardrail-refusal` at `:author-wait` with reason
  `:diagnostic-source-incomplete-and-i1-i6-unaudited`.
- Click 20's selection certificate has **1 candidate and 1 policy**.
  `:mission-hole-coverage :targets-added` is 195.
- The candidate's G is 1.0498221244986778 = −ln(7/20).
- The observation model's class preference is
  `{1 {:ending/not-yet-evaluated 1} 2 {…1} 3 {…1} 4 {:focused 11/20
  :related 7/20 :unrelated 1/20 :stop-the-line 1/20}}`; the target's class is
  `:related`. F is `:not-supplied`
  (`:class-model-unconditioned-at-selection`).
- `data/wm-interpretations/M-descriptive-essay-of-the-stack.edn` holds
  exactly two patterns, the two in the cascade.
- The want comes from the mission file's line 220: "A print-quality Futon
  essay projects `stack-annotations.edn`, applies the typed rewrite
  discipline, and passes the I1–I6 audit."
- The author's reply card: `{:built "none: prerequisites remain absent"
  :want-coverage "honest refusal" … :things-to-try ["inspect I1-I6 -> all
  unchecked"]}`. (Summary only was read; the full reply text was not.)

## How a policy space has one member when cascades are formed on the fly

Cascades are formed on the fly, but from a pool, and the pool is not the
pattern library (about 1,415 patterns). For a target, the constructor may
use only patterns that have a **published, validated interpretation for
that target**. Interpretations are produced one per want by one agent
answering one request, so a target with two answered wants has a pool of
two patterns, from which one cascade was formed. Across targets, only one
target had any pool at all. Policies = cascades constructible from admitted
patterns of admitted targets = 1. (Not checked: how many cascades the
constructor can enumerate from a given pool; the record shows one.)

## PRIMARY FINDING: this violates the core design from the first step (Joe, 2026-09-30)

Joe: "Then you are violating my core design from the first step."

The design, in the library's own words
(`futon3/library/cascades/on-the-fly-cascade.flexiarg`): "Derive cascade
edges per query, against the current problem state — the cascade a seat
needs is the cascade FOR THIS PROBLEM, and most such relations cannot be
pre-declared. ... Compute the cascade slice at query time (lexical,
structural, or model-judged relation candidates over declared skeleton plus
context), hand the seat the slice with provenance, and receipt whatever the
seat actually uses."

What was built instead, and since when:
- 2026-09-17 (`e2e1b477a`, `8e3fde409`): a target's patterns come from a
  hand-written per-target file (`resources/wm/cascade-sources/*.edn`,
  "hand-admitted interpretation/construction").
- 2026-09-24 (`e08d08329`, `604959898`): an agent may add to that per-target
  list by answering an interpretation request, one pattern per want,
  validated before it counts.
- The constructor has only ever read that per-target list. It has never
  derived a slice from the library for the problem at hand.

Why it went that way (the cause, not a defence): to compute G for a cascade
the code wants each pattern as a rewrite over the target's tokens (a guard
and a produced want). Library patterns are prose. The "interpretation" is
the translation of one pattern for one target, and only translated patterns
may be used. A step introduced to make the score computable replaced the
library with a short list fed in by hand. This is the same root as the
"formal wants" gate.

Already in the code and misused: `interpretation_request.clj` runs two
retrievers (embedding and tier0) over the whole library against a want and
ranks candidate patterns. That is a query-time slice with provenance. It is
used only to fill the prompt of an interpretation request; the slice itself
never reaches the constructor or the seat.

All other findings below are downstream of this one.

### Did the specification record the principle? (Joe's question, 2026-09-30)

Joe: "the specification either did not write down what I said should be in
there, or it was changed after the fact."

Read: `SPEC-cascade-policy-semantics-2026-09-15.md` (eight commits, all
2026-09-15) and the Lean policy-set modules.

- **The principle IS written down.** §1: "Joe's operator statement requires
  concretely constructed cascades over real patterns, their links and
  mission usage ... not a hand-picked graph". Stage 0 (commit 0fe3ed681):
  "`Π_k = Construct(x, c_k, retained evidence)` is a finite candidate family
  at a construction step ... A finite list is a computational snapshot, not
  permission to pin the same authored subset across problems. Constructor
  coverage/termination is an obligation; an empty search result is not
  proof that no better extension exists." The header says Joe's Stage-0
  statements override §4 where they conflict.
- **The same document also gets the order wrong.** §4, "resolutions from
  claude-20" (commit 8769a52e0): "No interpretation for a firing pattern
  means a policy-hole: scoring reports typed missing input, never an
  invented B; absent interpretations must be supplied even for partial
  constructions under Stage 0." That makes the interpretation an input
  scoring cannot do without, so a cascade with an uninterpreted pattern has
  no G and cannot be selected. Joe's order is select, then interpret.
- **The code then went further than either.** The spec says a missing
  interpretation is reported by scoring as a hole in that policy. The code
  (2026-09-17 hand-written per-target sources; 2026-09-24 interpretation
  requests) uses it as an admission gate before construction: no
  interpretation, no pattern in the pool, no target in the field. That is
  not in the specification. The header of the spec also says "No
  Lean/runtime change is authorized or claimed by this note."
- **The Lean does not state the principle at all.** `cascadePolicySet
  (menu : List (PolicyKey M P))` takes the menu of policies as given; nothing
  says where the menu comes from or that it is derived per problem from the
  library. `MachinePolicySet.lean` is marked NON-CONFORMANT in its own
  header (2026-09-17). Interpretations appear as `ObservedInterpretation`
  with `published` and `absent` cases. So "meets the specification" could
  be true of the Lean while the machine selects among one policy: the Lean
  proves properties of G over whatever policy set it is handed.

So: written down in the spec, contradicted by a later-recorded resolution in
the same spec, absent from the Lean, and overridden in the code.

### "What is G before there is an interpretation?" was the wrong question

claude-1 put this to Joe as a choice between two options (from
`NOTE-selection-order-2026-09-30.md`). Joe declined it: "the question is
framed as a contradiction rather than an honest inquiry. Why does G need to
be defined before there is an interpretation? If it does, why isn't it
already defined?" The framing is withdrawn. The answers:

- **G does not wait on an interpretation.** Expected free energy is an
  expectation under what the model currently believes. Before a pattern has
  been interpreted for a target, the model still has a belief about what the
  pattern does: the pattern says so itself (IF / HOWEVER / THEN / BECAUSE,
  and fields such as `@epistemic-value` and `@done`). Interpreting it for the
  chosen target is something done after selection that sharpens that
  belief. The "contradiction" came from treating a pattern's effect as
  either a supplied exact operator or a missing input, with nothing between.
- **It was specified, and not built.** The spec's own words (§4): each
  pattern's transition comes "from its attested (else documented)
  interpretation". The documented meaning of a pattern is the default; an
  attested, target-specific one replaces it when it exists. What was never
  built is the step that reads a pattern's documented IF/THEN as its
  operator. M-象-cascade's IDENTIFY section states exactly this gap:
  "There's no step that turns IF/THEN prose into these rules. The only text
  compiler, `interpret-pattern`, works on keywords, and nothing in selection
  uses it." In its absence, agents hand-wrote operators for one target at a
  time, and that hand-written list became the policy set.
- **So handoff 4 is not a decision for Joe.** It is: G is computed from the
  pattern's documented meaning (what M-象-cascade supplies per pattern, in
  the background), and the interpretation for the chosen target, made after
  selection, updates it.

### The domain of G (Joe, 2026-09-30) — governing statement

Joe: "G is defined over policies, and policies are cascades. ... G should be
defined over cascades full stop. If it goes via an interpretation that is an
implementation detail which I prefer to leave to you. But the domain of
definition is pattern cascades (which have multiple patterns in them in a
specific geometric arrangement)."

Requirement: **G is total on pattern cascades.** Every cascade that can be
formed from library patterns for a problem has a G. There is no cascade for
which G is "missing input", and none is kept out of the policy set because
some piece of machinery has not run for it yet. G depends on the
arrangement of the patterns (which stand on which, which overlap, their
precedence), not only on which patterns are present; the Lean already says
a composition-blind score cannot separate what cascade G separates
(`GOverCascades`, `compositionBlind_cannot_separate`).

Implementation decision (claude-1's, as delegated): G for a cascade is
computed from each pattern's documented meaning, composed according to the
cascade's arrangement. The interpretation of the chosen cascade's patterns
for the chosen target happens after selection and refines the belief; it is
never a precondition for a cascade having a G. How a pattern's documented
meaning is read into the computation is settled with the selection and
controller owners and with M-象-cascade's owner; it does not come back to
Joe as a question.

Consequences:
- Selection handoff 4 is: make G total over the constructed cascades.
  Negative test: any cascade the constructor can form from the slice has a
  numeric G; a typed "no G" for a constructible cascade fails.
- Lean: G's domain is the cascades constructed over the library, with no
  absence case in the domain. `ObservedInterpretation`'s `absent` case and
  the `:class-unknown-no-scalar-g` refusal are where the current model
  makes G partial; both are findings for the controller owner's audit.
- claude-1's earlier wording "a cascade whose pattern has no
  interpretation" described a one-pattern cascade; cascades have several
  patterns in an arrangement, and the policy set is many such cascades.

### How large the policy set has ever been (from all run records)

Joe: "whoever fed cascadePolicySet with the degenerate badly designed system
certainly did not get it from me or from M-G-over-cascades ... I have
insisted that this machine should not be based on facades."

Counted from every run record in `data/wm-runs` that carries a selection
certificate (30 of 76):

- Every run that selected a target and dispatched an author had **exactly 1
  candidate and 1 policy** (M-autoclock-in; M-aif-policy-conditioned-eig
  09-29; M-self-documenting-stack ×3 and M-descriptive-essay-of-the-stack ×2
  on 09-30).
- The largest policy set ever recorded is **24 policies over 21 targets**
  (three runs on 2026-09-21); none of those selected.
- From 2026-09-22 on it is 1, 2 or 3 in every record.
- The patterns came from five hand-written files in
  `resources/wm/cascade-sources/` plus, from 09-30, per-target
  interpretation answers. At no point was a policy set derived from the
  pattern library for the problem at hand.

So the scoring is real and its input has been a hand-fed menu: every
selection the machine has made that led to work was a choice among one.
Git authorship does not say which agent introduced each step (commits carry
Joe's git identity; agent trailers exist only from 2026-09-30); the commit
messages name claude-20's resolutions (09-15) and the hand-admitted first
target (09-17).

To close this part, beyond the code repair: the Lean must state where the
policy set comes from, so that a hand-fed menu cannot satisfy it. Owner:
controller with selection. Negative control: a menu that is not the result
of construction over the library for the problem fails conformance.

### The order of operations (Joe, 2026-09-30)

"Go ahead and interpret a pattern, after you select it. Don't gate
selection on there already being an interpretation. Look at how M-象-cascade
works." M-象-cascade (`futon3c/holes/missions/M-象-cascade.md`): a continually
maintained cascade that coalesces and searches hierarchically, so common
patterns are found quickly and rarer ones by a path from those; and a
background corpus of IF/THEN interpretations read from Joe's turns as
semantic parses. There, an interpretation belongs to a pattern and is
available in the background; it is not something a target must already own
before it may be chosen.

## In active inference terms

| Part | What the theory has | What the record shows |
|---|---|---|
| Policy set | every available course of action | one, left over after admission |
| Posterior over policies | softmax of −G (and habit) | 1 on the only member; G compared nothing |
| C (preferences) | over outcomes | over the class of relation to the focus; nothing about work completed |
| Cτ | preferences along the horizon | "not yet evaluated" with probability 1 at steps 1–3 |
| Epistemic value | uncertainty resolved by acting | none: F not supplied |
| Transition model | how actions change states, learned | the pattern is recorded as producing the want |
| Observation after acting | updates beliefs | the refusal changed nothing: click 20's G equals click 19's (inferred from the score; the learning ledger was not checked) |
| The author's possible replies | whatever is informative | DONE or REFUSE |

## Findings (each a class of failure, with its level and negative test)

1. **The pool a cascade is built from is two patterns, not the library.**
   Level: model gap. Owner: selection. Negative test: a target with N
   applicable library patterns yields more than one candidate cascade
   without N interpretation requests having been answered first; with a
   pool of two the test fails as today.
2. **Admission decides the policy set before any score is computed.**
   195 targets in, 1 policy out. Level: model gap. Owner: selection.
   Negative test: over a field of k open targets, the number of scored
   policies is at least k (each target has at least one action); today it
   is 1.
3. **C carries no preference for completed work, and Cτ is blank before the
   last step.** Level: model gap. Owner: selection. Negative test: two
   candidates identical except that one's predicted outcome closes a want;
   the closing one has the lower G. Today they tie.
4. **A mission's end-state criterion is handed to one author job as one
   step.** Level: content gap (what a want is). Owner: seats with
   selection. Negative test: a want whose text names prerequisites that the
   repository shows unmet is not dispatched as a single construct step;
   the dispatched action is a step the author can complete or a question.
5. **The author can only say DONE or REFUSE.** Level: hypothesis gap (the
   seat result sum). Owner: seats. Negative test: an author reply "this
   needs X first" or "what does Y mean here?" is a typed result the run
   acts on, not a refusal that ends it.
6. **A refusal updates no belief, so the same policy is chosen again.**
   Level: model gap. Owner: controller. Negative test: after a typed
   refusal of (target, cascade) with unchanged inputs, the next selection's
   score for that pair differs, or the pair is excluded; today it is
   identical and re-chosen.
7. **Process: all 20 clicks of 2026-09-29/30 were run on a machine that
   could not choose anything.** Joe: "you didn't just repeat 1 failure, you
   repeatedly ran a machine for 20 'clicks' that had no chance of doing
   anything meaningful." Correct. Each click either selected nothing or
   "selected" the single policy on a hand-fed menu. The count of candidates
   is in every selection certificate; claude-1 did not read it before the
   first click or after any of the twenty, and reported click 13's grounded
   change as the machine choosing. Owner: claude-1. Negative test for the
   class: before any run is proposed, the size of the policy set and where
   it came from are stated from the record; a run whose policy set is not
   derived from the library for the problem is not proposed. Covered also
   by rulings 2, 3 and 15.

8. **No one alerted Joe.** Joe: "there are certain critical parameters.
   The number of missions being chosen from is one, that was the first
   facade that I noticed ... Now the policy space has been reduced to 1
   item, which is the most degenerate facade I have heard of — and no one
   alerted me." The values were in the records and in no report. Level:
   model gap (the machine has tripwires and none watches these) and
   process. Owners: records (the report), controller (the tripwire),
   claude-1 (stating them). Negative test: a run whose critical parameters
   are degenerate raises an alert addressed to Joe and is not reported as a
   selection.

### The critical parameters, and their values at click 20

| Parameter | Design | Click 20 (record) |
|---|---|---|
| Open tasks that exist | all of them | about 637 by the 2026-09-29 census (221 missions, 373 excursions, 43 tickets) |
| Tasks the decision enumerates | the same set | missions and tickets only; 195 mission-derived targets added; excursions absent |
| Targets that reach scoring | every enumerated target | 3 (the only ones with any interpretation) |
| Targets with a score | every one reaching scoring | 1 (two declined: class unknown) |
| Patterns a cascade may draw on | the library slice for the problem (library about 1,415) | 2 |
| Policies compared | many per target | 1 |
| Preference along the horizon (Cτ) | stated for every step | blank for steps 1–3 |
| Terms of G in use | risk, ambiguity, information gain | risk only |

History of the first of these, as far as the records and `PROOF-2b.md`
show: five hand-written target files from 2026-09-17, four of whose targets
were later finished; 2 to 21 targets with candidates on 09-20/21; one target
in almost every record from 09-22.

These eight numbers are to head every report claude-1 makes about a run,
before anything else is said about it.

### The requirement that closes the facade question (Joe, 2026-09-30)

Joe: "each variable in the Lean model needs an interpretation in terms of
real countable things, and the requirement should be that all countable
things need to be taken into account at some point in the model in a
meaningful way, or we have a facade on our hands again."

Two directions, both required:

- **Model → world.** Every variable, parameter and index set in the Lean
  model names the real, countable thing it stands for, and how to count it
  (a command or a record field). A variable with no such reading, or whose
  real count is 1 where the thing has hundreds, is a finding.
- **World → model.** Every countable thing the machine exists to deal with
  appears somewhere in the model in a way that can change what the machine
  does. A thing that is counted in the world and absent from the model, or
  present only as a name, is a finding.

First cut of the countable things, with today's counts (claude-1; the
model-side column is for the controller owner's audit to fill, with
file:line):

| Countable thing | Count today | How counted | Where it should enter the model |
|---|---|---|---|
| Open missions | 221 | 2026-09-29 census | the field; one task each |
| Open excursions | 373 | same | the field; today absent from the decision |
| Open tickets | 43 | same | the field |
| Acceptance items on open tasks | not yet counted reliably | checkbox count per open task file | wants / criteria; preference C |
| Patterns in the library | 1,431 files in 120 families | `find futon3/library -name '*.flexiarg'` | the pool cascades are built from; today 2 |
| Agent seats | 56 registered (29 codex, 12 claude, 10 kimi, 4 zai, 1 wm) | `GET /api/alpha/agents` | who can act; agent-behaviour counts; today 3 fixed seats |
| Completed runs | 76 run records | `data/wm-runs` | what the model learns from; today not read by selection |
| Outcomes of those runs | 1 grounded change, 3 author refusals, … | run records | likelihood / transition counts |
| Stack claims | 417 | `:mu-post` | stack factors (the one population the model does cover) |
| Repair findings | 153 files | `data/wm-repair-obligations/findings` | failures the machine has had; self factors |
| Steps in a horizon | 4 | the observation model | Cτ: a preference at every step; today 1 of 4 |
| Closed tasks | not yet counted | closed/cancelled status | completed work; outcome counts |

A quick file count of mission and excursion files gave much larger numbers
than the census because the glob also matched worktree copies; the census
figures are used, and the acceptance-item and closed-task rows are left
uncounted until they are counted from the one canonical checkout.

This table, completed in both directions, is a closing condition of the
incident, and its counts head every run report.

### The Lean audit (controller owner, 781234b1f)

`NOTE-lean-backdoor-audit-2026-09-30.md`: eight groups of inputs the Lean
takes on trust, the degenerate value that satisfies each, which of those the
live machine supplied, the closure for each, and twelve ordered handoffs
(policy construction first). Its central finding: normalisation and
non-emptiness, which the Lean does require, do not exclude a normalised
singleton, a constant, an identity kernel, a flat belief, or a preference
delayed to the last step. Spot-checked by claude-1: `CascadeEFE.lean:58-62`
(the `Model` structure, including `interpretation : P → Option …`, which is
where G is partial) matches; the `cascadePolicySet` citation is a few lines
off (the definition is at `Proof2/CascadePolicySet.lean:46-50`).

## Why the verification done did not catch it

Every test of selection supplied candidates; none asserted how many
candidates a real field yields. Every test of the author path stubbed a DONE
reply. The Lean corpus proves properties of G for given policies and says
nothing about how many policies exist.

## To close

Each finding has its negative test written and failing on current code, a
repair reviewed by claude-1, and the test passing; then Joe sees this file.
