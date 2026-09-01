# C465 — shadow-run discovery

Date: 2026-09-01  
Scope: discovery only; no War Machine invocation was made.

## Result

**There is no presently safe command for the requested validation run.**  The
closest command is the diagnostic `futon2.run-tick-once` path, which deliberately
does not call `close-loop!` (`scripts/futon2/run_tick_once.clj:196-214`), but the
judgement scan unconditionally POSTs `/api/alpha/portfolio/step`
(`scripts/futon2/report/war_machine.clj:4844-4854`).  That endpoint is documented
as “run one AIF step,” not as a read (`scripts/futon2/report/war_machine.clj:2202-2209`).
There is no option on `run-tick-once` or `generate-war-machine` that disables this
POST.  By R1's acceptance rule, that makes the command **NOT SAFE TO RUN**.  This
discovery did not run it.

## 1. How the two existing records were produced

Both records have the exact shape emitted by `run-tick-once`: the program chooses
daily trace and receipt names (`scripts/futon2/run_tick_once.clj:26-32`), calls
`generate-war-machine` with tracing on and advisory lanes off
(`scripts/futon2/run_tick_once.clj:196-214`), and overwrites the daily receipt
(`scripts/futon2/run_tick_once.clj:225-227`).  Both receipts name the diagnostic
stub selector and say the trace was written
(`holes/labs/wm-contract/tick-run-record-2026-08-30.edn:1`;
`holes/labs/wm-contract/tick-run-record-2026-08-31.edn:1`).

The recorded invocation is:

```sh
clojure -M -m futon2.run-tick-once 14
```

It appears verbatim in the bounded-run resource receipt
(`holes/labs/wm-contract/C167-v20-resource-receipt.json:2`).  That receipt bounds
the 2026-08-31 process from 20:35:25.789 to 20:36:43.055, about 77.3 seconds
including its outer harness (`holes/labs/wm-contract/C167-v20-resource-receipt.json:4-16`).
Inside the tick receipt, start-to-final-route timestamps are about 23.7 seconds
(`holes/labs/wm-contract/tick-run-record-2026-08-31.edn:1`); the corresponding
2026-08-30 interval is about 21.1 seconds
(`holes/labs/wm-contract/tick-run-record-2026-08-30.edn:1`).  Each invocation runs
exactly one tick: `-main` calls `run-tick-once` once
(`scripts/futon2/run_tick_once.clj:239-252`), and each receipt is one EDN map.
No periodic cadence or multi-tick flag is recorded for these two runs.

At the observed 21–24 seconds of tick work, 20 sequential ticks would take about
7–8 minutes.  The only recorded harness timing is slower (77 seconds for one
bounded cold run), so a conservative outer bound based on that witness is about
26 minutes for 20 separately launched JVMs.  These are extrapolations from the
timestamps above, not measurements of a 20-tick run.

## 2. Existing things called shadow or dry

### Offline dark-mode replay

`scripts/dark_mode_shadow.bb` is an offline replay over persisted
`:ranked-actions`, explicitly described as read-only with respect to
`data/wm-trace` (`scripts/dark_mode_shadow.bb:2-7`).  It never enters
`generate-war-machine` or `close-loop!`; this is a separate code path, not a flag
on a tick.  It does write four reports under
`holes/labs/M-aif-faithfulness/` (`scripts/dark_mode_shadow.bb:144-166`).  Thus it
suppresses live effects by replaying old data, but it cannot prove traversal by a
new tick.

### Diagnostic one-tick path

`run-tick-once` records `:live-wire? false` and does not call `close-loop!`
(`scripts/futon2/run_tick_once.clj:205-214`).  It also passes
`:include-advisory-lanes? false`, which prevents construction of the cascade lane
(`scripts/futon2/run_tick_once.clj:209-214`;
`scripts/futon2/report/war_machine.clj:4820-4824`).  These are code-path choices,
not operator-selectable dry-run flags.  They prevent fold enactment and the
cascade subprocess, but they do **not** prevent the portfolio-step POST.

### Scheduled runner

The scheduled runner has one actual operator kill switch:
`FUTON_WM_LIVE_WIRE=0` or `false` makes it skip `close-loop!`
(`scripts/wm_scheduled_run.clj:30-37,98-113`).  The switch does not make the scan
read-only: `generate-war-machine` still performs the portfolio-step POST before
that branch.  Evidence emission has its own default-off flag,
`FUTON2_WM_EMIT_EVIDENCE` (`src/futon2/aif/evidence_emit.clj:4-5,15-21`), but that
only controls the later evidence POST (`src/futon2/aif/evidence_emit.clj:185-198`).

### Full-loop runner

The full-loop runner has dependency injection for tests, for example
`:dispatch-fn` at author and reviewer dispatch
(`src/futon2/aif/full_loop_runner.clj:2751-2761,2913-2929`).  Its production
configuration has no `dry`, `shadow`, or `no-dispatch` option
(`src/futon2/aif/full_loop_runner.clj:136-167`).  Production writes a phase log
(`src/futon2/aif/full_loop_runner.clj:169-185`), sends Agency whistles
(`src/futon2/aif/full_loop_runner.clj:410-424`) and bells
(`src/futon2/aif/full_loop_runner.clj:755-767`), and persists a run receipt
(`src/futon2/aif/full_loop_runner.clj:230-251`).  Supplying injected no-op
functions manually is a testing convention/API technique, not a supported shadow
mechanism, and it does not account for every effect.  `wm-full-loop once` is not
a safe substitute for this validation run.

## 3. Per-actuator accounting

The table distinguishes the requested diagnostic command from the scheduled and
full-loop entrypoints.  A read-only GET is listed because it crosses the process
boundary, but is not treated as an actuator that mutates the remote service.

| Effect | Where it fires | What disables it in the closest diagnostic path | Verdict |
|---|---|---|---|
| Portfolio AIF state step (HTTP POST) | `scripts/futon2/report/war_machine.clj:4844-4854`; semantics at `:2202-2209` | **Nothing found.** It is unconditional inside `judge`. | **NOT SAFE TO RUN** |
| Fold-engine process (`bb … fold_engine.clj apply`) | `src/futon2/aif/enact.clj:130-154`, reached by `close-loop!` at `:287-316` | `run-tick-once` never calls `close-loop!` (`scripts/futon2/run_tick_once.clj:196-214`). Scheduled path: `FUTON_WM_LIVE_WIRE=0` skips the call (`scripts/wm_scheduled_run.clj:30-37,105-109`). | Disabled for diagnostic command |
| Cascade-builder subprocess | process launch at `scripts/futon2/report/cascade_lane.clj:41-59`; judgement entry at `scripts/futon2/report/war_machine.clj:4820-4824` | `:include-advisory-lanes? false` (`scripts/futon2/run_tick_once.clj:209-214`). | Disabled |
| Trace append | `src/futon2/aif/trace.clj:553-567`; physical append and sidecar update at `src/futon2/aif/lane_futility.clj:198-211` | Not disabled; `:trace? true` is intentional (`scripts/futon2/run_tick_once.clj:209-214`). | Expected local write |
| Trace lock/index sidecars | `src/futon2/aif/lane_futility.clj:156-183,198-211` | Not disabled; part of trace coherence. | Expected local writes |
| Daily diagnostic receipt overwrite | `scripts/futon2/run_tick_once.clj:186-189,225-227` | Not disabled. | Expected local write; only last run of a UTC day remains |
| Evidence-store and mission HTTP reads | explicit diagnostic GETs at `scripts/futon2/run_tick_once.clj:102-130`; scan reads begin at `scripts/futon2/report/war_machine.clj:5195-5207` | Not disabled; they are the observation source. | Remote reads, no write claimed |
| Evidence summary POST | `src/futon2/aif/evidence_emit.clj:185-198`, called by scheduled runner at `scripts/wm_scheduled_run.clj:113-116` | Diagnostic runner never calls `evidence-emit`; scheduled runner requires default-off `FUTON2_WM_EMIT_EVIDENCE` (`src/futon2/aif/evidence_emit.clj:15-21`). | Disabled in diagnostic command |
| Agency strategic-selection POST | `src/futon2/aif/full_loop_runner.clj:597-616` | Diagnostic runner instead resolves a local selector and falls back to a non-executing stub (`scripts/futon2/run_tick_once.clj:56-100`). | Disabled in current diagnostic path; not a general full-loop dry mode |
| Agency readiness whistle | `src/futon2/aif/full_loop_runner.clj:410-424` | Full-loop has no production shadow flag (`src/futon2/aif/full_loop_runner.clj:136-167`); diagnostic runner does not enter full loop. | Full loop: **NOT SAFE**; diagnostic: absent |
| Agency author/reviewer bells (agents may edit and commit) | bell POST at `src/futon2/aif/full_loop_runner.clj:755-767`; author/reviewer call sites at `:2751-2761,2913-2929` | Injectable only in tests; no production dry flag (`src/futon2/aif/full_loop_runner.clj:136-167`). | Full loop: **NOT SAFE**; diagnostic: absent |
| Agency status mutation | `src/futon2/aif/full_loop_runner.clj:99-112` | Dynamic binding exists for tests only (`src/futon2/aif/full_loop_runner.clj:70-73`); no CLI flag. | Full loop: **NOT SAFE**; diagnostic: absent |
| Full-loop durable phase/run/canary files | phase log `src/futon2/aif/full_loop_runner.clj:169-185`; run receipt `:230-251`; optional canary `:2420-2423` | No production shadow flag (`:136-167`). | Full loop: **NOT SAFE**; diagnostic: absent |
| Full-loop cancellation POST | `src/futon2/aif/full_loop_runner.clj:769-775` | No shadow flag; only reached during full-loop job management. | Full loop: **NOT SAFE**; diagnostic: absent |

The enactment itself does not edit a mission, repository, or substrate.  Its
declared production is an in-memory boxes/policy-holes construction
(`src/futon2/aif/enact.clj:12-16`), and its concrete executor only shells the
fold engine and parses stdout (`src/futon2/aif/enact.clj:130-160`).  The much
larger repository-editing risk belongs to full-loop Agency dispatch: the runner
sends author/reviewer work requests, rather than editing a repository itself
(`src/futon2/aif/full_loop_runner.clj:2751-2761,2913-2929`).

## Requested command, retained as an unsafe proposal

If the portfolio-step actuator gains a genuine read-only suppression, the
diagnostic form for **N total ticks** would be:

```sh
for i in $(seq 1 "$N"); do
  FUTON_WM_TRACE_POLICY_DETAILS=1 FUTON_WM_FPI_DARK=1 \
    clojure -M -m futon2.run-tick-once 14 || exit $?
done
```

Both variables are read once when their namespaces load
(`src/futon2/aif/trace.clj:68-74`;
`scripts/futon2/report/war_machine.clj:75-80`).  F-pi reads prediction details
from the previous trace record (`scripts/futon2/report/war_machine.clj:92-110`),
so tick 1 is a seed unless its predecessor was already written with
`FUTON_WM_TRACE_POLICY_DETAILS=1`; results are available from tick 2.  To obtain
N measured F-pi ticks from an unknown predecessor, run `seq 0 "$N"` instead and
treat iteration 0 as the seed (N+1 physical ticks).

The command appends one record per tick to
`data/wm-trace/wm-trace-YYYY-MM-DD.edn` and updates that directory's lock/index
sidecars (`src/futon2/aif/trace.clj:59-83,553-567`;
`src/futon2/aif/lane_futility.clj:156-183,198-211`).  It overwrites
`holes/labs/wm-contract/tick-run-record-YYYY-MM-DD.edn` each time
(`scripts/futon2/run_tick_once.clj:26-32,186-189`).  The repository's cost note
records 291,060 bytes for the naive 110-candidate detail payload
(`holes/labs/wm-contract/C63-preference-stack-witness.md:55`), while the worklist
describes the containing trace as 13.2 MB
(`holes/labs/wm-contract/worklist.edn:259-262`).  The packet's newer estimate of
about 93 KB per 110-candidate tick is not found in the checked-in sources I
searched (`holes/labs/wm-contract`, `src`, and `scripts`); it should therefore be
treated as packet-supplied measurement, not independently established here.

## Safety statement and missing mechanism

Do not run the command above yet.  The smallest missing mechanism is an injected
or option-controlled portfolio reader that uses only
`GET /api/alpha/portfolio/state`, or an explicit `:portfolio-step? false` option
that prevents the POST at `scripts/futon2/report/war_machine.clj:4844-4854`.
Merely setting `FUTON_WM_LIVE_WIRE=0` is insufficient because that switch is
evaluated after `generate-war-machine` returns
(`scripts/wm_scheduled_run.clj:98-109`).  Likewise, calling the run “shadow” is
not a mechanism.  Once the portfolio POST is suppressed, the diagnostic path
has one intentionally retained mutation class: its trace/index/receipt files.
