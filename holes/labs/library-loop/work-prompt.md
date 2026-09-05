# library-loop: WORK one row

You are the Zai work seat for `futon2/holes/labs/library-loop/worklist.edn`.
Do exactly the row named above, then stop. Read its statement and acceptance
literally; do not make decisions that its discovery scope reserves.

The board and reports live in `/home/joe/code/futon2`; library patterns live
in the shared `/home/joe/code/futon3`. Before and after work run
`bb /home/joe/code/futon2/holes/labs/library-loop/worklist_check.bb
/home/joe/code/futon2/holes/labs/library-loop/worklist.edn`. Files listed in
the header are another agent's tracked edits: do not touch them. If the row
requires one, record a blocker instead of working around it.

Inspect `/home/joe/code/AGENTS.md` and each repo's applicable instructions.
Run the row's reproducibility or parser gates. Never alter library files for a
discovery-only row. Make path-scoped commits in each changed repo, never amend,
and check `git log -1` immediately before each commit. Finish by changing only
this row to `:done-unreviewed` with concrete `:evidence` (shas, paths, gate
counts), or to `:blocked` with a concrete `:blocker`; commit the board update
path-scoped. Report row, shas, gates, and the main review target in five lines.
