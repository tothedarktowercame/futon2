# Stage S5 — 2026-09-01 (I5 slice (c)): the R8 scalar F is gone from the record

Four ticks, one run lock (RUN12), each record carrying its `:run/id` (RUN11),
at futon2 sha `5a66411` — the commit that removed
`compute-variational-free-energy`, its per-tick call, the judgement key and the
record key, and retagged `:R8` at `fe/compute-prediction-error`.

    bash holes/labs/wm-contract/wm_run.sh 4 14 claude-20

`wm-trace-s5.edn` holds the four records, selected out of the shared per-date
trace file by `:run/id` (4e35e740, b69ec193, c51a8da3, 28da19d2).

**What the run shows.** All four records: `:producer-contract`
`:r8/retired-f-controller-v1`, `:wm-version :trace-schema-version` 21, and no
`:variational-free-energy` key at all — absent, not nil. `:selection-gain` and
the controller-map `:free-energy` shape are still present, which is why the r8
era needed a third member rather than a bumped version string.

The `:R8` hop is `R3 → R8` with
`:via "futon2.aif.free-energy/compute-prediction-error"`, so the route is
unchanged in shape: `R20 R12 R2 R7 R3 R8 R5 R6 R14 TRACE`, nine hops.

**One orphan record outside this run.** The first launch of `wm_run.sh` was
killed by a harness timeout partway through its first tick; the lock released
cleanly (the script's EXIT trap) and one record survives at
`2026-09-01T22:48:41Z`, `:run/id 84ca8231-…`, with the same contract and the
same absence. It is a valid record and it is NOT part of S5: run3 selects by
`:run/id`, so it is excluded by construction rather than by judgement. Its
receipt is left where the tick wrote it, beside the other loose receipts of the
day.

Full account: `../../C473-f-scalar-removal.md`.
