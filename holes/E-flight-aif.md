# E-flight-aif — retrofitting an AIF reading onto the flight-era wiring diagrams

Date: 2026-09-24
Parent: the PROOF-2 plan. Siblings: `E-cascade-real.md` (the machine does not
build its cascades), `E-outer-loop.md` (it does not choose its work).
Owner: claude-10. Driver: Joe.
Status: OPEN — worked example W1 written (M-first-flights); corpus pass not
started.

## Joe's framing (2026-09-24)

> "Design patterns basically represent 'things of interest' in AI models. So
> the question with interpreting those things of interest is in terms of
> making a more and more high-fidelity rule. What I did in the
> anatomy-of-a-flight era is to look for what we would wire things into (the
> hole or sorry) and then create the wiring diagram. All of that seemed to go
> reasonably well. Now we're in a position to retrofit that with an AIF
> interpretation, which, I see now, it didn't really have."

Excursion-sized (Joe). It follows from E-cascade-real's finding that the rule
form the machine runs (needs/forbids → produces, over a list) reads only the
IF→THEN of a pattern, and from the discussion of three readings of "a pattern
as a production rule": rewrite (IF→THEN), loss over forces (HOWEVER→THEN), and
a conditioned unit that primes others (context → firing).

## The fidelity ladder

A pattern's interpretation gets more specific in steps:
1. cited as relevant to some text (xlate; claude-1's turn loop);
2. wired into a specific hole or sorry;
3. a typed node in a wiring diagram, with a witness;
4. a term in a generative model: what it predicts, what would count as it
   working, what it costs.

The flight era reached step 3. This excursion asks what step 4 looks like on
the artifacts it left.

## Prior art (read before writing)

- `futon3c/holes/missions/M-first-flights.md` §8: a flight is a
  morphism-trace, the pilot an optic, after Tull, Kleiner and St Clere Smithe,
  "Active Inference in String Diagrams" (arXiv:2308.00861). Forward wire
  field-read → prediction → velocity → gate → act; backward wire measurement →
  interpretation class → self-record. So the era had an AIF *framing* of the
  flight's process. What it did not have is an AIF reading of the wiring
  diagram's *content*: its nodes, satieties and witnesses as model terms.
- M-first-flights Phase B item 8: "the cascade scored as a policy: at least
  one flight whose candidate cascade carries a rollout-grade G, closing the
  loop between the M-memes-arrows cascade object and the AIF policy object."
  Phase B was closed (`futon3c/holes/excursions/E-first-flights-policy-grade-G-closure.md`)
  on the `fold-realized` → γ calibration seam. Whether any flight's cascade
  was scored as a policy, as item 8 asks, is not established by that note;
  open question Q4 below.
- `futon4/holes/mission-lifecycle-wm-alignment.md` §3c (Joe, 2026-09-02): the
  cascade catalog. "This is where Q(o|π) at mission grain comes from ... the
  catalog IS the playout record." The mission-triples corpus below is a
  candidate carrier for that catalog.
- The machine's own term definitions: `holes/labs/wm-contract/aif-equations.edn`
  (A = P(o|s), B = P(s'|s,π), C = preferences over outcomes, q0/D = initial
  belief, E = habit prior over policies).

## W1 — worked example: M-first-flights

Artifacts (futon3c):
- `holes/flights/first-flights-cascade.edn` (`c0815c8a`, `4e41ffb1`,
  2026-06-12): ten nodes, nine of them patterns, each with `:level`,
  `:satiety` and `:realized` or `:owed`; twelve hyperedges, ten
  `:differentiates` (context → pattern) and two `:jointly-with` (a pattern's
  second parent). A semilattice.
- `holes/flights/first-flights-wiring.edn` (`9f48c484` 2026-06-12, edited
  `12b97452` 2026-07-06): have-port, want-port and five construction nodes,
  each with `:satiety` and a `:witness` or `:owed`; six `:composes` hyperedges
  from have to want. A chain.

The two files are different kinds of object. The cascade is the mission's
argument: which patterns apply, and how each narrows the space its context
sets. The wiring is the construction: what was built, in what order, and
what witnessed it.

### Reading, term by term

**Hidden state s.** The satiety of the five construction nodes plus the want
port: a vector of six booleans (full / hungry). The have port is the given.

**D (initial belief).** The have port, "flight-records-organs-typed-grounds-in-prose",
with every construction node hungry. The file cannot supply this directly:
it is edited in place (typed-grounds went hungry → full on 07-06), so the
belief at any date has to be read from git history. The history is short and
usable here (two commits); in general it is the only record of D.

**A (observation model).** Each `:witness` is an observation that makes a
node's satiety believed. The five witnesses are of two kinds:
- artifact checks: "landed under operator sentinel, verified live (ckpt 20)",
  the typed-grounds commit with its schema and spec files. These name
  something a check could re-observe, like the machine's C4 locators.
- verdicts: "operator verdict PASS", "both real witnesses ran". These are
  adjudications, not observations of state; they belong in A only with a
  stated precision, or outside it.
`:owed` (pretty-print: "witness = pilot review + ckpt 22") is an *expected*
observation, named and pending. The machine has no field for that; its
nearest is a locator not yet observed true.

**B (transitions).** Each `:composes` hyperedge reads as: with `from`
satisfied, the construction makes `to` satisfied. As a rewrite rule,
needs #{from} → produces #{to}. What licenses each transition is a
pattern, and that join is only in prose. Read off the `:realized` and
`:via` strings:

| construction node | licensing pattern (from the cascade's `:realized`/`:via`) |
|---|---|
| schema-v04 | `futon-theory/crime-relocates-to-a-scarcer-witness` ("the schema design IS the limit move; shipped v0.4") |
| logic-model | `aif/no-self-certification` ("logic model F1-F9 rejects unwitnessed tags") |
| substrate-roundtrip | `structure/two-projections-of-one-quantity` (wiring `:via`: "two-projections: the store projection verified against the schema") |
| pretty-print | `structure/two-projections-of-one-quantity` ("render lane") — inferred, weaker |
| typed-grounds | `realtime/structured-events-only` (`:owed` names "typed-grounds migration") |

Four of the cascade's nine patterns (`measurement-window-hygiene`,
`two-layer-calibration`, `determined-fork-proto-psr`,
`honest-map-over-flattering-counter`) license no wiring node. They are
realized as properties of the schema (R3, R1, R7, R6), not as construction
steps. They shape how the transitions are done, not which ones happen.

**C (preferences).** The want port, "flights-as-anatomy-corpus-canonical-organ-order-typed-grounds",
`{:hungry-for :payoff}`. Satiety puts a preference on every node, not only
the terminal one: each hungry node is an unmet local preference. That is the
machine's open `:c-schedule` question (terminal vs every step) arriving with
an answer already written into the diagram: preference at every node.

**The cascade as the level above.** The `:differentiates` edges say a context
pattern sets the space in which the lower pattern acts, and the two
`:jointly-with` edges give `measurement-window` and `determined-fork` two
parents each. In AIF terms the cascade reads as a higher level that supplies
priors over which lower-level patterns are live: the priming reading (E-cascade-real
N1), and a structural reason the argument is a semilattice while the
construction is a chain. Candidates for the AIF term: E (habit prior over
policies) if the lower level is "which construction to do", or a
hierarchical prior on B if it is "how a construction is done". W1 cannot
settle which from one mission.

**G (expected free energy of a policy).** A policy here is an order of
constructions through the wiring. Risk: divergence of the predicted want
port from C. Ambiguity: how much the predicted observations would
discriminate states, which on this diagram depends on the A split above:
artifact witnesses discriminate, verdicts much less. Epistemic value:
largest at owed nodes, where a named observation is pending. What G cannot
get from this diagram is any number. Satiety and witnesses are binary; A and
B as read are deterministic. The probabilities have to come from somewhere
else: §3c's answer is the catalog of how cascades like this one went on
other missions.

### Where the reading breaks (findings)

- **W1-a. No dated D.** Diagrams are edited in place; initial belief exists
  only in git history.
- **W1-b. The pattern → transition join is prose.** `:realized` and `:via`
  strings, not typed references. One of five mappings above is inferred.
- **W1-c. Witnesses mix observations and verdicts.** A needs the split
  typed.
- **W1-d. `:owed` has no counterpart in the machine.** An expected
  observation with a named witness and a due point.
- **W1-e. Two levels, two shapes.** Argument = semilattice (cascade),
  construction = chain (wiring, in this mission). The machine carries one
  list for both (E-cascade-real D18).
- **W1-f. No probabilities.** The diagram supplies structure for A, B, C, D;
  every number G needs must come from precedent.

## Corpus

`futon6/data/mission-triples/`: 81 EDN files (missions and a few excursions, plus `_summary.json`),
each a hole (have/want quoted from the mission file), a cascade of pattern
citations with offsets, and hyperedges. Field presence (grep, 2026-09-24):
`:satiety` 81, `:hyperedges` 81, `:witness` 7, `:owed` 0, `:realized` 0. So
the corpus has the structure (hole, satiety, hyperedges) but almost none of
the witness layer the worked example leans on for A. M-first-flights has its
own triple there too.

## Open questions

- Q1. Does the W1 reading hold mechanically across the 81 triples, and where
  does it break most often?
- Q2. Which AIF term does the cascade level map to: E, or a hierarchical
  prior on B?
- Q3. Where do the numbers come from: the §3c catalog, the machine's Dirichlet
  counts, or both?
- Q4. Did any flight's cascade get scored as a policy, as M-first-flights
  Phase B item 8 asks?
