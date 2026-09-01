# War Machine S2 shadow run — 2026-09-01 (RUN7, stage S2: dark beta)

Code sha `ed2be78` (`039b0b8` is the dark-beta implementation; `ed2be78` adds only the pre-flight
repair below and the arms script — `git diff 039b0b8 ed2be78 -- src scripts` is empty, so the
war-machine code that ran is `039b0b8`'s).

## Pre-flight (same sha, immediately before the run)
`clojure -M:test holes/labs/wm-contract/r6_zero_post_preflight.clj` → PASS: POSTs attempted 0;
paths read 1565; `.admintoken` reads 0; run lock released. Real diagnostic tick, 40.6 s.

### The first attempt was refused, by a lock its own pre-flight had stranded
The pre-flight intercepts `spit` so the tick leaves no artifact. `wm-run-lock/acquire!`
(RUN12) takes the lock by `.createNewFile` then `spit`ting the holder record; `release!`
deletes the file only when it can read its own token back out of it. With the write
intercepted the lock file stayed **zero bytes**, release read `:not-ours`, and the file
survived — which is exactly the state RUN12 fails closed on ("a lock file with no pid is
what an acquirer looks like between creating the file and writing it"). So the first S2
run attempt exited 3 against a lock nobody held, and every later run would have too.

The two mechanisms had never met: RUN12 ran no tick, and every pre-flight before it
predates the lock. Repaired in `ed2be78` — run-lock writes pass through, so a pre-flight
tick holds and releases the lock like the tick it is, and the pre-flight now prints whether
the lock is released when it exits.

## Command (verbatim, ×20, under one run lock — RUN12's `wm_run.sh`)

    FUTON_WM_TRACE_POLICY_DETAILS=1 FUTON_WM_FPI_DARK=1 FUTON_WM_BETA_DARK=1 \
      bash holes/labs/wm-contract/wm_run.sh 20 14 claude-20-s2

Three flags, coupled in a chain: `BETA_DARK` consumes what `FPI_DARK` computes, which
consumes what `TRACE_POLICY_DETAILS` wrote on the previous tick.

## Selection
`wm-trace-s2.edn` here is the records of the shared per-date file whose `:run/id` is one of
this run's receipts' — RUN11's by-identity selection, not a timestamp range.

## Result
- **20/20 ticks exit 0**, rc=0. 18:39:18 → ~19:07:15 UTC. Per-tick wall 78–93 s, mean 84 s
  (from the 20 start stamps in `run.log`); the whole run ~28 min.
- Three files written per tick, as RUN6b's pre-flight established: the trace record, the
  run receipt, and `data/wm-trace/.lane-futility-index.edn` — the last is **persistent state
  carried between ticks and mutated by this run**. Shared per-date trace file grew
  52,151,952 → 73,085,797 bytes.
- Selection: **by `:run/id`**, 20 receipts / 20 run ids, 20 of the 70 records sharing the
  per-date file. This is the first time RUN11's by-identity path has selected real records
  rather than a planted control — before this run no record in existence carried the key.
- **20/20 records carry `:policy-precision-state`**, `:status :present`,
  `:beta-source :converged-posterior`, `:converged? true`, `:bracketed? true`.
  145 aligned candidates on 19 ticks, 143 on tick 1.
- Cost: **646 bytes on a 1,045,650-byte record, 0.0618%.** The two 145-element
  distributions (π, π₀) are dropped and only the scalars kept.

### The carry, with its bound stated first
**β₀ = 1.0, 20 ticks.** C22/V6: a carried β over ~20 ticks is still dominated by its β₀ —
the four 07-04 trajectories were 0.50, 0.87, 1.58 and 2.48 apart at t=17 and only contracted
between t=17 and t=37. **So nothing below is converged and must not be read as such.**

    tick  1  β 1.002646073  γ 0.997360910
    tick 10  β 1.017837739  γ 0.982474869
    tick 20  β 1.034342317  γ 0.966797920

Monotone, ~+0.0017 β per tick, no reversal. Over 20 ticks γ falls 3.1%: were this live, the
selection temperature τ = β would have risen 3.4% — a flatter softmax, not a sharper one.

### Reproduction, by a solver that is not the one that ran
`run7_beta_arms.clj` carries its own bisection (it has to: the shipped `converge-beta` cannot
put ln E in π₀). Its ln-E-in-neither setting is checked against `converge-beta` first
(δ 0.0), and then reproduces all twenty recorded β from their recorded priors:
**delta 0.00e+00 on 20/20 ticks.** So the persisted numbers are not merely self-consistent.

### Conformance (not required by RUN7's acceptance; run because it was cheap)
`bb run3_conformance.bb runs/2026-09-01-s2` → **CONFORMANT**, selection by-run-id.
20 records, 20 routes, 180 hops, 9 distinct; 0 refutations, 0 unmapped; 1 ruling-unrealised
(R5→R6), 1 excluded dependency-grain (R2→R7), 19 of 22 drawn edges never fired. Identical to
S1b's verdict — twenty ticks again took one route.

## Not claimed
Nothing about what live β would select: π here includes F_π, which today's selection does
not. The pre-flight lock repair is not evidence that the tick is safe — the PASS is.
