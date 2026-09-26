# Historical construction receipt for Cτ discovery

Archived by codex-2 on 2026-09-26 during Joe's authorised sequential PROOF-2a
continuation. This is an existing record, not a new flight, replay, or test.

`tick-run-record-2026-09-26-flight-7f89646a-click-1.edn` is an exact byte copy
of the previously untracked `data/wm-runs/` file of the same name:

- 128505 bytes;
- SHA-256 `a8e04fb97e58808e8fabdb4ab771f3c414b4181ef82dac336729dd472a18d816`;
- `:run/id "2026-09-26-flight-7f89646a-click-1"`;
- `:startedAt "2026-09-26T02:19:28.673524329Z"`.

This is the hash cited in futon3c `holes/labs/M-wm-wiring/CTAU-D.md` §1(d).
The copy was compared byte for byte with that source and parsed as EDN.
The original namespace-load/source-digest metadata is retained. Archiving
does not certify that the record was produced by today's HEAD.

## What it establishes

The primary candidate is at:

```clojure
[:habit-reads :occurrences 1 :consumption :candidate-ids 0]
```

Its `:construction-receipt` records target `M-autoclock-in`, one
`:compose-by-need` move with value 1.25, pragmatic contribution 1.25,
epistemic contribution 0, cost 0, and one shared eight-token universe.
The order has seven units, `:horizon` is 4, `:budget-used` is 1 and
`:family-searched` is 2. Four wants are unreached: one `:no-producer` and
three `:beyond-horizon`. `:stopped-is-not-success` is true.

The other seven `:machine-constructed` receipts occur under this candidate's
`:interpretation-receipts`, each at `:validated :candidate
:construction-receipt`. They are validation receipts nested in the same
record, not seven additional observed flight clicks.

At futon2 `f4b99b544`, `interpretation_construction.clj`'s `construct`
builds the supported family and its move proposes that whole family as
`:proposed-family`; if it equals the current family, the generator returns
`:no-move` with `:family-already-constructed`. `construction.clj` records
`:no-admitted-move` when no move is admitted on the next iteration. Thus
the recorded stop after taking a move is consistent with that code; it is
not evidence that the useful move was rejected. The strict positive-value
admission rule remains in force.

## What it does not establish

The top-level decision explicitly records `:chosen {:status :absent
:reason :no-chosen-action}` and `:abstention {:status :absent :reason
:no-selection-decision-recorded}`. The receipt was retained in a habit-read
occurrence whose purpose is `:joint-selection`. It does not show an enacted
action, completed flight, or later consumption of an intermediate token.

This supplies a committed historical example of the proposal grain and of
typed beyond-horizon wants. It does not establish general adequacy under
measured A, any chain/detour/idle ordering, the current preference placement
in an observed run, or satisfaction of the theorem's intermediate-progress
requirement. Those obligations remain open in PROOF-2a.
