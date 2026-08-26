# Grounded feed: first missing input

## Witness tick

The first realized outcome in
`data/wm-trace/wm-trace-2026-07-04.edn:1` is:

```clojure
{:policy "M-bayesian-structure-learning"
 :expected-G -0.2
 :realized-G -0.5
 :tick 1783148628039}
```

That is a tick on which the old coverage producer demonstrably returned a
numeric outcome.

## First unavailable link: no accepted deposit

The current grounded path starts at `grounded-deposit`, which calls
`deposit-for-mission` (`src/futon2/aif/fold_realized.clj:118-125`). That reader
normalizes the requested mission id and searches the values returned by
`a3/deposits-by-id` (`src/futon2/aif/fold_realized.clj:103-108`).

For `M-bayesian-structure-learning`, there is a matching raw record:
`/home/joe/code/futon6/data/fold-turns/ft-bayesian-structure-learning-003.edn:252-253`
names mission `futon6-d/mission/bayesian-structure-learning` and fold-turn
`ft-bayesian-structure-learning-003`. Both its mission id and the trace's short
id normalize to `M-bayesian-structure-learning` through `a3/mission-key`
(`src/futon2/aif/actuator_a3.clj:140-141`).

But the record is not in the accepted deposit set. A throwaway-JVM call to
`fold-escrow/load-deposits` reports it as `:prompt-not-reconstructable`: its
stored prompt hash is
`523ca024a2fc8de4dd7447ad6bb4ad1ebc7dd0046906679a0b367a692c46fd99`, while
the current reconstruction is
`2f7afa04e53547e12ff22dcdfafb072b1d794d57771cefc947c843f1d93a9ec0`.
`load-deposits` records such validation failures under `:rejected`
(`src/futon2/aif/fold_escrow.clj:176-194`), and `a3/deposits-by-id` throws when
that collection is nonempty (`src/futon2/aif/actuator_a3.clj:143-152`). Thus
`deposit-for-mission` does not return a deposit and the actual call never
reaches `a3/box-match-snapshot`.

There is a second, independent absence behind that first failure. Direct EDN
inspection of the raw fold-turn shows neither `:clean` nor `:box-bindings`, and
`M-bayesian-structure-learning` has no entry in either
`a3/reviewed-candidate-cleans` (`src/futon2/aif/actuator_a3.clj:372-379`) or
`a3/reviewed-box-bindings` (the map begins at
`src/futon2/aif/actuator_a3.clj:63`). Even if the raw record were admitted,
`box-match-snapshot` obtains its rows solely from `box-bindings` and would have
zero rows (`src/futon2/aif/actuator_a3.clj:333-345`): `bound = 0`,
`inhabited = 0`, and `realized-score = nil` by
`src/futon2/aif/fold_realized.clj:159-173`.

## Measurement or unread record?

**This is a measurement nobody produces.** A historical fold-turn record does
exist, but it is rejected by the current deposit contract and, more
importantly, it contains no reviewed CLean or box-to-substrate bindings from
which the A5 endpoint measurement could be made. There is no already-produced
grounded snapshot elsewhere that this reader merely overlooks. The missing
input is an accepted, reviewed grounding specification for this mission; that
specification is what would let `box-match-snapshot` measure inhabited
endpoints.

No live service or R10 loop was used. The only evaluation was in a throwaway
JVM in `/home/joe/code/futon2` against repository files.
