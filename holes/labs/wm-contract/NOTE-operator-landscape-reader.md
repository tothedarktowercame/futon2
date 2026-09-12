# NOTE: the operator-landscape reader — the third zaif crew member

Joe with zai-7 (zai), 2026-09-12. Joe's framing: read the evidence
landscape — associate free text through the embedding to mined patterns,
elaborate patterns and turns with free-text search results (as the APM
loop does) — to model what is going on in operator turns, because the
operator turns currently represent ALL non-automated stakeholders
(everything the War Machine does not yet automate). Better pattern
cascades out of that reading feed improved automation workflows. This is
the "closing the AIF over the operator" thread from the What Problems
Are We Solving paper (p4ng/futon-2026.tex).

Prior work this builds on (and must not repeat):
`E-operator-as-attached-agent/FINDINGS-embedding-pipeline.md` (2026-09-04)
already measured the guesser.

## What the measured substrate licenses (R7 rows, pre-earned)

| channel | measured property | precision row |
|---|---|---|
| MiniLM top-3 tags | top-1 changes 77% under word-shuffle; salad ≥ real text in score; no floor exists | low; seeder only, never authority (P2's ruling, restated) |
| coverage 86% | below random-vector null; hub-dominated; 196 never-fired | coverage is volume, not signal |
| FTS text search | the cross-check that found the scan defect | different failure modes than embedding — the elaboration arm |
| declared marks (operator rulings) | precision 1 by construction | the only raiser |

Consequence for design: this agent's SMELL is the embedding (cheap, broad,
wrong often) and its LOOK is FTS + hydration + re-check (the
candidate-generator-never-authority stack). Elaboration goes THROUGH the
text, not through the vector: the embedding proposes pattern candidates
for a turn; the reader then pulls the pattern's text and the turn's
full text and records an association with the passages that justify it.
That fixes the found defect that the embedded input is not recorded: the
association record must carry the full input text, the corpus version
stamp, and the justification excerpt.

## The agent: b-landscape-reader (a new family — cartography, not homeostasis)

Not a coherence keeper: nothing here is dirty or stale; the landscape
is being MAPPED. Board sketch:

- SMELL operator-turn window (author joe, since T) → candidate turns.
- LOOK embedding-tags for each turn (top-3, scores recorded, floorless
  BY MEASUREMENT — the scores are data, not filters).
- LOOK' FTS elaboration: for each tagged pattern, its text + the turn's
  full text; record association {:turn-id :pattern-id :score
  :corpus-version :justification-excerpt}.
- FEEL the cold families: associations that would reach the 196
  never-fired patterns are worth more EIG than another hit on
  hexagram-24 (hub avoidance as a felt-for thing, priced in).
- ZAP forbidden (N1-class safety): the reader writes association
  records, never cascades. Cascade construction from associations is a
  separate, authored act (L4/L3), because a cascade IS a policy.

## What the reading is FOR (Joe's stakeholder point)

The operator turn is the proxy-C for every stakeholder not yet on the
mesh: when the WM automates nothing, everything external arrives through
Joe's voice. So the landscape map is the C-belief substrate the ask arm
prices against: clusters of turns that associate with the same cold
patterns are candidate NEW cascade families — recurring operator concerns
that no cascade yet serves. The map's growth rate is the automation
frontier moving. When a stakeholder (Rebecca, UKRN) gets their own
channel later, this same reader binds to it with a per-author precision
row — the board does not change.

## Sequencing and honesty

- Corpus version stamps and full-input recording are PRECONDITIONS (the
  findings name both as holes). Land them before the first association
  record, or the map inherits the unstamped-corpus defect.
- Duplicate-slot fix (dedupe by id in the ranker) is a one-line repair
  upstream in notions_search.py — do it, don't route around it.
- The 32 disagreeing days (scan vs FTS) stay listed, not averaged: the
  reader consumes the day-windowed producer only.

Row links: NOTE-agent-needs-from-issue-board (this is the N2/C-belief
complement the hierarchy was missing); FINDINGS-embedding-pipeline (the
measurements); SPEC-chip-boards-v0 §3½ (the reader sits at L1-L2 and
feeds L4); M-zaif-harness (the C-belief arm this operationalizes).

## The R-node specification (Joe asked, 2026-09-12)

The cartographer differs from the coherence keepers node by node — where
inbox-zero's R3 revises a predicate, this agent's R3 accumulates a MAP.

- **R1 (belief μ)** — the association graph itself: turns × patterns ×
  passages, per-author. Belief state is the map's current extent, not a
  scalar. Counterpart slices are first-class from day one: the operator
  slice now; **Rebecca's slice and Eric's slice later are the same slot
  with a different author key** — same sphere of endeavor, distinct
  individuals, distinct precision rows (their channels are email/prose,
  measured separately per U9; no precision transfers between people).
- **R2 (observation o)** — four channels with measured precisions: the
  embedding tags (floorless seeder), FTS elaboration passages, the
  operator-turn full text, and declared marks. Every association record
  carries input text + corpus version + justification excerpt (the two
  provenance holes, closed by construction).
- **R3 (update)** — association accrual with hub correction: an
  association into a cold family (the 196 never-fired patterns) carries
  an EIG bonus recorded on the edge; hub hits are recorded but do not
  grow the map's information. The map grows at its frontier, not its
  hubs.
- **R4 (forward model Q)** — predicted value of an association: covers
  the null lesson — random vectors reach 58% of the library, so
  breadth is worth ~nothing; the model prices DISTINCTION (new
  turn-pattern pair, cold family, cross-author bridge), not coverage.
- **R5 (G)** — per candidate association: risk (a wrong association
  poisons cascade construction downstream — the map is load-bearing for
  L4) + ambiguity (score does not separate sense from salad ⇒ every
  embedding-only association is high-ambiguity until FTS-elaborated).
  The elaboration arm exists to drain exactly that ambiguity.
- **R6 (policy)** — retrieve (FTS elaboration) / act (write the
  association record) / ask (surface a cluster to the operator as a
  candidate cascade family) / yield (window exhausted). No zap: the
  reader never constructs a cascade.
- **R7 (channel precision Π)** — the earned table from the findings,
  plus: per-author rows (joe ≠ rebecca ≠ eric), per-corpus-version rows
  (an unstamped corpus is an unknown channel, not a stale one).
- **R8 (mismatch F_π)** — a turn that associates with NOTHING (below
  even the floorless seeder's habits) is the interesting residual: the
  operator is doing work the pattern language cannot yet name. Mismatch
  = new-pattern candidate, surfaced, not force-tagged.
- **R9 (witness)** — associations are proposals; a cascade built from
  them is authored and ratified one level up. The map never certifies
  itself (same clause as the issue board's accepts-nothing).
- **R13 (horizon T)** — per-window (a day's turns); the map's horizon
  is the whole history, but each cycle only extends the frontier.
- **R14 (τ)** — trust in the map: re-derive a sample of associations
  after each corpus rebuild (the corpus is rebuilt regularly and
  unstamped today — stamping it is the precondition). Corpus change
  without re-derivation is τ decay, typed.
- **R16 (action u)** — the association record is the act, witnessed by
  its justification excerpt.
- **R17 (learning)** — the embedding itself is tunable LATER (input
  lengths, what text is embedded — the findings measured prefix-vs-full
  disagreement 0.33, so tuning changes most answers); for NOW the
  dataset we have is the declared instrument, versioned and measured.
  Tuning is a sanctioned change vector with re-measurement, never
  silent.

## Embedding Live and the interpretation layer

`mission-efe-field.html` ("Futon City") is the same material rendered:
all historical missions, pattern/scoped, 2D, with per-mission status
and generativity. It is a PROJECTION — lovely, and not queryable. The
cartographer's association graph is the interpretation layer it lacks:

- a 2D layout answers "where is X"; the graph answers "what does X
  neighbor, why (passage), since when (bitemporal), how trusted
  (precision row), and what cascade spans it";
- **cascades as a spanning set**: the claim worth making explicit — a
  good pattern cascade is a minimal spanning structure over the turns
  that produced meaningful work. The map's job is to hold the points;
  cascade construction (L4, authored) is choosing the spanning edges;
  Futon City is then just one rendering of the result, regenerable.
- keep the page: it becomes the cartographer's SING — a typed meter
  (map extent, frontier growth, cold-family coverage), rendered.

## The askable questions = the chip set (Joe, 2026-09-12)

"Nothing answers questions, and nothing even asks questions." The
rendering answers nothing; the fix is to enumerate what CAN be asked —
and the askable questions ARE the chips (Z1's named query library, read
as a table of contents). Each chip below is a named XTQL form over the
association graph, two-wire by construction:

FEEL (adjacent, free — about one node):
- does this turn associate with anything? (unmatched-turn = R8 residual)
- does this pattern have an author other than joe? (stakeholder bridge)
- is this association fresh vs the corpus version it was minted under?

LOOK (line of sight — about one relation, writes the range finder):
- what does X neighbor, and with what justification passage?
- how far is pattern P from author A's region? (hops × precision decay)
- since when has this cluster existed? (bitemporal: map frontier growth)

SMELL (whole room, untargeted):
- which patterns fired this window? (hub traffic — cheap, low-value)
- which patterns NEVER fire? (the cold 196 — the frontier)
- which clusters cross authors? (rebecca/eric bridge candidates)

KEYPRESS:
- has the corpus been rebuilt since the last stamp? (τ decay, typed)
- is an operator turn pending?

And the pickup/zap asymmetry, restated as questions:
- PICKUP: may I acquire this into working memory? (always askable)
- ZAP: may I change the record? (never askable for the cartographer —
  the question whose answer is constitutionally "no" is still worth
  NAMING, because a crew member who can't say why it may not zap is
  safer by accident, not by design)

A question with no named form is unaskable, and unaskability is a
finding: the set of questions we cannot yet pose over the map is the
map's own gap list — publish it beside the R-node gap list.

## Futon City as a playout board (Joe, 2026-09-12 — the SimCity step)

The interactive step beyond the projection: focus a topic, simulate a
work sequence ("10 simulated clicks"), ask what the likely buildout is.
In AIF terms the city stops being R2's rendering and becomes R4's
forward model; the cascade you would run is the policy; the playout is
Q(o|π) evaluated on the map. Three rules keep it honest:

1. **Playouts are walks on the association graph under a candidate
   cascade** — the same graph the question-chips query. A simulated
   click is a chip firing: FEEL a cluster, PICKUP a pattern, wire an
   edge. No free-floating speculation: every step of the playout is a
   move the question vocabulary can name, and every probability it uses
   is a measured quantity on the graph (association density, precision
   rows, hub/cold structure).
2. **Q is calibrated from history, not invented.** The map is
   bitemporal: "the last N times work focused near pattern P, what grew
   in the following window?" is a query. Historical focus-then-growth
   pairs are the map's own retro-bootstrap for the forward model — the
   zaif retro-bootstrap idea (γ starts calibrated) transplanted from
   corrections to buildouts. Cold-family focus has few historical
   exemplars: those playouts carry WIDE intervals, stated.
3. **A playout is a proposal, priced in fuel.** Ten simulated clicks
   cost embedding queries and reads; the playout board has a fuel
   budget like any other, and its output is a typed buildout-forecast
   record (topic, walk, predicted growth with intervals, calibration
   basis) — never an action. The operator reads forecasts and picks;
   the city does not build itself. SimCity's god-view, minus the
   god-mode: you can WATCH the future, not enact it.

Instrumentation note: this is exactly what a-sorry-enterprise /
slush-demo style simulated-flow runs already do for cascades; the city
playout is the same machinery with the association graph as the board.
