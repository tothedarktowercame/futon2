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

---

## Review addendum — 2026-08-26, claude-13

Verdict **confirmed on the narrow question**, and I verified it rather than
taking the trace: `scripts/wm_outer_loop.clj:98` is `(trace/read-trace-range
start end)`, and the ns `:require` list is `trace`, `intrinsic-values`,
`clojure.java.shell`, `clojure.string` — **no `enact`**. The only production
caller of `with-realized-outcome` is `src/futon2/aif/enact.clj:254`.

**But the seam is one step further over, and it overturns the premise this slice
was dispatched under.**

There are **two runners**. `scripts/wm_outer_loop.clj` reads traces and does not
enact. `scripts/wm_scheduled_run.clj` requires `futon2.aif.enact` (line 25) and
calls `(enact/close-loop! judgement (System/currentTimeMillis))` at line 108,
under `(live-wire?)`. That is the enacting path, and it writes to
`data/wm-trace/`, not to `data/wm-full-loop/`.

### Step ⑨ did fire — for five days

    data/wm-trace/wm-trace-2026-07-02.edn   18 realized outcomes
                             2026-07-03     18
                             2026-07-04     37
                             2026-07-05     13
                             2026-07-06      2
                             2026-07-09 …    0   (through the last file, 07-21)

**88 in total.** And each carries both legs:

    :realized-outcome {:policy "M-bayesian-structure-learning"
                       :expected-G -0.2, :realized-G -0.5, :tick 1783148628039}

That is exactly the per-tick mismatch R8 asks for. **R8's `:built` gate was met
in early July and then lapsed.**

### Three things this corrects

1. **My own finding.** "Zero realized outcomes across 62 attempts" was measured
   in `data/wm-full-loop/wm-outer-loop-*` — the archive of the runner that does
   not enact. Right measurement, wrong corpus.
2. **R8's own promotion test.** Its `:retro-trip` says *"the mismatch does NOT
   [exist]… 'replay with the mismatch frozen' cannot be run against the archive;
   there is nothing to freeze."* There are 88 mismatches to freeze, in a
   different directory. That note was corrected on 2026-08-25 "after checking
   the archive rather than assuming it" — and checked the same wrong archive.
3. **`E-R8-red-ring-fill`'s premise.** "The placeholder has never been filled"
   is wrong; it was filled for five days. The live question is **why it stopped
   on 2026-07-06**, which is a different and better question.

### What the mismatch data will and will not support

Parsed strictly, 77 of the 88 records give both legs. The mismatch varies —
`0.0` ×18, `-0.3` ×56, `-0.09` ×3 — so it is signal, not a constant.

**But all 77 carry one policy, `M-bayesian-structure-learning`.** R8's null
control asks whether *"the subsequent reward action differs from the action the
same tick sequence produces with the mismatch held constant"*. With a single
policy in the corpus there is no action sequence to differ. So the null control
is **closer to runnable than the retro-trip note claims and still not runnable
as stated** — and that limit is about policy diversity, not about the mismatch.

### Chain decision

Slice 2 is not dispatched. Its stated prerequisite is satisfiable in a way
nobody expected, but its acceptance test is not, and the more valuable question
now displaces it: **what stopped on 2026-07-06?**
