# War Machine S1 shadow run — 2026-09-01

This directory freezes the durable outputs of twenty isolated War Machine
ticks.  The run began and ended at Git SHA
`50062008d4d09d1a0a7fc784760e512ee21ca02f`; `git rev-parse HEAD` returned that
value both before the pre-flight and after the twentieth tick.

## Commands

The pre-flight command was run first, verbatim:

```sh
cd /home/joe/code/futon2
git rev-parse HEAD
clojure -M:test holes/labs/wm-contract/r6_zero_post_preflight.clj
```

Its output was:

```text
50062008d4d09d1a0a7fc784760e512ee21ca02f
trace/write-trace! failed: /home/joe/code/futon2/data/wm-trace/.lane-futility-index.edn.287762b2-a3e1-4860-9914-58aba33c6fe5.tmp -> /home/joe/code/futon2/data/wm-trace/.lane-futility-index.edn
r6-preflight: real diagnostic tick in 36008 ms
r6-preflight: POSTs attempted: 0
r6-preflight: writes the tick would make:
r6-preflight:   /home/joe/code/futon2/data/wm-trace/wm-trace-2026-09-01.edn
r6-preflight:   /home/joe/code/futon2/data/wm-trace/.lane-futility-index.edn.287762b2-a3e1-4860-9914-58aba33c6fe5.tmp
r6-preflight:   /home/joe/code/futon2/holes/labs/wm-contract/tick-run-record-2026-09-01.edn
r6-preflight: paths read: 1564; .admintoken reads: 0
r6-preflight: PASS — no POST on the real path, and the admin token was never read
```

The pre-flight exited 0.  Only after that PASS, the ticks were run with the
requested command, verbatim:

```sh
for i in $(seq 1 20); do
  FUTON_WM_TRACE_POLICY_DETAILS=1 FUTON_WM_FPI_DARK=1 \
    clojure -M -m futon2.run-tick-once 14 || exit $?
  done
```

The timing wrapper recorded epoch seconds immediately around each invocation;
it did not alter the invocation or its environment.

## Tick results

All twenty invocations exited 0 and all twenty appended one trace form.  All
twenty printed a nine-hop route in their run receipt.  The receipt path is
overwritten rather than appended, however, so the frozen receipt file contains
only tick 20's route; the trace forms themselves contain no `:wm/route`.  Thus
the run produced twenty routes observably, but only the last route survives in
the durable files.  This is a record-retention gap for Figure 5A conformance.

| tick | wall seconds | trace record | route printed |
|---:|---:|---|---|
| 1 | 83 | yes | yes |
| 2 | 78 | yes | yes |
| 3 | 81 | yes | yes |
| 4 | 84 | yes | yes |
| 5 | 80 | yes | yes |
| 6 | 102 | yes | yes |
| 7 | 94 | yes | yes |
| 8 | 95 | yes | yes |
| 9 | 81 | yes | yes |
| 10 | 82 | yes | yes |
| 11 | 78 | yes | yes |
| 12 | 78 | yes | yes |
| 13 | 79 | yes | yes |
| 14 | 80 | yes | yes |
| 15 | 77 | yes | yes |
| 16 | 80 | yes | yes |
| 17 | 78 | yes | yes |
| 18 | 80 | yes | yes |
| 19 | 81 | yes | yes |
| 20 | 81 | yes | yes |

The total was 1,652 seconds (27m32s), slightly longer than the stated
10–27-minute expectation.  Tick 6 was the slowest at 102 seconds; it completed
normally and wrote both trace and receipt.

## Effects and durable outputs

`futon2.aif.forward-model/*effects-mode*` evaluated to `:target-sensitive` in a
fresh process after the run.

Each tick writes three paths:

1. `data/wm-trace/wm-trace-2026-09-01.edn`, copied here as
   `wm-trace-2026-09-01.edn`;
2. `holes/labs/wm-contract/tick-run-record-2026-09-01.edn`, moved here under
   the same basename after the run;
3. `data/wm-trace/.lane-futility-index.edn`, copied here as
   `lane-futility-index.edn`.

The lane futility index is **persistent state mutated by this run and carried
between ticks**.  The files here are the final snapshots after tick 20; only
the trace is append-only across all twenty ticks.

## Field audit

The trace was read with `clojure.edn/read` in a loop.  It contains exactly 20
forms, each with 145 ranked actions.

| required datum | trace forms carrying it | finding |
|---|---:|---|
| `:wm/route` | 0/20 | **missing from every trace form**; only final overwritten receipt retains a route |
| `:prediction-mean` | 20/20 | present per candidate |
| `:prediction-variance` | 20/20 | present per candidate |
| `:prediction-variance-status` | 20/20 | present per candidate |
| `:softmax-weights-by-candidate-id` | 20/20 | present |
| `:controller-score` | 20/20 | present per candidate |
| `:f-pi-by-candidate-id` envelope | 20/20 | present |

The controller-score limitation of the July fields does **not** persist in
these records: all twenty carry the machine's own `:controller-score` for each
ranked candidate.  The dark F-pi envelopes have status `:absent` on tick 1 and
`:present` on ticks 2–20.  The first absence is consistent with the lack of a
previous tick prediction at the start, and it is retained here as an observed
result rather than filled in.
