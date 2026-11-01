# L5-23-D — Discovery: why 589 of 591 tasks have no known relation to the focus (claude-2 → codex-15, 2026-11-01)

Read-only; report only; worktree `~/worktrees/futon2/codex15-l5-23-d`, branch `codex15/l5-23-d`
(never check out a branch in `/home/joe/code/futon2`). Report to
`holes/labs/wm-contract/reconciliation/L5-23-D-relation-coverage-codex15-2026-11-01.md`.

## Why this matters

L5-19 (merged `95145ffbc`) rates every admitted task with the real outer scorer. On the click-1
replay only the two cascade picks had a known relation to the focus ("WM"); every other task was
`:unknown`, and an unknown relation lands on the stopped kernel, so 589 of 591 ratings equalled
stopping. L5-21 (in flight) gives an unknown relation a prior, but a prior is not knowledge: the
ranking Joe asked for needs the relation producer to say something about most of the field.

The producer is `src/futon2/aif/focus_receipt.clj`: (a) `stated-relation-path`, breadth-first over
a task's stated Relations through M- targets, `relation-hop-bound` = 2, to the first target with a
classified row (the hand-reviewed anchors); (b) an embedding route gated on a declared
`:embedding-floor` (Joe's ruling: 0.2). Each outcome is a reading with `:derived-via` or a typed
`:absent` reason (`:mission-text-not-found`, `:no-stated-path-to-a-classified-target`,
`:embedding-floor-not-declared`, `:graph-hop-bound-not-declared`, …).

## Questions

1. **Coverage census.** The first live record with `[:outer/selection :per-task-rating]` is the
   click now running (loop `bg-1793569682251-293`, click 1, started 21:48Z; look for the newest
   `data/wm-runs/selection-record-2026-11-01-wm-click-*.edn` carrying that key). From its rows (or,
   if it has not landed, by running the producer over the 591 admitted tasks of
   `selection-record-…wm-click-3c866bd4…edn` with the same inputs the click used): tabulate the
   relation outcomes — `:focus / :associated / :useful-elsewhere` vs each `:absent` reason — with
   counts. Which absence dominates?
2. **Anchors and hops.** How many classified anchor rows exist, which tasks they are, and how many
   admitted tasks reach one within 2 hops; within 3; within 4. Say what the hop bound buys and costs.
3. **Stated relations in files.** How many of the 591 task files carry a Relations section the
   producer can read at all (`stated-relations`), and how many of those name an M- target in the
   field. If most tasks state no relations, no hop bound helps; say so.
4. **Embedding route.** Is `:embedding-floor` declared on the click's inputs today (the record's
   `:construction :kernel-input` or the focus receipt), and does the route run? If it is absent, what
   declares it (Joe ruled 0.2) and what would the census look like with it on (compute it if the
   embeddings exist; otherwise say what is missing).
5. **Cost.** Wall time of the producer over 591 tasks per click, measured.

Bell claude-2 with the sha, the census table, and one recommendation (widen hops / declare the floor
/ author relations / something else) with its expected coverage.
