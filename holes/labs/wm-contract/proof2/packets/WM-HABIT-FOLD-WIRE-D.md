# WM-HABIT-FOLD-WIRE-D — where the folded habit lives between the flight and the tick

Discovery, read-only. claude-10, 2026-09-26, for claude-8. Read at futon2
d51f5d72 and futon3c 042a0570. No code changed, no flight, no click.

The wire is `[:r7-fold :r7-selection :enactment-records]`. WM-WIRE-TEST-I
(futon3c 042a0570) witnessed it only hermetically. Nothing outside tests calls
`enactment-habit/fold`, and the joint selection passes no `:enactment-fold`,
so every live selection has read the empty fold, uniform E. Each of the three
recorded habit reads says `{:status :absent :reason :no-enactment-fold}`.

The design question: the increment is made in the flight's process, and
selection happens in the tick, a POST into the serving JVM's runner. So the
habit has to live somewhere both can reach.

## What the code does today

**The fold is pure and has no caller.** `enactment_habit.clj` says so in its
namespace docstring: "Pure: no file, store or data/ access."
- `increment` (:66-98) turns one enactment record, its policy key and its W_c
  verdict into one receipt: `{:record-id [click candidate] :delta 0|1
  :policy-key … :attempts … :deviations … :basis :wm/enactment-habit-v1}`.
  `:delta` is 1 only on an empty failure vector; anything else gives 0, with
  the failures or the verdict's typed status kept.
- `fold` (:99-117) reduces receipts into a cascade-prior state. It counts a
  `:delta 1` receipt once per `:record-id`, lists repeats under `:duplicates`,
  and keeps the counted receipts under `:enactment-records`.
- A grep of futon2 `src/` and `scripts/` finds no call to `fold` outside the
  namespace, so nothing persists a folded state.

**Selection's contract** (`policy.clj:365-395`, `select-action-cascades`):
- It reads `:enactment-fold` from its opts, a cascade-prior state or nil.
- nil is the empty fold, which gives uniform E.
- `cascade-habit-store/attach-state` records what selection consumed on the
  habit-read receipt: `:status :present`, or `:absent` with `:reason
  :no-enactment-fold`, plus the `:state`, its EDN and sha256.
- The selection law records `:e-source {:source :enactment-fold :records n
  :samples s :uniform b}`.
- `:habit-state {:source :recorded-run}` is for replay only.

**The joint selection** (`war_machine.clj:6640`) calls
`select-action-cascades` with `:beta`, `:beta-state`, `:cascade-habit-path`,
`:ticket-queue`, `:ticket-queue-refusals` and `:novelty-inputs`, and no
`:enactment-fold`.
- `:cascade-habit-path` is still passed, but selection has stopped reading it
  (policy.clj:371-376).
- The fold would reach selection in the same options map, taken from the
  judge's options the way `:ticket-queue` is. I haven't traced every hop from
  `generate-war-machine`'s `judge-opts` to that map; the packet below has to.

**The increment in the flight** (`flight_runner.clj:881-920` `wc-verdict-fn`,
called by `flight.clj:345`):
- After `enact-fn` writes the enactment record, the W_c checker runs on the
  click's run record and the enactment record.
- Its verdict goes to `increment` unchanged.
- The result, `{:wc {:verdict … :click-record …} :increment <receipt>}`, is
  merged into the flight's `:enactments` entry for that click
  (`flight.clj:346-356`).
- With no checker: `{:wc {:absent :no-wc-checker-configured}}`, and increment
  is not called.

**Where the records go** (`flight_driver.clj:198-262`, `run-flight!`):
- **Enactment record:** `<store>/flights/enactments/<flight-id>-<click-id>.edn`.
  `enact-fn` writes it when it runs, so it is durable per click. It carries the
  attempts, checks, grain and `:decision-candidate`. It does **not** carry the
  W_c verdict or the increment.
- **Flight record:** `<store>/flights/<flight-id>.edn`, written by `write!`
  once, when the flight ends, or on abort (WM-SPIKE-FIX-III). Until then the
  flight lives in `run!`'s volatile (`keep!`, flight.clj:310), in the driver's
  process only.
- `<store>` defaults to `want-interpretation/default-store`,
  `/home/joe/code/futon2/data/wm-interpretations`. That is the same directory
  the tick already reads for machine interpretations
  (`war_machine.clj:7237`, `(or (:machine-interpretations-dir judge-opts)
  want-interpretation/default-store)`).
- Today it holds seven flight records (323,279 bytes). No
  `flights/enactments/` directory exists, because no flight has enacted.

**The click request** carries `(:flight judge-opts)`, which `flight/judge-opts`
(flight.clj:189-199) builds as the flight's id, target, wants, locators,
universe, want source and click number. It carries no enactments. futon3c
`http.clj:8955-8961` reads it into the runner's `:flight`.

**The legacy store** is `data/wm-habit/cascade-prior.edn`
(`cascade-habit-store/default-path`, 2,072 bytes, last written 2026-09-23
21:44).
- It holds selection-time counts from `record-selection!` (no caller left
  outside its own arity) and close-rule counts from
  `cascade-habit-reinforcement/close!`. The close rule counts a close whose
  outcome comparison has a `:predicted-and-observed` token.
- Neither rule is W_c-gated, and neither counts enactments.
- Its counts include `M-f11-…`, a target withdrawn on 2026-09-25.
- The runner still writes it: `full_loop_runner.clj:4606` calls `close!` with
  `(or (:cascade-habit-path opts) cascade-habit/default-path)`. Since step 8
  nothing reads it, so the runner keeps writing a file nobody reads.

**The delta's rule** (WM-EQUATIONS-DELTA-D §2.5, futon2
`holes/labs/wm-contract/WM-EQUATIONS-DELTA-D.md:247-265`): E is counted from
Clause C enactment records, at most one count per record, only when W_c
passes, and deduplicated by click × candidate. Node R17 feeds R6. The counting
rule is not formalised in Lean (`:absent :counting-rule-not-formalised`).

## (1) Where the folded state exists today after a flight's W_c verdict

**Nowhere durable, and nowhere at all as a folded state.** After a verdict:
- The increment receipt exists in the flight's in-memory record.
- It reaches disk only when the flight record is written, at flight end or
  abort, under `:enactments[i] :increment`.
- The enactment record on disk has no verdict.
- No code folds the receipts.

So a later click in the same flight cannot see an earlier click's increment by
any route. A click in a later flight could, only by reading the earlier
flight's record, and nothing does.

## (2) Candidate homes between the flight and the tick

| home | writer | reader | what it costs per click | against it |
|---|---|---|---|---|
| **a. The flight records in the interpretation store** (`<store>/flights/*.edn`, `:enactments[].:increment`), folded by the tick | the driver's `write!` (exists) | a new reader at the tick, feeding `fold`, then `:enactment-fold` | read and parse every flight record: today 7 files, 323 KB, growing linearly with flights; the fold itself is one pass over at most one receipt per enacted click | a click sees nothing from earlier clicks of its own flight until the record is written per click (see (4), follow-up); a linear read that will need an index later |
| **b. A folded-state file the tick reads** (e.g. `data/wm-habit/enactment-fold.edn`) | the flight, after each increment, under a lock (as `cascade-habit-store/publish!` does) | the tick at selection | one small file read; one locked write per enactment | a second, mutable copy of what the receipts already say: it can drift from them (a re-checked verdict, a deleted flight record) and nothing can check it against them; two flights in parallel contend on it; it adds file IO to the one namespace whose docstring says it has none, unless it goes in a new one |
| **c. The click request** (the flight's increments in `:flight-edn`) | the flight | the tick, from the POST body | a few hundred bytes a click | only the current flight's enactments, so E forgets every earlier flight; and the prior becomes whatever the caller posts: any POST to `/api/alpha/wm/click` could set E |
| **d. The run record** | the tick | the flight | none | the wrong direction: the run record is the tick's output, closed before the enactment it would have to count. It is where selection *records* what it consumed (the habit-read receipt already does), not a place to keep E |
| **e. The legacy store** (`data/wm-habit/cascade-prior.edn`) | the runner's close rule (still) | nothing | none today | a different counting rule (not W_c-gated, not enactment-grained), history with withdrawn targets in it, and a tick-side writer. It would make E answer to two rules at once |

## (3) Recompute at the tick, or carry the fold

**Recompute at the tick, from the increment receipts the flight records
already hold. Don't carry a folded state.**
- `fold` is pure, cheap and idempotent over the same receipts: it deduplicates
  by `[click candidate]`, so folding all receipts again at each click gives the
  same state as folding incrementally.
- The receipts are the one authority for E. Each carries its W_c verdict's
  outcome (`:delta`, with failures or the typed status) beside the click
  record's path under `:wc`. A carried fold (home b) would be a second record
  of the same facts that nothing can check against the first.
- The tick already has a place to record what it consumed: the habit-read
  receipt's `:state` and sha256, and `:e-source`. Recomputing leaves that
  receipt exactly as the source of E.

**Which receipts, from where:**
- the `:increment` values under `:enactments` in every flight record in
  `<store>/flights/*.edn`;
- where `<store>` is the machine-interpretations directory the tick already
  reads (`:machine-interpretations-dir`, default
  `futon2/data/wm-interpretations`).

A receipt that is refused, or has `:delta 0`, counts nothing; `fold` already
does that. What those records cannot give, until the flight record is written
per click, is the current flight's own earlier clicks.

## (4) The one-behaviour packet

### WM-HABIT-FOLD-CALL-I (claude-10's queue)

**Behaviour.** At the joint selection, E is folded from the flights' increment
receipts:
- A reader returns the increment receipts from the flight records in the
  machine-interpretations store, plus the paths and sha256s it read. It lives
  in a new small namespace, so `enactment_habit.clj` stays free of file access,
  as its docstring says.
- `enactment-habit/fold` folds them.
- The result is passed as `:enactment-fold` to `select-action-cascades` at
  `war_machine.clj:6640`.
- A `:enactment-fold` already in `judge-opts` (tests, replay) wins.

**On the record:**
- **No flight records**, or none with a receipt: selection gets the fold of
  nothing, supplied. The habit read then says `:status :present` with `:samples
  0`, which is distinct from today's `:absent :no-enactment-fold` (no fold
  passed at all).
- **A flight record that doesn't parse:** a typed entry on the read's
  provenance (path, reason). Never a skipped file, and never a refusal.
- **Provenance:** the paths and shas read go on the habit-read receipt as
  provenance, so what E was counted from can be checked from the run record.
  This is not a required field, and no refusal is added.

**Not in this packet:**
- Writing the flight record after each click, so a click sees its own flight's
  earlier increments. That's a separate behaviour in `flight_driver`/`run!`:
  WM-FLIGHT-RECORD-PER-CLICK-I, next.
- The runner's close rule still writing the legacy store (a question for
  claude-8: retire or keep as history).
- `[:r7-fold :r1-outer-cascade :enactment-records]`.

**Tests:**
1. A temp store holding one flight record whose click has a `:delta 1`
   increment. Through `judge`, stopped after selection as
   `mission-read-once-test` does, `:e-source` reads `{:records 1 :samples 1
   :uniform false}`, and the habit read names the flight record by path and
   sha256.
2. The bad cases:
   - a `:delta 0` receipt and a refused receipt count nothing;
   - a duplicate `[click candidate]` across two flight records counts once;
   - an unparseable flight record is typed on the provenance.
3. With the reader removed, the habit read is `:absent :no-enactment-fold`
   again. Pre-fix `war_machine.clj` loads from `git show` under a renamed
   namespace, never stashed.

**Map.**
- A box for the new reader, reading `:increment`, and one for the flight's
  writer of it (`wc-verdict-fn`, into the flight record through `run!`). This
  is a hand-off across the flight/tick boundary, of the kind WM-HANDOFFS-D
  lists.
- `[:r7-fold :r7-selection :enactment-records]` keeps its boxes, and gains a
  production caller.

**Wire test.** WM-WIRE-TEST-I's wire 1 moves from `:witnessed-hermetically`
to `:verified` the first time a flight enacts with a W_c checker configured.
Its pin is that flight's run record, whose habit read then carries the folded
`:enactment-records`, beside the flight record that holds the receipts.
