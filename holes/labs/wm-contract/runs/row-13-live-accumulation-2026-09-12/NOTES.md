# Row 13 live accumulation completion

## Caller census

The on-demand click path constructs traced judge options at
`scripts/futon2/run_tick_once.clj:246-276`, obtains the reason-bearing selector
at lines 285-289, and calls the complete judge at lines 290-292.  Its options
now merge the single committed configuration at line 272.

The scheduled production runner calls the complete judge at
`scripts/wm_scheduled_run.clj:98` and performs its trace append at lines
113-117.  It now supplies that same committed configuration.  The other
production judge call found by the census is the full-loop selection call at
`src/futon2/aif/full_loop_runner.clj:2982-2998`; it does not request or append a
War Machine trace and is therefore not an accumulation persistence caller.

The one configuration record is
`holes/labs/wm-contract/machine-accumulation-config.edn:1-6`.  Loading and
validation are centralized at `scripts/futon2/report/war_machine.clj:82-92`.
The accumulation anchor is lines 6327-6334 and the persisted judgement fields
are lines 6798-6803.  A caller that does not supply an entity still refuses at
the accumulation identity boundary; there is no per-caller entity or prior.

## Full-judge machinery evidence

`full-judge-evidence.edn` is the retained result of three complete redirected
judge calls, not direct calls to the accumulation seam and not a qualifying or
live run.  Its chain is `row13-full-1`, `row13-full-2`, `row13-full-3`.  It
compares all 294 coordinates (14 observation channels times seven states times
three ticks), retains raw IEEE deltas, and records zero as `0.0` only where the
actual subtraction was zero.  The three copied-trace controls refused before
append with `:accumulation-migration-required`, `:carry-chain-gap`, and
`:support-mismatch` respectively.

The first attempted full run is also material: it refused before trace write
because the harness had not supplied `:strategic-selection-fn`.  The committed
harness now obtains the same reason-bearing selector seam as the on-demand
runner (`scripts/futon2/run_tick_once.clj:102-111,185-193`) and the successful
evidence records which seam resolved.

## Reload and migration

After reload, an existing daily trace whose newest record lacks
`:accumulation-state` will refuse `:accumulation-migration-required` at
`scripts/futon2/report/war_machine.clj:1743-1748` before the write at the trace
boundary.  This is intentional and must not be discharged by retrying, deleting
history, or silently treating the tick as a first record.

The exact operator migration is: stop the sole trace writer; copy the current
daily trace as a byte-for-byte backup; read back its newest complete record and
record its `:run/id` (or `:timestamp` when absent); construct an explicit state
with `futon2.aif.machine-accumulation/initialize`, using the sorted keys of that
record's `:observation`, the sorted keys of the configured entity's `:mu-post`
row, and the declared prior/model revision from
`machine-accumulation-config.edn:3-4`; set that state's `:last-tick` to the
recorded identity; add the state plus the unchanged explicit initialization
record to that predecessor; atomically replace the daily file; read it back and
verify the identity, supports, authority, and all 98 initial coordinates before
starting the writer.  The first live tick then names exactly that predecessor
and performs a normal recurrence step.  Any mismatch leaves the writer stopped
and the original backup authoritative.  This is a one-time, reviewed migration
of the carry record, never an initialization fallback in the live judge.

Canonical edge flips for R1->R17 and R2->R17 remain outside this packet and
require the TN-9a second-read gate.
