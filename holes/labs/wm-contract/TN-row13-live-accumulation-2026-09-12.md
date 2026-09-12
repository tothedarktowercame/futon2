# TN: row 13 live accumulation feed

Date: 2026-09-12. Status: discovery only. No source, registry, live state, or
generated artifact is changed by this note.

## 1. Anchor

The smallest correct anchor is immediately after the inner belief loop returns,
at `scripts/futon2/report/war_machine.clj:6280`. The tick's fourteen-channel
`observation` was bound once by the production observer at
`scripts/futon2/report/war_machine.clj:5973`; that same immutable local remains
in lexical scope. The final post-update seven-status posterior is bound as
`wm-belief` at `scripts/futon2/report/war_machine.clj:6280`, after the terminal
inner step selected `belief'` at `scripts/futon2/report/war_machine.clj:6262-6279`.
Thus line 6228 is an update inside a possible multi-step loop, not the integration
anchor: the stable μ is the loop result at line 6280. Both values are already
joined into `wm-state` at `scripts/futon2/report/war_machine.clj:6295-6299`, and
the final judgement carries the identical pair at
`scripts/futon2/report/war_machine.clj:6744-6747`.

The implementation should call `machine-accumulation/step` in the outer `judge`
let after line 6280 and before `wm-state` at line 6295. Its inputs are the local
`observation` and a deliberately selected entity posterior from `wm-belief`;
selection must obey the single-entity machine-model contract rather than average
entities. Row 12's function already checks both coordinate supports and refuses
negative/nonfinite increments at `src/futon2/aif/machine_accumulation.clj:27-34`.

## 2. Carry authority

There are three nearby candidates.

1. **The WM daily trace is the recommended authority.** `judge` already loads
   recent records in chronological order and names the previous record at
   `scripts/futon2/report/war_machine.clj:6023-6027`. The writer constructs one
   record containing the exact evaluated `:mu-post` and `:observation` at
   `src/futon2/aif/trace.clj:543-553`, then appends it through the indexed trace
   writer at `src/futon2/aif/trace.clj:768-783`. It survives JVM and process
   restarts as EDN files. `recent-trace-records` returns bounded chronological
   history at `src/futon2/aif/trace.clj:849-864`; `latest-trace-record` also
   crosses UTC-day and pause boundaries at `src/futon2/aif/trace.clj:876-904`.
   Existing precision, selection-gain and habit state already use this
   previous-record pattern (`scripts/futon2/report/war_machine.clj:6042-6053`,
   `scripts/futon2/report/war_machine.clj:6102-6129`). Add an
   `:accumulation-state` field containing row 12's entire returned state,
   including `:last-tick`; add a current tick identity and `:previous-id` to the
   update-input record. The prior record is directly readable as
   `prev-trace-record`, so `:carry-chain-gap` is enforceable before scoring.

2. **Belief carry is a transformation, not a store.**
   `belief/reconcile-belief-carry` takes the prior trace's `:mu-post` and a fresh
   domain (`src/futon2/aif/belief.clj:510-522`); its caller supplies precisely
   that trace field at `scripts/futon2/report/war_machine.clj:6058-6063`. It
   survives restart only because the trace survives. Extending this pure
   reconciliation function with a differently indexed concentration matrix
   would conflate belief and learning state and would not add durability.

3. **The whole-loop run record is not the tick carry.** It writes one atomic
   terminal wrapper record under `tick-run-record-<run-id>.edn` at
   `src/futon2/aif/full_loop_runner.clj:377-435`. Its record body contains route,
   selection/terminal and execution metadata (`src/futon2/aif/full_loop_runner.clj:396-426`),
   not the WM observation/posterior pair. The call happens only at wrapper exit
   (`src/futon2/aif/full_loop_runner.clj:3883-3888`). It survives restart but is
   neither the scheduled tick chronology nor readable as `prev-trace-record`;
   making it authoritative would create a second, incomplete carry chain.

In-JVM atoms are also unsuitable: the nearby caches are declared at
`scripts/futon2/report/war_machine.clj:753-768` and disappear on restart. The
trace is already the explicit durable-state authority; no new atom or ledger is
needed.

## 3. Minimal implementation diff

Only the following production shapes are needed:

* Require `futon2.aif.machine-accumulation` in
  `scripts/futon2/report/war_machine.clj` and, after line 6280, construct the
  update-input capture from `observation`, the selected posterior, the current
  tick identity, and `trace-record-identity prev-trace-record`. That identity
  function deliberately chooses `:run/id` then timestamp at
  `scripts/futon2/report/war_machine.clj:1710-1718`.
* If `prev-trace-record` exists, require and validate its
  `:accumulation-state`; do not initialize when that field is absent. Row 12's
  `step` refuses a missing carried state and a nonmatching predecessor at
  `src/futon2/aif/machine_accumulation.clj:27-30`. A first-ever trace record may
  call `initialize`, but only from an explicit configured initialization record;
  row 12 labels that authority and prior at
  `src/futon2/aif/machine_accumulation.clj:8-20`. Persist that declaration beside
  the first result. A pre-existing trace with no state is migration-required,
  not a cold start.
* Add `:accumulation-state` and `:accumulation-update-input` to the judgement
  near the other carried states at `scripts/futon2/report/war_machine.clj:6766-6771`,
  and propagate them present-only in `trace-record` beside the existing carried
  fields at `src/futon2/aif/trace.clj:697-729`. The update-input must retain
  `{:tick-id ... :previous-id ... :entity/id ... :observation ... :belief ...
  :support ... :model/revision ...}`; the state retains the complete table,
  `:last-tick`, and initialization declaration.
* `write-trace-and-clock!` already writes before downstream rationale/clock
  work (`scripts/futon2/report/war_machine.clj:2309-2333`). A failed state read
  or `step` refusal must throw before the normal judgement reaches the write at
  `scripts/futon2/report/war_machine.clj:6845-6874`; it must not emit a normal
  trace record containing a reset table.

No edit belongs in `a4a.clj`, the existing belief update, EIG machinery, or the
full-loop terminal record.

## 4. Evidence plan

Use the on-demand click with a redirected temporary `:trace-dir`; `judge`
already takes `trace?`, `trace-dir`, and `run-id` at
`scripts/futon2/report/war_machine.clj:5959-5964`, and the writer honors the
redirect at `scripts/futon2/report/war_machine.clj:2324-2328`. Seed that
directory with an explicit initialization-bearing predecessor, then run three
machinery-test ticks. For each written record, assert:

* the update input equals the tick's persisted `:observation` and selected row
  of `:mu-post` (their single-source construction is
  `src/futon2/aif/trace.clj:543-553`);
* `previous-id` equals the exact preceding trace identity and the output
  `:last-tick` equals this record's identity;
* every concentration coordinate equals prior coordinate plus `o×μ`, with raw
  IEEE deltas retained; and
* deleting the predecessor state, skipping its identity, and mutating either
  support produce respectively missing-carry, `:carry-chain-gap`, and
  `:support-mismatch`, with no successful trace append.

The same `:accumulation-update-input` retention point should serve row 15's
belief-update measurement proof. It captures the exact `observation` and
post-update `belief` simultaneously at the anchor, while the record also keeps
`:mu-pre`/`:mu-post` (`src/futon2/aif/trace.clj:524-553`). Row 15 can add its
belief-update-specific event/prediction context to the shared envelope rather
than evaluate observation or serialize μ a second time. The shared shape is:

```clojure
{:tick-id id :previous-id previous-id :entity/id entity
 :observation observation :belief-pre selected-pre
 :belief-post selected-post :state-support statuses
 :observation-support channels :model/revision revision}
```

This sharing is byte identity of inputs, not evidence that accumulation and
belief update are the same function.

## 5. Registry boundary

Do **not** change `aif-equations.edn` in discovery or implementation. The two
edges remain recorded `:not-realised` at
`holes/labs/wm-contract/aif-equations.edn:1041-1042` until the live machinery
test and independent review succeed. Flipping R1→R17/R2→R17 is a canonical
registry edit and requires the TN-9a second-read gate; it is a later, separately
authorized accounting action. The target ruling itself remains at
`holes/labs/wm-contract/aif-equations.edn:210-215`.
