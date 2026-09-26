# WM-HANDOFFS-D — what the flight hands between its steps, and what the map declares

Discovery, read-only, claude-8, 2026-09-26 ~11:00Z, at futon2 87dfe212 and the map at futon3c ad3afb76
(74 boxes, 52 fields, 180 expected findings). Joe, on the harness figure: "the gaps between stages are
a critical finding."

## The finding, from the map

Of the map's 52 fields, 8 cross a step. Drawn in flight order, four steps are entered or left by no field:
the read step (row 2), the ask step (row 3), construction (row 4) and the rates (row 6). Between adjacent
steps, no declared field crosses the boundaries 2→3, 3→4, 4→5, 5→6 or 6→7a in the flight's direction.
The exemplar trace the map records for M-autoclock-in visits rows 1, dispatch, 2, 6, 7a, 8, 9 and back to 1;
it does not visit the ask step or construction at all.

So the map today declares each step's internal wiring and the loop's closing edges, and declares almost
none of the hand-offs. That is not what the code does; the code hands data across every one of those
boundaries. It does it through two carriers the map has no box for.

## The two carriers

**Carrier 1: the flight record**, threaded by `flight/run!` (`futon2/src/futon2/aif/flight.clj:286-360`).
Each step is a function the runner passes in; `run!` calls them in order and writes what they return onto
the flight map, which the next step receives:

| step | called as | reads from the flight | writes onto the flight | site |
|---|---|---|---|---|
| sources | `(sources-fn)` | — | `sources` (local) | `flight.clj` `run!` |
| read | `(read-fn f sources)` | `:want-source`, `:target`, `:clicks` | `:readings`, `:needs` | `flight.clj` `run!`; `flight_runner.clj:575` `read-fn` |
| wants | `(click-wants f sources)` | `:want-source`, `:carried-wants` | `wants` `{:wants :source :locators :universe}` (local) | `flight.clj:179` `click-wants` |
| ask | `(ask-fn f wants sources)` | `:wants`, the view (carrier 2) | `:asks`, `:needs` | `flight.clj` `run!`; `flight_runner.clj:369` `ask-fn` |
| click | `(click-fn (judge-opts f wants))` | `:target`, `:wants`, `:locators`, `:universe`, `:want-source`, `:click` | `result` `{:click-id :chosen :unreached-wants :abstention}` | `flight.clj:189` `judge-opts`; `flight_runner.clj:484` `http-click-fn` |
| enact | `(enact-fn f result)` | `:target`, result's `:chosen :candidate` | `:enactments` | `flight.clj` `run!`; `flight_runner.clj:767` `enact-fn` |
| W_c | `(wc-fn f enacted)` | the enactment | onto the enactment entry | `flight_runner.clj:881` `wc-verdict-fn` |
| record | `(record-click f …)` | result, wants, outcome | `:clicks` | `flight.clj:215` `record-click` |

Of these, the map declares `:chosen`, `:candidate`, `:enacted-steps`, `:attempts`, `:wc-verdict` and the
click entry's fields (rows 0, 5, 7, 10, 11): the hand-offs from selection onward. It declares none of
`:readings`, `:needs`, `:wants`, `:locators`, `:universe`, `:asks`, `:carried-wants`: the hand-offs from
the read step through the click. That is exactly the stretch the figure shows as islands.

**Carrier 2: the store's published view.** The read step and the ask step do not pass their results to
construction directly. They publish them (`reading/publish-criteria!` from `read-fn`;
`want-interpretation` `:272-273` writes `[:interpretations target :patterns id]` and `:receipts` after a
valid answer), and construction reads them back through `target-view` (`flight_runner.clj:230`), which
builds `{:universes {target facts} :interpretations {target {:patterns :receipts}}}` from the store. The
tick's construction then runs on that view: `flight-assembly-input` (`war_machine.clj:6037`) narrows the
tick to the flight's target and wants; `interpretation_construction/construct` builds candidates from the
patterns and writes `:construction-receipt` onto each candidate (`candidate_derivations.clj:62-94` reads
it; `interpretation_evidence.clj:297-325` requires exactly one). The rates (row 6) read the lane's admitted
labels inside the tick (`war_machine.clj:5889-5938`, `sourced-rates` → `:measurement`), not the flight.

So the ask step's isolation on the figure is real and explains itself: `:r3-flight-ask` writes
`:library-root` and `:r3-prompt` reads it, and those are the only fields the map gives row 3, because the
ask's actual output (`[:interpretations target :patterns]`) goes into the store and its actual input
(`:wants`, `[:universes target]`) comes from the flight and the store. Neither carrier is boxed.

## What declaring the hops would take

Each is one map edit plus the prover's textual check at the site, the way INSTANTIATE stage 1's hops
were done; one packet each, map test green at every commit, projection fixture re-pinned in the same
commit. Proposed, in flight order:

| hop | writer box (site) | field(s) | reader boxes |
|---|---|---|---|
| H1 read → flight | `flight/run!` (new box, `flight.clj`) | `:readings` `:needs` | `:r3-flight-ask` (reads `:needs`), the click's `judge-opts` |
| H2 wants | `flight/click-wants` (new box) | `:wants` `:locators` `:universe` | `flight/judge-opts` (new box), `:r3-flight-ask` |
| H3 ask → flight | `:r3-flight-ask` | `:asks` (`:asked`) | `flight/run!` |
| H4 ask → store | `want-interpretation` publish site (`:272`, new box) | `:interpretations` | `target-view` (new box), `:r4-constructor`'s caller (`interpretation_construction/construct`), `:r0-enact-step` (reads interpretations for the steps) |
| H5 view → construction | `target-view` | `:universes` | `:r3-flight-ask`, construction |
| H6 construction → candidate | `interpretation_construction/construct` (new box) | `:construction-receipt` | `candidate_derivations` (new box), `:r9-selection-law` |
| H7 flight → tick | `flight/judge-opts` | `:flight` (the tick's opts key) | `war_machine/flight-assembly-input` (new box) |

Two cautions. `:needs` and `:wants` are generic enough that the prover will report standing occurrences
across many boxes; that is the per-field-scope limit already on the record, not a reason to leave the hops
undeclared. And `:interpretations` has two textual writers (`want_interpretation.clj:272` and `:473`, the
declared and the fresh path); the map wants one writer per field, so the box should be the function
holding both sites or the field split as the code splits it. Both are for the packet author to settle
at the site, not here.

## What this changes in the proof

The harness figure will show these as lines the moment the map declares them; nothing is hand-drawn.
Until then the figure is right to show the gaps: the diagram proof attests declared wiring, and the
wiring from the read step to the click is not yet declared, only built.
