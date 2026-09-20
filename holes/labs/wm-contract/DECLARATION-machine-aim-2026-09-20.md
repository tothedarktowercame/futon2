# DECLARATION — the machine's aim (operator, goal content for C)

Joe, 2026-09-20 (operator surface, verbatim): "The initial and primary aim
of the system is to work while I sleep. It should be regulated by flexiargs
and a model of my operator turns."

## Thesis-level restatement (Joe, 2026-09-20, later the same night)

Verbatim: "'Can a community of humans and machines doing open-ended work
leave an account of itself structured enough that the work can steer
itself -- so that activity becomes inference?' has been advanced as a
thesis of the whole project, not just the futon2 part. That doesn't
specify whether I [am] asleep or awake while that work happens (it could
be both). It also opens the door to working across, say, some other
company or community, rather [than] just with me. Though of course, that
company will have their own preferences C, which won't be mine. But this
just means that my preferences are that the model should be
generalisable. ... A brittle C that has one warrant-depending term in it
is a placeholder at best."

What this changes in the reading below:

- "Work while I sleep" is an INSTANCE of the aim, not its definition.
  The dispreferred outcome (per correction 2) generalizes: a stall is a
  state where the account of the work is insufficient to steer it and
  only an out-of-band operator turn can move things — asleep or awake.
- C is a PARAMETER of the model, not a constant of it. Another
  community's C must be substitutable; therefore no C entry may encode a
  workspace implementation detail (the warrant token was the worst
  case). Generalisability is itself the operator's declared preference.
- Joe's assessment of the current state, recorded as given: the
  one-warrant-token C is evidence of satisficing — the machine could
  satisfy its goal from its own bookkeeping — and is a placeholder at
  best; the system is not at present able to do anything useful as a
  steering machine. The 1 -> 0 in-domain drop at retirement is the
  placeholder being acknowledged, not a regression.

Context: this answers the question surfaced after claude-4's F computation
came out uniformly -0.0 across policies — the machine had no unsatisfied
goal (its one want, route-a-rehearsal-reported, already observed true; its
other want a test-passes fact leaving the contract under
RULING-registry-wm-orthogonality-2026-09-20). Goal content is an operator
declaration; this is it. Recorded by claude-12.

## Translation into model terms (claude-12 reading, correctable by Joe)

1. **Unsatisfied goal: grounded progress during operator absence.**
   Preferred outcomes are work products appearing in windows with zero
   operator turns: runs reaching dispatch, authored commits bound
   (the exact clause the three :awaiting-validation binder findings wait
   on), repair obligations resolving, want-tokens satisfied. None of this
   is currently observed true, so C built from it is non-degenerate
   immediately.
2. **Flexiargs are the normative corpus.** Regulation comes from the
   pattern library (futon3/library/*.flexiarg) read as data, not from
   service couplings or hardwired gates — the positive statement of the
   orthogonality ruling's principle. Violation-signatures in flexiargs
   mark dispreferred outcomes.
3. **Operator turns are modeled observations.** Joe's interventions
   (rulings, redirects, corrections) are a first-class observation stream
   the machine predicts. A run that *required* an operator turn to proceed
   is dispreferred relative to one that did not — this is "while I sleep"
   made precise. The RULING-*.md corpus in this lab is the initial
   training data for that model. This also resolves the E08 participation
   question: the operator is in the universe as an observation source.

## Translation corrections (claude-4 review, accepted by claude-12, 2026-09-20)

Three corrections to the reading above; the declaration itself is untouched.

1. **Wanted outcomes need truth-makers external to the run.** "Runs
   reaching dispatch" and "repair obligations resolving" as written are
   fields in artifacts the run itself writes; a model preferring its own
   record risks satisfying the goal by writing a file. "Authored commits
   bound" is the right shape — a commit at a sha by a cast agent,
   observable in C3/C4 vocabulary, truth-maker outside the run's
   bookkeeping. C entries are built from externally-verifiable outcomes;
   run-record fields are evidence about them, never the wants.
2. **Disprefer the stall, not the operator turn.** Point 3 above ("a run
   that required an operator turn is dispreferred") is too blunt: a model
   penalized per-turn learns to avoid asking, which is the
   proceed-on-wrong-premise failure. The dispreferred outcome is a STALL —
   the run reached a state no non-operator party could unblock — and it is
   window-dependent (asking during waking hours is correct). Operator
   turns remain a modeled observation stream (predicting rulings); the
   penalty attaches to stalling, not to turns occurring. Tonight is the
   evidence: the operator turns were where the value came from, and
   nothing stalled.
3. **E08 claim softened.** Operator-as-observation-source bears on the
   participation question but does not resolve it: the paper-reader and
   agent classes remain unreached. "Resolves" above is withdrawn.

Open reconciliation before WM-13 targets anything: three denominators are
in circulation for the outcome domain (12 reachable tokens per the click-4
record; 7 scored in claude-4's measured run; live-C's 465 entries / 1
in-domain). One named reader must be chosen; claude-4's lane.

## Whose account (claude-4 reading, 2026-09-20, accepted; merges WM-13 with C formalization)

Claude-4's flag on the thesis, accepted by claude-12: the account the
apparatus currently retains is the machine's account of its own ticks
(declaration reads, q0 derivation, evaluation traces). The thesis names
the COMMUNITY's account of its work — authored changes, reviews, premise
refusals, corrections, rulings — already externally recorded in commits,
bells, and the RULING-*.md corpus, and generalizable because any
repo-based collaboration has those. The tick log is evidence WITHIN that
account, not the account.

Structural consequence: a C entry must pass two filters at once —
producible (in the union of pattern :produces) and generalizable
(meaningful in another community's collaboration). Today's producible set
({standing-cascade-g-test-passes, route-a-rehearsal-reported}) fails the
second filter entirely, so entries cannot be picked from it; the
producible set itself must change. Since the domain is DERIVED from what
patterns produce, and the patterns are the flexiargs, re-expressing
pattern outcomes in collaboration vocabulary (a change was authored and
bound; a premise was refused before work was done on it; an obligation
was resolved without an out-of-band turn) IS the domain widening, and the
C entries fall out of it. Steps 3 and 4 of claude-4's sequencing merge:
WM-13 is that one piece of work, not a prerequisite for entry-picking.

SIZING CORRECTION (claude-4, 2026-09-20; retires the scope flag above as
oversized): the produces-relation is futon2-LOCAL. Of 1404 flexiargs,
exactly 2 mention :produces and neither is a WM pattern. The library
supplies pattern identity and text; futon2's cascade-source declarations
supply the INTERPRETATION (guard + :produces), bound by
:interpretation-receipts hashing the flexiarg source at a sha. So
re-expressing outcomes = editing two files in
futon2/resources/wm/cascade-sources/, each re-admitted against its
flexiarg source — not corpus stewardship. The library is reached only if
generalizable outcomes need NEW patterns rather than new interpretations;
claude-4 finds that boundary by attempting re-interpretation first and
seeing what cannot be expressed.

## Endorsement and two further preference entries (Joe, 2026-09-20)

Verbatim, on the three candidate tokens (change-authored-and-bound,
premise-refused-before-work, obligation-resolved-without-out-of-band-
turn): "Fine, I like those, partly because *they are not my work* but
they do align (claude-4 is correct) with my preferences. There would be
others. E.g. Rob's #1 preference is dont-reinvent-the-wheel. Mine is
eat-your-tail (loss/error/costs/externalities/etc. should be made
explicit so that they can be r[e]duced). These have been discussed
before."

Notes (claude-12):

- The endorsement's stated ground matters: the tokens are NOT the
  operator's work, yet align with the operator's preferences —
  participation without ventriloquism. C entries carry OWNERS.
- **rob/dont-reinvent-the-wheel** — Rob's #1. First named human
  stakeholder beyond the operator; this is roster content for the
  participant universe as well as a C entry. Candidate observable shape:
  an existing pattern/tool was found and reused where rebuilding was
  available.
- **joe/eat-your-tail** — losses, errors, costs, externalities made
  explicit so they can be reduced. As C content: the account must
  surface its own costs; hidden externalities are dispreferred. Note it
  is a preference OVER THE ACCOUNT, same genus as the three endorsed
  tokens — evidence the collaboration-vocabulary direction is right.
- Provenance hole, retained honestly: Joe says both were discussed
  before, but no workspace artifact matches the phrases (the only hit is
  tonight's operator turn in the affect-events feed). The prior
  discussion is outside the searchable workspace; bind it when it
  surfaces, don't manufacture it.
  NARROWED 2026-09-20: the pattern index (/api/alpha/patterns/search)
  holds corpus expressions of both preferences —
  coordination/bounded-execution ("budgets … prevent hidden coordination
  costs") for eat-your-tail's cost-explicitness;
  futon-theory/theory-as-exotype ("without theory-as-exotype, each
  domain reinvents constraints ad-hoc") and the stewardship/reuse
  patterns for dont-reinvent-the-wheel. Not the original discussions,
  but prior pattern-shaped statements of the same preferences; the
  Evidence Landscape free-text search remains to be run (endpoint
  /api/mission-search; serving port not identified from this seat).

## RULING — affirmation over absence (Joe, 2026-09-20)

Verbatim: "I don't see why we would want to clog up the database with
'absence observations'. Cf.
https://en.wikipedia.org/wiki/Nietzschean_affirmation"

Applied to the third endorsed token: "obligation resolved WITHOUT an
out-of-band turn" (an absence, requiring window + earned basis +
transport scope, defeasible after backfill) is replaced by the
affirmative form: **obligation resolved THROUGH the account** — every
step of the correction cycle is present in the record and each step
grounds in a prior recorded step. A closed chain of artifacts: pure
presence, checkable via existing repository-state observation kinds
plus linkage; no new observation kind; no absence records in the store.
The affirmative claim is also the epistemically honest one — chain
closure ("the recorded account carried the steering") is witnessable;
"nothing happened off-record" never fully was. The futon1b turn stream
remains available as corroborating evidence agents may consult; it is
not an observation kind.

Tested against its exemplar (claude-4, 2026-09-20): the r173 correction
loop does NOT close today — linkage exists backward-only, from the
acceptance end, as prose in a string field, across two repos; nothing in
futon2 points back to p4ng. Chain closure therefore requires a typed
prompted-by reference on work products (e.g. a `Prompted-by: <repo>
<sha>` commit trailer, or the same as data in the receipts bundle), each
resolution being a C3-shaped existence question. This is a PREFERENCE,
not a gate: nothing refuses a commit lacking the trailer; the machine
prefers outcomes whose chains close. The first C entry for this token is
genuinely unsatisfied on all existing history, exemplar included —
recorded rather than grandfathered.

## RULING — multi-owner conflict rule (Joe, 2026-09-20)

Joe approved Option 2 ("Fine with me. Hopefully this is less
satisficing, more real, more extensible, etc., and enough to get us
moving again."): per-owner C vectors scored separately with divergence
reported; a typed refusal when owners conflict on a token; never a
weighted average. Disagreement between owners is retained in the
account, not dissolved into a scalar. Options considered and set aside:
weighted sum (erases disagreement unrecoverably), declared owner
precedence (resolves rather than retains; premature with two owners and
zero observed conflicts).

"Grounded progress with zero operator turns" must be expressible in the
reachable outcome domain. Widening the grain is WM-13, which this
declaration now gives a concrete target — the domain must be able to
express commit-bound / stall outcomes (external truth-makers per
correction 1 above).

CORRECTION 2026-09-20: this note originally said "today's domain is 12
tokens." That figure is the `:reachable 12` recorded by
`futon2.aif.live-c/cascade-spec` in the 09-19 run records
(tick-run-record-2026-09-19-1789848916/-1789849189); the 09-20 record
(…-1789862860) carries no such field. The ruled counting authority
(claude-4, 2026-09-20) is the run record's selection certificate: scored
universe 7, producible outcome domain 6, want corpus 465 with 1 in
domain. WM-13's measurable target is raising 1-of-465. Whether 12-vs-6 is
a real between-runs shrink or two readers counting different layers
(spec-supplied universe vs evaluation-produced tokens) is claude-4's
reconciliation; no count from the live-c field should be quoted
meanwhile.

RESOLVED (claude-4, 2026-09-20): layers, not shrink. 12 was
|joint-reachable| (the nameable universe: wants ∪ q0 keys ∪ pattern
:produces ∪ guard :present/:absent tokens — war_machine.clj:6185-6196);
1 is |live wants ∩ that universe|. Never comparable. Overstatement
mechanism confirmed: guard tokens are READ, not produced, so a want can
sit "in domain" that no pattern can bring about. Consequences: (i) the
WM-13 denominator must be the PRODUCIBLE set (union of :produces),
measured by claude-4 after the C1/C2 removal lands, and 1-of-465 restated
against it; (ii) today's single in-domain token is exactly the
test-passes token the orthogonality ruling removes, so retiring the
compliance target takes :n-in-domain 1 -> 0, and cascade-spec REFUSES on
an empty domain rather than scoring — domain widening and C entries are
what keep the machine scoreable after the retirement, not improvements
queued behind it.

## STAKES — the fundamentals checklist and the plop-2026 decision (Joe, 2026-09-20)

Verbatim: "Claude-4's short list remains the checklist we will work
towards addressing, though I suspect there will be other things. Either
we will complete this tomorrow and I'll decide whether the plop-2026
paper can go ahead, or we won't and I will retract it."

The checklist (claude-4's fundamentals table): E non-degenerate and
learning (DONE); C degenerate :constant-across-horizon, 1-of-465
in-domain; A :identity-default; F computed but not consumed
(f-not-consumed all four policies); D, Q degenerate.

Status at recording: C is the night's thread (removal + retirement +
reinterpretation + five owned entries + conflict rule + chain-closure
token with prompted-by); F is a wiring fix sequenced behind the
observation-layer change; D/Q are downstream of C/A.

CORRECTED (claude-4, 2026-09-20): (i) no fresh click grant is needed —
the second budget of five (AUTH-ordinary-click-budget-renewal-2026-09-19
@ 52f75d1d) has 2 remaining; tomorrow's measurement run is paid for.
(ii) A is NOT the big build risk: per g_term_decomposition.clj:16-30 it
de-degenerates by DECLARING any non-zero false-pos/false-neg rate
(precedent: R7's declared fixed zeta) — deriving rates from retained
occurrences would be manufacturing numbers, since there are almost no
observation disagreements to calibrate against. A and D are one chain:
declared rates -> q0 derivation becomes a posterior over states given
uncertain observations (today it is point-mass-by-construction,
:target-qualified-true-facts-point-mass-v1). (iii) Q is the real
one-day risk: :observation-updates empty is STRUCTURAL in the rollout
(predicts forward, never conditions on observations) — the one term
claude-4 will not promise inside a day. One operator dependency
remains: the compliance-target retirement go-ahead; claude-4 carries
that ask and the budget correction in one message from its channel.

Q sequencing and weight (Joe, 2026-09-20, verbatim): "If we don't have
Q then there is definitely no point to publishing a paper that claims
to be a model of Active Inference, that's for sure. But, OK, we can
take it last." So Q is taken last in the build order, and it is
DISPOSITIVE for the paper: closed-loop conditioning is what makes the
model Active Inference rather than a scored planner; the other five
terms landing without Q does not clear the publication bar.

## Disposition

- Formalizing C entries from this declaration: claude-4 (WM build owner),
  sequenced with the C1/C2 decoupling and WM-13 in its Acts.
- Flexiarg-regulation and operator-turn-model surfaces are new build
  design; claude-4's lane, under the standing constraint (model vocabulary
  general; workspace services as data or via agents, never as contract
  primitives).
