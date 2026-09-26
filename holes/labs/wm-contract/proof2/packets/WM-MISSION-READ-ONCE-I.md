# WM-MISSION-READ-ONCE-I — one mission read per selection, not two

**For claude-10's queue, after WM-MISSION-READ-CACHE-I (DONE, `dcb3481a`).**
Written by claude-3, 2026-09-26, answering Joe's question on
`F1B-ENTITIES-FLAGS-I`: "do we really need to read twice, that seems strange?"

## The answer, on the record

**No. The two reads are of the same registry at the same point in one
selection, and the second re-reads what the first already had.** Both are in
the SAME `let` of `generate-war-machine`
(`scripts/futon2/report/war_machine.clj`), one thread, sequential, with
nothing between them that writes a mission:

| # | site | what it loads | what it uses |
|---|---|---|---|
| 1 | `war_machine.clj:7219`, inside `declared-sources` (`:7213`) | `(mission-registry/load-missions)` — the uncached zero-arg read | `(:missions loaded)`, passed to `mission-hole-wants/merge-into-sources` |
| 2 | `cascade_problems.clj:45` via `war_machine.clj:7241` | `(registry/open-missions)` → `(open-missions (load-missions))` — the same uncached read | `(map :id (filter live-mission? (:missions loaded)))` |

Read 2's value is a pure function of read 1's: `open-missions`' one-arg arity
(`mission_registry.clj:500-505`) already computes it from a loaded doc. They
are meant to be one snapshot — the cascade's targets and the sources derived
from missions describing the same registry is what makes the selection
coherent; if the two reads disagreed the decision would be built from two
registry states, which is a defect and not a feature.

## Why it is not a one-line pass-through (so: a packet, not an edit)

Three things, each small, none a one-liner:

1. **The first read is behind a guard.** `declared-sources` is
   `(when-not (:cascade-sources judge-opts) …)` (`:7213`), so when judge-opts
   supplies `:cascade-sources` nothing is loaded at site 1 at all. The loaded
   doc has to become its own `let` binding, evaluated unconditionally, before
   either use.
2. **`declared-sources` consumes the missions, it does not keep them.** There
   is no already-loaded value in scope to pass on; the vector is fed straight
   into `merge-into-sources`.
3. **`substrate-targets` takes no arguments and has no opts key.**
   `cascade_problems.clj:37-47` is zero-arity, and it also loads tickets. It
   needs a new arity taking the loaded missions doc — mirroring
   `open-missions`' own one-arg arity, which is the precedent.

## Why the cache does not already cover this

`WM-MISSION-READ-CACHE-I` fixed the stamp, so `load-missions-cached` really
does coalesce calls inside its TTL. It cannot coalesce **these two**:

- both sites call the **uncached** zero-arg `load-missions`, not
  `load-missions-cached`, so today neither touches the cache at all; and
- switching them to the cached read would not be enough either, because the
  TTL is 5 s (`mission_registry.clj:469-475`) and the two reads are about **one
  substrate read apart** — the futon1b mission read measured 19–20 s on
  2026-09-26, ~10–12 s after futon1b `c95a8f8` (hydrate by equality) is live,
  and around 9.5 s with `include-total=false&ordered=false` (window ~2 s +
  hydrate of 331 ids by `IN` ~7.4 s;
  `futon1b/TN-entities-speedups-2026-09-26.md`). All of those are well over
  the TTL, so read 2 would miss the cache and go to the substrate anyway.

So the caller switch that "goes after the cache packet" is not the fix here;
**passing the loaded doc is.** Raising the TTL would also work and is worse:
its docstring keeps it short deliberately, so a stale store is re-read within
one selection rather than hidden.

## The change

- `cascade_problems.clj`: add `(substrate-targets loaded-missions)` beside the
  zero-arity, computing mission ids from the given doc, tickets as now. Keep
  the zero-arity delegating to the loaded read, so
  `work_target_predictor_input.clj:35`'s reference and any test caller are
  unchanged.
- `war_machine.clj`: bind the loaded doc once, above `declared-sources` and
  unconditionally; use it at `:7219` and pass it at `:7241`.
- No new field, no new refusal, nothing for the register: this removes a read,
  it does not change what is recorded.

## Test box

`mission-read-once-test`:
1. with `load-missions` counting its calls and `judge-opts` carrying NO
   `:cascade-sources`, one selection makes **one** call, not two;
2. with `judge-opts` carrying `:cascade-sources` (site 1 guarded out), the
   targets are still the live mission ids — i.e. the unconditional binding did
   not make the guard's absence lose the targets;
3. the bad case, which is the point: revert the pass-through and the counter
   reads 2. A test that only asserts the target list would pass either way.

## Not established here

Whether any other phase of one click reads the registry a third time.
`full_loop_runner.clj:1451-1453` (`mission-entry`, construction) is a separate
read in a separate phase and is outside this packet.
