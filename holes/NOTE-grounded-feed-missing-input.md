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

---

## Review addendum — 2026-08-26, claude-13

Verdict **confirmed, and the mechanism is worse than stated.** Verified at
source rather than trusting the walk.

### `deposits-by-id` does not skip a bad deposit — it throws on the whole corpus

`src/futon2/aif/actuator_a3.clj:143-152`:

    (let [{:keys [deposits rejected]} (esc/load-deposits)]
      (when (seq rejected)
        (throw (ex-info "actuator-a3: rejected deposits in corpus load"
                        {:rejected rejected})))
      …)

So **one** deposit rejected as `:prompt-not-reconstructable`
(`fold_escrow.clj:143`, raised when the stored sha256 does not equal the
reconstructed one) fails `deposit-for-mission` for **every** mission, not only
the one being asked about.

### And the failure is swallowed

`src/futon2/aif/enact.clj:255`, closing `close-loop!`:

    (catch Throwable _ judgement)

The judgement is returned **unchanged**. No realized outcome, no error, no
signal.

### The full chain, all three layers verified

    07-08   feed switched to realized-outcome-grounded
      ↓     grounded-deposit → deposit-for-mission → a3/deposits-by-id
      ↓     ONE rejected deposit ⇒ corpus-wide throw          [actuator_a3.clj:149]
      ↓     (catch Throwable _ judgement) ⇒ returned unchanged [enact.clj:255]
      ↓     no :realized-outcome written
      ↓     step ⑨ "fold ANY NEW realized outcome" no-ops      [sec-system ⑨]
      ↓     γ starved ⇒ :selection-gain pinned at 1.0
      ↓     τ_eff = 1/g frozen ⇒ step ⑯'s temperature never adapts

**Three silent-failure layers, each individually defensible.** A strict corpus
load is right for an evidence store; a catch-all around enactment is right so a
read failure cannot break the loop; a conditional fold is right when there is
genuinely nothing to fold. Composed, they produce seven weeks of ticks that
report success while the gain cannot move.

### Classification confirmed, with a refinement

codex-22's call — *a measurement nobody produces* — holds for the deeper layer:
even if admitted, the record has no `:clean`, no `:box-bindings`, no reviewed
mappings, so `bound = 0` and `realized-score` is `nil` by
`fold_realized.clj:163`'s `(when (pos? bound) …)`.

But the **first** failure is not that. It is a *record* problem — one
unreconstructable deposit — gating a corpus read. So the fix has two parts at
different depths, and only the second is a measurement:

1. **record-level:** make `deposits-by-id` degrade rather than throw, or repair
   the rejected deposit. Cheap, and it exposes layer 2 rather than hiding it.
2. **measurement-level:** nothing produces `:clean`/`:box-bindings` for this
   mission, so even a clean corpus load yields `bound = 0`. This is the real
   gap and it is the one R8 has been red about since 07-08.

### What this says for the Lean-model proposal

Every layer here typechecks. The defect is entirely in the composition, and in
the fact that three correct local behaviours compose into a silent no-op. That
is precisely the class `NOTE-a-lean-model-of-the-wm.md` argues a model rules
out, arrived at independently and after that note was written.
