# C509 — the inter-tick state boundary (worklist `:U54`)

Date: 2026-09-04. Row: `U54` (class `:D`, discovery). Epic: `EPIC-run-era.md`,
"the step era precedes the continuous era".

Machine-readable census: `C509-inter-tick-state-census.edn` — 65 items, each
with a `file:line` pointer, a snapshot-ability class, and either a named
restore mechanism or a typed gap. This file is the account of what the census
found. **No behaviour changed**: nothing under `src/` or `scripts/` was
edited, no producer was added, no flag default moved, no machine run was taken
and no run lock was held.

## Subject, and what it excludes

The tick is the one `futon2.run-tick-once` runs — the tick the U55 stepper will
drive. The scope is its transitive `:require` closure, 37 namespaces, computed
from the `ns` forms rather than assumed. Seven namespaces that own durable
stores are **not** in it: `tripwire`, `repair-obligation`, `actuator-a3`,
`actuator-a6`, `enact`, `close-loop`, `fold`. So `data/wm-tripwires`,
`data/wm-repair-obligations`, `data/actuator`, `data/fold-escrow`,
`data/wm-workspace-gate` and `data/wm-full-loop` are outside this checkpoint.
They come back the moment the stepper is pointed at the full-loop click path,
and that is a different census.

One edge leaves the closure dynamically: the live selector is reached by
`requiring-resolve` (`scripts/futon2/run_tick_once.clj:18-19,57-67`), so
whether it resolves is a property of the classpath, not of any file a pin would
copy. It is censused as item S5.

## What the machine already checkpoints, and it is more than the last record

Five fields are read out of the previous trace record and are present in all
four records of the last recorded run (`runs/2026-09-04-re5/wm-trace-re5.edn`):
`:mu-post` (belief), `:precision-state`, `:selection-gain`,
`:habit-prior-state`, `:morning-brief-consumed-event-ids`. Two more are read by
code that runs today but are absent from what the machine writes:
`:realized-outcome` appears in exactly five daily files
(`wm-trace-2026-07-02..06.edn`) and nowhere after, so γ holds at its prior; and
`:policy-precision-state` appears in exactly one (`wm-trace-2026-09-01.edn`),
behind three coupled flags.

**The pin cannot be the last record.** Three readers reach past it:
`recent-trace-records 12` supplies candidate enrichment
(`war_machine.clj:5959,6262-6263`); the cold-start habit prior folds *every*
`wm-trace-*.edn` file (`:79-80`, `:5984-5986`); and the U52 ladder's case-history
index does the same (`:2732-2734`). A pin that keeps the tail reconstructs a
different habit prior than the machine had.

## What a tick writes: five targets, and the measurement that says so

`data/wm-trace/wm-trace-<UTC-date>.edn` (append),
`data/wm-trace/.lane-futility-index.edn` (atomic replace, plus its lock and a
tmp), `data/wm-rationale/rationale-<date>-<run-id>.edn` (one per decision),
`holes/labs/wm-contract/tick-run-record-<date>-<run-id>.edn`, and
`data/wm-trace/.run-lock` (taken and released).

This is measured, not read off the code: `r6_zero_post_preflight.clj`
intercepts `spit` and `slurp` on a real tick and prints both sets. The
2026-09-01 run recorded three write targets and 1564 distinct paths read
(`runs/2026-09-01/README.md:23-30`); re5 recorded 1663 paths
(`runs/2026-09-04-re5/README.md:9-14`). The rationale store is the fourth
target and postdates that measurement, which is why the older list shows three.

Two of the five need naming rather than copying. The futility index is
**derived**: it is validated against a fingerprint of (name, length, mtime) per
trace file (`lane_futility.clj:82-86`) and rebuilt from the traces when stale,
so restoring the corpus is sufficient by construction — the one item in the
census where that is true. The run lock must be **excluded**: it holds pid,
pid-start-ms and a token belonging to the process that took it, so replaying a
pinned lock re-asserts a dead holder.

The receipt is the awkward one. It is the only write that lands in **tracked**
git state — `data/` is untracked, `holes/labs/wm-contract/` is not — and the
next tick's version stamp reads tracked dirtiness back into `:wm-version`
(`trace.clj:446-448`). Its path is built from `user.home` and the date and is
not parameterised at all (`run_tick_once.clj:36-39`).

## The four silent-degrade members, hunted

**cwd heuristics — not found, and the search is stated.** No `user.dir`, no
`(io/file ".")`, no `getCanonicalPath`, no bare relative path into
`slurp`/`io/file` anywhere in the 37 files. Every path is anchored either at
`System/getProperty "user.home"` (eleven sites, listed in item D1) or at a
literal absolute path (five sites: the p4ng control map, the futon3c curvature
JSON, the mathlib4 hole contract, the futon3 pattern library, the futon6
c-vector overlay). cwd reaches the tick in exactly two places, both outside
scoring: `clojure -M` classpath resolution — `wm_run.sh:16` cds to the futon2
root before every tick — and the `bb` subprocess at `war_machine.clj:6942`,
which inherits it. A pin need not capture cwd. The residual hazard is not cwd
but those five literals: they are unparameterised, so a sandboxed step cannot
redirect them.

**In-memory atoms — eight, and the reason they are invisible today is the
reason they will not stay invisible.** The CLI runs one JVM per tick
(`wm_run.sh:32-34`), so every `defonce` resets between ticks. Three of them
become inter-tick state the moment a stepper reuses a JVM to make steps fast:
five never-invalidated scan caches over cross-repo files (`:732,743-746`), the
whole-corpus habit seed held in a `delay` (`:79-80`), and
`case-history-index-memo` (`:2713`) — which is keyed by trace directory, so
redirecting the trace dir does **not** escape it, and an accepted step's own
decision never enters the history the next step reasons from. A fourth,
`mission-registry/missions-cache`, is not memory state but wall-clock state: its
validity is a 15-second TTL against `currentTimeMillis` (`mission_registry.clj:249`).

Two atoms resolve in the reassuring direction on this path, and the reason is
worth recording because it does not transfer. `c-vector/c-state` is only read
here — `ensure-belly-fresh!` has no caller in the closure — so it stays at its
initial `{:entries []}` and goal-outcome risk is 0. And
`intrinsic-values/state` feeds `:intrinsic-value` onto every proposed action
(`action_proposer.clj:44`), i.e. it reaches scoring, but
`rehydrate-from-store!` has exactly three callers and none is on this path
(`full_loop_cli.clj:727`, `scripts/wm_outer_loop.clj:279`,
`scripts/capability_zones_deposit.clj:67`). So the table is `{}` for the whole
tick and every class scores the Beta(1,1) prior 0.5 — constant, hence trivially
snapshot-able *here*, and live hidden state that no trace record carries on the
full-loop path.

**Evidence-store watermarks — there is no cursor.** The evidence query is
`:limit` plus `:since <date>` (`war_machine.clj:2817-2840,6968-6972`), a
sliding wall-clock window over a store that keeps growing. Searched the closure
for watermark/cursor/since-id: the only two watermarks are
`:morning-brief-consumed-event-ids` and the `:last-outcome-tick` inside the
γ-state, both carried in the trace, neither bounding this query. The closest
thing to a cursor already exists and nothing consumes it: `store-basis`
(`run_tick_once.clj:126-135`) records `{:count :max-at}` with their source URLs
into every receipt. That is the hook for a stepper that wants to detect that
the store moved under a pin.

**Wall-clock reads — eighteen, and one of them is in the score.**
`wm-time-pressure` (`war_machine.clj:6250-6252`) constructs `Instant/now` at
the call site and scales G-risk and homeostatic pressure by proximity to the
closest anticipated calendar event. Two steps from the same pin at different
wall times can rank differently for that reason alone. The remaining
seventeen name files, bin days, stamp records and bound queries; four are
parameterised and the parameter is simply not passed
(`latest-trace-record`'s `:end-date`, `anticipation-snapshot`'s `:now`,
move-class's `:now-ms`, mission-epistemic's `:epistemic-as-of`).

Two of them deserve to be seen before they surprise someone.

*Two timezones decide "today" in one tick.* The trace filename and
`latest-trace-record`'s two-day lookback use UTC (`run_tick_once.clj:24-25`,
`trace.clj:75-82,826-859`); the scan window, `since-str`, the operator-gate date
and `pattern-registry/since-date-str` use Europe/London (`war_machine.clj:693,
2781-2784,6962`, `pattern_registry.clj:41,119`). Under BST they disagree for
the hour 23:00–00:00 UTC. This row does not rule on that; it records that a
stepper pinning time has to pin it in both zones.

*The sharpest way reset lies is an age, not a content.* `:stale?` is
`age-min > 60.0` where age is `currentTimeMillis` minus the **mtime** of
`~/code/storage/futon0/mana-snapshot.json` (`:4159-4164,4207-4208`), and
`:stale?` is consumed at `:5936-5949` to suppress a `:stop-the-line` override.
A pin restored 61 minutes later runs the tick in a different **mode** with
every other input byte-identical. Copying the file byte-for-byte restores the
wrong thing.

## RNG: three sites, none of them in a score

`UUID/randomUUID` for the run id (`run_tick_once.clj:268`), `UUID/randomUUID`
for the run-lock token (`wm_run_lock.clj:156`), `random-uuid` for a tmp
filename that is renamed away in the same call (`lane_futility.clj:159`).
Searched all 37 files for `rand`, `rand-int`, `rand-nth`, `shuffle`,
`random-uuid`, `randomUUID`, `java.util.Random`, `Math/random`,
`ThreadLocalRandom`: nothing else. **No randomness enters any score, ranking or
tie-break.** That is why RE7 can verdict a 55-wide tie as a defect rather than
as noise, and it is what makes U55's determinism control meaningful.

The run id is not free of consequence, though: it names the receipt file, the
rationale file, and `:run/id` in the trace, and it is the join key between all
three. So "two steps byte-identical" can only ever mean identical modulo a
named exclusion list — the run id, `:timestamp`, `:startedAt`, and the
per-hop `:at` stamp that `route-tag` puts on every route hop
(`war_machine.clj:5836-5841`; nine hops per tick on the S-series runs).

## The world the tick reads, and the part of it a pin can hold

Thirteen named files across nine repos are pinnable: copy and hash them (item
R7). Four inputs are not: `~/code` is walked live for mission documents
(`mission_registry.clj:210-218`, ~10s, which is why the TTL cache exists), and
again under the enumeration flag; `git log --since` runs over sibling repos;
and a `bb` subprocess computes the futon4 VSATARCS projection at call time.
Pinning those means pinning every sibling repo's working tree including
uncommitted edits.

Services cannot be pinned at all: the futon3c evidence store and the futon1b
substrate. What *is* established about the service surface is a negative, and
it is measured rather than argued: the real tick issues **zero** HTTP POSTs and
reads the admin token **zero** times, on both recorded runs — and the
pre-flight's negative control shows the detectors do fire, since calling
`load-invariant-inventory` with the fallback enabled under the same
interception produces one POST to `:6768/eval` and one `.admintoken` read
(`r6_zero_post_preflight.clj:15-22`). The one service write on this path is the
mission-clock hyperedge, launched inside a `future` (`:2238-2245`) behind
`FUTON_WM_CLOCK_SELECTION`, default off — a step that ends is not a step whose
writes have landed.

## What this hands U55

Six items, recorded in the census under `:handoff-to-U55`:

1. **The sandbox seam exists but the entrypoint does not use it.**
   `:trace-dir` in judge-opts redirects the trace *and* the rationale
   (`war_machine.clj:2273-2282`, `trace/write-trace!`'s `:dir`), but
   `diagnostic-judge-opts` (`run_tick_once.clj:206-219`) never sets it, and the
   receipt path has no seam at all. That is the first edit U55 needs.
2. **Pin the whole trace directory**, not the last record.
3. **Exclude `.run-lock`; record `:selectorSeam`, the resolved `:wm-version`
   flag set, and the `store-basis` watermark** in the pin.
4. **The determinism control's exclusion list** is exactly: `:run/id`,
   `:timestamp`, `:startedAt`, the per-hop route `:at` stamps, and the
   filenames derived from the run id.
5. **A step run more than 60 minutes after the mana snapshot's mtime can change
   mode** with every other input identical. Pin the clock, or make the step
   record the age it saw and refuse the comparison.
6. **Reuse `r6_zero_post_preflight.clj`.** It already measures the read and
   write sets of a real tick by intercepting `slurp` and `spit`; a stepper
   should not re-derive them.

## Gates

```text
bash p4ng/empirics-futon/negative_controls.sh
bb   p4ng/empirics-futon/pointer_check.bb
bb   holes/labs/wm-contract/worklist_check.bb
```

Bare exits, recorded in the row's `:evidence`. `gen_aif_dag.bb` was **not** run
and nothing was regenerated into a publish (TN §9a). No `aif-equations.edn`
`:choices` entry and no `control-map-edges.edn` `:decisions` entry: this row
measures a boundary, it does not rule on one.
