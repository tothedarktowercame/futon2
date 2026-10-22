# Packet (discovery only, no code change): where are the habit counts? (2026-10-22)

From claude-2. Read-only. Joe approved (2026-10-22) changing what clicks record
so R20 rows can be filled; before building the habit part I need facts.

In both recent clicks (`data/wm-runs/tick-run-record-2026-10-21-fcf06c3f-*.edn`,
`...-752b48a7-*.edn`) `:habit-reads` is `{:status :absent :reason :never-read}`
and `:habit-reinforcement` is `{:absent :legacy-habit-store-retired}`
(`src/futon2/aif/full_loop_runner.clj` ~6347; the comment says the legacy store
was replaced by a W_c-gated habit fold, WM-HABIT-FOLD-WIRE-D, 655bbced).

Lean wants (`R15HabitPrior` / `R15TemporalHierarchy.TwoTickRecord`, `R20HabitPrior`):
counts of witnessed successes per class, +1 per witnessed success, carried to the
next click, normalised into the E used in the policy prior and the precision update.

## Questions
1. Where does the W_c-gated habit fold keep its counts now, and is it running in
   live clicks? Cite file:line and a run record.
2. What E does the live policy prior actually use (uniform? another source)?
   Cite the code path.
3. Why is `:habit-reads` `:never-read`: nothing reads the fold, or the read is
   not recorded?
4. What is the smallest change that would record before-counts, after-counts,
   outcome, and the counts the next click consumes? Describe, do not build.

Write answers to `holes/labs/wm-contract/NOTE-r20-habit-counts-2026-10-22.md`,
commit only that note. Bell claude-2 back with a summary + commit sha.
