# Row 26 r1 attempt discovery — 2026-09-13

## Scope and pins

This is a read-only account of run `wm-machinery-test-2026-09-13-claude-15-r1`
(`click/id` `wm-click-10e80d20-4dde-47dd-9239-861e81a1c25f`).  It does not
qualify the run or its loaded code.

| artifact | SHA-256 |
|---|---|
| `src/futon2/aif/full_loop_runner.clj` | `0bb61f3cceb26f52762af3a084503771ff3d853f1883edeaa229d83ec77cc71b` |
| `src/futon2/aif/full_loop_cohort.clj` | `f36bffb4e081df76d925ba8d2b2501d0e95190845e7c81934a052f857bed72cf` |
| `holes/labs/M-aif-full-loop-46/cohort.edn` | `e9031b3c66173bf10eb91cc38d43a7b750b1f3c24007c9340d506101e0c8f1ff` |
| `data/wm-full-loop-phases.edn.log` | `b013b37f865b272e292d1349b2f6d875e10920169bc9b9936a845f0cdbbde3e1` |
| r1 tick-run record | `11f4947497b8e32f34a8540cd589cfa535aa921cc069ff093bbaf894dd94392e` |

The inspected tree was futon2 `962713f998c37ab15bcffa34ea680cd2be3d5c77`.

## 1. The reported three attempts were old; r1 wrote none

Without an explicit `:execution-cohort`, the cohort module selects
`holes/labs/M-aif-full-loop-46/cohort.edn` and `data/wm-full-loop`
(`full_loop_cohort.clj:18-21`).  That preregistration names
`:wm-outer-loop-46-v1` and a target of three.  Its three persisted attempts
are:

| attempt | directory | closed outcome | closed at | closed-event SHA-256 |
|---|---|---|---|---|
| `attempt-059` | `data/wm-full-loop/wm-outer-loop-46-v1/attempt-059/` | `:grounded-change` | `2026-07-27T09:08:56.265866131Z` | `68a783a933b833450aab4e9e705876d2dd1fb0f7f98aa6ef7289251db8a21e6f` |
| `attempt-060` | `data/wm-full-loop/wm-outer-loop-46-v1/attempt-060/` | `:grounded-change` | `2026-07-27T09:22:56.016494436Z` | `bd84ccf0968cb73ba90f90830b512f352903d3f5d9174ef8ec8e8cba8f067d89` |
| `attempt-061` | `data/wm-full-loop/wm-outer-loop-46-v1/attempt-061/` | `:grounded-change` | `2026-07-27T10:59:53.609069187Z` | `b42158be30e415de97f3626b62a2fdc77d2ba030242a3b51130fa2badcf2bed5` |

Each directory contains the complete immutable sequence
`001-time-step.edn` through `007-closed.edn`.  Their filesystem mtimes are
also 2026-07-27.  A filesystem census under `data/wm-full-loop` for files
newer than `2026-09-13 16:40:00 UTC` returned no files.

The count in r1's terminal record is therefore not “three attempts made by
this click.”  `start-attempt!` counts existing cohort attempt directories
(`full_loop_cohort.clj:373-377`) and, because three is already the target,
throws `{:cohort/error :stopping-rule-reached :target 3 :attempted 3}` at
lines 378-381.  This happens before an attempt id is allocated, before an
attempt directory is created, and before `001-time-step.edn` is written
(lines 382-389).  Consequently neither `append-checkpoint!` nor
`close-attempt!` had an r1 attempt on which to operate.

The outer runner deliberately converts this typed exception into the
scheduler result `:cohort-complete` (`full_loop_runner.clj:3892-3901,
3920-3934`).  Its generated `cohort-complete-...` internal id is a result id,
not a persisted attempt id.

## 2. Why no construction trace existed

The r1 phase lines are exactly:

1. opportunity start, `16:44:21.761196333Z` (log line 7130);
2. agent-readiness start/end, `16:47:19.620307804Z` to
   `16:47:20.256488207Z` (7131-7132);
3. code-state start/end, `16:47:20.849432479Z` to
   `16:47:21.814524626Z` (7133-7134).

There is no attempt-bearing phase, substrate preflight, selection, or
construction event.  This follows the code order: readiness and code-state
run at `full_loop_runner.clj:2723-2736`; cohort admission is attempted at
2764-2768; only after successful admission is `attempt-id` bound and put into
the phase context at 2769-2778.  The stopping-rule exception occurred at that
admission boundary.

A trace is written only after selection and successful construction wiring
(`full_loop_runner.clj:3257-3288`), then stored in the construction checkpoint
at line 3318.  Close later reads exactly
`[:construction :judgment :trace-path]` (2951-2952), and the result carries it
at 2982-2988.  Since r1 never acquired an attempt, it never reached any of
these operations.  Thus `:traceWritten false` is accurate; it is not a lost
trace or a trace-reader failure.  The run record's sole
`COHORT -> STOPPING_RULE` hop is the complete route for this click.

The roughly 178-second opportunity-to-readiness gap is consistent with the
synchronous start-event tripwire scan: `emit-phase!` timestamps the record,
then calls `tripwire/observe!`, and only afterward prints/appends the phase
line (`full_loop_runner.clj:222-235`).  The observed T10
`:loaded-file-code-mismatch` can therefore explain work during that interval.
It cannot explain the absence of an attempt or trace: the independently
persisted default cohort was already exhausted, and the observed typed
`:cohort-complete` is precisely the current stopping-rule path.  Stale loaded
code could have affected phases after cohort admission, had admission
succeeded, so r1 supplies no loaded-source correspondence evidence.

## 3. Smallest r2 change

Use a fresh, reviewed, activated cohort with remaining capacity and pass its
exact server-owned binding as runner option `:execution-cohort`:

```clojure
{:preregistration "/absolute/path/to/cohort.edn"
 :data-root "/absolute/path/to/data-root"
 :cohort-id :the-reviewed-cohort-id
 :sha256 "<sha256-of-the-preregistration-bytes>"}
```

This is already the runner's intended seam.  `execution-context` strictly
pins the preregistration and authority (`full_loop_cohort.clj:145-159,
195-216`); the runner resolves it at `full_loop_runner.clj:2737-2743` and
passes its snapshot and data root to `start-attempt!`, checkpoint append, and
close (`2764-2768`, `2780-2798`, `2992-2995`).  No cohort persistence rewrite
is needed.

If the live click endpoint cannot currently supply that server-owned binding,
the smallest source change is bounded option plumbing from reviewed server
configuration to this existing runner option.  It must not accept an
arbitrary caller data root or caller-asserted hash.  Reloading the named
runner/trace/selection namespaces addresses the T10 code mismatch but does not
create cohort capacity.

A fresh cohort only permits an attempt to start.  To make r2 artifacts
candidates for rows 13/14/15/23, the reviewed r2 configuration must also keep
the required horizon/details/F-pi settings and the run must actually reach
the construction trace write.  An earlier typed refusal remains valid attempt
evidence but cannot be relabelled as a construction trace or as closure of
those rows.
