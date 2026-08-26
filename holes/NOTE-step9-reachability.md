# Step ⑨ reachability from `wm-outer-loop`

**Answer: no.** The `:wm-outer-loop` execution path does not reach
`futon2.aif.fold-realized/with-realized-outcome`.

## Static path

The reader-checkable path is:

1. `deps.edn:24` maps `:wm-outer-loop` to `wm-outer-loop/-main`.
2. `scripts/wm_outer_loop.clj:401` defines `-main`, which calls `run-once!` at
   `scripts/wm_outer_loop.clj:410-411`.
3. `run-once!` reads existing trace records through `read-window-records` at
   `scripts/wm_outer_loop.clj:286`.
4. `read-window-records` calls `futon2.aif.trace/read-trace-range` at
   `scripts/wm_outer_loop.clj:98`. This is the last reached function on the
   trace branch toward realized-outcome data. The remainder of `run-once!`
   extracts action-class emissions and updates R12 intrinsic-value posteriors;
   the namespace requires only `trace` and `intrinsic-values`
   (`scripts/wm_outer_loop.clj:34-37`) and has no call to `enact/close-loop!` or
   `fold-realized/with-realized-outcome`.

## Specific seam

`with-realized-outcome` belongs to the trace-producing R10 path, not the R12
outer-loop trace reader. `wm-scheduled-run/-main` calls `enact/close-loop!` at
`scripts/wm_scheduled_run.clj:106-109`; `close-loop!` conditionally calls
`with-realized-outcome` at `src/futon2/aif/enact.clj:250-254`; and that runner
then persists the resulting judgement with `trace/write-trace!` at
`scripts/wm_scheduled_run.clj:113`. In contrast, `wm-outer-loop` begins on the
other side of that persisted-trace seam by calling `trace/read-trace-range`.

Therefore an outer-loop invocation cannot itself produce a
`:realized-outcome`; it can only read one that an R10 tick previously wrote.
No live run is needed to settle this reachability question.
