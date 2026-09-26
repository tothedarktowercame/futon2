# H-T-CALLER-D — what chooses a flight's target, and the smallest path that makes the loop choose it

claude-2, 2026-09-26. Discovery, read-only: no code, no map edit.
Read: futon2 at `bc00cdaf` (tracked tree clean), futon3c at `1d10314c`
(map `d74fde21`), the eight flights' records under
`futon3c/holes/labs/M-wm-wiring/spike/`. Nothing was run; every statement about
behaviour is from source or from a record on disk.

## Answer to (1): what chooses a flight's target today

**An operator, by hand, per flight.** All eight flights on record took
`M-autoclock-in`, placed by hand:

| flight | target | `:target-source` | `:chosen-because` |
|---|---|---|---|
| `flight-74325007` (plan, driver error report) | `M-autoclock-in` | `:hand-placed` | `{:kind :requested :by "flight-driver"}` |
| `flight-d00574c8` | `M-autoclock-in` | `:hand-placed` | same |
| `flight-e70b4baf` | `M-autoclock-in` | `:hand-placed` | same |
| `flight-ffcd772b` | `M-autoclock-in` | `:hand-placed` | same |
| `flight-278b6988` | `M-autoclock-in` | `:hand-placed` | same |
| `flight-6cda5ee8` | `M-autoclock-in` | `:hand-placed` | same |
| `flight-7f89646a` | `M-autoclock-in` | `:hand-placed` | same |
| `flight-ada87008` | `M-autoclock-in` | `:hand-placed` | `{:kind :requested :by "flight-driver"}` |

(Read from each flight's `.edn` and, for `74325007`, its plan file and driver
error report; `ada87008`'s record and `plan.txt` both say `:hand-placed`.)
The `resolve-target` comment says why: the target arrives from the command line
(`flight_driver/parse-args`: a bare word or `--target`), and the record says so
in its own words, `:target-source :hand-placed`.

The machinery to say otherwise exists and has no writer. `flight_driver/resolve-target`
(`flight_driver.clj:65-87`) already takes `:chosen-target` and `:draw-seed`,
prefers a chosen target, records `{:hand-target-overridden t}` when a hand
target is also given, and carries a `:field-entry` "as given: eligibility is the
field's". `loop_closure_test.clj` pins all of it. Its own docstring says
`:chosen-target` is "written by nothing yet".

### The callers, by name

| function | production callers | target it passes and where it came from |
|---|---|---|
| `flight_driver/run-flight!` (`:198`) | `flight_driver/main*` (`:296`), only with `--run` | `(:target (resolve-target opts))`: a CLI word, or `:chosen-target` if given. No other caller in futon2 or futon3c src, scripts or dev. |
| `flight_driver/read-only!` | `main*`, with `--read` | same |
| `flight/start` (`flight.clj:164`) | `flight_driver/flight-for` (`:98`); `target_field/assess` (`:236`) | driver: `resolve-target` output with `:chosen-because {:kind :requested :by "flight-driver"}`. target-field: `(:target t)` of each *considered* target, `:chosen-because {:kind :target-field}`, id `target-field-<t>`, a synthetic flight built only to call `flight/click-wants` (it is never flown). |
| `flight/choose-target` (`flight.clj:151`) | none in production; `flight_test.clj` only | (would take `:requested` or the oldest open stop-line's repair target) |
| `futon2.aif.full-loop-runner` | requires `flight` for `throwable-summary` only | not a caller of start/run |

**`wm_scheduled_run/-main`** (`scripts/wm_scheduled_run.clj:66`) is one tick per
JVM: `cv/maybe-refresh!` (the belly), `wm/generate-war-machine`, optionally
`enact/close-loop!` (guarded by `live-wire?`), `trace/write-trace!`, an
evidence emit, one summary line. It never starts a flight. The flight's own
clicks are `POST /api/alpha/wm/click` against the serving JVM, through the
driver's `http-click-fn`. The two entry points do not share a process, an
input or a record.

**The target field** (`target_field.clj`) is complete as a module and has one
production entry point, its own `-main`, which prints `{:decision
{:target-field …}}` and exits; no other production namespace requires it.
`assess` calls `flight/start` itself for the synthetic flight above. The
enumerators (`mission-registry`'s mission, ticket and excursion proposers) and
`with-eligibility` / `requisition` / `with-pair-overlap` are all reached through
`target-field`. It writes nothing: "No target is chosen and nothing is scored."

## The recorded direction (so the path below is not invented)

- `M-wm-wiring.md` ARGUE (the outer cascade, row 1, "last"): `outer_cascade.clj`
  `select` — the action set is every eligible field entry with its own
  `:next-step`, plus `defer` and `learn`; the choice is the mixture law (every
  target keeps its E mass, targets with a ΔG are re-weighted among themselves,
  reducing to a seeded draw from E when no target has a G), "with the draw's
  generator, seed and value on the record so the choice can be recomputed". It
  writes `:chosen-target` and `:draw-seed`; the flight entry reads
  `:chosen-target` instead of `--target`; "the scheduled entry starts a flight
  instead of a tick".
- Joe, 2026-09-25 ~23:45Z: the outer cascade should be based on Cascade Live.
  OUTER-CASCADE-D §5 measured the field: 59 missions join Cascade Live's facts,
  282 excursions and tickets cannot by construction.
- The map: `:r1-outer-cascade` is `:not-built`, intended site
  `futon2/src/futon2/aif/outer_cascade.clj` `select`, writing
  `[:chosen-target :draw-seed]`; `:loop-entry` is `-main` writing `[:trigger]`;
  `:flight-entry` is `resolve-target`, reading `[:chosen-target :draw-seed]`.

## Answer to (2): the smallest production path

Three pieces, in this order, none of which changes what exists when it is not
asked for.

**(a) `outer-cascade/select`, pure.** Input: the field
(`target-field/target-field`'s map), an E over targets, a seed. Output:
`{:chosen-target t :draw-seed n :selection {…}}` where `:selection` is the
record of the choice:

```clojure
{:selection
 {:rule :seeded-draw-from-E          ; the mixture law's degenerate case, named
  :support  [t …]                    ; the eligible targets (T_f: feasibility as support)
  :posterior {t p …}                 ; selectionPosterior at target grain: E renormalised over T_f
  :E        {:basis :uniform-no-data ; or the habit-derived masses, with their basis
             :alpha 1.0}
  :g        {:absent :no-target-grain-g}   ; per target, typed; never a number standing in
  :draw     {:generator "java.util.Random" :seed n :value u :index i}
  :field-sha {…}                     ; the heads the field was read at (target_field -main already computes them)
  :excluded [{:target t :reason kw} …]}}
```

`:posterior` is Clause T's `selectionPosterior` at target grain: σ(ln E − G/β)
with G absent for every target, so it is E over the support. That is what the
mixture law reduces to, and it is exactly the case the ARGUE text names. The
draw is recomputable from `:seed` and `:support` in order (a
`java.util.Random` over the support sorted by target id; `arguing_worlds`
already uses `(java.util.Random. (long seed))` for a seeded shuffle, so the
idiom is in the tree).

**(b) The loop entry calls it, in a mode that plans and does not fly.**
`wm_scheduled_run/-main` keeps its default behaviour (the tick). A flag or an
env var (`FUTON_WM_FLIGHT=plan`, following the `FUTON_WM_TRIGGER` precedent in
the same function) selects the flight path, which:

1. loads the field the way `target_field/-main` does (`mr/load-missions-from-files`,
   `mr/load-tickets`, `mr/load-excursions`, `cs/load-declared`): that loading is
   inline in `-main` today, so it becomes a function the entry can call;
2. calls `target-field` (eligibility comes with it: `assess` →
   `with-eligibility`), then `select`;
3. hands the choice to the driver **as a map argument**, not as CLI strings:
   `driver/plan {:chosen-target … :draw-seed … :field-entry the chosen entry
   :selection … :repo (:repo entry) :path (:path entry) :seat … :sources …}`.
   `--repo` and `--path` are `check-args!` requirements and are on every field
   entry already (`considered` carries `:repo` and `:path`); the seat comes
   from configuration, and the driver's own refusal covers its absence;
4. prints the plan, which now shows `:placement {:target t :target-source
   :chosen :draw-seed n}`. **Nothing is sent and nothing is written**, the same
   contract as the driver without `--run`.

`FUTON_WM_FLIGHT=run` (flying) is a separate decision and is not in the first
packet: the ruling is no click and no flight until the spike is belled, and one
flight is long against an hourly cron. The path exists and is verifiable
without either.

**(c) The choice is recorded on the plan and therefore on every flight record.**
`flight_driver/plan` already carries `:placement (select-keys (resolve-target
opts) [:target :target-source :draw-seed :hand-target-overridden])`, and
`run-flight!`'s writer stores `{:plan planned :flight flown}`. Add `:selection`
to what `resolve-target` carries beside the target, as it carries `:field-entry`
("as given"), and to that `select-keys`. A hand-placed flight gets
`{:absent :hand-placed}` there, never nothing and never a value standing in.

### What the map gains

| | now | after |
|---|---|---|
| `:r1-outer-cascade` | `:not-built`, reads `[:trigger :next-step :eligible :pair-overlap :enactment-records :publication-observed :clock-lineage]`, writes `[:chosen-target :draw-seed]` | built, site `outer_cascade.clj` `select`; reads only what the first `select` reads, `[:next-step :eligible :pair-overlap]` from the field entries (writers: the field's `assess` and `with-pair-overlap`, both boxed or boxable); writes `[:chosen-target :draw-seed :selection]`. `:publication-observed`, `:clock-lineage` and `:enactment-records` stay off the box until `select` reads them; a read the code does not make is not declared. |
| `:loop-entry` | writes `[:trigger]` | additionally reads nothing keyed; passes the field into `select` (positional, `:passes {:from {:returns-of "target-field/target-field"} :to {:call "select" :arg 1}}`) and the chosen map into `driver/plan` (`:passes {:from {:literal-arg-key :chosen-target} …}`); both are the two `:passes` forms WM-PROVER-PASSES-I checks at both ends |
| `:flight-entry` | reads `[:chosen-target :draw-seed]` | also reads `:selection` (keyed: `resolve-target`'s destructure) and, through `plan`, forwards the whole opts map (a positional pass, arg 1) |
| new field | | `:selection`, typed `{:absent :hand-placed}` when a hand target flew |
| new test box | | `:outer-cascade-test` (`outer_cascade_test.clj`); `:loop-test` (`loop_closure_test.clj`) gains the selection |
| expected findings that go | | `:r1-outer-cascade` `:declaration-without-occurrence` (not built); the `:loop-entry` note in the org-layer answer ("today it runs a tick directly and never starts a flight") stays true in default mode and is qualified, not deleted |

No new required field and no new refusal: `:selection` is typed absent on the
hand path; the flag is opt-in; `select` refuses nothing (an empty support is a
recorded absence: `{:absent :no-eligible-target}` and no chosen target, which
`resolve-target` already turns into its existing `{:missing :target}` refusal).

### The wire test at the first layer

The first wire is `select` → `resolve-target` → the plan's placement. One
futon2 test namespace, `outer_cascade_test` beside `loop_closure_test`, on a
fixture field of three entries (two eligible, one `:requisition/…`-ineligible):

1. `select` chooses only from the eligible entries and names the ineligible one
   in `:excluded` with its `:ineligible-reason`;
2. the same seed and the same field give the same target, and the recorded
   `:draw` recomputes to it (recomputability is the property the ARGUE text
   asks for);
3. `:posterior` sums to 1 over the support and is uniform when E is
   `:uniform-no-data`; `:g` is `{:absent :no-target-grain-g}` on every entry;
4. `(driver/resolve-target {:chosen-target t :draw-seed n :selection s :field-entry e})`
   returns `:target-source :chosen` with the seed and the selection carried, and
   `driver/plan` puts them on `:placement`;
5. a hand `--target` alongside a chosen one is recorded as
   `:hand-target-overridden`, as today;
6. an empty support gives no chosen target and the driver's existing
   `{:missing :target}`.

Read "first layer" as this join. If claude-8 means a different layer of the
map's lane numbering, the test is the same and moves with the box.

## Answer to (3): what H-G-target part 2 is for

Part 1 is done: the target-grain ΔG is defined in Lean (`759b8ca884`) and its
comparability precondition is computed (`with-pair-overlap`). Part 2, in the
Lean module's own words, is "the outcome universe C ranges over when targets are
compared, and a prior over targets before any candidate exists", which "has no
definition in any document".

- **Needed for** a target-grain G that is a number: it needs a constructed
  candidate per target (q₁, the candidate law) and a C over a common universe.
  Joe's own anchor is that constructed candidates are built on the fly, not for
  every mission before choosing. So the *initial selection* cannot use G at all,
  and it needs exactly the second half of part 2: a prior over targets before any
  candidate exists.
- **What runs without it.** The support restriction (eligibility) runs, the E
  mass runs (uniform with no data: `enactment-habit`'s counts are per policy key
  `[… mission shown semilattice]` and nothing sums them to target grain, and no
  enactment has passed W_c), and the seeded draw runs. That is the mixture law's
  degenerate case, and it is honest: `:g {:absent :no-target-grain-g}` on every
  target, the rule named `:seeded-draw-from-E`, the prior named
  `:uniform-no-data`. `with-pair-overlap` already says the pairs are not
  comparable at HEAD (no target has a constructed candidate).
- **The current ranker does not apply here.** `rank-cascade-actions` scores
  candidates within a target (G per candidate over one universe); at target
  grain it has nothing to rank, for the reason above. It stays where it is,
  inside the flight, after the target is chosen.

So part 2 is not a prerequisite of a production caller. It is a prerequisite of
the choice being informed. A caller that records `:g` as typed absent and the
prior as uniform-no-data lets the loop close now and shows, on every record,
what part 2 would change.

## (4) H-T-CALLER-I: the one behaviour

> **The scheduled entry can choose a flight's target from the field, and plan
> the flight, recording the choice.** With `FUTON_WM_FLIGHT=plan`,
> `wm_scheduled_run/-main` reads the target field, calls `outer-cascade/select`
> (seeded draw over the eligible support, E `:uniform-no-data`, `:g` typed
> absent), and passes `{:chosen-target :draw-seed :selection :field-entry
> :repo :path …}` to `driver/plan`, which prints a plan whose `:placement` says
> `:target-source :chosen` with the seed and the selection. Nothing is sent or
> written. The default (no variable) is the tick, unchanged.

- **Files.** New `src/futon2/aif/outer_cascade.clj` (`select`, pure); a `load-field`
  function extracted from `target_field/-main`'s inline loading (behaviour
  unchanged); `flight_driver/resolve-target` and `plan` carry `:selection`;
  `scripts/wm_scheduled_run.clj` gains the branch. Tests:
  `outer_cascade_test.clj` (the six cases above), `loop_closure_test.clj`
  extended, and `wm_scheduled_run`'s default path pinned unchanged.
- **Map (futon3c), same commit as the code, per the ruling.** `:r1-outer-cascade`
  built with the fields above, its `:draw-seed` read by `:r1-test` as now; the
  two `:passes` on `:loop-entry`; `:selection` on `:flight-entry`; the expected
  finding for the unbuilt box deleted; the test box's namespace added; a warrant
  per new test namespace; the fidelity tripwire for the placement.
  Sequencing: this lands after WM-PROVER-PASSES-I (553d7edf), which it uses.
- **Not in it.** `FUTON_WM_FLIGHT=run`; any E derived from enactments; any G;
  the Cascade Live data the field lacks for excursions and tickets (their
  entries are in the support and carry E mass like any other; `select` reads no
  Cascade Live fact); the clock-in edge.
- **The seed.** A generator the tick controls: `(System/currentTimeMillis)` is
  the tick's existing convention (`enact/close-loop!` is given it), and the seed
  goes on the record, which is what makes the draw recomputable. A test passes
  its own.
- **What could go wrong.** The field read takes a while and touches every repo
  (`git show` per target); the flag keeps that off the hourly cron path. A
  chosen target that is `:not-lifecycle-shaped` is not in the support, so it
  cannot be chosen, but `resolve-target` would carry one if handed it, by
  design ("eligibility is the field's, not filtered here").

## Not checked

- Whether `bc00cdaf`'s `target_field/-main` loads exactly what the tick's
  enumerators load today; I read the code, not a run.
- `outer_cascade.clj`'s ARGUE description names E "from the fold"; I did not read
  the fold's per-mission masses, and this packet's E is uniform for that reason.
- Whether the cron cadence makes `plan` mode worth running from the scheduler at
  all, versus a manual command; that is Joe's call, and the packet does not
  depend on it (`select` and the driver join are the same either way).
