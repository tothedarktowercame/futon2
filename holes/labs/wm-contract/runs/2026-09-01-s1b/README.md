# War Machine S1b shadow run — 2026-09-01 (re-run of S1 with route persistence, RUN10)

Code sha `6917ba0dbd7cb3e2aec253e6aa95ed75384eaa40` (RUN10: persist per-tick routes). `git rev-parse
HEAD` moved to `fb7f503` during the run; `git diff --stat 6917ba0 fb7f503 -- src scripts test deps.edn`
is empty — that commit touched the ledger only. The code that ran is 6917ba0's.

## Pre-flight (same sha, immediately before)
`clojure -M:test holes/labs/wm-contract/r6_zero_post_preflight.clj` → PASS: POSTs attempted 0;
paths read 1564; .admintoken reads 0. Real diagnostic tick, 80 s.

## Command (verbatim, ×20, via /tmp/s1b-run.sh with a timing wrapper that alters nothing)
    FUTON_WM_TRACE_POLICY_DETAILS=1 FUTON_WM_FPI_DARK=1 clojure -M -m futon2.run-tick-once 14

## Boundary (see COLLISION-NOTE.md)
The per-date trace file is shared and records carry no run id (RUN11 open). S1b = records with
`:timestamp >= 2026-09-01T16:48:01Z` (epoch 1788281281); zero other run-tick-once JVMs were alive at
relaunch (pgrep on the java command line). `ticks.csv` gives each tick's [start,end] epoch window.
`wm-trace-s1b.edn` here is exactly those records, one form per line — read with `edn/read` in a loop.

## Result
- 20/20 ticks exit 0; wall 76–81 s per tick (ticks.csv); run 16:48:01 → 17:14:09 UTC.
- 20/20 records carry `:wm/route`; every route has 10 tags; a hop is a consecutive pair of
  `{:node :via :at}` tags (the receipt's from/to is a rendering).
- First route's nodes: R20 R12 R2 R7 R3 R8 R5 R6 R14 TRACE.
- 20/20 records carry `:controller-score` on ranked actions (the rank-proxy limit of C464 lifts).
- Dark F_π: NOT CHECKED here by key (the reader did not know the key name); RUN3/RUN7 to confirm.
- Receipts written in the window copied here (per-run ids, RUN10).

## Not claimed
Nothing about conformance: that is RUN3, on these twenty routes, with the by-grounds rule.
